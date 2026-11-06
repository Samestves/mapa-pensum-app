package mpensum.carrera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import mpensum.bienvenida.desenfoque
import mpensum.componentes.IconoDoble
import mpensum.componentes.IconoRelleno
import mpensum.componentes.Phosphor
import mpensum.componentes.pulsable
import mpensum.logica.Progreso
import mpensum.tema.Paleta
import mpensum.tema.acentoDe
import mpensum.tema.Tema
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.roundToInt

/*
 * La barra de la carrera, "Núcleo": una sola cápsula de cristal pequeña y
 * centrada con inicio y las vistas a los lados y el avance en medio. Las
 * pestañas miden siempre lo mismo, así que el avance no se mueve nunca; la
 * elegida no cambia de forma, solo se enciende.
 */

private val ANCHO_PESTANA = 56.dp
private val ANCHO_NUCLEO = 60.dp
val ALTO_BARRA = 58.dp
private val FORMA = RoundedCornerShape(29.dp)
private val ICONO = 22.dp
/** Medidas del riel lateral: cuatro celdas de 44 dp y el núcleo de 52 dp, con holgura */
internal val ANCHO_RIEL_NAVEGACION = 52.dp
private val ALTO_RIEL = 240.dp
/** El del riel lateral */
private val ICONO_FINO = 19.dp
/** El icono en reposo: en claro con más tinta, que sobre el cristal blanco el 40 % se lava */
private val REPOSO get() = Paleta.tinta.copy(alpha = porTema(0.4f, 0.55f))

/** El encendido: blanco en oscuro, la tinta en claro */
private val ACTIVO get() = porTema(Color.White, Paleta.tinta)

private val ESTILO_BARRA get() = HazeStyle(
    backgroundColor = Paleta.lienzo,
    // En claro, el blanco lechoso del cristal de la web: lo de detrás se intuye y los iconos se leen
    tint = HazeTint(porTema(Color(0xFF15171A).copy(alpha = 0.66f), Color.White.copy(alpha = 0.62f))),
    blurRadius = 28.dp,
    noiseFactor = 0f,
)

@OptIn(ExperimentalHazeApi::class)
@Composable
fun BarraCarrera(
    vista: Vista,
    progreso: Progreso?,
    cristal: HazeState,
    alInicio: () -> Unit,
    alCambiar: (Vista) -> Unit,
    alAvance: () -> Unit,
    /** El color de la carrera: el aro del avance lo toma (o el acento elegido); sin él, el verde de lo aprobado */
    color: Color? = null,
    modifier: Modifier = Modifier,
    /** En horizontal la barra pasa a riel lateral: una columna de cristal (52 x 240 dp) con el avance en medio de las pestañas */
    vertical: Boolean = false,
) {
    val haptica = LocalHapticFeedback.current
    fun ir(v: Vista) {
        if (v == vista) return
        haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
        alCambiar(v)
    }
    // El cristal de la pieza: la barra de abajo o el riel lateral
    fun cristalDe(forma: Shape, base: Modifier) = base
            .shadow(22.dp, forma, ambientColor = SOMBRA_FLOTANTE, spotColor = SOMBRA_FLOTANTE)
            .clip(forma)
            .hazeEffect(cristal, ESTILO_BARRA) { inputScale = HazeInputScale.Auto }
            .drawWithCache {
                // Arriba más claro que abajo, y el filo de luz del canto: lo que hace que se lea como cristal
                val claro = Tema.claro
                val fondo = if (claro) fondoVidrio() else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.12f)))
                val filo = if (claro) filoVidrio(0.48f) else Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.24f),
                    0.48f to Color.White.copy(alpha = 0.04f),
                    1f to Color.White.copy(alpha = 0.09f),
                )
                val g = 1.dp.toPx()
                val radio = CornerRadius(size.minDimension / 2 - g / 2)
                onDrawWithContent {
                    drawRect(fondo)
                    drawContent()
                    drawRoundRect(filo, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), radio, style = Stroke(g))
                }
            }
    if (vertical) {
        // Riel: una sola pieza de cristal con el avance en medio de las pestañas, como la barra de abajo
        val celda = DpSize(44.dp, 44.dp)
        Column(
            cristalDe(RoundedCornerShape(26.dp), modifier.size(ANCHO_RIEL_NAVEGACION, ALTO_RIEL)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Pestana(Phosphor.inicio, "Inicio", activa = false, tam = celda, lado = ICONO_FINO, alPulsar = alInicio)
            Pestana(Vista.MAPA.icono, Vista.MAPA.etiqueta, vista == Vista.MAPA, celda, ICONO_FINO) { ir(Vista.MAPA) }
            Nucleo(progreso, color, DpSize(ANCHO_RIEL_NAVEGACION, ANCHO_RIEL_NAVEGACION), alAvance)
            Pestana(Vista.LISTA.icono, Vista.LISTA.etiqueta, vista == Vista.LISTA, celda, ICONO_FINO) { ir(Vista.LISTA) }
            Pestana(Vista.HORARIO.icono, Vista.HORARIO.etiqueta, vista == Vista.HORARIO, celda, ICONO_FINO) { ir(Vista.HORARIO) }
        }
    } else {
        val celda = DpSize(ANCHO_PESTANA, 50.dp)
        Row(cristalDe(FORMA, modifier).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Pestana(Phosphor.inicio, "Inicio", activa = false, tam = celda, lado = ICONO, alPulsar = alInicio)
            Pestana(Vista.MAPA.icono, Vista.MAPA.etiqueta, vista == Vista.MAPA, celda, ICONO) { ir(Vista.MAPA) }
            Nucleo(progreso, color, DpSize(ANCHO_NUCLEO, ALTO_BARRA), alAvance)
            Pestana(Vista.LISTA.icono, Vista.LISTA.etiqueta, vista == Vista.LISTA, celda, ICONO) { ir(Vista.LISTA) }
            Pestana(Vista.HORARIO.icono, Vista.HORARIO.etiqueta, vista == Vista.HORARIO, celda, ICONO) { ir(Vista.HORARIO) }
        }
    }
}

