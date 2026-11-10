package mpensum.bienvenida

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.tema.Paleta

/**
 * Funde los bordes de una pieza con el fondo, sin recuadro: lo de dentro se
 * desvanece hacia arriba, abajo y los lados en vez de cortarse en seco.
 *
 * Desde que la bienvenida va sobre el lienzo liso (sin aurora), el fundido ya
 * no necesita una capa aparte con `DstIn`: basta con pintar encima, en cada
 * borde, un degradado del propio lienzo. Sin `saveLayer` por cuadro, que era
 * lo que costaba en gama baja. Lo de dentro se recorta a la zona (un recorte,
 * no una capa): la toma acerca el plano y no debe pisar el título ni la barra.
 * Solo vale sobre el lienzo liso; sobre una luz o una imagen se notaría.
 *
 * Esta versión mide los fundidos en fracciones del tamaño (0 a 1): la usan
 * las escenas, que se encogen a la par con la pantalla.
 */
fun Modifier.fundidoBordes(arriba: Float, abajo: Float, lados: Float): Modifier =
    fundido { floatArrayOf(arriba, abajo, lados) }

/** Lo mismo con los fundidos en dp: la lista de carreras los fija así */
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
 * por tamaño: los degradados viven en la caché del dibujo y solo se rehacen
 * si cambia el tamaño o el tema (el color del lienzo se lee al cachear).
 */
private fun Modifier.fundido(fracciones: CacheDrawScope.() -> FloatArray): Modifier =
    this.drawWithCache {
        val (arriba, abajo, lados) = fracciones()
        val lienzo = Paleta.lienzo
        val nada = lienzo.copy(alpha = 0f)
        val h = size.height
        val w = size.width
        val altoArriba = arriba * h
        val altoAbajo = abajo * h
        val anchoLado = lados * w
        val pArriba = if (altoArriba > 0f) Brush.verticalGradient(listOf(lienzo, nada), 0f, altoArriba) else null
        val pAbajo = if (altoAbajo > 0f) Brush.verticalGradient(listOf(nada, lienzo), h - altoAbajo, h) else null
        val pIzquierda = if (anchoLado > 0f) Brush.horizontalGradient(listOf(lienzo, nada), 0f, anchoLado) else null
        val pDerecha = if (anchoLado > 0f) Brush.horizontalGradient(listOf(nada, lienzo), w - anchoLado, w) else null
        onDrawWithContent {
            clipRect { this@onDrawWithContent.drawContent() }
            if (pArriba != null) drawRect(pArriba, Offset.Zero, Size(w, altoArriba))
            if (pAbajo != null) drawRect(pAbajo, Offset(0f, h - altoAbajo), Size(w, altoAbajo))
            if (pIzquierda != null) drawRect(pIzquierda, Offset.Zero, Size(anchoLado, h))
            if (pDerecha != null) drawRect(pDerecha, Offset(w - anchoLado, 0f), Size(anchoLado, h))
        }
    }
