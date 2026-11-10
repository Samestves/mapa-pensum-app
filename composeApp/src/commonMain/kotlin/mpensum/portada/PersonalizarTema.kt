package mpensum.portada

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.PaletaTemas
import mpensum.componentes.PantallaTema
import mpensum.componentes.PantallasTema
import mpensum.componentes.iconoCarrera
import mpensum.datos.Carrera
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Acento
import mpensum.tema.Espacio
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.jost

/* El tema y el acento en Personalizar: las dos pantallas, la paleta y los círculos de acento */

/** El nombre del juego que se está viendo en la paleta: el de la ranura activa con Automático */
@Composable
private fun juegoElegido(ranuras: Ranuras): Juego {
    val tema by Preferencias.tema.collectAsState()
    val dia by Preferencias.temaDia.collectAsState()
    val noche by Preferencias.temaNoche.collectAsState()
    return tema.juego ?: if (ranuras.noche) noche else dia
}

/** Aplica la elección de la paleta: lo que dice [elegirJuego], guardado en las preferencias */
private fun aplicarEleccion(juego: Juego, ranuras: Ranuras) {
    val e = elegirJuego(juego, Preferencias.tema.value, ranuras.noche, ranuras.diaTocado)
    e.tema?.let(Preferencias::guardarTema)
    e.noche?.let(Preferencias::guardarTemaNoche)
    e.dia?.let(Preferencias::guardarTemaDia)
    ranuras.diaTocado = e.diaTocado
}

/**
 * Las dos pantallas, el nombre del tema y la paleta. Acostado es el índice de
 * la izquierda: el rótulo TEMA con el nombre a la derecha, pantallas de 154 × 168
 * y toques de 50.
 */
@Composable
internal fun BloqueTema(propia: Carrera?, ranuras: Ranuras, apaisado: Boolean, lado: Dp) {
    val tema by Preferencias.tema.collectAsState()
    val dia by Preferencias.temaDia.collectAsState()
    val noche by Preferencias.temaNoche.collectAsState()
    val automatico = tema == Apariencia.SISTEMA
    val elegido = juegoElegido(ranuras)
    val actual = tema.juego ?: Tema.juego
    val izquierda = if (automatico) PantallaTema(dia, false, "De día", false) else PantallaTema(actual, false, "Portada", null)
    val derecha = if (automatico) PantallaTema(noche, false, "De noche", true) else PantallaTema(actual, true, "Mapa", null)
    if (apaisado) Rotulo("TEMA", elegido.nombre, Modifier.padding(top = Espacio.xs))
    PantallasTema(
        izquierda, derecha, propia?.color, propia?.silueta,
        ranuraActiva = if (automatico) (if (ranuras.noche) 1 else 0) else null,
        modifier = Modifier.padding(horizontal = lado).padding(top = if (apaisado) 0.dp else Espacio.xs),
        anchoMaximo = if (apaisado) 154.dp else 160.dp,
        altoMaximo = if (apaisado) 168.dp else 208.dp,
        hueco = if (apaisado) 14.dp else 15.dp,
        alElegirRanura = { ranuras.noche = it == 1 },
    )
    if (!apaisado) {
        BasicText(
            elegido.nombre,
            Modifier.fillMaxWidth().padding(top = Espacio.s, bottom = Espacio.xs),
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, textAlign = TextAlign.Center, color = Paleta.tinta),
        )
    }
    PaletaTemas(
        elegido = elegido,
        otro = if (automatico) (if (ranuras.noche) dia else noche) else null,
        otroEsNoche = !ranuras.noche,
        modifier = Modifier.padding(horizontal = lado).padding(top = if (apaisado) Espacio.xs else 0.dp),
        alElegir = { aplicarEleccion(it, ranuras) },
    )
}

/** Sistema: un solo interruptor. Encendido, las dos pantallas pasan a ser las ranuras de día y de noche */
@Composable
internal fun BloqueAutomatico(ranuras: Ranuras, lado: Dp, primero: Boolean) {
    val tema by Preferencias.tema.collectAsState()
    val automatico = tema == Apariencia.SISTEMA
    Tarjeta(lado, Modifier.padding(top = if (primero) Espacio.xs else Espacio.m)) {
        FilaInterruptor(
            "Automático", automatico,
            nota = if (automatico) "Claro de día · Oscuro de noche" else null,
            alCambiar = { encendido ->
                // Apagado guarda el juego que se está viendo, para que la pantalla no salte
                Preferencias.guardarTema(if (encendido) Apariencia.SISTEMA else aparienciaDe(Tema.juego))
            },
        )
    }
}

@Composable
internal fun BloqueAcento(propia: Carrera?, lado: Dp) {
    val acento by Preferencias.acento.collectAsState()
    Rotulo("ACENTO", acento?.nombre ?: "Tu carrera", Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Row(Modifier.fillMaxWidth().padding(horizontal = if (lado == 0.dp) 0.dp else Espacio.xs).semantics { selectableGroup() }, horizontalArrangement = Arrangement.SpaceBetween) {
        CirculoAcento(null, acento == null, propia)
        Acento.entries.forEach { CirculoAcento(it, acento == it, propia) }
    }
}

/**
 * Un acento: un círculo de 24 en un área táctil de 44; el elegido lleva un
 * aro de 36. `null` es «tu carrera»: el icono de la carrera en tinta neutra,
 * en un disco de 28 al 12 %.
 */
@Composable
private fun CirculoAcento(acento: Acento?, elegido: Boolean, propia: Carrera?) {
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
                Box(Modifier.size(28.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    IconoTrazo(iconoCarrera(propia?.slug), Paleta.tinta, 14.dp, 1.75f)
                }
            } else {
                Box(Modifier.size(24.dp).clip(CircleShape).background(acento.color))
            }
        }
    }
}
