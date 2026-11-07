package mpensum.logica

import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Por qué cada materia del próximo semestre y qué pasa si atrasas la cadena, con Sistemas de verdad */
class RecomendacionTest {

    private val carpeta = File("src/commonMain/composeResources/files/carreras")
    private val hoy = Periodo(2026, 2)
    private val sistemas by lazy { leerPensum(File(carpeta, "ingenieria-de-sistemas.json").readText()) }

    /** Primero a cuarto aprobados y dos de quinto en curso, como la captura horizontal */
    private val marcas by lazy {
        buildMap {
            sistemas.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 5 }.take(2).forEach { put(it.codigo, Marca.CURSANDO) }
        }
    }

    private fun nombre(codigo: String) = sistemas.todas.first { it.codigo == codigo }.nombre

    @Test
    fun sistemasConCuartoAprobadoYDosCursando() {
        val t0 = System.nanoTime()
        val p = proximoSemestre(sistemas, marcas, emptyMap(), 6, hoy)
        val c = cadenaExplicada(sistemas, marcas, emptyMap(), 6, hoy)
        val ms = (System.nanoTime() - t0) / 1e6
        val t1 = System.nanoTime()
        proximoSemestre(sistemas, marcas, emptyMap(), 6, hoy)
        cadenaExplicada(sistemas, marcas, emptyMap(), 6, hoy)
        val msCalentado = (System.nanoTime() - t1) / 1e6
        println("Tiempo proximoSemestre + cadenaExplicada: %.1f ms en frio, %.1f ms calentado".format(ms, msCalentado))
        println("Proximo ${p.periodo}: " + p.recomendadas.joinToString { "${nombre(it.codigo)} ${it.motivo} m${it.margen} abre${it.abre}" })
        println("Opcionales: " + p.opcionales.joinToString { "${nombre(it.codigo)} m${it.margen}" })
        println("Dejar: ${p.dejar?.let(::nombre)}; con una menos: ${p.conUnaMenos}")
        println("Cadena ${c.eslabones.map(::nombre)} ${c.periodos} final ${c.final} siAtrasas ${c.siAtrasas} holgura ${c.holgura}")
        println("Se corren: ${c.seCorren.map(::nombre)}; vigilar: ${c.vigilar?.let { nombre(it.first) to it.second }}")

        assertTrue(msCalentado < 100, "tardó $msCalentado ms")
        assertEquals(hoy.mas(1), p.periodo)
        assertEquals(estimarGraduacion(sistemas, marcas, emptyMap(), 6, hoy).plan[0].toSet(), p.recomendadas.map { it.codigo }.toSet())

        fun de(parte: String) = p.recomendadas.first { nombre(it.codigo).contains(parte, ignoreCase = true) }
        with(de("Sistemas de Costo")) { assertEquals(Motivo.FIJA_FECHA, motivo); assertEquals(0, margen) }
        assertEquals(1, de("Métodos Numéricos").margen)
        for (n in listOf("Sistemas de Operación", "Circuitos y Sistemas", "Teoría de Sistemas")) {
            with(de(n)) { assertEquals(Motivo.ABRE_CAMINO, motivo, n); assertEquals(2, margen, n); assertTrue(abre.isNotEmpty()) }
        }
        with(de("Análisis de Decisiones")) { assertEquals(Motivo.COMPLETA_CARGA, motivo); assertEquals(3, margen) }
        assertEquals(de("Análisis de Decisiones").codigo, p.dejar)
        // Los grupos van en orden: fija fecha, abre camino, completa carga
        assertEquals(p.recomendadas.sortedBy { it.motivo.ordinal }.map { it.codigo }, p.recomendadas.map { it.codigo })
        assertTrue(p.opcionales.any { nombre(it.codigo).contains("Leyes y Deontología", ignoreCase = true) && it.motivo == Motivo.OPCIONAL })
        assertEquals(estimarGraduacion(sistemas, marcas, emptyMap(), 5, hoy).periodoFinal, p.conUnaMenos)

        val base = estimarGraduacion(sistemas, marcas, emptyMap(), 6, hoy)
        assertEquals(base.cuelloDeBotella, c.eslabones)
        assertEquals(6, c.eslabones.size)
        assertEquals(base.periodoFinal, c.final)
        assertEquals(c.final!!.mas(1), c.siAtrasas)
        assertTrue(c.seCorren.containsAll(c.eslabones))
        assertEquals(0, c.holgura)
        assertTrue(nombre(c.vigilar!!.first).contains("Métodos Numéricos", ignoreCase = true))
        assertEquals(1, c.vigilar!!.second)
    }

    @Test
    fun sinVetoElPlanEsElDeSiempre() {
        for (carga in listOf(null, 1, 3, 5, 6, 8)) {
            val a = estimarGraduacion(sistemas, marcas, emptyMap(), carga, hoy)
            val b = estimarGraduacion(sistemas, marcas, emptyMap(), carga, hoy, noAntesDe = emptyMap())
            assertEquals(a.plan, b.plan)
            assertEquals(a.periodoFinal, b.periodoFinal)
            assertTrue(a.cuelloDeBotella.isNotEmpty())
        }
    }

    @Test
    fun unaCarreraSinMarcasNoReviente() {
        for (carrera in carpeta.listFiles()!!.filter { it.extension == "json" }.sorted()) {
            val pensum = leerPensum(carrera.readText())
            val p = proximoSemestre(pensum, emptyMap(), emptyMap(), null, hoy)
            val c = cadenaExplicada(pensum, emptyMap(), emptyMap(), null, hoy)
            assertTrue(p.recomendadas.isNotEmpty(), pensum.slug)
            assertTrue(c.eslabones.isNotEmpty(), pensum.slug)
        }
        // Y con todo aprobado no queda nada que recomendar
        val todo = sistemas.todas.associate { it.codigo to Marca.APROBADA }
        val p = proximoSemestre(sistemas, todo, emptyMap(), null, hoy)
        assertTrue(p.recomendadas.isEmpty())
        assertNull(p.dejar)
        assertNull(cadenaExplicada(sistemas, todo, emptyMap(), null, hoy).siAtrasas)
    }
}
