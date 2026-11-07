package mpensum.logica

import mpensum.datos.Asignatura
import mpensum.datos.ColorCarrera
import mpensum.datos.Pensum
import mpensum.datos.Semestre
import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cuándo te gradúas, contra los pensums de verdad y contra uno armado a mano para las cadenas */
class GraduacionTest {

    private val carpeta = File("src/commonMain/composeResources/files/carreras")
    private fun pensum(slug: String) = leerPensum(File(carpeta, "$slug.json").readText())
    private val carreras by lazy { carpeta.listFiles()!!.filter { it.extension == "json" }.sorted().map { leerPensum(it.readText()) } }

    /** Octubre de 2026: el lapso II en curso */
    private val hoy = Periodo(2026, 2)

    private val sistemas by lazy { pensum("ingenieria-de-sistemas") }

    /** Sistemas con primero a cuarto aprobados y dos de quinto en curso */
    private val sistemasACuarto by lazy {
        buildMap {
            sistemas.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 5 }.take(2).forEach { put(it.codigo, Marca.CURSANDO) }
        }
    }

    private fun todoAprobado(p: Pensum) = p.todas.associate { it.codigo to Marca.APROBADA }

    /** Cada pendiente sale una sola vez en el plan, y nunca antes que sus prelaciones */
    private fun assertPlanValido(p: Pensum, marcas: Map<String, Marca>, e: EstimacionGraduacion) {
        val codigos = e.plan.flatten()
        assertEquals(codigos.size, codigos.toSet().size, "${p.slug}: una materia dos veces en el plan")
        assertEquals(e.materiasRestantes, codigos.size, "${p.slug}: el plan no reparte todo lo que falta")
        val porCodigo = p.todas.associateBy { it.codigo }
        val periodoDe = e.plan.flatMapIndexed { i, periodo -> periodo.map { it to i } }.toMap()
        for ((codigo, i) in periodoDe) {
            for (pre in porCodigo[codigo]?.prerrequisitos.orEmpty()) {
                val suyo = periodoDe[pre] ?: continue
                assertTrue(suyo < i, "${p.slug}: $codigo va con o antes que su prelación $pre")
            }
        }
        assertTrue(e.semestresRestantes >= e.minimoPorPrelaciones)
        assertTrue(e.plan.all { it.size <= e.carga || it.size == 1 })
    }

    @Test
    fun periodosDeLaUdo() {
        assertEquals(Periodo(2026, 1), Periodo.de(2026, 1))
        assertEquals(Periodo(2026, 1), Periodo.de(2026, 6))
        assertEquals(Periodo(2026, 2), Periodo.de(2026, 7))
        assertEquals(Periodo(2026, 2), Periodo.de(2026, 10))
        // El corte se puede mover si la UDO arranca el II más tarde
        assertEquals(Periodo(2026, 1), Periodo.de(2026, 8, inicioLapsoII = 9))
        assertEquals(Periodo(2027, 1), hoy.mas(1))
        assertEquals(Periodo(2028, 2), hoy.mas(4))
        assertEquals(Periodo(2026, 1), hoy.mas(-1))
        assertEquals("2028-II", Periodo(2028, 2).toString())
        assertEquals("2027-I", Periodo(2027, 1).toString())
        assertEquals("Dic 2029", Periodo(2029, 2).mesYAnio())
        assertEquals("Jun 2030", Periodo(2030, 1).mesYAnio())
        assertEquals("Ene – Jun 2027", Periodo(2027, 1).rango())
        assertEquals("Jul – Dic 2026", Periodo(2026, 2).rango())
    }

    @Test
    fun sistemasConCuartoAprobadoYDosCursando() {
        val e = estimarGraduacion(sistemas, sistemasACuarto, emptyMap(), hoy = hoy)
        // Dos en curso son menos que su ritmo (23 aprobadas en cuatro semestres): manda el ritmo, no 17 semestres
        val aprobadas = sistemasACuarto.count { it.value == Marca.APROBADA }
        assertEquals(FuenteCarga.RITMO, e.fuenteCarga)
        assertEquals(redondear(aprobadas / 4.0), e.carga)
        assertTrue(e.cursaAhora)
        assertPlanValido(sistemas, sistemasACuarto, e)
        // El período en curso cuenta: el último simulado es hoy + (restantes - 1)
        assertEquals(hoy.mas(e.semestresRestantes - 1), e.periodoFinal)
        // Las dos que cursa no vuelven a salir en el plan
        val cursando = sistemasACuarto.filterValues { it == Marca.CURSANDO }.keys
        assertTrue(e.plan.flatten().none { it in cursando })

        // ¿Y si meto más? Nunca tarda más, y con mucha carga manda la cadena
        val dos = estimarGraduacion(sistemas, sistemasACuarto, emptyMap(), 2, hoy)
        val cinco = estimarGraduacion(sistemas, sistemasACuarto, emptyMap(), 5, hoy)
        val siete = estimarGraduacion(sistemas, sistemasACuarto, emptyMap(), 7, hoy)
        assertEquals(e.semestresRestantes, estimarGraduacion(sistemas, sistemasACuarto, emptyMap(), e.carga, hoy).semestresRestantes)
        assertTrue(e.semestresRestantes in 5..8, "con su ritmo: ${e.semestresRestantes}")
        assertTrue(dos.semestresRestantes > cinco.semestresRestantes)
        assertTrue(cinco.semestresRestantes >= siete.semestresRestantes)
        assertEquals(FuenteCarga.ELEGIDA, cinco.fuenteCarga)
        assertTrue(siete.semestresRestantes >= siete.minimoPorPrelaciones)
        println("Sistemas 1-4 + 2 cursando (hoy $hoy): " +
            listOf(e, dos, cinco, siete).joinToString { "carga ${it.carga} → ${it.semestresRestantes} sem, ${it.periodoFinal} (mín ${it.minimoPorPrelaciones})" })
    }

    @Test
    fun siCursasMasQueTuRitmoMandaLoQueCursas() {
        // Primero aprobado y segundo y tercero en curso a la vez: cursa más de lo que llevaba por semestre
        val marcas = buildMap {
            sistemas.obligatorias.filter { it.semestre == 1 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 2 || it.semestre == 3 }.forEach { put(it.codigo, Marca.CURSANDO) }
        }
        val e = estimarGraduacion(sistemas, marcas, emptyMap(), hoy = hoy)
        assertEquals(FuenteCarga.CURSANDO, e.fuenteCarga)
        assertEquals(minOf(MAX_CARGA, marcas.count { it.value == Marca.CURSANDO }), e.carga)
        assertPlanValido(sistemas, marcas, e)
    }

    @Test
    fun soloDosCursandoSinAprobadasVaAlPasoDelPensum() {
        // Quien solo marcó dos en curso casi seguro no marcó todo: se toma un semestre típico, no dos materias
        val marcas = sistemas.obligatorias.filter { it.semestre == 1 }.take(2).associate { it.codigo to Marca.CURSANDO }
        val e = estimarGraduacion(sistemas, marcas, emptyMap(), hoy = hoy)
        assertEquals(FuenteCarga.PENSUM, e.fuenteCarga)
        assertTrue(e.carga > 2)
        assertTrue(e.semestresRestantes <= sistemas.semestres.size + 3, "${e.semestresRestantes}")
    }

    @Test
    fun conPocasAprobadasSeSigueSuRitmo() {
        // Primero entero y una de segundo, sin cursar nada: siete en dos semestres recorridos
        val marcas = buildMap {
            sistemas.obligatorias.filter { it.semestre == 1 }.forEach { put(it.codigo, Marca.APROBADA) }
            put(sistemas.obligatorias.first { it.semestre == 2 && it.prerrequisitos.isEmpty() }.codigo, Marca.APROBADA)
        }
        val e = estimarGraduacion(sistemas, marcas, emptyMap(), hoy = hoy)
        assertEquals(FuenteCarga.RITMO, e.fuenteCarga)
        assertEquals(redondear(marcas.size / 2.0), e.carga)
        assertTrue(!e.cursaAhora)
        // Sin nada en curso, lo simulado empieza el período que viene
        assertEquals(hoy.mas(e.semestresRestantes), e.periodoFinal)
        assertPlanValido(sistemas, marcas, e)
    }

    @Test
    fun lasNueveSinMarcasYConTodoAprobado() {
        assertEquals(9, carreras.size)
        for (p in carreras) {
            val nueva = estimarGraduacion(p, emptyMap(), emptyMap(), hoy = hoy)
            assertEquals(FuenteCarga.PENSUM, nueva.fuenteCarga, p.slug)
            assertPlanValido(p, emptyMap(), nueva)
            // Un nuevo ingreso con la carga típica tarda más o menos lo que dice el pensum
            assertTrue(nueva.semestresRestantes in p.semestres.size - 1..p.semestres.size + 3, "${p.slug}: ${nueva.semestresRestantes}")
            assertTrue(nueva.minimoPorPrelaciones in 2..p.semestres.size, "${p.slug}: mínimo ${nueva.minimoPorPrelaciones}")
            assertEquals(nueva.minimoPorPrelaciones, nueva.cuelloDeBotella.size, p.slug)
            assertEquals(hoy.mas(nueva.semestresRestantes), nueva.periodoFinal)

            val lista = estimarGraduacion(p, todoAprobado(p), emptyMap(), hoy = hoy)
            assertEquals(0, lista.semestresRestantes, p.slug)
            assertEquals(0, lista.minimoPorPrelaciones, p.slug)
            assertTrue(lista.terminado)
            assertNull(lista.periodoFinal)
            assertTrue(lista.plan.isEmpty() && lista.cuelloDeBotella.isEmpty())
        }
    }

    @Test
    fun masCargaNuncaAlarga() {
        for (p in carreras) {
            val porCarga = listOf(1, 3, 6, 9).map { estimarGraduacion(p, emptyMap(), emptyMap(), it, hoy) }
            porCarga.forEach { assertPlanValido(p, emptyMap(), it) }
            val semestres = porCarga.map { it.semestresRestantes }
            assertEquals(semestres.sortedDescending(), semestres, "${p.slug}: $semestres")
            // Con una sola materia por período, es un período por materia
            assertEquals(porCarga[0].materiasRestantes, semestres[0], p.slug)
            // Con mucha carga no se baja del camino crítico
            assertTrue(semestres[3] >= porCarga[3].minimoPorPrelaciones)
        }
    }

    @Test
    fun soloElectivasPendientes() {
        // Sistemas: obligatorias listas y ninguna electiva; faltan las 8 casillas (5 técnicas de 3 UC y 3 humanísticas de 2)
        val obligatorias = sistemas.obligatorias.associate { it.codigo to Marca.APROBADA }
        val e = estimarGraduacion(sistemas, obligatorias, emptyMap(), 4, hoy)
        assertEquals(8, e.materiasRestantes)
        assertEquals(1, e.minimoPorPrelaciones)
        assertEquals(2, e.semestresRestantes)
        assertTrue(e.plan.flatten().all { it.startsWith("casilla-") }, "${e.plan}")

        // Ambiental: cada casilla de los semestres pide su electiva aunque no haya cuota
        val ambiental = pensum("ingenieria-ambiental")
        val a = estimarGraduacion(ambiental, ambiental.obligatorias.associate { it.codigo to Marca.APROBADA }, emptyMap(), 5, hoy)
        assertEquals(ambiental.asignaturas.count { it.esHueco }, a.materiasRestantes)
        assertEquals(1, a.semestresRestantes)

        // Una electiva aprobada sin casilla cubre la casilla más temprana
        val tecnica = ambiental.grupos.first { it.clave == "tecnica" }.asignaturas.first()
        val conUna = estimarGraduacion(ambiental, ambiental.obligatorias.associate { it.codigo to Marca.APROBADA } + (tecnica.codigo to Marca.APROBADA), emptyMap(), 5, hoy)
        assertEquals(a.materiasRestantes - 1, conUna.materiasRestantes)
    }

    @Test
    fun laElectivaElegidaEntraConSusPrelaciones() {
        val casilla = sistemas.asignaturas.first { it.esHueco && it.grupo == "tecnica" }
        val conPrelacion = sistemas.grupos.first { it.clave == "tecnica" }.asignaturas.first { it.prerrequisitos.isNotEmpty() }
        val e = estimarGraduacion(sistemas, emptyMap(), mapOf(casilla.codigo to conPrelacion.codigo), hoy = hoy)
        assertTrue(conPrelacion.codigo in e.plan.flatten())
        assertTrue(casilla.codigo !in e.plan.flatten(), "la casilla ya no es genérica")
        assertPlanValido(sistemas, emptyMap(), e)
    }

    @Test
    fun laCuotaSeCuentaEnUc() {
        // La técnica de 1 UC no cubre una casilla de 3: siguen faltando 14 UC, cinco casillas
        val tecnica = sistemas.grupos.first { it.clave == "tecnica" }.asignaturas
        val deUna = tecnica.first { it.uc == 1 }
        val base = sistemas.obligatorias.associate { it.codigo to Marca.APROBADA }
        assertEquals(8, estimarGraduacion(sistemas, base + (deUna.codigo to Marca.APROBADA), emptyMap(), 9, hoy).materiasRestantes)
        val deTres = tecnica.first { it.uc == 3 && it.prerrequisitos.isEmpty() }
        assertEquals(7, estimarGraduacion(sistemas, base + (deTres.codigo to Marca.APROBADA), emptyMap(), 9, hoy).materiasRestantes)
    }

    @Test
    fun lasUcMinimasEsperanSuTurno() {
        val ambiental = pensum("ingenieria-ambiental")
        val especial = ambiental.obligatorias.first { it.requisitoEspecial?.contains("UC") == true }
        val e = estimarGraduacion(ambiental, emptyMap(), emptyMap(), 9, hoy)
        val periodo = e.plan.indexOfFirst { especial.codigo in it }
        val ucAntes = e.plan.take(periodo).flatten().sumOf { c -> ambiental.todas.firstOrNull { it.codigo == c }?.uc ?: 3 }
        assertTrue(ucAntes >= 120, "entra con $ucAntes UC")
    }

    @Test
    fun loAtrasadoVaPrimero() {
        // Todo hasta sexto aprobado salvo una de primero que no abre nada: la ventana de nivel la empuja al frente
        val vieja = sistemas.obligatorias.first { o -> o.semestre == 1 && sistemas.todas.none { o.codigo in it.prerrequisitos } }
        val marcas = sistemas.obligatorias.filter { (it.semestre ?: 99) <= 6 && it.codigo != vieja.codigo }.associate { it.codigo to Marca.APROBADA }
        val e = estimarGraduacion(sistemas, marcas, emptyMap(), 3, hoy)
        assertTrue(vieja.codigo in e.plan.first(), "${e.plan.first()}")
        val alDia = estimarGraduacion(sistemas, marcas + (vieja.codigo to Marca.APROBADA), emptyMap(), 3, hoy)
        assertTrue(e.semestresRestantes >= alDia.semestresRestantes)
        assertPlanValido(sistemas, marcas, e)
    }

    /** Un pensum de juguete: una cadena de seis y un puñado de sueltas */
    private fun conCadena(): Pensum {
        val cadena = (1..6).map { Asignatura("C$it", "Cadena $it", semestre = it, uc = 3, prerrequisitos = if (it > 1) listOf("C${it - 1}") else emptyList()) }
        val sueltas = (1..4).map { Asignatura("S$it", "Suelta $it", semestre = 1, uc = 3) }
        return Pensum(
            slug = "juguete", nombre = "Juguete", nombreCorto = "J", color = ColorCarrera("#000000", "#000000"),
            semestres = (1..6).map { s -> Semestre(s, (cadena + sueltas).count { it.semestre == s }, 0) },
            asignaturas = cadena + sueltas,
        )
    }

    @Test
    fun laCadenaLargaManda() {
        val p = conCadena()
        val e = estimarGraduacion(p, emptyMap(), emptyMap(), 9, hoy)
        assertEquals(6, e.minimoPorPrelaciones)
        assertEquals(6, e.semestresRestantes)
        assertEquals((1..6).map { "C$it" }, e.cuelloDeBotella)
        // La cadena entra la primera del período, aunque la carga sea de una
        val una = estimarGraduacion(p, emptyMap(), emptyMap(), 1, hoy)
        assertEquals("C1", una.plan.first().single())
        assertEquals(10, una.semestresRestantes)
        // A mitad de la cadena y cursando el eslabón siguiente, solo queda lo que cuelga de él
        val sueltas = (1..4).associate { "S$it" to Marca.APROBADA }
        val mitad = estimarGraduacion(p, sueltas + mapOf("C1" to Marca.APROBADA, "C2" to Marca.APROBADA, "C3" to Marca.CURSANDO), emptyMap(), 9, hoy)
        assertEquals(4, mitad.minimoPorPrelaciones) // este período y C4, C5, C6
        assertEquals(4, mitad.semestresRestantes)
        assertEquals(hoy.mas(3), mitad.periodoFinal)
        // Con las sueltas de primero sin aprobar, la ventana de nivel no deja pasar a C4 hasta sacarlas
        val atrasado = estimarGraduacion(p, mapOf("C1" to Marca.APROBADA, "C2" to Marca.APROBADA, "C3" to Marca.CURSANDO), emptyMap(), 9, hoy)
        assertEquals((1..4).map { "S$it" }.toSet(), atrasado.plan.first().toSet())
        assertEquals(5, atrasado.semestresRestantes)
    }

    @Test
    fun cursandoLoUltimoTerminasEste() {
        val p = conCadena()
        val marcas = p.todas.associate { it.codigo to Marca.APROBADA } + ("C6" to Marca.CURSANDO)
        val e = estimarGraduacion(p, marcas, emptyMap(), hoy = hoy)
        assertEquals(1, e.semestresRestantes)
        assertEquals(hoy, e.periodoFinal)
        assertTrue(e.plan.isEmpty())
    }
}
