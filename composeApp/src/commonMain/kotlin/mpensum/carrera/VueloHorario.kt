package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.logica.Sesion
import mpensum.logica.enCarriles
import mpensum.logica.rangoDeClases
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/*
 * El viaje de las clases de la captura a la semana, de la maqueta I1/L1.
 * Tres tiempos: todas se condensan a la vez en un punto de su color, en su
 * sitio; los puntos viajan a su día, cada uno por su curva y con su retraso,
 * elegidos para que ninguna ruta roce a otra (el plan de vuelo.py, aquí
 * calculado una vez por horario); y cuando ya llegaron, cada punto florece
 * hasta su bloque, columna a columna. El choque florece el último, ya con su
 * corte. Un solo reloj en milisegundos; la escena lo lee al dibujar, así que
 * nada se recompone en cada cuadro.
 *
 * Todo se dibuja en un plano de 300 × 262 que se escala a dp.
 */

internal const val ESC_ANCHO = 300f
internal const val ESC_ALTO = 262f

private const val PT = 12f
private const val CONDENSA = 260f
private const val VIAJE = 520f
private const val FLORECE = 380f
private const val INSET = 9f
private val ARCOS = floatArrayOf(0f, 18f, -18f, 34f, -34f, 50f, -50f, 66f, -66f)

private fun tramo(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)

/** Cada clase con su sitio en la captura, su sitio en la semana y su plan de vuelo; `inicio` es cuándo se condensan, en ms del reloj */
internal class PlanoVuelo(todas: List<Sesion>, colores: List<Color>, val inicio: Float) {
    val sesiones = todas.filter { it.dia in 0..4 }
    val colores = todas.indices.filter { todas[it].dia in 0..4 }.map { colores[it] }
    val n = sesiones.size
    val origen = ArrayList<Rect>(n)
    val destino = ArrayList<Rect>(n)
    val carril = IntArray(n)
    val ay = FloatArray(n)
    val salida = FloatArray(n)
    val arco = FloatArray(n)
    val llegada = FloatArray(n)
    private val horas: Int

    /** Cuándo termina de florecer la última, desde `inicio` */
    val fin: Float

    /** Cuánto dura todo, con la línea de ahora incluida */
    val total: Float

    init {
        val (abre, cierra) = rangoDeClases(sesiones)
        horas = ((cierra - abre) / 60).coerceAtLeast(1)
        val hh = 228f / horas
        val tw = 254f / horas
        val colocadas = (0 until 5).flatMap { d -> enCarriles(sesiones.filter { it.dia == d }) }.associateBy { it.sesion.id }
        for ((i, s) in sesiones.withIndex()) {
            val c = colocadas.getValue(s.id)
            carril[i] = c.carril
            ay[i] = if (c.carril > 0) 0.75f else 0.5f
            val inset = INSET * c.carril
            destino += Rect(Offset(60f * s.dia + 3f + inset, 30f + (s.inicio - abre) / 60f * hh + 2f), Size(54f - inset, s.duracion / 60f * hh - 4f))
            val x = 38f + (s.inicio - abre) / 60f * tw
            val fila = 57f + s.dia * 39.2f
            val w = s.duracion / 60f * tw - 3.9f
            origen += if (c.carriles > 1) Rect(Offset(x, fila + c.carril * 18.1f), Size(w, 15.1f)) else Rect(Offset(x, fila), Size(w, 33.2f))
        }
        val a = origen.map { it.center }
        val b = destino.indices.map { Offset(destino[it].center.x, destino[it].top + destino[it].height * ay[it]) }
        val base = FloatArray(n) { CONDENSA - 60f + sesiones[it].dia * 30f }
        planear(a, b, base)
        val llegan = (salida.maxOrNull() ?: 0f) + VIAJE + 40f
        for (i in 0 until n) llegada[i] = llegan + sesiones[i].dia * 40f
        // El choque florece cuando la otra ya está en su sitio: se ve ya resuelto
        for (i in 0 until n) if (carril[i] > 0) {
            val antes = (0 until n).filter { sesiones[it].dia == sesiones[i].dia && carril[it] < carril[i] }
            if (antes.isNotEmpty()) llegada[i] = antes.maxOf { llegada[it] } + FLORECE * 0.55f
        }
        fin = (llegada.maxOrNull() ?: 0f) + FLORECE
        total = inicio + fin + 470f
    }

