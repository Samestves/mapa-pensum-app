package mpensum.logica

import mpensum.datos.Pensum
import mpensum.mapa.adoptarMarcadas
import mpensum.mapa.casillasLibres
import mpensum.mapa.esCasillaLibre
import mpensum.mapa.numeroDe

/**
 * Las casillas de electiva de una carrera, cada una con el grupo que admite.
 * Si las casillas de los semestres son toda la exigencia (Sistemas), solo
 * esas; si no, también las libres de la franja (hooks/useCasillas.js).
 */
fun casillasValidas(pensum: Pensum): Map<String, String> {
    val mapa = if (pensum.electivasEnCasillas) mutableMapOf() else casillasLibres(pensum.grupos).toMutableMap()
    for (a in pensum.asignaturas) if (a.esHueco && a.grupo != null) mapa[a.codigo] = a.grupo
    return mapa
}

/**
 * Limpia lo guardado de {casilla → electiva}: fuera las casillas o electivas
 * que ya no existen, la misma electiva en dos casillas (cursarla dos veces no
 * existe) y la electiva de otro grupo (una técnica no cuenta para la cuota
 * humanística aunque quepa en el hueco).
 */
fun depurarCasillas(datos: Map<String, String>?, pensum: Pensum): Map<String, String> {
    if (datos == null) return emptyMap()
    val casillas = casillasValidas(pensum)
    val grupoDe = pensum.grupos.flatMap { g -> g.asignaturas.map { it.codigo to g.clave } }.toMap()
    val usadas = mutableSetOf<String>()
    return buildMap {
        for ((casilla, codigo) in datos) {
            val grupo = casillas[casilla] ?: continue
            if (grupoDe[codigo] != grupo || !usadas.add(codigo)) continue
            put(casilla, codigo)
        }
    }
}

/**
 * Pone una electiva en una casilla, o la vacía con null. Si esa electiva ya
 * estaba en otra casilla, se muda en vez de aparecer dos veces.
 */
fun colocarElectiva(elegidas: Map<String, String>, casilla: String, codigo: String?): Map<String, String> {
    if (codigo == null) return elegidas - casilla
    return elegidas.filterValues { it != codigo } + (casilla to codigo)
}

/**
 * Dónde está de verdad cada electiva: lo que se dibuja y lo que cuenta la
 * fecha leen este mismo mapa, así que no pueden discrepar.
 *
 * Donde las casillas de los semestres son toda la exigencia (Sistemas) es lo
 * que elegiste. En las demás, lo hecho o elegido fuera de una casilla de
 * semestre la ocupa primero, mientras su grupo tenga una libre (Ambiental
 * tiene cinco), en orden de semestre; solo lo que sobra va a la franja. Así
 * una electiva aprobada desde la lista llena su casilla en vez de dejarla
 * vacía, y elegirla en la franja no suma una pendiente más.
 */
fun casillasEfectivas(pensum: Pensum, casillas: Map<String, String>, marcas: Map<String, Marca>): Map<String, String> {
    if (pensum.electivasEnCasillas) return casillas
    return adoptarMarcadas(alSemestre(pensum, casillas, marcas), pensum.grupos, marcas)
}

/** Pasa a las casillas libres de los semestres las electivas de la franja y las marcadas sin casilla, mientras quepan */
private fun alSemestre(pensum: Pensum, casillas: Map<String, String>, marcas: Map<String, Marca>): Map<String, String> {
    var resultado = casillas
    for (g in pensum.grupos) {
        val libres = pensum.asignaturas
            .filter { it.esHueco && it.grupo == g.clave && it.codigo !in resultado }
            .sortedBy { it.semestre ?: 99 }
            .map { it.codigo }
        if (libres.isEmpty()) continue
        val delGrupo = g.asignaturas.mapTo(mutableSetOf()) { it.codigo }
        val enFranja = resultado.entries.filter { esCasillaLibre(it.key) && it.value in delGrupo }.sortedBy { numeroDe(it.key) }.map { it.value }
        val sinCasilla = g.asignaturas.filter { marcas[it.codigo] != null && it.codigo !in resultado.values }.map { it.codigo }
        val mover = (enFranja + sinCasilla).take(libres.size)
        resultado = resultado.filterValues { it !in mover } + libres.zip(mover)
    }
    return resultado
}
