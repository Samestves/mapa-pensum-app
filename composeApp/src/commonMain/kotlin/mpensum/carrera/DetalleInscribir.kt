package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.componentes.IconoRelleno
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Phosphor
import mpensum.componentes.pulsable
import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.logica.compararEs
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tipo

/*
 * «Puedes inscribir»: las materias abiertas ahora, primero las que más
 * destraban (las que abren más materias después, directas o por cadena). Cada
 * renglón dice cuántas abre; tocarlo lo muestra en el mapa y el botón de abajo
 * lleva todas. Acostado: la cifra a la izquierda y la lista a la derecha.
 */

/** Una materia abierta y cuántas materias abre su aprobación, contando las de las que abre */
internal class Inscribible(val materia: Asignatura, val abre: Int)

/** Las inscribibles de más a menos que abren; en empate, las más atrasadas y por nombre */
internal fun inscribiblesPorImpacto(pensum: Pensum, abiertas: List<Asignatura>): List<Inscribible> {
    val hijos = HashMap<String, MutableList<String>>()
    for (a in pensum.obligatorias) for (p in a.prerrequisitos) hijos.getOrPut(p) { mutableListOf() }.add(a.codigo)
    fun alcanzadas(origen: String): Int {
        val vistas = HashSet<String>()
        val pila = ArrayDeque(hijos[origen].orEmpty())
        while (pila.isNotEmpty()) {
            val c = pila.removeLast()
            if (vistas.add(c)) pila.addAll(hijos[c].orEmpty())
        }
        return vistas.size
    }
    return abiertas.map { Inscribible(it, alcanzadas(it.codigo)) }
        .sortedWith(compareByDescending<Inscribible> { it.abre }.thenBy { it.materia.semestre ?: 99 }.then { a, b -> compararEs(a.materia.nombre, b.materia.nombre) })
}

@Composable
internal fun DetalleInscribir(estado: EstadoCarrera, alVerEnMapa: (List<String>) -> Unit) {
    val abiertas = estado.progreso.paraInscribir
    val lista = remember(abiertas, estado.pensum) { inscribiblesPorImpacto(estado.pensum, abiertas) }
    val ucs = abiertas.sumOf { it.uc ?: 0 }
    val todas = lista.map { it.materia.codigo }
    CuerpoDetalle(
        izquierda = {
            BasicText(
                buildAnnotatedString {
                    append("${abiertas.size}")
                    withStyle(SpanStyle(fontSize = Tipo.l, fontWeight = FontWeight.Normal, color = Paleta.suave, letterSpacing = 0.em)) {
                        append("  ${if (abiertas.size == 1) "materia" else "materias"}")
                        if (ucs > 0) append(", $ucs UC")
                    }
                },
                style = estilo(44, Paleta.azul, FontWeight.Light, 48).copy(letterSpacing = (-0.03).em),
            )
            BasicText(
                if (lista.isEmpty()) "Ahora no hay materias abiertas. Cuando apruebes las que cursas, aparecen aquí." else "Primero las que destraban más.",
                Modifier.padding(top = Espacio.xxs),
                style = estilo(Tipo.m, Paleta.suave, interlinea = 20),
            )
            if (lista.isNotEmpty()) {
                Spacer(Modifier.height(Espacio.m))
                BotonVerEnMapa(Modifier.fillMaxWidth()) { alVerEnMapa(todas) }
            }
        },
        derecha = {
            Column(Modifier.padding(top = Espacio.s)) {
                lista.forEachIndexed { k, i ->
                    if (k > 0) Hilo()
                    RenglonInscribible(i) { alVerEnMapa(listOf(i.materia.codigo)) }
                }
            }
        },
    )
}

@Composable
private fun BotonVerEnMapa(modifier: Modifier, alTocar: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(Radio.Grande)).background(Paleta.tinta).clickable(role = Role.Button, onClick = alTocar).heightIn(min = Alto.fila),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoRelleno(Phosphor.mapa, false, Paleta.lienzo, 18.dp)
            BasicText("Verlas en el mapa", Modifier.padding(start = Espacio.xs), style = estilo(Tipo.l, Paleta.lienzo, FontWeight.SemiBold, 22))
        }
    }
}

@Composable
private fun RenglonInscribible(i: Inscribible, alTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pulsable(alTocar, escala = 0.99f).heightIn(min = Alto.fila + Espacio.xs).padding(vertical = Espacio.xs, horizontal = Espacio.xxs).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        IconoTrazo(IconosHoja.candadoAbierto, Paleta.azul, 20.dp, 1.9f)
        Column(Modifier.weight(1f)) {
            BasicText(i.materia.nombre, style = estilo(Tipo.l, Paleta.tinta, interlinea = 20), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(
                if (i.abre == 0) "no abre otras" else "abre ${i.abre} ${if (i.abre == 1) "materia" else "materias"}",
                Modifier.padding(top = Espacio.hilo),
                style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16),
            )
        }
        i.materia.uc?.let { BasicText("$it UC", style = estilo(Tipo.s, Paleta.tenue)) }
    }
}
