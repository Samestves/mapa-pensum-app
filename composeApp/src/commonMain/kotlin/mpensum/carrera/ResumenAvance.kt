package mpensum.carrera

import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import mpensum.datos.Pensum
import mpensum.logica.AvanceGrupo
import mpensum.logica.EstadoMateria
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Periodo
import mpensum.logica.SemestreCapsulas
import mpensum.logica.SiApruebas
import mpensum.logica.avanceDeGrupos
import mpensum.logica.capsulasDe
import mpensum.logica.cargaMasRapida
import mpensum.logica.cursandoDe
import mpensum.logica.siApruebasLoQueCursas
import mpensum.logica.ucTipicaDe
import mpensum.mapa.nombreCasilla

/**
 * Lo que dice «Tu avance», sin dibujar nada: las filas del índice y de la hoja, y las cuentas que las
 * llenan. Lógica pura (se prueba sin pantalla); los trazos de los iconos los pone la interfaz a partir
 * de [IconoFila].
 */

/** Los iconos de las filas, por su nombre: la interfaz los traduce a trazos (`IconoFila.trazos()`) */
internal enum class IconoFila { REINICIAR, LIBRO_ABIERTO, USUARIOS, LLAVE, ESLABON, CALENDARIO, RELOJ_ARENA, BIRRETE }

/** Una fila de la hoja: su pastilla, el título, la nota, la cifra (con su unidad) y la pantalla a la que lleva */
internal class Pregunta(
    val icono: IconoFila,
    val azul: Boolean,
    val titulo: String,
    val nota: String,
    val valor: String?,
    val unidad: String?,
    val detalle: DetalleAvance,
)

/** Las preguntas de la hoja: solo las que tienen algo que decir */
internal fun preguntasDe(g: EstimacionGraduacion, hoy: Periodo, cursando: Int, seAbren: Int, rapida: Periodo?, nombre: (String) -> String): List<Pregunta> = buildList {
    if (cursando > 0) {
        val abren = when (seAbren) {
            0 -> "No te abren otras"
            1 -> "Te abren 1"
            else -> "Te abren $seAbren"
        }
        add(Pregunta(IconoFila.LIBRO_ABIERTO, false, "Este semestre", abren, "$cursando", null, DetalleAvance.EsteSemestre))
    }
    g.plan.firstOrNull()?.let { add(Pregunta(IconoFila.CALENDARIO, true, "Tu próximo semestre", hoy.mas(1).rango(), "${it.size}", null, DetalleAvance.Proximo)) }
    g.cuelloDeBotella.firstOrNull()?.let { add(Pregunta(IconoFila.ESLABON, false, "No puedes atrasar", "Empieza en ${nombre(it)}", "${g.cuelloDeBotella.size}", null, DetalleAvance.Cadena)) }
    if (!g.terminado) add(Pregunta(IconoFila.BIRRETE, false, "¿Cuándo me gradúo?", rapida?.let {
        // Si tu fecha ya es la más rápida posible, decirlo en vez de repetirla
        if (it == g.periodoFinal) "Ya vas lo más rápido" else "Lo más rápido: ${it.mesYAnio()}"
    } ?: "A tu ritmo", null, null, DetalleAvance.Graduacion))
}

/**
 * Lo que enseña el índice de «Tu avance», de pie y acostado, calculado de una
 * vez por marcas, casillas, carga y período: la barra de cápsulas, lo que
 * cursas y lo que te da aprobarlo, las preguntas y las electivas. Es lógica
 * pura y se crea dentro de un derivedStateOf (`EstadoCarrera.resumen`), que
 * `precalentar` lee en segundo plano: al abrir la hoja ya está.
 */
