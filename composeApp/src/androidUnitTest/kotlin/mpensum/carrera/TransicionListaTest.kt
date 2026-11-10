package mpensum.carrera

import mpensum.setContentSincrono
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import mpensum.carrera.lista.NavegacionLista
import mpensum.carrera.lista.VistaLista
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lista → página → atrás, cuadro a cuadro: la página entra de la derecha sin
 * saltos, vuelve por donde vino y la lista queda exactamente donde estaba.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w375dp-h812dp-xxhdpi")
class TransicionListaTest {

    @get:Rule
    val regla = createComposeRule()

    private fun estado(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val marcas = pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA }
        return EstadoCarrera(pensum, demostracion = marcas)
    }

    private fun cuadros(n: Int) = repeat(n) {
        regla.waitForIdle()
        regla.mainClock.advanceTimeByFrame()
    }

    private fun izquierdaDe(texto: String) = regla.onAllNodesWithText(texto).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.left

    @Test
    fun laPaginaEntraYSaleSinSaltosYLaListaQuedaDondeEstaba() {
        val navegacion = NavegacionLista()
        regla.mainClock.autoAdvance = false
        val carrera = estado()
        regla.setContentSincrono {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                VistaLista(carrera, navegacion, abajo = 72.dp, acostada = false, derecha = 0.dp, alElegirElectiva = { _, _ -> }, alVerEnMapa = {})
            }
        }
        cuadros(60)
        val filaAntes = regla.onNodeWithContentDescription("Sistemas de Costo, Puedes inscribirla").fetchSemanticsNode().boundsInRoot
        assertTrue(izquierdaDe("Por hacer") == null, "la página no debe existir antes de abrirla")

        // Entrada: la etiqueta «Por hacer» de la página viaja de derecha a izquierda, sin retroceder ni saltar
        regla.onNodeWithContentDescription("Sistemas de Costo, Puedes inscribirla").performClick()
        val entrada = mutableListOf<Float>()
        repeat(40) {
            cuadros(1)
            izquierdaDe("Por hacer")?.let { entrada += it }
        }
        assertTrue(entrada.size > 10, "la página no entró")
        entrada.zipWithNext().forEach { (a, b) -> assertTrue(b <= a + 0.5f, "la página retrocedió al entrar: $a -> $b") }
        val ancho = regla.onNodeWithContentDescription("Sistemas de Costo, Puedes inscribirla").fetchSemanticsNode().boundsInRoot.width
        entrada.zipWithNext().forEach { (a, b) -> assertTrue(a - b < ancho * 0.45f, "un salto de ${a - b} px en un cuadro") }
        assertTrue(entrada.last() < entrada.first(), "la página no se movió")
        cuadros(40)
        assertTrue(navegacion.abierta)

        // Salida: de vuelta por donde vino
        navegacion.abierta = false
        val salida = mutableListOf<Float>()
        repeat(40) {
            cuadros(1)
            izquierdaDe("Por hacer")?.let { salida += it }
        }
        salida.zipWithNext().forEach { (a, b) -> assertTrue(b >= a - 0.5f, "la página volvió a entrar al salir: $a -> $b") }
        cuadros(60)
        assertTrue(izquierdaDe("Por hacer") == null, "la página sigue montada tras salir")

        // La lista queda exactamente donde estaba
        val filaDespues = regla.onNodeWithContentDescription("Sistemas de Costo, Puedes inscribirla").fetchSemanticsNode().boundsInRoot
        assertEquals(filaAntes.top, filaDespues.top, 0.5f)
        assertEquals(filaAntes.left, filaDespues.left, 0.5f)
        assertEquals(filaAntes.width, filaDespues.width, 0.5f)
    }
}
