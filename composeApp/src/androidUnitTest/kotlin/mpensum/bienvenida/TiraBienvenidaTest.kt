package mpensum.bienvenida

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.tema.Juego
import mpensum.tema.Tema
import org.junit.Assume
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Lo que tarda en llegar el pensum del avance, en tiempo real: el reloj de la escena espera parado a que llegue */
private const val ESPERA_PENSUM_MS = 1_500L

/**
 * Monta un paso de la bienvenida con el reloj a mano y devuelve el instante
 * en que empieza su escena. El avance lee su pensum fuera del hilo principal:
 * se le da tiempo real y unos cuadros para que llegue y su reloj arranque.
 */
private fun montar(regla: ComposeContentTestRule, paso: Int, carreras: List<Carrera> = emptyList()): Long {
    regla.mainClock.autoAdvance = false
    regla.setContent { Bienvenida(carreras, pasoInicial = paso) {} }
    if (paso == 2) {
        Thread.sleep(ESPERA_PENSUM_MS)
        repeat(3) { regla.mainClock.advanceTimeByFrame() }
    }
    return regla.mainClock.currentTime
}

/** Avanza el reloj de prueba (cuadros de 16 ms) hasta `ms` después de `inicio` y fotografía la pantalla */
private fun foto(regla: ComposeContentTestRule, inicio: Long, ms: Long, ruta: String) {
    while (regla.mainClock.currentTime - inicio < ms) regla.mainClock.advanceTimeByFrame()
    regla.onRoot().captureRoboImage(ruta)
}

/** Pone las variantes en prueba antes de montar; `limpiar` las devuelve a las de la app */
private fun conVariantes(foto: VarianteFoto = Variantes.foto, avance: VarianteAvance = Variantes.avance) {
    Variantes.foto = foto
    Variantes.avance = avance
}

private val FOTO_APP = Variantes.foto
private val AVANCE_APP = Variantes.avance

private fun limpiarVariantes() {
    Variantes.foto = FOTO_APP
    Variantes.avance = AVANCE_APP
}

/** Los instantes de cada variante: lo que cuenta su historia, de la llegada al final vivo */
private val INSTANTES_INSTANTANEA = listOf(300L, 700L, 1_300L, 2_100L, 2_700L, 3_450L, 4_050L, 5_700L, 8_000L)
private val INSTANTES_GALERIA = listOf(400L, 850L, 1_100L, 1_450L, 2_100L, 2_700L, 3_450L, 5_700L, 8_000L)
private val INSTANTES_AVANCE = listOf(500L, 1_600L, 2_600L, 3_900L, 4_800L, 5_700L, 6_600L, 7_500L, 8_400L)

/**
 * Las tiras de las pantallas 2 y 3 de la bienvenida (v2), una por variante:
 * nueve instantes de cada ciclo, de pie, en build/capturas/tira/. Se unen con
 * ffmpeg en docs/maquetas/. No compara nada.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class TiraBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    @AfterTest
    fun limpiar() = limpiarVariantes()

    private fun tira(paso: Int, nombre: String, instantes: List<Long>) {
        val inicio = montar(regla, paso)
        instantes.forEachIndexed { i, ms -> foto(regla, inicio, ms, "build/capturas/tira/$nombre-pie-${i + 1}.png") }
    }

    @Test
    fun horarioInstantanea() {
        conVariantes(foto = VarianteFoto.INSTANTANEA)
        tira(1, "paso2-instantanea", INSTANTES_INSTANTANEA)
    }

    @Test
    fun horarioGaleria() {
        conVariantes(foto = VarianteFoto.GALERIA)
        tira(1, "paso2-galeria", INSTANTES_GALERIA)
    }

    @Test
    fun avanceTarjeta() {
        conVariantes(avance = VarianteAvance.TARJETA)
        tira(2, "paso3-a-tarjeta", INSTANTES_AVANCE)
    }

    @Test
    fun avanceAnillo() {
        conVariantes(avance = VarianteAvance.ANILLO)
        tira(2, "paso3-b-anillo", INSTANTES_AVANCE)
    }

    @Test
    fun avanceTitulo() {
        conVariantes(avance = VarianteAvance.TITULO)
        tira(2, "paso3-c-titulo", INSTANTES_AVANCE)
    }
}

/** Las mismas escenas acostadas (915 × 412 dp), en tres instantes */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class TiraHorizontalBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    @AfterTest
    fun limpiar() = limpiarVariantes()

    private fun tira(paso: Int, nombre: String, instantes: List<Long>) {
        val inicio = montar(regla, paso)
        instantes.forEachIndexed { i, ms -> foto(regla, inicio, ms, "build/capturas/tira/$nombre-acostado-${i + 1}.png") }
    }

    @Test
    fun horarioInstantanea() {
        conVariantes(foto = VarianteFoto.INSTANTANEA)
        tira(1, "paso2-instantanea", listOf(1_300L, 3_450L, 8_000L))
    }

    @Test
    fun horarioGaleria() {
        conVariantes(foto = VarianteFoto.GALERIA)
        tira(1, "paso2-galeria", listOf(850L, 2_100L, 8_000L))
    }

    @Test
    fun avanceTarjeta() {
        conVariantes(avance = VarianteAvance.TARJETA)
        tira(2, "paso3-a-tarjeta", listOf(2_600L, 5_700L, 8_400L))
    }

    @Test
    fun avanceAnillo() {
        conVariantes(avance = VarianteAvance.ANILLO)
        tira(2, "paso3-b-anillo", listOf(2_600L, 5_700L, 8_400L))
    }

    @Test
    fun avanceTitulo() {
        conVariantes(avance = VarianteAvance.TITULO)
        tira(2, "paso3-c-titulo", listOf(2_600L, 5_700L, 8_400L))
    }
}

