package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.ColorCarrera
import mpensum.datos.Pensum
import mpensum.datos.Semestre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** `casillaVaciaDe` y `aprobadasPorSemestre`, con pensums de juguete */
class SemestresAvanceTest {

    private fun materia(codigo: String, semestre: Int) = Asignatura(codigo = codigo, nombre = codigo, semestre = semestre)
    private fun hueco(codigo: String, semestre: Int) = Asignatura(codigo = codigo, nombre = codigo, semestre = semestre, esHueco = true, grupo = "tecnica")

    @Test
    fun casillaVaciaDevuelveLaPrimeraSinElectiva() {
        val nodos = listOf(materia("M1", 3), hueco("H1", 3), hueco("H2", 3), hueco("H3", 4))
        val electiva = materia("E1", 3)
        // H1 ya tiene su electiva: la vacía es H2
        assertEquals("H2", casillaVaciaDe(nodos, 3) { if (it == "H1") electiva else null })
        // Ninguna colocada: la primera
        assertEquals("H1", casillaVaciaDe(nodos, 3) { null })
    }

    @Test
    fun casillaVaciaIgnoraOtrosSemestresYMateriasNormales() {
        val nodos = listOf(materia("M1", 3), hueco("H3", 4))
        assertNull(casillaVaciaDe(nodos, 3) { null })
        assertEquals("H3", casillaVaciaDe(nodos, 4) { null })
        assertNull(casillaVaciaDe(nodos, 9) { null })
    }

    @Test
    fun casillaVaciaEsNullSiTodasEstanLlenas() {
        val nodos = listOf(hueco("H1", 3), hueco("H2", 3))
        val e = materia("E", 3)
        assertNull(casillaVaciaDe(nodos, 3) { e })
    }

    private fun pensum() = Pensum(
        slug = "x", nombre = "X", nombreCorto = "X", color = ColorCarrera("#fff", "#000"),
        semestres = listOf(Semestre(1, 2, 0), Semestre(2, 2, 0), Semestre(3, 1, 0)),
        asignaturas = listOf(materia("A", 1), materia("B", 1), materia("C", 2), hueco("H", 2), materia("D", 3)),
    )

    @Test
    fun aprobadasPorSemestreCuentaSoloObligatorias() {
        val marcas = mapOf("A" to Marca.APROBADA, "B" to Marca.CURSANDO, "C" to Marca.APROBADA, "H" to Marca.APROBADA)
        // B está cursando, H es una casilla y D no tiene marca
        assertEquals(listOf(1, 1, 0), aprobadasPorSemestre(pensum(), marcas))
    }

    @Test
    fun sinMarcasTodoEsCero() {
        assertEquals(listOf(0, 0, 0), aprobadasPorSemestre(pensum(), emptyMap()))
    }
}
