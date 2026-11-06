package mpensum.mapa

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.hypot

/*
 * Los gestos del mapa (useVistaGrafo.js): arrastrar con un dedo, pellizcar
 * con dos, lanzar al soltar y el doble toque en el vacío.
 */

/** Lo que puede moverse un dedo apoyado sin dejar de ser un toque: los 8 de Android */
internal val HOLGURA_TACTIL = 8.dp
/** El doble toque: cuánto pueden separarse los dos toques en tiempo y en espacio */
private const val DOBLE_TOQUE_MS = 300L
private const val DOBLE_TOQUE_DP = 30f

private class Muestra(val t: Long, val x: Float, val y: Float)

/**
 * La velocidad al soltar, en dp/ms: la de los últimos 80 ms, y ninguna si el
 * dedo se quedó quieto antes de levantarse (velocidadDeLanzamiento).
 */
private fun velocidadAlSoltar(muestras: List<Muestra>, ahora: Long): Offset? {
    if (muestras.size < 2) return null
    val ultima = muestras.last()
    if (ahora - ultima.t > 60) return null
    val primera = muestras.firstOrNull { ultima.t - it.t <= 80 } ?: muestras.first()
    val dt = (ultima.t - primera.t).toFloat()
    if (dt <= 0f) return null
    return Offset((ultima.x - primera.x) / dt, (ultima.y - primera.y) / dt)
}

private class Pellizco(val distancia: Float, val centro: Offset)

private fun PointerEvent.pellizco(): Pellizco? {
    val dedos = changes.filter { it.pressed }
    if (dedos.size < 2) return null
    val (a, b) = dedos
    return Pellizco(hypot(a.position.x - b.position.x, a.position.y - b.position.y), (a.position + b.position) / 2f)
}

/**
 * Las tarjetas reciben primero el toque: si lo recogen (un toque o una
 * pulsación larga), el mapa no hace nada. Solo se apodera del gesto cuando el
 * dedo se mueve más que la holgura o entra un segundo dedo.
 */
fun Modifier.gestosDelMapa(camara: Camara, alTocarVacio: () -> Unit): Modifier = pointerInput(camara) {
    coroutineScope {
        val alcance = this
        var ultimoToque: Muestra? = null
        val holgura = HOLGURA_TACTIL.toPx()
        awaitEachGesture {
            val primero = awaitFirstDown(requireUnconsumed = false)
            camara.detener()
            camara.tomado = false
            val d = density
            var arrastrando = false
            var pellizco: Pellizco? = null
            var deUnDedo = true
            // Al levantar un dedo del pellizco no se sigue arrastrando con el otro: haría un salto
            var trasPellizco = false
            var recogido = false
            val muestras = ArrayDeque<Muestra>()
            var fin = primero.uptimeMillis

            do {
                val evento = awaitPointerEvent()
                fin = evento.changes.maxOf { it.uptimeMillis }
                if (!arrastrando && pellizco == null && evento.changes.any { it.isConsumed }) recogido = true
                val ahora = evento.pellizco()
                when {
                    ahora != null -> {
                        deUnDedo = false
                        arrastrando = false
                        camara.tomado = true
                        val antes = pellizco
                        if (antes != null && antes.distancia > 0f) {
                            camara.zoomEn(ahora.distancia / antes.distancia, ahora.centro.x / d, ahora.centro.y / d)
                            camara.desplazar((ahora.centro.x - antes.centro.x) / d, (ahora.centro.y - antes.centro.y) / d)
                            camara.marcarGesto(alcance)
                        }
                        pellizco = ahora
                        evento.changes.forEach { it.consume() }
                    }
                    pellizco != null -> {
                        pellizco = null
                        trasPellizco = true
                    }
                    !trasPellizco && !recogido -> {
                        val dedo = evento.changes.firstOrNull { it.id == primero.id } ?: evento.changes.first()
                        if (!arrastrando && (dedo.position - primero.position).getDistance() > holgura) {
                            arrastrando = true
                            camara.tomado = true
                        }
                        if (arrastrando && dedo.pressed) {
                            val paso = dedo.positionChange()
                            camara.desplazar(paso.x / d, paso.y / d)
                            camara.marcarGesto(alcance)
                            dedo.consume()
                            muestras.addLast(Muestra(dedo.uptimeMillis, dedo.position.x / d, dedo.position.y / d))
                            while (muestras.size > 2 && dedo.uptimeMillis - muestras.first().t > 100) muestras.removeFirst()
                        }
                    }
                }
            } while (evento.changes.any { it.pressed })

            if (arrastrando && deUnDedo) {
                velocidadAlSoltar(muestras, fin)?.let { camara.lanzar(alcance, it.x, it.y) }
            } else if (!arrastrando && deUnDedo && !trasPellizco && !recogido) {
                // Un toque limpio en el vacío: suelta la materia, y puede ser la mitad de un doble toque
                val toque = Muestra(fin, primero.position.x / d, primero.position.y / d)
                val antes = ultimoToque
                if (antes != null && toque.t - antes.t < DOBLE_TOQUE_MS && abs(hypot(toque.x - antes.x, toque.y - antes.y)) < DOBLE_TOQUE_DP) {
                    ultimoToque = null
                    camara.dobleToque(alcance, toque.x, toque.y)
                } else {
                    ultimoToque = toque
                    alTocarVacio()
                }
            }
        }
    }
}
