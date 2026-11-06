package mpensum.portada

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import mpensum.componentes.esApaisado
import mpensum.carrera.LocalFondoHoja
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Rosa
import mpensum.componentes.Silueta
import mpensum.componentes.luzCenital
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.datos.TIPOS
import mpensum.datos.rememberResumenes
import mpensum.logica.ResumenPortada
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Acento
import mpensum.tema.acentoDe
import mpensum.tema.colorDe
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val SALIDA = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ENFATICA = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Lo que dura la entrada de tu tarjeta: el filo, los puntos uno a uno y la cifra contando */
private const val FIN_ENTRADA = 1800f

/**
 * El tinte del cristal de las tarjetas: casi el lienzo, a media opacidad, para
 * que la aurora pase. En claro, blanco lechoso más opaco (el 78 % del
 * .panel-cristal de la web): el blanco a media opacidad sobre papel no se
 * separa del fondo.
 */
private val TINTE get() = porTema(Color(0xFF101114).copy(alpha = 0.5f), Color.White.copy(alpha = 0.78f))

/**
 * La portada: la marca arriba, tu carrera en un cristal con tu avance de
 * verdad y, debajo, las demás en filas. Tocar cualquiera entra directo a su
 * mapa, como en la web. Detrás, una sola luz cenital fija; la cabecera se
 * vuelve vidrio en cuanto el contenido pasa por debajo, y el engranaje abre
 * "Ajustes y ayuda".
 *
 * Tu carrera es la que elegiste (en la guía o en los ajustes); si no elegiste
 * ninguna, la primera en la que marcaste algo. Abrir otra para mirarla no la
 * cambia.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Portada(
    carreras: List<Carrera>,
    propia: Carrera?,
    alAbrir: (Carrera, TransformOrigin) -> Unit,
    alVerGuia: () -> Unit,
    alElegirPropia: (Carrera) -> Unit,
) {
    // La coreografía de entrada es solo la primera vez: al volver de una carrera la portada ya está ahí
    DisposableEffect(Unit) { onDispose { EntradaPortada.hecha = true } }
    val desplazamiento = rememberScrollState()
    val cristal = remember { HazeState() }
    var ajustes by remember { mutableStateOf<PaginaAjustes?>(null) }
    val densidad = LocalDensity.current
    // Un solo valor continuo (0 arriba del todo, 1 a 56 dp de scroll): se lee en la fase de dibujo
    // y la cabecera se va formando con el dedo, sin umbral ni animación aparte
    val umbral = with(densidad) { 56.dp.toPx() }
    fun progreso(): Float {
        val t = (desplazamiento.value / umbral).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    val barra = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val resumenes = rememberResumenes(carreras)
    val suya = propia ?: resumenes?.let { r -> carreras.firstOrNull { it.slug in r } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Paleta.lienzo)) {
        // Horizontal: la tarjeta a la izquierda y las otras carreras a la derecha, sin riel y sin scroll de página
        val horizontal = esApaisado()
        val seguro = WindowInsets.safeDrawing.asPaddingValues()
        val izquierda = seguro.calculateLeftPadding(LayoutDirection.Ltr)
        val derecha = seguro.calculateRightPadding(LayoutDirection.Ltr)
        val conTarjeta = suya != null || resumenes != null
        val tarjeta: @Composable (Boolean) -> Unit = { lleno ->
            val resumen = suya?.let { resumenes?.get(it.slug) }
            Box(if (lleno) Modifier.fillMaxSize() else Modifier) {
                TuCarrera(suya, resumen, cargando = resumenes == null, Modifier.sube(80, 14.dp, 560), lleno) { origen ->
                    if (suya != null) alAbrir(suya, origen) else ajustes = PaginaAjustes.CARRERA
                }
            }
        }
        // En una sola lista, pero en el orden de siempre: ingenierías, licenciaturas y gerencia
        val otras = carreras.filter { it.slug != suya?.slug }.sortedBy { TIPOS.indexOf(it.tipo) }
        val filas: @Composable () -> Unit = {
            otras.forEachIndexed { i, c ->
                Fila(c, Modifier.padding(top = if (i > 0) 8.dp else 0.dp).sube(minOf(420, 180 + 40 * i), 12.dp, 480)) { origen -> alAbrir(c, origen) }
            }
        }
        val rotulo = if (suya != null) "OTRAS CARRERAS" else "CARRERAS"
        // La luz va fuera del scroll: es del fondo, no sube ni baja con la lista
        Box(Modifier.fillMaxSize().luzCenital().hazeSource(cristal)) {
            if (horizontal) {
                // Las dos columnas nacen y acaban a la misma altura: bajo la cabecera de 48 dp y a 32 del borde
                Row(Modifier.fillMaxSize().padding(start = izquierda + 24.dp, end = derecha + 24.dp, top = barra + 60.dp, bottom = 32.dp)) {
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        if (conTarjeta) tarjeta(true)
                    }
                    Spacer(Modifier.width(24.dp))
                    val desplazo = rememberScrollState()
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                // El fundido de 40 dp de abajo solo si hay más por ver (con las 9 carreras): con 8 caben enteras
                                if (desplazo.canScrollForward) {
                                    val f = 40.dp.toPx()
                                    drawRect(
                                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = size.height - f, endY = size.height),
                                        blendMode = BlendMode.DstOut,
                                    )
                                }
                            }
                            .verticalScroll(desplazo),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        otras.chunked(2).forEachIndexed { r, par ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                par.forEachIndexed { j, c ->
                                    FilaCompacta(c, Modifier.weight(1f).sube(minOf(420, 180 + 40 * (2 * r + j)), 12.dp, 480)) { origen -> alAbrir(c, origen) }
                                }
                                // Una ficha suelta al final deja su sitio, no se estira
                                if (par.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(desplazamiento)
                    .padding(start = 16.dp, end = 16.dp, top = barra + 76.dp, bottom = abajo + 60.dp),
            ) {
                // Sin carrera elegida la tarjeta espera a saber si hay marcas: si las hay, la primera con marcas es la tuya
                if (conTarjeta) tarjeta(false)
                BasicText(
                    rotulo,
                    Modifier.padding(start = 4.dp, top = if (conTarjeta) 32.dp else 0.dp, bottom = 12.dp).renglon(14.dp, 11.sp).sube(160, 0.dp, 480),
                    style = etiqueta(Paleta.tenue),
                )
                filas()
            }
        }

        // El vidrio de la cabecera: una sola pieza que tapa la barra de estado y la fila y se forma con el
        // scroll. Desde API 31 desenfoca de verdad; por debajo, un lienzo casi opaco (el desenfoque de
        // RenderScript no pinta nada en esas versiones, así que no se fuerza)
        val lienzo = Paleta.lienzo
        val desplazada by remember { derivedStateOf { progreso() > 0f } }
        if (desplazada) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(barra + 56.dp)
                    .hazeEffect(cristal, vidrio(lienzo)) {
                        alpha = progreso()
                        // A un tercio de resolución el desenfoque cuesta poco
                        inputScale = HazeInputScale.Auto
                    },
            ) {
                // El filo de luz de abajo: 1 px, más claro en el centro y nada en los bordes
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .graphicsLayer { alpha = progreso() }
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Paleta.tinta.copy(alpha = 0.16f), Color.Transparent))),
                )
            }
        }
        // En horizontal, el engranaje alinea su círculo (40 dp) con el borde de la rejilla, a 24 del lado
        Cabecera(Modifier.padding(start = izquierda + 16.dp, end = derecha + if (horizontal) 22.dp else 14.dp, top = barra + 4.dp)) { ajustes = PaginaAjustes.MENU }
        // El rótulo comparte fila con la marca y el engranaje, justo sobre la columna de las otras carreras
        if (horizontal) BasicText(
            rotulo,
            Modifier.padding(start = izquierda + 24.dp + 360.dp + 24.dp + 4.dp, top = barra + 4.dp + 17.dp).renglon(14.dp, 11.sp).sube(160, 0.dp, 480),
            style = etiqueta(Paleta.tenue),
        )

        if (!horizontal) Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(abajo + 60.dp)
                .background(Brush.verticalGradient(listOf(Paleta.lienzo.copy(alpha = 0f), Paleta.lienzo))),
        )

        // La hoja difumina la portada entera, la misma que recoge la cabecera de cristal
        CompositionLocalProvider(LocalFondoHoja provides cristal) {
            HojaAjustes(ajustes, carreras, suya, alCerrar = { ajustes = null }, alElegir = alElegirPropia, alVerGuia = alVerGuia)
        }
    }
}

/**
 * El vidrio de la cabecera: el lienzo a menos de la mitad sobre un desenfoque
 * de 24 dp. Sin API 31 no hay desenfoque y cae al lienzo casi opaco.
 */
