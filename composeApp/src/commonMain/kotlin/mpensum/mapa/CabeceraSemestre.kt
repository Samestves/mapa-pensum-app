package mpensum.mapa

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mpensum.componentes.iconoEstado
import mpensum.logica.CuentaSemestre
import mpensum.logica.MarcaSemestre
import mpensum.logica.Situacion
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * La cabecera de cada semestre en el mapa (RotulosGrafo.jsx, cabeceras.js y
 * GlifoSemestre.jsx). Todo contra cuatro líneas base fijas, como en la web:
 * el título, los datos con el porcentaje grande, la barra y el pie.
 */

private object Lineas {
    const val TITULO = 14f
    const val DATOS = 31f
    const val BARRA = 44f
    const val PIE = 64f
}

/** La casilla de marcar el semestre, a la izquierda del título; el título corrido lo que ocupa */
private const val CASILLA_ARRIBA = 2.8f
private const val SANGRIA_TITULO = 21f
private const val ALTO_CASILLA = 44f

/** Lo que dice la cabecera de un semestre */
data class DatosCabecera(
    val semestre: Int,
    val x: Float,
    val uc: Int,
    val total: Int,
    val hechas: Int,
    val cursando: Int,
    val inscribibles: Int,
    val casilla: CuentaSemestre?,
) {
    val completo get() = total > 0 && hechas == total
    val porcentaje get() = if (total > 0) kotlin.math.round(hechas * 100.0 / total).toInt() else 0
}

/** La línea base de un texto a la altura `y` (como el ::before de 40 px de la web) */
private fun Modifier.enBase(x: Float, y: Float, tamano: Float) = offset(x.dp, (y - tamano * 0.93f).dp)

@Composable
fun CabeceraSemestre(datos: DatosCabecera, tipos: TiposMapa, medidor: TextMeasurer, alAlternar: () -> Unit, compacta: Boolean = false) {
    if (compacta) {
        CabeceraCompacta(datos, tipos, alAlternar)
        return
    }
    val ancho = Nodo.ANCHO.toFloat()
    val anchoHechas by animateFloatAsState(if (datos.total > 0) ancho * datos.hechas / datos.total else 0f, tween(600, easing = Movimiento.FLUIDA), label = "hechas")
    val anchoCursando by animateFloatAsState(if (datos.total > 0) ancho * datos.cursando / datos.total else 0f, tween(600, easing = Movimiento.FLUIDA), label = "cursando")
    val faltaElegir = datos.casilla?.marca == MarcaSemestre.FALTA_ELEGIR
    val pie = when {
        datos.completo -> "COMPLETO"
        faltaElegir -> datos.casilla!!.huecos.let { if (it == 1) "FALTA ELEGIR LA ELECTIVA" else "FALTA ELEGIR $it ELECTIVAS" }
        else -> "${datos.hechas}/${datos.total} APROBADAS"
    }

    Box(
        Modifier
            .offset(datos.x.dp, Margen.ARRIBA.toFloat().dp)
            .size(ancho.dp, (Lineas.PIE + 8).dp)
            .drawBehind {
                val u = density
                val y = Lineas.BARRA * u
                drawRect(Paleta.tinta.copy(alpha = 0.1f), Offset(0f, y), Size(size.width, 2 * u))
                drawRect(Paleta.verde, Offset(0f, y), Size(anchoHechas * u, 2 * u))
                dibujarContorno(anchoHechas * u, y, anchoCursando * u, 2 * u)
                // Los estados de derecha a izquierda contra el borde: solo los que tienen alguna materia
                var hasta = size.width
                for ((s, n) in listOf(Situacion.INSCRIBIBLE to datos.inscribibles, Situacion.CURSANDO to datos.cursando)) {
                    if (n == 0) continue
                    val texto = "$n"
                    val inicio = hasta - texto.length * 11 * 0.6f * u - 16 * u
                    iconoEstado(s, inicio, (Lineas.PIE - 10) * u, 12 * u, Paleta.lienzo)
                    val medida = medidor.measure(texto, tipos.cuenta.copy(color = fuerte(s)))
                    drawText(medida, topLeft = Offset(inicio + 16 * u, Lineas.PIE * u - medida.firstBaseline))
                    hasta = inicio - 12 * u
                }
            },
    ) {
        BasicText("SEMESTRE ${datos.semestre.toString().padStart(2, '0')}", Modifier.enBase(SANGRIA_TITULO, Lineas.TITULO, 11.5f), style = tipos.titulo)
        BasicText(
            "${datos.uc} UC · ${datos.total} ${if (datos.total == 1) "MATERIA" else "MATERIAS"}",
            Modifier.enBase(0f, Lineas.DATOS, 10f),
            style = tipos.datos,
        )
        // La cifra del instrumento, contra el borde derecho; el % más pequeño y apagado: es la unidad, no el dato
        BasicText(
            buildAnnotatedString {
                append("${datos.porcentaje}")
                withStyle(SpanStyle(fontSize = tipos.unidad.fontSize, fontWeight = tipos.unidad.fontWeight, color = tipos.unidad.color, fontFamily = tipos.unidad.fontFamily)) { append(" %") }
            },
            Modifier.align(Alignment.TopEnd).offset(y = (Lineas.DATOS - 32 * 0.93f).dp),
            style = tipos.cifra.copy(color = if (datos.completo) Paleta.verde else Paleta.tinta),
        )
        BasicText(pie, Modifier.enBase(0f, Lineas.PIE, 9.5f), style = tipos.pie.copy(color = if (datos.completo) Paleta.verde else Paleta.tenue))

        // La casilla: una zona de pulsar amplia que abarca casilla y título; a la escala del mapa entero nadie acierta catorce píxeles
        datos.casilla?.let { cuenta -> CasillaSemestre(datos.semestre, cuenta, alAlternar) }
    }
}

