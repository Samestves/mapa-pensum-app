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
import mpensum.tema.Movimiento

/** Lo que reserva una pantalla interna mientras llega su cálculo: lo que mide una con unas seis materias */
internal val RESERVA_DETALLE: Dp = 480.dp

/**
 * Lo pesado de una pantalla interna (decenas de simulaciones). Lo normal es
 * que ya esté: `EstadoCarrera.precalentar` lo calcula en segundo plano al
 * entrar en la carrera, y entonces `listo` es true y la pantalla entra con sus
 * datos puestos, sin reserva ni fundido. Si todavía no está, se calcula fuera
 * del hilo principal, ocupa `reserva` de alto y al llegar el contenido se
 * funde una sola vez. Se vuelve a calcular cuando cambia `clave`.
 */
@Composable
internal fun <T : Any> Calculado(clave: Any, reserva: Dp = RESERVA_DETALLE, listo: () -> Boolean = { false }, calculo: () -> T, contenido: @Composable (T) -> Unit) {
    val inicial = remember { if (listo()) calculo() else null }
    val resultado = produceState(inicial, clave) { value = if (listo()) calculo() else withContext(Dispatchers.Default) { calculo() } }.value
    if (resultado == null) {
        Spacer(Modifier.fillMaxWidth().height(reserva))
        return
    }
    if (inicial != null) {
        contenido(resultado)
        return
    }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(Movimiento.MEDIA, easing = Movimiento.ESTANDAR)) }
    Box(Modifier.graphicsLayer { alpha = entrada.value }) { contenido(resultado) }
}
