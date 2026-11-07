package mpensum.logica

import mpensum.datos.Asignatura

/*
 * El horario de una carrera (layout/horario.js de la web): las clases de la
 * semana y las reglas que dicen qué toca ahora, dónde hay hueco y cómo
 * conviven dos clases que se pisan. Todo es función pura; la vista solo pinta.
 * El tiempo va en minutos desde medianoche: se compara y se resta sin parsear.
 */

/** La jornada: de seis de la mañana a siete de la tarde, para dibujar y para validar */
const val ABRE = 6 * 60
const val CIERRA = 19 * 60

/** Ninguna clase dura menos de media hora; los −/+ de la hoja van de media en media */
const val MIN_DURACION = 30
const val PASO_HORA = 30

/** Lo que dura una clase nueva si nada la corta: lo que dura casi todo en la UDO */
private const val DURACION_NUEVA = 120

/** Entre dos clases, un hueco más corto que esto no se cuenta como libre */
private const val HUECO_MINIMO = 30

val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
val DIAS_CORTOS = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE")
private val SEMANA_ENTERA = DIAS + listOf("Sábado", "Domingo")

/** Una clase: dónde la puso el estudiante. El día va de 0 (lunes) a 4 (viernes) */
data class Sesion(
    val id: String,
    val codigo: String,
    val dia: Int,
    val inicio: Int,
    val fin: Int,
    val seccion: String = "",
    val aula: String = "",
    val profesor: String = "",
) {
    val duracion: Int get() = fin - inicio
}

/** Una fila guardada solo entra si describe una clase posible: lo imposible se descarta, no se corrige */
fun esValida(s: Sesion): Boolean =
    s.dia in DIAS.indices && s.inicio >= ABRE && s.fin <= CIERRA && s.fin > s.inicio

/** El momento del teléfono: día de la semana (0 lunes … 6 domingo) y minuto del día */
data class Ahora(val dia: Int, val minuto: Int)

private val porInicio = compareBy<Sesion>({ it.inicio }, { it.fin })

/** Las clases de cada día, de lunes a domingo (sábado y domingo siempre vacíos), en orden */
fun porDia(sesiones: List<Sesion>): List<List<Sesion>> =
    List(7) { d -> sesiones.filter { it.dia == d }.sortedWith(porInicio) }

/* ---- Horas dichas para leer de reojo ------------------------------------ */

fun meridiano(min: Int) = if (min < 12 * 60) "AM" else "PM"

private fun hora12(min: Int) = ((min / 60 + 11) % 12) + 1

/** "7:00" de "7:00 AM": la hora grande, el meridiano aparte y pequeño */
fun horaDe(min: Int) = "${hora12(min)}:${(min % 60).toString().padStart(2, '0')}"

/** "12:00 PM" */
fun enDoceHoras(min: Int) = "${horaDe(min)} ${meridiano(min)}"

/** Hora en punto sin ceros, para el riel de la rejilla: "7 AM" */
fun horaEnPunto(min: Int) = "${hora12(min)} ${meridiano(min)}"

/** Un tramo en corto, como Calendario de Apple: "7 – 9 AM", "11 AM – 1 PM", "8:15 – 9:50 AM" */
fun tramoCorto(inicio: Int, fin: Int): String {
    fun h(m: Int) = if (m % 60 != 0) horaDe(m) else hora12(m).toString()
    return if (meridiano(inicio) == meridiano(fin)) "${h(inicio)} – ${h(fin)} ${meridiano(fin)}"
    else "${h(inicio)} ${meridiano(inicio)} – ${h(fin)} ${meridiano(fin)}"
}

/** Cuánto dura algo: "1 h 40 min", "2 h", "30 min" */
fun duracion(min: Int): String {
    val h = min / 60
    val r = min % 60
    return when {
        h > 0 && r > 0 -> "$h h $r min"
        h > 0 -> "$h h"
        else -> "$r min"
    }
}

fun clases(n: Int) = if (n == 1) "1 clase" else "$n clases"

/** "10 clases · 20 h a la semana" */
fun resumenSemana(sesiones: List<Sesion>) = "${clases(sesiones.size)} · ${duracion(sesiones.sumOf { it.duracion })} a la semana"

