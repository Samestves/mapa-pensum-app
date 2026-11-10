package mpensum.carrera.lista

import mpensum.componentes.BOTON
import mpensum.componentes.estiloRazon
import mpensum.componentes.TrazosLista
import mpensum.componentes.estiloAccion
import mpensum.componentes.conNegritas
import mpensum.componentes.estiloSeccion
import mpensum.componentes.IconoMas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoLucide
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * Lo que rodea a las filas: el título de la vista con su línea de avance y el
 * botón «…», los encabezados de sección (pegajosos, en mayúsculas finas) y el
 * menú con el otro orden.
 */

/** El alto de un encabezado de sección: también es su zona de toque */
internal val ALTO_ENCABEZADO = 44.dp

/** Lo que cae el velo bajo un encabezado fijo sobre una nube */
private val VELO_FIJO = 14.dp

/**
 * El título discreto en Jost y, debajo, UC y fecha de grado en una línea.
 * `compacta` es la lista acostada, con la letra del título un punto menor.
 * `fin` alinea el círculo del «…» con el borde derecho de las nubes.
 */
@Composable
internal fun CabeceraDeLista(titulo: String, subtitulo: String, compacta: Boolean, fin: Dp = 16.dp, alAbrirMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = fin, top = if (compacta) 14.dp else 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                titulo,
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = if (compacta) 24.sp else 28.sp, lineHeight = if (compacta) 30.sp else 34.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                conNegritas(subtitulo, Paleta.suave), Modifier.padding(top = 1.dp),
                style = estiloRazon(), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        BotonRedondo(alAbrirMenu, "Más opciones") { IconoMas(Paleta.tinta) }
    }
}

/** El botón redondo de 40 dp de las cabeceras, en una zona de toque de 44 */
@Composable
internal fun BotonRedondo(alTocar: () -> Unit, descripcion: String, modifier: Modifier = Modifier, icono: @Composable () -> Unit) {
    Box(
        modifier.size(48.dp).semantics { contentDescription = descripcion }.clickable(role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(BOTON), contentAlignment = Alignment.Center) { icono() }
    }
}

/**
 * El encabezado de una sección. Opaco, con el lienzo detrás, para que las
 * filas pasen por debajo sin verse. A la derecha, lo que cuenta la sección o,
 * si trae una, su acción (en la página, «+2 más» abre la lista entera).
 * Con `plegable` todo el renglón se toca y lleva un chevron que dice si está abierto.
 */
@Composable
internal fun EncabezadoDeSeccion(
    titulo: String,
    meta: String,
    plegable: Boolean,
    abierto: Boolean,
    alPlegar: () -> Unit,
    modifier: Modifier = Modifier,
    sangria: Dp = SANGRIA_ROTULO,
    alto: Dp = ALTO_ENCABEZADO,
    fijado: (() -> Boolean)? = null,
    accion: String? = null,
    alAccion: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(alto)
            .then(
                // Con nubes, fijo arriba deja caer un velo corto: la nube se hunde bajo el rótulo en vez de cortarse en seco
                if (fijado == null) Modifier else Modifier.drawWithContent {
                    drawContent()
                    if (fijado()) {
                        val alto = VELO_FIJO.toPx()
                        drawRect(
                            Brush.verticalGradient(0f to Paleta.lienzo, 1f to Paleta.lienzo.copy(alpha = 0f), startY = size.height, endY = size.height + alto),
                            Offset(0f, size.height), Size(size.width, alto),
                        )
                    }
                },
            )
            .background(Paleta.lienzo)
            .then(if (plegable) Modifier.clickable(role = Role.Button, onClick = alPlegar) else Modifier)
            .padding(start = sangria, end = sangria),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(titulo.uppercase(), Modifier.padding(bottom = 8.dp), style = estiloSeccion(), maxLines = 1)
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (accion != null) {
                Row(
                    Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = alAccion).padding(start = 16.dp, end = 0.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicText(accion, Modifier.padding(bottom = 8.dp), style = estiloAccion().copy(fontSize = 13.sp), maxLines = 1)
                    Box(Modifier.padding(bottom = 7.dp)) { IconoLucide(Lucide.siguiente, Paleta.suave, 16.dp) }
                }
            } else {
                // El contador con el mismo peso del rótulo, un tono más tenue: un solo estilo de rótulo
                BasicText(meta.uppercase(), Modifier.padding(bottom = 8.dp), style = estiloSeccion().copy(color = Paleta.tenue), maxLines = 1)
            }
            if (plegable) Box(Modifier.padding(bottom = 6.dp)) {
                IconoLucide(if (abierto) TrazosLista.chevronArriba else TrazosLista.chevronAbajo, Paleta.apagada, 16.dp)
            }
        }
    }
}

/** El menú del «…»: una hoja pequeña con el otro orden de la lista. */
@Composable
internal fun MenuDeLista(porEstado: Boolean, alElegir: () -> Unit, alCerrar: () -> Unit, arriba: Dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = alCerrar)) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = arriba + 52.dp, end = 12.dp)
                .width(244.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Paleta.panel)
                .border(1.dp, Paleta.tinta.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 46.dp).clickable(role = Role.Button, onClick = alElegir).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(
                    if (porEstado) "Ordenar por semestre" else "Ordenar por estado",
                    style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Paleta.tinta),
                )
                IconoTrazo(TrazosLista.orden, Paleta.tinta, 19.dp, 1.6f)
            }
        }
    }
}
