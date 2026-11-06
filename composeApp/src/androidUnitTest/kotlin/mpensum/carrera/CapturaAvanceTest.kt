package mpensum.carrera

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.datos.Guardado
import mpensum.datos.leerPensum
import mpensum.logica.FuenteCarga
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
import mpensum.plataforma.Apariencia
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La hoja de avance abierta sobre el mapa: con Sistemas del primero al cuarto
 * aprobados y dos en curso, con la carga movida desde el mando, sin marcas y
 * con la cadena mandando. Las imágenes van a build/capturas/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaAvanceTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
        // Con preferencias de verdad, para comprobar que la carga se guarda
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        // Un latido eterno no deja al reloj de pruebas llegar al reposo
        latidoActivo = false
    }

    /** Lo guardado no pasa a otras pruebas: el objeto Preferencias vive todo el proceso */
    @AfterTest
    fun limpiar() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun estado(slug: String = "ingenieria-de-sistemas", aprobados: Int = 4, cursando: Int = 2): EstadoCarrera {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
        return EstadoCarrera(pensum).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= aprobados }.forEach { marcar(it.codigo, Marca.APROBADA) }
            pensum.obligatorias.filter { it.semestre == aprobados + 1 }.take(cursando).forEach { marcar(it.codigo, Marca.CURSANDO) }
        }
    }

    private fun montar(estado: EstadoCarrera, avance: Long = 1600) {
        regla.mainClock.autoAdvance = false
        val camara = Camara(estado.pensum.slug + "avance")
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
        regla.mainClock.advanceTimeBy(avance)
    }

    /**
     * El scroll por semántica espera fotogramas: con el reloj parado gira sin
     * fin. Se deja correr solo mientras dura, y se vuelve a parar para la foto.
     */
    private fun desplazarA(texto: String): SemanticsNodeInteraction {
        regla.mainClock.advanceTimeBy(400)
        regla.mainClock.autoAdvance = true
        try {
            return regla.onNodeWithText(texto).performScrollTo()
        } finally {
            regla.mainClock.autoAdvance = false
        }
    }

    private fun foto(nombre: String) = regla.onRoot().captureRoboImage("build/capturas/$nombre.png")

    private fun abrirDetalle(pieza: String = "TE GRADÚAS") {
        regla.onNodeWithText(pieza).performClick()
        regla.mainClock.advanceTimeBy(1400)
    }

    /** Por semestres: un renglón por semestre con su anillo */
    @Test
    fun detallePorSemestres() {
        montar(estado())
        abrirDetalle("LLEVAS")
        foto("avance-detalle-5-por-semestres")
    }

    /** Puedes inscribir: las abiertas, primero las que más destraban */
    @Test
    fun detalleInscribir() {
        montar(estado())
        abrirDetalle("PUEDES INSCRIBIR")
        foto("avance-detalle-6-inscribir")
    }

    /** Electivas técnicas: la cuota, las casillas del plan y el selector al tocar una */
    @Test
    fun detalleElectivas() {
        montar(estado())
        abrirDetalle("TÉCNICAS")
        foto("avance-detalle-7-electivas")
        regla.onAllNodesWithText("Elegir ›")[0].performClick()
        regla.mainClock.advanceTimeBy(1400)
        foto("avance-detalle-8-selector")
    }

    @Test
    fun sistemasEnElQuinto() {
        val estado = estado()
        montar(estado)
        foto("avance-hoja-1-quinto")
        val g = estado.graduacion
        println("Hoja: carga ${g.carga} (${g.fuenteCarga}) → ${g.semestresRestantes} sem, ${g.periodoFinal}; cuello ${g.cuelloDeBotella}")
        // Dos en curso no bastan para decir 17 semestres: manda su ritmo
        assertEquals(FuenteCarga.RITMO, g.fuenteCarga)
    }

    /** A mitad de la entrada: la curva se barre y la cifra cuenta */
    @Test
    fun entrando() {
        montar(estado(), avance = 520)
        foto("avance-hoja-2-entrando")
    }

    /** El detalle con seis materias por semestre: la fecha, el anillo, la cadena, el ritmo y lo que inscribes */
    @Test
    fun detalleSeis() {
        val estado = estado()
        estado.elegirCarga(6)
        montar(estado)
        abrirDetalle()
        foto("avance-detalle-1-seis")
        assertEquals(6, estado.carga)
    }

    /** Con cuatro: la fecha se aleja y el anillo reparte más semestres */
    @Test
    fun detalleCuatro() {
        val estado = estado()
        estado.elegirCarga(4)
        montar(estado)
        abrirDetalle()
        foto("avance-detalle-2-cuatro")
        assertEquals(FuenteCarga.ELEGIDA, estado.graduacion.fuenteCarga)
    }

    /** Sin nada aprobado: HOY es el inicio y el anillo entero es plan */
    @Test
    fun detalleCero() {
        val estado = estado(aprobados = 0, cursando = 0)
        estado.elegirCarga(6)
        montar(estado)
        abrirDetalle()
        foto("avance-detalle-0-cero")
    }

    /** Semestre a semestre, con el próximo abierto y su botón al mapa */
    @Test
    fun semestres() {
        val estado = estado()
        estado.elegirCarga(6)
        montar(estado)
        abrirDetalle()
        desplazarA("Verlas en el mapa")
        regla.mainClock.advanceTimeBy(100)
        foto("avance-detalle-3-semestres")
    }

    /** La cadena de materias que no puedes atrasar */
    @Test
    fun noAtrasar() {
        val estado = estado()
        estado.elegirCarga(6)
        montar(estado)
        abrirDetalle()
        desplazarA("Verlas en el mapa")
        regla.mainClock.autoAdvance = true
        regla.onNodeWithText("Verlas en el mapa").performTouchInput { swipeUp(startY = bottom, endY = top - 1500f, durationMillis = 400) }
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(1500)
                foto("avance-detalle-4-no-atrasar")
    }

    /** El tema claro, en la hoja y en el detalle */
    @Test
    fun claro() {
        Preferencias.guardarTema(Apariencia.CLARO)
        val estado = estado()
        estado.elegirCarga(6)
        montar(estado)
        foto("avance-hoja-3-claro")
        abrirDetalle()
        foto("avance-detalle-5-claro")
    }

    /** Con mucha carga ya no se acorta: lo dice la cadena que frena */
    @Test
    fun cadenaManda() {
        val estado = estado()
        estado.elegirCarga(9)
        montar(estado)
        foto("avance-hoja-4-cadena")
        val g = estado.graduacion
        assertEquals(g.minimoPorPrelaciones, g.semestresRestantes)
    }

    @Test
    fun sinMarcas() {
        montar(estado(aprobados = 0, cursando = 0))
        foto("avance-hoja-5-sin-marcas")
    }

    /** Una carrera sin créditos oficiales ni cuotas: por materias y sin electivas */
    @Test
    fun ambiental() {
        montar(estado("ingenieria-ambiental", aprobados = 2, cursando = 5))
        foto("avance-hoja-6-ambiental")
    }

    @Test
    fun terminado() {
        val estado = estado(aprobados = 99, cursando = 0)
        // Las electivas justas para cubrir cada cuota, como lo haría un estudiante
        estado.pensum.grupos.forEach { g ->
            var uc = 0
            for (e in g.asignaturas.filter { (it.uc ?: 0) > 1 }) {
                if (uc >= (g.cuota ?: 0)) break
                estado.marcar(e.codigo, Marca.APROBADA)
                uc += e.uc ?: 0
            }
        }
        assertTrue(estado.graduacion.terminado)
        montar(estado)
        foto("avance-hoja-7-terminado")
    }

    /** El pie: la apariencia y reiniciar; elegir Claro se guarda para toda la app */
    @Test
    fun pieYApariencia() {
        montar(estado())
        desplazarA("Reiniciar mi avance")
        regla.mainClock.advanceTimeBy(100)
        foto("avance-hoja-9-pie")
        // El tema vive en una fila que se despliega y enseña el carrusel
        regla.onNodeWithText("Tema").performClick()
        regla.mainClock.advanceTimeBy(500)
        regla.onNodeWithContentDescription("Claro").performClick()
        regla.mainClock.advanceTimeBy(500)
        foto("avance-hoja-10-claro-elegido")
        assertEquals(Apariencia.CLARO, Preferencias.tema.value)
        assertEquals("claro", Preferencias.leerTexto("tema"))
    }

    /** Reiniciar te deja en cero: sin marcas, sin electivas puestas y con la carga habitual, también al reabrir */
    @Test
    fun reiniciarDejaTodoEnCero() {
        val estado = estado()
        val pensum = estado.pensum
        val casilla = pensum.asignaturas.first { it.esHueco }
        val electiva = pensum.grupos.first { it.clave == casilla.grupo }.asignaturas.first()
        estado.colocar(casilla.codigo, electiva.codigo)
        estado.marcar(electiva.codigo, Marca.APROBADA)
        estado.elegirCarga(3)
        montar(estado)
        desplazarA("Reiniciar mi avance").performClick()
        regla.mainClock.advanceTimeBy(300)
        desplazarA("Borrar todo")
        regla.mainClock.advanceTimeBy(100)
        foto("avance-hoja-11-confirmar")
        regla.onNodeWithText("Borrar todo").performClick()
        // La hoja ya dice que no queda nada: se baja hasta ahí, con el reloj andando para que se repinte
        desplazarA("Nada que reiniciar todavía")
        regla.mainClock.advanceTimeBy(1200)
        foto("avance-hoja-12-reiniciado")
        assertTrue(estado.marcas.isEmpty())
        assertTrue(estado.casillas.isEmpty())
        assertNull(estado.carga)
        assertEquals(0, estado.progreso.aprobadas)
        // Lo que lee la carrera al reabrirse
        assertTrue(Guardado.marcas(pensum).isEmpty())
        assertTrue(Guardado.casillas(pensum).isEmpty())
        assertNull(Guardado.carga(pensum.slug))
    }
}
