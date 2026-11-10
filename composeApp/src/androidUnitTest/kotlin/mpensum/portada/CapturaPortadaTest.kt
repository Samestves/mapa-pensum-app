package mpensum.portada

import mpensum.plataforma.iniciar
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.guardado.Guardado
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

/**
 * La portada con avance de verdad: se guardan marcas en el teléfono simulado
 * y se fotografía arriba, deslizada, con los ajustes abiertos y eligiendo
 * carrera. Las imágenes van a build/capturas/; solo falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaPortadaTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    /** Aprueba los semestres hasta `hasta` y deja `cursando` materias del siguiente en curso */
    private fun marcar(slug: String, hasta: Int, cursando: Int) {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
        val marcas = pensum.obligatorias.filter { (it.semestre ?: 99) <= hasta }.associate { it.codigo to Marca.APROBADA } +
            pensum.obligatorias.filter { it.semestre == hasta + 1 }.take(cursando).associate { it.codigo to Marca.CURSANDO }
        Guardado.guardarMarcas(slug, marcas)
    }

    private fun esperar(ms: Long) {
        var t = 0L
        // A pasos cortos: el avance se calcula aparte y tiene ocasión de llegar
        while (t < ms) {
            regla.mainClock.advanceTimeBy(100)
            t += 100
            Thread.sleep(3)
        }
    }

    @Test
    fun conAvance() {
        marcar("ingenieria-de-sistemas", 4, 2)
        marcar("licenciatura-en-contaduria-publica", 1, 0)
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(3000)
        regla.onRoot().captureRoboImage("build/capturas/portada-arriba.png")
        regla.onNodeWithContentDescription("Ajustes y ayuda").performClick()
        esperar(900)
        regla.onRoot().captureRoboImage("build/capturas/portada-ajustes.png")
        regla.onNodeWithText("Tu carrera").performClick()
        esperar(900)
        regla.onRoot().captureRoboImage("build/capturas/portada-elegir.png")
    }

    @Test
    fun conAvanceDesplazada() {
        marcar("ingenieria-de-sistemas", 4, 2)
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(2500)
        regla.onRoot().performTouchInput { swipeUp(startY = centerY + 200f, endY = centerY - 300f) }
        esperar(1200)
        regla.onRoot().captureRoboImage("build/capturas/portada-desplazada-con-avance.png")
    }

    /** Tu carrera elegida pero sin nada marcado: la silueta apagada, 0 % y «Empezar» */
    @Test
    fun sinMarcas() {
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, carreras.first { it.slug == "ingenieria-de-petroleo" }, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(2500)
        regla.onRoot().captureRoboImage("build/capturas/portada-sin-marcas.png")
    }

    @Test
    fun sinCarrera() {
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, null, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(2500)
        regla.onRoot().captureRoboImage("build/capturas/portada-sin-carrera.png")
        regla.onRoot().performTouchInput { swipeUp(startY = centerY + 200f, endY = centerY - 300f) }
        esperar(1200)
        regla.onRoot().captureRoboImage("build/capturas/portada-deslizada.png")
    }
}
