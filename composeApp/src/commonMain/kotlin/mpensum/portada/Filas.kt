package mpensum.portada

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Silueta
import mpensum.componentes.pulsable
import mpensum.componentes.renglon
import mpensum.datos.Carrera

import mpensum.tema.Espacio
import mpensum.tema.Tipo
import mpensum.tema.Paleta
import mpensum.tema.colorDe
import mpensum.tema.jost
import mpensum.tema.porTema

/**
 * Otra carrera: su silueta entera y tenue en su color, el nombre corto, el
 * tipo y la flecha. Una fila de cristal apenas marcada: lo que está a la
 * vista es tu carrera; esto es el índice.
 */
@Composable
internal fun Fila(c: Carrera, modifier: Modifier, alPulsar: (TransformOrigin) -> Unit) {
    val color = colorDe(c.color)
    val origen = rememberOrigen()
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.98f)
            .drawBehind {
                // En claro la fila es papel blanco a medias sobre el lienzo, con el mismo filo de tinta
                drawRoundRect(porTema(Paleta.tinta.copy(alpha = 0.035f), Color.White.copy(alpha = 0.62f)), cornerRadius = CornerRadius(16.dp.toPx()))
                borde(Paleta.tinta.copy(alpha = porTema(0.065f, 0.06f)), 16.dp)
            }
            .padding(start = Espacio.m, end = Espacio.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La silueta cabe en 56 × 36 como en la web; la que tiene más de siete filas asoma por debajo
        Box(Modifier.size(56.dp, 36.dp)) {
            Silueta(
                c.silueta, color.copy(alpha = 0.7f),
                hechas = c.silueta,
                punto = 2.5.dp,
                hueco = (56f / c.silueta.size - 2.5f).dp,
                huecoFila = (36f / 7f - 2.5f).dp,
            )
        }
        Column(Modifier.padding(start = Espacio.m, end = Espacio.s).weight(1f)) {
            BasicText(
                c.nombreCorto,
                Modifier.renglon(22.dp, 17.sp),
                style = TextStyle(fontFamily = jost(), fontSize = Tipo.xl, lineHeight = 22.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(c.tipo.uppercase(), Modifier.padding(top = Espacio.xxs).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
        }
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

/**
 * La ficha compacta de otra carrera para la rejilla de dos columnas en
 * horizontal: 68 dp, su silueta entera en 36 × 28, el nombre (que baja de 15 a
 * 13 sp antes de cortar) y el tipo. Sin flecha: toda la ficha es el toque.
 */
@Composable
internal fun FilaCompacta(c: Carrera, modifier: Modifier, alPulsar: (TransformOrigin) -> Unit) {
    val color = colorDe(c.color)
    val origen = rememberOrigen()
    Row(
        modifier
            .height(68.dp)
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.98f)
            .drawBehind {
                drawRoundRect(porTema(Paleta.tinta.copy(alpha = 0.035f), Color.White.copy(alpha = 0.62f)), cornerRadius = CornerRadius(16.dp.toPx()))
                borde(Paleta.tinta.copy(alpha = porTema(0.065f, 0.06f)), 16.dp)
            }
            .padding(start = 14.dp, end = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp, 28.dp)) {
            Silueta(
                c.silueta, color.copy(alpha = 0.75f),
                hechas = c.silueta,
                punto = 2.dp,
                hueco = (36f / c.silueta.size - 2f).dp,
                huecoFila = (28f / 7f - 2f).dp,
            )
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            BasicText(
                c.nombreCorto,
                Modifier.renglon(20.dp, 15.sp),
                style = TextStyle(fontFamily = jost(), fontSize = Tipo.l, lineHeight = 20.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(13.sp, 15.sp, 1.sp),
            )
            BasicText(c.tipo.uppercase(), Modifier.padding(top = Espacio.xxs).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
        }
    }
}
