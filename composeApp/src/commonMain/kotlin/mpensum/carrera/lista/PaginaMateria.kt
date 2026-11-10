package mpensum.carrera.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.carrera.IconoLucide
import mpensum.carrera.nombreArea
import mpensum.componentes.Lucide
import mpensum.datos.Asignatura
import mpensum.logica.EstadoMateria
import mpensum.logica.Marca
import mpensum.logica.MateriaLista
import mpensum.logica.Situacion
import mpensum.logica.cuantasCaben
import mpensum.logica.planMuelle
import mpensum.logica.repartirRelaciones
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * La página de una materia, sin scroll: lo que hace falta para decidir cabe
 * en la pantalla, con el muelle fijo abajo. De pie se empuja a pantalla
 * completa, como una página de iOS; acostada es el panel de la derecha.
 *
 * Arriba, semestre y código en pequeño, el nombre, el estado y dos pastillas
 * con las UC y el área; debajo, lo que pide y lo que abre, cada materia
 * enlazada a su página. Si no caben todas, cada lado enseña las que quepan y
 * su rótulo dice «+N más», que abre una subpágina con la lista entera: la
 * página nunca se desplaza, pero nada se pierde.
 */

/** Una materia de «Pide» o «Abre»: una línea, con la pastilla y el chevron */
private val ALTO_RELACION = 44.dp

/** El rótulo de cada sección de la página: el de la lista, que también es la zona de toque de «+N más» */
private val ALTO_ROTULO_RELACION = ALTO_ENCABEZADO

/** Lo que separa «Pide» de «Abre»; y, con sitio de sobra, el hueco y la fila más generosos */
private val HUECO_SECCIONES = 4.dp
private val HUECO_HOLGADO = 12.dp
private val ALTO_RELACION_HOLGADA = 52.dp

/** Acostada, el rótulo de cada columna, con su respiro */
private val ALTO_ROTULO_ACOSTADO = 32.dp

/** Lo que dice el rótulo de un lado sin nada: no hay nube vacía, solo esta palabra */
private const val NINGUNA = "Ninguna"

/** Una línea de «Pide» o «Abre»: una materia enlazada o una nota (el requisito especial) */
private class Linea(val materia: Asignatura?, val nota: String?)

