package mpensum.carrera

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mpensum.logica.FalloLectura
import mpensum.logica.Salida
import mpensum.componentes.margenPantalla
import mpensum.componentes.pulsable
import mpensum.logica.cuandoEs
import mpensum.tema.Movimiento
import mpensum.tema.Tema
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/*
 * Leer horario (L1): cuatro tiempos. «¿Es esta?» enseña la foto en grande;
 * Leyendo la recorre con una línea mientras la lista de materias crece al
 * ritmo de la lectura real (el contador es de lo leído, no un porcentaje);
 * después la foto se encoge a una miniatura y las clases vuelan a su sitio de
 * la semana, como en la pantalla inicial; y una hoja pide revisar antes de
 * guardar. Mientras no se guarda, la semana que se ve es provisional.
 * En horizontal la foto queda a la izquierda y lo que se dice y se hace, a la
 * derecha, bajo la misma cabecera.
 */

/** Cuánto mide la foto cuando comparte el renglón con el texto; el alto sale de su proporción */
/** Una pasada de la línea de lectura, y lo que tarda en bajar al ritmo de lo leído */
private const val BARRIDO_MS = 1800
private const val LINEA_MS = 600
private val ANCHO_FOTO = 320.dp
private val ANCHO_TEXTO = 340.dp
private val AIRE_FOTO_TEXTO = 48.dp

@Composable
internal fun LecturaHorario(horario: EstadoHorario, lectura: Lectura, arriba: Dp, abajo: Dp, modifier: Modifier, horizontal: Boolean) {
    when (lectura.fase) {
        FaseLectura.ELEGIR -> PasoElegir(horario, lectura, arriba, abajo, modifier, horizontal)
        FaseLectura.LEYENDO -> PasoLeyendo(horario, lectura, arriba, abajo, modifier, horizontal)
        FaseLectura.VOLANDO, FaseLectura.CONFIRMAR -> PasoSemana(horario, lectura, arriba, abajo, modifier, horizontal)
    }
}

/** La flecha de atrás y el rótulo centrado de la cabecera */
@Composable
private fun CabeceraLectura(rotulo: String, alVolver: () -> Unit) {
    val l = letrasHorario()
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        Box(
            Modifier.size(44.dp).pulsable(alVolver, escala = 0.9f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                val k = size.width / 24f
                val p = androidx.compose.ui.graphics.Path().apply { moveTo(15f * k, 5f * k); lineTo(8f * k, 12f * k); lineTo(15f * k, 19f * k) }
                drawPath(p, Paleta.tinta, style = Stroke(1.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        BasicText(rotulo, Modifier.align(Alignment.Center), style = l.etiqueta)
    }
}

/** La captura como tarjeta: la foto o, sin ella, el fantasma gris; `escaneo` pone encima la línea de lectura (0 a 1) */
@Composable
private fun TarjetaCaptura(lectura: Lectura, modifier: Modifier, escaneo: (() -> Float)? = null) {
    val filas = rememberFilasCaptura()
    val d = LocalDensity.current.density
    Canvas(modifier) {
        val k = size.width / ESC_ANCHO
        withTransform({ scale(k, k, Offset.Zero) }) {
            tarjeta(lectura.foto, filas, 1f, 14f, d / k)
            if (escaneo != null) lineaDeLectura(escaneo() * ESC_ALTO)
        }
    }
}

@Composable
private fun PasoElegir(horario: EstadoHorario, lectura: Lectura, arriba: Dp, abajo: Dp, modifier: Modifier, horizontal: Boolean) {
    val alcance = rememberCoroutineScope()
    val lector = horario.lector
    // Abre la galería; si no se puede abrir la imagen, el motivo se dice en la pantalla y se conserva la anterior
    suspend fun pedirFoto(): Boolean {
        if (lector == null) return false
        try {
            val foto = lector.elegir() ?: return false
            lectura.foto = foto
            lectura.fallo = null
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: FalloLectura) {
            lectura.fallo = e
            return true
        }
    }
    // La primera foto se pide al abrir; si se cancela la galería, no hay nada que leer
    LaunchedEffect(lectura) {
        if (lectura.foto == null && lectura.fallo == null && !pedirFoto()) horario.cancelarLectura()
    }
    val elegirOtra: () -> Unit = { alcance.launch { pedirFoto() } }
    Column(modifier.fillMaxSize().padding(top = arriba + Espacio.xs, bottom = abajo + Espacio.s, start = margenPantalla(), end = margenPantalla())) {
        CabeceraLectura("LEER HORARIO", horario::cancelarLectura)
        if (horizontal) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AIRE_FOTO_TEXTO, Alignment.CenterHorizontally),
            ) {
                TarjetaCaptura(lectura, Modifier.width(ANCHO_FOTO).aspectRatio(ESC_ANCHO / ESC_ALTO))
                Column(Modifier.width(ANCHO_TEXTO).fillMaxHeight().padding(vertical = Espacio.xs)) {
                    ExplicacionElegir(horario, lectura)
                    Spacer(Modifier.weight(1f))
                    AccionesElegir(lectura, elegirOtra)
                }
            }
        } else {
            TarjetaCaptura(lectura, Modifier.padding(top = 36.dp, start = 11.dp, end = 11.dp).fillMaxWidth().height(286.dp))
            Column(Modifier.padding(top = 36.dp)) { ExplicacionElegir(horario, lectura) }
            Spacer(Modifier.weight(1f))
            AccionesElegir(lectura, elegirOtra)
        }
    }
}

