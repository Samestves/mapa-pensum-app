package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.CarruselTemas
import mpensum.componentes.IconoTrazo
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.componentes.Lucide
import mpensum.componentes.tarjeta
import mpensum.datos.Pensum
import mpensum.logica.AvanceGrupo
import mpensum.logica.Periodo
import mpensum.logica.avanceDeGrupos
import mpensum.plataforma.ManejarAtras
import mpensum.plataforma.Preferencias
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.acentoDe
import mpensum.tema.colorDe
import mpensum.tema.inter
import kotlin.math.roundToInt

/*
 * La hoja del progreso en el teléfono, en su mismo orden:
 *
 *   1. La tarjeta héroe: cuánto llevas, cuándo te gradúas y la curva que lo
 *      cuenta. Tocarla (o la fila de debajo) abre el detalle.
 *   2. Tu próximo semestre, en una fila.
 *   3. Las cuotas de electivas, donde el título las exige.
 *   4. Lo que no es avance pero vive aquí, como en la web: la apariencia y
 *      reiniciar.
 *
 * El detalle ("¿Cuándo me gradúo?", DetalleGraduacion.kt) vive dentro de la
 * misma hoja: entra deslizándose desde la derecha y el atrás del sistema
 * vuelve a la hoja. Toda la hoja habla Inter, con las cifras en Light.
 *
 * En un teléfono acostado la hoja se ensancha a dos columnas y llena el alto
 * que hay: la hoja pone la tarjeta héroe a un lado y las filas al otro, y el
 * detalle, el anillo a un lado y lo demás con su propio scroll al otro.
 */

/** Lo más ancha que llega la hoja del progreso acostada, con sus dos columnas */
private val ANCHO_APAISADO = 840.dp

/** Los rótulos: mayúsculas semibold de 11 sp con 0,05 em de espaciado */
@Composable
internal fun versal(color: Color) = TextStyle(fontFamily = inter(), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.05.em, color = color)

/** Un texto de la hoja: Inter, con el tamaño, el peso y el color que pida */
@Composable
internal fun estilo(tam: Int, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam * 1.35f).roundToInt()) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = tam.sp, lineHeight = interlinea.sp, color = color, fontFeatureSettings = "tnum")

private const val ENTRADA_CIFRA = 700

/** Un número que cuenta hasta su valor en 600 ms, saliendo rápido y frenando (useNumeroAnimado) */
@Composable
fun numeroAnimado(destino: Float): Float {
    val valor = remember { Animatable(destino) }
    LaunchedEffect(destino) { valor.animateTo(destino, tween(600, easing = ENFATICA)) }
    return valor.value
}

/** La pastilla de icono: 32 dp, radio 9, la tinta al 7 % y el icono de 18 dp */
@Composable
internal fun Pastilla(trazos: List<Path>, modifier: Modifier = Modifier, color: Color = Paleta.suave) {
    Box(modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Paleta.tinta.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
        IconoTrazo(trazos, color, 18.dp, 1.9f)
    }
}

