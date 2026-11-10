package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Grupo

/*
 * La lista «Por hacer»: qué meto ya y qué no puedo atrasar. Aquí solo se
 * decide qué materia va en qué sección, en qué orden y qué dice su renglón;
 * cómo se pinta es de la vista. Todo sale de los estados de siempre: la lista
 * no guarda nada suyo.
 *
 * Los textos llevan «**así**» donde hay que destacar algo: la vista los
 * convierte en negrita sin que la lógica sepa de estilos.
 */

/** Las cuatro secciones de la lista por estado, en el orden en que se ven */
enum class SeccionEstado(val clave: String, val titulo: String) {
    INSCRIBIR("inscribir", "Puedes inscribir"),
    CURSANDO("cursando", "Cursando"),
    BLOQUEADAS("bloqueadas", "Bloqueadas"),
    APROBADAS("aprobadas", "Aprobadas"),
}

/** Cómo se lee una materia: su nombre y su estado, con lo que falta si está bloqueada. La comparten la lista y el mapa */
fun descripcionDeMateria(nombre: String, situacion: Situacion, nota: String = ""): String = when (situacion) {
    Situacion.HECHA -> "$nombre, Aprobada"
    Situacion.CURSANDO -> "$nombre, Cursando"
    Situacion.INSCRIBIBLE -> "$nombre, Puedes inscribirla"
    Situacion.PROXIMA, Situacion.LEJANA -> if (nota.isEmpty()) "$nombre, Bloqueada" else "$nombre, Bloqueada. $nota"
}

/**
 * Una materia con todo lo que hace falta para ordenarla y explicarla. Son
 * datos planos (texto, números y enums) para que Compose los compare por
 * valor: marcar una materia cambia la suya y las demás, iguales, no se pintan.
 */
data class MateriaLista(
    val codigo: String,
    val nombre: String,
    val uc: Int?,
    /** El semestre donde cuenta: el suyo o el de la casilla donde se eligió; null en la franja de electivas */
    val semestre: Int?,
    val situacion: Situacion,
    /** Lo que le falta aprobar, por nombre y en el orden del pensum */
    val faltan: List<String>,
    /** De lo que le falta, lo que ya cursas */
    val cursandoFaltan: List<String>,
    /** Las materias pendientes que abre directamente */
    val abre: List<String>,
    /** De ellas, las que se abren al aprobar esta porque solo esta las frena */
    val seAbren: List<String>,
    /** Cuántas materias pendientes cuelgan en fila de esta: el largo de la cadena que abre */
    val cadena: Int,
    val requisitoEspecial: String?,
    /** Su lugar en el pensum: desempata cualquier orden */
    val orden: Int,
)

/** Una casilla de electiva sin elegir: se toca para elegir */
data class CasillaVacia(
    val codigo: String,
    val nombre: String,
    val semestre: Int?,
    /** Cuántas electivas admite su grupo; 0 si no se sabe */
    val opciones: Int,
    /** El grupo de electivas al que pertenece: de él sale que varias casillas iguales se resuman en una fila */
    val grupo: String? = null,
    /** Cuántas casillas de su grupo siguen por elegir, esta incluida */
    val pendientes: Int = 1,
)

/** Todo lo que la lista muestra: las materias que cuentan y las casillas por elegir */
class UniversoLista(val materias: List<MateriaLista>, val casillas: List<CasillaVacia>) {
    private val porCodigo = materias.associateBy { it.codigo }
    private val casillaPorCodigo = casillas.associateBy { it.codigo }

    fun materia(codigo: String): MateriaLista? = porCodigo[codigo]
    fun casilla(codigo: String): CasillaVacia? = casillaPorCodigo[codigo]
}

/**
 * Arma el universo de la lista. `nodos` son las materias del mapa, casillas
 * incluidas; `enCasilla` dice qué electiva se eligió en cada una. Una electiva
 * elegida cuenta como cualquier materia, con el semestre de su casilla.
 */
