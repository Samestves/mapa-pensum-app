package mpensum.logica

/**
 * Cuántos semestres puede esperar una materia sin mover la fecha, con la misma
 * cuenta de la hoja de avance (`Simulador.margen`). Null si no está en tu plan
 * (aprobada, cursando o una electiva que no cuenta).
 */
fun margenDeMateria(sim: Simulador, codigo: String): Int? {
    if (sim.base.plan.none { codigo in it }) return null
    return sim.margen(codigo)
}
