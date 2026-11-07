package mpensum.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import mpensum.tema.Tema

/**
 * El único punto por donde pasa la vibración de la app. Quien vibra sigue
 * pidiendo `LocalHapticFeedback.current`, pero lo que recibe es esto: con
 * «Vibración» apagada en Personalizar no se llama al sistema, sin un `if` en
 * cada sitio (lista, barra, mapa y ficha).
 */
private class TactoOpcional(private val sistema: HapticFeedback) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (Tema.vibracion) sistema.performHapticFeedback(hapticFeedbackType)
    }
}

/** Pone el tacto opcional en lugar del del sistema para todo lo que cuelga de [contenido] */
@Composable
fun ProveerTacto(contenido: @Composable () -> Unit) {
    val sistema = LocalHapticFeedback.current
    val tacto = remember(sistema) { TactoOpcional(sistema) }
    CompositionLocalProvider(LocalHapticFeedback provides tacto, content = contenido)
}
