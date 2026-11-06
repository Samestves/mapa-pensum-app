package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.tema.Tipo
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * La gráfica del avance: UC acumuladas contra el tiempo. A la izquierda lo
 * recorrido (un punto por semestre del pensum, con lo aprobado), a la derecha
 * lo proyectado (un punto por período del plan), y la meta arriba con el
 * birrete donde llegas. El eje va en períodos, con el 0 en hoy.
 *
 * Todo lo que se mueve (la entrada, el cambio de plan, el latido) se lee al
 * dibujar: la gráfica no recompone nunca mientras anima.
 */

/** Los puntos con que se pinta cada tramo: suficientes para que la curva se lea lisa */
private const val MUESTRAS_PASADO = 32
private const val MUESTRAS_FUTURO = 48

/** Lo que dura la curva proyectada en pasar de un plan al otro, en ms */
private const val CAMBIO_MS = 450
private const val ENTRADA_MS = 900
private const val LATIDO_MS = 1_300

/** Las pruebas lo apagan: un latido eterno no deja que el reloj de pruebas llegue a estar en reposo */
internal var latidoActivo = true

/**
 * La gráfica ya calculada para un plan. `pasado` y `futuro` son puntos
 * (períodos desde hoy, UC) muestreados lisos; `futuro` parte de hoy y acaba en
 * `fin`, el período en que terminas.
 */
internal class Escena(
    val xmin: Float,
    val xmax: Float,
    val pasado: List<Offset>,
    val futuro: List<Offset>,
    val fin: Float,
    val llevas: Float,
    val meta: Float,
)

private fun mezcla(a: Float, b: Float, f: Float) = a + (b - a) * f

private fun mezcla(a: Escena, b: Escena, f: Float): Escena = when {
    f <= 0f -> a
    f >= 1f -> b
    else -> Escena(
        xmin = mezcla(a.xmin, b.xmin, f),
        xmax = mezcla(a.xmax, b.xmax, f),
        pasado = b.pasado,
        futuro = a.futuro.zip(b.futuro) { p, q -> Offset(mezcla(p.x, q.x, f), mezcla(p.y, q.y, f)) },
        fin = mezcla(a.fin, b.fin, f),
        llevas = b.llevas,
        meta = b.meta,
    )
}

/**
 * La curva de un plan. Cuenta UC donde el pensum las trae y materias donde no.
 * El pasado va por semestre del pensum con lo aprobado; el futuro, por período
 * del plan (lo que cursas cierra el período en curso). Lo que falta se reparte
 * entre los períodos según lo que pesa cada uno y acaba justo en la meta.
 */
