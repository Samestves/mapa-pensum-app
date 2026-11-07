package mpensum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.serialization.json.Json
import mpensum.carrera.Interior
import mpensum.datos.Carrera
import mpensum.datos.Guardado
import mpensum.datos.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de humo de la app entera en un teléfono simulado: la guía, la
 * portada y el interior de cada una de las nueve carreras en sus tres vistas,
 * con datos guardados normales, rotos y de otras versiones. No miran píxeles:
 * fallan si algo revienta o si un recorrido no llega a donde debe.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class QaAppTest {

    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        regla.mainClock.autoAdvance = false
    }

    /** Deja correr el reloj de prueba y, entre cuadros, el hilo donde se leen los pensums del paquete */
    private fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
        }
    }

    /** Corre el reloj hasta que se cumpla algo, con tope: en la JVM una hoja tarda en montarse más que en el teléfono */
    private fun hasta(que: String, tope: Long = 8000, cumple: () -> Boolean) {
        var t = 0L
        while (!cumple() && t < tope) {
            correr(100)
            t += 100
        }
        assertTrue(cumple(), "no llegó: $que")
    }

    private fun textos(t: String) = regla.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().size

    private fun yaVista(slug: String? = null) {
        Preferencias.marcarBienvenidaVista()
        if (slug != null) Preferencias.guardarCarrera(slug)
    }

    private fun atras() {
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
        correr(700)
    }

    @Test
    fun primeraVezSaltaYEligeCarrera() {
        regla.setContent { App() }
        correr(1500)
        regla.onNodeWithText("Saltar").performClick()
        correr(1500)
        // En el último paso Saltar ya no está, y el botón espera apagado
        assertEquals(0, regla.onAllNodesWithText("Saltar").fetchSemanticsNodes().size)
        regla.onNodeWithText("Elige tu carrera", substring = true).assertExists()
        regla.onNodeWithText("Petróleo").performClick()
        correr(800)
        regla.onNodeWithText("Empezar").performClick()
        correr(1500)
        assertEquals("ingenieria-de-petroleo", Preferencias.carrera())
        assertTrue(Preferencias.bienvenidaVista())
        // La guía acaba en la portada, no dentro del mapa: el interior ("Tu avance") aún no existe
        regla.onNodeWithContentDescription("Ajustes y ayuda").assertExists()
        assertEquals(0, regla.onAllNodesWithContentDescription("Tu avance", substring = true).fetchSemanticsNodes().size)
        assertTrue(textos("Petróleo") > 0, "la portada no muestra la carrera elegida")
    }

    @Test
    fun atrasEnLaGuiaVuelvePasoAPaso() {
        regla.setContent { App() }
        correr(1500)
        regla.onNodeWithText("Saltar").performClick()
        correr(1200)
        regla.onNodeWithText("Elige tu carrera", substring = true).assertExists()
        // Atrás del sistema en el paso 4 vuelve al 3, no cierra la actividad
        atras()
        assertTrue(!regla.activity.isFinishing)
    }

    @Test
    fun verLaGuiaOtraVezDesdeAjustesYVolver() {
        yaVista("ingenieria-de-sistemas")
        regla.setContent { App() }
        correr(3000)
        regla.onNodeWithContentDescription("Ajustes y ayuda").performClick()
        correr(900)
        regla.onNodeWithText("Ver la guía").performClick()
        correr(1500)
        // La guía llega con tu carrera ya elegida en su último paso; Atrás desde el primer paso vuelve a la portada
        regla.onNodeWithText("Saltar").assertExists()
        atras()
        correr(1500)
        regla.onNodeWithContentDescription("Ajustes y ayuda").assertExists()
        assertTrue(!regla.activity.isFinishing)
    }

    @Test
    fun verLaGuiaOtraVezTerminaEnLaPortadaConTuCarreraYaElegida() {
        yaVista("ingenieria-de-sistemas")
        regla.setContent { App() }
        correr(3000)
        regla.onNodeWithContentDescription("Ajustes y ayuda").performClick()
        correr(900)
        regla.onNodeWithText("Ver la guía").performClick()
        correr(1500)
        regla.onNodeWithText("Saltar").performClick()
        correr(1500)
        // Tu carrera ya viene elegida y el botón solo cierra la guía
        regla.onNodeWithText("Listo").performClick()
        correr(1500)
        assertEquals("ingenieria-de-sistemas", Preferencias.carrera())
        regla.onNodeWithContentDescription("Ajustes y ayuda").assertExists()
        assertEquals(0, regla.onAllNodesWithContentDescription("Tu avance", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun portadaAbreCarreraYAtrasVuelve() {
        yaVista("ingenieria-de-sistemas")
        Guardado.guardarMarcas("ingenieria-de-sistemas", pensumDe("ingenieria-de-sistemas").obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA })
        regla.setContent { App() }
        correr(3000)
        regla.onNodeWithText("Continuar").performClick()
        correr(2500)
        regla.onNodeWithContentDescription("Tu avance", substring = true).assertExists()
        atras()
        correr(1500)
        regla.onNodeWithContentDescription("Ajustes y ayuda").assertExists()
    }

    /** Cada carrera, cada vista y la hoja de avance. Se monta una vez y se cambia de carrera, como al salir y entrar */
    private fun recorrerTodas(ponerMarcas: (slug: String) -> Unit) {
        var actual by mutableStateOf(carreras.first())
        regla.setContent { key(actual.slug) { Interior(actual, alVolver = {}) } }
        for (c in carreras) {
            // Cada carrera parte de un teléfono limpio: se borran sus claves
            for (k in listOf("marcas", "casillas", "carga", "horario")) Preferencias.guardarTexto("mapa-pensum:$k:${c.slug}", "null")
            ponerMarcas(c.slug)
            actual = c
            correr(1500)
            regla.onNodeWithContentDescription("Tu avance", substring = true).assertExists()
            regla.onNodeWithContentDescription("Lista").performClick()
            correr(900)
            regla.onNodeWithContentDescription("Horario").performClick()
            correr(900)
            regla.onNodeWithContentDescription("Mapa").performClick()
            correr(900)
            regla.onNodeWithContentDescription("Tu avance", substring = true).performClick()
            correr(1200)
            regla.onNodeWithText("TU AVANCE").assertExists()
            atras()
            correr(600)
        }
    }

    private fun pensumDe(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    @Test
    fun lasNueveCarrerasSinMarcas() = recorrerTodas { }

    @Test
    fun lasNueveCarrerasConMarcasAMedias() = recorrerTodas { slug ->
        val p = pensumDe(slug)
        val m = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            p.obligatorias.filter { it.semestre == 5 }.take(3).associate { it.codigo to Marca.CURSANDO }
        Guardado.guardarMarcas(slug, m)
    }

    @Test
    fun lasNueveCarrerasTodoAprobado() = recorrerTodas { slug ->
        Guardado.guardarMarcas(slug, pensumDe(slug).todas.associate { it.codigo to Marca.APROBADA })
    }

    @Test
    fun lasNueveCarrerasConDatosRotos() = recorrerTodas { slug ->
        Preferencias.guardarTexto("mapa-pensum:marcas:$slug", "{esto no es json")
        Preferencias.guardarTexto("mapa-pensum:casillas:$slug", "[1,2,3]")
        Preferencias.guardarTexto("mapa-pensum:carga:$slug", "{\"unidad\":\"materias\",\"valor\":\"mucho\"}")
        Preferencias.guardarTexto("mapa-pensum:horario:$slug", "{\"no\":\"una lista\"}")
    }

    /** Monta el interior de Sistemas con unas marcas, para los recorridos con el dedo */
    private fun interiorDeSistemas(marcas: Map<String, Marca> = emptyMap()) {
        val slug = "ingenieria-de-sistemas"
        for (k in listOf("marcas", "casillas", "carga", "horario")) Preferencias.guardarTexto("mapa-pensum:$k:$slug", "null")
        if (marcas.isNotEmpty()) Guardado.guardarMarcas(slug, marcas)
        regla.setContent { Interior(carreras.first { it.slug == slug }, alVolver = {}) }
        correr(1500)
    }

    private fun sistemas() = pensumDe("ingenieria-de-sistemas")

    @Test
    fun atrasConUnaHojaAbiertaSobreLaListaConFiltroCierraLaHoja() {
        interiorDeSistemas(sistemas().obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA })
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(900)
        regla.onNodeWithContentDescription("Aprobadas", substring = true).performClick()
        correr(600)
        regla.onNodeWithContentDescription("Tu avance", substring = true).performClick()
        correr(1200)
        regla.onNodeWithText("TU AVANCE").assertExists()
        atras()
        correr(900)
        // Atrás debe cerrar la hoja de arriba, no quitar el filtro que hay debajo
        hasta("que la hoja de avance se cierre") { textos("TU AVANCE") == 0 }
        assertEquals(1, regla.onAllNodesWithContentDescription("Quitar el filtro", substring = true).fetchSemanticsNodes().size, "Atrás se llevó el filtro en vez de la hoja")
    }

    @Test
    fun marcarDesdeLaFichaDeLaListaSeGuarda() {
        interiorDeSistemas()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(900)
        // Tocar una fila abre su ficha; los botones de estado de la ficha son los que marcan
        regla.onNodeWithContentDescription("Matemáticas I, Puedes inscribirla").performClick()
        regla.waitForIdle()
        correr(900)
        regla.onNodeWithText("La estoy cursando").performClick()
        correr(500)
        assertEquals(Marca.CURSANDO, Guardado.marcas(sistemas())["0081814"])
        regla.onNodeWithText("Ya la aprobé").performClick()
        correr(500)
        assertEquals(Marca.APROBADA, Guardado.marcas(sistemas())["0081814"])
    }

    @Test
    fun aprobarUnSemestreEnteroConSuAro() {
        interiorDeSistemas()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(900)
        regla.onNodeWithContentDescription("Aprobar el semestre 1").performClick()
        correr(600)
        val s1 = sistemas().obligatorias.filter { it.semestre == 1 }
        val guardadas = Guardado.marcas(sistemas())
        assertTrue(s1.all { guardadas[it.codigo] == Marca.APROBADA }, "el aro no aprobó todo el semestre 1")
        regla.onNodeWithText("Deshacer").performClick()
        correr(900)
        assertTrue(Guardado.marcas(sistemas()).isEmpty(), "deshacer no devolvió el semestre")
    }

    @Test
    fun elAroDelSemestreConSoloLaElectivaPendienteAbreElSelector() {
        val p = sistemas()
        interiorDeSistemas(p.obligatorias.filter { it.semestre == 1 || it.semestre == 2 }.associate { it.codigo to Marca.APROBADA })
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(900)
        // Semestre 2: todo aprobado salvo la electiva sociohumanística, así que el aro pide elegirla
        regla.onNodeWithContentDescription("Aprobar el semestre 2").performClick()
        correr(1200)
        hasta("el selector de electivas") { textos("Es lo único que le falta") == 1 }
        atras()
        hasta("que el selector se cierre") { textos("Es lo único que le falta") == 0 }
        assertTrue(Guardado.casillas(p).isEmpty())
    }

    @Test
    fun elHorarioSeCreaSeGuardaYSeQuita() {
        interiorDeSistemas()
        regla.onNodeWithContentDescription("Horario").performClick()
        correr(900)
        regla.onNodeWithText("Crear a mano").performClick()
        hasta("la hoja de nueva clase") { textos("NUEVA CLASE") == 1 }
        // Sin materia elegida el botón no hace nada
        regla.onNodeWithText("Añadir clase").performClick()
        correr(400)
        assertTrue(HorarioGuardado.leer("ingenieria-de-sistemas", sistemas().todas.map { it.codigo }.toSet()).isEmpty())
        regla.onNodeWithText("Extraacadémica").performClick()
        correr(300)
        regla.onNodeWithText("Añadir clase").performClick()
        hasta("que la hoja se cierre") { textos("NUEVA CLASE") == 0 }
        val guardadas = HorarioGuardado.leer("ingenieria-de-sistemas", sistemas().todas.map { it.codigo }.toSet())
        assertEquals(1, guardadas.size)
        assertEquals("0021111", guardadas[0].codigo)
        // La clase aparece en el día y se abre su detalle
        assertTrue(textos("Extraacadémica") > 0, "la clase no aparece en el día")
    }
}
