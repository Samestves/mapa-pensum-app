package mpensum.carrera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.pulsable
import mpensum.logica.Colocada
import mpensum.logica.DIAS_CORTOS
import mpensum.logica.Sesion
import mpensum.logica.abreviarNombre
import mpensum.logica.choquesDe
import mpensum.logica.porDia
import mpensum.logica.rangoDeClases
import mpensum.logica.clases
import mpensum.logica.duracion
import mpensum.logica.enCarriles
import mpensum.logica.horaDe
import mpensum.logica.horasOcupadas
import mpensum.logica.materias
import mpensum.logica.meridiano
import mpensum.tema.Paleta
import kotlin.math.abs

/*
 * La semana en rejilla: cinco columnas que llenan el ancho y una hora por
 * fila, solo de la primera clase a la última. Las horas sin clase en toda la
 * semana (el almuerzo) se pliegan a una franja: no merecen el alto de una con
 * clase. Las clases son bloques sin borde, teñidos de su color y con su canto,
 * separados entre sí y de la columna vecina para que la semana respire y no
 * sea una pared de cajas. Las que se pisan se reparten la columna lado a lado.
 */

/** Lo que mide una hora con clase: una de 2 h deja sitio a tres renglones de nombre y al aula */
private val ALTO_HORA = 72.dp
/** En horizontal (375 dp de alto) la hora mide 40 dp: cabe más día en la pantalla */
private val ALTO_HORA_COMPACTA = 40.dp
/** Lo que mide una hora plegada: lo justo para leer que ahí se salta una hora */
private val ALTO_PLIEGUE = 24.dp
/** El carril de la izquierda, con las horas */
private val RIEL = 30.dp
/** Cuánto sale la rejilla por cada lado del margen de la vista: cada dp aquí es un dp más de nombre */
private val DESBORDE = 8.dp
/** El aire de un bloque: con el borde de su columna y por arriba y abajo */
private val HUECO_COLUMNA = 3.dp
/** Lo que se mete a la derecha la clase que empieza después de otra que aún dura, como en Calendario de Apple */
private val METIDA = 10.dp
/** El corte del color del fondo que separa la clase de encima de la de debajo */
private val CORTE = 2.dp
private val HUECO_V = 2.dp
private val RADIO = 10.dp
private val CANTO = 3.dp
/** El texto dentro del bloque: del canto al nombre, al borde derecho y arriba */
private val TEXTO_INICIO = 9.dp
private val TEXTO_FIN = 5.dp
private const val TEXTO_ARRIBA = 7
private const val RENGLON = 15

/** Dónde empieza cada fila de hora, en dp desde arriba; la última entrada es el alto total */
private fun cimas(ocupadas: List<Boolean>, compacto: Boolean): List<Dp> {
    val salida = ArrayList<Dp>(ocupadas.size + 1)
    var y = 0.dp
    salida += y
    for (o in ocupadas) {
        y += if (o) (if (compacto) ALTO_HORA_COMPACTA else ALTO_HORA) else ALTO_PLIEGUE
        salida += y
    }
    return salida
}

