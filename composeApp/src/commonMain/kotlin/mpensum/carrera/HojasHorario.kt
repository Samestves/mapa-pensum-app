package mpensum.carrera

import mpensum.componentes.HojaInferior
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.logica.Sesion
import mpensum.logica.clases
import mpensum.logica.cuandoEs
import mpensum.logica.duracion
import mpensum.tema.Espacio
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tema

/*
 * Las hojas del horario, de cristal y flotando sobre la barra: el detalle de
 * una clase, la de crear o editar una, el «⋯» y la pregunta antes de borrarlo
 * todo. Se montan en el interior después de la barra, como las demás hojas.
 */

internal val ANCHO_NOMBRE = 96.dp
internal val LINEA get() = Paleta.tinta.copy(alpha = 0.07f)

/**
 * Las hojas del horario. Van en el Box del interior DESPUÉS de la barra para
 * taparla con su velo; `cristal` es el HazeState que recoge la vista (el de la
 * barra), y sin él la hoja es el panel opaco de siempre.
 */
@Composable
fun BoxScope.HojasHorario(horario: EstadoHorario, cristal: HazeState?) {
    // Mientras baja al cerrarse, la hoja sigue enseñando lo último que tenía
    val recuerdo = remember { arrayOfNulls<HojaHorario>(1) }
    horario.hoja?.let { recuerdo[0] = it }
    val hoja = horario.hoja ?: recuerdo[0]
    val l = letrasHorario()
    val apaisado = esApaisado()
    // Cabecera y cuerpo cambian de página con un solo reloj; editar una clase no cuenta como página nueva
    val transicion = updateTransition(hoja, label = "hoja-horario")
    // Fija mientras la revisión esté en pantalla, también mientras sale: su lista desplazable no puede
    // quedar dentro de una hoja que desplaza (un desplazamiento dentro de otro no se puede medir)
    val fija = apaisado && (transicion.currentState == HojaHorario.Confirmar || transicion.targetState == HojaHorario.Confirmar)
    val empuje: AnimatedContentTransitionScope<HojaHorario?>.() -> ContentTransform = { fundidoDeHoja() }
    // Cerrar la revisión sin guardar descarta la lectura: la semana provisional no se puede dejar a medias
    val cerrar = { if (horario.hoja == HojaHorario.Confirmar) horario.cancelarLectura() else horario.hoja = null }

    HojaInferior(
        abierta = horario.hoja != null,
        alCerrar = cerrar,
        cristal = cristal,
        // Acostado, la revisión es más ancha y es su lista la que desplaza: el botón queda fijo abajo
        desplazable = !fija,
        cabecera = {
            transicion.AnimatedContent(transitionSpec = empuje, contentKey = ::clasePagina, contentAlignment = Alignment.TopStart) { h ->
                val (etiqueta, titulo, punto) = when (h) {
                    is HojaHorario.Detalle -> {
                        val s = sesionVista(horario, h.id)
                        Triple(s?.let { etiquetaDetalle(horario, it) }.orEmpty(), s?.let { horario.nombre(it.codigo) }.orEmpty(), s?.let { horario.color(it.codigo) })
                    }
                    is HojaHorario.Editar -> Triple(
                        if (h.borrador.id == null) "NUEVA CLASE" else "EDITAR CLASE",
                        h.borrador.codigo?.let(horario::nombre) ?: "¿Qué materia?",
                        null,
                    )
                    HojaHorario.Menu -> Triple("TU HORARIO", "${clases(horario.sesiones.size)} a la semana", null)
                    HojaHorario.Borrar -> Triple("TU HORARIO", "¿Borrar todo el horario?", null)
                    // Solo el título: leído de una captura y antes de guardar es lo que ya está pasando
                    HojaHorario.Confirmar -> Triple("", "Revisa y guarda", null)
                    null -> Triple("", "", null)
                }
                // «Quedan 40 min» es un dato: va en nota, junto al rótulo «EN CURSO»
                val quedan = (h as? HojaHorario.Detalle)?.let { sesionVista(horario, it.id) }?.takeIf { horario.enCurso(it) }
                    ?.let { "Quedan ${duracion(it.fin - horario.ahora.minuto)}" }
                Column(Modifier.fillMaxWidth().padding(start = Espacio.xl, end = Espacio.xl, top = Espacio.xs)) {
                    if (etiqueta.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                        if (punto != null) Box(Modifier.size(6.dp).clip(CircleShape).drawBehind { drawRect(punto) })
                        BasicText(etiqueta, style = l.etiqueta)
                        if (quedan != null) BasicText(quedan, style = l.texto)
                    }
                    BasicText(titulo, Modifier.padding(top = if (etiqueta.isNotEmpty()) Espacio.xxs else 0.dp), style = l.tituloHoja)
                }
            }
        },
    ) {
        // El peso va en el contenedor: fija, la lista de la revisión desplaza y el botón queda abajo
        transicion.AnimatedContent(
            modifier = if (fija) Modifier.weight(1f, fill = false) else Modifier,
            transitionSpec = empuje,
            contentKey = ::clasePagina,
            contentAlignment = Alignment.TopStart,
        ) { h ->
            Column(Modifier.padding(start = Espacio.xl, end = Espacio.xl, bottom = Espacio.l)) {
                when (h) {
                    is HojaHorario.Detalle -> sesionVista(horario, h.id)?.let { Detalle(horario, it) }
                    is HojaHorario.Editar -> Editar(horario, h.borrador)
                    HojaHorario.Menu -> Menu(horario)
                    HojaHorario.Borrar -> Borrar(horario)
                    HojaHorario.Confirmar -> Confirmar(horario)
                    null -> Unit
                }
            }
        }
    }
}

