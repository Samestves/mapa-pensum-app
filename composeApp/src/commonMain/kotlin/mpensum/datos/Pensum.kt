package mpensum.datos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import mpensum.recursos.Res

/**
 * El pensum completo de una carrera, tal como lo genera la web
 * (src/data/carreras/<slug>.json). Solo se modela lo que la app usa; el resto
 * del JSON (fuentes, avisos, correcciones) se ignora al leer.
 */
@Serializable
data class Pensum(
    val slug: String,
    val nombre: String,
    val nombreCorto: String,
    val color: ColorCarrera,
    val creditos: Creditos? = null,
    val semestres: List<Semestre>,
    val asignaturas: List<Asignatura>,
    val grupos: List<Grupo> = emptyList(),
    /** Si sus materias vienen clasificadas por área; si no, cada una lleva un tono de la carrera */
    val tieneAreas: Boolean = false,
    /** Si las casillas de los semestres son toda la exigencia de electivas (hoy solo Sistemas) */
    val electivasEnCasillas: Boolean = false,
) {
    /** Las materias de verdad: las casillas de electiva se dibujan, pero no se marcan ni cuentan */
    val obligatorias: List<Asignatura> by lazy { asignaturas.filter { !it.esHueco } }

    /** Obligatorias y electivas juntas: comparten las mismas marcas */
    val todas: List<Asignatura> by lazy { obligatorias + grupos.flatMap { it.asignaturas } }

    /** Cada materia, casilla y electiva por su código: el único índice, para no armarlo en cada pantalla */
    val porCodigo: Map<String, Asignatura> by lazy { (asignaturas + grupos.flatMap { it.asignaturas }).associateBy { it.codigo } }
}

@Serializable
data class Creditos(val titulo: Int? = null, val obligatorias: Int? = null)

@Serializable
data class Semestre(val numero: Int, val cantidad: Int, val uc: Int, val huecos: Int = 0)

@Serializable
data class Asignatura(
    val codigo: String,
    val nombre: String,
    val semestre: Int? = null,
    val uc: Int? = null,
    val area: String? = null,
    val prerrequisitos: List<String> = emptyList(),
    val profundidad: Int? = null,
    /** Casilla de electiva: el hueco donde va la que el estudiante elija */
    val esHueco: Boolean = false,
    /** Grupo de electivas al que pertenece la casilla */
    val grupo: String? = null,
    /** Materia que representa varias posibles (las "Electiva" sin detallar del documento) */
    val esComodin: Boolean = false,
    /** Requisito que no es otra materia (un mínimo de UC, por ejemplo), en palabras */
    val requisitoEspecial: String? = null,
) {
    /** Las UC aprobadas que pide el requisito especial («120 UC aprobadas»), leídas una sola vez; null si no pide */
    @Transient
    val ucMinimas: Int? = requisitoEspecial?.let { UC_MINIMAS.find(it)?.groupValues?.get(1)?.toIntOrNull() }
}

private val UC_MINIMAS = Regex("""(\d+)\s*UC""")

@Serializable
data class Grupo(
    val clave: String,
    val titulo: String,
    val tipo: String,
    val asignaturas: List<Asignatura>,
    /** UC que exige el título de este grupo; null si el documento no lo dice */
    val cuota: Int? = null,
)

private val lector = Json { ignoreUnknownKeys = true }

/** Lee un pensum del texto de su JSON */
fun leerPensum(texto: String): Pensum = lector.decodeFromString(texto)

/** El texto del JSON de una carrera, tal como viene en el paquete de la app */
suspend fun leerPensumCrudo(slug: String): String = Res.readBytes("files/carreras/$slug.json").decodeToString()

/** Lee y decodifica el pensum de una carrera del paquete, fuera del hilo principal (el JSON pesa 8-14 KB y decodificarlo no es gratis en gama baja) */
suspend fun leerPensumDelPaquete(slug: String, despachador: CoroutineDispatcher = Dispatchers.Default): Pensum =
    withContext(despachador) { leerPensum(leerPensumCrudo(slug)) }

/** El pensum de una carrera, leído del paquete de la app al abrirla. Null mientras llega */
@Composable
fun rememberPensum(slug: String): Pensum? {
    val estado = produceState<Pensum?>(null, slug) { value = leerPensumDelPaquete(slug) }
    return estado.value
}
