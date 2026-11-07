package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.ColorCarrera
import mpensum.datos.Pensum
import mpensum.datos.Semestre
import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lo que la hoja del progreso cuenta de lo que viene: qué abre aprobar, la cadena por períodos y la barra */
class ProyeccionTest {

    private val hoy = Periodo(2026, 2)

    /** A → B → C, y D suelta; B pide además a E */
    private val pensum = Pensum(
        slug = "prueba", nombre = "Prueba", nombreCorto = "P", color = ColorCarrera("#000000", "#000000"),
        semestres = listOf(Semestre(1, 3, 9), Semestre(2, 2, 6), Semestre(3, 1, 3)),
        asignaturas = listOf(
            Asignatura("A", "Álgebra", 1, 3),
            Asignatura("E", "Español", 1, 3),
            Asignatura("D", "Dibujo", 1, 3),
            Asignatura("B", "Cálculo", 2, 3, prerrequisitos = listOf("A", "E")),
            Asignatura("F", "Física", 2, 3, prerrequisitos = listOf("A")),
            Asignatura("C", "Mecánica", 3, 3, prerrequisitos = listOf("B")),
        ),
    )

    @Test
    fun loQueSeAbreEsLoQueQuedaDisponible() {
        // Cursando A y E: se abren B (pide las dos) y F (pide A); C no, pide B
        val marcas = mapOf("A" to Marca.CURSANDO, "E" to Marca.CURSANDO)
        val si = siApruebasLoQueCursas(pensum, marcas)
        assertEquals(setOf("B", "F"), si.seAbren.map { it.codigo }.toSet())
        assertEquals(setOf("B", "F"), abreAlAprobar("A", si.seAbren).map { it.codigo }.toSet())
        assertEquals(listOf("B"), abreAlAprobar("E", si.seAbren).map { it.codigo })
        assertTrue(abreAlAprobar("D", si.seAbren).isEmpty())
    }

    @Test
    fun loQueYaEstabaAbiertoNoSeAbre() {
        // Sin marcas, D, A y E ya están disponibles: aprobar A solo abre F (B sigue pidiendo E)
        assertEquals(listOf("F"), seAbrenAlAprobar(pensum, emptyMap(), listOf("A")).map { it.codigo })
    }

    @Test
    fun elProximoCuentaLoQueCursasComoAprobado() {
        val marcas = mapOf("A" to Marca.CURSANDO, "E" to Marca.APROBADA)
        assertEquals(setOf("B", "D", "F"), inscribiblesDelProximo(pensum, marcas).map { it.codigo }.toSet())
        assertEquals(listOf("C"), abreElProximo(pensum, marcas, "B").map { it.codigo })
        assertTrue(abreElProximo(pensum, marcas, "F").isEmpty())
    }

    @Test
    fun cadaEslabonVaEnSuPeriodoDelPlan() {
        val g = estimarGraduacion(pensum, emptyMap(), emptyMap(), 2, hoy)
        val periodos = periodosDeCadena(g, hoy)
        assertEquals(g.cuelloDeBotella.size, periodos.size)
        g.cuelloDeBotella.forEachIndexed { k, c ->
            val indice = g.plan.indexOfFirst { c in it }
            assertEquals(hoy.mas(indice + 1), periodos[k], "$c")
        }
        // Una cadena va de período en período, nunca dos eslabones en el mismo
        assertTrue(periodos.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun masDeLaCargaRapidaNoAdelanta() {
        val planes = planesPorCarga(pensum, emptyMap(), emptyMap(), hoy)
        val rapida = cargaMasRapida(planes)
        val minimo = planes.minOf { it.semestresRestantes }
        assertEquals(minimo, planes[rapida - 1].semestresRestantes)
        assertTrue(planes.take(rapida - 1).all { it.semestresRestantes > minimo })
    }

    @Test
    fun reprobarUnEslabonCorreLaFecha() {
        val marcas = mapOf("A" to Marca.APROBADA, "E" to Marca.APROBADA, "B" to Marca.CURSANDO, "D" to Marca.CURSANDO)
        // Con carga de sobra, lo único que puede mover la fecha es la cadena
        val g = estimarGraduacion(pensum, marcas, emptyMap(), 3, hoy)
        // B abre C: reprobarla mueve la fecha; D no abre nada
        assertTrue(fechaSiRepruebas(pensum, marcas, emptyMap(), 3, hoy, "B")!! > g.periodoFinal!!)
        assertEquals(g.periodoFinal, fechaSiRepruebas(pensum, marcas, emptyMap(), 3, hoy, "D"))
    }

    @Test
    fun laBarraVaPorSemestreYEstado() {
        val sistemas = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val marcas = buildMap {
            sistemas.obligatorias.filter { it.semestre == 1 }.drop(1).forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 1 }.take(1).forEach { put(it.codigo, Marca.CURSANDO) }
        }
        val barra = capsulasDe(sistemas, marcas) { null }
        // Una cápsula por nodo del mapa con semestre, casillas incluidas, y 153 UC con las casillas a sus UC típicas
        assertEquals(sistemas.asignaturas.count { it.semestre != null }, barra.sumOf { it.capsulas.size })
        assertEquals(153, barra.sumOf { s -> s.capsulas.sumOf { it.uc } })
        // Aprobadas, luego cursando, luego pendientes
        assertTrue(barra.all { s -> s.capsulas.map { it.estado.ordinal }.zipWithNext().all { (a, b) -> a <= b } })
        assertEquals(1, semestreActual(barra))
    }

    @Test
    fun elSemestreActualEsElQueCursas() {
        val sistemas = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        // Del 1 al 4 aprobadas (las casillas de electiva quedan sin elegir) y dos del 5 en curso
        val marcas = buildMap {
            sistemas.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 5 }.take(2).forEach { put(it.codigo, Marca.CURSANDO) }
        }
        assertEquals(5, semestreActual(capsulasDe(sistemas, marcas) { null }))
        // Sin nada en curso, el primero sin completar
        assertEquals(1, semestreActual(capsulasDe(sistemas, emptyMap()) { null }))
    }
}
