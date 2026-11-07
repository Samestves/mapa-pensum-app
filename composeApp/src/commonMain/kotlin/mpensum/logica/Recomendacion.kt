package mpensum.logica

import androidx.compose.runtime.mutableStateMapOf
import mpensum.datos.Pensum

/*
 * Por qué cada materia del próximo semestre y qué pasa si atrasas la cadena.
 * No inventa otro plan: explica el de estimarGraduacion. Lo nuevo es el
 * margen, los semestres que una materia puede esperar sin mover tu fecha, y
 * se mide simulando con el veto (noAntesDe), no con una fórmula, porque el
 * tope de carga hace optimista a cualquier aproximación.
 */

enum class Motivo { FIJA_FECHA, ABRE_CAMINO, COMPLETA_CARGA, OPCIONAL }

/** Una materia del próximo semestre con su porqué */
data class Recomendacion(
    val codigo: String,
    val motivo: Motivo,
    /** Semestres que puede esperar sin mover tu fecha (0: ninguno) */
    val margen: Int,
    /** Los códigos que te abre para el semestre siguiente (como abreElProximo); vacío en casillas */
    val abre: List<String>,
)

data class ProximoSemestre(
    val periodo: Periodo,
    /** Las del primer período del plan: primero las que fijan la fecha, luego las que abren camino, luego las que completan la carga */
    val recomendadas: List<Recomendacion>,
    /** Las que podrías inscribir y el plan deja para después; su margen se cuenta desde el próximo período */
    val opcionales: List<Recomendacion>,
    /** La de más margen (≥ 1) que, quitada del próximo período, deja la misma fecha; confirmado simulando */
    val dejar: String?,
    /** Tu fecha con una materia menos por semestre; null si ya metes una */
    val conUnaMenos: Periodo?,
)

/** Lo más que se espera una materia al medir su margen: pasado esto no se sigue simulando */
private const val TOPE_MARGEN = 8

/**
 * Un plan base y las simulaciones con veto sobre él, con la misma carga y el
 * mismo `hoy`. Se comparte entre «Tu próximo semestre» y «No puedes atrasar»:
 * el margen de cada materia se mide una vez y el que ya se midió no se repite.
 * `planes` y `base` pueden venir ya calculados (EstadoCarrera los comparte con
 * la hoja y con el mando de carga); si no, se calculan aquí.
 */
class Simulador(
    val pensum: Pensum,
    val marcas: Map<String, Marca>,
    val casillas: Map<String, String>,
    carga: Int?,
    val hoy: Periodo,
    planes: List<EstimacionGraduacion>? = null,
    base: EstimacionGraduacion? = null,
) {
    val planes: List<EstimacionGraduacion> by lazy { planes ?: planesPorCarga(pensum, marcas, casillas, hoy) }
    val base: EstimacionGraduacion = base ?: estimarGraduacion(pensum, marcas, casillas, carga, hoy)

    /** Los márgenes ya medidos; un mapa de estado porque dos pantallas pueden pedirlos desde hilos distintos */
    private val medidos = mutableStateMapOf<String, Int>()

    fun conVeto(codigo: String, periodo: Int) =
        estimarGraduacion(pensum, marcas, casillas, base.carga, hoy, mapOf(codigo to periodo))

    fun indiceEnPlan(codigo: String) = base.plan.indexOfFirst { codigo in it }.coerceAtLeast(0)

    /** Cuántos períodos más allá de donde el plan la pone puede esperar `codigo` sin mover la fecha */
    fun margen(codigo: String): Int = medidos.getOrPut(codigo) {
        val desde = indiceEnPlan(codigo)
        (1..TOPE_MARGEN).firstOrNull { j -> conVeto(codigo, desde + j).periodoFinal != base.periodoFinal }?.minus(1) ?: TOPE_MARGEN
    }
}

