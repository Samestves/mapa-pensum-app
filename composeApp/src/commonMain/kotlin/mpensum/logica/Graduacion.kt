package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.plataforma.mesLocal
import kotlin.math.max
import kotlin.math.min

/*
 * Cuándo te gradúas. Nace del plan de ruta de la web (layout/planificador.js)
 * y corrige lo que hacía que la fecha "siempre marcara los mismos meses":
 *
 *   - La web sumaba semestres × 6 meses a la fecha de hoy. Un múltiplo de seis
 *     meses cae siempre en el mes de hoy o en el opuesto: abierta en octubre,
 *     decía octubre o abril, fuera cual fuera tu avance. Aquí se cuenta en
 *     períodos de la UDO (I y II), no en meses desde hoy.
 *   - La carga salía de un valor fijo (16 UC) mientras no la tocaras en el
 *     plan, sin mirar cuántas cursas ni a qué ritmo vas. Aquí sale de ti.
 */

/**
 * Un período de la UDO: dos por año, el I y el II.
 *
 * La app no guarda en qué período vas, así que se deduce del calendario del
 * teléfono: de enero a junio es el I y de julio a diciembre el II. Es un
 * supuesto, no el cronograma oficial: la UDO arranca sus lapsos tarde a menudo
 * y en fechas que cambian cada año. `inicioLapsoII` deja moverlo sin tocar el
 * resto si algún día se sabe mejor.
 */
data class Periodo(val anio: Int, val lapso: Int) : Comparable<Periodo> {
    init {
        require(lapso == 1 || lapso == 2) { "Un año de la UDO tiene dos lapsos: I y II" }
    }

    /** Los períodos contados de uno en uno desde el año cero: sumar es sumar aquí */
    private val indice get() = anio * 2 + lapso - 1

    fun mas(periodos: Int): Periodo {
        val i = indice + periodos
        return Periodo(i.floorDiv(2), i.mod(2) + 1)
    }

    override fun compareTo(other: Periodo) = indice.compareTo(other.indice)

    /** Como lo escribe la UDO: "2028-II". Solo para depurar: en pantalla va [mesYAnio] o [rango], sin números romanos */
    override fun toString() = "$anio-${if (lapso == 1) "I" else "II"}"

    /** Cuándo termina el lapso, para decir cuándo te gradúas: "Jun 2030" (el I) o "Dic 2029" (el II) */
    fun mesYAnio() = if (lapso == 1) "Jun $anio" else "Dic $anio"

    /** Los meses que dura, para nombrar un semestre: "Ene – Jun 2027" (el I) o "Jul – Dic 2026" (el II) */
    fun rango() = if (lapso == 1) "Ene – Jun $anio" else "Jul – Dic $anio"

    companion object {
        /** El mes en que empieza el lapso II (julio) */
        const val INICIO_LAPSO_II = 7

        fun de(anio: Int, mes: Int, inicioLapsoII: Int = INICIO_LAPSO_II) = Periodo(anio, if (mes < inicioLapsoII) 1 else 2)

        /** El período de hoy según el teléfono */
        fun actual(): Periodo = mesLocal().let { de(it.anio, it.mes) }
    }
}

/** De dónde sale cuántas materias metes por período: lo que la hoja dice para explicar la fecha */
enum class FuenteCarga {
    /** La eligió el estudiante ("¿y si meto 5?") */
    ELEGIDA,
    /** Las que cursa ahora, cuando son más que su ritmo */
    CURSANDO,
    /** Lo que aprobó repartido entre los semestres que ya recorrió */
    RITMO,
    /** Sin aprobadas: lo que trae un semestre típico del pensum */
    PENSUM,
}

