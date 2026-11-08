package mpensum.carrera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.serialization.json.Json
import mpensum.datos.Carrera
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.logica.cadenaExplicada
import mpensum.logica.planesPorCarga
import mpensum.logica.proximoSemestre
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import mpensum.mapa.MapaCarrera
import mpensum.plataforma.Preferencias
import mpensum.portada.HojaAjustes
import mpensum.portada.PaginaAjustes
import mpensum.tema.Fondo
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs
import kotlin.system.measureNanoTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/*
 * Las transiciones cuadro a cuadro (docs/qa/transiciones.md): el reloj de
 * pruebas avanza de 16 en 16 ms y cada cuadro se fotografía. Cada recorrido
 * deja en build/capturas/transiciones/ una tira de fotogramas (el que salta
 * va con borde rojo) y un .txt con el borde de arriba de la hoja en cada
 * cuadro, que es lo que el ojo ve saltar.
 *
 * Con EXIGIR en true, además falla si la hoja asentada salta más de
 * SALTO_MAXIMO_DP en un solo cuadro: es la regresión de la hoja del progreso.
 */

/** Encender cuando la hoja deje de saltar: a partir de ahí, esta prueba es la regresión */
private const val EXIGIR = true

/** Lo que puede moverse el borde de la hoja asentada en un cuadro sin que se lea como salto */
private const val SALTO_MAXIMO_DP = 12f

/** Cuánto se graba de cada recorrido: 38 cuadros de 16 ms */
private const val GRABAR_MS = 608L

/** Los fotogramas de la tira, a un cuarto de su tamaño */
private const val ESCALA_TIRA = 4

