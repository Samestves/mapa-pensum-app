package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import mpensum.logica.Marca
import mpensum.mapa.Camara
import mpensum.mapa.DURACION_MOSTRAR
import mpensum.mapa.Nodo
import mpensum.mapa.NodoMapa
import mpensum.mapa.Vista
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/*
 * La ficha acostada (docs/ficha-y-lista.md §2): una burbuja de 336 dp pegada a
 * su tarjeta, con una punta que la señala. Se dibuja en la misma capa que el
 * mapa y sigue a la tarjeta leyendo la cámara solo al dibujar, así que no
 * tiembla al arrastrar. No es un `Popup`: una ventana aparte iría un cuadro
 * por detrás. Sin desenfoque ni velo; el mapa sigue vivo debajo.
 */

private const val ANCHO_CUERPO = 336f
private const val LARGO_PUNTA = 12f
private const val BASE_PUNTA = 22f
/** La punta queda a 8 dp de la tarjeta (4 más allá del aro de selección) y el cuerpo a 20 */
private const val SEPARA_PUNTA = 8f
private const val TOPE_ALTO = 364f
private const val RADIO_CUERPO = 28f
/** La punta no se acerca a menos de esto de una esquina del cuerpo */
private const val PUNTA_A_ESQUINA = 40f

/** Lo que cabe en pantalla: los bordes (dp) donde la burbuja y su tarjeta pueden estar; el riel cuenta como borde */
class ZonaSegura(val izq: Float, val arriba: Float, val der: Float, val abajo: Float)

private enum class Lado { DERECHA, IZQUIERDA }

/** Dónde se ve la tarjeta ahora, en dp de pantalla */
private fun tarjetaEnPantalla(c: Camara, n: NodoMapa): Rect {
    val e = c.escala
    return Rect(
        c.x + n.x.toFloat() * e, c.y + n.y.toFloat() * e,
        c.x + (n.x + Nodo.ANCHO).toFloat() * e, c.y + (n.y + Nodo.ALTO).toFloat() * e,
    )
}

/** De qué lado sale y cuánto hay que deslizar el mapa para que quepan tarjeta y burbuja; nunca acerca ni aleja */
private class Plan(val lado: Lado, val dx: Float, val dy: Float)

private fun planear(t: Rect, z: ZonaSegura): Plan {
    val necesario = SEPARA_PUNTA + LARGO_PUNTA + ANCHO_CUERPO
    val sitioDer = z.der - t.right
    val sitioIzq = t.left - z.izq
    val lado = when {
        sitioDer >= necesario -> Lado.DERECHA
        sitioIzq >= necesario -> Lado.IZQUIERDA
        sitioDer >= sitioIzq -> Lado.DERECHA
        else -> Lado.IZQUIERDA
    }
    var dx = if (lado == Lado.DERECHA) minOf(0f, z.der - necesario - t.right) else maxOf(0f, z.izq + necesario - t.left)
    // La tarjeta manda: si no caben las dos, la burbuja se queda sin sitio antes que la tarjeta sin verse
    if (t.left + dx < z.izq) dx = z.izq - t.left
    if (t.right + dx > z.der) dx = z.der - t.right
    val dy = when {
        t.top < z.arriba -> z.arriba - t.top
        t.bottom > z.abajo -> z.abajo - t.bottom
        else -> 0f
    }
    return Plan(lado, dx, dy)
}

/** Dónde queda la burbuja a lo alto (arriba de su caja) y a qué altura de ella la punta; `alto` es el de la caja */
private class Sitio(val arriba: Float, val punta: Float)

private fun sitioDe(t: Rect, alto: Float, z: ZonaSegura): Sitio {
    val centro = (t.top + t.bottom) / 2
    val arriba = (centro - alto / 2).coerceIn(z.arriba, maxOf(z.arriba, z.abajo - alto))
    return Sitio(arriba, (centro - arriba).coerceIn(PUNTA_A_ESQUINA, maxOf(PUNTA_A_ESQUINA, alto - PUNTA_A_ESQUINA)))
}

