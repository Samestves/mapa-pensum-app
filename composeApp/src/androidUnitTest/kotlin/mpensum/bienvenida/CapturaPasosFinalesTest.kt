package mpensum.bienvenida

import mpensum.setContentSincrono
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Los pasos 3 y 4 de la bienvenida, fotografiados en build/capturas/: el
 * avance en cuatro momentos de su ciclo y la elección de carrera antes y
 * después de tocar Sistemas. No compara nada, solo falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaPasosFinalesTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    /** El paso dentro del mismo marco que la bienvenida: fondo y el hueco de la barra de arriba */
    private fun montar(contenido: @Composable ColumnScope.() -> Unit) {
        regla.mainClock.autoAdvance = false
        regla.setContentSincrono {
            Column(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Spacer(Modifier.height(96.dp))
                contenido()
            }
        }
    }

    /**
     * Avanza el reloj a pasos cortos hasta `ms`: lo que se carga aparte (el
     * pensum) tiene ocasión de llegar. Se mide con el reloj de la regla, que
     * también avanza un poco con cada foto.
     */
    private fun hasta(ms: Long) {
        while (regla.mainClock.currentTime < ms) {
            regla.mainClock.advanceTimeBy(100)
        }
    }

    private fun foto(nombre: String) = regla.onRoot().captureRoboImage("build/capturas/$nombre.png")

    @Test
    fun avance() {
        montar { PasoAvance {} }
        listOf("avance-1-forma" to 1_100L, "avance-2-suma" to 2_700L, "avance-3-cursas" to 4_400L, "avance-4-falta" to 6_100L, "avance-5-meta" to 8_400L)
            .forEach { (nombre, ms) -> hasta(ms); foto(nombre) }
    }

    @Test
    fun carrera() {
        montar { PasoCarrera(carreras, repite = false) {} }
        hasta(1_600L)
        foto("carrera-1-inicial")
        regla.onNodeWithText("Sistemas").performClick()
        hasta(2_400L)
        foto("carrera-2-sistemas")
    }

    /** Quien vuelve a ver la guía: el botón dice «Listo» en vez de «Empezar» */
    @Test
    fun carreraAlRepetirLaGuia() {
        montar { PasoCarrera(carreras, repite = true) {} }
        hasta(1_600L)
        regla.onNodeWithText("Sistemas").performClick()
        hasta(2_400L)
        foto("carrera-3-repite")
    }
}
