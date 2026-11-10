package mpensum

import mpensum.plataforma.iniciar
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.robolectric.RuntimeEnvironment
import mpensum.guardado.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Juego
import mpensum.tema.Tema
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * La actividad de verdad (MainActivity) recreada, como cuando el teléfono
 * cambia de tema, de idioma o de tamaño de letra con la app abierta: el
 * usuario debe seguir donde estaba y nada debe reventar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class QaRecrearTest {

    // Sin lanzar la actividad sola: antes hay que dejar guardado lo que la app lee al arrancar
    @get:Rule
    val regla = createEmptyComposeRule()

    private fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
        }
    }

    private fun hasta(que: String, tope: Long = 8000, cumple: () -> Boolean) {
        var t = 0L
        while (!cumple() && t < tope) {
            correr(100)
            t += 100
        }
        assertTrue(cumple(), "no llegó: $que")
    }

    @Test
    fun recrearDentroDeUnaCarreraTeDejaEnLaCarrera() {
        val slug = "ingenieria-de-sistemas"
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Preferencias.marcarBienvenidaVista()
        Preferencias.guardarCarrera(slug)
        val p = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
        Guardado.guardarMarcas(slug, p.obligatorias.filter { it.semestre == 1 }.associate { it.codigo to Marca.APROBADA })
        val escenario = ActivityScenario.launch(MainActivity::class.java)
        regla.mainClock.autoAdvance = false
        correr(3000)
        regla.onNodeWithText("Seguir donde ibas").performClick()
        hasta("el interior de la carrera") { regla.onAllNodesWithContentDescription("Tu avance", substring = true).fetchSemanticsNodes().isNotEmpty() }
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(900)
        escenario.recreate()
        correr(3000)
        // Tras girar o cambiar el tema el usuario debería seguir dentro de su carrera
        hasta("seguir dentro de la carrera") { regla.onAllNodesWithContentDescription("Tu avance", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Con la apariencia «Sistema», pasar de día a noche no recrea la actividad (uiMode en configChanges):
     * el tema cambia solo y la actividad sigue siendo la misma.
     */
    @Test
    fun elCambioDeDiaANocheNoRecreaLaActividadYCambiaElTema() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Preferencias.marcarBienvenidaVista()
        Preferencias.guardarTema(Apariencia.SISTEMA)
        Preferencias.guardarTemaDia(Juego.CLARO)
        Preferencias.guardarTemaNoche(Juego.OSCURO)
        val escenario = ActivityScenario.launch(MainActivity::class.java)
        regla.mainClock.autoAdvance = false
        correr(3000)
        assertEquals(Juego.CLARO, Tema.juego)
        var antes: MainActivity? = null
        escenario.onActivity { antes = it }
        RuntimeEnvironment.setQualifiers("+night")
        correr(1500)
        var despues: MainActivity? = null
        escenario.onActivity { despues = it }
        assertSame(antes, despues, "la actividad se recreó")
        hasta("el tema de noche") { Tema.juego == Juego.OSCURO }
    }
}