    /** Las líneas de hora de la semana */
    fun horasVisibles() = horas

    /** Retrasos y arcos por clase: se prueba al azar un cambio y se queda si no aumenta los roces */
    private fun planear(a: List<Offset>, b: List<Offset>, base: FloatArray) {
        if (n == 0) return
        val azar = Random(7)
        for (i in 0 until n) salida[i] = base[i]
        val ps = Array(n) { Offset.Zero }
        fun roces(): Int {
            var malos = 0
            var t = salida.min()
            val t1 = salida.max() + VIAJE
            while (t <= t1) {
                for (i in 0 until n) ps[i] = enCurva(a[i], b[i], arco[i], Movimiento.SIMETRICA.transform(tramo(t, salida[i], salida[i] + VIAJE)))
                for (i in 0 until n) for (j in i + 1 until n) {
                    if (abs(ps[i].x - ps[j].x) < PT + 3 && abs(ps[i].y - ps[j].y) < PT + 3) malos++
                }
                t += 8f
            }
            return malos
        }
        var coste = roces()
        var vueltas = 0
        while (coste > 0 && vueltas++ < 4000 && n > 1) {
            val i = azar.nextInt(n)
            val s0 = salida[i]
            val a0 = arco[i]
            if (azar.nextBoolean()) arco[i] = ARCOS[azar.nextInt(ARCOS.size)] else salida[i] = base[i] + 20f * azar.nextInt(13)
            val nuevo = roces()
            if (nuevo <= coste) coste = nuevo else { salida[i] = s0; arco[i] = a0 }
        }
    }
}

/** Un punto en su curva: la recta de a a b, arqueada hacia un lado o el otro */
private fun enCurva(a: Offset, b: Offset, arco: Float, v: Float): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val largo = hypot(dx, dy).takeIf { it > 0f } ?: 1f
    val cx = (a.x + b.x) / 2 - dy / largo * arco
    val cy = (a.y + b.y) / 2 + dx / largo * arco
    val u = 1 - v
    return Offset(u * u * a.x + 2 * u * v * cx + v * v * b.x, u * u * a.y + 2 * u * v * cy + v * v * b.y)
}

/** Lo que cambia de una escena a otra: la inicial arranca con su captura y su línea; la de lectura, con la foto real que se encoge */
internal class OpcionesEscena(
    val foto: ImageBitmap? = null,
    /** La captura entra y se lee con una línea antes de condensarse */
    val entrada: Boolean = false,
    /** Dónde cae la línea de ahora, en el plano; null si no se ve */
    val ahoraY: Float? = null,
    /** Si no es null, la foto se encoge hasta este rectángulo y se queda */
    val miniatura: Rect? = null,
)

/** El reloj de una escena: corre una vez, lineal; con el movimiento reducido salta al final */
@Composable
internal fun rememberRelojVuelo(total: Float, alAcabar: () -> Unit = {}): Animatable<Float, *> {
    val reloj = remember(total) { Animatable(0f) }
    LaunchedEffect(total) {
        if (Tema.movimiento) reloj.snapTo(total) else reloj.animateTo(total, tween(total.toInt(), easing = LinearEasing))
        alAcabar()
    }
    return reloj
}

@Composable
internal fun EscenaVuelo(plano: PlanoVuelo, opciones: OpcionesEscena, reloj: () -> Float, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val fuente = inter()
    val claro = Tema.claro
    val dias = remember(medidor, fuente, claro) {
        val estilo = TextStyle(fontFamily = fuente, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp, letterSpacing = 0.14.em, color = Paleta.tenue)
        listOf("L", "M", "X", "J", "V").map { medidor.medir(it, estilo) }
    }
    val filas = remember(medidor, fuente, claro) {
        val estilo = TextStyle(fontFamily = fuente, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = Paleta.apagada)
        listOf("L", "M", "X", "J", "V").map { medidor.medir(it, estilo) }
    }
    val d = LocalDensity.current.density
    Canvas(modifier.size(ESC_ANCHO.dp, ESC_ALTO.dp)) {
        withTransform({ scale(d, d, Offset.Zero) }) { escena(plano, opciones, reloj(), dias, filas) }
    }
}

