package mpensum.carrera

import mpensum.mapa.vistaDeColumna
import mpensum.mapa.vistaDeAltura
import mpensum.mapa.rememberTiposMapa
import mpensum.mapa.gestosDelMapa
import mpensum.mapa.fuerte
import mpensum.mapa.cadenaDe
import mpensum.mapa.TiposMapa
import mpensum.mapa.TarjetaMapa
import mpensum.mapa.Segmento
import mpensum.mapa.NodoMapa
import mpensum.mapa.Margen
import mpensum.mapa.FranjaColocada
import mpensum.mapa.Franja
import mpensum.mapa.EfectosMapa
import mpensum.mapa.DatosCabecera
import mpensum.mapa.Camara
import mpensum.mapa.CabeceraSemestre
import mpensum.mapa.Arista
import mpensum.mapa.AnimacionesTarjeta
import mpensum.mapa.APAGADO_MS
import mpensum.mapa.ALTO_ENCABEZADO
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import mpensum.carrera.EstadoCarrera
import mpensum.componentes.rememberPartesListas
import mpensum.logica.Situacion
import mpensum.logica.Tramo
import mpensum.logica.marcasDeSemestres
import mpensum.logica.tramoDe
import mpensum.tema.Movimiento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.avivar
import mpensum.tema.porTema
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * El mapa de una carrera, plano a plano como en la web (GrafoPensum.jsx y
 * PlanosGrafo.jsx): la rejilla quieta detrás, y encima una capa que mueve la
 * cámara con los cables, la luz de la frontera, las cabeceras y las
 * tarjetas. Mover el mapa es mover esa capa: no recompone ni repinta nada.
 */

/** La opacidad de lo que queda fuera del foco en el teléfono (--opacidad-foco) */
private const val OPACIDAD_FOCO = 0.14f
/** Lo que tarda en enfocarse la cadena de la materia abierta, y en trazarse su ruta */
private const val FOCO_MS = 320
private const val TRAZADO_MS = 560
/** La llegada del mapa la primera vez por carrera */
private const val LLEGADA_MS = Movimiento.MEDIA + 100
/** La descarga espera a que el mapa se corra para enseñar lo que se abre, y luego recorre el cable */
private const val ESPERA_DESCARGA = 380
private const val DURACION_DESCARGA = 1300
/** La rejilla del fondo: cuadros de 34 en píxeles de pantalla, quietos (DefsGrafo.jsx) */
private const val PASO_REJILLA = 34f
/** «Lo aprobado: Atenuado»: la opacidad de las tarjetas aprobadas y de sus cables */
private const val ATENUADO = 0.45f
/** La luz de la frontera escribe su reloj a 30 cuadros por segundo, como la aurora */
private const val CUADRO_LUZ_NS = 32_000_000L
/** Sin tocar el mapa tanto tiempo, la luz se apaga hasta el siguiente gesto */
private const val DORMIR_LUZ_MS = 8_000L

