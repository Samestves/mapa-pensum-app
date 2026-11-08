package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.cos
import kotlin.math.sin

/*
 * Pantalla 2: una foto al horario y tu semana lista. Llega la captura de un
 * horario cualquiera (gris, sin marca), una línea la lee fila a fila, cada
 * clase vuela primero a su día y luego a su hora, y queda la semana con el
 * estilo de HorarioSemana. Cruza la línea de ahora (lunes, 10:40) y la clase
 * que va se abre en la tarjeta «En curso» de Horario.kt.
 *
 * Todo en un Canvas: rectángulos interpolados al dibujar y textos medidos
 * una vez. El reloj solo se lee en el dibujo; nada se recompone por cuadro.
 */

private const val CICLO = 10_000
/** Con las animaciones apagadas: la tarjeta abierta y la barra ya en 10:40 */
private const val QUIETO = 8_000f

/** La historia, en ms del ciclo (Onb2 aprobado) */
private const val ENTRA = 150f
private const val LEE = 1_150f
private const val LEE_DURA = 1_500f
private const val SUELTA = 2_850f
private const val AL_DIA = 3_000f
private const val A_LA_HORA = 3_400f
private const val AHORA_EN = 5_000f
private const val FOCO = 5_800f
private const val ABRE = 5_900f
private const val BARRA = 6_700f
private const val SE_VA = 9_100f

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val LATIDO = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private fun durante(t: Float, desde: Float, dura: Float) = ((t - desde) / dura).coerceIn(0f, 1f)
private fun mezclar(a: Float, b: Float, f: Float) = a + (b - a) * f

/** Una materia del horario: como sale en la captura, cómo se abrevia en su bloque y su área, que le da el color del tema */
private class MateriaHorario(val nombre: String, val bloque: List<String>, private val area: String) {
    val color get() = colorArea(area)
}

private val ESTADISTICA = MateriaHorario("ESTADÍSTICA II", listOf("Estad. II"), "estadistica")
private val INFERENCIA = MateriaHorario("INFERENCIA Y DISEÑO", listOf("Infer."), "estadistica")
private val COSTO = MateriaHorario("SISTEMAS DE COSTO", listOf("Sist. de", "Costo"), "gestion")
private val CIRCUITOS = MateriaHorario("CIRCUITOS Y SISTEMAS", listOf("Circuitos"), "electronica")
private val METODOS = MateriaHorario("MÉTODOS NUMÉRICOS", listOf("Métodos", "Num."), "ciencias-basicas")
private val TEORIA = MateriaHorario("TEORÍA DE SISTEMAS", listOf("Teoría", "Sistemas"), "sistemas")

/** Una fila de la captura, tal cual: horas en 24 y aulas abreviadas. La app la pasa a «Aula 12» */
private class FilaHorario(val materia: MateriaHorario, val dias: String, val hora: String, val aula: String, val aulaApp: String)

private val FILAS = listOf(
    FilaHorario(ESTADISTICA, "LU  MI", "07:00 – 09:00", "A-12", "Aula 12"),
    FilaHorario(INFERENCIA, "MA  JU", "09:00 – 11:00", "LAB-3", "Lab 3"),
    FilaHorario(COSTO, "LU", "10:00 – 12:00", "A-5", "Aula 5"),
    FilaHorario(CIRCUITOS, "MA  JU", "13:00 – 15:00", "A-8", "Aula 8"),
    FilaHorario(METODOS, "MI", "10:00 – 12:00", "A-2", "Aula 2"),
    FilaHorario(METODOS, "VI", "08:00 – 10:00", "A-2", "Aula 2"),
    FilaHorario(TEORIA, "VI", "13:00 – 15:00", "A-9", "Aula 9"),
)

/** Las diez sesiones en el orden en que se leen: fila, día, desde y hasta (en horas) */
private val SESIONES = listOf(
    intArrayOf(0, 0, 7, 9), intArrayOf(0, 2, 7, 9), intArrayOf(1, 1, 9, 11), intArrayOf(1, 3, 9, 11), intArrayOf(2, 0, 10, 12),
    intArrayOf(3, 1, 13, 15), intArrayOf(3, 3, 13, 15), intArrayOf(4, 2, 10, 12), intArrayOf(5, 4, 8, 10), intArrayOf(6, 4, 13, 15),
)

