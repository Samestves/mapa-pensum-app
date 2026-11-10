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
    /** De la carrera a la portada: un poco más corta que la ida */
    const val PANTALLA_VUELTA = 380
    /** Entre los elementos de una misma entrada */
    const val ESCALONADO = 50
    /** Lo que tarda una hoja en irse (por el aspa o Atrás): lo que hay que esperar para mirar lo que tapaba */
    const val SALIDA_HOJA = 260
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
    /** Material «standard»: sale y llega con calma, sin el arranque seco de ESTANDAR. Fundidos y cambios de la guía y del tema */
    val SUAVE = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    /** Entra y sale con el mismo peso (easeInOutCubic): lo que cruza la pantalla de un lado a otro */
    val SIMETRICA = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    /** Las curvas de los efectos del mapa (anillo del toque, destello, carga y luz por el cable); la guía y el mapa de verdad comparten las mismas */
    object Mapa {
        /** El anillo que sale de la tarjeta al tocarla */
        val ANILLO = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
        /** El destello al encenderse una materia, y también cómo llega el mapa y cómo se apaga una carga */
        val DESTELLO = CubicBezierEasing(0.22f, 0.61f, 0.2f, 1f)
        /** La llegada del mapa la primera vez: la misma curva que el destello */
        val LLEGADA = DESTELLO
        /** El relleno de la carga de una tarjeta */
        val CARGA = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
        /** La descarga que baja por la cadena de materias */
        val DESCARGA = CubicBezierEasing(0.55f, 0f, 0.25f, 1f)
        /** La luz que viaja por un cable */
        val VIAJE_LUZ = CubicBezierEasing(0.45f, 0f, 0.3f, 1f)
    }

    // Resortes, sin rebote
    /** Cambio de alto de una hoja: asienta en unos 300 ms y, si llega otro cambio, sigue desde donde iba */
    val ALTO = spring<IntSize>(dampingRatio = 1f, stiffness = 500f)

    /** Lo que se desliza una página al entrar o salir de una hoja: basta para decir «adelante» o «atrás» */
    val DESPLAZO = 24.dp

    /** Con «Reducir movimiento» todo pasa a un fundido corto, sin desplazamiento */
    val reducido: Boolean get() = Tema.movimiento
}