@Composable
fun MapaCarrera(
    estado: EstadoCarrera,
    camara: Camara,
    efectos: EfectosMapa,
    seleccionado: String?,
    congelado: Boolean,
    alTocarMateria: (String) -> Unit,
    alTocarVacio: () -> Unit,
    alAbrirCasilla: (String) -> Unit,
    alAlternarSemestre: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Sin fondo ni rejilla, el mapa flota sobre lo que haya detrás (la guía de bienvenida) */
    conFondo: Boolean = true,
    /** En horizontal el mapa pasa por detrás del riel: lo que ocupa a la izquierda (dp) se descuenta en el encuadre inicial; null en vertical */
    reservaIzquierda: Float? = null,
) {
    val layout = estado.layout
    val densidad = LocalDensity.current
    val arriba = with(densidad) { WindowInsets.statusBars.getTop(this).toDp().value }
    val abajo = with(densidad) { WindowInsets.navigationBars.getBottom(this).toDp().value }
    val tipos = rememberTiposMapa()
    val medidor = rememberTextMeasurer()
    // Los Path de los cables se arman una vez para los cables y la luz, y de nuevo solo si cambian el mapa, sus colores o el tema
    val cables = remember(layout, estado.colores, Tema.juego, Tema.contraste) { cablesDe(estado) }

    // El foco: lo que mira la ficha abierta o la ruta fijada; lo demás se aparta
    val mirada = seleccionado ?: efectos.ruta
    val cadena = remember(mirada, layout) { mirada?.let { cadenaDe(it, layout.relaciones) } }
    var cadenaVisible by remember { mutableStateOf<Set<String>?>(null) }
    val foco = remember { Animatable(0f) }
    val trazado = remember { Animatable(1f) }
    LaunchedEffect(cadena) {
        if (cadena != null) {
            cadenaVisible = cadena
            foco.animateTo(1f, tween(FOCO_MS, easing = Movimiento.FLUIDA))
        } else {
            foco.animateTo(0f, tween(FOCO_MS, easing = Movimiento.FLUIDA))
            cadenaVisible = null
        }
    }
    LaunchedEffect(mirada) {
        if (mirada == null) return@LaunchedEffect
        trazado.snapTo(0f)
        trazado.animateTo(1f, tween(TRAZADO_MS, easing = Movimiento.FLUIDA))
    }
    val descarga = remember { Animatable(1f) }
    LaunchedEffect(efectos.descarga?.n) {
        if (efectos.descarga == null) return@LaunchedEffect
        descarga.snapTo(0f)
        descarga.animateTo(1f, tween(ESPERA_DESCARGA + DURACION_DESCARGA, easing = LinearEasing))
    }
    LaunchedEffect(efectos.carga) {
        val c = efectos.carga ?: return@LaunchedEffect
        if (!c.hecha) return@LaunchedEffect
        delay(APAGADO_MS.toLong())
        efectos.apagarCarga(c.n)
    }
    // La llegada del mapa es solo la primera vez por carrera: al volver de la lista o del horario ya está puesto
    val llegada = remember { Animatable(if (camara.yaLlego) 1f else 0f) }
    LaunchedEffect(camara.colocada) {
        if (!camara.colocada) return@LaunchedEffect
        // Si otra instancia ya hizo la llegada (el AnimatedContent de la vista puede montar dos a la vez),
        // esta se pone visible de golpe: nunca puede quedarse en 0 esperando una llegada que ya pasó
        if (camara.yaLlego || Movimiento.reducido) {
            camara.yaLlego = true
            llegada.snapTo(1f)
            return@LaunchedEffect
        }
        camara.yaLlego = true
        llegada.animateTo(1f, tween(LLEGADA_MS, easing = Movimiento.Mapa.LLEGADA))
    }

    // El mapa entra por partes, unas pocas tarjetas por cuadro desde la columna que mira la cámara: el cuadro
    // en que arranca el zoom de entrada no carga con las sesenta, y las demás llegan mientras siguen fuera
    val entrada = remember(estado.nodos) { EntradaDelMapa(estado) }
    val listos = rememberPartesListas(entrada.lotes)

    // Recordada: cada tarjeta guarda la suya y no recompone cuando el mapa sí
    val opacidadDe = remember { { codigo: String? ->
        {
            val visible = cadenaVisible
            if (visible == null || (codigo != null && codigo in visible)) 1f else 1f - (1f - OPACIDAD_FOCO) * foco.value
        }
    } }

    Box(
        modifier
            .fillMaxSize()
            .then(if (conFondo) Modifier.background(Paleta.lienzo) else Modifier)
            .clipToBounds()
            .onSizeChanged { t ->
                val d = densidad.density
                camara.medir(t.width / d, t.height / d, layout.ancho.toFloat(), estado.alto.toFloat())
                // Abre en tu semestre a escala de lectura, o donde lo dejaste en esta sesión
                camara.colocarInicial {
                    val columna = semestreDeTrabajo(estado)?.let { s -> layout.columnas.firstOrNull { it.semestre == s } } ?: layout.columnas.first()
                    if (reservaIzquierda != null) vistaDeAltura(columna.x.toFloat(), layout.finSemestres.toFloat(), camara.alto, arriba, abajo, reservaIzquierda)
                    else vistaDeColumna(columna.x.toFloat(), camara.ancho, arriba)
                }
            }
            .gestosDelMapa(camara, alTocarVacio),
    ) {
        if (conFondo) Spacer(Modifier.fillMaxSize().fondoDelMapa())
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(layout.ancho.toFloat().dp, estado.alto.toFloat().dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = camara.x * density
                    translationY = (camara.y + 12f * (1f - llegada.value)) * density
                    scaleX = camara.escala
                    scaleY = camara.escala
                    alpha = llegada.value
                    // La entrada no pinta el plano entero aparte para fundirlo: cada trazo baja su alfa
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
        ) {
            Cables(estado, cables, cadena = { cadenaVisible }, foco = { foco.value }, trazado = { trazado.value }, descarga = { efectos.descarga?.codigo to descarga.value })
            // Al tocar no hay cables a la vista: tampoco la luz que viaja por ellos
            if (!Tema.sinBucles && Tema.cables != FormaCables.AL_TOCAR) LuzDeFrontera(estado, cables, congelada = { camara.enGesto || congelado || seleccionado != null }, sinGesto = { !camara.enGesto }, cadena = { cadenaVisible }, foco = { foco.value })

            CabecerasSemestres(estado, tipos, medidor, entrada, listos, alAlternarSemestre)
            estado.franja?.let { RotulosFranja(it, estado, tipos) }

            TarjetasMapa(estado, camara, efectos, tipos, entrada, listos, cadena, seleccionado, opacidadDe, alTocarMateria, alAbrirCasilla)
        }
    }
}

