package mpensum.carrera

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mpensum.componentes.SOMBRA_FLOTANTE
import mpensum.componentes.Vidrio
import mpensum.componentes.ventana
import mpensum.componentes.vidrio
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.tema.Radio
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.porTema
import kotlin.math.exp

/*
 * La hoja que sube desde abajo en el teléfono (HojaInferior.jsx). Todas las
 * hojas de la app pasan por aquí, así que el movimiento se define una vez:
 *
 * - Entra con un resorte críticamente amortiguado (sin rebote) y sale con una
 *   curva de aceleración: lo que llega se asienta, lo que se va no se detiene.
 * - Sigue al dedo 1:1 desde el asa, la cabecera o el propio contenido (cuando
 *   está arriba del todo), con goma al tirar hacia arriba. Se cierra por
 *   distancia o por velocidad; si no, vuelve con un resorte.
 * - Por encima de ella asoma siempre la pantalla de debajo, desenfocada y con
 *   un velo tenue. Todo va con una sola posición (`desp`) que solo se lee al
 *   dibujar: ni un cuadro recompone.
 */

/** Entrada: resorte sin rebote. Con 380 de rigidez asienta en unos 350 ms, y si se interrumpe sigue desde donde iba */
private val ENTRA = spring<Float>(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = 0.5f)
/** Vuelta cuando el tirón no llegó al umbral: algo más rígido, con la velocidad del dedo */
private val VUELVE = spring<Float>(dampingRatio = 1f, stiffness = 420f, visibilityThreshold = 0.5f)
/** Salida tras un tirón: arranca ya a la velocidad del dedo (pendiente 1) y acelera al final */
private val SALE_DEDO = CubicBezierEasing(0.35f, 0.35f, 0.9f, 0.55f)
/** Salida por atrás o por el aspa, desde arriba del todo: aceleración (Material), arranca lenta y se va */
private const val SALE_MS = Movimiento.SALIDA_HOJA

/** Cuánto hay que bajarla (fracción de su alto) o a qué velocidad (dp/s) basta un tirón corto */
private const val UMBRAL_FRACCION = 0.25f
private const val UMBRAL_VELOCIDAD_DP = 1200f
/** Lo más que se estira hacia arriba, en dp: la goma tiende a este tope sin llegar */
private const val GOMA_DP = 40f
/** Lo más ancha que llega una hoja si no pide otra cosa */
private val ANCHO_HOJA = 560.dp
/** Lo que baja el contenido de la hoja a pantalla completa, donde de pie va el asa */
internal val RESPIRO_COMPLETA = Espacio.xs
/** Posición de la hoja que no ha entrado: fuera de cualquier pantalla */
private const val OCULTA = 1e6f

/** El borde de los paneles (--panel-borde) */
val BORDE_PANEL get() = Paleta.borde

/** El vidrio de la hoja sobre el horario: el de la barra, con el filo más cerca del borde y la esquina de 32 dp */
private val VIDRIO_HOJA = Vidrio(
    fondoOscuro = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.1f))),
    filoOscuro = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.4f to Color.White.copy(alpha = 0.04f), 1f to Color.White.copy(alpha = 0.07f)),
    medioFilo = 0.4f,
    radioFilo = 32.dp,
)

/** El cristal de la hoja flotante: casi opaco, para leerse sobre cualquier cosa, y con lo de detrás apenas sugerido */
private val ESTILO_CRISTAL get() = HazeStyle(
    backgroundColor = Paleta.lienzo,
    tint = HazeTint(Paleta.vidrio.copy(alpha = 0.86f)),
    blurRadius = 28.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(Paleta.vidrio.copy(alpha = 0.86f)),
)

/**
 * Lo de detrás de la hoja: la pantalla desenfocada con un velo tenue. Sin
 * desenfoque (API < 31) queda solo el velo, más fuerte, para que la hoja se
 * separe igual. El radio es fijo: lo que se anima es la opacidad de la capa.
 */
