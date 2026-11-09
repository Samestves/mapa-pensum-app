package mpensum.portada

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.datos.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
import mpensum.tema.Juego
import mpensum.tema.Tema
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Las barras del sistema que se simulan, en dp: la de estado arriba, la de
 * navegación por sus lados (gestos abajo; tres botones abajo de pie y a un
 * lado acostado) y el corte de la cámara a la izquierda.
 */
data class Barras(val estado: Int, val navAbajo: Int = 0, val navIzquierda: Int = 0, val navDerecha: Int = 0, val camara: Int = 0)

/**
 * El home en los estados que debe resolver, con las barras del sistema
 * puestas de verdad (Robolectric no trae ninguna): así se ve lo que queda
 * debajo de la barra de estado, la cámara o la barra de gestos. Encima se
 * pintan esas zonas en rojo translúcido. Tu carrera es Sistemas con cuatro
 * semestres aprobados y dos materias en curso. Las imágenes van a
 * build/capturas/home-{tam}-{estado}.png; solo falla si algo revienta.
 */
abstract class BaseEstadosPortada(private val tam: String, private val barras: Barras) {
    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val carreras = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())
    private val sistemas get() = carreras.first { it.slug == "ingenieria-de-sistemas" }

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        // El Tema es global: lo que dejara otra prueba (o la detección de gama baja) no cuela en la captura
        Tema.ligero = false
        Tema.juego = Juego.OSCURO
        regla.mainClock.autoAdvance = false
    }

    @AfterTest
    fun volver() {
        Tema.juego = Juego.OSCURO
    }

    private fun pensum(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    /** Aprueba los semestres hasta `hasta` y deja `cursando` materias del siguiente en curso */
    private fun marcar(slug: String, hasta: Int, cursando: Int) {
        val p = pensum(slug)
        Guardado.guardarMarcas(
            slug,
            p.obligatorias.filter { (it.semestre ?: 99) <= hasta }.associate { it.codigo to Marca.APROBADA } +
                p.obligatorias.filter { it.semestre == hasta + 1 }.take(cursando).associate { it.codigo to Marca.CURSANDO },
        )
    }

    /** Todo aprobado, electivas incluidas: la carrera terminada */
    private fun terminar(slug: String) {
        Guardado.guardarMarcas(slug, pensum(slug).todas.associate { it.codigo to Marca.APROBADA })
    }

    private fun correr(ms: Long) {
        var t = 0L
        // A pasos cortos: el avance se calcula aparte y tiene ocasión de llegar
        while (t < ms) {
            regla.mainClock.advanceTimeBy(50)
            t += 50
            Thread.sleep(2)
            regla.waitForIdle()
        }
    }

    /** Entrega las barras a la vista de Compose, como haría el sistema con la app de borde a borde */
    private fun ponerBarras() {
        regla.runOnUiThread {
            val raiz = regla.activity.findViewById<View>(android.R.id.content)
            val d = raiz.resources.displayMetrics.density
            fun px(dp: Int) = (dp * d).roundToInt()
            val b = barras
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, px(b.estado), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(px(b.navIzquierda), 0, px(b.navDerecha), px(b.navAbajo)))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(px(b.camara), 0, 0, 0))
                .build()
            ViewCompat.dispatchApplyWindowInsets(raiz, insets)
        }
    }

    private fun montar(escalaLetra: Float = 1f, contenido: @Composable () -> Unit) {
        regla.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * escalaLetra)) {
                Box(Modifier.fillMaxSize()) {
                    contenido()
                    ZonasDelSistema()
                }
            }
        }
        // Compose escucha las barras tras su primer cuadro: se entregan entonces y otra vez antes de cada foto
        correr(100)
        ponerBarras()
        correr(3000)
    }

    private fun foto(estado: String) {
        ponerBarras()
        correr(600)
        regla.onRoot().captureRoboImage("build/capturas/home-$tam-$estado.png")
    }

    private fun portada(propia: Carrera?) = montar { Portada(carreras, propia, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }

    @Test
    fun conAvanceOscuro() {
        marcar(sistemas.slug, 4, 2)
        portada(sistemas)
        foto("con-avance-oscuro")
    }

    @Test
    fun conAvanceClaro() {
        Tema.juego = Juego.CLARO
        marcar(sistemas.slug, 4, 2)
        portada(sistemas)
        foto("con-avance-claro")
    }

    @Test
    fun sinCarreraOscuro() {
        portada(null)
        foto("sin-carrera-oscuro")
    }

    @Test
    fun sinCarreraClaro() {
        Tema.juego = Juego.CLARO
        portada(null)
        foto("sin-carrera-claro")
    }

    /** Tu carrera elegida y nada marcado: el primer día */
    @Test
    fun sinMarcas() {
        portada(sistemas)
        foto("sin-marcas-oscuro")
    }

    /** Todo aprobado: ¿qué te dice el home cuando ya no queda nada? */
    @Test
    fun terminada() {
        terminar(sistemas.slug)
        portada(sistemas)
        foto("terminada-oscuro")
    }

    /** La letra del sistema a 1,3 (y la de «Texto grande», 1,15, cabe dentro) */
    @Test
    fun letraGrande() {
        marcar(sistemas.slug, 4, 2)
        montar(1.3f) { Portada(carreras, sistemas, alAbrir = { _, _, _ -> }, alVerGuia = {}, alElegirPropia = {}) }
        foto("letra13-oscuro")
    }

    /** Bajada hasta el final: la cabecera de vidrio encima y lo último sobre la barra de abajo */
    @Test
    fun alFinal() {
        marcar(sistemas.slug, 4, 2)
        portada(sistemas)
        repeat(3) {
            regla.onRoot().performTouchInput { swipeUp(startY = bottom - 40f, endY = top + 40f) }
            correr(500)
        }
        foto("al-final-oscuro")
    }
}

