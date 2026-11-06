package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.tema.Espacio

/** Por debajo de este alto (dp) la ventana apaisada es un teléfono acostado, no una tableta */
private const val ALTO_TELEFONO_ACOSTADO = 480f

/** Por debajo de este ancho la ventana es compacta: teléfonos de 320-359 dp */
private val ANCHO_NORMAL = 360.dp

/** Desde este ancho la ventana es amplia: tabletas y teléfonos acostados */
private val ANCHO_AMPLIO = 600.dp

/** A partir de esta escala de letra (la del sistema por la de «Texto grande») la letra es grande */
private const val LETRA_GRANDE = 1.3f

/** El ancho de la ventana en tres niveles; las pantallas se adaptan a esto, no a un número de dp */
enum class Anchura { Compacta, Normal, Amplia }

/**
 * La ventana donde se dibuja la app, medida una vez en la raíz. Es la única
 * regla de la app para decidir la disposición: las pantallas no deben
 * inventar la suya ni leer `LocalWindowInfo` por su cuenta.
 */
@Immutable
data class Ventana(val ancho: Dp, val alto: Dp, val escalaLetra: Float = 1f) {
    /** Un teléfono en horizontal: más ancho que alto y bajo */
    val apaisado: Boolean get() = ancho > alto && alto.value < ALTO_TELEFONO_ACOSTADO

    val anchura: Anchura get() = when {
        ancho < ANCHO_NORMAL -> Anchura.Compacta
        ancho < ANCHO_AMPLIO -> Anchura.Normal
        else -> Anchura.Amplia
    }

    val compacta: Boolean get() = anchura == Anchura.Compacta

    /** Si la letra es grande: conviene partir en dos líneas lo que va en una fila */
    val letraGrande: Boolean get() = escalaLetra >= LETRA_GRANDE

    /** El margen lateral de pantalla: 16 dp en compacta, 20 en normal y 24 en amplia */
    val margen: Dp get() = when (anchura) {
        Anchura.Compacta -> Espacio.m
        Anchura.Normal -> Espacio.l
        Anchura.Amplia -> Espacio.xl
    }

    /**
     * Cuántas columnas de al menos [minimo] caben en el ancho útil (sin los
     * márgenes), separadas por [hueco], entre 1 y [maximo].
     */
    fun columnas(minimo: Dp, maximo: Int = 4, hueco: Dp = Espacio.xs): Int {
        val util = ancho - margen * 2
        return ((util + hueco) / (minimo + hueco)).toInt().coerceIn(1, maximo)
    }
}

/** La ventana de la raíz; nula fuera de App (en los tests de captura), y entonces se mide en el sitio */
val LocalVentana = staticCompositionLocalOf<Ventana?> { null }

/** Mide la ventana actual con la densidad vigente; App la mide una vez y la provee en [LocalVentana] */
@Composable
@ReadOnlyComposable
fun medirVentana(): Ventana {
    val tam = LocalWindowInfo.current.containerSize
    val densidad = LocalDensity.current
    return Ventana((tam.width / densidad.density).dp, (tam.height / densidad.density).dp, densidad.fontScale)
}

/** La ventana actual: la de la raíz si la hay, o medida aquí */
@Composable
@ReadOnlyComposable
fun ventana(): Ventana = LocalVentana.current ?: medirVentana()

/** El margen lateral de pantalla según la ventana: 16, 20 o 24 dp */
@Composable
@ReadOnlyComposable
fun margenPantalla(): Dp = ventana().margen