/** La semana en el plano, como HorarioSemana: riel de horas, cinco columnas de 64 y 44 dp por hora desde las 7 */
private const val GX = 54f
private const val GY = 28f
private const val COL = 64f
private const val HORA = 44f
private fun yDe(h: Float) = GY + (h - 7f) * HORA
private val Y_AHORA = yDe(10f + 40f / 60f)
private const val ANCHO_SEMANA = 5 * COL

/** La foto en el plano, centrada sobre la semana */
private const val FX = 39f
private const val FY = 87f
private const val FW = 312f
private const val FH = 236f
private val CENTRO_FOTO = Offset(FW / 2, FH / 2)
private val PAPEL = Color(0xFFD3D6DB)
private val TINTA_PAPEL = Color(0xFF2F333A)

/** La tarjeta «En curso» de Horario.kt, alineada a la columna del lunes */
private const val TX = GX
private const val TY = 122f
private const val TW = 320f
private const val TH = 164f
private val ROSA get() = colorArea("gestion")
/** La tarjeta y el fondo de los bloques: grafito en oscuro, papel blanco en claro */
private val PANEL_TARJETA get() = porTema(Color(0xFF141518), Paleta.panel)
private val FONDO_BLOQUE get() = porTema(Color(0xFF101114), Color.White)

/*
 * La toma: la misma Hermite monótona de la pantalla 1. Cerca de la foto
 * para leerla, un paso atrás para ver la semana entera y de nuevo adentro
 * cuando la clase de ahora se abre. Nunca más lejos de 0,9.
 *                      ms     x     y    zoom  incl  giro
 */
private val TOMA = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 195f, 205f, 0.96f, 12f, -1.6f),
        floatArrayOf(1_150f, 195f, 205f, 1.05f, 4f, -0.3f),
        floatArrayOf(2_700f, 195f, 206f, 1.07f, 2f, 0f),
        floatArrayOf(4_300f, 195f, 207f, 0.92f, 8f, 0.5f),
        floatArrayOf(5_600f, 195f, 206f, 0.91f, 7f, 0.6f),
        floatArrayOf(7_300f, 214f, 204f, 1.00f, 2f, 0f),
        floatArrayOf(8_900f, 214f, 204f, 1.03f, 1f, -0.2f),
        floatArrayOf(CICLO.toFloat(), 195f, 205f, 0.96f, 12f, -1.6f),
    ),
)

/** El hueco del tablero entre la escena y el título */
private val HUECO_TABLERO = 68.dp

@Composable
fun PasoHorario(alSeguir: () -> Unit) {
    val reloj = rememberRelojEscena(CICLO, QUIETO)
    val textos = rememberTextos()
    val sesiones = remember { SESIONES.mapIndexed { j, s -> SesionHorario(j, s) } }
    // Sus pinceles llevan los colores del tema: se rehacen si cambia
    val tarjeta = remember(Tema.claro) { TarjetaEnCurso() }
    PasoConEscena("Una foto al horario,", "tu semana lista.", "Continuar", alSeguir, hueco = HUECO_TABLERO) { modifier ->
        // Arriba y abajo se funde menos que en la pantalla 1: aquí hay que leer
        EscenaEnBucle(reloj, TOMA, arriba = 0.09f, abajo = 0.14f, lados = 0.06f, modifier = modifier) { t ->
            pintar(t, textos, sesiones, tarjeta)
        }
    }
}

/** Un pincel que solo se rehace cuando cambia lo que lo define: quieto, no reserva nada por cuadro */
private class PincelGuardado {
    private var a = Float.NaN
    private var b = Float.NaN
    private var c = Float.NaN
    private var pincel: Brush? = null

    fun de(a: Float, b: Float, c: Float, crear: () -> Brush): Brush {
        val p = pincel
        if (p != null && a == this.a && b == this.b && c == this.c) return p
        this.a = a
        this.b = b
        this.c = c
        return crear().also { pincel = it }
    }
}

/**
 * Una clase: sale de su fila de la foto (296 × 20) y llega a su celda (60 ×
 * lo que dura) en dos tiempos, como lee la app: primero el día (x y ancho),
 * luego la hora (y y alto). Así nadie se cruza y se entiende.
 */