private fun vidrio(lienzo: Color) = HazeStyle(
    backgroundColor = lienzo,
    tint = HazeTint(lienzo.copy(alpha = 0.46f)),
    blurRadius = 24.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(lienzo.copy(alpha = 0.94f)),
)

/** Las etiquetas: mayúsculas finas y espaciadas, el tamaño mínimo de la escala */
@Composable
private fun etiqueta(color: Color) = TextStyle(
    fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em, color = color,
)

/**
 * La marca como placa: la rosa, un filete de luz y el nombre en mayúsculas
 * espaciadas, sin caja. Al otro lado, el engranaje en su círculo de cristal.
 * Sin subtítulo: la portada ya dice de qué va.
 */
@Composable
private fun Cabecera(modifier: Modifier, alAjustes: () -> Unit) {
    Row(modifier.fillMaxWidth().height(48.dp).sube(0, 0.dp, 320), verticalAlignment = Alignment.CenterVertically) {
        Marca()
        Box(
            Modifier
                .padding(start = 12.dp, end = 14.dp)
                .size(1.dp, 22.dp)
                .background(Brush.verticalGradient(listOf(Paleta.tinta.copy(alpha = 0f), Paleta.tinta.copy(alpha = 0.3f), Paleta.tinta.copy(alpha = 0f)))),
        )
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = Paleta.suave)) { append("MAPA DE ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Paleta.tinta)) { append("PENSUM") }
            },
            Modifier.weight(1f),
            style = TextStyle(fontFamily = jost(), fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.24.em),
            maxLines = 1,
            softWrap = false,
        )
        Engranaje(alAjustes)
    }
}