/** «¿Es esta?» y, debajo, la pista o lo que falló y qué hacer */
@Composable
private fun ExplicacionElegir(horario: EstadoHorario, lectura: Lectura) {
    val l = letrasHorario()
    BasicText("¿Es esta?", Modifier.padding(start = Espacio.xxs), style = l.titulo.copy(fontSize = 36.sp, lineHeight = 40.sp))
    val fallo = lectura.fallo
    if (fallo == null) {
        BasicText(
            "Se ve la semana entera, de lunes a viernes. Así se lee mejor.",
            Modifier.padding(top = 10.dp, start = Espacio.xxs, end = Espacio.xxs),
            style = l.parrafo.copy(fontSize = 15.sp, lineHeight = 22.sp),
        )
    } else {
        // Lo que pasó y qué hacer, dentro de la propia pantalla: lo aplazado no es un error y no va en rojo
        BasicText(fallo.titulo, Modifier.padding(top = 10.dp, start = Espacio.xxs, end = Espacio.xxs), style = l.nombreFila.copy(color = if (fallo.aplazado) Paleta.tinta else Paleta.rojo))
        BasicText(fallo.consejo, Modifier.padding(top = Espacio.xxs, start = Espacio.xxs, end = Espacio.xxs), style = l.parrafo.copy(fontSize = 15.sp, lineHeight = 22.sp))
        if (fallo.salida == Salida.A_MANO) {
            Box(
                Modifier.padding(top = Espacio.xxs).height(44.dp).pulsable({
                    horario.cancelarLectura()
                    horario.abrirNueva(if (horario.ahora.dia < 5) horario.ahora.dia else 0, 7 * 60)
                }, escala = 0.96f).padding(horizontal = Espacio.xxs),
                contentAlignment = Alignment.CenterStart,
            ) { BasicText("Crearlo manualmente", style = l.accion.copy(fontSize = 15.sp)) }
        }
    }
}

/** El único botón lleno, «Elegir otra» y el sello */
@Composable
private fun AccionesElegir(lectura: Lectura, elegirOtra: () -> Unit) {
    val l = letrasHorario()
    BotonTinta("Leer esta captura", Modifier.padding(horizontal = Espacio.xxs), icono = { trazarEscaneo() }) {
        if (lectura.foto != null) lectura.fase = FaseLectura.LEYENDO
    }
    Box(
        Modifier.fillMaxWidth().height(48.dp).padding(top = Espacio.hilo).pulsable(elegirOtra, escala = 0.96f),
        contentAlignment = Alignment.Center,
    ) { BasicText("Elegir otra", style = l.accion.copy(fontSize = 15.sp, color = Paleta.suave)) }
    Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) { SelloIntradace() }
}

