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
    Situacion.CURSANDO -> Paleta.cursando
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
    /** Personalizar: tres fichas de color en abanico (swatch-book de Lucide) */
    val muestrario = listOf(trazo("M11 17a4 4 0 0 1-8 0V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2Z"), trazo("M16.7 13H19a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H7"), trazo("M7 17h.01"), trazo("m11 8 2.3-2.3a2.4 2.4 0 0 1 3.404.004L18.6 7.6a2.4 2.4 0 0 1 .026 3.434L9.9 19.8"))
    /** Tu carrera: el escudo con la fachada dentro. El birrete es del grado */
    val escudo = listOf(trazo("M5 3h14a1 1 0 0 1 1 1v7.5c0 5.2-3.6 8.4-8 10.2-4.4-1.8-8-5-8-10.2V4a1 1 0 0 1 1-1z"), trazo("M8 9.5h8L12 6.5z"), trazo("M9 11.5v4"), trazo("M12 11.5v4"), trazo("M15 11.5v4"), trazo("M8 17.5h8"))

    // Un icono por carrera (docs/personalizar-v2.md §5 y §9), en la rejilla de 24 con trazo 1,5
    /** Petróleo: el balancín de bombeo (cabeza, viga, caballete y manivela) */
    val balancin = listOf(trazo("M3 21h18"), trazo("m9 21 3-12.5 3 12.5"), trazo("M6.5 7.5 20 10"), trazo("M6.5 4a4 4 0 0 0 0 7z"), trazo("M4.5 7.5V21"), trazo("m20 10-1.7 5.6"), trazo("M18 15.5a1.5 1.5 0 1 0 0 3a1.5 1.5 0 1 0 0-3z"))
    /** Sistemas: el monitor con la consola; el </> es de «Código abierto» */
    val consola = listOf(trazo("M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"), trazo("M8 21h8"), trazo("M12 17v4"), trazo("m7 7.5 3 2.5-3 2.5"), trazo("M12.5 12.5h4.5"))
    /** Administración: el organigrama, una caja arriba y tres abajo */
    val organigrama = listOf(trazo("M10 2h4a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z"), trazo("M12 7v8"), trazo("M5 15v-2a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v2"), trazo("M3.5 15h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z"), trazo("M10.5 15h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z"), trazo("M17.5 15h3a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z"))
    /** Agronomía: la espiga (wheat de Lucide) */
    val espiga = listOf(trazo("M2 22 16 8"), trazo("M3.47 12.53 5 11l1.53 1.53a3.5 3.5 0 0 1 0 4.94L5 19l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"), trazo("M7.47 8.53 9 7l1.53 1.53a3.5 3.5 0 0 1 0 4.94L9 15l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"), trazo("M11.47 4.53 13 3l1.53 1.53a3.5 3.5 0 0 1 0 4.94L13 11l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z"), trazo("M20 2h2v2a4 4 0 0 1-4 4h-2V6a4 4 0 0 1 4-4Z"), trazo("M11.47 17.47 13 19l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L5 19l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"), trazo("M15.47 13.47 17 15l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L9 15l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"), trazo("M19.47 9.47 21 11l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L13 11l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z"))
    /** Ambiental: la hoja (leaf de Lucide) */
    val hoja = listOf(trazo("M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z"), trazo("M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"))
    /** Producción animal: la cabeza de res */
    val res = listOf(trazo("M8.5 7C6.5 7 5 5.5 5 3.5"), trazo("M15.5 7c2 0 3.5-1.5 3.5-3.5"), trazo("M7 13V8.5A1.5 1.5 0 0 1 8.5 7h7A1.5 1.5 0 0 1 17 8.5V13"), trazo("M7 9.5H4.5c0 1.7 1 2.5 2.5 2.5"), trazo("M17 9.5h2.5c0 1.7-1 2.5-2.5 2.5"), trazo("M10.5 13h3a4 4 0 0 1 0 8h-3a4 4 0 0 1 0-8z"), trazo("M10 17h.01"), trazo("M14 17h.01"), trazo("M9.8 10.2h.01"), trazo("M14.2 10.2h.01"))
    /** Contaduría: la calculadora (calculator de Lucide) */
    val calculadora = listOf(trazo("M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"), trazo("M8 6h8"), trazo("M16 14v4"), trazo("M16 10h.01"), trazo("M12 10h.01"), trazo("M8 10h.01"), trazo("M12 14h.01"), trazo("M8 14h.01"), trazo("M12 18h.01"), trazo("M8 18h.01"))
    /** Alimentos: el matraz (flask-conical de Lucide) */
    val matraz = listOf(trazo("M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2"), trazo("M6.453 15h11.094"), trazo("M8.5 2h7"))
    /** Recursos humanos: el apretón de manos (handshake de Lucide) */
    val apreton = listOf(trazo("m11 17 2 2a1 1 0 1 0 3-3"), trazo("m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4"), trazo("m21 3 1 11h-2"), trazo("M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3"), trazo("M3 4h8"))

    /** Inicio y horario (house de Lucide) */
    val casa = listOf(trazo("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"), trazo("M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"))
    /** Accesibilidad (person-standing de Lucide); no es el icono de «personas», que es de sociohumanísticas */
    val persona = listOf(trazo("M11 5a1 1 0 1 0 2 0a1 1 0 1 0-2 0z"), trazo("m9 20 3-6 3 6"), trazo("m6 8 6 2 6-2"), trazo("M12 10v4"))
    /** La ranura de día (sun de Lucide) */
    val sol = listOf(trazo("M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z"), trazo("M12 2v2"), trazo("M12 20v2"), trazo("m4.93 4.93 1.41 1.41"), trazo("m17.66 17.66 1.41 1.41"), trazo("M2 12h2"), trazo("M20 12h2"), trazo("m6.34 17.66-1.41 1.41"), trazo("m19.07 4.93-1.41 1.41"))
    /** La ranura de noche (moon de Lucide) */
    val luna = listOf(trazo("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"))
    /** El mapa plegado (map de Lucide): la vista de mapa */
    val mapa3 = listOf(trazo("M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z"), trazo("M15 5.764v15"), trazo("M9 3.236v15"))
    /** calendar-days de Lucide: el calendario con sus seis días */
    val calendarioDias = listOf(
        trazo("M8 2v4"), trazo("M16 2v4"), trazo("M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"), trazo("M3 10h18"),
        trazo("M8 14h.01"), trazo("M12 14h.01"), trazo("M16 14h.01"), trazo("M8 18h.01"), trazo("M12 18h.01"), trazo("M16 18h.01"),
    )
    val calendario = listOf(trazo("M6.5 5h11a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3v-9a3 3 0 0 1 3-3z"), trazo("M3.5 10h17"), trazo("M8 3v4"), trazo("M16 3v4"))
}

/** El icono de la carrera de ese slug; sin slug o con uno desconocido, el escudo de «Tu carrera» */
fun iconoCarrera(slug: String?): List<Path> = when (slug) {
    "ingenieria-de-petroleo" -> Lucide.balancin
    "ingenieria-de-sistemas" -> Lucide.consola
    "licenciatura-en-administracion" -> Lucide.organigrama
    "ingenieria-agronomica" -> Lucide.espiga
    "ingenieria-ambiental" -> Lucide.hoja
    "ingenieria-en-produccion-animal" -> Lucide.res
    "licenciatura-en-contaduria-publica" -> Lucide.calculadora
    "licenciatura-en-tecnologia-de-los-alimentos" -> Lucide.matraz
    "gerencia-de-recursos-humanos" -> Lucide.apreton
    else -> Lucide.escudo
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

/** Un icono relleno cuyo color se lee al dibujar: animarlo no recompone a quien lo lleva */
@Composable
fun IconoRellenoVivo(icono: IconoDoble, relleno: Boolean, color: () -> Color, tam: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam)) {
        val e = size.minDimension / 256f
        withTransform({ scale(e, e, pivot = Offset.Zero) }) {
            drawPath(if (relleno) icono.relleno else icono.reposo, color())
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