/** "Lunes y miércoles", "Lunes, martes y jueves" */
fun juntarDias(dias: List<Int>): String {
    val nombres = dias.sorted().distinct().mapIndexed { i, d -> if (i == 0) SEMANA_ENTERA[d] else SEMANA_ENTERA[d].lowercase() }
    return if (nombres.size < 2) nombres.firstOrNull().orEmpty()
    else nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
}

/**
 * Cuándo es una clase, juntando los días en que se repite a la misma hora:
 * "Lunes y miércoles · 7 – 9 AM". Si cada día va a una hora, solo el suyo.
 */
fun cuandoEs(sesion: Sesion, todas: List<Sesion>): String {
    val hermanas = todas.filter { it.codigo == sesion.codigo }
    val igual = hermanas.size > 1 && hermanas.all { it.inicio == sesion.inicio && it.fin == sesion.fin }
    val dias = if (igual) juntarDias(hermanas.map { it.dia }) else SEMANA_ENTERA[sesion.dia]
    return "$dias · ${tramoCorto(sesion.inicio, sesion.fin)}"
}

/* ---- Convivencia ---------------------------------------------------------- */

fun solapan(a: Sesion, b: Sesion) = a.dia == b.dia && a.inicio < b.fin && b.inicio < a.fin

/** La primera clase con la que choca una candidata; se ignora a sí misma por id para poder editarla sin moverla */
/** Los pares que se pisan: primero la que empieza antes (a igual hora, la que acaba antes) y después la que se mete encima */
fun choquesDe(sesiones: List<Sesion>): List<Pair<Sesion, Sesion>> {
    val orden = sesiones.sortedWith(porInicio)
    return orden.flatMapIndexed { i, a -> orden.drop(i + 1).filter { solapan(a, it) }.map { a to it } }
}

/** Las leídas que no repiten una clase ya guardada (misma materia, día y horas): leer dos veces la misma captura no la duplica */
fun sinRepetidas(leidas: List<Sesion>, guardadas: List<Sesion>): List<Sesion> =
    leidas.filter { l -> guardadas.none { it.codigo == l.codigo && it.dia == l.dia && it.inicio == l.inicio && it.fin == l.fin } }

/** Los choques de una importación: entre las nuevas y de cada nueva con lo guardado (primero la que ya estaba) */
fun choquesAlImportar(nuevas: List<Sesion>, guardadas: List<Sesion>): List<Pair<Sesion, Sesion>> =
    choquesDe(nuevas) + nuevas.flatMap { n -> guardadas.filter { solapan(it, n) }.map { it to n } }

/** Una materia leída de la captura, con todas sus clases de la semana */
data class MateriaLeida(val codigo: String, val sesiones: List<Sesion>)

/** El día del mes (1 a 31) de un día contado desde 1970-01-01, sin depender de la plataforma */
fun diaDelMes(epoca: Long): Int {
    val z = epoca + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    return (doy - (153 * mp + 2) / 5 + 1).toInt()
}

fun choqueCon(sesiones: List<Sesion>, candidata: Sesion): Sesion? =
    sesiones.firstOrNull { it.id != candidata.id && solapan(it, candidata) }

/** Una clase en su columna: en qué carril va y entre cuántos se reparte el ancho */
data class Colocada(val sesion: Sesion, val carril: Int, val carriles: Int)

/**
 * Reparte en carriles las clases de un día que se pisan. Las que se tocan en
 * cadena forman un racimo y comparten su ancho; cada una toma el primer carril
 * que ya quedó libre. Una clase sola ocupa la columna entera.
 */
fun enCarriles(delDia: List<Sesion>): List<Colocada> {
    val salida = mutableListOf<Colocada>()
    var racimo = mutableListOf<Sesion>()
    var finRacimo = -1
    fun cerrar() {
        val finDeCarril = mutableListOf<Int>()
        val carrilDe = racimo.map { s ->
            var c = finDeCarril.indexOfFirst { it <= s.inicio }
            if (c < 0) {
                c = finDeCarril.size
                finDeCarril += 0
            }
            finDeCarril[c] = s.fin
            c
        }
        racimo.forEachIndexed { i, s -> salida += Colocada(s, carrilDe[i], finDeCarril.size) }
        racimo = mutableListOf()
    }
    for (s in delDia.sortedWith(porInicio)) {
        if (racimo.isNotEmpty() && s.inicio >= finRacimo) cerrar()
        racimo += s
        finRacimo = maxOf(finRacimo, s.fin)
    }
    if (racimo.isNotEmpty()) cerrar()
    return salida
}

