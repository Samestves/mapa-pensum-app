package mpensum.logica

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import mpensum.datos.Asignatura

/*
 * El lado de la red de la lectura de horarios (data/leerHorario.js de la web):
 * qué se manda, qué se entiende de la respuesta y cómo se le cuenta al
 * estudiante cada fallo. La parte del teléfono (galería, imagen, HTTP) vive en
 * la plataforma; esto es puro y se prueba sin llamar a nadie.
 */

/** Lo que se le ofrece a quien le falló la lectura: la salida que de verdad lo arregla */
enum class Salida { REINTENTAR, OTRA_IMAGEN, A_MANO }

/**
 * Un fallo con código, en cristiano: qué pasó y qué hacer. `aplazado` marca los
 * que no son un error sino un «todavía no»; `espera` son los segundos tras los
 * que merece la pena volver, si el servidor lo sabe.
 */
class FalloLectura(val codigo: String, val tecnico: String? = null, val espera: Int? = null) : Exception(codigo) {
    private val texto = FALLOS[codigo] ?: AVERIA
    val titulo get() = texto.titulo
    val consejo get() = texto.consejo
    val salida get() = texto.salida
    val aplazado get() = texto.aplazado
}

private class Texto(val titulo: String, val consejo: String, val salida: Salida, val aplazado: Boolean = false)

/** Las averías de la IA (un modelo jubilado, una clave sin permiso) se dicen igual todas: no es por la foto */
private val AVERIA = Texto("La IA está fuera de servicio", "No es por tu imagen. Mientras vuelve, puedes armar el horario a mano.", Salida.A_MANO)

private val FALLOS = mapOf(
    "cola" to Texto("Sigue habiendo cola", "Lo intenté varias veces y la IA no tuvo sitio. Tu imagen sigue aquí.", Salida.REINTENTAR),
    "cuota" to Texto("La IA vuelve mañana", "Hoy ya leyó todos los horarios que permite. Tu horario no se ha tocado.", Salida.A_MANO, aplazado = true),
    "muchas" to Texto("Muchas lecturas seguidas", "La IA te deja volver dentro de una hora. Mientras, puedes armarlo a mano.", Salida.A_MANO, aplazado = true),
    "red" to Texto("Sin conexión", "Revisa tu conexión y vuelve a intentarlo. Tu imagen sigue aquí.", Salida.REINTENTAR),
    "ia" to Texto("No pude con esta imagen", "A veces sale a la segunda. Si no, prueba con una captura de pantalla.", Salida.REINTENTAR),
    "vacia" to Texto("No encontré nada que leer", "Vuelve a intentarlo, o prueba con una foto más nítida.", Salida.REINTENTAR),
    "json" to Texto("La lectura llegó a medias", "Vuelve a intentarlo: suele salir a la segunda.", Salida.REINTENTAR),
    "sin-clases" to Texto("No encontré clases ahí", "Prueba con una captura de pantalla, o con una foto más recta y con buena luz.", Salida.OTRA_IMAGEN),
    "sin-imagen" to Texto("No llegó ninguna imagen", "Elige la foto o la captura de tu horario.", Salida.OTRA_IMAGEN),
    "imagen-grande" to Texto("La imagen pesa demasiado", "Prueba con una captura de pantalla.", Salida.OTRA_IMAGEN),
    "tipo" to Texto("Ese formato no se puede leer", "Usa JPG, PNG o una captura de pantalla.", Salida.OTRA_IMAGEN),
    "no-es-imagen" to Texto("Eso no es una imagen", "Elige la foto o la captura de tu horario.", Salida.OTRA_IMAGEN),
    "pesada" to Texto("La imagen pesa más de 12 MB", "Prueba con una captura de pantalla.", Salida.OTRA_IMAGEN),
    "no-se-abre" to Texto("No se pudo abrir la imagen", "Prueba con una captura de pantalla.", Salida.OTRA_IMAGEN),
    // El servidor solo deja pasar a la propia app: si dice «fuera», esta versión se quedó vieja
    "fuera" to Texto("La lectura no está disponible", "Actualiza la app y vuelve a intentarlo, o arma el horario a mano.", Salida.A_MANO),
    // La respuesta no era JSON: algo se interpuso entre el teléfono y la IA
    "sin-servidor" to Texto("La IA no respondió", "Vuelve a intentarlo en un rato. Tu imagen sigue aquí.", Salida.REINTENTAR),
)

