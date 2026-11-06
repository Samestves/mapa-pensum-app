package mpensum

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

/** La lista en horizontal a 915x412 dp: columna de lectura, y la ficha al lado sin tapar los filtros */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalListaTest : BaseExtras() {

    private fun captura(nombre: String) {
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-lista-$nombre.png")
    }

    private fun abrirLista() {
        interior()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(1500)
    }

    @Test
    fun lista() {
        abrirLista()
        captura("lista")
    }

    @Test
    fun listaConFicha() {
        abrirLista()
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(1200)
        captura("ficha")
    }
}
