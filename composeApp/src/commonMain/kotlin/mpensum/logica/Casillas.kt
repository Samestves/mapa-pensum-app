package mpensum.logica

import mpensum.datos.Pensum
import mpensum.mapa.casillasLibres

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
