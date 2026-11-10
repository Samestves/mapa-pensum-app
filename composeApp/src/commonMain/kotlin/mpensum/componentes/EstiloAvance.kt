package mpensum.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.tema.Movimiento
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.Radio
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * El sistema de la hoja del progreso refinada (docs/refinado-progreso.md):
 * las opacidades de la tinta por rol, los estilos de texto por rol y las
 * piezas que comparten la hoja y sus pantallas internas (pastillas, filas
 * agrupadas, rótulos de sección, comparaciones y botones).
 */

/** Las opacidades de la tinta por rol: la primera en oscuro, la segunda en los claros */
internal object Velo {
    val superficie get() = porTema(0.05f, 0.045f)
    val hilo get() = porTema(0.09f, 0.12f)
    val pastilla get() = porTema(0.08f, 0.07f)
    val pendiente get() = porTema(0.15f, 0.13f)
    val pastillaAzul get() = porTema(0.16f, 0.10f)
}

/** El alto de las mayúsculas de Inter, en em: para recortar una cifra a su tope y su línea base */
private const val ALTO_MAYUSCULA = 0.727f

/** La fila agrupada: 64 dp de alto (48 la corta), 16 a los lados y 12 entre la pastilla y el texto */
private val ALTO_FILA = 64.dp
private val ALTO_FILA_CORTA = 48.dp
private val LADO_PASTILLA = 32.dp

/** Donde empieza el hilo entre filas: alineado con el texto (16 + 32 + 12) */
private val INICIO_HILO = 60.dp

// ─────────────────────────────────── Texto por rol ──────────────────────────────────

/** Rótulo: Inter 500, 11/16, +8 %, en mayúsculas (el texto ya viene en mayúsculas) */
@Composable
internal fun estiloRotulo(color: Color = Paleta.tenue) =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em, color = color, fontFeatureSettings = "tnum")

/** Título: Jost 300, 28/32, −0,5 % */
@Composable
internal fun estiloTitulo() =
    TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.005).em, color = Paleta.tinta)

/** Fila: Inter 500, 15/20, −0,9 % */
@Composable
internal fun estiloFila(color: Color = Paleta.tinta, peso: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em, color = color, fontFeatureSettings = "tnum")

/** Cuerpo: Inter 400, 15/20, −0,9 %, suave */
@Composable
internal fun estiloCuerpo(color: Color = Paleta.suave) = estiloFila(color, FontWeight.Normal)

/** Nota: Inter 400, 13/16, −0,3 %, cifras tabulares, tenue */
@Composable
internal fun estiloNota(color: Color = Paleta.tenue, peso: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = (-0.003).em, color = color, fontFeatureSettings = "tnum")

/** Cifra media (24/28, 300, −1,5 %): las fechas */
@Composable
internal fun estiloFecha(color: Color = Paleta.tinta) =
    estiloCifra(24.sp, color, FontWeight.Light, 28.sp).copy(letterSpacing = (-0.015).em)

/**
 * Recorta un texto a sus mayúsculas: el tope en la altura de las mayúsculas y
 * el pie en la línea base, como `text-box: trim-both cap alphabetic`. Así dos
 * cifras de tamaños distintos se alinean por arriba o por abajo de verdad.
 */
internal fun Modifier.recorteCap(tamaño: TextUnit): Modifier = layout { medible, limites ->
    val p = medible.measure(limites.copy(minHeight = 0))
    val base = p[FirstBaseline]
    if (base == androidx.compose.ui.layout.AlignmentLine.Unspecified) return@layout layout(p.width, p.height) { p.place(0, 0) }
    val cap = (tamaño.toPx() * ALTO_MAYUSCULA).roundToInt()
    layout(p.width, cap) { p.place(0, cap - base) }
}

/** El alto de las mayúsculas de una cifra de ese tamaño, en px */
internal fun altoMayuscula(tamañoPx: Float) = tamañoPx * ALTO_MAYUSCULA

// ─────────────────────────────────── Superficies ──────────────────────────────────

/** La tarjeta de la hoja: radio 24 y la tinta al 4,5 % (5 % en oscuro), sin filo */
internal fun Modifier.superficie(radio: Dp = Radio.Grande): Modifier =
    this.clip(RoundedCornerShape(radio)).background(Paleta.tinta.copy(alpha = Velo.superficie))

