package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.colorArea
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Pantalla 2 (v2, docs/bienvenida-animaciones-v2.md): «Sube una foto y
 * armamos tu horario», contado con las piezas del horario de hoy. Llega la
 * foto de un horario (abstracto a propósito: bloques y líneas, ningún texto
 * que leer), de una de dos maneras (Variantes.foto): una instantánea que sube
 * desde abajo y se asienta, o la galería, donde se elige la miniatura y se
 * pulsa Subir. La línea de lectura la recorre y cada bloque leído toma el
 * color de su materia; los bloques se condensan en un punto por clase, que
 * vuela en arco a su día y florece en la semana en miniatura (el viaje de
 * VueloHorario). Llega la línea de ahora (lunes, 10:40) y arriba se escribe
 * lo que dice el horario: EN CURSO, Sistemas de Costo, cuánto queda y su
 * trayecto. Queda vivo el punto que late.
 *
 * Todo en un Canvas: rectángulos interpolados al dibujar y textos medidos
 * una vez. El reloj solo se lee en el dibujo; nada se recompone por cuadro.
 */

private const val CICLO = 10_000
/** Con las animaciones apagadas o «Reducir movimiento»: la semana hecha y la clase en curso escrita */
private const val QUIETO = 8_200f

/** La historia, en ms del ciclo */
private const val ENTRA = 150f
private const val LEE = 1_750f
private const val LEE_DURA = 800f
private const val CONDENSA = 2_650f
private const val CONDENSA_DURA = 300f
private const val VUELA = 2_950f
private const val VUELO = 620f
private const val ESCALON_VUELO = 45f
private const val SEMANA = 2_900f
private const val FLORECE = 3_850f
private const val FLORECE_DURA = 380f
private const val ESCALON_COLUMNA = 70f
private const val AHORA = 4_500f
private const val EN_CURSO = 5_000f
private const val BARRA = 5_650f
private const val SE_VA = 9_300f
/** El latido del punto «En curso», el de PuntoQueLate: 2600 ms, el 70 % crece y se apaga */
private const val LATIDO_MS = 2_600f

/** La galería: aparece, se elige la miniatura, se pulsa Subir y la miniatura crece hasta la foto */
private const val ELIGE = 700f
private const val PULSA = 1_000f
private const val CRECE = 1_150f
private const val CRECE_DURA = 600f

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val LATIDO = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
/** El viaje de los puntos, el de VueloHorario: arranca y llega despacio */
private val IO = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/** Una materia del horario: su área le da el color del tema */
private class MateriaHorario(private val area: String) {
    val color get() = colorArea(area)
}

private val ESTADISTICA = MateriaHorario("estadistica")
private val INFERENCIA = MateriaHorario("estadistica")
private val COSTO = MateriaHorario("gestion")
private val CIRCUITOS = MateriaHorario("electronica")
private val METODOS = MateriaHorario("ciencias-basicas")
private val TEORIA = MateriaHorario("sistemas")

/** Una materia con sus días y sus horas, como las lee la app de la foto */
private class FilaHorario(val materia: MateriaHorario, val dias: List<Int>, val desde: Int, val hasta: Int)

private val FILAS = listOf(
    FilaHorario(ESTADISTICA, listOf(0, 2), 7, 9),
    FilaHorario(INFERENCIA, listOf(1, 3), 9, 11),
    FilaHorario(COSTO, listOf(0), 10, 12),
    FilaHorario(CIRCUITOS, listOf(1, 3), 13, 15),
    FilaHorario(METODOS, listOf(2), 10, 12),
    FilaHorario(METODOS, listOf(4), 8, 10),
    FilaHorario(TEORIA, listOf(4), 13, 15),
)

/** Ahora: lunes a las 10:40 */
private const val AHORA_MIN = 10 * 60 + 40

/*
 * El plano (390 × 468, en dp del tablero). Arriba, la cabecera del horario
 * con las medidas de Horario.kt; debajo, la semana en miniatura de Semanita
 * (la fila desborda 7 dp por lado, cinco columnas, cada día con 9 de aire),
 * más alta que en la app para que la escena tenga cuerpo.
 */
private const val MX = 24f
private const val W = 342f
private const val FILA_X = MX - 7f
private const val COL = (W + 14f) / 5f
private const val AIRE = 9f
private const val Y_ROTULO = 30f
private const val Y_TITULO = 50f
private const val Y_CUERPO = 102f
private const val Y_TRAYECTO = 142f
private const val Y_SEMANA = 190f
private const val LENTE_Y = 214f
private const val Y_DIAS = 226f
private const val MINI_Y = 251f
private const val MINI_ALTO = 108f
private const val LENTE_ALTO = MINI_Y - LENTE_Y + MINI_ALTO + 8f
private const val ABRE = 7 * 60
private const val CIERRA = 15 * 60
private fun yHora(min: Int) = MINI_Y + MINI_ALTO * (min - ABRE) / (CIERRA - ABRE)
private val Y_AHORA = yHora(AHORA_MIN)