/** Medir con densidad 1: el texto se dibuja dentro del plano escalado, así que 9 sp son 9 unidades del plano */
internal fun TextMeasurer.medir(texto: String, estilo: TextStyle): TextLayoutResult = measure(texto, estilo, density = Density(1f))

/** La captura de INTRADACE, gris y sin leer: cabecera, columnas de horas y una fila por día */
internal fun DrawScope.capturaFantasma(filas: List<TextLayoutResult>, alfa: Float) {
    val barra = Paleta.tinta.copy(alpha = 0.16f * alfa)
    drawRoundRect(barra, Offset(14f, 14f), Size(114f, 3f), CornerRadius(1.5f))
    drawRoundRect(barra.copy(alpha = 0.08f * alfa), Offset(14f, 26f), Size(66f, 3f), CornerRadius(1.5f))
    for (k in 0..8) {
        val x = 36f + 31.75f * k
        drawRect(Paleta.tinta.copy(alpha = 0.045f * alfa), Offset(x, 42f), Size(1f, 208f))
        if (k < 8) drawRoundRect(barra.copy(alpha = 0.09f * alfa), Offset(x + 4f, 45f), Size(14f, 3f), CornerRadius(1.5f))
    }
    for (k in 0..4) {
        drawRect(Paleta.tinta.copy(alpha = 0.06f * alfa), Offset(10f, 54f + 39.2f * k), Size(280f, 1f))
        texto(filas[k], Offset(14f, 68.6f + 39.2f * k), alfa)
    }
}

private fun DrawScope.texto(texto: TextLayoutResult, arriba: Offset, alfa: Float) {
    drawText(texto, topLeft = arriba, alpha = alfa)
}

/** La foto elegida, recortada para llenar un plano de ancho × alto */
internal fun DrawScope.fotoReal(foto: ImageBitmap, ancho: Float = ESC_ANCHO, alto: Float = ESC_ALTO) {
    val k = minOf(foto.width / ancho, foto.height / alto)
    val w = (ancho * k).toInt().coerceIn(1, foto.width)
    val h = (alto * k).toInt().coerceIn(1, foto.height)
    drawImage(foto, IntOffset((foto.width - w) / 2, (foto.height - h) / 2), IntSize(w, h), dstSize = IntSize(ancho.toInt(), alto.toInt()))
}

