package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.datos.Pensum
import mpensum.datos.rememberPensum
import mpensum.logica.Marca
import mpensum.logica.redondear
import mpensum.logica.resumenParaPortada
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.exp
import kotlin.math.min

/*
 * Paso 3: cuánto llevas y cuándo te gradúas, con un estudiante de Sistemas
 * que va por el quinto semestre. La silueta de la carrera se enciende en
 * verde semestre a semestre mientras la cifra suma sus UC de verdad; lo que
 * cursa late a medio llenar; lo que falta se traza en anillos, un semestre
 * por período, hasta la meta, que sube su asta al birrete y dice cuántos
 * semestres quedan. Una cámara de toma continua acompaña la historia.
 *
 * Todo sale de un solo reloj y se pinta en un Canvas, con los textos medidos
 * una vez: cada cuadro solo redibuja. Las medidas son las del tablero
 * aprobado (Onb3), en dp, sobre una escena de 390 × 468.
 */

private const val CARRERA = "ingenieria-de-sistemas"
/** El estudiante de la demostración: aprobó los cuatro primeros semestres y cursa dos materias del quinto */
private const val SEMESTRES_APROBADOS = 4
private const val MATERIAS_CURSANDO = 2

/** La historia, en ms del ciclo */
private const val CICLO = 10_000L
private const val NACE = 200f
private const val CIFRA = 900f
private const val ENCIENDE = 1_500f
private const val POR_SEMESTRE = 520f
private const val AHORA = 3_800f
private const val PROYECTA = 4_500f
private const val LLEGA = 7_300f
private const val SE_VA = 9_150f
/** Con las animaciones del sistema apagadas, la escena se queda en su final: la meta y su respuesta */
private const val QUIETO = 8_700f
/** El latido de lo que cursas: el mismo ritmo del punto «En curso» del horario */
private const val LATIDO = 2_600f

/** La escena del tablero y su centro, adonde mira la cámara */
private const val ESCENA_ANCHO = 390f
private const val ESCENA_ALTO = 468f
/** Del borde de abajo de la escena al título, como en el tablero */
private const val HUECO_TITULO = 68f

private val ENFATICA = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SALIDA = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val DESTELLO = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
/** La marcha del riel por lo que falta: arranca y llega suave, a ritmo parejo en medio */
private val MARCHA = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

private val TENUE get() = Paleta.tinta.copy(alpha = 0.16f)
/** El punto sin encender: grafito en oscuro, gris de papel en claro */
private val NODO get() = porTema(Color(0xFF2A2E34), Color(0xFFD5D7DB))

/** Lo que cuenta la escena, sacado del pensum con la misma lógica que la portada: nada escrito a mano */
private class AvanceDemo(
    /** Materias obligatorias por semestre: la silueta de la carrera */
    val silueta: List<Int>,
    /** Las UC que suma cada semestre aprobado, en orden */
    val ucPorSemestre: List<Int>,
    val cursando: Int,
    val porcentaje: Int,
    val llevas: Int,
    val de: Int,
    val unidad: String,
) {
    val hechos get() = ucPorSemestre.size
    /** Los semestres que faltan, contando el que cursas */
    val faltan get() = silueta.size - hechos
}

private fun avanceDeDemostracion(pensum: Pensum): AvanceDemo {
    val obligatorias = pensum.obligatorias
    val marcas = buildMap<String, String> {
        obligatorias.filter { (it.semestre ?: 99) <= SEMESTRES_APROBADOS }.forEach { put(it.codigo, Marca.APROBADA.valor) }
        obligatorias.filter { it.semestre == SEMESTRES_APROBADOS + 1 }.take(MATERIAS_CURSANDO).forEach { put(it.codigo, Marca.CURSANDO.valor) }
    }
    val resumen = resumenParaPortada(pensum, marcas)
    val silueta = pensum.semestres.map { it.cantidad }
    // Los semestres hechos son los que la portada da por terminados: los anteriores al que vas
    val hechos = (resumen.semestreActual(silueta) ?: (silueta.size + 1)) - 1
    val ucPorSemestre = (1..hechos).map { s ->
        obligatorias.filter { it.semestre == s && marcas[it.codigo] == Marca.APROBADA.valor }.sumOf { it.uc ?: 0 }
    }
    return AvanceDemo(silueta, ucPorSemestre, resumen.cursando, resumen.porcentaje, resumen.llevas, resumen.de, resumen.unidad)
}

