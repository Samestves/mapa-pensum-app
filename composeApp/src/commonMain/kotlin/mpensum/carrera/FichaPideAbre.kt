package mpensum.carrera

import mpensum.componentes.TarjetaLista
import mpensum.componentes.estiloFila
import mpensum.componentes.Velo
import mpensum.componentes.Chevron
import mpensum.componentes.estiloRotulo
import mpensum.componentes.estiloNota
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mpensum.componentes.pulsable
import mpensum.datos.Asignatura
import mpensum.logica.Situacion
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/* «Pide · Abre» de la ficha de una materia: sus prerrequisitos y lo que abre */

private val ANCHO_ROTULO = 36.dp

/**
 * Lo que pide y lo que abre, cada una con su estado y un toque que lleva a
 * ella. El rótulo va solo en la primera fila de cada grupo, y un hilo parte
 * los dos grupos.
 */
@Composable
internal fun PideAbre(codigo: String, estado: EstadoCarrera, alIrA: (String) -> Unit, modifier: Modifier = Modifier) {
    val relaciones = estado.layout.relaciones
    val porCodigo = estado.layout.porCodigo
    val pide = relaciones.atras[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val abre = relaciones.adelante[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val especial = porCodigo[codigo]?.asignatura?.requisitoEspecial
    TarjetaLista(modifier) {
        GrupoPideAbre("PIDE", pide, vacio = especial == null, estado, alIrA)
        // Una condición del pensum que no es una materia: no se toca, solo se dice
        if (especial != null) {
            BasicText("Además: $especial", Modifier.padding(start = Espacio.m + ANCHO_ROTULO + Espacio.xs, end = Espacio.m, bottom = Espacio.xs), style = estiloNota(Paleta.suave))
        }
        Box(Modifier.padding(start = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = Velo.hilo)))
        GrupoPideAbre("ABRE", abre, vacio = true, estado, alIrA)
    }
}

@Composable
private fun GrupoPideAbre(rotulo: String, materias: List<Asignatura>, vacio: Boolean, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    if (materias.isEmpty()) {
        if (vacio) FilaPideAbre(rotulo, null, estado, alIrA)
        return
    }
    materias.forEachIndexed { k, m -> FilaPideAbre(if (k == 0) rotulo else null, m, estado, alIrA) }
}

/** Una fila de 48: rótulo (36), icono de estado (16), nombre a dos líneas y chevrón; sin materia dice «Ninguna» */
@Composable
private fun FilaPideAbre(rotulo: String?, materia: Asignatura?, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val puedeIr = materia != null && estado.nodoDe(materia.codigo) != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (puedeIr) Modifier.pulsable({ alIrA(materia!!.codigo) }, escala = 0.985f) else Modifier)
            .padding(horizontal = Espacio.m, vertical = Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = ANCHO_ROTULO)) { if (rotulo != null) BasicText(rotulo, style = estiloRotulo(), maxLines = 1, softWrap = false) }
        if (materia == null) {
            Spacer(Modifier.width(Espacio.xs + 16.dp))
            BasicText("Ninguna", style = estiloFila(Paleta.tenue, FontWeight.Normal))
            return@Row
        }
        val s = estado.situaciones[materia.codigo] ?: Situacion.LEJANA
        Spacer(Modifier.width(Espacio.xs))
        IconoSituacion(s, 16.dp)
        BasicText(
            materia.nombre, Modifier.weight(1f).padding(horizontal = Espacio.xs),
            style = estiloFila(if (s == Situacion.HECHA) Paleta.suave else Paleta.tinta), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (puedeIr) Chevron()
    }
}

/**
 * Pide · Abre en una línea cada uno, para la burbuja: la primera materia y «y N más».
 * Tocar la fila lleva a esa primera; sin materias dice «Ninguna».
 */
@Composable
internal fun PideAbreResumen(codigo: String, estado: EstadoCarrera, alIrA: (String) -> Unit, modifier: Modifier = Modifier) {
    val relaciones = estado.layout.relaciones
    val porCodigo = estado.layout.porCodigo
    val pide = relaciones.atras[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val abre = relaciones.adelante[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    TarjetaLista(modifier) {
        FilaResumen("PIDE", pide, estado, alIrA)
        Box(Modifier.padding(start = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = Velo.hilo)))
        FilaResumen("ABRE", abre, estado, alIrA)
    }
}

/** Una fila de 44: rótulo (36), icono de estado (16), el nombre a una línea con «y N más» y el chevrón */
@Composable
private fun FilaResumen(rotulo: String, materias: List<Asignatura>, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val primera = materias.firstOrNull()
    val puedeIr = primera != null && estado.nodoDe(primera.codigo) != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .then(if (puedeIr) Modifier.pulsable({ alIrA(primera!!.codigo) }, escala = 0.985f) else Modifier)
            .padding(horizontal = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = ANCHO_ROTULO)) { BasicText(rotulo, style = estiloRotulo(), maxLines = 1, softWrap = false) }
        if (primera == null) {
            BasicText("Ninguna", Modifier.padding(start = Espacio.xs + 16.dp), style = estiloFila(Paleta.tenue, FontWeight.Normal))
            return@Row
        }
        val s = estado.situaciones[primera.codigo] ?: Situacion.LEJANA
        Spacer(Modifier.width(Espacio.xs))
        IconoSituacion(s, 16.dp)
        BasicText(
            primera.nombre, Modifier.weight(1f, fill = false).padding(start = Espacio.xs),
            style = estiloFila(if (s == Situacion.HECHA) Paleta.suave else Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (materias.size > 1) BasicText(" y ${materias.size - 1} más", style = estiloNota(), maxLines = 1, softWrap = false)
        Spacer(Modifier.weight(1f))
        if (puedeIr) Chevron()
    }
}

// ─────────────────────────────────── Botones ──────────────────────────────────
