package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import mpensum.datos.Pensum

/*
 * Lo que la hoja del progreso cuenta de lo que viene: qué te abre aprobar lo
 * que cursas, en qué período cae cada eslabón de tu cadena, la barra de
 * cápsulas y la fecha más rápida que dejan las prelaciones. Todo puro y sobre
 * las mismas reglas que el mapa (estadosDe) y la fecha (estimarGraduacion):
 * nada de esto decide por su cuenta qué está abierto.
 */

/** Lo más que se prueba en «materias por semestre» */
const val MAX_CARGA = 8

/** Las marcas como quedarían si apruebas todo lo que cursas */
fun conCursandoAprobadas(marcas: Map<String, Marca>): Map<String, Marca> =
    marcas.mapValues { (_, marca) -> if (marca == Marca.CURSANDO) Marca.APROBADA else marca }

/** Las que cursas ahora, en el orden del pensum: obligatorias y electivas */
fun cursandoDe(pensum: Pensum, marcas: Map<String, Marca>): List<Asignatura> =
    pensum.todas.filter { marcas[it.codigo] == Marca.CURSANDO }

/**
 * Las de `entre` que se te abren al aprobar `aprobar`: no estaban disponibles
 * y pasan a estarlo. Lo marcado (aprobado o en curso) nunca se «abre».
 */
fun seAbrenAlAprobar(pensum: Pensum, marcas: Map<String, Marca>, aprobar: Collection<String>, entre: List<Asignatura> = pensum.obligatorias): List<Asignatura> {
    if (aprobar.isEmpty()) return emptyList()
    val antes = estadosDe(pensum.todas, marcas)
    val despues = estadosDe(pensum.todas, conMarcas(marcas, aprobar.associateWith { Marca.APROBADA }))
    return entre.filter { a ->
        marcas[a.codigo] == null && a.codigo !in aprobar &&
            antes[a.codigo] != EstadoMateria.DISPONIBLE && despues[a.codigo] == EstadoMateria.DISPONIBLE
    }
}

/** Lo que te da aprobar lo que cursas: a qué avance llegas, cuánto sumas y qué se te abre */
data class SiApruebas(
    /** El avance (de 0 a 100) con lo que cursas ya aprobado */
    val avance: Double,
    /** Lo que sumas: UC con créditos oficiales, materias sin ellos */
    val suma: Int,
    val seAbren: List<Asignatura>,
)

fun siApruebasLoQueCursas(pensum: Pensum, marcas: Map<String, Marca>): SiApruebas {
    val futuras = conCursandoAprobadas(marcas)
    val ahora = progresoDe(pensum.obligatorias, estadosDe(pensum.todas, marcas), pensum.creditos, avanceDeGrupos(pensum.grupos, marcas))
    val luego = progresoDe(pensum.obligatorias, estadosDe(pensum.todas, futuras), pensum.creditos, avanceDeGrupos(pensum.grupos, futuras))
    val conUc = ahora.porcentaje != null
    val suma = if (conUc) (luego.ucAprobadas + luego.ucElectivas) - (ahora.ucAprobadas + ahora.ucElectivas) else luego.aprobadas - ahora.aprobadas
    val cursando = marcas.filterValues { it == Marca.CURSANDO }.keys
    return SiApruebas(luego.avance, suma, seAbrenAlAprobar(pensum, marcas, cursando))
}

/** De lo que se abre al aprobar todo lo que cursas, lo que pide `codigo`: lo que «abre» esa materia */
fun abreAlAprobar(codigo: String, seAbren: List<Asignatura>): List<Asignatura> =
    seAbren.filter { codigo in it.prerrequisitos }

/**
 * Lo que abriría una materia del semestre que viene: con lo que cursas ya
 * aprobado, las obligatorias que pasan a estar disponibles al aprobarla a ella.
 */
fun abreElProximo(pensum: Pensum, marcas: Map<String, Marca>, codigo: String): List<Asignatura> =
    seAbrenAlAprobar(pensum, conCursandoAprobadas(marcas), listOf(codigo))

/** Las obligatorias que podrás inscribir el semestre que viene: las disponibles una vez aprobado lo que cursas */
fun inscribiblesDelProximo(pensum: Pensum, marcas: Map<String, Marca>): List<Asignatura> {
    val futuras = conCursandoAprobadas(marcas)
    val estados = estadosDe(pensum.todas, futuras)
    return pensum.obligatorias.filter { estados[it.codigo] == EstadoMateria.DISPONIBLE }
        .sortedWith(compareBy<Asignatura> { it.semestre ?: 99 }.then { a, b -> compararEs(a.nombre, b.nombre) })
}

/**
 * El período de cada eslabón de la cadena: el del plan en que cae (el plan
 * empieza el período que viene). Si un eslabón no está en el plan, va el
 * período siguiente al del anterior.
 */