/** Letra de la escena (dibujo, en unidades del tablero)  */
private val TAM_CIFRA = 88.sp
private val TAM_FALTAN = 80.sp
private val TAM_SIGNO = 26.sp

/** Lo que se funde la escena con el fondo en cada borde */
private val FUNDIDO_ARRIBA = 37.dp
private val FUNDIDO_ABAJO = 47.dp
private val FUNDIDO_LADO = 23.dp

@Composable
fun PasoAvance(alSeguir: () -> Unit) {
    val pensum = rememberPensum(CARRERA)
    val avance = remember(pensum) { pensum?.let(::avanceDeDemostracion) }
    PasoConEscena("Cuánto llevas,", "cuándo te gradúas.", "Continuar", alSeguir) { modifier ->
        Box(
            modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Sin recuadro: la escena se funde con el fondo hacia los cuatro lados
                    val arriba = FUNDIDO_ARRIBA.toPx() / size.height
                    val abajo = FUNDIDO_ABAJO.toPx() / size.height
                    val lado = FUNDIDO_LADO.toPx() / size.width
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, arriba to Color.Black, 1f - abajo to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, lado to Color.Black, 1f - lado to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            if (avance != null) Escena(avance)
        }
    }
}

/** Los textos de la escena, medidos una vez en unidades de dp: se dibujan dentro de la cámara */
private class TextosAvance(
    val cifras: List<TextLayoutResult>,
    val signo: TextLayoutResult,
    val aprobado: TextLayoutResult,
    val ucs: List<TextLayoutResult>,
    val deTotal: TextLayoutResult,
    val numeros: List<TextLayoutResult>,
    val teGraduas: TextLayoutResult,
    val faltan: TextLayoutResult,
    val semestres: TextLayoutResult,
)

private fun medirTextos(a: AvanceDemo, medidor: TextMeasurer, jost: FontFamily, inter: FontFamily): TextosAvance {
    val cifra = TextStyle(fontFamily = jost, fontWeight = FontWeight.ExtraLight, fontSize = TAM_CIFRA, letterSpacing = (-0.035).em, color = Paleta.tinta, fontFeatureSettings = "tnum")
    val versal = TextStyle(fontFamily = jost, fontWeight = FontWeight.Medium, fontSize = Tipo.xxs, letterSpacing = 0.14.em, color = Paleta.suave)
    val numero = TextStyle(fontFamily = jost, fontWeight = FontWeight.Light, fontSize = Tipo.s, color = Paleta.apagada, fontFeatureSettings = "tnum")
    val uc = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = Tipo.s, color = Paleta.tinta, fontFeatureSettings = "tnum")
    return TextosAvance(
        // Una por cada valor que la cuenta puede tocar: contar no mide nada
        cifras = (0..a.porcentaje).map { medidor.measure("$it", cifra) },
        signo = medidor.measure("%", TextStyle(fontFamily = jost, fontWeight = FontWeight.Light, fontSize = TAM_SIGNO, color = Paleta.verde)),
        aprobado = medidor.measure("APROBADO", versal),
        ucs = (0..a.llevas).map { medidor.measure("$it", uc) },
        // El color va en el estilo y no en un tramo: el alfa de drawText no apaga los colores de tramo
        deTotal = medidor.measure(" de ${a.de} ${a.unidad}", TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = Tipo.s, color = Paleta.tenue)),
        numeros = a.silueta.indices.map { medidor.measure("${it + 1}", numero) },
        teGraduas = medidor.measure("TE GRADÚAS EN", versal),
        faltan = medidor.measure("${a.faltan}", TextStyle(fontFamily = jost, fontWeight = FontWeight.ExtraLight, fontSize = TAM_FALTAN, letterSpacing = (-0.035).em, color = Paleta.tinta)),
        semestres = medidor.measure(if (a.faltan == 1) "semestre" else "semestres", TextStyle(fontFamily = jost, fontWeight = FontWeight.Light, fontSize = Tipo.xxl, color = Paleta.azul)),
    )
}