/**
 * Los ajustes: 44 de toque y, dentro, un círculo de cristal de 40 con su filo
 * de luz arriba, el mismo lenguaje que las tarjetas.
 */
@Composable
private fun Engranaje(alPulsar: () -> Unit) {
    Box(
        Modifier.size(44.dp).semantics { contentDescription = "Ajustes y ayuda" }.pulsable(alPulsar, escala = 0.9f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).drawWithCache {
                val r = size.minDimension / 2f
                val g = 1.dp.toPx()
                val medio = Offset(size.width / 2f, size.height / 2f)
                val claro = Tema.claro
                // En claro el círculo es blanco sobre el papel, y la luz de arriba sigue siendo blanca
                val fondo = if (claro) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.6f)))
                else Brush.verticalGradient(listOf(Paleta.tinta.copy(alpha = 0.09f), Paleta.tinta.copy(alpha = 0.04f)))
                val luz = if (claro) Color.White else Paleta.tinta.copy(alpha = 0.16f)
                // El filo de arriba: el círculo menos el mismo círculo bajado un pixel, una media luna fina
                val luna = Path().apply {
                    op(
                        Path().apply { addOval(Rect(medio, r)) },
                        Path().apply { addOval(Rect(medio + Offset(0f, g), r)) },
                        PathOperation.Difference,
                    )
                }
                onDrawBehind {
                    drawCircle(fondo, r)
                    drawCircle(Paleta.tinta.copy(alpha = 0.08f), r - g / 2f, style = Stroke(g))
                    drawPath(luna, luz)
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            IconoTrazo(Lucide.engranaje, Paleta.tinta, 20.dp, 1.75f)
        }
    }
}

