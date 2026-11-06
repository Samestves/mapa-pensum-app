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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.unit.TextUnit
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
import mpensum.tema.Espacio
import mpensum.tema.Radio
import mpensum.tema.Tipo
import mpensum.tema.Alto
import mpensum.componentes.margenPantalla
import mpensum.componentes.ventana
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.acentoDe
import mpensum.tema.colorDe
import mpensum.tema.inter
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import mpensum.logica.CuentaSemestre
import mpensum.logica.EstimacionGraduacion
import mpensum.mapa.tituloGrupo
import mpensum.tema.jost
import kotlin.math.roundToInt

/*
 * La hoja del progreso en el teléfono, en bento (maqueta K1):
 *
 *   1. Una pieza ancha «LLEVAS»: el porcentaje, las UC y una barra partida con
 *      un segmento por semestre. Abre el detalle por semestres.
 *   2. «TE GRADÚAS» (abre ¿Cuándo me gradúo?), «PUEDES INSCRIBIR» (en azul) y
 *      una pieza por cada grupo de electivas con meta. Cada una abre su detalle.
 *   3. Dos renglones callados, sin tarjeta: el tema y reiniciar.
 *
 * Los detalles (DetalleGraduacion, DetalleSemestres, DetalleInscribir y
 * DetalleElectivas) viven dentro de la misma hoja: entran deslizándose desde
 * la derecha y el atrás del sistema vuelve a la hoja. Toda la hoja habla
 * Inter, con las cifras en Light.
 *
 * En un teléfono acostado la hoja se ensancha y la rejilla pasa a tres
 * columnas: «LLEVAS» ocupa la primera a dos filas de alto y las demás piezas
 * van en rejilla de dos a su derecha, con un solo scroll por si no cabe.
 */

/** Lo más ancha que llega la hoja del progreso acostada, con sus tres columnas */
private val ANCHO_APAISADO = 840.dp

/** El fondo de las piezas del bento: la tinta al 4,5 % */
private const val VELO_PIEZA = 0.045f

/** El círculo del «›» de cada pieza */
private val LADO_MAS = 22.dp

/** Los rótulos: mayúsculas semibold de 11 sp con 0,05 em de espaciado */
@Composable
internal fun versal(color: Color) = TextStyle(fontFamily = inter(), fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.05.em, color = color)

/** Un texto de la hoja: Inter, con el tamaño, el peso y el color que pida */
@Composable
internal fun estilo(tam: Int, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam * 1.35f).roundToInt()) =
    estilo(tam.sp, color, peso, interlinea)

/** Lo mismo con el tamaño de un token de [Tipo]: la interlínea por defecto es 1,35 veces el tamaño */
@Composable
internal fun estilo(tam: TextUnit, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam.value * 1.35f).roundToInt()) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = tam, lineHeight = interlinea.sp, color = color, fontFeatureSettings = "tnum")

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

/** Los detalles a los que lleva cada pieza de la hoja */
internal sealed interface DetalleAvance {
    data object Graduacion : DetalleAvance
    data object Semestres : DetalleAvance
    data object Inscribir : DetalleAvance
    data class Electivas(val clave: String) : DetalleAvance
}

