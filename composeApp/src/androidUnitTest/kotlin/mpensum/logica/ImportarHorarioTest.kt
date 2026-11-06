package mpensum.logica

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mpensum.datos.Asignatura

/** Las mismas pruebas que layout/importarHorario.test.js de la web: las mismas reglas tienen que dar lo mismo */
class ImportarHorarioTest {
    private val materias = listOf(
        Asignatura("0071814", "Matemática I"),
        Asignatura("0071824", "Matemática II"),
        Asignatura("0072914", "Física I"),
        Asignatura("0052131", "Laboratorio de Física I"),
        Asignatura("0075812", "Inglés Técnico I"),
        Asignatura("0081733", "Programación I"),
        Asignatura("0081743", "Base de Datos"),
    )

    private fun fila(
        codigo: String = "0071814", nombre: String = "Matemática I", dia: String = "Lunes",
        inicio: String = "07:00", fin: String = "08:40",
    ) = FilaCruda(codigo = codigo, nombre = nombre, dia = dia, inicio = inicio, fin = fin)

    /* ---- Los días y las horas ---- */

    @Test fun losDias() {
        assertEquals(0, aDia("Lunes"))
        assertEquals(2, aDia("MIÉRCOLES"))
        assertEquals(2, aDia("mie"))
        assertEquals(4, aDia("Vie."))
        assertEquals(3, aDia("J"))
        // Sábado y domingo no existen en esta rejilla; lo que no se entiende no se inventa
        assertNull(aDia("sábado"))
        assertNull(aDia("domingo"))
        assertNull(aDia(""))
        assertNull(aDia("???"))
        assertNull(aDia(null))
    }

    @Test fun lasHoras() {
        assertEquals(7 * 60, aHora("07:00"))
        assertEquals(7 * 60, aHora("7:00"))
        assertEquals(8 * 60 + 40, aHora("8:40"))
        assertEquals(13 * 60 + 40, aHora("1:40 PM"))
        assertEquals(11 * 60, aHora("11:00 AM"))
        assertEquals(12 * 60, aHora("12:00 PM"))
        assertEquals(7 * 60, aHora("7.00"))
    }

    @Test fun sinMeridianoLoQueCaeriaAntesDeAbrirEsDeLaTarde() {
        assertEquals(13 * 60 + 40, aHora("1:40"))
        assertEquals(15 * 60 + 20, aHora("3:20"))
        assertEquals(ABRE, aHora("06:00"))
        assertEquals(ABRE, aHora("6:00 AM"))
        // "5:30" a secas es la media de la tarde; "5:30 AM" es la de la madrugada
        assertEquals(17 * 60 + 30, aHora("5:30"))
        assertEquals(5 * 60 + 30, aHora("5:30 AM"))
    }

    @Test fun basuraFuera() {
        assertNull(aHora("mediodía"))
        assertNull(aHora("25:00"))
        assertNull(aHora("7:99"))
        assertNull(aHora(""))
    }

    /* ---- El parecido y el emparejamiento ---- */

    @Test fun elNivelManda() {
        assertEquals(0.0, parecido("Matemática I", "Matemática II"))
        assertEquals(0.0, parecido("MAT II", "Matemática I"))
        assertTrue(parecido("MATEMATICA I", "Matemática I") > 0.9)
    }

    @Test fun lasAbreviaturasCuentan() {
        assertTrue(parecido("PROG I", "Programación I") > 0.9)
        assertTrue(parecido("BASE DATOS", "Base de Datos") > 0.9)
        assertTrue(parecido("Física I", "Base de Datos") < 0.3)
    }

    @Test fun sobrarPalabrasCuesta() {
        val exacta = parecido("FISICA I", "Física I")
        val conSobras = parecido("FISICA I", "Laboratorio de Física I")
        assertEquals(1.0, exacta)
        assertTrue(conSobras < exacta, "$conSobras tendría que ser menor que $exacta")
        assertTrue(exacta - conSobras >= 0.15, "y por margen suficiente para poder elegir")
    }

    @Test fun elCodigoGanaAunqueElNombreVengaMalLeido() {
        val (materia, via) = emparejar(FilaCruda(codigo = "0072914", nombre = "FSICA"), materias)
        assertEquals("0072914", materia?.codigo)
        assertEquals("codigo", via)
    }

