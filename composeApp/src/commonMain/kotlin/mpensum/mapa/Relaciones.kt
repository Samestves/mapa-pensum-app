package mpensum.mapa

import mpensum.datos.Asignatura

/** Las prelaciones en las dos direcciones (layout/relaciones.js) */
data class Relaciones(
    /** código → sus prerrequisitos directos */
    val atras: Map<String, List<String>>,
    /** código → lo que desbloquea directamente */
    val adelante: Map<String, List<String>>,
)

fun construirRelaciones(asignaturas: List<Asignatura>): Relaciones {
    val atras = asignaturas.associate { it.codigo to it.prerrequisitos }
    val adelante = linkedMapOf<String, MutableList<String>>()
    for (a in asignaturas) adelante.getOrPut(a.codigo) { mutableListOf() }
    for (a in asignaturas) for (pre in a.prerrequisitos) adelante.getOrPut(pre) { mutableListOf() } += a.codigo
    return Relaciones(atras, adelante)
}

/** Todo lo que se alcanza siguiendo un mapa; inmune a ciclos */
private fun alcanzables(inicio: String, mapa: Map<String, List<String>>): Set<String> {
    val vistos = linkedSetOf<String>()
    val pila = ArrayDeque(mapa[inicio].orEmpty())
    while (pila.isNotEmpty()) {
        val codigo = pila.removeLast()
        if (!vistos.add(codigo)) continue
        pila.addAll(mapa[codigo].orEmpty())
    }
    return vistos
}

/** La cadena entera de una materia: lo que pide hacia atrás, lo que abre hacia adelante y ella misma */
fun cadenaDe(codigo: String, relaciones: Relaciones): Set<String> =
    setOf(codigo) + alcanzables(codigo, relaciones.atras) + alcanzables(codigo, relaciones.adelante)
