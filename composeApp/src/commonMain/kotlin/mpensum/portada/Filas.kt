package mpensum.portada

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
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
import mpensum.datos.Carrera

import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.colorDe
import mpensum.tema.jost
import mpensum.tema.porTema

/** Encabezado: Jost 17/22, −1 %; 300 en oscuro y 400 en claro, donde el trazo fino pierde cuerpo */
@Composable
private fun estiloEncabezado() =
    TextStyle(fontFamily = jost(), fontWeight = porTema(FontWeight.Light, FontWeight.Normal), fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta)

/**
 * La ficha compacta de otra carrera para la rejilla de dos columnas, de pie
 * y acostada: 68 dp, su silueta entera en 36 × 28 y el nombre (que baja de 17 a
 * 13 sp antes de cortar). Sin flecha: toda la ficha es el toque.
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
            .padding(start = 14.dp, end = 10.dp),
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
        val nombre = remember(c.nombreCorto) { partirEnDos(c.nombreCorto) }
        BasicText(
            nombre,
            Modifier.padding(start = Espacio.s).weight(1f),
            style = estiloEncabezado(),
            // El salto va escrito: si una palabra no cabe entera, sobra una línea y autoSize
            // baja la letra en vez de partir «Producción» a mitad de palabra
            maxLines = if ('\n' in nombre) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(13.sp, 17.sp, 1.sp),
        )
    }
}

/** Parte el nombre en dos renglones por el espacio más cercano al centro; sin espacios, queda igual */
private fun partirEnDos(nombre: String): String {
    val centro = nombre.length / 2
    val corte = nombre.indices.filter { nombre[it] == ' ' }.minByOrNull { kotlin.math.abs(it - centro) } ?: return nombre
    return nombre.substring(0, corte) + '\n' + nombre.substring(corte + 1)
}
