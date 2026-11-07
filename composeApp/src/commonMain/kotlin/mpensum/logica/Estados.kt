package mpensum.logica

import mpensum.datos.Asignatura

/**
 * Lo que el estudiante marca de una materia. Solo esto se guarda: lo demás
 * (disponible, bloqueada) se deduce de los prerrequisitos cada vez, y
 * guardarlo sería tener dos verdades sobre lo mismo (data/estados.js).
 */
enum class Marca(val valor: String) {
    APROBADA("aprobada"),
    CURSANDO("cursando");

    companion object {
        fun de(valor: String?): Marca? = entries.firstOrNull { it.valor == valor }
    }
}

/** En qué estado está una materia: lo marcado más lo deducido */
enum class EstadoMateria { APROBADA, CURSANDO, DISPONIBLE, BLOQUEADA }

/**
 * Limpia las marcas leídas del teléfono: fuera los códigos que ya no existen
 * en el pensum y cualquier valor que no sea una marca.
 */
fun depurarMarcas(datos: Map<String, String>?, codigosValidos: Set<String>): Map<String, Marca> {
    if (datos == null) return emptyMap()
    return buildMap {
        for ((codigo, valor) in datos) {
            val marca = Marca.de(valor) ?: continue
            if (codigo in codigosValidos) put(codigo, marca)
        }
    }
}

/**
 * El estado de cada materia. Lo marcado manda; lo demás está disponible si
 * todos sus prerrequisitos están aprobados y, si pide un mínimo de UC
 * («120 UC aprobadas»), ya las tienes; bloqueado si no. Sin prerrequisitos
 * nace disponible.
 *
 * Las UC mínimas cuentan solo lo aprobado: lo que cursas aún no suma. La fecha
 * de graduación sí da por hecho lo que cursas al cerrar el período (estimarGraduacion).
 */
fun estadosDe(todas: List<Asignatura>, marcas: Map<String, Marca>): Map<String, EstadoMateria> {
    val ucAprobadas by lazy { todas.sumOf { if (marcas[it.codigo] == Marca.APROBADA) it.uc ?: 0 else 0 } }
    return todas.associate { a ->
        val estado = when (marcas[a.codigo]) {
            Marca.APROBADA -> EstadoMateria.APROBADA
            Marca.CURSANDO -> EstadoMateria.CURSANDO
            null -> if (a.prerrequisitos.all { marcas[it] == Marca.APROBADA } && (a.ucMinimas == null || ucAprobadas >= a.ucMinimas)) EstadoMateria.DISPONIBLE else EstadoMateria.BLOQUEADA
        }
        a.codigo to estado
    }
}

/** Aplica varios cambios de una vez; una marca null desmarca */
fun conMarcas(marcas: Map<String, Marca>, cambios: Map<String, Marca?>): Map<String, Marca> =
    marcas.toMutableMap().apply {
        for ((codigo, marca) in cambios) if (marca != null) put(codigo, marca) else remove(codigo)
    }