fun universoDeLista(
    nodos: List<Asignatura>,
    grupos: List<Grupo>,
    enCasilla: (String) -> Asignatura?,
    estados: Map<String, EstadoMateria>,
    situaciones: Map<String, Situacion>,
    nombreDe: (String) -> String?,
): UniversoLista {
    val entradas = mutableListOf<Pair<Asignatura, Int?>>()
    val vacias = mutableListOf<CasillaVacia>()
    for (n in nodos) {
        if (!n.esHueco) {
            entradas += n to n.semestre
            continue
        }
        val elegida = enCasilla(n.codigo)
        if (elegida != null) entradas += elegida to n.semestre
        else vacias += CasillaVacia(n.codigo, n.nombre, n.semestre, grupos.firstOrNull { it.clave == n.grupo }?.asignaturas?.size ?: 0, n.grupo)
    }

    // Lo que cuelga de cada materia entre lo que aún no está aprobado
    val hijos = HashMap<String, MutableList<Asignatura>>()
    for ((a, _) in entradas) {
        if (estados[a.codigo] == EstadoMateria.APROBADA) continue
        for (p in a.prerrequisitos) hijos.getOrPut(p) { mutableListOf() } += a
    }
    val alturas = HashMap<String, Int>()
    fun altura(codigo: String, enCurso: MutableSet<String>): Int {
        alturas[codigo]?.let { return it }
        // Un ciclo no debería existir, pero no cuelga
        if (!enCurso.add(codigo)) return 0
        val h = hijos[codigo]?.maxOfOrNull { 1 + altura(it.codigo, enCurso) } ?: 0
        enCurso.remove(codigo)
        alturas[codigo] = h
        return h
    }

    val porGrupo = vacias.groupingBy { it.grupo }.eachCount()
    val materias = entradas.mapIndexed { i, (a, semestre) ->
        val faltanCodigos = a.prerrequisitos.filter { estados[it] != EstadoMateria.APROBADA }
        val abre = hijos[a.codigo].orEmpty()
        MateriaLista(
            codigo = a.codigo,
            nombre = a.nombre,
            uc = a.uc,
            semestre = semestre,
            situacion = situaciones[a.codigo] ?: Situacion.LEJANA,
            faltan = faltanCodigos.map { nombreDe(it) ?: it },
            cursandoFaltan = faltanCodigos.filter { estados[it] == EstadoMateria.CURSANDO }.map { nombreDe(it) ?: it },
            abre = abre.map { it.nombre },
            seAbren = abre.filter { h -> estados[h.codigo] == EstadoMateria.BLOQUEADA && h.prerrequisitos.all { it == a.codigo || estados[it] == EstadoMateria.APROBADA } }.map { it.nombre },
            cadena = if (estados[a.codigo] == EstadoMateria.APROBADA) 0 else altura(a.codigo, mutableSetOf()),
            requisitoEspecial = a.requisitoEspecial,
            orden = i,
        )
    }
    return UniversoLista(materias, vacias.map { it.copy(pendientes = porGrupo[it.grupo] ?: 1) })
}

fun seccionDe(s: Situacion): SeccionEstado = when (s) {
    Situacion.INSCRIBIBLE -> SeccionEstado.INSCRIBIR
    Situacion.CURSANDO -> SeccionEstado.CURSANDO
    Situacion.PROXIMA, Situacion.LEJANA -> SeccionEstado.BLOQUEADAS
    Situacion.HECHA -> SeccionEstado.APROBADAS
}

// ─────────────────────────────────── El orden ──────────────────────────────────

/** Un grupo de la lista con sus códigos en orden: la clave es la de su sección o `s<n>` de un semestre */
data class GrupoLista(val clave: String, val codigos: List<String>)

/** Una materia sin margen medido (no está en el plan) pasa al final de su empate */
private const val SIN_DATO = Int.MAX_VALUE

