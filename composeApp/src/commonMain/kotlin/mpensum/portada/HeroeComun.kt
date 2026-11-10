package mpensum.portada

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.BotonTinta
import mpensum.componentes.IconoLucide
import mpensum.componentes.Velo
import mpensum.componentes.altoMayuscula
import mpensum.componentes.conNegritas
import mpensum.componentes.estiloRazon
import mpensum.componentes.superficie
import mpensum.componentes.Lucide
import mpensum.componentes.SEPARACION_UNIDAD
import mpensum.componentes.dibujarCifraConUnidad
import mpensum.componentes.estiloCifra
import mpensum.componentes.estiloUnidad
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.logica.ResumenPortada
import mpensum.logica.subtituloDeLista
import mpensum.tema.Espacio
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.jost
import kotlin.math.roundToInt

/*
 * Las piezas de la tarjeta de tu carrera (home v4).
 * El idioma es el de la hoja del progreso: la superficie de tinta al 5 % sin
 * filo ni sombra, la cifra fina con su % en la línea base, la nota de Inter 13
 * y el botón de tinta. Los radios son concéntricos: la tarjeta de 32 con 16 de
 * relleno abraza botones de radio 16.
 */

/** El relleno de la tarjeta, el mismo de la hoja del progreso */
internal val RELLENO_HEROE = Espacio.m

/** Los botones de dentro: radio 16, así 16 + 16 = 32, el de la tarjeta */
internal val RADIO_BOTON_HEROE = 16.dp
internal val RADIO_HEROE = RELLENO_HEROE + RADIO_BOTON_HEROE

/** Lo que dura la entrada: la cifra cuenta y la barra se llena a la vez */
private const val ENTRADA_MS = 900

/**
 * Lo que la tarjeta enseña, ya decidido. `resumen` solo llega con marcas;
 * `bruto` llega también sin ellas, para saber qué puedes inscribir el primer día.
 */
internal class DatosHeroe(
    val c: Carrera?,
    val resumen: ResumenPortada?,
    val bruto: ResumenPortada?,
    val cargando: Boolean,
) {
    val sinMarcas get() = !cargando && c != null && resumen == null
    val terminada get() = (resumen?.porcentaje ?: 0) >= 100

    val verbo
        get() = when {
            c == null -> "Elegir carrera"
            sinMarcas -> "Empezar"
            terminada -> "Abrir tu mapa"
            else -> "Seguir donde ibas"
        }

    /** La fracción aprobada, de 0 a 1, para la barra */
    val fraccion get() = (resumen?.porcentaje ?: 0) / 100f

    /**
     * La línea de dato, la misma frase de la lista: «**62** de 153 UC · te
     * gradúas **Dic 2029**». `corta` quita el «te gradúas» para las tarjetas estrechas.
     */
    fun linea(corta: Boolean = false): String {
        val r = resumen
        return when {
            c == null -> "Aquí verás tu avance"
            cargando -> ""
            r == null -> "Marca lo que ya aprobaste"
            terminada -> "Completaste la carrera"
            else -> {
                val uc = r.unidad == "UC"
                val texto = subtituloDeLista(if (uc) r.llevas else 0, if (uc) r.de else null, r.llevas, r.graduacion)
                if (corta) texto.replace("te gradúas ", "") else texto
            }
        }
    }
}

/**
 * La entrada de la tarjeta: de 0 a 1 una sola vez por sesión, en cuanto el
 * avance está. Se lee al dibujar; nada recompone por cuadro. Con «Reducir
 * movimiento» ya empieza en 1.
 */
@Composable
internal fun rememberEntradaHeroe(slug: String?, cargando: Boolean): () -> Float {
    val f = remember(slug) { Animatable(if ((EntradaPortada.hecha && !cargando) || Movimiento.reducido || Tema.sinBucles) 1f else 0f) }
    LaunchedEffect(slug, cargando) {
        if (!cargando && f.value < 1f) f.animateTo(1f, tween(ENTRADA_MS, 220, Movimiento.ASIENTA))
    }
    return { f.value }
}

