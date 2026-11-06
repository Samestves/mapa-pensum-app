package mpensum.portada

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La hoja del engranaje sola, sin la portada debajo: con tu carrera, sin
 * carrera y en la lista para elegirla. Las imágenes van a build/capturas/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaAjustesTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    @AfterTest
    fun limpiar() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun montar(propia: Carrera?) {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, propia, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        regla.mainClock.advanceTimeBy(900)
    }

    @Test
    fun conCarrera() {
        montar(carreras.first { it.slug == "ingenieria-de-sistemas" })
        regla.onRoot().captureRoboImage("build/capturas/ajustes-1-menu.png")
        // La apariencia de aquí es la misma de la hoja de avance
        regla.onNodeWithText("Sistema").performClick()
        regla.mainClock.advanceTimeBy(500)
        assertEquals(Apariencia.SISTEMA, Preferencias.tema.value)
        regla.onNodeWithText("Tu carrera").performClick()
        regla.mainClock.advanceTimeBy(900)
        regla.onRoot().captureRoboImage("build/capturas/ajustes-3-elegir.png")
    }

    @Test
    fun sinCarrera() {
        montar(null)
        regla.onRoot().captureRoboImage("build/capturas/ajustes-2-sin-carrera.png")
    }
}
