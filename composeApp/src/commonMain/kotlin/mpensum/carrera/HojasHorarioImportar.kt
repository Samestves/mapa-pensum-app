package mpensum.carrera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.MateriaLeida
import mpensum.logica.Sesion
import mpensum.logica.choquesAlImportar
import mpensum.logica.clases
import mpensum.logica.cuandoEs
import mpensum.logica.tramoCorto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema

/* El menú del horario, borrarlo y confirmar lo que se leyó de una foto */

/**
 * Lo demás del horario. Compartir la imagen, descargarlo para el calendario y
 * volver a subir la captura llegan con la lectura de capturas; hoy solo se
 * puede borrar.
 */
@Composable
internal fun Menu(horario: EstadoHorario) {
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
internal fun Borrar(horario: EstadoHorario) {
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
internal fun Confirmar(horario: EstadoHorario) {
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
                    "Choca con ${horario.nombre(a.codigo)}, ${DIAS_CORTOS[b.dia].lowercase()} ${tramoCorto(b.inicio, minOf(a.fin, b.fin), Tema.hora24).replace(" – ", "–")}",
                    Modifier.padding(top = Espacio.hilo),
                    style = l.dato.copy(color = Paleta.rojo),
                )
            } else {
                val aula = primera.aula.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                BasicText(cuandoEs(primera, m.sesiones, Tema.hora24) + aula, Modifier.padding(top = Espacio.hilo), style = l.dato, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        BasicText(clases(m.sesiones.size), style = l.dato.copy(color = Paleta.tenue))
    }
}

/** Una línea fina arriba de la fila, como separador */
private fun Modifier.lineaArriba(): Modifier = drawBehind {
    drawRect(LINEA, Offset.Zero, Size(size.width, 1.dp.toPx()))
}
