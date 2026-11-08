package mpensum.carrera.lista

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.logica.AccionMarca
import mpensum.logica.Marca
import mpensum.logica.PlanMuelle
import mpensum.logica.Situacion
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.inter

/*
 * El muelle: lo que se decide va fijo abajo, al alcance del pulgar. Un solo
 * botón lleno de 56 dp con el paso que toca, el otro en texto debajo y, encima,
 * lo que pasa si se da. Acostado, los dos botones van lado a lado. Es el único
 * botón lleno de la pantalla.
 */

/** El alto del botón principal y de la acción de texto */
private val ALTO_PRINCIPAL = 56.dp
private val ALTO_TEXTO = 44.dp

/** Lo que tapa el muelle de pie, para que la página deje ese hueco al final: velo, consecuencia, botón y texto */
internal val ALTO_MUELLE = 34.dp + 18.dp + 12.dp + ALTO_PRINCIPAL + 6.dp + ALTO_TEXTO + 26.dp

/**
 * El muelle de pie: velo del lienzo que sube desde el borde para que el
 * contenido se desvanezca bajo él, y encima la consecuencia y los dos pasos.
 * `abajo` es lo que ocupan los gestos del sistema.
 */
@Composable
internal fun MuelleDePie(situacion: Situacion, plan: PlanMuelle, abajo: Dp, alMarcar: (Marca?) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().drawBehind {
            // Un velo corto: el contenido se apaga bajo la consecuencia y queda lienzo liso detrás del botón
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Paleta.lienzo, startY = 0f, endY = 26.dp.toPx()))
        }.padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 26.dp + abajo),
    ) {
        Crossfade(situacion, animationSpec = tween(Movimiento.CORTA), label = "muelle") { _ ->
            Column {
                Consecuencia(plan, Modifier.fillMaxWidth().padding(bottom = 12.dp), centrada = true)
                plan.principal?.let { Principal(it, plan.lleno, Modifier.fillMaxWidth(), alMarcar) }
                Box(
                    Modifier.fillMaxWidth().height(ALTO_TEXTO).padding(top = 6.dp)
                        .clickable(role = Role.Button) { alMarcar(plan.secundaria.marca) },
                    contentAlignment = Alignment.Center,
                ) { BasicText(plan.secundaria.texto, Modifier.padding(horizontal = DENTRO_BOTON), style = estiloBoton(15.sp, Paleta.tinta), maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 15.sp, 0.5.sp)) }
            }
        }
    }
}

/** El muelle acostado, en el pie del panel: la consecuencia y los dos pasos lado a lado, sin cajas */
@Composable
internal fun MuelleAcostado(situacion: Situacion, plan: PlanMuelle, alMarcar: (Marca?) -> Unit, modifier: Modifier = Modifier) {
    Crossfade(situacion, modifier, animationSpec = tween(Movimiento.CORTA), label = "muelle") { _ ->
        Column {
            Consecuencia(plan, Modifier.padding(bottom = 10.dp), centrada = false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                plan.principal?.let { Principal(it, plan.lleno, Modifier.weight(1.7f), alMarcar, alto = 50.dp, icono = 19.dp) }
                // Con paso lleno, el otro es un botón suave; sin él, es el único
                Box(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(BOTON)
                        .clickable(role = Role.Button) { alMarcar(plan.secundaria.marca) },
                    contentAlignment = Alignment.Center,
                ) { BasicText(plan.secundaria.texto, Modifier.padding(horizontal = DENTRO_BOTON), style = estiloBoton(15.sp, Paleta.tinta), maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 15.sp, 0.5.sp)) }
            }
        }
    }
}

/** Lo que pasa si se da el paso: el icono del estado al que lleva y una línea con lo destacado en tinta */
@Composable
private fun Consecuencia(plan: PlanMuelle, modifier: Modifier, centrada: Boolean) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, if (centrada) Alignment.CenterHorizontally else Alignment.Start),
    ) {
        IconoDeEstado(plan.icono, 14.dp)
        BasicText(conNegritas(plan.consecuencia), style = estiloRazon().copy(lineHeight = 18.sp), maxLines = 1)
    }
}

/** El botón del paso que toca: lleno en tinta cuando se puede dar, suave cuando no (una bloqueada) */
@Composable
private fun Principal(accion: AccionMarca, lleno: Boolean, modifier: Modifier, alMarcar: (Marca?) -> Unit, alto: Dp = ALTO_PRINCIPAL, icono: Dp = 20.dp) {
    val haptica = LocalHapticFeedback.current
    val letra = if (lleno) Paleta.lienzo else Paleta.tinta
    Row(
        modifier.height(alto).clip(RoundedCornerShape(alto / 2)).background(if (lleno) Paleta.tinta else BOTON)
            .clickable(role = Role.Button) {
                haptica.performHapticFeedback(HapticFeedbackType.Confirm)
                alMarcar(accion.marca)
            }
            .padding(horizontal = DENTRO_BOTON),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        accion.marca?.let { IconoDeEstadoSobre(if (it == Marca.APROBADA) Situacion.HECHA else Situacion.CURSANDO, letra, if (lleno) Paleta.tinta else Paleta.lienzo, icono) }
        // Acostado el botón es más estrecho: la letra baja antes de tocar el borde, nunca se corta
        BasicText(accion.texto, Modifier.weight(1f, fill = false), style = estiloBoton(16.sp, letra), maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 16.sp, 0.5.sp))
    }
}

/** Lo que el texto de un botón se aparta de sus bordes redondos */
private val DENTRO_BOTON = 18.dp

@Composable
private fun estiloBoton(tam: androidx.compose.ui.unit.TextUnit, color: Color) =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = tam, lineHeight = 20.sp, letterSpacing = (-0.005).em, color = color, textAlign = TextAlign.Center)
