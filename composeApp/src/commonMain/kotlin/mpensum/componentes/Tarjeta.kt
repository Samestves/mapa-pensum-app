package mpensum.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.logica.Situacion
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema
import mpensum.tema.jost
import mpensum.tema.plex

/**
 * Cómo se ve una tarjeta en cada situación, la tabla ASPECTO de la web.
 * `sombra` es la sombra de papel del claro: un rectángulo corrido 2 dp bajo
 * la tarjeta, sin desenfoque (un blur por tarjeta costaría en cada cuadro del
 * arrastre). En oscuro no hay: el filo ya separa.
 */
data class Aspecto(val fondo: Color, val borde: Color, val grosor: Float, val nombre: Color, val sombra: Color = Color.Transparent)

fun aspecto(e: Situacion): Aspecto {
    val o = !Tema.claro
    val tinta = Paleta.tinta
    // Todo sale de la paleta del juego: el mismo código vale para Grafito, Papel o Exterior
    return when (e) {
        Situacion.HECHA -> Aspecto(
            lerp(Paleta.panel, Paleta.verde, porTema(0.09f, 0.05f)),
            Paleta.verde.copy(alpha = porTema(0.30f, 0.40f)), 1f, Paleta.suave, sombraPapel(),
        )
        Situacion.CURSANDO -> Aspecto(
            lerp(Paleta.panel, Paleta.ambar, porTema(0.11f, 0.07f)),
            Paleta.ambar.copy(alpha = porTema(0.58f, 0.70f)), 1.25f, tinta, sombraPapel(),
        )
        // En Exterior lo inscribible lleva el borde azul: se reconoce de lejos y a pleno sol
        Situacion.INSCRIBIBLE ->
            if (Tema.fuerte) Aspecto(Paleta.panel, Paleta.azul, 1.75f, tinta, sombraPapel())
            else Aspecto(
                if (o) lerp(Paleta.panel, tinta, 0.07f) else Paleta.panel,
                tinta.copy(alpha = porTema(0.30f, 0.22f)), 1.25f, tinta, sombraPapel(),
            )
        Situacion.PROXIMA -> Aspecto(
            if (o) Paleta.panelSuave else lerp(Paleta.panel, Paleta.lienzo, 0.4f),
            tinta.copy(alpha = porTema(0.12f, 0.10f)), 1f, Paleta.suave, sombraPapel(),
        )
        // La bloqueada se hunde bajo el lienzo, sin sombra y con el nombre apagado
        Situacion.LEJANA -> Aspecto(
            if (o) lerp(Paleta.lienzo, Paleta.panel, 0.5f) else lerp(Paleta.lienzo, Paleta.borde, 0.5f),
            if (o) tinta.copy(alpha = 0.05f) else Color.Transparent, 1f,
            lerp(Paleta.tenue, Paleta.apagada, 0.3f),
        )
    }
}

/** La sombra de papel de los temas claros: una tinta casi transparente, sin desenfoque */
private fun sombraPapel(): Color = if (Tema.claro) Paleta.tinta.copy(alpha = 0.07f) else Color.Transparent

/**
 * La tarjeta de una materia del mapa web (224x96 a escala 0,8): código,
 * nombre, punto del área, UC y el icono de estado arriba a la derecha.
 *
 * Puede pasar de un estado a otro: `cambio()` va de 0 (estado `de`) a 1
 * (estado `a`). `giro()` abre el candado cuando la tarjeta se desbloquea.
 * Las lambdas se leen al dibujar, así la animación no recompone nada.
 */
@Composable
fun TarjetaMateria(
    codigo: String,
    nombre: String,
    uc: Int,
    area: Color,
    de: Situacion,
    a: Situacion = de,
    x: Float = 0f,
    y: Float = 0f,
    modifier: Modifier = Modifier,
    cambio: () -> Float = { 0f },
    giro: (() -> Float)? = null,
) {
    val asDe = aspecto(de)
    val asA = aspecto(a)
    Box(
        modifier
            .offset(x.dp, y.dp)
            .size(179.dp, 77.dp)
            .drawBehind {
                val f = cambio()
                val radio = CornerRadius(5.6.dp.toPx())
                val sombra = lerp(asDe.sombra, asA.sombra, f)
                if (sombra.alpha > 0f) drawRoundRect(sombra, Offset(0f, 2.dp.toPx()), size, radio)
                drawRoundRect(lerp(asDe.fondo, asA.fondo, f), cornerRadius = radio)
                val g = (asDe.grosor + (asA.grosor - asDe.grosor) * f).dp.toPx()
                drawRoundRect(
                    lerp(asDe.borde, asA.borde, f),
                    topLeft = Offset(g / 2, g / 2),
                    size = Size(size.width - g, size.height - g),
                    cornerRadius = radio,
                    style = Stroke(g),
                )
            },
    ) {
        val mono = TextStyle(fontFamily = plex(), fontSize = 8.sp, letterSpacing = 0.04.em, color = Paleta.tenue)
        BasicText(codigo, Modifier.offset(14.dp, 11.dp), style = mono)
        Box(Modifier.offset(14.dp, 24.dp).size(152.dp, 40.dp), contentAlignment = Alignment.CenterStart) {
            BasicText(
                nombre,
                style = TextStyle(fontFamily = jost(), fontSize = 11.2.sp, lineHeight = 13.2.sp, fontWeight = FontWeight.Normal),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = { lerp(asDe.nombre, asA.nombre, cambio()) },
            )
        }
        Canvas(Modifier.offset(14.4.dp, 61.8.dp).size(4.8.dp)) { drawCircle(area) }
        BasicText("$uc UC", Modifier.offset(24.dp, 58.5.dp), style = mono)
        Canvas(Modifier.offset(152.dp, 8.4.dp).size(14.dp)) {
            val f = cambio()
            val lado = size.width
            if (giro != null) {
                // El candado que se abre: color de rojo a azul y el arco girando sobre su pata
                val color = lerp(Paleta.rojo, Paleta.azul, f)
                iconoEstado(Situacion.LEJANA, 0f, 0f, lado, lerp(asDe.fondo, asA.fondo, f), color, giro())
            } else {
                if (f < 1f) iconoEstado(de, 0f, 0f, lado, asDe.fondo, colorEstado(de).copy(alpha = 1f - f))
                if (f > 0f) iconoEstado(a, 0f, 0f, lado, asA.fondo, colorEstado(a).copy(alpha = f))
            }
        }
    }
}