/** Lo que dice cada lado, ya como líneas; el requisito especial de una materia cuenta como una línea más de «Pide» */
private fun lineasDe(materias: List<Asignatura>, especial: String?): List<Linea> =
    materias.map { Linea(it, null) } + listOfNotNull(especial?.let { Linea(null, "Además pide $it") })

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
    val navegacion = sesion.navegacion
    val asignatura = estado.pensum.porCodigo[codigo]
    val plan = planMuelle(materia, sesion.ucCursando)
    val alMarcar: (Marca?) -> Unit = { sesion.marcar(mapOf(codigo to it)) }
    val relaciones = estado.layout.relaciones
    val pide = lineasDe(relaciones.atras[codigo].orEmpty().mapNotNull { estado.pensum.porCodigo[it] }, asignatura?.requisitoEspecial)
    val abre = lineasDe(relaciones.adelante[codigo].orEmpty().mapNotNull { estado.pensum.porCodigo[it] }, null)
    val area = if (estado.pensum.tieneAreas) nombreArea(asignatura?.area) else null
    val verEnMapa = { alVerEnMapa(codigo) }
    val situaciones = estado.situaciones

    // La subpágina de un lado, con todas sus materias; el rótulo dice de cuál
    val abierta = navegacion.relacion
    if (abierta != null) {
        SubpaginaDeRelacion(
            rotulo = abierta, materia = materia, lineas = if (abierta == ROTULO_PIDE) pide else abre,
            situaciones = situaciones, acostada = acostada, arriba = arriba, abajo = abajo,
            alVolver = { navegacion.relacion = null }, alIrA = alIrA, modifier = modifier,
        )
        return
    }
    val aprobadas = pide.count { it.materia != null && estado.estados[it.materia.codigo] == EstadoMateria.APROBADA }
    val metaPide = if (pide.isEmpty()) NINGUNA else "$aprobadas de ${pide.count { it.materia != null }}"
    val metaAbre = if (abre.isEmpty()) NINGUNA else "${abre.size}"
    val masPide = { navegacion.relacion = ROTULO_PIDE }
    val masAbre = { navegacion.relacion = ROTULO_ABRE }

    if (acostada) {
        Column(modifier.fillMaxSize().padding(start = 22.dp, end = 12.dp, top = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    Rotulo(materia)
                    Titulo(materia.nombre, 28.sp, Modifier.padding(top = 4.dp, bottom = 4.dp))
                    Estado(materia, 16.dp)
                    // Acostada no hay sitio para las pastillas: las UC y el área van en una línea tenue
                    BasicText(
                        listOfNotNull(materia.uc?.let { "$it UC" }, area).joinToString(" · "),
                        Modifier.padding(top = 2.dp), style = estiloRazon(), maxLines = 1,
                    )
                }
                BotonRedondo(verEnMapa, "Ver en el mapa") { IconoLucide(Lucide.mapa3, Paleta.tinta, 18.dp) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, end = 10.dp).clipToBounds()) {
                val cabe = ((maxHeight - ALTO_ROTULO_ACOSTADO) / ALTO_RELACION).toInt().coerceAtLeast(1)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ColumnaRelacion(ROTULO_PIDE, metaPide, pide, cuantasCaben(pide.size, cabe), situaciones, Modifier.weight(1f), alIrA, masPide)
                    ColumnaRelacion(ROTULO_ABRE, metaAbre, abre, cuantasCaben(abre.size, cabe), situaciones, Modifier.weight(1f), alIrA, masAbre)
                }
            }
            MuelleAcostado(materia.situacion, plan, alMarcar, Modifier.padding(end = 10.dp, top = 6.dp, bottom = 16.dp))
        }
        return
    }

    Column(modifier.fillMaxSize().padding(top = arriba)) {
        BarraDePagina(etiquetaVolver, alVolver, verEnMapa)
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
            Rotulo(materia)
            Titulo(materia.nombre, 32.sp, Modifier.padding(top = 6.dp, bottom = 6.dp))
            Estado(materia, 18.dp)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                materia.uc?.let { Pastilla("$it UC") }
                area?.let { Pastilla(it) }
            }
        }
        // Lo que pide y lo que abre se reparten el alto que queda entre la cabeza y el muelle
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = Espacio.xs).clipToBounds()) {
            val filas = ((maxHeight - ALTO_ROTULO_RELACION * 2 - HUECO_SECCIONES) / ALTO_RELACION).toInt().coerceAtLeast(0)
            val nPide = pide.size
            val nAbre = abre.size
            val (vePide, veAbre) = remember(nPide, nAbre, filas) { repartirRelaciones(nPide, nAbre, filas) }
            // Con sitio de sobra todo se ve y las filas respiran un poco más; si no, las de 44 dp de siempre
            val holgada = vePide == nPide && veAbre == nAbre &&
                maxHeight >= ALTO_ROTULO_RELACION * 2 + HUECO_HOLGADO + ALTO_RELACION_HOLGADA * (nPide + nAbre) + 24.dp
            val alto = if (holgada) ALTO_RELACION_HOLGADA else ALTO_RELACION
            Column(Modifier.fillMaxWidth()) {
                RotuloDeRelacion(ROTULO_PIDE, metaPide, nPide - vePide, masPide)
                NubeDeRelacion(pide, vePide, situaciones, alto, alIrA)
                Spacer(Modifier.height(if (holgada) HUECO_HOLGADO else HUECO_SECCIONES))
                RotuloDeRelacion(ROTULO_ABRE, metaAbre, nAbre - veAbre, masAbre)
                NubeDeRelacion(abre, veAbre, situaciones, alto, alIrA)
            }
        }
        MuelleDePie(materia.situacion, plan, abajo, alMarcar)
    }
}

// ─────────────────────────────────── Cabeza ──────────────────────────────────

/** «SEMESTRE 5 · 0713463»: dónde va y su código, discretos en Plex */
@Composable
private fun Rotulo(m: MateriaLista) {
    val texto = listOfNotNull(m.semestre?.let { "SEMESTRE $it" } ?: "ELECTIVA", m.codigo).joinToString(" · ")
    BasicText(texto, style = estiloPlex().copy(letterSpacing = 0.03.em))
}

/** El nombre en Jost Light; los largos bajan de tamaño antes de pasar de dos líneas */
@Composable
private fun Titulo(nombre: String, tam: TextUnit, modifier: Modifier) {
    BasicText(
        nombre, modifier,
        style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = tam, lineHeight = tam * 1.18f, letterSpacing = (-0.012).em, color = Paleta.tinta),
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased((tam.value * 0.72f).sp, tam, 1.sp),
    )
}

