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
import mpensum.componentes.desbordar
import mpensum.componentes.pulsable
import mpensum.componentes.ventana
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
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
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
/** En horizontal las columnas miden unos 155 dp: el carril respira más y la hora no queda pegada a la rejilla */
private val RIEL_COMPACTO = 44.dp
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

/* En pantallas compactas (< 360 dp) la semana cede ancho al nombre: riel angosto, menos aire y letra menor */
private val RIEL_ESTRECHO = 26.dp
private val HUECO_COLUMNA_ESTRECHO = 2.dp
private val METIDA_ESTRECHA = 6.dp
private val TEXTO_INICIO_ESTRECHO = 6.dp
private val TEXTO_FIN_ESTRECHO = 2.dp
/** Los cuerpos de letra del nombre, del mayor al menor: se baja uno solo si el nombre entero no cabe */
private val CUERPOS_ESTRECHOS = listOf(11.sp, 10.sp, 9.sp)
private val CUERPO_AULA_ESTRECHO = 9.sp
/** Palabras que no aportan en un bloque pequeño: no dan inicial */
private val ENLACES_BLOQUE = setOf("de", "del", "la", "las", "el", "los", "y", "e", "en", "a", "al", "para", "con", "por")
private val NUMERAL = Regex("""^([IVX]+|\d+)$""")

/** Las medidas de la rejilla que cambian entre una pantalla normal y una compacta */
private class AjusteSemana(val riel: Dp, val hueco: Dp, val metida: Dp, val textoInicio: Dp, val textoFin: Dp, val estrecha: Boolean)

/** El ancho de un bloque: su parte de la columna, menos el aire con la columna vecina y entre carriles */
private fun anchoDe(colW: Dp, c: Colocada, a: AjusteSemana): Dp = colW - a.hueco * 2 - a.metida * c.carril

/** Un bloque ya resuelto: sus renglones, su estilo (menor si la pantalla es estrecha) y si lleva aula */
private class BloqueResuelto(val c: Colocada, val lineas: List<String>, val conAula: Boolean, val estilo: TextStyle, val estiloAula: TextStyle)

/** Parte [palabras] en hasta [renglones] renglones sin abreviar nada; nulo si alguna palabra no cabe sola o sobran renglones */
private fun envolverEntero(palabras: List<String>, renglones: Int, entra: (String) -> Boolean): List<String>? {
    val salida = ArrayList<String>()
    var actual = ""
    for (p in palabras) {
        if (!entra(p)) return null
        val junta = if (actual.isEmpty()) p else "$actual $p"
        if (entra(junta)) actual = junta else {
            salida += actual
            actual = p
        }
    }
    if (actual.isNotEmpty()) salida += actual
    return if (salida.size <= renglones) salida else null
}

/**
 * La regla de abreviar de la semana compacta: la primera palabra completa y,
 * debajo, las iniciales de las demás con su punto («Inferencia» / «D. E.»).
 * Los numerales («II») se quedan enteros; nunca se corta una palabra a ciegas.
 */