/*
 * La foto. FX, FY y FW × FH son la instantánea con su marco (más ancho abajo,
 * como las de papel); dentro, la imagen de IW × IH: una hoja de horario
 * abstracta, con su cabecera en barras, los días en pastillas, las horas en
 * rayas y los bloques grises en su sitio. La galería termina con la imagen
 * en el mismo sitio, sin marco: los bloques salen de ahí en las dos.
 */
private const val FX = 77f
private const val FY = 30f
private const val FW = 236f
private const val FH = 320f
private const val MARCO = 10f
private const val IX = FX + MARCO
private const val IY = FY + MARCO
private const val IW = FW - 2 * MARCO
private const val IH = FH - MARCO - 40f
private val CENTRO_FOTO = Offset(FW / 2, FH / 2)
/** La rejilla de la imagen: el carril de horas a la izquierda y cinco días */
private const val REJILLA_X = 34f
private const val REJILLA_Y = 52f
private const val DIA_FOTO = (IW - 12f - REJILLA_X) / 5f
private const val HORA_FOTO = (IH - 12f - REJILLA_Y) / 8f

/** Los grises de la imagen: es una foto, no la app, así que no sigue el tema */
private val PAPEL = Color(0xFFDFE2E6)
private val GRIS_TITULO = Color(0xFF9AA0A8)
private val GRIS_SUAVE = Color(0xFFBDC2C8)
private val GRIS_BLOQUE = Color(0xFFC4C8CE)
private val GRIS_RAYA = Color(0xFFCDD1D6)
private val TINTA_FOTO = Color(0xFF2F333A)
/** El marco de la instantánea: papel blanco, un poco más apagado en oscuro para no deslumbrar */
private val MARCO_COLOR get() = porTema(Color(0xFFEDEEF0), Color.White)

/** La galería: el panel, la miniatura elegida (la del centro) y el botón Subir */
private const val GX = 45f
private const val GY = 34f
private const val GW = 300f
private const val GH = 392f
private const val MINI_FOTO = 89.33f
private const val HUECO_MINI = 4f
private const val ELEGIDA = 4
private val VISTO = Path().apply {
    moveTo(-3.5f, 0.2f)
    lineTo(-1f, 2.7f)
    lineTo(3.6f, -2.4f)
}

/** El punto de cada clase en vuelo: un cuadrito de su color, como en VueloHorario */
private const val PUNTO = 9f
/** Los arcos de los vuelos, alternos a cada lado: ninguna ruta pisa a la de al lado */
private val ARCOS = floatArrayOf(0f, 18f, -18f, 30f, -30f, 12f, -12f, 24f, -24f, 6f)

private val HILO = Stroke(1f)
private val ARO = Stroke(1.5f)
private val TRAZO_VISTO = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val ROSA get() = colorArea("gestion")

/*
 * La toma, la misma Hermite monótona de la pantalla 1: cerca de la foto
 * mientras llega y se lee, baja con los puntos hasta la semana y sube a la
 * cabecera cuando se escribe la clase en curso.
 *                      ms     x     y    zoom  incl  giro
 */
private val TOMA = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 195f, 200f, 1.02f, 10f, -1.2f),
        floatArrayOf(1_200f, 195f, 194f, 1.08f, 4f, -0.3f),
        floatArrayOf(2_550f, 195f, 192f, 1.12f, 2f, 0f),
        floatArrayOf(3_700f, 195f, 268f, 1.04f, 6f, 0.4f),
        floatArrayOf(4_700f, 195f, 236f, 1.00f, 4f, 0.3f),
        floatArrayOf(6_600f, 195f, 202f, 1.03f, 2f, 0f),
        floatArrayOf(8_800f, 195f, 200f, 1.05f, 1f, -0.2f),
        floatArrayOf(CICLO.toFloat(), 195f, 200f, 1.02f, 10f, -1.2f),
    ),
)

/** En modo ligero no hay toma: la escena se mira de frente, con la composición final en el centro */
private val CENTRO_FIJO = Offset(195f, 200f)

/** El hueco del tablero entre la escena y el título */
private val HUECO_TABLERO = 68.dp

