package mpensum.componentes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/*
 * El logo de la app (docs/marca/logo-app-2026-11.png): un mapa de papel blanco
 * plegado en tres paneles (el del centro en sombra) y una ruta azul de tres
 * puntos que acaba en una meta en aro. Las medidas son las del PNG de 1024 px,
 * para cotejarlo con él, y los colores están muestreados del PNG.
 *
 * El logo es siempre el mismo, en oscuro y en claro: nunca se invierte. Va
 * dentro de su mosaico (el azul noche del icono), que lo separa de los dos
 * fondos. Es el mismo dibujo que el icono del teléfono
 * (androidMain/res/drawable/ic_launcher_foreground.xml). Todo se calcula una
 * vez por tamaño (drawWithCache) y se dibuja sin capas: nada de desenfoques.
 */

/** La esquina de la caja del mapa en el PNG: va de x 218 a 805 y de y 238 a 805 */
private const val X0 = 218f
private const val Y0 = 238f

/** El lado del PNG: el mosaico es el icono entero, con el mapa donde está en él */
private const val LADO_PNG = 1024f

/** Los tres paneles: arriba sube, baja y sube (el zigzag del pliegue), abajo igual */
private val PANELES = listOf(
    listOf(218f to 300f, 410f to 238f, 410f to 740f, 218f to 805f),
    listOf(410f to 238f, 605f to 300f, 605f to 805f, 410f to 740f),
    listOf(605f to 300f, 805f to 238f, 805f to 740f, 605f to 805f),
)

/** La ruta: sale abajo a la izquierda, cruza los dos pliegues y sube a la meta */
private val RUTA = listOf(305f to 688f, 410f to 556f, 512f to 530f, 608f to 505f, 720f to 377f)

// Los colores, muestreados del PNG
private val PAPEL = Color(0xFFFFFFFF)
private val PAPEL_BAJO = Color(0xFFF1F3F6)
private val CENTRO_CLARO = Color(0xFFDDE1E7)
private val CENTRO_OSCURO = Color(0xFFC3C9D3)
private val DERECHO_PLIEGUE = Color(0xFFD9DDE4)
private val PLIEGUE_LUZ = Color(0xFFFFFFFF)
private val PLIEGUE_SOMBRA = Color(0xFFB9C0CB)
private val RUTA_INICIO = Color(0xFF1E40A6)
private val RUTA_MEDIO = Color(0xFF254BBE)
private val RUTA_FIN = Color(0xFF4F84F5)
private val META = Color(0xFF649AFE)

/** El azul noche del icono: más claro arriba y casi negro abajo, como el fondo del PNG */
private val NOCHE_ARRIBA = Color(0xFF2B3955)
private val NOCHE_MEDIO = Color(0xFF19213A)
private val NOCHE_ABAJO = Color(0xFF0A0C14)

/** El exponente de la curva del mosaico: 4 da la esquina continua y generosa de un icono de iOS */
private const val CURVA_MOSAICO = 4.0

/**
 * El mosaico: el icono de la app tal cual, un squircle en el azul noche con el
 * mapa donde está en el PNG. Es el mismo objeto que el icono del teléfono.
 * Con `canto`, el filo de arriba recoge un poco de luz y el de abajo apenas,
 * como el canto de un icono de iOS, y se apoya con una sombra corrida; sin
 * él, un filo blanco al 8 % parejo.
 */
@Composable
fun LogoMosaico(tam: Dp, modifier: Modifier = Modifier, canto: Boolean = false) {
    Spacer(modifier.size(tam).semantics { contentDescription = "Mapa de Pensum" }.drawWithCache { dibujoMosaico(canto) })
}

private fun CacheDrawScope.dibujoMosaico(canto: Boolean): DrawResult {
    val lado = size.minDimension
    val forma = squircle(Rect(Offset.Zero, Size(lado, lado)))
    val fondo = Brush.verticalGradient(0f to NOCHE_ARRIBA, 0.55f to NOCHE_MEDIO, 1f to NOCHE_ABAJO, startY = 0f, endY = lado)
    val e = lado / LADO_PNG
    val mapa = Mapa(this, Offset(X0 * e, Y0 * e), e)
    val g = max(1f, 0.75.dp.toPx())
    val filo: Brush = if (canto) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.42f), 0.3f to Color.White.copy(alpha = 0.08f), 0.75f to Color.Transparent, 1f to Color.White.copy(alpha = 0.05f), endY = lado)
    } else SolidColor(Color.White.copy(alpha = 0.08f))
    val apoyo = 1.5.dp.toPx()
    return onDrawBehind {
        // Con canto, el icono se apoya: su misma forma corrida 1,5 dp hacia abajo, sin desenfoque
        if (canto) translate(0f, apoyo) { drawPath(forma, Color.Black.copy(alpha = 0.16f)) }
        drawPath(forma, fondo)
        mapa.dibujar(this)
        drawPath(forma, filo, style = Stroke(g))
    }
}