private class SesionHorario(val j: Int, s: IntArray) {
    val fila = FILAS[s[0]]
    val color = fila.materia.color
    val esCosto = fila.materia === COSTO
    /** Lo que ya pasó hoy se atenúa como en la app cuando llega la línea de ahora */
    val esPasada = s[1] == 0 && s[3] <= 10f + 40f / 60f
    // Una fila con dos días se parte en dos mitades lado a lado: nacen sin pisarse y ya dicen «dos días»
    private val gemelas = SESIONES.filter { it[0] == s[0] }
    private val cual = gemelas.indexOfFirst { it[1] == s[1] }
    val w0 = 296f / gemelas.size
    val x0 = FX + 8f + cual * w0
    val y0 = FY + 60f + 24f * s[0] + 1f
    val x1 = GX + s[1] * COL + 2f
    val y1 = yDe(s[2].toFloat()) + 1.5f
    val w1 = COL - 4f
    val h1 = (s[3] - s[2]) * HORA - 3f
    val fondo = PincelGuardado()
}

/** Los pinceles de la tarjeta, que cambian solo mientras crece */
private class TarjetaEnCurso {
    val bloque = PincelGuardado()
    val cristal = PincelGuardado()
    val luz = Brush.radialGradient(listOf(ROSA.copy(alpha = 0.13f), ROSA.copy(alpha = 0f)), Offset(TX, TY), 215f)
    /** El filo de luz de 1 dp, en diagonal a 148° como el cristal de la app */
    val filo: Brush = run {
        val a = 148.0 * kotlin.math.PI / 180.0
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        val largo = TW * kotlin.math.abs(dx) + TH * kotlin.math.abs(dy)
        val c = Offset(TX + TW / 2, TY + TH / 2)
        val d = Offset(dx * largo / 2, dy * largo / 2)
        // De tinta: luz en oscuro y, a la mitad, una sombra fina en claro
        val f = porTema(1f, 0.55f)
        Brush.linearGradient(
            0f to Paleta.tinta.copy(alpha = 0.26f * f), 0.38f to Paleta.tinta.copy(alpha = 0.06f * f),
            0.58f to Paleta.tinta.copy(alpha = 0f), 1f to Paleta.tinta.copy(alpha = 0.10f * f),
            start = c - d, end = c + d,
        )
    }
    val hoy = Brush.verticalGradient(listOf(Paleta.tinta.copy(alpha = 0.035f), Paleta.tinta.copy(alpha = 0.015f)), GY, GY + 8 * HORA)
}

/** Los textos de la escena, medidos una vez: en cada cuadro solo se pintan */
private class TextosHorario(
    val horas: List<TextLayoutResult>,
    val dias: List<TextLayoutResult>,
    val chip: TextLayoutResult,
    val titulo: TextLayoutResult,
    val periodo: TextLayoutResult,
    val columnas: List<TextLayoutResult>,
    /** Por fila: nombre, días, hora y aula */
    val filas: List<List<TextLayoutResult>>,
    val bloques: Map<MateriaHorario, List<TextLayoutResult>>,
    val aulas: List<TextLayoutResult>,
    val enCurso: TextLayoutResult,
    val nombreCosto: TextLayoutResult,
    /** «Quedan », «1 h 20 min» y « · Aula 5»: por piezas, para que el fundido sea igual en todas */
    val quedan: List<TextLayoutResult>,
    val desde: TextLayoutResult,
    val hasta: TextLayoutResult,
    val pm: TextLayoutResult,
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
    // En dp y no en sp: son parte del dibujo y escalan con la cámara
    fun tam(v: Float) = with(d) { v.dp.toSp() }
    fun estilo(familia: FontFamily, tamano: Float, alto: Float? = null, peso: FontWeight = FontWeight.Normal, espacio: TextUnit = TextUnit.Unspecified, cifras: Boolean = false) =
        TextStyle(
            fontFamily = familia, fontSize = tam(tamano), fontWeight = peso, letterSpacing = espacio,
            lineHeight = if (alto != null) tam(alto) else TextUnit.Unspecified,
            // Como en la web: el interlineado se reparte arriba y abajo
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            fontFeatureSettings = if (cifras) "tnum" else null,
        )
    fun m(texto: String, e: TextStyle) = medidor.measure(texto, e, softWrap = false, density = d)

    val pequeno = estilo(inter, 11f, 14f, cifras = true)
    val dia = estilo(jost, 11f, 14f, FontWeight.Medium, 0.14.em)
    val papel = estilo(inter, 9.5f, peso = FontWeight.Medium, espacio = 0.02.em)
    val papelDato = estilo(inter, 9.5f, cifras = true)
    val bloque = estilo(jost, 12f, 15f, FontWeight.Medium)
    val texto13 = estilo(inter, 13f, 16f, cifras = true)
    return TextosHorario(
        horas = (0..8).map { i -> val h = 7 + i; m("${(h + 11) % 12 + 1} ${if (h < 12) "AM" else "PM"}", pequeno) },
        dias = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE").map { m(it, dia) },
        chip = m("10:40", pequeno.copy(fontWeight = FontWeight.Medium)),
        titulo = m("Horario de clases", estilo(inter, 11f, 14f, FontWeight.SemiBold)),
        periodo = m("Período 2026-I", estilo(inter, 9.5f, 12f)),
        columnas = listOf("ASIGNATURA", "DÍAS", "HORA", "AULA").map { m(it, estilo(inter, 8f, peso = FontWeight.SemiBold, espacio = 0.06.em)) },
        filas = FILAS.map { listOf(m(it.materia.nombre, papel), m(it.dias, papelDato), m(it.hora, papelDato), m(it.aula, papelDato)) },
        bloques = FILAS.map { it.materia }.distinct().associateWith { mat -> mat.bloque.map { m(it, bloque) } },
        aulas = FILAS.map { m(it.aulaApp, estilo(inter, 11f, 14f)) },
        enCurso = m("EN CURSO", dia),
        nombreCosto = m("Sistemas de Costo", estilo(jost, 36f, peso = FontWeight.Light, espacio = (-0.02).em)),
        quedan = listOf(m("Quedan ", texto13), m("1 h 20 min", texto13.copy(fontWeight = FontWeight.Medium)), m(" · Aula 5", texto13)),
        desde = m("10:00", texto13),
        hasta = m("12:00", texto13),
        pm = m("PM", estilo(inter, 11f)),
    )
}

