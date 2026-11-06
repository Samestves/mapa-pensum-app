package mpensum.bienvenida

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.Silueta
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.datos.TIPOS
import mpensum.logica.compararEs
import mpensum.plataforma.Preferencias
import mpensum.tema.Paleta
import mpensum.tema.colorDe
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema

private val SALIDA = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** Lo que mide una fila y su paso, como las de la portada */
private val ALTO_FILA = 72.dp
private val PASO_FILA = 80.dp
private val MARGEN_LISTA = 8.dp

/** El visto de la elegida, en la rejilla de 24 */
private val VISTO = PathParser().parsePathString("m7 12.5 3.2 3.2L17 9").toPath()

/**
 * Paso 4: elige tu carrera. Aquí la escena es la lista, así que el título
 * va arriba. Las nueve carreras en filas de cristal, como en la portada;
 * tocar una la elige (su borde y su filo se tiñen, su silueta se enciende y
 * las demás se apartan) y el único botón cierra la guía. Elegir no abre el
 * mapa: la guía termina en la portada, donde la elegida ya es «Tu carrera» y
 * desde donde se entra con un toque. Hasta elegir, el botón espera apagado:
 * no hay carrera por defecto que aceptar sin mirar.
 */
@Composable
fun ColumnScope.PasoCarrera(
    carreras: List<Carrera>,
    // Quien repite la guía no empieza nada: el botón solo la cierra. Un parámetro para que las capturas fijen el caso
    repite: Boolean = remember { Preferencias.bienvenidaVista() },
    alElegir: (Carrera?) -> Unit,
) {
    // En el orden de siempre: ingenierías, licenciaturas y gerencia, y dentro de cada tipo por nombre
    val ordenadas = remember(carreras) {
        carreras.sortedWith(compareBy<Carrera> { TIPOS.indexOf(it.tipo) }.then { a, b -> compararEs(a.nombreCorto, b.nombreCorto) })
    }
    // Si vuelves a ver la guía desde los ajustes, tu carrera ya viene elegida
    var elegida by rememberSaveable { mutableStateOf(Preferencias.carrera()) }
    val actual = ordenadas.firstOrNull { it.slug == elegida }
    val hay = animateFloatAsState(if (actual != null) 1f else 0f, tween(260, easing = ESTANDAR), label = "hay")

    Titulo()
    Lista(ordenadas, elegida, hay, Modifier.weight(1f)) { elegida = it.slug }
    Boton(actual, if (repite) "Listo" else "Empezar") { alElegir(actual) }
}

/** El título arriba, con la misma letra y la misma entrada que el de los otros pasos */
@Composable
private fun Titulo() {
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(1000, 300, ESTANDAR)) }
    BasicText(
        buildAnnotatedString {
            append("Elige tu carrera,\n")
            withStyle(SpanStyle(color = Paleta.azul)) { append("será tu inicio.") }
        },
        style = TextStyle(fontFamily = jost(), fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.02).em, color = Paleta.tinta),
        softWrap = false,
        maxLines = 2,
        // Como en los otros pasos: en un teléfono estrecho la letra baja antes que partirse
        autoSize = TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 40.sp, stepSize = 1.sp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp).graphicsLayer {
            alpha = entrada.value
            translationY = (1f - entrada.value) * 18.dp.toPx()
            renderEffect = desenfoque((12 * (1f - entrada.value)).dp.toPx())
        },
    )
}

/**
 * Las carreras, que se desplazan bajo el título y se funden arriba y abajo:
 * la séptima asoma detrás del fundido, así se ve que hay más. Detrás de la
 * elegida, la luz suave de su color, como el halo de la portada.
 */