private fun tituloDe(detalle: DetalleAvance, estado: EstadoCarrera): String = when (detalle) {
    DetalleAvance.Graduacion -> "¿Cuándo me gradúo?"
    DetalleAvance.Semestres -> "Por semestres"
    DetalleAvance.Inscribir -> "Puedes inscribir"
    is DetalleAvance.Electivas -> estado.pensum.grupos.firstOrNull { it.clave == detalle.clave }?.let(::tituloGrupo) ?: "Electivas"
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
    var detalle by remember(apertura) { mutableStateOf<DetalleAvance?>(null) }
    // La casilla de electiva que se está eligiendo: su selector sube por encima de esta hoja
    var casilla by remember(apertura) { mutableStateOf<String?>(null) }
    val apaisado = esApaisado()
    val hoy = remember(apertura) { Periodo.actual() }

    HojaInferior(
        abierta = abierta,
        alCerrar = alCerrar,
        ancho = if (apaisado) ANCHO_APAISADO else 560.dp,
        desplazable = !apaisado,
        cabecera = {
            val abajo = if (apaisado) Espacio.xs else Espacio.m
            AnimatedContent(detalle, transitionSpec = { fadeIn(tween(220, 60)) togetherWith fadeOut(tween(120)) }, label = "cabecera") { d ->
                if (d != null) CabeceraDetalle(tituloDe(d, estado), abajo) { detalle = null }
                else Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = abajo), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        BasicText("TU AVANCE", style = versal(Paleta.tenue))
                        // En ventana compacta el título se encoge antes que saltar de línea
                        if (ventana().compacta) BasicText(estado.pensum.nombre, Modifier.padding(top = Espacio.xxs), style = estilo(Tipo.titulo, Paleta.tinta, FontWeight.Light, 30), maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = Tipo.l, maxFontSize = Tipo.titulo))
                        else BasicText(estado.pensum.nombre, Modifier.padding(top = Espacio.xxs), style = estilo(Tipo.titulo, Paleta.tinta, FontWeight.Light, 30))
                    }
                    BotonCerrar(alCerrar)
                }
            }
        },
    ) {
        // Atrás vuelve del detalle a la hoja; ya en la hoja, la cierra (HojaInferior)
        ManejarAtras(detalle != null) { detalle = null }
        AnimatedContent(
            targetState = detalle,
            transitionSpec = {
                val entra = if (targetState != null) 3 else -6
                val sale = if (targetState != null) -6 else 3
                ((slideInHorizontally(tween(340, easing = ENFATICA)) { it / entra } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(340, easing = ENFATICA)) { it / sale } + fadeOut(tween(160)))).using(SizeTransform(clip = true) { _, _ -> snap() })
            },
            // Acostada la hoja no desplaza: el contenido toma el alto que queda
            modifier = if (apaisado) Modifier.weight(1f) else Modifier,
            contentAlignment = Alignment.TopStart,
            label = "detalle",
        ) { d ->
            when (d) {
                null -> ContenidoHoja(estado, hoy) { detalle = it }
                DetalleAvance.Graduacion -> DetalleGraduacion(estado, hoy, alVerEnMapa)
                DetalleAvance.Semestres -> DetalleSemestres(estado, alVerEnMapa)
                DetalleAvance.Inscribir -> DetalleInscribir(estado, alVerEnMapa)
                is DetalleAvance.Electivas -> DetalleElectivas(estado, d.clave) { casilla = it }
            }
        }
    }
    // Montado después de la hoja, el selector queda encima y su atrás se atiende primero
    SelectorElectiva(
        casilla = casilla,
        estado = estado,
        aprobarAlElegir = false,
        alColocar = { c, electiva ->
            estado.colocar(c, electiva)
            casilla = null
        },
        alCerrar = { casilla = null },
    )
}

@Composable
private fun CabeceraDetalle(titulo: String, abajo: Dp, alVolver: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = abajo), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(Alignment.CenterStart).size(Alto.icono).clip(RoundedCornerShape(Radio.Opcion)).background(Paleta.tinta.copy(alpha = 0.08f)).clickable(role = Role.Button, onClick = alVolver)
                .semantics { contentDescription = "Volver" },
            contentAlignment = Alignment.Center,
        ) { IconoTrazo(Lucide.atras, Paleta.suave, 18.dp, 2.2f) }
        // Un título largo («Electivas sociohumanísticas») se encoge antes que chocar con el botón
        BasicText(
            titulo,
            Modifier.padding(horizontal = Alto.icono + Espacio.xs),
            style = estilo(Tipo.xl, Paleta.tinta, FontWeight.Medium, 22).copy(textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = Tipo.m, maxFontSize = Tipo.xl),
        )
    }
}

