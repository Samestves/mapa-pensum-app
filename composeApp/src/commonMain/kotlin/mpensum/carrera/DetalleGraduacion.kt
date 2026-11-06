package mpensum.carrera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.componentes.IconoRelleno
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Phosphor
import mpensum.componentes.esApaisado
import mpensum.componentes.tarjeta
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Periodo
import mpensum.logica.estimarGraduacion
import mpensum.plataforma.mesLocal
import mpensum.tema.Espacio
import mpensum.tema.Tipo
import mpensum.tema.Alto
import mpensum.componentes.margenPantalla
import mpensum.componentes.ventana
import mpensum.tema.Paleta
import kotlin.math.abs
import kotlin.math.roundToInt
import mpensum.tema.Radio

/*
 * "¿Cuándo me gradúo?": el detalle de la hoja del progreso. De arriba abajo:
 * la fecha grande con cuánto falta, el anillo con una marca por materia, la
 * cadena de materias que no puedes atrasar, el deslizador de materias por
 * semestre y lo que inscribes el próximo semestre. Acostado, el anillo queda
 * a la izquierda, entero y quieto, y el resto (fecha incluida) en una columna
 * con su propio scroll a la derecha.
 *
 * Los ocho planes (de 1 a 8 materias por semestre) se calculan una sola vez al
 * abrir y se recalculan solo si cambian las marcas o las casillas. Mover el
 * deslizador solo elige entre ellos: la fecha, el anillo y las notas cambian
 * al instante, y la carga se guarda al soltar.
 */

private const val MAX_CARGA = 8

/** El alto mínimo de la pastilla con la consecuencia del ritmo ("Lo más rápido") */
private val ALTO_CHIP_RITMO = 24.dp

/** El ancho del anillo acostado: lo que piden sus anotaciones a los lados */
private val ANCHO_ANILLO_APAISADO = 340.dp

/** La entrada: el anillo se llena en 600 ms, luego salen los rótulos y al final el resto */
private const val ENTRADA_MS = 1_100

