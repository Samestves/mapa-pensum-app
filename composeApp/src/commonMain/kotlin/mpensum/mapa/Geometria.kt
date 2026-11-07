package mpensum.mapa

import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import kotlin.math.abs
import kotlin.math.max

/*
 * La geometría del mapa, la misma de la web (layout/constantes.js y
 * calcularLayout.js). Todo en unidades del plano: una tarjeta mide 224×96 y
 * la escala la pone quien dibuja. Es función pura: mismas materias, mismas
 * coordenadas, así que se calcula una vez por carrera.
 */

/** La tarjeta de una materia */
object Nodo {
    const val ANCHO = 224.0
    const val ALTO = 96.0
    const val RADIO = 7.0
    const val PAD_IZQ = 18.0
    const val PAD_DER = 16.0
    /** Ancho útil para el nombre */
    const val ANCHO_TEXTO = ANCHO - PAD_IZQ - PAD_DER
}

/** Huecos entre columnas (por donde se rutean los cables) y entre filas */
object Espaciado {
    const val COLUMNA = 150.0
    const val FILA = 26.0
}

object Margen {
    const val ARRIBA = 24.0
    const val DERECHA = 48.0
    const val ABAJO = 72.0
    const val IZQUIERDA = 48.0
}

/** Franja reservada arriba de cada columna para la cabecera del semestre */
const val ALTO_ENCABEZADO = 84.0

/** La misma franja con la cabecera en una línea y la barra debajo: en horizontal cada dp de alto cuenta */
const val ALTO_ENCABEZADO_COMPACTO = 40.0

object TextoTarjeta {
    const val CODIGO = 10.0
    const val NOMBRE = 14.0
    const val META = 10.0
    const val ALTO_LINEA = 16.5
    const val MAX_LINEAS = 3
    /** Línea base de la fila de arriba: código y estado */
    const val LINEA_SUPERIOR = 24.0
    /** Centro vertical del bloque del nombre */
    const val CENTRO_NOMBRE = 55.0
}

/** La franja de electivas, debajo de los semestres, en las carreras sin ruta oficial */
object Franja {
    /** Aire entre el último semestre y la franja: las electivas no son un semestre más */
    const val CORREDOR = 96.0
    /** Del rótulo del grupo a sus casillas */
    const val ENCABEZADO = 48.0
    const val FILA = 26.0
}

/** Una materia colocada en el plano, con su nombre ya partido en líneas */
data class NodoMapa(val asignatura: Asignatura, val x: Double, val y: Double, val lineasNombre: List<String>) {
    val codigo: String get() = asignatura.codigo
    val semestre: Int? get() = asignatura.semestre
}

data class ColumnaMapa(val semestre: Int, val x: Double, val cantidad: Int, val uc: Int)

/** Un trozo del trazado de un cable: se convierte en Path al dibujar */
sealed interface Segmento {
    /** Adonde llega el segmento */
    val x: Double
    val y: Double

    data class Mover(override val x: Double, override val y: Double) : Segmento
    data class Linea(override val x: Double, override val y: Double) : Segmento
    data class Curva(val x1: Double, val y1: Double, val x2: Double, val y2: Double, override val x: Double, override val y: Double) : Segmento
}

/** Un cable de prelación ya ruteado */
data class Arista(
    val origen: String,
    val destino: String,
    /** El color lo pone el área del prerrequisito: se sigue de dónde viene cada rama */
    val area: String?,
    val cruzaColumnas: Boolean,
    val trazo: List<Segmento>,
) {
    val id: String get() = "$origen->$destino"
}

data class LayoutMapa(
    val nodos: List<NodoMapa>,
    val columnas: List<ColumnaMapa>,
    /** Las electivas del catálogo, con su nombre partido, para cuando se colocan en una casilla */
    val electivas: List<NodoMapa>,
    val aristas: List<Arista>,
    val relaciones: Relaciones,
    /** Fondo del último nodo de los semestres */
    val finSemestres: Double,
    val maxFilas: Int,
    val ancho: Double,
    /** Lo que mide la cabecera de los semestres: de ello depende dónde cae la primera fila */
    val altoEncabezado: Double,
    /** Sin la franja de electivas, que añade su propio alto donde la hay */
    val alto: Double,
) {
    val porCodigo: Map<String, NodoMapa> by lazy { (nodos + electivas).associateBy { it.codigo } }
}

