package mpensum.carrera.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mpensum.carrera.IconoLucide
import mpensum.carrera.nombreArea
import mpensum.componentes.Lucide
import mpensum.datos.Asignatura
import mpensum.logica.EstadoMateria
import mpensum.logica.MargenMateria
import mpensum.logica.MateriaLista
import mpensum.logica.Situacion
import mpensum.logica.descendientes
import mpensum.logica.margenDeMateria
import mpensum.logica.planMuelle
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * La página de una materia. De pie se empuja a pantalla completa, como una
 * página de iOS; acostada es el panel de la derecha. Arriba, el código, el
 * nombre y el estado; luego tres datos sin caja, el margen dibujado y lo que
 * pide y abre, cada materia enlazada a su página. Abajo, el muelle.
 */

/** Lo que se reserva al pie del panel acostado para su muelle: consecuencia, botones y respiro */
private val RESERVA_MUELLE_ACOSTADO = 112.dp

@Composable
internal fun PaginaDeMateria(
    sesion: SesionLista,
    codigo: String,
    acostada: Boolean,
    etiquetaVolver: String,
    arriba: Dp,
    abajo: Dp,
    alVolver: () -> Unit,
    alVerEnMapa: (String) -> Unit,
    alIrA: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val materia = sesion.universo.materia(codigo) ?: return
    val estado = sesion.estado
    val asignatura = estado.pensum.porCodigo[codigo]

    // El margen se mide fuera del hilo principal; tras marcar, la materia sale del plan y se queda el último que se midió
    val sim = estado.simulador
    var margen by remember(codigo) { mutableStateOf<MargenMateria?>(null) }
    LaunchedEffect(codigo, sim) {
        val m = withContext(Dispatchers.Default) { margenDeMateria(sim, codigo) }
        if (m != null) margen = m
    }

    val plan = planMuelle(materia, sesion.ucCursando)
    val alMarcar: (mpensum.logica.Marca?) -> Unit = { sesion.marcar(mapOf(codigo to it)) }
    val relaciones = estado.layout.relaciones
    val pide = relaciones.atras[codigo].orEmpty().mapNotNull { estado.pensum.porCodigo[it] }
    val abre = relaciones.adelante[codigo].orEmpty().mapNotNull { estado.pensum.porCodigo[it] }
    val tras = descendientes(codigo, relaciones.adelante) - abre.size
    val totalSemestres = remember(estado.pensum) { estado.pensum.semestres.maxOfOrNull { it.numero } ?: 10 }
    val scroll = rememberScrollState()

    Box(modifier.fillMaxSize()) {
        if (acostada) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = RESERVA_MUELLE_ACOSTADO)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Rotulo(materia)
                        Titulo(materia.nombre, 28.sp, 34.sp, Modifier.padding(top = 4.dp, bottom = 2.dp))
                        Estado(materia, margen, 16.dp, conMargen = true)
                    }
                    BotonRedondo({ alVerEnMapa(codigo) }, "Ver en el mapa") { IconoLucide(Lucide.mapa3, Paleta.tinta, 18.dp) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ColumnaRelacion("PIDE", pide, estado.situaciones, Modifier.weight(1f), alIrA)
                    ColumnaRelacion("ABRE", abre, estado.situaciones, Modifier.weight(1f), alIrA)
                }
            }
            MuelleAcostado(materia.situacion, plan, alMarcar, Modifier.align(Alignment.BottomCenter).padding(start = 22.dp, end = 22.dp, bottom = 18.dp))
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(top = arriba)) {
                BarraDePagina(etiquetaVolver, alVolver) { alVerEnMapa(codigo) }
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
                    Rotulo(materia)
                    Titulo(materia.nombre, 34.sp, 40.sp, Modifier.padding(top = 8.dp, bottom = 8.dp))
                    Estado(materia, margen, 18.dp, conMargen = false)
                }
                DatosDeMateria(materia, margen, asignatura, estado.pensum.tieneAreas)
                val medido = margen
                if (medido != null && materia.semestre != null) ReglaDeMargen(materia.semestre, totalSemestres, medido, Modifier.padding(top = 18.dp))
                SeccionDeRelacion("Pide", if (pide.isEmpty()) "" else "${pide.count { estado.estados[it.codigo] == EstadoMateria.APROBADA }} de ${pide.size}", Modifier.padding(top = 8.dp))
                if (pide.isEmpty() && asignatura?.requisitoEspecial == null) NotaVacia("Sin prelaciones: puedes inscribirla desde ya")
                pide.forEachIndexed { i, a -> FilaDeRelacion(a, estado.situaciones, notaDePide(a), i == pide.lastIndex, alIrA) }
                asignatura?.requisitoEspecial?.let { NotaVacia("Además pide $it") }
                SeccionDeRelacion("Abre", if (abre.isEmpty()) "" else if (tras > 0) "${abre.size} y $tras tras ella" else "${abre.size}")
                if (abre.isEmpty()) NotaVacia("No abre ninguna otra materia")
                abre.forEachIndexed { i, a -> FilaDeRelacion(a, estado.situaciones, notaDeAbre(a, codigo, estado.estados, estado.pensum.porCodigo), i == abre.lastIndex, alIrA) }
                Spacer(Modifier.height(ALTO_MUELLE + abajo + 8.dp))
            }
            MuelleDePie(materia.situacion, plan, abajo, alMarcar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

// ─────────────────────────────────── Cabeza ──────────────────────────────────

/** «SEMESTRE 5 · 0713463»: dónde va y su código, en Plex */
@Composable
private fun Rotulo(m: MateriaLista) {
    val texto = listOfNotNull(m.semestre?.let { "SEMESTRE $it" } ?: "ELECTIVA", m.codigo).joinToString(" · ")
    BasicText(texto, style = estiloPlex().copy(letterSpacing = 0.03.em))
}

@Composable
private fun Titulo(nombre: String, tam: androidx.compose.ui.unit.TextUnit, alto: androidx.compose.ui.unit.TextUnit, modifier: Modifier) {
    BasicText(
        nombre, modifier,
        style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = tam, lineHeight = alto, letterSpacing = (-0.012).em, color = Paleta.tinta),
    )
}