@Composable
private fun Lista(carreras: List<Carrera>, elegida: String?, hay: State<Float>, modifier: Modifier, alTocar: (Carrera) -> Unit) {
    val desplazamiento = rememberScrollState()
    val indice = carreras.indexOfFirst { it.slug == elegida }
    val luzColor = animateColorAsState(carreras.getOrNull(indice)?.let { colorDe(it.color) } ?: Paleta.azul, tween(260), label = "luz")
    val luzFila = animateFloatAsState(indice.coerceAtLeast(0).toFloat(), tween(260, easing = ESTANDAR), label = "fila")
    Box(modifier.padding(top = 16.dp).fillMaxWidth()) {
        // La luz no se recorta con la lista: es del fondo y se sale de ella
        Box(
            Modifier.matchParentSize().drawBehind {
                val alfa = hay.value
                if (alfa <= 0f) return@drawBehind
                val y = (MARGEN_LISTA + PASO_FILA * luzFila.value + ALTO_FILA / 2).toPx() - desplazamiento.value
                val c = Offset(size.width / 2f, y)
                val rx = 240.dp.toPx()
                val color = luzColor.value
                scale(1f, 130f / 240f, c) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to color.copy(alpha = 0.16f), 0.55f to color.copy(alpha = 0.05f), 1f to color.copy(alpha = 0f),
                            center = c, radius = rx,
                        ),
                        rx, c, alpha = alfa,
                    )
                }
            },
        )
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val arriba = 16.dp.toPx() / size.height
                    val abajo = 64.dp.toPx() / size.height
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, arriba to Color.Black, 1f - abajo to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .verticalScroll(desplazamiento)
                // Abajo sobra lo justo para que la última suba por encima del fundido
                .padding(top = MARGEN_LISTA, bottom = 56.dp),
        ) {
            carreras.forEachIndexed { i, c ->
                FilaCarrera(c, i, c.slug == elegida, hay) { alTocar(c) }
            }
        }
    }
}

/**
 * Una carrera como en la portada: su silueta en su color, el nombre corto y
 * el tipo en versal, sin flecha (aquí tocar elige, no abre). La elegida tiñe
 * su borde, enciende un filo de luz arriba y lleva el visto; las demás bajan
 * al 55 %. Lo que cambia se lee al dibujar: elegir no recompone la lista.
 */
@Composable
private fun FilaCarrera(c: Carrera, i: Int, esElegida: Boolean, hay: State<Float>, alPulsar: () -> Unit) {
    val color = colorDe(c.color)
    val k = animateFloatAsState(if (esElegida) 1f else 0f, tween(260, easing = ESTANDAR), label = "elegida")
    val entra = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entra.animateTo(1f, tween(480, 150 + 40 * i, SALIDA)) }
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = PASO_FILA - ALTO_FILA)
            .graphicsLayer {
                // El alfa se aplica a cada trazo y no a una capa aparte: nueve capas por cuadro pesan en gama baja
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = entra.value * (1f - 0.45f * hay.value * (1f - k.value))
                translationY = (1f - entra.value) * 12.dp.toPx()
            }
            .pulsable(alPulsar, escala = 0.98f)
            .fillMaxWidth()
            .height(ALTO_FILA)
            .drawBehind { cristalFila(color, k.value) }
            .padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La silueta cabe en 56 × 36 como en la web; la que tiene más de siete filas asoma por debajo
        Box(Modifier.size(56.dp, 36.dp).graphicsLayer { compositingStrategy = CompositingStrategy.ModulateAlpha; alpha = 0.7f + 0.3f * k.value }) {
            Silueta(
                c.silueta, color,
                hechas = c.silueta,
                punto = 2.5.dp,
                hueco = (56f / c.silueta.size - 2.5f).dp,
                huecoFila = (36f / 7f - 2.5f).dp,
            )
        }
        Column(Modifier.padding(start = 16.dp, end = 12.dp).weight(1f)) {
            BasicText(
                c.nombreCorto,
                Modifier.renglon(22.dp, 17.sp),
                style = TextStyle(fontFamily = jost(), fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                c.tipo.uppercase(),
                Modifier.padding(top = 4.dp).renglon(14.dp, 11.sp),
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em, color = Paleta.tenue),
                maxLines = 1,
            )
        }
        // El visto: un disco en su color con la marca en lienzo; crece un poco al llegar, sin rebote
        Box(
            Modifier.size(20.dp).graphicsLayer {
                alpha = k.value
                val e = 0.8f + 0.2f * SALIDA.transform(k.value)
                scaleX = e
                scaleY = e
            }.drawBehind {
                drawCircle(color)
                scale(size.width / 24f, size.width / 24f, Offset.Zero) {
                    drawPath(VISTO, Paleta.lienzo, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            },
        )
    }
}

