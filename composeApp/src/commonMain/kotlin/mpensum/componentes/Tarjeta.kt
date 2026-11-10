package mpensum.componentes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import mpensum.logica.Situacion
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema

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
            // Cursando se reconoce por la forma: borde de 1,5 dp en tinta, sin tinte
            Paleta.panel, Paleta.cursando, 1.5f, tinta, sombraPapel(),
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
