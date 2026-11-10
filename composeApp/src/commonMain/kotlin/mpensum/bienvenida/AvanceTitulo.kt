package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import mpensum.componentes.Lucide
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/*
 * Pantalla 3, concepto C: «El título». Tu título, sobrio, en una hoja con su
 * doble filete: el sello con el birrete, la universidad, «otorga el título
 * de» y el nombre de la carrera. Debajo, la firma, que se va trazando
 * conforme avanza el %: lo aprobado la firma en tinta hasta el 41 %, con la
 * punta de la pluma donde vas; lo que falta se proyecta en trazo punteado
 * hasta el final mientras la fecha corre, y el sello se enciende con la del
 * grado. Queda viva la punta de la pluma, respirando.
 *
 * Mismo reloj y mismos datos que la tarjeta (RitmoAvance, GeometriaAvance);
 * la firma se corta de una medida hecha una vez, en trazos que se reutilizan.
 */

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** La hoja del título en el plano y lo que lleva, de arriba abajo */
private const val HX = 33f
private const val HY = 52f
private const val HW = 324f
private const val HH = 332f
private const val CX = HX + HW / 2
private val SELLO = Offset(CX, 98f)
private const val R_SELLO = 22f
private const val Y_UNIVERSIDAD = 132f
private const val Y_NUCLEO = 150f
private const val Y_OTORGA = 178f
private const val Y_CARRERA = 198f
/** La firma: su caja (220 × 44) y la línea sobre la que va */
private const val FIRMA_X = 85f
private const val FIRMA_Y = 246f
private const val ANCHO_FIRMA = 220f
private const val Y_LINEA = 296f
/** Abajo, lo aprobado a la izquierda y la fecha a la derecha */
private const val IZQ = HX + 24f
private const val DER = HX + HW - 24f
private const val BASE_CIFRA = 344f
private const val Y_UC = BASE_CIFRA + 8f
private const val BASE_FECHA = BASE_CIFRA

private val TRAZO_FIRMA = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val TRAZO_BIRRETE = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val HILO = Stroke(1f)
private val ARO = Stroke(1.5f)

/*
 * La toma: cerca de la firma mientras se traza, se aleja para ver la hoja
 * entera y se asienta de frente cuando el sello se enciende.
 *                      ms     x     y    zoom  incl  giro
 */
internal val TOMA_TITULO = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 195f, 212f, 1.06f, 10f, -1.0f),
        floatArrayOf(1_400f, 195f, 236f, 1.08f, 5f, -0.4f),
        floatArrayOf(3_600f, 195f, 228f, 1.03f, 3f, 0f),
        floatArrayOf(7_000f, 195f, 220f, 1.02f, 2f, 0.3f),
        floatArrayOf(8_800f, 195f, 218f, 1.03f, 1f, 0f),
        floatArrayOf(RitmoAvance.CICLO.toFloat(), 195f, 212f, 1.06f, 10f, -1.0f),
    ),
)

/** En modo ligero, la hoja de frente y en el centro */
internal val CENTRO_TITULO = Offset(195f, 218f)

/**
 * La firma de la demostración: una rúbrica de trazo continuo, hecha y medida
 * una vez. `hecha` y `falta` son los trozos que se cortan en cada cuadro; se
 * vacían y se rellenan, no se crean.
 */