@Composable
internal fun DetalleGraduacion(estado: EstadoCarrera, hoy: Periodo, alVerEnMapa: (List<String>) -> Unit) {
    val pensum = estado.pensum
    val nombre = remember(pensum) { nombresDelPlan(pensum) }
    // Una sola vez al abrir: cada plan con su anillo
    val planes = remember(estado.marcas, estado.casillas) { (1..MAX_CARGA).map { estimarGraduacion(pensum, estado.marcas, estado.casillas, it, hoy) } }
    val anillos = remember(planes) { planes.map { anilloDe(estado, it, hoy) } }
    val habitual = remember(estado.marcas, estado.casillas) { estimarGraduacion(pensum, estado.marcas, estado.casillas, null, hoy).carga.coerceIn(1, MAX_CARGA) }
    var elegida by remember { mutableIntStateOf((estado.carga ?: habitual).coerceIn(1, MAX_CARGA)) }
    val g = planes[elegida - 1]
    val fecha = g.periodoFinal?.mesYAnio() ?: "Completa"
    val porcentaje = estado.progreso.avance.roundToInt()
    // Lo más rápido que deja la cadena, y con cuántas materias se logra
    val minimo = remember(planes) { planes.minOf { it.semestresRestantes } }
    val cargaMinima = remember(planes) { planes.indexOfFirst { it.semestresRestantes == minimo } + 1 }

    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(ENTRADA_MS, easing = LinearEasing)) }

    // La fecha y cuánto falta
    val titular: @Composable (Int) -> Unit = { tam ->
        Column(
            Modifier.graphicsLayer {
                val o = suavizar(entrada.value / 0.25f)
                alpha = o
                translationY = (1f - o) * 10.dp.toPx()
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoTrazo(Lucide.birrete, Paleta.tinta.copy(alpha = 0.5f), 16.dp, 2f)
                BasicText(if (g.terminado) "TU CARRERA" else "TE GRADÚAS EN", Modifier.padding(start = Espacio.xxs), style = versal(Paleta.tinta.copy(alpha = 0.5f)).copy(letterSpacing = 0.06.em))
            }
            BasicText(fecha, Modifier.padding(top = Espacio.xxs), style = estilo(tam, Paleta.tinta, FontWeight.Light, tam + 4).copy(letterSpacing = (-0.045).em))
            if (!g.terminado) {
                Box(Modifier.padding(top = Espacio.s).height(28.dp).clip(RoundedCornerShape(14.dp)).background(Paleta.tinta.copy(alpha = 0.08f)).padding(horizontal = Espacio.s), contentAlignment = Alignment.Center) {
                    BasicText(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Paleta.tinta)) { append(cuantoFalta(g)) }
                            append(" · ${g.semestresRestantes} ${if (g.semestresRestantes == 1) "semestre" else "semestres"}")
                        },
                        style = estilo(Tipo.s, Paleta.tinta.copy(alpha = 0.62f), interlinea = 18),
                    )
                }
            }
        }
    }
    val anillo: @Composable (Modifier) -> Unit = { modifier ->
        AnilloGraduacion(anillos[elegida - 1], porcentaje, g.periodoFinal?.anio, hoy.anio, { entrada.value }, modifier)
    }
    // La cadena, el ritmo y lo que inscribes: entran al final
    val tarjetas: @Composable () -> Unit = {
        Column(
            Modifier.graphicsLayer {
                val o = suavizar((entrada.value - 0.7f) / 0.3f)
                alpha = o
                translationY = (1f - o) * 16.dp.toPx()
            },
        ) {
            if (g.cuelloDeBotella.isNotEmpty()) {
                NoPuedesAtrasar(g, hoy, nombre)
                Spacer(Modifier.height(Espacio.s))
            }
            TarjetaRitmo(
                valor = elegida,
                ritmo = habitual,
                consecuencia = consecuencia(g.semestresRestantes, planes[habitual - 1].semestresRestantes, g.semestresRestantes == minimo),
                nota = notaDelRitmo(elegida, habitual, cargaMinima),
                alCambiar = { elegida = it },
                alSoltar = { estado.elegirCarga(if (it == habitual) null else it) },
            )
            if (g.plan.isNotEmpty()) {
                Spacer(Modifier.height(Espacio.s))
                LoQueInscribes(hoy.mas(1), g.plan[0], g.cuelloDeBotella.toSet(), nombre, alVerEnMapa)
            }
        }
    }

    if (esApaisado()) {
        Row(Modifier.fillMaxSize().padding(start = margenPantalla(), end = margenPantalla(), bottom = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.xl)) {
            // El anillo mide 270 dp: centrado en el alto que hay, sin scroll
            Box(Modifier.width(ANCHO_ANILLO_APAISADO).fillMaxHeight(), contentAlignment = Alignment.Center) { anillo(Modifier) }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                titular(52)
                Spacer(Modifier.height(Espacio.m))
                tarjetas()
            }
        }
    } else {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xs, bottom = Espacio.xxl)) {
            titular(60)
            anillo(Modifier.padding(top = Espacio.xs, start = 0.dp))
            Column(Modifier.padding(top = Espacio.s)) { tarjetas() }
        }
    }
}

// ───────────────────────────────── Los textos ────────────────────────────────

/** "3 años", "3,5 años" o, si falta menos de un año, "8 meses": de hoy a la fecha de grado, a medio año */
private fun cuantoFalta(g: EstimacionGraduacion): String {
    val final = g.periodoFinal ?: return ""
    val ahora = mesLocal()
    val meses = (final.anio - ahora.anio) * 12 + (if (final.lapso == 1) 6 else 12) - ahora.mes
    if (meses < 12) return meses.coerceAtLeast(1).let { "$it ${if (it == 1) "mes" else "meses"}" }
    val medios = (meses / 6f).roundToInt()
    val anios = medios / 2
    return if (medios % 2 == 0) "$anios ${if (anios == 1) "año" else "años"}" else "$anios,5 años"
}