private fun DrawScope.escena(plano: PlanoVuelo, o: OpcionesEscena, t: Float, dias: List<TextLayoutResult>, filas: List<TextLayoutResult>) {
    val tl = t - plano.inicio
    val lienzo = Paleta.lienzo

    // La semana: etiquetas de día y una línea por hora, que aparecen cuando la captura se apaga
    val rejilla = Movimiento.FLUIDA.transform(tramo(tl, 50f, 500f))
    if (rejilla > 0f) {
        for (k in 0..4) texto(dias[k], Offset(60f * k + 30f - dias[k].size.width / 2f, 6f), rejilla)
        val horas = plano.horasVisibles()
        for (k in 0..horas) drawRect(Paleta.tinta.copy(alpha = 0.05f * rejilla), Offset(0f, 30f + 228f / horas * k), Size(ESC_ANCHO, 1f))
    }

    // La captura: entra, se lee con una línea y se apaga (o se encoge) dejando sus clases sueltas
    val entra = if (o.entrada) Movimiento.FLUIDA.transform(tramo(t, 0f, 450f)) else 1f
    val scan = if (o.entrada) 48f + Movimiento.SIMETRICA.transform(tramo(t, 400f, 1150f)) * 204f else -1f
    val sale = Movimiento.ESTANDAR.transform(tramo(tl, 0f, if (o.miniatura != null) 500f else 300f))
    if (o.miniatura != null) {
        val r = lerp(Rect(0f, 0f, ESC_ANCHO, ESC_ALTO), o.miniatura, sale)
        val k = r.width / ESC_ANCHO
        withTransform({ translate(r.left, r.top); scale(k, k, Offset.Zero) }) {
            tarjeta(o.foto, filas, 1f, 14f, 1f / k)
        }
    } else if (sale < 1f && entra > 0f) {
        withTransform({ translate(0f, (1 - entra) * 10f) }) { tarjeta(o.foto, filas, entra * (1 - sale), 14f, 1f) }
    }
    if (o.entrada) {
        val a = if (t > 370f && t < 1230f) minOf(1f, tramo(t, 370f, 470f)) * (1 - tramo(t, 1110f, 1230f)) else 0f
        if (a > 0f) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Paleta.tinta.copy(alpha = 0.10f * a)), startY = scan - 44f, endY = scan - 1f), Offset(0f, scan - 44f), Size(ESC_ANCHO, 43f))
            drawRect(Paleta.tinta.copy(alpha = 0.9f * a), Offset(0f, scan), Size(ESC_ANCHO, 1f))
        }
    }

    // Las clases, las del choque al final para que queden encima
    val orden = (0 until plano.n).sortedBy { plano.carril[it] }
    for (i in orden) {
        val orig = plano.origen[i]
        val dest = plano.destino[i]
        val color = plano.colores[i]
        val c = Movimiento.FLUIDA.transform(tramo(tl, 0f, CONDENSA))
        val floreciendo = tl >= plano.llegada[i]
        val b = Movimiento.FLUIDA.transform(tramo(tl, plano.llegada[i], plano.llegada[i] + FLORECE))
        fun punto(r: Rect, y: Float) = Rect(Offset(r.center.x - PT / 2, r.top + r.height * y - PT / 2), Size(PT, PT))
        val r = when {
            tl < plano.salida[i] -> lerp(orig, punto(orig, 0.5f), c)
            !floreciendo -> {
                val v = Movimiento.SIMETRICA.transform(tramo(tl, plano.salida[i], plano.salida[i] + VIAJE))
                val q = enCurva(punto(orig, 0.5f).center, punto(dest, plano.ay[i]).center, plano.arco[i], v)
                Rect(Offset(q.x - PT / 2, q.y - PT / 2), Size(PT, PT))
            }
            else -> lerp(punto(dest, plano.ay[i]), dest, b)
        }
        val radio = minOf(7f, minOf(r.width, r.height) / 3f)
        // Antes de condensarse es la clase gris de la captura; se enciende cuando la línea pasa por su fila
        val encendida = !o.entrada || tl >= 0f || scan > 54f + 39.2f * plano.sesiones[i].dia + 19.6f
        val gris = 1f - c
        if (!floreciendo && gris > 0f && (o.entrada || tl >= 0f)) {
            drawRoundRect(Paleta.tinta.copy(alpha = (if (encendida) 0.17f else 0.08f) * gris), r.topLeft, r.size, CornerRadius(radio))
            if (encendida) {
                drawRoundRect(Paleta.tinta.copy(alpha = 0.35f * gris), r.topLeft + Offset(.5f, .5f), Size(r.width - 1f, r.height - 1f), CornerRadius(radio), style = Stroke(1f))
            }
            val bar = Paleta.tinta.copy(alpha = (if (encendida) 0.5f else 0.17f) * gris)
            drawRoundRect(bar, r.topLeft + Offset(6f, 6f), Size((r.width - 12f) * 0.62f, 3f), CornerRadius(1.5f))
            drawRoundRect(bar.copy(alpha = bar.alpha * 0.6f), r.topLeft + Offset(6f, 13f), Size((r.width - 12f) * 0.38f, 3f), CornerRadius(1.5f))
        }
        val solido = if (!floreciendo) c else 1f - b
        if (solido > 0f && tl >= 0f) drawRoundRect(color.copy(alpha = solido), r.topLeft, r.size, CornerRadius(radio))
        if (floreciendo && b > 0f) bloque(r, color, plano.carril[i] > 0, b, lienzo)
    }

    // La línea de «ahora»: se traza al final, de izquierda a derecha
    val y = o.ahoraY
    if (y != null) {
        val ah = Movimiento.FLUIDA.transform(tramo(tl, plano.fin - 100f, plano.fin + 470f))
        if (ah > 0f) {
            drawRect(Paleta.rojo, Offset(0f, y), Size(ESC_ANCHO * ah, 1.5f))
            drawCircle(Paleta.rojo, 4f, Offset(1f, y + 0.75f))
        }
    }
}

