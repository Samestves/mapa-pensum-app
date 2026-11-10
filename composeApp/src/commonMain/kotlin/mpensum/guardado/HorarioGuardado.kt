package mpensum.guardado

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import mpensum.logica.Sesion
import mpensum.logica.esValida
import mpensum.plataforma.Preferencias

/**
 * El horario de cada carrera, guardado en el teléfono como lo guarda la web
 * (useHorario.js): una lista JSON de clases {id, codigo, dia, inicio, fin,
 * seccion, aula, profesor}, con la misma clave por carrera. Se guarda dónde
 * puso el estudiante cada clase, no qué materias hay: eso ya está en el pensum.
 * Al leer se descarta lo imposible (un día fuera de lunes–viernes, un fin
 * antes del inicio) y la clase de una materia que ya no existe.
 */
object HorarioGuardado {
    private fun clave(slug: String) = "mapa-pensum:horario:$slug"

    fun leer(slug: String, codigos: Set<String>): List<Sesion> {
        val texto = Preferencias.leerTexto(clave(slug)) ?: return emptyList()
        val lista = runCatching { Json.parseToJsonElement(texto) as? JsonArray }.getOrNull() ?: return emptyList()
        return lista.mapNotNull { (it as? JsonObject)?.let(::sesionDe) }
            .filter { esValida(it) && it.codigo in codigos }
            .distinctBy { it.id }
    }

    fun guardar(slug: String, sesiones: List<Sesion>) {
        val lista = JsonArray(
            sesiones.map { s ->
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(s.id),
                        "codigo" to JsonPrimitive(s.codigo),
                        "dia" to JsonPrimitive(s.dia),
                        "inicio" to JsonPrimitive(s.inicio),
                        "fin" to JsonPrimitive(s.fin),
                        "seccion" to JsonPrimitive(s.seccion),
                        "aula" to JsonPrimitive(s.aula),
                        "profesor" to JsonPrimitive(s.profesor),
                    ),
                )
            },
        )
        Preferencias.guardarTexto(clave(slug), lista.toString())
    }

    private fun sesionDe(o: JsonObject): Sesion? {
        fun texto(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        fun entero(k: String) = (o[k] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        val codigo = texto("codigo") ?: return null
        val dia = entero("dia") ?: return null
        val inicio = entero("inicio") ?: return null
        val fin = entero("fin") ?: return null
        return Sesion(
            // Las filas viejas de la web pueden venir sin id: se le da el mismo que les daba ella
            id = texto("id") ?: "$codigo-$dia-$inicio",
            codigo = codigo,
            dia = dia,
            inicio = inicio,
            fin = fin,
            seccion = texto("seccion").orEmpty(),
            aula = texto("aula").orEmpty(),
            profesor = texto("profesor").orEmpty(),
        )
    }
}