/**
 * Un ciclo entero de cada variante a 30 cuadros por segundo, para hacer el
 * vídeo con ffmpeg (build/capturas/video/). Tarda: solo corre con la
 * variable de entorno TIRA_VIDEO=1. A 1,5x para que los cuadros pesen poco.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-hdpi")
class VideoBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        Assume.assumeTrue(System.getenv("TIRA_VIDEO") == "1")
        System.setProperty("roborazzi.test.record", "true")
    }

    @AfterTest
    fun limpiar() = limpiarVariantes()

    private fun video(paso: Int, nombre: String) {
        val inicio = montar(regla, paso)
        for (i in 0 until 300) {
            val n = (i + 1).toString().padStart(3, '0')
            foto(regla, inicio, i * 1_000L / 30, "build/capturas/video/$nombre/f$n.png")
        }
    }

    @Test
    fun horarioInstantanea() {
        conVariantes(foto = VarianteFoto.INSTANTANEA)
        video(1, "paso2-instantanea")
    }

    @Test
    fun horarioGaleria() {
        conVariantes(foto = VarianteFoto.GALERIA)
        video(1, "paso2-galeria")
    }

    @Test
    fun avanceTarjeta() {
        conVariantes(avance = VarianteAvance.TARJETA)
        video(2, "paso3-a-tarjeta")
    }

    @Test
    fun avanceAnillo() {
        conVariantes(avance = VarianteAvance.ANILLO)
        video(2, "paso3-b-anillo")
    }

    @Test
    fun avanceTitulo() {
        conVariantes(avance = VarianteAvance.TITULO)
        video(2, "paso3-c-titulo")
    }
}

/**
 * El fondo nuevo (el lienzo liso, sin aurora): los cuatro pasos de pie en
 * oscuro, en un instante asentado de cada uno, y el horario en claro.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class FondoBienvenidaTest {

    @get:Rule
    val regla = createComposeRule()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    @AfterTest
    fun limpiar() {
        Tema.juego = Juego.OSCURO
    }

    private fun paso(paso: Int, ms: Long, nombre: String) {
        val inicio = montar(regla, paso, carreras)
        foto(regla, inicio, ms, "build/capturas/tira/fondo-$nombre.png")
    }

    @Test fun mapa() = paso(0, 7_400L, "1-mapa")
    @Test fun horario() = paso(1, 8_000L, "2-horario")
    @Test fun avance() = paso(2, 8_400L, "3-avance")
    @Test fun carrera() = paso(3, 1_600L, "4-carrera")

    @Test
    fun horarioClaro() {
        Tema.juego = Juego.CLARO
        paso(1, 8_000L, "5-horario-claro")
    }

    @Test
    fun avanceClaro() {
        Tema.juego = Juego.CLARO
        paso(2, 8_400L, "6-avance-claro")
    }
}
