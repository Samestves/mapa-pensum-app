package mpensum.carrera

import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import mpensum.componentes.desbordar
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.componentes.trazarCruz
import mpensum.logica.ABRE
import mpensum.logica.CIERRA
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.MIN_DURACION
import mpensum.logica.MateriaLeida
import mpensum.logica.PASO_HORA
import mpensum.logica.Sesion
import mpensum.logica.choqueCon
import mpensum.logica.choquesAlImportar
import mpensum.logica.clases
import mpensum.logica.cuandoEs
import mpensum.logica.duracion
import mpensum.logica.enDoceHoras
import mpensum.logica.materiasParaElegir
import mpensum.logica.tramoCorto
import mpensum.tema.Espacio
import mpensum.tema.Movimiento
import mpensum.tema.Paleta

/*
 * Las hojas del horario, de cristal y flotando sobre la barra: el detalle de
 * una clase, la de crear o editar una, el «⋯» y la pregunta antes de borrarlo
 * todo. Se montan en el interior después de la barra, como las demás hojas.
 */

private val ANCHO_NOMBRE = 96.dp
private val LINEA get() = Paleta.tinta.copy(alpha = 0.07f)

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
            "Cuándo" to cuandoEs(s, horario.sesiones.ifEmpty { listOf(s) }),
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
private fun Editar(horario: EstadoHorario, b: Borrador) {
    val l = letrasHorario()
    fun cambiar(nuevo: Borrador) {
        horario.hoja = HojaHorario.Editar(nuevo)
    }

    Rotulo("MATERIA", Modifier.padding(top = Espacio.l))
    val enHorario = remember(horario.sesiones) { horario.sesiones.map { it.codigo }.toSet() }
    val materias = remember(enHorario, horario.carrera.situaciones) {
        materiasParaElegir(horario.carrera.pensum.todas, horario.carrera.situaciones, enHorario)
    }
    // Al editar, la fila arranca con la materia de la clase a la vista
    val fila = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, materias.indexOfFirst { it.codigo == b.codigo }))
    LazyRow(
        Modifier.padding(top = Espacio.xs).desbordar(24.dp),
        state = fila,
        contentPadding = PaddingValues(horizontal = Espacio.xl),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        items(materias, key = { it.codigo }) { m ->
            val activa = b.codigo == m.codigo
            Ficha(activa, alPulsar = { cambiar(b.copy(codigo = m.codigo)) }, rol = Role.RadioButton) {
                Row(Modifier.padding(start = Espacio.s, end = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    val color = horario.color(m.codigo)
                    Box(Modifier.size(6.dp).clip(CircleShape).drawBehind { drawRect(color) })
                    BasicText(m.nombre, style = l.texto.copy(color = if (activa) Paleta.tinta else Paleta.suave), softWrap = false)
                }
            }
        }
    }

    Rotulo("DÍA", Modifier.padding(top = Espacio.l))
    Row(Modifier.padding(top = Espacio.xs).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
        DIAS_CORTOS.forEachIndexed { d, corto ->
            val activo = b.dia == d
            Ficha(activo, alPulsar = { cambiar(b.copy(dia = d)) }, rol = Role.RadioButton, alto = 40, radio = 14, apagada = 0.04f, modifier = Modifier.weight(1f)) {
                BasicText(corto, Modifier.padding(start = 1.5.dp), style = l.etiqueta.copy(color = if (activo) Paleta.tinta else Paleta.tenue))
            }
        }
    }

    Rotulo("HORA", Modifier.padding(top = Espacio.l))
    val dura = b.fin - b.inicio
    // Mover el inicio corre la clase entera, como en Calendario; el fin solo la alarga o la acorta
    FilaHora(
        "Empieza", b.inicio,
        menos = { cambiar(b.copy(inicio = maxOf(ABRE, b.inicio - PASO_HORA), fin = maxOf(ABRE, b.inicio - PASO_HORA) + dura)) },
        mas = { cambiar(b.copy(inicio = minOf(CIERRA - dura, b.inicio + PASO_HORA), fin = minOf(CIERRA - dura, b.inicio + PASO_HORA) + dura)) },
    )
    FilaHora(
        "Termina", b.fin,
        menos = { cambiar(b.copy(fin = maxOf(b.inicio + MIN_DURACION, b.fin - PASO_HORA))) },
        mas = { cambiar(b.copy(fin = minOf(CIERRA, b.fin + PASO_HORA))) },
    )
    FilaTexto("Aula", b.aula) { cambiar(b.copy(aula = it)) }
    FilaTexto("Sección", b.seccion) { cambiar(b.copy(seccion = it)) }
    FilaTexto("Profesor", b.profesor) { cambiar(b.copy(profesor = it)) }

    // El aviso siempre ocupa su renglón: aparecer no empuja el botón
    val choque = b.codigo?.let { choqueCon(horario.sesiones, Sesion(b.id ?: "", it, b.dia, b.inicio, b.fin)) }
    BasicText(
        choque?.let { "Choca con ${horario.nombre(it.codigo)}, ${tramoCorto(it.inicio, it.fin)}" }.orEmpty(),
        Modifier.padding(top = Espacio.xs).height(20.dp),
        style = l.parrafo.copy(color = Paleta.rojo),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    BotonLleno(if (b.id == null) "Añadir clase" else "Guardar", activo = b.codigo != null, Modifier.padding(top = Espacio.xs)) { horario.guardar(b) }
}

@Composable
private fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    BasicText(texto, modifier, style = letrasHorario().etiqueta)
}

