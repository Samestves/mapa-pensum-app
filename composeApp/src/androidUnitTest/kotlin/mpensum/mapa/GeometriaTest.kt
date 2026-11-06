package mpensum.mapa

import mpensum.datos.Asignatura
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Las piezas puras de la geometría del mapa: partir nombres en líneas y rutear los cables */
class GeometriaTest {

    // Cada carácter mide 0,5 em: con letra de 10 caben 5 por unidad de ancho, así que 50 son diez caracteres
    private fun partir(texto: String, ancho: Double = 50.0, maxLineas: Int = 3) = partirEnLineas(texto, ancho, 10.0, maxLineas)

    @Test
    fun unNombreCortoVaEnUnaLinea() {
        assertEquals(listOf("Calculo"), partir("Calculo"))
    }

    @Test
    fun partePorPalabrasSinCortarNinguna() {
        assertEquals(listOf("Calculo", "diferencial"), partir("Calculo diferencial"))
        assertEquals(listOf("uno dos", "tres"), partir("uno dos tres"))
    }

    @Test
    fun unaPalabraMasLargaQueLaLineaSeQuedaEntera() {
        assertEquals(listOf("electromagnetismo"), partir("electromagnetismo"))
    }

    @Test
    fun siNoCabeRecortaLaUltimaLineaConPuntosSuspensivos() {
        assertEquals(listOf("uno dos", "tres…"), partir("uno dos tres cuatro", maxLineas = 2))
        // La última línea se acorta hasta que quepa con los puntos
        assertEquals(listOf("abc…"), partir("abcdefgh ij", ancho = 20.0, maxLineas = 1))
    }

    private fun materia(codigo: String, semestre: Int, vararg pre: String, area: String? = null) =
        Asignatura(codigo = codigo, nombre = codigo, semestre = semestre, area = area, prerrequisitos = pre.toList())

    @Test
    fun hayUnCablePorCadaPrerrequisitoConElColorDelOrigen() {
        val layout = calcularLayout(
            listOf(materia("A", 1, area = "x"), materia("B", 1, area = "y"), materia("C", 2, "A", "B"), materia("D", 2, "A", "ZZZ")),
        )
        // ZZZ no existe: ese cable se ignora
        assertEquals(setOf("A->C", "B->C", "A->D"), layout.aristas.map { it.id }.toSet())
        assertEquals("x", layout.aristas.first { it.id == "A->C" }.area)
        assertEquals("y", layout.aristas.first { it.id == "B->C" }.area)
    }

    @Test
    fun elCableNaceEnElBordeDerechoDelOrigenYMuereEnElIzquierdoDelDestino() {
        val layout = calcularLayout(listOf(materia("A", 1), materia("B", 2, "A")))
        val a = layout.porCodigo.getValue("A")
        val b = layout.porCodigo.getValue("B")
        val cable = layout.aristas.single()
        val inicio = assertIs<Segmento.Mover>(cable.trazo.first())
        val fin = assertIs<Segmento.Curva>(cable.trazo.last())
        assertEquals(a.x + Nodo.ANCHO, inicio.x)
        assertEquals(b.x, fin.x)
        assertFalse(cable.cruzaColumnas)
    }

    @Test
    fun siSaltaUnSemestreCruzaPorSuPasillo() {
        val layout = calcularLayout(listOf(materia("A", 1), materia("B", 2), materia("C", 3, "A")))
        val cable = layout.aristas.single()
        assertTrue(cable.cruzaColumnas)
        // Entra al pasillo, lo recorre en recta y sale
        assertTrue(cable.trazo.any { it is Segmento.Linea })
    }

    @Test
    fun dosCablesDelMismoNodoSalenPorPuertosDistintos() {
        val layout = calcularLayout(listOf(materia("A", 1), materia("B", 2, "A"), materia("C", 2, "A")))
        val salidas = layout.aristas.map { (it.trazo.first() as Segmento.Mover).y }
        assertEquals(2, salidas.toSet().size)
    }
}
