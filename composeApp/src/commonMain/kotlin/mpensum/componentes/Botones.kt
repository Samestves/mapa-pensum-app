package mpensum.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio

/* Pastillas de icono y botones: redondo, de tinta, del mapa y el aspa de cerrar */

internal val LADO_PASTILLA = 32.dp

/** La pastilla de icono: 32 dp, radio 8, el icono Lucide de 18 dp. La azul es la de «Tu próximo semestre» */
@Composable
internal fun Pastilla(trazos: List<Path>, azul: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier.size(LADO_PASTILLA).clip(RoundedCornerShape(8.dp))
            .background(if (azul) Paleta.azul.copy(alpha = Velo.pastillaAzul) else Paleta.tinta.copy(alpha = Velo.pastilla)),
        contentAlignment = Alignment.Center,
    ) { IconoLucide(trazos, if (azul) Paleta.azul else Paleta.tinta) }
}

/** La pastilla vacía de una casilla sin elegir: contorno de 1,5 dp y el «+» tenue */
@Composable
internal fun PastillaVacia(trazos: List<Path> = IconosHoja.mas) {
    Box(
        Modifier.size(LADO_PASTILLA).border(1.5.dp, Paleta.tinta.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { IconoLucide(trazos, Paleta.tenue) }
}

/** Un icono suelto en el sitio de la pastilla, con el estado en su color */
@Composable
internal fun IconoDeFila(trazos: List<Path>, color: Color) {
    Box(Modifier.size(LADO_PASTILLA), contentAlignment = Alignment.Center) { IconoLucide(trazos, color) }
}

/** Un icono Lucide con el trazo visual de 1,5 dp a cualquier tamaño */
@Composable
internal fun IconoLucide(trazos: List<Path>, color: Color, tam: Dp = 18.dp, modifier: Modifier = Modifier) {
    IconoTrazo(trazos, color, tam, 1.5f * 24f / tam.value, modifier)
}

/** El chevron de 16 dp, apagado */
@Composable
internal fun Chevron(trazos: List<Path> = Lucide.siguiente, modifier: Modifier = Modifier) {
    IconoLucide(trazos, Paleta.apagada, 16.dp, modifier)
}

/** El círculo de atrás y de cerrar: 36 dp dibujados dentro de una zona de 44, el toque mínimo, y con su nombre para el lector de pantalla */
@Composable
internal fun BotonRedondo(trazos: List<Path>, descripcion: String, alTocar: () -> Unit) {
    Box(
        Modifier.size(Alto.toque).clip(CircleShape).clickable(role = Role.Button, onClick = alTocar).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = Velo.pastilla)), contentAlignment = Alignment.Center) {
            IconoLucide(trazos, Paleta.suave)
        }
    }
}

/**
 * El botón de tinta a lo ancho: 52 dp, radio 26. Con `nota` lleva una segunda
 * línea pequeña («Se te abre 1»); con `superficie` no es el lleno sino el
 * de la tinta al 8 %, el que acompaña al paso siguiente.
 */
@Composable
internal fun BotonTinta(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    nota: String? = null,
    colorNota: Color? = null,
    superficie: Boolean = false,
    /** La burbuja acostada usa botones de 48 con radio 16; la ficha de pie, los de 52 con radio 26 */
    alto: Dp = 52.dp,
    radio: Dp = Radio.Barra,
    /** Una flecha tras el verbo, a 8 dp: la acción que lleva a otra pantalla */
    flecha: Boolean = false,
) {
    val lleno = !superficie
    Column(
        modifier.fillMaxWidth().heightIn(min = alto).clip(RoundedCornerShape(radio))
            .background(if (lleno) Paleta.tinta else Paleta.tinta.copy(alpha = Velo.pastilla))
            .clickable(role = Role.Button, onClick = alTocar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val centrado = TextAlign.Center
        // Un botón dice su verbo en una línea: si no cabe (la burbuja acostada, 320 dp), la letra baja hasta 13 antes que partirse
        val letra = estiloFila(if (lleno) Paleta.panel else Paleta.tinta).copy(textAlign = centrado)
        val verbo: @Composable (Modifier) -> Unit = { m ->
            BasicText(
                texto, m.padding(horizontal = Espacio.xs, vertical = if (nota == null) Espacio.xs else 0.dp),
                style = letra, maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 15.sp, stepSize = 0.5.sp),
            )
        }
        if (flecha) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            verbo(Modifier.weight(1f, fill = false))
            IconoLucide(Lucide.flecha, letra.color, 18.dp)
        } else verbo(Modifier)
        if (nota != null) BasicText(nota, Modifier.padding(horizontal = Espacio.xs), style = estiloNota(colorNota ?: if (lleno) Paleta.panel.copy(alpha = 0.6f) else Paleta.tenue).copy(textAlign = centrado))
    }
}

/** El botón de tinta a lo ancho: 52 dp, radio 26 */
@Composable
internal fun BotonTinta(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(Radio.Barra)).background(Paleta.tinta)
            .clickable(role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { BasicText(texto, Modifier.padding(horizontal = Espacio.m, vertical = Espacio.xs), style = estiloFila(Paleta.panel).copy(textAlign = TextAlign.Center)) }
}

// ─────────────────────────────────── Filas agrupadas ──────────────────────────────────

/**
 * «Ver en el mapa», arriba a la derecha de la cabecera: una cápsula de 36 dp
 * (el alto de los círculos de atrás y cerrar) con el icono del mapa. Es una
 * acción de la pantalla, no un bloque más al final de la lista.
 */
@Composable
internal fun BotonMapa(alTocar: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Paleta.tinta.copy(alpha = Velo.pastilla))
            .clickable(role = Role.Button, onClick = alTocar).padding(start = Espacio.s, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        IconoLucide(Lucide.mapa, Paleta.tinta, 16.dp)
        BasicText("Ver en el mapa", style = estiloNota(Paleta.tinta, FontWeight.Medium), maxLines = 1)
    }
}

// ─────────────────────────────────── Iconos ──────────────────────────────────

/** El aspa de las hojas: el mismo círculo que el de cerrar de las demás pantallas */
@Composable
fun BotonCerrar(alCerrar: () -> Unit) = BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
