package mpensum.carrera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import mpensum.componentes.fuenteCristal
import mpensum.componentes.tinteLiso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpensum.componentes.iconoEstado
import mpensum.componentes.pulsable
import mpensum.logica.AccionSemestre
import mpensum.logica.FilaMateria
import mpensum.logica.FiltroLista
import mpensum.logica.Marca
import mpensum.logica.SeccionLista
import mpensum.logica.Situacion
import mpensum.logica.accionDeSemestre
import mpensum.logica.modeloLista
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.porTema

/*
 * La lista: la misma carrera que el mapa, por semestres, para marcar rápido
 * con el pulgar. Arriba, los cuatro estados con su cuenta, que filtran; cada
 * semestre es un grupo de cristal plegable y, por defecto, solo está abierto
 * el que estás cursando. Un toque en una materia abre su ficha, con los
 * botones de estado; el aro del semestre aprueba o desmarca, con "Deshacer".
 */

/** Lo que mide la franja de las pastillas debajo de la barra de estado: 12 + 44 + 12 */
private val ALTO_FILTROS = Espacio.s + Alto.toque + Espacio.s

/** Separación entre la lista y el panel de su derecha, acostado */
private val SEPARA_COLUMNAS = Espacio.xs

/** La lista toma esta parte del ancho que queda tras el riel, entre un mínimo y un máximo de lectura */
private const val PARTE_LISTA = 0.44f
private val MIN_LISTA = 340.dp
private val MAX_LISTA = 460.dp
/** Lo menos que se deja al panel (resumen o ficha) cuando la ventana es estrecha */
private val MIN_PANEL = 260.dp
private val MIN_LISTA_ESTRECHA = 300.dp

/** Dónde cae el panel de la lista acostada: sus márgenes desde el borde de la pantalla */
class PanelLateral(val inicio: Dp, val fin: Dp, val arriba: Dp)

/**
 * El reparto de la lista acostada: la columna de la lista y, a su derecha, el
 * panel que muestra el resumen del semestre o la ficha. Una sola fuente de
 * verdad para `Lista` e `Interior`. `panel` está medido desde el borde de la
 * pantalla, igual que la ficha lateral.
 */
internal class RepartoLista(val anchoLista: Dp, val panel: PanelLateral)

/**
 * `ancho` es el de la pantalla; `inicio`, lo que ocupa el riel por la
 * izquierda; `derecha`, el recorte seguro de ese lado. Con 640 dp la lista se
 * queda en 300 dp para no ahogar al panel; de 700 dp en adelante rige la
 * proporción.
 */
internal fun repartoLista(ancho: Dp, inicio: Dp, derecha: Dp, arriba: Dp): RepartoLista {
    val fin = derecha + Espacio.m
    val disponible = ancho - inicio - fin
    var lista = (disponible * PARTE_LISTA).coerceIn(MIN_LISTA, MAX_LISTA)
    if (disponible - lista - SEPARA_COLUMNAS < MIN_PANEL) {
        lista = (disponible - SEPARA_COLUMNAS - MIN_PANEL).coerceAtLeast(MIN_LISTA_ESTRECHA)
    }
    return RepartoLista(lista, PanelLateral(inicio + lista + SEPARA_COLUMNAS, fin, arriba + Espacio.m))
}

private val ESTILO_VELO get() = HazeStyle(
    backgroundColor = Paleta.lienzo,
    tint = HazeTint(porTema(Color(0xFF0E0F12).copy(alpha = 0.6f), Paleta.lienzo.copy(alpha = 0.72f))),
    blurRadius = 24.dp,
    noiseFactor = 0f,
)

/** Lo último que se marcó, para devolverlo tal cual estaba */
private class Deshacer(val fila: String?, val seccion: String?, val previas: Map<String, Marca?>)