@Composable
private fun Escena(a: AvanceDemo) {
    val reloj = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        if ((coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f) {
            reloj.floatValue = QUIETO
            return@LaunchedEffect
        }
        val inicio = withFrameMillis { it }
        while (true) withFrameMillis { reloj.floatValue = ((it - inicio) % CICLO).toFloat() }
    }
    // Medido a densidad 1: los textos viven en las unidades de la escena y la cámara los escala con todo lo demás
    val resolutor = LocalFontFamilyResolver.current
    val jost = jost()
    val inter = inter()
    // Los colores van medidos en cada texto: se vuelven a medir si cambia el tema
    val claro = Tema.claro
    val textos = remember(a, resolutor, jost, inter, claro) {
        medirTextos(a, TextMeasurer(resolutor, Density(1f), LayoutDirection.Ltr, 0), jost, inter)
    }
    val escena = remember(a) { GeometriaAvance(a) }
    val toma = remember { FloatArray(5) }
    Canvas(
        Modifier.fillMaxSize().graphicsLayer {
            TOMA.en(reloj.floatValue, toma)
            rotationX = toma[3]
            rotationZ = toma[4]
            // La perspectiva del tablero (1100 de distancia), en las unidades de la capa
            cameraDistance = 1100f / 72f * density
        },
    ) {
        val t = reloj.floatValue
        TOMA.en(t, toma)
        val z = exp(toma[2])
        // En pantallas menores que el tablero (320 dp) la escena entera se encoge a la par; nunca crece
        val ajuste = min(1f, min(size.width / density / ESCENA_ANCHO, size.height / density / ESCENA_ALTO))
        val u = density * z * ajuste
        // La escena se ancla al título, como en el tablero; si sobra alto, sobra arriba
        val centroY = min(size.height / 2f, size.height - (HUECO_TITULO + ESCENA_ALTO * ajuste / 2f) * density)
        translate(size.width / 2f - toma[0] * u, centroY - toma[1] * u) {
            scale(u, u, Offset.Zero) { pintar(t, a, escena, textos) }
        }
    }
}

/*
 * La toma, de un solo travelling sin cortes: empieza sobre lo aprobado y la
 * cifra, baja con el riel mientras lo que falta se proyecta y sube con el
 * asta hasta el birrete.
 *              t       x     y    zoom   incl  giro
 */
private val TOMA = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 160f, 206f, 1.12f, 8f, -1.0f),
        floatArrayOf(1_300f, 162f, 207f, 1.10f, 4f, -0.3f),
        floatArrayOf(3_600f, 172f, 209f, 1.07f, 2f, 0.0f),
        floatArrayOf(4_500f, 190f, 226f, 1.07f, 2f, 0.2f),
        floatArrayOf(7_300f, 246f, 236f, 1.11f, 3f, 0.4f),
        floatArrayOf(8_900f, 262f, 206f, 1.08f, 1f, 0.0f),
        floatArrayOf(10_000f, 160f, 206f, 1.12f, 8f, -1.0f),
    ),
)

/** Lo que no cambia entre cuadros: posiciones, cuándo se traza cada columna y las luces ya armadas */
private class GeometriaAvance(a: AvanceDemo) {
    val hechos = a.hechos
    val xAhora = centro(hechos)
    val xMeta = centro(a.silueta.lastIndex) + 48f
    /** Cuándo pasa la cabeza del riel por cada columna de lo que falta */
    val traza = FloatArray(a.silueta.size) { c ->
        if (c < hechos) 0f else {
            var t = PROYECTA
            while (t < LLEGA && cabeza(t) < xCol(c) + 3f) t += 10f
            t
        }
    }
    val luz = Brush.radialGradient(
        0f to Paleta.verde.copy(alpha = 0.16f), 0.55f to Paleta.verde.copy(alpha = 0.06f), 1f to Paleta.verde.copy(alpha = 0f),
        center = Offset(100f, 260f), radius = 140f,
    )
    val halo = Brush.radialGradient(listOf(Paleta.azul.copy(alpha = 0.30f), Paleta.azul.copy(alpha = 0f)), Offset(xMeta, 145f), 60f)
    val asta = Brush.verticalGradient(listOf(Paleta.azul.copy(alpha = 0.75f), Paleta.azul.copy(alpha = 0.35f)), 165f, 347f)
    val punteado = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    val trazoBirrete = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    /** La cabeza del riel en lo que falta: sale de ahora y llega a la meta a ritmo parejo */
    fun cabeza(t: Float) = xAhora + (xMeta - xAhora) * MARCHA.transform(tramoDe(t, PROYECTA, LLEGA - PROYECTA))
}