/** El contenido de un detalle: de pie, una columna (la hoja desplaza); acostado, dos con su propio scroll cada una */
@Composable
internal fun CuerpoDetalle(izquierda: @Composable ColumnScope.() -> Unit, derecha: @Composable ColumnScope.() -> Unit) {
    if (esApaisado()) {
        Row(Modifier.fillMaxSize().padding(start = margenPantalla(), end = margenPantalla(), bottom = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.xl)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), content = izquierda)
            Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()), content = derecha)
        }
    } else {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xs, bottom = Espacio.xxl)) {
            izquierda()
            derecha()
        }
    }
}

/** El hilo de 1 dp que separa los renglones callados */
@Composable
internal fun Hilo(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = 0.08f)))
}

// ─────────────────────────────────── La hoja ──────────────────────────────────

@Composable
private fun ContenidoHoja(estado: EstadoCarrera, hoy: Periodo, abrir: (DetalleAvance) -> Unit) {
    val g = estado.graduacion
    val cuotas = remember(estado.marcas) { avanceDeGrupos(estado.pensum.grupos, estado.marcas).values.filter { it.meta != null } }
    val cuentas = remember(estado.marcas, estado.casillas) { cuentasPorSemestre(estado) }
    val progreso = estado.progreso

    val llevas: @Composable (Modifier) -> Unit = { m -> PiezaLlevas(estado, cuentas, m) { abrir(DetalleAvance.Semestres) } }
    // Las demás piezas, de dos en dos en la rejilla
    val demas: List<@Composable (Modifier) -> Unit> = buildList {
        add { m -> PiezaGraduas(g, m) { abrir(DetalleAvance.Graduacion) } }
        add { m -> PiezaInscribir(progreso.disponibles, hoy.mas(1), m) { abrir(DetalleAvance.Inscribir) } }
        cuotas.forEach { c -> add { m -> PiezaCuota(c, m) { abrir(DetalleAvance.Electivas(c.clave)) } } }
    }
    val callados: @Composable () -> Unit = {
        Hilo()
        FilaTema()
        Hilo()
        BotonReinicio(estado)
    }

    if (esApaisado()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.l)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                llevas(Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    demas.chunked(2).forEach { FilaDePiezas(it) }
                }
            }
            Spacer(Modifier.height(Espacio.m))
            callados()
        }
    } else {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.xl), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            llevas(Modifier.fillMaxWidth())
            demas.chunked(2).forEach { FilaDePiezas(it) }
            Spacer(Modifier.height(Espacio.m))
            callados()
        }
    }
}

/** Una fila de la rejilla: una o dos piezas del mismo alto; la que va sola ocupa el ancho */
@Composable
private fun FilaDePiezas(piezas: List<@Composable (Modifier) -> Unit>) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        piezas.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
    }
}

/**
 * La pieza del bento: fondo de tinta al 4,5 %, radio de bloque, se hunde al
 * tocarla y lleva el circulito «›» arriba a la derecha. El contenido va en una
 * columna con el relleno de la tarjeta.
 */
@Composable
private fun Pieza(alTocar: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .pulsable(alTocar, escala = 0.985f)
            .clip(RoundedCornerShape(Radio.Bloque))
            .background(Paleta.tinta.copy(alpha = VELO_PIEZA))
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(Espacio.m), content = contenido)
        Box(
            Modifier.align(Alignment.TopEnd).padding(Espacio.s).size(LADO_MAS).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) { IconoTrazo(Lucide.siguiente, Paleta.suave, 12.dp, 2.2f) }
    }
}

