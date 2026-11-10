package mpensum.componentes

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mpensum.tema.Movimiento
import kotlin.math.exp

/* La máquina de la hoja: su posición, el resorte de entrada y salida y la goma al tirar */

/** Entrada: resorte sin rebote. Con 380 de rigidez asienta en unos 350 ms, y si se interrumpe sigue desde donde iba */
private val ENTRA = spring<Float>(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = 0.5f)

/** Vuelta cuando el tirón no llegó al umbral: algo más rígido, con la velocidad del dedo */
private val VUELVE = spring<Float>(dampingRatio = 1f, stiffness = 420f, visibilityThreshold = 0.5f)

/** Lo que tarda en irse una hoja soltada por el dedo: el mínimo, más el extra que crece con la distancia */
private const val SALE_DEDO_MIN_MS = 140

private const val SALE_DEDO_EXTRA_MS = 130

/** Salida tras un tirón: arranca ya a la velocidad del dedo (pendiente 1) y acelera al final */
private val SALE_DEDO = CubicBezierEasing(0.35f, 0.35f, 0.9f, 0.55f)

/** Salida por atrás o por el aspa, desde arriba del todo: aceleración (Material), arranca lenta y se va */
private const val SALE_MS = Movimiento.SALIDA_HOJA

/** Cuánto hay que bajarla (fracción de su alto) o a qué velocidad (dp/s) basta un tirón corto */
private const val UMBRAL_FRACCION = 0.25f

private const val UMBRAL_VELOCIDAD_DP = 1200f

/** Lo más que se estira hacia arriba, en dp: la goma tiende a este tope sin llegar */
internal const val GOMA_DP = 40f

/** Posición de la hoja que no ha entrado: fuera de cualquier pantalla */
internal const val OCULTA = 1e6f

/**
 * La posición de la hoja y todo lo que la mueve. `desp` es cuánto está bajada
 * respecto a su sitio, en px: 0 abierta, `oculto()` fuera de la pantalla. La
 * entrada, la salida, el dedo y el velo salen de este único número.
 */
@Stable
internal class Hoja(private val alcance: CoroutineScope) {
    var desp by mutableFloatStateOf(OCULTA)
    var alto by mutableFloatStateOf(0f)
    var abierta = false
    var alCerrar: () -> Unit = {}
    var porDedo = false
    var insets: WindowInsets = WindowInsets(0, 0, 0, 0)
    var densidad: Density = Density(1f)
    var extraPx = 0f
    /** Si la última composición la pintó a pantalla completa (acostado): al girar cambia */
    var completa = false

    /** Donde está el dedo sin goma: negativo es por encima de su sitio */
    private var crudo = 0f
    private var tomada = false
    private var trabajo: Job? = null

    /** Lo que sobra por debajo del borde de la hoja: barra de gestos, teclado, margen y la sombra */
    fun margen() = insets.getBottom(densidad) + extraPx

    /** Cuánto hay que bajarla para que no se vea nada, ni su sombra */
    fun oculto() = alto + margen()

    /** 1 con la hoja arriba, 0 con ella fuera: el velo y el desenfoque se aclaran con el tirón */
    fun progreso(): Float = if (alto <= 0f) 0f else 1f - (desp / alto).coerceIn(0f, 1f)

    /** Una animación a la vez: la nueva (o el dedo) corta la anterior donde esté */
    fun correr(bloque: suspend () -> Unit) {
        trabajo?.cancel()
        trabajo = alcance.launch { bloque() }
    }

    /** Fuera y sin medir, como antes de montarse: la próxima vez entra desde abajo con su alto nuevo */
    fun recoger() {
        trabajo?.cancel()
        alto = 0f
        desp = OCULTA
    }

    private suspend fun animar(hasta: Float, spec: AnimationSpec<Float>, velocidad: Float = 0f) {
        animate(desp, hasta, velocidad, spec) { v, _ -> desp = v }
    }

    suspend fun entrar() {
        porDedo = false
        desp = minOf(desp, oculto())
        // Con «Reducir movimiento» la hoja llega con una curva corta, sin el resorte
        animar(0f, if (Movimiento.reducido) tween(Movimiento.CORTA, easing = Movimiento.ESTANDAR) else ENTRA)
    }

    suspend fun salir() {
        val meta = oculto()
        val restante = ((meta - desp) / meta).coerceIn(0f, 1f)
        // Por distancia: desde arriba del todo 260 ms (o 270 tras un tirón); desde casi fuera, la mitad
        if (porDedo) animar(meta, tween((SALE_DEDO_MIN_MS + SALE_DEDO_EXTRA_MS * restante).toInt(), easing = SALE_DEDO))
        else animar(meta, tween((SALE_MS * (0.5f + 0.5f * restante)).toInt(), easing = Movimiento.SALE))
    }

    /** El dedo agarra la hoja, aunque esté a medio entrar o volviendo */
    fun tomar() {
        if (tomada || !abierta) return
        tomada = true
        trabajo?.cancel()
        crudo = minOf(desp, oculto())
    }

    /** Seguimiento 1:1 hacia abajo; hacia arriba, con goma que se endurece */
    fun arrastrar(d: Float) {
        crudo += d
        desp = if (crudo >= 0f) crudo else -goma(-crudo, GOMA_DP * densidad.density)
    }

    /** Al soltar: cierra por distancia o por velocidad (px/s, positiva hacia abajo); si no, vuelve con un resorte */
    fun soltar(velocidad: Float) {
        tomada = false
        if (!abierta) return
        val dp = densidad.density
        val tiron = velocidad >= UMBRAL_VELOCIDAD_DP * dp && desp > 8f * dp
        val lejos = desp >= UMBRAL_FRACCION * alto && velocidad > -UMBRAL_VELOCIDAD_DP * dp
        if (tiron || lejos) {
            porDedo = true
            alCerrar()
        } else {
            correr { animar(0f, VUELVE, velocidad) }
        }
    }

    /**
     * El contenido manda mientras pueda: con la hoja bajada, subir el dedo la
     * devuelve antes de desplazar nada; y cuando el contenido ya está arriba
     * del todo, lo que sobra del gesto baja la hoja.
     */
    val conexion = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!abierta || source != NestedScrollSource.UserInput || available.y >= 0f || desp <= 0f) return Offset.Zero
            tomar()
            val d = maxOf(available.y, -crudo.coerceAtLeast(0f))
            arrastrar(d)
            return Offset(0f, d)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!abierta || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            tomar()
            arrastrar(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!tomada) return Velocity.Zero
            soltar(available.y)
            return Velocity(0f, available.y)
        }
    }
}

/** Goma: 1:1 al principio y cada vez más dura, sin pasar nunca de `tope` */
private fun goma(x: Float, tope: Float) = tope * (1f - exp(-x / tope))
