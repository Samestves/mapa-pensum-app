package mpensum.portada

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.carrera.versal
import mpensum.componentes.CarruselTemas
import mpensum.componentes.MiniTema
import mpensum.plataforma.Preferencias
import mpensum.plataforma.animacionesQuitadas
import mpensum.tema.Acento
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * Personalizar: la subpantalla de Ajustes donde se elige cómo se ve la app.
 * Todo se guarda en el acto (Preferencias) y se ve en el acto: el tema se
 * funde, el resto cambia al componer.
 *
 *   - Tema: el carrusel de mini portadas.
 *   - Acento: «tu carrera» (el color de cada una) u otro de cinco.
 *   - Texto: normal o grande (×1,15).
 *   - Movimiento y mapa: luz de fondo, reducir movimiento, códigos.
 */

/** Lo que dice la fila de Ajustes: «Oscuro · tu carrera · texto normal» */
@Composable
internal fun resumenPersonalizar(): String {
    val tema by Preferencias.tema.collectAsState()
    val acento by Preferencias.acento.collectAsState()
    val grande by Preferencias.textoGrande.collectAsState()
    return "${tema.nombre} · ${acento?.nombre?.lowercase() ?: "tu carrera"} · texto ${if (grande) "grande" else "normal"}"
}

/** La mini vista del tema que se ve, para la fila de Ajustes */
@Composable
internal fun MiniVistaTema(modifier: Modifier = Modifier) {
    val tema by Preferencias.tema.collectAsState()
    val forma = RoundedCornerShape(7.dp)
    Box(modifier.size(36.dp, 44.dp).clip(forma).border(1.dp, Paleta.borde, forma), contentAlignment = Alignment.TopCenter) {
        MiniTema(tema.juego, escala = 0.6f)
    }
}

@Composable
internal fun Personalizar(colorCarrera: Color) {
    val acento by Preferencias.acento.collectAsState()
    val grande by Preferencias.textoGrande.collectAsState()
    val luz by Preferencias.luzFondo.collectAsState()
    val reducir by Preferencias.reducirMovimiento.collectAsState()
    val codigos by Preferencias.codigos.collectAsState()
    Column(Modifier.padding(top = 4.dp, bottom = 24.dp)) {
        Etiqueta("TEMA", Modifier.padding(start = 24.dp, top = 8.dp, bottom = 10.dp))
        CarruselTemas(relleno = PaddingValues(horizontal = 16.dp))

        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText("ACENTO", Modifier.weight(1f), style = versal(Paleta.tenue))
            BasicText(acento?.nombre ?: "Tu carrera", style = TextStyle(fontFamily = inter(), fontSize = 13.sp, color = Paleta.suave))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).semantics { selectableGroup() }, horizontalArrangement = Arrangement.SpaceBetween) {
            CirculoAcento(null, acento == null, colorCarrera)
            Acento.entries.forEach { CirculoAcento(it, acento == it, colorCarrera) }
        }

        Etiqueta("TEXTO", Modifier.padding(start = 24.dp, top = 24.dp, bottom = 10.dp))
        SegmentadoTexto(grande, Modifier.padding(horizontal = 20.dp))

        Etiqueta("MOVIMIENTO Y MAPA", Modifier.padding(start = 24.dp, top = 24.dp, bottom = 10.dp))
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .drawBehind {
                    val r = 16.dp.toPx()
                    val g = Tema.grosorBorde.dp.toPx()
                    drawRoundRect(Paleta.tinta.copy(alpha = 0.035f), cornerRadius = CornerRadius(r))
                    drawRoundRect(Paleta.tinta.copy(alpha = 0.065f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r - g / 2), style = Stroke(g))
                },
        ) {
            FilaInterruptor("Luz de fondo", "Una luz suave desde arriba en la portada", luz ?: (Tema.juego.aurora > 0f), Preferencias::guardarLuzFondo)
            Divisor()
            FilaInterruptor("Reducir movimiento", "Menos animación, más fluida", reducir ?: animacionesQuitadas(), Preferencias::guardarReducirMovimiento)
            Divisor()
            FilaInterruptor("Códigos en el mapa", "El código de cada materia", codigos, Preferencias::guardarCodigos)
        }
    }
}

