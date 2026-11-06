package mpensum

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.carrera.Interior
import mpensum.datos.Carrera
import mpensum.datos.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
import mpensum.portada.Portada
import mpensum.tema.Juego
import mpensum.tema.Tema
import org.junit.After
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
 * Los extras de la lista, la ficha, el mapa por temas y el modo horizontal:
 * Sistemas con los cuatro primeros semestres aprobados y dos materias del
 * quinto en curso. Las imágenes van a build/capturas/extras-*.png.
 */
abstract class BaseExtras {
    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    protected val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())
    protected val sistemas get() = carreras.first { it.slug == "ingenieria-de-sistemas" }

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Preferencias.marcarBienvenidaVista()
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        Guardado.guardarMarcas(
            sistemas.slug,
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
                mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO),
        )
        regla.mainClock.autoAdvance = false
    }

    @After
    fun volver() {
        Tema.juego = Juego.OSCURO
    }

    protected fun juego(j: Juego) = regla.runOnUiThread { Tema.juego = j }

    protected fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
            regla.waitForIdle()
        }
    }

    protected fun foto(nombre: String) {
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/extras-$nombre.png")
    }

    protected fun interior() {
        regla.setContent { Interior(sistemas, alVolver = {}) }
        correr(3000)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaExtrasVerticalTest : BaseExtras() {

    @Test
    fun listaYFicha() {
        interior()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(1500)
        foto("lista-oscuro")
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(1200)
        foto("ficha-oscuro")
        juego(Juego.PAPEL)
        foto("ficha-claro")
    }

    @Test
    fun mapaGrafito() {
        juego(Juego.GRAFITO)
        interior()
        foto("mapa-grafito")
    }

    @Test
    fun mapaPapel() {
        juego(Juego.PAPEL)
        interior()
        foto("mapa-papel")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w812dp-h375dp-land-xxhdpi")
class CapturaExtrasHorizontalTest : BaseExtras() {

    @Test
    fun portada() {
        regla.setContent { Portada(carreras, sistemas, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        correr(3000)
        foto("h-portada")
    }

    @Test
    fun mapa() {
        interior()
        foto("h-mapa")
    }

    @Test
    fun listaYFicha() {
        interior()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(1500)
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(1200)
        foto("h-lista-ficha")
    }
}

/** El mapa en horizontal a 915x412 dp, la medida de las maquetas: sin franja vacía abajo, sin panel y sin la primera columna bajo el riel */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaExtrasHorizontalAnchoTest : BaseExtras() {

    @Test
    fun mapa() {
        interior()
        foto("h915-mapa")
    }

    @Test
    fun portada() {
        regla.setContent { Portada(carreras, sistemas, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        correr(3000)
        foto("h915-portada")
    }

    @Test
    fun portadaSinMarcas() {
        Guardado.guardarMarcas(sistemas.slug, emptyMap())
        regla.setContent { Portada(carreras, sistemas, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        correr(3000)
        foto("h915-portada-sin-marcas")
    }
}
