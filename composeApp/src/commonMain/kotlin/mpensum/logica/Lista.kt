package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Grupo

/*
 * La lista: la misma carrera que el mapa, contada por semestres. Aquí solo se
 * decide qué fila va en qué grupo y qué dice; cómo se pinta es de la vista.
 * Todo sale de los estados de siempre: la lista no guarda nada suyo.
 */

/** Los cuatro estados por los que se filtra: la próxima y la lejana van juntas en Bloqueadas */
enum class FiltroLista(val una: String, val varias: String) {
    APROBADAS("aprobada", "aprobadas"),
    CURSANDO("cursando", "cursando"),
    DISPONIBLES("disponible", "disponibles"),
    BLOQUEADAS("bloqueada", "bloqueadas"),
}

fun filtroDe(s: Situacion): FiltroLista = when (s) {
    Situacion.HECHA -> FiltroLista.APROBADAS
    Situacion.CURSANDO -> FiltroLista.CURSANDO
    Situacion.INSCRIBIBLE -> FiltroLista.DISPONIBLES
    Situacion.PROXIMA, Situacion.LEJANA -> FiltroLista.BLOQUEADAS
}

/** El color con el que se lee un texto de la lista; la vista lo traduce a la paleta */
enum class Tono { TENUE, VERDE, AMBAR, AZUL }

fun tonoDe(f: FiltroLista): Tono = when (f) {
    FiltroLista.APROBADAS -> Tono.VERDE
    FiltroLista.CURSANDO -> Tono.AMBAR
    FiltroLista.DISPONIBLES -> Tono.AZUL
    FiltroLista.BLOQUEADAS -> Tono.TENUE
}

/*
 * Las filas son datos planos (texto, números y enums) para que Compose las
 * compare por valor: marcar una materia cambia su fila y las demás, iguales,
 * no se vuelven a pintar.
 */

/** Una materia: nombre; debajo, sus UC y, si trae un dato, la nota (vacía cuando el icono ya lo dice todo) */
data class FilaMateria(
    val codigo: String,
    val nombre: String,
    val uc: Int?,
    val situacion: Situacion,
    val nota: String,
    val tono: Tono,
) {
    /** Lo que lee el lector de pantalla: la fila ya no escribe su estado, lo dice el icono */
    val descripcion: String
        get() = when (situacion) {
            Situacion.HECHA -> "$nombre, Aprobada"
            Situacion.CURSANDO -> "$nombre, Cursando"
            Situacion.INSCRIBIBLE -> "$nombre, Puedes inscribirla"
            Situacion.PROXIMA, Situacion.LEJANA -> if (nota.isEmpty()) "$nombre, Bloqueada" else "$nombre, Bloqueada. $nota"
        }
}

/** Una casilla de electiva sin elegir: se toca para elegir */
data class FilaCasilla(
    val casilla: String,
    val nombre: String,
    /** "Elige 1 de 14" */
    val nota: String,
)

/** Un grupo plegable: un semestre o, donde no hay ruta de electivas, un grupo de ellas */
data class SeccionLista(
    val clave: String,
    /** null en los grupos de electivas: su aro no aprueba nada */
    val semestre: Int?,
    val titulo: String,
    /** El aro: lo aprobado y lo que cursas, de 0 a 1 */
    val aprobado: Float,
    val cursado: Float,
    val completo: Boolean,
    val resumen: String,
    val tonoResumen: Tono,
    /** " · 2 cursando", en ámbar detrás del resumen; vacío si no toca */
    val coletilla: String,
    val materias: List<FilaMateria>,
    val casillas: List<FilaCasilla>,
) {
    val vacia: Boolean get() = materias.isEmpty() && casillas.isEmpty()
}

data class ModeloLista(
    val secciones: List<SeccionLista>,
    /** Cuántas hay de cada estado, sin contar las filas que el filtro retiene un momento */
    val cuentas: Map<FiltroLista, Int>,
    /** El semestre por el que vas: el primero con alguna obligatoria sin aprobar */
    val actual: Int?,
)

/**
 * Lo que dice una materia tras sus UC. El estado ya lo cuentan el icono y el
 * color, así que la nota solo lleva un dato: lo que abre, lo que pide o cuándo.
 */
