package mpensum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import mpensum.carrera.BurbujaFicha
import mpensum.carrera.EstadoCarrera
import mpensum.carrera.ZonaSegura
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.carrera.MapaCarrera
import mpensum.mapa.Nodo
import mpensum.mapa.Encuadre
import mpensum.tema.Paleta
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * La burbuja de la ficha acostada (915 x 412 dp) sobre el mapa de Sistemas con
 * los semestres 1 a 4 aprobados: con la tarjeta en el centro y cerca del borde
 * derecho. Las imágenes van a build/capturas/burbuja-{sitio}.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w915dp-h412dp-land-xxhdpi")
class CapturaBurbujaFichaTest {
    @get:Rule
    val regla = createComposeRule()

    @BeforeTest
    fun grabar() {
        System.setProperty("roborazzi.test.record", "true")
    }

    private fun captura(nombre: String, codigo: String, xPantalla: Float) {
        regla.mainClock.autoAdvance = false
        val pensum = leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
        val estado = EstadoCarrera(pensum, compacto = true).apply {
            pensum.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { marcar(it.codigo, Marca.APROBADA) }
            marcar("0624622", Marca.CURSANDO)
        }
        val camara = Camara("burbuja-$nombre")
        val efectos = EfectosMapa()
        var elegida by mutableStateOf<String?>(null)
        regla.setContentSincrono {
            BoxWithConstraints(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                MapaCarrera(estado, camara, efectos, elegida, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                BurbujaFicha(
                    codigo = elegida, estado = estado, camara = camara,
                    zona = ZonaSegura(izq = 100f, arriba = 12f, der = maxWidth.value - 16f, abajo = maxHeight.value - 20f),
                    ancho = maxWidth.value, alto = maxHeight.value,
                    alCerrar = {}, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {},
                )
            }
        }
        fun correr(ms: Int) = repeat(ms / 50) {
            regla.mainClock.advanceTimeBy(50)
            regla.waitForIdle()
        }
        correr(1000)
        // La tarjeta se pone donde se pide (centro vertical, `xPantalla` de ancho) y entonces se abre la burbuja
        val n = estado.nodoDe(codigo)!!
        val e = camara.escala
        regla.runOnIdle {
            camara.irA(Encuadre(xPantalla - (n.x.toFloat() + Nodo.ANCHO.toFloat() / 2) * e, 206f - (n.y.toFloat() + Nodo.ALTO.toFloat() / 2) * e, e))
            elegida = codigo
        }
        correr(1500)
        regla.onRoot().captureRoboImage("build/capturas/burbuja-$nombre.png")
    }

    @Test
    fun centro() = captura("centro", "0713463", 457f)

    @Test
    fun bordeDerecho() = captura("borde-derecho", "0713463", 840f)
}
