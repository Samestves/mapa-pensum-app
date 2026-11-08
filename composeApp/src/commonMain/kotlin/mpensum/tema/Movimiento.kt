package mpensum.tema

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/*
 * El movimiento de la app, en un solo sitio (docs/qa/transiciones.md): nadie
 * escribe un `tween` con números ni una curva sueltos. Las duraciones se
 * eligen por tamaño y distancia; las curvas, por lo que hace el elemento.
 */
object Movimiento {
    // Duraciones, por tamaño y distancia
    /** Píldora, botón, estado de una fila: lo pequeño */
    const val CORTA = 150
    /** Un contenido que se funde dentro del mismo sitio */
    const val MEDIA = 250
    /** Una página dentro de una hoja: 100 sale y 250 entra, en serie */
    const val EMPUJE = 350
    /** Mapa, lista y horario: 100 sale y 200 entra */
    const val CAMBIO_VISTA = 300
    /** Portada a carrera */
    const val PANTALLA = 420
    /** Entre los elementos de una misma entrada */
    const val ESCALONADO = 50
    /** Lo que sale de un fundido en serie: rápido, para dejar sitio a lo que entra */
    const val SALIDA_RAPIDA = 100

    // Curvas (Material 3)
    /** Emphasized decelerate: lo que llega se posa */
    val ENTRA = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Emphasized accelerate: lo que se va no frena */
    val SALE = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Lo que cambia sin salir de su sitio */
    val ESTANDAR = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Entradas largas de escena (guía, portada): sale muy rápido y se asienta despacio */
    val ASIENTA = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    /** Lo que crece o se enfoca en el mapa y el horario */
    val FLUIDA = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    // Resortes, sin rebote
    /** Cambio de alto de una hoja: asienta en unos 300 ms y, si llega otro cambio, sigue desde donde iba */
    val ALTO = spring<IntSize>(dampingRatio = 1f, stiffness = 500f)

    /** Lo que se desliza una página al entrar o salir de una hoja: basta para decir «adelante» o «atrás» */
    val DESPLAZO = 24.dp

    /** Con «Reducir movimiento» todo pasa a un fundido corto, sin desplazamiento */
    val reducido: Boolean get() = Tema.movimiento
}