private val ESTILO_FONDO get() = HazeStyle(
    backgroundColor = Paleta.lienzo,
    tint = HazeTint(Paleta.lienzo.copy(alpha = 0.35f)),
    blurRadius = 20.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(VELO),
)

/**
 * El velo sin desenfoque: negro en oscuro; en claro, la tinta a menos de la
 * mitad, que sobre papel un negro fuerte se ve sucio y ya basta para separar.
 */
private val VELO get() = porTema(Color.Black.copy(alpha = 0.6f), Paleta.sombra.copy(alpha = 0.28f))

/**
 * El HazeState de la pantalla sobre la que sube la hoja. Cada pantalla lo
 * ofrece una vez y las hojas lo toman solas, sin pasarlo por cada función.
 */
val LocalFondoHoja = staticCompositionLocalOf<HazeState?> { null }

/**
 * La posición de la hoja y todo lo que la mueve. `desp` es cuánto está bajada
 * respecto a su sitio, en px: 0 abierta, `oculto()` fuera de la pantalla. La
 * entrada, la salida, el dedo y el velo salen de este único número.
 */
@Stable
private class Hoja(private val alcance: CoroutineScope) {
    var desp by mutableFloatStateOf(OCULTA)
    var alto by mutableFloatStateOf(0f)
    var abierta = false
    var alCerrar: () -> Unit = {}
    var porDedo = false
    var insets: WindowInsets = WindowInsets(0, 0, 0, 0)
    var densidad: Density = Density(1f)
    var extraPx = 0f
    /** Si la última composición la pintó a pantalla completa (acostado): al girar cambia */
    var completa = false

    /** Donde está el dedo sin goma: negativo es por encima de su sitio */
    private var crudo = 0f
    private var tomada = false
    private var trabajo: Job? = null

    /** Lo que sobra por debajo del borde de la hoja: barra de gestos, teclado, margen y la sombra */
    fun margen() = insets.getBottom(densidad) + extraPx

    /** Cuánto hay que bajarla para que no se vea nada, ni su sombra */
    fun oculto() = alto + margen()

    /** 1 con la hoja arriba, 0 con ella fuera: el velo y el desenfoque se aclaran con el tirón */
    fun progreso(): Float = if (alto <= 0f) 0f else 1f - (desp / alto).coerceIn(0f, 1f)

    /** Una animación a la vez: la nueva (o el dedo) corta la anterior donde esté */
    fun correr(bloque: suspend () -> Unit) {
        trabajo?.cancel()
        trabajo = alcance.launch { bloque() }
    }

    /** Fuera y sin medir, como antes de montarse: la próxima vez entra desde abajo con su alto nuevo */
    fun recoger() {
        trabajo?.cancel()
        alto = 0f
        desp = OCULTA
    }

    private suspend fun animar(hasta: Float, spec: AnimationSpec<Float>, velocidad: Float = 0f) {
        animate(desp, hasta, velocidad, spec) { v, _ -> desp = v }
    }

    suspend fun entrar() {
        porDedo = false
        desp = minOf(desp, oculto())
        // Con «Reducir movimiento» la hoja llega con una curva corta, sin el resorte
        animar(0f, if (Movimiento.reducido) tween(Movimiento.CORTA, easing = Movimiento.ESTANDAR) else ENTRA)
    }

    suspend fun salir() {
        val meta = oculto()
        val restante = ((meta - desp) / meta).coerceIn(0f, 1f)
        // Por distancia: desde arriba del todo 260 ms (o 270 tras un tirón); desde casi fuera, la mitad
        if (porDedo) animar(meta, tween((140 + 130 * restante).toInt(), easing = SALE_DEDO))
        else animar(meta, tween((SALE_MS * (0.5f + 0.5f * restante)).toInt(), easing = Movimiento.SALE))
    }

    /** El dedo agarra la hoja, aunque esté a medio entrar o volviendo */
    fun tomar() {
        if (tomada || !abierta) return
        tomada = true
        trabajo?.cancel()
        crudo = minOf(desp, oculto())
    }

