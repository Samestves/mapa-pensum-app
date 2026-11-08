package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.carrera.lista.NavegacionLista
import mpensum.carrera.lista.VistaLista
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Pinta la lista «Por hacer» y la página de la materia en un teléfono de pie,
 * como en el interior (con la barra Núcleo encima), en oscuro y en claro, y
 * guarda las imágenes en build/capturas/. Sistemas como en las maquetas:
 * semestres 1 a 4 aprobados y dos materias del quinto en curso.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w375dp-h812dp-xxhdpi")
class CapturaListaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    @After
    fun volver() {
        Tema.juego = Juego.OSCURO
    }

    private fun sistemas(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        // Con `demostracion` no se toca lo guardado: cada prueba parte de las mismas marcas
        val marcas = pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO)
        return EstadoCarrera(pensum, demostracion = marcas)
    }

    private fun montar(estado: EstadoCarrera, navegacion: NavegacionLista = NavegacionLista()) {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            val cristal = remember { HazeState() }
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaLista(estado, navegacion, abajo = 14.dp + ALTO_BARRA, acostada = false, derecha = 0.dp, alElegirElectiva = { _, _ -> }, alVerEnMapa = {})
                }
                // La barra Núcleo se va mientras la página está abierta, como en el interior
                if (!navegacion.abierta) BarraCarrera(Vista.LISTA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
            }
        }
        correr(1200)
    }

    /** Cuadro a cuadro, dejando correr el hilo principal entre uno y otro: el margen se mide fuera de él */
    private fun correr(ms: Long) {
        repeat((ms / 16).toInt()) {
            regla.waitForIdle()
            regla.mainClock.advanceTimeByFrame()
            Thread.sleep(6)
        }
    }

    private fun capturar(nombre: String) {
        correr(1200)
        regla.onRoot().captureRoboImage("build/capturas/$nombre.png")
    }

    @Test
    fun listaOscuro() {
        montar(sistemas())
        capturar("lista-v2-oscuro")
    }

    @Test
    fun listaClaro() {
        Tema.juego = Juego.CLARO
        montar(sistemas())
        capturar("lista-v2-claro")
    }

    @Test
    fun listaPorSemestre() {
        montar(sistemas(), NavegacionLista(porEstado = false))
        capturar("lista-v2-semestre")
    }

    @Test
    fun aprobadasDesplegadas() {
        montar(sistemas(), NavegacionLista(aprobadasAbiertas = true))
        capturar("lista-v2-aprobadas")
    }

    @Test
    fun paginaOscuro() {
        montar(sistemas(), NavegacionLista(elegida = "0713463", abierta = true))
        capturar("lista-v2-pagina-oscuro")
    }

    @Test
    fun paginaClaro() {
        Tema.juego = Juego.CLARO
        montar(sistemas(), NavegacionLista(elegida = "0713463", abierta = true))
        capturar("lista-v2-pagina-claro")
    }

    @Test
    fun paginaDeUnaBloqueada() {
        montar(sistemas(), NavegacionLista(elegida = "0713113", abierta = true))
        capturar("lista-v2-pagina-bloqueada")
    }

    /** Marcar desde la página y volver: la fila se queda en su sitio con «Deshacer» en línea */
    @Test
    fun filaRecienMarcadaConDeshacer() {
        val navegacion = NavegacionLista()
        montar(sistemas(), navegacion)
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(600)
        regla.onNodeWithText("La estoy cursando").performClick()
        correr(400)
        capturar("lista-v2-pagina-cursando")
        regla.runOnUiThread { navegacion.abierta = false }
        // El realce de la fila se apaga en 1,2 s: se captura a mitad
        correr(700)
        regla.onRoot().captureRoboImage("build/capturas/lista-v2-deshacer-realce.png")
        correr(1500)
        regla.onRoot().captureRoboImage("build/capturas/lista-v2-deshacer.png")
    }

    @Test
    fun filaRecienMarcadaClaro() {
        Tema.juego = Juego.CLARO
        val navegacion = NavegacionLista()
        montar(sistemas(), navegacion)
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(600)
        regla.onNodeWithText("La estoy cursando").performClick()
        correr(300)
        regla.runOnUiThread { navegacion.abierta = false }
        capturar("lista-v2-deshacer-claro")
    }
}
