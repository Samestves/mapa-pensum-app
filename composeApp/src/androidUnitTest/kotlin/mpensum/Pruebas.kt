package mpensum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlinx.coroutines.Dispatchers
import mpensum.carrera.LocalCalculo
import mpensum.carrera.LocalLatido

/**
 * Monta lo que se prueba con el cálculo pesado en el propio hilo de la prueba: nada espera a otro hilo, así
 * que entre cuadro y cuadro no hace falta dormir. Solo vale donde la pantalla recibe su estado ya hecho: lo
 * que lee recursos del paquete (App entera) sigue necesitando tiempo real.
 */
fun ComposeContentTestRule.setContentSincrono(contenido: @Composable () -> Unit) = setContent {
    CompositionLocalProvider(LocalCalculo provides Dispatchers.Unconfined, content = contenido)
}

/**
 * Sin el latido del anillo: un reposo del que medir cuadros. Con `sincrono`, además, el cálculo pesado corre
 * en el hilo de la prueba (ver [setContentSincrono]); sin él, en su hilo de siempre, que es lo que mide CosteCuadros.
 */
fun ComposeContentTestRule.setContentSinLatido(sincrono: Boolean = false, contenido: @Composable () -> Unit) = setContent {
    CompositionLocalProvider(
        LocalLatido provides false,
        LocalCalculo provides if (sincrono) Dispatchers.Unconfined else Dispatchers.Default,
        content = contenido,
    )
}