/** Un texto medido con su esquina de arriba en (x, y) del plano; `ancla` 0 a la izquierda, 0,5 al centro, 1 a la derecha */
private fun DrawScope.texto(t: TextLayoutResult, x: Float, y: Float, color: Color, alfa: Float, ancla: Float = 0f) {
    if (alfa <= 0.004f) return
    val px = 1f / density
    withTransform({
        translate(x, y)
        scale(px, px, Offset.Zero)
    }) { drawText(t, color, Offset(-t.size.width * ancla, 0f), alpha = alfa.coerceIn(0f, 1f)) }
}

/** El alto de un texto medido, en unidades del plano */
private fun DrawScope.alto(t: TextLayoutResult) = t.size.height / density
private fun DrawScope.ancho(t: TextLayoutResult) = t.size.width / density

private fun DrawScope.pintar(t: Float, x: TextosHorario, sesiones: List<SesionHorario>, tarjeta: TarjetaEnCurso) {
    // Al final de cada vuelta todo se apaga a la vez: la siguiente arranca de cero, sin salto
    val velo = 1f - Movimiento.ESTANDAR.transform(durante(t, SE_VA, 700f))
    val foco = ESTANDAR.transform(durante(t, FOCO, 600f))
    val fotoSeVa = ESTANDAR.transform(durante(t, SUELTA, 500f))
    semana(t, x, tarjeta, velo, foco)
    foto(t, x, fotoSeVa)
    for (s in sesiones) bloque(t, s, x, velo, foco, fotoSeVa)
    enCurso(t, x, sesiones.first { it.esCosto }, tarjeta, velo)
}