/**
 * La rosa entra girando y se asienta con un resorte casi sin sobrepaso (nada
 * de ir y volver), y luego enciende sus nodos. Solo al abrir la app: al
 * volver de una carrera ya está quieta.
 */
@Composable
private fun Marca() {
    val quieta = EntradaPortada.hecha
    val giro = remember { Animatable(if (quieta) 0f else -120f) }
    val escala = remember { Animatable(if (quieta) 1f else 0.6f) }
    val opacidad = remember { Animatable(if (quieta) 1f else 0f) }
    val nodos = remember { List(4) { Animatable(if (quieta) 1f else 0f) } }
    LaunchedEffect(Unit) {
        if (quieta) return@LaunchedEffect
        val asentarse = spring<Float>(dampingRatio = 0.9f, stiffness = 50f)
        launch { giro.animateTo(0f, asentarse) }
        launch { escala.animateTo(1f, asentarse) }
        launch { opacidad.animateTo(1f, tween(250)) }
        nodos.forEachIndexed { i, n ->
            launch { n.animateTo(1f, keyframes { durationMillis = 500; delayMillis = 450 + i * 80; 1.5f at 350 }) }
        }
    }
    Rosa(
        32.dp,
        Modifier.graphicsLayer {
            rotationZ = giro.value
            scaleX = escala.value
            scaleY = escala.value
            alpha = opacidad.value
        },
        nodos = { i -> nodos[i].value },
        halo = true,
    )
}

/**
 * Tu carrera, «Sencilla» (como la web en móvil): una tarjeta, un trabajo.
 * Responde a "¿sigo donde iba?" con tres niveles: lo que es (la silueta y el
 * nombre corto), lo que importa (cuánto aprobaste) y la acción. Toda la
 * tarjeta es el botón; el chip solo dice qué pasa al tocarla.
 *
 * Es cristal sin desenfoque (no hay nada nítido detrás que borrar, solo la
 * luz): un tinte a media opacidad, un borde de luz que cae desde arriba a la
 * izquierda y el filo de arriba en el color de la carrera.
 *
 * Al entrar, los puntos aprobados se encienden por columnas y la cifra cuenta
 * hasta tu avance; todo se lee al dibujar, nada recompone por cuadro.
 */
