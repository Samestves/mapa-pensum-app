package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import mpensum.tema.Paleta
import kotlin.math.floor

/*
 * Los textos de las escenas del avance, dibujados en unidades del plano: se
 * miden a la densidad del aparato (nítidos) y se pintan a 1/densidad dentro
 * de la cámara, que los escala con todo lo demás.
 */

/** Un texto medido con su esquina de arriba en (x, y) del plano; `ancla` 0 a la izquierda, 0,5 al centro, 1 a la derecha */
internal fun DrawScope.textoPlano(t: TextLayoutResult, x: Float, y: Float, color: Color, alfa: Float, ancla: Float = 0f) {
    if (alfa <= 0.004f) return
    val px = 1f / density
    withTransform({
        translate(x, y)
        scale(px, px, Offset.Zero)
    }) { drawText(t, color, Offset(-t.size.width * ancla, 0f), alpha = alfa.coerceIn(0f, 1f)) }
}

/** Lo mismo, colocado por su línea base */
internal fun DrawScope.textoPorBase(t: TextLayoutResult, x: Float, base: Float, color: Color, alfa: Float, ancla: Float = 0f) =
    textoPlano(t, x, base - t.firstBaseline / density, color, alfa, ancla)

internal fun DrawScope.anchoPlano(t: TextLayoutResult) = t.size.width / density

private val CAMBIO = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
/** Lo que tarda una fecha en dar paso a la siguiente */
private const val ROLLO_FECHA = 300f
/** Lo que sube una fecha al pasar a la siguiente: lo justo para salir de su ventana */
private const val SUBE_FECHA = 24f

/**
 * La fecha que corre, como un contador: de `desde` a `hasta` pasa período a
 * período por su propia ventana (la que se va sube y sale, la que llega entra
 * desde abajo) y la última se asienta en tinta; las de paso van en suave.
 * `x` y `ancla` la colocan; `base` es su línea base.
 */
internal fun DrawScope.fechaQueCorre(t: Float, fechas: List<TextLayoutResult>, x: Float, base: Float, ancla: Float, desde: Float, hasta: Float, op: Float) {
    val np = fechas.size
    if (np == 0 || op <= 0.004f) return
    val paso = if (np > 1) (hasta - desde) / (np - 1) else 1f
    val k = if (np > 1) floor((t - desde) / paso).toInt().coerceIn(0, np - 1) else 0
    val enK = desde + paso * k
    val r = if (k == 0) 1f else CAMBIO.transform(tramo(t, enK, enK + ROLLO_FECHA))
    val asienta = CAMBIO.transform(tramo(t, hasta, hasta + 400f))
    fun color(i: Int) = if (i == np - 1) mezclarColor(Paleta.suave, Paleta.tinta, asienta) else Paleta.suave
    // La ventana: tan ancha como la fecha más larga, del tope de las cifras a un pelo bajo la línea base
    val ancho = fechas.maxOf { anchoPlano(it) } + 4f
    val izquierda = x - ancho * ancla
    clipRect(izquierda - 2f, base - SUBE_FECHA - 2f, izquierda + ancho + 2f, base + 3f) {
        if (k > 0 && r < 1f) textoPorBase(fechas[k - 1], x, base - SUBE_FECHA * r, color(k - 1), op * (1f - r), ancla)
        textoPorBase(fechas[k], x, base + SUBE_FECHA * (1f - r), color(k), op * r, ancla)
    }
}
