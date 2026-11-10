package mpensum.carrera

import mpensum.navegacion.Vista
import mpensum.setContentSincrono
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
import androidx.compose.ui.unit.Dp
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
 * Pinta la lista «Por hacer» y la página de la materia como el interior (con la
 * barra Núcleo encima de la lista), en oscuro y en claro, y guarda las
 * imágenes en build/capturas/lista-v4-*. Sistemas como en las maquetas:
 * semestres 1 a 4 aprobados y dos materias del quinto en curso.
 *
 * Robolectric no trae insets: `arriba` y `abajo` los imitan con relleno (barra
 * de estado; abajo, gestos de 24 dp o la barra de tres botones de 48 dp), que
 * es lo que la página resta de su alto. Con 360×640 y tres botones quedan 568 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
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

    private fun carrera(archivo: String, hasta: Int, cursando: List<String> = emptyList()): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/$archivo.json").readText())
        // Con `demostracion` no se toca lo guardado: cada prueba parte de las mismas marcas
        val marcas = pensum.obligatorias.filter { (it.semestre ?: 99) <= hasta }.associate { it.codigo to Marca.APROBADA } + cursando.associateWith { Marca.CURSANDO }
        return EstadoCarrera(pensum, demostracion = marcas)
    }

    private fun sistemas() = carrera("ingenieria-de-sistemas", 4, listOf("0624622", "0713122"))

    private fun montar(
        estado: EstadoCarrera,
        navegacion: NavegacionLista = NavegacionLista(),
        arriba: Dp = 28.dp,
        abajo: Dp = 24.dp,
        acostada: Boolean = false,
    ) {
        regla.mainClock.autoAdvance = false
        regla.setContentSincrono {
            val cristal = remember { HazeState() }
            Box(Modifier.fillMaxSize().background(Paleta.lienzo).padding(top = if (acostada) 0.dp else arriba, bottom = if (acostada) 0.dp else abajo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaLista(estado, navegacion, abajo = if (acostada) 8.dp else 14.dp + ALTO_BARRA, acostada = acostada, derecha = 0.dp, alElegirElectiva = { _, _ -> }, alVerEnMapa = {})
                }
                // La barra Núcleo se va mientras la página está abierta, como en el interior
                if (!navegacion.abierta && !acostada) BarraCarrera(Vista.LISTA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
            }
        }
        correr(1200)
    }

    /** Cuadro a cuadro, dejando correr el hilo principal entre uno y otro: el margen se mide fuera de él */
    private fun correr(ms: Long) {
        repeat((ms / 16).toInt()) {
            regla.waitForIdle()
            regla.mainClock.advanceTimeByFrame()
        }
    }

    private fun capturar(nombre: String) {
        correr(1200)
        regla.onRoot().captureRoboImage("build/capturas/lista-v4-$nombre.png")
    }

    // ───────────────────────── 390×844, con gestos ─────────────────────────

    @Test
    fun listaOscuro() {
        montar(sistemas())
        capturar("lista-oscuro")
    }

    @Test
    fun listaClaro() {
        Tema.juego = Juego.CLARO
        montar(sistemas())
        capturar("lista-claro")
    }

    @Test
    fun listaPorSemestre() {
        montar(sistemas(), NavegacionLista(porEstado = false))
        capturar("lista-semestre")
    }

    @Test
    fun aprobadasDesplegadas() {
        montar(sistemas(), NavegacionLista(aprobadasAbiertas = true))
        capturar("lista-aprobadas")
    }

    @Test
    fun paginaOscuro() {
        montar(sistemas(), NavegacionLista(elegida = "0713463", abierta = true))
        capturar("pagina-oscuro")
    }

    @Test
    fun paginaClaro() {
        Tema.juego = Juego.CLARO
        montar(sistemas(), NavegacionLista(elegida = "0713463", abierta = true))
        capturar("pagina-claro")
    }

    @Test
    fun paginaDeUnaBloqueada() {
        montar(sistemas(), NavegacionLista(elegida = "0713113", abierta = true))
        capturar("pagina-bloqueada")
    }

    @Test
    fun paginaDeUnaAprobada() {
        montar(sistemas(), NavegacionLista(elegida = "0061013", abierta = true))
        capturar("pagina-aprobada")
    }

    /** Sin prelaciones: Comprensión y Expresión Lingüística I de Petróleo, también la que más abre (4) */
    @Test
    fun paginaSinPrelaciones() {
        montar(carrera("ingenieria-de-petroleo", 0), NavegacionLista(elegida = "0061013", abierta = true))
        capturar("pagina-sin-prelaciones")
    }

    // ───────────────────────── 360×640, con la barra de tres botones ─────────────────────────

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun lista360() {
        montar(sistemas(), arriba = 24.dp, abajo = 48.dp)
        capturar("lista-360")
    }

    /** El peor caso de «Pide»: Sistemas Inteligentes pide tres */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360MasPrelaciones() {
        // Es una electiva técnica: para verla se elige en una casilla
        val estado = sistemas().also { it.colocar("casilla-tecnica-4", "0714963") }
        montar(estado, NavegacionLista(elegida = "0714963", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-pide3")
    }

    /** El peor caso de las dos a la vez: Yacimientos I de Petróleo pide 2 y abre 4 */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360PideYAbre() {
        montar(carrera("ingenieria-de-petroleo", 4), NavegacionLista(elegida = "0633123", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-yacimientos")
    }

    /** Sin prelaciones y la que más abre */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360SinPrelaciones() {
        montar(carrera("ingenieria-de-petroleo", 0), NavegacionLista(elegida = "0061013", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-sin-prelaciones")
    }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360Bloqueada() {
        montar(sistemas(), NavegacionLista(elegida = "0713113", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-bloqueada")
    }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360Aprobada() {
        montar(sistemas(), NavegacionLista(elegida = "0061013", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-aprobada")
    }

    /** Un nombre largo (dos líneas) es el caso que más alto ocupa la cabeza */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun pagina360NombreLargo() {
        montar(sistemas(), NavegacionLista(elegida = "0713653", abierta = true), arriba = 24.dp, abajo = 48.dp)
        capturar("pagina-360-largo")
    }

    /** «+N más» abre la lista entera de ese lado */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun subpagina360() {
        val navegacion = NavegacionLista(elegida = "0633123", abierta = true)
        montar(carrera("ingenieria-de-petroleo", 4), navegacion, arriba = 24.dp, abajo = 48.dp)
        // A 360×640 «Abre» enseña una y cuenta las otras tres
        regla.onNodeWithText("+3 más").performClick()
        capturar("subpagina-360")
    }

    // ───────────────────────── 915×412, acostado ─────────────────────────

    @Config(qualifiers = "w915dp-h412dp-land-xxhdpi")
    @Test
    fun acostada() {
        montar(sistemas(), NavegacionLista(elegida = "0713463"), acostada = true)
        capturar("acostada")
    }

    @Config(qualifiers = "w915dp-h412dp-land-xxhdpi")
    @Test
    fun acostadaPideYAbre() {
        montar(carrera("ingenieria-de-petroleo", 4), NavegacionLista(elegida = "0633123"), acostada = true)
        capturar("acostada-yacimientos")
    }

    @Config(qualifiers = "w915dp-h412dp-land-xxhdpi")
    @Test
    fun acostadaClaroSinPrelaciones() {
        Tema.juego = Juego.CLARO
        montar(carrera("ingenieria-de-petroleo", 0), NavegacionLista(elegida = "0061013"), acostada = true)
        capturar("acostada-claro-sin-prelaciones")
    }

    // ───────────────────────── Marcar ─────────────────────────

    /** Marcar desde la página y volver: la fila se queda en su sitio con «Deshacer» en línea */
    @Test
    fun filaRecienMarcadaConDeshacer() {
        val navegacion = NavegacionLista()
        montar(sistemas(), navegacion)
        regla.onNodeWithText("Sistemas de Costo").performClick()
        correr(600)
        regla.onNodeWithText("La estoy cursando").performClick()
        correr(400)
        capturar("pagina-cursando")
        regla.runOnUiThread { navegacion.abierta = false }
        // El realce de la fila se apaga en 1,2 s: se captura a mitad
        correr(700)
        regla.onRoot().captureRoboImage("build/capturas/lista-v4-deshacer-realce.png")
        correr(1500)
        regla.onRoot().captureRoboImage("build/capturas/lista-v4-deshacer.png")
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
        capturar("deshacer-claro")
    }
}