@Composable
fun BoxScope.HojaAvance(abierta: Boolean, estado: EstadoCarrera, alCerrar: () -> Unit, alVerEnMapa: (List<String>) -> Unit) {
    // Cada vez que se abre, se abre en la hoja y no en el detalle donde se dejó
    var apertura by remember { mutableIntStateOf(0) }
    var estabaAbierta by remember { mutableStateOf(abierta) }
    if (abierta != estabaAbierta) {
        estabaAbierta = abierta
        if (abierta) apertura++
    }
    var detalle by remember(apertura) { mutableStateOf(false) }
    val apaisado = esApaisado()
    val hoy = remember(apertura) { Periodo.actual() }

    HojaInferior(
        abierta = abierta,
        alCerrar = alCerrar,
        ancho = if (apaisado) ANCHO_APAISADO else 560.dp,
        desplazable = !apaisado,
        cabecera = {
            val abajo = if (apaisado) 8.dp else 16.dp
            AnimatedContent(detalle, transitionSpec = { fadeIn(tween(220, 60)) togetherWith fadeOut(tween(120)) }, label = "cabecera") { d ->
                if (d) CabeceraDetalle(abajo) { detalle = false }
                else Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = abajo), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        BasicText("TU AVANCE", style = versal(Paleta.tenue))
                        BasicText(estado.pensum.nombre, Modifier.padding(top = 4.dp), style = estilo(24, Paleta.tinta, FontWeight.Light, 30))
                    }
                    BotonCerrar(alCerrar)
                }
            }
        },
    ) {
        // Atrás vuelve del detalle a la hoja; ya en la hoja, la cierra (HojaInferior)
        ManejarAtras(detalle) { detalle = false }
        AnimatedContent(
            targetState = detalle,
            transitionSpec = {
                val entra = if (targetState) 3 else -6
                val sale = if (targetState) -6 else 3
                ((slideInHorizontally(tween(340, easing = ENFATICA)) { it / entra } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(340, easing = ENFATICA)) { it / sale } + fadeOut(tween(160)))).using(SizeTransform(clip = true) { _, _ -> snap() })
            },
            // Acostada la hoja no desplaza: el contenido toma el alto que queda
            modifier = if (apaisado) Modifier.weight(1f) else Modifier,
            contentAlignment = Alignment.TopStart,
            label = "detalle",
        ) { d ->
            if (d) DetalleGraduacion(estado, hoy, alVerEnMapa)
            else ContenidoHoja(estado, hoy) { detalle = true }
        }
    }
}

@Composable
private fun CabeceraDetalle(abajo: Dp, alVolver: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = abajo), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(Alignment.CenterStart).size(40.dp).clip(RoundedCornerShape(20.dp)).background(Paleta.tinta.copy(alpha = 0.08f)).clickable(role = Role.Button, onClick = alVolver)
                .semantics { contentDescription = "Volver" },
            contentAlignment = Alignment.Center,
        ) { IconoTrazo(Lucide.atras, Paleta.suave, 18.dp, 2.2f) }
        BasicText("¿Cuándo me gradúo?", style = estilo(17, Paleta.tinta, FontWeight.Medium, 22))
    }
}

// ─────────────────────────────────── La hoja ──────────────────────────────────

@Composable
private fun ContenidoHoja(estado: EstadoCarrera, hoy: Periodo, abrirDetalle: () -> Unit) {
    val g = estado.graduacion
    val cuotas = remember(estado.marcas) { avanceDeGrupos(estado.pensum.grupos, estado.marcas).values.filter { it.meta != null } }
    // Las filas de la hoja, iguales de pie o acostada; solo cambian los huecos que las separan
    val filas: @Composable (Dp, Dp, Dp) -> Unit = { huecoProximo, huecoElectivas, huecoTema ->
        if (g.plan.isNotEmpty()) {
            Spacer(Modifier.height(huecoProximo))
            FilaProximo(g.plan[0].size, hoy.mas(1), abrirDetalle)
        }
        if (cuotas.isNotEmpty()) {
            Spacer(Modifier.height(huecoElectivas))
            Electivas(cuotas)
        }
        Spacer(Modifier.height(huecoTema))
        FilaTema()
        Spacer(Modifier.height(12.dp))
        BotonReinicio(estado)
    }
    if (esApaisado()) {
        // Tarjeta héroe a un lado, filas al otro, con un solo scroll para las dos
        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) { TarjetaHeroe(estado, hoy, abrirDetalle) }
            Column(Modifier.weight(1f)) { filas(0.dp, 20.dp, 20.dp) }
        }
    } else {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
            TarjetaHeroe(estado, hoy, abrirDetalle)
            filas(12.dp, 32.dp, 24.dp)
        }
    }
}

/**
 * La tarjeta héroe: el porcentaje y las UC a la izquierda, la fecha de
 * graduación con su birrete a la derecha y debajo la curva. Detrás de la
 * curva, un brillo del color de la carrera.
 */
