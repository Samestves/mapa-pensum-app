package mpensum.carrera

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import mpensum.componentes.LocalVentana
import mpensum.componentes.Ventana
import mpensum.datos.leerPensum
import mpensum.logica.Marca
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Girar el teléfono con una hoja abierta (docs/qa/auditoria-bugs-visuales.md,
 * A1, A2, A3 y A7). El giro se simula cambiando la `Ventana`, que es lo único
 * que la hoja mira para pasar de flotante a pantalla completa.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class QaGirarHojaTest {

    @get:Rule
    val regla: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val dePie = Ventana(390.dp, 844.dp)
    private val acostado = Ventana(844.dp, 390.dp)

    @BeforeTest
    fun preparar() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        latidoActivo = false
        regla.mainClock.autoAdvance = false
    }

    @AfterTest
    fun limpiar() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun correr(ms: Long) {
        var t = 0L
        while (t < ms) {
            regla.mainClock.advanceTimeBy(16)
            t += 16
        }
    }

    private fun estado(): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        return EstadoCarrera(pensum).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { marcar(it.codigo, Marca.APROBADA) }
        }
    }

    private fun cuantos(texto: String) = regla.onAllNodesWithText(texto).fetchSemanticsNodes().size

    /**
     * A1 y A7: con el selector de electiva sobre «Tu avance», girar no rehace
     * nada (lo escrito en el buscador sigue ahí), la hoja del avance no vuelve a
     * subir desde abajo y atrás cierra el selector, no el avance.
     */
    @Test
    fun girarConElSelectorAbiertoNoPierdeLoEscritoYAtrasCierraElSelector() {
        val estado = estado()
        var ventana by mutableStateOf(dePie)
        var abierta by mutableStateOf(true)
        regla.setContent {
            CompositionLocalProvider(LocalVentana provides ventana) {
                Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                    HojaAvance(abierta, estado, alCerrar = { abierta = false }, alVerEnMapa = {})
                }
            }
        }
        correr(1600)
        regla.onNodeWithText("Técnicas").performClick()
        correr(1400)
        regla.onAllNodesWithText("Elegir")[0].performClick()
        correr(1400)
        regla.onNodeWithContentDescription("Buscar por nombre o código").performTextInput("redes")
        correr(300)

        ventana = acostado
        Snapshot.sendApplyNotifications()
        correr(48)
        // La hoja del avance ya está en su sitio: no vuelve a entrar desde fuera de la pantalla
        val titulo = regla.onNodeWithText("Tu avance").fetchSemanticsNode().boundsInRoot
        val altoPx = with(regla.density) { 390.dp.toPx() }
        assertTrue(titulo.top < altoPx / 2, "la hoja del avance volvió a subir al girar (título en ${titulo.top} de $altoPx)")
        correr(600)
        assertTrue(
            regla.onAllNodesWithContentDescription("Buscar por nombre o código").fetchSemanticsNodes().any { n -> hasText("redes").matches(n) },
            "lo escrito en el buscador se perdió al girar",
        )

        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        Snapshot.sendApplyNotifications()
        correr(900)
        assertTrue(abierta, "atrás cerró el avance en vez del selector")
        assertEquals(0, regla.onAllNodesWithContentDescription("Buscar por nombre o código").fetchSemanticsNodes().size, "el selector siguió abierto")
        assertEquals(1, cuantos("Tu avance"))
    }

    /**
     * A2: la ficha se cierra en el mismo cuadro en que se gira (en el mapa
     * acostado pasa a burbuja). La hoja que sale no se pinta a pantalla
     * completa sobre el mapa mientras baja: desaparece en el acto.
     */
    @Test
    fun girarMientrasSaleLaRecogeSinTaparLaPantalla() {
        var ventana by mutableStateOf(dePie)
        var abierta by mutableStateOf(true)
        regla.setContent {
            CompositionLocalProvider(LocalVentana provides ventana) {
                Box(Modifier.fillMaxSize()) {
                    HojaInferior(abierta = abierta, alCerrar = { abierta = false }, modal = false, cabecera = { BasicText("Cabecera") }) {
                        BasicText("Contenido")
                    }
                }
            }
        }
        correr(800)
        assertEquals(1, regla.onAllNodesWithTag("hoja-cabecera").fetchSemanticsNodes().size)

        abierta = false
        ventana = acostado
        Snapshot.sendApplyNotifications()
        correr(32)
        assertEquals(0, regla.onAllNodesWithTag("hoja-cabecera").fetchSemanticsNodes().size, "la hoja que sale tapó la pantalla al girar")

        // Y se vuelve a abrir como siempre, desde abajo y con su alto nuevo
        abierta = true
        Snapshot.sendApplyNotifications()
        correr(800)
        val cabecera = regla.onAllNodesWithTag("hoja-cabecera").fetchSemanticsNodes().single().boundsInRoot
        assertTrue(cabecera.top < with(regla.density) { 100.dp.toPx() }, "la hoja no volvió a entrar tras recogerla (cabecera en ${cabecera.top})")
    }

    /**
     * A3: abrir una materia tras cerrar otra no enseña la anterior ni un
     * cuadro mientras la hoja sube.
     */
    @Test
    fun laFichaSubeYaConLaMateriaNueva() {
        val estado = estado()
        val quinto = estado.pensum.obligatorias.filter { it.semestre == 5 }
        val (a, b) = quinto[0] to quinto[1]
        var codigo by mutableStateOf<String?>(a.codigo)
        regla.setContent {
            CompositionLocalProvider(LocalVentana provides dePie) {
                Box(Modifier.fillMaxSize()) {
                    FichaMateria(codigo, estado, alCerrar = { codigo = null }, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {}, alEncuadrar = { _, _ -> })
                }
            }
        }
        correr(1200)
        assertTrue(cuantos(a.nombre) > 0, "no se abrió la primera materia")
        codigo = null
        Snapshot.sendApplyNotifications()
        correr(1200)

        codigo = b.codigo
        Snapshot.sendApplyNotifications()
        var t = 0L
        while (t < 600) {
            regla.mainClock.advanceTimeByFrame()
            assertEquals(0, cuantos(a.nombre), "la ficha subió enseñando la materia anterior (a los $t ms)")
            t += 16
        }
        assertTrue(cuantos(b.nombre) > 0, "no se abrió la materia nueva")
    }
}