fun notaDe(situacion: Situacion, faltan: List<String>, abre: Int = 0): Pair<String, Tono> {
    val quien = when {
        faltan.isEmpty() -> ""
        faltan.size == 1 -> faltan[0]
        else -> "${faltan[0]} y ${faltan.size - 1} más"
    }
    return when (situacion) {
        Situacion.HECHA, Situacion.CURSANDO -> "" to Tono.TENUE
        Situacion.INSCRIBIBLE -> (if (abre > 0) "Abre $abre" else "") to Tono.AZUL
        Situacion.PROXIMA -> (if (quien.isEmpty()) "El próximo semestre" else "Pide $quien") to Tono.TENUE
        Situacion.LEJANA -> (if (quien.isEmpty()) "" else "Pide $quien") to Tono.TENUE
    }
}

/** El siguiente paso de un toque: sin cursar, cursando, aprobada y vuelta a empezar */
fun siguienteMarca(marca: Marca?): Marca? = when (marca) {
    null -> Marca.CURSANDO
    Marca.CURSANDO -> Marca.APROBADA
    Marca.APROBADA -> null
}

/**
 * Arma la lista. `nodos` son las materias del mapa, casillas incluidas (las de
 * los semestres y las de la franja); `enCasilla` dice qué electiva se eligió
 * en cada casilla. Con un `filtro`, cada grupo se queda con lo de ese estado;
 * `fijadas` retiene en su grupo de antes la fila que acabas de marcar, para
 * que no se vaya de debajo del dedo mientras puedes deshacerlo.
 */
