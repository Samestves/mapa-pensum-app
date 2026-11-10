package mpensum.portada

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.plataforma.Preferencias
import mpensum.plataforma.animacionesQuitadas
import mpensum.plataforma.telefonoModesto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter
import mpensum.tema.jost

/* La página «Accesibilidad» de Personalizar */

/** La nota de la fila Accesibilidad: «Texto normal · ligero automático», más «· más contraste» si lo hay */
@Composable
internal fun resumenAccesibilidad(): String {
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    val modo = when (ligero) { null -> "automático"; true -> "sí"; false -> "no" }
    return "Texto ${if (grande) "grande" else "normal"} · ligero $modo${if (contraste) " · más contraste" else ""}"
}

@Composable
internal fun PaginaAccesibilidad(lado: Dp) {
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val luz by Preferencias.luzFondo.collectAsState()
    val reducir by Preferencias.reducirMovimiento.collectAsState()
    val vibracion by Preferencias.vibracion.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    val exterior = Tema.juego.exterior
    Rotulo("LECTURA", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xxs))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Normal", 14, !grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(false) }
        OpcionTexto("Grande", 18, grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(true) }
    }
    Tarjeta(lado, Modifier.padding(top = Espacio.m)) {
        // Un Exterior ya lo trae: se ve encendido y no se puede apagar
        FilaInterruptor(
            "Más contraste", contraste || exterior, habilitado = !exterior,
            nota = if (exterior) "Exterior ya lo trae" else null,
            alCambiar = Preferencias::guardarContraste,
        )
    }
    Rotulo("MOVIMIENTO Y TACTO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Tarjeta(lado) {
        FilaInterruptor("Luz de fondo", luz ?: (Tema.juego.aurora > 0f), alCambiar = Preferencias::guardarLuzFondo)
        Divisor(Espacio.m)
        FilaInterruptor("Reducir movimiento", reducir ?: animacionesQuitadas(), alCambiar = Preferencias::guardarReducirMovimiento)
        Divisor(Espacio.m)
        FilaInterruptor("Vibración", vibracion, alCambiar = Preferencias::guardarVibracion)
    }
    Rotulo("MODO LIGERO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Automático", null, ligero == null, Modifier.weight(1.4f)) { Preferencias.guardarModoLigero(null) }
        OpcionTexto("Sí", null, ligero == true, Modifier.weight(1f)) { Preferencias.guardarModoLigero(true) }
        OpcionTexto("No", null, ligero == false, Modifier.weight(1f)) { Preferencias.guardarModoLigero(false) }
    }
    val explica = when (ligero) {
        null -> if (telefonoModesto()) "Activo: sin desenfoque" else "Apagado"
        true -> "Activo: sin desenfoque"
        false -> "Todo el cristal y las luces"
    }
    Nota(explica, Modifier.padding(horizontal = lado).padding(top = Espacio.xs), Paleta.tenue)
}

/**
 * Una opción del segmentado. La elegida pasa a tinta al 11 % con un filo de
 * tinta al 12 % y texto 500 (en oscuro, el `panel` con sombra quedaba más
 * oscuro que la pista). Con `tamAa`, la muestra «Aa» delante a ese tamaño; con
 * `delante`, lo que se dibuje, que recibe el color del texto.
 */
@Composable
internal fun OpcionTexto(nombre: String, tamAa: Int?, elegida: Boolean, modifier: Modifier, delante: (@Composable (Color) -> Unit)? = null, alPulsar: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(forma)
            .then(if (elegida) Modifier.background(Paleta.tinta.copy(alpha = 0.11f)).border(1.dp, Paleta.tinta.copy(alpha = 0.12f), forma) else Modifier)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (elegida) Paleta.tinta else Paleta.tenue
        if (tamAa != null) BasicText("Aa", Modifier.padding(end = Espacio.xs), style = TextStyle(fontFamily = jost(), fontSize = tamAa.sp, color = color))
        delante?.invoke(color)
        BasicText(nombre, style = TextStyle(fontFamily = inter(), fontSize = Tipo.l, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