/** Lo que cambia tu fecha frente a tu ritmo: "Lo más rápido", "+6 meses", "+1 año", "−1 año y medio"… */
private fun consecuencia(semestres: Int, ritmo: Int, esMinimo: Boolean): String {
    if (esMinimo) return "Lo más rápido"
    val d = semestres - ritmo
    if (d == 0) return "Tu ritmo"
    val medios = abs(d)
    val tiempo = if (medios == 1) "6 meses" else "${medios / 2} ${if (medios / 2 == 1) "año" else "años"}" + if (medios % 2 == 1) " y medio" else ""
    return (if (d > 0) "+" else "−") + tiempo
}

/** La nota de una línea bajo el deslizador */
private fun notaDelRitmo(valor: Int, ritmo: Int, cargaMinima: Int): String = when {
    valor >= cargaMinima && cargaMinima < MAX_CARGA -> "Con ${(cargaMinima + 1..MAX_CARGA).joinToString(" u ")} no adelantas: te frena la cadena."
    valor >= cargaMinima -> "Ya vas lo más rápido que deja la cadena."
    valor != ritmo -> "El punto azul es tu ritmo actual: $ritmo."
    else -> "Con $cargaMinima materias por semestre te gradúas antes."
}

// ───────────────────────────────── Tu ritmo ────────────────────────────────

/** "Tu ritmo": el valor grande con su consecuencia a la derecha, el deslizador de 1 a 8 debajo y una nota */
@Composable
private fun TarjetaRitmo(valor: Int, ritmo: Int, consecuencia: String, nota: String, alCambiar: (Int) -> Unit, alSoltar: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().tarjeta(Radio.Grande).padding(Espacio.l)) {
        // Con letra grande o ventana estrecha, la consecuencia baja a su propia línea para no chocar con el título
        val apilada = ventana().compacta || ventana().letraGrande
        val consecuenciaChip: @Composable (Modifier) -> Unit = { m ->
            Box(m.heightIn(min = ALTO_CHIP_RITMO).clip(RoundedCornerShape(Radio.Chip)).background(Paleta.tinta.copy(alpha = 0.09f)).padding(horizontal = Espacio.s), contentAlignment = Alignment.Center) {
                BasicText(consecuencia, style = estilo(Tipo.xs, Paleta.tinta, FontWeight.SemiBold, 16), maxLines = if (apilada) 2 else 1)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText("Tu ritmo", style = estilo(Tipo.xl, Paleta.tinta, FontWeight.SemiBold, 22))
                BasicText("materias por semestre", style = estilo(Tipo.s, Paleta.tinta.copy(alpha = 0.64f), interlinea = 18))
            }
            if (!apilada) consecuenciaChip(Modifier)
            BasicText(
                "$valor",
                Modifier.padding(start = Espacio.s).widthIn(min = 26.dp),
                style = estilo(44, Paleta.tinta, FontWeight.Light, 44).copy(letterSpacing = (-0.03).em, textAlign = TextAlign.End),
            )
        }
        if (apilada) consecuenciaChip(Modifier.padding(top = Espacio.xs))
        Spacer(Modifier.height(Espacio.s))
        Deslizador(valor, ritmo, alCambiar, alSoltar)
        BasicText(nota, Modifier.padding(top = Espacio.s), style = estilo(Tipo.s, Paleta.tinta.copy(alpha = 0.64f), interlinea = 18))
    }
}

/**
 * Riel de 4 dp con el tramo lleno en tinta, pomo blanco de 28 dp, una muesca
 * por número y los números debajo. El pomo sigue al dedo y al soltar se
 * asienta en el número más cercano; su posición solo se lee al dibujar. Un
 * punto azul marca tu ritmo cuando te alejas de él.
 */
