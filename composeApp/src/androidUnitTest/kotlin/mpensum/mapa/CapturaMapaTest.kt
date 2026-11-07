package mpensum.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.BarraCarrera
import mpensum.carrera.FichaMateria
import mpensum.carrera.Vista
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Pinta el mapa en un teléfono simulado y guarda la imagen en
 * build/capturas/: sirve para mirarlo sin instalar nada. No compara nada;
 * solo falla si algo revienta al componer, medir o dibujar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaMapaTest {

    @get:Rule
    val regla = createComposeRule()

    /** Sin el plugin de Gradle, Roborazzi solo guarda la imagen si se lo pide así */
    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    private fun estado(slug: String, semestresAprobados: Int, cursando: Int = 2): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
        return EstadoCarrera(pensum).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= semestresAprobados }.forEach { marcar(it.codigo, Marca.APROBADA) }
            pensum.obligatorias.filter { it.semestre == semestresAprobados + 1 }.take(cursando).forEach { marcar(it.codigo, Marca.CURSANDO) }
        }
    }

    private fun capturar(nombre: String, estado: EstadoCarrera, elegida: String? = null) {
        regla.mainClock.autoAdvance = false
        val camara = Camara(estado.pensum.slug + nombre)
        val efectos = EfectosMapa()
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, elegida, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                FichaMateria(elegida, estado, alCerrar = {}, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {}, alEncuadrar = { _, _ -> })
                BarraCarrera(Vista.MAPA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
            }
        }
        // La luz de los cables no para nunca: se avanza el reloj a mano en vez de esperar a que se quede quieto
        regla.mainClock.advanceTimeBy(1600)
        regla.onRoot().captureRoboImage("build/capturas/$nombre.png")
    }

    @Test
    fun sistemasEnElQuinto() = capturar("sistemas-quinto", estado("ingenieria-de-sistemas", 4))

    @Test
    fun sistemasConUnaElegida() = capturar("sistemas-elegida", estado("ingenieria-de-sistemas", 4), elegida = "0713463")

    @Test
    fun ambientalConFranja() = capturar("ambiental-inicio", estado("ingenieria-ambiental", 0, 0))
}