    /** Seguimiento 1:1 hacia abajo; hacia arriba, con goma que se endurece */
    fun arrastrar(d: Float) {
        crudo += d
        desp = if (crudo >= 0f) crudo else -goma(-crudo, GOMA_DP * densidad.density)
    }

    /** Al soltar: cierra por distancia o por velocidad (px/s, positiva hacia abajo); si no, vuelve con un resorte */
    fun soltar(velocidad: Float) {
        tomada = false
        if (!abierta) return
        val dp = densidad.density
        val tiron = velocidad >= UMBRAL_VELOCIDAD_DP * dp && desp > 8f * dp
        val lejos = desp >= UMBRAL_FRACCION * alto && velocidad > -UMBRAL_VELOCIDAD_DP * dp
        if (tiron || lejos) {
            porDedo = true
            alCerrar()
        } else {
            correr { animar(0f, VUELVE, velocidad) }
        }
    }

    /**
     * El contenido manda mientras pueda: con la hoja bajada, subir el dedo la
     * devuelve antes de desplazar nada; y cuando el contenido ya está arriba
     * del todo, lo que sobra del gesto baja la hoja.
     */
    val conexion = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!abierta || source != NestedScrollSource.UserInput || available.y >= 0f || desp <= 0f) return Offset.Zero
            tomar()
            val d = maxOf(available.y, -crudo.coerceAtLeast(0f))
            arrastrar(d)
            return Offset(0f, d)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!abierta || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            tomar()
            arrastrar(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!tomada) return Velocity.Zero
            soltar(available.y)
            return Velocity(0f, available.y)
        }
    }
}

/** Goma: 1:1 al principio y cada vez más dura, sin pasar nunca de `tope` */
private fun goma(x: Float, tope: Float) = tope * (1f - exp(-x / tope))

