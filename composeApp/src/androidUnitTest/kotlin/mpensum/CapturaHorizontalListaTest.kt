package mpensum

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

/** La lista en horizontal a 915x412 dp: columna de lectura y, al lado, el resumen del semestre o la ficha */
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
        // Hay dos con ese nombre: la fila de la lista y el renglón del resumen; aquí se toca la fila
        regla.onAllNodesWithText("Sistemas de Costo")[0].performClick()
        correr(1200)
        captura("ficha")
    }

    @Test
    fun fichaDesdeElResumen() {
        abrirLista()
        regla.onAllNodesWithText("Sistemas de Costo")[1].performClick()
        correr(1200)
        captura("ficha-desde-resumen")
    }
}
