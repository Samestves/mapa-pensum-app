package mpensum.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tokens
import mpensum.tema.acentoDe
import mpensum.tema.inter
import mpensum.tema.tokensDe

/*
 * El selector de tema: un carrusel de mini portadas, una por juego y la de
 * Sistema partida en diagonal (claro arriba a la izquierda, oscuro abajo a
 * la derecha), con un hilo entre los oscuros, los claros y los de exterior.
 * Lo usan Personalizar, en Ajustes, y la hoja de avance: elegir es guardar
 * Preferencias.tema, y la app cambia en el acto.
 *
 * Aquí vive también la vista previa fija de Personalizar: dos lienzos
 * pintados con la Paleta del tema que se ve, sin desenfoque ni animación.
 */

private const val ANCHO_MINI = 60f
private const val ALTO_MINI = 84f

/**
 * La portada de un juego en miniatura (60×84), pintada con los tokens de ESE
 * juego y no con los del tema que se ve. `juego` null es Sistema, partida en
 * diagonal. `escala` la reduce para las filas.
 */
@Composable
fun MiniTema(juego: Juego?, modifier: Modifier = Modifier, escala: Float = 1f) {
    Canvas(modifier.size((ANCHO_MINI * escala).dp, (ALTO_MINI * escala).dp)) {
        scale(density * escala, Offset.Zero) {
            if (juego == null) {
                miniatura(Juego.CLARO)
                val oscuro = Path().apply { moveTo(ANCHO_MINI, 0f); lineTo(ANCHO_MINI, ALTO_MINI); lineTo(0f, ALTO_MINI); close() }
                clipPath(oscuro) { miniatura(Juego.OSCURO) }
            } else miniatura(juego)
        }
    }
}

/** Las medidas están en dp y el lienzo ya viene escalado a ellos */
private fun DrawScope.miniatura(juego: Juego) {
    val t: Tokens = tokensDe(juego)
    val vidrio = if (juego.claro) t.vidrio else t.panel
    drawRect(t.lienzo, size = Size(ANCHO_MINI, ALTO_MINI))
    // La aurora propia de cada juego: dos luces, sin nada de animación
    if (juego.aurora > 0f) {
        val fuerza = (juego.aurora * 0.4f).coerceAtMost(0.5f)
        drawRect(Brush.radialGradient(listOf(t.aurora.copy(alpha = fuerza), Color.Transparent), Offset(8f, 4f), 46f), size = Size(ANCHO_MINI, ALTO_MINI))
        drawRect(Brush.radialGradient(listOf(t.aurora2.copy(alpha = fuerza * 0.8f), Color.Transparent), Offset(54f, 80f), 40f), size = Size(ANCHO_MINI, ALTO_MINI))
    }
    drawRoundRect(t.tinta.copy(alpha = 0.85f), Offset(6f, 7f), Size(22f, 3f), CornerRadius(1.5f))
    // La tarjeta de tu carrera: puntos, cifra, etiqueta y el chip
    drawRoundRect(vidrio, Offset(5f, 16f), Size(50f, 31f), CornerRadius(6f))
    drawRoundRect(t.borde, Offset(5.5f, 16.5f), Size(49f, 30f), CornerRadius(5.5f), style = Stroke(1f))
    for (c in 0 until 4) for (f in 0 until 3) drawCircle(t.azul, 0.9f, Offset(11f + c * 3.5f, 22f + f * 3.5f))
    drawRoundRect(t.tinta, Offset(38f, 22f), Size(11f, 6f), CornerRadius(1f))
    drawRoundRect(t.suave, Offset(11f, 38f), Size(17f, 3f), CornerRadius(1.5f))
    drawRoundRect(t.azul, Offset(35.5f, 36.5f), Size(14f, 5f), CornerRadius(2.5f), style = Stroke(1f))
    // Las filas de otras carreras
    for (i in 0 until 3) {
        val y = 51f + i * 11f
        drawRoundRect(vidrio, Offset(5f, y), Size(50f, 8f), CornerRadius(3f))
        drawRoundRect(t.borde, Offset(5.5f, y + 0.5f), Size(49f, 7f), CornerRadius(2.5f), style = Stroke(1f))
    }
}