@Composable
fun PasoHorario(alSeguir: () -> Unit) {
    // Por encima de la bifurcación de pie o acostado: al girar, la escena sigue donde iba
    val reloj = rememberRelojEscena(CICLO, QUIETO, parado = Movimiento.reducido)
    val textos = rememberTextos()
    val sesiones = remember { sesionesDemo() }
    val variante = remember { Variantes.foto }
    // La estela es de la tinta de la foto, que no cambia con el tema: se hace una vez
    val estela = remember { Brush.verticalGradient(listOf(Color.Transparent, TINTA_FOTO.copy(alpha = 0.10f)), startY = -36f, endY = -1f) }
    val ligero = Tema.ligero
    val latido = !Tema.sinBucles
    PasoConEscena("Sube una foto", "y armamos tu horario.", "Continuar", alSeguir, hueco = HUECO_TABLERO) { modifier ->
        EscenaEnBucle(
            reloj, TOMA, arriba = 0.08f, abajo = 0.12f, lados = 0.04f, modifier = modifier,
            fija = if (ligero) CENTRO_FIJO else null,
        ) { t -> pintar(t, textos, sesiones, estela, variante, latido) }
    }
}

/**
 * Una clase: es un bloque gris de la foto que la lectura tiñe de su materia,
 * se condensa en un punto, vuela en arco al centro de su bloque y, cuando ha
 * llegado todo su día, florece hasta el bloque de la semana en miniatura.
 */
private class SesionDemo(j: Int, fila: Int, val dia: Int, desde: Int, hasta: Int) {
    private val materia = FILAS[fila].materia
    val color get() = materia.color
    val esCosto = materia === COSTO
    /** Lo que ya pasó hoy se atenúa cuando llega la línea de ahora, como en MiniDia */
    val esPasada = dia == 0 && hasta * 60 <= AHORA_MIN
    /** El bloque en la imagen de la foto, en sus coordenadas */
    val fx = REJILLA_X + dia * DIA_FOTO + 2.5f
    val fy = REJILLA_Y + (desde - 7) * HORA_FOTO + 1.5f
    val fw = DIA_FOTO - 5f
    val fh = (hasta - desde) * HORA_FOTO - 3f
    /** Cuándo pasa la lectura por su medio */
    val leidaEn = LEE + LEE_DURA * ((fy + fh / 2 - REJILLA_Y) / (8 * HORA_FOTO))
    /** Dónde nace su punto, ya en el plano: el centro del bloque de la foto asentada */
    val x0 = IX + fx + fw / 2
    val y0 = IY + fy + fh / 2
    val bx = FILA_X + dia * COL + AIRE
    val bw = COL - 2 * AIRE
    val by = yHora(desde * 60)
    val bh = yHora(hasta * 60) - by
    val x1 = bx + bw / 2
    val y1 = by + bh / 2
    val saleEn = VUELA + ESCALON_VUELO * j
    val floreceEn = FLORECE + ESCALON_COLUMNA * dia
    val arco = ARCOS[j % ARCOS.size]
    /** La normal de la ruta, hacia donde se curva el arco */
    val nx: Float
    val ny: Float

    init {
        val dx = x1 - x0
        val dy = y1 - y0
        val largo = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        nx = -dy / largo
        ny = dx / largo
    }
}

/** Las diez clases en el orden de la lectura: materia a materia y, dentro de cada una, sus días */
private fun sesionesDemo(): List<SesionDemo> {
    var j = 0
    return FILAS.flatMapIndexed { i, f -> f.dias.map { d -> SesionDemo(j++, i, d, f.desde, f.hasta) } }
}

/** Los textos de la escena, medidos una vez: en cada cuadro solo se pintan */
private class TextosHorario(
    val recientes: TextLayoutResult,
    val subir: TextLayoutResult,
    val enCurso: TextLayoutResult,
    val nombre: TextLayoutResult,
    /** «Quedan », «1 h 20 min» y « · Aula 5»: por piezas, porque el alfa no apaga los colores de un tramo */
    val quedan: List<TextLayoutResult>,
    val desde: TextLayoutResult,
    val am: TextLayoutResult,
    val hasta: TextLayoutResult,
    val pm: TextLayoutResult,
    val semana: TextLayoutResult,
    val suma: TextLayoutResult,
    val dias: List<TextLayoutResult>,
)

@Composable
private fun rememberTextos(): TextosHorario {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val j = jost()
    val i = inter()
    return remember(densidad, j, i) { medir(medidor, densidad, j, i) }
}

