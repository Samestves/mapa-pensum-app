package mpensum.bienvenida

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.BarraPasos
import mpensum.componentes.BotonIcono
import mpensum.componentes.BotonPrincipal
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.auroraViva
import mpensum.componentes.esApaisado
import mpensum.componentes.grano
import mpensum.componentes.pulsable
import mpensum.componentes.rememberRelojAurora
import mpensum.datos.Carrera
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost

const val PASOS = 4

/** En horizontal: lo que ocupa la escena y lo que ocupa el texto (con la barra de pasos y los botones) */
const val PESO_ESCENA = 1.5f
const val PESO_TEXTO = 1f
private const val FRACCION_TEXTO = PESO_TEXTO / (PESO_ESCENA + PESO_TEXTO)

/** Lo que mide la barra de pasos: el texto del lado apaisado baja bajo ella */
val ALTO_BARRA = 52.dp

/**
 * La guía de la primera vez, en cuatro pasos: marca lo aprobado y mira qué
 * se abre, la foto del horario, cuánto llevas y tu carrera. Cada paso vende
 * una sola cosa. `pasoInicial` solo lo usan las capturas.
 */
@Composable
fun Bienvenida(carreras: List<Carrera>, pasoInicial: Int = 0, alTerminar: (Carrera?) -> Unit) {
    var paso by rememberSaveable { mutableIntStateOf(pasoInicial) }
    ManejarAtras(paso > 0) { paso -= 1 }
    // La aurora viva de la portada, sin la luz de carrera: aún no hay carrera elegida
    val reloj = rememberRelojAurora(entrar = true, pausada = { false })
    val apaisado = esApaisado()
    Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
        Box(Modifier.fillMaxSize().auroraViva(reloj))
        Box(Modifier.fillMaxSize().grano())
        AnimatedContent(
            targetState = paso,
            transitionSpec = { (fadeIn(tween(500, 120)) + scaleIn(tween(600, 120), initialScale = 0.98f)) togetherWith fadeOut(tween(180)) },
            label = "paso",
        ) { p ->
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                // En vertical la escena empieza justo bajo la barra de pasos (6 + 44 + 2); en horizontal la barra
                // vive sobre el texto y la escena ocupa todo el alto
                if (!apaisado) Spacer(Modifier.height(ALTO_BARRA))
                when (p) {
                    0 -> PasoMapa(alSeguir = { paso = 1 })
                    1 -> PasoHorario(alSeguir = { paso = 2 })
                    2 -> PasoAvance(alSeguir = { paso = 3 })
                    else -> PasoCarrera(carreras, alElegir = alTerminar)
                }
            }
        }

        // La barra de arriba: volver, los pasos y saltar. Cada pieza tiene su sitio fijo: los pasos
        // no cambian de ancho cuando Saltar desaparece en el último
        Box(Modifier.fillMaxWidth().safeDrawingPadding().padding(top = 6.dp).height(44.dp)) {
            // En horizontal la barra se queda sobre el texto, al lado de la escena
            Box(if (apaisado) Modifier.align(Alignment.CenterEnd).fillMaxWidth(FRACCION_TEXTO).fillMaxHeight() else Modifier.fillMaxSize()) {
                if (paso > 0) {
                    BotonIcono(alPulsar = { paso -= 1 }, modifier = Modifier.padding(start = 10.dp)) { IconoTrazo(Lucide.atras, Paleta.tinta, 22.dp, 2f) }
                }
                BarraPasos(
                    paso, PASOS,
                    Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(start = if (paso > 0) 62.dp else 32.dp, end = 86.dp),
                )
                // En el último no hay nada que saltar: elegir es el paso
                if (paso < PASOS - 1) {
                    Box(
                        Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).height(44.dp).pulsable({ paso = PASOS - 1 }).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText("Saltar", style = TextStyle(fontFamily = inter(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Paleta.tenue))
                    }
                }
            }
        }
    }
}

/**
 * La disposición de un paso con escena: en vertical la escena arriba y el
 * texto abajo (con `hueco` entre ambos); en horizontal la escena a un lado
 * y el texto al otro, con el mismo movimiento. `escena` recibe el modifier
 * que le da su sitio.
 */
@Composable
fun PasoConEscena(primera: String, segunda: String, boton: String, alSeguir: () -> Unit, hueco: Dp = 0.dp, escena: @Composable (Modifier) -> Unit) {
    if (esApaisado()) {
        Row(Modifier.fillMaxSize()) {
            escena(Modifier.weight(PESO_ESCENA).fillMaxHeight())
            Box(Modifier.weight(PESO_TEXTO).fillMaxHeight().padding(top = ALTO_BARRA), contentAlignment = Alignment.CenterStart) {
                BloqueTexto(primera, segunda, boton, alSeguir)
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            escena(Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(hueco))
            BloqueTexto(primera, segunda, boton, alSeguir)
        }
    }
}

/**
 * El bloque de abajo de cada paso: el título en dos líneas (la segunda en
 * azul) y el botón, con el borde del título a 40 dp del botón. El título
 * entra una sola vez, desenfocado, y no con cada vuelta de la escena. Si una
 * línea no cabe en un teléfono estrecho, la letra baja antes que partirse.
 */
@Composable
fun BloqueTexto(primera: String, segunda: String, boton: String, alPulsar: () -> Unit, retardo: Int = 300) {
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(1000, retardo)) }
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        BasicText(
            buildAnnotatedString {
                append(primera)
                append("\n")
                withStyle(SpanStyle(color = Paleta.azul)) { append(segunda) }
            },
            style = TextStyle(
                fontFamily = jost(), fontSize = 40.sp, lineHeight = 1.1.em, fontWeight = FontWeight.Light, letterSpacing = (-0.02).em, color = Paleta.tinta,
                // Como la web: el interlineado se reparte arriba y abajo, así la caja mide 88 y el hueco con el botón es el del tablero
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
            softWrap = false,
            maxLines = 2,
            autoSize = TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 40.sp, stepSize = 1.sp),
            // La caja de línea de la web (40/44 con interlineado negativo) acaba a 40 dp del botón, es decir, con la
            // última línea base a 48. Android no reparte un interlineado menor que la letra como la web: se fija la base
            modifier = Modifier.paddingFromBaseline(bottom = 48.dp).graphicsLayer {
                alpha = entrada.value
                translationY = (1f - entrada.value) * 18.dp.toPx()
                renderEffect = desenfoque((12 * (1f - entrada.value)).dp.toPx())
            },
        )
        BotonPrincipal(boton, alPulsar)
    }
}

/**
 * El desenfoque de las entradas, puesto en la capa: así se anima sin
 * recomponer el texto. Sin efecto cuando ya casi no se nota.
 */
fun desenfoque(radio: Float): BlurEffect? = if (radio > 0.5f) BlurEffect(radio, radio, TileMode.Decal) else null

/** Un color con transparencia, sin repetir .copy(alpha=) por todos lados */
fun Color.a(alfa: Float) = copy(alpha = alfa)
