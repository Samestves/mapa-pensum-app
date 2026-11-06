package mpensum.mapa

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/*
 * Lo que el mapa anima en respuesta a algo que hiciste (estilos/mapa.css):
 * el anillo de la tarjeta que marcaste, la descarga verde por los cables de
 * la que aprobaste, el destello de lo que se abre y el contorno que se carga
 * al mantener el dedo en una tarjeta. Cada disparo lleva su número: marcar
 * dos veces la misma vuelve a lanzar la animación.
 */

/** Antes de esto soltar es un toque; con 180 cabe también quien toca despacio */
const val ESPERA_MS = 180
/** Lo que tarda el contorno en cerrarse una vez que asoma */
const val CARGA_MS = 320
/** Cuando la ruta queda fijada */
const val FIJA_MS = ESPERA_MS + CARGA_MS
/** Lo que tarda el contorno en irse cuando ya cumplió */
const val APAGADO_MS = 450

data class Disparo(val codigo: String, val n: Int)

/** La pulsación larga en curso: cargando, o ya fijada y apagándose */
data class Carga(val codigo: String, val n: Int, val hecha: Boolean = false)

@Stable
class EfectosMapa {
    private var cuenta = 0

    /** La última materia aprobada: su descarga por los cables y el destello de lo que abre */
    var descarga by mutableStateOf<Disparo?>(null)
        private set
    /** La última tarjeta marcada: su anillo de confirmación */
    var toque by mutableStateOf<Disparo?>(null)
        private set
    var carga by mutableStateOf<Carga?>(null)
        private set
    /** La ruta fijada manteniendo el dedo: su cadena queda encendida mientras se recorre el mapa */
    var ruta by mutableStateOf<String?>(null)

    fun tocar(codigo: String) {
        toque = Disparo(codigo, ++cuenta)
    }

    fun descargar(codigo: String) {
        descarga = Disparo(codigo, ++cuenta)
    }

    fun empezarCarga(codigo: String) {
        carga = Carga(codigo, ++cuenta)
    }

    fun soltarCarga() {
        if (carga?.hecha == false) carga = null
    }

    fun fijarRuta(codigo: String) {
        ruta = codigo
        carga = carga?.copy(hecha = true)
    }

    fun apagarCarga(n: Int) {
        if (carga?.n == n) carga = null
    }
}

/**
 * El toque y la pulsación larga de una tarjeta (hooks/useMantenerRuta.js).
 * Un toque abre la ficha; mantener el dedo quieto medio segundo fija su
 * ruta. Si el dedo se mueve más que la holgura, o el mapa empieza a moverse
 * por un pellizco, no es ninguna de las dos: el gesto es del mapa.
 */
fun Modifier.toqueYMantener(
    clave: Any,
    camara: Camara,
    alEmpezar: () -> Unit,
    alSoltar: () -> Unit,
    alTocar: () -> Unit,
    alFijar: () -> Unit,
): Modifier = pointerInput(clave) {
    val holgura = HOLGURA_TACTIL.toPx()
    awaitEachGesture {
        val abajo = awaitFirstDown(requireUnconsumed = false)
        alEmpezar()
        var ultimo = abajo.uptimeMillis
        val limite = abajo.uptimeMillis + FIJA_MS
        while (true) {
            val evento = withTimeoutOrNull(maxOf(1L, limite - ultimo)) { awaitPointerEvent() }
            if (evento == null) {
                if (camara.tomado) {
                    alSoltar()
                    return@awaitEachGesture
                }
                // Cumplió el tiempo con el dedo quieto: se fija la ruta y el toque de soltar se traga
                alFijar()
                do {
                    val resto = awaitPointerEvent()
                    resto.changes.forEach { it.consume() }
                } while (resto.changes.any { it.pressed })
                return@awaitEachGesture
            }
            ultimo = evento.changes.maxOf { it.uptimeMillis }
            val dedo = evento.changes.firstOrNull { it.id == abajo.id }
            val otroDedo = evento.changes.count { it.pressed } > 1
            if (dedo == null || otroDedo || camara.tomado || (dedo.position - abajo.position).getDistance() > holgura) {
                alSoltar()
                return@awaitEachGesture
            }
            if (!dedo.pressed) {
                dedo.consume()
                alSoltar()
                alTocar()
                return@awaitEachGesture
            }
        }
    }
}
