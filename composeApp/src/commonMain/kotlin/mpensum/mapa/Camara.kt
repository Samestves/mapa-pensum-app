package mpensum.mapa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.TimeSource

/*
 * La cámara del mapa, con las reglas de la web (hooks/useVistaGrafo.js,
 * layout/limitesVista.js y layout/camara.js). Todo en dp de pantalla, que
 * son los px de CSS de la web: las mismas cuentas sirven sin traducir nada.
 * La vista es {x, y, escala}: un punto (px, py) del plano se ve en
 * (x + px·escala, y + py·escala). Solo la lee la capa que mueve el plano.
 */

/** Cuánto se deja pasar del borde: llegar al final no se siente como chocar contra una pared */
private const val MARGEN_PAN = 96f
/** La ficha tapa la parte de abajo: el mapa sube hasta dejar su borde a media pantalla */
private const val HOLGURA_TELEFONO = 0.62f
const val ZOOM_MIN = 0.08f
const val ZOOM_MAX = 2.5f
/** La inercia de iOS: pierde dos tercios de su velocidad en 325 ms */
private const val FRICCION_MS = 325f
/** Por debajo de esta velocidad (dp/ms) soltar no lanza el mapa */
private const val LANZAMIENTO_MIN = 0.25f
/** Y a esta se da por parado: algo más de un dp por cuadro */
private const val VELOCIDAD_PARADO = 0.02f
/** Lo que tarda en darse por acabado un gesto: entonces vuelve a correr la luz de los cables */
private const val FIN_GESTO_MS = 250L
/** Lo que tarda el mapa en apartarse para enseñar algo */
const val DURACION_MOSTRAR = 420
/** El doble toque acerca al doble; si ya estás muy cerca, vuelve a la escala de lectura */
private const val ACERCA_DOBLE_TOQUE = 2f

data class Vista(val x: Float, val y: Float, val escala: Float)

