package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.pulsable
import mpensum.logica.Sesion
import mpensum.tema.Paleta

/*
 * La pantalla inicial del horario (I1): sin clases, una captura gris de
 * INTRADACE se lee con una línea fina, cada clase se condensa en un punto de
 * su color, viaja por su curva y florece en su sitio de la semana; al final se
 * traza la línea roja de «ahora». Corre una vez. Con el movimiento reducido se
 * ve directamente el final.
 */

/** La semana de ejemplo: día, hora de entrada, de salida y cuál de los cinco colores */
private val EJEMPLO = listOf(
    listOf(0, 7, 9, 0), listOf(2, 7, 9, 0), listOf(1, 9, 11, 0), listOf(3, 9, 11, 0), listOf(0, 10, 12, 1),
    listOf(1, 13, 15, 2), listOf(3, 13, 15, 2), listOf(2, 10, 12, 3), listOf(4, 8, 10, 3), listOf(4, 10, 12, 4),
)

/** Cinco colores distintos de las materias de la carrera: los mismos que verá en su mapa */
@Composable
private fun rememberColoresEjemplo(horario: EstadoHorario): List<Color> {
    val carrera = horario.carrera
    return remember(carrera, Paleta.azul) {
        val propios = carrera.pensum.obligatorias.map { carrera.colores.de(it) }
            // Solo los que se distinguen del fondo: un gris apagado no dice nada en una semana de ejemplo
            .filter { maxOf(it.red, it.green, it.blue) - minOf(it.red, it.green, it.blue) > 0.3f }.distinct().take(5)
        propios + List(5 - propios.size) { Paleta.azul }
    }
}

/**
 * En vertical la escena va arriba y la invitación debajo; en horizontal sobra
 * ancho y falta alto: la escena a la izquierda y el texto con el botón a la derecha.
 */
@Composable
internal fun HorarioVacio(horario: EstadoHorario, arriba: Dp, abajo: Dp, modifier: Modifier, horizontal: Boolean) {
    val colores = rememberColoresEjemplo(horario)
    val plano = remember(colores) {
        val sesiones = EJEMPLO.mapIndexed { i, (d, a, b, _) -> Sesion("demo$i", "demo", d, a * 60, b * 60) }
        PlanoVuelo(sesiones, EJEMPLO.map { colores[it[3]] }, inicio = 1150f)
    }
    val reloj = rememberRelojVuelo(plano.total)
    val opciones = remember { OpcionesEscena(entrada = true, ahoraY = 134.5f) }
    if (horizontal) {
        Row(
            modifier.fillMaxSize().padding(top = arriba + 16.dp, bottom = abajo + 16.dp, start = MARGEN_HORARIO, end = MARGEN_HORARIO),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
        ) {
            EscenaVuelo(plano, opciones, { reloj.value })
            Invitacion(horario, Modifier.width(340.dp), horizontal = true)
        }
    } else {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = arriba + 76.dp, bottom = abajo + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EscenaVuelo(plano, opciones, { reloj.value })
            Spacer(Modifier.height(52.dp))
            Invitacion(horario, Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontal = false)
        }
    }
}

/** El rótulo, el título, el párrafo y los dos accesos: centrados bajo la escena y, al lado de ella, pegados a la izquierda */
@Composable
private fun Invitacion(horario: EstadoHorario, modifier: Modifier, horizontal: Boolean) {
    val l = letrasHorario()
    val alineado = if (horizontal) TextAlign.Start else TextAlign.Center
    Column(modifier, horizontalAlignment = if (horizontal) Alignment.Start else Alignment.CenterHorizontally) {
        BasicText("MI HORARIO", style = l.etiqueta)
        BasicText("Arma tu semana", Modifier.padding(top = 8.dp), style = l.titulo.copy(fontSize = 36.sp, lineHeight = 40.sp, textAlign = alineado))
        BasicText(
            "Sube la captura de tu horario de Intradace y la pasamos a tu semana, clase por clase.",
            Modifier.padding(top = 14.dp).widthIn(max = 300.dp),
            style = l.parrafo.copy(fontSize = 15.sp, lineHeight = 22.sp, textAlign = alineado),
        )
        BotonTinta("Subir mi horario", Modifier.padding(top = if (horizontal) 24.dp else 30.dp), icono = { trazarImagen(Paleta.lienzo) }, alPulsar = horario::subir)
        Box(
            Modifier.padding(top = 4.dp).fillMaxWidth().height(48.dp).pulsable({ horario.abrirNueva(if (horario.ahora.dia < 5) horario.ahora.dia else 0, 7 * 60) }, escala = 0.96f),
            contentAlignment = Alignment.Center,
        ) { BasicText("Crearlo manualmente", style = l.accion.copy(fontSize = 15.sp, color = Paleta.suave)) }
        SelloIntradace(Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally))
    }
}

/** «Optimizado para INTRADACE», el sello de la pantalla */
@Composable
internal fun SelloIntradace(modifier: Modifier = Modifier) {
    val l = letrasHorario()
    BasicText(
        buildAnnotatedString {
            append("Optimizado para ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.18.em, color = Paleta.suave)) { append("INTRADACE") }
        },
        modifier,
        style = l.dato.copy(color = Paleta.tenue),
    )
}

/** El botón lleno en tinta, el único de la pantalla */
@Composable
internal fun BotonTinta(texto: String, modifier: Modifier = Modifier, icono: (DrawScope.() -> Unit)? = null, alPulsar: () -> Unit) {
    val l = letrasHorario()
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .pulsable(alPulsar, atenuar = 0.88f)
            .clip(RoundedCornerShape(27.dp))
            .drawBehind { drawRect(Paleta.tinta) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icono != null) {
            Canvas(Modifier.size(20.dp), onDraw = icono)
            Spacer(Modifier.size(9.dp))
        }
        BasicText(texto, style = l.boton.copy(fontSize = 16.sp))
    }
}

/** El icono de imagen: un marco, un sol y una montaña, en la rejilla de 24 */
internal fun DrawScope.trazarImagen(color: Color) {
    val k = size.width / 24f
    val g = Stroke(1.8f * k, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    drawRoundRect(color, Offset(3.5f * k, 4.5f * k), Size(17f * k, 15f * k), CornerRadius(2.5f * k), style = g)
    drawCircle(color, 1.6f * k, Offset(9f * k, 10f * k), style = g)
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(20f * k, 16f * k); lineTo(15.5f * k, 11.5f * k); lineTo(6f * k, 19.5f * k)
    }
    drawPath(p, color, style = g)
}