/** Lo que la lista recuerda mientras está abierta: nada de esto se guarda */
@Stable
private class EstadoVistaLista {
    var filtro by mutableStateOf<FiltroLista?>(null)
    /** Lo que el estudiante plegó o abrió a mano, sin filtro y con filtro (que empieza todo abierto) */
    var abiertos by mutableStateOf(emptyMap<String, Boolean>())
    var abiertosFiltro by mutableStateOf(emptyMap<String, Boolean>())
    var deshacer by mutableStateOf<Deshacer?>(null)
    /** Con filtro, la fila recién marcada se queda en su grupo mientras se puede deshacer */
    var fijadas by mutableStateOf(emptyMap<String, FiltroLista>())
    var avisoCodigo by mutableStateOf<String?>(null)
    var avisoVez by mutableIntStateOf(0)
    /** La espera tras deshacer antes de soltar las retenidas; una marca nueva la cancela */
    var soltar: Job? = null
}

/**
 * La vista Lista de una carrera.
 *
 * `abajo` es el alto que hay que dejar libre al final para la barra Núcleo
 * (sus márgenes incluidos). `alAbrirFicha` recibe el código de la materia;
 * `alElegirElectiva` recibe la casilla y si lo elegido debe quedar aprobado
 * (cuando el aro de un semestre solo espera por su electiva), igual que
 * `abrirCasilla` en el interior. `tapada` es que hay una ficha u hoja encima:
 * Atrás es de ella, y el filtro espera su turno.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun VistaLista(
    estado: EstadoCarrera,
    abajo: Dp,
    modifier: Modifier = Modifier,
    tapada: Boolean = false,
    alAbrirFicha: (String) -> Unit,
    alElegirElectiva: (casilla: String, aprobar: Boolean) -> Unit,
    /** Acostada, el panel de al lado resume el semestre que miras: aquí se avisa de cuál es (el primero abierto) */
    alCambiarSemestre: (Int?) -> Unit = {},
) {
    val control = remember(estado) { EstadoVistaLista() }
    val haptica = LocalHapticFeedback.current
    val alcance = rememberCoroutineScope()
    val abrirFicha by rememberUpdatedState(alAbrirFicha)
    val elegirElectiva by rememberUpdatedState(alElegirElectiva)
    val local = remember { HazeState() }
    val arriba = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    // Todo lo que se pinta sale de aquí: se recalcula una vez por marca, no por fila
    val modelo by remember(estado) {
        derivedStateOf {
            estado.modeloDeLista(control.filtro, control.fijadas)
        }
    }
    // El semestre que se abre solo es el de cuando entraste: aprobar uno no debe plegarlo bajo el dedo
    val actual = remember(estado) { modelo.actual }
    // Se entra con el semestre actual a la vista y el anterior asomando arriba, como en la maqueta
    val lista = rememberLazyListState(
        initialFirstVisibleItemIndex = (modelo.secciones.indexOfFirst { it.semestre == actual } - 1).coerceAtLeast(0),
    )

    fun estaAbierta(s: SeccionLista) = if (control.filtro != null) control.abiertosFiltro[s.clave] ?: true
    else control.abiertos[s.clave] ?: (s.semestre != null && s.semestre == actual)
    val semestreVisto by remember { derivedStateOf { modelo.secciones.firstOrNull { it.semestre != null && estaAbierta(it) }?.semestre } }
    val avisarSemestre by rememberUpdatedState(alCambiarSemestre)
    LaunchedEffect(semestreVisto) { avisarSemestre(semestreVisto) }

    // "Deshacer" dura unos segundos; al irse, las filas retenidas por el filtro van a su sitio
    LaunchedEffect(control.deshacer) {
        if (control.deshacer == null) return@LaunchedEffect
        delay(PLAZO_DESHACER_MS)
        control.deshacer = null
        control.fijadas = emptyMap()
    }
    // Esta lista se monta después que las hojas del interior y, sin la guarda, su Atrás pasaría por delante de ellas
    ManejarAtras(control.filtro != null && !tapada) { control.filtro = null }

    val acciones = remember(estado) {
        accionesDeLista(estado, control, haptica, alcance, { abrirFicha(it) }, { casilla, aprobar -> elegirElectiva(casilla, aprobar) })
    }

    fun filtrar(f: FiltroLista?) {
        if (f == control.filtro) return
        haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
        control.filtro = f
        control.abiertosFiltro = emptyMap()
        control.deshacer = null
        control.fijadas = emptyMap()
        control.avisoCodigo = null
    }
    // Al cambiar de filtro la lista es otra: se empieza desde arriba
    val primera = remember { booleanArrayOf(true) }
    LaunchedEffect(control.filtro) {
        if (primera[0]) primera[0] = false else lista.scrollToItem(0)
    }

    // Acostada, quien llama fija el ancho de la columna (`repartoLista`); filtros, cabecera y filas la comparten
    val columna = Modifier

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            modifier = columna.fillMaxSize().fuenteCristal(local),
            contentPadding = PaddingValues(start = Espacio.m, end = Espacio.m, top = arriba + ALTO_FILTROS, bottom = abajo + Espacio.m),
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            val m = modelo
            items(m.secciones, key = { it.clave }, contentType = { "seccion" }) { s ->
                val abierto = estaAbierta(s)
                val d = control.deshacer
                SeccionDeLista(
                    seccion = s,
                    abierto = abierto,
                    deshacerSeccion = d?.seccion == s.clave,
                    deshacerFila = d?.fila,
                    avisoCodigo = control.avisoCodigo,
                    avisoVez = control.avisoVez,
                    acciones = acciones,
                    modifier = Modifier.animateItem(fadeInSpec = tween(240), placementSpec = tween(360, easing = ENFATICA), fadeOutSpec = tween(160)),
                )
            }
            if (m.secciones.isEmpty()) {
                item(key = "vacio") { SinResultados(control.filtro) { filtrar(null) } }
            }
        }

        CabeceraLista(
            columna = columna,
            arriba = arriba,
            fondo = local,
            lista = lista,
            cuentas = modelo.cuentas,
            filtro = control.filtro,
            alFiltrar = ::filtrar,
        )
    }
}

