package mpensum.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tokens
import mpensum.tema.inter
import mpensum.tema.tokensDe

/*
 * El selector de tema: un carrusel de mini portadas, una por juego y la de
 * Sistema partida en diagonal (claro arriba a la izquierda, oscuro abajo a
 * la derecha). Lo usan Personalizar, en Ajustes, y la hoja de avance: elegir
 * es guardar Preferencias.tema, y la app cambia en el acto.
 */

private const val ANCHO_MINI = 60f
private const val ALTO_MINI = 84f

/**
 * La portada de un juego en miniatura (60×84), pintada con los tokens de ESE
 * juego y no con los del tema que se ve. `juego` null es Sistema, partida en
 * diagonal. `escala` la reduce para las filas.
 */
@Composable
fun MiniTema(juego: Juego?, modifier: Modifier = Modifier, escala: Float = 1f) {
    Canvas(modifier.size((ANCHO_MINI * escala).dp, (ALTO_MINI * escala).dp)) {
        scale(density * escala, Offset.Zero) {
            if (juego == null) {
                miniatura(tokensDe(Juego.CLARO), false)
                val oscuro = Path().apply { moveTo(ANCHO_MINI, 0f); lineTo(ANCHO_MINI, ALTO_MINI); lineTo(0f, ALTO_MINI); close() }
                clipPath(oscuro) { miniatura(tokensDe(Juego.OSCURO), true) }
            } else miniatura(tokensDe(juego), juego == Juego.OSCURO)
        }
    }
}

/** Las medidas están en dp y el lienzo ya viene escalado a ellos */
private fun DrawScope.miniatura(t: Tokens, oscuro: Boolean) {
    val vidrio = if (oscuro) t.panel else t.vidrio
    drawRect(t.lienzo, size = Size(ANCHO_MINI, ALTO_MINI))
    drawRoundRect(t.tinta.copy(alpha = 0.85f), Offset(6f, 7f), Size(22f, 3f), CornerRadius(1.5f))
    // La tarjeta de tu carrera: puntos, cifra, etiqueta y el chip
    drawRoundRect(vidrio, Offset(5f, 16f), Size(50f, 31f), CornerRadius(6f))
    drawRoundRect(t.borde, Offset(5.5f, 16.5f), Size(49f, 30f), CornerRadius(5.5f), style = Stroke(1f))
    for (c in 0 until 4) for (f in 0 until 3) drawCircle(t.azul, 0.9f, Offset(11f + c * 3.5f, 22f + f * 3.5f))
    drawRoundRect(t.tinta, Offset(38f, 22f), Size(11f, 6f), CornerRadius(1f))
    drawRoundRect(t.suave, Offset(11f, 38f), Size(17f, 3f), CornerRadius(1.5f))
    drawRoundRect(t.azul, Offset(35.5f, 36.5f), Size(14f, 5f), CornerRadius(2.5f), style = Stroke(1f))
    // Las filas de otras carreras
    for (i in 0 until 3) {
        val y = 51f + i * 11f
        drawRoundRect(vidrio, Offset(5f, y), Size(50f, 8f), CornerRadius(3f))
        drawRoundRect(t.borde, Offset(5.5f, y + 0.5f), Size(49f, 7f), CornerRadius(2.5f), style = Stroke(1f))
    }
}

private val TEMAS: List<Apariencia> = Apariencia.entries

/**
 * El carrusel de temas: celdas de 72×96 con la mini tarjeta de 60×84 dentro
 * (radio 10) y el nombre debajo; la elegida lleva un aro de 2 dp en tinta con
 * radio 16. `relleno` deja el aire de los lados sin cortar el desplazamiento.
 */
@Composable
fun CarruselTemas(modifier: Modifier = Modifier, relleno: PaddingValues = PaddingValues(horizontal = 16.dp)) {
    val actual by Preferencias.tema.collectAsState()
    val lista = rememberLazyListState(initialFirstVisibleItemIndex = (TEMAS.indexOf(actual) - 1).coerceAtLeast(0))
    LazyRow(
        modifier.semantics { selectableGroup() },
        state = lista,
        contentPadding = relleno,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TEMAS) { a -> CeldaTema(a, a == actual) { Preferencias.guardarTema(a) } }
    }
}

@Composable
private fun CeldaTema(apariencia: Apariencia, elegida: Boolean, alPulsar: () -> Unit) {
    Column(
        Modifier
            .width(72.dp)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar)
            .semantics { contentDescription = apariencia.nombre },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp, 96.dp)
                .drawBehind {
                    if (elegida) {
                        val g = 2.dp.toPx()
                        drawRoundRect(Paleta.tinta, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(16.dp.toPx() - g / 2), style = Stroke(g))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val forma = RoundedCornerShape(10.dp)
            MiniTema(apariencia.juego, Modifier.clip(forma).border(1.dp, Paleta.borde, forma))
        }
        BasicText(
            apariencia.nombre,
            Modifier.padding(top = 8.dp),
            style = TextStyle(fontFamily = inter(), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = if (elegida) Paleta.tinta else Paleta.tenue),
        )
    }
}
