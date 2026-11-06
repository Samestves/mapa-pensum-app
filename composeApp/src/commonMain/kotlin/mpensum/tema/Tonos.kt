package mpensum.tema

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * El color de cada materia en el mapa (theme/areas.js y theme/paleta.js).
 * Donde el pensum clasifica por áreas, el del área. Donde no, cada materia
 * toma uno de diez tonos repartidos en arco alrededor del color de la
 * carrera, elegido por su código: siempre el mismo para la misma materia.
 */

private const val TONOS = 10
/** Lo que se abre el arco a cada lado del color de la carrera, en grados */
private const val ARCO = 116.0

/** Qué tono le toca a un código: FNV-1a de 32 bits, como la web */
internal fun tonoDe(codigo: String): Int {
    var h = 2166136261L.toInt()
    for (c in codigo) {
        h = h xor c.code
        h *= 16777619
    }
    return abs(h) % TONOS + 1
}

private fun hexAHsl(hex: String): Triple<Double, Double, Double> {
    val n = hex.removePrefix("#")
    val r = n.substring(0, 2).toInt(16) / 255.0
    val g = n.substring(2, 4).toInt(16) / 255.0
    val b = n.substring(4, 6).toInt(16) / 255.0
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2
    val d = max - min
    if (d == 0.0) return Triple(0.0, 0.0, l * 100)
    val s = d / (1 - abs(2 * l - 1))
    val h = when (max) {
        r -> ((g - b) / d) % 6
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    }
    return Triple((h * 60 + 360) % 360, s * 100, l * 100)
}

private fun hslAColor(h: Double, s: Double, l: Double): Color {
    val sn = s / 100
    val ln = l / 100
    val c = (1 - abs(2 * ln - 1)) * sn
    val x = c * (1 - abs(((h / 60) % 2) - 1))
    val m = ln - c / 2
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0.0)
        h < 120 -> Triple(x, c, 0.0)
        h < 180 -> Triple(0.0, c, x)
        h < 240 -> Triple(0.0, x, c)
        h < 300 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }
    fun canal(v: Double) = ((v + m) * 255).roundToInt().coerceIn(0, 255)
    return Color(canal(r), canal(g), canal(b))
}

/** Los diez tonos de una carrera, en arco alrededor de su color, con una pizca de luz además del matiz */
private fun tonosDeCarrera(hexBase: String): List<Color> {
    val (h, s, l) = hexAHsl(hexBase)
    return List(TONOS) { i ->
        val t = i.toDouble() / (TONOS - 1)
        val tono = (h - ARCO + t * ARCO * 2 + 360) % 360
        val luz = (l + (t - 0.5) * 12).coerceIn(22.0, 76.0)
        hslAColor(tono, s, luz)
    }
}

/**
 * El color de cada materia de una carrera, resuelto una vez por tema: en
 * claro el arco sale del color claro de la carrera (variablesDeTono de la
 * web), que ya viene profundo para leerse sobre blanco.
 */
class ColoresMapa(pensum: Pensum) {
    private val oscuros = if (pensum.tieneAreas) null else tonosDeCarrera(pensum.color.oscuro)
    private val claros by lazy { if (pensum.tieneAreas) null else tonosDeCarrera(pensum.color.claro) }

    fun de(materia: Asignatura): Color {
        val tonos = if (Tema.claro) claros else oscuros
        return when {
            materia.area != null -> colorArea(materia.area)
            tonos != null -> tonos[tonoDe(materia.codigo) - 1]
            else -> Paleta.suave
        }
    }
}

/**
 * La luz de un cable encendido (--flujo-luz y --flujo-mezcla): en oscuro, el
 * color aclarado a medias hacia el blanco; en claro hacia el blanco se
 * perdería en el lienzo, así que va un poco hacia la tinta.
 */
fun avivar(color: Color): Color = if (Tema.claro) lerp(color, Paleta.tinta, 0.18f) else lerp(color, Color.White, 0.5f)
