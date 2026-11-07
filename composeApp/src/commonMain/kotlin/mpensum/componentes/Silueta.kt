package mpensum.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import mpensum.tema.Paleta
import mpensum.tema.Tema
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min
import kotlin.math.sin

/** Lo que ocupa una silueta: columnas de puntos, una por semestre */
fun tamSilueta(silueta: List<Int>, punto: Dp, hueco: Dp, huecoFila: Dp): Pair<Dp, Dp> {
    val filas = silueta.maxOrNull() ?: 0
    val ancho = punto * silueta.size + hueco * (silueta.size - 1)
    val alto = punto * filas + huecoFila * max(0, filas - 1)
    return ancho to alto
}

/**
 * La silueta de una carrera: un punto por materia, una columna por semestre.
 * Es el icono de cada carrera en la web. Con `hechas`, lo aprobado va
 * encendido y lo demás tenue. Con `onda`, una luz recorre los puntos de
 * izquierda a derecha cada pocos segundos. Con `brillo`, la fila de arriba
 * lleva un halo del color de la carrera.
 *
 * `apagado` es cuánto se ve lo que no has aprobado, y `encendido(columna,
 * fila)` cuánto ha llegado a encenderse cada punto aprobado (de 0 a 1): se lee
 * al dibujar, así una entrada escalonada no recompone la silueta. El alfa del
 * `color` se respeta, para siluetas enteras más tenues.
 *
 * Con `porEstado` (las cursando y las disponibles por columna) cada columna se
 * pinta de arriba abajo: aprobadas en `color`, cursando en contorno, disponibles
 * en `inscribible` y el resto apagado. El contorno se barre con `encendido`;
 * las disponibles se funden con `aparece(orden)`, en orden de lectura.
 */
@Composable
fun Silueta(
    silueta: List<Int>,
    color: Color,
    modifier: Modifier = Modifier,
    hechas: List<Int>? = null,
    punto: Dp = 6.dp,
    hueco: Dp = 6.dp,
    huecoFila: Dp = 3.dp,
    onda: Boolean = false,
    brillo: Boolean = false,
    apagado: Float = 0.28f,
    encendido: (columna: Int, fila: Int) -> Float = { _, _ -> 1f },
    porEstado: Pair<List<Int>, List<Int>>? = null,
    inscribible: Color = Paleta.azul,
    aparece: (orden: Int) -> Float = { 1f },
) {
    val (ancho, alto) = tamSilueta(silueta, punto, hueco, huecoFila)
    // El reloj de la onda se lee al dibujar: la silueta no se recompone en cada fotograma
    val reloj: State<Float>? = if (onda && !Tema.sinBucles) {
        rememberInfiniteTransition(label = "onda")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    } else null
    // Con onda, en su propia capa: cada cuadro repinta los puntos y no la tarjeta que la lleva
    Canvas(modifier.size(ancho, alto).then(if (reloj != null) Modifier.graphicsLayer {} else Modifier)) {
        val p = punto.toPx()
        val h = hueco.toPx()
        val hf = huecoFila.toPx()
        val t = reloj?.value ?: -1f
        val trazo = Stroke(1.5.dp.toPx())
        var orden = 0
        silueta.forEachIndexed { c, n ->
            for (j in 0 until n) {
                if (porEstado != null && hechas != null) {
                    val hechasCol = hechas.getOrNull(c) ?: 0
                    val cursandoCol = porEstado.first.getOrNull(c) ?: 0
                    val disponiblesCol = porEstado.second.getOrNull(c) ?: 0
                    val centro = Offset(c * (p + h) + p / 2f, j * (p + hf) + p / 2f)
                    val r = p / 2f
                    when {
                        j < hechasCol -> drawCircle(color.copy(alpha = color.alpha * (apagado + (1f - apagado) * encendido(c, j))), r, centro)
                        j < hechasCol + cursandoCol -> {
                            // El contorno entero apagado de fondo y encima el barrido que lo dibuja
                            val f = encendido(c, j)
                            val radio = r - trazo.width / 2f
                            drawCircle(color.copy(alpha = color.alpha * apagado), radio, centro, style = trazo)
                            if (f > 0f) drawArc(
                                color, -90f, 360f * f, false,
                                Offset(centro.x - radio, centro.y - radio), androidx.compose.ui.geometry.Size(radio * 2f, radio * 2f), style = trazo,
                            )
                        }
                        j < hechasCol + cursandoCol + disponiblesCol -> {
                            val f = aparece(orden++)
                            drawCircle(color.copy(alpha = color.alpha * apagado * (1f - f)), r, centro)
                            if (f > 0f) drawCircle(inscribible.copy(alpha = f), r, centro)
                        }
                        else -> drawCircle(color.copy(alpha = color.alpha * apagado), r, centro)
                    }
                    continue
                }
                val aprobado = hechas?.let { j < (it.getOrNull(c) ?: 0) } ?: true
                val alfa = when {
                    hechas == null -> 1f - j * 0.08f
                    aprobado -> apagado + (1f - apagado) * encendido(c, j)
                    else -> apagado
                }
                var r = p / 2f
                var luz = 0f
                if (t >= 0f) {
                    // La onda pasa por cada columna con un pequeño retraso por fila
                    val fase = (t * 1.6f) - (c * 0.07f + j * 0.02f)
                    luz = if (fase in 0f..0.12f) sin(fase / 0.12f * PI).toFloat() else 0f
                    r *= 1f + 0.45f * luz
                }
                val centro = Offset(c * (p + h) + p / 2f, j * (p + hf) + p / 2f)
                if (brillo && j == 0) {
                    val halo = r + 8.dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), centro, halo), halo, centro)
                }
                drawCircle(
                    color = color.copy(alpha = color.alpha * min(1f, max(0f, alfa + 0.3f * luz))),
                    radius = r,
                    center = centro,
                )
            }
        }
    }
}