/* ---- El día como agenda -------------------------------------------------- */

sealed interface Renglon {
    data class Clase(val sesion: Sesion) : Renglon
    data class Libre(val inicio: Int, val fin: Int) : Renglon
}

/**
 * Un día como se lee en una agenda: sus clases en orden y, entre una y la
 * siguiente, lo que queda libre. Dos que se pisan no dejan renglón: el libre
 * empieza donde acaba la que acaba más tarde.
 */
fun agendaDe(delDia: List<Sesion>): List<Renglon> {
    val renglones = mutableListOf<Renglon>()
    var ocupadoHasta: Int? = null
    for (s in delDia.sortedWith(porInicio)) {
        val hasta = ocupadoHasta
        if (hasta != null && s.inicio - hasta >= HUECO_MINIMO) renglones += Renglon.Libre(hasta, s.inicio)
        renglones += Renglon.Clase(s)
        ocupadoHasta = maxOf(hasta ?: s.fin, s.fin)
    }
    return renglones
}

/**
 * De qué hora a qué hora va la rejilla: de la primera clase a la última, en
 * horas enteras, y no la jornada entera, que dejaría media tarde vacía. Abarca
 * al menos `minimo`, sin salirse de la jornada.
 */
fun rangoDeClases(sesiones: List<Sesion>, minimo: Int = 4 * 60): Pair<Int, Int> {
    if (sesiones.isEmpty()) return 7 * 60 to 7 * 60 + minimo
    var desde = sesiones.minOf { it.inicio } / 60 * 60
    var hasta = (sesiones.maxOf { it.fin } + 59) / 60 * 60
    if (hasta - desde < minimo) {
        hasta = minOf(CIERRA, desde + minimo)
        desde = maxOf(ABRE, hasta - minimo)
    }
    return desde to hasta
}

/**
 * Qué horas de la rejilla, de `abre` a `cierra`, tienen alguna clase en
 * cualquier día. Las que no se pliegan: una hora vacía en toda la semana (el
 * almuerzo) no merece el alto de una con clase. Ninguna clase cruza una hora
 * plegada, así que dentro de un bloque el tiempo sigue siendo lineal.
 */
fun horasOcupadas(sesiones: List<Sesion>, abre: Int, cierra: Int): List<Boolean> =
    List((cierra - abre) / 60) { i ->
        val h = abre + i * 60
        sesiones.any { it.inicio < h + 60 && h < it.fin }
    }

/** "6 materias": lo primero que dice el resumen de la semana, porque es lo que se eligió */
fun materias(n: Int) = if (n == 1) "1 materia" else "$n materias"

/**
 * La franja que se propone para una clase nueva que empieza en `inicio`: dos
 * horas, recortadas por la clase siguiente del día y por el cierre, para que
 * crear desde un hueco libre no produzca un choque.
 */
fun franjaNueva(delDia: List<Sesion>, inicio: Int): Pair<Int, Int> {
    val desde = inicio.coerceIn(ABRE, CIERRA - MIN_DURACION)
    val siguiente = delDia.filter { it.inicio >= desde }.minOfOrNull { it.inicio } ?: CIERRA
    val hasta = minOf(desde + DURACION_NUEVA, siguiente, CIERRA)
    return desde to maxOf(hasta, desde + MIN_DURACION)
}

/** Dónde empieza «Añadir clase» de un día: detrás de la última, o a las siete si no hay ninguna */
fun inicioParaAnadir(delDia: List<Sesion>): Int =
    minOf(delDia.maxOfOrNull { it.fin } ?: (7 * 60), 17 * 60)

/** Un id que no choca con ninguno: legible, y estable para las pruebas */
fun idNuevo(codigo: String, dia: Int, inicio: Int, ocupados: Set<String>): String {
    var id = "$codigo-$dia-$inicio"
    while (id in ocupados) id += "-b"
    return id
}

/* ---- Ahora y después ------------------------------------------------------ */

/** La primera clase de los días que vienen y a cuántos días está; da la vuelta a la semana */
data class Proxima(val sesion: Sesion, val dentroDe: Int)

sealed interface Momento {
    /** Hay una clase a esta hora */
    data class EnCurso(val clase: Sesion, val avance: Float, val quedan: Int, val despues: Sesion?) : Momento
    /** Ahora no, pero hoy queda alguna */
    data class Luego(val clase: Sesion, val faltan: Int, val despues: Sesion?) : Momento
    /** Hoy ya no queda ninguna: se acabaron (`terminado`) o no había */
    data class Libre(val terminado: Boolean, val proxima: Proxima?) : Momento
    /** El horario no tiene ninguna clase */
    data object Vacio : Momento
}