@Composable
private fun TuCarrera(
    c: Carrera?,
    resumen: ResumenPortada?,
    cargando: Boolean,
    modifier: Modifier,
    /** En horizontal la tarjeta llena el alto de su columna: silueta grande, cifra a 56 sp, filete y una fila con el dato */
    lleno: Boolean = false,
    alPulsar: (TransformOrigin) -> Unit,
) {
    val acento by animateColorAsState(c?.let { acentoDe(colorDe(it.color)) } ?: Paleta.tenue, tween(400, easing = SALIDA), label = "acento")
    // Una sola línea de tiempo para toda la entrada; arranca cuando el avance está, para contar desde algo cierto
    val tiempo = remember(c?.slug) { Animatable(if (EntradaPortada.hecha && !cargando) FIN_ENTRADA else 0f) }
    LaunchedEffect(c?.slug, cargando) {
        if (!cargando && tiempo.value < FIN_ENTRADA) tiempo.animateTo(FIN_ENTRADA, tween(FIN_ENTRADA.toInt(), easing = LinearEasing))
    }
    fun tramo(desde: Float, dura: Float) = ((tiempo.value - desde) / dura).coerceIn(0f, 1f)
    val origen = rememberOrigen()

    Box(
        (if (lleno) modifier.fillMaxSize() else modifier.fillMaxWidth().height(152.dp))
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.985f)
            .drawBehind { cristalTarjeta(acento, ENFATICA.transform(tramo(300f, 700f)), if (lleno) 28.dp else 24.dp) },
    ) {
        if (lleno) {
            if (c != null) {
                // Los puntos a paso de 16 (de 7 dp): la silueta es la protagonista de la tarjeta
                Silueta(
                    c.silueta, acento, Modifier.padding(start = 24.dp, top = 28.dp),
                    hechas = resumen?.hechas ?: emptyList(),
                    punto = 7.dp,
                    hueco = 9.dp,
                    huecoFila = 9.dp,
                    apagado = 0.22f,
                    encendido = { col, fila -> ENFATICA.transform(tramo(380f + col * 70f + fila * 12f, 320f)) },
                )
                if (resumen != null || !cargando) {
                    Column(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 24.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Cifra(resumen?.porcentaje ?: 0, 56.sp) { if (resumen != null) ENFATICA.transform(tramo(380f, 900f)) else 1f }
                        BasicText(if (resumen != null) "APROBADO" else "SIN MARCAS", Modifier.renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave))
                    }
                }
            }
            // De abajo hacia arriba: la acción, el filete, y el nombre con su tipo sobre él
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp)) {
                BasicText(
                    c?.nombreCorto ?: "Elige tu carrera",
                    Modifier.renglon(38.dp, 32.sp),
                    style = TextStyle(fontFamily = jost(), fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(20.sp, 32.sp, 1.sp),
                )
                BasicText(c?.tipo?.uppercase() ?: "TOCA UNA DE LA DERECHA", Modifier.padding(top = 4.dp).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
                if (c != null) {
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = 0.08f)))
                    Row(
                        Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Dato(c, resumen, cargando, Modifier.weight(1f))
                        Chip(if (resumen == null && !cargando) "Empezar" else "Continuar", acento)
                    }
                }
            }
            return@Box
        }
        if (c != null) {
            // Lo aprobado encendido por columnas, de izquierda a derecha, y dentro de cada una de arriba abajo
            Silueta(
                c.silueta, acento, Modifier.padding(start = 20.dp, top = 20.dp),
                hechas = resumen?.hechas ?: emptyList(),
                punto = 4.dp,
                hueco = (84f / c.silueta.size - 4f).dp,
                huecoFila = (54f / 7f - 4f).dp,
                apagado = 0.22f,
                encendido = { col, fila -> ENFATICA.transform(tramo(380f + col * 70f + fila * 12f, 320f)) },
            )
            // Cargando no hay cifra: nada de brillos que finjan un dato
            if (resumen != null || !cargando) {
                Column(
                    Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp).height(54.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Cifra(resumen?.porcentaje ?: 0) { if (resumen != null) ENFATICA.transform(tramo(380f, 900f)) else 1f }
                    BasicText(if (resumen != null) "APROBADO" else "SIN MARCAS", Modifier.renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave))
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp).height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    c?.nombreCorto ?: "Elige tu carrera",
                    Modifier.renglon(28.dp, 24.sp),
                    style = TextStyle(fontFamily = jost(), fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Los nombres largos (o la letra del sistema grande) bajan de tamaño antes de cortar con "…"
                    autoSize = TextAutoSize.StepBased(17.sp, 24.sp, 1.sp),
                )
                BasicText(c?.tipo?.uppercase() ?: "TOCA UNA DE ABAJO", Modifier.padding(top = 4.dp).renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave), maxLines = 1)
            }
            if (c != null) Chip(if (resumen == null && !cargando) "Empezar" else "Continuar", acento)
        }
    }
}

/**
 * El dato de la tarjeta grande: lo que puedes inscribir (el azul de lo
 * inscribible) y el semestre por el que vas; sin marcas, una invitación.
 * Mientras carga no dice nada: nada de cifras que finjan un dato.
 */
