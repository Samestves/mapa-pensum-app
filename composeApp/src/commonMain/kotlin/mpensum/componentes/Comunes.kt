package mpensum.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpensum.tema.inter
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.TimeSource

/** El resplandor azul y turquesa de la portada web, arriba a la izquierda y a la derecha; en claro, apenas un tinte */
fun Modifier.aurora(intensidad: Float = 1f): Modifier = drawBehind {
    val alto = size.height * 0.6f
    val intensidad = intensidad * porTema(1f, FUERZA_CLARA)
    drawRect(
        Brush.radialGradient(
            listOf(Paleta.aurora.copy(alpha = 0.17f * intensidad), Color.Transparent),
            center = Offset(size.width * 0.12f, -alto * 0.1f),
            radius = size.width * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(Paleta.aurora2.copy(alpha = 0.10f * intensidad), Color.Transparent),
            center = Offset(size.width * 0.8f, -alto * 0.15f),
            radius = size.width * 0.6f,
        ),
    )
}

/*
 * La aurora viva de la portada: las mismas luces, pero vivas. Derivan,
 * respiran y recorren su gama de tonos muy despacio, con periodos de decenas
 * de segundos primos entre sí: el camino no se ve repetirse.
 *
 * El reloj vive en un estado que solo se lee al dibujar, en capas propias:
 * cada cuadro redibuja la luz y nada más, sin recomponer. Va a 30 cuadros como
 * mucho (a esta velocidad no se nota) y se para cuando se le pide (la cabecera
 * es cristal y desenfocaría la luz en cada cuadro) y en segundo plano: ahí
 * Compose ya pausa su reloj de cuadros. Con las animaciones del sistema
 * apagadas se queda quieta y encendida.
 */

/** Entra rápido y se asienta muy largo: la luz se enciende, no se desliza */
private val ENCENDER = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val SALIDA_LUZ = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private const val CUADRO_NS = 32_000_000L
/** Cada luz, del centro al borde, con un final largo para que no se vea dónde acaba */
private val RAMPA = floatArrayOf(1f, 0.8f, 0.52f, 0.26f, 0.08f, 0f)

private val AZULES = listOf(Color(0xFF6094FF), Color(0xFF8B7BFF), Color(0xFF5AA8FF), Color(0xFF6E84FF))
private val AGUAS = listOf(Color(0xFF2FE8C6), Color(0xFF34C6EA), Color(0xFF3FD6A0), Color(0xFF2FD3C0))
private val INDIGOS = listOf(Color(0xFF8B7BFF), Color(0xFF6094FF), Color(0xFF786EF0))

/*
 * En claro la luz no tiene hacia dónde subir: sobre papel, los mismos tonos
 * vivos se ven sucios. Van los profundos (los claros de cada carrera y área),
 * que tiñen en vez de brillar, y a poco más de media fuerza: un velo de
 * color, no un resplandor.
 */
private val AZULES_CLAROS = listOf(Color(0xFF2751CC), Color(0xFF4B3BC4), Color(0xFF1B7FB0), Color(0xFF3E6AE1))
private val AGUAS_CLARAS = listOf(Color(0xFF0A9781), Color(0xFF0B7391), Color(0xFF0F8A75), Color(0xFF0A8577))
private val INDIGOS_CLAROS = listOf(Color(0xFF4B3BC4), Color(0xFF2751CC), Color(0xFF6F42C9))

/** Cuánto pesa la luz de la portada en claro frente al oscuro */
private const val FUERZA_CLARA = 0.5f

/** Dónde iba la aurora: al volver de una carrera sigue desde ahí, sin saltar */
private var ultimoSegundo = 0f

/** El tiempo de la aurora, en segundos de deriva y en milisegundos de entrada */
@Stable
class RelojAurora internal constructor(segundos: Float, entrando: Boolean) {
    internal var segundos by mutableFloatStateOf(segundos)
    internal var ms by mutableFloatStateOf(if (entrando) 0f else ENTRADA_HECHA)

    /** Una ola de periodo `p` segundos, de −1 a 1 */
    internal fun ola(p: Float): Float = sin(2f * PI.toFloat() * segundos / p)

    /** Cuánto se ha encendido el cielo al aparecer */
    internal val entrada: Float get() = ENCENDER.transform((ms / 900f).coerceIn(0f, 1f))