/** Qué toca ahora y qué viene después (momentoDe de la web) */
fun momentoDe(dias: List<List<Sesion>>, ahora: Ahora): Momento {
    if (dias.all { it.isEmpty() }) return Momento.Vacio
    val hoy = dias.getOrNull(ahora.dia).orEmpty().sortedWith(porInicio)
    val porVenir = hoy.filter { it.inicio > ahora.minuto }
    val actual = hoy.firstOrNull { it.inicio <= ahora.minuto && ahora.minuto < it.fin }
    if (actual != null) {
        return Momento.EnCurso(
            clase = actual,
            avance = (ahora.minuto - actual.inicio).toFloat() / actual.duracion,
            quedan = actual.fin - ahora.minuto,
            despues = porVenir.firstOrNull(),
        )
    }
    if (porVenir.isNotEmpty()) return Momento.Luego(porVenir[0], porVenir[0].inicio - ahora.minuto, porVenir.getOrNull(1))
    var proxima: Proxima? = null
    for (k in 1..7) {
        val delDia = dias.getOrNull((ahora.dia + k) % 7).orEmpty()
        if (delDia.isNotEmpty()) {
            proxima = Proxima(delDia.sortedWith(porInicio).first(), k)
            break
        }
    }
    return Momento.Libre(terminado = hoy.isNotEmpty(), proxima = proxima)
}

/** El nombre del día de la semana, de 0 (lunes) a 6 (domingo) */
fun nombreDia(dia: Int): String = SEMANA_ENTERA.getOrElse(dia) { "" }

/** "Mañana", "El miércoles", "El próximo lunes" */
fun cuandoViene(p: Proxima): String = when (p.dentroDe) {
    1 -> "Mañana"
    7 -> "El próximo ${SEMANA_ENTERA[p.sesion.dia].lowercase()}"
    else -> "El ${SEMANA_ENTERA[p.sesion.dia].lowercase()}"
}

/** El título de un día sin más clases */
fun tituloLibre(m: Momento.Libre, ahora: Ahora): String = when {
    m.terminado -> "Listo por hoy"
    ahora.dia < 5 -> "Sin clases hoy"
    else -> "Fin de semana"
}

/** Dónde es y con quién, solo lo que se sabe: "Aula 12 · Sec. 01" */
fun lugarDe(s: Sesion, conProfesor: Boolean = false): String =
    listOfNotNull(s.aula.ifBlank { null }, s.seccion.ifBlank { null }?.let { "Sec. $it" }, s.profesor.ifBlank { null }?.takeIf { conProfesor })
        .joinToString(" · ")

/* ---- Qué materias ofrecer al crear una clase ----------------------------- */

/**
 * Las materias para elegir, en el orden en que se buscan: las que ya están en
 * el horario y las que cursas, luego las que puedes inscribir y al final el
 * resto. Las aprobadas no se ofrecen: nadie las vuelve a poner en su semana.
 */
fun materiasParaElegir(todas: List<Asignatura>, situaciones: Map<String, Situacion>, enHorario: Set<String>): List<Asignatura> {
    fun rango(a: Asignatura): Int = when {
        a.codigo in enHorario || situaciones[a.codigo] == Situacion.CURSANDO -> 0
        situaciones[a.codigo] == Situacion.INSCRIBIBLE -> 1
        situaciones[a.codigo] == Situacion.HECHA -> 3
        else -> 2
    }
    return todas.filter { !it.esHueco && !it.esComodin && rango(it) < 3 }
        .sortedWith(compareBy({ rango(it) }, { it.semestre ?: 99 }))
}

/* ---- Nombres que caben en un bloque -------------------------------------- */

private val ENLACES = setOf("de", "del", "la", "las", "los", "el", "y", "e", "en", "a", "al", "para", "con", "por")
private const val VOCALES = "aeiouáéíóúü"

/** Lo que va pegado a la palabra anterior: "II" no se queda solo en un renglón */
private val ROMANO = Regex("^[IVX]+$|^[0-9]+$")
private const val PEGADO = '\u00A0'