/**
 * Con `cristal`, la hoja flota: separada 8 dp de los bordes, redonda entera
 * y de cristal sobre lo que pasa por ese HazeState (el horario). Sin él, es
 * el panel pegado abajo de siempre.
 *
 * `fondo` es el HazeState de la pantalla de debajo, la que se desenfoca por
 * encima de la hoja; por defecto sale de [LocalFondoHoja]. Sin ninguno, solo
 * hay velo.
 *
 * `ancho` es lo más ancha que llega (centrada). Con `desplazable` en falso
 * la hoja no desplaza su contenido: lo estira a su alto y cada columna del
 * contenido se desplaza por su cuenta. Con `altoFijo` la hoja va siempre a su
 * alto grande (la pantalla menos lo que asoma arriba) y no mide su contenido:
 * navegar dentro de ella no mueve su borde. Es lo que hace iOS con las hojas
 * que tienen páginas.
 *
 * Con el teléfono acostado no hay alto para una hoja que deja asomar lo de
 * detrás: sube igual, pero ocupa la pantalla entera (como en el iPhone cuando
 * falta alto). Sin asa ni tirón para cerrarla, que con columnas que desplazan
 * cerraría sin querer: se cierra con su aspa o con atrás. El contenido se
 * aparta solo de la barra de estado, la cámara y la barra de gestos. Girar con
 * la hoja abierta no la rehace: la cabecera y el contenido se mueven enteros
 * de una forma a la otra, con su desplazamiento, lo escrito y sus `remember`.
 *
 * Con `sobreOtraHoja` (el selector de electiva sobre el progreso) no se desenfoca
 * nada: la hoja de debajo se ve atenuada por un velo a media fuerza, como las
 * hojas apiladas de iOS, y no se pierde de dónde vienes.
 *
 * Con `modal` en falso (la ficha de materia) no hay velo ni desenfoque: el mapa
 * de debajo sigue vivo, los toques fuera de la hoja le llegan y la separación
 * es el color del panel con una sombra tenue por encima, pintada una vez. Con
 * `tope` (fracción del alto de la pantalla) la hoja no crece más y lo que
 * sobra se desplaza dentro. `alMedir` recibe su alto cada vez que cambia, de
 * pie y acostada.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun BoxScope.HojaInferior(
    abierta: Boolean,
    alCerrar: () -> Unit,
    cristal: HazeState? = null,
    fondo: HazeState? = cristal ?: LocalFondoHoja.current,
    ancho: Dp = ANCHO_HOJA,
    desplazable: Boolean = true,
    altoFijo: Boolean = false,
    modal: Boolean = true,
    sobreOtraHoja: Boolean = false,
    tope: Float? = null,
    alMedir: (Dp) -> Unit = {},
    cabecera: @Composable () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    var montada by remember { mutableStateOf(abierta) }
    if (abierta && !montada) montada = true
    val alcance = rememberCoroutineScope()
    val hoja = remember { Hoja(alcance) }
    val completa = ventana().apaisado
    if (completa != hoja.completa) {
        hoja.completa = completa
        // Girar mientras sale: la que se va cambiaría de forma a medio camino (acostada, un panel liso
        // que tapa el mapa unos 260 ms). Ya iba a desaparecer: se recoge en este mismo cuadro
        if (!abierta && montada) {
            hoja.recoger()
            montada = false
        }
    }
    // A pantalla completa no hay hoja flotante: el cristal es para asomar por los bordes, y no hay bordes
    val flotante = cristal != null && !completa
    val densidad = LocalDensity.current
    hoja.abierta = abierta
    hoja.alCerrar = alCerrar
    hoja.densidad = densidad
    // Con la hoja flotante también cuenta la barra de gestos; con el panel pegado, solo el teclado
    hoja.insets = if (flotante) WindowInsets.ime.union(WindowInsets.navigationBars) else WindowInsets.ime
    hoja.extraPx = with(densidad) { (if (flotante) 8.dp else 0.dp).toPx() + 32.dp.toPx() }

    // Solo cuenta si ya está medida, no cuánto mide: mientras la ficha anima su alto, la hoja no se recompone en cada cuadro
    val medida by remember { derivedStateOf { hoja.alto > 0f } }
    // Se mide antes de moverla: entra desde fuera de la pantalla, sea cual sea su alto
    LaunchedEffect(abierta, medida) {
        if (!medida) return@LaunchedEffect
        if (abierta) {
            hoja.correr { hoja.entrar() }
        } else if (montada) {
            hoja.correr {
                hoja.salir()
                montada = false
                hoja.alto = 0f
                hoja.desp = OCULTA
            }
        }
    }
    ManejarAtras(abierta, alCerrar)

    // La cabecera y el contenido se componen una sola vez y se mueven de la hoja flotante a la pantalla
    // completa al girar: así no pierden el desplazamiento, lo escrito ni sus remember. Se descartan al desmontarse
    val cabeceraActual by rememberUpdatedState(cabecera)
    val contenidoActual by rememberUpdatedState(contenido)
    val desplazableActual by rememberUpdatedState(desplazable)
    val cabeceraMovil = remember { movableContentOf { cabeceraActual() } }
    val cuerpoMovil = remember {
        // `fuera` es lo que pone la columna de la hoja (el peso); `dentro`, lo que se desplaza con el contenido
        movableContentOf { fuera: Modifier, dentro: Modifier ->
            Column(
                fuera.then(if (desplazableActual) Modifier.verticalScroll(rememberScrollState()) else Modifier).then(dentro),
                content = contenidoActual,
            )
        }
    }
    val medir: (IntSize) -> Unit = {
        hoja.alto = it.height.toFloat()
        alMedir(with(densidad) { it.height.toDp() })
    }

    if (!montada) return

    if (modal) FondoHoja(if (sobreOtraHoja) null else fondo, hoja, remember { MutableInteractionSource() }, alCerrar, if (sobreOtraHoja) 0.5f else 1f)

    if (completa) {
        PantallaCompleta(hoja, medir, cabeceraMovil) { cuerpoMovil(Modifier.weight(1f), Modifier) }
        return
    }

    val forma = if (flotante) RoundedCornerShape(32.dp) else RoundedCornerShape(topStart = Radio.Hoja, topEnd = Radio.Hoja)
    // Nunca más alta que la pantalla menos 64 (o la barra de estado y 32): arriba siempre asoma lo que hay detrás
    val libreArriba = maxOf(64.dp, WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 32.dp)
    val altoMaximo = tope?.let { ventana().alto * it }
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = libreArriba)
            .windowInsetsPadding(if (flotante) WindowInsets.ime.union(WindowInsets.navigationBars) else WindowInsets.ime)
            .then(if (flotante) Modifier.padding(start = Espacio.xs, end = Espacio.xs, bottom = Espacio.xs) else Modifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                // En horizontal la hoja no se estira a todo el ancho: unos 560 dp, centrada
                .widthIn(max = ancho)
                .fillMaxWidth()
                .then(if (altoMaximo != null) Modifier.heightIn(max = altoMaximo) else Modifier)
                .then(if (altoFijo) Modifier.fillMaxHeight() else Modifier)
                .onSizeChanged(medir)
                .graphicsLayer {
                    // Antes de medirla, o ya entera fuera, no asoma ni un píxel
                    translationY = minOf(hoja.desp, size.height + hoja.margen())
                }
                .then(
                    // Al estirarla hacia arriba el panel pegado no deja un hueco abajo: el color sigue por debajo, fuera del recorte (el shadow ya recorta, por eso va antes)
                    if (flotante) Modifier else Modifier.drawBehind { drawRect(Paleta.panel, Offset(0f, size.height - 1f), Size(size.width, GOMA_DP.dp.toPx() + 2f)) },
                )
                .then(
                    // Sin velo detrás, la hoja se separa del mapa con un degradado que sube 40 dp, no con una sombra grande
                    // En ligero la sombra de elevación se vuelve a teselar en cada cuadro de la subida: un degradado cuesta una vez
                    if (modal && !Tema.ligero) Modifier.shadow(24.dp, forma, ambientColor = SOMBRA_FLOTANTE, spotColor = SOMBRA_FLOTANTE) else Modifier.sombraSuave()
                )
                .clip(forma)
                .then(
                    if (cristal != null) {
                        // Mientras sube se mueve: esmerilarla de nuevo en cada cuadro es el coste; se esmerila al asentarse
                        Modifier.vidrio(cristal, ESTILO_CRISTAL, VIDRIO_HOJA) { hoja.desp == 0f }
                    } else {
                        Modifier.panelPegado()
                    },
                )
                .nestedScroll(hoja.conexion)
                .pointerInput(Unit) { detectTapGestures {} },
        ) {
            AsaYCabecera(hoja, abierta, modal, cabeceraMovil)
            cuerpoMovil(
                if (desplazable) Modifier else Modifier.weight(1f, fill = altoFijo),
                // Pegada abajo, el contenido no queda bajo la barra de gestos; con teclado, sube con él
                Modifier.padding(bottom = if (flotante) 0.dp else WindowInsets.navigationBars.exclude(WindowInsets.ime).asPaddingValues().calculateBottomPadding()),
            )
        }
    }
}

/**
 * La hoja acostada: sube como siempre, pero a toda la pantalla y lisa, del
 * color del panel. El contenido se aparta de la barra de estado, de la cámara
 * y de la barra de gestos; el fondo del panel sí llega a los bordes. `cuerpo`
 * ya trae su peso y su desplazamiento: es el mismo de la hoja de pie.
 */