private fun medir(medidor: TextMeasurer, d: Density, jost: FontFamily, inter: FontFamily): TextosHorario {
    // En dp y no en sp: son parte del dibujo y escalan con la cámara, no con la letra del sistema
    fun tam(v: Float) = with(d) { v.dp.toSp() }
    fun estilo(familia: FontFamily, tamano: Float, alto: Float? = null, peso: FontWeight = FontWeight.Normal, espacio: TextUnit = TextUnit.Unspecified) =
        TextStyle(
            fontFamily = familia, fontSize = tam(tamano), fontWeight = peso, letterSpacing = espacio,
            lineHeight = if (alto != null) tam(alto) else TextUnit.Unspecified,
            // Como en la web: el interlineado se reparte arriba y abajo
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            fontFeatureSettings = "tnum",
        )
    fun m(texto: String, e: TextStyle) = medidor.measure(texto, e, softWrap = false, density = d)

    // Las letras de Horario.kt (LetrasHorario): rótulo, titular, cuerpo y dato
    val etiqueta = estilo(inter, 11f, 16f, FontWeight.Medium, 0.08.em)
    val cuerpo = estilo(inter, 15f, 20f, espacio = (-0.009).em)
    val dato = estilo(inter, 13f, 16f, espacio = (-0.003).em)
    val meridiano = estilo(inter, 11f)
    return TextosHorario(
        recientes = m("Recientes", estilo(inter, 15f, 20f, FontWeight.Medium, (-0.009).em)),
        subir = m("Subir", estilo(inter, 15f, 20f, FontWeight.Medium, (-0.009).em)),
        enCurso = m("EN CURSO", etiqueta),
        nombre = m("Sistemas de Costo", estilo(jost, 40f, 44f, FontWeight.Light, (-0.02).em)),
        quedan = listOf(m("Quedan ", cuerpo), m("1 h 20 min", cuerpo.copy(fontWeight = FontWeight.Medium)), m(" · Aula 5", cuerpo)),
        desde = m("10:00", dato),
        am = m(" AM", meridiano),
        hasta = m("12:00", dato),
        pm = m(" PM", meridiano),
        semana = m("SEMANA", etiqueta),
        suma = m("10 clases · 20 h · 7 AM – 3 PM", dato),
        dias = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE").map { m(it, etiqueta) },
    )
}

/** Un texto medido con su esquina de arriba en (x, y) del plano; `ancla` 0 a la izquierda, 0,5 al centro, 1 a la derecha */
private fun DrawScope.escribir(t: TextLayoutResult, x: Float, y: Float, color: Color, alfa: Float, ancla: Float = 0f) {
    if (alfa <= 0.004f) return
    val px = 1f / density
    withTransform({
        translate(x, y)
        scale(px, px, Offset.Zero)
    }) { drawText(t, color, Offset(-t.size.width * ancla, 0f), alpha = alfa.coerceIn(0f, 1f)) }
}

/** El alto y el ancho de un texto medido, en unidades del plano */
private fun DrawScope.alto(t: TextLayoutResult) = t.size.height / density
private fun DrawScope.ancho(t: TextLayoutResult) = t.size.width / density

private fun DrawScope.pintar(t: Float, x: TextosHorario, sesiones: List<SesionDemo>, estela: Brush, variante: VarianteFoto, latido: Boolean) {
    // Al final de cada vuelta todo se apaga a la vez: la siguiente arranca de cero, sin salto
    val velo = 1f - Movimiento.ESTANDAR.transform(tramo(t, SE_VA, SE_VA + 700f))
    if (velo <= 0.004f) return
    semana(t, x, velo)
    when (variante) {
        VarianteFoto.INSTANTANEA -> instantanea(t, sesiones, estela, velo)
        VarianteFoto.GALERIA -> galeria(t, x, sesiones, estela, velo)
    }
    for (s in sesiones) clase(t, s, velo)
    lineaDeAhora(t, velo)
    cabecera(t, x, velo, latido)
}

private fun DrawScope.semana(t: Float, x: TextosHorario, velo: Float) {
    val aparece = ESTANDAR.transform(tramo(t, SEMANA, SEMANA + 600f)) * velo
    if (aparece <= 0.004f) return
    val hoy = ESTANDAR.transform(tramo(t, AHORA, AHORA + 420f))
    // SEMANA y lo que suma, como el renglón de Semanita
    val rotulo = Movimiento.ASIENTA.transform(tramo(t, AHORA, AHORA + 560f))
    val dy = (1f - rotulo) * 6f
    escribir(x.semana, MX, Y_SEMANA + dy, Paleta.tenue, rotulo * velo)
    escribir(x.suma, MX + W, Y_SEMANA + dy, Paleta.tenue, rotulo * velo, ancla = 1f)
    // La lente: un velo plano bajo el día elegido
    if (hoy > 0f) drawRoundRect(Paleta.tinta, Offset(FILA_X + 2f, LENTE_Y), Size(COL - 4f, LENTE_ALTO), CornerRadius(13f), alpha = 0.075f * hoy * velo)
    for (d in 0..4) {
        val cx = FILA_X + d * COL + COL / 2
        val r = x.dias[d]
        escribir(r, cx, Y_DIAS, if (d == 0) mezclarColor(Paleta.tenue, Paleta.tinta, hoy) else Paleta.tenue, aparece, ancla = 0.5f)
        // El punto rojo de hoy, a la izquierda del día sin moverlo del centro
        if (d == 0 && hoy > 0f) drawCircle(Paleta.rojo, 2f, Offset(cx - ancho(r) / 2 - 5f, Y_DIAS + 8f), alpha = hoy * velo)
        drawRoundRect(Paleta.tinta, Offset(FILA_X + d * COL + AIRE, MINI_Y), Size(COL - 2 * AIRE, MINI_ALTO), CornerRadius(5f), alpha = 0.04f * aparece)
    }
}


