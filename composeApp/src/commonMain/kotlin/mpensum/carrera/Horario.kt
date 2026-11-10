package mpensum.carrera

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpensum.componentes.pulsable
import mpensum.componentes.margenPantalla
import mpensum.componentes.ventana
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import kotlin.math.min

/*
 * El horario de la carrera, en una sola forma: qué toca, la semana en
 * miniatura y el día elegido como línea de tiempo. La rejilla de la semana
 * (HorarioSemana.kt) solo se usa para revisar lo leído de una captura.
 * Las hojas (detalle, nueva clase, más) viven en HojasHorario.kt y se montan
 * fuera de la vista, encima de la barra, como las demás hojas del interior.
 */

/* ---- Medidas y letras ------------------------------------------------------ */

internal val CURVA_LATIDO = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
/** Ancho del carril de las horas en la lista del día (--ancho-hora de la web) */
private val ANCHO_HORA = 72.dp
/** En pantallas compactas (< 360 dp) el carril de horas cede 8 dp al nombre */
private val ANCHO_HORA_ESTRECHO = 64.dp
/** El riel y su aire a cada lado: la hora, el riel y el texto comparten estas tres columnas en todos los renglones */
internal val ANCHO_RIEL = 9.dp
internal val AIRE_RIEL = 12.dp
/** El hueco de un botón de icono al borde derecho: el toque mide 44 y el icono queda a 8 dp del margen, como el -mr de la web */
internal val TOQUE = Alto.toque
internal val SALIDA_DERECHA = 14.dp

/** Las letras del horario, resueltas una vez: Jost nombra y da la hora, Inter cuenta */
@Immutable
internal class LetrasHorario(j: FontFamily, i: FontFamily) {
    private val numeros = "tnum"
    /** Rótulo: Inter 500, 11/16, +8 %, en mayúsculas */
    val etiqueta = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em, color = Paleta.tenue, fontFeatureSettings = numeros)
    /** Titular: Jost 300, 40 con interlínea de 1,1 em y −2 %; el que lo pone le añade el `autoSize` */
    val titulo = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 1.1.em, letterSpacing = (-0.02).em, color = Paleta.tinta)
    val tituloHoja = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em, color = Paleta.tinta)
    val horaGrande = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta, fontFeatureSettings = numeros)
    /** La hora de entrada de una fila del día, grande y fina; la de salida, pequeña */
    val horaEntrada = TextStyle(fontFamily = j, fontWeight = FontWeight.Light, fontSize = 21.sp, lineHeight = 21.sp, letterSpacing = (-0.01).em, color = Paleta.tinta, fontFeatureSettings = numeros)
    val horaSalida = TextStyle(fontFamily = i, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = (-0.003).em, color = Paleta.tenue, fontFeatureSettings = numeros)
    /** Fila: Inter 500, 15/20, −0,9 % */
    val nombreFila = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em, color = Paleta.tinta, fontFeatureSettings = numeros)
    val bloque = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, color = Paleta.tinta)
    /** Cuerpo: Inter 400, 15/20, −0,9 %: «Quedan 1 h 20 min · Aula 5» y la clase de después */
    val cuerpo = TextStyle(fontFamily = i, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em, color = Paleta.suave, fontFeatureSettings = numeros)
    /** Nota: Inter 400, 13/16, −0,3 % */
    val texto = TextStyle(fontFamily = i, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = (-0.003).em, color = Paleta.tenue, fontFeatureSettings = numeros)
    val parrafo = texto.copy(lineHeight = 20.sp, color = Paleta.suave)
    val dato = texto
    val pequeno = TextStyle(fontFamily = i, fontSize = 11.sp, lineHeight = 14.sp, color = Paleta.tenue, fontFeatureSettings = numeros)
    /** La unidad de la hora: 11 sp, un peso más que la cifra, tenue */
    val meridiano = TextStyle(fontFamily = i, fontSize = 11.sp, color = Paleta.tenue)
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

/** El meridiano pegado a una hora, como unidad suya: la hora es el número, «AM» solo desambigua */
internal val SPAN_MERIDIANO get() = SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = Paleta.tenue)

/* ---- Iconos (propios del horario) ------------------------------------------ */

/** Un icono callado al borde derecho: sin fondo, con su toque de 44 y el dibujo a 8 dp del margen */
@Composable
internal fun IconoDerecho(tamano: Dp, alPulsar: () -> Unit, modifier: Modifier = Modifier, dibujo: DrawScope.() -> Unit) {
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
    val margen = margenPantalla()
    if (horizontal) {
        // Acostado, dos columnas que se desplazan cada una por su lado: lo de
        // ahora y la semanita a la izquierda, el día elegido a la derecha
        Row(modifier.fillMaxSize().padding(start = margen, end = margen), horizontalArrangement = Arrangement.spacedBy(Espacio.xxl)) {
            Column(
                Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState())
                    .padding(top = arriba + Espacio.xs, bottom = abajo + Espacio.l),
            ) {
                Cabecera(horario)
                AhoraYDespues(horario)
                Semanita(horario)
            }
            Column(
                Modifier.weight(0.58f).fillMaxHeight().verticalScroll(rememberScrollState())
                    .padding(top = arriba + Espacio.xs, bottom = abajo + Espacio.l),
            ) {
                DiaElegido(horario, arriba = 0.dp)
            }
        }
        return
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = margen, end = margen, top = arriba + Espacio.xs, bottom = abajo + Espacio.xl + Espacio.xxs),
    ) {
        Cabecera(horario)
        AhoraYDespues(horario)
        Semanita(horario)
        DiaElegido(horario, arriba = Espacio.xl + Espacio.xxs)
    }
}

/** El carril de las horas del día: más angosto en compactas */
@Composable
internal fun anchoHora(): Dp = if (ventana().compacta) ANCHO_HORA_ESTRECHO else ANCHO_HORA
