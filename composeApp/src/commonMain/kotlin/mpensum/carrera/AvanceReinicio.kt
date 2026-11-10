package mpensum.carrera

import mpensum.componentes.superficie
import mpensum.componentes.estiloFila
import mpensum.componentes.IconosHoja
import mpensum.componentes.Velo
import mpensum.componentes.IconoLucide
import mpensum.componentes.estiloNota
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/* Reiniciar el avance: lo único de la hoja que no se deshace */

/**
 * Reiniciar el avance de la carrera: lo único de la hoja que no se deshace, y
 * por eso lo más callado hasta que se pide. Te deja en el 0 %: sin marcas,
 * sin las electivas que pusiste y con la carga habitual. Pulsarlo no borra
 * nada: dice qué se va a perder y deja Cancelar del lado del pulgar. Sin caja
 * roja: el rojo solo va en el botón que borra.
 */
@Composable
internal fun BotonReinicio(estado: EstadoCarrera) {
    val hay = estado.hayAvance
    var confirmando by remember { mutableStateOf(false) }
    if (!confirmando || !hay) {
        val color = Paleta.suave.copy(alpha = if (hay) 1f else 0.5f)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).superficie(24.dp).clickable(enabled = hay, role = Role.Button) { confirmando = true }.padding(horizontal = Espacio.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hay) IconoLucide(IconosHoja.reiniciar, color, 16.dp, Modifier.padding(end = Espacio.xs))
            BasicText(if (hay) "Reiniciar mi avance" else "Nada que reiniciar todavía", style = estiloFila(color))
        }
        return
    }
    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        BasicText("¿Empezar de cero?", style = estiloFila())
        BasicText(
            "Se borra${loQueSeBorra(estado)} en esta carrera. Vuelves al 0% y no se puede deshacer.",
            Modifier.padding(top = Espacio.xxs),
            style = estiloNota(Paleta.suave).copy(lineHeight = 18.sp),
        )
        Row(Modifier.padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Paleta.tinta.copy(alpha = Velo.pastilla)).clickable(role = Role.Button) { confirmando = false },
                contentAlignment = Alignment.Center,
            ) { BasicText("Cancelar", style = estiloFila()) }
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Paleta.rojo.copy(alpha = 0.16f)).clickable(role = Role.Button) {
                    estado.reiniciar()
                    confirmando = false
                },
                contentAlignment = Alignment.Center,
            ) { BasicText("Borrar todo", style = estiloFila(ROJO_LETRA)) }
        }
    }
}

/** " tu materia marcada, las electivas que pusiste y tu carga por semestre": solo lo que de verdad hay */
private fun loQueSeBorra(estado: EstadoCarrera): String {
    val cuantas = estado.marcas.size
    val partes = listOfNotNull(
        when (cuantas) {
            0 -> null
            1 -> "tu materia marcada"
            else -> "tus $cuantas materias marcadas"
        },
        if (estado.casillas.isNotEmpty()) "las electivas que pusiste" else null,
        if (estado.carga != null) "tu carga por semestre" else null,
    )
    // "Se borran" con más de una cosa o con varias materias; "se borra" con una sola
    val plural = partes.size > 1 || cuantas > 1
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
    return "${if (plural) "n" else ""} $lista"
}

/** El rojo mezclado un cuarto con la tinta: se lee sobre su propio velo (AA) */
private val ROJO_LETRA get() = lerp(Paleta.rojo, Paleta.tinta, 0.3f)