/** La cabecera de cada semestre: su número, lo que cuenta y el aro que lo aprueba entero */
@Composable
private fun CabecerasSemestres(estado: EstadoCarrera, tipos: TiposMapa, medidor: TextMeasurer, entrada: EntradaDelMapa, listos: Int, alAlternarSemestre: (Int) -> Unit) {
    val layout = estado.layout
    val sit = estado.situaciones
    val porColumna = remember(layout) { layout.nodos.groupBy { it.semestre } }
    val cuentas = remember(layout, estado.estados, estado.casillas) { marcasDeSemestres(layout.nodos.map { it.asignatura }, estado::enCasilla, estado.estados) }
    layout.columnas.forEach { columna ->
        // Cada cabecera entra con la primera tarjeta de su semestre
        if (entrada.loteDe(columna.semestre) >= listos) return@forEach
        val delSemestre = porColumna[columna.semestre].orEmpty()
        val materias = delSemestre.map { estado.materiaDe(it) }
        val situaciones = materias.map { m -> m?.let { sit[it.codigo] } ?: Situacion.LEJANA }
        CabeceraSemestre(
            DatosCabecera(
                semestre = columna.semestre,
                x = columna.x.toFloat(),
                uc = materias.sumOf { it?.uc ?: 0 },
                total = delSemestre.size,
                hechas = situaciones.count { it == Situacion.HECHA },
                cursando = situaciones.count { it == Situacion.CURSANDO },
                inscribibles = situaciones.count { it == Situacion.INSCRIBIBLE },
                casilla = cuentas[columna.semestre],
            ),
            tipos, medidor,
            compacta = layout.altoEncabezado < ALTO_ENCABEZADO,
            alAlternar = { alAlternarSemestre(columna.semestre) },
        )
    }
}

