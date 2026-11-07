package mpensum.componentes

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema

/** La sombra de lo que flota: en claro más suave, que sobre papel se nota antes */
internal val SOMBRA_FLOTANTE get() = Paleta.sombra.copy(alpha = porTema(1f, 0.5f))

/**
 * El relleno de un cristal flotante (la barra, la hoja sobre el horario):
 * arriba más claro que abajo. En claro es blanco lechoso, más denso arriba
 * (el --cristal-fondo de la web).
 */
internal fun fondoVidrio(): Brush =
    if (Tema.claro) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.25f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.1f)))

/**
 * El filo de 1 px de un cristal, de arriba abajo, con su punto más flojo en
 * `medio`. En oscuro es luz; en claro, luz blanca arriba que pasa a un hilo de
 * tinta en los lados y abajo: el blanco solo se fundiría con el papel.
 */
internal fun filoVidrio(medio: Float): Brush =
    if (Tema.claro) Brush.verticalGradient(0f to Color.White, medio to Paleta.tinta.copy(alpha = 0.07f), 1f to Paleta.tinta.copy(alpha = 0.12f))
    else Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), medio to Color.White.copy(alpha = 0.04f), 1f to Color.White.copy(alpha = 0.07f))

/**
 * Cómo se pinta un cristal por encima del desenfoque: el relleno y el filo en
 * oscuro (en claro son siempre `fondoVidrio` y `filoVidrio`), dónde se afloja
 * el filo y el radio de su esquina; sin radio, la esquina es una cápsula.
 */
internal class Vidrio(val fondoOscuro: Brush, val filoOscuro: Brush, val medioFilo: Float, val radioFilo: Dp? = null)

/** El cristal de las piezas flotantes: la sombra, el recorte a su forma y el vidrio por dentro */
@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.cristal(estado: HazeState, forma: Shape, estilo: HazeStyle, elevacion: Dp, vidrio: Vidrio): Modifier =
    this
        .shadow(elevacion, forma, ambientColor = SOMBRA_FLOTANTE, spotColor = SOMBRA_FLOTANTE)
        .clip(forma)
        .vidrio(estado, estilo, vidrio)

/**
 * Lo que recoge el cristal (la vista de debajo). En modo ligero no hay
 * desenfoque (o es un Exterior), así que tampoco se graba lo de detrás en cada cuadro.
 */
fun Modifier.fuenteCristal(estado: HazeState): Modifier = if (Tema.sinDesenfoque) this else hazeSource(estado)

/**
 * El cristal del modo ligero: sin desenfoque, el fondo del estilo con su tinte
 * encima, liso. Es lo mismo que pinta Haze cuando el teléfono no desenfoca
 * (API < 31), así que se ve igual de asentado. `alfa` se lee al dibujar.
 */
fun Modifier.tinteLiso(estilo: HazeStyle, alfa: () -> Float = { 1f }): Modifier = drawBehind {
    val a = alfa()
    if (a <= 0f) return@drawBehind
    val tinte = estilo.fallbackTint.color.takeIf { it.isSpecified } ?: estilo.tints.firstOrNull()?.color?.takeIf { it.isSpecified }
    drawRect(estilo.backgroundColor, alpha = a)
    if (tinte != null) drawRect(tinte, alpha = a)
}

/** El vidrio sin sombra ni recorte: desenfoque, relleno más claro arriba que abajo y el filo de luz del canto */
@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.vidrio(estado: HazeState, estilo: HazeStyle, vidrio: Vidrio): Modifier =
    this
        .then(if (Tema.sinDesenfoque) Modifier.tinteLiso(estilo) else Modifier.hazeEffect(estado, estilo) { inputScale = HazeInputScale.Auto })
        .drawWithCache {
            val claro = Tema.claro
            val relleno = if (claro) fondoVidrio() else vidrio.fondoOscuro
            val filo = if (claro) filoVidrio(vidrio.medioFilo) else vidrio.filoOscuro
            val g = 1.dp.toPx()
            val r = CornerRadius((vidrio.radioFilo?.toPx() ?: (size.minDimension / 2)) - g / 2)
            onDrawWithContent {
                drawRect(relleno)
                drawContent()
                drawRoundRect(filo, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), r, style = Stroke(g))
            }
        }
