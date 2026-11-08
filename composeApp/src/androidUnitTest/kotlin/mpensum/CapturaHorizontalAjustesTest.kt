package mpensum

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.plataforma.Preferencias
import mpensum.portada.HojaAjustes
import mpensum.portada.PaginaAjustes
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

/**
 * Las hojas de Ajustes y Personalizar con el teléfono acostado (915×412 dp).
 * Las imágenes van a build/capturas/horizontal-ajustes-*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalAjustesTest {

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

    private fun capturar(pagina: PaginaAjustes, archivo: String) {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(pagina, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        regla.mainClock.advanceTimeBy(900)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-ajustes-$archivo.png")
    }

    @Test
    fun menu() = capturar(PaginaAjustes.MENU, "1-menu")

    @Test
    fun personalizar() = capturar(PaginaAjustes.PERSONALIZAR, "2-personalizar")

    @Test
    fun subpaginaMapa() = capturar(PaginaAjustes.MAPA, "3-mapa")

    @Test
    fun subpaginaAccesibilidad() = capturar(PaginaAjustes.ACCESIBILIDAD, "4-accesibilidad")

    /**
     * Menú → Personalizar → Volver, cuadro a cuadro. Mientras Personalizar sale, su columna
     * desplazable sigue montada: si la hoja volviera a desplazar en ese momento, la app se cerraba
     * («Vertically scrollable component was measured with an infinity maximum height»).
     */
    @Test
    fun volverDePersonalizarNoSeCierra() {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        regla.mainClock.advanceTimeBy(900)
        regla.onNodeWithText("Personalizar").performClick()
        repeat(60) { regla.mainClock.advanceTimeByFrame() }
        regla.onNodeWithContentDescription("Volver").performClick()
        // Con el reloj manual, la vuelta necesita un respiro para recomponerse (artefacto del test)
        regla.waitForIdle()
        repeat(60) { regla.mainClock.advanceTimeByFrame() }
        regla.onNodeWithText("Tu carrera").assertExists()
        regla.onRoot().captureRoboImage("build/capturas/horizontal-ajustes-5-volver.png")
    }
}