/** Una sombra blanda sin desenfoque: tres capas de negro, cada una más ancha, más baja y más tenue */
private fun DrawScope.sombra(x: Float, y: Float, w: Float, h: Float, radio: Float, alfa: Float) {
    if (alfa <= 0.004f) return
    val fuerza = alfa * porTema(1f, 0.55f)
    for (i in 0..2) {
        val g = i * 5f
        drawRoundRect(
            Color.Black, Offset(x - g, y + 6f + i * 5f - g), Size(w + 2 * g, h + 2 * g), CornerRadius(radio + g),
            alpha = fuerza * (0.20f - i * 0.065f),
        )
    }
}

/**
 * La imagen de la foto, en sus coordenadas (IW × IH): una hoja de horario
 * abstracta. Cabecera en barras, los días en pastillas, las horas en rayas y
 * los bloques grises en su sitio; la lectura baja y cada bloque que pasa toma
 * el color de su materia. Al condensarse, los bloques los dibuja ya `clase`.
 */
private fun DrawScope.imagenHorario(t: Float, sesiones: List<SesionDemo>, estela: Brush, radio: Float, op: Float) {
    drawRoundRect(PAPEL, Offset.Zero, Size(IW, IH), CornerRadius(radio), alpha = op)
    drawRoundRect(GRIS_TITULO, Offset(12f, 12f), Size(84f, 7f), CornerRadius(3.5f), alpha = op)
    drawRoundRect(GRIS_SUAVE, Offset(12f, 25f), Size(52f, 5f), CornerRadius(2.5f), alpha = op)
    drawRoundRect(GRIS_SUAVE, Offset(IW - 42f, 12f), Size(30f, 10f), CornerRadius(5f), alpha = op)
    for (d in 0..4) {
        drawRoundRect(GRIS_SUAVE, Offset(REJILLA_X + d * DIA_FOTO + DIA_FOTO / 2 - 8f, 39f), Size(16f, 4f), CornerRadius(2f), alpha = op)
    }
    for (h in 0..8) {
        val y = REJILLA_Y + h * HORA_FOTO
        drawRect(GRIS_RAYA, Offset(REJILLA_X, y), Size(IW - 12f - REJILLA_X, 1f), alpha = op)
        if (h < 8) drawRoundRect(GRIS_SUAVE, Offset(12f, y + 2f), Size(13f, 3f), CornerRadius(1.5f), alpha = op)
    }
    if (t < CONDENSA) {
        for (s in sesiones) {
            val leida = ESTANDAR.transform(tramo(t, s.leidaEn - 40f, s.leidaEn + 220f))
            drawRoundRect(mezclarColor(GRIS_BLOQUE, s.color, leida), Offset(s.fx, s.fy), Size(s.fw, s.fh), CornerRadius(4f), alpha = op)
        }
    }
    // La línea de lectura: un hilo de tinta con su estela, a paso constante
    val lectura = ESTANDAR.transform(tramo(t, LEE - 150f, LEE + 50f)) *
        (1f - ESTANDAR.transform(tramo(t, LEE + LEE_DURA, LEE + LEE_DURA + 200f))) * op
    if (lectura > 0.004f) {
        val yL = REJILLA_Y + 8 * HORA_FOTO * tramo(t, LEE, LEE + LEE_DURA)
        // La estela está hecha una vez en torno a 0: se lleva con la línea en vez de rehacerla
        withTransform({ translate(0f, yL) }) { drawRect(estela, Offset(0f, -36f), Size(IW, 35f), alpha = lectura) }
        drawRect(TINTA_FOTO, Offset(6f, yL), Size(IW - 12f, 1.5f), alpha = 0.85f * lectura)
    }
}

/** Lo que tarda la foto en irse cuando sus bloques ya se han condensado */
private fun seVaLaFoto(t: Float) = ESTANDAR.transform(tramo(t, CONDENSA + 100f, CONDENSA + 700f))

/**
 * Variante «instantánea»: la foto con su marco de papel sube desde abajo
 * inclinada 10°, como si la soltaras, y se asienta a 4°; antes de leerla se
 * endereza, para que la lectura vaya recta.
 */
