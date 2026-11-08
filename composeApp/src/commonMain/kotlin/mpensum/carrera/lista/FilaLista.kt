package mpensum.carrera.lista

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mpensum.logica.FilaLista
import mpensum.logica.Marca
import mpensum.logica.Paso
import mpensum.logica.Situacion
import mpensum.logica.pasoSiguiente
import mpensum.tema.Movimiento
import mpensum.tema.Paleta

/*
 * La fila de la lista: icono de estado, el nombre en Jost, una sola razón
 * debajo y, a la derecha, las UC. Cada línea es un solo BasicText y no hay
 * sombras. Lo que cambia con el tiempo (el realce que se apaga, el
 * desplazamiento del gesto) se lee al dibujar, no al componer.
 */

/** Alto mínimo de una fila: el pulgar llega con holgura */
internal val ALTO_FILA = 56.dp

/** Lo que mide la columna del icono, y el margen que la separa del borde y del texto */
private val ANCHO_ICONO = 44.dp
private val MARGEN_IZQ = 8.dp
private val SEPARA_TEXTO = 6.dp
private val SANGRIA = MARGEN_IZQ + ANCHO_ICONO + SEPARA_TEXTO

/** Cuánto hay que arrastrar a la derecha para que el gesto cuente, y hasta dónde se deja correr la fila */
private val UMBRAL_GESTO = 96.dp
private val TOPE_GESTO = 132.dp

/** El gris del gesto «Cursando»: el mismo en claro y en oscuro, como en la maqueta */
private val GRIS_GESTO = Color(0xFF2E3137)

/** Lo que hace cada toque de una fila: un objeto estable para que ninguna fila se repinte por culpa de otra */
@Stable
internal class AccionesFila(
    val tocar: (FilaLista) -> Unit,
    val deslizar: (FilaLista, Paso) -> Unit,
    val deshacer: () -> Unit,
)

@Composable
internal fun FilaDeLista(
    fila: FilaLista,
    ultima: Boolean,
    seleccionada: Boolean,
    ofreceDeshacer: Boolean,
    realce: Int,
    acciones: AccionesFila,
    modifier: Modifier = Modifier,
) {
    val paso = remember(fila.situacion) { fila.situacion?.let(::pasoSiguiente) }
    val desplazo = remember { mutableFloatStateOf(0f) }
    val arrastrando by remember { derivedStateOf { desplazo.floatValue > 0f } }
    val alcance = rememberCoroutineScope()
    val haptica = LocalHapticFeedback.current
    val densidad = LocalDensity.current
    val umbral = with(densidad) { UMBRAL_GESTO.toPx() }
    val tope = with(densidad) { TOPE_GESTO.toPx() }

    // El realce nace encendido y se apaga en 1,2 s; solo se lee al dibujar
    val brillo = remember { Animatable(0f) }
    LaunchedEffect(realce) {
        if (realce > 0) {
            brillo.snapTo(1f)
            brillo.animateTo(0f, tween(REALCE_MS, easing = Movimiento.ESTANDAR))
        }
    }

    val arrastre = rememberDraggableState { d -> desplazo.floatValue = (desplazo.floatValue + d).coerceIn(0f, tope) }

    Box(modifier.fillMaxWidth().clipToBounds()) {
        if (paso != null && arrastrando) FondoDelGesto(paso, Modifier.matchParentSize())
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = desplazo.floatValue }
                .semantics { contentDescription = fila.descripcion }
                .drawBehind {
                    drawRect(Paleta.lienzo)
                    val dentro = MARGEN_IZQ.toPx()
                    val caja = Size(size.width - 2 * dentro, size.height)
                    val radio = CornerRadius(14.dp.toPx())
                    if (seleccionada) drawRoundRect(REALCE, Offset(dentro, 0f), caja, radio)
                    if (brillo.value > 0f) drawRoundRect(Paleta.tinta.copy(alpha = 0.09f * brillo.value), Offset(dentro, 0f), caja, radio)
                    if (!ultima && !seleccionada) drawLine(RAYA, Offset(SANGRIA.toPx(), size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
                }
                .draggable(
                    arrastre, Orientation.Horizontal, enabled = paso != null,
                    onDragStopped = {
                        if (paso != null && desplazo.floatValue >= umbral) {
                            haptica.performHapticFeedback(HapticFeedbackType.Confirm)
                            acciones.deslizar(fila, paso)
                        }
                        alcance.launch {
                            animate(desplazo.floatValue, 0f, animationSpec = tween(Movimiento.MEDIA, easing = Movimiento.ESTANDAR)) { v, _ -> desplazo.floatValue = v }
                        }
                    },
                )
                .clickable(role = Role.Button) { acciones.tocar(fila) }
                .heightIn(min = ALTO_FILA)
                .padding(start = MARGEN_IZQ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(ANCHO_ICONO).heightIn(min = ALTO_FILA), contentAlignment = Alignment.Center) { IconoDeEstado(fila.situacion) }
            Column(Modifier.weight(1f).padding(start = SEPARA_TEXTO, top = 9.dp, bottom = 9.dp)) {
                BasicText(
                    fila.nombre,
                    style = estiloNombre(if (fila.situacion == Situacion.HECHA) Paleta.suave else Paleta.tinta),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    conNegritas(fila.razon), Modifier.padding(top = 1.dp),
                    style = estiloRazon(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                ofreceDeshacer -> Box(
                    Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = acciones.deshacer).padding(start = 12.dp, end = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText("Deshacer", style = estiloAccion()) }
                fila.esCasilla -> BasicText("Elegir", Modifier.padding(start = 12.dp, end = 20.dp), style = estiloAccion())
                fila.uc != null -> BasicText("${fila.uc} UC", Modifier.padding(start = 10.dp, end = 20.dp), style = estiloPlex(), maxLines = 1)
                else -> Box(Modifier.width(20.dp))
            }
        }
    }
}

/** Lo que se descubre bajo la fila al arrastrarla: el paso que se va a dar, en su color */
@Composable
private fun FondoDelGesto(paso: Paso, modifier: Modifier) {
    val aprobar = paso.marca == Marca.APROBADA
    val fondo = if (aprobar) Paleta.verde else GRIS_GESTO
    Row(
        modifier.drawBehind { drawRect(fondo) }.padding(start = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconoDeEstadoSobre(if (aprobar) Situacion.HECHA else Situacion.CURSANDO, Color.White, fondo)
        BasicText(paso.etiqueta, style = estiloAccion().copy(color = Color.White, fontSize = 14.sp))
    }
}
