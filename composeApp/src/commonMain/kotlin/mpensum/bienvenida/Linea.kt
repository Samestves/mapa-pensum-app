package mpensum.bienvenida

import androidx.compose.ui.graphics.Color
import kotlin.math.min

/**
 * Herramientas para las escenas con línea de tiempo: cada escena tiene un
 * reloj de 0 a 1 que da la vuelta, y todo lo que se mueve se calcula a
 * partir de él. Así se lee en el dibujo (sin recomponer) y nada se desfasa.
 */

/** Cuánto ha avanzado `t` entre `a` y `b`, de 0 a 1 */
fun tramo(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)

/** Sube entre e0 y e1, se mantiene y baja entre s0 y s1 */
fun ventana(t: Float, e0: Float, e1: Float, s0: Float, s1: Float): Float = min(tramo(t, e0, e1), 1f - tramo(t, s0, s1))

/** Suavizado de entrada y salida */
fun suave(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f

fun mezcla(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Interpola por fotogramas clave (tiempo, valores), suavizando cada tramo */
fun claves(t: Float, puntos: List<Pair<Float, FloatArray>>): FloatArray {
    if (t <= puntos.first().first) return puntos.first().second
    for (i in 0 until puntos.size - 1) {
        val (t0, v0) = puntos[i]
        val (t1, v1) = puntos[i + 1]
        if (t <= t1) {
            val f = suave(tramo(t, t0, t1))
            return FloatArray(v0.size) { k -> mezcla(v0[k], v1[k], f) }
        }
    }
    return puntos.last().second
}

/** Mezcla dos colores en sRGB y con su alfa, como el rgba() interpolado de los tableros */
internal fun mezclarColor(a: Color, b: Color, f: Float) =
    Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, a.alpha + (b.alpha - a.alpha) * f)
