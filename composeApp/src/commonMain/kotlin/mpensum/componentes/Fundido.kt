package mpensum.componentes

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Un fundido en el borde de abajo de una columna que desplaza, solo mientras
 * queda algo por ver: lo que sigue se desvanece en vez de cortarse en seco.
 * Va antes del `verticalScroll`, para que el fundido quede quieto en el borde.
 *
 * `DstOut` necesita una capa aparte, y se abre dentro del dibujo (`saveLayer`),
 * no como capa de composición (`CompositingStrategy.Offscreen`): esa se guarda
 * como textura y Android no la repinta si se grabó con la columna fuera de la
 * pantalla, como pasa mientras la hoja sube. Acostado, la columna izquierda del
 * progreso se quedaba en blanco hasta que algo dentro de ella cambiaba. Con la
 * columna entera a la vista no se abre ninguna capa: no cuesta nada.
 */
fun Modifier.fundidoAbajo(desplazo: ScrollState, alto: Dp = 32.dp): Modifier =
    this.drawWithCache {
        val f = alto.toPx()
        val brocha = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = size.height - f, endY = size.height)
        val limites = Rect(Offset.Zero, size)
        val capa = Paint()
        onDrawWithContent {
            if (!desplazo.canScrollForward) {
                drawContent()
                return@onDrawWithContent
            }
            drawIntoCanvas { it.saveLayer(limites, capa) }
            drawContent()
            drawRect(brocha, blendMode = BlendMode.DstOut)
            drawIntoCanvas { it.restore() }
        }
    }
