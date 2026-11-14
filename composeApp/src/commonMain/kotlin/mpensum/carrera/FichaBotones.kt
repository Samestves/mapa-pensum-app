package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mpensum.componentes.BotonesDePaso
import mpensum.componentes.ventana
import mpensum.logica.AccionMarca
import mpensum.logica.Marca
import mpensum.logica.Situacion

/* Los botones para marcar una materia (cursando, aprobada) en su ficha */

/** Qué botones lleva cada estado: el de la izquierda es siempre el siguiente paso */
private enum class Botones { DISPONIBLE, CURSANDO, BLOQUEADA, APROBADA }

private fun botonesDe(marca: Marca?, bloqueada: Boolean) = when {
    marca == Marca.APROBADA -> Botones.APROBADA
    marca == Marca.CURSANDO -> Botones.CURSANDO
    bloqueada -> Botones.BLOQUEADA
    else -> Botones.DISPONIBLE
}

/**
 * Los botones de marcar (`BotonesDePaso`, los mismos de la lista). Con 320 dp
 * o letra grande se apilan; si no, van lado a lado. Los textos se funden en
 * 150 ms al cambiar de estado.
 */
@Composable
internal fun BotonesMarca(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, modifier: Modifier = Modifier, compacto: Boolean = false) {
    val marca = estado.marcas[codigo]
    val bloqueada = estado.situaciones[codigo].let { it == Situacion.PROXIMA || it == Situacion.LEJANA }
    val apilados = !compacto && ventana().let { it.compacta || it.letraGrande }
    val alto = if (compacto) 48.dp else 52.dp
    fun marcar(m: Marca?) = alMarcar(codigo, m)
    AnimatedContent(
        targetState = botonesDe(marca, bloqueada),
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { fundidoEnSerie().using(null) },
        label = "botones",
    ) { modo ->
        val cursando = AccionMarca("La estoy cursando", Marca.CURSANDO)
        val aprobada = AccionMarca("Ya la aprobé", Marca.APROBADA)
        // El de la izquierda es siempre el paso que toca; una aprobada solo se puede desmarcar
        val (principal, secundaria) = when (modo) {
            Botones.DISPONIBLE, Botones.BLOQUEADA -> cursando to aprobada
            Botones.CURSANDO -> aprobada to AccionMarca("Ya no la curso", null)
            Botones.APROBADA -> null to AccionMarca("Quitar la aprobación", null)
        }
        BotonesDePaso(principal, secundaria, ::marcar, alto = alto, apilados = apilados)
    }
}
