package mpensum.datos

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mpensum.logica.MAX_CARGA
import mpensum.logica.Marca
import mpensum.logica.depurarCasillas
import mpensum.logica.depurarMarcas
import mpensum.plataforma.Preferencias

/**
 * Lo que el estudiante decide de cada carrera, guardado en el teléfono: sus
 * marcas {código → aprobada | cursando} y sus casillas {casilla → electiva}.
 * Cada carrera por separado, con las mismas claves que la web, para que mirar
 * otra carrera no ensucie la tuya. Lo leído se depura contra el pensum: un
 * código que ya no existe no cuenta, y un dato roto arranca en limpio.
 */
object Guardado {
    private fun claveMarcas(slug: String) = "mapa-pensum:marcas:$slug"
    private fun claveCasillas(slug: String) = "mapa-pensum:casillas:$slug"

    /** Las marcas tal cual, sin depurar: la portada solo quiere saber cuánto llevas */
    fun marcasCrudas(slug: String): Map<String, String>? = leerMapa(claveMarcas(slug))

    fun marcas(pensum: Pensum): Map<String, Marca> =
        depurarMarcas(marcasCrudas(pensum.slug), pensum.todas.map { it.codigo }.toSet())

    fun guardarMarcas(slug: String, marcas: Map<String, Marca>) =
        guardarMapa(claveMarcas(slug), marcas.mapValues { it.value.valor })

    fun casillas(pensum: Pensum): Map<String, String> = depurarCasillas(leerMapa(claveCasillas(pensum.slug)), pensum)

    /** Las casillas tal como están guardadas, sin depurar: la portada las lee sin tener el pensum a mano */
    fun casillasCrudas(slug: String): Map<String, String>? = leerMapa(claveCasillas(slug))

    fun guardarCasillas(slug: String, casillas: Map<String, String>) = guardarMapa(claveCasillas(slug), casillas)

    /*
     * Cuántas materias por semestre eligió en el mando de la hoja de avance.
     * Con la forma de la web ({"unidad":"materias","valor":5}), pero por
     * carrera: mirar otra carrera no le cambia la fecha a la suya. Null es
     * "la habitual", la que sale de sus marcas.
     */
    private fun claveCarga(slug: String) = "mapa-pensum:carga:$slug"

    fun carga(slug: String): Int? {
        val mapa = leerMapa(claveCarga(slug)) ?: return null
        if (mapa["unidad"] != "materias") return null
        return mapa["valor"]?.toIntOrNull()?.coerceIn(1, MAX_CARGA)
    }

    fun guardarCarga(slug: String, carga: Int?) {
        val valor = carga?.let { JsonObject(mapOf("unidad" to JsonPrimitive("materias"), "valor" to JsonPrimitive(it))) }
        // "null" se lee como sin carga elegida: vuelve a la habitual
        Preferencias.guardarTexto(claveCarga(slug), valor?.toString() ?: "null")
    }

    /**
     * Empezar de cero en una carrera: sus marcas, las electivas puestas en sus
     * casillas y la carga elegida. El horario no: es tu semana, no tu avance.
     */
    fun borrarAvance(slug: String) {
        guardarMarcas(slug, emptyMap())
        guardarCasillas(slug, emptyMap())
        guardarCarga(slug, null)
    }

    private fun leerMapa(clave: String): Map<String, String>? {
        val texto = Preferencias.leerTexto(clave) ?: return null
        return runCatching {
            (Json.parseToJsonElement(texto) as JsonObject).mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap()
        }.getOrNull()
    }

    private fun guardarMapa(clave: String, mapa: Map<String, String>) {
        Preferencias.guardarTexto(clave, JsonObject(mapa.mapValues { JsonPrimitive(it.value) }).toString())
    }
}