/** La barra de avance de la cabecera compacta: bajo la línea de texto */
private const val BARRA_COMPACTA = 26f

/**
 * La cabecera de una sola línea ("SEMESTRE 01 · 17 UC · 0/6 · 0 %") con la barra
 * debajo, para el mapa en horizontal: baja de 84 a 40 unidades del plano y la
 * columna gana una fila de alto. Mismos tipos y misma casilla que la normal.
 */
@Composable
private fun CabeceraCompacta(datos: DatosCabecera, tipos: TiposMapa, alAlternar: () -> Unit) {
    val ancho = Nodo.ANCHO.toFloat()
    val anchoHechas by animateFloatAsState(if (datos.total > 0) ancho * datos.hechas / datos.total else 0f, tween(600, easing = Movimiento.FLUIDA), label = "hechas")
    val anchoCursando by animateFloatAsState(if (datos.total > 0) ancho * datos.cursando / datos.total else 0f, tween(600, easing = Movimiento.FLUIDA), label = "cursando")
    val apagado = SpanStyle(fontFamily = tipos.datos.fontFamily, fontWeight = tipos.datos.fontWeight, fontSize = tipos.datos.fontSize, color = Paleta.tenue, letterSpacing = tipos.datos.letterSpacing)
    Box(
        Modifier
            .offset(datos.x.dp, Margen.ARRIBA.toFloat().dp)
            .size(ancho.dp, (BARRA_COMPACTA + 2).dp)
            .drawBehind {
                val u = density
                val y = BARRA_COMPACTA * u
                drawRect(Paleta.tinta.copy(alpha = 0.1f), Offset(0f, y), Size(size.width, 2 * u))
                drawRect(Paleta.verde, Offset(0f, y), Size(anchoHechas * u, 2 * u))
                dibujarContorno(anchoHechas * u, y, anchoCursando * u, 2 * u)
            },
    ) {
        BasicText(
            buildAnnotatedString {
                append("SEMESTRE ${datos.semestre.toString().padStart(2, '0')}")
                withStyle(apagado) { append(" · ${datos.uc} UC · ${datos.hechas}/${datos.total} · ") }
                withStyle(apagado.copy(color = if (datos.completo) Paleta.verde else Paleta.suave)) { append("${datos.porcentaje} %") }
            },
            Modifier.enBase(SANGRIA_TITULO, Lineas.TITULO, 11.5f),
            style = tipos.titulo.copy(fontSize = tipos.datos.fontSize),
            maxLines = 1,
            softWrap = false,
        )
        datos.casilla?.let { cuenta -> CasillaSemestre(datos.semestre, cuenta, alAlternar) }
    }
}

