package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Cuántas partes de algo pesado ya pueden componerse: la primera enseguida y
 * una más en cada cuadro, hasta `total`. Así ningún cuadro carga con todo
 * (crear los nodos y medir sus textos es lo que cuesta) y la animación que
 * arranca a la vez no pierde cuadros (docs/qa/rendimiento-60fps.md).
 *
 * Sirve donde lo que aún falta no se ve: una página que entra 100 ms después
 * de salir la anterior, una hoja que sube desde fuera de la pantalla o las
 * columnas del mapa que la cámara todavía no mira.
 */
@Composable
fun rememberPartesListas(total: Int): Int {
    var listas by remember { mutableIntStateOf(1) }
    LaunchedEffect(total) {
        while (listas < total) {
            withFrameNanos { }
            listas++
        }
    }
    return minOf(listas, total)
}
