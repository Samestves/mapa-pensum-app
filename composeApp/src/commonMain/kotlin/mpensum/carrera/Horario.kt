package mpensum.carrera

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpensum.componentes.desbordar
import mpensum.componentes.pulsable
import mpensum.componentes.trazarCruz
import mpensum.componentes.trazarPuntos
import mpensum.componentes.margenPantalla
import mpensum.componentes.ventana
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.logica.DIAS
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.Momento
import mpensum.logica.Renglon
import mpensum.logica.Sesion
import mpensum.logica.agendaDe
import mpensum.logica.clases
import mpensum.logica.cuandoViene
import mpensum.logica.duracion
import mpensum.logica.enCarriles
import mpensum.logica.enDoceHoras
import mpensum.logica.horaDe
import mpensum.logica.inicioParaAnadir
import mpensum.logica.lugarDe
import mpensum.logica.meridiano
import mpensum.logica.tituloLibre
import mpensum.logica.tramoCorto
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import kotlin.math.min

/*
 * El horario de la carrera, en dos formas: Día (qué toca, la semana en
 * miniatura y el día elegido como línea de tiempo) y Semana (la rejilla).
 * Las hojas (detalle, nueva clase, más) viven en HojasHorario.kt y se montan
 * fuera de la vista, encima de la barra, como las demás hojas del interior.
 */

/* ---- Medidas y letras ------------------------------------------------------ */

internal val CURVA_HORARIO = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val CURVA_SEGMENTO = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val CURVA_LATIDO = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
/** El margen de la vista: el px-5 de la web en el teléfono */
internal val MARGEN_HORARIO = Espacio.l
/** Ancho del carril de las horas en la lista del día (--ancho-hora de la web) */
private val ANCHO_HORA = 72.dp
/** En pantallas compactas (< 360 dp) el carril de horas cede 8 dp al nombre */
private val ANCHO_HORA_ESTRECHO = 64.dp
/** El selector Día | Semana: cada opción mide 66 dp, y 58 en compactas para que quepa junto al rótulo */
private val ANCHO_OPCION = 66.dp
private val ANCHO_OPCION_ESTRECHA = 58.dp
/** El riel y su aire a cada lado: la hora, el riel y el texto comparten estas tres columnas en todos los renglones */
private val ANCHO_RIEL = 9.dp
private val AIRE_RIEL = 12.dp
/** El hueco de un botón de icono al borde derecho: el toque mide 44 y el icono queda a 8 dp del margen, como el -mr de la web */
private val TOQUE = Alto.toque
private val SALIDA_DERECHA = 14.dp

/** Las letras del horario, resueltas una vez: Jost nombra y da la hora, Inter cuenta */
@Immutable
internal class LetrasHorario(j: FontFamily, i: FontFamily) {
    private val numeros = "tnum"
    /** El rótulo en mayúsculas finas y espaciadas (.rotulo-horario de la web, a 11 como mínimo) */
    val etiqueta = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.em, color = Paleta.tenue)
    val titulo = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 37.sp, letterSpacing = (-0.015).em, color = Paleta.tinta)
    val tituloHoja = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em, color = Paleta.tinta)
    val horaGrande = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta, fontFeatureSettings = numeros)
    /** La hora de entrada de una fila del día, grande y fina; la de salida, pequeña */
    val horaEntrada = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 21.sp, lineHeight = 21.sp, letterSpacing = (-0.01).em, color = Paleta.tinta, fontFeatureSettings = numeros)
    val horaSalida = TextStyle(fontFamily = i, fontSize = 12.5.sp, lineHeight = 13.sp, color = Paleta.tenue, fontFeatureSettings = numeros)
    val nombreFila = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 15.5.sp, lineHeight = 20.sp, letterSpacing = (-0.012).em, color = Paleta.tinta)
    val bloque = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, color = Paleta.tinta)
    /** El cuerpo del momento: «Quedan 1 h 20 min · Aula 5» y la clase de después */
    val cuerpo = TextStyle(fontFamily = i, fontSize = 14.sp, lineHeight = 20.sp, color = Paleta.suave, fontFeatureSettings = numeros)
    val texto = TextStyle(fontFamily = i, fontSize = 13.sp, lineHeight = 17.sp, color = Paleta.tenue, fontFeatureSettings = numeros)
    val parrafo = texto.copy(lineHeight = 20.sp, color = Paleta.suave)
    val dato = TextStyle(fontFamily = i, fontSize = 12.5.sp, lineHeight = 16.sp, color = Paleta.tenue, fontFeatureSettings = numeros)
    val pequeno = TextStyle(fontFamily = i, fontSize = 11.sp, lineHeight = 14.sp, color = Paleta.tenue, fontFeatureSettings = numeros)
    val meridiano = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 8.5.sp, letterSpacing = 0.1.em, color = Paleta.tenue)
    val accion = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Paleta.tinta)
    val boton = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = Paleta.lienzo)
}