/**
 * Un acento: un círculo de 24 en un área táctil de 44; el elegido lleva un
 * aro de 36. `null` es «tu carrera», dibujada como una mini silueta de puntos.
 */
@Composable
private fun CirculoAcento(acento: Acento?, elegido: Boolean, colorCarrera: Color) {
    val nombre = acento?.nombre ?: "Tu carrera"
    Box(
        Modifier
            .size(44.dp)
            .selectable(elegido, role = Role.RadioButton) { Preferencias.guardarAcento(acento) }
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .then(if (elegido) Modifier.border(2.dp, Paleta.tinta, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (acento == null) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(Paleta.panelSuave).border(1.dp, Paleta.borde, CircleShape), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp, 16.dp)) {
                        // La silueta en pequeño: cuatro columnas que bajan de cuatro a dos puntos
                        val filas = intArrayOf(4, 4, 3, 2)
                        val paso = 4.dp.toPx()
                        val r = 1.1.dp.toPx()
                        filas.forEachIndexed { c, n -> for (f in 0 until n) drawCircle(colorCarrera, r, Offset(r + c * paso, r + f * paso)) }
                    }
                }
            } else {
                Box(Modifier.size(24.dp).clip(CircleShape).background(acento.color))
            }
        }
    }
}

/** «Aa Normal / Aa Grande»: la cápsula con el elegido sobre el panel */
@Composable
private fun SegmentadoTexto(grande: Boolean, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .border(1.dp, Paleta.tinta.copy(alpha = 0.065f), RoundedCornerShape(24.dp))
            .padding(4.dp)
            .semantics { selectableGroup() },
    ) {
        OpcionTexto("Normal", 14, !grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(false) }
        OpcionTexto("Grande", 18, grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(true) }
    }
}

@Composable
private fun OpcionTexto(nombre: String, tamAa: Int, elegida: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .then(if (elegida) Modifier.shadow(1.dp, RoundedCornerShape(20.dp)).background(Paleta.panel, RoundedCornerShape(20.dp)) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (elegida) Paleta.tinta else Paleta.tenue
        BasicText("Aa", Modifier.padding(end = 8.dp), style = TextStyle(fontFamily = jost(), fontSize = tamAa.sp, color = color))
        BasicText(nombre, style = TextStyle(fontFamily = inter(), fontSize = 15.sp, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = color))
    }
}

@Composable
private fun Divisor() {
    Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(Paleta.borde))
}

/** Una fila con su interruptor: el título en Jost, lo que hace debajo y el interruptor a la derecha */
@Composable
private fun FilaInterruptor(titulo: String, texto: String, activo: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(activo, role = Role.Switch, onValueChange = alCambiar)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            BasicText(titulo, style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta))
            BasicText(texto, Modifier.padding(top = 2.dp), style = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.tenue))
        }
        Interruptor(activo)
    }
}

/** El interruptor de iOS, 51×31: la pista en tinta y el pomo en el panel (nada de verde); apagado, pista de borde y pomo blanco */
@Composable
private fun Interruptor(activo: Boolean) {
    val x by animateDpAsState(if (activo) 22.dp else 2.dp, tween(if (Tema.movimiento) 0 else 200), label = "pomo")
    Box(Modifier.size(51.dp, 31.dp).clip(RoundedCornerShape(16.dp)).background(if (activo) Paleta.tinta else Paleta.borde)) {
        Box(Modifier.offset(x, 2.dp).size(27.dp).shadow(2.dp, CircleShape).background(if (activo) Paleta.panel else Color.White, CircleShape))
    }
}
