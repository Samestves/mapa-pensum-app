package mpensum.componentes

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/** Los tres puntos de «más», en la rejilla de 24 */
internal fun DrawScope.trazarPuntos(color: Color) {
    val e = size.minDimension / 24f
    for (x in listOf(5f, 12f, 19f)) drawCircle(color, 1.6f * e, Offset(x * e, 12f * e))
}

/** Un + (o un − sin el trazo vertical) en la rejilla de 24 de Lucide; el grosor va en esa rejilla */
internal fun DrawScope.trazarCruz(color: Color, grosor: Float = 2f, vertical: Boolean = true) {
    val e = size.minDimension / 24f
    drawLine(color, Offset(5 * e, 12 * e), Offset(19 * e, 12 * e), grosor * e, StrokeCap.Round)
    if (vertical) drawLine(color, Offset(12 * e, 5 * e), Offset(12 * e, 19 * e), grosor * e, StrokeCap.Round)
}

/** Ensancha una pieza más allá del margen de su padre, como un margin negativo */
internal fun Modifier.desbordar(lado: Dp): Modifier = layout { medible, limites ->
    val extra = (lado * 2).roundToPx()
    val p = medible.measure(limites.copy(minWidth = limites.minWidth + extra, maxWidth = limites.maxWidth + extra))
    layout(limites.maxWidth, p.height) { p.place(-extra / 2, 0) }
}
