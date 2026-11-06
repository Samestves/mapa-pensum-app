package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Si la ventana es un teléfono en horizontal: más ancha que alta y baja.
 * Es fachada de [Ventana.apaisado], que es la única regla de la app; se
 * mantiene para no tocar los usos que ya había.
 */
@Composable
@ReadOnlyComposable
fun esApaisado(): Boolean = ventana().apaisado
