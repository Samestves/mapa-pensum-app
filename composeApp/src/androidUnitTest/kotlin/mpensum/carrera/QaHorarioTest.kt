package mpensum.carrera

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import mpensum.datos.leerPensum
import mpensum.logica.Momento
import mpensum.plataforma.HoraLocal
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El horario a cualquier hora: fin de semana, antes de abrir la jornada,
 * pasadas las siete de la tarde y justo en un borde. Con el reloj inyectado
 * se monta cada vista y se comprueba que la tarjeta de «ahora» dice algo
 * coherente; lo que se busca es que ninguna combinación reviente.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class QaHorarioTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun preparar() {
        mpensum.plataforma.Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun h(x: Int) = x * 60

    private fun horario(dia: Int, minuto: Int, conClases: Boolean = true): EstadoHorario {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val carrera = EstadoCarrera(pensum, demostracion = emptyMap())
        return EstadoHorario(carrera) { HoraLocal(dia, minuto, 30) }.apply {
            if (conClases) {
                guardar(Borrador(null, "0021111", 0, h(6), h(8)))
                guardar(Borrador(null, "0081814", 4, h(17), h(19)))
                guardar(Borrador(null, "0101214", 2, h(10), h(12)))
                guardar(Borrador(null, "0071823", 2, h(11), h(13)))
            }
        }
    }

    private var actual by mutableStateOf<EstadoHorario?>(null)
    private var montado = false

    /** Se monta una vez y se cambia de horario por debajo, como con el reloj del teléfono */
    private fun montar(e: EstadoHorario) {
        actual = e
        if (!montado) {
            montado = true
            regla.mainClock.autoAdvance = false
            regla.setContent {
                val estado = actual
                if (estado != null) key(estado) {
                    Box(Modifier.fillMaxSize()) {
                        VistaHorario(estado, abajo = 80.dp)
                        HojasHorario(estado, HazeState())
                    }
                }
            }
        }
        regla.mainClock.advanceTimeBy(1500)
    }

    @Test
    fun cadaMomentoMontaEnDiaYEnSemana() {
        // Sábado de noche, domingo de madrugada, lunes antes de la primera clase, a la hora justa de empezar, de acabar y viernes tras la última
        val momentos = listOf(5 to h(22), 6 to h(3), 0 to h(5) + 59, 0 to h(6), 0 to h(8), 4 to h(19), 4 to h(19) + 1, 4 to h(23) + 59, 2 to h(11) + 30, 6 to 0)
        for ((dia, minuto) in momentos) {
            for (modo in ModoHorario.entries) {
                val e = horario(dia, minuto).apply { this.modo = modo }
                montar(e)
                // Y un día cualquiera elegido, con la hoja de nueva clase en un hueco
                e.abrirNueva(e.dia, h(8))
                regla.mainClock.advanceTimeBy(800)
                e.hoja = null
                regla.mainClock.advanceTimeBy(800)
            }
        }
    }

    @Test
    fun elHorarioVacioAUnaHoraRara() {
        montar(horario(6, h(23), conClases = false))
        regla.mainClock.advanceTimeBy(500)
    }

    @Test
    fun elMomentoDeFinDeSemanaMiraAlLunes() {
        val sabado = horario(5, h(22))
        val m = sabado.momento
        assertTrue(m is Momento.Libre && !m.terminado, "el sábado no tiene clases: $m")
        assertEquals(2, (m as Momento.Libre).proxima?.dentroDe)
        val domingo = horario(6, h(3))
        assertEquals(1, (domingo.momento as Momento.Libre).proxima?.dentroDe)
        // Pasado el cierre del viernes, lo que sigue es el lunes y hoy está terminado
        val viernes = horario(4, h(19) + 1).momento
        assertTrue(viernes is Momento.Libre && viernes.terminado)
        assertEquals(3, (viernes as Momento.Libre).proxima?.dentroDe)
    }

    @Test
    fun editarAlLimiteNoSaleDeLaJornada() {
        val e = horario(0, h(7))
        val s = e.sesiones.first { it.dia == 4 }
        e.editar(s)
        montar(e)
        // Lo máximo que se puede correr: la clase de las 17 a las 19 ya está en el cierre, y no se pasa de ahí
        var b = (e.hoja as HojaHorario.Editar).borrador
        repeat(10) { b = b.copy(inicio = minOf(mpensum.logica.CIERRA - (b.fin - b.inicio), b.inicio + 30)); b = b.copy(fin = b.inicio + (s.fin - s.inicio)) }
        e.guardar(b)
        assertTrue(mpensum.logica.esValida(e.sesiones.first { it.id == s.id }))
    }
}
