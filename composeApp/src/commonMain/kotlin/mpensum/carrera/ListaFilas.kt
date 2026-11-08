package mpensum.carrera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpensum.componentes.colorEstado
import mpensum.componentes.iconoEstado
import mpensum.logica.FilaCasilla
import mpensum.logica.FilaMateria
import mpensum.logica.SeccionLista
import mpensum.logica.Situacion
import mpensum.logica.Tono
import mpensum.tema.Movimiento
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tipo
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Las piezas de la lista: el grupo de cristal de cada semestre, su cabecera
 * con el aro y las filas. Cada fila recibe sus datos ya resueltos y se anima
 * sola, en la capa o al dibujar: marcar una no repinta las de al lado.
 */

/** La curva enfática de Material: arranca decidida y se posa despacio */
/** El "ease" de CSS, para los fundidos */
private val SUAVE = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

internal val COLUMNA_ICONO = 52.dp
private val ALTO_CABECERA = 60.dp
private val ALTO_FILA = Alto.grande
private val RADIO_GRUPO = Radio.Opcion
/** Lo que se aparta el nombre por la derecha cuando asoma «Deshacer» */
private val HUECO_DESHACER_FILA = 100.dp
private val HUECO_DESHACER_CABECERA = 76.dp
/** Lo que dura "Deshacer" a la vista, y el rojo de una bloqueada */
internal const val PLAZO_DESHACER_MS = 4000L
private const val AVISO_MS = 1400L

private val SEPARADOR get() = porTema(Paleta.borde, Paleta.tinta.copy(alpha = 0.08f))
/** Cuánto se mete la raya entre filas: bajo el icono, a la altura del texto */
private val SANGRIA = 56.dp
/**
 * El fondo del grupo: en oscuro, el panel opaco (la luz de la carrera ya no
 * se cuela entre filas); en claro, papel blanco casi entero sobre el lienzo.
 */
private val FONDO_GRUPO get() = porTema(Paleta.panelSuave, Color.White.copy(alpha = 0.78f))

internal fun colorDe(t: Tono): Color = when (t) {
    Tono.TENUE -> Paleta.tenue
    Tono.VERDE -> Paleta.verde
    Tono.CURSANDO -> Paleta.cursando
    Tono.AZUL -> Paleta.azul
}

/** Lo que la lista hace al tocarla; una sola instancia, para que las filas no cambien de parámetros */
@Stable
internal class AccionesLista(
    val tocar: (FilaMateria) -> Unit,
    val mantener: (String) -> Unit,
    val elegir: (String) -> Unit,
    val aro: (SeccionLista) -> Unit,
    val plegar: (String, Boolean) -> Unit,
    val deshacer: () -> Unit,
)

/**
 * Un degradado lineal con ángulo de CSS (0° hacia arriba, en el sentido del
 * reloj) sobre una caja: así se calca el filo de luz de las maquetas.
 */
internal fun degradadoCss(grados: Float, size: Size, vararg paradas: Pair<Float, Color>): Brush {
    val r = grados * PI.toFloat() / 180f
    val dx = sin(r)
    val dy = -cos(r)
    val largo = abs(size.width * dx) + abs(size.height * dy)
    val c = Offset(size.width / 2, size.height / 2)
    val d = Offset(dx * largo / 2, dy * largo / 2)
    return Brush.linearGradient(*paradas, start = c - d, end = c + d)
}

/** El filo de 1 px del cristal, más claro arriba a la izquierda, por donde entra la luz */
internal fun filoCristal(size: Size, alfa: Float = 1f) = degradadoCss(
    148f, size,
    // La tinta hace el filo: luz en oscuro y, a la mitad, una sombra fina en claro
    *paradasFilo(alfa * porTema(1f, 0.55f)),
)