/**
 * La casilla de marcar el semestre: una zona de pulsar amplia que abarca casilla y título (44 dp de alto en el
 * plano, para que con el mapa alejado siga siendo un blanco). La lee el lector de pantalla como una casilla con nombre y estado.
 */
@Composable
private fun CasillaSemestre(semestre: Int, cuenta: CuentaSemestre, alAlternar: () -> Unit) {
    val completo = cuenta.marca == MarcaSemestre.COMPLETO
    Box(
        Modifier
            .offset((-6).dp, (CASILLA_ARRIBA - 16).dp)
            .size(150.dp, ALTO_CASILLA.dp)
            .semantics {
                contentDescription = if (completo) "Desmarcar el semestre $semestre" else "Aprobar el semestre $semestre"
                toggleableState = if (completo) ToggleableState.On else ToggleableState.Off
            }
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox, onClick = alAlternar),
    ) {
        Canvas(Modifier.offset(6.dp, 16.dp).size(14.dp)) { glifoSemestre(cuenta) }
    }
}

/* El aro de la casilla (layout/tramos.js): un tramo por materia, en una caja de 14 */
private const val CENTRO = 7f
private const val RADIO = 5.9f

private fun tramo(desde: Float, hasta: Float): Path = Path().apply {
    val pasos = 24
    for (k in 0..pasos) {
        val a = 2 * PI * (desde + (hasta - desde) * k / pasos)
        val px = CENTRO + RADIO * sin(a).toFloat()
        val py = CENTRO - RADIO * cos(a).toFloat()
        if (k == 0) moveTo(px, py) else lineTo(px, py)
    }
}

private fun DrawScope.glifoSemestre(c: CuentaSemestre) {
    val u = size.width / 14f
    scaleTo(u) {
        if (c.marca == MarcaSemestre.COMPLETO) {
            drawCircle(Paleta.verde, 7f, Offset(CENTRO, CENTRO))
            drawPath(Path().apply { moveTo(4f, 7.3f); lineTo(6.2f, 9.5f); lineTo(10.3f, 4.9f) }, Paleta.lienzo, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            return@scaleTo
        }
        val hueco = if (c.total <= 1) 0f else if (c.total > 7) 0.04f else 0.05f
        val finCursando = c.hechas + c.cursando
        val finPendientes = c.total - c.huecos
        for (i in 0 until c.total) {
            val (color, grosor) = when {
                i < c.hechas -> Paleta.verde to 2.2f
                i < finCursando -> Paleta.cursando.copy(alpha = 0.45f) to 2.2f
                i < finPendientes -> Paleta.tinta.copy(alpha = 0.24f) to 1.5f
                else -> Paleta.tinta.copy(alpha = 0.16f) to 1.5f
            }
            drawPath(tramo(i.toFloat() / c.total + hueco / 2, (i + 1f) / c.total - hueco / 2), color, style = Stroke(grosor))
        }
        if (c.marca == MarcaSemestre.FALTA_ELEGIR) {
            drawPath(Path().apply { moveTo(7f, 4.6f); lineTo(7f, 9.4f); moveTo(4.6f, 7f); lineTo(9.4f, 7f) }, Paleta.tinta, style = Stroke(1.4f, cap = StrokeCap.Round))
        }
    }
}

private inline fun DrawScope.scaleTo(factor: Float, crossinline bloque: DrawScope.() -> Unit) {
    scale(factor, Offset.Zero) { bloque() }
}

/** «Cursando» en una barra: contorno fino en tinta, sin relleno */
private fun DrawScope.dibujarContorno(x: Float, y: Float, ancho: Float, alto: Float) {
    val g = density * 0.8f
    if (ancho <= 2 * g) return
    drawRect(Paleta.cursando, Offset(x + g / 2, y + g / 2), Size(ancho - g, alto - g), style = Stroke(g))
}
