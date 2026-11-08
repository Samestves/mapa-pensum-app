package mpensum.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
import mpensum.datos.ColorCarrera
import mpensum.tema.Espacio
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.Tokens
import mpensum.tema.colorDe
import mpensum.tema.inter
import mpensum.tema.tokensDe

/*
 * El selector de tema de Personalizar, en dos piezas que no animan nada:
 *
 *   - PantallasTema: dos pantallas pintadas con los tokens de un juego (la
 *     portada y el mapa, o la ranura de día y la de noche con Automático).
 *   - PaletaTemas: los doce juegos como discos de 40 dp en dos filas (oscuros
 *     arriba, claros abajo, los dos Exterior aparte tras un hilo).
 *
 * Todo es Canvas que lee tokens ya resueltos: se redibuja solo cuando cambia
 * una preferencia, sin desenfoque ni bucles (coste de gama baja).
 */

/** Los juegos de cada fila de la paleta, en orden */
private val OSCUROS = Juego.entries.filter { !it.claro && !it.exterior }
private val CLAROS = Juego.entries.filter { it.claro && !it.exterior }

/** El disco de la paleta: 40 dp dentro de un toque de hasta 52 */
private val DISCO = 40.dp
private val TOQUE_MAXIMO = 52.dp

/** El hilo que separa los Exterior: 1 dp con 8 a cada lado */
private val HILO = 17.dp

/**
 * La paleta de 2 × 6. `elegido` lleva el aro; `otro` (con `otroEsNoche`) lleva
 * la insignia de la otra ranura de Automático. Los toques se reparten a lo
 * ancho que haya, hasta 52 dp.
 */
@Composable
fun PaletaTemas(elegido: Juego, otro: Juego?, otroEsNoche: Boolean, modifier: Modifier = Modifier, alElegir: (Juego) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val toque = ((maxWidth - HILO) / 6).coerceAtMost(TOQUE_MAXIMO)
        Column(Modifier.fillMaxWidth().semantics { selectableGroup() }, verticalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
            for ((fila, exterior) in listOf(OSCUROS to Juego.EXTERIOR_OSCURO, CLAROS to Juego.EXTERIOR)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row { fila.forEach { DiscoTema(it, toque, it == elegido, if (it == otro) otroEsNoche else null, alElegir) } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(horizontal = Espacio.xs).width(1.dp).height(36.dp).background(Paleta.tinta.copy(alpha = 0.16f)))
                        DiscoTema(exterior, toque, exterior == elegido, if (exterior == otro) otroEsNoche else null, alElegir)
                    }
                }
            }
        }
    }
}