@Composable
fun BoxScope.BurbujaFicha(
    codigo: String?,
    estado: EstadoCarrera,
    camara: Camara,
    zona: ZonaSegura,
    /** El tamaño de la pantalla (dp): fuera de él, la tarjeta ya no está */
    ancho: Float,
    alto: Float,
    alCerrar: () -> Unit,
    alMarcar: (String, Marca?) -> Unit,
    alIrA: (String) -> Unit,
    alCambiarElectiva: (String) -> Unit,
) {
    // La última que se enseñó sigue pintándose mientras la burbuja se cierra
    var ultima by remember { mutableStateOf(codigo) }
    if (codigo != null) ultima = codigo
    var montada by remember { mutableStateOf(codigo != null) }
    if (codigo != null) montada = true
    var lado by remember { mutableStateOf(Lado.DERECHA) }
    val alcance = rememberCoroutineScope()
    val apertura = remember { Animatable(0f) }
    val salto = remember { Animatable(1f) }

    ManejarAtras(codigo != null, alCerrar)
    LaunchedEffect(codigo != null) {
        if (codigo != null) apertura.animateTo(1f, tween(180)) else {
            apertura.animateTo(0f, tween(120))
            montada = false
        }
    }
    // Otra materia: salta (escala 0,96 → 1 desde la punta) y el mapa se desliza lo justo
    LaunchedEffect(codigo) {
        val c = codigo ?: return@LaunchedEffect
        val nodo = estado.nodoDe(c) ?: return@LaunchedEffect
        val plan = planear(tarjetaEnPantalla(camara, nodo), zona)
        lado = plan.lado
        if (abs(plan.dx) > 1f || abs(plan.dy) > 1f) camara.animarHacia(alcance, Vista(camara.x + plan.dx, camara.y + plan.dy, camara.escala), DURACION_MOSTRAR)
        salto.snapTo(0f)
        salto.animateTo(1f, tween(150))
    }
    // Al soltar el mapa se recoloca el lado; durante el gesto se queda fijo
    LaunchedEffect(codigo) {
        val nodo = codigo?.let { estado.nodoDe(it) } ?: return@LaunchedEffect
        snapshotFlow { camara.enGesto }.collect { if (!it) lado = planear(tarjetaEnPantalla(camara, nodo), zona).lado }
    }
    // Se cierra si la tarjeta sale de la pantalla o si empiezas a pellizcar
    LaunchedEffect(codigo) {
        val nodo = codigo?.let { estado.nodoDe(it) } ?: return@LaunchedEffect
        val escala0 = camara.escala
        snapshotFlow {
            val t = tarjetaEnPantalla(camara, nodo)
            abs(camara.escala - escala0) > 0.003f || (t.left + t.right) / 2 !in 0f..ancho || (t.top + t.bottom) / 2 !in 0f..alto
        }.first { it }
        alCerrar()
    }

    if (!montada) return
    val actual = ultima ?: return
    val nodo = estado.nodoDe(actual) ?: return
    val izquierda = lado == Lado.IZQUIERDA
    val tope = minOf(TOPE_ALTO, alto - zona.arriba - 12f)

    Box(
        Modifier
            .align(Alignment.TopStart)
            .width((ANCHO_CUERPO + LARGO_PUNTA).dp)
            .heightIn(max = tope.dp)
            .graphicsLayer {
                if (size.height <= 0f) {
                    // Antes de medirla no se ve: no aparece un cuadro en la esquina
                    this.alpha = 0f
                    return@graphicsLayer
                }
                val t = tarjetaEnPantalla(camara, nodo)
                val h = size.height / density
                val s = sitioDe(t, h, zona)
                val x = if (izquierda) t.left - SEPARA_PUNTA - (ANCHO_CUERPO + LARGO_PUNTA) else t.right + SEPARA_PUNTA
                translationX = x * density
                translationY = s.arriba * density
                transformOrigin = TransformOrigin(if (izquierda) 1f else 0f, s.punta / h)
                val escala = 0.96f + 0.04f * apertura.value * salto.value
                scaleX = escala
                scaleY = escala
                this.alpha = apertura.value
            }
            .drawBehind {
                val s = sitioDe(tarjetaEnPantalla(camara, nodo), size.height / density, zona)
                dibujarBurbuja(s.punta.dp.toPx(), izquierda)
            }
            .pointerInput(Unit) { detectTapGestures {} },
    ) {
        Column(
            Modifier
                .padding(start = if (izquierda) 0.dp else LARGO_PUNTA.dp, end = if (izquierda) LARGO_PUNTA.dp else 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(RADIO_CUERPO.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            FundidoDeMateria(actual) { c -> CuerpoBurbuja(c, estado, alMarcar, alIrA, alCambiarElectiva) }
        }
    }
}

/** Las piezas de la ficha de pie, en pequeño: sin X (se cierra tocando el mapa o con Atrás) y con Pide · Abre en una línea */
@Composable
private fun CuerpoBurbuja(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, alIrA: (String) -> Unit, alCambiar: (String) -> Unit) {
    val enc = encabezadoDe(codigo, estado) ?: return
    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)) {
        RotuloFicha(enc, alCerrar = {}, alCambiar = alCambiar, conCerrar = false)
        TituloFicha(enc, Modifier.padding(top = 2.dp), compacto = true)
        DatosFicha(enc, Modifier.padding(top = Espacio.xs))
        RespuestaFicha(codigo, estado, Modifier.padding(top = Espacio.s))
        PideAbreResumen(codigo, estado, alIrA, Modifier.padding(top = Espacio.xs))
        BotonesMarca(codigo, estado, alMarcar, Modifier.padding(top = Espacio.s), compacto = true)
    }
}