/** La línea de estado con su icono */
@Composable
private fun Estado(m: MateriaLista, icono: Dp) {
    val texto = when (m.situacion) {
        Situacion.INSCRIBIBLE -> "Puedes inscribirla"
        Situacion.CURSANDO -> "La estás cursando"
        Situacion.HECHA -> "La aprobaste"
        Situacion.PROXIMA -> "Se abre al aprobar lo que cursas"
        Situacion.LEJANA -> "Bloqueada"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconoDeEstado(m.situacion, icono)
        BasicText(
            texto,
            style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp, color = Paleta.suave),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Un dato corto (las UC, el área) en una pastilla suave, la misma de los botones de la cabecera */
@Composable
private fun Pastilla(texto: String) {
    BasicText(
        texto,
        Modifier.background(BOTON, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, color = Paleta.suave),
        maxLines = 1,
    )
}

/** «‹ Por hacer» y, si hay mapa al que ir, el botón que lleva a él, alineado con el borde de las nubes */
@Composable
private fun BarraDePagina(etiqueta: String, alVolver: () -> Unit, alVerEnMapa: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = alVolver).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoLucide(Lucide.atras, Paleta.tinta, 24.dp)
            BasicText(etiqueta, Modifier.padding(start = 2.dp), style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Paleta.tinta), maxLines = 1)
        }
        if (alVerEnMapa != null) BotonRedondo(alVerEnMapa, "Ver en el mapa") { IconoLucide(Lucide.mapa3, Paleta.tinta, 19.dp) }
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

/** El rótulo de «Pide» o «Abre»; si quedan materias fuera, en vez de la cuenta dice cuántas y abre la lista entera */
@Composable
private fun RotuloDeRelacion(titulo: String, meta: String, faltan: Int, alMas: () -> Unit) {
    EncabezadoDeSeccion(
        titulo, meta, plegable = false, abierto = false, alPlegar = {}, alto = ALTO_ROTULO_RELACION,
        accion = if (faltan > 0) "+$faltan más" else null, alAccion = alMas,
    )
}

/** Un lado en su nube: las `visibles` primeras líneas; sin ninguna, no hay nube */
@Composable
private fun NubeDeRelacion(lineas: List<Linea>, visibles: Int, situaciones: Map<String, Situacion>, alto: Dp, alIrA: (String) -> Unit) {
    val ve = visibles.coerceIn(0, lineas.size)
    lineas.take(ve).forEachIndexed { i, l -> LineaEnNube(l, situaciones, i == 0, i == ve - 1, alIrA, alto) }
}

/** Una línea en su trozo de nube: la materia enlazada o su nota */
@Composable
private fun LineaEnNube(l: Linea, situaciones: Map<String, Situacion>, primera: Boolean, ultima: Boolean, alIrA: (String) -> Unit, alto: Dp = ALTO_RELACION) {
    val a = l.materia
    if (a != null) FilaDeRelacion(a, situaciones[a.codigo], primera, ultima, alIrA, alto)
    else TrozoDeNube(primera, ultima, Espacio.m, null, null, alto) {
        BasicText(l.nota.orEmpty(), Modifier.padding(vertical = 12.dp), style = estiloRazon(), maxLines = 1)
    }
}

/**
 * Un trozo de nube a lo ancho de la página: su fondo, el hilo que lo separa del
 * de arriba (desde `hilo`) y, si lleva `alTocar`, el toque con su descripción.
 */
@Composable
private fun TrozoDeNube(primera: Boolean, ultima: Boolean, hilo: Dp, descripcion: String?, alTocar: (() -> Unit)?, alto: Dp, contenido: @Composable RowScope.() -> Unit) {
    val forma = remember(primera, ultima) { formaDeTrozo(primera, ultima) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MARGEN_TARJETA).clip(forma).background(FONDO_TARJETA)
            .then(if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier)
            .then(if (alTocar != null) Modifier.clickable(role = Role.Button, onClick = alTocar) else Modifier)
            .heightIn(min = alto)
            .drawBehind { if (!primera) drawLine(HILO_TARJETA, Offset(hilo.toPx(), 0.5f), Offset(size.width, 0.5f), 1f) }
            .padding(start = Espacio.m, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido,
    )
}

