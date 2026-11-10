package mpensum.bienvenida

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.estiloTitular
import mpensum.componentes.Silueta
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.componentes.renglon
import mpensum.componentes.ventana
import mpensum.datos.Carrera
import mpensum.datos.TIPOS
import mpensum.logica.compararEs
import mpensum.plataforma.Preferencias
import mpensum.tema.Movimiento
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.colorDe
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** Lo que mide una fila y su paso, como las de la portada */
private val ALTO_FILA = 72.dp
private val PASO_FILA = 80.dp
private val MARGEN_LISTA = Espacio.xs

/** El título baja hasta aquí antes de partirse: en 320 dp y con letra grande sigue entero */
private val TITULO_MINIMO = 20.sp

/** Lo que dice el botón mientras no hay carrera elegida: el color apagado ya dice que falta elegir */
private const val APAGADO = "Empezar"

/** Lo que respira el botón bajo él, y el fundido de la lista sobre él */
private val BOTON_ABAJO = Espacio.xl + Espacio.hilo
private val FUNDIDO_ARRIBA = Espacio.m
private val FUNDIDO_ABAJO = 64.dp
private val ABAJO_UNA_COLUMNA = 56.dp
private val ABAJO_DOS_COLUMNAS = 72.dp

/** La silueta de la fila (56 × 36 como en la web), su punto y el visto */
private val SILUETA_ANCHO = 56.dp
private val SILUETA_ALTO = Alto.chip
private val VISTO_TAM = 20.dp

// Acostado la lista pide más que los 1,5 : 1 de los demás pasos: con dos columnas los nombres se cortaban
private const val PESO_LISTA_APAISADA = 2f
private const val PESO_TEXTO_APAISADO = 1f