/** Un disco: su toque, el aro si es el elegido y la insignia si es el de la otra ranura (`esNoche` null: sin insignia) */
@Composable
private fun DiscoTema(juego: Juego, toque: Dp, elegido: Boolean, esNoche: Boolean?, alElegir: (Juego) -> Unit) {
    val t = remember(juego) { tokensDe(juego) }
    Box(
        Modifier
            .size(toque)
            .selectable(elegido, role = Role.RadioButton) { alElegir(juego) }
            .semantics { contentDescription = juego.nombre },
        contentAlignment = Alignment.Center,
    ) {
        if (elegido) Box(Modifier.size(toque).border(2.dp, Paleta.tinta, CircleShape))
        Canvas(Modifier.size(DISCO).clip(CircleShape)) { discoDe(juego, t) }
        if (esNoche != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(Paleta.panel).border(1.dp, Paleta.tinta.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { IconoTrazo(if (esNoche) Lucide.luna else Lucide.sol, Paleta.tinta, 12.dp, 2f) }
        }
    }
}

/** El disco con los tokens de ESE juego: lienzo, dos luces y su filo; los Exterior, sin luz y con el símbolo de contraste */
private fun DrawScope.discoDe(juego: Juego, t: Tokens) {
    val dp = density
    val w = size.width
    val h = size.height
    drawRect(t.lienzo)
    if (juego.exterior) {
        drawCircle(t.tinta.copy(alpha = 0.7f), w / 2 - 0.75f * dp, style = Stroke(1.5f * dp))
        val radio = 10 * dp
        drawCircle(t.tinta, radio, style = Stroke(1.5f * dp))
        drawArc(t.tinta, 90f, 180f, true, Offset(w / 2 - radio, h / 2 - radio), Size(radio * 2, radio * 2))
        return
    }
    val base = minOf(juego.aurora * 0.24f, 0.3f)
    drawRect(Brush.radialGradient(listOf(t.aurora.copy(alpha = (base * 1.7f).coerceAtMost(1f)), Color.Transparent), Offset(w * 0.26f, h * 0.2f), w * 0.66f))
    drawRect(Brush.radialGradient(listOf(t.aurora2.copy(alpha = (base * 1.1f).coerceAtMost(1f)), Color.Transparent), Offset(w * 0.84f, h * 0.92f), w * 0.58f))
    drawCircle(t.tinta.copy(alpha = 0.18f), w / 2 - 0.5f * dp, style = Stroke(dp))
}

/** Lo que enseña una de las dos pantallas: su juego, qué pantalla es, su rótulo y, con Automático, si es la de noche (null: no hay ranuras) */
class PantallaTema(val juego: Juego, val mapa: Boolean, val rotulo: String, val noche: Boolean?)

/**
 * Las dos pantallas de Personalizar, centradas. Cada una lleva debajo su
 * rótulo (siempre, para que el layout no cambie al activar Automático). Con
 * `ranuraActiva` (0 o 1) esa pantalla lleva el aro de 2 dp a 4 dp de su borde,
 * y las dos se pueden tocar para elegirlas; null: sin ranuras. `color` es el de
 * tu carrera (null: sin carrera).
 */
@Composable
fun PantallasTema(
    izquierda: PantallaTema,
    derecha: PantallaTema,
    color: ColorCarrera?,
    silueta: List<Int>?,
    ranuraActiva: Int?,
    modifier: Modifier = Modifier,
    anchoMaximo: Dp = 160.dp,
    altoMaximo: Dp = 208.dp,
    hueco: Dp = 15.dp,
    alElegirRanura: (Int) -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        // Si no caben a tamaño completo, se encogen sin cambiar de proporción
        val ancho = ((maxWidth - hueco) / 2).coerceAtMost(anchoMaximo)
        val alto = altoMaximo * (ancho / anchoMaximo)
        Row(Modifier.semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(hueco)) {
            listOf(izquierda, derecha).forEachIndexed { i, p ->
                PantallaConRotulo(p, color, silueta, ancho, alto, ranuraActiva == i, ranuraActiva != null) { alElegirRanura(i) }
            }
        }
    }
}

