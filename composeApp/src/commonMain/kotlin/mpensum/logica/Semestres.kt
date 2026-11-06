package mpensum.logica

import mpensum.datos.Asignatura

/**
 * Cómo va un semestre entero, para su casilla de marcar (data/semestre.js).
 * No es un sí / no / a medias: un semestre con todo aprobado y la electiva sin
 * elegir no está completo, y se dice.
 */
enum class MarcaSemestre { VACIO, PARCIAL, FALTA_ELEGIR, COMPLETO }

data class CuentaSemestre(val total: Int, val hechas: Int, val cursando: Int, val huecos: Int) {
    val marca: MarcaSemestre
        get() = when {
            hechas == total -> MarcaSemestre.COMPLETO
            huecos > 0 && hechas == total - huecos -> MarcaSemestre.FALTA_ELEGIR
            hechas > 0 -> MarcaSemestre.PARCIAL
            else -> MarcaSemestre.VACIO
        }
}

/**
 * La cuenta de cada semestre. `nodos` son las materias del mapa, casillas
 * incluidas; `enCasilla` dice qué electiva se eligió para una casilla.
 */
fun marcasDeSemestres(
    nodos: List<Asignatura>,
    enCasilla: (String) -> Asignatura?,
    estados: Map<String, EstadoMateria>,
): Map<Int, CuentaSemestre> {
    val cuentas = linkedMapOf<Int, CuentaSemestre>()
    for (nodo in nodos) {
        val semestre = nodo.semestre ?: continue
        val previa = cuentas[semestre] ?: CuentaSemestre(0, 0, 0, 0)
        val materia = if (nodo.esHueco) enCasilla(nodo.codigo) else nodo
        cuentas[semestre] = when {
            materia == null -> previa.copy(total = previa.total + 1, huecos = previa.huecos + 1)
            estados[materia.codigo] == EstadoMateria.APROBADA -> previa.copy(total = previa.total + 1, hechas = previa.hechas + 1)
            estados[materia.codigo] == EstadoMateria.CURSANDO -> previa.copy(total = previa.total + 1, cursando = previa.cursando + 1)
            else -> previa.copy(total = previa.total + 1)
        }
    }
    return cuentas
}

/** Los códigos que cuentan en un semestre: las materias y lo elegido en sus casillas */
fun materiasDeSemestre(nodos: List<Asignatura>, semestre: Int, enCasilla: (String) -> Asignatura?): List<String> =
    nodos.filter { it.semestre == semestre }.mapNotNull { (if (it.esHueco) enCasilla(it.codigo) else it)?.codigo }

/** La primera casilla del semestre que sigue sin electiva elegida */
fun casillaVaciaDe(nodos: List<Asignatura>, semestre: Int, enCasilla: (String) -> Asignatura?): String? =
    nodos.firstOrNull { it.semestre == semestre && it.esHueco && enCasilla(it.codigo) == null }?.codigo

/** Lo que hace la casilla de un semestre al pulsarla */
sealed interface AccionSemestre {
    data class Aprobar(val codigos: List<String>) : AccionSemestre
    data class Elegir(val casilla: String) : AccionSemestre
    data class Desmarcar(val codigos: List<String>) : AccionSemestre
}

fun accionDeSemestre(
    nodos: List<Asignatura>,
    semestre: Int,
    enCasilla: (String) -> Asignatura?,
    estados: Map<String, EstadoMateria>,
): AccionSemestre? {
    val codigos = materiasDeSemestre(nodos, semestre, enCasilla)
    val pendientes = codigos.filter { estados[it] != EstadoMateria.APROBADA }
    if (pendientes.isNotEmpty()) return AccionSemestre.Aprobar(pendientes)
    casillaVaciaDe(nodos, semestre, enCasilla)?.let { return AccionSemestre.Elegir(it) }
    return if (codigos.isNotEmpty()) AccionSemestre.Desmarcar(codigos) else null
}
