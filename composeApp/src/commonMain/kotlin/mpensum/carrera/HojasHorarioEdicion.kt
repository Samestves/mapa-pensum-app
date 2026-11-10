package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mpensum.componentes.desbordar
import mpensum.componentes.pulsable
import mpensum.componentes.trazarCruz
import mpensum.logica.ABRE
import mpensum.logica.CIERRA
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.MIN_DURACION
import mpensum.logica.PASO_HORA
import mpensum.logica.Sesion
import mpensum.logica.choqueCon
import mpensum.logica.enDoceHoras
import mpensum.logica.materiasParaElegir
import mpensum.logica.tramoCorto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema

/* Editar una clase del horario: su ficha, las horas, el texto y los botones */

@Composable
internal fun Editar(horario: EstadoHorario, b: Borrador) {
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
        choque?.let { "Choca con ${horario.nombre(it.codigo)}, ${tramoCorto(it.inicio, it.fin, Tema.hora24)}" }.orEmpty(),
        Modifier.padding(top = Espacio.xs).height(20.dp),
        style = l.parrafo.copy(color = Paleta.rojo),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    BotonLleno(if (b.id == null) "Añadir clase" else "Guardar", activo = b.codigo != null, Modifier.padding(top = Espacio.xs)) { horario.guardar(b) }
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
        BasicText(enDoceHoras(minuto, Tema.hora24), Modifier.weight(1f), style = l.horaGrande)
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
internal fun BotonLleno(texto: String, activo: Boolean, modifier: Modifier = Modifier, alPulsar: () -> Unit) {
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