/** El rótulo de una pieza: deja sitio al «›» y, si es largo, baja a otra línea */
@Composable
private fun Rotulo(texto: String, color: Color = Paleta.tenue) {
    BasicText(texto, Modifier.padding(end = LADO_MAS + Espacio.xxs), style = versal(color), maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/** Una cifra con su unidad pequeña detrás: «11 materias», «0 / 15 UC» */
@Composable
private fun CifraConUnidad(cifra: String, unidad: String, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        buildAnnotatedString {
            append(cifra)
            withStyle(SpanStyle(fontSize = Tipo.s, fontWeight = FontWeight.Normal, color = Paleta.tenue, letterSpacing = 0.em)) { append(" $unidad") }
        },
        modifier,
        style = estilo(Tipo.cifra, color, FontWeight.Light, 36).copy(letterSpacing = (-0.02).em),
    )
}

/** «LLEVAS»: el porcentaje, lo aprobado y la barra partida en un segmento por semestre */
@Composable
private fun PiezaLlevas(estado: EstadoCarrera, cuentas: List<Pair<Int, CuentaSemestre>>, modifier: Modifier, alTocar: () -> Unit) {
    val progreso = estado.progreso
    val conUc = progreso.porcentaje != null
    val llevas = if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas
    val de = if (conUc) progreso.ucTitulo!! else progreso.total
    val porcentaje = progreso.avance.roundToInt()
    // Acostada la pieza es una columna angosta: los textos van bajo la cifra, como en compactas
    val apilada = ventana().compacta || ventana().letraGrande || esApaisado()
    Pieza(alTocar, modifier) {
        Rotulo("LLEVAS")
        val textos: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Paleta.tinta)) { append("$llevas") }
                        append(" de $de ${if (conUc) "UC" else "materias"}")
                    },
                    style = estilo(Tipo.xs, Paleta.tenue, interlinea = 17),
                )
                BasicText(
                    "${progreso.aprobadas} ${if (progreso.aprobadas == 1) "aprobada" else "aprobadas"} · ${progreso.cursando} cursando",
                    style = estilo(Tipo.xs, Paleta.tenue, interlinea = 17),
                )
            }
        }
        Spacer(Modifier.height(Espacio.xs))
        if (apilada) {
            CifraDibujada(porcentaje)
            textos(Modifier.padding(top = Espacio.xxs))
        } else {
            val digitos = porcentaje.toString().length
            Row(verticalAlignment = Alignment.Bottom) {
                CifraDibujada(porcentaje, Modifier.width((digitos * 40 + 26).dp))
                textos(Modifier.padding(start = Espacio.xs, bottom = Espacio.xxs))
            }
        }
        if (cuentas.isNotEmpty()) {
            Spacer(Modifier.height(Espacio.m))
            Spacer(Modifier.weight(1f))
            BarraPartida(cuentas.map { it.second })
            Row(Modifier.fillMaxWidth().padding(top = Espacio.xs)) {
                BasicText("Semestre ${cuentas.first().first}", Modifier.weight(1f), style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16))
                BasicText("${cuentas.last().first}", style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16))
            }
        }
    }
}

/**
 * Un segmento por semestre, de 6 dp: lo aprobado en tinta y, a continuación,
 * lo que cursas en tinta al 45 %. Se dibuja de una sola vez, sin un nodo por segmento.
 */
@Composable
private fun BarraPartida(cuentas: List<CuentaSemestre>) {
    val tinta = Paleta.tinta
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val hueco = 3.dp.toPx()
        val n = cuentas.size
        val ancho = (size.width - hueco * (n - 1)) / n
        val radio = CornerRadius(size.height / 2f)
        cuentas.forEachIndexed { k, c ->
            val x = k * (ancho + hueco)
            drawRoundRect(tinta.copy(alpha = 0.10f), Offset(x, 0f), Size(ancho, size.height), radio)
            if (c.total > 0) {
                val cursando = (c.hechas + c.cursando).toFloat() / c.total
                val hechas = c.hechas.toFloat() / c.total
                if (cursando > 0f) drawRoundRect(tinta.copy(alpha = 0.45f), Offset(x, 0f), Size(ancho * cursando, size.height), radio)
                if (hechas > 0f) drawRoundRect(tinta, Offset(x, 0f), Size(ancho * hechas, size.height), radio)
            }
        }
    }
}

