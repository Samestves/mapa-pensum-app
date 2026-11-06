package mpensum.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.logica.Situacion
import mpensum.tema.Paleta

private fun trazo(d: String): Path = PathParser().parsePathString(d).toPath()


/** Los trazos de los iconos de estado, en la rejilla de 20 de la web */
private object Estados {
    val sello = trazo("M2.5 10a7.5 7.5 0 1 0 15 0a7.5 7.5 0 1 0-15 0z")
    val check = trazo("M6.6 10.3l2.3 2.3 4.5-4.9")
    val libro = trazo("M10 6.1C8.3 4.85 6.3 4.35 4 4.5v9.9c2.3-.15 4.3.35 6 1.6 1.7-1.25 3.7-1.75 6-1.6V4.5c-2.3-.15-4.3.35-6 1.6z")
    val lomo = trazo("M10 6.1V16")
    val cuerpo = trazo("M6.5 9h7a2.25 2.25 0 0 1 2.25 2.25v3.75a2.25 2.25 0 0 1-2.25 2.25h-7a2.25 2.25 0 0 1-2.25-2.25v-3.75a2.25 2.25 0 0 1 2.25-2.25z")
    val grilleteAbierto = trazo("M13.25 9L14.696 7.276A3.25 3.25 0 0 0 9.717 3.098L9.075 3.865")
    val grilleteCerrado = trazo("M6.75 9V6.75a3.25 3.25 0 0 1 6.5 0V9")
    val ojo = trazo("M10 12.3v1.7")
    val reloj = trazo("M6 3.25h8M6 16.75h8M7 3.25v2.3c0 1.9 3 2.5 3 4.45s-3 2.55-3 4.45v2.3M13 3.25v2.3c0 1.9-3 2.5-3 4.45s3 2.55 3 4.45v2.3")
}

/** El color de cada estado, el de la tarjeta del mapa */
fun colorEstado(e: Situacion): Color = when (e) {
    Situacion.HECHA -> Paleta.verde
    Situacion.CURSANDO -> Paleta.ambar
    Situacion.INSCRIBIBLE -> Paleta.azul
    Situacion.PROXIMA -> Paleta.suave
    Situacion.LEJANA -> Paleta.rojo
}

private val linea = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val calado = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/**
 * Dibuja un icono de estado de lado `lado` con su esquina en (x, y).
 * `sobre` es el color de la superficie, para lo calado (el check, el ojo).
 * `giro` abre el candado: 0 cerrado, 40 abierto (el giro es sobre la pata derecha).
 */
fun DrawScope.iconoEstado(
    e: Situacion,
    x: Float,
    y: Float,
    lado: Float,
    sobre: Color,
    color: Color = colorEstado(e),
    giro: Float? = null,
) {
    withTransform({
        translate(x, y)
        scale(lado / 20f, lado / 20f, pivot = Offset.Zero)
    }) {
        when (e) {
            Situacion.HECHA -> {
                drawPath(Estados.sello, color)
                drawPath(Estados.check, sobre, style = calado)
            }
            Situacion.CURSANDO -> {
                drawPath(Estados.libro, color.copy(alpha = 0.14f))
                drawPath(Estados.libro, color, style = linea)
                drawPath(Estados.lomo, color, style = linea)
            }
            Situacion.PROXIMA -> drawPath(Estados.reloj, color, style = linea)
            Situacion.INSCRIBIBLE, Situacion.LEJANA -> {
                drawPath(Estados.cuerpo, color)
                drawPath(Estados.cuerpo, color, style = linea)
                val g = giro ?: if (e == Situacion.INSCRIBIBLE) 40f else 0f
                rotate(g, pivot = Offset(13.25f, 9f)) { drawPath(Estados.grilleteCerrado, color, style = linea) }
                drawPath(Estados.ojo, sobre, style = calado)
            }
        }
    }
}

/** Iconos de trazo (rejilla de 24, como Lucide) que usa la interfaz */
object Lucide {
    val flecha = listOf(trazo("M5 12h14"), trazo("m12 5 7 7-7 7"))
    val atras = listOf(trazo("m15 18-6-6 6-6"))
    val cerrar = listOf(trazo("M18 6 6 18"), trazo("m6 6 12 12"))
    val siguiente = listOf(trazo("m9 18 6-6-6-6"))
    val engranaje = listOf(
        trazo("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"),
        trazo("M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"),
    )
    val birrete = listOf(
        trazo("M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"),
        trazo("M22 10v6"),
        trazo("M6 12.5V16a6 3 0 0 0 12 0v-3.5"),
    )
    val mapa = listOf(
        trazo("M9.5 4.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"), trazo("m10.2 6.3-3.9 3.9"),
        trazo("M2 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"), trazo("M7 12h10"),
        trazo("M17 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"), trazo("m13.8 17.7 3.9-3.9"),
        trazo("M9.5 19.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"),
    )
    val lista = listOf(trazo("M8 6h13"), trazo("M8 12h13"), trazo("M8 18h13"), trazo("M3 6h.01"), trazo("M3 12h.01"), trazo("M3 18h.01"))
    val ayuda = listOf(trazo("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z"), trazo("M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"), trazo("M12 17h.01"))
    val bandera = listOf(trazo("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"), trazo("M4 22v-7"))
    val codigo = listOf(trazo("m16 18 6-6-6-6"), trazo("m8 6-6 6 6 6"))
    val mundo = listOf(trazo("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z"), trazo("M12 2a14.5 14.5 0 0 0 0 20a14.5 14.5 0 0 0 0-20"), trazo("M2 12h20"))
    val listo = listOf(trazo("M20 6 9 17l-5-5"))
    val calendario = listOf(trazo("M6.5 5h11a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3v-9a3 3 0 0 1 3-3z"), trazo("M3.5 10h17"), trazo("M8 3v4"), trazo("M16 3v4"))
}

