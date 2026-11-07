package mpensum.portada

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.carrera.BotonTinta
import mpensum.carrera.estiloCuerpo
import mpensum.carrera.estiloFecha
import mpensum.carrera.estiloNota
import mpensum.carrera.estiloRotulo
import mpensum.carrera.estiloTitulo
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.SEPARACION_UNIDAD
import mpensum.componentes.Silueta
import mpensum.componentes.anchoCifraConUnidad
import mpensum.componentes.baseJost
import mpensum.componentes.dibujarCifraConUnidad
import mpensum.componentes.estiloCifra
import mpensum.componentes.estiloUnidad
import mpensum.componentes.pulsable
import mpensum.componentes.renglon
import mpensum.componentes.ventana
import mpensum.datos.Carrera
import mpensum.logica.ResumenPortada
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.acentoDe
import mpensum.tema.colorDe
import mpensum.tema.porTema
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Lo que dura la entrada de tu tarjeta: el filo, los puntos uno a uno y la cifra contando */
private const val FIN_ENTRADA = 1800f

/** Con «Reducir movimiento» o en modo ligero nada se escalona: todo entra en un fundido de este largo */
private const val FUNDIDO_SIMPLE = 200f

/**
 * El tinte del cristal de las tarjetas: casi el lienzo, a media opacidad, para
 * que la aurora pase. En claro, blanco lechoso más opaco (el 78 % del
 * .panel-cristal de la web): el blanco a media opacidad sobre papel no se
 * separa del fondo.
 */
private val TINTE get() = porTema(Color(0xFF101114).copy(alpha = 0.5f), Color.White.copy(alpha = 0.78f))

/** La silueta de pie: punto de 8 y 80 de alto; la tarjeta, 320 */
private val PUNTO = 8.dp
private val ALTO_SILUETA = 80.dp
private val ALTO_TARJETA = 320.dp

/** Cuánto ha avanzado un tramo de la entrada (de 0 a 1): se lee al dibujar, nunca recompone */
private typealias Tramo = (desde: Float, dura: Float) -> Float

/**
 * Lo que la tarjeta pinta. `resumen` solo trae algo si hay marcas; `silueta`
 * también lo trae sin marcas, para el azul de lo que ya se puede inscribir.
 */
private class DatosTarjeta(
    val c: Carrera?,
    val resumen: ResumenPortada?,
    val silueta: ResumenPortada?,
    val sinMarcas: Boolean,
) {
    val verbo
        get() = when {
            c == null -> "Elegir carrera"
            sinMarcas -> "Empezar"
            else -> "Seguir donde ibas"
        }
}

/**
 * Tu carrera, «Silueta viva»: una tarjeta, un trabajo, volver a tu carrera.
 * Tres niveles: lo que es (nombre y tipo), lo que importa (la silueta pintada
 * con tu estado, lo que cursas, lo que puedes inscribir y tu fecha de grado) y
 * la acción («Seguir donde ibas» y el atajo al horario).
 *
 * Es cristal sin desenfoque (no hay nada nítido detrás que borrar, solo la
 * luz): un tinte a media opacidad, un borde de luz que cae desde arriba a la
 * izquierda y el filo de arriba en el color de la carrera, el único sitio de
 * ese color (el azul de la silueta es de lo inscribible).
 *
 * Al entrar, los puntos se encienden por columnas y la cifra cuenta hasta tu
 * avance; todo se lee al dibujar, nada recompone por cuadro.
 */
