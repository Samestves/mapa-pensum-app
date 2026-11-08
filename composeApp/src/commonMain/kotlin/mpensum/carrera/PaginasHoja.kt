package mpensum.carrera

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import mpensum.tema.Movimiento

/*
 * Patrón A de docs/qa/transiciones.md: una hoja con páginas. La hoja no
 * cambia de alto; la cabecera y el cuerpo comparten un solo reloj
 * (`updateTransition`) y en cada cambio primero sale una página y después
 * entra la otra, sin un cuadro en que se lean las dos a la vez.
 */

/**
 * El empuje de una página a otra dentro de una hoja: la que sale se va rápido
 * hacia atrás (100 ms) y la que entra llega después (250 ms) desde 24 dp. Con
 * «Reducir movimiento», un fundido corto y sin desplazamiento. El alto no se
 * anima (`snap`) salvo con `animarAlto`: con la hoja de alto fijo no mueve nada.
 */
internal fun empujeDePagina(adelante: Boolean, desplazoPx: Int, animarAlto: Boolean = false): ContentTransform {
    if (Movimiento.reducido) return ContentTransform(fadeIn(tween(Movimiento.CORTA)), fadeOut(tween(Movimiento.CORTA)), sizeTransform = SizeTransform(clip = true) { _, _ -> if (animarAlto) Movimiento.ALTO else snap() })
    val signo = if (adelante) 1 else -1
    val entra = fadeIn(tween(Movimiento.MEDIA, Movimiento.SALIDA_RAPIDA, Movimiento.ENTRA)) +
        slideInHorizontally(tween(Movimiento.MEDIA, Movimiento.SALIDA_RAPIDA, Movimiento.ENTRA)) { signo * desplazoPx }
    val sale = fadeOut(tween(Movimiento.SALIDA_RAPIDA, easing = Movimiento.SALE)) +
        slideOutHorizontally(tween(Movimiento.SALIDA_RAPIDA, easing = Movimiento.SALE)) { -signo * desplazoPx }
    // Una hoja de alto fijo no necesita animarlo; la que mide su contenido lo anima una sola vez, con el resorte de las hojas
    val alto = SizeTransform(clip = true) { _, _ -> if (animarAlto) Movimiento.ALTO else snap() }
    return ContentTransform(entra, sale, sizeTransform = alto)
}

/**
 * Una cabecera de página que nunca mide menos de lo que ya midió: la del
 * título grande es más alta que la de «volver», y si la hoja la encogiera al
 * cambiar de página, todo el cuerpo saltaría esa diferencia en un cuadro.
 */
@Composable
internal fun CabeceraDeAltoEstable(contenido: @Composable () -> Unit) {
    val mayor = remember { IntArray(1) }
    Box(
        Modifier.layout { medible, restriccion ->
            val colocable = medible.measure(restriccion)
            mayor[0] = maxOf(mayor[0], colocable.height)
            val alto = mayor[0].coerceIn(restriccion.minHeight, restriccion.maxHeight)
            layout(colocable.width, alto) { colocable.placeRelative(0, 0) }
        },
    ) { contenido() }
}

/** La clave con que cada página guarda su estado (su desplazamiento) mientras no está a la vista */
internal fun DetalleAvance?.clave(): String = when (this) {
    null -> "indice"
    DetalleAvance.EsteSemestre -> "este-semestre"
    DetalleAvance.Proximo -> "proximo"
    DetalleAvance.Cadena -> "cadena"
    DetalleAvance.Graduacion -> "graduacion"
    is DetalleAvance.Electivas -> "electivas-$clave"
}