/**
 * Los iconos de la barra de vistas de la web (IconosSF.jsx, Phosphor en
 * rejilla de 256): cada uno en reposo y relleno, porque la vista elegida se
 * enseña rellena.
 */
class IconoDoble(val reposo: Path, val relleno: Path)

object Phosphor {
    val inicio = IconoDoble(
        trazo("M219.31,108.68l-80-80a16,16,0,0,0-22.62,0l-80,80A15.87,15.87,0,0,0,32,120v96a8,8,0,0,0,8,8h64a8,8,0,0,0,8-8V160h32v56a8,8,0,0,0,8,8h64a8,8,0,0,0,8-8V120A15.87,15.87,0,0,0,219.31,108.68ZM208,208H160V152a8,8,0,0,0-8-8H104a8,8,0,0,0-8,8v56H48V120l80-80,80,80Z"),
        trazo("M224,120v96a8,8,0,0,1-8,8H160a8,8,0,0,1-8-8V164a4,4,0,0,0-4-4H108a4,4,0,0,0-4,4v52a8,8,0,0,1-8,8H40a8,8,0,0,1-8-8V120a16,16,0,0,1,4.69-11.31l80-80a16,16,0,0,1,22.62,0l80,80A16,16,0,0,1,224,120Z"),
    )
    // MapTrifold: el mapa plegado en tres paneles (relleno: los tres paneles con su pliegue)
    val mapa = IconoDoble(
        trazo("M228.92,49.69a8,8,0,0,0-6.86-1.45L160.93,63.52,99.58,32.84a8,8,0,0,0-5.52-.6l-64,16A8,8,0,0,0,24,56V200a8,8,0,0,0,9.94,7.76l61.13-15.28,61.35,30.68A8.15,8.15,0,0,0,160,224a8,8,0,0,0,1.94-.24l64-16A8,8,0,0,0,232,200V56A8,8,0,0,0,228.92,49.69ZM104,52.94l48,24V203.06l-48-24ZM40,62.25l48-12v127.5l-48,12Zm176,131.5-48,12V78.25l48-12Z"),
        trazo("M24,56Q24,50 30,48.2L92,32.7V193.5L34,207.8Q24,210 24,200ZM100,33L156,61V223L100,195ZM164,62.7L226,47.2Q232,48 232,56V200Q232,206 226,207.8L164,223.2Z"),
    )
    val lista = IconoDoble(
        trazo("M80,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H88A8,8,0,0,1,80,64Zm136,56H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16Zm0,64H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16ZM44,52A12,12,0,1,0,56,64,12,12,0,0,0,44,52Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,116Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,180Z"),
        trazo("M208,32H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V48A16,16,0,0,0,208,32ZM68,188a12,12,0,1,1,12-12A12,12,0,0,1,68,188Zm0-48a12,12,0,1,1,12-12A12,12,0,0,1,68,140Zm0-48A12,12,0,1,1,80,80,12,12,0,0,1,68,92Zm124,92H104a8,8,0,0,1,0-16h88a8,8,0,0,1,0,16Zm0-48H104a8,8,0,0,1,0-16h88a8,8,0,0,1,0,16Zm0-48H104a8,8,0,0,1,0-16h88a8,8,0,0,1,0,16Z"),
    )
    val horario = IconoDoble(
        trazo("M208,32H184V24a8,8,0,0,0-16,0v8H88V24a8,8,0,0,0-16,0v8H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V48A16,16,0,0,0,208,32ZM72,48v8a8,8,0,0,0,16,0V48h80v8a8,8,0,0,0,16,0V48h24V80H48V48ZM208,208H48V96H208V208Zm-68-76a12,12,0,1,1-12-12A12,12,0,0,1,140,132Zm44,0a12,12,0,1,1-12-12A12,12,0,0,1,184,132ZM96,172a12,12,0,1,1-12-12A12,12,0,0,1,96,172Zm44,0a12,12,0,1,1-12-12A12,12,0,0,1,140,172Zm44,0a12,12,0,1,1-12-12A12,12,0,0,1,184,172Z"),
        trazo("M208,32H184V24a8,8,0,0,0-16,0v8H88V24a8,8,0,0,0-16,0v8H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V48A16,16,0,0,0,208,32ZM84,184a12,12,0,1,1,12-12A12,12,0,0,1,84,184Zm44,0a12,12,0,1,1,12-12A12,12,0,0,1,128,184Zm0-40a12,12,0,1,1,12-12A12,12,0,0,1,128,144Zm44,40a12,12,0,1,1,12-12A12,12,0,0,1,172,184Zm0-40a12,12,0,1,1,12-12A12,12,0,0,1,172,144Zm36-64H48V48H72v8a8,8,0,0,0,16,0V48h80v8a8,8,0,0,0,16,0V48h24Z"),
    )
}

@Composable
fun IconoRelleno(icono: IconoDoble, relleno: Boolean, color: Color, tam: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam)) {
        val e = size.minDimension / 256f
        withTransform({ scale(e, e, pivot = Offset.Zero) }) {
            drawPath(if (relleno) icono.relleno else icono.reposo, color)
        }
    }
}

@Composable
fun IconoTrazo(trazos: List<Path>, color: Color, tam: Dp = 20.dp, grosor: Float = 1.8f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam)) {
        val e = size.minDimension / 24f
        withTransform({ scale(e, e, pivot = Offset.Zero) }) {
            val estilo = Stroke(width = grosor, cap = StrokeCap.Round, join = StrokeJoin.Round)
            trazos.forEach { drawPath(it, color, style = estilo) }
        }
    }
}