@Composable
internal fun SemanaRejilla(horario: EstadoHorario, sesiones: List<Sesion> = horario.sesiones, provisional: Boolean = false, compacto: Boolean = false) {
    val l = letrasHorario()
    val (abre, cierra) = if (provisional) rangoDeClases(sesiones) else horario.rango
    val ahora = horario.ahora
    val porDias = remember(sesiones) { porDia(sesiones) }
    val ocupadas = remember(sesiones, abre, cierra) { horasOcupadas(sesiones, abre, cierra) }
    val choques = remember(sesiones) { choquesDe(sesiones) }
    // El tramo que pisa cada clase de encima: día, desde y hasta
    val pisados = remember(choques) { choques.map { (a, b) -> Triple(b.dia, b.inicio, minOf(a.fin, b.fin)) } }
    val cimas = remember(ocupadas, compacto) { cimas(ocupadas, compacto) }
    val filas = ocupadas.size
    // Un minuto, en dp desde arriba: lineal dentro de cada hora, sea plegada o no
    fun y(min: Int): Dp {
        val i = ((min - abre) / 60).coerceIn(0, filas - 1)
        val dentro = (min - abre - i * 60) / 60f
        return cimas[i] + (cimas[i + 1] - cimas[i]) * dentro
    }
    val ahoraSeVe = ahora.dia < 5 && ahora.minuto in abre..cierra
    val yAhora = if (ahoraSeVe) y(ahora.minuto) else 0.dp

    // Lo que se eligió y lo que pesa, en una línea; en horizontal sube a la cabecera, junto a «TU SEMANA»
    if (!compacto) {
        BasicText(resumenSemana(sesiones), Modifier.padding(top = 4.dp), style = l.texto)
    }

    Column(
        Modifier
            .padding(top = if (compacto) 8.dp else 20.dp)
            .desbordar(DESBORDE)
            .drawBehind {
                // Hoy se marca con un velo apenas visible de arriba abajo, nombre del día incluido
                if (ahora.dia < 5) {
                    val riel = RIEL.toPx()
                    val col = (size.width - riel) / 5
                    drawRoundRect(
                        Paleta.tinta.copy(alpha = 0.03f),
                        Offset(riel + ahora.dia * col + 1.dp.toPx(), 0f),
                        Size(col - 2.dp.toPx(), size.height),
                        CornerRadius(12.dp.toPx()),
                    )
                }
            }
            .padding(bottom = 8.dp),
    ) {
        val numeros = horario.numerosDia
        Row(Modifier.fillMaxWidth().height(if (numeros != null) 62.dp else 40.dp)) {
            Spacer(Modifier.width(RIEL))
            for (d in 0 until 5) {
                val hoy = d == ahora.dia && !provisional
                Column(
                    Modifier.weight(1f).fillMaxHeight().then(
                        if (provisional) Modifier else Modifier.pulsable({
                            horario.dia = d
                            horario.modo = ModoHorario.DIA
                        }, escala = 0.94f),
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // El tracking deja aire a la derecha de la última letra; se compensa a la izquierda para que quede centrado
                    BasicText(
                        DIAS_CORTOS[d],
                        Modifier.padding(start = 2.dp),
                        style = l.etiqueta.copy(color = if (hoy) Paleta.tinta else Paleta.tenue, fontWeight = if (hoy) FontWeight.SemiBold else FontWeight.Medium),
                    )
                    // El número del día; el de hoy, en un círculo de tinta
                    if (numeros != null) Box(
                        Modifier.padding(top = 6.dp).size(26.dp).drawBehind { if (hoy) drawCircle(Paleta.tinta) },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText("${numeros[d]}", style = l.cuerpo.copy(color = if (hoy) Paleta.lienzo else Paleta.suave, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
                    }
                }
            }
        }

        BoxWithConstraints(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(cimas[filas])
                .drawWithContent {
                    val riel = RIEL.toPx()
                    val col = (size.width - riel) / 5
                    val g = 1.dp.toPx()
                    // Una cuadrícula que casi no se ve: solo las horas, sin líneas entre días (los separa el aire)
                    for (i in 0..filas) {
                        val pliegueArriba = i > 0 && !ocupadas[i - 1]
                        val pliegueAbajo = i < filas && !ocupadas[i]
                        if (pliegueArriba && pliegueAbajo) continue
                        drawRect(Paleta.tinta.copy(alpha = 0.05f), Offset(riel, cimas[i].toPx()), Size(size.width - riel, g))
                    }
                    // Dentro de un pliegue, un punteado: ahí se salta una hora
                    for (i in 0 until filas) if (!ocupadas[i]) {
                        val ym = ((cimas[i] + cimas[i + 1]) / 2).toPx()
                        drawLine(
                            Paleta.tinta.copy(alpha = 0.12f), Offset(riel, ym), Offset(size.width, ym), g,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())),
                        )
                    }
                    drawContent()
                    // El tramo pisado de cada choque: una raya roja fina en el borde de la columna
                    for ((d, desde, hasta) in pisados) {
                        val x = riel + (d + 1) * col - 1.dp.toPx()
                        val y0 = y(desde).toPx()
                        drawRoundRect(Paleta.rojo, Offset(x - g, y0), Size(1.5.dp.toPx(), y(hasta).toPx() - y0), CornerRadius(g))
                    }
                    // La línea de ahora: tenue por la semana y marcada sobre hoy, por encima de las clases
                    if (ahoraSeVe) {
                        val ya = yAhora.toPx()
                        drawRect(Paleta.rojo.copy(alpha = 0.35f), Offset(riel, ya - g / 2), Size(size.width - riel, g))
                        val x = riel + ahora.dia * col
                        drawRect(Paleta.rojo, Offset(x, ya - g), Size(col, 2 * g))
                        drawCircle(Paleta.rojo, 3.dp.toPx(), Offset(x + 1.dp.toPx(), ya))
                    }
                },
        ) {
            val colW = (maxWidth - RIEL) / 5
            val medidor = rememberTextMeasurer()
            val densidad = LocalDensity.current.density
            // Cada nombre se abrevia una vez por horario y ancho: sin partir palabras, en lo que cabe
            val bloques = remember(porDias, colW, cimas) {
                (0 until 5).flatMap { d ->
                    enCarriles(porDias[d]).map { c ->
                        val ancho = anchoDe(colW, c)
                        val alto = (y(c.sesion.fin) - y(c.sesion.inicio) - HUECO_V * 2).value.toInt()
                        val renglones = ((alto - TEXTO_ARRIBA * 2) / RENGLON).coerceIn(1, 3)
                        val cabe = ((ancho - TEXTO_INICIO - TEXTO_FIN).value * densidad).toInt()
                        val entra = { t: String -> medidor.measure(t, l.bloque).size.width <= cabe }
                        // Lo que no cabe entero no se pinta: en un carril estrecho el bloque queda en su color, y tocarlo abre el detalle
                        val lineas = abreviarNombre(horario.nombre(c.sesion.codigo), renglones, entra).takeWhile(entra)
                        val conAula = lineas.isNotEmpty() && c.sesion.aula.isNotBlank() &&
                            medidor.measure(c.sesion.aula, l.pequeno).size.width <= cabe &&
                            alto >= TEXTO_ARRIBA * 2 + RENGLON * lineas.size + 16
                        Triple(c, lineas, conAula)
                    }
                }
            }

            for (i in 0..filas) {
                // Entre dos pliegues no hay hora que leer; y la de ahora tapa la que tenga encima
                if (i in 1 until filas && !ocupadas[i - 1] && !ocupadas[i]) continue
                if (ahoraSeVe && abs((cimas[i] - yAhora).value) < 14) continue
                RotuloHora(abre + i * 60, Modifier.offset(y = cimas[i] - 7.dp))
            }
            for ((c, lineas, conAula) in bloques) {
                BloqueClase(horario, c, lineas, conAula, colW, y(c.sesion.inicio), y(c.sesion.fin), provisional)
            }
            if (ahoraSeVe) {
                BasicText(
                    horaDe(ahora.minuto),
                    Modifier.offset(y = yAhora - 7.dp).width(RIEL - 6.dp).wrapContentWidth(Alignment.End, unbounded = true),
                    style = l.pequeno.copy(color = Paleta.rojo, fontWeight = FontWeight.SemiBold),
                    softWrap = false,
                )
            }
        }
    }
}

/** El ancho de un bloque: su parte de la columna, menos el aire con la columna vecina y entre carriles */
private fun anchoDe(colW: Dp, c: Colocada): Dp = colW - HUECO_COLUMNA * 2 - METIDA * c.carril

/** Una hora del riel, como en la web: el número y el meridiano aparte, pequeño */
@Composable
private fun RotuloHora(minuto: Int, modifier: Modifier) {
    val l = letrasHorario()
    val h = ((minuto / 60 + 11) % 12) + 1
    BasicText(
        buildAnnotatedString {
            append("$h")
            withStyle(SpanStyle(fontSize = 8.sp, letterSpacing = 0.08.em, color = Paleta.apagada)) { append(" ${meridiano(minuto)}") }
        },
        // Pegada a la derecha del carril, con aire antes de la rejilla; si no cabe, sobresale a la izquierda
        modifier.width(RIEL - 6.dp).wrapContentWidth(Alignment.End, unbounded = true),
        style = l.pequeno.copy(fontWeight = FontWeight.Medium),
        softWrap = false,
    )
}

/**
 * Una clase en la rejilla: un relleno tenue de su color, sin borde, con el
 * canto de color a la izquierda y las esquinas redondas. La que va ahora se
 * tiñe un poco más; las de hoy que ya pasaron se apagan.
 */
@Composable
private fun BloqueClase(horario: EstadoHorario, c: Colocada, lineas: List<String>, conAula: Boolean, colW: Dp, desde: Dp, hasta: Dp, provisional: Boolean) {
    val l = letrasHorario()
    val s = c.sesion
    val enCurso = horario.enCurso(s)
    val pasada = horario.pasada(s)
    val color = horario.color(s.codigo)
    val ancho = anchoDe(colW, c)
    Column(
        Modifier
            .offset {
                IntOffset(
                    (RIEL + colW * s.dia + HUECO_COLUMNA + METIDA * c.carril).roundToPx(),
                    (desde + HUECO_V).roundToPx(),
                )
            }
            .size(ancho, hasta - desde - HUECO_V * 2)
            .graphicsLayer { alpha = if (pasada) 0.5f else 1f }
            .then(if (provisional) Modifier else Modifier.pulsable({ horario.hoja = HojaHorario.Detalle(s.id) }, escala = 0.96f))
            .drawBehind {
                // La que se mete sobre otra lleva un corte del color del fondo
                if (c.carril > 0) {
                    val corte = CORTE.toPx()
                    drawRoundRect(Paleta.lienzo, Offset(-corte, -corte), Size(size.width + corte * 2, size.height + corte * 2), CornerRadius((RADIO + CORTE).toPx()))
                }
            }
            .clip(RoundedCornerShape(RADIO))
            .drawBehind {
                // Y es opaca: si no, se vería la de debajo a través
                if (c.carril > 0) drawRect(Paleta.lienzo)
                drawRect(color.copy(alpha = if (enCurso) 0.30f else 0.19f))
                // El canto va recto y el radio del bloque lo redondea al recortar
                drawRect(color, size = Size(CANTO.toPx(), size.height))
            }
            .padding(start = TEXTO_INICIO, end = TEXTO_FIN, top = TEXTO_ARRIBA.dp, bottom = 6.dp),
    ) {
        lineas.forEach { BasicText(it, style = l.bloque, softWrap = false, maxLines = 1) }
        if (conAula) {
            BasicText(s.aula, Modifier.padding(top = 3.dp), style = l.pequeno.copy(color = Paleta.tinta.copy(alpha = 0.6f)), softWrap = false, maxLines = 1)
        }
    }
}


/** «6 materias · 10 clases · 20 h», con los choques en rojo si los hay */
@Composable
internal fun resumenSemana(sesiones: List<Sesion>): AnnotatedString {
    val choques = remember(sesiones) { choquesDe(sesiones) }
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Paleta.suave)) {
            append(materias(sesiones.map { it.codigo }.distinct().size))
        }
        append(" · ${clases(sesiones.size)} · ${duracion(sesiones.sumOf { it.duracion })}")
        if (choques.isNotEmpty()) withStyle(SpanStyle(color = Paleta.rojo)) { append(" · " + if (choques.size == 1) "1 choque" else "${choques.size} choques") }
    }
}