/** La tarjeta: la superficie de la casa con su radio concéntrico, toda ella el toque de «Seguir» */
@Composable
internal fun Modifier.tarjetaHeroe(origen: Origen, alPulsar: () -> Unit): Modifier =
    this.fillMaxWidth().then(origen.medir).pulsable(alPulsar, escala = 0.985f).superficie(RADIO_HEROE)

/** El nombre de la carrera: Jost Light; baja de tamaño antes de cortar con «…» */
@Composable
internal fun NombreHeroe(d: DatosHeroe, tam: TextUnit = 24.sp, modifier: Modifier = Modifier, peso: FontWeight = FontWeight.Light) {
    BasicText(
        d.c?.nombreCorto ?: "Elige tu carrera",
        modifier,
        style = TextStyle(fontFamily = jost(), fontWeight = peso, fontSize = tam, lineHeight = tam * 1.25f, letterSpacing = (-0.01).em, color = Paleta.tinta),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased((tam.value * 0.75f).sp, tam, 1.sp),
    )
}

/** La línea de dato con sus negritas, como el subtítulo de la lista */
@Composable
internal fun DatoHeroe(texto: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    BasicText(conNegritas(texto, Paleta.suave), modifier, style = estiloRazon(), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/**
 * El porcentaje fino con su % en la línea base, recortado a sus mayúsculas: su
 * tope es el tope del número, como la cifra héroe del progreso. Cuenta hasta
 * tu avance al dibujar, sin recomponer.
 */
@Composable
internal fun CifraHeroe(porcentaje: Int, tam: TextUnit, cuenta: () -> Float, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val numero = estiloCifra(tam, interlinea = tam)
    val signo = estiloUnidad(tam)
    val (ancho, alto) = with(LocalDensity.current) {
        val final = medidor.measure("$porcentaje", numero)
        val por = medidor.measure("%", signo)
        (final.size.width + SEPARACION_UNIDAD.toPx() + por.size.width).toDp() to altoMayuscula(tam.toPx()).toDp()
    }
    Canvas(modifier.size(ancho, alto).semantics { contentDescription = "$porcentaje por ciento aprobado" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta()).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        // Mientras cuenta, el número es más estrecho: se ancla a la izquierda y el % lo sigue
        dibujarCifraConUnidad(cifra, por, 0f, size.height)
    }
}

/**
 * La barra del avance: 4 dp, la pista de tinta al 8 % y lo aprobado en verde,
 * el mismo verde del anillo de la barra Núcleo. Se llena con la entrada.
 */
@Composable
internal fun BarraHeroe(fraccion: Float, entrada: () -> Float, modifier: Modifier = Modifier) {
    val pista = Paleta.tinta.copy(alpha = Velo.pastilla)
    val verde = Paleta.verde
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(pista, cornerRadius = r)
        val lleno = size.width * (fraccion * entrada()).coerceIn(0f, 1f)
        if (lleno > 0f) drawRoundRect(verde, Offset.Zero, Size(maxOf(lleno, size.height), size.height), r)
    }
}

/** El botón cuadrado del horario: 52 dp, radio 16, la tinta al 8 % y el calendario */
@Composable
internal fun BotonHorario(alPulsar: () -> Unit, lado: Dp = 52.dp) {
    Box(
        Modifier
            .size(lado)
            .semantics { contentDescription = "Tu horario" }
            .pulsable(alPulsar, escala = 0.92f)
            .clip(RoundedCornerShape(RADIO_BOTON_HEROE))
            .background(Paleta.tinta.copy(alpha = Velo.pastilla)),
        contentAlignment = Alignment.Center,
    ) { IconoLucide(Lucide.calendarioDias, Paleta.tinta, 20.dp) }
}

/** La acción: el botón lleno, que ocupa lo que quede, y el del horario a 8 dp (sin carrera, solo el botón) */
@Composable
internal fun AccionesHeroe(d: DatosHeroe, alPulsar: () -> Unit, alHorario: () -> Unit, modifier: Modifier = Modifier, conHorario: Boolean = true) {
    Row(modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.spacedBy(Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        BotonTinta(d.verbo, alPulsar, Modifier.weight(1f), radio = RADIO_BOTON_HEROE, flecha = true)
        if (conHorario && d.c != null && !d.sinMarcas) BotonHorario(alHorario)
    }
}
