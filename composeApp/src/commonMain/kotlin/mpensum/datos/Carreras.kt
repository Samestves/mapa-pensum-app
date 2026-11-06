package mpensum.datos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mpensum.logica.ResumenPortada
import mpensum.logica.resumenParaPortada
import mpensum.recursos.Res

/**
 * Una carrera tal como la describe el índice que genera la web
 * (src/data/carreras/indice.json), más `uc`: las ucObligatorias de su JSON.
 */
@Serializable
data class Carrera(
    val slug: String,
    val nombre: String,
    val nombreCorto: String,
    val color: ColorCarrera,
    val semestres: Int,
    val asignaturas: Int,
    val electivas: Int,
    val uc: Int,
    val silueta: List<Int>,
) {
    /** Ingeniería, Licenciatura o Gerencia, sacado del nombre como en data/portada.js */
    val tipo: String get() = TIPOS.firstOrNull { nombre.startsWith(it) } ?: ""
}

@Serializable
data class ColorCarrera(val oscuro: String, val claro: String)

val TIPOS = listOf("Ingeniería", "Licenciatura", "Gerencia")
val PLURAL = mapOf("Ingeniería" to "Ingenierías", "Licenciatura" to "Licenciaturas", "Gerencia" to "Gerencia")

private val lector = Json { ignoreUnknownKeys = true }

/** Las nueve carreras, leídas una vez del índice empaquetado con la app */
@Composable
fun rememberCarreras(): List<Carrera> {
    val estado = produceState(emptyList<Carrera>()) {
        val texto = Res.readBytes("files/indice.json").decodeToString()
        value = lector.decodeFromString<List<Carrera>>(texto)
    }
    return estado.value
}

/**
 * Lo que llevas de cada carrera en la que marcaste algo, sacado de tus marcas
 * guardadas: la portada lo enseña en tu tarjeta y en los mosaicos. Las que no
 * tienen marcas no se leen. Se calcula aparte del hilo de la pantalla y se
 * vuelve a calcular cada vez que la portada aparece, así que al volver de una
 * carrera ya trae lo que acabas de marcar.
 */
@Composable
fun rememberResumenes(carreras: List<Carrera>): Map<String, ResumenPortada>? {
    // Arranca con lo último que se calculó: al volver de una carrera nada parpadea mientras se recalcula
    val estado = produceState(UltimosResumenes.valor, carreras) {
        value = withContext(Dispatchers.Default) {
            carreras.mapNotNull { c ->
                val crudas = Guardado.marcasCrudas(c.slug)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val pensum = leerPensum(Res.readBytes("files/carreras/${c.slug}.json").decodeToString())
                c.slug to resumenParaPortada(pensum, crudas)
            }.toMap()
        }
        UltimosResumenes.valor = value
    }
    return estado.value
}

/** Lo último que calculó la portada en esta sesión */
private object UltimosResumenes {
    var valor: Map<String, ResumenPortada>? = null
}