/** La pastilla de icono: 32 dp, radio 8, el icono Lucide de 18 dp. La azul es la de «Tu próximo semestre» */
@Composable
internal fun Pastilla(trazos: List<Path>, azul: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier.size(LADO_PASTILLA).clip(RoundedCornerShape(8.dp))
            .background(if (azul) Paleta.azul.copy(alpha = Velo.pastillaAzul) else Paleta.tinta.copy(alpha = Velo.pastilla)),
        contentAlignment = Alignment.Center,
    ) { IconoLucide(trazos, if (azul) Paleta.azul else Paleta.tinta) }
}

/** La pastilla vacía de una casilla sin elegir: contorno de 1,5 dp y el «+» tenue */
@Composable
internal fun PastillaVacia(trazos: List<Path> = IconosHoja.mas) {
    Box(
        Modifier.size(LADO_PASTILLA).border(1.5.dp, Paleta.tinta.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { IconoLucide(trazos, Paleta.tenue) }
}

/** Un icono suelto en el sitio de la pastilla, con el estado en su color */
@Composable
internal fun IconoDeFila(trazos: List<Path>, color: Color) {
    Box(Modifier.size(LADO_PASTILLA), contentAlignment = Alignment.Center) { IconoLucide(trazos, color) }
}

/** Un icono Lucide con el trazo visual de 1,5 dp a cualquier tamaño */
@Composable
internal fun IconoLucide(trazos: List<Path>, color: Color, tam: Dp = 18.dp, modifier: Modifier = Modifier) {
    IconoTrazo(trazos, color, tam, 1.5f * 24f / tam.value, modifier)
}

/** El chevron de 16 dp, apagado */
@Composable
internal fun Chevron(trazos: List<Path> = Lucide.siguiente, modifier: Modifier = Modifier) {
    IconoLucide(trazos, Paleta.apagada, 16.dp, modifier)
}

/** El círculo de atrás y de cerrar: 36 dp dibujados dentro de una zona de 44, el toque mínimo, y con su nombre para el lector de pantalla */
@Composable
internal fun BotonRedondo(trazos: List<Path>, descripcion: String, alTocar: () -> Unit) {
    Box(
        Modifier.size(Alto.toque).clip(CircleShape).clickable(role = Role.Button, onClick = alTocar).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = Velo.pastilla)), contentAlignment = Alignment.Center) {
            IconoLucide(trazos, Paleta.suave)
        }
    }
}

/**
 * El botón de tinta a lo ancho: 52 dp, radio 26. Con `nota` lleva una segunda
 * línea pequeña («Se te abre 1»); con `superficie` no es el lleno sino el
 * de la tinta al 8 %, el que acompaña al paso siguiente.
 */
@Composable
internal fun BotonTinta(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    nota: String? = null,
    colorNota: Color? = null,
    superficie: Boolean = false,
    /** La burbuja acostada usa botones de 48 con radio 16; la ficha de pie, los de 52 con radio 26 */
    alto: Dp = 52.dp,
    radio: Dp = Radio.Barra,
    /** Una flecha tras el verbo, a 8 dp: la acción que lleva a otra pantalla */
    flecha: Boolean = false,
) {
    val lleno = !superficie
    Column(
        modifier.fillMaxWidth().heightIn(min = alto).clip(RoundedCornerShape(radio))
            .background(if (lleno) Paleta.tinta else Paleta.tinta.copy(alpha = Velo.pastilla))
            .clickable(role = Role.Button, onClick = alTocar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val centrado = TextAlign.Center
        // Un botón dice su verbo en una línea: si no cabe (la burbuja acostada, 320 dp), la letra baja hasta 13 antes que partirse
        val letra = estiloFila(if (lleno) Paleta.panel else Paleta.tinta).copy(textAlign = centrado)
        val verbo: @Composable (Modifier) -> Unit = { m ->
            BasicText(
                texto, m.padding(horizontal = Espacio.xs, vertical = if (nota == null) Espacio.xs else 0.dp),
                style = letra, maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 15.sp, stepSize = 0.5.sp),
            )
        }
        if (flecha) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            verbo(Modifier.weight(1f, fill = false))
            IconoLucide(Lucide.flecha, letra.color, 18.dp)
        } else verbo(Modifier)
        if (nota != null) BasicText(nota, Modifier.padding(horizontal = Espacio.xs), style = estiloNota(colorNota ?: if (lleno) Paleta.panel.copy(alpha = 0.6f) else Paleta.tenue).copy(textAlign = centrado))
    }
}