/** El icono de escanear: cuatro esquinas y una línea, en la rejilla de 24 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.trazarEscaneo() {
    val k = size.width / 24f
    val g = Stroke(1.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(4f * k, 8f * k); lineTo(4f * k, 5f * k); lineTo(7f * k, 5f * k)
        moveTo(17f * k, 5f * k); lineTo(20f * k, 5f * k); lineTo(20f * k, 8f * k)
        moveTo(20f * k, 16f * k); lineTo(20f * k, 19f * k); lineTo(17f * k, 19f * k)
        moveTo(7f * k, 19f * k); lineTo(4f * k, 19f * k); lineTo(4f * k, 16f * k)
        moveTo(4f * k, 12f * k); lineTo(20f * k, 12f * k)
    }
    drawPath(p, Paleta.lienzo, style = g)
}

@Composable
private fun PasoLeyendo(horario: EstadoHorario, lectura: Lectura, arriba: Dp, abajo: Dp, modifier: Modifier, horizontal: Boolean) {
    // Lee de verdad, pero la lectura corre en el horario y no aquí: si se cambia de vista a media lectura,
    // sigue, y al volver esta llamada no lanza otra (cada una gasta una de las lecturas del día)
    LaunchedEffect(lectura) { horario.leerCaptura(lectura, respaldo = this) }
    val leidas = lectura.leidas.size
    // Mientras el servidor lee no se sabe cuánto falta: la línea recorre la foto sin decir un porcentaje. Con las materias, baja al ritmo de lo leído
    val barrido by rememberInfiniteTransition(label = "barrido").animateFloat(
        0f, 1f, infiniteRepeatable(tween(BARRIDO_MS, easing = LinearEasing), RepeatMode.Reverse), label = "barrido",
    )
    val real by animateFloatAsState(if (lectura.total > 0) (leidas.toFloat() / lectura.total).coerceIn(0f, 1f) else 0f, tween(LINEA_MS, easing = Movimiento.FLUIDA), label = "linea")
    val escaneo = { if (lectura.total > 0) real else barrido }
    Column(modifier.fillMaxSize().padding(top = arriba + Espacio.xs, bottom = abajo + Espacio.s, start = margenPantalla(), end = margenPantalla())) {
        CabeceraLectura("LEER HORARIO", horario::cancelarLectura)
        if (horizontal) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AIRE_FOTO_TEXTO, Alignment.CenterHorizontally),
            ) {
                TarjetaCaptura(lectura, Modifier.width(ANCHO_FOTO).aspectRatio(ESC_ANCHO / ESC_ALTO), escaneo)
                Column(Modifier.width(ANCHO_TEXTO).fillMaxHeight().padding(top = Espacio.s)) { ListaLeyendo(horario, lectura) }
            }
        } else {
            TarjetaCaptura(lectura, Modifier.padding(top = Espacio.xl, start = 27.dp, end = 27.dp).fillMaxWidth().height(258.dp), escaneo)
            Column(Modifier.padding(top = 26.dp)) { ListaLeyendo(horario, lectura) }
        }
    }
}

/** El título, cuántas van y la lista de materias, que crece al ritmo de la lectura */
@Composable
private fun ListaLeyendo(horario: EstadoHorario, lectura: Lectura) {
    val l = letrasHorario()
    BasicText("Leyendo tu captura", Modifier.padding(start = Espacio.xxs), style = l.tituloHoja.copy(fontSize = 26.sp, lineHeight = 30.sp))
    BasicText(
        if (lectura.total > 0) "${lectura.leidas.size} de ${lectura.total} materias" else "Mirando la captura",
        Modifier.padding(top = Espacio.xxs, start = Espacio.xxs),
        style = l.dato,
    )
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
        for (m in lectura.leidas) key(m.codigo) { FilaLeyendo(horario, m) }
    }
}