@Composable
private fun Deslizador(valor: Int, ritmo: Int, alCambiar: (Int) -> Unit, alSoltar: (Int) -> Unit) {
    // Dónde está el pomo, de 0 a MAX_CARGA - 1 (fraccionario mientras se arrastra)
    var pos by remember { mutableFloatStateOf((valor - 1).toFloat()) }
    var arrastrando by remember { mutableStateOf(false) }
    val asentar = remember { Animatable(0f) }
    LaunchedEffect(valor, arrastrando) {
        if (!arrastrando && pos != (valor - 1).toFloat()) {
            val desde = pos
            asentar.snapTo(0f)
            asentar.animateTo(1f, tween(160, easing = ENFATICA)) { pos = desde + ((valor - 1) - desde) * value }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val radio = 14.dp
        val ancho = maxWidth
        fun centro(n: Int) = radio + (ancho - radio * 2) * (n / (MAX_CARGA - 1f))
        Column {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(Alto.toque)
                    .semantics { contentDescription = "Materias por semestre: $valor" }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val toque = awaitFirstDown()
                            arrastrando = true
                            var ultimo = valor
                            fun mover(x: Float) {
                                val r = 14.dp.toPx()
                                pos = ((x - r) / (size.width - 2 * r) * (MAX_CARGA - 1)).coerceIn(0f, MAX_CARGA - 1f)
                                val nuevo = pos.roundToInt() + 1
                                if (nuevo != ultimo) {
                                    ultimo = nuevo
                                    alCambiar(nuevo)
                                }
                            }
                            mover(toque.position.x)
                            drag(toque.id) {
                                mover(it.position.x)
                                it.consume()
                            }
                            pos = pos.roundToInt().toFloat()
                            arrastrando = false
                            alSoltar(ultimo)
                        }
                    },
            ) {
                val r = 14.dp.toPx()
                val yRiel = 18.dp.toPx()
                val grueso = 4.dp.toPx()
                val util = size.width - 2 * r
                val x = r + util * pos / (MAX_CARGA - 1)
                drawRoundRect(Paleta.tinta.copy(alpha = 0.10f), Offset(0f, yRiel - grueso / 2), Size(size.width, grueso), CornerRadius(grueso / 2))
                drawRoundRect(Paleta.tinta, Offset(0f, yRiel - grueso / 2), Size(x, grueso), CornerRadius(grueso / 2))
                for (n in 0 until MAX_CARGA) {
                    val cx = r + util * n / (MAX_CARGA - 1)
                    drawRect(Paleta.tinta.copy(alpha = 0.28f), Offset(cx - 0.75.dp.toPx(), yRiel + 16.dp.toPx()), Size(1.5.dp.toPx(), 6.dp.toPx()))
                }
                // Tu ritmo, un punto azul sobre el riel, mientras no estés en él
                if (valor != ritmo) drawCircle(Paleta.azul, 3.dp.toPx(), Offset(r + util * (ritmo - 1) / (MAX_CARGA - 1), yRiel - 11.dp.toPx()))
                // El pomo: blanco, con una sombra suave debajo
                drawCircle(Color.Black.copy(alpha = 0.10f), r + 3.dp.toPx(), Offset(x, yRiel + 3.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.16f), r + 1.dp.toPx(), Offset(x, yRiel + 2.dp.toPx()))
                drawCircle(Color.White, r, Offset(x, yRiel))
            }
            Box(Modifier.fillMaxWidth().height(18.dp)) {
                for (n in 1..MAX_CARGA) {
                    val elegido = n == valor
                    Box(Modifier.offset(x = centro(n - 1) - 20.dp).width(40.dp), contentAlignment = Alignment.Center) {
                        BasicText("$n", style = estilo(Tipo.xs, if (elegido) Paleta.tinta else Paleta.tenue, if (elegido) FontWeight.SemiBold else FontWeight.Normal, 16), maxLines = 1)
                    }
                }
            }
        }
    }
}

// ───────────────────────────────── Lo que inscribes ────────────────────────────────