/** El modelo de la lista de esta carrera; el panel de resumen lo pide sin filtro */
internal fun EstadoCarrera.modeloDeLista(filtro: FiltroLista? = null, fijadas: Map<String, FiltroLista> = emptyMap()) = modeloLista(
    nodos = nodos.map { it.asignatura },
    grupos = pensum.grupos,
    enCasilla = ::enCasilla,
    estados = estados,
    situaciones = situaciones,
    nombreDe = { layout.porCodigo[it]?.asignatura?.nombre },
    filtro = filtro,
    fijadas = fijadas,
)

/** El nombre de cada estado y su color, el mismo de los iconos */
private fun nombreFiltro(f: FiltroLista) = when (f) {
    FiltroLista.APROBADAS -> "Aprobadas"
    FiltroLista.CURSANDO -> "Cursando"
    FiltroLista.DISPONIBLES -> "Disponibles"
    FiltroLista.BLOQUEADAS -> "Bloqueadas"
}

private fun situacionDe(f: FiltroLista) = when (f) {
    FiltroLista.APROBADAS -> Situacion.HECHA
    FiltroLista.CURSANDO -> Situacion.CURSANDO
    FiltroLista.DISPONIBLES -> Situacion.INSCRIBIBLE
    FiltroLista.BLOQUEADAS -> Situacion.LEJANA
}

private fun colorFiltro(f: FiltroLista) = when (f) {
    FiltroLista.APROBADAS -> Paleta.verde
    FiltroLista.CURSANDO -> Paleta.cursando
    FiltroLista.DISPONIBLES -> Paleta.azul
    FiltroLista.BLOQUEADAS -> Paleta.rojo
}

/**
 * Una pastilla de estado: su icono y cuántas hay. Tocarla deja solo esas;
 * tocarla otra vez lo quita. La activa se tiñe de su color y las demás se
 * apagan a la mitad. El cambio va al dibujar, sin recomponer.
 */