/**
 * Lo que lleva el carrusel, en orden: Sistema, los seis oscuros, un hilo
 * (null), los cinco claros, otro hilo y los dos Exterior.
 */
private val TEMAS: List<Apariencia?> = buildList {
    add(Apariencia.SISTEMA)
    addAll(Apariencia.entries.filter { it.juego != null && !it.juego.claro && !it.juego.exterior })
    add(null)
    addAll(Apariencia.entries.filter { it.juego != null && it.juego.claro && !it.juego.exterior })
    add(null)
    addAll(Apariencia.entries.filter { it.juego?.exterior == true })
}

/**
 * El carrusel de temas: celdas de 72×96 con la mini tarjeta de 60×84 dentro
 * (radio 10) y el nombre debajo; la elegida lleva un aro de 2 dp en tinta con
 * radio 16. `relleno` deja el aire de los lados sin cortar el desplazamiento.
 */
@Composable
fun CarruselTemas(modifier: Modifier = Modifier, relleno: PaddingValues = PaddingValues(horizontal = 16.dp)) {
    val actual by Preferencias.tema.collectAsState()
    val lista = rememberLazyListState(initialFirstVisibleItemIndex = (TEMAS.indexOf(actual) - 1).coerceAtLeast(0))
    LazyRow(
        modifier.semantics { selectableGroup() },
        state = lista,
        contentPadding = relleno,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(TEMAS, key = { i, a -> a?.name ?: "hilo-$i" }) { _, a ->
            // El hilo (1×64) separa los grupos; no se pulsa ni se anuncia
            if (a == null) Box(Modifier.height(96.dp).padding(vertical = 16.dp).width(1.dp).background(Paleta.borde))
            else CeldaTema(a, a == actual) { Preferencias.guardarTema(a) }
        }
    }
}

@Composable
private fun CeldaTema(apariencia: Apariencia, elegida: Boolean, alPulsar: () -> Unit) {
    Column(
        Modifier
            .width(72.dp)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar)
            .semantics { contentDescription = apariencia.nombre },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp, 96.dp)
                .drawBehind {
                    if (elegida) {
                        val g = 2.dp.toPx()
                        drawRoundRect(Paleta.tinta, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(16.dp.toPx() - g / 2), style = Stroke(g))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val forma = RoundedCornerShape(10.dp)
            MiniTema(apariencia.juego, Modifier.clip(forma).border(1.dp, Paleta.borde, forma))
        }
        BasicText(
            apariencia.nombre,
            Modifier.padding(top = 8.dp),
            style = TextStyle(fontFamily = inter(), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = if (elegida) Paleta.tinta else Paleta.tenue),
            maxLines = 2,
        )
    }
}

/**
 * La vista previa fija de Personalizar: la portada y el mapa lado a lado, en
 * un marco de radio 24 con relleno 8 (las ventanas llevan 16: radios
 * concéntricos). Pinta con la Paleta y el Tema de ahora, así que solo se
 * redibuja cuando algo de eso cambia. `silueta` es la de tu carrera (null,
 * una de muestra).
 */