/** La rejilla de la semana, que aparece debajo mientras las clases buscan su día, y la línea de ahora */
private fun DrawScope.semana(t: Float, x: TextosHorario, tarjeta: TarjetaEnCurso, velo: Float, foco: Float) {
    val op = ESTANDAR.transform(durante(t, 3_100f, 900f)) * velo * (1f - 0.5f * foco)
    if (op <= 0.004f) return
    val lineaAhora = Movimiento.ESTANDAR.transform(durante(t, AHORA_EN, 650f))
    val alto = 8 * HORA
    // Hoy se distingue apenas, cuando llega la línea de ahora
    if (lineaAhora > 0f) drawRect(tarjeta.hoy, Offset(GX, GY), Size(COL, alto), alpha = lineaAhora * op)
    for (i in 1..4) drawRect(Paleta.tinta.copy(alpha = 0.05f * op), Offset(GX + i * COL, GY), Size(1f, alto))
    // «11 AM» queda bajo la etiqueta de ahora: se aparta antes de que llegue, como en la app. Y todas se
    // retiran con el foco: medio cortadas por el borde de la tarjeta parecerían un error
    val apartaOnce = 1f - ESTANDAR.transform(durante(t, AHORA_EN, 250f))
    for (i in 0..8) {
        val y = GY + i * HORA
        drawRect(Paleta.tinta.copy(alpha = 0.07f * op), Offset(GX, y), Size(ANCHO_SEMANA, 1f))
        val rotulo = (if (7 + i == 11) apartaOnce else 1f) * (1f - foco)
        texto(x.horas[i], 46f, y - 7f, Paleta.tenue, rotulo * op, ancla = 1f)
    }
    for (d in 0..4) {
        val r = x.dias[d]
        val cx = GX + d * COL + COL / 2
        texto(r, cx, 4f, if (d == 0) Paleta.tinta else Paleta.tenue, op, ancla = 0.5f)
        // El punto rojo de hoy, delante del día
        if (d == 0 && lineaAhora > 0f) drawCircle(Paleta.rojo.copy(alpha = lineaAhora * op), 2f, Offset(cx - ancho(r) / 2 - 5f, 4f + 7f))
    }
    // La línea de ahora cruza la semana tenue y se marca fuerte sobre hoy
    if (lineaAhora > 0f) drawRect(Paleta.rojo.copy(alpha = 0.45f * op), Offset(GX, Y_AHORA), Size(ANCHO_SEMANA * lineaAhora, 1f))
    val grueso = Movimiento.ESTANDAR.transform(durante(t, AHORA_EN + 120f, 500f))
    if (grueso > 0f) drawRect(Paleta.rojo.copy(alpha = op), Offset(GX, Y_AHORA - 0.5f), Size(COL * grueso, 2f))
    // La etiqueta de ahora se retira cuando la tarjeta toma la palabra: no debe asomar cortada por su borde
    val chip = Movimiento.ASIENTA.transform(durante(t, AHORA_EN + 250f, 400f)) * (1f - foco) * op
    if (chip > 0.004f) {
        drawRoundRect(Paleta.rojo.copy(alpha = chip), Offset(10f, Y_AHORA - 8f), Size(36f, 16f), CornerRadius(8f))
        texto(x.chip, 28f, Y_AHORA - 8f + (16f - alto(x.chip)) / 2, Color.White, chip, ancla = 0.5f)
    }
}

/** La foto: entra con 3° de giro y un leve hundimiento, se endereza, se lee y se va hacia atrás */
private fun DrawScope.foto(t: Float, x: TextosHorario, seVa: Float) {
    val entra = Movimiento.ASIENTA.transform(durante(t, ENTRA, 600f))
    val endereza = Movimiento.ESTANDAR.transform(durante(t, ENTRA, 1_000f))
    val op = entra * (1f - seVa)
    if (op <= 0.004f) return
    val esc = (0.97f + 0.03f * endereza) * (1f - 0.05f * seVa)
    withTransform({
        translate(FX, FY + (1f - entra) * 28f)
        rotate(-3f * (1f - endereza), CENTRO_FOTO)
        scale(esc, esc, CENTRO_FOTO)
    }) {
        drawRoundRect(PAPEL.copy(alpha = op), size = Size(FW, FH), cornerRadius = CornerRadius(6f))
        texto(x.titulo, 14f, 15f, TINTA_PAPEL, op)
        texto(x.periodo, FW - 14f, 16f, Color(0xFF6B7079), op, ancla = 1f)
        drawRect(Color(0xFFC2C6CD).copy(alpha = op), Offset(0f, 40f), Size(FW, 20f))
        val xs = floatArrayOf(14f, 154f, 198f, 270f)
        x.columnas.forEachIndexed { i, c -> texto(c, xs[i], 40f + (20f - alto(c)) / 2, Color(0xFF4A4F57), op) }
        // Cada fila que pasa la lectura toma el color de su materia; en SUELTA se la llevan las clases, que nacen encima
        FILAS.forEachIndexed { i, f ->
            val y = 60f + 24f * i
            drawRect(Color(0xFFC3C7CD).copy(alpha = op), Offset(0f, y), Size(FW, 1f))
            val barrido = ESTANDAR.transform(durante(t, leidaEn(i) - 60f, 320f))
            if (barrido > 0f && t < SUELTA) {
                val w = 296f * barrido
                drawRoundRect(f.materia.color.copy(alpha = 0.26f * op), Offset(8f, y + 1f), Size(w, 20f), CornerRadius(4f))
                clipRect(8f, y + 1f, 8f + minOf(3f, w), y + 21f) {
                    drawRoundRect(f.materia.color.copy(alpha = op), Offset(8f, y + 1f), Size(w, 20f), CornerRadius(4f))
                }
            }
            x.filas[i].forEachIndexed { k, r -> texto(r, xs[k], y + (22f - alto(r)) / 2, TINTA_PAPEL, op) }
        }
        // La línea de lectura: un filo azul con su estela, a velocidad constante como un escáner
        val lectura = ESTANDAR.transform(durante(t, LEE - 120f, 200f)) * (1f - ESTANDAR.transform(durante(t, LEE + LEE_DURA, 200f))) * op
        if (lectura > 0f) {
            val yL = 60f + 168f * durante(t, LEE, LEE_DURA)
            drawRect(
                Brush.verticalGradient(listOf(Paleta.azul.copy(alpha = 0f), Paleta.azul.copy(alpha = 0.18f)), yL - 37f, yL - 1f),
                Offset(8f, yL - 37f), Size(FW - 16f, 36f), alpha = lectura,
            )
            drawRoundRect(Paleta.azul.copy(alpha = lectura), Offset(8f, yL - 1f), Size(FW - 16f, 2f), CornerRadius(1f))
        }
    }
}

