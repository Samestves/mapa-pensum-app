package mpensum.tema

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CancellationException

/**
 * Pone el tema y, al cambiarlo, funde el viejo sobre el nuevo.
 *
 * El fundido no anima colores: un teléfono modesto no puede recomponer la app
 * entera en cada cuadro durante un cuarto de segundo. Hace lo que hace iOS:
 * toma una foto de la pantalla tal como está, cambia el tema de una vez (una
 * sola recomposición) y deja la foto encima, apagándose en 250 ms. Lo único
 * que se anima es la opacidad de una capa: no recompone ni redibuja nada.
 *
 * La primera vez el tema se pone sin fundido: la app abre ya en el suyo, y no
 * se recrea la actividad en ningún cambio.
 */
@Composable
fun TemaVivo(juego: Juego, contenido: @Composable () -> Unit) {
    remember { Tema.juego = juego }
    val capa = rememberGraphicsLayer()
    var foto by remember { mutableStateOf<ImageBitmap?>(null) }
    val velo = remember { Animatable(1f) }

    LaunchedEffect(juego) {
        if (Tema.juego == juego) return@LaunchedEffect
        // Si la foto falla (sin capa todavía, sin memoria), el tema cambia igual, sin fundido
        val tomada = try {
            capa.toImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
        // La foto y el tema nuevo llegan en el mismo cuadro: no hay ni uno con el tema a medias
        Snapshot.withMutableSnapshot {
            foto = tomada
            Tema.juego = juego
        }
        if (tomada == null) return@LaunchedEffect
        try {
            velo.snapTo(1f)
            velo.animateTo(0f, tween(Movimiento.MEDIA, easing = Movimiento.SUAVE))
        } finally {
            foto = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Lo que se pinta pasa por una capa propia: es la que se fotografía
        Box(
            Modifier.fillMaxSize().drawWithContent {
                capa.record { this@drawWithContent.drawContent() }
                drawLayer(capa)
            },
        ) { contenido() }
        foto?.let { f ->
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = velo.value }) { drawImage(f) }
        }
    }
}