fun periodosDeCadena(g: EstimacionGraduacion, hoy: Periodo): List<Periodo> {
    var anterior = -1
    return g.cuelloDeBotella.map { c ->
        val k = g.plan.indexOfFirst { c in it }.takeIf { it >= 0 } ?: (anterior + 1)
        anterior = k
        hoy.mas(k + 1)
    }
}

/** Los planes de 1 a [MAX_CARGA] materias por semestre, en orden */
fun planesPorCarga(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>, hoy: Periodo): List<EstimacionGraduacion> =
    (1..MAX_CARGA).map { estimarGraduacion(pensum, marcas, casillas, it, hoy) }

/** La carga más baja con la que ya te gradúas lo antes posible: más de esto no adelanta */
fun cargaMasRapida(planes: List<EstimacionGraduacion>): Int {
    val minimo = planes.minOf { it.semestresRestantes }
    return planes.indexOfFirst { it.semestresRestantes == minimo } + 1
}

/**
 * Cuándo te graduarías si repruebas `codigo` (una que cursas): vuelve a
 * faltar, y se recalcula con la misma carga. null si ya no quedara nada.
 */
fun fechaSiRepruebas(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>, carga: Int, hoy: Periodo, codigo: String): Periodo? =
    estimarGraduacion(pensum, marcas - codigo, casillas, carga, hoy).periodoFinal

/**
 * Las que cursas que, si las repruebas, corren tu fecha `g`, cada una con la
 * fecha a la que la corren: las que están en tu cadena. Una simulación por
 * materia en curso, con la carga de `g`.
 */
fun retrasosSiRepruebas(pensum: Pensum, marcas: Map<String, Marca>, casillas: Map<String, String>, g: EstimacionGraduacion, hoy: Periodo): List<Pair<Asignatura, Periodo>> {
    val final = g.periodoFinal ?: return emptyList()
    return cursandoDe(pensum, marcas).mapNotNull { c ->
        fechaSiRepruebas(pensum, marcas, casillas, g.carga, hoy, c.codigo)?.takeIf { it > final }?.let { c to it }
    }
}

/** Las UC más repetidas entre las electivas del grupo: las de una casilla sin elegir */
fun ucTipicaDe(grupo: Grupo): Int =
    grupo.asignaturas.mapNotNull { it.uc }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 3

// ──────────────────────────────── La barra de cápsulas ───────────────────────────────

enum class EstadoCapsula { APROBADA, CURSANDO, PENDIENTE }

/** Una materia de la barra: su ancho sale de sus UC */
data class Capsula(val uc: Int, val estado: EstadoCapsula)

data class SemestreCapsulas(val numero: Int, val capsulas: List<Capsula>) {
    val completo: Boolean get() = capsulas.all { it.estado == EstadoCapsula.APROBADA }

    /** La cuenta del semestre, para decir por cuál vas con la misma regla que la portada y la lista */
    val cuenta: CuentaSemestre
        get() = CuentaSemestre(
            total = capsulas.size,
            hechas = capsulas.count { it.estado == EstadoCapsula.APROBADA },
            cursando = capsulas.count { it.estado == EstadoCapsula.CURSANDO },
            huecos = 0,
        )
}

/**
 * Una cápsula por materia, semestre a semestre. Dentro de cada semestre van
 * las aprobadas, luego las que cursas y luego las pendientes, cada grupo en
 * el orden del pensum. Una casilla de electiva cuenta: con la electiva que
 * pusiste (sus UC y su estado) o, vacía, pendiente con las UC típicas de su grupo.
 */
fun capsulasDe(pensum: Pensum, marcas: Map<String, Marca>, enCasilla: (String) -> Asignatura?): List<SemestreCapsulas> {
    val tipicas = pensum.grupos.associate { it.clave to ucTipicaDe(it) }
    return pensum.asignaturas.filter { it.semestre != null }.groupBy { it.semestre!! }.toSortedMap().map { (numero, nodos) ->
        val capsulas = nodos.map { nodo ->
            val materia = if (nodo.esHueco) enCasilla(nodo.codigo) else nodo
            val uc = materia?.uc ?: nodo.uc ?: tipicas[nodo.grupo] ?: 1
            val estado = when (materia?.let { marcas[it.codigo] }) {
                Marca.APROBADA -> EstadoCapsula.APROBADA
                Marca.CURSANDO -> EstadoCapsula.CURSANDO
                null -> EstadoCapsula.PENDIENTE
            }
            Capsula(maxOf(1, uc), estado)
        }
        SemestreCapsulas(numero, capsulas.sortedBy { it.estado.ordinal })
    }
}

/** El semestre por el que vas según la barra (ver la regla en `semestreActual` de las cuentas) */
fun semestreActual(semestres: List<SemestreCapsulas>): Int? = semestreActual(semestres.associate { it.numero to it.cuenta })
