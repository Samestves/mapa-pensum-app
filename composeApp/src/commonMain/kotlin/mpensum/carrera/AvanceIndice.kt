package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.CifraConUnidad
import mpensum.componentes.Lucide
import mpensum.componentes.fundidoAbajo
import mpensum.componentes.margenPantalla
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Periodo
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import kotlin.math.roundToInt

/*
 * La hoja del progreso con el teléfono acostado: índice y detalle a pantalla
 * completa (docs/maquetas/hojas-acostadas.html, opción C), como los Ajustes
 * del iPad.
 *
 *   - A la izquierda, el índice: el aspa con «Tu avance», tu cifra y tu fecha,
 *     y las preguntas en una tarjeta. La elegida lleva una pastilla por dentro.
 *   - A la derecha, la respuesta de la elegida, con su cabecera fija y su
 *     cuerpo con scroll propio. Tocar otra pregunta cambia solo la derecha.
 *
 * Las medidas son las de la hoja de pie: márgenes de pantalla a los lados, 16
 * arriba, tarjetas de radio 24 y la pastilla elegida de radio 20 a 4 dp del
 * borde (concéntrica). Las dos cabeceras miden 36 y sus títulos van a la misma
 * altura; entre las columnas, 24 dp con un hilo en medio.
 */

/** Lo que baja el cuerpo de una pantalla interna bajo su cabecera, dentro del panel */
internal val ARRIBA_CUERPO_PANEL = Espacio.l

/** De lo alto de la hoja a las cabeceras de las dos columnas: con lo que ya baja la hoja, 16 dp */
private val ARRIBA = Espacio.m - RESPIRO_COMPLETA

/** Las cabeceras: el alto del botón redondo, con el título centrado */
/** El panel del índice se funde: el nuevo entra tras un respiro y el viejo sale antes */
private const val ENTRA_PANEL_MS = 220
private const val ESPERA_PANEL_MS = 60
private const val SALE_PANEL_MS = 120
private val ALTO_CABECERA = 36.dp

/** Lo que separa el índice del detalle; el hilo va en medio */
private val SEPARA = Espacio.xl

/** La fila del índice y lo que la pastilla elegida se aparta del borde de la tarjeta */
private val ALTO_FILA_INDICE = 44.dp
private val RESQUICIO = 4.dp

/** La pastilla elegida, concéntrica con la tarjeta: 24 − 4 */
private val FORMA_ELEGIDA = RoundedCornerShape(Radio.Grande - RESQUICIO)

/** Dentro de la fila: el margen hasta la pastilla del icono, que sumado al resquicio da los 16 de siempre */
private val DENTRO_FILA = Espacio.s

/** El índice: un tercio del ancho, entre 288 y 340 dp */
private const val FRACCION_INDICE = 0.33f
private val INDICE_MIN = 288.dp
private val INDICE_MAX = 340.dp