/**
 * Las secciones por estado, cada una en su orden de urgencia.
 *
 * - Puedes inscribir: primero lo que no tiene margen, y de ahí lo que más
 *   puede esperar (el margen que mide la hoja de avance); a igual margen, la
 *   cadena más larga y lo que más abre. Detrás van las casillas de electiva
 *   que ya se pueden cursar: las de los semestres que vas o ya pasaste.
 * - Bloqueadas: primero las que solo esperan algo que ya cursas.
 */
fun estructuraPorEstado(u: UniversoLista, margenes: Map<String, Int>, semestreActual: Int?): List<GrupoLista> {
    fun delEstado(s: SeccionEstado) = u.materias.filter { seccionDe(it.situacion) == s }

    val inscribibles = delEstado(SeccionEstado.INSCRIBIR).sortedWith(
        compareBy<MateriaLista> { margenes[it.codigo] ?: SIN_DATO }
            .thenByDescending { it.cadena }
            .thenByDescending { it.abre.size }
            .thenBy { it.semestre ?: 99 }
            .thenBy { it.orden },
    ).map { it.codigo }
    // Varias casillas iguales (tres sociohumanísticas) son una sola fila: la más atrasada
    val casillas = u.casillas
        .filter { it.semestre != null && semestreActual != null && it.semestre <= semestreActual }
        .sortedBy { it.semestre }
        .distinctBy { it.grupo ?: it.codigo }
        .map { it.codigo }

    val bloqueadas = delEstado(SeccionEstado.BLOQUEADAS).sortedWith(
        compareBy<MateriaLista> { if (it.situacion == Situacion.PROXIMA) 0 else 1 }
            .thenBy { it.faltan.size }
            .thenBy { it.orden },
    ).map { it.codigo }

    return listOf(
        GrupoLista(SeccionEstado.INSCRIBIR.clave, inscribibles + casillas),
        GrupoLista(SeccionEstado.CURSANDO.clave, delEstado(SeccionEstado.CURSANDO).sortedBy { it.orden }.map { it.codigo }),
        GrupoLista(SeccionEstado.BLOQUEADAS.clave, bloqueadas),
        GrupoLista(SeccionEstado.APROBADAS.clave, delEstado(SeccionEstado.APROBADAS).sortedWith(compareBy<MateriaLista> { it.semestre ?: 99 }.thenBy { it.orden }).map { it.codigo }),
    ).filter { it.codigos.isNotEmpty() }
}

/** La clave del grupo de un semestre, y la de las electivas sin semestre */
fun claveDeSemestre(semestre: Int?): String = if (semestre == null) "electivas" else "s$semestre"

/** «Ordenar por semestre»: el orden del pensum, con las casillas por elegir al final de su semestre */
fun estructuraPorSemestre(u: UniversoLista): List<GrupoLista> {
    val claves = (u.materias.map { it.semestre } + u.casillas.map { it.semestre }).distinct().sortedBy { it ?: 99 }
    return claves.map { s ->
        GrupoLista(
            claveDeSemestre(s),
            u.materias.filter { it.semestre == s }.sortedBy { it.orden }.map { it.codigo } + u.casillas.filter { it.semestre == s }.map { it.codigo },
        )
    }.filter { it.codigos.isNotEmpty() }
}

/** El semestre por el que vas, el mismo que usan la portada y la barra: se calcula una sola vez para todos */
fun semestreActualDeLista(nodos: List<Asignatura>, enCasilla: (String) -> Asignatura?, estados: Map<String, EstadoMateria>): Int? =
    semestreActual(marcasDeSemestres(nodos, enCasilla, estados))

// ─────────────────────────────────── Lo que dice cada fila ──────────────────────────────────