/*
 * Ancho medio de un carácter como fracción del tamaño de letra. Es una
 * estimación a propósito, como en la web: medir haría que el layout
 * dependiera de cuándo se monta. Con Jost a peso 380 y 14 px, a 0,50 no se
 * sale ningún nombre de las nueve carreras.
 */
private const val FACTOR_CARACTER = 0.5

private fun anchoAproximado(texto: String, tamano: Double) = texto.length * tamano * FACTOR_CARACTER

/** Parte un nombre en líneas que quepan en la tarjeta; si no cabe, recorta la última con "…" */
fun partirEnLineas(texto: String, anchoDisponible: Double, tamano: Double, maxLineas: Int): List<String> {
    val lineas = mutableListOf<String>()
    var actual = ""
    for (palabra in texto.split(' ')) {
        val tentativa = if (actual.isNotEmpty()) "$actual $palabra" else palabra
        if (anchoAproximado(tentativa, tamano) <= anchoDisponible || actual.isEmpty()) {
            actual = tentativa
        } else {
            lineas += actual
            actual = palabra
        }
    }
    if (actual.isNotEmpty()) lineas += actual
    if (lineas.size <= maxLineas) return lineas

    val recortadas = lineas.take(maxLineas).toMutableList()
    var ultima = recortadas.last()
    while (ultima.length > 1 && anchoAproximado("$ultima…", tamano) > anchoDisponible) ultima = ultima.dropLast(1)
    recortadas[maxLineas - 1] = "$ultima…"
    return recortadas
}

/**
 * La posición de cada materia: X según el semestre (columna) e Y según su
 * orden dentro del semestre (fila), respetando el orden del JSON. El alto de
 * la cabecera entra como parámetro (no hay globales): en horizontal es la compacta.
 */
fun calcularLayout(
    asignaturas: List<Asignatura>,
    grupos: List<Grupo> = emptyList(),
    altoEncabezado: Double = ALTO_ENCABEZADO,
    /** Cables con codos octilineales en vez de curvas */
    rectos: Boolean = false,
): LayoutMapa {
    val semestres = asignaturas.mapNotNull { it.semestre }.distinct().sorted()
    val nodos = mutableListOf<NodoMapa>()
    val columnas = mutableListOf<ColumnaMapa>()
    var maxFilas = 0
    val partir = { nombre: String -> partirEnLineas(nombre, Nodo.ANCHO_TEXTO, TextoTarjeta.NOMBRE, TextoTarjeta.MAX_LINEAS) }

    semestres.forEachIndexed { indice, semestre ->
        val delSemestre = asignaturas.filter { it.semestre == semestre }
        val x = Margen.IZQUIERDA + indice * (Nodo.ANCHO + Espaciado.COLUMNA)
        columnas += ColumnaMapa(semestre, x, delSemestre.size, delSemestre.sumOf { it.uc ?: 0 })
        delSemestre.forEachIndexed { fila, a ->
            nodos += NodoMapa(a, x, Margen.ARRIBA + altoEncabezado + fila * (Nodo.ALTO + Espaciado.FILA), partir(a.nombre))
        }
        maxFilas = max(maxFilas, delSemestre.size)
    }

    val columnasN = max(semestres.size, 1)
    val filasN = max(maxFilas, 1)
    val finSemestres = Margen.ARRIBA + altoEncabezado + filasN * Nodo.ALTO + (filasN - 1) * Espaciado.FILA
    // Las electivas se dibujan en casillas, no como catálogo: sin x ni y propias
    val electivas = grupos.flatMap { g -> g.asignaturas.map { NodoMapa(it.copy(grupo = g.clave), 0.0, 0.0, partir(it.nombre)) } }

    return LayoutMapa(
        nodos = nodos,
        columnas = columnas,
        electivas = electivas,
        // Cero cables hacia las electivas: un cable que bajara desde la malla
        // diría "esta electiva es parte del semestre X", y no lo es
        aristas = generarAristas(nodos, rectos),
        // Las relaciones sí las incluyen: señalar una electiva ilumina lo que pide
        relaciones = construirRelaciones((nodos + electivas).map { it.asignatura }),
        finSemestres = finSemestres,
        maxFilas = maxFilas,
        ancho = Margen.IZQUIERDA + columnasN * Nodo.ANCHO + (columnasN - 1) * Espaciado.COLUMNA + Margen.DERECHA,
        altoEncabezado = altoEncabezado,
        alto = finSemestres + Margen.ABAJO,
    )
}

