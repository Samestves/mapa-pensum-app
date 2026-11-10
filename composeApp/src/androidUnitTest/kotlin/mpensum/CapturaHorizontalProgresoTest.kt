package mpensum

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.HojaAvance
import mpensum.carrera.LocalFondoHoja
import mpensum.carrera.latidoActivo
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
import mpensum.plataforma.Preferencias
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * La hoja de progreso y su detalle «¿Cuándo me gradúo?» con el teléfono
 * acostado (915 × 412 dp). Las imágenes van a build/capturas/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaHorizontalProgresoTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        // Un latido eterno no deja al reloj de pruebas llegar al reposo
        latidoActivo = false
    }

    @AfterTest
    fun limpiar() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    /** Sistemas con el primer al cuarto semestre aprobados y dos materias en curso */
    private fun estado(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        return EstadoCarrera(pensum, compacto = true).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { marcar(it.codigo, Marca.APROBADA) }
            pensum.obligatorias.filter { it.semestre == 5 }.take(2).forEach { marcar(it.codigo, Marca.CURSANDO) }
            elegirCarga(6)
        }
    }

    private fun montar(estado: EstadoCarrera) {
        regla.mainClock.autoAdvance = false
        val camara = Camara(estado.pensum.slug + "horizontal-progreso")
        val efectos = EfectosMapa()
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, null, congelado = true, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                CompositionLocalProvider(LocalFondoHoja provides cristal) {
                    HojaAvance(true, estado, alCerrar = {}, alVerEnMapa = {})
                }
            }
        }
        regla.mainClock.advanceTimeBy(1600)
    }

    private fun foto(nombre: String) = regla.onRoot().captureRoboImage("build/capturas/horizontal-progreso-$nombre.png")

    @Test
    fun hoja() {
        montar(estado())
        foto("1-hoja")
    }

    private fun detalle(fila: String, nombre: String) {
        montar(estado())
        // La fila del índice va antes que el título del panel, que puede decir lo mismo
        regla.onAllNodesWithText(fila)[0].performClick()
        regla.mainClock.advanceTimeBy(1400)
        foto(nombre)
    }

    @Test
    fun graduacion() = detalle("¿Cuándo me gradúo?", "2-graduacion")

    @Test
    fun esteSemestre() = detalle("Este semestre", "3-este-semestre")

    @Test
    fun proximo() = detalle("Tu próximo semestre", "4-proximo")

    @Test
    fun cadena() = detalle("No puedes atrasar", "5-cadena")
}
