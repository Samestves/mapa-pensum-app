package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.SOMBRA_FLOTANTE
import mpensum.componentes.ventana
import mpensum.componentes.iconoEstado
import mpensum.datos.Asignatura
import mpensum.logica.EstadoMateria
import mpensum.logica.Marca
import mpensum.logica.Situacion
import mpensum.tema.PESO_NOMBRE
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.Radio
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema
import mpensum.tema.plex

/*
 * La ficha de la materia en el teléfono (DetalleAsignatura.jsx y
 * ficha/TarjetaTelefono.jsx): una tarjeta que flota abajo, donde llega el
 * pulgar, sin oscurecer el mapa que está explicando. Arriba quién es; en
 * medio por qué está como está, qué pide y qué abre; al pie, las tres marcas.
 * Se cierra con la X, tocando el vacío o bajándola con el dedo.
 */

private val SUBIR = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val BAJAR = CubicBezierEasing(0.4f, 0f, 1f, 1f)
/** Cuánto hay que bajarla para cerrarla, o con qué velocidad (dp/ms) basta un tirón */
private const val CIERRE_DISTANCIA = 90f
private const val CIERRE_VELOCIDAD = 0.6f

/** Lo que dice cada situación en la cabecera, con su color (ASPECTO.marca de la web) */
private fun palabraDe(s: Situacion): Pair<String, Color> = when (s) {
    Situacion.HECHA -> "APROBADA" to Paleta.verde
    Situacion.CURSANDO -> "CURSANDO" to Paleta.ambar
    Situacion.INSCRIBIBLE -> "DISPONIBLE" to Paleta.azul
    Situacion.PROXIMA -> "PRÓXIMA" to Paleta.suave
    Situacion.LEJANA -> "BLOQUEADA" to Paleta.tenue
}

private fun nombreArea(area: String?) = when (area) {
    "generales" -> "Generales"
    "ciencias-basicas" -> "Ciencias básicas"
    "estadistica" -> "Estadística"
    "computacion" -> "Computación"
    "electronica" -> "Electrónica"
    "sistemas" -> "Sistemas"
    "gestion" -> "Gestión"
    "tesis" -> "Trabajo de grado"
    else -> null
}

private val ANCHO_PANEL_LATERAL = 360.dp
/** El texto de los avisos y notas de la ficha (12,5 sp, entre Tipo.xs y Tipo.s) */
private val TEXTO_AVISO = 12.5.sp
/** Por debajo de este ancho por marca, el selector pone el icono sobre la etiqueta */
private val ANCHO_MARCA_HOLGADA = 112.dp

/** Dónde cae la ficha cuando va al lado de la lista (horizontal): sus márgenes desde el interior */
class PanelLateral(val inicio: Dp, val fin: Dp, val arriba: Dp)

