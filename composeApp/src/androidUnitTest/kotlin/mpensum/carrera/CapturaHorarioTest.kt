package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import mpensum.plataforma.LectorCaptura
import mpensum.logica.MateriaLeida
import mpensum.logica.Sesion
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.datos.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.plataforma.HoraLocal
import mpensum.tema.Paleta
import mpensum.tema.colorDe
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
 * El horario en un teléfono simulado, con la semana de ejemplo de Sistemas
 * del tablero aprobado y el reloj parado el lunes a las 10:40. Guarda las
 * imágenes en build/capturas/ para mirarlas contra el tablero.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CapturaHorarioTest {

    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
        // Cada prueba con su propio guardado: si no, las clases de la anterior se suman a las de esta
        mpensum.plataforma.Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun h(x: Int) = x * 60

    private var ejemploLeido: List<MateriaLeida> = emptyList()

    /** Una captura gris de 300 × 262, como la que se subiría */
    private fun capturaGris(): ImageBitmap {
        val mapa = ImageBitmap(300, 262)
        Canvas(mapa).drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, 300f, 262f), Paint().apply { color = Color(0xFF16181B) })
        return mapa
    }

    /** Un lector de mentira: entrega las materias de una en una y, si `entero` es falso, se queda esperando tras la tercera */
    private fun lector(entero: Boolean) = object : LectorCaptura {
        override suspend fun elegir() = capturaGris()
        override suspend fun leer(foto: ImageBitmap, alTotal: (Int) -> Unit, alLeer: (MateriaLeida) -> Unit) {
            alTotal(ejemploLeido.size)
            for ((i, m) in ejemploLeido.withIndex()) {
                if (!entero && i == 3) awaitCancellation()
                delay(250)
                alLeer(m)
            }
        }
    }

    /** Sistemas en el quinto: lo de antes aprobado y las seis materias del ejemplo cursándose */
    private fun horario(minuto: Int = 640, dia: Int = 0, vacio: Boolean = false): EstadoHorario {
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
        val carrera = EstadoCarrera(pensum).apply {
            marcarVarias(pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA })
            marcarVarias(ejemplo.associate { it.first.first to Marca.CURSANDO })
        }
        // El lunes 2 de noviembre de 2026 es el día 20759 desde 1970: de ahí salen los números de la semana
        return EstadoHorario(carrera) { HoraLocal(dia, minuto, 0, 20759L + dia) }.apply {
            if (!vacio) ejemplo.forEach { (c, aula) ->
                guardar(Borrador(null, c.first, c.second, c.third.first, c.third.second, aula = aula, seccion = "01"))
            }
            this.dia = dia
        }
    }

    private fun capturar(nombre: String, horario: EstadoHorario, ms: Long = 1600, tambien: Map<Long, String> = emptyMap()) {
        regla.mainClock.autoAdvance = false
        val cristal = HazeState()
        val color = colorDe(horario.carrera.pensum.color.oscuro)
        regla.setContent {
            // El fondo del interior: el lienzo con la luz de la carrera arriba
            Box(
                Modifier.fillMaxSize().background(Paleta.lienzo).drawBehind {
                    val r = size.width * 0.9f
                    val c = Offset(size.width * 0.15f, -size.width * 0.1f)
                    drawCircle(Brush.radialGradient(0f to color.copy(alpha = 0.14f), 0.7f to Color.Transparent, center = c, radius = r), r, c)
                },
            ) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    VistaHorario(horario, abajo = 14.dp + ALTO_BARRA, horizontal = false)
                }
                BarraCarrera(Vista.HORARIO, horario.carrera.progreso, cristal, alInicio = {}, alCambiar = {}, alAvance = {}, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), vertical = false)
                HojasHorario(horario, cristal)
            }
        }
        var t = 0L
        for ((cuando, sufijo) in tambien.toSortedMap()) {
            regla.mainClock.advanceTimeBy(cuando - t)
            t = cuando
            regla.onRoot().captureRoboImage("build/capturas/$nombre-$sufijo.png")
        }
        regla.mainClock.advanceTimeBy(ms - t)
        regla.onRoot().captureRoboImage("build/capturas/$nombre.png")
    }

    @Test
    fun dia() = capturar("horario-dia", horario())

    @Test
    fun diaAcabado() = capturar("horario-dia-hoy", horario(minuto = h(13)))

    @Test
    fun semana() = capturar("horario-semana", horario().apply { modo = ModoHorario.SEMANA })

    /** Dos clases que se pisan el miércoles: se reparten la columna sin tocarse */
    @Test
    fun semanaConChoque() = capturar(
        "horario-semana-choque",
        horario().apply {
            guardar(Borrador(null, "0713463", 2, h(11), h(13), aula = "Aula 8"))
            dia = 0
            modo = ModoHorario.SEMANA
        },
    )

    /** La pantalla inicial: un cuadro leyendo la captura, uno a mitad del vuelo y el final con «ahora» trazada */
    @Test
    fun vacio() = capturar("horario-vacio", horario(vacio = true), ms = 3800, tambien = mapOf(700L to "leyendo", 1900L to "vuelo", 2400L to "vuelo2"))

    /** Lectura 1: «¿Es esta?» con la foto en grande */
    @Test
    fun leerElegir() = capturar("horario-leer-1-elegir", horario(vacio = true).apply {
        lector = lector(entero = true)
        lectura = Lectura().apply { foto = capturaGris() }
    })

    /** Lectura 2: la línea recorre la foto y la lista va por la mitad */
    @Test
    fun leerLeyendo() = capturar("horario-leer-2-leyendo", horario(vacio = true).apply {
        lector = lector(entero = false)
        lectura = Lectura().apply { foto = capturaGris(); fase = FaseLectura.LEYENDO }
    }, ms = 1600)

    /** Lectura 3 y 4: las clases vuelan a la semana y llega la hoja «Antes de guardar», con el choque en su fila */
    @Test
    fun leerVuelaYConfirma() = capturar("horario-leer-4-confirmar", horario(vacio = true).apply {
        lector = lector(entero = true)
        lectura = Lectura().apply { foto = capturaGris(); fase = FaseLectura.VOLANDO; leidas += ejemploLeido }
    }, ms = 5200, tambien = mapOf(1200L to "3-vuelo", 2300L to "3-flor"))

    @Test
    fun detalle() = capturar("horario-detalle", horario().apply { hoja = HojaHorario.Detalle(sesiones.first { it.codigo == "0713172" }.id) })

    @Test
    fun nueva() = capturar("horario-nueva", horario().apply { abrirNueva(1, h(11)) })

    @Test
    fun editarConChoque() = capturar(
        "horario-editar",
        horario().apply {
            val s = sesiones.first { it.codigo == "0723913" && it.dia == 2 }
            editar(s)
            hoja = HojaHorario.Editar((hoja as HojaHorario.Editar).borrador.copy(dia = 0))
        },
    )
}