/** El botón de tinta a lo ancho: 52 dp, radio 26 */
@Composable
internal fun BotonTinta(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(Radio.Barra)).background(Paleta.tinta)
            .clickable(role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { BasicText(texto, Modifier.padding(horizontal = Espacio.m, vertical = Espacio.xs), style = estiloFila(Paleta.panel).copy(textAlign = TextAlign.Center)) }
}

// ─────────────────────────────────── Filas agrupadas ──────────────────────────────────

/** Un grupo de filas: la tarjeta de radio 24 */
@Composable
internal fun TarjetaLista(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().superficie(), content = contenido)
}

/**
 * Una fila agrupada (como Ajustes de iPhone): la pastilla, el título con su
 * nota debajo, un valor opcional y el chevron. Lleva arriba el hilo que la
 * separa de la anterior, desde donde empieza el texto.
 */
@Composable
internal fun FilaLista(
    titulo: String,
    nota: AnnotatedString?,
    primera: Boolean,
    icono: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    estiloTitulo: TextStyle = estiloFila(),
    colorNota: Color = Paleta.tenue,
    pesoNota: FontWeight = FontWeight.Normal,
    corta: Boolean = false,
    chevron: List<Path>? = null,
    giroChevron: () -> Float = { 0f },
    alTocar: (() -> Unit)? = null,
    valor: (@Composable RowScope.() -> Unit)? = null,
) {
    val hilo = Paleta.tinta.copy(alpha = Velo.hilo)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (alTocar != null) Modifier.pulsable(alTocar, escala = 0.985f).semantics(mergeDescendants = true) {} else Modifier)
            .heightIn(min = if (corta) ALTO_FILA_CORTA else ALTO_FILA)
            .drawBehind { if (!primera) drawRect(hilo, Offset(INICIO_HILO.toPx(), 0f), Size(size.width - INICIO_HILO.toPx(), 1.dp.toPx())) }
            .padding(horizontal = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icono()
        Column(Modifier.weight(1f).padding(start = Espacio.s, end = Espacio.s, top = Espacio.xs, bottom = Espacio.xs)) {
            BasicText(titulo, style = estiloTitulo, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (nota != null) BasicText(nota, style = estiloNota(colorNota, pesoNota), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        valor?.invoke(this)
        if (chevron != null) Chevron(chevron, Modifier.padding(start = if (valor != null) Espacio.xs else 0.dp).graphicsLayer { rotationZ = giroChevron() })
    }
}

/** El texto de una nota sin adornos */
internal fun nota(texto: String) = AnnotatedString(texto)

/** La cifra de fila: Inter 300, 20/24, −1 %, con su unidad de 11 sp en la línea base */
@Composable
internal fun CifraFila(valor: String, unidad: String? = null, color: Color = Paleta.tinta) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicText(
            valor,
            Modifier.alignByBaseline(),
            style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em, color = color, fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
        if (unidad != null) {
            BasicText(
                unidad,
                Modifier.alignByBaseline(),
                style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = tamañoUnidad(20.sp), color = Paleta.tenue, fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
        }
    }
}

/**
 * Las filas de una lista larga: se ven `visibles` y una fila corta «N más»
 * despliega las demás (y las vuelve a plegar).
 */
@Composable
internal fun <T> FilasPlegables(elementos: List<T>, visibles: Int = 3, fila: @Composable (T, Boolean) -> Unit) {
    var abierta by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierta) 180f else 0f, tween(if (Tema.movimiento) 0 else 220, easing = Movimiento.ESTANDAR), label = "giro")
    val mostradas = if (abierta || elementos.size <= visibles + 1) elementos else elementos.take(visibles)
    mostradas.forEachIndexed { k, e -> fila(e, k == 0) }
    if (elementos.size > visibles + 1) {
        FilaLista(
            titulo = if (abierta) "Menos" else "${elementos.size - visibles} más",
            nota = null,
            primera = false,
            icono = { Spacer(Modifier.width(LADO_PASTILLA)) },
            estiloTitulo = estiloFila(Paleta.tenue, FontWeight.Normal),
            corta = true,
            chevron = IconosHoja.abajo,
            giroChevron = { giro },
            alTocar = { abierta = !abierta },
        )
    }
}

/** El rótulo de una sección («TE CONVIENE · 6»), con un valor opcional a la derecha: 24 arriba y 8 hasta su lista */
@Composable
internal fun RotuloSeccion(texto: String, color: Color = Paleta.tenue, derecha: String? = null, arriba: Dp = Espacio.xl) {
    Row(Modifier.fillMaxWidth().padding(start = Espacio.m, end = Espacio.m, top = arriba, bottom = Espacio.xs)) {
        BasicText(texto, Modifier.weight(1f), style = estiloRotulo(color))
        if (derecha != null) BasicText(derecha, style = estiloRotulo())
    }
}