/** La línea de estado con su icono; acostada suma el margen a su lado */
@Composable
private fun Estado(m: MateriaLista, margen: MargenMateria?, icono: Dp, conMargen: Boolean) {
    val texto = when (m.situacion) {
        Situacion.INSCRIBIBLE -> "Puedes inscribirla"
        Situacion.CURSANDO -> "La estás cursando"
        Situacion.HECHA -> "La aprobaste"
        Situacion.PROXIMA -> "Se abre al aprobar lo que cursas"
        Situacion.LEJANA -> "Bloqueada"
    }
    val extra = if (conMargen && margen != null && m.situacion == Situacion.INSCRIBIBLE) (if (margen.margen == 0) " · **sin margen**" else " · **${margen.margen}** de margen") else ""
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconoDeEstado(m.situacion, icono)
        BasicText(
            conNegritas(texto + extra),
            style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp, color = Paleta.suave),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** «‹ Por hacer» y, a la derecha, el botón que lleva al mapa */
@Composable
private fun BarraDePagina(etiqueta: String, alVolver: () -> Unit, alVerEnMapa: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = alVolver).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoLucide(Lucide.atras, Paleta.tinta, 24.dp)
            BasicText(etiqueta, Modifier.padding(start = 2.dp), style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Paleta.tinta))
        }
        BotonRedondo(alVerEnMapa, "Ver en el mapa") { IconoLucide(Lucide.mapa3, Paleta.tinta, 19.dp) }
    }
}

// ─────────────────────────────────── Datos ──────────────────────────────────

/** Tres datos sueltos entre dos hilos: UC, semestres de margen y área; la cifra en Jost y la etiqueta en mayúsculas finas */
@Composable
private fun DatosDeMateria(m: MateriaLista, margen: MargenMateria?, asignatura: Asignatura?, conAreas: Boolean) {
    val area = if (conAreas) nombreArea(asignatura?.area) else null
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp).drawBehind {
            drawLine(RAYA, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1f)
            drawLine(RAYA, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
        }.padding(top = 16.dp, bottom = 14.dp),
    ) {
        Dato("${m.uc ?: "—"}", null, "UC", Modifier.weight(0.7f), primero = true)
        Dato(
            if (margen == null) "—" else if (margen.sinLimite) "∞" else "${margen.margen}",
            if (margen != null && !margen.sinLimite) "sem." else null,
            "De margen", Modifier.weight(1.15f),
        )
        if (area != null) Dato(area, null, "Área", Modifier.weight(1.3f), cifra = false)
        else Dato(m.semestre?.toString() ?: "—", null, "Semestre", Modifier.weight(1.3f))
    }
}

