package mpensum

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

/** La lista acostada en el ancho más estrecho del reparto (640x360 dp): el resumen debe seguir leyéndose */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w640dp-h360dp-land-xxhdpi")
class CapturaHorizontalListaEstrechaTest : BaseExtras() {
    @Test
    fun resumen() {
        interior()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(1500)
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-lista-estrecha.png")
    }
}