// ─────────────────────────────────── Comparaciones ──────────────────────────────────

/** Una tarjeta partida en dos columnas iguales con un hilo entre ellas */
@Composable
internal fun Comparacion(
    modifier: Modifier = Modifier,
    relleno: Dp = Espacio.m,
    izquierda: @Composable ColumnScope.() -> Unit,
    derecha: @Composable ColumnScope.() -> Unit,
) {
    val hilo = Paleta.tinta.copy(alpha = Velo.hilo)
    // El hilo se dibuja en medio, a todo el alto: las cifras recortadas engañan a la medida intrínseca
    Row(
        modifier.fillMaxWidth().drawBehind {
            val r = relleno.toPx()
            drawRect(hilo, Offset(size.width / 2f - 0.5.dp.toPx(), r), Size(1.dp.toPx(), size.height - 2 * r))
        }.padding(relleno),
    ) {
        Column(Modifier.weight(1f), content = izquierda)
        Spacer(Modifier.width(Espacio.m * 2 + 1.dp))
        Column(Modifier.weight(1f), content = derecha)
    }
}

/** Una columna de comparación: el rótulo, la cifra recortada a sus mayúsculas 12 dp abajo y la nota 8 dp más abajo */
@Composable
internal fun ColumnaComparacion(rotulo: String, nota: String?, colorRotulo: Color = Paleta.tenue, cifra: @Composable () -> Unit) {
    BasicText(rotulo, style = estiloRotulo(colorRotulo), maxLines = 2)
    Box(Modifier.padding(top = Espacio.s)) { cifra() }
    if (nota != null) BasicText(nota, Modifier.padding(top = Espacio.xs), style = estiloNota())
}

/** Una fecha como cifra media, recortada a sus mayúsculas */
@Composable
internal fun Fecha(texto: String, color: Color = Paleta.tinta) {
    BasicText(texto, Modifier.recorteCap(24.sp), style = estiloFecha(color), maxLines = 1)
}

/**
 * Dos bloques a lo ancho, uno a cada lado y alineados por arriba; si no caben
 * juntos, el segundo baja debajo del primero.
 */
@Composable
internal fun DosALoAncho(hueco: Dp, vertical: Dp, porAbajo: Boolean = false, izquierda: @Composable () -> Unit, derecha: @Composable () -> Unit) {
    Layout(content = {
        Box { izquierda() }
        Box { derecha() }
    }) { medibles, limites ->
        val sueltos = limites.copy(minWidth = 0, minHeight = 0)
        val a = medibles[0].measure(sueltos)
        val b = medibles[1].measure(sueltos)
        val ancho = limites.maxWidth
        if (a.width + hueco.roundToPx() + b.width <= ancho) {
            val alto = max(a.height, b.height)
            layout(ancho, alto) {
                a.place(0, if (porAbajo) alto - a.height else 0)
                b.place(ancho - b.width, if (porAbajo) alto - b.height else 0)
            }
        } else {
            val separa = vertical.roundToPx()
            layout(ancho, a.height + separa + b.height) {
                a.place(0, 0)
                b.place(0, a.height + separa)
            }
        }
    }
}

// ─────────────────────────────────── Cabeceras ──────────────────────────────────

/** Lo que dice la cabecera de una pantalla interna: su pastilla y el título; subtítulo solo si trae un dato que el título no dice */
internal class DatosCabecera(val icono: List<Path>, val titulo: String, val subtitulo: String? = null, val azul: Boolean = false)

