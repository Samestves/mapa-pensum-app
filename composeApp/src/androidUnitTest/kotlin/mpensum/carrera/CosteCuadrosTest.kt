package mpensum.carrera

import mpensum.setContentSinLatido
import mpensum.plataforma.iniciar
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.rules.ActivityScenarioRule
import mpensum.App
import mpensum.datos.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
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

/*
 * Lo que cuesta cada cuadro en el hilo principal en los tres recorridos que
 * daban picos en el teléfono (docs/qa/rendimiento-60fps.md): entrar a la
 * carrera desde la portada, abrir «Tu avance» e ir a «Tu próximo semestre» y
 * volver. Cada cuadro avanza el reloj 16 ms y mide la pantalla, y un
 * CompositionTracer cuenta cuántas funciones se componen y cuánto tardan.
 *
 * En la JVM los milisegundos no son los del teléfono: lo que vale es la forma
 * (cuántos cuadros caros, cuántas funciones en el peor) y el antes y después.
 * Cada recorrido se repite para calentar la JVM y se cuenta el último. No
 * afirma tiempos: deja el informe en build/capturas/coste-cuadros.txt.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CosteCuadrosTest {

    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val slug = "ingenieria-de-sistemas"
    private val trazador = Trazador()
    private val informe = StringBuilder()

    @OptIn(InternalComposeTracingApi::class)
    @BeforeTest
    fun preparar() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        Preferencias.marcarBienvenidaVista()
        Preferencias.guardarCarrera(slug)
        // Se empieza en la portada: entrar a la carrera es uno de los recorridos
        Preferencias.guardarAbrirEnCarrera(false)
        val p = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
        // Sistemas en el quinto: del primero al cuarto aprobados y dos en curso
        Guardado.guardarMarcas(
            slug,
            p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
                p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO },
        )
        regla.mainClock.autoAdvance = false
        Composer.setTracer(trazador)
    }

    @AfterTest
    fun limpiar() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        File("build/capturas").mkdirs()
        File("build/capturas/coste-cuadros.txt").writeText(informe.toString())
        println(informe)
    }

    /**
     * Un cuadro: el reloj avanza 16 ms (recomposición y animaciones) y después
     * se mide y se coloca. No se dibuja: en Robolectric el dibujo es por
     * software y su coste no se parece al del teléfono.
     */
    private fun cuadro(): Cuadro {
        trazador.empezar()
        val t0 = System.nanoTime()
        regla.mainClock.advanceTimeByFrame()
        val t1 = System.nanoTime()
        regla.waitForIdle()
        val t2 = System.nanoTime()
        Thread.sleep(2)
        return trazador.terminar((t2 - t0) / 1e6, (t2 - t1) / 1e6)
    }

    private fun cuadros(ms: Long): List<Cuadro> = (0 until ms / 16).map { cuadro() }

    private fun esperar(ms: Long) = repeat((ms / 16).toInt()) {
        regla.mainClock.advanceTimeByFrame()
        Thread.sleep(2)
    }

    /** El toque como acción de semántica: no mueve el reloj, así el cuadro del cambio cae dentro de lo medido */
    private fun SemanticsNodeInteraction.tocar() {
        performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun hay(descripcion: String) = regla.onAllNodesWithContentDescription(descripcion, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun registrar(nombre: String, cuadros: List<Cuadro>) {
        val tiempos = cuadros.map { it.ms }.sorted()
        fun p(q: Double) = tiempos[((tiempos.size - 1) * q).toInt()]
        informe.appendLine("== $nombre: ${cuadros.size} cuadros · p50 %.1f · p90 %.1f · máx %.1f ms".format(p(0.5), p(0.9), tiempos.last()))
        informe.appendLine("   por cuadro (ms/funciones): " + cuadros.joinToString(" ") { "%.0f/%d".format(it.ms, it.llamadas) })
        cuadros.withIndex().sortedByDescending { it.value.ms }.take(4).forEach { (i, c) ->
            informe.appendLine("   cuadro %2d: %.1f ms (componer %.1f, medir %.1f), %d funciones · %s".format(i, c.ms, c.componerMs, c.medirMs, c.llamadas, c.mayores))
        }
    }

    /** Desde la portada: entra a la carrera, abre «Tu avance», va a «Tu próximo semestre», vuelve y sale */
    private fun recorrido(medir: Boolean) {
        // Atrás hasta la portada: la hoja, la carrera
        var n = 0
        while (regla.onAllNodesWithText("Seguir donde ibas").fetchSemanticsNodes().isEmpty() && n++ < 40) {
            if (n % 10 == 0) regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
            esperar(100)
        }
        // La carrera que sale tiene que irse del todo: si no, entrar la recupera a medio salir y no compone nada
        esperar(1500)
        regla.onNodeWithText("Seguir donde ibas").tocar()
        val entrar = cuadros(700)
        var t = 0
        while (!hay("Tu avance") && t++ < 200) esperar(32)
        esperar(1500)
        regla.onNodeWithContentDescription("Tu avance", substring = true).tocar()
        val abrir = cuadros(700)
        esperar(1200)
        regla.onNodeWithText("Tu próximo semestre").tocar()
        val ida = cuadros(500)
        esperar(800)
        regla.onNodeWithContentDescription("Volver").tocar()
        val vuelta = cuadros(500)
        esperar(400)
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        esperar(800)
        if (!medir) return
        registrar("Entrar a la carrera", entrar)
        registrar("Abrir «Tu avance»", abrir)
        registrar("Ir a «Tu próximo semestre»", ida)
        registrar("Volver al índice", vuelta)
    }

    @Test
    fun cuadrosDeLosTresRecorridos() {
        regla.setContentSinLatido { App() }
        esperar(3000)
        repeat(2) { recorrido(medir = false) }
        recorrido(medir = true)
    }
}

/** Lo medido en un cuadro: el total, lo que fue componer, cuántas funciones y las que más tardaron */
private class Cuadro(val ms: Double, val componerMs: Double, val medirMs: Double, val llamadas: Int, val mayores: String)

/**
 * Cuenta las funciones que se componen (las que no se saltan) y su tiempo
 * inclusivo, por nombre. Solo mira mientras un cuadro está abierto.
 */
@OptIn(InternalComposeTracingApi::class)
private class Trazador : CompositionTracer {
    private var activo = false
    private var llamadas = 0
    private var raiz = 0L
    private val pila = ArrayList<Long>()
    private val nombres = ArrayList<String>()
    private val porFuncion = HashMap<String, Long>()

    fun empezar() {
        activo = true
        llamadas = 0
        raiz = 0L
        pila.clear()
        nombres.clear()
        porFuncion.clear()
    }

    fun terminar(ms: Double, medirMs: Double): Cuadro {
        activo = false
        // Solo las de la app: las de la biblioteca están dentro de ellas
        val mayores = porFuncion.entries.filter { it.key.startsWith("mpensum.") && "<anonymous>" !in it.key }.sortedByDescending { it.value }.take(14)
            .joinToString(" · ") { "%s %.1f".format(it.key.removePrefix("mpensum.").substringBefore(' '), it.value / 1e6) }
        return Cuadro(ms, raiz / 1e6, medirMs, llamadas, mayores)
    }

    override fun isTraceInProgress() = activo

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        llamadas++
        pila.add(System.nanoTime())
        nombres.add(info)
    }

    override fun traceEventEnd() {
        if (pila.isEmpty()) return
        val d = System.nanoTime() - pila.removeAt(pila.lastIndex)
        val nombre = nombres.removeAt(nombres.lastIndex)
        if (pila.isEmpty()) raiz += d
        // Una función que se llama a sí misma (o dos veces anidada) no se cuenta doble
        if (nombre !in nombres) porFuncion[nombre] = (porFuncion[nombre] ?: 0L) + d
    }
}
