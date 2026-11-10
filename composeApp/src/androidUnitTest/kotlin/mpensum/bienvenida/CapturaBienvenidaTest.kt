package mpensum.bienvenida

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Las pantallas 1 y 2 de la bienvenida, enteras (fondo, barra de pasos,
 * escena y título), fotografiadas en los momentos de las tiras aprobadas y
 * guardadas en build/capturas/. Como CapturaMapaTest: no compara nada, solo
 * falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    private fun paso(numero: Int, instantes: List<Pair<String, Long>>) {
        regla.mainClock.autoAdvance = false
        regla.setContent { Bienvenida(emptyList(), pasoInicial = numero) {} }
        // El reloj de prueba avanza por cuadros de 16 ms y redondea hacia arriba: se mide el tiempo real, no lo pedido
        val inicio = regla.mainClock.currentTime
        instantes.forEach { (nombre, ms) ->
            while (regla.mainClock.currentTime - inicio < ms) regla.mainClock.advanceTimeByFrame()
            regla.onRoot().captureRoboImage("build/capturas/$nombre.png")
        }
    }

    @Test
    fun mapa() = paso(
        0,
        listOf(
            "onb1-1-se-arma" to 1_500L, "onb1-2-toque" to 3_500L, "onb1-3-descarga" to 4_900L,
            "onb1-4-se-abren" to 6_600L, "onb1-5-cambiado" to 8_600L, "onb1-6-vuelta" to 10_300L,
        ),
    )

    @Test
    fun horario() = paso(
        1,
        listOf(
            "onb2-1-foto" to 1_000L, "onb2-2-lectura" to 2_200L, "onb2-3-al-dia" to 3_800L,
            "onb2-4-semana" to 5_300L, "onb2-5-en-curso" to 7_600L,
        ),
    )
}