data class EstimacionGraduacion(
    /**
     * Los períodos que te faltan, contando el que cursas si cursas algo: con
     * las dos últimas materias en curso es 1 ("este"), no 0. Cero es que ya
     * terminaste.
     */
    val semestresRestantes: Int,
    /**
     * Lo menos que puede tardar por las prelaciones, metas las materias que
     * metas: la cadena más larga de lo pendiente (más el período en curso si
     * cursas algo). semestresRestantes nunca baja de aquí.
     */
    val minimoPorPrelaciones: Int,
    /** Cuántas materias se metieron por período en la cuenta */
    val carga: Int,
    val fuenteCarga: FuenteCarga,
    /** El período en que terminarías; null si ya terminaste */
    val periodoFinal: Periodo?,
    /**
     * La cadena crítica: los códigos de la cadena más larga de lo pendiente,
     * de la primera a la última. Es lo que de verdad marca el mínimo. Una
     * casilla sin electiva elegida sale con el código de la casilla.
     */
    val cuelloDeBotella: List<String>,
    /**
     * Lo que se metería en cada período que viene, en orden. Las casillas sin
     * electiva elegida salen con el código de la casilla, o "grupo#n" si su
     * grupo no tiene casillas en los semestres.
     */
    val plan: List<List<String>>,
    /** Las que faltan sin contar las que cursas, casillas incluidas */
    val materiasRestantes: Int,
    val cursaAhora: Boolean,
    /**
     * Por cada pendiente, su cadena: cuántos períodos hacen falta, como
     * mínimo, para ella y lo que cuelga de ella (con el freno del nivel). Es
     * lo primero que decide quién entra antes, y sirve para medir cuánto
     * puede esperar una materia.
     */
    val cadenaDe: Map<String, Int> = emptyMap(),
) {
    val terminado: Boolean get() = semestresRestantes == 0
}

/**
 * Cuántos semestres por encima de tu obligatoria más atrasada puede ir lo que
 * se mete en un período. La misma regla que la web (VENTANA_NIVEL): la carrera
 * se cursa por niveles, y sin ella la cuenta metía materias de quinto sin
 * prelación en el primer período de un nuevo ingreso.
 */
private const val VENTANA_NIVEL = 2

/** Lo máximo que la UDO deja inscribir en un período (LIMITES_CARGA de la web) */
private const val TOPE_UC_PERIODO = 28

/** Si los datos tuvieran un ciclo, que la cuenta acabe igual */
private const val TOPE_PERIODOS = 200

/** Lo que falta, ya convertido en algo que se inscribe: una materia o una casilla por llenar */
private class Pendiente(
    val codigo: String,
    val semestre: Int?,
    val uc: Int,
    val prerrequisitos: List<String>,
    /** Obligatoria con semestre: la más atrasada de estas dice por qué nivel vas */
    val marcaNivel: Boolean,
    /** Casilla sin electiva elegida: cuenta como una materia, pero no se sabe cuál */
    val generica: Boolean,
    /** Requisito de UC aprobadas ("120 UC aprobadas"), si lo tiene */
    val ucMinimas: Int?,
)

private fun pendienteDe(a: Asignatura, semestre: Int?, marcaNivel: Boolean) = Pendiente(
    codigo = a.codigo,
    semestre = semestre,
    uc = a.uc ?: 0,
    prerrequisitos = a.prerrequisitos,
    marcaNivel = marcaNivel,
    generica = false,
    ucMinimas = a.ucMinimas,
)

/** La mediana, redondeando la mitad hacia arriba; null sin datos */
private fun mediana(valores: List<Int>): Int? {
    if (valores.isEmpty()) return null
    val orden = valores.sorted()
    val medio = orden.size / 2
    return if (orden.size % 2 == 1) orden[medio] else redondear((orden[medio - 1] + orden[medio]) / 2.0)
}

/** Cuántas materias trae un semestre típico del pensum, casillas de electiva incluidas */
private fun cargaDelPensum(pensum: Pensum): Int = max(1, mediana(pensum.semestres.map { it.cantidad + it.huecos }) ?: 1)

/**
 * Lo que falta para el título. Lo marcado (aprobado o en curso) ya no falta:
 * lo que cursas se da por aprobado al final de este período.
 *
 * Las electivas siguen la misma idea que la web y las casillas del mapa:
 *   - Donde el grupo tiene cuota de UC (Sistemas), faltan las UC que no
 *     cubre lo hecho: primero con las que elegiste en tus casillas, y lo que
 *     quede con casillas genéricas de las UC típicas del grupo.
 *   - Donde no la tiene, cada casilla de los semestres pide una electiva
 *     (Ambiental), y entran además las que elegiste en la franja: ya
 *     decidiste cursarlas. Sin casillas ni elegidas no se inventa nada.
 *   - Lo hecho ocupa primero las casillas donde lo pusiste, y lo que no tiene
 *     casilla, las más tempranas: así lo genérico cae en los semestres de las
 *     casillas que siguen vacías. Dónde está cada electiva lo dice
 *     `casillasEfectivas`, el mismo mapa con el que se dibuja el mapa.
 */