internal class FirmaDemo {
    private val trazo = Path().apply {
        // En una caja de 240 × 44; se dibuja a 220 de ancho
        moveTo(0f, 32f)
        cubicTo(14f, 8f, 28f, 6f, 30f, 22f)
        cubicTo(32f, 36f, 22f, 44f, 18f, 34f)
        cubicTo(14f, 24f, 40f, 12f, 52f, 18f)
        cubicTo(62f, 23f, 56f, 36f, 66f, 34f)
        cubicTo(76f, 32f, 80f, 14f, 92f, 14f)
        cubicTo(104f, 14f, 98f, 36f, 110f, 34f)
        cubicTo(122f, 32f, 126f, 16f, 138f, 18f)
        cubicTo(150f, 20f, 146f, 38f, 160f, 34f)
        cubicTo(174f, 30f, 178f, 10f, 192f, 12f)
        cubicTo(204f, 14f, 200f, 34f, 214f, 32f)
        cubicTo(226f, 30f, 232f, 22f, 240f, 26f)
    }
    private val medida = PathMeasure().apply { setPath(trazo, false) }
    val largo = medida.length
    val hecha = Path()
    val falta = Path()
    /** Lo que falta se proyecta punteado: aún no está firmado */
    val punteado = Stroke(1.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4.5f), 0f))

    /** Corta en `destino` la firma de `desde` a `hasta` (en longitud) */
    fun cortar(destino: Path, desde: Float, hasta: Float) {
        destino.rewind()
        if (hasta > desde) medida.getSegment(desde, hasta, destino, true)
    }

    fun punto(en: Float): Offset = medida.getPosition(en)
}