    @Test fun unCodigoQueNoEsDeEstaCarreraNoVale() {
        val (materia, _) = emparejar(FilaCruda(codigo = "9999999", nombre = "Base de Datos"), materias)
        assertEquals("0081743", materia?.codigo, "cae al nombre, no se queda sin materia")
    }

    @Test fun unaMateriaQueExisteSeEncuentraAunqueHayaUnLaboratorioSuyo() {
        assertEquals("Física I", emparejar(FilaCruda(nombre = "FISICA I"), materias).first?.nombre)
    }

    @Test fun sinNadaQueSeParezcaNinguna() {
        assertNull(emparejar(FilaCruda(nombre = "Yoga Aplicada"), materias).first)
    }

    @Test fun dosCandidatasIgualDeBuenasNoEligen() {
        // "Matemática" sin nivel está a la misma distancia de la I y de la II
        assertNull(emparejar(FilaCruda(nombre = "Matemática"), materias).first)
    }

    @Test fun unNumeralRomanoNoAbrePalabras() {
        val alimentos = listOf(
            Asignatura("MAT1", "Matemáticas I"), Asignatura("MAT2", "Matemáticas II"),
            Asignatura("FIS", "Física"), Asignatura("ING", "Inglés Técnico I"),
        )
        assertEquals(0.0, parecido("Informática", "Matemáticas I"))
        assertNull(emparejar(FilaCruda(nombre = "Informática"), alimentos).first)
        assertNull(emparejar(FilaCruda(nombre = "Inglés"), alimentos).first, "no es Matemáticas I")
        assertTrue(parecido("ING TEC I", "Inglés Técnico I") > 0.9)
    }

    /* ---- La revisión ---- */

    @Test fun unaFilaLimpiaEntraMarcadaYSinAvisos() {
        val c = revisar(listOf(fila()), materias).single()
        assertEquals("0071814", c.codigo)
        assertEquals(0, c.dia)
        assertEquals(420, c.inicio)
        assertEquals(emptyList(), c.avisos)
        assertTrue(c.incluir)
    }

    @Test fun loQueNoSeEntiendeSaleMarcadoYNoSeIncluye() {
        val c = revisar(listOf(fila(dia = "sábado", nombre = "Yoga", codigo = "")), materias).single()
        assertEquals(listOf("sin-dia", "sin-materia"), c.avisos.sorted())
        assertEquals(false, c.incluir)
    }

    @Test fun fueraDeLaJornada() {
        val c = revisar(listOf(fila(inicio = "5:00 AM", fin = "6:00 AM")), materias).single()
        assertTrue("fuera" in c.avisos)
    }

    @Test fun unaClaseALas6EsValidaYUnaALas530QuedaFuera() {
        val alAbrir = revisar(listOf(fila(inicio = "6:00 AM", fin = "7:40 AM")), materias).single()
        assertEquals(emptyList(), alAbrir.avisos)
        assertEquals(ABRE, alAbrir.inicio)
        val antes = revisar(listOf(fila(inicio = "5:30 AM", fin = "7:00 AM")), materias).single()
        assertTrue("fuera" in antes.avisos)
    }

    @Test fun elFinAntesDelInicioNoEsUnaClase() {
        val c = revisar(listOf(fila(inicio = "10:00", fin = "8:00 AM")), materias).single()
        assertTrue("sin-hora" in c.avisos)
    }

    @Test fun dosClasesQueSePisanSeMarcanLasDos() {
        val cs = revisar(listOf(fila(), fila(codigo = "0072914", nombre = "Física I", inicio = "08:00", fin = "09:40")), materias)
        assertTrue(cs.all { "choca" in it.avisos })
    }

    @Test fun chocarConLoQueYaEstabaGuardadoTambienCuenta() {
        val guardadas = listOf(Sesion("x", "A", 0, 420, 500))
        val c = revisar(listOf(fila()), materias, guardadas).single()
        assertTrue("choca" in c.avisos)
    }

    @Test fun loQueNoVieneNoRompeNada() {
        assertEquals(emptyList(), revisar(emptyList(), materias))
        assertEquals(false, revisar(listOf(FilaCruda()), materias).single().incluir)
    }