internal class ResumenAvance(estado: EstadoCarrera) {
    val semestres: List<SemestreCapsulas> = capsulasDe(estado.pensum, estado.marcas, estado::enCasilla)
    val cursando: List<Asignatura> = cursandoDe(estado.pensum, estado.marcas)
    val si: SiApruebas? = if (cursando.isNotEmpty()) siApruebasLoQueCursas(estado.pensum, estado.marcas) else null
    val preguntas: List<Pregunta> = preguntasDe(
        estado.graduacion, estado.hoy, cursando.size, si?.seAbren?.size ?: 0,
        estado.planes[cargaMasRapida(estado.planes) - 1].periodoFinal, nombresDelPlan(estado.pensum),
    )
    val electivas: List<Pregunta> = electivasDe(estado)
}

/** Las electivas con cuota: una fila por grupo, con lo que llevas de lo que pide el título */
private fun electivasDe(estado: EstadoCarrera): List<Pregunta> =
    avanceDeGrupos(estado.pensum.grupos, estado.marcas).values.filter { it.meta != null }.map { c ->
        val grupo = estado.pensum.grupos.first { it.clave == c.clave }
        Pregunta(iconoDeGrupo(grupo), false, nombreCorto(c.titulo), subtituloDeGrupo(estado, grupo, c), "${c.uc}", "/${c.meta} UC", DetalleAvance.Electivas(c.clave))
    }

/** El icono de un grupo de electivas: la llave para las técnicas, las personas para las sociohumanísticas y el libro para las demás */
internal fun iconoDeGrupo(g: Grupo): IconoFila = when {
    g.clave == "tecnica" || g.titulo.contains("Técnica", ignoreCase = true) -> IconoFila.LLAVE
    g.clave == "humanistica" || g.titulo.contains("Sociohuman", ignoreCase = true) -> IconoFila.USUARIOS
    else -> IconoFila.LIBRO_ABIERTO
}

/** «Electivas Técnicas» → «Técnicas» */
internal fun nombreCorto(titulo: String): String = titulo.removePrefix("Electivas ").replaceFirstChar { it.uppercase() }

/** Lo útil de un grupo: si ya cubres su cuota, cuántas puedes cursar ya o desde qué semestre se abren */
internal fun subtituloDeGrupo(estado: EstadoCarrera, grupo: Grupo, avance: AvanceGrupo): String {
    if (avance.completa) return "Ya cubres las ${avance.meta} UC"
    val abiertas = grupo.asignaturas.count { estado.estados[it.codigo] == EstadoMateria.DISPONIBLE }
    if (abiertas > 0) return "Ya puedes cursar $abiertas"
    val desde = estado.pensum.asignaturas.filter { it.esHueco && it.grupo == grupo.clave }.mapNotNull { it.semestre }.minOrNull()
    return if (desde != null) "Desde el semestre $desde" else "Aún no se abre ninguna"
}

/** Cómo se llama cada código del plan: la materia, la casilla, o "Electiva técnica" para lo que pide la cuota de un grupo */
internal fun nombresDelPlan(pensum: Pensum): (String) -> String {
    val grupos = pensum.grupos.associateBy { it.clave }
    return { codigo ->
        pensum.porCodigo[codigo]?.nombre
            ?: grupos[codigo.substringBefore('#')]?.let(::nombreCasilla)
            ?: codigo
    }
}

/** Las UC de cada código del plan: las de la materia o, en una casilla sin elegir, las típicas de su grupo */
internal fun ucsDelPlan(pensum: Pensum): (String) -> Int {
    val tipicas = pensum.grupos.associate { it.clave to ucTipicaDe(it) }
    return { codigo -> pensum.porCodigo[codigo]?.uc ?: tipicas[pensum.porCodigo[codigo]?.grupo ?: codigo.substringBefore('#')] ?: 0 }
}

/** Si un código del plan es una casilla de electiva sin elegir */
internal fun esCasillaDelPlan(pensum: Pensum): (String) -> Boolean {
    val casillas = pensum.asignaturas.filter { it.esHueco }.map { it.codigo }.toSet()
    return { codigo -> codigo in casillas || '#' in codigo }
}
