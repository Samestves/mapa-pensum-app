package mpensum.logica

import mpensum.datos.Asignatura

/*
 * Lo que la lectura devuelve es texto que alguien (o algo) leyó de una foto
 * (layout/importarHorario.js de la web). Aquí no se confía en nada: cada fila
 * se comprueba contra el pensum de LA carrera abierta y contra las reglas de la
 * rejilla, y la que no cuadre sale marcada, no corregida a la brava. Todo es
 * función pura: se prueba sin red.
 */

/** Una fila tal como la devuelve el servidor: todo texto, y todo puede faltar */
data class FilaCruda(
    val codigo: String = "",
    val nombre: String = "",
    val dia: String = "",
    val inicio: String = "",
    val fin: String = "",
    val seccion: String = "",
    val aula: String = "",
    val profesor: String = "",
)

/** Lo que se leyó, crudo: es lo que permite comparar la fila con la foto */
data class Leido(val nombre: String, val dia: String, val horas: String)

/**
 * Una fila entendida: con qué materia se emparejó y qué le pasa. Los avisos:
 *   "sin-materia"  no se pudo emparejar con ninguna del pensum
 *   "sin-dia"      el día no se entendió o cae fuera de lunes a viernes
 *   "sin-hora"     falta una hora, o el fin no va después del inicio
 *   "fuera"        la clase cae fuera de la jornada de la rejilla
 *   "corta"        dura menos que el mínimo
 *   "choca"        se pisa con otra clase, leída o ya guardada
 */
data class Candidata(
    val id: String,
    val leido: Leido,
    val codigo: String?,
    val materia: Asignatura?,
    val via: String?,
    val dia: Int?,
    val inicio: Int?,
    val fin: Int?,
    val seccion: String,
    val aula: String,
    val profesor: String,
    val avisos: List<String>,
    val incluir: Boolean,
    val grupo: String,
    val dudosa: Boolean = false,
)

private val DIAS_ALIAS = listOf(
    listOf("lunes", "lun", "monday", "mon", "l"),
    listOf("martes", "mar", "tuesday", "tue", "m"),
    listOf("miercoles", "mie", "wednesday", "wed", "x"),
    listOf("jueves", "jue", "thursday", "thu", "j"),
    listOf("viernes", "vie", "friday", "fri", "v"),
)

private val ROMANOS = mapOf("i" to 1, "ii" to 2, "iii" to 3, "iv" to 4, "v" to 5, "vi" to 6, "vii" to 7, "viii" to 8, "ix" to 9, "x" to 10)

private const val CON_TILDE = "áàâäãéèêëíìîïóòôöõúùûüñç"
private const val SIN_TILDE = "aaaaaeeeeiiiiooooouuuunc"

/** Sin tildes y en minúsculas (sinTildes de la web): la ñ también pierde su virgulilla, como en NFD */
fun sinTildes(texto: String): String = texto.lowercase().map { c ->
    val i = CON_TILDE.indexOf(c)
    if (i >= 0) SIN_TILDE[i] else c
}.joinToString("")

/** Sin tildes, en minúsculas y sin espacios en los bordes: lo que llega de una foto trae de todo */
private fun normalizar(t: String?) = sinTildes(t.orEmpty()).trim()

/** El día como índice 0..4, o null si no se reconoce */
fun aDia(crudo: String?): Int? {
    val t = normalizar(crudo).filter { it in 'a'..'z' }
    if (t.isEmpty()) return null

    val exacto = DIAS_ALIAS.indexOfFirst { t in it }
    if (exacto >= 0) return exacto

    // "miercoles" mal escrito, "juev", "vier": por prefijo, que es como se abrevian
    val porPrefijo = DIAS_ALIAS.indexOfFirst { it[0].startsWith(t) || t.startsWith(it[0]) }
    return if (porPrefijo >= 0) porPrefijo else null
}

private val HORA = Regex("^(\\d{1,2})[:h]?(\\d{2})?\\s*(am|pm)?$")