@Composable
private fun PantallaCompleta(
    hoja: Hoja,
    medir: (IntSize) -> Unit,
    cabecera: @Composable () -> Unit,
    cuerpo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .onSizeChanged(medir)
            .graphicsLayer { translationY = minOf(hoja.desp, size.height + hoja.margen()) }
            .background(Paleta.panel)
            // Se queda con los toques que caen en su fondo (si no, llegarían al velo y la cerrarían), sin volverse un botón
            .pointerInput(Unit) { detectTapGestures {} }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            // Sin asa, la cabecera baja 8 dp: no queda pegada al borde
            .padding(top = RESPIRO_COMPLETA),
    ) {
        Column(Modifier.testTag("hoja-cabecera")) { cabecera() }
        cuerpo()
    }
}

/** El fondo: la pantalla de debajo, difuminada, que entra y sale con la hoja; sin pantalla que difuminar (o en modo ligero), un velo */
@OptIn(ExperimentalHazeApi::class)
@Composable
private fun FondoHoja(fondo: HazeState?, hoja: Hoja, toque: MutableInteractionSource, alCerrar: () -> Unit, fuerza: Float) {
    if (fondo != null && !Tema.sinDesenfoque) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeEffect(fondo, ESTILO_FONDO) {
                    inputScale = HazeInputScale.Auto
                    alpha = hoja.progreso()
                }
                .clickable(toque, indication = null, onClick = alCerrar),
        )
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = hoja.progreso() * fuerza }
                .background(VELO)
                .clickable(toque, indication = null, onClick = alCerrar),
        )
    }
}