internal fun escenaDe(estado: EstadoCarrera, g: EstimacionGraduacion): Escena {
    val pensum = estado.pensum
    val progreso = estado.progreso
    val conUc = progreso.porcentaje != null
    val meta = (if (conUc) progreso.ucTitulo!! else progreso.total).toFloat().coerceAtLeast(1f)
    val llevas = (if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas).toFloat().coerceAtMost(meta)
    val todas = pensum.todas
    val porCodigo = todas.associateBy { it.codigo }
    fun peso(uc: Int?) = if (conUc) (uc ?: 0).toFloat() else 1f

    // El pasado: lo aprobado hasta cada semestre; el último punto es hoy, con las electivas incluidas
    val aprobadas = pensum.obligatorias.filter { estado.marcas[it.codigo] == Marca.APROBADA }
    val recorridos = max(1, aprobadas.mapNotNull { it.semestre }.maxOrNull() ?: 0)
    var tope = 0f
    val pasado = (0..recorridos).map { k ->
        val acum = if (k == recorridos) llevas else aprobadas.filter { (it.semestre ?: 99) <= k }.sumOf { peso(it.uc).toDouble() }.toFloat()
        tope = max(tope, min(acum, llevas))
        Offset((k - recorridos).toFloat(), tope)
    }

    // El futuro: lo que cursas ahora y lo que mete cada período del plan
    val tipico = (todas.mapNotNull { it.uc }.filter { it > 0 }.sorted().let { if (it.isEmpty()) null else it[it.size / 2] } ?: 3).let { if (conUc) it.toFloat() else 1f }
    fun pesoDe(codigo: String): Float = porCodigo[codigo]?.let { peso(it.uc) }?.takeIf { it > 0f } ?: tipico
    val incrementos = buildList {
        if (g.cursaAhora) add(todas.filter { estado.marcas[it.codigo] == Marca.CURSANDO }.sumOf { peso(it.uc).toDouble() }.toFloat().coerceAtLeast(1f))
        g.plan.forEach { periodo -> add(periodo.sumOf { pesoDe(it).toDouble() }.toFloat()) }
    }
    val suma = incrementos.sum()
    val escala = if (suma > 0f) max(0f, meta - llevas) / suma else 0f
    var y = llevas
    val futuro = listOf(Offset(0f, llevas)) + incrementos.mapIndexed { i, d ->
        y += d * escala
        Offset(i + 1f, y)
    }
    val fin = incrementos.size.toFloat()
    return Escena(
        xmin = pasado.first().x,
        // Si el plan pasa del eje, el eje crece con él
        xmax = max(fin + 1f, 4f),
        pasado = muestrear(pasado, MUESTRAS_PASADO),
        futuro = muestrear(futuro, MUESTRAS_FUTURO),
        fin = fin,
        llevas = llevas,
        meta = meta,
    )
}

/** n puntos repartidos en x por una curva cúbica que no sube ni baja de más entre los datos (Fritsch-Carlson) */
private fun muestrear(p: List<Offset>, n: Int): List<Offset> {
    if (p.size < 2) return List(n) { p.first() }
    val m = p.size - 1
    val d = FloatArray(m) { (p[it + 1].y - p[it].y) / (p[it + 1].x - p[it].x) }
    val t = FloatArray(p.size)
    t[0] = d[0]
    t[m] = d[m - 1]
    for (i in 1 until m) t[i] = if (d[i - 1] * d[i] <= 0f) 0f else (d[i - 1] + d[i]) / 2f
    for (i in 0 until m) {
        if (d[i] == 0f) {
            t[i] = 0f
            t[i + 1] = 0f
        } else {
            val a = t[i] / d[i]
            val b = t[i + 1] / d[i]
            val h = a * a + b * b
            if (h > 9f) {
                val tau = 3f / sqrt(h)
                t[i] = tau * a * d[i]
                t[i + 1] = tau * b * d[i]
            }
        }
    }
    val x0 = p.first().x
    val x1 = p.last().x
    var seg = 0
    return List(n) { k ->
        val x = x0 + (x1 - x0) * k / (n - 1)
        while (seg < m - 1 && x > p[seg + 1].x) seg++
        val h = p[seg + 1].x - p[seg].x
        val s = ((x - p[seg].x) / h).coerceIn(0f, 1f)
        val s2 = s * s
        val s3 = s2 * s
        val y = (2 * s3 - 3 * s2 + 1) * p[seg].y + (s3 - 2 * s2 + s) * h * t[seg] + (-2 * s3 + 3 * s2) * p[seg + 1].y + (s3 - s2) * h * t[seg + 1]
        Offset(x, y)
    }
}

