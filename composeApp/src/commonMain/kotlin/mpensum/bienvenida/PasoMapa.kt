package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.componentes.aspecto
import mpensum.componentes.iconoEstado
import mpensum.logica.Situacion
import mpensum.tema.Movimiento
import mpensum.tema.PESO_NOMBRE
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.avivar
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.plex
import mpensum.tema.porTema
import kotlin.math.max
import kotlin.math.min

/*
 * Pantalla 1: marca lo aprobado y mira qué se abre. Cinco tarjetas del mapa,
 * las de verdad (TarjetaMapa: 224 × 96, código, nombre, punto y UC, el icono
 * de su estado), en tres semestres. Se arma el mapa, un dedo aprueba
 * Matemáticas I, la descarga verde baja por sus cables y Física I y
 * Matemáticas II pasan a azul: «Puedes inscribirla». La cámara no para.
 *
 * Son pocas a propósito: el mapa entero en un teléfono no se lee, y aquí lo
 * que se vende es la idea. Todo va en un solo Canvas que lee el reloj al
 * dibujar: ni una recomposición por cuadro. Nada de esto se guarda.
 */

private const val CICLO = 10_000
/** Con las animaciones apagadas: el mapa ya cambiado, las dos abiertas con su rótulo */
private const val QUIETO = 7_400f

/** La historia, en ms del ciclo (Onb1 aprobado) */
private const val TOCA = 3_200f
private const val APRUEBA = 3_750f
private const val DESCARGA_EN = 4_300f
private const val ABRE = 5_450f
private const val ROTULO = 5_950f
private const val SE_VA = 9_100f

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val ANILLO = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val DESTELLO = CubicBezierEasing(0.22f, 0.61f, 0.2f, 1f)
private val DESCARGA = CubicBezierEasing(0.55f, 0f, 0.25f, 1f)
private val VIAJE_LUZ = CubicBezierEasing(0.45f, 0f, 0.3f, 1f)

private fun durante(t: Float, desde: Float, dura: Float) = ((t - desde) / dura).coerceIn(0f, 1f)

private const val ANCHO = 224f
private const val ALTO = 96f
/**
 * De una columna a la siguiente: la tarjeta y un pasillo de 92, más
 * estrecho que el del mapa (150). Así dos semestres enteros caben en el
 * encuadre abierto sin que la cámara tenga que alejarse hasta dejarlas
 * diminutas; los cables siguen leyéndose (su curva pide 44 como mínimo).
 */
private const val COLUMNA = ANCHO + 92f

/**
 * Una materia de Ingeniería de Petróleo, con el tono que le da su carrera en
 * el mapa. En claro, el mismo tono más profundo: los vivos se lavan sobre
 * blanco y los cables dejarían de verse.
 */
private class MateriaDemo(val codigo: String, val nombre: String, private val oscuro: Color, private val claro: Color, val col: Int, val fila: Int, val y: Float) {
    val x = col * COLUMNA
    val acento get() = porTema(oscuro, claro)
}

private val MATERIAS = listOf(
    MateriaDemo("0081814", "Matemáticas I", Color(0xFFFF7FB1), Color(0xFFC2326A), 0, 0, 61f),
    MateriaDemo("0051324", "Física I", Color(0xFF5DB5FF), Color(0xFF1B7FB0), 1, 0, 0f),
    MateriaDemo("0081824", "Matemáticas II", Color(0xFF996AFF), Color(0xFF6F42C9), 1, 1, 122f),
    MateriaDemo("0052134", "Física II", Color(0xFFFF78E7), Color(0xFFB02C9C), 2, 0, 0f),
    MateriaDemo("0082814", "Matemáticas III", Color(0xFF4FFFB8), Color(0xFF0F8A62), 2, 1, 122f),
)

/** Cada cable: de qué materia sale, a cuál llega y, si se abre al aprobar, en qué orden (−1 si no) */
private val CABLES = listOf(Triple(0, 1, 0), Triple(0, 2, 1), Triple(1, 3, -1), Triple(2, 4, -1))

/*
 * La toma: abre con los dos primeros semestres, se acerca a Matemáticas I
 * para el toque, sigue la descarga hasta lo que se abre y vuelve atrás. El
 * tablero abría a 0,54 con 19° de inclinación y las tarjetas se veían
 * diminutas: ahora el plano abierto encuadra los dos semestres enteros,
 * dentro del fundido, a 0,67 (un 24 % más cerca) y se inclina menos.
 *                      ms     x     y    zoom  incl  giro
 */
