package mpensum

import mpensum.plataforma.Preferencias
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import mpensum.carrera.Borrador
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.EstadoHorario
import mpensum.carrera.FaseLectura
import mpensum.carrera.HojaHorario
import mpensum.carrera.HojasHorario
import mpensum.carrera.Lectura
import mpensum.carrera.ModoHorario
import mpensum.carrera.VistaHorario
import mpensum.datos.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.LectorCaptura
import mpensum.logica.Marca
import mpensum.logica.MateriaLeida
import mpensum.logica.Sesion
import mpensum.plataforma.HoraLocal
import mpensum.tema.Paleta
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
 * Las pantallas del horario en un teléfono acostado (915 × 412 dp), con el
 * riel de 84 dp a la izquierda ya aplicado como padding, igual que en el
 * interior: el inicio animado, la semana, Leyendo y «Antes de guardar». Las
 * imágenes van a build/capturas/horizontal-horario-*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-xxhdpi")
class CapturaHorizontalHorarioTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
        // Cada prueba con su propio guardado: si no, las clases de la anterior se suman a las de esta
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun h(x: Int) = x * 60

    private var ejemploLeido: List<MateriaLeida> = emptyList()

    /** Una captura gris de 300 × 262, como la que se subiría */
    private fun capturaGris(): ImageBitmap {
        val mapa = ImageBitmap(300, 262)
        Canvas(mapa).drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, 300f, 262f), Paint().apply { color = Color(0xFF16181B) })
        return mapa
    }

    /** Un lector de mentira: entrega las materias de una en una y se queda esperando tras la tercera */
    private fun lector() = object : LectorCaptura {
        override suspend fun elegir() = capturaGris()
        override suspend fun leer(foto: ImageBitmap, alTotal: (Int) -> Unit, alLeer: (MateriaLeida) -> Unit) {
            alTotal(ejemploLeido.size)
            for ((i, m) in ejemploLeido.withIndex()) {
                if (i == 3) awaitCancellation()
                delay(250)
                alLeer(m)
            }
        }
    }

    /** Sistemas en el quinto con la semana de ejemplo, el lunes a las 10:40; `vacio` la deja sin clases */
    private fun horario(vacio: Boolean = false): EstadoHorario {
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val ejemplo = listOf(
            Triple("0624622", 0, h(7) to h(9)) to "Aula 12", Triple("0624622", 2, h(7) to h(9)) to "Aula 12",
            Triple("0713122", 1, h(9) to h(11)) to "Lab 3", Triple("0713122", 3, h(9) to h(11)) to "Lab 3",
            Triple("0713172", 0, h(10) to h(12)) to "Aula 5",
            Triple("0713463", 1, h(13) to h(15)) to "Aula 8", Triple("0713463", 3, h(13) to h(15)) to "Aula 8",
            Triple("0723913", 2, h(10) to h(12)) to "Aula 2", Triple("0723913", 4, h(8) to h(10)) to "Aula 2",
            // Teoría de Sistemas entra una hora sobre Métodos: el choque del tablero
            Triple("0713632", 4, h(9) to h(11)) to "Aula 9",
        )
        ejemploLeido = ejemplo.groupBy({ it.first.first }, { it.first.second to it.second }).entries.mapIndexed { n, (codigo, dias) ->
            MateriaLeida(codigo, dias.mapIndexed { k, (d, aula) ->
                val t = ejemplo.first { it.first.first == codigo && it.first.second == d }.first.third
                Sesion("leida-$n-$k", codigo, d, t.first, t.second, "01", aula)
            })
        }
        HorarioGuardado.guardar(pensum.slug, emptyList())
        val carrera = EstadoCarrera(pensum, compacto = true).apply {
            marcarVarias(pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA })
            marcarVarias(ejemplo.associate { it.first.first to Marca.CURSANDO })
        }
        // El lunes 2 de noviembre de 2026 es el día 20759 desde 1970
        return EstadoHorario(carrera) { HoraLocal(0, 640, 0, 20759L) }.apply {
            if (!vacio) ejemplo.forEach { (c, aula) ->
                guardar(Borrador(null, c.first, c.second, c.third.first, c.third.second, aula = aula, seccion = "01"))
            }
            dia = 0
        }
    }

    private fun capturar(nombre: String, horario: EstadoHorario, ms: Long = 1600, tambien: Map<Long, String> = emptyMap()) {
        regla.mainClock.autoAdvance = false
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaHorario(horario, abajo = 24.dp, modifier = Modifier.padding(start = RIEL), horizontal = true)
                }
                HojasHorario(horario, cristal)
            }
        }
        var t = 0L
        for ((cuando, sufijo) in tambien.toSortedMap()) {
            regla.mainClock.advanceTimeBy(cuando - t)
            t = cuando
            regla.onRoot().captureRoboImage("build/capturas/horizontal-horario-$nombre-$sufijo.png")
        }
        regla.mainClock.advanceTimeBy(ms - t)
        regla.onRoot().captureRoboImage("build/capturas/horizontal-horario-$nombre.png")
    }

    /** La pantalla inicial: leyendo la captura, a mitad del vuelo y el final con «ahora» trazada */
    @Test
    fun inicio() = capturar("inicio", horario(vacio = true), ms = 3800, tambien = mapOf(700L to "leyendo", 1900L to "vuelo"))

    @Test
    fun semana() = capturar("semana", horario().apply { modo = ModoHorario.SEMANA })

    /** Leer 1: «¿Es esta?» con la foto a la izquierda */
    @Test
    fun leerElegir() = capturar("leer-1-elegir", horario(vacio = true).apply {
        lector = lector()
        lectura = Lectura().apply { foto = capturaGris() }
    })

    /** Leer 2: la línea recorre la foto y la lista va por la mitad */
    @Test
    fun leerLeyendo() = capturar("leyendo", horario(vacio = true).apply {
        lector = lector()
        lectura = Lectura().apply { foto = capturaGris(); fase = FaseLectura.LEYENDO }
    })

    /** Leer 3 y 4: las clases vuelan a la semana y llega la hoja «Antes de guardar» */
    @Test
    fun leerConfirmar() = capturar("confirmar", horario(vacio = true).apply {
        lector = lector()
        lectura = Lectura().apply { foto = capturaGris(); fase = FaseLectura.VOLANDO; leidas += ejemploLeido }
    }, ms = 5200, tambien = mapOf(1200L to "vuelo"))

    private companion object {
        /** El riel de la izquierda del interior en horizontal */
        val RIEL = 84.dp
    }
}