@Composable
private fun TarjetaHeroe(estado: EstadoCarrera, hoy: Periodo, alTocar: () -> Unit) {
    val g = estado.graduacion
    val progreso = estado.progreso
    val conUc = progreso.porcentaje != null
    val llevas = if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas
    val de = if (conUc) progreso.ucTitulo!! else progreso.total
    val porcentaje = progreso.avance.roundToInt()
    val color = acentoDe(colorDe(estado.pensum.color))
    val fecha = g.periodoFinal?.mesYAnio() ?: "Completa"
    val escena = remember(g, estado.marcas, estado.casillas) { escenaDe(estado, g) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .drawBehind {
                drawRect(Paleta.tinta.copy(alpha = 0.06f))
                drawRect(Brush.radialGradient(0f to color.copy(alpha = 0.22f), 1f to Color.Transparent, center = Offset(size.width / 2f, size.height), radius = size.width * 0.7f))
            }
            .border(1.dp, Paleta.tinta.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                CifraDibujada(porcentaje)
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Paleta.tinta)) { append("$llevas") }
                        append(" / $de ${if (conUc) "UC" else "materias"}")
                    },
                    Modifier.padding(top = 2.dp),
                    style = estilo(15, Paleta.tenue),
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 2.dp)) {
                BasicText(if (g.terminado) "TU CARRERA" else "TE GRADÚAS", style = versal(Paleta.tenue))
                BasicText(fecha, Modifier.padding(top = 2.dp), style = estilo(22, Paleta.tinta, FontWeight.Normal, 26))
            }
            Pastilla(Lucide.birrete, Modifier.padding(start = 12.dp))
        }
        GraficaAvance(escena, fecha, porcentaje, hoy, color, alto = 150.dp, detalle = false, modifier = Modifier.padding(top = 4.dp))
    }
}

/** El porcentaje grande y fino (Inter Light) con el signo pequeño arriba. Cuenta al dibujar, sin recomponer */
@Composable
private fun CifraDibujada(porcentaje: Int) {
    val cuenta = remember { Animatable(0f) }
    LaunchedEffect(porcentaje) { cuenta.animateTo(1f, tween(ENTRADA_CIFRA, easing = ENFATICA)) }
    val medidor = rememberTextMeasurer()
    val numero = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-0.04).em, color = Paleta.tinta, fontFeatureSettings = "tnum")
    val signo = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 24.sp, color = Paleta.tenue)
    Canvas(Modifier.fillMaxWidth().height(60.dp).semantics { contentDescription = "$porcentaje por ciento aprobado" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta.value).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        val base = size.height - 6.dp.toPx()
        // Por línea base: la cifra apoyada abajo y el signo con su cabeza a la altura de la de los números
        val cabeza = base - 64.sp.toPx() * 0.73f
        drawText(cifra, topLeft = Offset(-3.dp.toPx(), base - cifra.firstBaseline))
        drawText(por, topLeft = Offset(cifra.size.width + 4.dp.toPx(), cabeza - 2.dp.toPx()))
    }
}

/** "Ene – Jun 2027 · 6 materias / Tu próximo semestre ›" */
@Composable
private fun FilaProximo(materias: Int, periodo: Periodo, alTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tarjeta(22.dp).clickable(role = Role.Button, onClick = alTocar).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pastilla(Lucide.calendario)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            BasicText("${periodo.rango()} · $materias ${if (materias == 1) "materia" else "materias"}", style = estilo(17, Paleta.tinta, FontWeight.Medium, 22), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText("Tu próximo semestre", style = estilo(15, Paleta.tenue, interlinea = 20))
        }
        IconoTrazo(Lucide.siguiente, Paleta.tenue, 16.dp, 2.2f)
    }
}