@Composable
private fun PantallaConRotulo(p: PantallaTema, color: ColorCarrera?, silueta: List<Int>?, ancho: Dp, alto: Dp, activa: Boolean, elegible: Boolean, alPulsar: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    val contraste = Tema.contraste
    val t = remember(p.juego, contraste) { tokensDe(p.juego, contraste) }
    val colorCarrera = remember(p.juego, color, t) { color?.let { colorDe(if (p.juego.claro) it.claro else it.oscuro) } ?: t.suave }
    val acento = Tema.acento?.colorEn(p.juego.claro, t.suave) ?: colorCarrera
    val luz = if (Tema.luzFondo) p.juego.aurora else 0f
    Column(
        Modifier
            .width(ancho)
            .then(if (elegible) Modifier.selectable(activa, role = Role.RadioButton, onClick = alPulsar) else Modifier)
            .semantics { contentDescription = "${p.rotulo}: ${p.juego.nombre}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .size(ancho, alto)
                // El aro de la ranura activa: a 4 dp de la pantalla, con radio 24 (20 + 4)
                .drawBehind {
                    if (activa) {
                        val d = 4.dp.toPx()
                        val g = 2.dp.toPx()
                        drawRoundRect(Paleta.tinta, Offset(-d + g / 2, -d + g / 2), Size(size.width + 2 * d - g, size.height + 2 * d - g), CornerRadius(24.dp.toPx() - g / 2), style = Stroke(g))
                    }
                }
                // Una sola sombra estática, solo en oscuro y sin modo ligero
                .then(if (!Tema.claro && !Tema.ligero) Modifier.shadow(18.dp, forma, ambientColor = Color.Black.copy(alpha = 0.55f), spotColor = Color.Black.copy(alpha = 0.55f)) else Modifier)
                .clip(forma)
                .border(1.dp, Paleta.tinta.copy(alpha = 0.1f), forma),
        ) {
            if (p.mapa) previaMapa(t, colorCarrera, 3, 3, ESTADOS_ESTRECHO, true)
            else previaPortada(t, luz, acento, silueta)
        }
        Row(Modifier.padding(top = Espacio.xs).height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            val estilo = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = if (activa) Paleta.tinta else Paleta.tenue)
            if (p.noche != null) {
                IconoTrazo(if (p.noche) Lucide.luna else Lucide.sol, estilo.color, 16.dp, 1.75f, Modifier.padding(end = Espacio.xxs))
                BasicText(p.rotulo + " · ", style = estilo)
                BasicText(p.juego.nombre, style = estilo.copy(fontWeight = FontWeight.Medium))
            } else BasicText(p.rotulo, style = estilo)
        }
    }
}

/** La portada: tarjeta de tu carrera con su silueta, el chip en el acento y cuatro filas de otras carreras */
private fun DrawScope.previaPortada(t: Tokens, luz: Float, acento: Color, silueta: List<Int>?) {
    val w = size.width
    val h = size.height
    val dp = density
    drawRect(t.lienzo)
    if (luz > 0f) {
        val fuerza = (luz * 0.4f).coerceAtMost(0.5f)
        drawRect(Brush.radialGradient(listOf(t.aurora.copy(alpha = fuerza), Color.Transparent), Offset(w * 0.15f, 0f), w * 0.9f))
        drawRect(Brush.radialGradient(listOf(t.aurora2.copy(alpha = fuerza * 0.8f), Color.Transparent), Offset(w * 0.9f, h), w * 0.8f))
    }
    val g = Tema.grosorBorde * dp
    val m = 12 * dp
    drawRoundRect(t.tinta.copy(alpha = 0.85f), Offset(m, 16 * dp), Size(w * 0.3f, 4 * dp), CornerRadius(2 * dp))
    val cima = 32 * dp
    val alto = h * 0.4f
    drawRoundRect(t.panel, Offset(m, cima), Size(w - 2 * m, alto), CornerRadius(12 * dp))
    drawRoundRect(t.borde, Offset(m, cima), Size(w - 2 * m, alto), CornerRadius(12 * dp), style = Stroke(g))
    val columnas = silueta?.takeIf { it.isNotEmpty() } ?: listOf(4, 4, 3, 3, 2)
    val paso = minOf(5 * dp, (w * 0.5f) / columnas.size)
    columnas.forEachIndexed { c, n ->
        for (f in 0 until n.coerceAtMost(5)) drawCircle(acento, 1.4f * dp, Offset(m + 10 * dp + c * paso, cima + 12 * dp + f * paso))
    }
    drawRoundRect(t.tinta, Offset(w - m - 38 * dp, cima + 10 * dp), Size(26 * dp, 8 * dp), CornerRadius(2 * dp))
    drawRoundRect(t.suave, Offset(m + 10 * dp, cima + alto - 20 * dp), Size(34 * dp, 4 * dp), CornerRadius(2 * dp))
    drawRoundRect(acento, Offset(w - m - 50 * dp, cima + alto - 24 * dp), Size(40 * dp, 14 * dp), CornerRadius(7 * dp), style = Stroke(1.2f * dp))
    for (i in 0 until 4) {
        val y = cima + alto + 12 * dp + i * 18 * dp
        if (y + 13 * dp > h) break
        drawRoundRect(t.panel, Offset(m, y), Size(w - 2 * m, 13 * dp), CornerRadius(6 * dp))
        drawRoundRect(t.borde, Offset(m, y), Size(w - 2 * m, 13 * dp), CornerRadius(6 * dp), style = Stroke(g))
    }
}