/** Las zonas del sistema en rojo translúcido, leídas de Compose: si no salen, las barras no llegaron */
@Composable
private fun ZonasDelSistema() {
    val d = LocalDensity.current
    val dir = LocalLayoutDirection.current
    val arriba = WindowInsets.statusBars.getTop(d)
    val nav = WindowInsets.navigationBars
    val camara = WindowInsets.displayCutout.getLeft(d, dir)
    val abajo = nav.getBottom(d)
    val izquierda = nav.getLeft(d, dir)
    val derecha = nav.getRight(d, dir)
    val rojo = Color(0xFFFF3B30)
    Canvas(Modifier.fillMaxSize()) {
        val velo = rojo.copy(alpha = 0.22f)
        val filo = rojo.copy(alpha = 0.7f)
        fun zona(o: Offset, s: Size) {
            if (s.width <= 0f || s.height <= 0f) return
            drawRect(velo, o, s)
        }
        zona(Offset.Zero, Size(size.width, arriba.toFloat()))
        zona(Offset(0f, size.height - abajo), Size(size.width, abajo.toFloat()))
        zona(Offset.Zero, Size(maxOf(camara, izquierda).toFloat(), size.height))
        zona(Offset(size.width - derecha, 0f), Size(derecha.toFloat(), size.height))
        // El filo por dentro de cada barra: donde empieza lo que es de la app
        if (arriba > 0) drawLine(filo, Offset(0f, arriba.toFloat()), Offset(size.width, arriba.toFloat()), 1.5f)
        if (abajo > 0) drawLine(filo, Offset(0f, size.height - abajo), Offset(size.width, size.height - abajo), 1.5f)
        val izq = maxOf(camara, izquierda)
        if (izq > 0) drawLine(filo, Offset(izq.toFloat(), 0f), Offset(izq.toFloat(), size.height), 1.5f)
        if (derecha > 0) drawLine(filo, Offset(size.width - derecha, 0f), Offset(size.width - derecha, size.height), 1.5f)
    }
}

/** De pie en un teléfono pequeño y modesto: barra de estado de 24 y la de tres botones de 48 abajo */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class CapturaPortadaEstados360Test : BaseEstadosPortada("360x640", Barras(estado = 24, navAbajo = 48))

/** Acostado (915 × 412): barra de estado arriba, la cámara a la izquierda y la barra de gestos abajo */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaPortadaEstados915Test : BaseEstadosPortada("915x412", Barras(estado = 24, navAbajo = 20, camara = 32))

/** Acostado con la barra de tres botones, que en horizontal se va a un lado (a la derecha) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaPortadaEstados915BotonesTest : BaseEstadosPortada("915x412-botones", Barras(estado = 24, navDerecha = 48, camara = 32))