/** La fila ya resuelta: lo que se pinta, en datos planos */
data class FilaLista(
    val codigo: String,
    val nombre: String,
    val uc: Int?,
    /** null en una casilla sin elegir: no es una materia ni tiene estado */
    val situacion: Situacion?,
    /** La única razón bajo el nombre, con «**negrita**» donde destaca */
    val razon: String,
) {
    val esCasilla: Boolean get() = situacion == null

    /** Lo que lee el lector de pantalla: el estado ya lo dice el icono, aquí se dice con palabras */
    val descripcion: String get() = if (situacion == null) "$nombre, Electiva por elegir" else descripcionDeMateria(nombre, situacion, razon.replace("**", ""))
}

/**
 * Lo más largo que puede ser la razón de una fila: cabe entera en una línea
 * de 13 sp en un teléfono de 360 dp, junto a las UC. Lo que no cabe no se
 * corta con «…»: se dice de forma más corta.
 */
const val LARGO_RAZON = 30

/** La versión larga si cabe (sin contar las marcas de negrita); si no, la corta */
private fun quepa(larga: String, corta: String) = if (larga.replace("**", "").length <= LARGO_RAZON) larga else corta

private fun semestreTexto(semestre: Int?) = if (semestre == null) "Electiva" else "Semestre $semestre"

/**
 * Lo que dice una materia bajo su nombre: una sola razón corta. De una
 * inscribible, cuánto puede esperar (y, si no puede, cuánto frena); de una
 * cursando, qué abre; de una bloqueada, qué le falta. Los nombres van solo si
 * caben; si no, se cuentan. `margen` es null mientras se mide (o si la materia
 * no está en el plan).
 */
fun razonDe(m: MateriaLista, margen: Int?): String = when (m.situacion) {
    Situacion.INSCRIBIBLE -> when {
        margen == 0 && m.cadena >= 2 -> "**Sin margen** · abre ${m.cadena} en cadena"
        margen == 0 -> "**Sin margen**"
        margen != null -> "Puede esperar $margen"
        else -> "Puedes inscribirla"
    }
    Situacion.CURSANDO -> when {
        m.seAbren.size == 1 -> quepa("Al aprobarla abre ${m.seAbren[0]}", "Al aprobarla abre 1")
        m.seAbren.size > 1 -> "Al aprobarla abre ${m.seAbren.size}"
        else -> semestreTexto(m.semestre)
    }
    Situacion.PROXIMA -> when (m.cursandoFaltan.size) {
        1 -> quepa("Al aprobar ${m.cursandoFaltan[0]}", "Al aprobar lo que cursas")
        else -> "Al aprobar lo que cursas"
    }
    Situacion.LEJANA -> when {
        m.faltan.size == 1 -> quepa("Falta ${m.faltan[0]}", "Falta 1 materia")
        m.faltan.size > 1 -> "Faltan ${materias(m.faltan.size)}"
        m.requisitoEspecial != null -> quepa("Pide ${m.requisitoEspecial}", "Tiene un requisito especial")
        else -> "Bloqueada"
    }
    Situacion.HECHA -> semestreTexto(m.semestre)
}

fun filaDe(m: MateriaLista, margen: Int?): FilaLista = FilaLista(m.codigo, m.nombre, m.uc, m.situacion, razonDe(m, margen))

/** `resumida` es la fila que habla por todas las de su grupo: dice cuántas faltan en vez de en qué semestre cae solo esa */
fun filaDeCasilla(c: CasillaVacia, resumida: Boolean = false): FilaLista = FilaLista(
    c.codigo, c.nombre, null, null,
    listOfNotNull(
        if (resumida && c.pendientes > 1) "Faltan ${c.pendientes}" else c.semestre?.let { "Del semestre $it" },
        if (c.opciones > 0) "elige 1 de ${c.opciones}" else "por elegir",
    ).joinToString(" · "),
)

/**
 * Lo que dice la fila de una materia recién marcada, mientras se puede
 * deshacer: la fila se queda en su sitio y cuenta qué pasó.
 */
fun razonTrasMarcar(marca: Marca?): String = when (marca) {
    Marca.CURSANDO -> "Pasó a cursando"
    Marca.APROBADA -> "Aprobada"
    null -> "Sin cursar"
}