/** Las cuotas de electivas que exige el título: un grupo por fila, con su icono y lo que llevas */
@Composable
private fun Electivas(cuotas: List<AvanceGrupo>) {
    BasicText("ELECTIVAS", style = versal(Paleta.tenue))
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().tarjeta(22.dp)) {
        cuotas.forEachIndexed { k, g ->
            val meta = g.meta ?: return@forEachIndexed
            if (k > 0) Box(Modifier.padding(start = 58.dp).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = 0.08f)))
            val titulo = g.titulo.removePrefix("Electivas ").replaceFirstChar { it.uppercase() }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Pastilla(if (titulo.startsWith("Socio", ignoreCase = true)) IconosHoja.usuarios else IconosHoja.libroAbierto)
                BasicText(titulo, Modifier.weight(1f).padding(start = 14.dp, end = 8.dp), style = estilo(17, Paleta.tinta, FontWeight.Medium, 22), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Paleta.tinta)) { append("${g.uc}") }
                        append(" / $meta UC")
                    },
                    style = estilo(16, Paleta.tenue),
                )
                IconoTrazo(Lucide.siguiente, Paleta.tenue, 16.dp, 2.2f, Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * El tema: el icono del que tienes, «Tema» y su nombre con la flecha. Tocarla
 * abre debajo el mismo carrusel de mini portadas de Personalizar, y elegir
 * uno cambia la app en el acto (Preferencias.tema, la misma de Ajustes).
 */
@Composable
private fun FilaTema() {
    val actual by Preferencias.tema.collectAsState()
    var abierto by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierto) 90f else 0f, tween(if (Tema.movimiento) 0 else 240, easing = ENFATICA), label = "giro")
    Column(Modifier.fillMaxWidth().tarjeta(22.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .pulsable({ abierto = !abierto }, escala = 0.99f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pastilla(if (Tema.claro) IconosHoja.sol else IconosHoja.luna)
            BasicText("Tema", Modifier.weight(1f).padding(start = 14.dp), style = estilo(17, Paleta.tinta, FontWeight.Medium, 22))
            BasicText(actual.nombre, style = estilo(15, Paleta.tenue))
            IconoTrazo(Lucide.siguiente, Paleta.tenue, 16.dp, 2.2f, Modifier.padding(start = 8.dp).graphicsLayer { rotationZ = giro })
        }
        AnimatedVisibility(abierto) {
            CarruselTemas(Modifier.padding(bottom = 12.dp), PaddingValues(horizontal = 12.dp))
        }
    }
}

/** Cómo se llama cada código del plan: la materia, la casilla, o "Electiva técnica" para lo que pide la cuota de un grupo */
internal fun nombresDelPlan(pensum: Pensum): (String) -> String {
    val porCodigo = (pensum.asignaturas + pensum.grupos.flatMap { it.asignaturas }).associateBy { it.codigo }
    val grupos = pensum.grupos.associateBy { it.clave }
    return { codigo ->
        porCodigo[codigo]?.nombre
            ?: grupos[codigo.substringBefore('#')]?.titulo?.let { t -> t.split(' ').joinToString(" ") { if (it.endsWith("as")) it.dropLast(1) else it } }
            ?: codigo
    }
}


/**
 * Los trazos Lucide que solo usan esta hoja y Ajustes (sun, moon, smartphone,
 * rotate-ccw, arrow-up-right), en la rejilla de 24 de IconoTrazo.
 */
internal object IconosHoja {
    private fun trazo(d: String): Path = PathParser().parsePathString(d).toPath()