/** El próximo semestre: sus materias con el candado abierto en azul y el botón que las lleva al mapa */
@Composable
private fun LoQueInscribes(periodo: Periodo, codigos: List<String>, cadena: Set<String>, nombre: (String) -> String, alVerEnMapa: (List<String>) -> Unit) {
    Column(Modifier.fillMaxWidth().tarjeta(Radio.Grande).padding(Espacio.l)) {
        BasicText("LO QUE INSCRIBES", style = versal(Paleta.azul).copy(letterSpacing = 0.06.em))
        Row(Modifier.fillMaxWidth().padding(top = Espacio.xxs), verticalAlignment = Alignment.Bottom) {
            BasicText(periodo.rango(), Modifier.weight(1f), style = estilo(20, Paleta.tinta, FontWeight.SemiBold, 26))
            BasicText("${codigos.size} ${if (codigos.size == 1) "materia" else "materias"}", style = estilo(Tipo.l, Paleta.tinta.copy(alpha = 0.64f), interlinea = 22))
        }
        Column(Modifier.padding(top = Espacio.xs)) {
            codigos.forEach { c ->
                Row(Modifier.fillMaxWidth().heightIn(min = Alto.toque), verticalAlignment = Alignment.CenterVertically) {
                    IconoTrazo(IconosHoja.candadoAbierto, Paleta.azul, 20.dp, 1.9f)
                    BasicText(nombre(c), Modifier.weight(1f).padding(start = Espacio.s, top = Espacio.xxs, bottom = Espacio.xxs), style = estilo(Tipo.l, Paleta.tinta, interlinea = 22))
                    if (c in cadena) IconoTrazo(IconosHoja.eslabon, Paleta.tinta.copy(alpha = 0.44f), 16.dp, 1.8f)
                }
            }
        }
        Box(
            Modifier.padding(top = Espacio.s).fillMaxWidth().heightIn(min = Alto.fila).clip(RoundedCornerShape(Radio.Grande)).background(Paleta.tinta)
                .clickable(role = Role.Button) { alVerEnMapa(codigos) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoRelleno(Phosphor.mapa, false, Paleta.lienzo, 18.dp)
                BasicText("Verlas en el mapa", Modifier.padding(start = Espacio.xs), style = estilo(Tipo.l, Paleta.lienzo, FontWeight.SemiBold, 22))
            }
        }
    }
}

// ───────────────────────────────── Lo que no puedes atrasar ────────────────────────────────

/**
 * La cadena crítica: cada materia abre la siguiente, así que atrasar una
 * mueve la fecha. En la tarjeta va resumida, una línea de nodos con la
 * primera (la que inscribes ya) y la última (el trabajo de grado); al tocarla
 * se despliega entera, en vertical y con el período de cada una. Si el plan va
 * con margen, la frase dice cuánto.
 */
