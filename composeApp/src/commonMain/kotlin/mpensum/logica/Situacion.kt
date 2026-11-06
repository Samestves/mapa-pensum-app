package mpensum.logica

/**
 * Lo que una materia significa hoy para quien mira el mapa (layout/situacion.js).
 *
 * BLOQUEADA mete en el mismo saco la materia que se abre el semestre que
 * viene si apruebas lo que cursas y la que queda a tres semestres. Para
 * planificar, la primera es la más importante del mapa; por eso se separan.
 * Es solo lectura: no se guarda ni cuenta UC.
 */
enum class Situacion { HECHA, CURSANDO, INSCRIBIBLE, PROXIMA, LEJANA }

fun situacionDe(codigo: String, prerrequisitos: List<String>, estados: Map<String, EstadoMateria>): Situacion =
    when (estados[codigo]) {
        EstadoMateria.APROBADA -> Situacion.HECHA
        EstadoMateria.CURSANDO -> Situacion.CURSANDO
        EstadoMateria.DISPONIBLE -> Situacion.INSCRIBIBLE
        else -> {
            // Queda a un semestre si todo lo que le falta lo estás cursando ahora.
            // Una materia sin prelaciones nunca llega aquí: nace disponible.
            val seAbre = prerrequisitos.isNotEmpty() &&
                prerrequisitos.all { estados[it] == EstadoMateria.APROBADA || estados[it] == EstadoMateria.CURSANDO }
            if (seAbre) Situacion.PROXIMA else Situacion.LEJANA
        }
    }

/**
 * Lo que dice un cable según sus dos extremos.
 * FRONTERA: de algo aprobado a algo que ya puedes inscribir; es el único que se enciende.
 * RECORRIDO: camino hecho. PROXIMA: hacia lo que se abre el semestre que viene.
 * LEJANA: todo lo demás, que se dibuja apenas.
 */
enum class Tramo { FRONTERA, RECORRIDO, PROXIMA, LEJANA }

fun tramoDe(origen: Situacion, destino: Situacion): Tramo = when {
    origen == Situacion.HECHA && destino == Situacion.INSCRIBIBLE -> Tramo.FRONTERA
    origen == Situacion.HECHA && (destino == Situacion.HECHA || destino == Situacion.CURSANDO) -> Tramo.RECORRIDO
    (origen == Situacion.HECHA || origen == Situacion.CURSANDO) && destino == Situacion.PROXIMA -> Tramo.PROXIMA
    else -> Tramo.LEJANA
}