/**
 * La hora como minutos desde medianoche, o null. Acepta "7:00", "07:00",
 * "7:00 AM", "1:40 PM" y "7.00". Sin meridiano nada cae antes de ABRE: "5:30"
 * se lee como las 17:30; para una hora madrugadora hay que escribir "5:30 AM".
 */
fun aHora(crudo: String?): Int? {
    val t = normalizar(crudo).replace(".", "")
    if (t.isEmpty()) return null

    val partes = HORA.find(t) ?: return null
    var h = partes.groupValues[1].toInt()
    val min = partes.groupValues[2].ifEmpty { "0" }.toInt()
    if (min > 59 || h > 23) return null

    val tarde = partes.groupValues[3] == "pm"
    val manana = partes.groupValues[3] == "am"

    if (tarde && h < 12) h += 12
    else if (manana && h == 12) h = 0
    // Sin meridiano, una hora que caería antes de que abra la facultad es de la tarde
    else if (!tarde && !manana && h * 60 + min < ABRE) h += 12

    val total = h * 60 + min
    return if (total < 24 * 60) total else null
}

/** El nivel de una materia: el I, II o III del final; es lo único que separa "Matemática I" de "Matemática II" */
private fun nivelDe(trozos: List<String>): Int? {
    val ultimo = trozos.lastOrNull() ?: return null
    ROMANOS[ultimo]?.let { return it }
    return ultimo.toIntOrNull()?.takeIf { ultimo.all { c -> c in '0'..'9' } }
}

private val RUIDO = setOf("de", "del", "la", "el", "los", "las", "y", "a", "en", "para")

private fun trocear(nombre: String): List<String> =
    normalizar(nombre).map { if (it in 'a'..'z' || it in '0'..'9' || it.isWhitespace()) it else ' ' }
        .joinToString("").split(Regex("\\s+")).filter { it.isNotEmpty() }

/**
 * Cuánto se parecen dos nombres de materia, de 0 a 1. Se cuentan las palabras
 * en común y se mide contra LAS DOS listas, no contra la más corta: así
 * "Física I" no empata con "Laboratorio de Física I". El nivel es un veto: si
 * los dos lo declaran y no coincide, es 0.
 */
fun parecido(a: String, b: String): Double {
    val ta = trocear(a)
    val tb = trocear(b)
    if (ta.isEmpty() || tb.isEmpty()) return 0.0

    val na = nivelDe(ta)
    val nb = nivelDe(tb)
    if (na != null && nb != null && na != nb) return 0.0

    fun utiles(t: List<String>) = if (t.any { it !in RUIDO }) t.filter { it !in RUIDO } else t
    val ua = utiles(ta)
    val ub = utiles(tb)

    // Una palabra corta se compara entera; desde tres letras vale que una empiece por la otra ("ING" por "Inglés")
    val resto = ub.toMutableList()
    var comunes = 0
    for (p in ua) {
        val i = resto.indexOfFirst { q -> q == p || (p.length >= 3 && q.length >= 3 && (q.startsWith(p) || p.startsWith(q))) }
        if (i >= 0) {
            comunes++
            resto.removeAt(i)
        }
    }
    if (comunes == 0) return 0.0

    val cubreLoLeido = comunes.toDouble() / ua.size
    val cubreLaMateria = comunes.toDouble() / ub.size
    return 2 * cubreLoLeido * cubreLaMateria / (cubreLoLeido + cubreLaMateria)
}

/** Por debajo de esto no se propone nada, y si el segundo candidato está pegado al primero tampoco */
private const val UMBRAL = 0.6
private const val DISTANCIA_MINIMA = 0.15

/** La materia que corresponde a una fila: primero por código (exacto) y si no por nombre */
fun emparejar(fila: FilaCruda, materias: List<Asignatura>): Pair<Asignatura?, String?> {
    if (fila.codigo.isNotBlank()) {
        val buscado = fila.codigo.trim()
        materias.find { it.codigo == buscado }?.let { return it to "codigo" }
    }

    if (fila.nombre.isEmpty()) return null to null

    val puntuadas = materias.map { it to parecido(fila.nombre, it.nombre) }.sortedByDescending { it.second }
    val mejor = puntuadas.firstOrNull()
    val segunda = puntuadas.getOrNull(1)
    if (mejor == null || mejor.second < UMBRAL) return null to null
    if (segunda != null && mejor.second - segunda.second < DISTANCIA_MINIMA) return null to null

    return mejor.first to "nombre"
}

