package mpensum.logica

/**
 * Cuánto puede esperar una materia sin mover la fecha, con la misma cuenta de
 * la hoja de avance (`Simulador.margen`): lo que ya midió esa hoja no se mide
 * dos veces.
 */
data class MargenMateria(
    /** Semestres que puede esperar; `TOPE_MARGEN` es que no hay semestre en que mueva la fecha */
    val margen: Int,
    /** Tu fecha de grado si la dejas un semestre más allá de su margen; null si nunca la mueve */
    val siLaDejas: Periodo?,
    /** Tu fecha de grado hoy */
    val fecha: Periodo?,
) {
    val sinLimite: Boolean get() = margen >= TOPE_MARGEN
}

/** El margen de una materia, o null si no está en tu plan (aprobada, cursando o una electiva que no cuenta) */
fun margenDeMateria(sim: Simulador, codigo: String): MargenMateria? {
    if (sim.base.plan.none { codigo in it }) return null
    val margen = sim.margen(codigo)
    val tarde = if (margen >= TOPE_MARGEN) null else sim.conVeto(codigo, sim.indiceEnPlan(codigo) + margen + 1).periodoFinal
    return MargenMateria(margen, tarde, sim.base.periodoFinal)
}
