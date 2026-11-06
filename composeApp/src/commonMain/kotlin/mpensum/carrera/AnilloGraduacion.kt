package mpensum.carrera

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.em
import mpensum.componentes.Lucide
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.tema.Paleta
import mpensum.tema.Tema
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * El anillo de "¿Cuándo me gradúo?": una marca radial por materia, en sentido
 * horario desde arriba (el hueco de arriba lleva el birrete). Lo aprobado en
 * verde y cada vez más vivo hacia hoy, lo que cursas en tinta, el próximo
 * semestre en azul y el plan en tinta cada vez más apagada hacia la meta.
 * META e INICIO curvos por dentro, a cada lado del birrete; por fuera solo
 * HOY (con su rayita en codo), los años y dos anotaciones con línea guía, y el
 * porcentaje al centro. Todo en un solo Canvas; la entrada y el latido de HOY
 * solo se leen al dibujar, así que no recompone nunca mientras anima.
 */

private const val RO = 100f
private const val RI = 82f
private const val HUECO = 6
private const val LATIDO_ANILLO_MS = 1_300

/** Lo mínimo que se separan del borde las anotaciones, en dp; se ensancha si su texto lo pide */
private const val MARGEN_NOTA = 64f

/** Una materia en el anillo: en qué casilla va y de qué tipo es (0 aprobada, 1 cursando, 2 próximo semestre, 3 plan) */
internal class Ficha(val k: Int, val tipo: Int)

/** El hueco que abre un año nuevo: la casilla vacía antes del primer semestre de ese año */
internal class Anio(val anio: Int, val hueco: Int)

internal class Anillo(
    val fichas: List<Ficha>,
    val casillas: Int,
    val anios: List<Anio>,
    /** La casilla (fraccionaria) donde cae HOY */
    val hoyK: Float,
    val aprobadas: Int,
    val porAprobar: Int,
)

private class Grupo(val periodo: Periodo, val n: Int, val tipo: Int)

/** El anillo de un plan: lo aprobado por semestre del pensum, lo que cursas y lo que mete cada período */
internal fun anilloDe(estado: EstadoCarrera, g: EstimacionGraduacion, hoy: Periodo): Anillo {
    val marcas = estado.marcas
    val todas = estado.pensum.todas
    val aprobadas = todas.filter { marcas[it.codigo] == Marca.APROBADA }
    val cursando = todas.count { marcas[it.codigo] == Marca.CURSANDO }
    val ultimo = aprobadas.mapNotNull { it.semestre }.maxOrNull()
    val pasados = aprobadas.groupingBy { it.semestre ?: ultimo ?: 0 }.eachCount().entries.sortedBy { it.key }.map { it.value }
    val grupos = buildList {
        pasados.forEachIndexed { i, n -> add(Grupo(hoy.mas(i - pasados.size), n, 0)) }
        if (cursando > 0) add(Grupo(hoy, cursando, 1))
        g.plan.forEachIndexed { j, p -> if (p.isNotEmpty()) add(Grupo(hoy.mas(j + 1), p.size, if (j == 0) 2 else 3)) }
    }
    val fichas = ArrayList<Ficha>()
    val anios = ArrayList<Anio>()
    var k = 0
    var primeroDelPlan = -1f
    grupos.forEachIndexed { i, grupo ->
        if (i > 0) k++
        if (grupo.periodo.lapso == 1) anios.add(Anio(grupo.periodo.anio, k - 1))
        if (grupo.tipo >= 2 && primeroDelPlan < 0f) primeroDelPlan = k.toFloat()
        repeat(grupo.n) {
            fichas.add(Ficha(k, grupo.tipo))
            k++
        }
    }
    val cursan = fichas.filter { it.tipo == 1 }
    val hoyK = when {
        cursan.isNotEmpty() -> cursan.map { it.k }.average().toFloat()
        primeroDelPlan >= 0f -> primeroDelPlan - 1f
        else -> k.toFloat()
    }
    return Anillo(fichas, k, anios, hoyK, aprobadas.size, g.materiasRestantes + cursando)
}