/** Las tarjetas de las materias y las casillas, con el foco de la cadena y las animaciones de toque, descarga y carga */
@Composable
private fun TarjetasMapa(
    estado: EstadoCarrera,
    camara: Camara,
    efectos: EfectosMapa,
    tipos: TiposMapa,
    entrada: EntradaDelMapa,
    listos: Int,
    cadena: Set<String>?,
    seleccionado: String?,
    opacidadDe: (String?) -> () -> Float,
    alTocarMateria: (String) -> Unit,
    alAbrirCasilla: (String) -> Unit,
) {
    val layout = estado.layout
    val sit = estado.situaciones
    val haptica = LocalHapticFeedback.current
    val descargado = efectos.descarga
    // Los toques llegan por estados: así las lambdas de cada tarjeta no cambian al cambiar la elegida, y la tarjeta se salta
    val tocarMateria by rememberUpdatedState(alTocarMateria)
    val abrirCasilla by rememberUpdatedState(alAbrirCasilla)
    estado.nodos.forEach { nodo ->
        if (entrada.loteDe(nodo) >= listos) return@forEach
        key(nodo.codigo) {
            val materia = estado.materiaDe(nodo)
            val codigo = materia?.codigo
            val situacion = codigo?.let { sit[it] }
            // «Lo aprobado: Atenuado»: la tarjeta aprobada baja al 45 % salvo la que se está mirando
            val atenuada = Tema.atenuarAprobadas && situacion == Situacion.HECHA && codigo != seleccionado
            val opacidad = remember(codigo, opacidadDe, atenuada) {
                val delFoco = opacidadDe(codigo)
                if (atenuada) ({ delFoco() * ATENUADO }) else delFoco
            }
            TarjetaMapa(
                nodo = nodo,
                materia = materia,
                lineas = codigo?.let { layout.porCodigo[it]?.lineasNombre } ?: nodo.lineasNombre,
                situacion = situacion,
                acento = materia?.let(estado.colores::de) ?: Paleta.suave,
                seleccionada = codigo != null && codigo == seleccionado,
                resaltada = codigo != null && cadena?.contains(codigo) == true,
                opacidad = opacidad,
                animaciones = AnimacionesTarjeta(
                    toque = efectos.toque?.takeIf { it.codigo == codigo }?.n,
                    destello = descargado?.takeIf { situacion == Situacion.INSCRIBIBLE && materia?.prerrequisitos?.contains(it.codigo) == true }?.n,
                    carga = efectos.carga?.takeIf { it.codigo == codigo },
                ),
                escala = { camara.escala },
                tipos = tipos,
                camara = camara,
                alTocar = { if (codigo != null) tocarMateria(codigo) else abrirCasilla(nodo.codigo) },
                alEmpezarCarga = { if (codigo != null) efectos.empezarCarga(codigo) },
                alSoltarCarga = { efectos.soltarCarga() },
                alFijar = {
                    if (codigo != null) {
                        efectos.fijarRuta(codigo)
                        haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
        }
    }
}

/** Las tarjetas que se componen en cada cuadro de la entrada: crear sus nodos y medir sus tres textos es lo que cuesta */
private const val TARJETAS_POR_CUADRO = 4

/**
 * En qué cuadro de la entrada se compone cada tarjeta. Por columnas desde la
 * que encuadra la cámara al abrir ([semestreDeTrabajo]) y después las demás
 * por cercanía, primero la de su derecha, que es la que asoma en la pantalla;
 * dentro de cada columna, de arriba abajo. Lo que no es de un semestre (la
 * franja de electivas, abajo del todo) va al final.
 */
private class EntradaDelMapa(estado: EstadoCarrera) {
    private val porCodigo: Map<String, Int>
    private val porSemestre: Map<Int, Int>

    /** Cuántos cuadros dura la entrada */
    val lotes: Int

    init {
        val semestres = estado.layout.columnas.map { it.semestre }
        val centro = semestreDeTrabajo(estado) ?: semestres.firstOrNull()
        val cercania = centro?.let { c -> semestres.sortedWith(compareBy<Int>({ abs(it - c) }, { it < c })).withIndex().associate { (i, s) -> s to i } }.orEmpty()
        val orden = estado.nodos.sortedWith(compareBy<NodoMapa>({ it.semestre?.let(cercania::get) ?: Int.MAX_VALUE }, { it.y }))
        porCodigo = orden.withIndex().associate { (i, n) -> n.codigo to i / TARJETAS_POR_CUADRO }
        porSemestre = semestres.associateWith { s -> orden.indexOfFirst { it.semestre == s }.coerceAtLeast(0) / TARJETAS_POR_CUADRO }
        lotes = maxOf(1, (orden.size + TARJETAS_POR_CUADRO - 1) / TARJETAS_POR_CUADRO)
    }

    fun loteDe(nodo: NodoMapa): Int = porCodigo[nodo.codigo] ?: 0

    /** La cabecera de un semestre entra con su primera tarjeta */
    fun loteDe(semestre: Int): Int = porSemestre[semestre] ?: 0
}

/**
 * Dónde encuadra la cámara al abrir: el semestre más bajo donde hay algo que
 * hacer (inscribible o en curso); si no, lo primero pendiente (layout/camara.js).
 * No es «el semestre actual» de la portada y la barra: aquí interesa lo
 * accionable, y por eso es otra regla con otro nombre.
 */
private fun semestreDeTrabajo(estado: EstadoCarrera): Int? {
    val reales = estado.layout.nodos.filter { !it.asignatura.esHueco }
    if (reales.isEmpty()) return null
    val sit = estado.situaciones
    reales.filter { sit[it.codigo] == Situacion.INSCRIBIBLE || sit[it.codigo] == Situacion.CURSANDO }.mapNotNull { it.semestre }.minOrNull()?.let { return it }
    reales.filter { sit[it.codigo] != Situacion.HECHA }.mapNotNull { it.semestre }.minOrNull()?.let { return it }
    return reales.mapNotNull { it.semestre }.maxOrNull()
}

/** Los puntos del fondo, en los cruces de la rejilla. Se arman una vez por tamaño, no en cada cuadro */
private fun puntosDeRejilla(ancho: Float, alto: Float, paso: Float): List<Offset> {
    val columnas = (ancho / paso).toInt() + 1
    val filas = (alto / paso).toInt() + 1
    val puntos = ArrayList<Offset>(columnas * filas)
    for (f in 0 until filas) for (c in 0 until columnas) puntos += Offset(c * paso, f * paso)
    return puntos
}

/**
 * El fondo quieto del mapa: la rejilla, puntos en sus cruces o nada (liso),
 * según [Tema.fondo]. Los puntos son una sola llamada con la lista guardada
 * por tamaño; cambiar de fondo o de tema rehace solo este cuadro.
 */
private fun Modifier.fondoDelMapa(): Modifier = drawWithCache {
    val fondo = Tema.fondo
    val paso = PASO_REJILLA * density
    val color = Paleta.rejilla
    val puntos = if (fondo == Fondo.PUNTOS) puntosDeRejilla(size.width, size.height, paso) else emptyList()
    val colorPunto = lerp(color, Paleta.tinta, 0.1f)
    onDrawBehind {
        when (fondo) {
            Fondo.REJILLA -> {
                var x = 0f
                while (x <= size.width) {
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += paso
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                    y += paso
                }
            }
            Fondo.PUNTOS -> drawPoints(puntos, PointMode.Points, colorPunto, 2f * density, StrokeCap.Round)
            Fondo.LISO -> Unit
        }
    }
}

/* La fuerza de cada cable según lo que dice (Arista.jsx) */
private fun opacidad(t: Tramo): Float {
    val base = when (t) {
        Tramo.FRONTERA -> 0.55f
        Tramo.RECORRIDO -> 0.5f
        Tramo.PROXIMA -> 0.32f
        Tramo.LEJANA -> porTema(0.12f, 0.2f)
    }
    // En Exterior los cables se firman más: a pleno sol los finos se pierden
    return if (Tema.fuerte) (base * 1.5f).coerceAtMost(0.9f) else base
}

private fun grosor(t: Tramo) = when (t) {
    Tramo.FRONTERA -> 1.75f
    Tramo.RECORRIDO -> 1.5f
    Tramo.PROXIMA -> 1.25f
    Tramo.LEJANA -> 1f
}

/** Un cable ya convertido a Path, medido, con sus extremos y sus colores */
private class CableDibujo(val arista: Arista, val ruta: Path, val desde: Color, val hacia: Color, val inicio: Offset, val fin: Offset) {
    private val medida = PathMeasure().apply { setPath(ruta, false) }
    val largo = medida.length
    private val parte = Path()

    /** El trozo del principio hasta la fracción `f` del largo. Reutiliza siempre el mismo Path */
    fun hasta(f: Float): Path {
        parte.reset()
        medida.getSegment(0f, largo * f.coerceIn(0f, 1f), parte, true)
        return parte
    }

    /** El trozo entre dos distancias, acotadas al largo del cable. Reutiliza siempre el mismo Path */
    fun trozo(desde: Float, hasta: Float): Path {
        parte.reset()
        val ini = desde.coerceIn(0f, largo)
        val fin = hasta.coerceIn(0f, largo)
        if (fin > ini) medida.getSegment(ini, fin, parte, true)
        return parte
    }
}

private fun cablesDe(estado: EstadoCarrera): List<CableDibujo> {
    val porCodigo = estado.layout.porCodigo
    return estado.layout.aristas.map { a ->
        val ruta = Path().apply {
            for (s in a.trazo) when (s) {
                is Segmento.Mover -> moveTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Linea -> lineTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Curva -> cubicTo(s.x1.toFloat(), s.y1.toFloat(), s.x2.toFloat(), s.y2.toFloat(), s.x.toFloat(), s.y.toFloat())
            }
        }
        val inicio = a.trazo.first()
        val fin = a.trazo.last()
        CableDibujo(
            a, ruta,
            porCodigo[a.origen]?.asignatura?.let(estado.colores::de)?.let(::colorDeCable) ?: Paleta.suave,
            porCodigo[a.destino]?.asignatura?.let(estado.colores::de)?.let(::colorDeCable) ?: Paleta.suave,
            Offset(inicio.x.toFloat(), inicio.y.toFloat()), Offset(fin.x.toFloat(), fin.y.toFloat()),
        )
    }
}

/** El color de un cable: el de su área, más o menos tirado a la tinta según el tema (`Juego.cables`: 1 el del área, 0 la tinta) */
private fun colorDeCable(area: Color): Color {
    val color = Tema.juego.cables
    return if (color >= 1f) area else lerp(Paleta.tinta, area, color)
}

/**
 * La luz de la frontera no es de un color: sale con el del área de la que
 * viene, pasa por ese mismo color girado medio tono y llega con el de la
 * materia que puedes inscribir (el oklch de la web, aquí en HSL).
 */
private fun girar(color: Color, grados: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    if (mx == mn) return color
    val l = (mx + mn) / 2
    val d = mx - mn
    val s = if (l > 0.5f) d / (2 - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } * 60f
    return Color.hsl((h + grados) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun degradado(c: CableDibujo, vivo: Boolean): Brush {
    val medio = girar(c.desde, 48f)
    val colores = if (vivo) listOf(avivar(c.desde), avivar(medio), avivar(c.hacia)) else listOf(c.desde, medio, c.hacia)
    return Brush.linearGradient(colores, c.inicio, c.fin)
}

/**
 * Los cables: la base de cada uno con la fuerza de lo que dice, la cadena
 * de la materia que se mira dibujándose de origen a destino, y la descarga
 * verde de la que se acaba de aprobar. Las rutas se arman una vez; en cada
 * cuadro solo cambian opacidades y trozos. En modo ligero la frontera va del
 * color de su origen, sin degradado: un shader por cable menos que pintar.
 */
@Composable
private fun Cables(
    estado: EstadoCarrera,
    cables: List<CableDibujo>,
    cadena: () -> Set<String>?,
    foco: () -> Float,
    trazado: () -> Float,
    descarga: () -> Pair<String?, Float>,
) {
    Spacer(
        // En su propia capa: la luz de la frontera, las cabeceras o el foco no la repintan, y moverla no la toca
        Modifier.fillMaxSize().graphicsLayer {}.drawWithCache {
            // Degradados y trazos se arman una vez: en cada cuadro solo se eligen
            val degradados = cables.map { degradado(it, false) }
            // El grosor elegido es un factor del trazo que ya se crea: no cuesta nada más
            val factor = Tema.grosor.factor
            val trazos = Tramo.entries.associateWith { Stroke(grosor(it) * factor, cap = StrokeCap.Round) }
            val linea = Stroke(2.25f * factor, cap = StrokeCap.Round)
            onDrawBehind {
                scale(density, Offset.Zero) {
                    val sit = estado.situaciones
                    val enFoco = cadena()
                    val f = foco()
                    val (aprobada, tiempo) = descarga()
                    val conDegradado = !Tema.ligero
                    // Al tocar: solo los cables de la materia señalada (la cadena), que entran con el foco
                    val soloTocar = Tema.cables == FormaCables.AL_TOCAR
                    val atenuar = Tema.atenuarAprobadas
                    for ((i, c) in cables.withIndex()) {
                        val a = c.arista
                        val tramo = tramoDe(sit[a.origen] ?: Situacion.LEJANA, sit[a.destino] ?: Situacion.LEJANA)
                        val frontera = tramo == Tramo.FRONTERA
                        val enCadena = enFoco != null && a.origen in enFoco && a.destino in enFoco
                        if (!soloTocar || enCadena) {
                            val apartado = if (enFoco != null && !enCadena) 1f - (1f - OPACIDAD_FOCO) * f else 1f
                            // Dentro del foco la base baja a 0,2 y la cadena se dibuja encima, en su color
                            val base = if (enCadena) opacidad(tramo) + (0.2f - opacidad(tramo)) * f else opacidad(tramo) * apartado
                            // Lo aprobado, atenuado, y al tocar la base entra con el foco
                            val atenuado = if (atenuar && tramo == Tramo.RECORRIDO) ATENUADO else 1f
                            val alfa = base * atenuado * (if (soloTocar) f else 1f)
                            val estilo = trazos.getValue(tramo)
                            if (frontera && conDegradado) drawPath(c.ruta, degradados[i], alpha = alfa, style = estilo)
                            else drawPath(c.ruta, c.desde.copy(alpha = alfa), style = estilo)
                            if (enCadena && f > 0f) {
                                val trozo = c.hasta(trazado())
                                if (frontera && conDegradado) drawPath(trozo, degradados[i], alpha = 0.95f * f, style = linea)
                                else drawPath(trozo, c.desde.copy(alpha = 0.95f * f), style = linea)
                            }
                            // El punto de llegada: el de la frontera siempre, el de la cadena con el foco puesto
                            if (frontera) drawCircle(c.hacia.copy(alpha = (if (enCadena) 1f else 0.9f) * apartado), 3f, c.fin)
                            else if (enCadena && f > 0f) drawCircle(c.desde.copy(alpha = f), 3f, c.fin)
                        }
                        if (aprobada != null && a.origen == aprobada && tiempo < 1f) descargaPor(c, tiempo)
                    }
                }
            }
        },
    )
}

/**
 * La luz al aprobar: el cable entero se enciende en verde de origen a
 * destino, se queda encendido un instante y se apaga. Con un halo ancho y
 * flojo debajo que hace de resplandor sin filtro.
 */
private fun DrawScope.descargaPor(c: CableDibujo, tiempo: Float) {
    val total = (ESPERA_DESCARGA + DURACION_DESCARGA).toFloat()
    val t = ((tiempo * total) - ESPERA_DESCARGA) / DURACION_DESCARGA
    if (t <= 0f) return
    val p = Movimiento.Mapa.DESCARGA.transform(t.coerceIn(0f, 1f))
    val lleno = min(1f, p / 0.58f)
    val alfa = if (p < 0.58f) 1f else 1f - (p - 0.58f) / 0.42f
    val trozo = c.hasta(lleno)
    drawPath(trozo, Paleta.verde.copy(alpha = 0.18f * alfa), style = Stroke(9f, cap = StrokeCap.Round))
    drawPath(trozo, Paleta.verde.copy(alpha = alfa), style = Stroke(2.5f, cap = StrokeCap.Round))
}

/**
 * La luz que viaja por la frontera hacia lo que puedes inscribir: un guion
 * del 9 % del cable que nace y muere apagado. Cada luz a su ritmo, entre 2,6
 * y 3,2 s, y nacida a media marcha. Va en su propia capa, y se congela
 * mientras el mapa se mueve o hay una hoja encima: en un teléfono modesto
 * la luz corriendo durante un gesto le quita cuadros.
 */
@Composable
private fun LuzDeFrontera(estado: EstadoCarrera, cables: List<CableDibujo>, congelada: () -> Boolean, sinGesto: () -> Boolean, cadena: () -> Set<String>?, foco: () -> Float) {
    // El reloj solo corre mientras la luz se ve moverse: congelada, el bucle se para y no pide cuadros;
    // al reanudar sigue desde donde iba, sin salto
    var reloj by remember { mutableFloatStateOf(0f) }
    val estaCongelada by rememberUpdatedState(congelada)
    val estaSinGesto by rememberUpdatedState(sinGesto)
    // Tras DORMIR_LUZ_MS sin gesto la luz se apaga del todo; el siguiente gesto la despierta
    var dormida by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { estaSinGesto() }.collectLatest { quieto ->
            dormida = false
            if (!quieto) return@collectLatest
            delay(DORMIR_LUZ_MS)
            dormida = true
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { estaCongelada() || dormida }.collectLatest { quieta ->
            if (quieta) return@collectLatest
            var antes = -1L
            var acumulado = 0L
            while (true) {
                withFrameNanos { n ->
                    // Tras un cuadro perdido la luz no da un salto
                    if (antes >= 0) acumulado += minOf(n - antes, 100_000_000L)
                    antes = n
                    // A 30 cuadros como la aurora: cada escritura invalida el cristal de la barra
                    if (acumulado >= CUADRO_LUZ_NS) {
                        reloj += acumulado / 1e9f
                        acumulado = 0L
                    }
                }
            }
        }
    }
    Spacer(
        // En su propia capa: cada cuadro de la luz repinta solo la luz, no los cables ni las cabeceras de debajo
        Modifier.fillMaxSize().graphicsLayer {}.drawWithCache {
            // Las fronteras y su degradado cambian al marcar, no en cada cuadro
            val sit = estado.situaciones
            val fronteras = cables.withIndex().filter { (_, c) ->
                tramoDe(sit[c.arista.origen] ?: Situacion.LEJANA, sit[c.arista.destino] ?: Situacion.LEJANA) == Tramo.FRONTERA
            }
            val vivos = fronteras.map { (_, c) -> degradado(c, true) }
            val trazoLuz = Stroke(2.25f, cap = StrokeCap.Round)
            onDrawBehind {
                val enFoco = cadena()
                val f = foco()
                if (fronteras.isEmpty()) return@onDrawBehind
                val segundos = reloj
                scale(density, Offset.Zero) {
                    for ((k, ic) in fronteras.withIndex()) {
                        val (i, c) = ic
                        val enCadena = enFoco != null && c.arista.origen in enFoco && c.arista.destino in enFoco
                        val apagado = if (enFoco != null && !enCadena) 1f - f else 1f
                        if (apagado <= 0f) continue
                        val duracion = 2.6f + ((i * 5) % 7) * 0.1f
                        val retraso = ((i * 7) % 11) * 0.26f
                        val bruto = ((segundos + retraso) / duracion) % 1f
                        val p = Movimiento.Mapa.VIAJE_LUZ.transform(bruto)
                        val alfa = when {
                            bruto < 0.14f -> bruto / 0.14f
                            bruto > 0.82f -> (1f - bruto) / 0.18f
                            else -> 1f
                        } * apagado
                        // El guion (9 % del cable) entra por el origen y sale por el destino
                        val fin = c.largo * p
                        val trozo = c.trozo(fin - c.largo * 0.09f, fin)
                        drawPath(trozo, vivos[k], alpha = alfa.coerceIn(0f, 1f), style = trazoLuz)
                    }
                }
            }
        },
    )
}

/** El rótulo de cada grupo en la franja de electivas, y la raya que la separa de los semestres */
@Composable
private fun RotulosFranja(franja: FranjaColocada, estado: EstadoCarrera, tipos: TiposMapa) {
    val primera = franja.filas.firstOrNull() ?: return
    Spacer(
        Modifier
            .offset(Margen.IZQUIERDA.toFloat().dp, (primera.y - Franja.CORREDOR / 2).toFloat().dp)
            .size((estado.layout.ancho - Margen.IZQUIERDA - Margen.DERECHA).toFloat().dp, 1.dp)
            .background(Paleta.tinta.copy(alpha = 0.08f)),
    )
    franja.filas.forEach { f ->
        val datos = listOfNotNull(
            if (f.elegidas > 0) "${f.elegidas} EN TU MAPA" else "ELIGE LAS TUYAS",
            "${f.opciones} ${if (f.opciones == 1) "OPCIÓN" else "OPCIONES"}",
            f.cuota?.let { "ELIGE $it UC" },
        )
        Column(Modifier.offset(f.x.toFloat().dp, (f.y + 14 - 11.5 * 0.93).toFloat().dp)) {
            BasicText(f.titulo.uppercase(), style = tipos.titulo)
            BasicText(datos.joinToString(" · "), Modifier.offset(y = 6.dp), style = tipos.datos)
        }
    }
}