private fun pendientesDe(pensum: Pensum, marcas: Map<String, Marca>, elegidasEnCasillas: Map<String, String>): List<Pendiente> {
    val casillas = casillasEfectivas(pensum, elegidasEnCasillas, marcas)
    val hecha = { codigo: String -> marcas[codigo] != null }
    val pendientes = pensum.obligatorias.filter { !hecha(it.codigo) }.map { pendienteDe(it, it.semestre, marcaNivel = it.semestre != null) }.toMutableList()
    val semestreDeCasilla = pensum.asignaturas.filter { it.esHueco }.associate { it.codigo to it.semestre }

    for (g in pensum.grupos) {
        val delGrupo = g.asignaturas.associateBy { it.codigo }
        val hechas = g.asignaturas.filter { hecha(it.codigo) }
        val puestas = casillas.filterValues { it in delGrupo }
        val elegidas = puestas.entries.filter { !hecha(it.value) }.sortedBy { semestreDeCasilla[it.key] ?: 99 }
        val hechasSinCasilla = hechas.count { it.codigo !in puestas.values }
        // Las casillas que siguen pidiendo electiva, menos las que ya cubre lo hecho sin casilla
        val vacias = pensum.asignaturas
            .filter { it.esHueco && it.grupo == g.clave && it.codigo !in puestas }
            .sortedBy { it.semestre ?: 99 }
            .drop(hechasSinCasilla)
        val ucTipica = ucTipicaDe(g)
        var genericas = 0
        fun generica() = Pendiente(
            codigo = vacias.getOrNull(genericas)?.codigo ?: "${g.clave}#${genericas + 1}",
            // Si hacen falta más que casillas hay, las de más van con la última
            semestre = vacias.getOrNull(min(genericas, vacias.lastIndex))?.semestre,
            uc = ucTipica,
            prerrequisitos = emptyList(),
            marcaNivel = false,
            generica = true,
            ucMinimas = null,
        ).also { genericas += 1 }

        if (g.cuota != null) {
            var falta = g.cuota - hechas.sumOf { it.uc ?: 0 }
            for ((casilla, codigo) in elegidas) {
                if (falta <= 0) break
                val e = delGrupo.getValue(codigo)
                pendientes += pendienteDe(e, semestreDeCasilla[casilla], marcaNivel = false)
                falta -= max(1, e.uc ?: 0)
            }
            while (falta > 0) pendientes += generica().also { falta -= it.uc }
        } else {
            for ((casilla, codigo) in elegidas) pendientes += pendienteDe(delGrupo.getValue(codigo), semestreDeCasilla[casilla], marcaNivel = false)
            repeat(vacias.size) { pendientes += generica() }
        }
    }
    return pendientes
}

/**
 * Cuántas materias metes por período cuando no eliges: lo más alto entre las
 * que cursas y tu ritmo. El ritmo es lo aprobado entre los semestres que ya
 * recorriste (hasta el más alto donde aprobaste algo).
 *
 * Si lo aprobado es escaso para esos semestres (menos de la mitad de lo que
 * trae uno típico: quien marca solo lo que recuerda, o convalidó una materia
 * de quinto), no es un ritmo, es que falta marcar. Entonces va la carga del
 * pensum, la misma que sin aprobadas: dividir daba un ritmo de 1 y una
 * graduación a los 25 años.
 *
 * No basta con lo que cursas: quien marca solo dos en curso casi nunca mete
 * dos, es que no las ha marcado todas, y la cuenta le daba 17 semestres. Y
 * tampoco basta el ritmo: quien este período mete más que antes, va a ese paso.
 * A igual valor gana lo que cursas, que es el dato más de hoy.
 */
