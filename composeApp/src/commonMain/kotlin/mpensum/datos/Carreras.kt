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
        // Fuera del hilo principal: la primera pantalla espera a esta lista
        value = withContext(Dispatchers.Default) {
            lector.decodeFromString<List<Carrera>>(Res.readBytes("files/indice.json").decodeToString())
        }
    }
    return estado.value
}

/**
 * Lo que llevas de cada carrera en la que marcaste algo, sacado de tus marcas
 * guardadas (y de `tambien`, aunque no tenga): la portada lo enseña en tu tarjeta y en los mosaicos. Las que no
 * tienen marcas no se leen. Se calcula aparte del hilo de la pantalla y se
 * vuelve a calcular cada vez que la portada aparece, así que al volver de una
 * carrera ya trae lo que acabas de marcar.
 */
@Composable
fun rememberResumenes(carreras: List<Carrera>, tambien: String? = null): Map<String, ResumenPortada>? {
    // Arranca con lo último que se calculó: al volver de una carrera nada parpadea mientras se recalcula
    val estado = produceState(UltimosResumenes.valor, carreras, tambien) {
        value = withContext(Dispatchers.Default) {
            carreras.mapNotNull { c ->
                // Sin marcas solo se lee la que pide `tambien` (tu carrera): su silueta enseña en azul lo que ya puedes inscribir
                val crudas = Guardado.marcasCrudas(c.slug)?.takeIf { it.isNotEmpty() } ?: if (c.slug == tambien) emptyMap() else return@mapNotNull null
                // Una carrera que no se pueda leer no mata la portada: simplemente no trae resumen
                runCatching {
                    val pensum = leerPensum(leerPensumCrudo(c.slug))
                    c.slug to resumenParaPortada(pensum, crudas, Guardado.casillasCrudas(c.slug), conGraduacion = true, carga = Guardado.carga(c.slug))
                }.getOrNull()
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