/** Qué le pasa a una fila mirándola sola; el choque no se mira aquí porque depende de las demás */
fun avisosDe(materia: Asignatura?, dia: Int?, inicio: Int?, fin: Int?): List<String> {
    val avisos = mutableListOf<String>()
    if (materia == null) avisos += "sin-materia"
    if (dia == null) avisos += "sin-dia"
    if (inicio == null || fin == null || fin <= inicio) avisos += "sin-hora"
    else {
        if (inicio < ABRE || fin > CIERRA) avisos += "fuera"
        if (fin - inicio < MIN_DURACION) avisos += "corta"
    }
    return avisos
}

/**
 * Convierte lo leído en candidatas revisables: con lo que se entendió, con qué
 * materia se emparejó y qué le pasa a cada una. `yaGuardadas` son las clases
 * que el horario ya tenía, para avisar de los choques con ellas.
 */
fun revisar(filas: List<FilaCruda>, materias: List<Asignatura>, yaGuardadas: List<Sesion> = emptyList()): List<Candidata> {
    val candidatas = filas.mapIndexed { i, fila ->
        val (materia, via) = emparejar(fila, materias)
        val dia = aDia(fila.dia)
        val inicio = aHora(fila.inicio)
        val fin = aHora(fila.fin)
        val avisos = avisosDe(materia, dia, inicio, fin)
        Candidata(
            id = "leida-$i",
            leido = Leido(fila.nombre.trim(), fila.dia.trim(), "${fila.inicio.trim()} – ${fila.fin.trim()}"),
            codigo = materia?.codigo,
            materia = materia,
            via = via,
            dia = dia,
            inicio = inicio,
            fin = fin,
            seccion = fila.seccion.trim(),
            aula = fila.aula.trim(),
            profesor = fila.profesor.trim(),
            avisos = avisos,
            incluir = avisos.isEmpty(),
            grupo = materia?.codigo ?: "leida-$i",
        )
    }
    return marcarChoques(candidatas, yaGuardadas).map { it.copy(dudosa = it.avisos.isNotEmpty()) }
}

private fun solapa(d1: Int?, i1: Int?, f1: Int?, d2: Int?, i2: Int?, f2: Int?): Boolean =
    d1 != null && d1 == d2 && i1 != null && f1 != null && i2 != null && f2 != null && i1 < f2 && i2 < f1

/** Vuelve a repasar los choques de toda la lista: desmarcar una fila puede liberar a la que chocaba con ella */
fun marcarChoques(candidatas: List<Candidata>, yaGuardadas: List<Sesion> = emptyList()): List<Candidata> {
    val activas = candidatas.filter { it.incluir && it.dia != null && it.inicio != null && it.fin != null }

    return candidatas.map { c ->
        val sinChoque = c.avisos.filter { it != "choca" }
        if (c !in activas) return@map c.copy(avisos = sinChoque)

        val choca = yaGuardadas.any { solapa(it.dia, it.inicio, it.fin, c.dia, c.inicio, c.fin) } ||
            activas.any { it.id != c.id && solapa(it.dia, it.inicio, it.fin, c.dia, c.inicio, c.fin) }
        c.copy(avisos = if (choca) sinChoque + "choca" else sinChoque)
    }
}

/**
 * Las candidatas marcadas y sanas, ya como sesiones del horario. Con
 * `conChoques`, las que solo tienen el aviso de choque también entran: la hoja
 * «Antes de guardar» enseña el choque y deja que se guarde.
 */
fun aSesiones(candidatas: List<Candidata>, conChoques: Boolean = false): List<Sesion> =
    candidatas
        .filter { it.incluir && it.avisos.all { a -> conChoques && a == "choca" } }
        .mapIndexed { i, c -> Sesion("ia-$i", c.codigo.orEmpty(), c.dia ?: 0, c.inicio ?: 0, c.fin ?: 0, c.seccion, c.aula, c.profesor) }