/** Una pestaña: el icono en reposo, y la elegida en blanco con un halo del mismo icono desenfocado detrás */
@Composable
private fun Pestana(icono: IconoDoble, etiqueta: String, activa: Boolean, tam: DpSize, lado: Dp, alPulsar: () -> Unit) {
    val luz by animateFloatAsState(if (activa) 1f else 0f, tween(350), label = "luz")
    Box(
        Modifier
            .size(tam)
            .semantics {
                selected = activa
                contentDescription = etiqueta
            }
            .pulsable(alPulsar, Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        // El halo es luz: en claro sería una mancha de tinta, así que allí la elegida solo se oscurece
        if (luz > 0f && !Tema.claro) {
            // Más grande que el icono para que el desenfoque no se corte en el borde de su capa
            Box(
                Modifier.size(38.dp).graphicsLayer {
                    alpha = luz * 0.55f
                    renderEffect = desenfoque(6.dp.toPx())
                },
                contentAlignment = Alignment.Center,
            ) { IconoRelleno(icono, false, ACTIVO, lado) }
        }
        IconoRelleno(icono, false, lerp(REPOSO, ACTIVO, luz), lado)
    }
}

/** El avance en el centro: un aro fino con la cifra dentro, sin más */
@Composable
private fun Nucleo(progreso: Progreso?, color: Color?, tam: DpSize, alPulsar: () -> Unit) {
    val avance = numeroAnimado((progreso?.avance ?: 0.0).toFloat().coerceIn(0f, 100f))
    val cifra = avance.roundToInt()
    Box(
        Modifier
            .size(tam)
            .semantics { contentDescription = "Tu avance: $cifra por ciento" }
            .pulsable(alPulsar),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val g = 1.5.dp.toPx()
            val r = size.minDimension / 2 - g / 2
            val caja = Size(r * 2, r * 2)
            val esquina = Offset(center.x - r, center.y - r)
            drawCircle(Paleta.tinta.copy(alpha = 0.12f), r, style = Stroke(g))
            if (avance > 0.5f) drawArc(color?.let { acentoDe(it) } ?: Paleta.verde, -90f, 3.6f * avance, false, esquina, caja, style = Stroke(g, cap = StrokeCap.Round))
        }
        BasicText(
            buildAnnotatedString {
                append("$cifra")
                withStyle(SpanStyle(fontSize = 8.5.sp, color = Paleta.tenue)) { append("%") }
            },
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
        )
    }
}