private fun lerp(a: Rect, b: Rect, v: Float) = Rect(
    a.left + (b.left - a.left) * v, a.top + (b.top - a.top) * v,
    a.right + (b.right - a.right) * v, a.bottom + (b.bottom - a.bottom) * v,
)

internal fun DrawScope.tarjeta(foto: ImageBitmap?, filas: List<TextLayoutResult>, alfa: Float, radio: Float, filo: Float) {
    val forma = Path().apply { addRoundRect(RoundRect(0f, 0f, ESC_ANCHO, ESC_ALTO, CornerRadius(radio))) }
    drawRoundRect(Paleta.panel.copy(alpha = alfa), Offset.Zero, Size(ESC_ANCHO, ESC_ALTO), CornerRadius(radio))
    clipPath(forma) {
        if (foto != null) fotoReal(foto) else capturaFantasma(filas, alfa)
    }
    drawRoundRect(Paleta.tinta.copy(alpha = 0.07f * alfa), Offset(filo / 2, filo / 2), Size(ESC_ANCHO - filo, ESC_ALTO - filo), CornerRadius(radio), style = Stroke(filo))
}

/** La clase ya en su sitio: relleno tenue, canto de color a la izquierda y dos renglones; el choque, con el corte del color del fondo */
private fun DrawScope.bloque(r: Rect, color: Color, choque: Boolean, alfa: Float, lienzo: Color) {
    if (choque) drawRoundRect(lienzo.copy(alpha = alfa), r.topLeft - Offset(2f, 2f), Size(r.width + 4f, r.height + 4f), CornerRadius(9f))
    val forma = Path().apply { addRoundRect(RoundRect(r, CornerRadius(7f))) }
    clipPath(forma) {
        if (choque) drawRect(lienzo.copy(alpha = alfa), r.topLeft, r.size)
        drawRect(color.copy(alpha = 0.22f * alfa), r.topLeft, r.size)
        drawRect(color.copy(alpha = alfa), r.topLeft, Size(2.5f, r.height))
    }
    val ancho = r.width - 15f
    drawRoundRect(color.copy(alpha = 0.75f * alfa), r.topLeft + Offset(9f, 7f), Size(ancho * 0.7f, 3f), CornerRadius(1.5f))
    drawRoundRect(color.copy(alpha = 0.4f * alfa), r.topLeft + Offset(9f, 14f), Size(ancho * 0.45f, 3f), CornerRadius(1.5f))
}

/** Las letras L M X J V de las filas de la captura, medidas una vez por tema */
@Composable
internal fun rememberFilasCaptura(): List<TextLayoutResult> {
    val medidor = rememberTextMeasurer()
    val fuente = inter()
    val claro = Tema.claro
    return remember(medidor, fuente, claro) {
        val estilo = TextStyle(fontFamily = fuente, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = Paleta.apagada)
        listOf("L", "M", "X", "J", "V").map { medidor.medir(it, estilo) }
    }
}

/** La línea fina que lee la captura, con su estela hacia arriba */
internal fun DrawScope.lineaDeLectura(y: Float, alfa: Float = 1f) {
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Paleta.tinta.copy(alpha = 0.10f * alfa)), startY = y - 44f, endY = y - 1f), Offset(0f, y - 44f), Size(ESC_ANCHO, 43f))
    drawRect(Paleta.tinta.copy(alpha = 0.9f * alfa), Offset(0f, y), Size(ESC_ANCHO, 1f))
}