@Composable
internal fun IndiceYDetalle(
    estado: EstadoCarrera,
    hoy: Periodo,
    detalle: DetalleAvance?,
    alElegir: (DetalleAvance) -> Unit,
    alCerrar: () -> Unit,
    alVerEnMapa: (List<String>) -> Unit,
    alAbrirCasilla: (String) -> Unit,
) {
    val g = estado.graduacion
    val resumen = estado.resumen
    val filas = remember(resumen) { resumen.preguntas + resumen.electivas }
    // Siempre hay una elegida: la que se abrió (también de pie, al girar) o la primera
    val elegido = detalle?.takeIf { d -> filas.any { it.detalle == d } } ?: filas.firstOrNull()?.detalle
    val margen = margenPantalla()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val anchoIndice = (maxWidth * FRACCION_INDICE).coerceIn(INDICE_MIN, INDICE_MAX)
        Row(Modifier.fillMaxSize()) {
            val desplazo = rememberScrollState()
            Column(
                Modifier
                    .width(margen + anchoIndice)
                    .fillMaxHeight()
                    .fundidoAbajo(desplazo)
                    .verticalScroll(desplazo)
                    .padding(start = margen, top = ARRIBA, bottom = Espacio.xl),
            ) {
                Row(Modifier.height(ALTO_CABECERA), verticalAlignment = Alignment.CenterVertically) {
                    BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
                    BasicText("Tu avance", Modifier.padding(start = Espacio.s), style = estiloTitulo(), maxLines = 1)
                }
                ResumenIndice(estado, g, Modifier.padding(top = Espacio.m))
                if (filas.isNotEmpty()) TarjetaIndice(filas, elegido, alElegir, Modifier.padding(top = Espacio.s))
                Spacer(Modifier.height(Espacio.xl))
                BotonReinicio(estado)
            }
            // El hilo entre las dos columnas, a la altura del contenido
            Box(Modifier.width(SEPARA).fillMaxHeight().padding(top = ARRIBA, bottom = Espacio.m), contentAlignment = Alignment.Center) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(Paleta.tinta.copy(alpha = Velo.hilo)))
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(end = margen)) {
                if (elegido == null) {
                    BasicText("Completaste la carrera.", Modifier.align(Alignment.Center), style = estiloCuerpo())
                } else {
                    AnimatedContent(elegido, transitionSpec = { fadeIn(tween(ENTRA_PANEL_MS, ESPERA_PANEL_MS)) togetherWith fadeOut(tween(SALE_PANEL_MS)) }, label = "panel") { d ->
                        Column(Modifier.fillMaxSize()) {
                            CabeceraPanel(cabeceraDe(d, estado, g, hoy), codigosDelMapa(d, g)?.let { codigos -> { alVerEnMapa(codigos) } })
                            Box(Modifier.weight(1f)) { ContenidoDetalle(d, estado, hoy, alVerEnMapa, alAbrirCasilla) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * La cabecera del panel: la pastilla y el título a la altura del «Tu avance»
 * del índice; a la derecha, en la misma línea, «Ver en el mapa» si la
 * pantalla lo tiene; y el subtítulo, si lo hay, alineado con el título.
 */
@Composable
private fun CabeceraPanel(d: DatosCabecera, alVerEnMapa: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(top = ARRIBA)) {
        Row(Modifier.fillMaxWidth().height(ALTO_CABECERA), verticalAlignment = Alignment.CenterVertically) {
            Pastilla(d.icono, d.azul)
            BasicText(d.titulo, Modifier.weight(1f).padding(start = Espacio.s, end = Espacio.s), style = estiloTitulo(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (alVerEnMapa != null) BotonMapa(alVerEnMapa)
        }
        if (d.subtitulo != null) BasicText(d.subtitulo, Modifier.padding(start = 32.dp + Espacio.s, top = Espacio.hilo), style = estiloCuerpo(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tu cifra con lo aprobado debajo y, enfrente, tu fecha: la tarjeta de pie sin la barra ni la frase, que no caben */
@Composable
private fun ResumenIndice(estado: EstadoCarrera, g: EstimacionGraduacion, modifier: Modifier) {
    Column(modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        CifraYFecha(estado, g, Espacio.s) { CifraConUnidad("${estado.progreso.avance.roundToInt()}", "%", 40.sp, Modifier.recorteCap(40.sp)) }
    }
}

/** Las preguntas y las electivas en una sola tarjeta; la elegida, con su pastilla por dentro */
@Composable
private fun TarjetaIndice(filas: List<Pregunta>, elegido: DetalleAvance?, alElegir: (DetalleAvance) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().superficie().padding(RESQUICIO)) {
        filas.forEachIndexed { k, f ->
            val esta = f.detalle == elegido
            // El hilo no toca a la elegida: ni encima ni debajo de su pastilla
            val hilo = k > 0 && !esta && filas[k - 1].detalle != elegido
            FilaIndice(f, esta, hilo) { alElegir(f.detalle) }
        }
    }
}

@Composable
private fun FilaIndice(f: Pregunta, elegida: Boolean, hilo: Boolean, alTocar: () -> Unit) {
    val colorHilo = Paleta.tinta.copy(alpha = Velo.hilo)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA_INDICE)
            .drawBehind {
                if (hilo) {
                    val inicio = (DENTRO_FILA + 32.dp + Espacio.s).toPx()
                    drawRect(colorHilo, Offset(inicio, 0f), Size(size.width - inicio - DENTRO_FILA.toPx(), 1.dp.toPx()))
                }
            }
            .clip(FORMA_ELEGIDA)
            .then(if (elegida) Modifier.background(Paleta.tinta.copy(alpha = Velo.pastilla)) else Modifier)
            .clickable(role = Role.Tab, onClick = alTocar)
            .semantics { selected = elegida }
            .padding(horizontal = DENTRO_FILA),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Pastilla(f.icono.trazos(), f.azul)
        BasicText(f.titulo, Modifier.weight(1f), style = estiloFila(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (f.valor != null) CifraFila(f.valor, f.unidad)
    }
}
