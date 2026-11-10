package mpensum.carrera.lista

import mpensum.componentes.BOTON
import mpensum.componentes.IconoDeEstado
import mpensum.componentes.estiloAccion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.Velo
import mpensum.logica.Situacion
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio

/*
 * Las «nubes» de la lista y de la página: las superficies redondeadas de la
 * hoja de progreso. Una nube no es un contenedor: la LazyColumn solo sigue
 * perezosa si cada fila pinta su trozo. La primera redondea arriba, la última
 * abajo y las de en medio son rectas; juntas se leen como una sola tarjeta. El
 * color es opaco (la tinta al 5 % ya mezclada con el lienzo) para que la fila
 * que se arrastra tape lo que descubre el gesto sin capas extra.
 */

/** Lo que separa la nube del borde de la pantalla: el mismo margen de la hoja de progreso y del home */
internal val MARGEN_TARJETA = Espacio.l

/** El radio de la nube: el de las tarjetas del progreso */
internal val RADIO_TARJETA = Radio.Grande

/** Lo que separa dos nubes de una misma sección (lo urgente de lo demás) */
internal val HUECO_TARJETA = Espacio.s

/** El rótulo de una sección va alineado con lo de dentro de la nube: margen más relleno */
internal val SANGRIA_ROTULO = MARGEN_TARJETA + Espacio.m

/** Las palabras de las dos secciones de relación de la página. Otra fase las cambia: viven solo aquí */
internal const val ROTULO_PIDE = "Pide"
internal const val ROTULO_ABRE = "Abre"

/** El fondo de la nube, opaco: la superficie del progreso ya mezclada con el lienzo */
internal val FONDO_TARJETA: Color get() = Paleta.tinta.copy(alpha = Velo.superficie).compositeOver(Paleta.lienzo)

/** El hilo entre filas dentro de una nube: el mismo del progreso */
internal val HILO_TARJETA: Color get() = Paleta.tinta.copy(alpha = Velo.hilo)

/** La forma del trozo de nube de una fila: redondo arriba si es la primera, abajo si es la última */
internal fun formaDeTrozo(primera: Boolean, ultima: Boolean, radio: Dp = RADIO_TARJETA): Shape =
    if (!primera && !ultima) RectangleShape
    else RoundedCornerShape(
        topStart = if (primera) radio else 0.dp, topEnd = if (primera) radio else 0.dp,
        bottomStart = if (ultima) radio else 0.dp, bottomEnd = if (ultima) radio else 0.dp,
    )

/** La pastilla de icono del progreso (radio 8) con el icono de estado; lo inscribible, en su azul */
@Composable
internal fun PastillaDeEstado(s: Situacion?, modifier: Modifier = Modifier, lado: Dp = 32.dp) {
    val fondo = if (s == Situacion.INSCRIBIBLE) Paleta.azul.copy(alpha = Velo.pastillaAzul) else Paleta.tinta.copy(alpha = Velo.pastilla)
    Box(modifier.size(lado).background(fondo, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { IconoDeEstado(s, lado * 0.56f) }
}

/** «Deshacer» o «Elegir» en una pastilla, dentro de una zona de toque de 44 dp */
@Composable
internal fun AccionEnPastilla(texto: String, alTocar: (() -> Unit)?) {
    Box(
        Modifier.heightIn(min = 44.dp)
            .then(if (alTocar != null) Modifier.clickable(role = Role.Button, onClick = alTocar) else Modifier)
            .padding(start = 8.dp, end = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(texto, Modifier.background(BOTON, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), style = estiloAccion())
    }
}

/**
 * El pie de la nube de «Cursando»: «Aprobé las N» o, tras tocarlo,
 * «Deshacer». Sin acción posible queda tenue y no se toca: así la nube no
 * cambia de alto bajo el dedo.
 */
@Composable
internal fun PieDeTarjeta(texto: String, conSello: Boolean, alTocar: (() -> Unit)?) {
    val forma = formaDeTrozo(primera = false, ultima = true)
    Box(Modifier.fillMaxWidth().padding(horizontal = MARGEN_TARJETA)) {
        Row(
            Modifier.fillMaxWidth().clip(forma).background(FONDO_TARJETA)
                .then(if (alTocar != null) Modifier.clickable(role = Role.Button, onClick = alTocar) else Modifier)
                .heightIn(min = 52.dp)
                .padding(start = Espacio.m, end = Espacio.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                if (conSello) IconoDeEstado(Situacion.HECHA, 18.dp)
            }
            BasicText(
                texto, Modifier.padding(start = Espacio.s),
                style = estiloAccion().copy(fontSize = 15.sp, lineHeight = 20.sp, color = if (alTocar != null) Paleta.tinta else Paleta.tenue),
            )
        }
    }
}
