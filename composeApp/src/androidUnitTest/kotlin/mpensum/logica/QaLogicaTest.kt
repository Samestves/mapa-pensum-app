package mpensum.logica

import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import mpensum.mapa.calcularFranja
import mpensum.mapa.calcularLayout
import mpensum.mapa.casillaLibre
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Los hallazgos del QA de lógica, uno por prueba: cada una falló antes de su arreglo */
class QaLogicaTest {

    private fun pensum(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
    private val hoy = Periodo(2026, 2)
    private val ambiental by lazy { pensum("ingenieria-ambiental") }
    private val sistemas by lazy { pensum("ingenieria-de-sistemas") }

    private fun todasLasObligatorias(p: Pensum) = p.obligatorias.associate { it.codigo to Marca.APROBADA }

    private fun tecnicas(n: Int) = ambiental.grupos.first { it.clave == "tecnica" }.asignaturas.take(n).map { it.codigo }
    private fun humanisticas(n: Int) = ambiental.grupos.first { it.clave == "humanistica" }.asignaturas.take(n).map { it.codigo }

    /** Tres técnicas y dos humanísticas: justo lo que piden las cinco casillas de Ambiental */
    private fun cincoElectivas() = tecnicas(3) + humanisticas(2)

    // ───────────────────────────── C1: las casillas de Ambiental ─────────────────────────────

    @Test
    fun ambientalConLasCincoElectivasAprobadasNoDebeNadaEsteDondeEste() {
        val marcas = todasLasObligatorias(ambiental) + cincoElectivas().associateWith { Marca.APROBADA }
        // Sin casilla (marcadas desde la lista o la ficha)
        assertEquals(0, estimarGraduacion(ambiental, marcas, emptyMap(), hoy = hoy).materiasRestantes)
        // Puestas en la franja libre con el selector: antes seguían pendientes las cinco genéricas
        val enFranja = tecnicas(3).mapIndexed { i, c -> casillaLibre("tecnica", i + 1) to c }.toMap() +
            humanisticas(2).mapIndexed { i, c -> casillaLibre("humanistica", i + 1) to c }.toMap()
        val e = estimarGraduacion(ambiental, marcas, enFranja, hoy = hoy)
        assertEquals(0, e.materiasRestantes)
        assertTrue(e.terminado, "semestres restantes: ${e.semestresRestantes}")
    }

    @Test
    fun elegirUnaElectivaEnLaFranjaNoSubeLoPendiente() {
        val marcas = todasLasObligatorias(ambiental)
        val antes = estimarGraduacion(ambiental, marcas, emptyMap(), hoy = hoy).materiasRestantes
        val despues = estimarGraduacion(ambiental, marcas, mapOf(casillaLibre("tecnica", 1) to tecnicas(1).single()), hoy = hoy).materiasRestantes
        assertEquals(antes, despues, "elegir una electiva cubre una casilla, no suma otra")
    }

    @Test
    fun lasElectivasMarcadasLlenanLasCasillasDeLosSemestresYNoLaFranja() {
        val marcas = todasLasObligatorias(ambiental) + cincoElectivas().associateWith { Marca.APROBADA }
        val efectivas = casillasEfectivas(ambiental, emptyMap(), marcas)
        val huecos = ambiental.asignaturas.filter { it.esHueco }
        assertEquals(huecos.map { it.codigo }.toSet(), efectivas.keys, "cada casilla de semestre lleva su electiva")
        assertEquals(cincoElectivas().toSet(), efectivas.values.toSet())
        // Cada una, en una casilla de su grupo
        for ((casilla, codigo) in efectivas) {
            val grupo = ambiental.grupos.first { g -> g.asignaturas.any { it.codigo == codigo } }.clave
            assertEquals(grupo, huecos.first { it.codigo == casilla }.grupo)
        }
        // Lo que se dibuja: ningún semestre queda con casillas por elegir y la franja no repite lo ya puesto
        val estados = estadosDe(ambiental.todas, marcas)
        val layout = calcularLayout(ambiental.asignaturas, ambiental.grupos)
        val porCodigo = ambiental.todas.associateBy { it.codigo }
        val cuentas = marcasDeSemestres(layout.nodos.map { it.asignatura }, { c -> efectivas[c]?.let(porCodigo::get) }, estados)
        assertTrue(cuentas.values.all { it.marca == MarcaSemestre.COMPLETO }, "$cuentas")
        val franja = calcularFranja(ambiental.grupos, efectivas.filterKeys { it.startsWith("libre-") }, layout.columnas.map { it.x }, layout.finSemestres)
        assertTrue(franja.filas.all { it.elegidas == 0 })
    }

    @Test
    fun unaElectivaPuestaEnLaFranjaPasaALaCasillaDelSemestreSiHayLibre() {
        val tecnica = tecnicas(1).single()
        val efectivas = casillasEfectivas(ambiental, mapOf(casillaLibre("tecnica", 1) to tecnica), emptyMap())
        val casilla = efectivas.entries.single { it.value == tecnica }.key
        assertTrue(ambiental.asignaturas.first { it.codigo == casilla }.esHueco, "se quedó en la franja: $casilla")
    }

    @Test
    fun lasQueSobranDeLasCasillasSiguenEnLaFranja() {
        val cuatro = tecnicas(4)
        val efectivas = casillasEfectivas(ambiental, emptyMap(), cuatro.associateWith { Marca.APROBADA })
        assertEquals(cuatro.toSet(), efectivas.values.toSet(), "ninguna aprobada desaparece del mapa")
        assertEquals(1, efectivas.keys.count { it.startsWith("libre-") })
    }

    @Test
    fun enSistemasLasCasillasSonLasQueElegisteSinAdopcion() {
        val tecnica = sistemas.grupos.first { it.clave == "tecnica" }.asignaturas.first().codigo
        val casilla = sistemas.asignaturas.first { it.esHueco && it.grupo == "tecnica" }.codigo
        assertEquals(mapOf(casilla to tecnica), casillasEfectivas(sistemas, mapOf(casilla to tecnica), mapOf(tecnica to Marca.APROBADA)))
        assertEquals(emptyMap(), casillasEfectivas(sistemas, emptyMap(), mapOf(tecnica to Marca.APROBADA)))
    }

    // ───────────────────────────── A1: las UC mínimas ─────────────────────────────

    @Test
    fun elProyectoDeGradoNoEsInscribibleHastaTener120Uc() {
        val proyecto = ambiental.obligatorias.first { it.requisitoEspecial != null }
        assertEquals(120, proyecto.ucMinimas)
        assertEquals(EstadoMateria.BLOQUEADA, estadosDe(ambiental.todas, emptyMap()).getValue(proyecto.codigo))
        var suma = 0
        val marcas = buildMap {
            for (a in ambiental.obligatorias) {
                if (a.codigo == proyecto.codigo || suma >= 120) continue
                put(a.codigo, Marca.APROBADA)
                suma += a.uc ?: 0
            }
        }
        assertTrue(suma >= 120)
        assertEquals(EstadoMateria.DISPONIBLE, estadosDe(ambiental.todas, marcas).getValue(proyecto.codigo))
        // Cursando no suma: solo lo aprobado
        assertEquals(EstadoMateria.BLOQUEADA, estadosDe(ambiental.todas, marcas.mapValues { Marca.CURSANDO }).getValue(proyecto.codigo))
    }

    // ───────────────────────────── A3: la carga con pocas aprobadas ─────────────────────────────

    @Test
    fun unaAprobadaDelSemestre5NoDaUnaFechaAbsurda() {
        val una = sistemas.obligatorias.first { it.semestre == 5 }.codigo
        val e = estimarGraduacion(sistemas, mapOf(una to Marca.APROBADA), emptyMap(), hoy = hoy)
        assertEquals(FuenteCarga.PENSUM, e.fuenteCarga)
        assertTrue(e.carga > 1)
        assertTrue(e.semestresRestantes <= 14, "${e.semestresRestantes} semestres")
        // Un semestre completo más una del quinto tampoco
        val s1 = sistemas.obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA }
        val e2 = estimarGraduacion(sistemas, s1 + (una to Marca.APROBADA), emptyMap(), hoy = hoy)
        assertTrue(e2.semestresRestantes <= 14, "${e2.semestresRestantes} semestres")
    }

