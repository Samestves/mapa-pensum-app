package mpensum.mapa

import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import mpensum.logica.PREFIJO
import mpensum.logica.esCasillaLibre
import mpensum.logica.numeroDe
import mpensum.logica.primeraLibre
import kotlin.math.ceil
import kotlin.math.max

/*
 * La franja de electivas (layout/franjaElectivas.js): lo que tienen las
 * carreras sin ruta oficial en vez de casillas dentro de los semestres. Cada
 * grupo son las electivas que tú pusiste y, al final, una casilla vacía para
 * la siguiente. Donde la universidad no dice cuándo, el mapa tampoco.
 *
 * Por dentro son casillas corrientes, "libre-<grupo>-<n>", guardadas con el
 * mismo {casilla → electiva} que las de Sistemas.
 */

/** Los datos traen "Areas de Grado" sin tilde, tal como lo publica la DACE */
fun tituloGrupo(grupo: Grupo) = grupo.titulo.replace(Regex("^Areas\\b"), "Áreas")

/** Cómo se llama la casilla vacía de un grupo */
fun nombreCasilla(grupo: Grupo) = when (grupo.clave) {
    "tecnica" -> "Electiva Técnica"
    "humanistica" -> "Electiva Sociohumanística"
    else -> tituloGrupo(grupo).replace(Regex("^Áreas\\b"), "Área")
}

/** El rótulo de un grupo en la franja */
data class FilaFranja(
    val clave: String,
    val titulo: String,
    val cuota: Int?,
    val opciones: Int,
    val elegidas: Int,
    val x: Double,
    val y: Double,
)

data class FranjaColocada(val nodos: List<NodoMapa>, val filas: List<FilaFranja>, val alto: Double)

/**
 * Coloca la franja debajo de los semestres: los grupos seguidos en la misma
 * fila, alineados con las columnas del mapa; cada uno con su rótulo, primero
 * sus casillas ocupadas y detrás la primera libre. Lo que no cabe sigue abajo.
 */
fun calcularFranja(grupos: List<Grupo>, elegidas: Map<String, String>, xColumnas: List<Double>, yInicio: Double): FranjaColocada {
    val nodos = mutableListOf<NodoMapa>()
    val filas = mutableListOf<FilaFranja>()
    val columnas = xColumnas.size
    val altoFila = Franja.ENCABEZADO + Nodo.ALTO + Franja.FILA
    fun lugar(p: Int) = xColumnas[p % columnas] to yInicio + (p / columnas) * altoFila
    var posicion = 0

    for (g in grupos) {
        if (g.asignaturas.isEmpty()) continue
        val ocupadas = elegidas.keys
            .filter { esCasillaLibre(it) && it.startsWith("$PREFIJO${g.clave}-") && numeroDe(it) <= g.asignaturas.size }
            .sortedBy(::numeroDe)
        val casillas = primeraLibre(g, elegidas)?.let { ocupadas + it } ?: ocupadas
        val (xFila, yFila) = lugar(posicion)
        filas += FilaFranja(g.clave, tituloGrupo(g), g.cuota, g.asignaturas.size, ocupadas.size, xFila, yFila)
        casillas.forEachIndexed { i, codigo ->
            val (x, y) = lugar(posicion + i)
            val casilla = Asignatura(codigo = codigo, nombre = nombreCasilla(g), grupo = g.clave, esHueco = true)
            nodos += NodoMapa(casilla, x, y + Franja.ENCABEZADO, listOf(casilla.nombre))
        }
        posicion += casillas.size
    }
    val filasUsadas = max(1, ceil(posicion.toDouble() / columnas).toInt())
    return FranjaColocada(nodos, filas, filasUsadas * altoFila - Franja.FILA + Margen.ABAJO)
}
