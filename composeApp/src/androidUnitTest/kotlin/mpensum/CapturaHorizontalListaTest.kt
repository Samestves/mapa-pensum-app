package mpensum

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import mpensum.tema.Juego
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

/** La lista acostada a 915x412 dp: el riel, la lista de unos 400 dp y la página de la materia como panel a la derecha */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalListaTest : BaseExtras() {

    private fun captura(nombre: String) {
        correr(1200)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-lista-$nombre.png")
    }

    private fun abrirLista() {
        interior()
        regla.onNodeWithContentDescription("Lista").performClick()
        correr(2500)
    }

    /** Al entrar se elige la primera de «Puedes inscribir»: el panel nunca queda vacío */
    @Test
    fun lista() {
        abrirLista()
        captura("lista")
    }

    @Test
    fun listaClaro() {
        juego(Juego.CLARO)
        abrirLista()
        captura("lista-claro")
    }

    /** Tocar otra fila cambia el panel con un fundido */
    @Test
    fun otraMateria() {
        abrirLista()
        regla.onNodeWithText("Circuitos y Sistemas").performClick()
        captura("otra-materia")
    }
}