internal fun DrawScope.icono(trazos: List<Path>, color: Color, izq: Float, arriba: Float, lado: Float, grosor: Float = 2f) {
    val e = lado / 24f
    withTransform({
        translate(izq, arriba)
        scale(e, e, pivot = Offset.Zero)
    }) {
        trazos.forEach { drawPath(it, color, style = Stroke(grosor, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    }
}

/**
 * La gráfica. `detalle` es la grande del detalle (con la pastilla de la fecha
 * y la burbuja del porcentaje); sin él, la de la hoja (solo el birrete).
 */
@Composable
internal fun GraficaAvance(
    escena: Escena,
    fecha: String,
    porcentaje: Int,
    hoy: Periodo,
    color: Color,
    alto: Dp,
    detalle: Boolean,
    modifier: Modifier = Modifier,
) {
    var desde by remember { mutableStateOf(escena) }
    var hacia by remember { mutableStateOf(escena) }
    val cambio = remember { Animatable(1f) }
    LaunchedEffect(escena) {
        if (escena === hacia) return@LaunchedEffect
        // Si cambia a medio camino, sigue desde donde iba
        desde = mezcla(desde, hacia, cambio.value)
        hacia = escena
        cambio.snapTo(0f)
        cambio.animateTo(1f, tween(CAMBIO_MS, easing = ENFATICA))
    }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(ENTRADA_MS, easing = ENFATICA)) }
    val latido = if (latidoActivo && !Tema.sinBucles) rememberInfiniteTransition(label = "latido").animateFloat(0f, 1f, infiniteRepeatable(tween(LATIDO_MS, easing = LinearEasing), RepeatMode.Restart), label = "latido") else null

    val medidor = rememberTextMeasurer(cacheSize = 24)
    val i = inter()
    val etiqueta = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, letterSpacing = 0.05.em, color = Paleta.tenue)
    val etiquetaHoy = etiqueta.copy(color = Paleta.tinta)
    val textoPastilla = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = Tipo.xs, letterSpacing = 0.05.em, color = Paleta.tinta)
    val textoBurbuja = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = Tipo.xs, color = Paleta.lienzo)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(alto)
            // Con latido, en su propia capa: cada cuadro repinta la curva y no la hoja entera
            .then(if (latido != null) Modifier.graphicsLayer {} else Modifier)
            .semantics { contentDescription = "Tu curva de avance: $porcentaje por ciento hoy, te gradúas en $fecha" },
    ) {
        val e = mezcla(desde, hacia, cambio.value)
        val w = size.width
        val arriba = 18.dp.toPx()
        val abajo = size.height - 26.dp.toPx()
        val rango = e.xmax - e.xmin
        fun px(x: Float) = (x - e.xmin) / rango * w
        fun py(v: Float) = abajo - (v / e.meta).coerceIn(0f, 1f) * (abajo - arriba)
        val g1 = 1.dp.toPx()

        // La rejilla: el suelo, dos líneas y la meta arriba, más firme
        val rejilla = Paleta.tinta.copy(alpha = 0.06f)
        drawLine(rejilla, Offset(0f, abajo), Offset(w, abajo), g1)
        for (k in 1..2) drawLine(rejilla, Offset(0f, abajo - (abajo - arriba) * k / 3f), Offset(w, abajo - (abajo - arriba) * k / 3f), g1)
        drawLine(Paleta.tinta.copy(alpha = 0.22f), Offset(0f, arriba), Offset(w, arriba), g1)

        fun curva(puntos: List<Offset>) = Path().apply {
            puntos.forEachIndexed { k, p -> if (k == 0) moveTo(px(p.x), py(p.y)) else lineTo(px(p.x), py(p.y)) }
        }
        fun area(puntos: List<Offset>, linea: Path) = Path().apply {
            addPath(linea)
            lineTo(px(puntos.last().x), abajo)
            lineTo(px(puntos.first().x), abajo)
            close()
        }

        val hoyX = px(0f)
        val hoyY = py(e.llevas)
        // La entrada barre la curva de izquierda a derecha
        clipRect(right = w * entrada.value) {
            val lineaFutura = curva(e.futuro)
            if (e.fin > 0f) {
                drawPath(area(e.futuro, lineaFutura), Brush.verticalGradient(listOf(color.copy(alpha = 0.13f), color.copy(alpha = 0f)), arriba, abajo))
                drawPath(lineaFutura, color.copy(alpha = 0.5f), style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val lineaPasada = curva(e.pasado)
            drawPath(area(e.pasado, lineaPasada), Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)), arriba, abajo))
            drawPath(lineaPasada, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // La meta: la pastilla con el birrete donde llegas
        val metaX = px(e.fin)
        val centroY = arriba
        if (detalle) {
            val texto = medidor.measure(fecha.uppercase(), textoPastilla)
            val ancho = 12.dp.toPx() + 14.dp.toPx() + 6.dp.toPx() + texto.size.width + 12.dp.toPx()
            val alt = 30.dp.toPx()
            val izq = (metaX - ancho / 2f).coerceIn(0f, w - ancho)
            val cima = centroY - alt / 2f
            drawRoundRect(Paleta.lienzo, Offset(izq, cima), Size(ancho, alt), CornerRadius(alt / 2f))
            drawRoundRect(Paleta.tinta.copy(alpha = 0.14f), Offset(izq, cima), Size(ancho, alt), CornerRadius(alt / 2f), style = Stroke(g1))
            icono(Lucide.birrete, Paleta.tinta, izq + 12.dp.toPx(), centroY - 7.dp.toPx(), 14.dp.toPx(), 2f)
            drawText(texto, topLeft = Offset(izq + 32.dp.toPx(), centroY - texto.size.height / 2f))
        } else {
            val r = 14.dp.toPx()
            val cx = metaX.coerceIn(r, w - r)
            drawCircle(Paleta.lienzo, r, Offset(cx, centroY))
            drawCircle(Paleta.tinta.copy(alpha = 0.14f), r - g1 / 2f, Offset(cx, centroY), style = Stroke(g1))
            icono(Lucide.birrete, Paleta.tinta, cx - 7.dp.toPx(), centroY - 7.dp.toPx(), 14.dp.toPx(), 2f)
        }

        // Hoy: el punto con su anillo y el halo que late
        val aparece = ((entrada.value * w - hoyX) / (24.dp.toPx())).coerceIn(0f, 1f)
        if (aparece > 0f) {
            if (latido != null) {
                val l = latido.value
                drawCircle(color.copy(alpha = 0.45f * (1f - l) * aparece), 4.5.dp.toPx() * (1f + 1.6f * l), Offset(hoyX, hoyY))
            }
            drawCircle(color.copy(alpha = 0.4f * aparece), 7.dp.toPx(), Offset(hoyX, hoyY))
            drawCircle(Paleta.tinta.copy(alpha = aparece), 4.5.dp.toPx(), Offset(hoyX, hoyY))
            if (detalle) {
                val burbuja = medidor.measure("$porcentaje %", textoBurbuja)
                val bw = burbuja.size.width + 20.dp.toPx()
                val bh = 26.dp.toPx()
                val izq = (hoyX - bw / 2f).coerceIn(0f, w - bw)
                val cima = hoyY - 14.dp.toPx() - bh
                drawRoundRect(Paleta.tinta.copy(alpha = aparece), Offset(izq, cima), Size(bw, bh), CornerRadius(bh / 2f))
                drawText(burbuja, topLeft = Offset(izq + 10.dp.toPx(), cima + (bh - burbuja.size.height) / 2f), alpha = aparece)
            }
        }

        // El eje: HOY, y cada año centrado en lo que dura
        val yEje = abajo + 8.dp.toPx()
        val textoHoy = medidor.measure("HOY", etiquetaHoy)
        drawText(textoHoy, topLeft = Offset((hoyX - textoHoy.size.width / 2f).coerceAtLeast(0f), yEje))
        val inicioHoy = hoy.anio * 2 + hoy.lapso
        for (anio in hoy.anio..hoy.anio + ceil(e.xmax / 2f).toInt() + 1) {
            val centro = (anio * 2 - inicioHoy).toFloat() + 2f
            if (centro < 1.5f || centro > e.xmax - 0.3f) continue
            val t = medidor.measure("$anio", etiqueta)
            val x = px(centro) - t.size.width / 2f
            // Si el año se pega a HOY, se queda sin etiqueta
            if (x < hoyX + textoHoy.size.width / 2f + 6.dp.toPx()) continue
            drawText(t, topLeft = Offset(x, yEje))
        }
    }
}
