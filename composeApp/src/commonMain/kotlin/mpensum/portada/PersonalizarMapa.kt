package mpensum.portada

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.VistaPreviaMapa
import mpensum.datos.Carrera
import mpensum.plataforma.Preferencias
import mpensum.tema.Espacio
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter

/* La página «Mapa» de Personalizar: fondo, cables y sus baldosas */

/** La nota de la fila Mapa: «Rejilla · cables curvos» */
@Composable
internal fun resumenMapa(): String {
    val fondo = when (Tema.fondo) { Fondo.REJILLA -> "Rejilla"; Fondo.PUNTOS -> "Puntos"; Fondo.LISO -> "Liso" }
    val cables = when (Tema.cables) { FormaCables.CURVOS -> "curvos"; FormaCables.RECTOS -> "rectos"; FormaCables.AL_TOCAR -> "al tocar" }
    return "$fondo · cables $cables"
}

@Composable
internal fun PaginaMapa(propia: Carrera?, lado: Dp) {
    val fondo = Tema.fondo
    val cables by Preferencias.cables.collectAsState()
    val grosor by Preferencias.grosorCables.collectAsState()
    val atenuar by Preferencias.atenuarAprobadas.collectAsState()
    val codigos by Preferencias.codigos.collectAsState()
    VistaPreviaMapa(propia?.color, Modifier.padding(horizontal = lado).padding(top = Espacio.xxs))
    Rotulo("FONDO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Baldosas(lado) {
        Baldosa("Rejilla", fondo == Fondo.REJILLA, { Preferencias.guardarFondoMapa(Fondo.REJILLA) }) { dibujoFondo(Fondo.REJILLA) }
        Baldosa("Puntos", fondo == Fondo.PUNTOS, { Preferencias.guardarFondoMapa(Fondo.PUNTOS) }) { dibujoFondo(Fondo.PUNTOS) }
        Baldosa("Liso", fondo == Fondo.LISO, { Preferencias.guardarFondoMapa(Fondo.LISO) }) { dibujoFondo(Fondo.LISO) }
    }
    Rotulo("CABLES", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Baldosas(lado) {
        Baldosa("Curvos", cables == FormaCables.CURVOS, { Preferencias.guardarCables(FormaCables.CURVOS) }) { dibujoCables(FormaCables.CURVOS) }
        Baldosa("Rectos", cables == FormaCables.RECTOS, { Preferencias.guardarCables(FormaCables.RECTOS) }) { dibujoCables(FormaCables.RECTOS) }
        Baldosa("Al tocar", cables == FormaCables.AL_TOCAR, { Preferencias.guardarCables(FormaCables.AL_TOCAR) }) { dibujoCables(FormaCables.AL_TOCAR) }
    }
    Rotulo("GROSOR", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Segmentado(Modifier.padding(horizontal = lado)) {
        Grosor.entries.forEachIndexed { i, g ->
            val nombre = when (g) { Grosor.FINO -> "Fino"; Grosor.NORMAL -> "Normal"; Grosor.GRUESO -> "Grueso" }
            OpcionTexto(nombre, null, grosor == g, Modifier.weight(1f), delante = { colorTexto ->
                // La línea que dibuja el grosor: 18 dp de largo y 1, 2 o 3 de alto
                Box(Modifier.padding(end = Espacio.xs).size(18.dp, (i + 1).dp).background(colorTexto, CircleShape))
            }) { Preferencias.guardarGrosorCables(g) }
        }
    }
    Tarjeta(lado, Modifier.padding(top = Espacio.xl)) {
        FilaInterruptor("Atenuar lo aprobado", atenuar, alCambiar = Preferencias::guardarAtenuarAprobadas)
        Divisor(Espacio.m)
        FilaInterruptor("Códigos de materia", codigos, alCambiar = Preferencias::guardarCodigos)
    }
}

/** Las baldosas de una opción, repartidas a lo ancho con 8 dp entre ellas */
@Composable
private fun Baldosas(lado: Dp, contenido: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = lado).semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(Espacio.xs), content = contenido)
}

/**
 * Una baldosa de 76 de alto, radio 16, con su dibujo de 64×30 y su nombre; la
 * elegida lleva aro de 2 dp y el nombre en tinta.
 */
@Composable
private fun RowScope.Baldosa(nombre: String, elegida: Boolean, alPulsar: () -> Unit, dibujo: DrawScope.() -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .weight(1f)
            .height(76.dp)
            .clip(forma)
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .border(if (elegida) 2.dp else 1.dp, if (elegida) Paleta.tinta else Paleta.tinta.copy(alpha = 0.065f), forma)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar)
            .semantics { contentDescription = nombre },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(64.dp, 30.dp)) { dibujo() }
        BasicText(nombre, Modifier.padding(top = Espacio.xs), style = TextStyle(fontFamily = inter(), fontSize = Tipo.xs, lineHeight = 16.sp, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = if (elegida) Paleta.tinta else Paleta.tenue))
    }
}

// Los dibujos de las baldosas: 64×30 dp, con la Paleta del tema que se ve

private fun DrawScope.dibujoFondo(fondo: Fondo) {
    val dp = density
    val linea = Paleta.tenue.copy(alpha = 0.5f)
    when (fondo) {
        Fondo.REJILLA -> {
            for (i in 1..3) drawLine(linea, Offset(i * 16 * dp, 0f), Offset(i * 16 * dp, size.height), dp)
            for (j in 1..1) drawLine(linea, Offset(0f, j * 15 * dp), Offset(size.width, j * 15 * dp), dp)
            drawLine(linea, Offset(0f, 0f), Offset(size.width, 0f), dp)
        }
        Fondo.PUNTOS -> {
            val puntos = ArrayList<Offset>()
            for (i in 0..3) for (j in 0..1) puntos.add(Offset(8 * dp + i * 16 * dp, 7.5f * dp + j * 15 * dp))
            drawPoints(puntos, PointMode.Points, Paleta.tenue, 3f * dp, StrokeCap.Round)
        }
        Fondo.LISO -> drawRoundRect(Paleta.tenue.copy(alpha = 0.18f), cornerRadius = CornerRadius(6 * dp))
    }
}

private fun DrawScope.dibujoCables(forma: FormaCables) {
    val dp = density
    val a = Offset(8 * dp, 22 * dp)
    val b = Offset(56 * dp, 8 * dp)
    val trazo = Stroke(2 * dp, cap = StrokeCap.Round)
    val ruta = Path().apply {
        moveTo(a.x, a.y)
        when (forma) {
            FormaCables.RECTOS -> { lineTo(32 * dp, a.y); lineTo(32 * dp, b.y); lineTo(b.x, b.y) }
            else -> cubicTo(32 * dp, a.y, 32 * dp, b.y, b.x, b.y)
        }
    }
    drawPath(ruta, Paleta.suave.copy(alpha = if (forma == FormaCables.AL_TOCAR) 0.3f else 0.9f), style = trazo)
    drawCircle(Paleta.suave, 3.5f * dp, a)
    drawCircle(if (forma == FormaCables.AL_TOCAR) Paleta.tinta else Paleta.suave, 3.5f * dp, b)
}
