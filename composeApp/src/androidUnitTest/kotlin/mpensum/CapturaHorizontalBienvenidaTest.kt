package mpensum

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.bienvenida.Bienvenida
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

/**
 * Los cuatro pasos de la bienvenida con el teléfono acostado (915 × 412 dp),
 * fotografiados en un momento de cada escena y guardados en build/capturas/.
 * Como CapturaBienvenidaTest: no compara nada, solo falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    // Las carreras de verdad: el último paso es una lista
    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun paso(numero: Int, nombre: String, instantes: List<Long>) {
        regla.mainClock.autoAdvance = false
        regla.setContent { Bienvenida(carreras, pasoInicial = numero) {} }
        // El reloj de prueba avanza por cuadros de 16 ms y redondea hacia arriba: se mide el tiempo real, no lo pedido
        val inicio = regla.mainClock.currentTime
        instantes.forEachIndexed { i, ms ->
            while (regla.mainClock.currentTime - inicio < ms) regla.mainClock.advanceTimeByFrame()
            regla.onRoot().captureRoboImage("build/capturas/horizontal-bienvenida-$nombre-${i + 1}.png")
        }
    }

    @Test
    fun mapa() = paso(0, "1-mapa", listOf(1_500L, 4_900L, 8_600L))

    @Test
    fun horario() = paso(1, "2-horario", listOf(1_000L, 3_800L, 7_600L))

    @Test
    fun avance() = paso(2, "3-avance", listOf(1_500L, 5_000L, 8_000L))

    @Test
    fun carrera() = paso(3, "4-carrera", listOf(1_500L))
}