@Composable
fun VistaPrevia(colorCarrera: Color, silueta: List<Int>?, alto: Dp, modifier: Modifier = Modifier) {
    val marco = RoundedCornerShape(24.dp)
    Row(
        modifier
            .height(alto)
            .clip(marco)
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .padding(8.dp)
            .semantics { contentDescription = "Vista previa: así se ven tu portada y tu mapa" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val ventana = RoundedCornerShape(16.dp)
        Canvas(Modifier.weight(1f).fillMaxSize().clip(ventana).border(1.dp, Paleta.borde, ventana)) { previaPortada(colorCarrera, silueta) }
        Canvas(Modifier.weight(1f).fillMaxSize().clip(ventana).border(1.dp, Paleta.borde, ventana)) { previaMapa(colorCarrera) }
    }
}

/** Las ventanas se dibujan en dp sobre su propio tamaño */
private fun DrawScope.previaPortada(colorCarrera: Color, silueta: List<Int>?) {
    val acento = acentoDe(colorCarrera)
    val w = size.width
    val h = size.height
    val dp = density
    drawRect(Paleta.lienzo)
    val luz = Tema.intensidadAurora
    if (luz > 0f) {
        val fuerza = (luz * 0.4f).coerceAtMost(0.5f)
        drawRect(Brush.radialGradient(listOf(Paleta.aurora.copy(alpha = fuerza), Color.Transparent), Offset(w * 0.15f, 0f), w * 0.9f))
        drawRect(Brush.radialGradient(listOf(Paleta.aurora2.copy(alpha = fuerza * 0.8f), Color.Transparent), Offset(w * 0.9f, h), w * 0.8f))
    }
    val m = 12 * dp
    drawRoundRect(Paleta.tinta.copy(alpha = 0.85f), Offset(m, 14 * dp), Size(w * 0.3f, 4 * dp), CornerRadius(2 * dp))
    // La tarjeta de tu carrera, en cristal liso
    val cima = 28 * dp
    val alto = h * 0.42f
    drawRoundRect(Paleta.panel, Offset(m, cima), Size(w - 2 * m, alto), CornerRadius(12 * dp))
    drawRoundRect(Paleta.borde, Offset(m, cima), Size(w - 2 * m, alto), CornerRadius(12 * dp), style = Stroke(Tema.grosorBorde * dp))
    val columnas = silueta?.takeIf { it.isNotEmpty() } ?: listOf(4, 4, 3, 3, 2)
    val paso = minOf(5 * dp, (w * 0.5f) / columnas.size)
    columnas.forEachIndexed { c, n ->
        for (f in 0 until n.coerceAtMost(5)) drawCircle(acento, 1.4f * dp, Offset(m + 10 * dp + c * paso, cima + 12 * dp + f * paso))
    }
    drawRoundRect(Paleta.tinta, Offset(w - m - 38 * dp, cima + 10 * dp), Size(26 * dp, 8 * dp), CornerRadius(2 * dp))
    drawRoundRect(Paleta.suave, Offset(m + 10 * dp, cima + alto - 20 * dp), Size(34 * dp, 4 * dp), CornerRadius(2 * dp))
    // «Continuar →» en el acento
    drawRoundRect(acento, Offset(w - m - 50 * dp, cima + alto - 24 * dp), Size(40 * dp, 14 * dp), CornerRadius(7 * dp), style = Stroke(1.2f * dp))
    // Otras carreras
    for (i in 0 until 3) {
        val y = cima + alto + 10 * dp + i * 16 * dp
        drawRoundRect(Paleta.panel, Offset(m, y), Size(w - 2 * m, 12 * dp), CornerRadius(6 * dp))
        drawRoundRect(Paleta.borde, Offset(m, y), Size(w - 2 * m, 12 * dp), CornerRadius(6 * dp), style = Stroke(Tema.grosorBorde * dp))
    }
}

/** El mapa: fondo, ocho tarjetas en sus estados, los cables y la barra con el anillo en el acento */
private fun DrawScope.previaMapa(colorCarrera: Color) {
    val acento = acentoDe(colorCarrera)
    val dp = density
    val w = size.width
    val h = size.height
    drawRect(Paleta.lienzo)
    when (Tema.fondo) {
        Fondo.REJILLA -> {
            var x = 16 * dp
            while (x < w) { drawLine(Paleta.rejilla, Offset(x, 0f), Offset(x, h), 1f); x += 16 * dp }
            var y = 16 * dp
            while (y < h) { drawLine(Paleta.rejilla, Offset(0f, y), Offset(w, y), 1f); y += 16 * dp }
        }
        Fondo.PUNTOS -> {
            val puntos = ArrayList<Offset>()
            var y = 12 * dp
            while (y < h) { var x = 12 * dp; while (x < w) { puntos.add(Offset(x, y)); x += 16 * dp }; y += 16 * dp }
            drawPoints(puntos, PointMode.Points, lerp(Paleta.rejilla, Paleta.tinta, 0.1f), 2.5f * dp, StrokeCap.Round)
        }
        Fondo.LISO -> Unit
    }
    // Ocho tarjetas en tres columnas: (columna, fila, estado) con 0 normal, 1 aprobada, 2 inscribible, 3 cursando, 4 bloqueada
    val ancho = 38 * dp
    val alto = 18 * dp
    val sep = (w - 3 * ancho) / 4
    val estados = intArrayOf(1, 1, 2, 1, 3, 2, 4, 0)
    fun origen(i: Int) = Offset(sep + (i % 3) * (ancho + sep), 14 * dp + (i / 3) * 32 * dp)
    val grosor = 1.2f * dp * Tema.grosor.factor * (if (Tema.fuerte) 1.5f else 1f)
    val cable = lerp(Paleta.tinta, colorCarrera, Tema.juego.cables).copy(alpha = 0.7f)
    // Cada tarjeta se une con la de su derecha y con la de abajo (lo que da el mapa); Al tocar pinta solo los de la primera
    val uniones = listOf(0 to 1, 1 to 2, 0 to 3, 1 to 4, 4 to 5, 3 to 4, 4 to 7, 5 to 8).filter { it.second < 8 }
    for ((a, b) in uniones) {
        if (Tema.cables == FormaCables.AL_TOCAR && a != 4 && b != 4) continue
        val p = origen(a); val q = origen(b)
        val ini = if (b % 3 == a % 3) Offset(p.x + ancho / 2, p.y + alto) else Offset(p.x + ancho, p.y + alto / 2)
        val fin = if (b % 3 == a % 3) Offset(q.x + ancho / 2, q.y) else Offset(q.x, q.y + alto / 2)
        val ruta = Path().apply {
            moveTo(ini.x, ini.y)
            when {
                Tema.cables == FormaCables.RECTOS -> { val m = (ini.x + fin.x) / 2; if (b % 3 == a % 3) lineTo(fin.x, fin.y) else { lineTo(m, ini.y); lineTo(m, fin.y); lineTo(fin.x, fin.y) } }
                b % 3 == a % 3 -> lineTo(fin.x, fin.y)
                else -> cubicTo((ini.x + fin.x) / 2, ini.y, (ini.x + fin.x) / 2, fin.y, fin.x, fin.y)
            }
        }
        drawPath(ruta, cable, style = Stroke(grosor, cap = StrokeCap.Round))
    }
    for (i in 0 until 8) {
        val o = origen(i)
        val forma = CornerRadius(5 * dp)
        val u = Size(ancho, alto)
        val g = Tema.grosorBorde * dp
        val (relleno, borde) = when (estados[i]) {
            1 -> Paleta.verde.copy(alpha = if (Tema.atenuarAprobadas) 0.12f else 0.22f) to Paleta.verde.copy(alpha = if (Tema.atenuarAprobadas) 0.45f else 1f)
            2 -> Paleta.panel to Paleta.azul
            3 -> Paleta.panel to Paleta.tinta
            4 -> Paleta.panel to Paleta.borde
            else -> Paleta.panel to Paleta.borde
        }
        drawRoundRect(Paleta.lienzo, o, u, forma)
        drawRoundRect(relleno, o, u, forma)
        drawRoundRect(borde, Offset(o.x + g / 2, o.y + g / 2), Size(u.width - g, u.height - g), CornerRadius(5 * dp - g / 2), style = Stroke(g))
        drawRoundRect(Paleta.suave.copy(alpha = if (estados[i] == 4) 0.4f else 0.8f), Offset(o.x + 6 * dp, o.y + 7 * dp), Size(ancho * 0.55f, 3 * dp), CornerRadius(1.5f * dp))
    }
    // La barra Núcleo con el anillo del avance en el acento
    val bw = w * 0.62f
    val bh = 26 * dp
    val bx = (w - bw) / 2
    val by = h - bh - 10 * dp
    drawRoundRect(Paleta.panel, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2))
    drawRoundRect(Paleta.borde, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2), style = Stroke(Tema.grosorBorde * dp))
    for (i in 0 until 3) drawCircle(Paleta.apagada, 2.2f * dp, Offset(bx + bh + i * 14 * dp, by + bh / 2))
    drawCircle(acento, 7 * dp, Offset(bx + bw - bh / 2, by + bh / 2), style = Stroke(2f * dp))
}