@Composable
internal fun TuCarrera(
    c: Carrera?,
    resumen: ResumenPortada?,
    cargando: Boolean,
    modifier: Modifier,
    /** En horizontal la tarjeta llena el alto de su columna: la silueta comparte franja con la fecha y los bloques se reparten el alto */
    lleno: Boolean = false,
    alPulsar: (TransformOrigin) -> Unit,
    alAbrirHorario: (TransformOrigin) -> Unit = {},
) {
    val acento by animateColorAsState(c?.let { acentoDe(colorDe(it.color)) } ?: Paleta.tenue, tween(400, easing = SALIDA), label = "acento")
    // Una sola línea de tiempo para toda la entrada; arranca cuando el avance está, para contar desde algo cierto
    val tiempo = remember(c?.slug) { Animatable(if (EntradaPortada.hecha && !cargando) FIN_ENTRADA else 0f) }
    LaunchedEffect(c?.slug, cargando) {
        if (!cargando && tiempo.value < FIN_ENTRADA) tiempo.animateTo(FIN_ENTRADA, tween(FIN_ENTRADA.toInt(), easing = LinearEasing))
    }
    val simple = Tema.sinBucles
    val tramo: Tramo = remember(simple) {
        { desde, dura ->
            if (simple) (tiempo.value / FUNDIDO_SIMPLE).coerceIn(0f, 1f) else ((tiempo.value - desde) / dura).coerceIn(0f, 1f)
        }
    }
    val origen = rememberOrigen()
    // En 320 dp el margen interior baja de 20 a 16: el nombre y el botón respiran
    val pad = if (ventana().compacta) Espacio.m else Espacio.l
    val sinMarcas = !cargando && c != null && resumen?.conMarcas != true
    val datos = DatosTarjeta(c, resumen?.takeIf { it.conMarcas }, resumen, sinMarcas)
    val abrir = { alPulsar(origen.valor) }
    val abrirHorario = { alAbrirHorario(origen.valor) }

    Box(
        (if (lleno) modifier.fillMaxSize() else modifier.fillMaxWidth().height(ALTO_TARJETA))
            .then(origen.medir)
            .pulsable(abrir, escala = 0.985f)
            .drawBehind { cristalTarjeta(acento, if (c == null) 0f else ENFATICA.transform(tramo(300f, 700f)), if (lleno) 28.dp else 24.dp) },
    ) {
        if (lleno) Acostada(datos, tramo, abrir, abrirHorario) else DePie(datos, tramo, pad, abrir, abrirHorario)
    }
}

/** De pie: 20 + cabeza 52 + 20 + silueta 80 + 16 + dato 44 + 16 + acción 52 + 20 = 320 dp */
@Composable
private fun DePie(d: DatosTarjeta, tramo: Tramo, pad: Dp, alPulsar: () -> Unit, alHorario: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = Espacio.l)) {
        Cabeza(d, tramo)
        BoxWithConstraints(Modifier.padding(top = Espacio.l).fillMaxWidth().height(ALTO_SILUETA)) {
            val c = d.c
            if (c == null) SiluetaFantasma(maxWidth)
            else {
                val columnas = c.silueta.size
                val filas = c.silueta.maxOrNull() ?: 1
                // Todo el ancho en diez columnas; el paso vertical es 12 y, con más de siete filas, la silueta no crece
                val pasoH = (maxWidth - PUNTO) / (columnas - 1).coerceAtLeast(1)
                val pasoV = if (filas > 7) (ALTO_SILUETA - PUNTO) / (filas - 1) else 12.dp
                SiluetaViva(d, tramo, PUNTO, pasoH - PUNTO, pasoV - PUNTO)
            }
        }
        Row(
            Modifier.padding(top = Espacio.m).fillMaxWidth().height(44.dp).graphicsLayer { alpha = tramo(1280f, 240f) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Dato(d, enLinea = false)
            Fecha(d)
        }
        Acciones(d, Modifier.padding(top = Espacio.m), alPulsar, alHorario)
    }
}

