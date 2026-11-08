package mpensum

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.serialization.json.Json
import mpensum.carrera.Interior
import mpensum.componentes.LocalVentana
import mpensum.componentes.Ventana
import mpensum.datos.Carrera
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
 * Girar el teléfono con la carrera abierta no debe rehacer su estado: el
 * página de la lista y la hoja del horario a medio llenar siguen donde
 * estaban (antes nacía un EstadoCarrera, un EstadoHorario y una lista nuevos).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class QaGirarTest {

    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        regla.mainClock.autoAdvance = false
    }

    private fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
        }
    }

    private fun textos(t: String) = regla.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().size

    @Test
    fun girarNoPierdeLaMateriaDeLaListaNiLaHojaDelHorario() {
        val slug = "ingenieria-de-sistemas"
        for (k in listOf("marcas", "casillas", "carga", "horario")) Preferencias.guardarTexto("mapa-pensum:$k:$slug", "null")
        var ventana by mutableStateOf(Ventana(390.dp, 844.dp))
        regla.setContent { CompositionLocalProvider(LocalVentana provides ventana) { Interior(carreras.first { it.slug == slug }, alVolver = {}) } }
        correr(1500)

        regla.onNodeWithContentDescription("Lista").performClick()
        regla.waitForIdle()
        correr(900)
        // Se abre la página de una materia (de pie) y se gira: acostada es el panel de la derecha, con la misma materia
        regla.onNodeWithContentDescription("Matemáticas I, Puedes inscribirla").performClick()
        regla.waitForIdle()
        correr(900)
        assertTrue(textos("Por hacer") > 0, "no se empujó la página")
        ventana = Ventana(844.dp, 390.dp)
        regla.waitForIdle()
        correr(1500)
        assertTrue(textos("SEMESTRE 1 · 0081814") > 0, "el panel perdió la materia al girar")
        assertEquals(0, textos("Por hacer"), "acostada no hay página empujada")

        // De vuelta de pie la página sigue abierta: el estado vive en el interior, no en la vista
        ventana = Ventana(390.dp, 844.dp)
        regla.waitForIdle()
        correr(1200)
        assertTrue(textos("Por hacer") > 0, "la página no sobrevivió al giro")
        regla.onNodeWithText("Por hacer").performClick()
        correr(900)
        // Y con la hoja de una clase a medio llenar: se abre de pie y se gira
        regla.onNodeWithContentDescription("Horario").performClick()
        regla.waitForIdle()
        correr(900)
        regla.onNodeWithText("Crear a mano").performClick()
        regla.waitForIdle()
        correr(1500)
        assertTrue(textos("NUEVA CLASE") > 0, "no se abrió la hoja")
        ventana = Ventana(844.dp, 390.dp)
        regla.waitForIdle()
        correr(1500)
        assertTrue(textos("NUEVA CLASE") > 0, "la hoja del horario se perdió al girar")
    }
}
