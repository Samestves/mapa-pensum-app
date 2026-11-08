package mpensum.componentes

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Un fundido en el borde de abajo de una columna que desplaza, solo mientras
 * queda algo por ver: lo que sigue se desvanece en vez de cortarse en seco.
 * Va antes del `verticalScroll`, para que el fundido quede quieto en el borde.
 * La capa fuera de pantalla que necesita `DstOut` solo existe mientras hay
 * algo que fundir: con la columna entera a la vista no cuesta nada.
 */
fun Modifier.fundidoAbajo(desplazo: ScrollState, alto: Dp = 32.dp): Modifier =
    this
        .graphicsLayer { compositingStrategy = if (desplazo.canScrollForward) CompositingStrategy.Offscreen else CompositingStrategy.Auto }
        .drawWithCache {
            val f = alto.toPx()
            val brocha = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = size.height - f, endY = size.height)
            onDrawWithContent {
                drawContent()
                if (desplazo.canScrollForward) drawRect(brocha, blendMode = BlendMode.DstOut)
            }
        }