/** Cuándo pasa la lectura por el medio de cada fila */
private fun leidaEn(i: Int) = LEE + LEE_DURA * (24f * i + 12f) / 168f

private fun DrawScope.bloque(t: Float, s: SesionHorario, x: TextosHorario, velo: Float, foco: Float, papelFuera: Float) {
    if (t < SUELTA) return
    val fuerte = if (s.esCosto) ESTANDAR.transform(durante(t, AHORA_EN + 250f, 400f)) else 0f
    val pasada = if (s.esPasada) 1f - 0.45f * ESTANDAR.transform(durante(t, AHORA_EN + 250f, 400f)) else 1f
    val apagada = if (s.esCosto) 1f else 1f - 0.75f * foco
    // La de ahora se va al abrirse: desde ese instante la lleva la tarjeta
    val op = velo * pasada * apagada * (if (s.esCosto && t >= ABRE) 0f else 1f)
    if (op <= 0.004f) return
    val pDia = Movimiento.ESTANDAR.transform(durante(t, AL_DIA + 60f * s.j, 600f))
    val enHora = A_LA_HORA + 75f * s.j
    val pHora = Movimiento.ESTANDAR.transform(durante(t, enHora, 800f))
    val bx = mezclar(s.x0, s.x1, pDia)
    val bw = mezclar(s.w0, s.w1, pDia)
    val by = mezclar(s.y0, s.y1, pHora)
    val bh = mezclar(20f, s.h1, pHora)
    val radio = CornerRadius(mezclar(4f, 8f, pHora))
    val aterriza = ESTANDAR.transform(durante(t, enHora + 440f, 360f))
    val c = s.color
    val o = Offset(bx, by)
    val tam = Size(bw, bh)
    // En el papel la fila es un tinte; sin papel debajo, el tinte sube para seguir viéndose
    if (aterriza < 1f) drawRoundRect(c.copy(alpha = (0.26f + 0.2f * papelFuera) * (1f - aterriza) * op), o, tam, radio)
    if (aterriza > 0f) {
        // Ya en su celda: el cristal teñido de HorarioSemana, más intenso el de la clase que va
        drawRoundRect(FONDO_BLOQUE.copy(alpha = 0.4f * aterriza * op), o, tam, radio)
        val fondo = s.fondo.de(by, bh, fuerte) {
            Brush.verticalGradient(listOf(c.copy(alpha = 0.26f + 0.14f * fuerte), c.copy(alpha = 0.14f + 0.12f * fuerte)), by, by + bh)
        }
        drawRoundRect(fondo, o, tam, radio, alpha = aterriza * op)
        drawRect(Color.White.copy(alpha = 0.08f * aterriza * op), Offset(bx + radio.x / 2, by), Size(bw - radio.x, 1f))
        drawRoundRect(c.copy(alpha = aterriza * (0.28f + 0.27f * fuerte) * op), Offset(bx + 0.5f, by + 0.5f), Size(bw - 1f, bh - 1f), radio, style = Stroke(1f))
    }
    // La barra de su color a la izquierda, recortada por las esquinas del bloque
    clipRect(bx, by, bx + 3f, by + bh) { drawRoundRect(c.copy(alpha = op), o, tam, radio) }
    val letras = ESTANDAR.transform(durante(t, enHora + 560f, 300f)) * op
    if (letras > 0.004f) clipRect(bx, by, bx + bw, by + bh) { rotuloBloque(x, s, bx, by, letras) }
}