    val sol = listOf(
        trazo("M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z"),
        trazo("M12 2v2"), trazo("M12 20v2"), trazo("m4.93 4.93 1.41 1.41"), trazo("m17.66 17.66 1.41 1.41"),
        trazo("M2 12h2"), trazo("M20 12h2"), trazo("m6.34 17.66-1.41 1.41"), trazo("m19.07 4.93-1.41 1.41"),
    )
    val luna = listOf(trazo("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"))
    val telefono = listOf(trazo("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"), trazo("M12 18h.01"))
    val reiniciar = listOf(trazo("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"), trazo("M3 3v5h5"))
    val mitad = listOf(trazo("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z"), trazo("M12 2a10 10 0 0 1 0 20z"))
    val libroAbierto = listOf(trazo("M12 7v14"), trazo("M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"))
    val usuarios = listOf(trazo("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"), trazo("M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0z"), trazo("M22 21v-2a4 4 0 0 0-3-3.87"), trazo("M16 3.13a4 4 0 0 1 0 7.75"))
    val eslabon = listOf(trazo("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"), trazo("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"))
    val candado = listOf(trazo("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"), trazo("M7 11V7a5 5 0 0 1 10 0v4"))
    val candadoAbierto = listOf(trazo("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"), trazo("M7 11V7a5 5 0 0 1 9.9-1"))
    val flechaAbajo = listOf(trazo("M12 5v14"), trazo("m19 12-7 7-7-7"))
    val fuera = listOf(trazo("M7 7h10v10"), trazo("M7 17 17 7"))
}

// ──────────────────────────────── 6. Reiniciar ───────────────────────────────

/**
 * Reiniciar el avance de la carrera: lo único de la hoja que no se deshace, y
 * por eso lo más callado hasta que se pide. Te deja en el 0 %: sin marcas,
 * sin las electivas que pusiste y con la carga habitual. Pulsarlo no borra
 * nada: dice qué se va a perder y deja Cancelar del lado del pulgar. Sin caja
 * roja: el rojo solo va en el botón que borra.
 */
@Composable
private fun BotonReinicio(estado: EstadoCarrera) {
    val i = inter()
    val hay = estado.hayAvance
    var confirmando by remember { mutableStateOf(false) }
    if (!confirmando || !hay) {
        val color = Paleta.tenue.copy(alpha = if (hay) 1f else 0.5f)
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Paleta.tinta.copy(alpha = 0.04f))
                .clickable(enabled = hay, role = Role.Button) { confirmando = true },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hay) IconoTrazo(IconosHoja.reiniciar, color, 15.dp, 2f, Modifier.padding(end = 8.dp))
            BasicText(
                if (hay) "Reiniciar mi avance" else "Nada que reiniciar todavía",
                style = TextStyle(fontFamily = i, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = color),
            )
        }
        return
    }
    Column {
        BasicText("¿Empezar de cero?", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Paleta.tinta))
        BasicText(
            "Se borra${loQueSeBorra(estado)} en esta carrera. Vuelves al 0 % y no se puede deshacer.",
            Modifier.padding(top = 4.dp),
            style = TextStyle(fontFamily = i, fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.suave),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Paleta.tinta.copy(alpha = 0.08f)).clickable(role = Role.Button) { confirmando = false },
                contentAlignment = Alignment.Center,
            ) { BasicText("Cancelar", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Paleta.tinta)) }
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Paleta.rojo.copy(alpha = 0.16f)).clickable(role = Role.Button) {
                    estado.reiniciar()
                    confirmando = false
                },
                contentAlignment = Alignment.Center,
            ) { BasicText("Borrar todo", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ROJO_LETRA)) }
        }
    }
}

/** " tu materia marcada, las electivas que pusiste y tu carga por semestre": solo lo que de verdad hay */
private fun loQueSeBorra(estado: EstadoCarrera): String {
    val cuantas = estado.marcas.size
    val partes = listOfNotNull(
        when (cuantas) {
            0 -> null
            1 -> "tu materia marcada"
            else -> "tus $cuantas materias marcadas"
        },
        if (estado.casillas.isNotEmpty()) "las electivas que pusiste" else null,
        if (estado.carga != null) "tu carga por semestre" else null,
    )
    // "Se borran" con más de una cosa o con varias materias; "se borra" con una sola
    val plural = partes.size > 1 || cuantas > 1
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
    return "${if (plural) "n" else ""} $lista"
}

/** El rojo mezclado un cuarto con la tinta: se lee sobre su propio velo (AA) */
private val ROJO_LETRA get() = lerp(Paleta.rojo, Paleta.tinta, 0.3f)
