package mpensum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.FichaMateria
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.logica.Situacion
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
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
 * La ficha de materia del mapa (docs/ficha-y-lista.md §1) sobre Sistemas con
 * los semestres 1 a 4 aprobados y dos materias en curso: disponible, cursando,
 * aprobada, bloqueada y la casilla de electiva vacía. Las imágenes van a
 * build/capturas/ficha-{estado}-{tam}.png.
 */
abstract class BaseFichaMapa(private val tam: String) {
    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    private fun sistemas(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        return EstadoCarrera(pensum).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { marcar(it.codigo, Marca.APROBADA) }
            marcar("0624622", Marca.CURSANDO)
            marcar("0713122", Marca.CURSANDO)
        }
    }

    private fun captura(nombre: String, elegir: (EstadoCarrera) -> String) {
        regla.mainClock.autoAdvance = false
        val estado = sistemas()
        val codigo = elegir(estado)
        val camara = Camara("ficha-$nombre-$tam")
        val efectos = EfectosMapa()
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, codigo, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                FichaMateria(codigo, estado, alCerrar = {}, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {}, alEncuadrar = { _, _ -> })
            }
        }
        // El cálculo de la respuesta va en otro hilo: se deja pasar tiempo real además del del reloj
        repeat(40) {
            regla.mainClock.advanceTimeBy(50)
            Thread.sleep(10)
            regla.waitForIdle()
        }
        regla.onRoot().captureRoboImage("build/capturas/ficha-$nombre-$tam.png")
    }

    private fun primera(e: EstadoCarrera, s: Situacion) = e.layout.nodos.first { it.asignatura.semestre != null && !it.asignatura.esHueco && e.situaciones[it.codigo] == s }.codigo

    @Test
    fun disponible() = captura("disponible") { it.situaciones.entries.first { (c, s) -> s == Situacion.INSCRIBIBLE && it.estados[c] != null && c == "0713463" }.key }

    @Test
    fun cursando() = captura("cursando") { "0624622" }

    @Test
    fun aprobada() = captura("aprobada") { primera(it, Situacion.HECHA) }

    @Test
    fun bloqueada() = captura("bloqueada") { primera(it, Situacion.LEJANA) }

    @Test
    fun casillaVacia() = captura("casilla") { e -> e.layout.nodos.first { it.asignatura.esHueco && e.enCasilla(it.codigo) == null }.codigo }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xxhdpi")
class CapturaFichaMapa360Test : BaseFichaMapa("360")

/** El teléfono pequeño: aquí los botones van apilados */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-xxhdpi")
class CapturaFichaMapa320Test : BaseFichaMapa("320")

/** Acostado: de momento la hoja ocupa la pantalla, con las mismas piezas en dos columnas */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w780dp-h360dp-land-xxhdpi")
class CapturaFichaMapaAcostadaTest : BaseFichaMapa("acostada")