private fun DrawScope.instantanea(t: Float, sesiones: List<SesionDemo>, estela: Brush, velo: Float) {
    val entra = Movimiento.ASIENTA.transform(tramo(t, ENTRA, ENTRA + 900f))
    val aparece = ESTANDAR.transform(tramo(t, ENTRA, ENTRA + 350f))
    val endereza = Movimiento.ESTANDAR.transform(tramo(t, LEE - 450f, LEE + 150f))
    val seVa = seVaLaFoto(t)
    val op = aparece * (1f - seVa) * velo
    if (op <= 0.004f) return
    val giro = mezcla(-10f, -4f, entra) * (1f - endereza)
    val esc = mezcla(0.9f, 1f, entra) * (1f - 0.06f * seVa)
    withTransform({
        translate(FX, FY + (1f - entra) * 90f - seVa * 12f)
        rotate(giro, CENTRO_FOTO)
        scale(esc, esc, CENTRO_FOTO)
    }) {
        sombra(0f, 0f, FW, FH, 12f, op)
        drawRoundRect(MARCO_COLOR, Offset.Zero, Size(FW, FH), CornerRadius(12f), alpha = op)
        withTransform({ translate(MARCO, MARCO) }) {
            clipRect(0f, 0f, IW, IH) { imagenHorario(t, sesiones, estela, 3f, op) }
        }
    }
}

/** Las miniaturas de la galería: tres por fila, debajo de «Recientes» */
private fun miniX(k: Int) = GX + 12f + (k % 3) * (MINI_FOTO + HUECO_MINI)
private fun miniY(k: Int) = GY + 48f + (k / 3) * (MINI_FOTO + HUECO_MINI)

/** Las otras fotos de la galería: solo tonos de tinta, que no compitan con la elegida */
private val TONOS = floatArrayOf(0.10f, 0.06f, 0.13f, 0.08f, 0f, 0.11f, 0.07f, 0.12f, 0.09f)

/**
 * Variante «galería»: el panel de Recientes sube, la miniatura del horario se
 * elige (aro y visto de tinta), Subir se aviva y se pulsa, y la miniatura
 * crece hasta la foto mientras el panel se va.
 */
private fun DrawScope.galeria(t: Float, x: TextosHorario, sesiones: List<SesionDemo>, estela: Brush, velo: Float) {
    val sube = Movimiento.ASIENTA.transform(tramo(t, ENTRA, ENTRA + 600f))
    val aparece = ESTANDAR.transform(tramo(t, ENTRA, ENTRA + 400f))
    val crece = Movimiento.FLUIDA.transform(tramo(t, CRECE, CRECE + CRECE_DURA))
    val panel = aparece * (1f - ESTANDAR.transform(tramo(t, CRECE, CRECE + 350f))) * velo
    val dy = (1f - sube) * 40f
    if (panel > 0.004f) {
        drawRoundRect(Paleta.panel, Offset(GX, GY + dy), Size(GW, GH), CornerRadius(24f), alpha = panel)
        drawRoundRect(Paleta.tinta, Offset(GX + 0.5f, GY + dy + 0.5f), Size(GW - 1f, GH - 1f), CornerRadius(24f), style = HILO, alpha = 0.07f * panel)
        drawRoundRect(Paleta.tinta, Offset(GX + GW / 2 - 18f, GY + dy + 8f), Size(36f, 4f), CornerRadius(2f), alpha = 0.2f * panel)
        escribir(x.recientes, GX + 16f, GY + dy + 20f, Paleta.tinta, panel)
        for (k in 0 until 9) {
            if (k == ELEGIDA) continue
            drawRoundRect(Paleta.tinta, Offset(miniX(k), miniY(k) + dy), Size(MINI_FOTO, MINI_FOTO), CornerRadius(6f), alpha = TONOS[k] * panel)
        }
        // Subir: se aviva al elegir y se hunde un momento al pulsarlo
        val activo = ESTANDAR.transform(tramo(t, ELIGE, ELIGE + 250f))
        val hunde = 1f - 0.04f * suave(ventana(t, PULSA, PULSA + 90f, PULSA + 150f, PULSA + 320f))
        val bx = GX + 12f
        val bw = GW - 24f
        val by = GY + dy + GH - 56f
        val centro = Offset(bx + bw / 2, by + 22f)
        withTransform({ scale(hunde, hunde, centro) }) {
            drawRoundRect(Paleta.tinta, Offset(bx, by), Size(bw, 44f), CornerRadius(22f), alpha = mezcla(0.06f, 0.14f, activo) * panel)
            val color = mezclarColor(Paleta.apagada, Paleta.tinta, activo)
            val x0 = centro.x - (14f + 8f + ancho(x.subir)) / 2
            flechaArriba(x0 + 7f, centro.y, color, panel)
            escribir(x.subir, x0 + 22f, centro.y - alto(x.subir) / 2, color, panel)
        }
    }
    // La elegida: de miniatura a foto. Recorta a su caja: cuadrada, solo enseña lo de arriba de la hoja
    val op = aparece * (1f - seVaLaFoto(t)) * velo
    if (op <= 0.004f) return
    val seVa = seVaLaFoto(t)
    val l = mezcla(miniX(ELEGIDA), IX, crece)
    val a = mezcla(miniY(ELEGIDA) + dy, IY, crece) - seVa * 12f
    val w = mezcla(MINI_FOTO, IW, crece) * (1f - 0.06f * seVa)
    val h = mezcla(MINI_FOTO, IH, crece) * (1f - 0.06f * seVa)
    val radio = mezcla(6f, 16f, crece)
    val esc = w / IW
    sombra(l, a, w, h, radio, op * crece)
    clipRect(l, a, l + w, a + h) {
        withTransform({
            translate(l, a)
            scale(esc, esc, Offset.Zero)
        }) { imagenHorario(t, sesiones, estela, radio / esc, op) }
    }
    // El aro y el visto de la elección, que se van al crecer
    val elegida = Movimiento.ASIENTA.transform(tramo(t, ELIGE, ELIGE + 300f)) * (1f - ESTANDAR.transform(tramo(t, CRECE, CRECE + 200f))) * op
    if (elegida > 0.004f) {
        drawRoundRect(Paleta.tinta, Offset(l - 3f, a - 3f), Size(w + 6f, h + 6f), CornerRadius(radio + 3f), style = ARO, alpha = elegida)
        val c = Offset(l + w - 12f, a + 12f)
        drawCircle(Paleta.tinta, 9f * (0.6f + 0.4f * elegida), c, alpha = elegida)
        withTransform({ translate(c.x, c.y) }) { drawPath(VISTO, Paleta.lienzo, alpha = elegida, style = TRAZO_VISTO) }
    }
}