/** Una opción redonda: la elegida, encendida y con filo; las demás, apenas un velo */
@Composable
private fun Ficha(
    activa: Boolean,
    alPulsar: () -> Unit,
    rol: Role,
    modifier: Modifier = Modifier,
    alto: Int = 36,
    radio: Int = 18,
    apagada: Float = 0.05f,
    contenido: @Composable () -> Unit,
) {
    Box(
        modifier
            .height(alto.dp)
            .pulsable(alPulsar, rol = rol, escala = 0.95f)
            .drawBehind {
                val r = CornerRadius(radio.dp.toPx())
                val g = 1.dp.toPx()
                drawRoundRect(Paleta.tinta.copy(alpha = if (activa) 0.14f else apagada), cornerRadius = r)
                val borde = if (activa) 0.3f else if (apagada >= 0.05f) 0.06f else 0f
                if (borde > 0f) {
                    drawRoundRect(Paleta.tinta.copy(alpha = borde), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2), style = Stroke(g))
                }
            },
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

@Composable
private fun FilaHora(nombre: String, minuto: Int, menos: () -> Unit, mas: () -> Unit) {
    val l = letrasHorario()
    Row(Modifier.fillMaxWidth().height(48.dp).lineaAbajo(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(nombre, Modifier.width(ANCHO_NOMBRE), style = l.texto)
        BasicText(enDoceHoras(minuto), Modifier.weight(1f), style = l.horaGrande)
        BotonPaso(alPulsar = menos, vertical = false)
        BotonPaso(alPulsar = mas, vertical = true)
    }
}

/** − o +: un círculo de 32 dentro de un blanco de 44 para el pulgar */
@Composable
private fun BotonPaso(alPulsar: () -> Unit, vertical: Boolean) {
    Box(Modifier.size(44.dp).pulsable(alPulsar, escala = 0.9f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(32.dp).clip(CircleShape).drawBehind { drawRect(Paleta.tinta.copy(alpha = 0.07f)) }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(14.dp)) { trazarCruz(Paleta.tinta, vertical = vertical) }
        }
    }
}

@Composable
private fun FilaTexto(nombre: String, valor: String, alCambiar: (String) -> Unit) {
    val l = letrasHorario()
    Row(Modifier.fillMaxWidth().height(48.dp).lineaAbajo(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(nombre, Modifier.width(ANCHO_NOMBRE), style = l.texto)
        BasicTextField(
            valor,
            alCambiar,
            Modifier.weight(1f),
            textStyle = l.texto.copy(color = Paleta.tinta),
            singleLine = true,
            cursorBrush = SolidColor(Paleta.tinta),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            decorationBox = { campo ->
                Box {
                    if (valor.isEmpty()) BasicText("Añadir", style = l.texto)
                    campo()
                }
            },
        )
    }
}

private fun Modifier.lineaAbajo(): Modifier = drawBehind {
    val g = 1.dp.toPx()
    drawRect(LINEA, Offset(0f, size.height - g), Size(size.width, g))
}

/** El único botón lleno de la hoja; apagado mientras falte lo necesario */
@Composable
private fun BotonLleno(texto: String, activo: Boolean, modifier: Modifier = Modifier, alPulsar: () -> Unit) {
    val l = letrasHorario()
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (activo) Modifier.pulsable(alPulsar, atenuar = 0.88f) else Modifier)
            .clip(RoundedCornerShape(26.dp))
            .drawBehind { drawRect(if (activo) Paleta.tinta else Paleta.tinta.copy(alpha = 0.08f)) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(texto, style = l.boton.copy(color = if (activo) Paleta.lienzo else Paleta.apagada))
    }
}

/* ---- Más y borrar ---------------------------------------------------------- */

/**
 * Lo demás del horario. Compartir la imagen, descargarlo para el calendario y
 * volver a subir la captura llegan con la lectura de capturas; hoy solo se
 * puede borrar.
 */
@Composable
private fun Menu(horario: EstadoHorario) {
    val l = letrasHorario()
    Column(Modifier.padding(top = Espacio.s)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pulsable({ horario.hoja = HojaHorario.Borrar }, escala = 0.98f)
                .drawBehind { drawRect(LINEA, size = Size(size.width, 1.dp.toPx())) },
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText("Borrar horario", style = l.boton.copy(fontWeight = FontWeight.Normal, color = Paleta.rojo))
        }
    }
}

/**
 * Antes de borrarlo todo se pregunta, como en la web (ConfirmarBorrado): detrás
 * puede haber una tarde de armarlo. Lo que destruye no va lleno; lo fácil es
 * cancelar.
 */
