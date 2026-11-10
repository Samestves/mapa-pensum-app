package mpensum.carrera

import mpensum.componentes.estilo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.pulsable
import mpensum.componentes.trazarCruz
import mpensum.componentes.trazarPuntos
import mpensum.componentes.ventana
import mpensum.tema.Movimiento
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.logica.DIAS
import mpensum.logica.Renglon
import mpensum.logica.Sesion
import mpensum.logica.agendaDe
import mpensum.logica.clases
import mpensum.logica.duracion
import mpensum.logica.enDoceHoras
import mpensum.logica.horaDe
import mpensum.logica.inicioParaAnadir
import mpensum.logica.lugarDe
import mpensum.logica.meridiano
import mpensum.tema.Paleta
import mpensum.tema.Tema
import kotlin.math.min

/* El día elegido del horario: sus clases, sus huecos y el cambio de un día a otro */

/** Cambiar de día en el horario: el nuevo se desliza y se funde tras un respiro, el viejo se va antes */
private const val DIA_ENTRA_MS = 320

private const val DIA_SALE_MS = 260

private const val DIA_FUNDE_ENTRA_MS = 220

private const val DIA_ESPERA_MS = 60

private const val DIA_FUNDE_SALE_MS = 140

/** Sin bucles ni deslizamientos: solo un fundido corto */
private const val DIA_FUNDE_SIN_BUCLES_MS = 160

/**
 * Un día leído de arriba abajo: cada clase con su tramo, lo libre entre una y
 * otra y, al final, dónde añadir la siguiente. Tres columnas compartidas (la
 * hora, el riel y lo que es) hacen que el día se lea como una sola línea.
 */
@Composable
internal fun DiaElegido(horario: EstadoHorario, arriba: Dp) {
    val umbral = with(LocalDensity.current) { 56.dp.toPx() }
    // Deslizar a los lados cambia de día: el gesto se suma y se decide al soltar
    val arrastre = remember { floatArrayOf(0f) }
    Column(
        Modifier
            .padding(top = arriba)
            .fillMaxWidth()
            .pointerInput(horario) {
                detectHorizontalDragGestures(
                    onDragStart = { arrastre[0] = 0f },
                    onHorizontalDrag = { _, dx -> arrastre[0] += dx },
                    onDragEnd = {
                        val dx = arrastre[0]
                        if (dx < -umbral && horario.dia < 4) horario.dia++
                        else if (dx > umbral && horario.dia > 0) horario.dia--
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = horario.dia,
            transitionSpec = {
                val haciaDelante = targetState > initialState
                val sentido = if (haciaDelante) 1 else -1
                if (Tema.sinBucles) fadeIn(tween(DIA_FUNDE_SIN_BUCLES_MS)) togetherWith fadeOut(tween(Movimiento.SALIDA_RAPIDA))
                else (slideInHorizontally(tween(DIA_ENTRA_MS, easing = Movimiento.FLUIDA)) { sentido * it / 5 } + fadeIn(tween(DIA_FUNDE_ENTRA_MS, DIA_ESPERA_MS))) togetherWith
                    (slideOutHorizontally(tween(DIA_SALE_MS, easing = Movimiento.FLUIDA)) { -sentido * it / 5 } + fadeOut(tween(DIA_FUNDE_SALE_MS)))
            },
            label = "dia",
        ) { d -> ContenidoDia(horario, d) }
    }
}

@Composable
private fun ContenidoDia(horario: EstadoHorario, d: Int) {
    val l = letrasHorario()
    val delDia = horario.dias[d]
    val esHoy = d == horario.ahora.dia
    Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().height(TOQUE), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Paleta.tinta)) { append(DIAS[d].uppercase()) }
                if (esHoy) append(" · HOY")
            },
            Modifier.weight(1f),
            style = l.etiqueta,
        )
        BasicText(
            if (delDia.isEmpty()) "Sin clases" else "${clases(delDia.size)} · sales ${enDoceHoras(delDia.maxOf { it.fin }, Tema.hora24)}",
            style = l.dato,
        )
    }
    Column(Modifier.padding(top = Espacio.s).fillMaxWidth()) {
        var anterior: Renglon? = null
        for (r in agendaDe(delDia)) {
            when (r) {
                is Renglon.Clase -> {
                    // Dos clases seguidas se separan con aire; un tramo libre ya separa por sí mismo
                    if (anterior is Renglon.Clase) Spacer(Modifier.height(Espacio.s))
                    FilaClase(horario, r.sesion)
                }
                is Renglon.Libre -> FilaLibre(r.fin - r.inicio) { horario.abrirNueva(d, r.inicio) }
            }
            anterior = r
        }
        Row(
            Modifier
                .padding(top = Espacio.xs)
                .fillMaxWidth()
                .heightIn(min = Alto.fila)
                .pulsable({ horario.abrirNueva(d, inicioParaAnadir(delDia)) }, escala = 0.98f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(anchoHora()))
            Box(Modifier.width(AIRE_RIEL * 2 + ANCHO_RIEL), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(16.dp)) { trazarCruz(Paleta.suave, 1.75f) }
            }
            BasicText("Añadir clase", style = l.accion.copy(fontSize = 14.sp, color = Paleta.suave))
        }
    }
    }
}