/** La flecha de subir: un asta y su punta, en la rejilla de 14 */
private fun DrawScope.flechaArriba(cx: Float, cy: Float, color: Color, alfa: Float) {
    drawLine(color, Offset(cx, cy + 6f), Offset(cx, cy - 6f), strokeWidth = 1.8f, cap = StrokeCap.Round, alpha = alfa)
    drawLine(color, Offset(cx - 5f, cy - 1f), Offset(cx, cy - 6f), strokeWidth = 1.8f, cap = StrokeCap.Round, alpha = alfa)
    drawLine(color, Offset(cx + 5f, cy - 1f), Offset(cx, cy - 6f), strokeWidth = 1.8f, cap = StrokeCap.Round, alpha = alfa)
}

/** Una clase: bloque de la foto que se condensa en un punto, vuela y florece en su día */
private fun DrawScope.clase(t: Float, s: SesionDemo, velo: Float) {
    if (t < CONDENSA) return
    val color = s.color
    val florece = Movimiento.ESTANDAR.transform(tramo(t, s.floreceEn, s.floreceEn + FLORECE_DURA))
    if (florece <= 0f) {
        val p = IO.transform(tramo(t, s.saleEn, s.saleEn + VUELO))
        if (p <= 0f) {
            // Aún en la foto: el bloque se recoge hasta su punto
            val c = Movimiento.ESTANDAR.transform(tramo(t, CONDENSA, CONDENSA + CONDENSA_DURA))
            val w = mezcla(s.fw, PUNTO, c)
            val h = mezcla(s.fh, PUNTO, c)
            drawRoundRect(color, Offset(s.x0 - w / 2, s.y0 - h / 2), Size(w, h), CornerRadius(mezcla(4f, 2.5f, c)), alpha = velo)
            return
        }
        val arco = s.arco * sin(PI.toFloat() * p)
        val px = mezcla(s.x0, s.x1, p) + s.nx * arco
        val py = mezcla(s.y0, s.y1, p) + s.ny * arco
        drawRoundRect(color, Offset(px - PUNTO / 2, py - PUNTO / 2), Size(PUNTO, PUNTO), CornerRadius(2.5f), alpha = velo)
        return
    }
    // Del punto al bloque de MiniDia: su color al 58 %, radio 3
    val l = mezcla(s.x1 - PUNTO / 2, s.bx, florece)
    val a = mezcla(s.y1 - PUNTO / 2, s.by, florece)
    var alfa = mezcla(1f, 0.58f, florece)
    if (s.esPasada) alfa = mezcla(alfa, 0.3f, ESTANDAR.transform(tramo(t, AHORA + 250f, AHORA + 650f)))
    // La de ahora se enciende un momento cuando la cabecera la nombra: de ahí sale lo que se escribe arriba
    if (s.esCosto) alfa += 0.32f * suave(ventana(t, EN_CURSO, EN_CURSO + 300f, EN_CURSO + 700f, EN_CURSO + 1_300f))
    drawRoundRect(
        color, Offset(l, a), Size(mezcla(PUNTO, s.bw, florece), mezcla(PUNTO, s.bh, florece)),
        CornerRadius(mezcla(2.5f, 3f, florece)), alpha = alfa * velo,
    )
}

