package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import mpensum.mapa.calcularLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La lista «Por hacer»: qué va en qué sección, en qué orden y qué dice cada fila, contra el pensum real de Sistemas */
class ListaTest {

    private val p: Pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())

    private val costo = "0713172"
    private val metodos = "0723913"
    private val operacion = "0713643"
    private val circuitos = "0713463"
    private val teoria = "0713632"
    private val leyes = "0625822"

    /** Sistemas como en las maquetas: semestres 1 a 4 aprobados y dos del quinto en curso */
    private val base = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
        mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO)

    private data class Foto(val u: UniversoLista, val actual: Int?)

    private fun foto(marcas: Map<String, Marca> = base, enCasilla: (String) -> Asignatura? = { null }): Foto {
        val estados = estadosDe(p.todas, marcas)
        val situaciones = p.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
        val nodos = calcularLayout(p.asignaturas, p.grupos).nodos.map { it.asignatura }
        val nombres = p.todas.associate { it.codigo to it.nombre }
        return Foto(universoDeLista(nodos, p.grupos, enCasilla, estados, situaciones, nombres::get), semestreActualDeLista(nodos, enCasilla, estados))
    }

    private val margenes = mapOf(costo to 0, metodos to 1, operacion to 1, circuitos to 2, teoria to 3, leyes to 5)

    private fun grupo(f: Foto, clave: String, m: Map<String, Int> = margenes) =
        estructuraPorEstado(f.u, m, f.actual).firstOrNull { it.clave == clave }?.codigos.orEmpty()

    @Test
    fun cuatroSeccionesEnSuOrdenYConLasCuentasDeLaMaqueta() {
        val f = foto()
        val grupos = estructuraPorEstado(f.u, margenes, f.actual)
        assertEquals(listOf("inscribir", "cursando", "bloqueadas", "aprobadas"), grupos.map { it.clave })
        assertEquals(6, f.u.materias.count { it.situacion == Situacion.INSCRIBIBLE })
        assertEquals(2, grupo(f, "cursando").size)
        assertEquals(18, grupo(f, "bloqueadas").size)
        assertEquals(23, grupo(f, "aprobadas").size)
        assertEquals("2 · 4 UC", metaDeGrupo("cursando", f.u))
        assertEquals("18", metaDeGrupo("bloqueadas", f.u))
        assertEquals("23 · 62 UC", metaDeGrupo("aprobadas", f.u))
    }

    @Test
    fun loInscribibleVaPrimeroLoQueNoTieneMargen() {
        val f = foto()
        val inscribir = grupo(f, "inscribir")
        // Sin margen, luego el que menos puede esperar y, a igual margen, la cadena más larga
        assertEquals(listOf(costo, metodos, operacion, circuitos, teoria, leyes), inscribir.take(6))
        // Una materia con margen medido más bajo pasa por delante aunque su cadena sea más corta
        val alReves = grupo(f, "inscribir", mapOf(teoria to 0, costo to 4, metodos to 4, operacion to 4, circuitos to 4, leyes to 4))
        assertEquals(teoria, alReves.first())
    }

    @Test
    fun sinMargenMedidoSeOrdenaPorLaCadena() {
        val f = foto()
        assertEquals(listOf(costo, metodos, operacion, circuitos, teoria, leyes), grupo(f, "inscribir", emptyMap()).take(6))
    }

    @Test
    fun laElectivaSinElegirSaleJuntoALoInscribibleUnaSolaVez() {
        val f = foto()
        val inscribir = grupo(f, "inscribir")
        // Tres sociohumanísticas vacías (semestres 2 a 4) son una fila: la más atrasada; las técnicas aún no tocan
        assertEquals(7, inscribir.size)
        assertEquals("casilla-humanistica-1", inscribir.last())
        val c = f.u.casilla(inscribir.last())!!
        assertEquals("Faltan 3 · elige 1 de 14", filaDeCasilla(c, resumida = true).razon)
        assertEquals("Del semestre 2 · elige 1 de 14", filaDeCasilla(c).razon)
        assertTrue(filaDeCasilla(c).esCasilla)
    }

    @Test
    fun unaElectivaElegidaCuentaComoCualquierMateriaConElSemestreDeSuCasilla() {
        val elegida = p.grupos.first { it.clave == "humanistica" }.asignaturas.first()
        val f = foto(base + (elegida.codigo to Marca.CURSANDO)) { if (it == "casilla-humanistica-1") elegida else null }
        val m = f.u.materia(elegida.codigo)!!
        assertEquals(2, m.semestre)
        assertEquals(Situacion.CURSANDO, m.situacion)
        assertNull(f.u.casilla("casilla-humanistica-1"))
    }

    @Test
    fun laRazonDeCadaFilaEsUnaSola() {
        val f = foto()
        fun razon(c: String) = razonDe(f.u.materia(c)!!, margenes[c])
        assertEquals("**Sin margen** · abre 5 en cadena", razon(costo))
        assertEquals("Puede esperar 1 · abre Optimización de Operaciones", razon(metodos))
        assertEquals("Puede esperar 2 · abre Electrónica", razon(circuitos))
        assertEquals("Puede esperar 5", razon(leyes))
        // Mientras el margen se mide, solo se dice lo que abre
        assertEquals("Abre Electrónica", razonDe(f.u.materia(circuitos)!!, null))
        assertEquals("Puedes inscribirla", razonDe(f.u.materia(leyes)!!, null))
        // Cursando: qué se abre al aprobarla, o en qué semestre va
        assertEquals("Semestre 5", razon("0624622"))
        assertEquals("Al aprobarla se abre Análisis de Decisiones", razon("0713122"))
        // Bloqueadas: lo que ya cursas, o lo que falta
        assertEquals("Al aprobar Inferencia y Diseño de Experimentos", razon("0713653"))
        assertEquals("Falta Sistemas de Costo", razon("0713113"))
        assertEquals("Falta Modelos de Operaciones II y 1 más", razon("0715116"))
        assertEquals("Semestre 3", razon("0052134"))
    }

    @Test
    fun enBloqueadasPrimeroLaQueSoloEsperaLoQueCursas() {
        val f = foto()
        val bloqueadas = grupo(f, "bloqueadas")
        assertEquals("0713653", bloqueadas.first())
        assertEquals(Situacion.PROXIMA, f.u.materia(bloqueadas.first())!!.situacion)
        assertTrue(bloqueadas.drop(1).all { f.u.materia(it)!!.situacion == Situacion.LEJANA })
    }

    @Test
    fun laCadenaCuentaLoPendienteQueCuelgaEnFila() {
        val f = foto()
        assertEquals(5, f.u.materia(costo)!!.cadena)
        assertEquals(0, f.u.materia(leyes)!!.cadena)
        // Lo aprobado ya no frena nada
        assertEquals(0, f.u.materia("0722123")!!.cadena)
    }

    @Test
    fun porSemestreEsElOrdenDelPensumConSusCasillas() {
        val f = foto()
        val grupos = estructuraPorSemestre(f.u)
        assertEquals((1..10).map { "s$it" }, grupos.map { it.clave })
        assertEquals("Semestre 5", tituloDeGrupo("s5"))
        assertEquals("0 de 7 · 17 UC", metaDeGrupo("s5", f.u))
        // Cada materia aparece una vez y las casillas al final de su semestre
        val s2 = grupos.first { it.clave == "s2" }.codigos
        assertEquals("casilla-humanistica-1", s2.last())
        assertEquals(grupos.sumOf { it.codigos.size }, f.u.materias.size + f.u.casillas.size)
    }

    @Test
    fun elSemestreActualEsElDeLaBarra() {
        assertEquals(5, foto().actual)
    }

    @Test
    fun elPasoSiguienteDeUnToque() {
        assertEquals(Paso(Marca.CURSANDO, "Cursando"), pasoSiguiente(Situacion.INSCRIBIBLE))
        assertEquals(Paso(Marca.APROBADA, "Aprobada"), pasoSiguiente(Situacion.CURSANDO))
        assertNull(pasoSiguiente(Situacion.HECHA))
        assertNull(pasoSiguiente(Situacion.PROXIMA))
        assertNull(pasoSiguiente(Situacion.LEJANA))
    }

    @Test
    fun elMuelleOfreceElPasoQueTocaConSuConsecuencia() {
        val f = foto()
        val ins = planMuelle(f.u.materia(circuitos)!!, ucCursando = 4)
        assertEquals("Sumas **3 UC** a tu semestre · llevas 4", ins.consecuencia)
        assertEquals(AccionMarca("La estoy cursando", Marca.CURSANDO), ins.principal)
        assertEquals(AccionMarca("Ya la aprobé", Marca.APROBADA), ins.secundaria)
        assertTrue(ins.lleno)

        val cur = planMuelle(f.u.materia("0713122")!!, ucCursando = 4)
        assertEquals("Al aprobarla se te abre **Análisis de Decisiones**", cur.consecuencia)
        assertEquals(AccionMarca("Ya la aprobé", Marca.APROBADA), cur.principal)
        assertEquals(AccionMarca("Ya no la curso", null), cur.secundaria)

        val bloq = planMuelle(f.u.materia("0713113")!!, ucCursando = 4)
        assertEquals("Te falta **Sistemas de Costo**", bloq.consecuencia)
        assertTrue(!bloq.lleno)

        val hecha = planMuelle(f.u.materia("0052134")!!, ucCursando = 4)
        assertNull(hecha.principal)
        assertEquals(null, hecha.secundaria.marca)
    }

    @Test
    fun laFilaMarcadaDiceQuePaso() {
        assertEquals("Pasó a cursando", razonTrasMarcar(Marca.CURSANDO))
        assertEquals("Aprobada", razonTrasMarcar(Marca.APROBADA))
        assertEquals("Sin cursar", razonTrasMarcar(null))
    }

    @Test
    fun aprobarLoQueCursasAbreMateriasNuevasEnLaSeccion() {
        val f = foto(base + mapOf("0713122" to Marca.APROBADA))
        assertEquals(Situacion.INSCRIBIBLE, f.u.materia("0713653")!!.situacion)
        assertTrue("0713653" in grupo(f, "inscribir"))
        assertEquals(1, grupo(f, "cursando").size)
    }

    @Test
    fun elSubtituloDiceLoAvanzadoYCuandoTeGradúas() {
        assertEquals("**62** de 153 UC · te gradúas **Dic 2029**", subtituloDeLista(62, 153, 23, Periodo(2029, 2)))
        assertEquals("**23** aprobadas", subtituloDeLista(0, null, 23, null))
        assertEquals("**1** aprobada · te gradúas **Jun 2030**", subtituloDeLista(0, null, 1, Periodo(2030, 1)))
    }

    @Test
    fun elMargenSeDibujaUnPuntoPorSemestre() {
        val puntos = puntosDeMargen(10, semestre = 5, margen = 2)
        assertEquals(10, puntos.size)
        assertEquals(List(4) { PuntoMargen.HECHO }, puntos.take(4))
        assertEquals(PuntoMargen.ACTUAL, puntos[4])
        assertEquals(listOf(PuntoMargen.VENTANA, PuntoMargen.VENTANA), puntos.subList(5, 7))
        assertEquals(List(3) { PuntoMargen.LEJOS }, puntos.drop(7))
        // Sin margen solo hay el aro
        assertEquals(0, puntosDeMargen(10, 5, 0).count { it == PuntoMargen.VENTANA })
        // Cerca del final la ventana no se sale
        assertEquals(10, puntosDeMargen(10, 10, 3).size)
    }

    @Test
    fun descendientesCuentaDirectosEIndirectosUnaVez() {
        val adelante = mapOf("a" to listOf("b", "c"), "b" to listOf("d"), "c" to listOf("d"), "d" to emptyList())
        assertEquals(3, descendientes("a", adelante))
        assertEquals(0, descendientes("d", adelante))
        // Un ciclo no cuelga
        assertEquals(2, descendientes("x", mapOf("x" to listOf("y"), "y" to listOf("x"))))
    }

    @Test
    fun laDescripcionParaElLectorDicePorPalabrasLoQueElIconoDice() {
        val f = foto()
        assertEquals("Circuitos y Sistemas, Puedes inscribirla", filaDe(f.u.materia(circuitos)!!, 2).descripcion)
        assertEquals("Economía de Empresas, Bloqueada. Falta Sistemas de Costo", filaDe(f.u.materia("0713113")!!, null).descripcion)
        assertEquals("Física II, Aprobada", filaDe(f.u.materia("0052134")!!, null).descripcion)
    }
}
