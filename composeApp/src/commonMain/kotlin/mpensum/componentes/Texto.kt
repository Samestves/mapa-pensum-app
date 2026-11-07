package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.jost

/**
 * Una línea de texto de Jost como la mide la web: `alto` de renglón aunque
 * sea menor que la letra. Android no encoge el renglón por debajo de las
 * métricas de la fuente (Jost sube 1,07 y baja 0,375 em), así que se coloca
 * por su línea base, donde CSS la pondría: a media altura más 0,3475 em.
 */
internal fun Modifier.renglon(alto: Dp, tam: TextUnit): Modifier = layout { medible, limites ->
    val p = medible.measure(limites.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val base = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
    val h = alto.roundToPx()
    layout(p.width, h) { p.place(0, baseJost(alto, tam).roundToPx() - base) }
}

/** Dónde cae la línea base de Jost en un renglón CSS de `alto` */
internal fun Density.baseJost(alto: Dp, tam: TextUnit): Dp = alto / 2 + (tam.toPx() * 0.3475f).toDp()

/**
 * Titular: Jost 300, 40 con interlínea de 1,1 em y −2 %. Es la voz de la
 * bienvenida y del horario; quien lo pone en un `BasicText` le añade el
 * `autoSize` que baja la letra antes de partir una línea.
 */
@Composable
internal fun estiloTitular(tamaño: TextUnit = Tipo.gigante) =
    TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = tamaño, lineHeight = 1.1.em, letterSpacing = (-0.02).em, color = Paleta.tinta)