@Composable
private fun Pastilla(filtro: FiltroLista, numero: Int, activa: Boolean, apagada: Boolean, alPulsar: () -> Unit, modifier: Modifier) {
    val tinte = animateFloatAsState(if (activa) 1f else 0f, tween(250), label = "tinte")
    val opacidad = animateFloatAsState(if (apagada) 0.5f else 1f, tween(250), label = "opacidad")
    val color = colorFiltro(filtro)
    Row(
        modifier
            .heightIn(min = Alto.toque)
            .semantics {
                selected = activa
                contentDescription = "${nombreFiltro(filtro)}: $numero. ${if (activa) "Quitar el filtro" else "Ver solo estas"}"
            }
            .pulsable(alPulsar, escala = 0.95f)
            .graphicsLayer { alpha = opacidad.value }
            .drawWithCache {
                val r = CornerRadius(size.height / 2)
                val g = 1.dp.toPx()
                val reposo = Brush.verticalGradient(listOf(Paleta.tinta.copy(alpha = porTema(0.07f, 0.03f)), Paleta.tinta.copy(alpha = 0.02f)))
                val conColor = Brush.verticalGradient(listOf(color.copy(alpha = porTema(0.22f, 0.14f)), color.copy(alpha = porTema(0.10f, 0.06f))))
                // El filo es de tinta: luz en oscuro y, a la mitad, una sombra fina en claro
                val f = porTema(1f, 0.55f)
                val filoReposo = degradadoCss(
                    148f, size,
                    0f to Paleta.tinta.copy(alpha = 0.22f * f), 0.38f to Paleta.tinta.copy(alpha = 0.05f * f),
                    0.58f to Paleta.tinta.copy(alpha = 0f), 1f to Paleta.tinta.copy(alpha = 0.09f * f),
                )
                val base = porTema(Color(0xFF101114), Color.White)
                val alfaBase = porTema(0.5f, 0.75f)
                val filoActivo = degradadoCss(
                    148f, size,
                    0f to color.copy(alpha = 0.75f), 0.45f to color.copy(alpha = 0.22f),
                    0.7f to color.copy(alpha = 0.12f), 1f to color.copy(alpha = 0.35f),
                )
                val borde = Size(size.width - g, size.height - g)
                val rBorde = CornerRadius(r.x - g / 2)
                onDrawBehind {
                    val t = tinte.value
                    drawRoundRect(base.copy(alpha = alfaBase + 0.05f * t), cornerRadius = r)
                    if (t < 1f) {
                        drawRoundRect(reposo, cornerRadius = r, alpha = 1f - t)
                        drawRoundRect(filoReposo, Offset(g / 2, g / 2), borde, rBorde, alpha = 1f - t, style = Stroke(g))
                    }
                    if (t > 0f) {
                        drawRoundRect(conColor, cornerRadius = r, alpha = t)
                        drawRoundRect(filoActivo, Offset(g / 2, g / 2), borde, rBorde, alpha = t, style = Stroke(g))
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val s = situacionDe(filtro)
        Canvas(Modifier.size(18.dp)) { iconoEstado(s, 0f, 0f, size.minDimension, Paleta.panel) }
        BasicText(
            "$numero",
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Normal, fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em, fontFeatureSettings = "tnum"),
            color = { lerp(Paleta.suave, Paleta.tinta, tinte.value) },
            maxLines = 1,
        )
    }
}

/** El filtro dejó la lista vacía: se dice en una línea y se ofrece volver a verlo todo */
@Composable
private fun SinResultados(filtro: FiltroLista?, alVerTodas: () -> Unit) {
    val texto = when (filtro) {
        FiltroLista.APROBADAS -> "Aún no has aprobado materias"
        FiltroLista.CURSANDO -> "No estás cursando materias"
        FiltroLista.DISPONIBLES -> "Nada por inscribir ahora"
        FiltroLista.BLOQUEADAS -> "Nada bloqueado"
        null -> return
    }
    Column(Modifier.fillMaxWidth().padding(start = Espacio.xl, end = Espacio.xl, top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            texto,
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.titulo, lineHeight = 28.sp, letterSpacing = (-0.01).em, color = Paleta.suave, textAlign = TextAlign.Center),
        )
        Box(
            Modifier.padding(top = Espacio.xs).heightIn(min = Alto.toque).clickable(interactionSource = null, indication = null, onClick = alVerTodas).padding(horizontal = Espacio.m),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("Ver todas", style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = Tipo.s, color = Paleta.tinta))
        }
    }
}