/** «TE GRADÚAS»: la fecha y el ritmo con que se calculó */
@Composable
private fun PiezaGraduas(g: EstimacionGraduacion, modifier: Modifier, alTocar: () -> Unit) {
    Pieza(alTocar, modifier) {
        Rotulo(if (g.terminado) "TU CARRERA" else "TE GRADÚAS")
        BasicText(
            g.periodoFinal?.mesYAnio() ?: "Completa",
            Modifier.padding(top = Espacio.xs),
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.titulo, lineHeight = 28.sp, color = Paleta.tinta),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = Tipo.l, maxFontSize = Tipo.titulo),
        )
        if (!g.terminado) BasicText("a ${g.carga} ${if (g.carga == 1) "materia" else "materias"} por semestre", Modifier.padding(top = Espacio.xxs), style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16))
    }
}

/** «PUEDES INSCRIBIR»: en azul, cuántas materias están abiertas y para qué periodo */
@Composable
private fun PiezaInscribir(cuantas: Int, periodo: Periodo, modifier: Modifier, alTocar: () -> Unit) {
    Pieza(alTocar, modifier) {
        Rotulo("PUEDES INSCRIBIR", Paleta.azul)
        if (cuantas == 0) {
            BasicText("Nada por ahora", Modifier.padding(top = Espacio.xs), style = estilo(Tipo.xl, Paleta.tenue, FontWeight.Light, 24))
        } else {
            CifraConUnidad("$cuantas", if (cuantas == 1) "materia" else "materias", Paleta.azul, Modifier.padding(top = Espacio.xxs))
            BasicText(periodo.rango(), Modifier.padding(top = Espacio.xxs), style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16))
        }
    }
}

/** Una cuota de electivas: su rótulo corto, lo que llevas de lo que pide el título y una barrita */
@Composable
private fun PiezaCuota(g: AvanceGrupo, modifier: Modifier, alTocar: () -> Unit) {
    val meta = g.meta ?: return
    Pieza(alTocar, modifier) {
        Rotulo(rotuloCorto(g.titulo))
        CifraConUnidad("${g.uc}", "/ $meta UC", Paleta.tinta, Modifier.padding(top = Espacio.xxs))
        Spacer(Modifier.height(Espacio.s))
        BarraDeUc(g.uc, meta, 4.dp)
    }
}

/** «Electivas técnicas» → «TÉCNICAS»; «Electivas sociohumanísticas» → «SOCIOHUM.» */
private fun rotuloCorto(titulo: String): String {
    val resto = titulo.removePrefix("Electivas ").uppercase()
    return if (resto.startsWith("SOCIO")) "SOCIOHUM." else resto
}

/** La barrita de lo que llevas de una cuota: tinta sobre tinta al 8 % */
@Composable
internal fun BarraDeUc(uc: Int, meta: Int, alto: Dp) {
    val fraccion = if (meta > 0) (uc.toFloat() / meta).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(alto).clip(RoundedCornerShape(alto / 2)).background(Paleta.tinta.copy(alpha = 0.08f))) {
        if (fraccion > 0f) Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().clip(RoundedCornerShape(alto / 2)).background(Paleta.tinta))
    }
}