@Composable
internal fun letrasHorario(): LetrasHorario {
    val j = jost()
    val i = inter()
    // Los colores van dentro de cada estilo: cambian con el tema
    val claro = Tema.claro
    return remember(j, i, claro) { LetrasHorario(j, i) }
}

/** El meridiano pegado a una hora, pequeño y espaciado: la hora es el número, «AM» solo desambigua */
private val SPAN_MERIDIANO get() = SpanStyle(fontSize = 8.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.em, color = Paleta.tenue)

/* ---- Iconos (propios del horario) ------------------------------------------ */

/** Un icono callado al borde derecho: sin fondo, con su toque de 44 y el dibujo a 8 dp del margen */
@Composable
private fun IconoDerecho(tamano: Dp, alPulsar: () -> Unit, modifier: Modifier = Modifier, dibujo: DrawScope.() -> Unit) {
    Box(
        modifier.offset(x = SALIDA_DERECHA).size(TOQUE).pulsable(alPulsar, escala = 0.9f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(tamano), onDraw = dibujo)
    }
}

/* ---- La vista ---------------------------------------------------------------- */

/**
 * La vista Horario de una carrera, como la agenda de la web en el teléfono:
 * sin cajas, las cosas separadas por aire y por el riel de cada clase.
 * `abajo` es el alto que tapa la barra Núcleo desde el borde de la pantalla
 * (barra del sistema incluida): el contenido termina por encima. Las hojas no
 * van aquí: las monta `HojasHorario` encima de la barra.
 */
@Composable
fun VistaHorario(horario: EstadoHorario, abajo: Dp, modifier: Modifier = Modifier, horizontal: Boolean) {
    // El "ahora" cambia una vez por minuto. Se relee cada 15 s como mucho porque
    // el reloj del sistema se para cuando el teléfono duerme y la espera llegaría tarde
    LaunchedEffect(horario) {
        while (true) delay(min(15_000L, horario.actualizarAhora() + 50))
    }
    val arriba = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val lectura = horario.lectura
    if (lectura != null) {
        LecturaHorario(horario, lectura, arriba, abajo, modifier, horizontal)
        return
    }
    if (horario.sesiones.isEmpty()) {
        HorarioVacio(horario, arriba, abajo, modifier, horizontal)
        return
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = margenPantalla(), end = margenPantalla(), top = arriba + Espacio.xs, bottom = abajo + Espacio.xl + Espacio.xxs),
    ) {
        Cabecera(horario, horizontal)
        Crossfade(horario.modo, animationSpec = tween(220), label = "modo") { modo ->
            if (modo == ModoHorario.DIA) {
                Column {
                    AhoraYDespues(horario)
                    Semanita(horario)
                    DiaElegido(horario)
                }
            } else {
                SemanaRejilla(horario, compacto = horizontal)
            }
        }
    }
}

/**
 * El renglón de arriba, como el de la web: el rótulo del momento a la
 * izquierda y, a la derecha y callados, Día · Semana y el «⋯». Sin pastilla
 * de fondo para el selector: solo la opción elegida lleva un velo.
 */
@Composable
private fun Cabecera(horario: EstadoHorario, compacta: Boolean = false) {
    val l = letrasHorario()
    Row(Modifier.fillMaxWidth().height(TOQUE), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            val momento = horario.momento
            if (horario.modo == ModoHorario.SEMANA) {
                BasicText("TU SEMANA", style = l.etiqueta)
                // En horizontal sobra ancho: el resumen de la semana va en la misma línea
                if (compacta) BasicText(resumenSemana(horario.sesiones), style = l.texto, maxLines = 1)
            } else {
                val enCurso = momento is Momento.EnCurso
                if (momento is Momento.EnCurso && !Tema.sinBucles) PuntoQueLate(horario.color(momento.clase.codigo))
                BasicText(
                    when (momento) {
                        is Momento.EnCurso -> "EN CURSO"
                        is Momento.Luego -> "PRÓXIMA CLASE"
                        else -> "HOY"
                    },
                    style = l.etiqueta.copy(color = if (enCurso) Paleta.tinta else Paleta.tenue),
                )
            }
        }
        SelectorModo(horario)
        IconoDerecho(18.dp, { horario.hoja = HojaHorario.Menu }) { trazarPuntos(Paleta.suave) }
    }
}

@Composable
private fun SelectorModo(horario: EstadoHorario) {
    val l = letrasHorario()
    val posicion = remember { Animatable(horario.modo.ordinal.toFloat()) }
    LaunchedEffect(horario.modo) { posicion.animateTo(horario.modo.ordinal.toFloat(), tween(320, easing = CURVA_SEGMENTO)) }
    Row(
        Modifier
            .fillMaxHeight()
            .drawBehind {
                // El velo de la opción elegida corre por debajo del texto, en el dibujo y no en el layout
                val alto = 30.dp.toPx()
                val ancho = size.width / 2
                drawRoundRect(
                    Paleta.tinta.copy(alpha = 0.09f),
                    Offset(posicion.value * ancho, (size.height - alto) / 2),
                    Size(ancho, alto),
                    CornerRadius(alto / 2),
                )
            },
    ) {
        ModoHorario.entries.forEach { modo ->
            Box(
                Modifier.width(if (ventana().compacta) ANCHO_OPCION_ESTRECHA else ANCHO_OPCION).fillMaxHeight().pulsable({ horario.modo = modo }, escala = 0.96f),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    if (modo == ModoHorario.DIA) "Día" else "Semana",
                    style = l.accion.copy(color = if (horario.modo == modo) Paleta.tinta else Paleta.tenue),
                )
            }
        }
    }
}