/** La línea de ahora sobre hoy: se traza de izquierda a derecha, con su punto, por encima de los bloques */
private fun DrawScope.lineaDeAhora(t: Float, velo: Float) {
    val f = Movimiento.ESTANDAR.transform(tramo(t, AHORA + 150f, AHORA + 650f))
    if (f <= 0f) return
    val x0 = FILA_X + AIRE - 3f
    drawRect(Paleta.rojo, Offset(x0, Y_AHORA - 0.5f), Size((COL - 2 * AIRE + 6f) * f, 1f), alpha = velo)
    drawCircle(Paleta.rojo, 2.5f, Offset(x0 - 1f + 2.5f, Y_AHORA), alpha = velo * f)
}

/** La cabecera del horario en curso, línea a línea: EN CURSO con su punto, el nombre, cuánto queda y el trayecto */
private fun DrawScope.cabecera(t: Float, x: TextosHorario, velo: Float, latido: Boolean) {
    fun linea(en: Float) = Movimiento.ASIENTA.transform(tramo(t, en, en + 560f))

    val l1 = linea(EN_CURSO + 50f)
    if (l1 > 0f) {
        val dy = (1f - l1) * 8f
        val punto = Offset(MX + 3.5f, Y_ROTULO + 8f + dy)
        // El latido de PuntoQueLate; en ligero o con «Reducir movimiento» el punto se queda quieto
        if (latido && t >= EN_CURSO + 600f) {
            val q = LATIDO.transform((((t - EN_CURSO - 600f) % LATIDO_MS) / LATIDO_MS / 0.7f).coerceAtMost(1f))
            drawCircle(ROSA, 3.5f * (1f + 2.4f * q), punto, alpha = 0.5f * (1f - q) * l1 * velo)
        }
        drawCircle(ROSA, 3.5f, punto, alpha = l1 * velo)
        escribir(x.enCurso, MX + 15f, Y_ROTULO + dy, Paleta.tinta, l1 * velo)
    }
    val l2 = linea(EN_CURSO + 120f)
    escribir(x.nombre, MX, Y_TITULO + (1f - l2) * 8f, Paleta.tinta, l2 * velo)
    val l3 = linea(EN_CURSO + 190f)
    var xq = MX
    x.quedan.forEachIndexed { i, q ->
        escribir(q, xq, Y_CUERPO + (1f - l3) * 8f, if (i == 1) Paleta.tinta else Paleta.suave, l3 * velo)
        xq += ancho(q)
    }
    val l4 = linea(EN_CURSO + 260f)
    if (l4 > 0f) trayecto(t, x, Y_TRAYECTO + (1f - l4) * 8f, l4 * velo)
}

/** El trayecto de Horario.kt: de 10:00 AM a 12:00 PM, de un punto a un aro, y la cabeza llega a las 10:40 */
private fun DrawScope.trayecto(t: Float, x: TextosHorario, y: Float, op: Float) {
    escribir(x.desde, MX, y, Paleta.suave, op)
    val xAm = MX + ancho(x.desde)
    // El meridiano, más pequeño y tenue, en la línea base de la hora
    escribir(x.am, xAm, y + (x.desde.firstBaseline - x.am.firstBaseline) / density, Paleta.tenue, op)
    val xPm = MX + W - ancho(x.pm)
    escribir(x.pm, xPm, y + (x.hasta.firstBaseline - x.pm.firstBaseline) / density, Paleta.tenue, op)
    val xHasta = xPm - ancho(x.hasta)
    escribir(x.hasta, xHasta, y, Paleta.suave, op)
    val x0 = xAm + ancho(x.am) + 13f
    val x1 = xHasta - 13f
    val yc = y + alto(x.desde) / 2
    val rosa = ROSA
    drawLine(Paleta.tinta, Offset(x0, yc), Offset(x1, yc), strokeWidth = 2f, cap = StrokeCap.Round, alpha = 0.15f * op)
    val avance = 40f / 120f * ESTANDAR.transform(tramo(t, BARRA, BARRA + 900f))
    val xc = x0 + (x1 - x0) * avance
    if (avance > 0f) drawLine(rosa, Offset(x0, yc), Offset(xc, yc), strokeWidth = 2f, alpha = op)
    drawCircle(rosa, 3.5f, Offset(x0 - 1f + 3.5f, yc), alpha = op)
    val fin = Offset(x1 + 1f - 3.5f, yc)
    drawCircle(Paleta.lienzo, 3.5f, fin, alpha = op)
    drawCircle(rosa, 2.75f, fin, style = ARO, alpha = op)
    val cabeza = Movimiento.ASIENTA.transform(tramo(t, BARRA, BARRA + 300f)) * op
    if (cabeza > 0f) {
        drawCircle(Paleta.lienzo, 8.5f, Offset(xc, yc), alpha = cabeza)
        drawCircle(rosa, 5.5f, Offset(xc, yc), alpha = cabeza)
    }
}