// ─────────────────────────────────── Los encabezados ──────────────────────────────────

/** Las UC de un grupo de materias; las que no dicen cuántas valen no suman */
private fun uc(materias: List<MateriaLista>) = materias.sumOf { it.uc ?: 0 }

/**
 * Lo que cuenta el encabezado de una sección: cuántas hay y cuánto pesan. Se
 * saca de lo que cada materia es ahora y no de lo que había al entrar: la fila
 * recién marcada sigue en su sitio, pero la cuenta ya es la nueva.
 */
fun metaDeGrupo(clave: String, u: UniversoLista): String {
    fun cuenta(s: SeccionEstado) = u.materias.filter { seccionDe(it.situacion) == s }
    return when (clave) {
        SeccionEstado.INSCRIBIR.clave -> cuenta(SeccionEstado.INSCRIBIR).let { "${it.size} · ${uc(it)} UC" }
        SeccionEstado.CURSANDO.clave -> cuenta(SeccionEstado.CURSANDO).let { "${it.size} · ${uc(it)} UC" }
        SeccionEstado.BLOQUEADAS.clave -> "${cuenta(SeccionEstado.BLOQUEADAS).size}"
        SeccionEstado.APROBADAS.clave -> cuenta(SeccionEstado.APROBADAS).let { "${it.size} · ${uc(it)} UC" }
        "electivas" -> "${u.materias.count { it.semestre == null }} elegidas"
        else -> {
            val semestre = clave.removePrefix("s").toIntOrNull()
            val delSemestre = u.materias.filter { it.semestre == semestre }
            val total = delSemestre.size + u.casillas.count { it.semestre == semestre }
            "${delSemestre.count { it.situacion == Situacion.HECHA }} de $total · ${uc(delSemestre)} UC"
        }
    }
}

/** El título de un grupo de la lista */
fun tituloDeGrupo(clave: String): String = SeccionEstado.entries.firstOrNull { it.clave == clave }?.titulo
    ?: clave.removePrefix("s").toIntOrNull()?.let { "Semestre $it" } ?: "Electivas"

/** La línea de bajo el título: «62 de 153 UC · te gradúas Dic 2029». Sin créditos oficiales, cuenta materias */
fun subtituloDeLista(ucAprobadas: Int, ucTitulo: Int?, aprobadas: Int, fin: Periodo?): String {
    val avance = if (ucTitulo != null) "**$ucAprobadas** de $ucTitulo UC" else "**$aprobadas** ${if (aprobadas == 1) "aprobada" else "aprobadas"}"
    return if (fin != null) "$avance · te gradúas **${fin.mesYAnio()}**" else avance
}

// ─────────────────────────────────── Marcar ──────────────────────────────────

/** Lo que hace deslizar una fila a la derecha: la marca que sigue y cómo se llama el gesto */
data class Paso(val marca: Marca?, val etiqueta: String)

/** El paso siguiente de una materia; las bloqueadas y las aprobadas no se marcan con un gesto */
fun pasoSiguiente(s: Situacion): Paso? = when (s) {
    Situacion.INSCRIBIBLE -> Paso(Marca.CURSANDO, "Cursando")
    Situacion.CURSANDO -> Paso(Marca.APROBADA, "Aprobada")
    else -> null
}

/** Una acción del muelle: lo que dice el botón y lo que marca; null desmarca */
data class AccionMarca(val texto: String, val marca: Marca?)

/**
 * Lo que ofrece el muelle de la página de una materia: la consecuencia del
 * paso que toca, ese paso en el botón lleno y el otro en texto. Una materia
 * aprobada no tiene paso siguiente: solo se puede quitar la marca.
 */
data class PlanMuelle(
    /** «Sumas **3 UC** a tu semestre · llevas 4» */
    val consecuencia: String,
    /** El icono que acompaña la consecuencia */
    val icono: Situacion,
    val principal: AccionMarca?,
    val secundaria: AccionMarca,
    /** Si el botón principal va lleno en tinta: solo cuando la materia puede darse ese paso */
    val lleno: Boolean,
)

