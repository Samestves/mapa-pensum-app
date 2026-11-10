package mpensum.portada

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoLucide
import mpensum.componentes.BOTON
import mpensum.componentes.LogoMosaico
import mpensum.componentes.Lucide
import mpensum.componentes.pulsable
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.jost

/*
 * La cabecera del home: el icono de la app en su mosaico, el nombre y el
 * engranaje. Sin cristal en reposo (el vidrio solo aparece al bajar,
 * VidrioCabecera), sin subtítulo y sin mayúsculas espaciadas. El borde
 * izquierdo del mosaico cae en el margen de la tarjeta; el círculo del
 * engranaje, en su borde derecho.
 */

/** El alto de la fila de la cabecera: el toque del engranaje (48) */
internal val ALTO_CABECERA = 48.dp

/** El mosaico: el icono de la app a 34 dp, a la altura óptica de la letra de 21 */
private val LADO_MOSAICO = 34.dp

/** El engranaje se dibuja en 40 dentro de 48: su círculo queda 4 dp por dentro de la zona de toque */
internal val HOLGURA_ENGRANAJE = 4.dp

/** El nombre de la app: Jost Medium 21, apretado un 2 % para que se lea como un nombre propio */
@Composable
private fun estiloMarca() =
    TextStyle(fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = (-0.02).em, color = Paleta.tinta)

/** El engranaje de los ajustes: el círculo liso de 40 de la cabecera de la lista, en una zona de toque de 48 */
@Composable
private fun Engranaje(alAjustes: () -> Unit) {
    Box(
        Modifier.size(ALTO_CABECERA).semantics { contentDescription = "Ajustes y ayuda" }.pulsable(alAjustes, escala = 0.9f),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(BOTON), contentAlignment = Alignment.Center) {
            IconoLucide(Lucide.engranaje, Paleta.tinta, 20.dp)
        }
    }
}

/** El mosaico con su canto de luz y «Mapa de Pensum» a la izquierda, el engranaje a la derecha */
@Composable
internal fun CabeceraMarca(modifier: Modifier, alAjustes: () -> Unit) {
    Row(modifier.fillMaxWidth().height(ALTO_CABECERA), verticalAlignment = Alignment.CenterVertically) {
        LogoMosaico(LADO_MOSAICO, canto = true)
        Spacer(Modifier.width(Espacio.s))
        BasicText(
            "Mapa de Pensum",
            Modifier.weight(1f),
            style = estiloMarca(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Con la letra del sistema grande baja antes de cortarse (en 320 dp a 1,3 la marca vieja no cabía)
            autoSize = TextAutoSize.StepBased(15.sp, 21.sp, 1.sp),
        )
        Engranaje(alAjustes)
    }
}