    @Test fun sacarUnaDeDosQueChocanLiberaALaOtra() {
        val chocan = revisar(
            listOf(fila(), fila(codigo = "0072914", nombre = "Física I", inicio = "08:00", fin = "09:40")),
            materias,
        )
        val tras = marcarChoques(chocan.mapIndexed { i, c -> if (i == 1) c.copy(incluir = false) else c })
        assertEquals(emptyList(), tras[0].avisos)
        assertEquals(emptyList(), tras[1].avisos, "la que se sacó ya no choca con nadie")
    }

    /* ---- El paso a sesiones ---- */

    @Test fun soloPasanLasMarcadasYSanas() {
        fun c(id: String, codigo: String, incluir: Boolean, avisos: List<String>) = Candidata(
            id, Leido("", "", ""), codigo, null, null, 0, 420, 500, "01", "", "", avisos, incluir, codigo,
        )
        val candidatas = listOf(c("1", "A", true, emptyList()), c("2", "B", false, emptyList()), c("3", "C", true, listOf("choca")))
        val sesiones = aSesiones(candidatas)
        assertEquals(listOf("A"), sesiones.map { it.codigo })
        // La hoja «Antes de guardar» sí deja pasar el choque, que enseña
        assertEquals(listOf("A", "C"), aSesiones(candidatas, conChoques = true).map { it.codigo })
    }

    /* ---- De la respuesta del servidor a las materias leídas ---- */

    @Test fun unaRespuestaBuenaSeAgrupaPorMateria() {
        val json = """{"clases":[
            {"codigo":"0071814","nombre":"Matemática I","dia":"Lunes","inicio":"07:00","fin":"08:40","aula":"A-12"},
            {"codigo":"0071814","nombre":"Matemática I","dia":"Miércoles","inicio":"07:00","fin":"08:40","aula":"A-12"},
            {"codigo":"","nombre":"Yoga","dia":"Viernes","inicio":"07:00","fin":"08:40"},
            {"codigo":"0072914","nombre":"Física I","dia":"Martes","inicio":"09:00","fin":"10:40"}]}"""
        val leidas = materiasLeidas(filasDeRespuesta(200, true, json), materias)
        assertEquals(listOf("0071814", "0072914"), leidas.map { it.codigo })
        assertEquals(listOf(0, 2), leidas[0].sesiones.map { it.dia })
        assertEquals("A-12", leidas[0].sesiones[0].aula)
    }

    @Test fun sinClasesEsUnFalloDeLaFoto() {
        val e = assertFailsWith<FalloLectura> { materiasLeidas(filasDeRespuesta(200, true, """{"clases":[]}"""), materias) }
        assertEquals("sin-clases", e.codigo)
        assertEquals(Salida.OTRA_IMAGEN, e.salida)
    }

    @Test fun losFallosDelServidorLlevanSuCodigoYSuEspera() {
        val cola = assertFailsWith<FalloLectura> { filasDeRespuesta(429, true, """{"error":"cola","espera":12}""") }
        assertEquals("cola", cola.codigo)
        assertEquals(12, cola.espera)
        assertEquals(Salida.REINTENTAR, cola.salida)
        val cuota = assertFailsWith<FalloLectura> { filasDeRespuesta(429, true, """{"error":"cuota"}""") }
        assertTrue(cuota.aplazado)
        assertEquals(Salida.A_MANO, cuota.salida)
        // Un código que no conocemos se dice como avería, no con su nombre
        assertEquals(Salida.A_MANO, assertFailsWith<FalloLectura> { filasDeRespuesta(500, true, """{"error":"sin-clave"}""") }.salida)
    }

    @Test fun unaRespuestaQueNoEsJsonEsSinServidor() {
        assertEquals("sin-servidor", assertFailsWith<FalloLectura> { filasDeRespuesta(200, false, "<html>") }.codigo)
    }

    @Test fun laPeticionLlevaImagenTipoYMaterias() {
        val cuerpo = cuerpoDePeticion("AAAA", listOf(Asignatura("0071814", "Matemática I")))
        assertEquals("""{"imagen":"AAAA","tipo":"image/jpeg","materias":[{"codigo":"0071814","nombre":"Matemática I"}]}""", cuerpo)
    }
}