/* ---- Día: lo que toca ahora --------------------------------------------- */

/** El carril de las horas del día: más angosto en compactas */
@Composable
private fun anchoHora(): Dp = if (ventana().compacta) ANCHO_HORA_ESTRECHO else ANCHO_HORA

/** Lo que se sangra el texto de un renglón del día: la hora, el riel y su aire */
@Composable
private fun sangriaDia(): Dp = anchoHora() + AIRE_RIEL + ANCHO_RIEL + AIRE_RIEL

/** El nombre manda: baja un cuerpo si es largo, como el NOMBRE_LARGO de la web */
private fun LetrasHorario.tituloPara(nombre: String) =
    if (nombre.length > 26) titulo.copy(fontSize = 27.sp, lineHeight = 30.sp) else titulo

/**
 * Lo primero que dice el horario: qué toca y qué viene. Sin tarjeta: el
 * título es la página, y el trayecto de la clase en curso es el único dibujo.
 */
@Composable
private fun AhoraYDespues(horario: EstadoHorario) {
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
        BasicText(titulo, Modifier.padding(top = Espacio.xxs), style = l.tituloPara(titulo))

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
                append(listOfNotNull(enDoceHoras(s.inicio), s.aula.ifBlank { null }).joinToString(" · ").replace(' ', ' '))
            },
            style = l.cuerpo,
        )
    }
}

/** El punto de «En curso»: un aro que se expande y se apaga, dibujado; no recompone nada */
@Composable
private fun PuntoQueLate(color: Color) {
    val t by rememberInfiniteTransition(label = "latido").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "latido",
    )
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
                append(horaDe(clase.inicio))
                if (meridiano(clase.inicio) != meridiano(clase.fin)) withStyle(SPAN_MERIDIANO) { append(" ${meridiano(clase.inicio)}") }
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
                append(horaDe(clase.fin))
                withStyle(SPAN_MERIDIANO) { append(" ${meridiano(clase.fin)}") }
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
private fun Semanita(horario: EstadoHorario) {
    val l = letrasHorario()
    val (abre, cierra) = horario.rango
    Row(Modifier.padding(top = Espacio.xxl).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText("SEMANA", Modifier.weight(1f), style = l.etiqueta)
        BasicText(tramoCorto(abre, cierra), style = l.dato)
    }
    val lente = remember { Animatable(horario.dia.toFloat()) }
    LaunchedEffect(horario.dia) { lente.animateTo(horario.dia.toFloat(), tween(380, easing = CURVA_HORARIO)) }
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

/**
 * Un día leído de arriba abajo: cada clase con su tramo, lo libre entre una y
 * otra y, al final, dónde añadir la siguiente. Tres columnas compartidas (la
 * hora, el riel y lo que es) hacen que el día se lea como una sola línea.
 */
@Composable
private fun DiaElegido(horario: EstadoHorario) {
    val l = letrasHorario()
    val d = horario.dia
    val delDia = horario.dias[d]
    val esHoy = d == horario.ahora.dia
    Row(Modifier.padding(top = Espacio.xl + Espacio.xxs).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Paleta.tinta)) { append(DIAS[d].uppercase()) }
                if (esHoy) append(" · HOY")
            },
            Modifier.weight(1f),
            style = l.etiqueta,
        )
        BasicText(
            if (delDia.isEmpty()) "Sin clases" else "${clases(delDia.size)} · sales ${enDoceHoras(delDia.maxOf { it.fin })}",
            style = l.dato,
        )
    }
    Column(Modifier.padding(top = Espacio.l).fillMaxWidth()) {
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
        if (delDia.isEmpty()) {
            BasicText("Este día no tienes clases.", Modifier.padding(start = sangriaDia(), bottom = Espacio.xxs), style = l.texto)
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
            HoraConMeridiano(horaDe(s.inicio), meridiano(s.inicio), entrada.copy(color = if (pasada) Paleta.tenue else Paleta.tinta), l)
            // La de salida solo repite el meridiano si cambia: "10:00 AM" arriba, "12:00 PM" abajo; "7:00 AM", "9:00"
            HoraConMeridiano(horaDe(s.fin), if (meridiano(s.fin) != meridiano(s.inicio)) meridiano(s.fin) else "", l.horaSalida, l)
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
                style = l.dato.copy(fontSize = 12.sp, color = if (enCurso) Paleta.suave else Paleta.tenue),
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
        BasicText(mer, Modifier.width(18.dp).padding(start = 3.dp).alignByBaseline(), style = l.meridiano)
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
