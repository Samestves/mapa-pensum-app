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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import mpensum.tema.Espacio
import mpensum.tema.Movimiento
import mpensum.tema.Paleta

/*
 * La fila de la lista, dentro de su nube: la pastilla de estado, el nombre en
 * Jost (dos líneas si hace falta, nunca cortado a la mitad de una palabra
 * corta), una sola razón corta debajo y, a la derecha, las UC en pequeño. No
 * hay sombras. Lo que cambia con el tiempo (el realce que se apaga, el
 * desplazamiento del gesto) se lee al dibujar, no al componer.
 *
 * La fila pinta su trozo de nube (ver NubesLista.kt): el recorte redondo va en
 * la misma capa que ya mueve el gesto, así que no cuesta una capa más.
 */

/** Alto mínimo de una fila: el de la hoja de progreso */
private val ALTO_FILA = 64.dp

/** La pastilla a 16 dp del borde de la nube y el texto a 60, como en el progreso. El hilo empieza donde el texto */
private val SANGRIA = Espacio.m + 32.dp + Espacio.s

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

/**
 * `primera` y `ultima` dicen qué esquinas de la nube redondea la fila; `hueco`
 * la separa de la nube anterior de su misma sección.
 */
@Composable
internal fun FilaDeLista(
    fila: FilaLista,
    primera: Boolean,
    ultima: Boolean,
    seleccionada: Boolean,
    ofreceDeshacer: Boolean,
    realce: Int,
    acciones: AccionesFila,
    modifier: Modifier = Modifier,
    hueco: Boolean = false,
) {
    val forma = remember(primera, ultima) { formaDeTrozo(primera, ultima) }
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

    Box(modifier.fillMaxWidth().padding(start = MARGEN_TARJETA, end = MARGEN_TARJETA, top = if (hueco) HUECO_TARJETA else 0.dp).clipToBounds()) {
        if (paso != null && arrastrando) FondoDelGesto(paso, Modifier.matchParentSize().clip(forma))
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = desplazo.floatValue
                    shape = forma
                    clip = true
                }
                .semantics { contentDescription = fila.descripcion }
                .drawBehind {
                    // El trozo de nube, opaco; la elegida y el realce lo tiñen entero, sin hilo
                    drawRect(FONDO_TARJETA)
                    if (seleccionada) drawRect(REALCE)
                    if (brillo.value > 0f) drawRect(Paleta.tinta.copy(alpha = 0.09f * brillo.value))
                    if (!ultima && !seleccionada) drawLine(HILO_TARJETA, Offset(SANGRIA.toPx(), size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
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
                .padding(start = Espacio.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PastillaDeEstado(fila.situacion)
            Column(Modifier.weight(1f).padding(start = Espacio.s, top = 11.dp, bottom = 11.dp)) {
                BasicText(
                    fila.nombre,
                    style = estiloNombre(if (fila.situacion == Situacion.HECHA) Paleta.suave else Paleta.tinta),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    conNegritas(fila.razon), Modifier.padding(top = 2.dp),
                    style = estiloRazon(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                ofreceDeshacer -> AccionEnPastilla("Deshacer", acciones.deshacer)
                fila.esCasilla -> AccionEnPastilla("Elegir", null)
                // Las UC en pequeño: se leen al sumar, no compiten con el nombre
                fila.uc != null -> BasicText("${fila.uc} UC", Modifier.padding(start = 10.dp, end = Espacio.m), style = estiloPlex(tam = 11f), maxLines = 1)
                else -> Box(Modifier.width(Espacio.m))
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
        modifier.drawBehind { drawRect(fondo) }.padding(start = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconoDeEstadoSobre(if (aprobar) Situacion.HECHA else Situacion.CURSANDO, Color.White, fondo)
        BasicText(paso.etiqueta, style = estiloAccion().copy(color = Color.White, fontSize = 14.sp))
    }
}
