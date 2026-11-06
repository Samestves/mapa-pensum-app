package mpensum

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

/** El mapa en horizontal a 915x412 dp: sin ficha y con una materia tocada (la ficha va de panel a la derecha) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalFichaMapaTest : BaseExtras() {

    private fun captura(nombre: String) {
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-mapa-$nombre.png")
    }

    @Test
    fun mapa() {
        interior()
        correr(1500)
        captura("mapa")
    }

    @Test
    fun mapaConFicha() {
        interior()
        correr(1500)
        // El mapa es un lienzo: se toca por posición, sobre una tarjeta del encuadre inicial
        regla.onRoot().performTouchInput { click(Offset(width * 0.30f, height * 0.40f)) }
        correr(1500)
        captura("ficha")
    }
}