/** Un rectángulo en unidades del plano */
data class Caja(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

/** Márgenes de pantalla, en dp, para dejar algo a la vista */
data class Margenes(val arriba: Float = 40f, val abajo: Float = 40f, val izq: Float = 40f, val der: Float = 40f)

/** Al aprobar, en el teléfono: abajo queda la barra de las vistas (layout/cajas.js) */
val MARGEN_AL_APROBAR = Margenes(arriba = 32f, abajo = 112f, izq = 24f, der = 24f)

/**
 * La escala a la que se lee el mapa en un teléfono: tu semestre entero y algo
 * más de media columna del siguiente, que es donde llegan sus cables. Tope de
 * 0,8: por encima el nombre de la tarjeta ya es más grande que el texto de la pantalla.
 */
fun escalaDeLectura(ancho: Float): Float =
    if (ancho < ANCHO_COMPACTO) {
        // Teléfono estrecho: tu columna entera con su margen y, tras el hueco, solo
        // el borde de la siguiente (ASOMO dp) a propósito, para invitar a deslizar
        min(0.8f, (ancho - MARGEN_IZQ_LECTURA - ASOMO_SIGUIENTE) / (Nodo.ANCHO.toFloat() + Espaciado.COLUMNA.toFloat()))
    } else {
        min(0.8f, (ancho - 24f) / (Nodo.ANCHO.toFloat() * 1.55f + Espaciado.COLUMNA.toFloat()))
    }

/** Mismo corte que `Ventana.compacta` (360 dp); la cámara no depende de la UI */
private const val ANCHO_COMPACTO = 360f
private const val MARGEN_IZQ_LECTURA = 16f
/** Cuánto de la segunda columna se deja ver en compacta: asoma, no se parte a la mitad */
private const val ASOMO_SIGUIENTE = 40f

/** Tu semestre a escala de lectura, pegado a la izquierda */
fun vistaDeColumna(xColumna: Float, ancho: Float, arriba: Float): Vista {
    val escala = escalaDeLectura(ancho)
    return Vista(16f - xColumna * escala, arriba + 12f - Margen.ARRIBA.toFloat() * escala, escala)
}

/**
 * El primer encuadre en horizontal: la columna más alta, con su cabecera,
 * llena el alto libre (entre la barra de estado y la de gestos) y tu semestre
 * queda pegado al margen izquierdo, que es lo que ocupa el riel. A esta escala
 * no hace falta `escalaDeLectura`: el alto manda, no el ancho.
 */
fun vistaDeAltura(xColumna: Float, finColumna: Float, alto: Float, arriba: Float, abajo: Float, izq: Float): Vista {
    val libre = alto - (arriba + 6f) - (abajo + 14f)
    val escala = (libre / (finColumna - Margen.ARRIBA.toFloat())).coerceIn(ZOOM_MIN, ZOOM_MAX)
    return Vista(izq - xColumna * escala, arriba + 6f - Margen.ARRIBA.toFloat() * escala, escala)
}

/** Dónde dejaste cada mapa durante esta sesión: volver a la carrera te deja donde estabas */
private object CamarasDeLaSesion {
    val vistas = mutableMapOf<String, Pair<Vista, Pair<Float, Float>>>()
}

@Stable
class Camara(private val clave: String) {
    var x by mutableFloatStateOf(0f)
        private set
    var y by mutableFloatStateOf(0f)
        private set
    var escala by mutableFloatStateOf(1f)
        private set

    /** Si ya se colocó: hasta entonces el mapa no se enseña (el primer cuadro sería el plano en la esquina) */
    var colocada by mutableStateOf(false)
        private set

    /** Si el mapa ya hizo su llegada: solo la hace la primera vez, no cada vez que se vuelve a la vista */
    var yaLlego = false

    /** Mientras algo mueve el mapa (dedo, inercia o viaje): la luz de los cables se congela */
    var enGesto by mutableStateOf(false)
        private set

    /** Si el gesto en curso ya es del mapa (arrastre o pellizco): una tarjeta debajo deja de contar el toque */
    internal var tomado = false

    var ancho = 0f
        private set
    var alto = 0f
        private set
    private var anchoContenido = 0f
    private var altoContenido = 0f
    private var movimiento: Job? = null
    /** El vigilante que da el gesto por acabado; uno solo, no una corrutina por evento */
    private var vigilante: Job? = null
    private var ultimoMovimiento = TimeSource.Monotonic.markNow()

    val vista get() = Vista(x, y, escala)

    fun medir(anchoVentana: Float, altoVentana: Float, anchoPlano: Float, altoPlano: Float) {
        ancho = anchoVentana
        alto = altoVentana
        anchoContenido = anchoPlano
        altoContenido = altoPlano
        if (colocada) aplicar(vista)
    }

    /** La primera vez: donde la dejaste en esta sesión si la pantalla no cambió de ancho, o la que se pida */
    fun colocarInicial(inicial: () -> Vista) {
        if (colocada || ancho == 0f) return
        val guardada = CamarasDeLaSesion.vistas[clave]?.takeIf { (_, medida) -> abs(medida.first - ancho) <= 2f && abs(medida.second - alto) <= 160f }
        aplicar(guardada?.first ?: inicial())
        colocada = true
        recordar()
    }

    private fun recordar() {
        CamarasDeLaSesion.vistas[clave] = vista to (ancho to alto)
    }

    /** Todo pasa por aquí: acotar en este punto basta para que el mapa no pueda quedar fuera de la pantalla */
    private fun aplicar(v: Vista): Vista {
        val acotada = acotar(v)
        x = acotada.x
        y = acotada.y
        escala = acotada.escala
        return acotada
    }

    fun acotar(v: Vista): Vista {
        if (ancho == 0f || alto == 0f) return v
        fun rango(ventana: Float, contenido: Float, extra: Float = 0f): Pair<Float, Float> {
            val suelo = ventana - contenido - MARGEN_PAN - extra
            return if (suelo <= MARGEN_PAN) suelo to MARGEN_PAN else MARGEN_PAN to suelo
        }
        val e = v.escala.coerceIn(ZOOM_MIN, ZOOM_MAX)
        val (minX, maxX) = rango(ancho, anchoContenido * e)
        val (minY, maxY) = rango(alto, altoContenido * e, alto * HOLGURA_TELEFONO)
        return Vista(v.x.coerceIn(minX, maxX), v.y.coerceIn(minY, maxY), e)
    }

    /** Marca que el mapa se está moviendo; se da por quieto un cuarto de segundo después del último movimiento */
    fun marcarGesto(alcance: CoroutineScope) {
        enGesto = true
        ultimoMovimiento = TimeSource.Monotonic.markNow()
        // Cada evento y cada cuadro de la inercia llegan aquí: solo anotan la hora, y el vigilante, que duerme
        // lo que falta, es el único que se lanza
        if (vigilante?.isActive == true) return
        vigilante = alcance.launch {
            while (true) {
                val falta = FIN_GESTO_MS - ultimoMovimiento.elapsedNow().inWholeMilliseconds
                if (falta <= 0) break
                delay(falta)
            }
            enGesto = false
            recordar()
        }
    }

    /** La mano manda: un dedo a mitad de viaje o de inercia se queda con el mapa donde iba */
    fun detener() {
        movimiento?.cancel()
        movimiento = null
    }

    fun desplazar(dx: Float, dy: Float) {
        aplicar(Vista(x + dx, y + dy, escala))
    }

    /** Zoom manteniendo fijo el punto (px, py) de la pantalla */
    fun zoomEn(factor: Float, px: Float, py: Float) {
        aplicar(conZoom(vista, factor, px, py))
    }

    private fun conZoom(v: Vista, factor: Float, px: Float, py: Float): Vista {
        val e = (v.escala * factor).coerceIn(ZOOM_MIN, ZOOM_MAX)
        val k = e / v.escala
        return Vista(px - (px - v.x) * k, py - (py - v.y) * k, e)
    }

    /** Salta a una vista sin viaje ni gesto: la mueve quien lleva su propio reloj (la guía de bienvenida) */
    fun irA(v: Vista) {
        detener()
        aplicar(v)
    }

    /** Lleva la vista hasta `hasta` en `duracion` ms, saliendo rápido y frenando al llegar (easeOutCubic) */
    fun animarHacia(alcance: CoroutineScope, hasta: Vista, duracion: Int) {
        detener()
        val desde = vista
        val destino = acotar(hasta)
        movimiento = alcance.launch {
            Animatable(0f).animateTo(1f, tween(duracion, easing = LinearEasing)) {
                val k = 1f - (1f - value).pow(3)
                aplicar(Vista(desde.x + (destino.x - desde.x) * k, desde.y + (destino.y - desde.y) * k, desde.escala + (destino.escala - desde.escala) * k))
                marcarGesto(alcance)
            }
        }
    }

    /**
     * Mueve la vista lo justo para que se vea `caja` dentro de los márgenes.
     * Si ya se ve, no se mueve nada; si asoma por un lado, solo se corre ese
     * lado. Si no cabe a esta escala se aleja lo necesario; acercarse, nunca.
     * Con `centrar`, la caja queda en el centro del hueco libre aunque ya se viera.
     */
    fun mostrar(alcance: CoroutineScope, caja: Caja, m: Margenes, centrar: Boolean = false) {
        if (ancho == 0f || alto == 0f) return
        val v = vista
        val libreX = ancho - m.izq - m.der
        val libreY = alto - m.arriba - m.abajo
        val e = minOf(v.escala, libreX / (caja.x1 - caja.x0), libreY / (caja.y1 - caja.y0)).coerceIn(ZOOM_MIN, ZOOM_MAX)
        // Al alejarse, el centro de la caja se queda donde estaba en pantalla
        val cx = (caja.x0 + caja.x1) / 2
        val cy = (caja.y0 + caja.y1) / 2
        val x0 = v.x + cx * (v.escala - e)
        val y0 = v.y + cy * (v.escala - e)
        fun correr(pos: Float, a: Float, b: Float, min: Float, max: Float): Float {
            val inicio = pos + a * e
            val fin = pos + b * e
            return when {
                centrar || fin - inicio > max - min -> pos + (min + max) / 2 - (inicio + fin) / 2
                inicio < min -> pos + min - inicio
                fin > max -> pos - (fin - max)
                else -> pos
            }
        }
        val hasta = Vista(correr(x0, caja.x0, caja.x1, m.izq, ancho - m.der), correr(y0, caja.y0, caja.y1, m.arriba, alto - m.abajo), e)
        val quieta = abs(hasta.x - v.x) < 1 && abs(hasta.y - v.y) < 1 && abs(hasta.escala - v.escala) < 0.001f
        if (!quieta) animarHacia(alcance, hasta, DURACION_MOSTRAR)
    }

    /** Doble toque en el vacío: acerca al doble alrededor del dedo, o vuelve a la escala de lectura */
    fun dobleToque(alcance: CoroutineScope, px: Float, py: Float) {
        val factor = if (escala >= 1.6f) escalaDeLectura(ancho) / escala else ACERCA_DOBLE_TOQUE
        animarHacia(alcance, conZoom(vista, factor, px, py), 300)
    }

    /**
     * Sigue sola tras soltar el arrastre, como cualquier lista de un teléfono.
     * El recorrido entero se sabe al soltar: avance = v·τ·(1 − e^(−t/τ)), y
     * cada eje se queda en el borde si lo toca (trayectoriaDeLanzamiento).
     * `vx`, `vy` en dp por milisegundo.
     */
    fun lanzar(alcance: CoroutineScope, vx: Float, vy: Float) {
        val rapidez = hypot(vx, vy)
        if (rapidez < LANZAMIENTO_MIN) return
        val desde = vista
        fun libre(t: Float): Vista {
            val avance = FRICCION_MS * (1 - exp(-t / FRICCION_MS))
            return Vista(desde.x + vx * avance, desde.y + vy * avance, desde.escala)
        }
        val hastaParar = FRICCION_MS * ln(rapidez / VELOCIDAD_PARADO)
        val destino = acotar(libre(hastaParar))
        val sinBorde = libre(hastaParar)
        fun llegada(v: Float, recorrido: Float, recorridoLibre: Float): Float {
            if (v == 0f || recorrido == 0f) return 0f
            if (recorrido == recorridoLibre) return hastaParar
            val hecho = recorrido / (v * FRICCION_MS)
            return if (hecho > 0 && hecho < 1) -FRICCION_MS * ln(1 - hecho) else hastaParar
        }
        val duracion = min(hastaParar, maxOf(llegada(vx, destino.x - desde.x, sinBorde.x - desde.x), llegada(vy, destino.y - desde.y, sinBorde.y - desde.y)))
        if (duracion <= 0f) return
        detener()
        movimiento = alcance.launch {
            Animatable(0f).animateTo(duracion, tween(duracion.toInt(), easing = LinearEasing)) {
                aplicar(libre(value))
                marcarGesto(alcance)
            }
        }
    }
}