private fun cargaHabitual(pensum: Pensum, marcas: Map<String, Marca>): Pair<Int, FuenteCarga> {
    val todas = pensum.todas
    val cursando = todas.count { marcas[it.codigo] == Marca.CURSANDO }
    val aprobadas = todas.count { marcas[it.codigo] == Marca.APROBADA }
    val recorridos = pensum.obligatorias.filter { marcas[it.codigo] == Marca.APROBADA }.mapNotNull { it.semestre }.maxOrNull()
    val delPensum = cargaDelPensum(pensum)
    val porSemestre = if (aprobadas > 0 && recorridos != null) aprobadas.toDouble() / recorridos else null
    val ritmo = if (porSemestre != null && porSemestre >= delPensum / 2.0) max(1, redondear(porSemestre)) to FuenteCarga.RITMO else delPensum to FuenteCarga.PENSUM
    return if (cursando >= ritmo.first) cursando to FuenteCarga.CURSANDO else ritmo
}

/**
 * Estima cuándo te gradúas con lo que llevas y lo que metes por período.
 *
 * 1. Lo que falta: obligatorias y electivas (por casillas o por cuota) sin
 *    marcar; lo que cursas se da por aprobado al cerrar este período.
 * 2. La carga: la elegida, o la habitual (lo más alto entre lo que cursas y
 *    tu ritmo, o el pensum).
 * 3. Cada materia lleva su cadena: cuántos períodos hacen falta, como mínimo,
 *    para ella y lo que cuelga de ella. Una obligatoria vieja hereda además la
 *    cadena de lo que su nivel frena (la ventana de nivel no deja pasar).
 * 4. Se simula período a período: entre lo que tiene sus prelaciones (y sus
 *    UC mínimas) cumplidas y cae en la ventana de nivel, se meten hasta
 *    `carga` materias sin pasar de 28 UC, primero la de cadena más larga y
 *    luego la más atrasada. Siempre entra al menos una.
 *    `noAntesDe` veta: una materia vetada a k no entra antes del período k del
 *    plan (0 es el que viene); un período puede quedar vacío si solo hay vetadas.
 * 5. Los períodos simulados (más el que cursas) son la estimación; la cadena
 *    más larga de lo pendiente es el mínimo, y la estimación nunca baja de él.
 *
 * Pura y barata (unas decenas de materias, una docena de vueltas): se puede
 * recalcular en cada marca. `marcas` y `casillas` son las de EstadoCarrera.
 */