/** Qué página es: la misma mientras el borrador o la clase cambian por dentro, para que editar no la anime */
private fun clasePagina(h: HojaHorario?): Any = when (h) {
    is HojaHorario.Detalle -> "detalle"
    is HojaHorario.Editar -> "editar"
    HojaHorario.Menu -> "menu"
    HojaHorario.Borrar -> "borrar"
    HojaHorario.Confirmar -> "confirmar"
    null -> "ninguna"
}

/** Patrón B: sale una hoja y entra la otra, sin desplazamiento (es el mismo sitio), y el alto se anima una sola vez */
private fun fundidoDeHoja(): ContentTransform {
    val alto = SizeTransform(clip = true) { _, _ -> Movimiento.ALTO }
    if (Movimiento.reducido) return ContentTransform(fadeIn(tween(Movimiento.CORTA)), fadeOut(tween(Movimiento.CORTA)), sizeTransform = alto)
    return ContentTransform(
        fadeIn(tween(Movimiento.CORTA, Movimiento.SALIDA_RAPIDA, Movimiento.ENTRA)),
        fadeOut(tween(Movimiento.SALIDA_RAPIDA, easing = Movimiento.SALE)),
        sizeTransform = alto,
    )
}

/** La clase de la hoja de detalle; si se acaba de quitar, la que se vio por última vez, para que baje entera */
@Composable
private fun sesionVista(horario: EstadoHorario, id: String): Sesion? {
    val visto = remember(id) { arrayOfNulls<Sesion>(1) }
    horario.sesion(id)?.let { visto[0] = it }
    return visto[0]
}

private fun etiquetaDetalle(horario: EstadoHorario, s: Sesion): String {
    if (horario.enCurso(s)) return "EN CURSO"
    return listOfNotNull(horario.materia(s.codigo)?.uc?.let { "$it UC" }, s.seccion.ifBlank { null }?.let { "SECCIÓN $it" })
        .joinToString(" · ").ifEmpty { "CLASE" }
}

/* ---- Detalle ------------------------------------------------------------ */

@Composable
private fun Detalle(horario: EstadoHorario, s: Sesion) {
    val l = letrasHorario()
    Column(Modifier.padding(top = Espacio.m)) {
        val datos = listOf(
            "Cuándo" to cuandoEs(s, horario.sesiones.ifEmpty { listOf(s) }, Tema.hora24),
            "Aula" to s.aula,
            "Sección" to s.seccion,
            "Profesor" to s.profesor,
        )
        for ((nombre, valor) in datos) {
            // Lo que falta se ofrece: tocarlo lleva a editar la clase
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(if (valor.isBlank()) Modifier.pulsable({ horario.editar(s) }, escala = 0.99f) else Modifier)
                    .drawBehind { drawRect(LINEA, size = Size(size.width, 1.dp.toPx())) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(nombre, Modifier.width(ANCHO_NOMBRE), style = l.texto)
                BasicText(
                    valor.ifBlank { "Añadir" },
                    Modifier.weight(1f),
                    style = l.texto.copy(color = if (valor.isBlank()) Paleta.tenue else Paleta.tinta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    BotonLleno("Editar", activo = true, Modifier.padding(top = Espacio.m)) { horario.editar(s) }
    Box(
        Modifier.padding(top = Espacio.xxs).fillMaxWidth().height(44.dp).pulsable({ horario.quitar(s.id) }, escala = 0.97f),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("Quitar del horario", style = l.accion.copy(color = Paleta.rojo))
    }
}

/* ---- Nueva o editar ------------------------------------------------------ */

@Composable
internal fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    BasicText(texto, modifier, style = letrasHorario().etiqueta)
}