/* Separación de los puntos de control respecto al borde de la tarjeta */
private const val TENSION = 0.45
private const val TENSION_MINIMA = 44.0
/* Fracción del alto de la tarjeta donde se reparten los puertos: los cables no salen por las esquinas */
private const val ZONA_PUERTOS = 0.46

/** Reparte n puertos a lo alto del borde: uno sale por el centro, varios en abanico */
private fun puerto(indice: Int, total: Int): Double {
    if (total <= 1) return Nodo.ALTO / 2
    val util = Nodo.ALTO * ZONA_PUERTOS
    return (Nodo.ALTO - util) / 2 + util * indice / (total - 1)
}

/** Los pasillos libres de una columna: entre dos tarjetas y bajo la última */
private class Pasillo(val x: Double, val centros: List<Double>) {
    val usados = mutableSetOf<Int>()

    /** El pasillo libre más cercano a la altura ideal del cable */
    fun elegir(yDeseada: Double): Double {
        val orden = centros.withIndex().sortedBy { abs(it.value - yDeseada) }
        val libre = orden.firstOrNull { it.index !in usados } ?: orden.first()
        usados += libre.index
        return libre.value
    }
}

private fun pasillosPorSemestre(nodos: List<NodoMapa>): Map<Int, Pasillo> =
    nodos.filter { it.semestre != null }.groupBy { it.semestre!! }.mapValues { (_, lista) ->
        val ys = lista.map { it.y }.sorted()
        val centros = ys.zipWithNext { a, b -> (a + Nodo.ALTO + b) / 2 } + (ys.last() + Nodo.ALTO + Espaciado.FILA / 2)
        Pasillo(lista.first().x, centros)
    }

private class Cruce(val xEntrada: Double, val xSalida: Double, val y: Double)

/** Curvas suaves entre columnas y tramos rectos por los pasillos: el cable nunca toca una tarjeta */
private fun trazar(x1: Double, y1: Double, x2: Double, y2: Double, cruces: List<Cruce>): List<Segmento> {
    val trazo = mutableListOf<Segmento>(Segmento.Mover(x1, y1))
    var cx = x1
    var cy = y1
    for (c in cruces) {
        val k = max((c.xEntrada - cx) * TENSION, 26.0)
        trazo += Segmento.Curva(cx + k, cy, c.xEntrada - k, c.y, c.xEntrada, c.y)
        trazo += Segmento.Linea(c.xSalida, c.y)
        cx = c.xSalida
        cy = c.y
    }
    val k = max((x2 - cx) * TENSION, TENSION_MINIMA)
    trazo += Segmento.Curva(cx + k, cy, x2 - k, y2, x2, y2)
    return trazo
}

/* El codo octilineal: radio del chaflán de 45°, aire que se deja a las tarjetas y corrimiento del canal por puerto */
private const val CHAFLAN = 16.0
private const val AIRE_CODO = 4.0
private const val CORRIMIENTO_CANAL = 6.0

/**
 * Cuánto se corre el canal vertical de un cable según su puerto, para que los
 * de una misma tarjeta no se monten en el mismo: el de más arriba gira más
 * tarde al bajar y más pronto al subir, y así no se cruzan.
 */
private fun corrimiento(indice: Int, total: Int, bajando: Boolean): Double {
    if (total <= 1) return 0.0
    val k = -CORRIMIENTO_CANAL + 2 * CORRIMIENTO_CANAL * indice / (total - 1)
    return if (bajando) -k else k
}

/**
 * Un tramo recto entre dos puntos con el codo octilineal: horizontal hasta
 * `xm − d`, 45° hasta `y1 ± d`, vertical, 45° y horizontal hasta el final.
 * `d = min(16, |dy| / 2, (x2 − x1) / 2 − 4)`; sin desnivel, una línea.
 */
