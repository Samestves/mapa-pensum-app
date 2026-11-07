package mpensum

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import mpensum.carrera.EstadoCarrera
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
import mpensum.plataforma.Preferencias
import mpensum.tema.FormaCables
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
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
 * El mapa en los juegos nuevos: fondo liso, de puntos y de rejilla, cables
 * rectos y Más contraste. Las imágenes van a build/capturas/mapa-tema-*.png;
 * no compara nada, solo falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaTemaMapaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    @AfterTest
    fun limpiar() {
        Tema.juego = Juego.OSCURO
        Tema.fondo = Juego.OSCURO.fondo
        Tema.cables = FormaCables.CURVOS
        Tema.contraste = false
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun mapa(nombre: String, juego: Juego, cables: FormaCables = FormaCables.CURVOS, contraste: Boolean = false) {
        Tema.juego = juego
        Tema.fondo = juego.fondo
        Tema.cables = cables
        Tema.contraste = contraste
        val p = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val marcas = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO }
        val estado = EstadoCarrera(p, demostracion = marcas)
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                MapaCarrera(estado, Camara(p.slug + nombre), EfectosMapa(), null, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
            }
        }
        regla.mainClock.advanceTimeBy(1600)
        regla.onRoot().captureRoboImage("build/capturas/mapa-tema-$nombre.png")
    }

    @Test fun exteriorOscuro() = mapa("exterior-oscuro", Juego.EXTERIOR_OSCURO)
    @Test fun exterior() = mapa("exterior", Juego.EXTERIOR)
    @Test fun grafitoPuntos() = mapa("grafito-puntos", Juego.GRAFITO)
    @Test fun negroLiso() = mapa("negro-liso", Juego.NEGRO)
    @Test fun medianocheRectos() = mapa("medianoche-rectos", Juego.MEDIANOCHE, FormaCables.RECTOS)
    @Test fun cuarzoContraste() = mapa("cuarzo-contraste", Juego.CUARZO, contraste = true)
}