@Composable
fun FichaMateria(
    codigo: String?,
    estado: EstadoCarrera,
    reservaAbajo: Dp,
    alCerrar: () -> Unit,
    alMarcar: (String, Marca?) -> Unit,
    alIrA: (String) -> Unit,
    alCambiarElectiva: (String) -> Unit,
    alTapar: (Float) -> Unit,
    /** Al lado de la lista, en horizontal: un panel de alto completo, con las marcas fijas abajo */
    lateral: PanelLateral? = null,
    /** Horizontal sin lista al lado (el mapa): la ficha flota, pero sin estirarse a todo el ancho */
    horizontal: Boolean,
) {
    // La última que se enseñó sigue pintándose mientras baja, el tiempo de su salida
    var mostrado by remember { mutableStateOf<String?>(null) }
    val entrada = remember { Animatable(0f) }
    val salida = remember { Animatable(0f) }
    val bajada = remember { Animatable(0f) }
    LaunchedEffect(codigo) {
        if (codigo != null) {
            val nueva = mostrado == null || salida.value > 0f
            mostrado = codigo
            if (nueva) {
                salida.snapTo(0f)
                bajada.snapTo(0f)
                entrada.snapTo(0f)
                entrada.animateTo(1f, tween(320, easing = SUBIR))
            }
        } else if (mostrado != null) {
            salida.animateTo(1f, tween(240, easing = BAJAR))
            mostrado = null
            salida.snapTo(0f)
        }
    }
    val actual = mostrado ?: return
    val materia = estado.layout.porCodigo[actual]?.asignatura ?: return
    val alcance = rememberCoroutineScope()
    val densidad = LocalDensity.current.density

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tope = if (horizontal) maxHeight - reservaAbajo - Espacio.xl else minOf(maxHeight * 0.66f, maxHeight - reservaAbajo - 64.dp)
        val forma = RoundedCornerShape(if (lateral != null) Radio.Grande else Radio.Campo)
        val caja = if (lateral != null) Modifier.fillMaxSize().padding(start = lateral.inicio, end = lateral.fin, top = lateral.arriba, bottom = reservaAbajo)
        else Modifier.fillMaxSize().padding(start = Espacio.s, end = Espacio.s, bottom = reservaAbajo)
        Box(caja, contentAlignment = if (horizontal) Alignment.BottomEnd else Alignment.BottomCenter) {
            Column(
                Modifier
                    // Al lado de la lista es una columna fija a la derecha: no se come a la lista ni sus filtros
                    .then(if (horizontal) Modifier.widthIn(max = if (lateral != null) ANCHO_PANEL_LATERAL else 480.dp) else Modifier)
                    .fillMaxWidth()
                    .then(if (lateral != null) Modifier.fillMaxHeight() else Modifier.heightIn(max = tope))
                    // Cuánto tapa del mapa: el mapa se corre para que la materia quede a la vista encima
                    .onSizeChanged { alTapar(it.height / densidad + reservaAbajo.value) }
                    .graphicsLayer {
                        val e = entrada.value
                        val s = salida.value
                        translationY = ((1f - e) * 28f + s * 56f + bajada.value) * density
                        val escala = 0.985f + 0.015f * e
                        scaleX = escala
                        scaleY = escala
                        alpha = (e * (1f - s)) * (if (bajada.value > 0f) maxOf(0.4f, 1f - bajada.value / 320f) else 1f)
                    }
                    .shadow(28.dp, forma, ambientColor = SOMBRA_FLOTANTE, spotColor = SOMBRA_FLOTANTE)
                    .clip(forma)
                    .background(Paleta.panel)
                    .border(1.dp, BORDE_PANEL, forma)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) {
                // La cabecera es también el asa: desde ella se arrastra la tarjeta hacia abajo para cerrarla
                Column(
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val abajo = awaitFirstDown()
                            val rastreo = VelocityTracker()
                            var dy = 0f
                            do {
                                val e = awaitPointerEvent()
                                val dedo = e.changes.firstOrNull { it.id == abajo.id } ?: break
                                rastreo.addPosition(dedo.uptimeMillis, dedo.position)
                                dy = ((dedo.position.y - abajo.position.y) / densidad).coerceAtLeast(0f)
                                if (dy > 2f) dedo.consume()
                                alcance.launch { bajada.snapTo(dy) }
                            } while (dedo.pressed)
                            val velocidad = rastreo.calculateVelocity().y / densidad / 1000f
                            if (dy > CIERRE_DISTANCIA || velocidad > CIERRE_VELOCIDAD) alCerrar()
                            else alcance.launch { bajada.animateTo(0f, tween(240, easing = SUBIR)) }
                        }
                    },
                ) {
                    CabeceraFicha(materia, estado, alCerrar)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(BORDE_PANEL))
                Column(
                    Modifier.weight(1f, fill = lateral != null).verticalScroll(rememberScrollState()).padding(start = Espacio.l, end = Espacio.l, top = 14.dp, bottom = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    Informacion(materia, estado, alIrA)
                    estado.casillaDe(materia.codigo)?.let { casilla -> BotonCambiarElectiva { alCambiarElectiva(casilla) } }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(BORDE_PANEL))
                // Las marcas, al pie: es lo que se viene a hacer, y abajo llega el pulgar sin cambiar de mano
                Box(Modifier.padding(start = Espacio.m, end = Espacio.m, top = Espacio.s, bottom = 14.dp)) {
                    SelectorMarca(estado.marcas[materia.codigo]) { alMarcar(materia.codigo, it) }
                }
            }
        }
    }
}

private val TITULO_SECCION get() = TextStyle(fontWeight = FontWeight.Medium, fontSize = 9.5.sp, letterSpacing = 0.26.em, color = Paleta.tenue)

