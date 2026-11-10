package mpensum.portada

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.versal
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter

/* Las piezas que comparten las páginas de Personalizar: rótulos, tarjetas, segmentos e interruptores */

/** El rótulo de un bloque en versales y, a la derecha, lo elegido: 8 dp debajo */
@Composable
internal fun Rotulo(texto: String, valor: String?, modifier: Modifier) {
    Row(modifier.fillMaxWidth().padding(bottom = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        BasicText(texto, Modifier.weight(1f), style = versal(Paleta.tenue))
        if (valor != null) BasicText(valor, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, color = Paleta.suave))
    }
}

/** La nota de una opción: Inter 13/18 en `tenue` */
@Composable
internal fun Nota(texto: String, modifier: Modifier = Modifier, color: Color = Paleta.tenue, maxLines: Int = Int.MAX_VALUE) {
    BasicText(texto, modifier, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = color), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Una tarjeta de filas: velo de tinta, filo de 1 y radio 24 */
@Composable
internal fun Tarjeta(lado: Dp, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier
            .padding(horizontal = lado)
            .fillMaxWidth()
            .drawBehind {
                val r = Radio.Grande.toPx()
                val g = Tema.grosorBorde.dp.toPx()
                drawRoundRect(Paleta.tinta.copy(alpha = 0.035f), cornerRadius = CornerRadius(r))
                drawRoundRect(Paleta.tinta.copy(alpha = 0.065f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r - g / 2), style = Stroke(g))
            },
    ) { contenido() }
}

/** La cápsula de un segmentado: las opciones en fila sobre un fondo de tinta muy floja */
@Composable
internal fun Segmentado(modifier: Modifier, opciones: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .border(1.dp, Paleta.tinta.copy(alpha = 0.065f), RoundedCornerShape(24.dp))
            .padding(Espacio.xxs)
            .semantics { selectableGroup() },
        content = opciones,
    )
}

/** El separador de una tarjeta, que arranca en `desde` (a la altura del texto si hay una pastilla) */
@Composable
internal fun Divisor(desde: Dp) {
    Box(Modifier.padding(start = desde).fillMaxWidth().height(1.dp).background(Paleta.borde))
}

/**
 * Una fila con su interruptor, de 64 de alto: el título en Inter 500 (15/20,
 * como las demás opciones) y, si hace falta, una nota debajo. Deshabilitada
 * se atenúa y no responde.
 */
@Composable
internal fun FilaInterruptor(titulo: String, activo: Boolean, habilitado: Boolean = true, nota: String? = null, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(activo, enabled = habilitado, role = Role.Switch, onValueChange = alCambiar)
            .padding(start = Espacio.m, end = Espacio.m, top = Espacio.xs, bottom = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Espacio.s)) {
            BasicText(titulo, style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = Tipo.l, lineHeight = 20.sp, color = Paleta.tinta))
            if (nota != null) BasicText(nota, Modifier.padding(top = Espacio.hilo), style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.tenue))
        }
        Interruptor(activo, habilitado)
    }
}

/** El interruptor de iOS, 51×31: la pista en tinta y el pomo en el panel (nada de verde); apagado, pista de borde y pomo blanco */
@Composable
private fun Interruptor(activo: Boolean, habilitado: Boolean) {
    // El pomo se lee como State dentro del offset en lambda: así su carrera solo recoloca, sin recomponer
    // la fila en cada cuadro de los 200 ms
    val x = animateDpAsState(if (activo) 22.dp else 2.dp, tween(if (Tema.movimiento) 0 else 200), label = "pomo")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background((if (activo) Paleta.tinta else Paleta.borde).let { if (habilitado) it else it.copy(alpha = 0.55f) }),
    ) {
        Box(Modifier.offset { IntOffset(x.value.roundToPx(), 2.dp.roundToPx()) }.size(27.dp).shadow(2.dp, CircleShape).background(if (activo) Paleta.panel else Color.White, CircleShape))
    }
}
