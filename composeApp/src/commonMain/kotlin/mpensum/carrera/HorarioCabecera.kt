package mpensum.carrera

import mpensum.componentes.estilo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.desbordar
import mpensum.componentes.pulsable
import mpensum.componentes.trazarPuntos
import mpensum.componentes.ventana
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.Momento
import mpensum.logica.Sesion
import mpensum.logica.clases
import mpensum.logica.cuandoViene
import mpensum.logica.duracion
import mpensum.logica.enCarriles
import mpensum.logica.enDoceHoras
import mpensum.logica.horaDe
import mpensum.logica.meridiano
import mpensum.logica.nombreDia
import mpensum.logica.tituloLibre
import mpensum.logica.tramoCorto
import mpensum.tema.Paleta
import mpensum.tema.Tema
import kotlin.math.min

/* La cabecera del horario: lo que toca ahora, lo que sigue y la semana de un vistazo */

/**
 * El renglón de arriba: el rótulo del momento a la izquierda y el «⋯» a la
 * derecha. Debajo, callado, lo que suma la semana.
 */
@Composable
internal fun Cabecera(horario: EstadoHorario) {
    val l = letrasHorario()
    Row(Modifier.fillMaxWidth().height(TOQUE), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            val momento = horario.momento
            val enCurso = momento is Momento.EnCurso
            if (momento is Momento.EnCurso && !Tema.sinBucles) PuntoQueLate(horario.color(momento.clase.codigo))
            BasicText(
                when (momento) {
                    is Momento.EnCurso -> "EN CURSO"
                    is Momento.Luego -> "PRÓXIMA CLASE"
                    else -> nombreDia(horario.ahora.dia).uppercase()
                },
                style = l.etiqueta.copy(color = if (enCurso) Paleta.tinta else Paleta.tenue),
            )
        }
        IconoDerecho(18.dp, { horario.hoja = HojaHorario.Menu }) { trazarPuntos(Paleta.suave) }
    }
}

/* ---- Día: lo que toca ahora --------------------------------------------- */

/**
 * Lo primero que dice el horario: qué toca y qué viene. Sin tarjeta: el
 * título es la página, y el trayecto de la clase en curso es el único dibujo.
 */
@Composable
internal fun AhoraYDespues(horario: EstadoHorario) {
    val l = letrasHorario()
    val momento = horario.momento
    val ahora = horario.ahora
    // La clase de la que se habla: la que va o la que viene hoy
    val clase = when (momento) {
        is Momento.EnCurso -> momento.clase
        is Momento.Luego -> momento.clase
        else -> null
    }
    val proxima = (momento as? Momento.Libre)?.proxima
    val alTocar = clase ?: proxima?.sesion
    Column(Modifier.fillMaxWidth().pulsable({ alTocar?.let { horario.hoja = HojaHorario.Detalle(it.id) } }, escala = 0.99f)) {
        val titulo = when {
            clase != null -> horario.nombre(clase.codigo)
            momento is Momento.Libre -> tituloLibre(momento, ahora)
            else -> ""
        }
        // El nombre manda: la letra baja de 40 a 28 antes de partirlo en más de dos líneas
        BasicText(
            titulo, Modifier.padding(top = Espacio.xxs), style = l.titulo,
            maxLines = 2, overflow = TextOverflow.Ellipsis, autoSize = TextAutoSize.StepBased(28.sp, 40.sp, 1.sp),
        )

        if (clase != null) {
            val enCurso = momento is Momento.EnCurso
            val color = horario.color(clase.codigo)
            BasicText(
                buildAnnotatedString {
                    append(if (enCurso) "Quedan " else "Empieza en ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Paleta.tinta)) {
                        append(duracion(if (momento is Momento.EnCurso) momento.quedan else (momento as Momento.Luego).faltan))
                    }
                    if (clase.aula.isNotBlank()) append(" · ${clase.aula}")
                },
                Modifier.padding(top = Espacio.xs),
                style = l.cuerpo,
            )
            Trayecto(clase, color, if (enCurso) ({ horario.ahora.minuto }) else null)
            val despues = (momento as? Momento.EnCurso)?.despues ?: (momento as? Momento.Luego)?.despues
            if (despues != null) {
                EnUnRenglon(horario, "DESPUÉS", despues)
            } else {
                BasicText("DESPUÉS", Modifier.padding(top = Espacio.xl), style = l.etiqueta)
                BasicText("Es tu última clase de hoy.", Modifier.padding(top = Espacio.xs), style = l.cuerpo)
            }
        }
        if (proxima != null) EnUnRenglon(horario, cuandoViene(proxima).uppercase(), proxima.sesion)
    }
}