private fun xCol(c: Int) = 40f + 32f * c
private fun centro(c: Int) = xCol(c) + 7f
private const val Y_RIEL = 356f

/** Cuánto ha avanzado `t` en el tramo que empieza en `desde` y dura `dura` */
private fun tramoDe(t: Float, desde: Float, dura: Float) = ((t - desde) / dura).coerceIn(0f, 1f)

/** Mezcla dos colores en sRGB y con su alfa, como el rgba() interpolado del tablero */
internal fun mezclarColor(a: Color, b: Color, f: Float) =
    Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, a.alpha + (b.alpha - a.alpha) * f)

/** Un punto o nodo a medio llenar: abajo lleno, arriba hueco, con su aro por dentro */
private fun DrawScope.medioPunto(c: Offset, r: Float, abajo: Color, arriba: Color, aro: Color, grosor: Float, alfa: Float) {
    val caja = Offset(c.x - r, c.y - r)
    drawArc(abajo, 0f, 180f, true, caja, Size(2 * r, 2 * r), alpha = alfa)
    drawArc(arriba, 180f, 180f, true, caja, Size(2 * r, 2 * r), alpha = alfa)
    drawCircle(aro, r - grosor / 2f, c, alpha = alfa, style = Stroke(grosor))
}

/** Un texto colocado por su línea base */
private fun DrawScope.texto(t: TextLayoutResult, x: Float, base: Float, alfa: Float, color: Color = Color.Unspecified) {
    if (alfa <= 0.002f) return
    drawText(t, color, Offset(x, base - t.firstBaseline), alfa)
}