/** La cabecera en rejilla: la pastilla centrada con el título y el subtítulo debajo del título */
@Composable
internal fun CabeceraInterna(d: DatosCabecera, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pastilla(d.icono, d.azul)
            BasicText(d.titulo, Modifier.padding(start = Espacio.s), style = estiloTitulo())
        }
        if (d.subtitulo != null) BasicText(d.subtitulo, Modifier.padding(start = LADO_PASTILLA + Espacio.s, top = Espacio.hilo), style = estiloCuerpo(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * «Ver en el mapa», arriba a la derecha de la cabecera: una cápsula de 36 dp
 * (el alto de los círculos de atrás y cerrar) con el icono del mapa. Es una
 * acción de la pantalla, no un bloque más al final de la lista.
 */
@Composable
internal fun BotonMapa(alTocar: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Paleta.tinta.copy(alpha = Velo.pastilla))
            .clickable(role = Role.Button, onClick = alTocar).padding(start = Espacio.s, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        IconoLucide(Lucide.mapa, Paleta.tinta, 16.dp)
        BasicText("Ver en el mapa", style = estiloNota(Paleta.tinta, FontWeight.Medium), maxLines = 1)
    }
}

// ─────────────────────────────────── Iconos ──────────────────────────────────

/**
 * Los trazos Lucide de la hoja y de Ajustes, en la rejilla de 24 de IconoTrazo.
 */
internal object IconosHoja {
    private fun trazo(d: String): Path = PathParser().parsePathString(d).toPath()

    val reiniciar = listOf(trazo("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"), trazo("M3 3v5h5"))
    val libroAbierto = listOf(trazo("M12 7v14"), trazo("M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"))
    val usuarios = listOf(trazo("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"), trazo("M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0z"), trazo("M22 21v-2a4 4 0 0 0-3-3.87"), trazo("M16 3.13a4 4 0 0 1 0 7.75"))
    val llave = listOf(trazo("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"))
    val eslabon = listOf(trazo("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"), trazo("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"))
    val calendario = listOf(trazo("M8 2v4"), trazo("M16 2v4"), trazo("M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"), trazo("M3 10h18"))
    val calendarioListo = calendario + listOf(trazo("m9 16 2 2 4-4"))
    val candado = listOf(trazo("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"), trazo("M7 11V7a5 5 0 0 1 10 0v4"))
    val candadoAbierto = listOf(trazo("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"), trazo("M7 11V7a5 5 0 0 1 9.9-1"))
    val mas = listOf(trazo("M5 12h14"), trazo("M12 5v14"))
    val abajo = listOf(trazo("m6 9 6 6 6-6"))
    val relojArena = listOf(
        trazo("M5 22h14"),
        trazo("M5 2h14"),
        trazo("M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22"),
        trazo("M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2"),
    )
    val fuera = listOf(trazo("M7 7h10v10"), trazo("M7 17 17 7"))
}

/** Los trazos de cada icono de fila: la interfaz los dibuja, la lógica solo dice cuál */
internal fun IconoFila.trazos(): List<Path> = when (this) {
    IconoFila.REINICIAR -> IconosHoja.reiniciar
    IconoFila.LIBRO_ABIERTO -> IconosHoja.libroAbierto
    IconoFila.USUARIOS -> IconosHoja.usuarios
    IconoFila.LLAVE -> IconosHoja.llave
    IconoFila.ESLABON -> IconosHoja.eslabon
    IconoFila.CALENDARIO -> IconosHoja.calendario
    IconoFila.RELOJ_ARENA -> IconosHoja.relojArena
    IconoFila.BIRRETE -> Lucide.birrete
}

// ─────────────────────────────────── Para el anillo ──────────────────────────────────

/** Un icono Lucide dibujado dentro de un Canvas, en la caja de `lado` px que empieza en (izq, arriba) */
internal fun DrawScope.icono(trazos: List<Path>, color: Color, izq: Float, arriba: Float, lado: Float, grosor: Float = 2f) {
    val e = lado / 24f
    withTransform({
        translate(izq, arriba)
        scale(e, e, pivot = Offset.Zero)
    }) {
        trazos.forEach { drawPath(it, color, style = Stroke(grosor, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    }
}

/** Los rótulos: mayúsculas semibold de 11 sp con 0,05 em de espaciado */
@Composable
internal fun versal(color: Color) = TextStyle(fontFamily = inter(), fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.05.em, color = color)

/** Un texto de la hoja: Inter, con el tamaño, el peso y el color que pida */
@Composable
internal fun estilo(tam: Int, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam * 1.35f).roundToInt()) =
    estilo(tam.sp, color, peso, interlinea)

/** Lo mismo con el tamaño de un token de [Tipo]: la interlínea por defecto es 1,35 veces el tamaño */
@Composable
internal fun estilo(tam: TextUnit, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam.value * 1.35f).roundToInt()) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = tam, lineHeight = interlinea.sp, color = color, fontFeatureSettings = "tnum")

/** Los iconos de las filas, por su nombre: la interfaz los traduce a trazos (`IconoFila.trazos()`) */
internal enum class IconoFila { REINICIAR, LIBRO_ABIERTO, USUARIOS, LLAVE, ESLABON, CALENDARIO, RELOJ_ARENA, BIRRETE }

/** El aspa de las hojas: el mismo círculo que el de cerrar de las demás pantallas */
@Composable
fun BotonCerrar(alCerrar: () -> Unit) = BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