fun planMuelle(m: MateriaLista, ucCursando: Int): PlanMuelle {
    val uc = m.uc ?: 0
    val aprobada = AccionMarca("Ya la aprobé", Marca.APROBADA)
    val cursando = AccionMarca("La estoy cursando", Marca.CURSANDO)
    return when (m.situacion) {
        Situacion.INSCRIBIBLE -> PlanMuelle("Sumas **$uc UC** a tu semestre · llevas $ucCursando", Situacion.CURSANDO, cursando, aprobada, lleno = true)
        Situacion.CURSANDO -> PlanMuelle(
            when {
                m.seAbren.size == 1 -> "Al aprobarla se te abre **${m.seAbren[0]}**"
                m.seAbren.size > 1 -> "Al aprobarla se te abren **${m.seAbren.size}**"
                else -> "Al aprobarla sumas **$uc UC** a tu avance"
            },
            if (m.seAbren.isEmpty()) Situacion.HECHA else Situacion.LEJANA,
            aprobada, AccionMarca("Ya no la curso", null), lleno = true,
        )
        Situacion.HECHA -> PlanMuelle("Cuenta **$uc UC** en tu avance", Situacion.HECHA, null, AccionMarca("Quitar la aprobación", null), lleno = false)
        Situacion.PROXIMA -> PlanMuelle("Se abre al aprobar **${m.cursandoFaltan.firstOrNull() ?: "lo que cursas"}**", Situacion.PROXIMA, cursando, aprobada, lleno = false)
        Situacion.LEJANA -> PlanMuelle(
            when {
                m.faltan.size == 1 -> "Te falta **${m.faltan[0]}**"
                m.faltan.size > 1 -> "Te faltan **${m.faltan.size} materias**"
                else -> "Aún no puedes inscribirla"
            },
            Situacion.LEJANA, cursando, aprobada, lleno = false,
        )
    }
}

// ─────────────────────────────────── Margen ──────────────────────────────────

/** Cuántas materias cuelgan, directa o indirectamente, de una: «1 y 2 tras ella» */
fun descendientes(codigo: String, adelante: Map<String, List<String>>): Int {
    val vistos = HashSet<String>()
    val pila = ArrayDeque(adelante[codigo].orEmpty())
    while (pila.isNotEmpty()) {
        val c = pila.removeLast()
        if (vistos.add(c)) pila.addAll(adelante[c].orEmpty())
    }
    return vistos.size
}

/** Las filas que ocupa un lado si enseña `k` de sus `n` materias: una cada una; sin ninguna no hay nube ni filas */
private fun filasDeLado(n: Int, k: Int) = if (n == 0) 0 else k

/** Cuántas materias de un lado de `n` se enseñan si le caben `filas` filas: nunca menos de una */
fun cuantasCaben(n: Int, filas: Int): Int = minOf(n, maxOf(1, filas))

/**
 * Cuántas materias de «Pide» y de «Abre» se enseñan en la página cuando caben
 * `filas`: la página no tiene scroll. Cada lado enseña al menos una (un lado
 * sin ninguna no ocupa nada) y el sitio que sobra se reparte por turnos, empezando
 * por lo que pide. Lo que no cabe lo cuenta «+N más» en el rótulo del lado,
 * que abre la lista entera.
 */
fun repartirRelaciones(pide: Int, abre: Int, filas: Int): Pair<Int, Int> {
    var p = minOf(pide, 1)
    var a = minOf(abre, 1)
    var libres = filas - filasDeLado(pide, p) - filasDeLado(abre, a)
    while (libres > 0 && (p < pide || a < abre)) {
        if (p < pide) { p++; libres-- }
        if (libres > 0 && a < abre) { a++; libres-- }
    }
    return p to a
}
