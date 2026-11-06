package mpensum

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.carrera.Interior
import mpensum.datos.Carrera
import mpensum.datos.Guardado
import mpensum.datos.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.logica.Sesion
import mpensum.plataforma.Preferencias
import mpensum.portada.HojaAjustes
import mpensum.portada.PaginaAjustes
import mpensum.portada.Portada
import mpensum.tema.Paleta
import mpensum.tema.Tema
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * La matriz responsive: las ocho pantallas principales en un teléfono
 * pequeño (320), uno corriente (360) y el de las maquetas (412) con la letra
 * del sistema a 1,3. Sistemas con los cuatro primeros semestres aprobados,
 * dos materias en curso y una semana guardada. Las imágenes van a
 * build/capturas/resp-{pantalla}-{tam}.png; la guía es docs/sistema-diseno.md.
 */
abstract class BaseResponsive(private val tam: String, private val escalaLetra: Float = 1f) {
    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())
    private val sistemas get() = carreras.first { it.slug == "ingenieria-de-sistemas" }

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Preferencias.marcarBienvenidaVista()
        // El Tema es global: lo que dejara otra prueba (o la detección de gama baja) no cuela en la captura
        Tema.ligero = false
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        Guardado.guardarMarcas(
            sistemas.slug,
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
                mapOf("0624622" to Marca.CURSANDO, "0713122" to Marca.CURSANDO),
        )
        // Cuatro clases de las dos materias en curso: la semana tiene con qué llenarse
        HorarioGuardado.guardar(
            sistemas.slug,
            listOf(
                Sesion("r1", "0624622", 0, 7 * 60, 9 * 60, "01", "Aula 12"),
                Sesion("r2", "0624622", 2, 7 * 60, 9 * 60, "01", "Aula 12"),
                Sesion("r3", "0713122", 1, 9 * 60, 11 * 60, "01", "Lab 3"),
                Sesion("r4", "0713122", 3, 9 * 60, 11 * 60, "01", "Lab 3"),
            ),
        )
        regla.mainClock.autoAdvance = false
    }

    private fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
            regla.waitForIdle()
        }
    }

    private fun foto(pantalla: String) {
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/resp-$pantalla-$tam.png")
    }

    /** Monta con la escala de letra de esta configuración encima de la del sistema */
    private fun montar(contenido: @Composable () -> Unit) {
        regla.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * escalaLetra)) { contenido() }
        }
        correr(3000)
    }

    private fun interior() = montar { Interior(sistemas, alVolver = {}) }

    private fun pulsar(texto: String, ms: Long = 1400) {
        regla.onNodeWithText(texto).performClick()
        correr(ms)
    }

    private fun pulsarIcono(descripcion: String, ms: Long = 1500) {
        regla.onNodeWithContentDescription(descripcion, substring = true).performClick()
        correr(ms)
    }

    @Test
    fun portada() {
        montar { Portada(carreras, sistemas, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        foto("portada")
    }

    @Test
    fun mapa() {
        interior()
        foto("mapa")
    }

    /** El mapa en modo ligero: cristal liso, cables sin degradado y sin la luz que corre */
    @Test
    fun mapaLigero() {
        Tema.ligero = true
        try {
            interior()
            foto("mapa-ligero")
        } finally {
            Tema.ligero = false
        }
    }

    @Test
    fun lista() {
        interior()
        pulsarIcono("Lista")
        foto("lista")
    }

    @Test
    fun ficha() {
        interior()
        pulsarIcono("Lista")
        pulsar("Sistemas de Costo", 1200)
        foto("ficha")
    }

    @Test
    fun avance() {
        interior()
        pulsarIcono("Tu avance")
        foto("avance")
    }

    @Test
    fun graduacion() {
        interior()
        pulsarIcono("Tu avance")
        pulsar("TE GRADÚAS")
        foto("graduacion")
    }

    @Test
    fun avanceSemestres() {
        interior()
        pulsarIcono("Tu avance")
        pulsar("LLEVAS")
        foto("avance-semestres")
    }

    @Test
    fun avanceInscribir() {
        interior()
        pulsarIcono("Tu avance")
        pulsar("PUEDES INSCRIBIR")
        foto("avance-inscribir")
    }

    @Test
    fun avanceElectivas() {
        interior()
        pulsarIcono("Tu avance")
        pulsar("TÉCNICAS")
        foto("avance-electivas")
    }

    @Test
    fun horarioSemana() {
        interior()
        pulsarIcono("Horario")
        pulsar("Semana")
        foto("horario-semana")
    }

    @Test
    fun ajustes() {
        montar {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, sistemas, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        foto("ajustes")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h640dp-xxhdpi")
class CapturaResponsive320Test : BaseResponsive("320")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xxhdpi")
class CapturaResponsive360Test : BaseResponsive("360")

/** El teléfono de las maquetas con la letra del sistema a 1,3: lo que más rompe filas y botones */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class CapturaResponsive412Test : BaseResponsive("412-letra13", escalaLetra = 1.3f)