private val TOMA = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 272f, 106f, 0.67f, 13f, -1.6f),
        floatArrayOf(1_800f, 266f, 108f, 0.68f, 11f, -1.1f),
        floatArrayOf(3_300f, 134f, 109f, 1.08f, 7f, -0.3f),
        floatArrayOf(4_400f, 146f, 109f, 1.12f, 5f, 0f),
        floatArrayOf(5_600f, 422f, 109f, 1.00f, 5f, 0.3f),
        floatArrayOf(7_300f, 436f, 108f, 1.04f, 6f, 0.5f),
        floatArrayOf(8_800f, 274f, 106f, 0.68f, 11f, -1.1f),
        floatArrayOf(CICLO.toFloat(), 272f, 106f, 0.67f, 13f, -1.6f),
    ),
)

/** El hueco del tablero entre la escena y el título */
private val HUECO_TABLERO = 68.dp

@Composable
fun PasoMapa(alSeguir: () -> Unit) {
    val reloj = rememberRelojEscena(CICLO, QUIETO)
    val textos = rememberTextos()
    // Los degradados de los cables llevan los tonos del tema: se rearman si cambia
    val cables = remember(Tema.claro) { CABLES.map { (o, d, j) -> CableDemo(MATERIAS[o], MATERIAS[d], j) } }
    // El hueco del tablero entre la escena y el título: el fundido de abajo respira antes del texto
    PasoConEscena("Marca lo aprobado,", "mira qué se abre.", "Continuar", alSeguir, hueco = HUECO_TABLERO) { modifier ->
        EscenaEnBucle(reloj, TOMA, arriba = 0.17f, abajo = 0.2f, lados = 0.05f, modifier = modifier) { t ->
            pintar(t, textos, cables)
        }
    }
}

/** Los textos de la escena, medidos una vez: en cada cuadro solo se pintan */
private class TextosMapa(
    val codigos: List<TextLayoutResult>,
    val nombres: List<TextLayoutResult>,
    val uc: TextLayoutResult,
    val rotulo: TextLayoutResult,
    val semestres: List<TextLayoutResult>,
)

@Composable
private fun rememberTextos(): TextosMapa {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val j = jost()
    val i = inter()
    val p = plex()
    // El peso de los nombres cambia con el tema
    val claro = Tema.claro
    return remember(densidad, j, i, p, claro) { medir(medidor, densidad, j, i, p) }
}

private fun medir(medidor: TextMeasurer, d: Density, jost: FontFamily, inter: FontFamily, plex: FontFamily): TextosMapa {
    // En dp y no en sp: son parte del dibujo y escalan con la cámara, como en el mapa
    fun tam(v: Float) = with(d) { v.dp.toSp() }
    val mono = TextStyle(fontFamily = plex, fontWeight = FontWeight.Light, fontSize = tam(10f), letterSpacing = 0.04.em)
    val nombre = TextStyle(fontFamily = jost, fontWeight = PESO_NOMBRE, fontSize = tam(14f))
    val rotulo = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = tam(12f))
    val semestre = TextStyle(fontFamily = jost, fontWeight = FontWeight.Medium, fontSize = tam(11.5f), letterSpacing = 0.3.em)
    fun m(texto: String, estilo: TextStyle) = medidor.measure(texto, estilo, softWrap = false, density = d)
    return TextosMapa(
        codigos = MATERIAS.map { m(it.codigo, mono) },
        nombres = MATERIAS.map { m(it.nombre, nombre) },
        uc = m("4 UC", mono),
        rotulo = m("Puedes inscribirla", rotulo),
        semestres = (1..3).map { m("SEMESTRE 0$it", semestre) },
    )
}

/**
 * Un cable del mapa, de la derecha del origen a la izquierda del destino
 * (k = máx(0,45·dx, 44), como Geometria.kt). Ruta, medida y degradados se
 * arman una vez; en cada cuadro solo se recortan trozos del mismo Path.
 */
private class CableDemo(val origen: MateriaDemo, val destino: MateriaDemo, val orden: Int) {
    val inicio = Offset(origen.x + ANCHO, origen.y + ALTO / 2)
    val fin = Offset(destino.x, destino.y + ALTO / 2)
    val ruta = Path().apply {
        val k = max(0.45f * (fin.x - inicio.x), 44f)
        moveTo(inicio.x, inicio.y)
        cubicTo(inicio.x + k, inicio.y, fin.x - k, fin.y, fin.x, fin.y)
    }
    private val medida = PathMeasure().apply { setPath(ruta, false) }
    private val largo = medida.length
    private val trozo = Path()

