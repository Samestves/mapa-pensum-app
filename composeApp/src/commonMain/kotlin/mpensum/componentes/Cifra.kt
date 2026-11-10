package mpensum.componentes

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.interCifra
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Regla del % y de las unidades (docs/refinado-progreso.md): la unidad (%, UC,
 * «materias») se apoya en la línea base de la cifra, nunca arriba como
 * exponente. Mide el 40 % de la cifra (mínimo 11 sp), pesa uno más, va en
 * color tenue y se separa 2 dp. Aquí está en sus dos formas: componible
 * ([CifraConUnidad]) y para dibujar en un Canvas ([dibujarCifraConUnidad]).
 */

/** La separación entre la cifra y su unidad */
val SEPARACION_UNIDAD = 2.dp

/** El tamaño de la unidad: el 40 % de la cifra, redondeado y nunca menos de 11 sp */
fun tamañoUnidad(cifra: TextUnit): TextUnit = max(11, (cifra.value * 0.4f).roundToInt()).sp

/** Un peso más que el de la cifra (200 → 300) */
fun pesoUnidad(cifra: FontWeight): FontWeight = FontWeight((cifra.weight + 100).coerceAtMost(900))

/** El estilo de una cifra grande: Inter «Display» fino, de cifras tabulares y apretado un 2 % */
@Composable
fun estiloCifra(
    tamaño: TextUnit,
    color: Color = Paleta.tinta,
    peso: FontWeight = FontWeight.ExtraLight,
    interlinea: TextUnit = TextUnit.Unspecified,
) = estiloCifra(interCifra(), tamaño, color, peso, interlinea)

/** Lo mismo con la familia ya resuelta, para quien mide fuera de la composición */
fun estiloCifra(
    familia: FontFamily,
    tamaño: TextUnit,
    color: Color = Paleta.tinta,
    peso: FontWeight = FontWeight.ExtraLight,
    interlinea: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = familia, fontWeight = peso, fontSize = tamaño, lineHeight = interlinea,
    letterSpacing = (-0.02).em, color = color, fontFeatureSettings = "tnum",
)

/** El estilo de la unidad que acompaña a una cifra de ese tamaño y peso */
@Composable
fun estiloUnidad(tamañoCifra: TextUnit, pesoCifra: FontWeight = FontWeight.ExtraLight, color: Color = Paleta.tenue) =
    estiloUnidad(inter(), tamañoCifra, pesoCifra, color)

/** Lo mismo con la familia ya resuelta, para quien mide fuera de la composición */
fun estiloUnidad(familia: FontFamily, tamañoCifra: TextUnit, pesoCifra: FontWeight = FontWeight.ExtraLight, color: Color = Paleta.tenue) = TextStyle(
    fontFamily = familia, fontWeight = pesoUnidad(pesoCifra), fontSize = tamañoUnidad(tamañoCifra),
    color = color, fontFeatureSettings = "tnum",
)

/** «12%», «3 materias», «0 /15 UC»: la cifra y su unidad pequeña sobre la misma línea base */
@Composable
fun CifraConUnidad(
    valor: String,
    unidad: String,
    tamaño: TextUnit,
    modifier: Modifier = Modifier,
    peso: FontWeight = FontWeight.ExtraLight,
    color: Color = Paleta.tinta,
    colorUnidad: Color = Paleta.tenue,
    separacion: Dp = SEPARACION_UNIDAD,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(separacion)) {
        BasicText(valor, Modifier.alignByBaseline(), style = estiloCifra(tamaño, color, peso))
        BasicText(unidad, Modifier.alignByBaseline(), style = estiloUnidad(tamaño, peso, colorUnidad))
    }
}

/** Lo que mide, a lo ancho, una cifra con su unidad ya medidas */
fun anchoCifraConUnidad(cifra: TextLayoutResult, unidad: TextLayoutResult, separacion: Float): Float =
    cifra.size.width + separacion + unidad.size.width

/**
 * Dibuja una cifra y su unidad ya medidas, las dos con la línea base en `base`
 * y la cifra empezando en `x`. Devuelve donde acaba la unidad. `alfa` apaga
 * las dos juntas (sus colores van en los estilos).
 */
fun DrawScope.dibujarCifraConUnidad(
    cifra: TextLayoutResult,
    unidad: TextLayoutResult,
    x: Float,
    base: Float,
    separacion: Float = SEPARACION_UNIDAD.toPx(),
    alfa: Float = 1f,
): Float {
    drawText(cifra, topLeft = Offset(x, base - cifra.firstBaseline), alpha = alfa)
    val xu = x + cifra.size.width + separacion
    drawText(unidad, topLeft = Offset(xu, base - unidad.firstBaseline), alpha = alfa)
    return xu + unidad.size.width
}
