package mpensum

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
import mpensum.datos.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
import mpensum.portada.HojaAjustes
import mpensum.portada.PaginaAjustes
import mpensum.portada.Portada
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
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
 * La portada en cada juego de color y la subpantalla Personalizar. Las
 * imágenes van a build/capturas/ con el prefijo «tema-»; no compara nada.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaTemasTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        val p = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        Guardado.guardarMarcas(
            p.slug,
            p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
                p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO },
        )
    }

    @AfterTest
    fun limpiar() {
        Tema.juego = Juego.OSCURO
        Tema.luzFondo = true
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun esperar(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(100)
            t += 100
            Thread.sleep(3)
        }
    }

    private fun portada(juego: Juego) {
        Tema.juego = juego
        Tema.luzFondo = juego.aurora > 0f
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(3000)
        regla.onRoot().captureRoboImage("build/capturas/tema-portada-${juego.name.lowercase()}.png")
    }

    @Test fun grafito() = portada(Juego.GRAFITO)
    @Test fun negro() = portada(Juego.NEGRO)
    @Test fun medianoche() = portada(Juego.MEDIANOCHE)
    @Test fun papel() = portada(Juego.PAPEL)
    @Test fun exterior() = portada(Juego.EXTERIOR)

    @Test
    fun personalizar() {
        Tema.juego = Juego.OSCURO
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        esperar(900)
        regla.onRoot().captureRoboImage("build/capturas/tema-ajustes.png")
        regla.onNodeWithText("Personalizar").performClick()
        esperar(900)
        regla.onRoot().captureRoboImage("build/capturas/tema-personalizar.png")
    }
}