@Composable
private fun Dato(valor: String, unidad: String?, etiqueta: String, modifier: Modifier, primero: Boolean = false, cifra: Boolean = true) {
    Column(
        modifier.drawBehind { if (!primero) drawLine(RAYA, Offset(0.5f, 0f), Offset(0.5f, size.height), 1f) }
            .padding(start = if (primero) 0.dp else 16.dp),
    ) {
        val tam = if (cifra) 30.sp else 21.sp
        val texto: AnnotatedString = buildAnnotatedString {
            append(valor)
            if (unidad != null) withStyle(SpanStyle(fontFamily = inter(), fontSize = 13.sp, color = Paleta.tenue)) { append(" $unidad") }
        }
        Box(Modifier.height(34.dp), contentAlignment = Alignment.CenterStart) {
            BasicText(
                texto,
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = tam, lineHeight = tam, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        BasicText(
            etiqueta.uppercase(), Modifier.padding(top = 6.dp),
            style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.12.em, color = Paleta.tenue),
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────── Pide y abre ──────────────────────────────────

private fun palabraDeEstado(s: Situacion?) = when (s) {
    Situacion.HECHA -> "aprobada"
    Situacion.CURSANDO -> "cursando"
    Situacion.INSCRIBIBLE -> "puedes inscribirla"
    Situacion.PROXIMA, Situacion.LEJANA -> "bloqueada"
    null -> ""
}

/** Lo que dice una materia que se pide: su semestre y cómo está */
private fun notaDePide(a: Asignatura): (Situacion?) -> String = { s -> listOfNotNull(a.semestre?.let { "Semestre $it" }, palabraDeEstado(s).ifEmpty { null }).joinToString(" · ") }

/** Lo que dice una materia que se abre: su semestre y si solo espera a esta */
private fun notaDeAbre(a: Asignatura, deCodigo: String, estados: Map<String, EstadoMateria>, todas: Map<String, Asignatura>): (Situacion?) -> String = { _ ->
    val otras = a.prerrequisitos.filter { it != deCodigo && estados[it] != EstadoMateria.APROBADA }
    val resto = when {
        estados[a.codigo] == EstadoMateria.APROBADA -> "aprobada"
        estados[a.codigo] == EstadoMateria.CURSANDO -> "cursando"
        otras.isEmpty() -> "solo espera a esta"
        else -> "también pide ${todas[otras[0]]?.nombre ?: otras[0]}"
    }
    listOfNotNull(a.semestre?.let { "Semestre $it" }, resto).joinToString(" · ")
}

@Composable
private fun SeccionDeRelacion(titulo: String, meta: String, modifier: Modifier = Modifier) {
    EncabezadoDeSeccion(titulo, meta, accion = null, alAccion = {}, plegable = false, abierto = false, alPlegar = {}, modifier = modifier)
}

@Composable
private fun NotaVacia(texto: String) {
    BasicText(texto, Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp), style = estiloRazon())
}

/** Una materia enlazada: icono, nombre, cómo está y un chevron; tocarla abre su página */
@Composable
private fun FilaDeRelacion(a: Asignatura, situaciones: Map<String, Situacion>, nota: (Situacion?) -> String, ultima: Boolean, alIrA: (String) -> Unit) {
    val s = situaciones[a.codigo]
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "${a.nombre}, ${palabraDeEstado(s)}. Abrir su página" }
            .clickable(role = Role.Button) { alIrA(a.codigo) }
            .heightIn(min = ALTO_FILA)
            .drawBehind { if (!ultima) drawLine(RAYA, Offset(58.dp.toPx(), size.height - 0.5f), Offset(size.width - 20.dp.toPx(), size.height - 0.5f), 1f) }
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(44.dp).heightIn(min = ALTO_FILA), contentAlignment = Alignment.Center) { IconoDeEstado(s) }
        Column(Modifier.weight(1f).padding(start = 6.dp, top = 9.dp, bottom = 9.dp)) {
            BasicText(a.nombre, style = estiloNombre(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(nota(s), Modifier.padding(top = 1.dp), style = estiloRazon(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.padding(end = 18.dp)) { IconoLucide(Lucide.siguiente, Paleta.apagada, 18.dp) }
    }
}

/** Acostada, lo que pide o abre en una columna: icono y nombre, cada uno enlazado */
@Composable
private fun ColumnaRelacion(rotulo: String, materias: List<Asignatura>, situaciones: Map<String, Situacion>, modifier: Modifier, alIrA: (String) -> Unit) {
    Column(modifier) {
        BasicText(rotulo, Modifier.padding(bottom = 4.dp), style = estiloSeccion().copy(fontSize = 10.5.sp, color = Paleta.tenue))
        if (materias.isEmpty()) BasicText("Ninguna", Modifier.padding(vertical = 8.dp), style = estiloRazon())
        materias.forEach { a ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                    .semantics { contentDescription = "${a.nombre}, ${palabraDeEstado(situaciones[a.codigo])}. Abrir su página" }
                    .clickable(role = Role.Button) { alIrA(a.codigo) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconoDeEstado(situaciones[a.codigo], 15.dp)
                BasicText(a.nombre, style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, color = Paleta.tinta), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
