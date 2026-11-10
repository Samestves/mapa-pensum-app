package mpensum.carrera

import mpensum.componentes.superficie
import mpensum.componentes.nota
import mpensum.componentes.BotonTinta
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import mpensum.componentes.ventana
import mpensum.logica.Marca
import mpensum.logica.Situacion
import mpensum.logica.seAbrenAlAprobar
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio

/* Los botones para marcar una materia (cursando, aprobada) en su ficha */

/** Qué botones lleva cada estado: el lleno es siempre el siguiente paso */
private enum class Botones { DISPONIBLE, CURSANDO, BLOQUEADA, APROBADA }

private fun botonesDe(marca: Marca?, bloqueada: Boolean) = when {
    marca == Marca.APROBADA -> Botones.APROBADA
    marca == Marca.CURSANDO -> Botones.CURSANDO
    bloqueada -> Botones.BLOQUEADA
    else -> Botones.DISPONIBLE
}

/**
 * Los botones de marcar. Con 320 dp o letra grande se apilan; si no, van en
 * dos columnas. «Se te abre N» solo sale si aprobarla abre algo. Cada marca
 * lleva su háptico, y los textos se funden en 150 ms al cambiar de estado.
 */
@Composable
internal fun BotonesMarca(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, modifier: Modifier = Modifier, compacto: Boolean = false) {
    val haptica = LocalHapticFeedback.current
    val marca = estado.marcas[codigo]
    val bloqueada = estado.situaciones[codigo].let { it == Situacion.PROXIMA || it == Situacion.LEJANA }
    val abre = remember(estado.marcas, codigo) { seAbrenAlAprobar(estado.pensum, estado.marcas, listOf(codigo)).size }
    val apilados = !compacto && ventana().let { it.compacta || it.letraGrande }
    val alto = if (compacto) 48.dp else 52.dp
    val radio = if (compacto) 16.dp else Radio.Barra
    fun marcar(m: Marca?) {
        haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
        alMarcar(codigo, m)
    }
    val nota = when (abre) {
        0 -> null
        1 -> "Se te abre 1"
        else -> "Se te abren $abre"
    }
    AnimatedContent(
        targetState = botonesDe(marca, bloqueada),
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { fundidoEnSerie().using(null) },
        label = "botones",
    ) { modo ->
        val cursando = @Composable { m: Modifier, lleno: Boolean -> BotonTinta("La estoy cursando", { marcar(Marca.CURSANDO) }, m, superficie = !lleno, alto = alto, radio = radio) }
        val aprobada = @Composable { m: Modifier, lleno: Boolean ->
            // Sobre el botón lleno la nota sigue la tinta del panel; en uno de superficie, azul: es lo que se te abre
            BotonTinta("Ya la aprobé", { marcar(Marca.APROBADA) }, m, nota = nota, colorNota = if (lleno) null else Paleta.azul, superficie = !lleno, alto = alto, radio = radio)
        }
        val desmarcar = @Composable { m: Modifier -> BotonTinta("Desmarcar", { marcar(null) }, m, superficie = true, alto = alto, radio = radio) }
        val dos = modo != Botones.APROBADA
        val primero: @Composable (Modifier) -> Unit = { m ->
            when (modo) {
                Botones.DISPONIBLE -> cursando(m, true)
                Botones.CURSANDO -> aprobada(m, true)
                Botones.BLOQUEADA -> cursando(m, false)
                Botones.APROBADA -> desmarcar(m)
            }
        }
        val segundo: @Composable (Modifier) -> Unit = { m ->
            when (modo) {
                Botones.DISPONIBLE, Botones.BLOQUEADA -> aprobada(m, false)
                else -> desmarcar(m)
            }
        }
        if (apilados) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                primero(Modifier)
                if (dos) segundo(Modifier)
            }
        } else if (dos) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                primero(Modifier.weight(1f))
                segundo(Modifier.weight(1f))
            }
        } else primero(Modifier)
    }
}