/** De 0 a 1, frenando al final */
internal fun suavizar(v: Float): Float {
    val c = 1f - v.coerceIn(0f, 1f)
    return 1f - c * c * c
}

private fun distancia(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return min(d, 360f - d)
}

@Composable
internal fun AnilloGraduacion(a: Anillo, porcentaje: Int, anioMeta: Int?, anioHoy: Int, entrada: () -> Float, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer(cacheSize = 96)
    val rotulo = estilo(11, Color.Unspecified, FontWeight.SemiBold).copy(letterSpacing = 0.04.em)
    val meta = estilo(10, Color.Unspecified, FontWeight.SemiBold).copy(letterSpacing = 0.04.em)
    val inicioRotulo = estilo(9, Color.Unspecified, FontWeight.SemiBold).copy(letterSpacing = 0.04.em)
    val anio = estilo(10, Color.Unspecified, FontWeight.SemiBold).copy(letterSpacing = 0.04.em)
    val numeroNota = estilo(17, Color.Unspecified, FontWeight.SemiBold, 20)
    val palabraNota = estilo(12, Color.Unspecified, FontWeight.Normal, 16)
    val grande = estilo(34, Color.Unspecified, FontWeight.Light, 36).copy(letterSpacing = (-0.03).em)
    val porciento = estilo(20, Color.Unspecified, FontWeight.Light, 24)
    val pie = estilo(10, Color.Unspecified, FontWeight.SemiBold, 14).copy(letterSpacing = 0.06.em)
    val latido = if (latidoActivo && !Tema.movimiento) {
        rememberInfiniteTransition(label = "latidoAnillo").animateFloat(0f, 1f, infiniteRepeatable(tween(LATIDO_ANILLO_MS, easing = LinearEasing), RepeatMode.Restart), label = "latidoAnillo")
    } else null

    Canvas(
        modifier
            .fillMaxWidth()
            .height(270.dp)
            .semantics { contentDescription = "${a.aprobadas} materias aprobadas y ${a.porAprobar} por aprobar, el $porcentaje % del título" },
    ) {
        val u = 1.dp.toPx()
        val w = size.width
        val cx = w / 2f
        val cy = 132f * u
        val t = entrada()
        val avance = suavizar(t / 0.55f)
        val oa = suavizar((t - 0.5f) / 0.25f)
        val tinta = Paleta.tinta
        val paso = 360f / (a.casillas + HUECO)
        fun ang(k: Float) = (k + HUECO / 2f + 0.5f) * paso
        fun xy(r: Float, grados: Float): Offset {
            val rad = grados * 0.017453292f
            return Offset(cx + r * u * sin(rad), cy - r * u * cos(rad))
        }

        // Las marcas: el pasado se aviva hacia hoy, el futuro se apaga hacia la meta
        val aprobadas = a.fichas.filter { it.tipo == 0 }
        val plan = a.fichas.filter { it.tipo == 3 }
        val visibles = (avance * a.fichas.size).roundToInt()
        a.fichas.forEachIndexed { n, f ->
            var color = when (f.tipo) {
                0 -> Paleta.verde
                2 -> Paleta.azul
                else -> tinta
            }
            var opacidad = when (f.tipo) {
                0 -> 0.45f + 0.55f * (f.k - aprobadas.first().k) / max(1, aprobadas.last().k - aprobadas.first().k)
                3 -> 0.26f - 0.14f * (f.k - plan.first().k) / max(1, plan.last().k - plan.first().k)
                else -> 1f
            }
            if (n >= visibles) {
                color = tinta
                opacidad = 0.06f
            }
            drawLine(color.copy(alpha = opacidad), xy(RI, ang(f.k.toFloat())), xy(RO, ang(f.k.toFloat())), strokeWidth = 3.5f * u, cap = StrokeCap.Round)
        }
        // La meta: el birrete en el hueco de arriba
        val birrete = xy((RI + RO) / 2f, 0f)
        icono(Lucide.birrete, tinta, birrete.x - 10f * u, birrete.y - 10f * u, 20f * u, 1.9f)

        // Texto curvo por dentro del anillo: cada letra rotada a lo largo del arco, con la línea base en r = 68
        fun curvo(texto: String, desde: Float, alFinal: Boolean, color: Color, estiloTexto: TextStyle) {
            val letras = texto.map { medidor.measure(it.toString(), estiloTexto) }
            val radio = (RI - 14f) * u
            val grado = 57.29578f / radio
            val largo = letras.sumOf { it.size.width.toDouble() }.toFloat()
            var inicio = if (alFinal) desde - largo * grado else desde
            letras.forEach { l ->
                val ancho = l.size.width.toFloat()
                rotate(inicio + ancho / 2f * grado, Offset(cx, cy)) {
                    drawText(l, color, Offset(cx - ancho / 2f, cy - radio - l.firstBaseline))
                }
                inicio += ancho * grado
            }
        }
        val abertura = HUECO / 2f * paso
        curvo(if (anioMeta != null) "META $anioMeta" else "META", -abertura, true, tinta.copy(alpha = oa), meta)
        curvo("INICIO", abertura, false, tinta.copy(alpha = 0.44f * oa), inicioRotulo)

        // HOY, lo único de fuera con color: punto azul con su halo que late y una rayita en codo hacia el lado libre
        val gradosHoy = ang(a.hoyK)
        val punto = xy(RO + 9f, gradosHoy)
        val fase = latido?.value
        if (fase != null) drawCircle(Paleta.azul.copy(alpha = 0.35f * (1f - fase) * oa), (5f + 9f * fase) * u, punto)
        else drawCircle(Paleta.azul.copy(alpha = 0.16f * oa), 8.5f * u, punto)
        drawCircle(Paleta.azul.copy(alpha = oa), 4f * u, punto)
        val ladoHoy = if (sin(gradosHoy * 0.017453292f) >= -0.05f) 1 else -1
        val h0 = xy(RO + 16f, gradosHoy)
        val h1 = xy(RO + 26f, gradosHoy)
        val hx = h1.x + 16f * u * ladoHoy
        val guiaHoy = tinta.copy(alpha = 0.5f * oa)
        drawLine(guiaHoy, h0, h1, strokeWidth = u)
        drawLine(guiaHoy, h1, Offset(hx, h1.y), strokeWidth = u)
        val hoyTexto = medidor.measure("HOY", rotulo)
        val hoyX = if (ladoHoy > 0) hx + 5f * u else hx - 5f * u - hoyTexto.size.width
        val hoyCaja = Rect(Offset(hoyX, h1.y - hoyTexto.size.height / 2f), hoyTexto.size.toSize())
        drawText(hoyTexto, tinta.copy(alpha = oa), hoyCaja.topLeft)

        // Anotaciones: el ángulo de su arco más lejos de los años, sin salirse de la caja
        val angulosAnio = a.anios.map { ang(it.hueco.toFloat()) }
        val usados = ArrayList<Float>()
        val textoAprobadas = if (a.aprobadas == 1) "aprobada" else "aprobadas"
        // El margen de cada lado es lo que ocupa su texto, para que ninguno se corte ni deje la nota sin sitio
        fun margenDe(numero: String, palabra: String) =
            max(MARGEN_NOTA * u, max(medidor.measure(numero, numeroNota).size.width, medidor.measure(palabra, palabraNota).size.width) + 2f * u)
        val margenDer = margenDe("${a.aprobadas}", textoAprobadas)
        val margenIzq = margenDe("${a.porAprobar}", "por aprobar")
        fun elegir(tipos: List<Int>, lado: Int, preferido: Float, holgado: Boolean = false): Float? {
            val margen = if (lado > 0) margenDer else margenIzq
            val minHoy = if (holgado) 20f else 34f
            val minBirrete = if (holgado) 40f else 52f
            var mejor: Float? = null
            var puntaje = -Float.MAX_VALUE
            a.fichas.filter { it.tipo in tipos }.forEach { f ->
                val g = ang(f.k.toFloat())
                val x1 = xy(RO + 13f, g).x
                val cabe = (lado > 0 && x1 <= w - margen - 8f * u) || (lado < 0 && x1 >= margen + 8f * u)
                if (cabe && distancia(g, gradosHoy) > minHoy && distancia(g, 0f) > minBirrete && usados.none { distancia(g, it) < 40f }) {
                    val libre = angulosAnio.fold(30f) { acc, ya -> min(acc, distancia(g, ya)) }
                    val p = libre - 0.08f * distancia(g, preferido)
                    if (p > puntaje) {
                        puntaje = p
                        mejor = g
                    }
                }
            }
            return mejor
        }
        fun nota(g: Float, lado: Int, numero: String, palabra: String, color: Color) {
            usados.add(g)
            val p0 = xy(RO + 5f, g)
            val p1 = xy(RO + 13f, g)
            val xe = if (lado > 0) w - margenDer else margenIzq
            val guia = tinta.copy(alpha = 0.35f * oa)
            drawLine(guia, p0, p1, strokeWidth = u)
            drawLine(guia, p1, Offset(xe - 4f * lado * u, p1.y), strokeWidth = u)
            drawCircle(tinta.copy(alpha = 0.6f * oa), 1.75f * u, p0)
            val n = medidor.measure(numero, numeroNota)
            val pal = medidor.measure(palabra, palabraNota)
            val arriba = p1.y - 20f * u
            val xn = if (lado > 0) xe else xe - n.size.width
            val xp = if (lado > 0) xe else xe - pal.size.width
            drawText(n, color.copy(alpha = oa), Offset(xn, arriba))
            drawText(pal, tinta.copy(alpha = 0.56f * oa), Offset(xp, arriba + 20f * u))
        }
        // "aprobadas" manda sobre los años: si no hay hueco limpio, se acerca a HOY o al birrete antes que perderse
        (elegir(listOf(0), 1, 58f) ?: elegir(listOf(0), 1, 58f, holgado = true))
            ?.let { nota(it, 1, "${a.aprobadas}", textoAprobadas, Paleta.verde) }
        (elegir(listOf(2, 3), -1, 225f) ?: elegir(listOf(2, 3), -1, 225f, holgado = true))?.let { nota(it, -1, "${a.porAprobar}", "por aprobar", tinta) }

        // Los años: un tic de 11 dp en cada cambio de año y el número un poco más afuera, si no pisa nada. Nunca el de la meta.
        a.anios.forEach { an ->
            val g = ang(an.hueco.toFloat())
            if (an.anio == anioMeta || distancia(g, 0f) < 50f || distancia(g, gradosHoy) < 22f || usados.any { distancia(g, it) < 34f }) return@forEach
            val rad = g * 0.017453292f
            val rt = RO + 21f + 13f * abs(sin(rad)) + 6f * abs(cos(rad))
            val l = medidor.measure("${an.anio}", anio)
            val c = xy(rt, g)
            val caja = Rect(Offset(c.x - l.size.width / 2f, c.y - l.size.height / 2f), l.size.toSize())
            if (caja.inflate(3f * u).overlaps(hoyCaja.inflate(3f * u))) return@forEach
            drawLine(tinta.copy(alpha = 0.4f * oa), xy(RO + 5f, g), xy(RO + 16f, g), strokeWidth = u)
            drawText(l, tinta.copy(alpha = (if (an.anio == anioHoy) 1f else 0.44f) * oa), caja.topLeft)
        }

        // El centro: el porcentaje y "del título"
        val cifra = medidor.measure("$porcentaje", grande)
        val signo = medidor.measure("%", porciento)
        val inicio = cx - (cifra.size.width + 2f * u + signo.size.width) / 2f
        val arriba = cy - 26f * u
        drawText(cifra, tinta.copy(alpha = oa), Offset(inicio, arriba))
        drawText(signo, tinta.copy(alpha = oa), Offset(inicio + cifra.size.width + 2f * u, arriba + cifra.firstBaseline - signo.firstBaseline))
        val titulo = medidor.measure("DEL TÍTULO", pie)
        drawText(titulo, tinta.copy(alpha = 0.5f * oa), Offset(cx - titulo.size.width / 2f, arriba + 38f * u))
    }
}