fun modeloLista(
    nodos: List<Asignatura>,
    grupos: List<Grupo>,
    enCasilla: (String) -> Asignatura?,
    estados: Map<String, EstadoMateria>,
    situaciones: Map<String, Situacion>,
    nombreDe: (String) -> String?,
    filtro: FiltroLista?,
    fijadas: Map<String, FiltroLista> = emptyMap(),
): ModeloLista {
    val cuentas = FiltroLista.entries.associateWith { 0 }.toMutableMap()

    fun fila(m: Asignatura): FilaMateria {
        val s = situaciones[m.codigo] ?: Situacion.LEJANA
        val faltan = m.prerrequisitos.filter { estados[it] != EstadoMateria.APROBADA }.map { nombreDe(it) ?: it }
        val abre = nodos.count { o -> m.codigo in o.prerrequisitos && estados[o.codigo] != EstadoMateria.APROBADA }
        val (nota, tono) = notaDe(s, faltan, abre)
        return FilaMateria(m.codigo, m.nombre, m.uc, s, nota, tono)
    }

    fun pasa(f: FilaMateria) = filtro == null || (fijadas[f.codigo] ?: filtroDe(f.situacion)) == filtro

    /** Con filtro, el subtítulo dice cuántas quedan de ese estado: así el icono de la pastilla tiene nombre */
    fun resumenFiltrado(filas: List<FilaMateria>, f: FiltroLista): String {
        val uc = filas.sumOf { it.uc ?: 0 }
        return "${filas.size} ${if (filas.size == 1) f.una else f.varias} · $uc UC"
    }

    val secciones = mutableListOf<SeccionLista>()
    val porSemestre = nodos.filter { it.semestre != null }.groupBy { it.semestre!! }.toSortedMap()
    var actual: Int? = null

    for ((s, delSemestre) in porSemestre) {
        val obligatorias = delSemestre.filter { !it.esHueco }
        val huecos = delSemestre.filter { it.esHueco }
        if (actual == null && obligatorias.any { estados[it.codigo] != EstadoMateria.APROBADA }) actual = s
        // Primero las obligatorias, en el orden del pensum; las electivas, detrás
        val todas = (obligatorias + huecos.mapNotNull { enCasilla(it.codigo) }).map(::fila)
        todas.forEach { cuentas[filtroDe(it.situacion)] = cuentas.getValue(filtroDe(it.situacion)) + 1 }
        val vacias = huecos.filter { enCasilla(it.codigo) == null }

        val total = delSemestre.size
        val hechas = todas.count { it.situacion == Situacion.HECHA }
        val cursando = todas.count { it.situacion == Situacion.CURSANDO }
        val completo = total > 0 && hechas == total
        val materias = todas.filter(::pasa)
        val (resumen, tono) = when {
            filtro != null -> resumenFiltrado(materias, filtro) to tonoDe(filtro)
            completo -> "Completo" to Tono.VERDE
            vacias.isNotEmpty() && hechas + vacias.size == total ->
                (if (vacias.size > 1) "Faltan ${vacias.size} electivas" else "Falta la electiva") to Tono.TENUE
            else -> "$hechas de $total aprobadas" to Tono.TENUE
        }
        secciones += SeccionLista(
            clave = "s$s",
            semestre = s,
            titulo = "Semestre $s",
            aprobado = if (total > 0) hechas.toFloat() / total else 0f,
            cursado = if (total > 0) cursando.toFloat() / total else 0f,
            completo = completo,
            resumen = resumen,
            tonoResumen = tono,
            coletilla = if (filtro == null && !completo && cursando > 0) " · $cursando cursando" else "",
            materias = materias,
            // Elegir es una acción, no un estado: con filtro no tiene a qué grupo ir
            casillas = if (filtro == null) vacias.map { FilaCasilla(it.codigo, it.nombre, opcionesDe(it, grupos)) } else emptyList(),
        )
    }

    // Donde la universidad no dice cuándo va cada electiva, cada grupo es una sección al final
    val franja = nodos.filter { it.semestre == null && it.esHueco }
    for (g in grupos) {
        val casillas = franja.filter { it.grupo == g.clave }
        if (casillas.isEmpty()) continue
        val elegidas = casillas.mapNotNull { enCasilla(it.codigo) }.map(::fila)
        elegidas.forEach { cuentas[filtroDe(it.situacion)] = cuentas.getValue(filtroDe(it.situacion)) + 1 }
        val vacias = casillas.filter { enCasilla(it.codigo) == null }
        val ucHechas = elegidas.filter { it.situacion == Situacion.HECHA }.sumOf { it.uc ?: 0 }
        val ucCursando = elegidas.filter { it.situacion == Situacion.CURSANDO }.sumOf { it.uc ?: 0 }
        val meta = g.cuota
        val completo = meta != null && ucHechas >= meta
        val materias = elegidas.filter(::pasa)
        val (resumen, tono) = when {
            filtro != null -> resumenFiltrado(materias, filtro) to tonoDe(filtro)
            completo -> "Completo" to Tono.VERDE
            meta != null -> "$ucHechas de $meta UC" to Tono.TENUE
            else -> "${elegidas.size} ${if (elegidas.size == 1) "elegida" else "elegidas"}" to Tono.TENUE
        }
        val cursando = elegidas.count { it.situacion == Situacion.CURSANDO }
        secciones += SeccionLista(
            clave = "g-${g.clave}",
            semestre = null,
            titulo = g.titulo.replace(Regex("^Areas\\b"), "Áreas"),
            aprobado = if (meta != null && meta > 0) (ucHechas.toFloat() / meta).coerceAtMost(1f) else 0f,
            cursado = if (meta != null && meta > 0) (ucCursando.toFloat() / meta).coerceAtMost(1f) else 0f,
            completo = completo,
            resumen = resumen,
            tonoResumen = tono,
            coletilla = if (filtro == null && !completo && cursando > 0) " · $cursando cursando" else "",
            materias = materias,
            casillas = if (filtro == null) vacias.map { FilaCasilla(it.codigo, it.nombre, opcionesDe(it, grupos)) } else emptyList(),
        )
    }

    return ModeloLista(
        secciones = if (filtro == null) secciones else secciones.filter { !it.vacia },
        cuentas = cuentas,
        actual = actual,
    )
}

private fun opcionesDe(casilla: Asignatura, grupos: List<Grupo>): String {
    val n = grupos.firstOrNull { it.clave == casilla.grupo }?.asignaturas?.size ?: 0
    return if (n > 0) "Elige 1 de $n" else "Por elegir"
}