private fun conIniciales(nombre: String, renglones: Int, entra: (String) -> Boolean): List<String> {
    val palabras = nombre.split(' ').filter { it.isNotBlank() }
    val primera = palabras.firstOrNull() ?: return emptyList()
    val cabeza = if (entra(primera)) primera else abreviarNombre(primera, 1, entra).firstOrNull { entra(it) } ?: return emptyList()
    if (renglones < 2) return listOf(cabeza)
    val resto = palabras.drop(1).filter { it.lowercase() !in ENLACES_BLOQUE }
    if (resto.isEmpty()) return listOf(cabeza)
    val letras = resto.map { if (NUMERAL.matches(it)) it else it.first().uppercaseChar().toString() }
    val intentos = listOf(
        letras.joinToString(" ") { if (it.length == 1) "$it." else it },
        letras.joinToString(" "),
        letras.joinToString(""),
    )
    val iniciales = intentos.firstOrNull(entra) ?: return listOf(cabeza)
    return listOf(cabeza, iniciales)
}

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
    val estrecha = !compacto && ventana().compacta
    val aj = remember(compacto, estrecha) {
        if (estrecha) AjusteSemana(RIEL_ESTRECHO, HUECO_COLUMNA_ESTRECHO, METIDA_ESTRECHA, TEXTO_INICIO_ESTRECHO, TEXTO_FIN_ESTRECHO, true)
        else AjusteSemana(if (compacto) RIEL_COMPACTO else RIEL, HUECO_COLUMNA, METIDA, TEXTO_INICIO, TEXTO_FIN, false)
    }
    val riel = aj.riel
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
        BasicText(resumenSemana(sesiones), Modifier.padding(top = Espacio.xxs), style = l.texto)
    }

    Column(
        Modifier
            .padding(top = if (compacto) Espacio.xs else Espacio.l)
            .desbordar(DESBORDE)
            .drawBehind {
                // Hoy se marca con un velo apenas visible de arriba abajo, nombre del día incluido
                if (ahora.dia < 5) {
                    val rielPx = riel.toPx()
                    val col = (size.width - rielPx) / 5
                    drawRoundRect(
                        Paleta.tinta.copy(alpha = 0.03f),
                        Offset(rielPx + ahora.dia * col + 1.dp.toPx(), 0f),
                        Size(col - 2.dp.toPx(), size.height),
                        CornerRadius(12.dp.toPx()),
                    )
                }
            }
            .padding(bottom = Espacio.xs),
    ) {
        CabeceraDias(horario, riel, ahora.dia, provisional, compacto)

        BoxWithConstraints(
            Modifier
                .padding(top = Espacio.xxs)
                .fillMaxWidth()
                .height(cimas[filas])
                .drawWithContent {
                    val rielPx = riel.toPx()
                    val col = (size.width - rielPx) / 5
                    val g = 1.dp.toPx()
                    // Una cuadrícula que casi no se ve: solo las horas, sin líneas entre días (los separa el aire)
                    for (i in 0..filas) {
                        val pliegueArriba = i > 0 && !ocupadas[i - 1]
                        val pliegueAbajo = i < filas && !ocupadas[i]
                        if (pliegueArriba && pliegueAbajo) continue
                        drawRect(Paleta.tinta.copy(alpha = 0.05f), Offset(rielPx, cimas[i].toPx()), Size(size.width - rielPx, g))
                    }
                    // Dentro de un pliegue, un punteado: ahí se salta una hora
                    for (i in 0 until filas) if (!ocupadas[i]) {
                        val ym = ((cimas[i] + cimas[i + 1]) / 2).toPx()
                        drawLine(
                            Paleta.tinta.copy(alpha = 0.12f), Offset(rielPx, ym), Offset(size.width, ym), g,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())),
                        )
                    }
                    drawContent()
                    // El tramo pisado de cada choque: una raya roja fina en el borde de la columna
                    for ((d, desde, hasta) in pisados) {
                        val x = rielPx + (d + 1) * col - 1.dp.toPx()
                        val y0 = y(desde).toPx()
                        drawRoundRect(Paleta.rojo, Offset(x - g, y0), Size(1.5.dp.toPx(), y(hasta).toPx() - y0), CornerRadius(g))
                    }
                    // La línea de ahora: tenue por la semana y marcada sobre hoy, por encima de las clases
                    if (ahoraSeVe) {
                        val ya = yAhora.toPx()
                        drawRect(Paleta.rojo.copy(alpha = 0.35f), Offset(rielPx, ya - g / 2), Size(size.width - rielPx, g))
                        val x = rielPx + ahora.dia * col
                        drawRect(Paleta.rojo, Offset(x, ya - g), Size(col, 2 * g))
                        drawCircle(Paleta.rojo, 3.dp.toPx(), Offset(x + 1.dp.toPx(), ya))
                    }
                },
        ) {
            val colW = (maxWidth - riel) / 5
            val medidor = rememberTextMeasurer()
            val densidad = LocalDensity.current.density
            // Cada nombre se resuelve una vez por horario y ancho: sin partir palabras, en lo que cabe
            val bloques = remember(porDias, colW, cimas, aj, l) {
                (0 until 5).flatMap { d ->
                    enCarriles(porDias[d]).map { c ->
                        val ancho = anchoDe(colW, c, aj)
                        val alto = (y(c.sesion.fin) - y(c.sesion.inicio) - HUECO_V * 2).value.toInt()
                        val cabe = ((ancho - aj.textoInicio - aj.textoFin).value * densidad).toInt()
                        val nombre = horario.nombre(c.sesion.codigo)
                        if (aj.estrecha) {
                            resolverEstrecho(c, nombre, alto, cabe, medidor, l)
                        } else {
                            val renglones = ((alto - TEXTO_ARRIBA * 2) / RENGLON).coerceIn(1, 3)
                            val entra = { t: String -> medidor.measure(t, l.bloque).size.width <= cabe }
                            // Lo que no cabe entero no se pinta: en un carril estrecho el bloque queda en su color, y tocarlo abre el detalle
                            val lineas = abreviarNombre(nombre, renglones, entra).takeWhile(entra)
                            val conAula = lineas.isNotEmpty() && c.sesion.aula.isNotBlank() &&
                                medidor.measure(c.sesion.aula, l.pequeno).size.width <= cabe &&
                                alto >= TEXTO_ARRIBA * 2 + RENGLON * lineas.size + 16
                            BloqueResuelto(c, lineas, conAula, l.bloque, l.pequeno.copy(color = Paleta.tinta.copy(alpha = 0.6f)))
                        }
                    }
                }
            }

            for (i in 0..filas) {
                // Entre dos pliegues no hay hora que leer; y la de ahora tapa la que tenga encima
                if (i in 1 until filas && !ocupadas[i - 1] && !ocupadas[i]) continue
                if (ahoraSeVe && abs((cimas[i] - yAhora).value) < 14) continue
                RotuloHora(abre + i * 60, riel, Modifier.offset(y = cimas[i] - 7.dp), aj.estrecha)
            }
            for (b in bloques) {
                BloqueClase(horario, b, aj, colW, y(b.c.sesion.inicio), y(b.c.sesion.fin), provisional)
            }
            if (ahoraSeVe) {
                BasicText(
                    horaDe(ahora.minuto),
                    Modifier.offset(y = yAhora - 7.dp).width(riel - 6.dp).wrapContentWidth(Alignment.End, unbounded = true),
                    style = l.pequeno.copy(color = Paleta.rojo, fontWeight = FontWeight.SemiBold),
                    softWrap = false,
                )
            }
        }
    }
}