@Composable
private fun Borrar(horario: EstadoHorario) {
    val l = letrasHorario()
    val cuantas = remember { horario.sesiones.size }
    BasicText(
        "${if (cuantas == 1) "Se borra tu clase" else "Se borran tus $cuantas clases"}. No se puede deshacer.",
        Modifier.padding(top = Espacio.xs),
        style = l.parrafo,
    )
    Box(
        Modifier
            .padding(top = Espacio.l)
            .fillMaxWidth()
            .height(52.dp)
            .pulsable({ horario.vaciar() }, escala = 0.98f)
            .clip(RoundedCornerShape(26.dp))
            .drawBehind { drawRect(Paleta.rojo.copy(alpha = 0.14f)) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText("Borrar todo", style = l.boton.copy(color = Paleta.rojo))
    }
    Box(
        Modifier.padding(top = Espacio.xxs).fillMaxWidth().height(44.dp).pulsable({ horario.hoja = null }, escala = 0.97f),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("Cancelar", style = l.accion.copy(color = Paleta.suave))
    }
}

/* ---- Antes de guardar ------------------------------------------------------ */

/**
 * La revisión de lo leído: una fila por materia con sus días y su aula, y la
 * que se pisa con otra lo dice en rojo, en su propia fila. El único botón
 * guarda todas las clases.
 */
@Composable
private fun Confirmar(horario: EstadoHorario) {
    val lectura = horario.lectura ?: return
    val nuevas = horario.nuevasDeLaLectura
    // Lo repetido no se guarda; lo que choca con tu horario de antes se avisa igual que lo que choca entre sí
    val repetidas = lectura.sesiones.size - nuevas.size
    val pisan = remember(nuevas, horario.sesiones) { choquesAlImportar(nuevas, horario.sesiones) }
    // En horizontal la hoja es ancha y baja: las materias van de dos en dos para que el botón quede a la vista
    val apaisado = esApaisado()
    val columnas = if (apaisado) 2 else 1
    Column(Modifier.fillMaxWidth()) {
        // Acostado la hoja no desplaza: la lista sí, y el botón queda fijo debajo
        Column(if (apaisado) Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()) else Modifier) {
            for ((i, fila) in lectura.leidas.chunked(columnas).withIndex()) {
                Row(Modifier.fillMaxWidth().then(if (i > 0) Modifier.lineaArriba() else Modifier), horizontalArrangement = Arrangement.spacedBy(Espacio.xl)) {
                    for (m in fila) key(m.codigo) {
                        FilaLeida(horario, m, pisan, Modifier.weight(1f))
                    }
                    // Una fila impar deja su hueco: la última materia no se estira a todo el ancho
                    repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (repetidas > 0) BasicText("Ya tenías ${clases(repetidas)}", Modifier.padding(top = Espacio.s), style = letrasHorario().dato)
        BotonLleno("Guardar ${clases(nuevas.size)}", nuevas.isNotEmpty(), Modifier.padding(top = Espacio.m), horario::guardarLectura)
    }
}

/** Una materia leída: su marca de color, el nombre y, debajo, cuándo y dónde o con qué choca */
@Composable
private fun FilaLeida(horario: EstadoHorario, m: MateriaLeida, pisan: List<Pair<Sesion, Sesion>>, modifier: Modifier) {
    val l = letrasHorario()
    val primera = m.sesiones.first()
    // La clase de esta materia que se mete sobre otra, con lo que pisa
    val choque = pisan.firstOrNull { (_, b) -> b.codigo == m.codigo }
    Row(modifier.heightIn(min = 52.dp).padding(vertical = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        val color = horario.color(m.codigo)
        Box(Modifier.width(3.dp).height(36.dp).drawBehind { drawRoundRect(color, cornerRadius = CornerRadius(1.5.dp.toPx())) })
        Column(Modifier.weight(1f).padding(start = Espacio.s, end = Espacio.s)) {
            BasicText(horario.nombre(m.codigo), style = l.nombreFila, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (choque != null) {
                val (a, b) = choque
                BasicText(
                    "Choca con ${horario.nombre(a.codigo)}, ${DIAS_CORTOS[b.dia].lowercase()} ${tramoCorto(b.inicio, minOf(a.fin, b.fin)).replace(" – ", "–")}",
                    Modifier.padding(top = Espacio.hilo),
                    style = l.dato.copy(color = Paleta.rojo),
                )
            } else {
                val aula = primera.aula.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                BasicText(cuandoEs(primera, m.sesiones) + aula, Modifier.padding(top = Espacio.hilo), style = l.dato, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        BasicText(clases(m.sesiones.size), style = l.dato.copy(color = Paleta.tenue))
    }
}

/** Una línea fina arriba de la fila, como separador */
private fun Modifier.lineaArriba(): Modifier = drawBehind {
    drawRect(LINEA, Offset.Zero, Size(size.width, 1.dp.toPx()))
}
