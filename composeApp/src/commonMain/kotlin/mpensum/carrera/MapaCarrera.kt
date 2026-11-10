package mpensum.carrera

import mpensum.mapa.vistaDeColumna
import mpensum.mapa.vistaDeAltura
import mpensum.mapa.rememberTiposMapa
import mpensum.mapa.gestosDelMapa
import mpensum.mapa.cadenaDe
import mpensum.mapa.TiposMapa
import mpensum.mapa.TarjetaMapa
import mpensum.mapa.NodoMapa
import mpensum.mapa.EfectosMapa
import mpensum.mapa.Camara
import mpensum.mapa.AnimacionesTarjeta
import mpensum.mapa.APAGADO_MS
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mpensum.carrera.EstadoCarrera
import mpensum.componentes.rememberPartesListas
import mpensum.logica.Situacion
import mpensum.tema.Movimiento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Paleta
import mpensum.tema.Tema
import kotlin.math.abs

/*
 * El mapa de una carrera, plano a plano como en la web (GrafoPensum.jsx y
 * PlanosGrafo.jsx): la rejilla quieta detrás, y encima una capa que mueve la
 * cámara con los cables, la luz de la frontera, las cabeceras y las
 * tarjetas. Mover el mapa es mover esa capa: no recompone ni repinta nada.
 */

/** La opacidad de lo que queda fuera del foco en el teléfono (--opacidad-foco) */
internal const val OPACIDAD_FOCO = 0.14f
/** Lo que tarda en enfocarse la cadena de la materia abierta, y en trazarse su ruta */
private const val FOCO_MS = 320
private const val TRAZADO_MS = 560
/** La llegada del mapa la primera vez por carrera */
private const val LLEGADA_MS = Movimiento.MEDIA + 100
/** La descarga espera a que el mapa se corra para enseñar lo que se abre, y luego recorre el cable */
internal const val ESPERA_DESCARGA = 380
internal const val DURACION_DESCARGA = 1300
/** La rejilla del fondo: cuadros de 34 en píxeles de pantalla, quietos (DefsGrafo.jsx) */
private const val PASO_REJILLA = 34f
/** «Lo aprobado: Atenuado»: la opacidad de las tarjetas aprobadas y de sus cables */
internal const val ATENUADO = 0.45f

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
internal class EntradaDelMapa(estado: EstadoCarrera) {
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