/** El fondo de una fila: cristal apenas marcado; elegida, teñida de su color con el filo de luz arriba */
private fun DrawScope.cristalFila(color: Color, k: Float) {
    val r = CornerRadius(16.dp.toPx())
    val g = 1.dp.toPx()
    // En claro la fila es papel blanco a medias sobre el lienzo, como en la portada
    drawRoundRect(porTema(Paleta.tinta.copy(alpha = 0.035f), Color.White.copy(alpha = 0.62f)), cornerRadius = r)
    if (k > 0f) {
        drawRoundRect(Brush.verticalGradient(listOf(color.copy(alpha = 0.10f * k), color.copy(alpha = 0.03f * k))), cornerRadius = r)
    }
    drawRoundRect(
        mezclarColor(Paleta.tinta.copy(alpha = porTema(0.065f, 0.06f)), color.copy(alpha = 0.565f), k),
        Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2), style = Stroke(g),
    )
    if (k > 0f) {
        val margen = 24.dp.toPx()
        drawRect(
            Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color, color.copy(alpha = 0f)), margen, size.width - margen),
            Offset(margen, 0f), Size(size.width - 2f * margen, g), alpha = k,
        )
    }
}

/**
 * El único botón lleno: apagado hasta que eliges y entonces cierra la guía
 * («Empezar», o «Listo» si ya la habías visto). Los dos textos no se montan:
 * el viejo se va en la primera mitad del cambio y el nuevo llega en la segunda.
 */
@Composable
private fun Boton(actual: Carrera?, texto: String, alPulsar: () -> Unit) {
    val b = animateFloatAsState(if (actual != null) 1f else 0f, tween(250, 150, ESTANDAR), label = "boton")
    val letra = TextStyle(fontFamily = inter(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Box(
        Modifier
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 26.dp)
            .then(if (actual != null) Modifier.pulsable(alPulsar, atenuar = 0.88f) else Modifier)
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind { drawRoundRect(Paleta.tinta.copy(alpha = 0.08f + 0.92f * b.value), cornerRadius = CornerRadius(size.height / 2f)) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            "Elige una carrera",
            Modifier.graphicsLayer { alpha = (1f - 2f * b.value).coerceIn(0f, 1f) },
            style = letra.copy(color = Paleta.apagada),
        )
        BasicText(
            texto,
            Modifier.graphicsLayer { alpha = (2f * b.value - 1f).coerceIn(0f, 1f) },
            style = letra.copy(color = Paleta.lienzo),
        )
    }
}

/**
 * Una línea de Jost como la mide la web: `alto` de renglón aunque sea menor
 * que la letra (Android no encoge el renglón por debajo de las métricas de la
 * fuente), colocada por su línea base donde CSS la pondría. Igual que en la
 * portada.
 */
private fun Modifier.renglon(alto: Dp, tam: TextUnit): Modifier = layout { medible, limites ->
    val p = medible.measure(limites.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val base = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
    val h = alto.roundToPx()
    layout(p.width, h) { p.place(0, baseJost(alto, tam).roundToPx() - base) }
}

private fun Density.baseJost(alto: Dp, tam: TextUnit): Dp = alto / 2 + (tam.toPx() * 0.3475f).toDp()