@Composable
private fun FilaLeyendo(horario: EstadoHorario, m: mpensum.logica.MateriaLeida) {
    val l = letrasHorario()
    // Entra al leerse: sube desde 8 dp mientras aparece
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(420, easing = Movimiento.FLUIDA)) }
    val color = horario.color(m.codigo)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { alpha = entrada.value; translationY = (1f - entrada.value) * 8.dp.toPx() }
            .padding(start = Espacio.xxs, end = Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(32.dp).drawBehind { drawRoundRect(color, cornerRadius = CornerRadius(1.5.dp.toPx())) })
        Column(Modifier.weight(1f).padding(start = Espacio.s)) {
            BasicText(horario.nombre(m.codigo), style = l.nombreFila.copy(fontSize = 14.5.sp), maxLines = 1)
            BasicText(cuandoEs(m.sesiones.first(), m.sesiones, Tema.hora24), Modifier.padding(top = 1.dp), style = l.pequeno, maxLines = 1)
        }
        Canvas(Modifier.size(16.dp)) {
            val k = size.width / 16f
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(3f * k, 8.5f * k); lineTo(6.5f * k, 12f * k); lineTo(13f * k, 4.5f * k) }
            drawPath(p, Paleta.suave, style = Stroke(1.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Los colores de cada clase leída: los de su materia en el mapa */
@Composable
private fun PasoSemana(horario: EstadoHorario, lectura: Lectura, arriba: Dp, abajo: Dp, modifier: Modifier, horizontal: Boolean) {
    val l = letrasHorario()
    val sesiones = remember(lectura.fase) { lectura.sesiones }
    val volando = lectura.fase == FaseLectura.VOLANDO
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = margenPantalla(), end = margenPantalla(), top = arriba + Espacio.xs, bottom = abajo + if (horizontal) Espacio.s else Espacio.xl + Espacio.xxs),
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            Box(Modifier.size(44.dp).pulsable(horario::cancelarLectura, escala = 0.9f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    val p = androidx.compose.ui.graphics.Path().apply { moveTo(15f * k, 5f * k); lineTo(8f * k, 12f * k); lineTo(15f * k, 19f * k) }
                    drawPath(p, Paleta.tinta, style = Stroke(1.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (!volando) BasicText("Tu semana", Modifier.align(Alignment.CenterStart).padding(start = 46.dp), style = l.tituloHoja)
            // En horizontal el resumen cabe en la cabecera, a la derecha, y la rejilla empieza más arriba
            if (!volando && horizontal) BasicText(resumenSemana(sesiones), Modifier.align(Alignment.CenterEnd), style = l.texto, maxLines = 1)
        }
        Crossfade(volando, animationSpec = tween(Movimiento.CAMBIO_VISTA), label = "semana-leida") { vuela ->
            if (vuela) {
                val plano = remember(sesiones) { PlanoVuelo(sesiones, sesiones.map { horario.color(it.codigo) }, inicio = 0f) }
                val reloj = rememberRelojVuelo(plano.total) {
                    if (lectura.fase == FaseLectura.VOLANDO) {
                        lectura.fase = FaseLectura.CONFIRMAR
                        horario.hoja = HojaHorario.Confirmar
                    }
                }
                val opciones = remember(lectura.foto) { OpcionesEscena(foto = lectura.foto, miniatura = Rect(262f, -58f, 292f, -32f)) }
                // La miniatura de la foto sube 58 unidades sobre la escena: ese es el aire que necesita arriba
                Box(Modifier.fillMaxWidth().padding(top = if (horizontal) 52.dp else 90.dp), contentAlignment = Alignment.TopCenter) {
                    EscenaVuelo(plano, opciones, { reloj.value })
                }
            } else {
                Column { SemanaRejilla(horario, sesiones, provisional = true, compacto = horizontal) }
            }
        }
    }
}