private val ESTADOS_ESTRECHO = intArrayOf(1, 1, 2, 1, 3, 2, 4, 0, 0)
private val ESTADOS_ANCHO = intArrayOf(1, 1, 2, 0, 1, 3, 2, 4, 1, 2, 4, 0)

/**
 * El mapa: fondo, tarjetas en sus estados (0 normal, 1 aprobada, 2 inscribible,
 * 3 cursando, 4 bloqueada) en `columnas` × `filas`, sus cables y, si `conBarra`,
 * la barra Núcleo abajo. Lee del [Tema] lo que Personalizar cambia: fondo,
 * cables, grosor, atenuar lo aprobado y códigos.
 */
private fun DrawScope.previaMapa(t: Tokens, colorCarrera: Color, columnas: Int, filas: Int, estados: IntArray, conBarra: Boolean) {
    val dp = density
    val w = size.width
    val h = size.height
    drawRect(t.lienzo)
    when (Tema.fondo) {
        Fondo.REJILLA -> {
            var x = 16 * dp
            while (x < w) { drawLine(t.rejilla, Offset(x, 0f), Offset(x, h), 1f); x += 16 * dp }
            var y = 16 * dp
            while (y < h) { drawLine(t.rejilla, Offset(0f, y), Offset(w, y), 1f); y += 16 * dp }
        }
        Fondo.PUNTOS -> {
            val puntos = ArrayList<Offset>()
            var y = 12 * dp
            while (y < h) { var x = 12 * dp; while (x < w) { puntos.add(Offset(x, y)); x += 16 * dp }; y += 16 * dp }
            drawPoints(puntos, PointMode.Points, lerp(t.rejilla, t.tinta, 0.1f), 2.5f * dp, StrokeCap.Round)
        }
        Fondo.LISO -> Unit
    }
    val ancho = (w / columnas) * 0.66f
    val alto = 20 * dp
    val sep = (w - columnas * ancho) / (columnas + 1)
    val cima = 16 * dp
    val zona = h - cima - (if (conBarra) 50 * dp else 16 * dp) - alto
    val paso = if (filas > 1) minOf(zona / (filas - 1), 56 * dp) else 0f
    fun origen(i: Int) = Offset(sep + (i % columnas) * (ancho + sep), cima + (i / columnas) * paso)
    val total = columnas * filas
    val foco = total / 2
    val grosor = 1.2f * dp * Tema.grosor.factor * (if (Tema.fuerte) 1.5f else 1f)
    val cable = lerp(t.tinta, colorCarrera, Tema.juego.cables).copy(alpha = 0.7f)
    // Cada tarjeta se une con la de su derecha o la de abajo, alternando; Al tocar pinta solo los de la del centro
    val uniones = buildList {
        for (i in 0 until total) {
            if (i % columnas < columnas - 1 && (i / columnas + i) % 2 == 0) add(i to i + 1)
            if (i + columnas < total && i % 2 == 0) add(i to i + columnas)
        }
    }
    for ((a, b) in uniones) {
        if (Tema.cables == FormaCables.AL_TOCAR && a != foco && b != foco) continue
        val vertical = b - a == columnas
        val p = origen(a)
        val q = origen(b)
        val ini = if (vertical) Offset(p.x + ancho / 2, p.y + alto) else Offset(p.x + ancho, p.y + alto / 2)
        val fin = if (vertical) Offset(q.x + ancho / 2, q.y) else Offset(q.x, q.y + alto / 2)
        val ruta = Path().apply {
            moveTo(ini.x, ini.y)
            val medio = (ini.x + fin.x) / 2
            when {
                vertical -> lineTo(fin.x, fin.y)
                Tema.rectos -> { lineTo(medio, ini.y); lineTo(medio, fin.y); lineTo(fin.x, fin.y) }
                else -> cubicTo(medio, ini.y, medio, fin.y, fin.x, fin.y)
            }
        }
        drawPath(ruta, cable, style = Stroke(grosor, cap = StrokeCap.Round))
    }
    val forma = CornerRadius(5 * dp)
    val g = Tema.grosorBorde * dp
    for (i in 0 until total) {
        val o = origen(i)
        val u = Size(ancho, alto)
        val estado = estados.getOrElse(i) { 0 }
        val (relleno, borde) = when (estado) {
            1 -> t.verde.copy(alpha = if (Tema.atenuarAprobadas) 0.12f else 0.22f) to t.verde.copy(alpha = if (Tema.atenuarAprobadas) 0.45f else 1f)
            2 -> t.panel to t.azul
            3 -> t.panel to t.tinta
            else -> t.panel to t.borde
        }
        drawRoundRect(t.lienzo, o, u, forma)
        drawRoundRect(relleno, o, u, forma)
        drawRoundRect(borde, Offset(o.x + g / 2, o.y + g / 2), Size(u.width - g, u.height - g), CornerRadius(5 * dp - g / 2), style = Stroke(g))
        val tinte = t.suave.copy(alpha = if (estado == 4) 0.4f else 0.8f)
        // Con «Códigos de materia» la tarjeta lleva una barrita corta arriba del nombre
        if (Tema.codigos) drawRoundRect(tinte.copy(alpha = tinte.alpha * 0.6f), Offset(o.x + 6 * dp, o.y + 5 * dp), Size(ancho * 0.28f, 2.5f * dp), CornerRadius(1.25f * dp))
        drawRoundRect(tinte, Offset(o.x + 6 * dp, o.y + (if (Tema.codigos) 11f else 8.5f) * dp), Size(ancho * 0.55f, 3 * dp), CornerRadius(1.5f * dp))
    }
    if (conBarra) {
        val bw = w * 0.62f
        val bh = 26 * dp
        val bx = (w - bw) / 2
        val by = h - bh - 12 * dp
        drawRoundRect(t.panel, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2))
        drawRoundRect(t.borde, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2), style = Stroke(g))
        for (i in 0 until 3) drawCircle(t.apagada, 2.2f * dp, Offset(bx + bh + i * 14 * dp, by + bh / 2))
        drawCircle(colorCarrera, 7 * dp, Offset(bx + bw - bh / 2, by + bh / 2), style = Stroke(2f * dp))
    }
}

/**
 * La vista previa ancha de la página Mapa: 168 dp de alto, radio 24, con el
 * mapa en cuatro columnas y tres filas, sin barra Núcleo (esas opciones no la tocan).
 */
@Composable
fun VistaPreviaMapa(color: ColorCarrera?, modifier: Modifier = Modifier) {
    val contraste = Tema.contraste
    val juego = Tema.juego
    val t = remember(juego, contraste) { tokensDe(juego, contraste) }
    val colorCarrera = color?.let { colorDe(if (juego.claro) it.claro else it.oscuro) } ?: t.suave
    val acento = Tema.acento?.color ?: colorCarrera
    val forma = RoundedCornerShape(24.dp)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(forma)
            .border(1.dp, Paleta.tinta.copy(alpha = 0.1f), forma)
            .semantics { contentDescription = "Vista previa: así se ve tu mapa" },
    ) { previaMapa(t, acento, 4, 3, ESTADOS_ANCHO, false) }
}