/** Una materia enlazada en su trozo de nube: pastilla, nombre en una línea y chevron; tocarla abre su página */
@Composable
private fun FilaDeRelacion(a: Asignatura, s: Situacion?, primera: Boolean, ultima: Boolean, alIrA: (String) -> Unit, alto: Dp) {
    TrozoDeNube(primera, ultima, Espacio.m + 28.dp + Espacio.s, "${a.nombre}, ${palabraDeEstado(s)}. Abrir su página", { alIrA(a.codigo) }, alto) {
        PastillaDeEstado(s, lado = 28.dp)
        BasicText(
            a.nombre, Modifier.weight(1f).padding(start = Espacio.s, end = 8.dp),
            style = estiloNombre().copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(13.sp, 16.sp, 0.5.sp),
        )
        IconoLucide(Lucide.siguiente, Paleta.apagada, 18.dp)
    }
}

/** Acostada, lo que pide o abre en una columna: icono y nombre, cada uno enlazado; lo que no cabe se cuenta en «+N más» */
@Composable
private fun ColumnaRelacion(
    rotulo: String,
    meta: String,
    lineas: List<Linea>,
    visibles: Int,
    situaciones: Map<String, Situacion>,
    modifier: Modifier,
    alIrA: (String) -> Unit,
    alMas: () -> Unit,
) {
    val ve = visibles.coerceIn(0, lineas.size)
    val faltan = lineas.size - ve
    Column(modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min = ALTO_ROTULO_ACOSTADO), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(rotulo.uppercase(), style = estiloSeccion().copy(fontSize = 10.5.sp, color = Paleta.tenue))
            if (faltan == 0 && lineas.isEmpty()) BasicText(meta.uppercase(), style = estiloSeccion().copy(fontSize = 10.5.sp, color = Paleta.tenue))
            if (faltan > 0) Row(
                Modifier.heightIn(min = ALTO_ROTULO_ACOSTADO).clip(RoundedCornerShape(8.dp))
                    .semantics { contentDescription = "$faltan más. Ver todas" }
                    .clickable(role = Role.Button, onClick = alMas).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("+$faltan más", style = estiloAccion().copy(fontSize = 12.sp), maxLines = 1)
                IconoLucide(Lucide.siguiente, Paleta.suave, 14.dp)
            }
        }
        lineas.take(ve).forEach { l ->
            val a = l.materia
            if (a != null) Row(
                Modifier.fillMaxWidth().heightIn(min = ALTO_RELACION).clip(RoundedCornerShape(10.dp))
                    .semantics { contentDescription = "${a.nombre}, ${palabraDeEstado(situaciones[a.codigo])}. Abrir su página" }
                    .clickable(role = Role.Button) { alIrA(a.codigo) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconoDeEstado(situaciones[a.codigo], 15.dp)
                BasicText(a.nombre, style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, color = Paleta.tinta), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    // Los nombres largos bajan de letra antes de cortarse
                    autoSize = TextAutoSize.StepBased(10.sp, 14.sp, 0.5.sp))
            } else BasicText(l.nota.orEmpty(), Modifier.heightIn(min = ALTO_RELACION).padding(top = 12.dp), style = estiloRazon(), maxLines = 2)
        }
    }
}

// ─────────────────────────────────── Subpágina ──────────────────────────────────

/**
 * Todo lo que pide o abre una materia, en una lista que sí se desplaza si hace
 * falta (es la única que puede ser larga). Se llega con «+N más» y se vuelve
 * con la flecha o con Atrás.
 */
@Composable
private fun SubpaginaDeRelacion(
    rotulo: String,
    materia: MateriaLista,
    lineas: List<Linea>,
    situaciones: Map<String, Situacion>,
    acostada: Boolean,
    arriba: Dp,
    abajo: Dp,
    alVolver: () -> Unit,
    alIrA: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().padding(top = arriba, start = if (acostada) 10.dp else 0.dp)) {
        BarraDePagina("Atrás", alVolver, null)
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)) {
            BasicText(
                rotulo,
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = if (acostada) 26.sp else 32.sp, lineHeight = 36.sp, letterSpacing = (-0.012).em, color = Paleta.tinta),
            )
            BasicText(materia.nombre, Modifier.padding(top = 2.dp), style = estiloRazon(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = abajo + 16.dp)) {
            lineas.forEachIndexed { i, l -> LineaEnNube(l, situaciones, i == 0, i == lineas.lastIndex, alIrA) }
        }
    }
}