fun estimarGraduacion(
    pensum: Pensum,
    marcas: Map<String, Marca>,
    casillas: Map<String, String>,
    carga: Int? = null,
    hoy: Periodo = Periodo.actual(),
    noAntesDe: Map<String, Int> = emptyMap(),
): EstimacionGraduacion {
    val (pedida, fuente) = if (carga != null) carga to FuenteCarga.ELEGIDA else cargaHabitual(pensum, marcas)
    // Un solo tope para todo: lo que ves en el mando es lo que se cuenta
    val cargaUsada = pedida.coerceIn(1, MAX_CARGA)
    val todas = pensum.todas
    val cursaAhora = todas.any { marcas[it.codigo] == Marca.CURSANDO }
    val pendientes = pendientesDe(pensum, marcas, casillas)
    val porCodigo = pendientes.associateBy { it.codigo }

    // Lo que cada pendiente desbloquea entre lo pendiente: lo ya marcado no cuenta en ninguna cadena
    val siguientes = mutableMapOf<String, MutableList<String>>()
    for (p in pendientes) for (q in p.prerrequisitos) if (q in porCodigo) siguientes.getOrPut(q) { mutableListOf() } += p.codigo

    val alturas = mutableMapOf<String, Int>()
    fun altura(codigo: String, enCurso: MutableSet<String> = mutableSetOf()): Int {
        alturas[codigo]?.let { return it }
        // Un ciclo no debería existir (el validador de la web lo impide), pero no cuelga
        if (!enCurso.add(codigo)) return 0
        val h = 1 + (siguientes[codigo]?.maxOfOrNull { altura(it, enCurso) } ?: 0)
        enCurso.remove(codigo)
        alturas[codigo] = h
        return h
    }

    // La cadena con el freno del nivel: lo más alto que cuelga de los semestres que esta materia frena
    val alturaDesdeSemestre = IntArray((pendientes.maxOfOrNull { it.semestre ?: 0 } ?: 0) + 2)
    for (p in pendientes) if (p.marcaNivel) alturaDesdeSemestre[p.semestre!!] = max(alturaDesdeSemestre[p.semestre], altura(p.codigo))
    for (s in alturaDesdeSemestre.size - 2 downTo 0) alturaDesdeSemestre[s] = max(alturaDesdeSemestre[s], alturaDesdeSemestre[s + 1])
    val cadena = pendientes.associate { p ->
        val frenadas = if (p.marcaNivel) alturaDesdeSemestre.getOrElse(p.semestre!! + VENTANA_NIVEL + 1) { 0 } else 0
        p.codigo to max(altura(p.codigo), if (frenadas > 0) frenadas + 1 else 0)
    }

    // A igual cadena, lo más atrasado; en el mismo semestre, la materia concreta antes que la casilla
    val prioridad = compareByDescending<Pendiente> { cadena.getValue(it.codigo) }
        .thenByDescending { altura(it.codigo) }
        .thenBy { it.semestre ?: 99 }
        .thenBy { it.generica }
        .thenByDescending { siguientes[it.codigo]?.size ?: 0 }
        .thenByDescending { it.uc }
        .thenBy { it.codigo }

    val plan = mutableListOf<List<String>>()
    val restantes = pendientes.toMutableList()
    val porHacer = pendientes.mapTo(mutableSetOf()) { it.codigo }
    var ucHechas = todas.filter { marcas[it.codigo] != null }.sumOf { it.uc ?: 0 }
    while (restantes.isNotEmpty() && plan.size < TOPE_PERIODOS) {
        val abiertas = restantes.filter { p -> p.prerrequisitos.none { it in porHacer } && (p.ucMinimas == null || ucHechas >= p.ucMinimas) }
        // El veto: lo que aún no puede entrar este período se queda fuera
        val listas = if (noAntesDe.isEmpty()) abiertas else abiertas.filter { (noAntesDe[it.codigo] ?: 0) <= plan.size }
        val nivel = restantes.filter { it.marcaNivel }.minOfOrNull { it.semestre!! }
        val enNivel = if (nivel == null) listas else listas.filter { it.semestre == null || it.semestre <= nivel + VENTANA_NIVEL }
        // Si el nivel no deja nada o los datos no cuadran (una prelación imposible), mejor avanzar que atascarse
        val candidatas = if (listas.isEmpty() && abiertas.isNotEmpty()) emptyList() else enNivel.ifEmpty { listas }.ifEmpty { restantes }.sortedWith(prioridad)

        val periodo = mutableListOf<Pendiente>()
        var uc = 0
        for (c in candidatas) {
            if (periodo.size >= cargaUsada) break
            // Siempre entra una, aunque pase del tope: una de 6 UC no puede atascar la cuenta
            if (periodo.isEmpty() || uc + c.uc <= TOPE_UC_PERIODO) {
                periodo += c
                uc += c.uc
            }
        }
        plan += periodo.map { it.codigo }
        restantes -= periodo.toSet()
        periodo.forEach { porHacer -= it.codigo }
        ucHechas += uc
    }

    // La cadena crítica, de su primera materia a la última; a igual altura, la concreta y la más atrasada
    val desempate = compareBy<Pendiente> { it.generica }.thenBy { it.semestre ?: 99 }.thenBy { it.codigo }
    val cuello = mutableListOf<String>()
    var eslabon = pendientes.sortedWith(compareByDescending<Pendiente> { altura(it.codigo) }.then(desempate)).firstOrNull()
    while (eslabon != null) {
        cuello += eslabon.codigo
        val h = altura(eslabon.codigo)
        eslabon = siguientes[eslabon.codigo].orEmpty().map(porCodigo::getValue).filter { altura(it.codigo) == h - 1 }.sortedWith(desempate).firstOrNull()
    }

    val enCurso = if (cursaAhora) 1 else 0
    val minimo = enCurso + (pendientes.maxOfOrNull { altura(it.codigo) } ?: 0)
    return EstimacionGraduacion(
        semestresRestantes = max(enCurso + plan.size, minimo),
        minimoPorPrelaciones = minimo,
        carga = cargaUsada,
        fuenteCarga = fuente,
        // Lo simulado empieza el período que viene: cursando o no, este ya no se puede llenar
        periodoFinal = when {
            plan.isNotEmpty() -> hoy.mas(plan.size)
            cursaAhora -> hoy
            else -> null
        },
        cuelloDeBotella = cuello,
        plan = plan,
        materiasRestantes = pendientes.size,
        cursaAhora = cursaAhora,
        cadenaDe = cadena,
    )
}