@Composable
private fun NoPuedesAtrasar(g: EstimacionGraduacion, hoy: Periodo, nombre: (String) -> String) {
    val cadena = g.cuelloDeBotella
    val holgura = (g.plan.size - cadena.size).coerceAtLeast(0)
    // El período de cada una: donde la mete el plan; si no está, el siguiente al de la anterior
    val periodos = remember(g) {
        var anterior = -1
        cadena.map { c ->
            val k = g.plan.indexOfFirst { c in it }.takeIf { it >= 0 } ?: (anterior + 1)
            anterior = k
            hoy.mas(k + 1)
        }
    }
    var abierta by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierta) 90f else 0f, tween(220, easing = ENFATICA), label = "giro")
    Column(Modifier.fillMaxWidth().tarjeta(Radio.Grande).clickable(role = Role.Button) { abierta = !abierta }.padding(Espacio.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("No puedes atrasar", Modifier.weight(1f), style = estilo(Tipo.l, Paleta.tinta, FontWeight.SemiBold, 20))
            IconoTrazo(Lucide.siguiente, Paleta.tinta.copy(alpha = 0.44f), 18.dp, 2f, Modifier.graphicsLayer { rotationZ = giro })
        }
        BasicText(
            "Cada una abre la siguiente. " + when (holgura) {
                0 -> "Si atrasas una, te gradúas 6 meses después."
                1 -> "Tienes 1 semestre de margen antes de que se mueva tu fecha."
                else -> "Tienes $holgura semestres de margen antes de que se mueva tu fecha."
            },
            Modifier.padding(top = Espacio.xxs),
            style = estilo(Tipo.s, Paleta.tinta.copy(alpha = 0.64f), interlinea = 18),
        )
        AnimatedVisibility(!abierta, enter = expandVertically(tween(260, easing = ENFATICA)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(120))) {
            Column {
                CadenaHorizontal(cadena.size)
                Row(Modifier.fillMaxWidth().padding(top = Espacio.xs), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Column(Modifier.weight(1f)) {
                        BasicText(nombre(cadena.first()), style = estilo(Tipo.s, Paleta.tinta, FontWeight.Medium, 17), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        BasicText(periodos.first().rango(), style = estilo(Tipo.s, Paleta.azul, interlinea = 17))
                    }
                    if (cadena.size > 1) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            BasicText(nombre(cadena.last()), style = estilo(Tipo.s, Paleta.tinta, FontWeight.Medium, 17).copy(textAlign = TextAlign.End), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            BasicText(periodos.last().rango(), style = estilo(Tipo.s, Paleta.tinta.copy(alpha = 0.44f), interlinea = 17))
                        }
                    }
                }
            }
        }
        AnimatedVisibility(abierta, enter = expandVertically(tween(260, easing = ENFATICA)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(120))) {
            Column(Modifier.padding(top = Espacio.s)) {
                cadena.forEachIndexed { k, c ->
                    val primera = k == 0
                    val ultima = k == cadena.lastIndex
                    if (k > 0) {
                        Row(Modifier.padding(start = Espacio.l, top = Espacio.xxs, bottom = Espacio.xxs), verticalAlignment = Alignment.CenterVertically) {
                            IconoTrazo(IconosHoja.flechaAbajo, Paleta.tenue, 14.dp, 1.8f)
                            BasicText("abre", Modifier.padding(start = Espacio.xs), style = estilo(Tipo.s, Paleta.tenue, interlinea = 18))
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Alto.boton)
                            .then(
                                if (primera) Modifier.clip(RoundedCornerShape(Radio.Tarjeta)).background(Paleta.azul.copy(alpha = 0.08f)).border(1.dp, Paleta.azul.copy(alpha = 0.45f), RoundedCornerShape(Radio.Tarjeta))
                                else Modifier,
                            )
                            .padding(horizontal = Espacio.m, vertical = Espacio.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            when {
                                ultima -> IconoTrazo(Lucide.birrete, Paleta.suave, 20.dp, 1.9f)
                                primera -> IconoTrazo(IconosHoja.candadoAbierto, Paleta.azul, 20.dp, 1.9f)
                                else -> IconoTrazo(IconosHoja.candado, Paleta.suave, 20.dp, 1.9f)
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = Espacio.m, end = Espacio.xs)) {
                            BasicText(nombre(c), style = estilo(16, Paleta.tinta, interlinea = 22))
                            if (primera) BasicText("Inscríbela en ${periodos[k].rango()}", style = estilo(Tipo.m, Paleta.azul, interlinea = 20))
                        }
                        if (!primera) BasicText(periodos[k].rango(), style = estilo(Tipo.s, Paleta.tenue, interlinea = 18))
                    }
                }
            }
        }
    }
}

/** Los nodos de la cadena unidos por una línea: el primero azul, los de en medio huecos y el último, el birrete */
@Composable
private fun CadenaHorizontal(nodos: Int) {
    Canvas(Modifier.padding(top = Espacio.m).fillMaxWidth().height(24.dp)) {
        val u = 1.dp.toPx()
        val tinta = Paleta.tinta
        val n = nodos.coerceAtLeast(1)
        val xs = List(n) { j -> 10f * u + j * (size.width - 30f * u) / max1(n - 1) }
        val cy = 12f * u
        fun radio(j: Int) = when (j) {
            0 -> 6f * u
            n - 1 -> 12f * u
            else -> 4.5f * u
        }
        for (j in 0 until n - 1) {
            drawLine(tinta.copy(alpha = 0.2f), Offset(xs[j] + radio(j) + 1.5f * u, cy), Offset(xs[j + 1] - radio(j + 1) - 1.5f * u, cy), strokeWidth = 1.5f * u, cap = StrokeCap.Round)
        }
        xs.forEachIndexed { j, x ->
            when {
                j == 0 -> drawCircle(Paleta.azul, 6f * u, Offset(x, cy))
                j == n - 1 -> icono(Lucide.birrete, tinta, x - 11f * u, 1f * u, 22f * u, 1.9f)
                else -> drawCircle(tinta.copy(alpha = 0.5f), 4.5f * u, Offset(x, cy), style = Stroke(1.5f * u))
            }
        }
    }
}

private fun max1(v: Int) = if (v < 1) 1 else v