    /** La luz de la carrera llega un poco después que el cielo */
    internal val luz: Float get() = SALIDA_LUZ.transform(((ms - 200f) / 700f).coerceIn(0f, 1f))

    internal companion object {
        const val ENTRADA_HECHA = 10_000f
    }
}

/**
 * El reloj de la aurora. `entrar` hace que la luz se encienda al aparecer
 * (solo al abrir la app); `pausada` se lee como estado y para el reloj
 * mientras devuelva true.
 */
@Composable
fun rememberRelojAurora(entrar: Boolean, pausada: () -> Boolean): RelojAurora {
    val reloj = remember { RelojAurora(ultimoSegundo, entrar) }
    val pausa by rememberUpdatedState(pausada)
    LaunchedEffect(reloj, Tema.sinBucles) {
        // Con el movimiento reducido, en modo ligero o con las animaciones quitadas, la luz se queda quieta y encendida
        if (Tema.sinBucles || (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f) {
            reloj.ms = RelojAurora.ENTRADA_HECHA
            return@LaunchedEffect
        }
        snapshotFlow { pausa() }.collectLatest { quieta ->
            if (quieta) return@collectLatest
            var antes = -1L
            while (true) {
                withFrameNanos { n ->
                    if (antes < 0) antes = n
                    val paso = n - antes
                    if (paso >= CUADRO_NS) {
                        // Tras un tirón largo (un cuadro perdido, volver de fondo) la luz no da un salto
                        val s = minOf(paso, 100_000_000L) / 1e9f
                        reloj.segundos += s
                        if (reloj.ms < RelojAurora.ENTRADA_HECHA) reloj.ms += s * 1000f
                        ultimoSegundo = reloj.segundos
                        antes = n
                    }
                }
            }
        }
    }
    return reloj
}

/** Recorre una gama en bucle; cada paso se funde con una curva suave, sin cortes */
private fun gama(colores: List<Color>, segundos: Float, periodo: Float, fase: Float = 0f): Color {
    val p = (((segundos / periodo + fase) % 1f) + 1f) % 1f * colores.size
    val i = p.toInt().coerceAtMost(colores.size - 1)
    val f = p - i
    val k = f * f * (3f - 2f * f)
    val a = colores[i]
    val b = colores[(i + 1) % colores.size]
    return Color(a.red + (b.red - a.red) * k, a.green + (b.green - a.green) * k, a.blue + (b.blue - a.blue) * k)
}

/** Una luz redonda con la rampa de la aurora */
private fun DrawScope.luz(centro: Offset, radio: Float, color: Color, alfa: Float) {
    if (alfa <= 0f || radio <= 0f) return
    val paradas = Array(RAMPA.size) { i -> i * 0.2f to color.copy(alpha = (alfa * RAMPA[i]).coerceIn(0f, 1f)) }
    drawCircle(Brush.radialGradient(*paradas, center = centro, radius = radio), radio, centro)
}

/**
 * La aurora viva: azul arriba a la izquierda, turquesa a la derecha y un
 * índigo tenue más abajo. Las medidas son las del tablero aprobado (390 de
 * ancho); el turquesa se ancla al borde derecho de la pantalla que toque.
 */
fun Modifier.auroraViva(reloj: RelojAurora): Modifier = graphicsLayer {}.drawBehind {
    val seg = reloj.segundos
    val e = reloj.entrada * Tema.intensidadAurora
    if (e <= 0f) return@drawBehind
    val claro = Tema.claro
    fun ola(p: Float) = reloj.ola(p)
    // Dos ondas por eje: el camino no se repite a la vista y la velocidad punta baja
    fun deriva(a: Float, p1: Float, p2: Float) = a.dp.toPx() * (0.65f * ola(p1) + 0.35f * ola(p2))
    val crece = 0.9f + 0.1f * e
    val rA = 360.dp.toPx() * (1f + 0.06f * ola(23f)) * crece
    val rT = 300.dp.toPx() * (1f + 0.07f * ola(17f)) * crece
    val rI = 240.dp.toPx() * (1f + 0.08f * ola(29f)) * crece
    luz(
        Offset(40.dp.toPx() + deriva(80f, 37f, 23f), -40.dp.toPx() + deriva(30f, 41f, 19f)), rA,
        (Tema.acento?.color ?: gama(if (claro) AZULES_CLAROS else AZULES, seg, 67f)), 0.32f * (1f + 0.12f * ola(19f)) * e,
    )
    luz(
        Offset(size.width - 50.dp.toPx() - deriva(64f, 43f, 29f), -30.dp.toPx() + deriva(26f, 47f, 31f)), rT,
        gama(if (claro) AGUAS_CLARAS else AGUAS, seg, 73f, 0.4f), 0.21f * (1f + 0.15f * ola(53f)) * e,
    )
    luz(
        Offset(200.dp.toPx() + deriva(110f, 59f, 31f), 70.dp.toPx() + deriva(40f, 61f, 37f)), rI,
        gama(if (claro) INDIGOS_CLAROS else INDIGOS, seg, 79f, 0.7f), 0.12f * (1f + 0.3f * ola(43f)) * e,
    )
}

/**
 * La luz cenital de la portada: una sola luz de tinta, fija, que cae desde
 * arriba al centro y se apaga hacia el 78 % del alto. Sin reloj: se pinta una
 * vez y no se mueve con el scroll (va fuera de él). En claro es un tinte
 * apenas perceptible. La preferencia «Luz de fondo» la enciende o la apaga.
 */
fun Modifier.luzCenital(): Modifier = drawBehind {
    if (!Tema.luzFondo || size.height <= 0f) return@drawBehind
    val fuerza = porTema(1f, 0.25f)
    val rx = size.width * 1.5f
    val ry = size.height * 0.52f
    val centro = Offset(size.width / 2f, -size.height * 0.14f)
    // Una elipse ancha: círculo de radio rx aplastado a ry, con el final largo para que no se vea dónde acaba
    scale(1f, ry / rx, centro) {
        drawCircle(
            Brush.radialGradient(
                0f to Paleta.tinta.copy(alpha = 0.15f * fuerza),
                0.52f to Paleta.tinta.copy(alpha = 0.045f * fuerza),
                0.78f to Color.Transparent,
                1f to Color.Transparent,
                center = centro, radius = rx,
            ),
            rx, centro,
        )
    }
}

/**
 * Grano fino encima de la luz, como la película de las referencias: quita el
 * aspecto de degradado de ordenador. Es ruido fijo, pintado una vez en un
 * mosaico de 128 y repetido; su capa no se vuelve a dibujar. En claro el
 * punto blanco no se ve sobre el papel: se tiñe de tinta y a menos de la
 * mitad, que en claro el grano se nota mucho antes. En modo ligero no va:
 * un mosaico más que componer en cada cuadro de la luz.
 */
fun Modifier.grano(intensidad: Float = 0.06f): Modifier = if (Tema.ligero) this else graphicsLayer {}.drawBehind {
    val imagen = GRANO
    val lado = 128.dp.roundToPx()
    val tinte = if (Tema.claro) ColorFilter.tint(Paleta.tinta, BlendMode.SrcIn) else null
    val intensidad = intensidad * porTema(1f, 0.4f)
    var y = 0
    while (y < size.height) {
        var x = 0
        while (x < size.width) {
            drawImage(imagen, dstOffset = IntOffset(x, y), dstSize = IntSize(lado, lado), alpha = intensidad, colorFilter = tinte, filterQuality = FilterQuality.Low)
            x += lado
        }
        y += lado
    }
}

/** El ruido del grano: puntos blancos de opacidad al azar, con la semilla fija para que sea siempre el mismo */
private val GRANO: ImageBitmap by lazy {
    val lado = 128
    val niveles = 8
    val imagen = ImageBitmap(lado, lado)
    val azar = Random(7)
    // Por niveles de opacidad: ocho pasadas de puntos en vez de un pixel cada vez
    val puntos = List(niveles) { ArrayList<Offset>() }
    for (y in 0 until lado) for (x in 0 until lado) {
        // La suma de dos azares se agrupa en el medio, como el ruido fractal de la web
        val v = (azar.nextFloat() + azar.nextFloat()) / 2f
        puntos[minOf(niveles - 1, (v * niveles).toInt())] += Offset(x + 0.5f, y + 0.5f)
    }
    val lienzo = Canvas(imagen)
    val pincel = Paint().apply {
        color = Color.White
        strokeWidth = 1f
        strokeCap = StrokeCap.Square
    }
    puntos.forEachIndexed { i, lista ->
        pincel.alpha = (i + 0.5f) / niveles
        lienzo.drawPoints(PointMode.Points, lista, pincel)
    }
    imagen
}

/** Hundirse es casi instantáneo; volver, sin prisa y sin rebote */
private val HUNDIR = spring<Float>(dampingRatio = 1f, stiffness = 1500f)
private val SOLTAR = spring<Float>(dampingRatio = 1f, stiffness = 400f)
/** Lo mínimo que se ve hundido: dentro de un scroll el toque llega tarde y, si es rápido, junto con la suelta */
private const val HUNDIDO_MINIMO_MS = 90L

/**
 * Hace que un elemento se encoja un poco al tocarlo, como en iOS, con
 * resortes en la capa: no recompone al que lo usa. Va PRIMERO en la cadena de
 * modificadores, para que encoja la superficie entera y no solo su contenido.
 * La escala va por tamaño, para que todo se hunda unos 5–10 dp: 0,985 una
 * tarjeta grande, 0,96 un mosaico, 0,9 un botón de icono. `atenuar` apaga un
 * poco lo que es blanco, como los botones llenos de iOS.
 */
@Composable
fun Modifier.pulsable(alPulsar: () -> Unit, rol: Role = Role.Button, escala: Float = 0.97f, atenuar: Float = 1f): Modifier {
    val fuente = remember { MutableInteractionSource() }
    val hundido = remember { Animatable(0f) }
    LaunchedEffect(fuente) {
        var desde = TimeSource.Monotonic.markNow()
        var suelta: Job? = null
        fuente.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> {
                    suelta?.cancel()
                    desde = TimeSource.Monotonic.markNow()
                    launch { hundido.animateTo(1f, HUNDIR) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> suelta = launch {
                    delay(HUNDIDO_MINIMO_MS - desde.elapsedNow().inWholeMilliseconds)
                    hundido.animateTo(0f, SOLTAR)
                }
            }
        }
    }
    return graphicsLayer {
        val e = 1f - (1f - escala) * hundido.value
        scaleX = e
        scaleY = e
        alpha = 1f - (1f - atenuar) * hundido.value
    }.clickable(interactionSource = fuente, indication = null, role = rol, onClick = alPulsar)
}

/** El botón lleno en tinta, el de "Continuar" */
@Composable
fun BotonPrincipal(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .pulsable(alPulsar, atenuar = 0.88f)
            .clip(RoundedCornerShape(28.dp))
            .background(Paleta.tinta),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(texto, style = TextStyle(fontFamily = inter(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Paleta.lienzo))
    }
}

/** Botón redondo de icono, relleno y sin aro, como los de la barra de iOS */
@Composable
fun BotonIcono(alPulsar: () -> Unit, modifier: Modifier = Modifier, fondo: Color = Color.Transparent, contenido: @Composable () -> Unit) {
    Box(
        modifier.size(44.dp).pulsable(alPulsar, escala = 0.9f).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/**
 * La barra de pasos del onboarding: los anteriores llenos y el actual se
 * enciende ya lleno, con un fundido de 250 ms (curva estándar). Sin barrido
 * de ancho: un relleno que avanza parece un temporizador que obliga a esperar.
 */
@Composable
fun BarraPasos(paso: Int, total: Int, modifier: Modifier = Modifier) {
    val encendido = remember(paso) { Animatable(0f) }
    LaunchedEffect(paso) { encendido.animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until total) {
            Box(
                Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Paleta.tinta.copy(alpha = 0.16f))
                    .drawBehind {
                        val a = when {
                            i < paso -> 1f
                            i == paso -> encendido.value
                            else -> 0f
                        }
                        if (a > 0f) drawRect(Paleta.tinta, alpha = a)
                    },
            )
        }
    }
}

/** El fondo de las tarjetas de la hoja: la tinta al 6 % con un filo de 1 dp al 10 % */
internal fun Modifier.tarjeta(radio: Dp) =
    this.clip(RoundedCornerShape(radio)).background(Paleta.tinta.copy(alpha = 0.06f)).border(1.dp, Paleta.tinta.copy(alpha = 0.10f), RoundedCornerShape(radio))
