package mpensum.bienvenida

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
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
 * Funde los bordes de una pieza con el fondo, sin recuadro: lo de dentro se
 * desvanece hacia arriba, abajo y los lados en vez de cortarse en seco.
 *
 * Mismo patrón que `fundidoAbajo` (componentes/Fundido.kt): `DstIn` necesita
 * una capa aparte y se abre dentro del dibujo (`saveLayer`), no como capa de
 * composición (`CompositingStrategy.Offscreen`). Esa se guarda como textura y
 * Android no la repinta si se grabó con la pieza fuera de la pantalla: hoy la
 * guía entra con fundido y escala, pero bastaría con meterla en una hoja o en
 * una transición desde el borde para que la escena quedara vieja o en blanco.
 * Sin ningún borde que fundir no se abre capa.
 *
 * Esta versión mide los fundidos en fracciones del tamaño (0 a 1): la usan
 * las escenas, que se encogen a la par con la pantalla.
 */
fun Modifier.fundidoBordes(arriba: Float, abajo: Float, lados: Float): Modifier =
    fundido { floatArrayOf(arriba, abajo, lados) }

/** Lo mismo con los fundidos en dp: la lista de carreras y la escena del avance los fijan así */
fun Modifier.fundidoBordes(arriba: Dp = 0.dp, abajo: Dp = 0.dp, lados: Dp = 0.dp): Modifier =
    fundido {
        floatArrayOf(
            if (size.height > 0f) arriba.toPx() / size.height else 0f,
            if (size.height > 0f) abajo.toPx() / size.height else 0f,
            if (size.width > 0f) lados.toPx() / size.width else 0f,
        )
    }

/**
 * El fundido en sí. `fracciones` da arriba, abajo y lados, y se mide una vez
 * por tamaño: las brochas viven en la caché del dibujo y no se rehacen por cuadro.
 */
private fun Modifier.fundido(fracciones: CacheDrawScope.() -> FloatArray): Modifier =
    this.drawWithCache {
        val (arriba, abajo, lados) = fracciones()
        val vertical = if (arriba > 0f || abajo > 0f) Brush.verticalGradient(*paradas(arriba, abajo)) else null
        val horizontal = if (lados > 0f) Brush.horizontalGradient(*paradas(lados, lados)) else null
        val limites = Rect(Offset.Zero, size)
        val capa = Paint()
        onDrawWithContent {
            if (vertical == null && horizontal == null) {
                drawContent()
                return@onDrawWithContent
            }
            drawIntoCanvas { it.saveLayer(limites, capa) }
            drawContent()
            // DstIn conserva lo de dentro con el alfa de la máscara: negro deja ver, transparente borra
            if (vertical != null) drawRect(vertical, blendMode = BlendMode.DstIn)
            if (horizontal != null) drawRect(horizontal, blendMode = BlendMode.DstIn)
            drawIntoCanvas { it.restore() }
        }
    }

/**
 * Las paradas de la máscara: transparente en cada extremo que se funde y
 * negro en medio. Un extremo sin fundido arranca ya en negro: dos paradas
 * en la misma posición dejarían una línea de un píxel borrada.
 */
private fun paradas(inicio: Float, fin: Float): Array<Pair<Float, Color>> = buildList {
    if (inicio > 0f) {
        add(0f to Color.Transparent)
        add(inicio to Color.Black)
    } else {
        add(0f to Color.Black)
    }
    if (fin > 0f) {
        add(1f - fin to Color.Black)
        add(1f to Color.Transparent)
    } else {
        add(1f to Color.Black)
    }
}.toTypedArray()