/** El nombre abreviado y el aula de una clase, como en BloqueClase */
private fun DrawScope.rotuloBloque(x: TextosHorario, s: SesionHorario, bx: Float, by: Float, alfa: Float) {
    val lineas = x.bloques.getValue(s.fila.materia)
    var y = by + 6f
    for (l in lineas) {
        texto(l, bx + 8f, y, Paleta.tinta, alfa)
        y += 15f
    }
    texto(x.aulas[FILAS.indexOf(s.fila)], bx + 8f, y + 2f, Paleta.tinta.copy(alpha = 0.62f), alfa)
}

/** La de ahora: el bloque de Sistemas de Costo crece hasta la tarjeta «En curso» de Horario.kt */
private fun DrawScope.enCurso(t: Float, x: TextosHorario, costo: SesionHorario, tarjeta: TarjetaEnCurso, velo: Float) {
    if (t < ABRE || velo <= 0.004f) return
    val crece = Movimiento.ESTANDAR.transform(durante(t, ABRE, 900f))
    val cristal = ESTANDAR.transform(durante(t, ABRE, 450f))
    val bx = mezclar(costo.x1, TX, crece)
    val by = mezclar(costo.y1, TY, crece)
    val bw = mezclar(costo.w1, TW, crece)
    val bh = mezclar(costo.h1, TH, crece)
    val radio = CornerRadius(mezclar(8f, 24f, crece))
    val o = Offset(bx, by)
    val tam = Size(bw, bh)
    val op = velo
    // Base opaca: a mitad del fundido las dos capas translúcidas dejarían ver la línea de ahora
    drawRoundRect(PANEL_TARJETA.copy(alpha = ESTANDAR.transform(durante(t, ABRE, 300f)) * op), o, tam, radio)
    val bloque = (1f - cristal) * op
    if (bloque > 0.004f) {
        drawRoundRect(FONDO_BLOQUE.copy(alpha = 0.4f * bloque), o, tam, radio)
        val p = tarjeta.bloque.de(by, bh, 0f) { Brush.verticalGradient(listOf(ROSA.copy(alpha = 0.40f), ROSA.copy(alpha = 0.26f)), by, by + bh) }
        drawRoundRect(p, o, tam, radio, alpha = bloque)
    }
    val vidrio = cristal * op
    if (vidrio > 0.004f) {
        drawRoundRect(PANEL_TARJETA.copy(alpha = vidrio), o, tam, radio)
        val p = tarjeta.cristal.de(by, bh, 0f) {
            Brush.verticalGradient(0f to Paleta.tinta.copy(alpha = 0.07f), 0.6f to Paleta.tinta.copy(alpha = 0.015f), 1f to Paleta.tinta.copy(alpha = 0.015f), startY = by, endY = by + bh)
        }
        drawRoundRect(p, o, tam, radio, alpha = vidrio)
        drawRoundRect(tarjeta.luz, o, tam, radio, alpha = vidrio)
        drawRoundRect(tarjeta.filo, Offset(bx + 0.5f, by + 0.5f), Size(bw - 1f, bh - 1f), radio, alpha = vidrio, style = Stroke(1f))
    }
    if (bloque > 0.004f) {
        clipRect(bx, by, bx + 3f, by + bh) { drawRoundRect(ROSA.copy(alpha = bloque), o, tam, radio) }
        drawRoundRect(ROSA.copy(alpha = 0.55f * bloque), Offset(bx + 0.5f, by + 0.5f), Size(bw - 1f, bh - 1f), radio, style = Stroke(1f))
        drawRect(Color.White.copy(alpha = 0.08f * bloque), Offset(bx + radio.x / 2, by), Size(bw - radio.x, 1f))
    }
    clipRect(bx, by, bx + bw, by + bh) {
        val rotulo = (1f - ESTANDAR.transform(durante(t, ABRE, 220f))) * op
        if (rotulo > 0.004f) rotuloBloque(x, costo, bx, by, rotulo)
        contenido(t, x, bx, by, op)
    }
}