/**
 * Una clase dicha en un renglón: su marca de color, el nombre y, detrás,
 * cuándo y dónde. Lo de detrás va con espacios que no parten: si no cabe
 * después del nombre, baja entero (el <Entero> de la web).
 */
@Composable
private fun EnUnRenglon(horario: EstadoHorario, rotulo: String, s: Sesion) {
    val l = letrasHorario()
    val color = horario.color(s.codigo)
    BasicText(rotulo, Modifier.padding(top = Espacio.xl), style = l.etiqueta)
    Row(Modifier.padding(top = Espacio.xs)) {
        Box(Modifier.padding(top = Espacio.xxs).size(3.dp, 13.dp).clip(RoundedCornerShape(2.dp)).drawBehind { drawRect(color) })
        Spacer(Modifier.width(Espacio.xs))
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Paleta.tinta)) { append(horario.nombre(s.codigo)) }
                append(" · ")
                append(listOfNotNull(enDoceHoras(s.inicio, Tema.hora24), s.aula.ifBlank { null }).joinToString(" · ").replace(' ', ' '))
            },
            style = l.cuerpo,
        )
    }
}

private const val CICLO_LATIDO_MS = 2600L

private const val CUADRO_LATIDO_MS = 33L

/** El punto de «En curso»: un aro que se expande y se apaga, dibujado; no recompone nada */
@Composable
private fun PuntoQueLate(color: Color) {
    // A 30 cuadros como mucho: a 120 Hz el latido redibujaría el cristal de la barra 120 veces por segundo
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var ultimo = 0L
        while (true) {
            withFrameMillis { ahora ->
                if (ahora - ultimo >= CUADRO_LATIDO_MS) {
                    ultimo = ahora
                    t = (ahora % CICLO_LATIDO_MS) / CICLO_LATIDO_MS.toFloat()
                }
            }
        }
    }
    // En su propia capa: el latido repinta el punto y no la cabecera
    Canvas(Modifier.size(7.dp).graphicsLayer {}) {
        val r = size.minDimension / 2
        // El 70 % del ciclo crece y se apaga; el resto descansa
        val f = CURVA_LATIDO.transform((t / 0.7f).coerceAtMost(1f))
        drawCircle(color.copy(alpha = 0.5f * (1f - f)), r * (1f + 2.4f * f))
        drawCircle(color, r)
    }
}

/** De la hora de inicio a la de fin, tumbado: sale de un punto y llega a un aro; el avance se lee al dibujar */
@Composable
private fun Trayecto(clase: Sesion, color: Color, minuto: (() -> Int)?) {
    val l = letrasHorario()
    val estilo = l.dato.copy(color = Paleta.suave)
    Row(
        Modifier.padding(top = Espacio.l).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        BasicText(
            buildAnnotatedString {
                append(horaDe(clase.inicio, Tema.hora24))
                if (meridiano(clase.inicio, Tema.hora24) != meridiano(clase.fin, Tema.hora24)) withStyle(SPAN_MERIDIANO) { append(" ${meridiano(clase.inicio, Tema.hora24)}") }
            },
            style = estilo,
        )
        Canvas(Modifier.weight(1f).height(11.dp)) {
            val y = size.height / 2
            val g = 2.dp.toPx()
            val r = 3.5.dp.toPx()
            val avance = minuto?.let { ((it() - clase.inicio).toFloat() / clase.duracion).coerceIn(0f, 1f) } ?: 0f
            val x = size.width * avance
            drawLine(Paleta.tinta.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), g, StrokeCap.Round)
            if (avance > 0f) drawLine(color, Offset(0f, y), Offset(x, y), g)
            drawCircle(color, r, Offset(-1.dp.toPx() + r, y))
            val fin = Offset(size.width + 1.dp.toPx() - r, y)
            drawCircle(Paleta.lienzo, r, fin)
            drawCircle(color, r - 0.75.dp.toPx(), fin, style = Stroke(1.5.dp.toPx()))
            if (minuto != null) {
                drawCircle(Paleta.lienzo, 5.5.dp.toPx() + 3.dp.toPx(), Offset(x, y))
                drawCircle(color, 5.5.dp.toPx(), Offset(x, y))
            }
        }
        BasicText(
            buildAnnotatedString {
                append(horaDe(clase.fin, Tema.hora24))
                meridiano(clase.fin, Tema.hora24).takeIf { it.isNotEmpty() }?.let { withStyle(SPAN_MERIDIANO) { append(" $it") } }
            },
            style = estilo,
        )
    }
}

/* ---- Día: la semana en miniatura ------------------------------------------- */