internal fun DrawScope.pintarTitulo(t: Float, a: AvanceDemo, g: GeometriaAvance, x: TextosAvance, firma: FirmaDemo, respira: Boolean) {
    val velo = 1f - Movimiento.ESTANDAR.transform(tramo(t, RitmoAvance.SE_VA, RitmoAvance.SE_VA + 700f))
    if (velo <= 0.004f) return
    fun linea(en: Float) = Movimiento.ASIENTA.transform(tramo(t, en, en + 560f))

    // La hoja, con su doble filete de diploma
    val hoja = linea(RitmoAvance.NACE - 100f)
    val opHoja = hoja * velo
    val dyHoja = (1f - hoja) * 16f
    drawRoundRect(Paleta.panel, Offset(HX, HY + dyHoja), Size(HW, HH), CornerRadius(22f), alpha = opHoja)
    drawRoundRect(Paleta.tinta, Offset(HX + 0.5f, HY + dyHoja + 0.5f), Size(HW - 1f, HH - 1f), CornerRadius(22f), style = HILO, alpha = 0.08f * opHoja)
    drawRoundRect(Paleta.tinta, Offset(HX + 9f, HY + dyHoja + 9f), Size(HW - 18f, HH - 18f), CornerRadius(14f), style = HILO, alpha = 0.06f * opHoja)

    // El sello: su aro se traza al nacer; tenue hasta que llega la fecha del grado, y entonces se enciende
    val llega = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.LLEGA - 100f, RitmoAvance.LLEGA + 500f))
    val aro = ESTANDAR.transform(tramo(t, 300f, 1_100f))
    val sello = Offset(SELLO.x, SELLO.y + dyHoja)
    if (aro > 0f) {
        drawArc(
            Paleta.tinta, -90f, 360f * aro, false, Offset(sello.x - R_SELLO, sello.y - R_SELLO), Size(2 * R_SELLO, 2 * R_SELLO),
            style = ARO, alpha = mezcla(0.3f, 0.9f, llega) * velo,
        )
    }
    val onda = tramo(t, RitmoAvance.LLEGA, RitmoAvance.LLEGA + 900f)
    if (onda > 0f && onda < 1f) {
        drawCircle(Paleta.tinta, R_SELLO * (1f + 0.6f * Movimiento.ASIENTA.transform(onda)), sello, style = ARO, alpha = 0.4f * (1f - onda) * velo)
    }
    translate(sello.x - 10f, sello.y - 10f) {
        scale(20f / 24f, 20f / 24f, Offset.Zero) {
            Lucide.birrete.forEach { drawPath(it, Paleta.tinta, alpha = mezcla(0.3f, 1f, llega) * aro * velo, style = TRAZO_BIRRETE) }
        }
    }

    // Lo que dice la hoja, línea a línea
    val l1 = linea(400f)
    textoPlano(x.universidad, CX, Y_UNIVERSIDAD + dyHoja + (1f - l1) * 6f, Paleta.suave, l1 * velo, ancla = 0.5f)
    val l2 = linea(460f)
    textoPlano(x.nucleo, CX, Y_NUCLEO + dyHoja + (1f - l2) * 6f, Paleta.tenue, l2 * velo, ancla = 0.5f)
    val l3 = linea(560f)
    textoPlano(x.otorga, CX, Y_OTORGA + dyHoja + (1f - l3) * 6f, Paleta.tenue, l3 * velo, ancla = 0.5f)
    val l4 = linea(640f)
    textoPlano(x.carrera, CX, Y_CARRERA + dyHoja + (1f - l4) * 6f, Paleta.tinta, l4 * velo, ancla = 0.5f)

    // La línea de la firma, que se traza de izquierda a derecha
    val raya = ESTANDAR.transform(tramo(t, 700f, 1_300f))
    if (raya > 0f) drawRect(Paleta.tinta, Offset(IZQ + 18f, Y_LINEA + dyHoja), Size((DER - IZQ - 36f) * raya, 1f), alpha = 0.18f * velo)

    // La firma: lo aprobado en tinta, a la par de la cifra; lo que falta, punteado mientras corre la fecha
    val f = g.llenado(t)
    val hecho = firma.largo * a.porcentaje / 100f * f
    val proyecta = tramo(t, RitmoAvance.PROYECTA, RitmoAvance.LLEGA)
    val k = ANCHO_FIRMA / 240f
    translate(FIRMA_X, FIRMA_Y + dyHoja) {
        scale(k, k, Offset.Zero) {
            if (proyecta > 0f) {
                firma.cortar(firma.falta, hecho, mezcla(hecho, firma.largo, proyecta))
                drawPath(firma.falta, Paleta.tinta, alpha = 0.3f * velo, style = firma.punteado)
            }
            if (hecho > 0.5f) {
                firma.cortar(firma.hecha, 0f, hecho)
                drawPath(firma.hecha, Paleta.tinta, alpha = velo, style = TRAZO_FIRMA)
                // La punta de la pluma, donde vas: respira cuando ya firmó lo aprobado
                val fin = RitmoAvance.CUENTA + RitmoAvance.DURA_CUENTA + RitmoAvance.LLENA
                var pluma = 1f
                if (respira && t > fin) pluma = 0.65f + 0.35f * cos(2f * PI.toFloat() * (t - fin) / RitmoAvance.RESPIRA)
                drawCircle(Paleta.tinta, 2.6f / k, firma.punto(hecho), alpha = pluma * velo)
            }
        }
    }

    // Abajo a la izquierda, cuánto llevas: la cifra de 40 con su % y las UC
    val e = linea(RitmoAvance.CIFRA)
    val op = e * velo
    if (op > 0.004f) {
        val dy = (1f - e) * 8f + dyHoja
        val cifra = x.cifras40[(a.porcentaje * f).roundToInt().coerceIn(0, a.porcentaje)]
        textoPorBase(cifra, IZQ, BASE_CIFRA + dy, Paleta.tinta, op)
        textoPorBase(x.signo40, IZQ + anchoPlano(cifra) + 2f, BASE_CIFRA + dy, Paleta.tenue, op)
        val uc = x.ucs[(a.llevas * f).roundToInt().coerceIn(0, a.llevas)]
        textoPlano(uc, IZQ, Y_UC + dy, Paleta.tinta, op)
        textoPlano(x.deTotal, IZQ + anchoPlano(uc), Y_UC + dy, Paleta.tenue, op)
    }

    // Abajo a la derecha, cuándo: la fecha que corre y «Te gradúas»
    val sale = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.PROYECTA - 250f, RitmoAvance.PROYECTA + 300f))
    val opFecha = sale * velo
    if (opFecha > 0.004f) {
        val dy = (1f - sale) * 8f + dyHoja
        fechaQueCorre(t, x.fechas, DER, BASE_FECHA + dy, 1f, RitmoAvance.PROYECTA, RitmoAvance.LLEGA, opFecha)
        textoPlano(x.teGraduas, DER, BASE_FECHA + 8f + dy, Paleta.tenue, opFecha, ancla = 1f)
    }
}
