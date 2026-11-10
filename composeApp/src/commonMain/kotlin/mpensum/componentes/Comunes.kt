package mpensum.componentes

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpensum.tema.inter
import kotlin.time.TimeSource

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