/**
 * El cuerpo y la punta en un solo `Path`: relleno del panel, filo de 1 dp y una
 * sombra corta de 2 dp (sin desenfoque). La punta mira a la izquierda y se
 * espeja si la burbuja sale por el otro lado; `punta` es su altura desde arriba.
 */
private fun DrawScope.dibujarBurbuja(punta: Float, izquierda: Boolean) {
    val l = LARGO_PUNTA.dp.toPx()
    val w = size.width
    val h = size.height
    val r = minOf(RADIO_CUERPO.dp.toPx(), h / 2)
    val mitad = BASE_PUNTA.dp.toPx() / 2
    val camino = Path().apply {
        fun esquina(v: Offset, desde: Offset, hacia: Offset, d: Float) {
            val a = (desde - v).let { v + it / it.getDistance() * d }
            val b = (hacia - v).let { v + it / it.getDistance() * d }
            lineTo(a.x, a.y)
            quadraticTo(v.x, v.y, b.x, b.y)
        }
        moveTo(l + r, 0f)
        lineTo(w - r, 0f)
        arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
        lineTo(w, h - r)
        arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
        lineTo(l + r, h)
        arcTo(Rect(l, h - 2 * r, l + 2 * r, h), 90f, 90f, false)
        val abajo = Offset(l, punta + mitad)
        val vertice = Offset(0f, punta)
        val arriba = Offset(l, punta - mitad)
        esquina(abajo, Offset(l, h - r), vertice, 3.dp.toPx())
        esquina(vertice, abajo, arriba, 6.dp.toPx())
        esquina(arriba, vertice, Offset(l, r), 3.dp.toPx())
        lineTo(l, r)
        arcTo(Rect(l, 0f, l + 2 * r, 2 * r), 180f, 90f, false)
        close()
    }
    val pintar: DrawScope.() -> Unit = {
        translate(0f, 2.dp.toPx()) { drawPath(camino, Paleta.sombra.copy(alpha = 0.35f)) }
        drawPath(camino, Paleta.panel)
        drawPath(camino, Paleta.tinta.copy(alpha = 0.16f), style = Stroke(1.dp.toPx()))
    }
    if (izquierda) scale(-1f, 1f, Offset(w / 2, 0f), pintar) else pintar()
}
