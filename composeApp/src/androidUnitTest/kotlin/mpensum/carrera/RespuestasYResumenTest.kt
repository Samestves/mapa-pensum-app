package mpensum.carrera

import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.logica.Simulador
import mpensum.logica.Situacion
import mpensum.logica.estimarGraduacion
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Las cuentas de «Tu avance» y de la ficha, ya sin dibujo: se prueban en la JVM con Sistemas de verdad */
class RespuestasYResumenTest {

    private val hoy = Periodo(2026, 2)
    private val sistemas by lazy { leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText()) }

    private val marcas by lazy {
        buildMap {
            sistemas.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 5 }.take(2).forEach { put(it.codigo, Marca.CURSANDO) }
        }
    }

    private val sim by lazy { Simulador(sistemas, marcas, emptyMap(), 6, hoy) }

    @Test
    fun laRespuestaDeUnaAprobadaNoExiste() {
        val aprobada = marcas.entries.first { it.value == Marca.APROBADA }.key
        assertNull(respuestaDe(aprobada, Situacion.HECHA, sim))
    }

    @Test
    fun laQueCursasDiceQueTeGraduasSiLaRepruebas() {
        val cursando = marcas.entries.first { it.value == Marca.CURSANDO }.key
        val r = assertNotNull(respuestaDe(cursando, Situacion.CURSANDO, sim))
        assertEquals(IconoFila.REINICIAR, r.icono)
        assertEquals("Si la repruebas", r.titulo)
        assertEquals(Situacion.CURSANDO, r.para)
    }

    @Test
    fun unaInscribibleQueNoPuedeEsperarLlevaElEslabon() {
        val fija = sim.base.plan.first().first { sim.margen(it) == 0 }
        val r = assertNotNull(respuestaDe(fija, Situacion.INSCRIBIBLE, sim))
        assertEquals(IconoFila.ESLABON, r.icono)
        assertEquals("No puede esperar", r.titulo)
        // Una que sí puede esperar lleva el reloj de arena y no mueve tu fecha
        val libre = sim.base.plan.first().firstOrNull { sim.margen(it) > 0 }
        if (libre != null) {
            val l = assertNotNull(respuestaDe(libre, Situacion.INSCRIBIBLE, sim))
            assertEquals(IconoFila.RELOJ_ARENA, l.icono)
            assertEquals("Sin mover tu fecha", l.nota?.antes)
        }
    }

    @Test
    fun unaInscribibleFueraDelPlanNoTieneRespuesta() {
        assertNull(respuestaDe("no-esta-en-el-plan", Situacion.INSCRIBIBLE, sim))
    }

    @Test
    fun lasPreguntasDelIndiceSoloTraenLoQueTieneAlgoQueDecir() {
        val g = estimarGraduacion(sistemas, marcas, emptyMap(), 6, hoy)
        val preguntas = preguntasDe(g, hoy, cursando = 2, seAbren = 1, rapida = null, nombre = { it })
        assertEquals(listOf("Este semestre", "Tu próximo semestre", "No puedes atrasar", "¿Cuándo me gradúo?"), preguntas.map { it.titulo })
        assertEquals(listOf(IconoFila.LIBRO_ABIERTO, IconoFila.CALENDARIO, IconoFila.ESLABON, IconoFila.BIRRETE), preguntas.map { it.icono })
        assertEquals("Te abren 1", preguntas[0].nota)
        assertEquals("A tu ritmo", preguntas.last().nota)
        // Sin nada cursando no hay «Este semestre»
        val sin = preguntasDe(g, hoy, cursando = 0, seAbren = 0, rapida = null, nombre = { it })
        assertTrue(sin.none { it.titulo == "Este semestre" })
        // Si la fecha ya es la más rápida, lo dice
        assertEquals("Ya vas lo más rápido", preguntasDe(g, hoy, 0, 0, g.periodoFinal, { it }).last().nota)
    }
}