@Composable
private fun CabeceraFicha(materia: Asignatura, estado: EstadoCarrera, alCerrar: () -> Unit) {
    val situacion = estado.situaciones[materia.codigo] ?: Situacion.LEJANA
    val (palabra, color) = palabraDe(situacion)
    val j = jost()
    val p = plex()
    Box(Modifier.fillMaxWidth()) {
        // El filo de luz del borde de arriba, en el color de su estado
        Box(
            Modifier.padding(horizontal = Espacio.xl).fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.8f), Color.Transparent))),
        )
        Column {
            Box(Modifier.fillMaxWidth().padding(top = Espacio.xs), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 5.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.16f)))
            }
            Column(Modifier.padding(start = Espacio.l, end = Espacio.l, top = Espacio.xs, bottom = Espacio.m)) {
                Row(Modifier.padding(top = Espacio.xxs, end = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    BasicText(materia.semestre?.let { "SEMESTRE ${it.toString().padStart(2, '0')}" } ?: "ELECTIVA", style = TITULO_SECCION.copy(fontFamily = j))
                    Box(Modifier.size(16.dp, 1.dp).background(BORDE_PANEL))
                    BasicText(materia.codigo, style = TextStyle(fontFamily = p, fontWeight = FontWeight.Light, fontSize = Tipo.xxs, letterSpacing = 0.04.em, color = Paleta.tenue))
                }
                BasicText(
                    materia.nombre,
                    Modifier.padding(top = Espacio.xs, end = 48.dp),
                    style = TextStyle(fontFamily = j, fontWeight = FontWeight.Normal, fontSize = Tipo.titulo, lineHeight = 27.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                )
                Row(Modifier.padding(top = Espacio.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(estado.colores.de(materia)))
                    nombreArea(materia.area)?.let { BasicText(it, style = TextStyle(fontFamily = j, fontSize = Tipo.xs, color = Paleta.suave), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    BasicText("${materia.uc ?: 0} UC", style = TextStyle(fontFamily = p, fontWeight = FontWeight.Light, fontSize = Tipo.xxs, color = Paleta.tenue))
                    Spacer(Modifier.weight(1f))
                    BasicText(palabra, style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 9.5.sp, letterSpacing = 0.22.em, color = color))
                }
            }
        }
        // La X en su esquina, fuera del flujo: la fila de abajo llega hasta el borde
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = Espacio.s, end = Espacio.m).size(Alto.icono).clip(CircleShape)
                .background(Paleta.tinta.copy(alpha = 0.05f)).border(1.dp, Paleta.tinta.copy(alpha = 0.16f), CircleShape)
                .clickable(role = Role.Button, onClick = alCerrar),
            contentAlignment = Alignment.Center,
        ) {
            IconoTrazo(Lucide.cerrar, Paleta.suave, 18.dp, 1.75f)
        }
    }
}