/** Las abreviaturas de una palabra, de la más larga a la más corta: cortan siempre antes de una vocal ("Estadíst.", "Estad.") */
private fun abreviaturas(p: String): List<String> {
    if (PEGADO in p) {
        val base = p.substringBefore(PEGADO)
        return abreviaturas(base).map { it + PEGADO + p.substringAfter(PEGADO) }
    }
    if (p.length <= 4 || p.any { it.isDigit() } || p.all { it.isUpperCase() }) return emptyList()
    return (p.length - 1 downTo 3).filter { i ->
        p[i].lowercaseChar() in VOCALES && p[i - 1].isLetter() && p[i - 1].lowercaseChar() !in VOCALES
    }.map { p.take(it) + "." }
}

/** Reparte palabras en renglones sin partir ninguna; null si alguna no cabe sola o no alcanzan los renglones */
private fun envolver(palabras: List<String>, lineas: Int, cabe: (String) -> Boolean): List<String>? {
    val salida = mutableListOf<String>()
    var actual = ""
    for (p in palabras) {
        if (!cabe(p)) return null
        val junto = if (actual.isEmpty()) p else "$actual $p"
        if (cabe(junto)) actual = junto
        else {
            salida += actual
            actual = p
        }
    }
    if (actual.isNotEmpty()) salida += actual
    // Una «y» o un «de» solo en su renglón no dice nada y parece un error: mejor probar lo siguiente
    return salida.takeIf { it.size <= lineas && (it.size == 1 || it.none { r -> r.lowercase() in ENLACES }) }
}

/**
 * El nombre de una materia en un bloque estrecho de la rejilla, en `lineas`
 * renglones como mucho, sin partir palabras ni poner guiones. Por orden: el
 * nombre entero; las palabras que no caben, abreviadas; sin las palabras de
 * enlace; abreviando más; y si aun así no cabe, los primeros renglones.
 * `cabe` dice si un texto entra en el ancho del bloque.
 */
fun abreviarNombre(nombre: String, lineas: Int, cabe: (String) -> Boolean): List<String> {
    val palabras = mutableListOf<String>()
    for (p in nombre.split(' ').filter { it.isNotBlank() }) {
        if (palabras.isNotEmpty() && ROMANO.matches(p)) palabras[palabras.lastIndex] += PEGADO + p else palabras += p
    }
    return abreviarPalabras(palabras, lineas) { cabe(it.replace(PEGADO, ' ')) }.map { it.replace(PEGADO, ' ') }
}

private fun abreviarPalabras(palabras: List<String>, lineas: Int, cabe: (String) -> Boolean): List<String> {
    envolver(palabras, lineas, cabe)?.let { return it }

    // Cada palabra que no entra sola, en su abreviatura más larga que sí entra
    fun ajustada(p: String) = if (cabe(p)) p else abreviaturas(p).firstOrNull(cabe) ?: abreviaturas(p).lastOrNull() ?: p
    val ajustadas = palabras.map(::ajustada)
    envolver(ajustadas, lineas, cabe)?.let { return it }

    // Sin enlaces en medio ("Teoría Sistemas"), que no dicen nada en un bloque
    val sinEnlaces = ajustadas.filterIndexed { i, p -> i == 0 || p.lowercase() !in ENLACES }.toMutableList()
    envolver(sinEnlaces, lineas, cabe)?.let { return it }

    // Abreviando el resto, de la palabra más larga a la más corta, a su forma más breve. En un
    // solo renglón no: ahí se lee mejor la primera palabra entera ("Circuitos") que dos mochas
    val orden = if (lineas > 1) sinEnlaces.indices.sortedByDescending { sinEnlaces[it].length } else emptyList()
    for (i in orden) {
        val corta = abreviaturas(sinEnlaces[i]).lastOrNull() ?: continue
        sinEnlaces[i] = corta
        envolver(sinEnlaces, lineas, cabe)?.let { return it }
    }

    // Los primeros renglones; lo que queda fuera se pierde entero, nunca a mitad de palabra
    val salida = mutableListOf<String>()
    var actual = ""
    for (p in sinEnlaces) {
        val junto = if (actual.isEmpty()) p else "$actual $p"
        if (actual.isEmpty() || cabe(junto)) actual = junto
        else {
            salida += actual
            if (salida.size == lineas) return salida
            actual = p
        }
    }
    if (actual.isNotEmpty() && salida.size < lineas) salida += actual
    return salida
}
