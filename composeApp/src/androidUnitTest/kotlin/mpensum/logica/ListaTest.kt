package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import mpensum.mapa.calcularLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Qué fila va en qué grupo y qué dice, contra el pensum real de Sistemas */
class ListaTest {

    private fun pensum(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    /** Sistemas como en las maquetas: semestres 1 a 4 aprobados salvo las electivas, y dos del quinto en curso */
    private fun modelo(p: Pensum, filtro: FiltroLista? = null, fijadas: Map<String, FiltroLista> = emptyMap(), extra: Map<String, Marca> = emptyMap()): ModeloLista {
        val marcas = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO) + extra
        val estados = estadosDe(p.todas, marcas)
        val situaciones = p.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
        val layout = calcularLayout(p.asignaturas, p.grupos)
        val nombres = p.todas.associate { it.codigo to it.nombre }
        return modeloLista(layout.nodos.map { it.asignatura }, p.grupos, { null as Asignatura? }, estados, situaciones, nombres::get, filtro, fijadas)
    }

    @Test
    fun cuentaComoLasMaquetas() {
        val m = modelo(pensum("ingenieria-de-sistemas"))
        assertEquals(mapOf(FiltroLista.APROBADAS to 23, FiltroLista.CURSANDO to 2, FiltroLista.DISPONIBLES to 6, FiltroLista.BLOQUEADAS to 18), m.cuentas)
        assertEquals(5, m.actual)
        assertEquals((1..10).map { "s$it" }, m.secciones.map { it.clave })
    }

    @Test
    fun resumenesDeSemestre() {
        val s = modelo(pensum("ingenieria-de-sistemas")).secciones
        assertEquals("Completo", s[0].resumen)
        assertEquals("Falta la electiva", s[3].resumen)
        assertEquals("0 de 7 aprobadas", s[4].resumen)
        assertEquals(" · 2 cursando", s[4].coletilla)
        // La casilla sin elegir va al final del semestre, con cuántas opciones hay
        assertEquals("Elige 1 de 14", s[3].casillas.single().nota)
    }

    @Test
    fun loQueDiceCadaFila() {
        val quinto = modelo(pensum("ingenieria-de-sistemas")).secciones[4].materias.associateBy { it.codigo }
        // El estado lo dicen el icono y el color: la nota solo trae un dato
        assertEquals("", quinto.getValue("0624622").nota)
        assertEquals("Cursando", quinto.getValue("0624622").descripcion.substringAfter(", "))
        assertEquals("Abre 1", quinto.getValue("0713172").nota)
        assertEquals("Puedes inscribirla", quinto.getValue("0713172").descripcion.substringAfter(", "))
        val sexto = modelo(pensum("ingenieria-de-sistemas")).secciones[5].materias.associateBy { it.codigo }
        // Análisis de Decisiones pide Inferencia, que estás cursando: se abre el semestre que viene
        assertEquals("Pide Inferencia y Diseño de Experimentos", sexto.getValue("0713653").nota)
        assertEquals("Pide Sistemas de Costo", sexto.getValue("0713113").nota)
    }

    @Test
    fun elFiltroDejaSoloLosDeEseEstado() {
        val m = modelo(pensum("ingenieria-de-sistemas"), FiltroLista.DISPONIBLES)
        assertEquals(listOf("s5", "s9"), m.secciones.map { it.clave })
        assertEquals("5 disponibles · 13 UC", m.secciones[0].resumen)
        assertEquals("1 disponible · 2 UC", m.secciones[1].resumen)
        assertTrue(m.secciones.all { it.casillas.isEmpty() })
    }

    @Test
    fun laFilaMarcadaSeQuedaMientrasSePuedeDeshacer() {
        val p = pensum("ingenieria-de-sistemas")
        val marcada = mapOf("0713172" to Marca.CURSANDO)
        val suelta = modelo(p, FiltroLista.DISPONIBLES, extra = marcada)
        assertTrue(suelta.secciones[0].materias.none { it.codigo == "0713172" })
        val retenida = modelo(p, FiltroLista.DISPONIBLES, fijadas = mapOf("0713172" to FiltroLista.DISPONIBLES), extra = marcada)
        assertEquals(Situacion.CURSANDO, retenida.secciones[0].materias.first { it.codigo == "0713172" }.situacion)
        // Las cuentas de arriba sí dicen la verdad desde el primer momento
        assertEquals(5, retenida.cuentas[FiltroLista.DISPONIBLES])
    }

    @Test
    fun elCicloDelToque() {
        assertEquals(Marca.CURSANDO, siguienteMarca(null))
        assertEquals(Marca.APROBADA, siguienteMarca(Marca.CURSANDO))
        assertEquals(null, siguienteMarca(Marca.APROBADA))
    }
}