/** Acostada: cabeza, silueta con la fecha en su franja, la leyenda en una línea y el botón; se reparten el alto */
@Composable
private fun Acostada(d: DatosTarjeta, tramo: Tramo, alPulsar: () -> Unit, alHorario: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(Espacio.xl), verticalArrangement = Arrangement.SpaceBetween) {
        Cabeza(d, tramo)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val c = d.c
            if (c == null) SiluetaFantasma(178.dp, Modifier.height(73.dp))
            else {
                val filas = c.silueta.maxOrNull() ?: 1
                // Punto 7, paso 19 × 11 (73 de alto); con más de siete filas el paso vertical encoge y no crece
                val pasoV = if (filas > 7) 66.dp / (filas - 1) else 11.dp
                SiluetaViva(d, tramo, 7.dp, 12.dp, pasoV - 7.dp)
            }
            Box(Modifier.graphicsLayer { alpha = tramo(1280f, 240f) }) { Fecha(d) }
        }
        Box(Modifier.fillMaxWidth().height(16.dp).graphicsLayer { alpha = tramo(1280f, 240f) }) { Dato(d, enLinea = true) }
        Acciones(d, Modifier, alPulsar, alHorario)
    }
}

/** La cabeza, 52 dp: a la izquierda el nombre y el tipo, a la derecha tu avance */
@Composable
private fun Cabeza(d: DatosTarjeta, tramo: Tramo) {
    val c = d.c
    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Column(Modifier.weight(1f)) {
            BasicText(
                c?.nombreCorto ?: "Elige tu carrera",
                Modifier.renglon(32.dp, 28.sp),
                style = estiloTitulo(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Los nombres largos (o la letra del sistema grande) bajan de tamaño antes de cortar con «…»
                autoSize = TextAutoSize.StepBased(20.sp, 28.sp, 1.sp),
            )
            if (c != null) BasicText(c.tipo.uppercase(), Modifier.padding(top = Espacio.xxs), style = estiloRotulo(), maxLines = 1)
        }
        // Cargando o sin marcas no hay cifra: nada de brillos que finjan un dato
        val r = d.resumen
        if (c != null && r != null) Cifra(r.porcentaje, 40.sp, Modifier.offset(y = (-5).dp)) { ENFATICA.transform(tramo(380f, 900f)) }
    }
}

/** La silueta con el estado de tu carrera: aprobado, cursando, inscribible y el resto apagado */
@Composable
private fun SiluetaViva(d: DatosTarjeta, tramo: Tramo, punto: Dp, hueco: Dp, huecoFila: Dp) {
    val c = d.c ?: return
    val s = d.silueta
    Silueta(
        c.silueta, Paleta.tinta,
        hechas = s?.hechas ?: emptyList(),
        punto = punto, hueco = hueco, huecoFila = huecoFila,
        apagado = 0.16f,
        encendido = { col, fila -> ENFATICA.transform(tramo(380f + col * 70f + fila * 12f, 320f)) },
        porEstado = (s?.cursandoPorSemestre ?: emptyList()) to (s?.disponiblesPorSemestre ?: emptyList()),
        // Lo inscribible aparece después de todo lo aprobado, escalonado y dentro de la línea de tiempo
        aparece = { i -> tramo(1080f + minOf(i * 40f, 240f), 280f) },
    )
}

/** Sin carrera: el «por elegir» de la lista, una silueta fantasma de contorno punteado */
@Composable
private fun SiluetaFantasma(ancho: Dp, modifier: Modifier = Modifier) {
    val color = Paleta.tinta.copy(alpha = 0.5f)
    Canvas(modifier.width(ancho).fillMaxHeight()) {
        val p = PUNTO.toPx()
        val pasoH = (size.width - p) / 9f
        val pasoV = (size.height - p) / 6f
        val trazo = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())))
        for (col in 0 until 10) for (fila in 0 until 7) {
            drawCircle(color, p / 2f - trazo.width / 2f, Offset(col * pasoH + p / 2f, fila * pasoV + p / 2f), style = trazo)
        }
    }
}

/**
 * El dato: dos renglones («◯ 2 cursando», «● 6 por inscribir»), uno si el otro
 * es 0; sin marcas una invitación y sin carrera lo que verás. Cargando, nada.
 */
