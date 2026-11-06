package mpensum.logica

import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Las reglas de las casillas de electiva (hooks/useCasillas.js), escritas a mano */
class CasillasTest {

    private fun pensum(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    @Test
    fun depuraLoGuardado() {
        val sistemas = pensum("ingenieria-de-sistemas")
        val tecnica = sistemas.grupos.first { it.clave == "tecnica" }.asignaturas
        val humanistica = sistemas.grupos.first { it.clave == "humanistica" }.asignaturas
        val guardado = mapOf(
            "casilla-humanistica-1" to humanistica[0].codigo,
            // Una técnica en una casilla humanística no cuenta
            "casilla-humanistica-2" to tecnica[0].codigo,
            // La misma electiva dos veces tampoco: se queda la primera
            "casilla-humanistica-3" to humanistica[0].codigo,
            "casilla-tecnica-4" to tecnica[1].codigo,
            // Casillas o electivas que ya no existen
            "casilla-inventada" to tecnica[2].codigo,
            "casilla-tecnica-5" to "0000000",
            // En Sistemas no hay franja: las casillas libres no valen
            "libre-tecnica-1" to tecnica[3].codigo,
        )
        assertEquals(
            mapOf("casilla-humanistica-1" to humanistica[0].codigo, "casilla-tecnica-4" to tecnica[1].codigo),
            depurarCasillas(guardado, sistemas),
        )
    }

    @Test
    fun laFranjaAceptaCasillasLibres() {
        val ambiental = pensum("ingenieria-ambiental")
        val g = ambiental.grupos.first()
        val guardado = mapOf("libre-${g.clave}-1" to g.asignaturas[0].codigo, "libre-${g.clave}-999" to g.asignaturas[1].codigo)
        assertEquals(mapOf("libre-${g.clave}-1" to g.asignaturas[0].codigo), depurarCasillas(guardado, ambiental))
    }

    @Test
    fun colocarMudaLaElectiva() {
        val antes = mapOf("a" to "X", "b" to "Y")
        assertEquals(mapOf("b" to "Y", "c" to "X"), colocarElectiva(antes, "c", "X"))
        assertEquals(mapOf("b" to "Y"), colocarElectiva(antes, "a", null))
    }
}