private val CARPETA = File("build/capturas/transiciones")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class CapturaTransicionesTest {

    @get:Rule
    val regla = createComposeRule()

    /** Un fotograma: cuándo, la imagen reducida, el borde de arriba de la hoja (dp) y si el contenido pedido ya está */
    private class Cuadro(val t: Long, val imagen: Bitmap, val tope: Float?, val listo: Boolean?) {
        var delta: Float? = null
        var salto = false
    }

    @BeforeTest
    fun preparar() {
        System.setProperty("roborazzi.test.record", "true")
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
        // Un latido eterno no deja ver el reposo, y aquí cada cuadro cuenta
        latidoActivo = false
        CARPETA.mkdirs()
    }

    @AfterTest
    fun limpiar() {
        Tema.juego = Juego.OSCURO
        Tema.fondo = Fondo.REJILLA
        latidoActivo = true
        RuntimeEnvironment.getApplication().getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE).edit().clear().commit()
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private fun pensum(slug: String = "ingenieria-de-sistemas") =
        leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())

    /** Sistemas en el quinto: del primero al cuarto aprobados y dos en curso, sin tocar lo guardado */
    private fun sistemas(): EstadoCarrera {
        val p = pensum()
        val marcas = p.obligatorias.filter { (it.semestre ?: 99) <= 4 }.associate { it.codigo to Marca.APROBADA } +
            p.obligatorias.filter { it.semestre == 5 }.take(2).associate { it.codigo to Marca.CURSANDO }
        return EstadoCarrera(p, demostracion = marcas)
    }

    private val densidad get() = regla.density.density

    /** El borde de arriba de la cabecera de la hoja (bajo el asa), en dp; null si no hay hoja */
    private fun topeHoja(): Float? =
        regla.onAllNodesWithTag("hoja-cabecera", useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.top?.div(densidad)

    private fun hay(texto: String) = regla.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()

    /**
     * La pantalla como Bitmap. captureToImage espera un redibujado con el reloj
     * parado y no llega nunca: se pasa por Roborazzi, que la pinta sin esperar.
     */
    private fun pantalla(): Bitmap {
        val tmp = File(CARPETA, "cuadro.tmp.png")
        regla.onRoot().captureRoboImage(tmp)
        return BitmapFactory.decodeFile(tmp.path).also { tmp.delete() }
    }

    private fun foto(listo: (() -> Boolean)?, t: Long): Cuadro {
        val entera = pantalla()
        val reducida = Bitmap.createScaledBitmap(entera, entera.width / ESCALA_TIRA, entera.height / ESCALA_TIRA, true)
        return Cuadro(t, reducida, topeHoja(), listo?.invoke())
    }

    /** Deja correr el reloj a pasos de un cuadro: lo que se calcula en otro hilo tiene ocasión de llegar */
    private fun esperar(ms: Long) {
        val inicio = regla.mainClock.currentTime
        while (regla.mainClock.currentTime - inicio < ms) {
            regla.mainClock.advanceTimeByFrame()
            Thread.sleep(2)
        }
    }

    /**
     * Graba un recorrido: el cuadro quieto de antes, la acción y `ms` de
     * cuadros de 16 ms. Marca en rojo los cuadros en que la hoja salta y
     * devuelve el salto más grande, en dp.
     */
    private fun grabar(nombre: String, listo: (() -> Boolean)? = null, marcarSaltos: Boolean = true, ms: Long = GRABAR_MS, accion: () -> Unit): Float {
        val cuadros = mutableListOf(foto(listo, -16))
        accion()
        val t0 = regla.mainClock.currentTime
        while (regla.mainClock.currentTime - t0 < ms) {
            regla.mainClock.advanceTimeByFrame()
            Thread.sleep(2)
            cuadros += foto(listo, regla.mainClock.currentTime - t0)
        }
        var mayor = 0f
        cuadros.zipWithNext { a, b ->
            val d = if (a.tope != null && b.tope != null) b.tope - a.tope else null
            b.delta = d
            if (marcarSaltos && d != null && abs(d) >= SALTO_MAXIMO_DP) b.salto = true
            if (d != null && marcarSaltos) mayor = maxOf(mayor, abs(d))
        }
        escribirTira(nombre, cuadros)
        val registro = buildString {
            appendLine("$nombre: un cuadro cada 16 ms; tope = borde de arriba de la cabecera de la hoja, en dp")
            cuadros.forEach { c ->
                val tope = c.tope?.let { "%6.1f".format(it) } ?: "     -"
                val delta = c.delta?.let { "%+6.1f".format(it) } ?: "     -"
                val listoTexto = when (c.listo) { null -> ""; true -> "  contenido: sí"; false -> "  contenido: no" }
                appendLine("t=%4d ms  tope=%s  Δ=%s%s%s".format(c.t, tope, delta, listoTexto, if (c.salto) "  ← SALTO" else ""))
            }
            appendLine("Salto mayor de la hoja asentada en un cuadro: %.1f dp".format(mayor))
        }
        File(CARPETA, "$nombre.txt").writeText(registro)
        println(registro)
        return mayor
    }

    /** La tira: diez fotogramas por fila, cada uno con su tiempo y su salto encima */
    private fun escribirTira(nombre: String, cuadros: List<Cuadro>) {
        val columnas = 10
        val w = cuadros.first().imagen.width
        val h = cuadros.first().imagen.height
        val rotulo = 30
        val hueco = 6
        val filas = (cuadros.size + columnas - 1) / columnas
        val tira = Bitmap.createBitmap(columnas * (w + hueco), filas * (h + rotulo + hueco), Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(tira)
        lienzo.drawColor(android.graphics.Color.rgb(34, 36, 40))
        val texto = Paint().apply { color = android.graphics.Color.WHITE; textSize = 19f; isAntiAlias = true }
        val rojo = Paint().apply { color = android.graphics.Color.rgb(255, 59, 48); style = Paint.Style.STROKE; strokeWidth = 6f }
        cuadros.forEachIndexed { i, c ->
            val x = (i % columnas) * (w + hueco) + hueco / 2f
            val y = (i / columnas) * (h + rotulo + hueco) + hueco / 2f
            val delta = c.delta?.let { " Δ%+.0f".format(it) }.orEmpty()
            lienzo.drawText("${c.t} ms$delta${if (c.listo == false) " ·vacío" else ""}", x + 4f, y + 21f, texto)
            lienzo.drawBitmap(c.imagen, x, y + rotulo, null)
            if (c.salto) lienzo.drawRect(x + 3f, y + rotulo + 3f, x + w - 3f, y + rotulo + h - 3f, rojo)
        }
        tira.captureRoboImage(File(CARPETA, "$nombre.png"))
    }

    // ─────────────────────────────── El progreso de pie ──────────────────────────────

    private fun montarProgreso(estado: EstadoCarrera) {
        regla.mainClock.autoAdvance = false
        val camara = Camara(estado.pensum.slug + "transiciones")
        val cristal = HazeState()
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize().hazeSource(cristal)) {
                    MapaCarrera(estado, camara, EfectosMapa(), null, congelado = true, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                // Como en la carrera de verdad: lo pesado se calcula en segundo plano antes de abrir nada
                LaunchedEffect(estado) { estado.precalentar() }
                CompositionLocalProvider(LocalFondoHoja provides cristal) {
                    HojaAvance(true, estado, alCerrar = {}, alVerEnMapa = {})
                }
            }
        }
        // La hoja sube, la cifra cuenta y los planes se calculan: todo quieto antes de grabar
        esperar(1600)
        var intentos = 0
        while (!(estado.simulador.proximoListo && estado.simulador.cadenaLista) && intentos++ < 300) esperar(32)
    }

    /** De la hoja a una pregunta y de vuelta: la ida y la vuelta van en tiras aparte */
    private fun idaYVuelta(nombre: String, fila: String, listo: () -> Boolean) {
        montarProgreso(sistemas())
        val ida = grabar("$nombre-ida", listo) { regla.onNodeWithText(fila).performClick() }
        esperar(1500)
        val vuelta = grabar("$nombre-vuelta") { regla.onNodeWithContentDescription("Volver").performClick() }
        if (EXIGIR) assertTrue(maxOf(ida, vuelta) < SALTO_MAXIMO_DP, "La hoja salta ${maxOf(ida, vuelta)} dp en un cuadro")
    }

    /** «Tu próximo semestre»: lo pesado llega de otro hilo y mientras tanto reserva 480 dp */
    @Test
    fun progresoProximo() = idaYVuelta("progreso-proximo", "Tu próximo semestre") { hay("FIJA TU FECHA") || hay("TE ABREN CAMINO") }

    /** «No puedes atrasar»: el mismo patrón de cálculo aparte */
    @Test
    fun progresoCadena() = idaYVuelta("progreso-cadena", "No puedes atrasar") { hay("LA CADENA") }

    /** «¿Cuándo me gradúo?»: sin cálculo aparte, con su propia entrada de 1,1 s */
    @Test
    fun progresoGraduacion() = idaYVuelta("progreso-graduacion", "¿Cuándo me gradúo?") { hay("POR SEMESTRE") }

    /** Con el índice desplazado hasta las electivas: la hoja comparte un solo scroll entre el índice y el detalle */
    @Test
    fun progresoDesplazado() {
        montarProgreso(sistemas())
        regla.mainClock.autoAdvance = true
        try {
            regla.onNodeWithText("Reiniciar mi avance").performScrollTo()
        } finally {
            regla.mainClock.autoAdvance = false
        }
        esperar(400)
        grabar("progreso-desplazado-ida") { regla.onNodeWithText("Técnicas").performClick() }
        esperar(1500)
        grabar("progreso-desplazado-vuelta") { regla.onNodeWithContentDescription("Volver").performClick() }
    }

    // ─────────────────────────────── Ajustes → Personalizar ──────────────────────────────

    @Test
    fun ajustesPersonalizar() {
        val carreras = Json { ignoreUnknownKeys = true }.decodeFromString<List<Carrera>>(File("src/commonMain/composeResources/files/indice.json").readText())
        regla.mainClock.autoAdvance = false
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                HojaAjustes(PaginaAjustes.MENU, carreras, carreras.first { it.slug == "ingenieria-de-sistemas" }, alCerrar = {}, alElegir = {}, alVerGuia = {})
            }
        }
        esperar(1200)
        grabar("ajustes-personalizar-ida") { regla.onNodeWithText("Personalizar").performClick() }
        esperar(1200)
        grabar("ajustes-personalizar-vuelta") { regla.onNodeWithContentDescription("Volver").performClick() }
    }

    // ─────────────────────────────── La ficha en tema claro ──────────────────────────────

    /**
     * La ficha abierta sobre el mapa en Papel (puntos) y en Claro (rejilla):
     * la franja de encima de la hoja, entera y ampliada, con el color de cada
     * fila de píxeles en el .txt. Y en Papel, la ficha entrando y el cambio a
     * una materia más corta (desde Pide · Abre), cuadro a cuadro.
     */
    @Test
    fun fichaClaroPapel() = fichaClara(Juego.PAPEL, Fondo.PUNTOS, "papel", conTiras = true)

    @Test
    fun fichaClaroClaro() = fichaClara(Juego.CLARO, Fondo.REJILLA, "claro", conTiras = false)

    private fun fichaClara(juego: Juego, fondo: Fondo, nombre: String, conTiras: Boolean) {
        Tema.juego = juego
        Tema.fondo = fondo
        regla.mainClock.autoAdvance = false
        val estado = sistemas()
        val camara = Camara(estado.pensum.slug + "ficha-" + nombre)
        val a = "0713463"
        val b = estado.pensum.obligatorias.first { it.semestre == 1 }.codigo
        var codigo by mutableStateOf<String?>(null)
        var encuadre: Float? = null
        regla.setContent {
            Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
                Box(Modifier.fillMaxSize()) {
                    MapaCarrera(estado, camara, EfectosMapa(), codigo, congelado = false, alTocarMateria = {}, alTocarVacio = {}, alAbrirCasilla = {}, alAlternarSemestre = {})
                }
                FichaMateria(codigo, estado, alCerrar = {}, alMarcar = { _, _ -> }, alIrA = {}, alCambiarElectiva = {}, alEncuadrar = { _, alto -> encuadre = alto.value })
            }
        }
        esperar(1200)
        if (conTiras) grabar("ficha-$nombre-entra", marcarSaltos = false, ms = 480) { codigo = a } else {
            codigo = a
        }
        esperar(1200)
        franja(nombre, a)
        if (!conTiras) return
        val abierta = topeHoja()
        encuadre = null
        grabar("ficha-$nombre-cambia", ms = 400) { codigo = b }
        esperar(800)
        val asentada = topeHoja()
        File(CARPETA, "ficha-$nombre-cambia.txt").appendText(
            "Encuadre pedido con la hoja de ${encuadre?.let { "%.1f".format(it) }} dp; la hoja asentada mide %.1f dp (tope de la cabecera: de %.1f a %.1f dp)\n"
                .format(800f - (asentada ?: 0f), abierta ?: 0f, asentada ?: 0f),
        )
    }

    /** La franja de encima de la hoja: la pantalla entera, el recorte ampliado y el color de cada fila de píxeles */
    private fun franja(nombre: String, codigo: String) {
        val entera = pantalla()
        entera.captureRoboImage(File(CARPETA, "ficha-$nombre-$codigo.png"))
        val d = densidad
        // El borde de la hoja: 20 dp por encima de su cabecera (8 + asa de 4 + 8)
        val borde = ((topeHoja() ?: return) - 20f) * d
        val arriba = (borde - 56 * d).toInt().coerceAtLeast(0)
        val abajo = (borde + 24 * d).toInt().coerceAtMost(entera.height)
        val recorte = Bitmap.createBitmap(entera, 0, arriba, entera.width, abajo - arriba)
        Bitmap.createScaledBitmap(recorte, recorte.width, recorte.height * 2, true).captureRoboImage(File(CARPETA, "ficha-$nombre-franja.png"))
        // Una columna sin tarjetas: el centro entre las dos columnas del mapa no vale siempre; se toma la mediana de la fila
        val registro = buildString {
            appendLine("ficha-$nombre: color medio de cada fila de píxeles, de 56 dp encima del borde de la hoja a 8 dp dentro")
            var y = arriba
            while (y < (borde + 8 * d).toInt()) {
                val fila = IntArray(entera.width).also { entera.getPixels(it, 0, entera.width, 0, y, entera.width, 1) }
                val luz = fila.map { (android.graphics.Color.red(it) + android.graphics.Color.green(it) + android.graphics.Color.blue(it)) / 3 }.sorted()
                appendLine("%+6.1f dp  luz mediana %3d  (lienzo %d)".format((y - borde) / d, luz[luz.size / 2], lienzoLuz()))
                y += d.toInt()
            }
        }
        File(CARPETA, "ficha-$nombre-franja.txt").writeText(registro)
        println(registro)
    }

    private fun lienzoLuz(): Int {
        val c = Paleta.lienzo
        return ((c.red + c.green + c.blue) / 3f * 255).toInt()
    }

    // ─────────────────────────────── Lo que cuesta el cálculo ──────────────────────────────

    /** Lo que tarda en este equipo lo que la hoja calcula al abrirse y lo que cada pantalla interna pide aparte (en frío y repetido) */
    @Test
    fun costeDelCalculo() {
        val estado = sistemas()
        val p = estado.pensum
        val registro = buildString {
            repeat(2) { vuelta ->
                val planes = measureNanoTime { planesPorCarga(p, estado.marcas, estado.casillas, estado.hoy) } / 1e6
                val sim = estado.simulador
                val proximo = measureNanoTime { proximoSemestre(sim.pensum, sim.marcas, sim.casillas, estado.carga, sim.hoy, sim) } / 1e6
                val cadena = measureNanoTime { cadenaExplicada(sim.pensum, sim.marcas, sim.casillas, estado.carga, sim.hoy, sim) } / 1e6
                appendLine("%s: planesPorCarga %.1f ms · proximoSemestre %.1f ms · cadenaExplicada %.1f ms".format(if (vuelta == 0) "En frío" else "Repetido", planes, proximo, cadena))
            }
        }
        File(CARPETA, "coste-del-calculo.txt").writeText(registro)
        println(registro)
    }
}