    // ───────────────────────────── A4: el semestre actual ─────────────────────────────

    @Test
    fun portadaBarraYListaDicenElMismoSemestre() {
        val s1 = sistemas.obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA }
        val cursando = sistemas.obligatorias.filter { it.semestre == 3 }.take(3).associate { it.codigo to Marca.CURSANDO }
        val marcas = s1 + cursando
        val portada = resumenParaPortada(sistemas, marcas.mapValues { it.value.valor }).semestreActual
        val barra = semestreActual(capsulasDe(sistemas, marcas) { null })
        val estados = estadosDe(sistemas.todas, marcas)
        val situaciones = sistemas.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
        val layout = calcularLayout(sistemas.asignaturas, sistemas.grupos)
        val lista = modeloLista(layout.nodos.map { it.asignatura }, sistemas.grupos, { null }, estados, situaciones, { null }, null).actual
        assertEquals(3, barra)
        assertEquals(barra, portada)
        assertEquals(barra, lista)
    }

    @Test
    fun sinNadaEnCursoVaElPrimeroSinCompletar() {
        val s1 = sistemas.obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA }
        assertEquals(2, resumenParaPortada(sistemas, s1.mapValues { it.value.valor }).semestreActual)
        assertEquals(1, resumenParaPortada(sistemas, emptyMap()).semestreActual)
    }

    // ───────────────────────────── M4: un solo tope de carga ─────────────────────────────

    @Test
    fun unSoloTopeDeCarga() {
        // Quien cursa diez ve el chip 8 y la fecha se calcula con 8: lo que ves es lo que cuenta
        val diez = sistemas.obligatorias.filter { it.semestre == 1 || it.semestre == 2 }.take(10).associate { it.codigo to Marca.CURSANDO }
        assertEquals(MAX_CARGA, estimarGraduacion(sistemas, diez, emptyMap(), hoy = hoy).carga)
        assertEquals(MAX_CARGA, estimarGraduacion(sistemas, diez, emptyMap(), 12, hoy).carga)
    }

    // ───────────────────────────── M1: importar sobre un horario guardado ─────────────────────────────

    @Test
    fun leerDosVecesLaMismaCapturaNoDuplicaLasClases() {
        val guardadas = listOf(Sesion("a", "X", 0, 420, 520), Sesion("b", "Y", 2, 600, 700))
        val leidas = listOf(Sesion("leida-0-0", "X", 0, 420, 520), Sesion("leida-1-0", "Z", 1, 480, 580))
        assertEquals(listOf("Z"), sinRepetidas(leidas, guardadas).map { it.codigo })
        // Otra hora de la misma materia sí es otra clase
        assertEquals(1, sinRepetidas(listOf(Sesion("n", "X", 0, 540, 600)), guardadas).size)
    }

    @Test
    fun laImportacionAvisaDeLosChoquesConLoGuardado() {
        val guardadas = listOf(Sesion("a", "X", 0, 420, 520))
        val nueva = Sesion("leida-0-0", "Z", 0, 480, 580)
        assertEquals(listOf(guardadas[0] to nueva), choquesAlImportar(listOf(nueva), guardadas))
        assertTrue(choquesAlImportar(listOf(Sesion("n", "Z", 1, 480, 580)), guardadas).isEmpty())
    }
}
