package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** Por debajo de este alto (dp) la ventana apaisada es un teléfono acostado, no una tableta */
private const val ALTO_TELEFONO_ACOSTADO = 480f

/**
 * Si la ventana es un teléfono en horizontal: más ancha que alta y baja.
 * Es la única regla de la app para decidir la disposición apaisada; las
 * pantallas no deben inventar la suya.
 */
@Composable
@ReadOnlyComposable
fun esApaisado(): Boolean {
    val ventana = LocalWindowInfo.current.containerSize
    val alto = ventana.height / LocalDensity.current.density
    return ventana.width > ventana.height && alto < ALTO_TELEFONO_ACOSTADO
}
