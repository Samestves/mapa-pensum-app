package mpensum.bienvenida

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sign

/**
 * La toma de una escena en bucle, como en cine: una curva de Hermite que
 * pasa por unas pocas claves y vuelve a la primera, así que la vuelta no
 * tiene costura. Las pendientes son monótonas (Fritsch-Carlson): ningún
 * canal se pasa de largo ni rebota. El zoom va en logaritmo, que es como lo
 * percibe el ojo.
 *
 * Cada clave es (ms, x, y, zoom, inclinación, giro); la última debe repetir
 * la primera con el ms del ciclo.
 */
class TomaEnBucle(claves: List<FloatArray>) {
    private val c: List<FloatArray> = claves.map { floatArrayOf(it[0], it[1], it[2], ln(it[3]), it[4], it[5]) }
    private val n = c.size - 1

    /** La pendiente media de un tramo; los índices dan la vuelta, por eso no hay costura */
    private fun secante(j: Int, k: Int): Float {
        val i = ((j % n) + n) % n
        val a = c[i]
        val b = c[i + 1]
        return (b[k] - a[k]) / (b[0] - a[0])
    }

    private fun pendiente(j: Int, k: Int): Float {
        val d0 = secante(j - 1, k)
        val d1 = secante(j, k)
        if (d0 * d1 <= 0f) return 0f
        val m = (d0 + d1) / 2f
        return sign(m) * min(abs(m), min(3f * abs(d0), 3f * abs(d1)))
    }

    // Las pendientes no cambian: se calculan una vez
    private val pendientes = Array(c.size) { j -> FloatArray(6) { k -> if (k == 0) 0f else pendiente(j, k) } }

    /** Escribe en `salida` (5 canales: x, y, ln zoom, inclinación, giro) la toma en el ms `t` del ciclo */
    fun en(t: Float, salida: FloatArray) {
        var i = 0
        while (i < n - 1 && c[i + 1][0] <= t) i++
        val a = c[i]
        val b = c[i + 1]
        val h = b[0] - a[0]
        val s = ((t - a[0]) / h).coerceIn(0f, 1f)
        val s2 = s * s
        val s3 = s2 * s
        for (k in 1..5) {
            salida[k - 1] = (2 * s3 - 3 * s2 + 1) * a[k] + (s3 - 2 * s2 + s) * h * pendientes[i][k] +
                (-2 * s3 + 3 * s2) * b[k] + (s3 - s2) * h * pendientes[i + 1][k]
        }
    }
}

/**
 * El reloj de una escena: los ms dentro de su ciclo, de un solo
 * withFrameMillis. Se lee solo al dibujar, así que avanzar no recompone
 * nada. Con las animaciones del sistema apagadas se queda quieto en
 * `quieto`, el momento que mejor cuenta la historia.
 */
@Composable
fun rememberRelojEscena(ciclo: Int, quieto: Float): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        if ((coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f) {
            t.floatValue = quieto
            return@LaunchedEffect
        }
        val inicio = withFrameMillis { it }
        while (true) withFrameMillis { t.floatValue = ((it - inicio) % ciclo).toFloat() }
    }
    return t
}

/** Lo que mide la escena en los tableros (390 × 468): en pantallas menores todo se encoge a la par */
private const val ANCHO_TABLERO = 390f
private const val ALTO_TABLERO = 468f

/**
 * La zona de una escena: sin recuadro, se funde con el fondo hacia los
 * cuatro lados (`arriba`, `abajo` y `lados` son las fracciones del fundido),
 * y dentro el plano visto por la toma, con la inclinación y el giro en la
 * capa y la perspectiva de los tableros (1100 dp). `dibujar` recibe el ms
 * del ciclo y dibuja en unidades del plano (dp del tablero).
 */
@Composable
fun EscenaEnBucle(
    reloj: State<Float>,
    toma: TomaEnBucle,
    arriba: Float,
    abajo: Float,
    lados: Float,
    modifier: Modifier = Modifier,
    dibujar: DrawScope.(t: Float) -> Unit,
) {
    val enCapa = remember { FloatArray(5) }
    val enDibujo = remember { FloatArray(5) }
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, arriba to Color.Black, 1f - abajo to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
                drawRect(
                    Brush.horizontalGradient(0f to Color.Transparent, lados to Color.Black, 1f - lados to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Spacer(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    toma.en(reloj.value, enCapa)
                    rotationX = enCapa[3]
                    rotationZ = enCapa[4]
                    // La perspectiva CSS de 1100 px: la cámara de Android cuenta en pulgadas de 72 px
                    cameraDistance = 1100f / 72f * density
                }
                .drawBehind {
                    val t = reloj.value
                    toma.en(t, enDibujo)
                    val ajuste = min(1f, min(size.width / density / ANCHO_TABLERO, size.height / density / ALTO_TABLERO))
                    val z = exp(enDibujo[2]) * ajuste * density
                    withTransform({
                        translate(size.width / 2f, size.height / 2f)
                        scale(z, z, Offset.Zero)
                        translate(-enDibujo[0], -enDibujo[1])
                    }) { dibujar(t) }
                },
        )
    }
}