/** La entrada escalonada de las filas, en ms: cuándo arranca la primera, el paso entre una y otra y lo que dura cada una */
private const val ENTRADA_FILA_RETARDO = 150
private const val ENTRADA_FILA_PASO = 40
private const val ENTRADA_FILA_DURA = 480

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
fun PasoCarrera(
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

    val boton = if (repite) "Listo" else "Empezar"
    // Lo que debe sobrevivir al giro va por encima de la bifurcación: la entrada del título, la de las filas
    // y el scroll de la lista. Dentro de cada rama se montarían de nuevo y todo entraría otra vez desde arriba
    val titulo = rememberEntrada()
    val filas = rememberEntradaFilas(ordenadas.size)
    val desplazamiento = rememberScrollState()
    if (esApaisado()) {
        // La lista a un lado, en dos columnas para que quepan más carreras sin desplazar; el título y el botón al otro,
        // bajo la barra de pasos, como en los demás pasos
        Row(Modifier.fillMaxSize()) {
            Lista(ordenadas, elegida, hay, filas, desplazamiento, Modifier.weight(PESO_LISTA_APAISADA).fillMaxHeight().padding(top = ALTO_BARRA - Espacio.m), columnas = 2) { elegida = it.slug }
            Column(Modifier.weight(PESO_TEXTO_APAISADO).fillMaxHeight().padding(top = ALTO_BARRA), verticalArrangement = Arrangement.Center) {
                Titulo(titulo)
                Boton(actual, boton) { alElegir(actual) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Titulo(titulo)
            Lista(ordenadas, elegida, hay, filas, desplazamiento, Modifier.weight(1f)) { elegida = it.slug }
            Boton(actual, boton) { alElegir(actual) }
        }
    }
}

/**
 * La entrada escalonada de las filas, con un solo reloj en milisegundos: la
 * fila `i` arranca a los 150 + 40·i ms y tarda 480 con la curva de asentarse
 * (ver [entradaFila]). Un reloj para las nueve en vez de nueve animaciones.
 */
@Composable
private fun rememberEntradaFilas(cuantas: Int): State<Float> {
    val reloj = remember { Animatable(0f) }
    val total = (ENTRADA_FILA_RETARDO + ENTRADA_FILA_PASO * (cuantas - 1).coerceAtLeast(0) + ENTRADA_FILA_DURA).toFloat()
    LaunchedEffect(Unit) { reloj.animateTo(total, tween(total.toInt(), easing = LinearEasing)) }
    return reloj.asState()
}

/** Cuánto ha entrado la fila `i` (0 a 1) cuando el reloj de las filas marca `ms` */
private fun entradaFila(ms: Float, i: Int): Float {
    val t = ((ms - ENTRADA_FILA_RETARDO - ENTRADA_FILA_PASO * i) / ENTRADA_FILA_DURA).coerceIn(0f, 1f)
    return Movimiento.ASIENTA.transform(t)
}

/** El título arriba, con la misma letra y la misma entrada que el de los otros pasos */
@Composable
private fun Titulo(entrada: State<Float>) {
    val margen = if (ventana().compacta) Espacio.m else Espacio.xl
    BasicText(
        buildAnnotatedString {
            append("Elige tu carrera,\n")
            withStyle(SpanStyle(color = Paleta.azul)) { append("será tu inicio.") }
        },
        style = estiloTitular(),
        softWrap = false,
        maxLines = 2,
        // Como en los otros pasos: en un teléfono estrecho la letra baja antes que partirse
        autoSize = TextAutoSize.StepBased(minFontSize = TITULO_MINIMO, maxFontSize = Tipo.gigante, stepSize = 1.sp),
        modifier = Modifier.fillMaxWidth().padding(start = margen, end = margen, top = Espacio.s).graphicsLayer {
            alpha = entrada.value
            translationY = (1f - entrada.value) * 12.dp.toPx()
        },
    )
}

/**
 * Las carreras, que se desplazan bajo el título y se funden arriba y abajo:
 * la séptima asoma detrás del fundido, así se ve que hay más. Detrás de la
 * elegida, la luz suave de su color, como el halo de la portada. En horizontal
 * va en `columnas` columnas.
 */
@Composable
private fun Lista(
    carreras: List<Carrera>,
    elegida: String?,
    hay: State<Float>,
    entrada: State<Float>,
    desplazamiento: ScrollState,
    modifier: Modifier,
    columnas: Int = 1,
    alTocar: (Carrera) -> Unit,
) {
    val indice = carreras.indexOfFirst { it.slug == elegida }
    val luzColor = animateColorAsState(carreras.getOrNull(indice)?.let { colorDe(it.color) } ?: Paleta.azul, tween(260), label = "luz")
    val luzFila = animateFloatAsState((indice.coerceAtLeast(0) / columnas).toFloat(), tween(260, easing = ESTANDAR), label = "fila")
    val luzColumna = animateFloatAsState((indice.coerceAtLeast(0) % columnas).toFloat(), tween(260, easing = ESTANDAR), label = "columna")
    Box(modifier.padding(top = Espacio.m).fillMaxWidth()) {
        // La luz no se recorta con la lista: es del fondo y se sale de ella
        Box(
            Modifier.matchParentSize().drawBehind {
                val alfa = hay.value
                if (alfa <= 0f) return@drawBehind
                val y = (MARGEN_LISTA + PASO_FILA * luzFila.value + ALTO_FILA / 2).toPx() - desplazamiento.value
                val c = Offset(size.width * (luzColumna.value + 0.5f) / columnas, y)
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
                // Antes del scroll, para que el fundido quede quieto en los bordes mientras la lista pasa por debajo
                .fundidoBordes(arriba = FUNDIDO_ARRIBA, abajo = FUNDIDO_ABAJO)
                .verticalScroll(desplazamiento)
                // Abajo sobra lo justo para que la última suba por encima del fundido
                .padding(top = MARGEN_LISTA, bottom = if (columnas > 1) ABAJO_DOS_COLUMNAS else ABAJO_UNA_COLUMNA),
        ) {
            carreras.chunked(columnas).forEachIndexed { r, grupo ->
                Row(Modifier.fillMaxWidth()) {
                    grupo.forEachIndexed { k, c ->
                        FilaCarrera(c, r * columnas + k, c.slug == elegida, hay, entrada, columnas > 1, Modifier.weight(1f)) { alTocar(c) }
                    }
                }
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
private fun FilaCarrera(c: Carrera, i: Int, esElegida: Boolean, hay: State<Float>, entrada: State<Float>, compacta: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val color = colorDe(c.color)
    // En dos columnas los márgenes bajan y el nombre puede ocupar dos renglones antes de cortarse
    // También se aprieta y se parte en dos renglones con la ventana estrecha o la letra grande
    val ajustada = ventana().compacta || ventana().letraGrande
    val flexible = compacta || ajustada
    val margen = if (compacta) Espacio.xs else Espacio.m
    val relleno = if (compacta) Espacio.s else Espacio.m
    val k = animateFloatAsState(if (esElegida) 1f else 0f, tween(260, easing = ESTANDAR), label = "elegida")
    Row(
        modifier
            .padding(start = margen, end = margen, bottom = PASO_FILA - ALTO_FILA)
            .graphicsLayer {
                // El alfa se aplica a cada trazo y no a una capa aparte: nueve capas por cuadro pesan en gama baja
                compositingStrategy = CompositingStrategy.ModulateAlpha
                val entra = entradaFila(entrada.value, i)
                alpha = entra * (1f - 0.45f * hay.value * (1f - k.value))
                translationY = (1f - entra) * 12.dp.toPx()
            }
            .pulsable(alPulsar, escala = 0.98f)
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA)
            .drawBehind { cristalFila(color, k.value) }
            .padding(start = relleno, end = relleno + Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La silueta cabe en 56 × 36 como en la web; la que tiene más de siete filas asoma por debajo
        Box(Modifier.size(SILUETA_ANCHO, SILUETA_ALTO).graphicsLayer { compositingStrategy = CompositingStrategy.ModulateAlpha; alpha = 0.7f + 0.3f * k.value }) {
            Silueta(
                c.silueta, color,
                hechas = c.silueta,
                punto = 2.5.dp,
                hueco = (56f / c.silueta.size - 2.5f).dp,
                huecoFila = (36f / 7f - 2.5f).dp,
            )
        }
        Column(Modifier.padding(start = relleno, end = Espacio.xs).weight(1f)) {
            BasicText(
                c.nombreCorto,
                if (flexible) Modifier else Modifier.renglon(22.dp, Tipo.xl),
                style = TextStyle(fontFamily = jost(), fontSize = Tipo.xl, lineHeight = 22.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = if (flexible) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                c.tipo.uppercase(),
                Modifier.padding(top = Espacio.xxs).let { if (ajustada) it else it.renglon(14.dp, Tipo.xxs) },
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.14.em, color = Paleta.tenue),
                maxLines = 1,
            )
        }
        // El visto: un disco en su color con la marca en lienzo; crece un poco al llegar, sin rebote
        Box(
            Modifier.size(VISTO_TAM).graphicsLayer {
                alpha = k.value
                val e = 0.8f + 0.2f * Movimiento.ASIENTA.transform(k.value)
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
    val margen = if (ventana().compacta) Espacio.m else Espacio.xl
    val letra = TextStyle(fontFamily = inter(), fontSize = Tipo.xl, fontWeight = FontWeight.SemiBold)
    Box(
        Modifier
            .padding(start = margen, end = margen, top = Espacio.m, bottom = BOTON_ABAJO)
            .then(if (actual != null) Modifier.pulsable(alPulsar, atenuar = 0.88f) else Modifier)
            .fillMaxWidth()
            .heightIn(min = Alto.grande)
            .drawBehind { drawRoundRect(Paleta.tinta.copy(alpha = 0.08f + 0.92f * b.value), cornerRadius = CornerRadius(size.height / 2f)) },
        contentAlignment = Alignment.Center,
    ) {
        if (texto == APAGADO) {
            // Mismo texto en los dos estados: no se montan dos, solo cambia el color
            BasicText(texto, style = letra.copy(color = lerp(Paleta.apagada, Paleta.lienzo, b.value)))
        } else {
            BasicText(
                APAGADO,
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
}