/** La escena entera en el instante `t`, en las unidades del tablero */
private fun DrawScope.pintar(t: Float, a: AvanceDemo, g: GeometriaAvance, x: TextosAvance) {
    fun en(desde: Float, dura: Float) = tramoDe(t, desde, dura)
    val hechos = a.hechos
    val velo = 1f - ENFATICA.transform(en(SE_VA, 650f))
    fun enciendeEn(c: Int) = ENCIENDE + POR_SEMESTRE * c

    // La cifra suma las UC reales de cada semestre mientras se enciende, y redondea como la portada
    var ucHechas = 0f
    a.ucPorSemestre.forEachIndexed { c, uc -> ucHechas += uc * ESTANDAR.transform(en(enciendeEn(c), POR_SEMESTRE + 120f)) }
    // Al proyectar lo que falta, la cifra cede el foco a la meta y se retira entera
    val cede = ESTANDAR.transform(en(PROYECTA - 200f, 900f))

    // La luz de lo aprobado: crece con la cifra
    val luz = ESTANDAR.transform(en(ENCIENDE, 2_200f)) * velo * (1f - 0.5f * cede)
    if (luz > 0f) {
        val e = 0.75f + 0.25f * (if (a.llevas > 0) ucHechas / a.llevas else 1f)
        scale(e, e, Offset(100f, 260f)) { drawCircle(g.luz, 140f, Offset(100f, 260f), alpha = luz) }
    }

    // Lo que llevas: la cifra de la portada sobre las columnas que la explican
    val eC = SALIDA.transform(en(CIFRA, 500f))
    val alfaCifra = eC * velo * (1f - cede)
    if (alfaCifra > 0f) {
        val dy = (1f - eC) * 8f
        val n = redondear(ucHechas * 100.0 / a.de).coerceIn(0, a.porcentaje)
        val cifra = x.cifras[n]
        texto(cifra, 40f, 106.6f + dy, alfaCifra)
        texto(x.signo, 40f + cifra.size.width + 4f, 66f + dy, alfaCifra)
        drawCircle(Paleta.verde, 4f, Offset(44f, 133f + dy), alpha = alfaCifra)
        texto(x.aprobado, 56f, 136.8f + dy, alfaCifra)
        val uc = x.ucs[redondear(ucHechas.toDouble()).coerceIn(0, a.llevas)]
        texto(uc, 40f, 158.7f + dy, alfaCifra)
        texto(x.deTotal, 40f + uc.size.width, 158.7f + dy, alfaCifra)
    }

    // La silueta: tenue al nacer; lo hecho se llena de verde, lo que cursas a medias en tinta, lo que falta en anillo gris
    a.silueta.forEachIndexed { c, filas ->
        val nace = ENFATICA.transform(en(NACE + 70f * c, 520f))
        val alfa = nace * velo
        if (alfa <= 0f) return@forEachIndexed
        val r = 7f * (0.6f + 0.4f * nace)
        for (fila in 0 until filas) {
            val p = Offset(centro(c), 195f + 22f * fila)
            when {
                c < hechos -> {
                    val k = ESTANDAR.transform(en(enciendeEn(c) + 45f * fila, 380f))
                    drawCircle(mezclarColor(TENUE, Paleta.verde, k), r, p, alpha = alfa)
                }
                c == hechos && fila < a.cursando -> {
                    val k = ESTANDAR.transform(en(AHORA + 90f * fila, 420f))
                    medioPunto(
                        p, r, mezclarColor(TENUE, Paleta.tinta, k), mezclarColor(TENUE, Paleta.tinta.copy(alpha = 0.06f), k),
                        Paleta.tinta.copy(alpha = 0.9f * k), 1.5f * r / 7f, alfa,
                    )
                }
                else -> {
                    val k = ESTANDAR.transform(en(g.traza[c] + 40f * fila, 380f))
                    drawCircle(mezclarColor(TENUE, Paleta.tinta.copy(alpha = 0f), k), r, p, alpha = alfa)
                    if (k > 0f) drawCircle(Paleta.tinta.copy(alpha = 0.34f * k), r - 0.75f * r / 7f, p, alpha = alfa, style = Stroke(1.5f * r / 7f))
                }
            }
        }
    }
    // El latido de lo que cursas: un anillo que se abre y se apaga
    if (t >= AHORA + 400f && a.cursando > 0) {
        val q = DESTELLO.transform(((((t - AHORA - 400f) % LATIDO) / LATIDO) / 0.7f).coerceIn(0f, 1f))
        val e = 1f + 0.9f * q
        val alfa = 0.6f * (1f - q) * velo
        if (alfa > 0f) for (fila in 0 until a.cursando) {
            drawCircle(Paleta.tinta, 7f * e - 0.75f * e, Offset(centro(hechos), 195f + 22f * fila), alpha = alfa, style = Stroke(1.5f * e))
        }
    }

    // El riel del tiempo: hecho en verde, ahora en tinta, lo que falta punteado en gris, período a período
    val x0 = centro(0)
    val base = ESTANDAR.transform(en(300f, 1_000f)) * velo
    if (base > 0f) drawRoundRect(Paleta.tinta.copy(alpha = 0.08f), Offset(x0, Y_RIEL - 1f), Size((g.xMeta - x0) * base, 2f), CornerRadius(1f))
    if (hechos > 0) {
        var hecho = 0f
        for (c in 0 until hechos) hecho += ESTANDAR.transform(en(enciendeEn(c), POR_SEMESTRE))
        hecho = hecho / hechos * velo
        val fin = centro(hechos - 1)
        if (hecho > 0f) drawRoundRect(Paleta.verde, Offset(x0, Y_RIEL - 1f), Size((fin - x0) * hecho, 2f), CornerRadius(1f))
        val ahora = ESTANDAR.transform(en(AHORA - 100f, 500f)) * velo
        if (ahora > 0f) drawRect(Paleta.tinta.copy(alpha = 0.7f), Offset(fin, Y_RIEL - 1f), Size((g.xAhora - fin) * ahora, 2f))
    }
    val falta = (g.cabeza(t) - g.xAhora) * velo
    if (falta > 0.5f) {
        drawLine(Paleta.tinta.copy(alpha = 0.32f), Offset(g.xAhora, Y_RIEL), Offset(g.xAhora + falta, Y_RIEL), 2f, pathEffect = g.punteado)
    }

    // Una marca y un número por semestre, en el color de sus puntos
    for (c in a.silueta.indices) {
        val alfa = ESTANDAR.transform(en(500f + 70f * c, 500f)) * velo
        if (alfa <= 0f) continue
        val p = Offset(centro(c), Y_RIEL)
        val color = when {
            c < hechos -> {
                val k = ESTANDAR.transform(en(enciendeEn(c) + 200f, 300f))
                drawCircle(mezclarColor(NODO, Paleta.verde, k), 4f, p, alpha = alfa)
                mezclarColor(Paleta.apagada, Paleta.suave, k)
            }
            c == hechos -> {
                // La marca de ahora: medio llena como sus puntos, y su número en tinta, el más claro del riel
                val k = ESTANDAR.transform(en(AHORA, 400f))
                medioPunto(p, 4f, mezclarColor(NODO, Paleta.tinta, k), mezclarColor(NODO, Paleta.lienzo, k), Paleta.tinta.copy(alpha = 0.9f * k), 1.5f, alfa)
                mezclarColor(Paleta.apagada, Paleta.tinta, k)
            }
            else -> {
                val k = ESTANDAR.transform(en(g.traza[c], 300f))
                drawCircle(mezclarColor(NODO, Paleta.lienzo, k), 4f, p, alpha = alfa)
                if (k > 0f) drawCircle(Paleta.tinta.copy(alpha = 0.4f * k), 3.25f, p, alpha = alfa, style = Stroke(1.5f))
                mezclarColor(Paleta.apagada, Paleta.tenue, k)
            }
        }
        val numero = x.numeros[c]
        texto(numero, p.x - numero.size.width / 2f, 380.5f, alfa, color)
    }

    // La meta, como una bandera: el nodo se llena al llegar el riel y suelta una onda; el asta sube al disco del birrete
    val meta = Offset(g.xMeta, Y_RIEL)
    val alfaMeta = ESTANDAR.transform(en(1_100f, 500f)) * velo
    if (alfaMeta > 0f) {
        val ultima = g.traza.last()
        val aro = mezclarColor(Paleta.tinta.copy(alpha = 0.22f), Paleta.azul, ESTANDAR.transform(en(ultima, LLEGA - ultima)))
        drawCircle(Paleta.lienzo.copy(alpha = 0.9f), 9f, meta, alpha = alfaMeta)
        drawCircle(aro, 8.25f, meta, alpha = alfaMeta, style = Stroke(1.5f))
    }
    val lleno = ENFATICA.transform(en(LLEGA - 60f, 420f)) * velo
    if (lleno > 0f) drawCircle(Paleta.azul, 9f * lleno, meta)
    val onda = en(LLEGA, 1_100f)
    if (onda > 0f && onda < 1f) {
        val e = 1f + 1.8f * SALIDA.transform(onda)
        drawCircle(Paleta.azul, 9f * e - 0.75f * e, meta, alpha = 0.6f * (1f - onda) * velo, style = Stroke(1.5f * e))
    }
    val asta = ENFATICA.transform(en(LLEGA + 60f, 520f)) * velo
    if (asta > 0f) {
        scale(1f, asta, Offset(meta.x, 347f)) { drawRect(g.asta, Offset(meta.x - 0.75f, 165f), Size(1.5f, 182f)) }
    }
    val disco = SALIDA.transform(en(LLEGA + 420f, 460f)) * velo
    val cima = Offset(meta.x, 145f)
    if (disco > 0f) {
        drawCircle(g.halo, 60f, cima, alpha = disco)
        drawCircle(Paleta.azul, 20f * (0.7f + 0.3f * disco), cima, alpha = disco)
    }
    val birrete = SALIDA.transform(en(LLEGA + 520f, 420f)) * velo
    if (birrete > 0f) {
        translate(cima.x - 11f, cima.y - 11f + (1f - birrete) * 3f) {
            scale(22f / 24f, 22f / 24f, Offset.Zero) {
                Lucide.birrete.forEach { drawPath(it, porTema(Paleta.tinta, Color.White), alpha = birrete, style = g.trazoBirrete) }
            }
        }
    }

    // La respuesta cuelga del disco, alineada a la derecha a 14 de él: versal y «6 semestres» por línea base
    val derecha = cima.x - 34f
    fun linea(desde: Float): Pair<Float, Float> {
        val e = SALIDA.transform(en(desde, 560f))
        return e * velo to (1f - e) * 8f
    }
    val (op1, dy1) = linea(LLEGA + 480f)
    // La versal acaba en su última letra, no en el espacio que la sigue
    texto(x.teGraduas, derecha - x.teGraduas.size.width + 11f * 0.07f, 86.8f + dy1, op1)
    val (op2, dy2) = linea(LLEGA + 580f)
    val xSemestres = derecha - x.semestres.size.width
    texto(x.semestres, xSemestres, 157.8f + dy2, op2)
    texto(x.faltan, xSemestres - 8f - x.faltan.size.width, 157.8f + dy2, op2)
}