/** Una superelipse que llena `r`: la esquina continua de los iconos de Apple, sin el quiebre de un arco */
private fun squircle(r: Rect): Path = Path().apply {
    val a = r.width / 2f
    val b = r.height / 2f
    val c = r.center
    val pasos = 96
    for (i in 0..pasos) {
        val t = 2 * PI * i / pasos
        val ct = cos(t)
        val st = sin(t)
        val x = c.x + a * sign(ct).toFloat() * abs(ct).pow(2.0 / CURVA_MOSAICO).toFloat()
        val y = c.y + b * sign(st).toFloat() * abs(st).pow(2.0 / CURVA_MOSAICO).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** El mapa ya medido a una escala `e` (px por px del PNG), con su esquina de arriba a la izquierda en `origen` */
private class Mapa(scope: CacheDrawScope, origen: Offset, e: Float) {
    private val paneles: List<Path>
    private val brochas: List<Brush>
    private val pliegues: List<Pair<Offset, Offset>>
    private val grosorPliegue: Float
    private val ruta: Path
    private val brochaRuta: Brush
    private val trazo: Stroke
    private val puntos: List<Pair<Offset, Size>>
    private val meta: Offset
    private val ladoMeta: Size

    init {
        with(scope) {
            fun q(x: Float, y: Float) = Offset(origen.x + (x - X0) * e, origen.y + (y - Y0) * e)
            paneles = PANELES.map { poligonoRedondeado(it.map { (x, y) -> q(x, y) }, 14f * e) }
            // El izquierdo, papel que cae de blanco; el del centro, en sombra y más oscuro hacia el segundo pliegue;
            // el derecho, gris en el pliegue y blanco en cuanto se aleja de él
            brochas = listOf(
                Brush.verticalGradient(listOf(PAPEL, PAPEL_BAJO), q(0f, 238f).y, q(0f, 805f).y),
                Brush.horizontalGradient(listOf(CENTRO_CLARO, CENTRO_OSCURO), q(410f, 0f).x, q(605f, 0f).x),
                Brush.horizontalGradient(listOf(DERECHO_PLIEGUE, PAPEL), q(605f, 0f).x, q(675f, 0f).x),
            )
            // Los pliegues: una línea de luz en el canto del panel izquierdo y una de sombra en el segundo
            pliegues = listOf(q(404f, 250f) to q(404f, 734f), q(607f, 308f) to q(607f, 798f))
            grosorPliegue = max(5f * e, 0.5.dp.toPx())

            ruta = Path().apply {
                RUTA.forEachIndexed { i, (x, y) -> q(x, y).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
            }
            // La ruta nunca más fina que 1,2 dp: a 20 dp de mapa el trazo del PNG se quedaría en un pelo
            trazo = Stroke(max(22f * e, 1.2.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)
            brochaRuta = Brush.linearGradient(
                0f to RUTA_INICIO, 0.5f to RUTA_MEDIO, 1f to RUTA_FIN,
                start = q(RUTA.first().first, RUTA.first().second), end = q(RUTA.last().first, RUTA.last().second),
            )
            // Los puntos son óvalos, como en el PNG (el mapa está en perspectiva), y no bajan de un tamaño legible
            val m = 1.dp.toPx()
            puntos = listOf(
                q(305f, 688f) to Size(max(68f * e, 3.2f * m), max(56f * e, 2.6f * m)),
                q(512f, 530f) to Size(max(76f * e, 3.6f * m), max(64f * e, 3f * m)),
            )
            meta = q(720f, 377f)
            ladoMeta = Size(max(114f * e, 5.4f * m), max(100f * e, 4.7f * m))
        }
    }

    fun dibujar(d: DrawScope) = with(d) {
        paneles.forEachIndexed { i, panel -> drawPath(panel, brochas[i]) }
        drawLine(PLIEGUE_LUZ, pliegues[0].first, pliegues[0].second, grosorPliegue)
        drawLine(PLIEGUE_SOMBRA.copy(alpha = 0.7f), pliegues[1].first, pliegues[1].second, grosorPliegue)
        drawPath(ruta, brochaRuta, style = trazo)
        ovalo(puntos[0].first, puntos[0].second, RUTA_INICIO)
        ovalo(puntos[1].first, puntos[1].second, RUTA_MEDIO)
        // La meta: el aro claro, el hueco de papel y el punto del centro
        ovalo(meta, ladoMeta, META)
        ovalo(meta, ladoMeta * 0.56f, PAPEL)
        ovalo(meta, ladoMeta * 0.26f, META)
    }

    /** Un óvalo girado con la ruta, unos 20°, como en el dibujo */
    private fun DrawScope.ovalo(c: Offset, s: Size, color: Color) {
        rotate(-20f, c) { drawOval(color, Offset(c.x - s.width / 2f, c.y - s.height / 2f), s) }
    }
}

/** Un polígono con las esquinas suavizadas: cada vértice se cambia por una curva que empieza y acaba a `radio` de él */
private fun poligonoRedondeado(puntos: List<Offset>, radio: Float): Path = Path().apply {
    val n = puntos.size
    fun haciaDe(a: Offset, b: Offset, d: Float): Offset {
        val largo = hypot(b.x - a.x, b.y - a.y)
        val f = if (largo == 0f) 0f else minOf(d / largo, 0.5f)
        return Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
    }
    for (i in 0 until n) {
        val v = puntos[i]
        val antes = haciaDe(v, puntos[(i - 1 + n) % n], radio)
        val despues = haciaDe(v, puntos[(i + 1) % n], radio)
        if (i == 0) moveTo(antes.x, antes.y) else lineTo(antes.x, antes.y)
        quadraticTo(v.x, v.y, despues.x, despues.y)
    }
    close()
}
