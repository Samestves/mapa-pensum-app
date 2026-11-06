package mpensum.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.recursos.Res
import mpensum.recursos.rosa
import mpensum.tema.Paleta
import mpensum.tema.Tema
import org.jetbrains.compose.resources.painterResource

/**
 * Los cuatro nodos de la marca, en fracciones de la caja del logotipo
 * (centro x, centro y, radio), medidos sobre los trazos de logoTrazos.js.
 */
val NODOS_ROSA = listOf(
    Triple(480.1f / 1230f, 292f / 1230f, 20.3f / 1230f),
    Triple(971.8f / 1230f, 538.1f / 1230f, 20.3f / 1230f),
    Triple(324.9f / 1230f, 806.6f / 1230f, 22.1f / 1230f),
    Triple(838f / 1230f, 1032.8f / 1230f, 37.5f / 1230f),
)

/**
 * La rosa de los vientos de la web. El cuerpo es el vector original; los
 * nodos se dibujan aparte para poder encenderlos uno a uno.
 * `nodos(i)` devuelve la escala de cada nodo (0 apagado, 1 normal).
 * Con `halo`, cada nodo lleva un resplandor corto de su azul: en tamaños
 * pequeños, como la cabecera, el punto solo no llega a leerse.
 */
@Composable
fun Rosa(tam: Dp, modifier: Modifier = Modifier, colorNodos: Color = Paleta.azul, nodos: (Int) -> Float = { 1f }, halo: Boolean = false) {
    Box(modifier.size(tam)) {
        // El vector viene en la tinta del oscuro; en claro se tiñe de la tinta clara
        Image(
            painterResource(Res.drawable.rosa), contentDescription = "Mapa de Pensum", modifier = Modifier.fillMaxSize(),
            colorFilter = if (Tema.claro) ColorFilter.tint(Paleta.tinta) else null,
        )
        Canvas(Modifier.fillMaxSize()) {
            NODOS_ROSA.forEachIndexed { i, (x, y, r) ->
                val e = nodos(i)
                if (e <= 0f) return@forEachIndexed
                val centro = Offset(x * size.width, y * size.height)
                val radio = r * size.width * e * 1.05f
                if (halo) {
                    val luz = radio + 3.dp.toPx() * e
                    drawCircle(Brush.radialGradient(listOf(colorNodos.copy(alpha = 0.75f), colorNodos.copy(alpha = 0f)), centro, luz), luz, centro)
                }
                drawCircle(colorNodos, radius = radio, center = centro)
            }
        }
    }
}
