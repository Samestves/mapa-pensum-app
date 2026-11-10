package mpensum.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.Radio
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.roundToInt

/* Los estilos de texto y la superficie de las hojas y las pantallas internas */

/** Las opacidades de la tinta por rol: la primera en oscuro, la segunda en los claros */
internal object Velo {
    val superficie get() = porTema(0.05f, 0.045f)
    val hilo get() = porTema(0.09f, 0.12f)
    val pastilla get() = porTema(0.08f, 0.07f)
    val pendiente get() = porTema(0.15f, 0.13f)
    val pastillaAzul get() = porTema(0.16f, 0.10f)
}

/** El alto de las mayúsculas de Inter, en em: para recortar una cifra a su tope y su línea base */
private const val ALTO_MAYUSCULA = 0.727f

/** Rótulo: Inter 500, 11/16, +8 %, en mayúsculas (el texto ya viene en mayúsculas) */
@Composable
internal fun estiloRotulo(color: Color = Paleta.tenue) =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em, color = color, fontFeatureSettings = "tnum")

/** Título: Jost 300, 28/32, −0,5 % */
@Composable
internal fun estiloTitulo() =
    TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.005).em, color = Paleta.tinta)

/** Fila: Inter 500, 15/20, −0,9 % */
@Composable
internal fun estiloFila(color: Color = Paleta.tinta, peso: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em, color = color, fontFeatureSettings = "tnum")

/** Cuerpo: Inter 400, 15/20, −0,9 %, suave */
@Composable
internal fun estiloCuerpo(color: Color = Paleta.suave) = estiloFila(color, FontWeight.Normal)

/** Nota: Inter 400, 13/16, −0,3 %, cifras tabulares, tenue */
@Composable
internal fun estiloNota(color: Color = Paleta.tenue, peso: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = (-0.003).em, color = color, fontFeatureSettings = "tnum")

/** Cifra media (24/28, 300, −1,5 %): las fechas */
@Composable
internal fun estiloFecha(color: Color = Paleta.tinta) =
    estiloCifra(24.sp, color, FontWeight.Light, 28.sp).copy(letterSpacing = (-0.015).em)

/**
 * Recorta un texto a sus mayúsculas: el tope en la altura de las mayúsculas y
 * el pie en la línea base, como `text-box: trim-both cap alphabetic`. Así dos
 * cifras de tamaños distintos se alinean por arriba o por abajo de verdad.
 */
internal fun Modifier.recorteCap(tamaño: TextUnit): Modifier = layout { medible, limites ->
    val p = medible.measure(limites.copy(minHeight = 0))
    val base = p[FirstBaseline]
    if (base == androidx.compose.ui.layout.AlignmentLine.Unspecified) return@layout layout(p.width, p.height) { p.place(0, 0) }
    val cap = (tamaño.toPx() * ALTO_MAYUSCULA).roundToInt()
    layout(p.width, cap) { p.place(0, cap - base) }
}

/** El alto de las mayúsculas de una cifra de ese tamaño, en px */
internal fun altoMayuscula(tamañoPx: Float) = tamañoPx * ALTO_MAYUSCULA

// ─────────────────────────────────── Superficies ──────────────────────────────────

/** La tarjeta de la hoja: radio 24 y la tinta al 4,5 % (5 % en oscuro), sin filo */
internal fun Modifier.superficie(radio: Dp = Radio.Grande): Modifier =
    this.clip(RoundedCornerShape(radio)).background(Paleta.tinta.copy(alpha = Velo.superficie))

/** Los rótulos: mayúsculas semibold de 11 sp con 0,05 em de espaciado */
@Composable
internal fun versal(color: Color) = TextStyle(fontFamily = inter(), fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.05.em, color = color)

/** Un texto de la hoja: Inter, con el tamaño, el peso y el color que pida */
@Composable
internal fun estilo(tam: Int, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam * 1.35f).roundToInt()) =
    estilo(tam.sp, color, peso, interlinea)

/** Lo mismo con el tamaño de un token de [Tipo]: la interlínea por defecto es 1,35 veces el tamaño */
@Composable
internal fun estilo(tam: TextUnit, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam.value * 1.35f).roundToInt()) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = tam, lineHeight = interlinea.sp, color = color, fontFeatureSettings = "tnum")
