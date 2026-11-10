package mpensum.carrera

import mpensum.componentes.LocalFondoHoja
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.up
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.carrera.MapaCarrera
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Las hojas inferiores sobre el mapa: abierta, a media entrada y a mitad de
 * arrastre (con el fondo desenfocado y el velo aclarándose), y que el gesto
 * cierre por distancia y vuelva si se queda corto. Las imágenes van a
 * build/capturas/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaHojaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    private fun estado(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        return EstadoCarrera(pensum).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { marcar(it.codigo, Marca.APROBADA) }
            pensum.obligatorias.filter { it.semestre == 5 }.take(2).forEach { marcar(it.codigo, Marca.CURSANDO) }
        }
    }

    private var abierta by mutableStateOf(true)

    private fun montar() {
        regla.mainClock.autoAdvance = false
        abierta = true
        val estado = estado()
        val camara = Camara(estado.pensum.slug + "hoja")
        val efectos = EfectosMapa()
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, null, congelado = true, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                CompositionLocalProvider(LocalFondoHoja provides cristal) {
                    HojaAvance(abierta, estado, alCerrar = { abierta = false }, alVerEnMapa = {})
                }
            }
        }
    }

    private fun foto(nombre: String) = regla.onRoot().captureRoboImage("build/capturas/$nombre.png")

    @Test
    fun abierta() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        foto("hoja-1-abierta")
    }

    /** A los 120 ms del resorte: ya va por la mitad y el fondo se está difuminando */
    @Test
    fun entrando() {
        montar()
        regla.mainClock.advanceTimeBy(160)
        foto("hoja-2-entrando")
    }

    @Test
    fun amitadDeArrastre() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, 300f))
        }
        regla.mainClock.advanceTimeBy(50)
        foto("hoja-3-arrastre")
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput { up() }
    }

    @Test
    fun tirarHaciaArribaEsGoma() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, -40f))
            moveBy(Offset(0f, -600f))
        }
        regla.mainClock.advanceTimeBy(50)
        foto("hoja-4-goma")
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput { up() }
        regla.mainClock.advanceTimeBy(1000)
        assertTrue(abierta)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun uncortoVuelve() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, 60f))
            // Parado antes de soltar: sin velocidad, y por debajo del 25 %
            advanceEventTime(300)
            up()
        }
        regla.mainClock.advanceTimeBy(1000)
        assertTrue(abierta)
        foto("hoja-5-vuelve")
    }

    @Test
    fun largoCierra() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, 700f))
            advanceEventTime(300)
            up()
        }
        regla.mainClock.advanceTimeBy(100)
        foto("hoja-6-saliendo")
        regla.mainClock.advanceTimeBy(1000)
        assertFalse(abierta)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun tironRapidoCierra() {
        montar()
        regla.mainClock.advanceTimeBy(1600)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).performTouchInput {
            swipeDown(startY = top + 10f, endY = top + 160f, durationMillis = 60)
        }
        regla.mainClock.advanceTimeBy(1000)
        assertFalse(abierta)
        regla.onNodeWithTag("hoja-cabecera", useUnmergedTree = true).assertDoesNotExist()
    }
}
