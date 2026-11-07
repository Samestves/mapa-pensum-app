package mpensum.portada

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import mpensum.componentes.fuenteCristal
import mpensum.componentes.tinteLiso
import kotlinx.coroutines.launch
import mpensum.componentes.esApaisado
import mpensum.carrera.LocalFondoHoja
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Rosa
import mpensum.componentes.luzCenital
import mpensum.componentes.pulsable
import mpensum.componentes.renglon
import mpensum.datos.Carrera
import mpensum.datos.PLURAL
import mpensum.datos.TIPOS
import mpensum.carrera.estiloRotulo
import mpensum.datos.rememberResumenes

import mpensum.tema.Espacio
import mpensum.tema.Tipo
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.jost
import kotlin.math.sin

internal val SALIDA = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
internal val ENFATICA = CubicBezierEasing(0.2f, 0f, 0f, 1f)

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
        // La tarjeta acostada: algo más de un tercio del ancho útil, entre 300 y 380 dp; las otras carreras se quedan lo demás
        val anchoTarjeta = ((maxWidth - izquierda - derecha - Espacio.xl * 3) * 0.42f).coerceIn(300.dp, 380.dp)
        val tarjeta: @Composable (Boolean) -> Unit = { lleno ->
            val resumen = suya?.let { resumenes?.get(it.slug) }
            Box(if (lleno) Modifier.fillMaxSize() else Modifier) {
                TuCarrera(suya, resumen, cargando = resumenes == null, Modifier.sube(80, 14.dp, 560), lleno) { origen ->
                    if (suya != null) alAbrir(suya, origen) else ajustes = PaginaAjustes.CARRERA
                }
            }
        }
        // Agrupadas por tipo y en el orden de siempre: ingenierías, licenciaturas y gerencia
        val otras = carreras.filter { it.slug != suya?.slug }.sortedBy { TIPOS.indexOf(it.tipo) }
        val grupos = otras.groupBy { it.tipo }.toList()
        val filas: @Composable () -> Unit = {
            var i = 0
            grupos.forEachIndexed { g, (tipo, lista) ->
                RotuloGrupo(tipo, Modifier.padding(top = if (g > 0) Espacio.xl else 0.dp, bottom = Espacio.xs))
                lista.forEach { c ->
                    Fila(c, Modifier.padding(top = if (c !== lista.first()) Espacio.xs else 0.dp).sube(minOf(420, 180 + 40 * i), 12.dp, 480)) { origen -> alAbrir(c, origen) }
                    i++
                }
            }
        }
        // La luz va fuera del scroll: es del fondo, no sube ni baja con la lista
        Box(Modifier.fillMaxSize().luzCenital().fuenteCristal(cristal)) {
            if (horizontal) {
                // Las dos columnas nacen y acaban a la misma altura: bajo la cabecera de 48 dp y a 32 del borde
                Row(Modifier.fillMaxSize().padding(start = izquierda + Espacio.xl, end = derecha + Espacio.xl, top = barra + 60.dp, bottom = Espacio.xxl)) {
                    Box(Modifier.width(anchoTarjeta).fillMaxHeight()) {
                        if (conTarjeta) tarjeta(true)
                    }
                    Spacer(Modifier.width(24.dp))
                    OtrasCompactas(otras, alAbrir)
                }
            } else Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(desplazamiento)
                    .padding(start = Espacio.m, end = Espacio.m, top = barra + 76.dp, bottom = abajo + 60.dp),
            ) {
                // Sin carrera elegida la tarjeta espera a saber si hay marcas: si las hay, la primera con marcas es la tuya
                if (conTarjeta) tarjeta(false)
                Spacer(Modifier.height(if (conTarjeta) Espacio.xxl else 0.dp))
                filas()
            }
        }

        // El vidrio de la cabecera: una sola pieza que tapa la barra de estado y la fila y se forma con el
        // scroll. Desde API 31 desenfoca de verdad; por debajo, un lienzo casi opaco (el desenfoque de
        // RenderScript no pinta nada en esas versiones, así que no se fuerza)
        VidrioCabecera(cristal, barra, ::progreso)
        // En horizontal, el engranaje alinea su círculo (40 dp) con el borde de la rejilla, a 24 del lado
        Cabecera(Modifier.padding(start = izquierda + 16.dp, end = derecha + if (horizontal) 22.dp else 14.dp, top = barra + 4.dp)) { ajustes = PaginaAjustes.MENU }

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
internal fun etiqueta(color: Color) = TextStyle(
    fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.14.em, color = color,
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
                .padding(start = Espacio.s, end = 14.dp)
                .size(1.dp, 22.dp)
                .background(Brush.verticalGradient(listOf(Paleta.tinta.copy(alpha = 0f), Paleta.tinta.copy(alpha = 0.3f), Paleta.tinta.copy(alpha = 0f)))),
        )
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = Paleta.suave)) { append("MAPA DE ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Paleta.tinta)) { append("PENSUM") }
            },
            Modifier.weight(1f),
            style = TextStyle(fontFamily = jost(), fontSize = Tipo.m, lineHeight = 18.sp, letterSpacing = 0.24.em),
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

/** El filo de un pixel por dentro de una caja redondeada */
internal fun DrawScope.borde(color: Color, radio: Dp) {
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
internal class Origen {
    var valor = TransformOrigin.Center
    val medir = Modifier.onGloballyPositioned { c ->
        val raiz = c.findRootCoordinates().size
        val caja = c.boundsInRoot()
        if (raiz.width > 0 && raiz.height > 0) valor = TransformOrigin(caja.center.x / raiz.width, caja.center.y / raiz.height)
    }
}

@Composable
internal fun rememberOrigen() = remember { Origen() }

/** El rótulo de un grupo de carreras en plural («INGENIERÍAS»); sin tipo reconocido, «CARRERAS» */
@Composable
private fun RotuloGrupo(tipo: String, modifier: Modifier = Modifier) {
    BasicText(
        (PLURAL[tipo] ?: "Carreras").uppercase(),
        modifier.padding(start = Espacio.xxs).sube(160, 0.dp, 480),
        style = estiloRotulo(),
    )
}

/** Horizontal: las otras carreras en fichas de dos en dos, con su propio scroll y un fundido abajo si hay más por ver */
@Composable
private fun RowScope.OtrasCompactas(otras: List<Carrera>, alAbrir: (Carrera, TransformOrigin) -> Unit) {
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
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        var n = 0
        otras.groupBy { it.tipo }.toList().forEachIndexed { g, (tipo, lista) ->
            // El rótulo del grupo va dentro del scroll; el primero pega arriba y los demás dejan 16 dp (más los 8 del hueco)
            RotuloGrupo(tipo, Modifier.padding(top = if (g > 0) Espacio.m else 0.dp))
            lista.chunked(2).forEach { par ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    par.forEach { c ->
                        FilaCompacta(c, Modifier.weight(1f).sube(minOf(420, 180 + 40 * n++), 12.dp, 480)) { origen -> alAbrir(c, origen) }
                    }
                    // Una ficha suelta al final deja su sitio, no se estira
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun VidrioCabecera(cristal: HazeState, barra: Dp, progreso: () -> Float) {
    val lienzo = Paleta.lienzo
    val desplazada by remember { derivedStateOf { progreso() > 0f } }
    if (desplazada) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barra + 56.dp)
                .then(
                    // En modo ligero, el mismo lienzo casi opaco que sin API 31
                    if (Tema.ligero) Modifier.tinteLiso(vidrio(lienzo), progreso)
                    else Modifier.hazeEffect(cristal, vidrio(lienzo)) {
                        alpha = progreso()
                        // A un tercio de resolución el desenfoque cuesta poco
                        inputScale = HazeInputScale.Auto
                    },
                ),
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
}