/** El contenido de la tarjeta, que entra línea a línea cuando ya casi ha crecido */
private fun DrawScope.contenido(t: Float, x: TextosHorario, bx: Float, by: Float, op: Float) {
    fun linea(en: Float) = Movimiento.ASIENTA.transform(durante(t, en, 500f))
    val izq = bx + 20f

    // EN CURSO, con el latido del punto (el de la app: 2600 ms, un anillo que crece y se apaga)
    val l1 = linea(ABRE + 380f)
    if (l1 > 0f) {
        val dy = (1f - l1) * 6f
        val punto = Offset(izq + 3.5f, by + 28f + dy)
        if (t >= BARRA) {
            val q = LATIDO.transform((((t - BARRA) % 2_600f) / 2_600f / 0.7f).coerceIn(0f, 1f))
            drawCircle(ROSA.copy(alpha = 0.45f * (1f - q) * l1 * op), 3.5f * (1f + 1.6f * q), punto)
        }
        drawCircle(ROSA.copy(alpha = l1 * op), 3.5f, punto)
        texto(x.enCurso, izq + 17f, by + 21f + dy, Paleta.tinta, l1 * op)
    }
    // El nombre, por su línea base: la de la web con 36/40 (más baja que la letra) cae a 76,5 del borde
    val l2 = linea(ABRE + 460f)
    texto(x.nombreCosto, izq, by + 76.5f - x.nombreCosto.firstBaseline / density + (1f - l2) * 6f, Paleta.tinta, l2 * op)
    val l3 = linea(ABRE + 540f)
    var xq = izq
    x.quedan.forEachIndexed { i, q ->
        texto(q, xq, by + 92f + (1f - l3) * 6f, if (i == 1) Paleta.tinta else Paleta.suave, l3 * op)
        xq += ancho(q)
    }

    // La barra de la clase: de 10:00 a 12:00 PM, y llega a 10:40
    val l4 = linea(ABRE + 620f)
    if (l4 <= 0f) return
    val a4 = l4 * op
    val y4 = by + 128f + (1f - l4) * 6f
    texto(x.desde, izq, y4, Paleta.suave, a4)
    val fin = bx + TW - 20f
    // «PM» más pequeño y tenue, en la misma línea base que la hora
    val xPm = fin - ancho(x.pm)
    texto(x.pm, xPm, y4 + (x.hasta.firstBaseline - x.pm.firstBaseline) / density, Paleta.tenue, a4)
    val xHasta = xPm - 3f - ancho(x.hasta)
    texto(x.hasta, xHasta, y4, Paleta.suave, a4)
    val x0 = izq + ancho(x.desde) + 12f
    val x1 = xHasta - 12f
    val w = x1 - x0
    val yb = y4 + 8f
    drawRoundRect(Paleta.tinta.copy(alpha = 0.12f * a4), Offset(x0, yb - 1f), Size(w, 2f), CornerRadius(1f))
    val avance = 40f / 120f * ESTANDAR.transform(durante(t, BARRA, 900f))
    if (avance > 0f) drawRoundRect(ROSA.copy(alpha = a4), Offset(x0, yb - 1f), Size(w * avance, 2f), CornerRadius(1f))
    drawCircle(ROSA.copy(alpha = a4), 3.5f, Offset(x0 + 2.5f, yb))
    drawCircle(porTema(Color(0xFF15171A), Paleta.panel).copy(alpha = a4), 3.5f, Offset(x1 - 2.5f, yb))
    drawCircle(ROSA.copy(alpha = a4), 2.75f, Offset(x1 - 2.5f, yb), style = Stroke(1.5f))
    val cabeza = Movimiento.ASIENTA.transform(durante(t, BARRA, 300f)) * a4
    if (cabeza > 0f) {
        val c = Offset(x0 + w * avance, yb)
        drawCircle(Paleta.panelSuave.copy(alpha = cabeza), 8.5f, c)
        drawCircle(ROSA.copy(alpha = cabeza), 5.5f, c)
    }
}