/**
 * La semana en miniatura y el mando para cambiar de día: cada día enseña sus
 * clases a su hora y con su color. La lente que marca el elegido es un velo
 * plano, como el de la web, que corre hasta el día que se toca.
 */
@Composable
internal fun Semanita(horario: EstadoHorario) {
    val l = letrasHorario()
    val (abre, cierra) = horario.rango
    Row(Modifier.padding(top = Espacio.xxl).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText("SEMANA", Modifier.weight(1f), style = l.etiqueta)
        BasicText("${clases(horario.sesiones.size)} · ${duracion(horario.sesiones.sumOf { it.duracion })} · ${tramoCorto(abre, cierra, Tema.hora24)}", style = l.dato, maxLines = 1)
    }
    val lente = remember { Animatable(horario.dia.toFloat()) }
    LaunchedEffect(horario.dia) { lente.animateTo(horario.dia.toFloat(), tween(380, easing = Movimiento.FLUIDA)) }
    Row(
        Modifier
            .padding(top = Espacio.xs)
            .desbordar(7.dp)
            .drawBehind {
                val ancho = size.width / 5
                val m = 2.dp.toPx()
                val r = CornerRadius(13.dp.toPx())
                val forma = Size(ancho - 2 * m, size.height)
                drawRoundRect(Paleta.tinta.copy(alpha = 0.075f), Offset(lente.value * ancho + m, 0f), forma, r)
            },
    ) {
        for (d in 0 until 5) {
            val activo = horario.dia == d
            Column(
                Modifier.weight(1f).pulsable({ horario.dia = d }, escala = 0.96f).padding(start = if (ventana().compacta) Espacio.xxs else Espacio.xs + Espacio.hilo / 2, end = if (ventana().compacta) Espacio.xxs else Espacio.xs + Espacio.hilo / 2, top = Espacio.s, bottom = Espacio.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RotuloDiaHorario(DIAS_CORTOS[d], hoy = d == horario.ahora.dia, style = l.etiqueta.copy(color = if (activo) Paleta.tinta else Paleta.tenue))
                MiniDia(horario, d, abre, cierra, Modifier.padding(top = 9.dp).fillMaxWidth().height(66.dp))
            }
        }
    }
}

/** El nombre corto de un día, con el punto rojo de hoy a su izquierda sin moverlo del centro */
@Composable
internal fun RotuloDiaHorario(texto: String, hoy: Boolean, style: TextStyle, modifier: Modifier = Modifier) {
    BasicText(
        texto,
        modifier
            // El tracking deja aire a la derecha de la última letra; se compensa a la izquierda para que quede centrado
            .padding(start = 2.dp)
            .drawBehind {
                if (hoy) drawCircle(Paleta.rojo, 2.dp.toPx(), Offset(-5.dp.toPx(), size.height / 2))
            },
        style = style,
    )
}

@Composable
private fun MiniDia(horario: EstadoHorario, d: Int, abre: Int, cierra: Int, modifier: Modifier) {
    val colocadas = remember(horario.dias[d]) { enCarriles(horario.dias[d]) }
    val colores = colocadas.map { horario.color(it.sesion.codigo) }
    val esHoy = d == horario.ahora.dia
    Canvas(modifier) {
        val ahora = horario.ahora
        fun y(min: Int) = size.height * (min - abre) / (cierra - abre)
        drawRoundRect(Paleta.tinta.copy(alpha = 0.04f), cornerRadius = CornerRadius(5.dp.toPx()))
        val hueco = 1.dp.toPx()
        colocadas.forEachIndexed { i, c ->
            val w = size.width / c.carriles
            val alto = maxOf(y(c.sesion.fin) - y(c.sesion.inicio), 3.dp.toPx())
            val pasada = esHoy && c.sesion.fin <= ahora.minuto
            drawRoundRect(
                colores[i].copy(alpha = if (pasada) 0.3f else 0.58f),
                Offset(c.carril * w + if (c.carril > 0) hueco / 2 else 0f, y(c.sesion.inicio)),
                Size(w - if (c.carriles > 1) hueco / 2 else 0f, alto),
                CornerRadius(3.dp.toPx()),
            )
        }
        if (esHoy && ahora.minuto in abre..cierra) {
            val ya = y(ahora.minuto)
            val m = 3.dp.toPx()
            drawLine(Paleta.rojo, Offset(-m, ya), Offset(size.width + m, ya), 1.dp.toPx())
            drawCircle(Paleta.rojo, 2.5.dp.toPx(), Offset(-m - 1.dp.toPx() + 2.5.dp.toPx(), ya))
        }
    }
}

/* ---- Día: el día elegido como línea de tiempo ------------------------------ */
