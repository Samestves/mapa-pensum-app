package mpensum.carrera.lista

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.logica.MargenMateria
import mpensum.logica.PuntoMargen
import mpensum.logica.puntosDeMargen
import mpensum.tema.Paleta
import mpensum.tema.inter

/*
 * El margen dibujado: un punto por semestre. Los anteriores a la materia en
 * verde, el suyo con un aro y el número, los que puede esperar en tinta dentro
 * de una ventana tenue, y el resto apagados. Debajo, lo que cuesta pasarse.
 */

@Composable
internal fun ReglaDeMargen(semestre: Int, total: Int, datos: MargenMateria, modifier: Modifier = Modifier) {
    val puntos = remember(total, semestre, datos.margen) { puntosDeMargen(total, semestre, datos.margen) }
    val ultimo = if (datos.sinLimite) total else minOf(total, semestre + datos.margen)
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText("MARGEN", style = estiloSeccion())
            BasicText(if (datos.sinLimite) "sem. $semestre en adelante" else "sem. $semestre a $ultimo", style = estiloPlex())
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(30.dp).drawBehind {
                // La ventana en que puede caer sin mover la fecha, sobre sus puntos
                val celda = size.width / puntos.size
                val inicio = (semestre - 1) * celda
                val ancho = (ultimo - semestre + 1) * celda
                val radio = CornerRadius(11.dp.toPx())
                drawRoundRect(REALCE, Offset(inicio, 4.dp.toPx()), Size(ancho, size.height - 8.dp.toPx()), radio)
                drawRoundRect(RAYA, Offset(inicio, 4.dp.toPx()), Size(ancho, size.height - 8.dp.toPx()), radio, style = Stroke(1f))
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            puntos.forEachIndexed { i, p ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    when (p) {
                        PuntoMargen.HECHO -> Box(Modifier.size(7.dp).background(Paleta.verde, CircleShape))
                        PuntoMargen.ACTUAL -> Box(Modifier.size(20.dp).border(1.5.dp, Paleta.tinta, CircleShape), contentAlignment = Alignment.Center) {
                            BasicText("${i + 1}", style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Paleta.tinta, textAlign = TextAlign.Center))
                        }
                        PuntoMargen.VENTANA -> Box(Modifier.size(7.dp).background(Paleta.tinta.copy(alpha = 0.6f), CircleShape))
                        PuntoMargen.LEJOS -> Box(Modifier.size(7.dp).background(Paleta.apagada.copy(alpha = 0.55f), CircleShape))
                    }
                }
            }
        }
        val pie = when {
            datos.sinLimite -> "Puedes dejarla para cuando quieras: no mueve tu fecha de grado."
            datos.siLaDejas != null && datos.fecha != null ->
                "Si la dejas para el ${semestre + datos.margen + 1}, te gradúas en **${datos.siLaDejas.mesYAnio()}** y no en **${datos.fecha.mesYAnio()}**."
            else -> "Puede esperar ${datos.margen} ${if (datos.margen == 1) "semestre" else "semestres"} sin mover tu fecha."
        }
        BasicText(conNegritas(pie), Modifier.padding(top = 8.dp), style = estiloRazon())
    }
}