/** La mayor medida (px) a la que se reduce la imagen antes de subirla, y su calidad JPEG (0 a 100) */
const val LADO_MAXIMO = 1600
const val CALIDAD_JPEG = 85
const val TIPO_SUBIDA = "image/jpeg"
const val PESO_MAXIMO = 12L * 1024 * 1024

/** Dónde vive el servidor de la web; la petición se hace pasar por la propia web, que es lo único que acepta */
const val DOMINIO = "https://mapa-pensum.vercel.app"
const val URL_LECTURA = "$DOMINIO/api/leer-horario"

private val JSON = Json { ignoreUnknownKeys = true }

/** Las materias que se le dan al servidor para que emparejen por código: las del pensum, sin huecos ni comodines */
fun materiasParaLeer(todas: List<Asignatura>): List<Asignatura> = todas.filter { !it.esHueco && !it.esComodin }

/** El cuerpo de la petición: `{ imagen, tipo, materias: [{codigo, nombre}] }` */
fun cuerpoDePeticion(base64: String, materias: List<Asignatura>): String = buildJsonObject {
    put("imagen", base64)
    put("tipo", TIPO_SUBIDA)
    put("materias", buildJsonArray {
        for (m in materias) add(buildJsonObject { put("codigo", m.codigo); put("nombre", m.nombre) })
    })
}.toString()

private fun JsonObject.texto(clave: String): String = (this[clave] as? JsonPrimitive)?.contentOrNull.orEmpty()

/**
 * Lo que el servidor contestó, en filas. Con un estado que no es 2xx lanza el
 * fallo que dice `error`; si no era JSON, «sin-servidor».
 */
fun filasDeRespuesta(estado: Int, esJson: Boolean, cuerpo: String): List<FilaCruda> {
    if (!esJson) throw FalloLectura("sin-servidor")
    val datos = runCatching { JSON.parseToJsonElement(cuerpo) as? JsonObject }.getOrNull()
    if (estado !in 200..299) {
        val espera = (datos?.get("espera") as? JsonPrimitive)?.doubleOrNull?.toInt()
        throw FalloLectura(datos?.texto("error")?.ifEmpty { null } ?: "desconocido", datos?.texto("detalle"), espera)
    }
    val clases = datos?.get("clases") as? JsonArray ?: return emptyList()
    return clases.mapNotNull { (it as? JsonObject)?.let { f ->
        FilaCruda(f.texto("codigo"), f.texto("nombre"), f.texto("dia"), f.texto("inicio"), f.texto("fin"), f.texto("seccion"), f.texto("aula"), f.texto("profesor"))
    } }
}

/**
 * De las filas crudas a las materias leídas, una por materia con todas sus
 * clases. Entran las que se entendieron del todo (el choque no las saca: la hoja
 * «Antes de guardar» lo enseña); el resto se descarta. Sin ninguna, es «sin-clases».
 */
fun materiasLeidas(filas: List<FilaCruda>, materias: List<Asignatura>): List<MateriaLeida> {
    val buenas = aSesiones(revisar(filas, materias), conChoques = true)
    if (buenas.isEmpty()) throw FalloLectura("sin-clases")
    return buenas.groupBy { it.codigo }.entries.mapIndexed { n, (codigo, sesiones) ->
        MateriaLeida(codigo, sesiones.mapIndexed { k, s -> s.copy(id = "leida-$n-$k") })
    }
}
