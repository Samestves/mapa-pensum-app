package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import mpensum.logica.EstadoCapsula
import mpensum.logica.SemestreCapsulas
import mpensum.logica.semestreActual
import mpensum.tema.Paleta
import mpensum.tema.jost

/*
 * La barra de la carrera en cápsulas (docs/refinado-progreso.md, «Barra de la
 * carrera: cápsulas»): una cápsula por materia, ancha según sus UC y todas de
 * 14 dp de alto, con 2 dp entre materias y 7 entre semestres. Aprobada llena
 * de tinta, cursando en contorno de 1,5 dp y el resto con la tinta al 13 %
 * (15 % en oscuro). Debajo, a 6 dp, la regla de semestres: un hilo, una marca
 * en el centro de cada uno y su número en Jost 11; el actual en un círculo de
 * tinta. Se dibuja de una vez en un Canvas, sin un nodo por cápsula.
 */

private val ALTO_CAPSULA = 14.dp
private val RADIO_CAPSULA = 2.5.dp
private val HUECO_MATERIA = 2.dp
private val HUECO_SEMESTRE = 7.dp
private val CONTORNO = 1.5.dp

/** Lo que la regla pide por debajo de las cápsulas: 6 hasta el hilo y 26 para la marca y el número */
private val ALTO_REGLA = 32.dp

@Composable
internal fun BarraCapsulas(semestres: List<SemestreCapsulas>, modifier: Modifier = Modifier) {
    if (semestres.isEmpty()) return
    val medidor = rememberTextMeasurer()
    val familia = jost()
    val actual = semestreActual(semestres)
    val tinta = Paleta.tinta
    val panel = Paleta.panel
    val apagada = Paleta.apagada
    val hilo = tinta.copy(alpha = Velo.hilo)
    val pendiente = tinta.copy(alpha = Velo.pendiente)
    // La regla es un dibujo: su número no crece con la letra del sistema, como el círculo que lo rodea
    val tamNumero = with(LocalDensity.current) { 11.dp.toSp() }
    // Los números se miden una vez por barra y tema, no en cada recomposición (son más que la caché del medidor)
    val numeros = remember(semestres, actual, medidor, familia, tamNumero, panel, tinta, apagada) {
        semestres.map { s ->
            val fuerte = s.numero == actual || s.completo
            medidor.measure(
                "${s.numero}",
                TextStyle(
                    fontFamily = familia,
                    fontWeight = if (fuerte) FontWeight.Medium else FontWeight.Normal,
                    fontSize = tamNumero,
                    color = when {
                        s.numero == actual -> panel
                        s.completo -> tinta
                        else -> apagada
                    },
                ),
            )
        }
    }
    val aprobadas = semestres.sumOf { s -> s.capsulas.count { it.estado == EstadoCapsula.APROBADA } }
    val cursando = semestres.sumOf { s -> s.capsulas.count { it.estado == EstadoCapsula.CURSANDO } }
    val total = semestres.sumOf { it.capsulas.size }

    Canvas(
        modifier.fillMaxWidth().height(ALTO_CAPSULA + ALTO_REGLA).semantics {
            contentDescription = "Tu carrera: $aprobadas de $total materias aprobadas, $cursando en curso"
        },
    ) {
        val n = total
        val s = semestres.size
        val uc = semestres.sumOf { sem -> sem.capsulas.sumOf { it.uc } }
        var gM = HUECO_MATERIA.toPx()
        var gS = HUECO_SEMESTRE.toPx()
        // En un teléfono muy estrecho los huecos se comen la barra: se afinan antes de que una materia de 1 UC desaparezca
        fun escala() = (size.width - gM * (n - s) - gS * (s - 1)) / uc
        if (escala() < 0.5.dp.toPx()) {
            gM = 1.dp.toPx()
            gS = 4.dp.toPx()
        }
        val k = escala().coerceAtLeast(0.1f)
        val alto = ALTO_CAPSULA.toPx()
        val radio = CornerRadius(RADIO_CAPSULA.toPx())
        val trazo = CONTORNO.toPx()
        val centros = FloatArray(s)
        var x = 0f
        semestres.forEachIndexed { i, sem ->
            val x0 = x
            sem.capsulas.forEachIndexed { j, c ->
                val w = c.uc * k
                when (c.estado) {
                    EstadoCapsula.APROBADA -> drawRoundRect(tinta, Offset(x, 0f), Size(w, alto), radio)
                    EstadoCapsula.CURSANDO -> drawRoundRect(
                        tinta,
                        Offset(x + trazo / 2, trazo / 2),
                        Size((w - trazo).coerceAtLeast(0.5f), alto - trazo),
                        radio,
                        style = Stroke(trazo),
                    )
                    EstadoCapsula.PENDIENTE -> drawRoundRect(pendiente, Offset(x, 0f), Size(w, alto), radio)
                }
                x += w + if (j < sem.capsulas.lastIndex) gM else 0f
            }
            centros[i] = (x0 + x) / 2f
            x += gS
        }

        // La regla
        val u = 1.dp.toPx()
        val y0 = alto + 6 * u
        drawLine(hilo, Offset(0f, y0), Offset(size.width, y0), u)
        semestres.forEachIndexed { i, sem ->
            val cx = centros[i]
            val esActual = sem.numero == actual
            drawLine(if (esActual || sem.completo) tinta else hilo, Offset(cx, y0), Offset(cx, y0 + 4 * u), u)
            if (esActual) drawCircle(tinta, 8.5f * u, Offset(cx, y0 + 15 * u))
            val t = numeros[i]
            drawText(t, topLeft = Offset(cx - t.size.width / 2f, y0 + 19 * u - t.firstBaseline))
        }
    }
}
