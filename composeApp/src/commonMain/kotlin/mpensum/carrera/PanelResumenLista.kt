package mpensum.carrera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.iconoEstado
import mpensum.componentes.pulsable
import mpensum.logica.FilaMateria
import mpensum.logica.SeccionLista
import mpensum.logica.Situacion
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tipo

/*
 * El panel de la derecha de la lista, acostado, cuando no hay ficha: así la
 * mitad de la pantalla nunca queda vacía. Resume el semestre que miras (el
 * primero abierto en la lista): su aro, lo que puedes inscribir y lo que te
 * falta. Un toque en un renglón abre su ficha en este mismo panel.
 */

/** El fondo de la pieza, el mismo de las piezas del avance: la tinta al 4,5 % */
private const val VELO_PIEZA = 0.045f
private val LADO_ARO = 64.dp
private val GROSOR_ARO = 5.dp
private val LADO_ICONO_RENGLON = 18.dp

@Composable
internal fun PanelResumenLista(
    estado: EstadoCarrera,
    /** El semestre que mira la lista; null mientras no ha avisado: se usa el actual */
    semestre: Int?,
    /** false cuando hay ficha: el resumen se funde y se va */
    visible: Boolean,
    panel: PanelLateral,
    abajo: Dp,
    alElegir: (String) -> Unit,
) {
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "resumen")
    if (opacidad <= 0f) return

    val modelo by remember(estado) { derivedStateOf { estado.modeloDeLista() } }
    val semestres = modelo.secciones.filter { it.semestre != null }
    val seccion = semestres.firstOrNull { it.semestre == (semestre ?: modelo.actual) } ?: semestres.lastOrNull()

    Box(Modifier.fillMaxSize().padding(start = panel.inicio, end = panel.fin, top = panel.arriba, bottom = abajo)) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = opacidad }
                .clip(RoundedCornerShape(Radio.Grande))
                .background(Paleta.tinta.copy(alpha = VELO_PIEZA)),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Espacio.m, end = Espacio.m, top = Espacio.m)) {
                if (seccion != null) ResumenSemestre(seccion, visible, alElegir)
            }
            BasicText(
                "Toca una materia para ver su ficha",
                Modifier.fillMaxWidth().padding(horizontal = Espacio.m, vertical = Espacio.s),
                style = estilo(Tipo.xs, Paleta.tenue).copy(textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun ResumenSemestre(s: SeccionLista, activo: Boolean, alElegir: (String) -> Unit) {
    val total = s.materias.size + s.casillas.size
    val hechas = s.materias.count { it.situacion == Situacion.HECHA }
    val cursando = s.materias.count { it.situacion == Situacion.CURSANDO }
    val inscribibles = s.materias.filter { it.situacion == Situacion.INSCRIBIBLE }
    val bloqueadas = s.materias.filter { it.situacion == Situacion.PROXIMA || it.situacion == Situacion.LEJANA }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Aro(s, hechas, total)
        Column(Modifier.weight(1f)) {
            BasicText("SEMESTRE ${s.semestre}", style = versal(Paleta.tenue))
            BasicText(
                "$hechas de $total aprobadas",
                Modifier.padding(top = Espacio.hilo),
                style = estilo(Tipo.m, Paleta.tinta, FontWeight.Medium),
            )
            if (cursando > 0) BasicText("$cursando cursando", style = estilo(Tipo.xs, Paleta.suave))
        }
    }

    if (s.completo) {
        Row(
            Modifier.padding(top = Espacio.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Canvas(Modifier.size(LADO_ICONO_RENGLON)) { iconoEstado(Situacion.HECHA, 0f, 0f, size.minDimension, Paleta.lienzo) }
            BasicText("Semestre completo", style = estilo(Tipo.m, Paleta.verde, FontWeight.Medium))
        }
        return
    }

    if (inscribibles.isNotEmpty()) {
        Grupo("PUEDES INSCRIBIR", Paleta.azul) {
            inscribibles.forEach { Renglon(it, activo, alElegir) }
        }
    }
    if (bloqueadas.isNotEmpty()) {
        Grupo("TE FALTA", Paleta.tenue) {
            bloqueadas.forEach { Renglon(it, activo, alElegir) }
        }
    }
    if (inscribibles.isEmpty() && bloqueadas.isEmpty()) {
        BasicText("Nada por inscribir ahora", Modifier.padding(top = Espacio.l), style = estilo(Tipo.s, Paleta.tenue))
    }
}

@Composable
private fun Grupo(rotulo: String, color: Color, contenido: @Composable () -> Unit) {
    BasicText(rotulo, Modifier.padding(top = Espacio.l, bottom = Espacio.xxs), style = versal(color))
    contenido()
}

/** El aro del semestre: lo aprobado en verde y, detrás, lo que cursas en ámbar; completo, con su check */
@Composable
private fun Aro(s: SeccionLista, hechas: Int, total: Int) {
    Box(Modifier.size(LADO_ARO), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(LADO_ARO)) {
            val g = GROSOR_ARO.toPx()
            val caja = Size(size.width - g, size.height - g)
            val esquina = Offset(g / 2, g / 2)
            drawArc(Paleta.tinta.copy(alpha = 0.16f), 0f, 360f, false, esquina, caja, style = Stroke(g))
            val v = s.aprobado * 360f
            val a = s.cursado * 360f
            if (v > 0.1f) drawArc(Paleta.verde, -90f, v, false, esquina, caja, style = Stroke(g, cap = StrokeCap.Round))
            if (a > 0.1f) drawArc(Paleta.ambar, -90f + v, a, false, esquina, caja, style = Stroke(g, cap = StrokeCap.Round))
        }
        if (s.completo) Canvas(Modifier.size(LADO_ARO / 2)) { iconoEstado(Situacion.HECHA, 0f, 0f, size.minDimension, Paleta.lienzo) }
        else BasicText("$hechas/$total", style = estilo(Tipo.l, Paleta.tinta, FontWeight.Medium))
    }
}

/** "Requiere X" y "Se abre al aprobar X" se leen aquí como "pide X" */
private fun pide(nota: String): String? = when {
    nota.startsWith("Requiere ") -> "pide " + nota.removePrefix("Requiere ")
    nota.startsWith("Se abre al aprobar ") -> "pide " + nota.removePrefix("Se abre al aprobar ")
    else -> null
}

@Composable
private fun Renglon(f: FilaMateria, activo: Boolean, alElegir: (String) -> Unit) {
    val libre = f.situacion == Situacion.INSCRIBIBLE
    val secundario = if (libre) f.uc?.let { "$it UC" } else pide(f.nota)
    Row(
        Modifier
            .fillMaxWidth()
            .pulsable({ if (activo) alElegir(f.codigo) }, escala = 0.99f)
            .heightIn(min = Alto.toque)
            .padding(vertical = Espacio.xs)
            .semantics(mergeDescendants = true) { contentDescription = "${f.nombre}, ${f.nota}. Ver la ficha" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        IconoTrazo(
            if (libre) IconosHoja.candadoAbierto else IconosHoja.candado,
            if (libre) Paleta.azul else Paleta.tenue,
            LADO_ICONO_RENGLON,
            1.9f,
        )
        Column(Modifier.weight(1f)) {
            BasicText(
                f.nombre,
                style = estilo(Tipo.m, if (libre) Paleta.tinta else Paleta.suave, interlinea = 18),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (secundario != null) BasicText(
                secundario,
                Modifier.padding(top = Espacio.hilo),
                style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