/** Lo que responde "¿puedo verla?, ¿qué pide?, ¿qué abre?" */
@Composable
private fun Informacion(materia: Asignatura, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val relaciones = estado.layout.relaciones
    val porCodigo = estado.layout.porCodigo
    val requiere = relaciones.atras[materia.codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val desbloquea = relaciones.adelante[materia.codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    AvisoSituacion(materia, requiere, estado)
    ListaPrelaciones("REQUIERE", requiere, if (materia.requisitoEspecial != null) null else "Nada: puedes verla desde el inicio.", estado, alIrA)
    materia.requisitoEspecial?.let { especial ->
        BasicText(
            buildAnnotatedString {
                append("Además: ")
                withStyle(SpanStyle(color = Paleta.tinta)) { append(especial) }
                append(". Es una condición del pensum, no una materia.")
            },
            style = TextStyle(fontFamily = jost(), fontSize = TEXTO_AVISO, lineHeight = 17.sp, color = Paleta.suave),
        )
    }
    ListaPrelaciones("DESBLOQUEA", desbloquea, "Nada: es final de rama.", estado, alIrA)
}

/** Quién falta, dicho con nombre: "A y B", o las dos primeras y cuántas quedan */
private fun nombrar(materias: List<Asignatura>): String {
    val n = materias.map { it.nombre }
    return if (n.size <= 2) n.joinToString(" y ") else "${n.take(2).joinToString(", ")} y ${n.size - 2} más"
}

@Composable
private fun AvisoSituacion(materia: Asignatura, requiere: List<Asignatura>, estado: EstadoCarrera) {
    val e = estado.estados[materia.codigo]
    val situacion = estado.situaciones[materia.codigo] ?: Situacion.LEJANA
    val estilo = TextStyle(fontFamily = jost(), fontSize = TEXTO_AVISO, lineHeight = 17.sp, color = Paleta.suave)
    val (icono, texto) = when {
        e == EstadoMateria.DISPONIBLE -> Situacion.INSCRIBIBLE to buildAnnotatedString { append("Puedes inscribirla: tienes aprobadas todas sus prelaciones.") }
        e != EstadoMateria.BLOQUEADA -> return
        else -> {
            val pendientes = requiere.filter { estado.estados[it.codigo] != EstadoMateria.APROBADA }
            val sinEmpezar = pendientes.filter { estado.estados[it.codigo] != EstadoMateria.CURSANDO }
            if (situacion == Situacion.PROXIMA && pendientes.isNotEmpty()) {
                Situacion.PROXIMA to buildAnnotatedString {
                    append("Se abre el próximo semestre si apruebas ")
                    withStyle(SpanStyle(color = Paleta.tinta)) { append(nombrar(pendientes)) }
                    append(".")
                }
            } else {
                Situacion.LEJANA to buildAnnotatedString {
                    if (sinEmpezar.isNotEmpty()) {
                        append("Aún te falta${if (sinEmpezar.size > 1) "n" else ""} ")
                        withStyle(SpanStyle(color = Paleta.tinta)) { append(nombrar(sinEmpezar)) }
                        append(".")
                    } else append("Te falta aprobar sus prelaciones.")
                    append(" Puedes marcarla igual si ya la viste.")
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Canvas(Modifier.padding(top = Espacio.hilo).size(13.dp)) { iconoEstado(icono, 0f, 0f, size.minDimension, Paleta.panel) }
        BasicText(texto, style = estilo)
    }
}

/** Requiere o Desbloquea: el título con su raya hasta la cuenta y una fila por materia que lleva hasta ella */
@Composable
private fun ListaPrelaciones(titulo: String, materias: List<Asignatura>, vacio: String?, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val j = jost()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            BasicText(titulo, style = TITULO_SECCION.copy(fontFamily = j, letterSpacing = 0.24.em))
            Box(Modifier.weight(1f).height(1.dp).background(BORDE_PANEL))
            BasicText("${materias.size}", style = TextStyle(fontFamily = plex(), fontWeight = FontWeight.Light, fontSize = Tipo.xxs, color = Paleta.tenue))
        }
        if (materias.isEmpty()) {
            vacio?.let { BasicText(it, Modifier.padding(top = Espacio.xxs), style = TextStyle(fontFamily = j, fontSize = TEXTO_AVISO, color = Paleta.tenue)) }
        } else {
            Column(Modifier.padding(top = Espacio.xxs)) {
                materias.forEach { m ->
                    val s = estado.situaciones[m.codigo] ?: Situacion.LEJANA
                    val puedeIr = estado.nodoDe(m.codigo) != null
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (puedeIr) Modifier.clickable(role = Role.Button) { alIrA(m.codigo) } else Modifier)
                            .heightIn(min = Alto.toque)
                            .padding(vertical = Espacio.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    ) {
                        Canvas(Modifier.size(15.dp)) { iconoEstado(s, 0f, 0f, size.minDimension, Paleta.panel) }
                        BasicText(
                            m.nombre, Modifier.weight(1f),
                            style = TextStyle(fontFamily = j, fontWeight = PESO_NOMBRE, fontSize = Tipo.m, color = if (s == Situacion.HECHA) Paleta.suave else Paleta.tinta),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(m.codigo, style = TextStyle(fontFamily = plex(), fontWeight = FontWeight.Light, fontSize = Tipo.xxs, letterSpacing = 0.04.em, color = Paleta.tenue))
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonCambiarElectiva(alPulsar: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, BORDE_PANEL, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = alPulsar).heightIn(min = Alto.toque).padding(vertical = Espacio.xs),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("CAMBIAR ESTA ELECTIVA", style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = 9.5.sp, letterSpacing = 0.2.em, color = Paleta.suave))
    }
}

/** Las tres marcas, en el orden en que se leen */
private val MARCAS get() = listOf(
    Triple(Marca.APROBADA, "Aprobada", Paleta.verde),
    Triple(Marca.CURSANDO, "Cursando", Paleta.ambar),
    Triple(null, "Sin cursar", Paleta.suave),
)

/**
 * El selector de marcas del teléfono (SelectorMarca.jsx): una pista de 56 dp
 * con una lente de 48 que se desliza hasta la marca elegida y se tiñe con su
 * color; «Sin cursar» no tiene color propio y lleva la tinta.
 */
@Composable
private fun SelectorMarca(marca: Marca?, alMarcar: (Marca?) -> Unit) {
    val haptica = LocalHapticFeedback.current
    val indice = MARCAS.indexOfFirst { it.first == marca }.coerceAtLeast(0)
    val posicion by animateFloatAsState(indice.toFloat(), tween(380, easing = SUBIR), label = "lente")
    val color = MARCAS[indice].third
    val i = inter()
    val pista = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(Alto.grande).clip(pista)
            .background(Paleta.tinta.copy(alpha = porTema(0.035f, 0.03f)))
            .border(1.dp, Paleta.borde, pista)
            .padding(Espacio.xxs),
    ) {
        // Con poco ancho por marca, el icono va sobre la etiqueta (más corta) y nada se pisa
        val apretado = (maxWidth - Espacio.xxs * 2) / MARCAS.size < ANCHO_MARCA_HOLGADA || ventana().letraGrande
        Box(
            Modifier.fillMaxWidth(1f / 3f).height(48.dp)
                .graphicsLayer { translationX = size.width * posicion }
                .drawBehind {
                    val r = CornerRadius(12.dp.toPx())
                    val c = if (marca == null) Paleta.tinta else color
                    val relleno = if (marca == null) 0.06f else porTema(0.14f, 0.10f)
                    val borde = if (marca == null) 0.16f else porTema(0.40f, 0.45f)
                    drawRoundRect(c.copy(alpha = relleno), cornerRadius = r)
                    drawRoundRect(c.copy(alpha = borde), cornerRadius = r, style = Stroke(1.dp.toPx()))
                    // El brillo del canto de la lente: en claro no hay luz que sumar sobre blanco
                    if (!Tema.claro) drawRoundRect(Color.White.copy(alpha = 0.08f), Offset(12.dp.toPx(), 0f), Size(size.width - 24.dp.toPx(), 1.dp.toPx()))
                },
        )
        Row(Modifier.fillMaxSize()) {
            MARCAS.forEach { (m, texto, c) ->
                val activa = m == marca
                val alPulsar = {
                    haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    alMarcar(m)
                }
                val icono = @Composable { Canvas(Modifier.size(16.dp).graphicsLayer { alpha = if (activa) 1f else 0.85f }) { iconoMarca(m, c) } }
                val etiqueta = @Composable {
                    BasicText(
                        texto,
                        style = TextStyle(fontFamily = i, fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium, fontSize = if (apretado) Tipo.xs else Tipo.s, color = if (activa) c else Paleta.suave),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                val toque = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(Radio.Chip)).clickable(role = Role.Button, onClick = alPulsar)
                if (apretado) {
                    Column(toque, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        icono()
                        Spacer(Modifier.height(Espacio.hilo))
                        etiqueta()
                    }
                } else {
                    Row(toque, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        icono()
                        Spacer(Modifier.width(6.dp))
                        etiqueta()
                    }
                }
            }
        }
    }
}

/** El icono de cada marca: el de su situación, y un aro vacío para "sin cursar" */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.iconoMarca(m: Marca?, color: Color) {
    when (m) {
        Marca.APROBADA -> iconoEstado(Situacion.HECHA, 0f, 0f, size.minDimension, Paleta.panel)
        Marca.CURSANDO -> iconoEstado(Situacion.CURSANDO, 0f, 0f, size.minDimension, Paleta.panel)
        null -> drawCircle(color, size.minDimension * 5.6f / 14f, style = Stroke(size.minDimension * 1.5f / 14f))
    }
}