/** El porcentaje grande y fino (Inter Light) con el signo pequeño arriba. Cuenta al dibujar, sin recomponer */
@Composable
private fun CifraDibujada(porcentaje: Int, modifier: Modifier = Modifier.fillMaxWidth()) {
    val cuenta = remember { Animatable(0f) }
    LaunchedEffect(porcentaje) { cuenta.animateTo(1f, tween(ENTRADA_CIFRA, easing = ENFATICA)) }
    val medidor = rememberTextMeasurer()
    val numero = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-0.04).em, color = Paleta.tinta, fontFeatureSettings = "tnum")
    val signo = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 24.sp, color = Paleta.tenue)
    Canvas(modifier.height(60.dp).semantics { contentDescription = "$porcentaje por ciento aprobado" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta.value).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        val base = size.height - 6.dp.toPx()
        // Por línea base: la cifra apoyada abajo y el signo con su cabeza a la altura de la de los números
        val cabeza = base - 64.sp.toPx() * 0.73f
        drawText(cifra, topLeft = Offset(-3.dp.toPx(), base - cifra.firstBaseline))
        drawText(por, topLeft = Offset(cifra.size.width + 4.dp.toPx(), cabeza - 2.dp.toPx()))
    }
}

/**
 * El tema: «Tema» y el nombre del que tienes, con la flecha. Tocarla abre
 * debajo el mismo carrusel de mini portadas de Personalizar, y elegir uno
 * cambia la app en el acto (Preferencias.tema, la misma de Ajustes).
 */
@Composable
private fun FilaTema() {
    val actual by Preferencias.tema.collectAsState()
    var abierto by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierto) 90f else 0f, tween(if (Tema.movimiento) 0 else 240, easing = ENFATICA), label = "giro")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .pulsable({ abierto = !abierto }, escala = 0.99f)
                .heightIn(min = Alto.fila)
                .padding(horizontal = Espacio.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("Tema", Modifier.weight(1f), style = estilo(Tipo.l, Paleta.tinta))
            BasicText(actual.nombre, style = estilo(Tipo.m, Paleta.tenue))
            IconoTrazo(Lucide.siguiente, Paleta.tenue, 16.dp, 2.2f, Modifier.padding(start = Espacio.xs).graphicsLayer { rotationZ = giro })
        }
        AnimatedVisibility(abierto) {
            CarruselTemas(Modifier.padding(bottom = Espacio.s), PaddingValues(horizontal = Espacio.xxs))
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
        val color = Paleta.suave.copy(alpha = if (hay) 1f else 0.5f)
        Row(
            Modifier.fillMaxWidth().heightIn(min = Alto.fila).clickable(enabled = hay, role = Role.Button) { confirmando = true }.padding(horizontal = Espacio.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(if (hay) "Reiniciar mi avance" else "Nada que reiniciar todavía", Modifier.weight(1f), style = estilo(Tipo.l, color))
            if (hay) IconoTrazo(IconosHoja.reiniciar, color, 16.dp, 2f)
        }
        return
    }
    Column(Modifier.padding(vertical = Espacio.s, horizontal = Espacio.xxs)) {
        BasicText("¿Empezar de cero?", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = Tipo.m, lineHeight = 20.sp, color = Paleta.tinta))
        BasicText(
            "Se borra${loQueSeBorra(estado)} en esta carrera. Vuelves al 0 % y no se puede deshacer.",
            Modifier.padding(top = Espacio.xxs),
            style = TextStyle(fontFamily = i, fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.suave),
        )
        Row(Modifier.padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Box(
                Modifier.weight(1f).heightIn(min = Alto.toque).clip(RoundedCornerShape(Radio.Bloque)).background(Paleta.tinta.copy(alpha = 0.08f)).clickable(role = Role.Button) { confirmando = false },
                contentAlignment = Alignment.Center,
            ) { BasicText("Cancelar", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = Tipo.s, color = Paleta.tinta)) }
            Box(
                Modifier.weight(1f).heightIn(min = Alto.toque).clip(RoundedCornerShape(Radio.Bloque)).background(Paleta.rojo.copy(alpha = 0.16f)).clickable(role = Role.Button) {
                    estado.reiniciar()
                    confirmando = false
                },
                contentAlignment = Alignment.Center,
            ) { BasicText("Borrar todo", style = TextStyle(fontFamily = i, fontWeight = FontWeight.SemiBold, fontSize = Tipo.s, color = ROJO_LETRA)) }
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