private fun paradasFilo(a: Float) = arrayOf(
    0f to Paleta.tinta.copy(alpha = 0.22f * a),
    0.38f to Paleta.tinta.copy(alpha = 0.05f * a),
    0.58f to Paleta.tinta.copy(alpha = 0f),
    1f to Paleta.tinta.copy(alpha = 0.08f * a),
)

/**
 * Un semestre: la cabecera (aro, nombre y resumen, chevrón) y, debajo, sus
 * filas, que se pliegan como en Ajustes de iOS. Plegado no compone las filas.
 */
@Composable
internal fun SeccionDeLista(
    seccion: SeccionLista,
    abierto: Boolean,
    deshacerSeccion: Boolean,
    deshacerFila: String?,
    avisoCodigo: String?,
    avisoVez: Int,
    acciones: AccionesLista,
    modifier: Modifier = Modifier,
) {
    val apertura = remember { Animatable(if (abierto) 1f else 0f) }
    LaunchedEffect(abierto) { apertura.animateTo(if (abierto) 1f else 0f, tween(360, easing = Movimiento.ESTANDAR)) }
    val opacidad = animateFloatAsState(if (abierto) 1f else 0f, tween(240, easing = SUAVE), label = "filas")
    // Solo cambia al terminar de plegarse: no recompone en cada cuadro de la animación
    val asoma by remember { derivedStateOf { apertura.value > 0f } }

    Column(
        modifier
            .fillMaxWidth()
            .drawWithCache {
                val r = CornerRadius(RADIO_GRUPO.toPx())
                val g = 1.dp.toPx()
                val filo = filoCristal(size)
                onDrawBehind {
                    drawRoundRect(FONDO_GRUPO, cornerRadius = r)
                    if (!Tema.claro) {
                        // El anillo de 1 dp y, encima, la línea de luz que cae por el canto de arriba
                        drawRoundRect(Paleta.tinta.copy(alpha = 0.07f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2), style = Stroke(g))
                        drawLine(Color.White.copy(alpha = 0.06f), Offset(r.x, g * 1.5f), Offset(size.width - r.x, g * 1.5f), g)
                    }
                    if (Tema.claro) drawRoundRect(filo, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2), style = Stroke(g))
                }
            },
    ) {
        CabeceraSeccion(seccion, abierto, deshacerSeccion, acciones)
        if (abierto || asoma) {
            Box(
                Modifier
                    .clipToBounds()
                    // El alto se anima al medir: el contenido no se recompone, solo se recorta
                    .layout { medible, restricciones ->
                        val p = medible.measure(restricciones.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                        val alto = (p.height * apertura.value).roundToInt()
                        layout(p.width, alto) { p.place(0, 0) }
                    }
                    .graphicsLayer { alpha = opacidad.value },
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(1.dp).drawBehind { drawRect(SEPARADOR) })
                    val total = seccion.materias.size + seccion.casillas.size
                    seccion.materias.forEachIndexed { i, fila ->
                        key(fila.codigo) {
                            FilaDeMateria(
                                fila = fila,
                                ultima = i == total - 1,
                                conDeshacer = deshacerFila == fila.codigo,
                                aviso = if (avisoCodigo == fila.codigo) avisoVez else 0,
                                acciones = acciones,
                            )
                        }
                    }
                    seccion.casillas.forEachIndexed { i, fila ->
                        key(fila.casilla) {
                            FilaDeCasilla(fila, ultima = seccion.materias.size + i == total - 1, acciones = acciones)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraSeccion(seccion: SeccionLista, abierto: Boolean, conDeshacer: Boolean, acciones: AccionesLista) {
    val giro = animateFloatAsState(if (abierto) 90f else 0f, tween(320, easing = Movimiento.ESTANDAR), label = "chevron")
    val i = inter()
    Box(
        Modifier
            .fillMaxWidth()
            .height(ALTO_CABECERA)
            .drawBehind {
                // En oscuro, la cabecera del semestre abierto se alza un punto sobre sus filas
                if (abierto && !Tema.claro) {
                    val r = CornerRadius(RADIO_GRUPO.toPx())
                    val p = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, r, r, CornerRadius.Zero, CornerRadius.Zero)) }
                    drawPath(p, lerp(Paleta.panelSuave, Paleta.tinta, 0.035f))
                }
            },
    ) {
        Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            val s = seccion.semestre
            Box(
                Modifier
                    .size(COLUMNA_ICONO, ALTO_CABECERA)
                    .then(
                        // El aro de un grupo de electivas cuenta UC, no aprueba nada
                        if (s == null) Modifier
                        else Modifier
                            .semantics {
                                contentDescription = if (seccion.completo) "Desmarcar el semestre $s" else "Aprobar el semestre $s"
                                stateDescription = when {
                                    seccion.completo -> "Completo"
                                    seccion.aprobado > 0f -> "A medias"
                                    else -> "Sin aprobar"
                                }
                            }
                            .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { acciones.aro(seccion) },
                    ),
                contentAlignment = Alignment.Center,
            ) { AroSemestre(seccion.aprobado, seccion.cursado, seccion.completo) }

            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { contentDescription = "${seccion.titulo}, ${seccion.resumen}. ${if (abierto) "Plegar" else "Desplegar"}" }
                    .clickable(interactionSource = null, indication = null, role = Role.Button) { acciones.plegar(seccion.clave, abierto) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // "Deshacer" ocupa el final de la cabecera sin mover el chevrón: el resumen se recorta antes
                Column(Modifier.weight(1f).padding(end = if (conDeshacer) HUECO_DESHACER_CABECERA else 0.dp)) {
                    BasicText(
                        seccion.titulo,
                        style = TextStyle(fontFamily = jost(), fontWeight = porTema(FontWeight.Normal, FontWeight.Light), fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.005).em, color = Paleta.tinta),
                        maxLines = 1,
                    )
                    BasicText(
                        buildAnnotatedString {
                            append(seccion.resumen)
                            if (seccion.coletilla.isNotEmpty()) withStyle(SpanStyle(color = Paleta.cursando, fontWeight = FontWeight.Medium)) { append(seccion.coletilla) }
                        },
                        Modifier.padding(top = Espacio.hilo),
                        style = TextStyle(fontFamily = i, fontSize = Tipo.s, lineHeight = 16.sp, fontFeatureSettings = "tnum", color = colorDe(seccion.tonoResumen)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Canvas(Modifier.size(Alto.toque, ALTO_CABECERA).graphicsLayer { rotationZ = giro.value }) {
                    val e = size.width / 44f
                    val trazo = Path().apply {
                        moveTo(19.5f * e, 25f * e)
                        lineTo(24.5f * e, 30f * e)
                        lineTo(19.5f * e, 35f * e)
                    }
                    drawPath(trazo, Paleta.tenue, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        AnimatedVisibility(conDeshacer, Modifier.align(Alignment.TopEnd).padding(end = Alto.toque, top = Espacio.xs), enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            BotonDeshacer(Modifier.heightIn(min = Alto.toque).padding(start = Espacio.s, end = Espacio.xs), acciones.deshacer)
        }
    }
}

@Composable
private fun BotonDeshacer(relleno: Modifier, alPulsar: () -> Unit) {
    Box(
        Modifier.clickable(interactionSource = null, indication = null, role = Role.Button, onClick = alPulsar).then(relleno),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("Deshacer", style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = Tipo.s, color = Paleta.tinta))
    }
}

/**
 * El aro del semestre: lleno de verde con su check si está completo; si no,
 * el arco de lo aprobado en verde y, detrás, lo que cursas en tinta a media opacidad. Los
 * arcos se mueven al dibujar, sin recomponer.
 */
@Composable
private fun AroSemestre(aprobado: Float, cursado: Float, completo: Boolean) {
    val verde = remember { Animatable(aprobado) }
    val ambar = remember { Animatable(cursado) }
    LaunchedEffect(aprobado, cursado) {
        launch { verde.animateTo(aprobado, tween(400, easing = Movimiento.ESTANDAR)) }
        ambar.animateTo(cursado, tween(400, easing = Movimiento.ESTANDAR))
    }
    Canvas(Modifier.size(24.dp)) {
        val lado = size.minDimension
        if (completo) {
            iconoEstado(Situacion.HECHA, 0f, 0f, lado, Paleta.lienzo)
            return@Canvas
        }
        val e = lado / 24f
        val r = 9.25f * e
        val g = 1.5f * e
        val caja = Size(r * 2, r * 2)
        val esquina = Offset(center.x - r, center.y - r)
        drawCircle(Paleta.tinta.copy(alpha = 0.16f), r, style = Stroke(g))
        val v = verde.value * 360f
        val a = ambar.value * 360f
        if (v > 0.1f) drawArc(Paleta.verde, -90f, v, false, esquina, caja, style = Stroke(g))
        if (a > 0.1f) drawArc(Paleta.cursando.copy(alpha = 0.45f), -90f + v, a, false, esquina, caja, style = Stroke(g))
    }
}

/** El icono de estado de una fila; la próxima en gris tenue, como en la maqueta */
internal fun DrawScope.iconoFila(s: Situacion, lado: Float) = iconoEstado(
    s, (size.width - lado) / 2, (size.height - lado) / 2, lado, Paleta.panel,
    color = if (s == Situacion.PROXIMA) Paleta.tenue else colorEstado(s),
)

private fun colorNombre(s: Situacion): Color = when (s) {
    Situacion.HECHA, Situacion.PROXIMA -> Paleta.suave
    Situacion.CURSANDO, Situacion.INSCRIBIBLE -> Paleta.tinta
    Situacion.LEJANA -> Paleta.tenue
}

/** El brillo tras tocar una fila y la raya que la separa de la siguiente; la última redondea con el grupo */
private fun DrawScope.fondoFila(brillo: Float, ultima: Boolean) {
    if (brillo > 0f) {
        val color = Paleta.tinta.copy(alpha = 0.045f * brillo)
        if (ultima) {
            val r = CornerRadius(RADIO_GRUPO.toPx())
            val p = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius.Zero, CornerRadius.Zero, r, r))
            }
            drawPath(p, color)
        } else drawRect(color)
    }
    if (!ultima) {
        val g = 1.dp.toPx()
        drawRect(SEPARADOR, Offset(SANGRIA.toPx(), size.height - g), Size(size.width - SANGRIA.toPx(), g))
    }
}

/**
 * Una materia. Cualquier toque abre su ficha, la misma del mapa, con los
 * botones de estado; la marca ya no cambia desde la fila.
 */
@Composable
private fun FilaDeMateria(
    fila: FilaMateria,
    ultima: Boolean,
    conDeshacer: Boolean,
    aviso: Int,
    acciones: AccionesLista,
) {
    val pop = remember { Animatable(1f) }
    val brillo = remember { Animatable(0f) }
    val sacudida = remember { Animatable(1f) }
    var rojo by remember { mutableStateOf(false) }
    // La situación con la que se pintó la primera vez: al volver a la vista no salta nada
    val vista = remember { arrayOf(fila.situacion) }
    LaunchedEffect(fila.situacion) {
        if (fila.situacion == vista[0]) return@LaunchedEffect
        vista[0] = fila.situacion
        launch {
            brillo.snapTo(1f)
            delay(500)
            brillo.animateTo(0f, tween(300, easing = SUAVE))
        }
        pop.snapTo(0.72f)
        pop.animateTo(1f, tween(260, easing = Movimiento.ESTANDAR))
    }
    LaunchedEffect(aviso) {
        if (aviso == 0) {
            rojo = false
            return@LaunchedEffect
        }
        rojo = true
        launch {
            sacudida.snapTo(0f)
            sacudida.animateTo(1f, tween(360, easing = LinearEasing))
        }
        delay(AVISO_MS)
        rojo = false
    }
    val colorNota = animateColorAsState(if (rojo) Paleta.rojo else colorDe(fila.tono), tween(300), label = "nota")
    val nombre = animateColorAsState(colorNombre(fila.situacion), tween(300), label = "nombre")

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA)
            .drawBehind { fondoFila(brillo.value, ultima) }
            .semantics { contentDescription = fila.descripcion }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClickLabel = "Ver la ficha") { acciones.tocar(fila) },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = ALTO_FILA), verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                Modifier
                    .size(COLUMNA_ICONO, ALTO_FILA)
                    .graphicsLayer {
                        val t = sacudida.value
                        translationX = if (t < 1f) 3.dp.toPx() * sin(t * PI.toFloat() * 4f) * (1f - t) else 0f
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            ) {
                iconoFila(fila.situacion, 20.dp.toPx())
            }
            Column(Modifier.weight(1f).padding(vertical = Espacio.xs).padding(end = if (conDeshacer) HUECO_DESHACER_FILA else Espacio.m)) {
                BasicText(
                    fila.nombre,
                    style = TextStyle(fontFamily = jost(), fontWeight = porTema(FontWeight.Normal, FontWeight.Light), fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em),
                    color = { nombre.value },
                    // Dos líneas y la fila crece: los nombres largos ya no se cortan con «…»
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fila.uc != null || fila.nota.isNotEmpty()) Row(Modifier.padding(top = Espacio.hilo)) {
                    fila.uc?.let {
                        BasicText("$it UC", Modifier.alignByBaseline(), style = estiloNota(), maxLines = 1)
                    }
                    if (fila.nota.isNotEmpty()) BasicText(
                        if (fila.uc != null) " · ${fila.nota}" else fila.nota,
                        Modifier.alignByBaseline(),
                        style = estiloNota(),
                        color = { colorNota.value },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        AnimatedVisibility(conDeshacer, Modifier.align(Alignment.CenterEnd), enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            BotonDeshacer(Modifier.heightIn(min = ALTO_FILA).padding(start = Espacio.s, end = Espacio.m), acciones.deshacer)
        }
    }
}

/** Una casilla de electiva sin elegir: el aro punteado, su grupo y "Elegir" */
@Composable
private fun FilaDeCasilla(fila: FilaCasilla, ultima: Boolean, acciones: AccionesLista) {
    val i = inter()
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA)
            .drawBehind { fondoFila(0f, ultima) }
            .semantics { contentDescription = "${fila.nombre}, por elegir" }
            .clickable(interactionSource = null, indication = null, role = Role.Button) { acciones.elegir(fila.casilla) },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = ALTO_FILA), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(COLUMNA_ICONO, ALTO_FILA)) {
                val e = 20.dp.toPx() / 20f
                drawCircle(
                    Paleta.tenue, 7.25f * e,
                    style = Stroke(1.5f * e, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.6f * e, 2.6f * e))),
                )
            }
            Column(Modifier.weight(1f).padding(vertical = Espacio.xs).padding(end = Espacio.s)) {
                BasicText(
                    fila.nombre,
                    style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.suave),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(fila.nota, Modifier.padding(top = Espacio.hilo), style = estiloNota(), maxLines = 1)
            }
            Box(
                Modifier
                    .padding(end = Espacio.s)
                    .heightIn(min = 32.dp)
                    .drawWithCache {
                        val r = CornerRadius(size.height / 2)
                        val g = 1.dp.toPx()
                        onDrawBehind {
                            drawRoundRect(Paleta.tinta.copy(alpha = 0.07f), cornerRadius = r)
                            drawRoundRect(Paleta.tinta.copy(alpha = 0.12f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r.x - g / 2), style = Stroke(g))
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = Espacio.xxs),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("Elegir", style = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = Tipo.s, color = Paleta.tinta))
            }
        }
    }
}
