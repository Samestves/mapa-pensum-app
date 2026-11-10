package mpensum.portada

import mpensum.setContentSincrono
import mpensum.plataforma.iniciar
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
        regla.setContentSincrono {
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
        regla.onNodeWithText("Tu carrera").performClick()
        regla.mainClock.advanceTimeBy(900)
        regla.onRoot().captureRoboImage("build/capturas/ajustes-3-elegir.png")
    }

    /** Automático se enciende en Personalizar y guarda Sistema */
    @Test
    fun elegirApariencia() {
        montar(carreras.first { it.slug == "ingenieria-de-sistemas" })
        regla.onNodeWithText("Personalizar").performClick()
        regla.mainClock.advanceTimeBy(900)
        regla.onNodeWithText("Automático").performClick()
        regla.mainClock.advanceTimeBy(500)
        assertEquals(Apariencia.SISTEMA, Preferencias.tema.value)
    }

    /** Personalizar de pie, con y sin Automático */
    @Test
    fun personalizar() {
        montar(carreras.first { it.slug == "ingenieria-de-sistemas" })
        regla.onNodeWithText("Personalizar").performClick()
        regla.mainClock.advanceTimeBy(900)
        regla.onRoot().captureRoboImage("build/capturas/personalizar-1-de-pie.png")
        regla.onNodeWithText("Automático").performClick()
        regla.mainClock.advanceTimeBy(500)
        regla.onRoot().captureRoboImage("build/capturas/personalizar-2-automatico.png")
    }

    /** Cuadro a cuadro y con un respiro real: el reloj de pruebas solo no deja asentar la hoja */
    private fun esperar(ms: Long) {
        val inicio = regla.mainClock.currentTime
        while (regla.mainClock.currentTime - inicio < ms) {
            regla.mainClock.advanceTimeByFrame()
        }
    }

    /** Accesibilidad abierta directo: su fila queda bajo el pliegue de Personalizar y no se alcanza con un toque */
    @Test
    fun accesibilidad() {
        regla.mainClock.autoAdvance = false
        regla.setContentSincrono {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.ACCESIBILIDAD, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        esperar(1200)
        regla.onRoot().captureRoboImage("build/capturas/personalizar-3-accesibilidad.png")
    }

    /** Las subpáginas Mapa e Inicio y horario, entrando por su fila */
    @Test
    fun subpaginas() {
        montar(carreras.first { it.slug == "ingenieria-de-sistemas" })
        regla.onNodeWithText("Personalizar").performClick()
        regla.mainClock.advanceTimeBy(900)
        for ((fila, archivo) in listOf("cables" to "mapa", "12 h" to "inicio")) {
            // Se pulsa por el resumen de la fila, que es único en la página
            regla.onNodeWithText(fila, substring = true).performClick()
            esperar(1200)
            regla.onRoot().captureRoboImage("build/capturas/personalizar-3-$archivo.png")
            regla.onNodeWithContentDescription("Volver").performClick()
            // Sin esto la vuelta no se recompone: el reloj manual no vacía los cambios de estado
            regla.waitForIdle()
            esperar(1200)
            regla.onRoot().captureRoboImage("build/capturas/personalizar-4-vuelta-$archivo.png")
        }
    }

    @Test
    fun sinCarrera() {
        montar(null)
        regla.onRoot().captureRoboImage("build/capturas/ajustes-2-sin-carrera.png")
    }
}