/**
 * La separación de la hoja sin velo: en oscuro una sombra; en claro, el mapa
 * que se funde en el papel (una sombra gris sobre el lienzo claro ensucia las
 * tarjetas). El degradado pasa de `y = 0` y se queda en su tono final hasta
 * bajar el radio de la esquina: así las esquinas redondas de la hoja no dejan
 * asomar el mapa sin teñir bajo un corte recto.
 */
private fun Modifier.sombraSuave(): Modifier = drawWithCache {
    val alto = 32.dp.toPx()
    val esquina = Radio.Hoja.toPx()
    val tono = if (Tema.claro) Paleta.lienzo.copy(alpha = 0.88f) else Paleta.sombra.copy(alpha = 0.45f)
    val brocha = Brush.verticalGradient(0f to tono.copy(alpha = 0f), 1f to tono, startY = -alto, endY = 0f)
    onDrawBehind { drawRect(brocha, Offset(0f, -alto), Size(size.width, alto + esquina)) }
}

/** El panel pegado abajo: fondo liso y el filo de luz de arriba, sin desenfoque */
private fun Modifier.panelPegado(): Modifier =
    this
        .background(Paleta.panel)
        .drawWithCache {
            // El mismo filo de luz que el cristal, solo arriba: se apaga a los 120 dp y deja un hilo en los lados
            val g = 1.dp.toPx()
            // En claro la hoja es blanca: un filo blanco no se vería, lo dibuja la tinta muy floja
            val filo = if (Tema.claro) {
                Brush.verticalGradient(0f to Paleta.tinta.copy(alpha = 0.1f), 1f to Paleta.tinta.copy(alpha = 0.04f), startY = 0f, endY = 120.dp.toPx())
            } else {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.2f),
                    1f to Color.White.copy(alpha = 0.04f),
                    startY = 0f,
                    endY = 120.dp.toPx(),
                )
            }
            val r = CornerRadius(28.dp.toPx() - g / 2)
            // El trazo se alarga por debajo, fuera del recorte: así no hay esquinas abajo
            val colgante = 48.dp.toPx()
            onDrawWithContent {
                drawContent()
                drawRoundRect(filo, Offset(g / 2, g / 2), Size(size.width - g, size.height - g / 2 + colgante), r, style = Stroke(g))
            }
        }

@Composable
private fun AsaYCabecera(hoja: Hoja, abierta: Boolean, modal: Boolean, cabecera: @Composable () -> Unit) {
    // La zona de la que se tira: el asa y la cabecera, una franja entera y no solo la rayita
    Column(
        Modifier
            .testTag("hoja-cabecera")
            .draggable(
                state = rememberDraggableState { hoja.arrastrar(it) },
                orientation = Orientation.Vertical,
                enabled = abierta,
                onDragStarted = { hoja.tomar() },
                onDragStopped = { hoja.soltar(it) },
            ),
    ) {
        // La hoja sin velo lleva el asa centrada entre su borde y la cabecera: 8 arriba y 8 abajo
        Box(Modifier.fillMaxWidth().padding(top = if (modal) 10.dp else 8.dp, bottom = if (modal) 5.dp else 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Paleta.tinta.copy(alpha = 0.28f)))
        }
        cabecera()
    }
}