fun proximoSemestre(
    pensum: Pensum,
    marcas: Map<String, Marca>,
    casillas: Map<String, String>,
    carga: Int?,
    hoy: Periodo,
    sim: Simulador = Simulador(pensum, marcas, casillas, carga, hoy),
): ProximoSemestre {
    val base = sim.base
    val obligatorias = pensum.obligatorias.mapTo(mutableSetOf()) { it.codigo }
    val ucDe = pensum.todas.associate { it.codigo to (it.uc ?: 0) }

    fun abreDe(codigo: String) =
        if (codigo in obligatorias) abreElProximo(pensum, marcas, codigo).map { it.codigo } else emptyList()

    val primero = base.plan.firstOrNull().orEmpty()
    val delPlan = primero.map { codigo ->
        val margen = sim.margen(codigo)
        val abre = abreDe(codigo)
        val motivo = when {
            margen == 0 -> Motivo.FIJA_FECHA
            abre.isNotEmpty() -> Motivo.ABRE_CAMINO
            else -> Motivo.COMPLETA_CARGA
        }
        Recomendacion(codigo, motivo, margen, abre)
    }
    val recomendadas = delPlan.sortedWith(
        compareBy<Recomendacion> { it.motivo.ordinal }
            .thenBy { it.margen }
            .thenByDescending { it.abre.size }
            .thenByDescending { ucDe[it.codigo] ?: 0 },
    )

    val opcionales = inscribiblesDelProximo(pensum, marcas)
        .filter { it.codigo !in primero }
        .map { Recomendacion(it.codigo, Motivo.OPCIONAL, sim.indiceEnPlan(it.codigo) + sim.margen(it.codigo), abreDe(it.codigo)) }

    // Su margen ya es la confirmación: es lo que aguantó la simulación con la materia vetada
    val dejar = recomendadas.filter { it.margen >= 1 }
        .sortedWith(compareByDescending<Recomendacion> { it.margen }.thenByDescending { it.motivo.ordinal }.thenBy { it.abre.size })
        .firstOrNull()?.codigo

    val conUnaMenos = if (base.carga > 1 && base.plan.isNotEmpty()) {
        sim.planes[base.carga - 2].periodoFinal
    } else null

    return ProximoSemestre(hoy.mas(1), recomendadas, opcionales, dejar, conUnaMenos)
}

data class CadenaExplicada(
    val eslabones: List<String>,
    val periodos: List<Periodo>,
    val final: Periodo?,
    /** Tu fecha si el primer eslabón entra un período más tarde */
    val siAtrasas: Periodo?,
    /** Las del plan que caen más tarde en esa simulación, en el orden del plan */
    val seCorren: List<String>,
    /** Semestres que puede esperar el primer eslabón sin mover la fecha (0: atrasar una mueve la fecha) */
    val holgura: Int,
    /** La materia fuera de la cadena con menor margen (≥ 1): la siguiente rama en volverse crítica */
    val vigilar: Pair<String, Int>?,
)

fun cadenaExplicada(
    pensum: Pensum,
    marcas: Map<String, Marca>,
    casillas: Map<String, String>,
    carga: Int?,
    hoy: Periodo,
    sim: Simulador = Simulador(pensum, marcas, casillas, carga, hoy),
): CadenaExplicada {
    val base = sim.base
    val eslabones = base.cuelloDeBotella
    val primero = eslabones.firstOrNull()

    var siAtrasas: Periodo? = null
    var seCorren = emptyList<String>()
    var holgura = 0
    if (primero != null && base.plan.isNotEmpty()) {
        val atrasada = sim.conVeto(primero, sim.indiceEnPlan(primero) + 1)
        siAtrasas = atrasada.periodoFinal
        val nuevo = atrasada.plan.withIndex().flatMap { (i, p) -> p.map { it to i } }.toMap()
        seCorren = base.plan.withIndex().flatMap { (i, p) -> p.filter { (nuevo[it] ?: i) > i } }
        holgura = sim.margen(primero)
    }

    // Fuera de la cadena, la de menor margen ≥ 1; a igual margen, la que entra antes en el plan
    val enCadena = eslabones.toSet()
    val vigilar = base.plan.withIndex()
        .flatMap { (i, p) -> p.filter { it !in enCadena }.map { it to i } }
        .map { (codigo, i) -> Triple(codigo, sim.margen(codigo), i) }
        .filter { it.second >= 1 }
        .minWithOrNull(compareBy<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
        ?.let { it.first to it.second }

    return CadenaExplicada(eslabones, periodosDeCadena(base, hoy), base.periodoFinal, siAtrasas, seCorren, holgura, vigilar)
}
