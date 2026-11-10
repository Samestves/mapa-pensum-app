package mpensum.carrera

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import mpensum.carrera.EstadoCarrera
import mpensum.logica.Situacion
import mpensum.logica.Tramo
import mpensum.logica.tramoDe
import mpensum.tema.Movimiento

/* La luz que viaja por los cables de la frontera de lo que puedes cursar */

/** La luz de la frontera escribe su reloj a 30 cuadros por segundo, como la aurora */
private const val CUADRO_LUZ_NS = 32_000_000L

/** Sin tocar el mapa tanto tiempo, la luz se apaga hasta el siguiente gesto */
private const val DORMIR_LUZ_MS = 8_000L

/**
 * La luz que viaja por la frontera hacia lo que puedes inscribir: un guion
 * del 9 % del cable que nace y muere apagado. Cada luz a su ritmo, entre 2,6
 * y 3,2 s, y nacida a media marcha. Va en su propia capa, y se congela
 * mientras el mapa se mueve o hay una hoja encima: en un teléfono modesto
 * la luz corriendo durante un gesto le quita cuadros.
 */
@Composable
internal fun LuzDeFrontera(estado: EstadoCarrera, cables: List<CableDibujo>, congelada: () -> Boolean, sinGesto: () -> Boolean, cadena: () -> Set<String>?, foco: () -> Float) {
    // El reloj solo corre mientras la luz se ve moverse: congelada, el bucle se para y no pide cuadros;
    // al reanudar sigue desde donde iba, sin salto
    var reloj by remember { mutableFloatStateOf(0f) }
    val estaCongelada by rememberUpdatedState(congelada)
    val estaSinGesto by rememberUpdatedState(sinGesto)
    // Tras DORMIR_LUZ_MS sin gesto la luz se apaga del todo; el siguiente gesto la despierta
    var dormida by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { estaSinGesto() }.collectLatest { quieto ->
            dormida = false
            if (!quieto) return@collectLatest
            delay(DORMIR_LUZ_MS)
            dormida = true
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { estaCongelada() || dormida }.collectLatest { quieta ->
            if (quieta) return@collectLatest
            var antes = -1L
            var acumulado = 0L
            while (true) {
                withFrameNanos { n ->
                    // Tras un cuadro perdido la luz no da un salto
                    if (antes >= 0) acumulado += minOf(n - antes, 100_000_000L)
                    antes = n
                    // A 30 cuadros como la aurora: cada escritura invalida el cristal de la barra
                    if (acumulado >= CUADRO_LUZ_NS) {
                        reloj += acumulado / 1e9f
                        acumulado = 0L
                    }
                }
            }
        }
    }
    Spacer(
        // En su propia capa: cada cuadro de la luz repinta solo la luz, no los cables ni las cabeceras de debajo
        Modifier.fillMaxSize().graphicsLayer {}.drawWithCache {
            // Las fronteras y su degradado cambian al marcar, no en cada cuadro
            val sit = estado.situaciones
            val fronteras = cables.withIndex().filter { (_, c) ->
                tramoDe(sit[c.arista.origen] ?: Situacion.LEJANA, sit[c.arista.destino] ?: Situacion.LEJANA) == Tramo.FRONTERA
            }
            val vivos = fronteras.map { (_, c) -> degradado(c, true) }
            val trazoLuz = Stroke(2.25f, cap = StrokeCap.Round)
            onDrawBehind {
                val enFoco = cadena()
                val f = foco()
                if (fronteras.isEmpty()) return@onDrawBehind
                val segundos = reloj
                scale(density, Offset.Zero) {
                    for ((k, ic) in fronteras.withIndex()) {
                        val (i, c) = ic
                        val enCadena = enFoco != null && c.arista.origen in enFoco && c.arista.destino in enFoco
                        val apagado = if (enFoco != null && !enCadena) 1f - f else 1f
                        if (apagado <= 0f) continue
                        val duracion = 2.6f + ((i * 5) % 7) * 0.1f
                        val retraso = ((i * 7) % 11) * 0.26f
                        val bruto = ((segundos + retraso) / duracion) % 1f
                        val p = Movimiento.Mapa.VIAJE_LUZ.transform(bruto)
                        val alfa = when {
                            bruto < 0.14f -> bruto / 0.14f
                            bruto > 0.82f -> (1f - bruto) / 0.18f
                            else -> 1f
                        } * apagado
                        // El guion (9 % del cable) entra por el origen y sale por el destino
                        val fin = c.largo * p
                        val trozo = c.trozo(fin - c.largo * 0.09f, fin)
                        drawPath(trozo, vivos[k], alpha = alfa.coerceIn(0f, 1f), style = trazoLuz)
                    }
                }
            }
        },
    )
}
