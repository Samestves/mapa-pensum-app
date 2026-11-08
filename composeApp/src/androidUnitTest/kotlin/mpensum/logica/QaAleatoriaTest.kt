package mpensum.logica

import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import mpensum.mapa.calcularLayout
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de estrés de la lógica: marcas al azar (con semilla, así que se
 * reproducen) en las nueve carreras, para cazar lo que las pruebas a mano no
 * piensan: más carga que alarga, un porcentaje que se pasa de 100, un plan que
 * pierde materias o una lista que cuenta de más.
 */
class QaAleatoriaTest {

    private val carpeta = File("src/commonMain/composeResources/files/carreras")
    private val pensums by lazy { carpeta.listFiles()!!.filter { it.extension == "json" }.sorted().map { leerPensum(it.readText()) } }
    private val hoy = Periodo(2026, 2)

    /** Marcas al azar: cada materia aprobada, en curso o sin marcar según la semilla */
    private fun azar(p: Pensum, semilla: Int): Map<String, Marca> {
        val r = Random(semilla)
        val pAprobada = r.nextDouble()
        val pCursando = r.nextDouble() * (1 - pAprobada) * 0.3
        return buildMap {
            for (a in p.todas) {
                val x = r.nextDouble()
                when {
                    x < pAprobada -> put(a.codigo, Marca.APROBADA)
                    x < pAprobada + pCursando -> put(a.codigo, Marca.CURSANDO)
                }
            }
        }
    }

    @Test
    fun masCargaNuncaAlargaConMarcasAlAzar() {
        for (p in pensums) for (semilla in 0 until 40) {
            val marcas = azar(p, semilla)
            var anterior = Int.MAX_VALUE
            for (carga in 1..9) {
                val e = estimarGraduacion(p, marcas, emptyMap(), carga, hoy)
                assertTrue(e.semestresRestantes <= anterior, "${p.slug} semilla $semilla: con $carga materias tarda ${e.semestresRestantes} y con ${carga - 1} tardaba $anterior")
                assertTrue(e.semestresRestantes >= e.minimoPorPrelaciones)
                assertEquals(e.materiasRestantes, e.plan.flatten().size, "${p.slug} semilla $semilla: el plan pierde o repite materias")
                anterior = e.semestresRestantes
            }
        }
    }

    @Test
    fun terminadoEsCeroYSinPeriodo() {
        for (p in pensums) {
            val todo = p.todas.associate { it.codigo to Marca.APROBADA }
            val e = estimarGraduacion(p, todo, emptyMap(), null, hoy)
            assertEquals(0, e.semestresRestantes, p.slug)
            assertNull(e.periodoFinal, p.slug)
            assertTrue(e.terminado)
            assertTrue(e.plan.isEmpty())
            // Con la carga elegida, igual
            assertEquals(0, estimarGraduacion(p, todo, emptyMap(), 9, hoy).semestresRestantes, p.slug)
        }
    }

    @Test
    fun elPorcentajeNoSePasaDeCien() {
        for (p in pensums) for (semilla in 0 until 40) {
            val marcas = azar(p, semilla)
            val r = resumenParaPortada(p, marcas.mapValues { it.value.valor })
            assertTrue(r.porcentaje in 0..100, "${p.slug} semilla $semilla: ${r.porcentaje} %")
            val prog = progresoDe(p.obligatorias, estadosDe(p.todas, marcas), p.creditos, avanceDeGrupos(p.grupos, marcas))
            assertTrue(prog.avance in 0.0..100.0, "${p.slug} semilla $semilla: avance ${prog.avance}")
        }
        // Todo aprobado: exactamente 100 en las que tienen créditos
        for (p in pensums) {
            val todo = p.todas.associate { it.codigo to Marca.APROBADA }
            val r = resumenParaPortada(p, todo.mapValues { it.value.valor })
            assertTrue(r.porcentaje in 0..100, "${p.slug} todo aprobado: ${r.porcentaje} %")
        }
    }

    @Test
    fun laListaCuentaCadaMateriaUnaVez() {
        for (p in pensums) {
            val layout = calcularLayout(p.asignaturas, p.grupos)
            val nodos = layout.nodos.map { it.asignatura }
            for (semilla in 0 until 15) {
                val marcas = azar(p, semilla)
                val estados = estadosDe(p.todas, marcas)
                val situaciones = p.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
                val u = universoDeLista(nodos, p.grupos, { null }, estados, situaciones) { c -> p.todas.firstOrNull { it.codigo == c }?.nombre }
                val actual = semestreActualDeLista(nodos, { null }, estados)
                // Sin electivas puestas, las únicas materias son las obligatorias con semestre; las casillas salen aparte
                val esperadas = p.obligatorias.count { it.semestre != null }
                assertEquals(esperadas, u.materias.size, "${p.slug} semilla $semilla")
                val porEstado = estructuraPorEstado(u, emptyMap(), actual)
                val porSemestre = estructuraPorSemestre(u)
                // Cada materia sale en una sola sección de cada orden; las casillas, a lo sumo una vez
                for (grupos in listOf(porEstado, porSemestre)) {
                    val codigos = grupos.flatMap { it.codigos }
                    assertEquals(codigos.size, codigos.toSet().size, "${p.slug} semilla $semilla: un código repetido")
                    assertTrue(u.materias.all { it.codigo in codigos }, "${p.slug} semilla $semilla: una materia no sale")
                }
                // Cada materia cae en la sección de su estado
                for (g in porEstado) for (c in g.codigos) u.materia(c)?.let { assertEquals(g.clave, seccionDe(it.situacion).clave, "${p.slug} semilla $semilla $c") }
            }
        }
    }

    @Test
    fun accionDeSemestreNoReventaYAprobarLoCompleta() {
        for (p in pensums) {
            val layout = calcularLayout(p.asignaturas, p.grupos)
            val nodos = layout.nodos.map { it.asignatura }
            for (semilla in 0 until 10) {
                var marcas = azar(p, semilla)
                for (c in layout.columnas) {
                    val estados = estadosDe(p.todas, marcas)
                    val accion = accionDeSemestre(nodos, c.semestre, { null }, estados)
                    if (accion is AccionSemestre.Aprobar) {
                        marcas = conMarcas(marcas, accion.codigos.associateWith { Marca.APROBADA })
                        val despues = accionDeSemestre(nodos, c.semestre, { null }, estadosDe(p.todas, marcas))
                        // Aprobado lo que había, solo queda elegir una casilla o desmarcar
                        assertTrue(despues !is AccionSemestre.Aprobar, "${p.slug} s${c.semestre}: aprobar no completa")
                    }
                }
            }
        }
    }
}
