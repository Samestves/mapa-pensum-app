package mpensum.portada

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.SEPARACION_UNIDAD
import mpensum.componentes.anchoCifraConUnidad
import mpensum.componentes.baseJost
import mpensum.componentes.dibujarCifraConUnidad
import mpensum.componentes.estiloCifra
import mpensum.componentes.estiloUnidad
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Silueta
import mpensum.componentes.tamSilueta
import mpensum.componentes.pulsable
import mpensum.componentes.renglon
import mpensum.componentes.ventana
import mpensum.datos.Carrera
import mpensum.logica.ResumenPortada

import mpensum.tema.Espacio
import mpensum.tema.Tipo
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
/** En la tarjeta llena: de dónde arranca la silueta, lo mínimo que la separa del nombre y lo que reserva la cifra a su derecha */
private val ARRIBA_SILUETA = 28.dp
private val HUECO_SILUETA = 12.dp
private val ANCHO_CIFRA = 120.dp

@Composable
internal fun TuCarrera(
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
    // En 320 dp el margen interior baja de 20 a 16: el nombre y «Continuar» respiran
    val pad = if (ventana().compacta) Espacio.m else Espacio.l

    Box(
        (if (lleno) modifier.fillMaxSize() else modifier.fillMaxWidth().height(152.dp))
            .then(origen.medir)
            .pulsable({ alPulsar(origen.valor) }, escala = 0.985f)
            .drawBehind { cristalTarjeta(acento, ENFATICA.transform(tramo(300f, 700f)), if (lleno) 28.dp else 24.dp) },
    ) {
        if (lleno) Column(Modifier.fillMaxSize()) {
            // Arriba la silueta y la cifra; abajo el nombre. Son dos franjas de una columna: nunca se pisan
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (c != null) {
                    // Los puntos a paso de 16 (de 7 dp) si caben; en un teléfono bajito la silueta encoge entera, sin cortarse
                    val (anchoNatural, altoNatural) = tamSilueta(c.silueta, 7.dp, 9.dp, 9.dp)
                    val escala = minOf(
                        1f,
                        (maxHeight - ARRIBA_SILUETA - HUECO_SILUETA) / altoNatural,
                        (maxWidth - Espacio.xl * 2 - ANCHO_CIFRA) / anchoNatural,
                    ).coerceAtLeast(0.4f)
                    Silueta(
                        c.silueta, acento, Modifier.padding(start = Espacio.xl, top = ARRIBA_SILUETA),
                        hechas = resumen?.hechas ?: emptyList(),
                        punto = 7.dp * escala,
                        hueco = 9.dp * escala,
                        huecoFila = 9.dp * escala,
                        apagado = 0.22f,
                        encendido = { col, fila -> ENFATICA.transform(tramo(380f + col * 70f + fila * 12f, 320f)) },
                    )
                    if (resumen != null || !cargando) {
                        Column(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = Espacio.xl), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Cifra(resumen?.porcentaje ?: 0, 56.sp) { if (resumen != null) ENFATICA.transform(tramo(380f, 900f)) else 1f }
                            BasicText(if (resumen != null) "APROBADO" else "SIN MARCAS", Modifier.renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave))
                        }
                    }
                }
            }
            // De abajo hacia arriba: la acción, el filete, y el nombre con su tipo sobre él
            Column(Modifier.fillMaxWidth().padding(start = Espacio.xl, end = Espacio.xl, bottom = Espacio.l)) {
                BasicText(
                    c?.nombreCorto ?: "Elige tu carrera",
                    Modifier.renglon(38.dp, 32.sp),
                    style = TextStyle(fontFamily = jost(), fontSize = Tipo.cifra, lineHeight = 38.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(20.sp, 32.sp, 1.sp),
                )
                BasicText(c?.tipo?.uppercase() ?: "TOCA UNA DE LA DERECHA", Modifier.padding(top = Espacio.xxs).renglon(14.dp, 11.sp), style = etiqueta(Paleta.tenue), maxLines = 1)
                if (c != null) {
                    Box(Modifier.padding(top = Espacio.s).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = 0.08f)))
                    Row(
                        Modifier.padding(top = Espacio.s).fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    ) {
                        Dato(c, resumen, cargando, Modifier.weight(1f))
                        Chip(if (resumen == null && !cargando) "Empezar" else "Continuar", acento)
                    }
                }
            }
        }
        if (lleno) return@Box
        if (c != null) {
            // Lo aprobado encendido por columnas, de izquierda a derecha, y dentro de cada una de arriba abajo
            Silueta(
                c.silueta, acento, Modifier.padding(start = pad, top = pad),
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
                    Modifier.align(Alignment.TopEnd).padding(top = pad, end = pad).height(54.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Cifra(resumen?.porcentaje ?: 0) { if (resumen != null) ENFATICA.transform(tramo(380f, 900f)) else 1f }
                    BasicText(if (resumen != null) "APROBADO" else "SIN MARCAS", Modifier.renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave))
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(pad).height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    c?.nombreCorto ?: "Elige tu carrera",
                    Modifier.renglon(28.dp, 24.sp),
                    style = TextStyle(fontFamily = jost(), fontSize = Tipo.titulo, lineHeight = 28.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.01).em, color = Paleta.tinta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Los nombres largos (o la letra del sistema grande) bajan de tamaño antes de cortar con "…"
                    autoSize = TextAutoSize.StepBased(17.sp, 24.sp, 1.sp),
                )
                BasicText(c?.tipo?.uppercase() ?: "TOCA UNA DE ABAJO", Modifier.padding(top = Espacio.xxs).renglon(14.dp, 11.sp), style = etiqueta(Paleta.suave), maxLines = 1)
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
    val estilo = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 16.sp, color = Paleta.suave)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        when {
            resumen != null -> {
                val semestre = resumen.semestreActual(c.silueta)
                // El punto va con el primer renglón, no centrado entre los dos
                Box(Modifier.align(if (semestre != null) Alignment.Top else Alignment.CenterVertically).padding(top = if (semestre != null) 5.dp else 0.dp).size(6.dp).background(Paleta.azul, CircleShape))
                // En dos renglones: junto a «Continuar» una sola línea con los dos datos se cortaba con «…»
                Column {
                    BasicText("${resumen.disponibles} por inscribir", style = estilo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (semestre != null) BasicText("Semestre $semestre", style = estilo.copy(color = Paleta.tenue), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
 * El porcentaje grande, fino, con el signo pequeño apoyado en la línea base
 * (CifraConUnidad). Se dibuja en vez de componerse: mientras cuenta cambia en
 * cada cuadro sin recomponer nada. Alineado a la derecha, como el número de
 * una wallet.
 */
@Composable
private fun Cifra(porcentaje: Int, tam: TextUnit = 36.sp, cuenta: () -> Float) {
    val medidor = rememberTextMeasurer()
    // Todo escala con el tamaño: a 36 sp son las medidas de siempre
    val k = tam.value / 36f
    val numero = estiloCifra(tam)
    val signo = estiloUnidad(tam)
    Canvas(Modifier.size(96.dp * k, 30.dp * k).semantics { contentDescription = "$porcentaje por ciento" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta()).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        val sep = SEPARACION_UNIDAD.toPx()
        // La línea base común, donde apoyaba la cifra: el signo ya no sube
        val base = baseJost((30f * k).dp, tam).toPx()
        dibujarCifraConUnidad(cifra, por, size.width - anchoCifraConUnidad(cifra, por, sep), base, sep)
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
            .padding(start = Espacio.m, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(texto, animationSpec = tween(200), label = "accion") { t ->
            BasicText(t, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = letra))
        }
        IconoTrazo(Lucide.flecha, letra, 16.dp, 2f)
    }
}

/** Mezcla dos colores en sRGB, como el color-mix de la web (lerp de Compose va por Oklab y da otro tono) */
private fun mezclar(a: Color, b: Color, f: Float) =
    Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f)