private fun codo(trazo: MutableList<Segmento>, x1: Double, y1: Double, x2: Double, y2: Double, corrida: Double) {
    val dy = y2 - y1
    val ancho = (x2 - x1) / 2
    val d = minOf(CHAFLAN, abs(dy) / 2, ancho - AIRE_CODO - abs(corrida))
    if (abs(dy) < 1.0 || d <= 0.0) {
        trazo += Segmento.Linea(x2, y2)
        return
    }
    val xm = x1 + ancho + corrida
    val s = if (dy > 0) 1.0 else -1.0
    trazo += Segmento.Linea(xm - d, y1)
    trazo += Segmento.Linea(xm, y1 + s * d)
    trazo += Segmento.Linea(xm, y2 - s * d)
    trazo += Segmento.Linea(xm + d, y2)
    trazo += Segmento.Linea(x2, y2)
}

/**
 * El trazado recto de un cable: un codo por cada salto entre columnas y los
 * mismos tramos rectos por los pasillos. `salida` y `entrada` son el puerto
 * (índice, total) en los dos extremos; el primer salto corre su canal por el
 * puerto de salida y el último por el de entrada.
 */
private fun trazarRecto(x1: Double, y1: Double, x2: Double, y2: Double, cruces: List<Cruce>, salida: Pair<Int, Int>, entrada: Pair<Int, Int>): List<Segmento> {
    val trazo = mutableListOf<Segmento>(Segmento.Mover(x1, y1))
    var cx = x1
    var cy = y1
    for ((i, c) in cruces.withIndex()) {
        val corrida = if (i == 0) corrimiento(salida.first, salida.second, c.y > cy) else 0.0
        codo(trazo, cx, cy, c.xEntrada, c.y, corrida)
        trazo += Segmento.Linea(c.xSalida, c.y)
        cx = c.xSalida
        cy = c.y
    }
    val puerto = if (cruces.isEmpty()) salida else entrada
    codo(trazo, cx, cy, x2, y2, corrimiento(puerto.first, puerto.second, y2 > cy))
    return trazo
}

/** Un cable por cada par prerrequisito → materia, ya ruteado; con `rectos`, de codos octilineales en vez de curvas */
fun generarAristas(nodos: List<NodoMapa>, rectos: Boolean = false): List<Arista> {
    val porCodigo = nodos.associateBy { it.codigo }
    val pasillos = pasillosPorSemestre(nodos)

    // Los cables que salen de un nodo y los que entran, ordenados por la altura
    // del otro extremo: así dos cables del mismo nodo no se cruzan al salir
    val salidas = mutableMapOf<String, MutableList<String>>()
    val entradas = mutableMapOf<String, MutableList<String>>()
    for (destino in nodos) for (pre in destino.asignatura.prerrequisitos) {
        if (pre !in porCodigo) continue
        salidas.getOrPut(pre) { mutableListOf() } += destino.codigo
        entradas.getOrPut(destino.codigo) { mutableListOf() } += pre
    }
    val porAltura = compareBy<String> { porCodigo.getValue(it).y }
    salidas.values.forEach { it.sortWith(porAltura) }
    entradas.values.forEach { it.sortWith(porAltura) }

    val aristas = mutableListOf<Arista>()
    for (destino in nodos) for (pre in destino.asignatura.prerrequisitos) {
        val origen = porCodigo[pre] ?: continue
        val salida = salidas.getValue(pre)
        val entrada = entradas.getValue(destino.codigo)
        val x1 = origen.x + Nodo.ANCHO
        val y1 = origen.y + puerto(salida.indexOf(destino.codigo), salida.size)
        val x2 = destino.x
        val y2 = destino.y + puerto(entrada.indexOf(pre), entrada.size)

        // Un cruce recto por cada semestre que hay que atravesar por el medio
        val cruces = mutableListOf<Cruce>()
        val desde = origen.semestre ?: 0
        val hasta = destino.semestre ?: 0
        for (s in desde + 1 until hasta) {
            val pasillo = pasillos[s] ?: continue
            cruces += Cruce(pasillo.x, pasillo.x + Nodo.ANCHO, pasillo.elegir((y1 + y2) / 2))
        }
        val trazo = if (rectos) {
            trazarRecto(x1, y1, x2, y2, cruces, salida.indexOf(destino.codigo) to salida.size, entrada.indexOf(pre) to entrada.size)
        } else trazar(x1, y1, x2, y2, cruces)
        aristas += Arista(origen.codigo, destino.codigo, origen.asignatura.area, cruces.isNotEmpty(), trazo)
    }
    return aristas
}