@Composable
private fun Dato(d: DatosTarjeta, enLinea: Boolean) {
    val r = d.resumen
    when {
        d.c == null -> BasicText("Aquí verás tu avance", style = estiloCuerpo(Paleta.tenue))
        d.sinMarcas -> BasicText("Marca lo que ya aprobaste", style = estiloCuerpo(Paleta.tenue), maxLines = 1, overflow = TextOverflow.Ellipsis)
        r != null -> {
            val renglones: @Composable () -> Unit = {
                if (r.cursando > 0) RenglonDato(r.cursando, "cursando", cursando = true)
                if (r.disponibles > 0) RenglonDato(r.disponibles, "por inscribir", cursando = false)
            }
            if (enLinea) Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) { renglones() }
            else Column(verticalArrangement = Arrangement.spacedBy(Espacio.xxs)) { renglones() }
        }
    }
}

/** Un renglón del dato: el glifo de 8 dp (el mismo punto de la silueta), a 8 del texto, y la cifra en 500 y tinta */
@Composable
private fun RenglonDato(n: Int, texto: String, cursando: Boolean) {
    val azul = Paleta.azul
    val tinta = Paleta.tinta
    Row(Modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Canvas(Modifier.size(PUNTO)) {
            if (cursando) drawCircle(tinta, size.minDimension / 2f - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            else drawCircle(azul)
        }
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = tinta)) { append("$n") }
                append(" $texto")
            },
            style = estiloNota(),
            maxLines = 1,
        )
    }
}

/** La fecha de grado, a la derecha; sin fecha (sin marcas o sin plan) el bloque no va y el dato se queda a la izquierda */
@Composable
private fun Fecha(d: DatosTarjeta) {
    val f = d.resumen?.graduacion ?: return
    Column(horizontalAlignment = Alignment.End) {
        BasicText(f.mesYAnio(), Modifier.renglon(28.dp, 24.sp), style = estiloFecha(), maxLines = 1)
        BasicText("Te gradúas", style = estiloNota(), maxLines = 1)
    }
}

/** La acción: el botón lleno, que ocupa lo que quede, y el círculo del horario a 8 dp */
@Composable
private fun Acciones(d: DatosTarjeta, modifier: Modifier, alPulsar: () -> Unit, alHorario: () -> Unit) {
    Row(modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        BotonTinta(d.verbo, alPulsar, Modifier.weight(1f), flecha = true)
        if (d.c != null) Box(
            Modifier
                .size(52.dp)
                .semantics { contentDescription = "Tu horario" }
                .pulsable(alHorario, escala = 0.9f)
                .drawBehind {
                    drawCircle(Paleta.tinta.copy(alpha = 0.08f))
                    drawCircle(Paleta.tinta.copy(alpha = 0.06f), size.minDimension / 2f - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) { IconoTrazo(Lucide.calendarioDias, Paleta.tinta, 20.dp, 1.75f) }
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
 * El porcentaje grande, fino, con el signo pequeño apoyado en la línea base
 * (CifraConUnidad). Se dibuja en vez de componerse: mientras cuenta cambia en
 * cada cuadro sin recomponer nada. Alineado a la derecha, como el número de
 * una wallet.
 */
@Composable
private fun Cifra(porcentaje: Int, tam: TextUnit, modifier: Modifier = Modifier, cuenta: () -> Float) {
    val medidor = rememberTextMeasurer()
    // Todo escala con el tamaño: a 36 sp son las medidas de siempre
    val k = tam.value / 36f
    val numero = estiloCifra(tam)
    val signo = estiloUnidad(tam)
    Canvas(modifier.size(96.dp * k, 30.dp * k).semantics { contentDescription = "$porcentaje por ciento" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta()).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        val sep = SEPARACION_UNIDAD.toPx()
        // La línea base común, donde apoyaba la cifra: el signo ya no sube
        val base = baseJost((30f * k).dp, tam).toPx()
        dibujarCifraConUnidad(cifra, por, size.width - anchoCifraConUnidad(cifra, por, sep), base, sep)
    }
}