/** Lo que hace cada toque de la lista: abrir la ficha, el aro del semestre, plegar y deshacer */
private fun accionesDeLista(
    estado: EstadoCarrera,
    control: EstadoVistaLista,
    haptica: HapticFeedback,
    alcance: CoroutineScope,
    abrirFicha: (String) -> Unit,
    elegirElectiva: (casilla: String, aprobar: Boolean) -> Unit,
): AccionesLista {
    fun cambiar(cambios: Map<String, Marca?>, fila: String?, seccion: String?) {
        control.soltar?.cancel()
        val previas = cambios.keys.associateWith { estado.marcas[it] }
        control.filtro?.let { f -> control.fijadas = control.fijadas + cambios.keys.associateWith { control.fijadas[it] ?: f } }
        control.avisoCodigo = null
        estado.marcarVarias(cambios)
        control.deshacer = Deshacer(fila, seccion, previas)
    }
    return AccionesLista(
        // Cualquier toque abre la ficha, con los botones de estado: ya no se marca desde la fila
        tocar = { f: FilaMateria ->
            haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
            abrirFicha(f.codigo)
        },
        mantener = { codigo: String -> abrirFicha(codigo) },
        elegir = { casilla: String -> elegirElectiva(casilla, false) },
        aro = { s: SeccionLista ->
            val semestre = s.semestre
            if (semestre != null) {
                when (val accion = accionDeSemestre(estado.layout.nodos.map { it.asignatura }, semestre, estado::enCasilla, estado.estados)) {
                    is AccionSemestre.Aprobar -> {
                        haptica.performHapticFeedback(HapticFeedbackType.Confirm)
                        cambiar(accion.codigos.associateWith { Marca.APROBADA }, fila = null, seccion = s.clave)
                    }
                    is AccionSemestre.Desmarcar -> {
                        haptica.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        cambiar(accion.codigos.associateWith { null }, fila = null, seccion = s.clave)
                    }
                    is AccionSemestre.Elegir -> elegirElectiva(accion.casilla, true)
                    null -> Unit
                }
            }
        },
        plegar = { clave: String, abierto: Boolean ->
            if (control.filtro != null) control.abiertosFiltro = control.abiertosFiltro + (clave to !abierto)
            else control.abiertos = control.abiertos + (clave to !abierto)
        },
        deshacer = {
            control.deshacer?.let { d ->
                estado.marcarVarias(d.previas)
                control.deshacer = null
                // Un respiro antes de soltar las retenidas: que se vea volver la marca en su sitio
                control.soltar = alcance.launch {
                    delay(600)
                    control.fijadas = emptyMap()
                }
            }
        },
    )
}

/** Filtros por estado sobre un cristal que se esmerila en cuanto la lista pasa por debajo */
@OptIn(ExperimentalHazeApi::class)
@Composable
private fun BoxScope.CabeceraLista(
    columna: Modifier,
    arriba: Dp,
    fondo: HazeState,
    lista: LazyListState,
    cuentas: Map<FiltroLista, Int>,
    filtro: FiltroLista?,
    alFiltrar: (FiltroLista?) -> Unit,
) {
    // El cristal de arriba: transparente al llegar, esmerilado en cuanto la lista pasa por debajo
    val enCima by remember { derivedStateOf { lista.firstVisibleItemIndex == 0 && lista.firstVisibleItemScrollOffset == 0 } }
    val vidrio = animateFloatAsState(if (enCima) 0f else 1f, tween(350), label = "vidrio")
    Box(
        columna
            .fillMaxWidth()
            .height(arriba + ALTO_FILTROS)
            .then(
                if (Tema.sinDesenfoque) Modifier.tinteLiso(ESTILO_VELO) { vidrio.value }
                else Modifier.hazeEffect(fondo, ESTILO_VELO) {
                    alpha = vidrio.value
                    blurEnabled = vidrio.value > 0f
                    inputScale = HazeInputScale.Auto
                },
            )
            .drawBehind {
                val g = 1.dp.toPx()
                drawRect(Paleta.tinta.copy(alpha = 0.06f * vidrio.value), Offset(0f, size.height - g), Size(size.width, g))
            },
    )
    Row(
        columna.fillMaxWidth().padding(start = Espacio.m, end = Espacio.m, top = arriba + Espacio.s).semantics { contentDescription = "Filtrar por estado" },
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        FiltroLista.entries.forEach { f ->
            Pastilla(
                filtro = f,
                numero = cuentas[f] ?: 0,
                activa = filtro == f,
                apagada = filtro != null && filtro != f,
                alPulsar = { alFiltrar(if (filtro == f) null else f) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
