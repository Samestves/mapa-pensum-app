package mpensum

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.serialization.json.Json
import mpensum.bienvenida.Bienvenida
import mpensum.carrera.ALTO_BARRA
import mpensum.carrera.BarraCarrera
import mpensum.carrera.Borrador
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.EstadoHorario
import mpensum.carrera.FichaMateria
import mpensum.carrera.HojaAvance
import mpensum.carrera.HojasHorario
import mpensum.carrera.LocalFondoHoja
import mpensum.carrera.Vista
import mpensum.carrera.VistaHorario
import mpensum.carrera.VistaLista
import mpensum.datos.Carrera
import mpensum.datos.Guardado
import mpensum.datos.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
import mpensum.plataforma.HoraLocal
import mpensum.plataforma.Preferencias
import mpensum.portada.HojaAjustes
import mpensum.portada.PaginaAjustes
import mpensum.portada.Portada
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.colorDe
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
 * La app entera en el tema claro: portada, guía, mapa, lista, horario, ficha
 * y las hojas, montadas como en el teléfono (el lienzo con la luz de la
 * carrera y la barra Núcleo encima). Las imágenes van a build/capturas/ con
 * el prefijo «claro-»; no compara nada, solo falla si algo revienta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaTemaClaroTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Tema.juego = Juego.CLARO
    }

    /** El tema y lo guardado no pasan a otras pruebas: los dos viven todo el proceso */
    @AfterTest
    fun limpiar() {
        Tema.juego = Juego.OSCURO
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun pensum(slug: String = "ingenieria-de-sistemas") =
        leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    /** Sistemas en el quinto, como las maquetas: del primero al cuarto aprobados y dos en curso */
    private fun sistemas(): EstadoCarrera {
        val p = pensum()
        val marcas = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO }
        return EstadoCarrera(p, demostracion = marcas)
    }

    private fun foto(nombre: String) = regla.onRoot().captureRoboImage("build/capturas/claro-$nombre.png")

    private fun esperar(ms: Long) {
        var t = 0L
        // A pasos cortos: el avance de la portada se calcula aparte y tiene ocasión de llegar
        while (t < ms) {
            regla.mainClock.advanceTimeBy(100)
            t += 100
            Thread.sleep(3)
        }
    }

    /** El fondo del interior: el lienzo con la luz de la carrera arriba a la izquierda */
    @Composable
    private fun Interior(estado: EstadoCarrera, contenido: @Composable BoxScope.(HazeState) -> Unit) {
        val color = colorDe(estado.pensum.color)
        val cristal = HazeState()
        Box(
            Modifier.fillMaxSize().background(Paleta.lienzo).drawBehind {
                val r = size.width * 0.9f
                val c = Offset(size.width * 0.15f, -size.width * 0.1f)
                drawCircle(Brush.radialGradient(0f to color.copy(alpha = 0.14f), 0.7f to Color.Transparent, center = c, radius = r), r, c)
            },
        ) { contenido(cristal) }
    }

    private fun guardarMarcas() {
        val p = pensum()
        Guardado.guardarMarcas(
            p.slug,
            p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
                p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO },
        )
    }

    @Test
    fun portada() {
        guardarMarcas()
        regla.mainClock.autoAdvance = false
        regla.setContent { Portada(carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alAbrir = { _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        esperar(3000)
        foto("portada-arriba")
        regla.onRoot().performTouchInput { swipeUp(startY = centerY + 200f, endY = centerY - 300f) }
        esperar(1200)
        foto("portada-desplazada")
        regla.onNodeWithContentDescription("Ajustes y ayuda").performClick()
        esperar(900)
        foto("portada-ajustes")
    }

    @Test
    fun ajustes() {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        regla.mainClock.advanceTimeBy(900)
        foto("ajustes")
    }

    /** La guía: el mapa en demostración en el toque, y el paso de elegir carrera */
    @Test
    fun guia() {
        regla.mainClock.autoAdvance = false
        regla.setContent { Bienvenida(carreras, pasoInicial = 0) {} }
        val inicio = regla.mainClock.currentTime
        while (regla.mainClock.currentTime - inicio < 4_900L) regla.mainClock.advanceTimeByFrame()
        foto("guia-mapa")
    }

    @Test
    fun guiaCarrera() {
        regla.mainClock.autoAdvance = false
        regla.setContent { Bienvenida(carreras, pasoInicial = 3) {} }
        regla.mainClock.advanceTimeBy(1500)
        foto("guia-carrera")
    }

    @Test
    fun mapa() = mapaCon(null, "mapa")

    /** La ficha de una materia que puedes inscribir, abierta sobre el mapa */
    @Test
    fun ficha() = mapaCon("0713463", "ficha")

    private fun mapaCon(elegida: String?, nombre: String) {
        regla.mainClock.autoAdvance = false
        val estado = sistemas()
        val camara = Camara(estado.pensum.slug + "claro" + nombre)
        val efectos = EfectosMapa()
        regla.setContent {
            Interior(estado) { cristal ->
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, elegida, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                FichaMateria(elegida, estado, alCerrar = {}, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {}, alEncuadrar = { _, _ -> })
                BarraCarrera(Vista.MAPA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
            }
        }
        // La luz de los cables no para nunca: se avanza el reloj a mano en vez de esperar a que se quede quieto
        regla.mainClock.advanceTimeBy(1600)
        foto(nombre)
    }

    @Test
    fun lista() {
        regla.mainClock.autoAdvance = false
        val estado = sistemas()
        regla.setContent {
            Interior(estado) { cristal ->
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaLista(estado, abajo = 14.dp + ALTO_BARRA, alAbrirFicha = {}, alElegirElectiva = { _, _ -> })
                }
                BarraCarrera(Vista.LISTA, estado.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
            }
        }
        repeat(90) {
            regla.waitForIdle()
            regla.mainClock.advanceTimeByFrame()
        }
        foto("lista")
    }

    /** El horario de ejemplo de Sistemas, con el reloj parado el lunes a las 10:40 */
    private fun horario(): EstadoHorario {
        val p = pensum()
        fun h(x: Int) = x * 60
        val ejemplo = listOf(
            Triple("0624622", 0, h(7) to h(9)) to "Aula 12", Triple("0624622", 2, h(7) to h(9)) to "Aula 12",
            Triple("0713122", 1, h(9) to h(11)) to "Lab 3", Triple("0713122", 3, h(9) to h(11)) to "Lab 3",
            Triple("0713172", 0, h(10) to h(12)) to "Aula 5",
            Triple("0713463", 1, h(13) to h(15)) to "Aula 8", Triple("0713463", 3, h(13) to h(15)) to "Aula 8",
            Triple("0723913", 2, h(10) to h(12)) to "Aula 2", Triple("0723913", 4, h(8) to h(10)) to "Aula 2",
            Triple("0713632", 4, h(13) to h(15)) to "Aula 9",
        )
        HorarioGuardado.guardar(p.slug, emptyList())
        val carrera = EstadoCarrera(p).apply {
            marcarVarias(p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA })
            marcarVarias(ejemplo.associate { it.first.first to Marca.CURSANDO })
        }
        return EstadoHorario(carrera) { HoraLocal(0, 640, 0) }.apply {
            ejemplo.forEach { (c, aula) -> guardar(Borrador(null, c.first, c.second, c.third.first, c.third.second, aula = aula, seccion = "01")) }
            dia = 0
        }
    }

    private fun capturarHorario(nombre: String, horario: EstadoHorario) {
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Interior(horario.carrera) { cristal ->
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaHorario(horario, abajo = 14.dp + ALTO_BARRA, horizontal = false)
                }
                BarraCarrera(Vista.HORARIO, horario.carrera.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
                HojasHorario(horario, cristal)
            }
        }
        regla.mainClock.advanceTimeBy(1600)
        foto(nombre)
    }

    @Test
    fun horarioDia() = capturarHorario("horario-dia", horario())

    /** La hoja de avance abierta sobre el mapa */
    @Test
    fun avance() {
        regla.mainClock.autoAdvance = false
        val estado = sistemas()
        val camara = Camara(estado.pensum.slug + "claro-avance")
        val efectos = EfectosMapa()
        regla.setContent {
            Interior(estado) { cristal ->
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, efectos, null, congelado = true, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                CompositionLocalProvider(LocalFondoHoja provides cristal) {
                    HojaAvance(true, estado, alCerrar = {}, alVerEnMapa = {})
                }
            }
        }
        regla.mainClock.advanceTimeBy(1600)
        foto("avance")
    }
}