/** Los cinco días sobre la rejilla: el nombre y, en vertical, su número; hoy lleva el número en un círculo y tocar un día lo abre */
@Composable
private fun CabeceraDias(horario: EstadoHorario, riel: Dp, diaHoy: Int, provisional: Boolean, compacto: Boolean) {
    val l = letrasHorario()
    val numeros = horario.numerosDia
    // En horizontal el nombre y el número van en un renglón: la cabecera no le quita alto a las horas
    val altoCabecera = if (numeros != null && !compacto) 62.dp else Alto.icono
    Row(Modifier.fillMaxWidth().height(altoCabecera)) {
        Spacer(Modifier.width(riel))
        for (d in 0 until 5) {
            val hoy = d == diaHoy && !provisional
            Column(
                Modifier.weight(1f).fillMaxHeight().then(
                    if (provisional) Modifier else Modifier.pulsable({ horario.dia = d }, escala = 0.94f),
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val nombre = @Composable {
                    // El tracking deja aire a la derecha de la última letra; se compensa a la izquierda para que quede centrado
                    BasicText(
                        DIAS_CORTOS[d],
                        Modifier.padding(start = Espacio.hilo),
                        style = l.etiqueta.copy(color = if (hoy) Paleta.tinta else Paleta.tenue, fontWeight = if (hoy) FontWeight.SemiBold else FontWeight.Medium),
                    )
                }
                // El número del día; el de hoy, en un círculo de tinta
                val numero = @Composable { n: Int ->
                    Box(Modifier.size(26.dp).drawBehind { if (hoy) drawCircle(Paleta.tinta) }, contentAlignment = Alignment.Center) {
                        BasicText("$n", style = l.cuerpo.copy(color = if (hoy) Paleta.lienzo else Paleta.suave, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
                    }
                }
                if (compacto) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
                        nombre()
                        if (numeros != null) numero(numeros[d])
                    }
                } else {
                    nombre()
                    if (numeros != null) Box(Modifier.padding(top = Espacio.xxs)) { numero(numeros[d]) }
                }
            }
        }
    }
}

/** Una hora del riel, como en la web: el número y el meridiano aparte, pequeño */
@Composable
private fun RotuloHora(minuto: Int, riel: Dp, modifier: Modifier, estrecha: Boolean = false) {
    val l = letrasHorario()
    val h = ((minuto / 60 + 11) % 12) + 1
    BasicText(
        buildAnnotatedString {
            append("$h")
            withStyle(SpanStyle(fontSize = 8.sp, letterSpacing = 0.08.em, color = Paleta.apagada)) { append(" ${meridiano(minuto)}") }
        },
        // Pegada a la derecha del carril, con aire antes de la rejilla; si no cabe, sobresale a la izquierda
        modifier.width(riel - 6.dp).wrapContentWidth(Alignment.End, unbounded = true),
        style = l.pequeno.copy(fontWeight = FontWeight.Medium, fontSize = if (estrecha) CUERPO_AULA_ESTRECHO else l.pequeno.fontSize),
        softWrap = false,
    )
}

/**
 * Una clase en la rejilla: un relleno tenue de su color, sin borde, con el
 * canto de color a la izquierda y las esquinas redondas. La que va ahora se
 * tiñe un poco más; las de hoy que ya pasaron se apagan.
 */
@Composable
private fun BloqueClase(horario: EstadoHorario, b: BloqueResuelto, aj: AjusteSemana, colW: Dp, desde: Dp, hasta: Dp, provisional: Boolean) {
    val c = b.c
    val s = c.sesion
    val enCurso = horario.enCurso(s)
    val pasada = horario.pasada(s)
    val color = horario.color(s.codigo)
    val ancho = anchoDe(colW, c, aj)
    Column(
        Modifier
            .offset {
                IntOffset(
                    (aj.riel + colW * s.dia + aj.hueco + aj.metida * c.carril).roundToPx(),
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
            .padding(start = aj.textoInicio, end = aj.textoFin, top = TEXTO_ARRIBA.dp, bottom = 6.dp),
    ) {
        b.lineas.forEach { BasicText(it, style = b.estilo, softWrap = false, maxLines = 1) }
        if (b.conAula) {
            BasicText(s.aula, Modifier.padding(top = 3.dp), style = b.estiloAula, softWrap = false, maxLines = 1)
        }
    }
}

/**
 * El bloque en una pantalla compacta: primero el nombre entero en 1 a 3
 * renglones, bajando la letra hasta 9 sp; si ni así cabe, la primera palabra
 * completa con las iniciales del resto.
 */
private fun resolverEstrecho(c: Colocada, nombre: String, alto: Int, cabe: Int, medidor: TextMeasurer, l: LetrasHorario): BloqueResuelto {
    val aula = l.pequeno.copy(fontSize = CUERPO_AULA_ESTRECHO, lineHeight = CUERPO_AULA_ESTRECHO * 1.25f, color = Paleta.tinta.copy(alpha = 0.6f))
    fun estiloDe(t: TextUnit) = l.bloque.copy(fontSize = t, lineHeight = t * 1.25f)
    fun renglonesDe(t: TextUnit) = ((alto - TEXTO_ARRIBA * 2) / (t.value * 1.25f)).toInt().coerceIn(1, 3)
    fun cierre(lineas: List<String>, e: TextStyle): BloqueResuelto {
        val conAula = lineas.isNotEmpty() && c.sesion.aula.isNotBlank() &&
            medidor.measure(c.sesion.aula, aula).size.width <= cabe &&
            alto >= TEXTO_ARRIBA * 2 + e.lineHeight.value * lineas.size + 14
        return BloqueResuelto(c, lineas, conAula, e, aula)
    }
    val palabras = nombre.split(' ').filter { it.isNotBlank() }
    for (t in CUERPOS_ESTRECHOS) {
        val e = estiloDe(t)
        val entra = { x: String -> medidor.measure(x, e).size.width <= cabe }
        envolverEntero(palabras, renglonesDe(t), entra)?.let { return cierre(it, e) }
    }
    val e = estiloDe(CUERPOS_ESTRECHOS.last())
    val entra = { x: String -> medidor.measure(x, e).size.width <= cabe }
    return cierre(conIniciales(nombre, renglonesDe(CUERPOS_ESTRECHOS.last()), entra), e)
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