    /** La luz de la frontera: sale con el color del origen, pasa por ese mismo girado medio tono y llega con el del destino */
    val frontera: Brush
    val viva: Brush

    init {
        val medio = girarTono(origen.acento, 48f)
        frontera = Brush.linearGradient(listOf(origen.acento, medio, destino.acento), inicio, fin)
        viva = Brush.linearGradient(listOf(avivar(origen.acento), avivar(medio), avivar(destino.acento)), inicio, fin)
    }

    /** El trozo entre las fracciones `a` y `b` del largo; reutiliza siempre el mismo Path */
    fun entre(a: Float, b: Float): Path {
        trozo.reset()
        medida.getSegment(largo * a.coerceIn(0f, 1f), largo * b.coerceIn(0f, 1f), trozo, true)
        return trozo
    }
}

/** El tono girado en HSL, el mismo cálculo que el mapa para la luz de la frontera */
private fun girarTono(color: Color, grados: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    if (mx == mn) return color
    val l = (mx + mn) / 2
    val d = mx - mn
    val s = if (l > 0.5f) d / (2 - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } * 60f
    return Color.hsl((h + grados) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/** El dedo se posa aquí, sobre Matemáticas I: un poco a la derecha del nombre, donde cae el pulgar */
private val DEDO = Offset(154f, 114f)

/** Un texto medido, con su línea base en (x, base) del plano; `derecha` lo alinea por su final */
private fun DrawScope.texto(t: TextLayoutResult, x: Float, base: Float, color: Color, alfa: Float, derecha: Boolean = false) {
    if (alfa <= 0.004f) return
    val px = 1f / density
    withTransform({
        translate(x, base)
        scale(px, px, Offset.Zero)
    }) { drawText(t, color, Offset(if (derecha) -t.size.width.toFloat() else 0f, -t.firstBaseline), alpha = alfa.coerceIn(0f, 1f)) }
}

private fun DrawScope.pintar(t: Float, x: TextosMapa, cables: List<CableDemo>) {
    // Al final de cada vuelta todo se apaga a la vez: la vuelta siguiente arranca de cero, sin salto
    val velo = 1f - Movimiento.ESTANDAR.transform(durante(t, SE_VA, 700f))

    // Las cabeceras de semestre, como RotulosGrafo: aro tenue y mayúsculas espaciadas
    for (c in 0..2) {
        val op = entrada(t, c, 0) * velo * 0.9f
        if (op <= 0f) continue
        val x0 = c * COLUMNA
        drawCircle(Paleta.tinta.copy(alpha = 0.42f * op), 6f, Offset(x0 + 7f, -84f + 9.8f), style = Stroke(1.6f))
        texto(x.semestres[c], x0 + 21f, -84f + 14f, Paleta.tinta, op)
    }

    for (c in cables) cable(t, c, velo)
    MATERIAS.forEachIndexed { i, m -> tarjeta(t, i, m, x, velo) }

    // El dedo: un círculo de luz, sin sombra; se posa, aprieta y se va. Vive en el plano y viaja con la cámara
    val aparece = Movimiento.ASIENTA.transform(durante(t, TOCA, 380f))
    val aprieta = Movimiento.ESTANDAR.transform(durante(t, 3_580f, 140f)) - Movimiento.ESTANDAR.transform(durante(t, 3_740f, 240f))
    val seVa = Movimiento.ESTANDAR.transform(durante(t, 3_820f, 360f))
    val opDedo = aparece * (1f - seVa)
    if (opDedo > 0f) {
        val r = 22f * ((1.18f - 0.18f * aparece) * (1f - 0.12f * aprieta) + 0.12f * seVa)
        drawCircle(Paleta.tinta.copy(alpha = 0.16f * opDedo), r, DEDO)
        drawCircle(Paleta.tinta.copy(alpha = 0.55f * opDedo), r - 0.75f, DEDO, style = Stroke(1.5f))
    }
}

/** Cuánto ha entrado lo de una columna y fila: semestre a semestre y, dentro, de arriba abajo */
private fun entrada(t: Float, col: Int, fila: Int) = Movimiento.ASIENTA.transform(durante(t, 150f + col * 240f + fila * 90f, 650f))

private fun DrawScope.cable(t: Float, c: CableDemo, velo: Float) {
    val o = c.origen
    // El cable crece detrás de su tarjeta mientras el mapa se arma
    val dibujo = Movimiento.ESTANDAR.transform(durante(t, 150f + o.col * 240f + o.fila * 90f + 380f, 800f))
    var frontera = 0f
    var lleno = 0f
    var descarga = 0f
    var luz = 0f
    var luzDesde = 0f
    var luzHasta = 0f
    if (c.orden >= 0) {
        val j = c.orden
        frontera = Movimiento.ESTANDAR.transform(durante(t, ABRE - 50f + j * 120f, 500f))
        // La descarga: el verde llena el cable en el 58 % del tiempo, se queda un instante y se apaga (2 s: a cámara lenta)
        val r = durante(t, DESCARGA_EN, 2_000f)
        if (r > 0f && r < 1f) {
            val p = DESCARGA.transform(r)
            lleno = min(1f, p / 0.58f)
            descarga = if (p < 0.58f) 1f else 1f - (p - 0.58f) / 0.42f
        }
        // La luz de la frontera: un guion del 9 % que nace y muere apagado, cada cable a su ritmo
        val inicio = ROTULO + j * 700f
        val dura = if (j == 0) 2_600f else 3_100f
        if (t >= inicio) {
            val bruto = ((t - inicio) / dura) % 1f
            val p = VIAJE_LUZ.transform(bruto)
            val alfa = if (bruto < 0.14f) bruto / 0.14f else if (bruto > 0.82f) (1f - bruto) / 0.18f else 1f
            val desde = p * 1.09f - 0.09f
            luzDesde = max(0f, desde)
            luzHasta = min(1f, desde + 0.09f)
            luz = alfa * frontera
        }
    }
    val base = 0.12f * (1f - frontera) * velo
    if (base > 0f && dibujo > 0f) drawPath(c.entre(0f, dibujo), o.acento.copy(alpha = base), style = Stroke(1f, cap = StrokeCap.Round))
    if (frontera > 0f) drawPath(c.ruta, c.frontera, alpha = 0.55f * frontera * velo, style = Stroke(1.75f, cap = StrokeCap.Round))
    if (descarga > 0f) {
        val trozo = c.entre(0f, lleno)
        drawPath(trozo, Paleta.verde.copy(alpha = 0.18f * descarga * velo), style = Stroke(9f, cap = StrokeCap.Round))
        drawPath(trozo, Paleta.verde.copy(alpha = descarga * velo), style = Stroke(2.5f, cap = StrokeCap.Round))
    }
    if (luz > 0f && luzHasta > luzDesde) drawPath(c.entre(luzDesde, luzHasta), c.viva, alpha = luz * velo, style = Stroke(2.25f, cap = StrokeCap.Round))
    if (frontera > 0f) drawCircle(c.destino.acento.copy(alpha = 0.9f * frontera * velo), 3f, c.fin)
}

/** El sitio y la forma del icono de estado, los de TarjetaMapa */
private const val X_ICONO = 190.5f
private const val Y_ICONO = 10.5f
private const val LADO_ICONO = 17.5f
private val RADIO = CornerRadius(7f)
private val CENTRO = Offset(ANCHO / 2, ALTO / 2)

private fun DrawScope.tarjeta(t: Float, i: Int, m: MateriaDemo, x: TextosMapa, velo: Float) {
    val e = entrada(t, m.col, m.fila)
    val op = e * velo
    if (op <= 0.004f) return
    var de = Situacion.LEJANA
    var a = Situacion.LEJANA
    var k = 0f
    var candado = Paleta.rojo
    var giro = 0f
    var candadoA = 1f
    var candadoE = 1f
    var selloA = 0f
    var selloE = 0.6f
    var rotuloA = 0f
    var rotuloY = 0f
    var anilloA = 0f
    var anilloG = 2f
    var destelloA = 0f
    var destelloX = 1f
    var destelloY = 1f
    var esc = 1f
    if (i == 0) {
        // La que se aprueba: la única en azul al principio, verde al toque
        de = Situacion.INSCRIBIBLE
        a = Situacion.HECHA
        k = Movimiento.ESTANDAR.transform(durante(t, APRUEBA, 380f))
        candado = Paleta.azul
        giro = 40f
        candadoA = 1f - durante(t, APRUEBA, 180f)
        candadoE = 1f - 0.2f * durante(t, APRUEBA, 180f)
        selloA = Movimiento.ASIENTA.transform(durante(t, APRUEBA, 300f))
        selloE = 0.6f + 0.4f * Movimiento.ESTANDAR.transform(durante(t, APRUEBA, 450f))
        rotuloA = 1f - ESTANDAR.transform(durante(t, APRUEBA - 50f, 220f))
        val r = durante(t, APRUEBA - 50f, 620f)
        if (r > 0f && r < 1f) {
            val p = ANILLO.transform(r)
            anilloA = 0.7f * (1f - p)
            anilloG = 2f + 12f * p
        }
        // Se hunde un poco bajo el dedo, como un botón
        val hundido = Movimiento.ESTANDAR.transform(durante(t, 3_600f, 120f)) - Movimiento.ESTANDAR.transform(durante(t, 3_740f, 260f))
        esc = 1f - 0.025f * hundido
    } else if (i <= 2) {
        // Lo que abre: el candado gira abierto y pasa de rojo a azul, un destello y luego el rótulo
        val j = i - 1
        a = Situacion.INSCRIBIBLE
        k = Movimiento.ESTANDAR.transform(durante(t, ABRE + j * 120f, 450f))
        candado = lerp(Paleta.rojo, Paleta.azul, k)
        giro = 40f * k
        val r = durante(t, ABRE + 150f + j * 120f, 1_100f)
        if (r > 0f && r < 1f) {
            val p = DESTELLO.transform(r)
            destelloA = (if (r < 0.18f) 0.8f * (r / 0.18f) else 0.8f * (1f - (p - 0.18f) / 0.82f)).coerceIn(0f, 1f)
            destelloX = 1f + 0.06f * p
            destelloY = 1f + 0.28f * p
        }
        val f = Movimiento.ASIENTA.transform(durante(t, ROTULO + j * 120f, 500f))
        rotuloA = f
        rotuloY = (1f - f) * 4f
    }
    val asDe = aspecto(de)
    val asA = aspecto(a)
    val fondo = lerp(asDe.fondo, asA.fondo, k)
    val borde = lerp(asDe.borde, asA.borde, k)
    val g = asDe.grosor + (asA.grosor - asDe.grosor) * k

    // Entra subiendo 22 dp mientras aparece
    withTransform({
        translate(m.x, m.y + (1f - e) * 22f)
        scale(esc, esc, CENTRO)
    }) {
        drawRoundRect(fondo.copy(alpha = fondo.alpha * op), size = Size(ANCHO, ALTO), cornerRadius = RADIO)
        drawRoundRect(borde.copy(alpha = borde.alpha * op), Offset(g / 2, g / 2), Size(ANCHO - g, ALTO - g), RADIO, style = Stroke(g))
        // El anillo del toque se abre desde el canto, como en el mapa
        if (anilloA > 0f) drawRoundRect(Paleta.tinta.copy(alpha = anilloA * op), size = Size(ANCHO, ALTO), cornerRadius = RADIO, style = Stroke(anilloG))
        if (destelloA > 0f) {
            scale(destelloX, destelloY, CENTRO) {
                drawRoundRect(Paleta.azul.copy(alpha = destelloA * op), size = Size(ANCHO, ALTO), cornerRadius = RADIO, style = Stroke(1.5f))
            }
        }
        texto(x.codigos[i], 18f, 22f, Paleta.tenue, op)
        texto(x.nombres[i], 18f, 55.3f, lerp(asDe.nombre, asA.nombre, k), op)
        drawCircle(m.acento.copy(alpha = op), 3f, Offset(21f, 80f))
        texto(x.uc, 30f, 83.5f, Paleta.tenue, op)
        if (rotuloA > 0f) texto(x.rotulo, 206f, 83.5f + rotuloY, Paleta.azul, rotuloA * op, derecha = true)
        // El sello verde crece desde el centro del icono; el candado se encoge y se va
        if (selloA > 0f) {
            scale(selloE, selloE, Offset(X_ICONO + LADO_ICONO / 2, Y_ICONO + LADO_ICONO / 2)) {
                iconoEstado(Situacion.HECHA, X_ICONO, Y_ICONO, LADO_ICONO, fondo, Paleta.verde.copy(alpha = selloA * op))
            }
        }
        if (candadoA > 0f) {
            scale(candadoE, candadoE, Offset(X_ICONO + LADO_ICONO / 2, Y_ICONO + LADO_ICONO * 0.6f)) {
                iconoEstado(Situacion.LEJANA, X_ICONO, Y_ICONO, LADO_ICONO, fondo, candado.copy(alpha = candadoA * op), giro)
            }
        }
    }
}
