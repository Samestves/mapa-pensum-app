package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Creditos
import mpensum.datos.Grupo
import mpensum.datos.Pensum
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Cuánto llevas de un grupo de electivas */
data class AvanceGrupo(
    val clave: String,
    val titulo: String,
    val elegidas: List<Asignatura>,
    val uc: Int,
    val meta: Int?,
    val completa: Boolean,
    /** Lo que pasa de la cuota: no suma para el título, pero se muestra */
    val excedente: Int,
)

fun avanceDeGrupos(grupos: List<Grupo>, marcas: Map<String, Marca>): Map<String, AvanceGrupo> =
    grupos.associate { g ->
        val elegidas = g.asignaturas.filter { marcas[it.codigo] == Marca.APROBADA }
        val uc = elegidas.sumOf { it.uc ?: 0 }
        g.clave to AvanceGrupo(
            clave = g.clave,
            titulo = g.titulo,
            elegidas = elegidas,
            uc = uc,
            meta = g.cuota,
            completa = g.cuota != null && uc >= g.cuota,
            excedente = if (g.cuota != null) max(0, uc - g.cuota) else 0,
        )
    }

/** El avance de una carrera, contado sobre las obligatorias más lo que cabe de electivas */
data class Progreso(
    val ucAprobadas: Int,
    val ucTotales: Int,
    /** UC del título; null donde el pensum no trae créditos oficiales */
    val ucTitulo: Int?,
    val ucElectivas: Int,
    /** De 0 a 100 sobre las UC del título; null sin créditos oficiales: no se inventa un porcentaje */
    val porcentaje: Double?,
    val porcentajeObligatorias: Double,
    val aprobadas: Int,
    val cursando: Int,
    val disponibles: Int,
    /** Las que puedes inscribir, de la más atrasada a la más adelantada */
    val paraInscribir: List<Asignatura>,
    val bloqueadas: Int,
    val total: Int,
) {
    /** El avance hacia el título: por UC si hay créditos oficiales, por materias si no */
    val avance: Double get() = porcentaje ?: if (total > 0) aprobadas * 100.0 / total else 0.0
}

fun progresoDe(
    asignaturas: List<Asignatura>,
    estados: Map<String, EstadoMateria>,
    creditos: Creditos?,
    avanceGrupos: Map<String, AvanceGrupo>,
): Progreso {
    var ucAprobadas = 0
    var aprobadas = 0
    var cursando = 0
    val paraInscribir = mutableListOf<Asignatura>()
    for (a in asignaturas) {
        when (estados[a.codigo]) {
            EstadoMateria.APROBADA -> {
                ucAprobadas += a.uc ?: 0
                aprobadas += 1
            }
            EstadoMateria.CURSANDO -> cursando += 1
            EstadoMateria.DISPONIBLE -> paraInscribir += a
            else -> Unit
        }
    }
    val ucTotales = asignaturas.sumOf { it.uc ?: 0 }
    // Solo cuentan las UC electivas que caben en su cuota
    val ucElectivas = avanceGrupos.values.sumOf { g -> if (g.meta != null) min(g.uc, g.meta) else 0 }
    val ucTitulo = creditos?.titulo
    return Progreso(
        ucAprobadas = ucAprobadas,
        ucTotales = ucTotales,
        ucTitulo = ucTitulo,
        ucElectivas = ucElectivas,
        porcentaje = ucTitulo?.takeIf { it > 0 }?.let { (ucAprobadas + ucElectivas) * 100.0 / it },
        porcentajeObligatorias = if (ucTotales > 0) ucAprobadas * 100.0 / ucTotales else 0.0,
        aprobadas = aprobadas,
        cursando = cursando,
        disponibles = paraInscribir.size,
        paraInscribir = paraInscribir.sortedWith(compareBy<Asignatura> { it.semestre ?: 99 }.then { a, b -> compararEs(a.nombre, b.nombre) }),
        bloqueadas = asignaturas.size - aprobadas - cursando - paraInscribir.size,
        total = asignaturas.size,
    )
}

/** Cuántas obligatorias aprobaste en cada semestre, en orden */
fun aprobadasPorSemestre(pensum: Pensum, marcas: Map<String, Marca>): List<Int> {
    val obligatorias = pensum.obligatorias
    return pensum.semestres.map { s -> obligatorias.count { it.semestre == s.numero && marcas[it.codigo] == Marca.APROBADA } }
}

/** Lo que la portada enseña de una carrera: su silueta encendida y cuánto llevas */
data class ResumenPortada(
    val hechas: List<Int>,
    val porcentaje: Int,
    val llevas: Int,
    val de: Int,
    /** "UC" si hay créditos oficiales, "materias" si no */
    val unidad: String,
    val disponibles: Int,
    /** Las que marcaste como cursando */
    val cursando: Int = 0,
) {
    /** El semestre por el que vas: el primero que te queda a medias; null si ya los terminaste todos */
    fun semestreActual(silueta: List<Int>): Int? =
        silueta.indices.firstOrNull { (hechas.getOrNull(it) ?: 0) < silueta[it] }?.plus(1)
}

fun resumenParaPortada(pensum: Pensum, marcasGuardadas: Map<String, String>?): ResumenPortada {
    val marcas = depurarMarcas(marcasGuardadas, pensum.todas.map { it.codigo }.toSet())
    val progreso = progresoDe(pensum.obligatorias, estadosDe(pensum.todas, marcas), pensum.creditos, avanceDeGrupos(pensum.grupos, marcas))
    val conUc = progreso.porcentaje != null
    return ResumenPortada(
        hechas = aprobadasPorSemestre(pensum, marcas),
        porcentaje = redondear(progreso.avance),
        llevas = if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas,
        de = if (conUc) progreso.ucTitulo!! else progreso.total,
        unidad = if (conUc) "UC" else "materias",
        disponibles = progreso.disponibles,
        cursando = progreso.cursando,
    )
}

/** Math.round de JS: las mitades suben siempre, también en negativo */
internal fun redondear(x: Double): Int = kotlin.math.floor(x + 0.5).roundToInt()

/**
 * Orden alfabético en español sin depender de la plataforma: sin tildes ni
 * mayúsculas para decidir, la ñ después de la n, y en empate el texto tal cual.
 * Basta para ordenar nombres de materias como lo hace localeCompare('es').
 */
internal fun compararEs(a: String, b: String): Int {
    val porLetra = claveEs(a).compareTo(claveEs(b))
    return if (porLetra != 0) porLetra else a.compareTo(b)
}

private fun claveEs(texto: String): String = buildString {
    for (c in texto.lowercase()) {
        when (c) {
            'á', 'à', 'ä', 'â' -> append('a')
            'é', 'è', 'ë', 'ê' -> append('e')
            'í', 'ì', 'ï', 'î' -> append('i')
            'ó', 'ò', 'ö', 'ô' -> append('o')
            'ú', 'ù', 'ü', 'û' -> append('u')
            'ñ' -> append("n~")
            else -> append(c)
        }
    }
}