/** Una clase del día: la hora grande, el riel de punto a aro y el nombre con su aula y lo que dura */
@Composable
private fun FilaClase(horario: EstadoHorario, s: Sesion) {
    val l = letrasHorario()
    val pasada = horario.pasada(s)
    val enCurso = horario.enCurso(s)
    val color = horario.color(s.codigo)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Alto.fila + Espacio.xs + Espacio.hilo)
            .height(IntrinsicSize.Min)
            .pulsable({ horario.hoja = HojaHorario.Detalle(s.id) }, escala = 0.985f),
    ) {
        val estrecha = ventana().compacta
        // En compactas la hora de entrada baja de 21 a 19 sp: "10:00" cabe en el carril sin salirse del margen
        val entrada = if (estrecha) l.horaEntrada.copy(fontSize = 19.sp, lineHeight = 19.sp) else l.horaEntrada
        Column(Modifier.width(anchoHora()).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            HoraConMeridiano(horaDe(s.inicio, Tema.hora24), meridiano(s.inicio, Tema.hora24), entrada.copy(color = if (pasada) Paleta.tenue else Paleta.tinta), l)
            // La de salida solo repite el meridiano si cambia: "10:00 AM" arriba, "12:00 PM" abajo; "7:00 AM", "9:00"
            HoraConMeridiano(horaDe(s.fin, Tema.hora24), if (meridiano(s.fin, Tema.hora24) != meridiano(s.inicio, Tema.hora24)) meridiano(s.fin, Tema.hora24) else "", l.horaSalida, l)
        }
        Canvas(
            Modifier
                .padding(horizontal = AIRE_RIEL)
                .width(ANCHO_RIEL)
                .fillMaxHeight()
                .padding(top = 7.dp, bottom = 3.dp)
                .graphicsLayer { alpha = if (pasada) 0.45f else 1f },
        ) {
            val r = 3.5.dp.toPx()
            val cx = size.width / 2
            drawCircle(color, r, Offset(cx, r))
            val desde = 2 * r + 2.dp.toPx()
            val hasta = size.height - 2 * r - 2.dp.toPx()
            val g = 2.dp.toPx()
            if (enCurso) {
                // La vía se llena hasta donde va la clase; el resto queda en hilo
                val avance = ((horario.ahora.minuto - s.inicio).toFloat() / s.duracion).coerceIn(0f, 1f)
                val corte = desde + (hasta - desde) * avance
                drawLine(Paleta.tinta.copy(alpha = 0.15f), Offset(cx, corte), Offset(cx, hasta), g, StrokeCap.Round)
                drawLine(color, Offset(cx, desde), Offset(cx, corte), g, StrokeCap.Round)
            } else {
                drawLine(color, Offset(cx, desde), Offset(cx, hasta), g, StrokeCap.Round)
            }
            drawCircle(color, r - 0.75.dp.toPx(), Offset(cx, size.height - r), style = Stroke(1.5.dp.toPx()))
        }
        Column(Modifier.weight(1f).padding(bottom = 1.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                horario.nombre(s.codigo),
                style = l.nombreFila.copy(color = if (pasada) Paleta.suave else Paleta.tinta),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val lugar = lugarDe(s)
            if (lugar.isNotBlank()) BasicText(lugar, style = l.texto.copy(color = Paleta.suave), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(
                if (enCurso) "En curso · quedan ${duracion(s.fin - horario.ahora.minuto)}" else duracion(s.duracion),
                style = l.dato.copy(color = if (enCurso) Paleta.suave else Paleta.tenue),
            )
        }
        // El «⋯» es la pista de que hay más: toda la fila abre el detalle. Va a la altura del nombre
        Box(
            Modifier
                .offset(x = SALIDA_DERECHA)
                .layout { m, c ->
                    val p = m.measure(c.copy(minHeight = 0, maxHeight = TOQUE.roundToPx()))
                    layout(p.width, 24.dp.roundToPx()) { p.place(0, -12.dp.roundToPx()) }
                }
                .size(TOQUE),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(17.dp)) { trazarPuntos(Paleta.tenue) }
        }
    }
}

@Composable
private fun HoraConMeridiano(hora: String, mer: String, estilo: TextStyle, l: LetrasHorario) {
    Row {
        // "10:00" a 21 sp ocupa casi todo el carril: si no cabe, sobresale hacia la izquierda en vez de partirse
        BasicText(hora, Modifier.weight(1f).alignByBaseline().wrapContentWidth(Alignment.End, unbounded = true), style = estilo, softWrap = false)
        // En 24 h no hay meridiano y el carril no guarda su hueco
        if (!Tema.hora24) BasicText(mer, Modifier.width(22.dp).padding(start = 3.dp).alignByBaseline(), style = l.meridiano)
    }
}

/** El hueco entre dos clases: el riel sigue en puntos, se lee lo que dura y, con el +, se pone una clase ahí */
@Composable
private fun FilaLibre(minutos: Int, alPulsar: () -> Unit) {
    val l = letrasHorario()
    Row(Modifier.fillMaxWidth().height(40.dp).pulsable(alPulsar, escala = 0.98f), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(anchoHora()))
        Canvas(Modifier.padding(horizontal = AIRE_RIEL).width(ANCHO_RIEL).fillMaxHeight().padding(vertical = 4.dp)) {
            val g = 2.dp.toPx()
            drawLine(
                Paleta.tinta.copy(alpha = 0.24f), Offset(size.width / 2, g / 2), Offset(size.width / 2, size.height - g / 2), g,
                StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(0.01f, 4.dp.toPx())),
            )
        }
        BasicText("Libre · ${duracion(minutos)}", Modifier.weight(1f), style = l.dato)
        Box(Modifier.offset(x = SALIDA_DERECHA).size(TOQUE), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(16.dp)) { trazarCruz(Paleta.tenue, 1.75f) }
        }
    }
}

/* ---- Vacío ------------------------------------------------------------------- */
