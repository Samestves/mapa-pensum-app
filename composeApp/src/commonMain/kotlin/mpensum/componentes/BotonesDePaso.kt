package mpensum.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.logica.AccionMarca
import mpensum.logica.Marca
import mpensum.logica.Situacion
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.inter

/*
 * Los botones con que se marca una materia, los mismos en la ficha del mapa y
 * en la página de la lista. Los dos van en la misma superficie suave: ninguno
 * es «el blanco» ni «el negro». Lo que distingue cada paso es su icono, con el
 * color de su estado (el libro en tinta, el sello en verde), y el orden: a la
 * izquierda, el paso que toca.
 */

/** Lo que el contenido de un botón se aparta de sus bordes */
private val DENTRO = 14.dp

/**
 * `principal` es el paso que toca (null si no hay: una aprobada solo se puede
 * desmarcar) y `secundaria` el otro. `apilados` los pone uno sobre otro, para
 * pantallas estrechas o letra grande.
 */
@Composable
internal fun BotonesDePaso(
    principal: AccionMarca?,
    secundaria: AccionMarca,
    alMarcar: (Marca?) -> Unit,
    modifier: Modifier = Modifier,
    alto: Dp = 52.dp,
    apilados: Boolean = false,
) {
    when {
        principal == null -> BotonDePaso(secundaria, alMarcar, modifier.fillMaxWidth(), alto)
        apilados -> Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonDePaso(principal, alMarcar, Modifier.fillMaxWidth(), alto)
            BotonDePaso(secundaria, alMarcar, Modifier.fillMaxWidth(), alto)
        }
        else -> Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // El paso que toca es un poco más ancho: su texto suele ser el largo («La estoy cursando»)
            BotonDePaso(principal, alMarcar, Modifier.weight(1.25f), alto)
            BotonDePaso(secundaria, alMarcar, Modifier.weight(1f), alto)
        }
    }
}

@Composable
private fun BotonDePaso(accion: AccionMarca, alMarcar: (Marca?) -> Unit, modifier: Modifier, alto: Dp) {
    val haptica = LocalHapticFeedback.current
    Row(
        modifier.height(alto).clip(RoundedCornerShape(Radio.Campo)).background(BOTON)
            .clickable(role = Role.Button) {
                haptica.performHapticFeedback(HapticFeedbackType.Confirm)
                alMarcar(accion.marca)
            }
            .padding(horizontal = DENTRO),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        when (accion.marca) {
            Marca.APROBADA -> IconoDeEstado(Situacion.HECHA, 18.dp)
            Marca.CURSANDO -> IconoDeEstado(Situacion.CURSANDO, 18.dp)
            null -> IconoLucide(Lucide.deshacer, Paleta.suave, 18.dp)
        }
        // La letra baja antes de tocar el borde; nunca se corta
        BasicText(
            accion.texto, Modifier.weight(1f, fill = false),
            style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.005).em, color = Paleta.tinta),
            maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 15.sp, 0.5.sp),
        )
    }
}
