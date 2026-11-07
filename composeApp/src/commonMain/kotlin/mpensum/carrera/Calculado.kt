package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lo que reserva una pantalla interna mientras llega su cálculo: lo que mide una con unas seis materias */
internal val RESERVA_DETALLE: Dp = 480.dp

/**
 * Lo pesado de una pantalla interna (decenas de simulaciones), calculado
 * fuera del hilo principal para no perder cuadros mientras entra. Se vuelve a
 * calcular cuando cambia `clave`. Mientras llega ocupa `reserva` de alto, y al
 * llegar el contenido entra con un fundido corto: ni salto de altura ni parpadeo.
 */
@Composable
internal fun <T : Any> Calculado(clave: Any, reserva: Dp = RESERVA_DETALLE, calculo: () -> T, contenido: @Composable (T) -> Unit) {
    val resultado = produceState<T?>(null, clave) { value = withContext(Dispatchers.Default) { calculo() } }.value
    if (resultado == null) {
        Spacer(Modifier.fillMaxWidth().height(reserva))
        return
    }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(160)) }
    Box(Modifier.graphicsLayer { alpha = entrada.value }) { contenido(resultado) }
}
