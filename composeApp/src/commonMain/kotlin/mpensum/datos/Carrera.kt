package mpensum.datos

import kotlinx.serialization.Serializable

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
