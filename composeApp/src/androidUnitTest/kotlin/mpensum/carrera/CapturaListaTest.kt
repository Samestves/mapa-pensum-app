package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.tema.Paleta
import mpensum.tema.colorDe
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Pinta la lista en un teléfono simulado, como en el interior (con la barra
 * Núcleo encima), y guarda la imagen en build/capturas/. Sistemas como en las
 * maquetas: semestres 1 a 4 aprobados y dos materias del quinto en curso.
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

    private fun sistemas(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        // Con `demostracion` no se toca lo guardado: cada prueba parte de las mismas marcas
        val marcas = pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO)
        return EstadoCarrera(pensum, demostracion = marcas)
    }

    private fun montar(estado: EstadoCarrera) {
        regla.mainClock.autoAdvance = false
        val color = colorDe(estado.pensum.color.oscuro)
        regla.setContent {
            val cristal = remember { HazeState() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Paleta.lienzo)
                    .drawBehind {
                        val r = size.width * 0.9f
                        val c = Offset(size.width * 0.15f, -size.width * 0.1f)
                        drawCircle(Brush.radialGradient(0f to color.copy(alpha = 0.14f), 0.7f to Color.Transparent, center = c, radius = r), r, c)
                    },
            ) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaLista(estado, abajo = 14.dp + ALTO_BARRA, alAbrirFicha = {}, alElegirElectiva = { _, _ -> })
                }
                BarraCarrera(Vista.LISTA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
            }
        }
        regla.mainClock.advanceTimeBy(1200)
    }

    private fun capturar(nombre: String) {
        // Cuadro a cuadro, dejando correr el hilo principal entre uno y otro: lo que cambia un toque se aplica ahí
        repeat(75) {
            regla.waitForIdle()
            regla.mainClock.advanceTimeByFrame()
        }
        regla.onRoot().captureRoboImage("build/capturas/$nombre.png")
    }

    @Test
    fun inicial() {
        montar(sistemas())
        capturar("lista-inicial")
    }

    @Test
    fun conFiltroDisponibles() {
        montar(sistemas())
        regla.onNodeWithContentDescription("Disponibles", substring = true).performClick()
        capturar("lista-disponibles")
    }

    @Test
    fun filaRecienMarcada() {
        montar(sistemas())
        regla.onNodeWithText("Sistemas de Costo").performClick()
        // Dentro del plazo de deshacer: la fila ya dice "Cursando" y ofrece volver atrás
        capturar("lista-marcada")
    }
}