@Composable
private fun Dato(c: Carrera, resumen: ResumenPortada?, cargando: Boolean, modifier: Modifier) {
    val estilo = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 16.sp, color = Paleta.suave)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            resumen != null -> {
                Box(Modifier.size(6.dp).background(Paleta.azul, CircleShape))
                val semestre = resumen.semestreActual(c.silueta)
                BasicText(
                    buildAnnotatedString {
                        append("${resumen.disponibles} por inscribir")
                        if (semestre != null) {
                            withStyle(SpanStyle(color = Paleta.apagada)) { append("  ·  ") }
                            append("Semestre $semestre")
                        }
                    },
                    style = estilo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            !cargando -> BasicText("Marca lo que ya aprobaste", style = estilo, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** El fondo de la tarjeta: tinte, luz que cae de arriba, el borde de luz a 148° y el filo de la carrera */
private fun DrawScope.cristalTarjeta(acento: Color, filo: Float, radio: Dp = 24.dp) {
    val r = radio.toPx()
    val g = Tema.grosorBorde.dp.toPx()
    val claro = Tema.claro
    // En claro la tarjeta se apoya en el papel con una sombra corrida y sin desenfoque, como las del mapa
    if (claro) drawRoundRect(Paleta.sombra.copy(alpha = 0.035f), Offset(0f, 2.dp.toPx()), size, CornerRadius(r))
    drawRoundRect(TINTE, cornerRadius = CornerRadius(r))
    // La luz que cae de arriba: en claro ya es blanca la tarjeta, no hay qué aclarar
    if (!claro) {
        drawRoundRect(
            Brush.verticalGradient(0f to Paleta.tinta.copy(alpha = 0.07f), 0.6f to Paleta.tinta.copy(alpha = 0.015f), 1f to Paleta.tinta.copy(alpha = 0.015f)),
            cornerRadius = CornerRadius(r),
        )
    }
    // El borde es de tinta, simétrico: luz en oscuro y sombra fina en claro, a la mitad para que no dibuje una caja
    val f = porTema(1f, 0.5f)
    // La línea del degradado como en CSS: a 148° desde arriba y tan larga que toca las dos esquinas opuestas
    val angulo = 148f * PI.toFloat() / 180f
    val dir = Offset(sin(angulo), -cos(angulo))
    val largo = abs(size.width * sin(angulo)) + abs(size.height * cos(angulo))
    drawRoundRect(
        Brush.linearGradient(
            0f to Paleta.tinta.copy(alpha = 0.24f * f),
            0.38f to Paleta.tinta.copy(alpha = 0.06f * f),
            0.58f to Paleta.tinta.copy(alpha = 0f),
            1f to Paleta.tinta.copy(alpha = 0.10f * f),
            start = center - dir * (largo / 2f),
            end = center + dir * (largo / 2f),
        ),
        Offset(g / 2f, g / 2f), Size(size.width - g, size.height - g), CornerRadius(r - g / 2f), style = Stroke(g),
    )
    if (filo > 0f) {
        val margen = 28.dp.toPx()
        drawRect(
            Brush.horizontalGradient(listOf(acento.copy(alpha = 0f), acento.copy(alpha = 0.9f), acento.copy(alpha = 0f)), margen, size.width - margen),
            Offset(margen, 0f), Size(size.width - 2f * margen, g), alpha = filo,
        )
    }
}

/**
 * El porcentaje grande, fino, con el signo pequeño arriba. Se dibuja en vez
 * de componerse: mientras cuenta cambia en cada cuadro sin recomponer nada.
 * Alineado a la derecha, como el número de una wallet.
 */
@Composable
private fun Cifra(porcentaje: Int, tam: TextUnit = 36.sp, cuenta: () -> Float) {
    val medidor = rememberTextMeasurer()
    // Todo escala con el tamaño: a 36 sp son las medidas de siempre
    val k = tam.value / 36f
    val numero = TextStyle(
        fontFamily = jost(), fontWeight = FontWeight.ExtraLight, fontSize = tam, letterSpacing = (-0.02).em,
        color = Paleta.tinta, fontFeatureSettings = "tnum",
    )
    val signo = TextStyle(
        fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = (16f * k).sp, color = Paleta.tenue,
    )
    Canvas(Modifier.size(96.dp * k, 30.dp * k).semantics { contentDescription = "$porcentaje por ciento" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta()).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        val x = size.width - por.size.width
        // Por línea base, como en la web: el signo arriba, a la altura de la cabeza del número
        drawText(por, topLeft = Offset(x, baseJost((16f * k).dp, (16f * k).sp).toPx() - por.firstBaseline))
        drawText(cifra, topLeft = Offset(x - 2.dp.toPx() * k - cifra.size.width, baseJost((30f * k).dp, tam).toPx() - cifra.firstBaseline))
    }
}

/**
 * La acción, en el color de la carrera y sin relleno: el único botón lleno de
 * la app sigue siendo el blanco del onboarding. El texto se mezcla un cuarto
 * con la tinta para leerse sobre el cristal (AA).
 */
@Composable
private fun Chip(texto: String, color: Color) {
    // En Exterior con acento neutro el gris suave no basta sobre blanco: el texto va en tinta
    val letra = if (Tema.fuerte && Tema.acento == Acento.NEUTRO) Paleta.tinta else mezclar(color, Paleta.tinta, 0.25f)
    Row(
        Modifier
            .height(36.dp)
            .drawBehind {
                val r = CornerRadius(18.dp.toPx())
                val g = 1.dp.toPx()
                drawRoundRect(color.copy(alpha = 0.1f), cornerRadius = r)
                drawRoundRect(color.copy(alpha = 0.35f), Offset(g / 2f, g / 2f), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2f), style = Stroke(g))
            }
            .padding(start = 16.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(texto, animationSpec = tween(200), label = "accion") { t ->
            BasicText(t, style = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = letra))
        }
        IconoTrazo(Lucide.flecha, letra, 16.dp, 2f)
    }
}

/** Mezcla dos colores en sRGB, como el color-mix de la web (lerp de Compose va por Oklab y da otro tono) */
private fun mezclar(a: Color, b: Color, f: Float) =
    Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f)

/**
 * Otra carrera: su silueta entera y tenue en su color, el nombre corto, el
 * tipo y la flecha. Una fila de cristal apenas marcada: lo que está a la
 * vista es tu carrera; esto es el índice.
 */
@Composable
private fun Fila(c: Carrera, modifier: Modifier, alPulsar: (TransformOrigin) -> Unit) {
    val color = colorDe(c.color)
    val origen = rememberOrigen()
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.98f)
            .drawBehind {
                // En claro la fila es papel blanco a medias sobre el lienzo, con el mismo filo de tinta
                drawRoundRect(porTema(Paleta.tinta.copy(alpha = 0.035f), Color.White.copy(alpha = 0.62f)), cornerRadius = CornerRadius(16.dp.toPx()))
                borde(Paleta.tinta.copy(alpha = porTema(0.065f, 0.06f)), 16.dp)
            }
            .padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La silueta cabe en 56 × 36 como en la web; la que tiene más de siete filas asoma por debajo
        Box(Modifier.size(56.dp, 36.dp)) {
            Silueta(
                c.silueta, color.copy(alpha = 0.7f),
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
            BasicText(c.tipo.uppercase(), Modifier.padding(top = 4.dp).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
        }
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

/**
 * La ficha compacta de otra carrera para la rejilla de dos columnas en
 * horizontal: 68 dp, su silueta entera en 36 × 28, el nombre (que baja de 15 a
 * 13 sp antes de cortar) y el tipo. Sin flecha: toda la ficha es el toque.
 */
@Composable
private fun FilaCompacta(c: Carrera, modifier: Modifier, alPulsar: (TransformOrigin) -> Unit) {
    val color = colorDe(c.color)
    val origen = rememberOrigen()
    Row(
        modifier
            .height(68.dp)
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.98f)
            .drawBehind {
                drawRoundRect(porTema(Paleta.tinta.copy(alpha = 0.035f), Color.White.copy(alpha = 0.62f)), cornerRadius = CornerRadius(16.dp.toPx()))
                borde(Paleta.tinta.copy(alpha = porTema(0.065f, 0.06f)), 16.dp)
            }
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp, 28.dp)) {
            Silueta(
                c.silueta, color.copy(alpha = 0.75f),
                hechas = c.silueta,
                punto = 2.dp,
                hueco = (36f / c.silueta.size - 2f).dp,
                huecoFila = (28f / 7f - 2f).dp,
            )
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            BasicText(
                c.nombreCorto,
                Modifier.renglon(20.dp, 15.sp),
                style = TextStyle(fontFamily = jost(), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(13.sp, 15.sp, 1.sp),
            )
            BasicText(c.tipo.uppercase(), Modifier.padding(top = 4.dp).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
        }
    }
}

/**
 * Una línea de texto de Jost como la mide la web: `alto` de renglón aunque
 * sea menor que la letra. Android no encoge el renglón por debajo de las
 * métricas de la fuente (Jost sube 1,07 y baja 0,375 em), así que se coloca
 * por su línea base, donde CSS la pondría: a media altura más 0,3475 em.
 */
private fun Modifier.renglon(alto: Dp, tam: TextUnit): Modifier = layout { medible, limites ->
    val p = medible.measure(limites.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val base = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
    val h = alto.roundToPx()
    layout(p.width, h) { p.place(0, baseJost(alto, tam).roundToPx() - base) }
}

/** Dónde cae la línea base de Jost en un renglón CSS de `alto` */
private fun Density.baseJost(alto: Dp, tam: TextUnit): Dp = alto / 2 + (tam.toPx() * 0.3475f).toDp()

/** El filo de un pixel por dentro de una caja redondeada */
private fun DrawScope.borde(color: Color, radio: Dp) {
    val g = Tema.grosorBorde.dp.toPx()
    drawRoundRect(color, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(radio.toPx() - g / 2), style = Stroke(g))
}

/** Si la portada ya hizo su entrada en esta sesión */
internal object EntradaPortada {
    var hecha = false
}

/**
 * La entrada de cada bloque: sube un poco y se asienta, con su retardo. Sin
 * escala: la escala queda para el toque, y así no significa dos cosas. Se
 * puede tocar desde el primer cuadro: la capa no bloquea nada.
 */
@Composable
private fun Modifier.sube(retardo: Int, distancia: Dp = 16.dp, duracion: Int = 480): Modifier {
    val f = remember { Animatable(if (EntradaPortada.hecha) 1f else 0f) }
    LaunchedEffect(Unit) { if (f.value < 1f) f.animateTo(1f, tween(duracion, retardo, SALIDA)) }
    return graphicsLayer {
        alpha = f.value
        translationY = (1f - f.value) * distancia.toPx()
    }
}

/**
 * Dónde está un bloque en la pantalla, como fracción de ella: el mapa nace
 * desde ahí al abrirlo y vuelve ahí al cerrarse. Se guarda sin estado: medir
 * no recompone nada.
 */
private class Origen {
    var valor = TransformOrigin.Center
    val medir = Modifier.onGloballyPositioned { c ->
        val raiz = c.findRootCoordinates().size
        val caja = c.boundsInRoot()
        if (raiz.width > 0 && raiz.height > 0) valor = TransformOrigin(caja.center.x / raiz.width, caja.center.y / raiz.height)
    }
}

@Composable
private fun rememberOrigen() = remember { Origen() }
