package mpensum.carrera.lista

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.carrera.EstadoCarrera
import mpensum.logica.GrupoLista
import mpensum.logica.Marca
import mpensum.logica.SeccionEstado
import mpensum.logica.Situacion
import mpensum.logica.claveDeSemestre
import mpensum.logica.filaDe
import mpensum.logica.filaDeCasilla
import mpensum.logica.metaDeGrupo
import mpensum.logica.razonTrasMarcar
import mpensum.logica.subtituloDeLista
import mpensum.logica.tituloDeGrupo
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Movimiento
import mpensum.tema.Paleta

/*
 * La vista Lista, «Por hacer»: la misma carrera que el mapa, ordenada por lo
 * que el estudiante decide: qué meto ya y qué no puedo atrasar. Una sola
 * LazyColumn con la materia por clave y los encabezados pegajosos. Tocar una
 * fila abre la página de la materia; deslizarla a la derecha da el paso
 * siguiente. De pie la página se empuja encima; acostada es el panel de la
 * derecha y la mitad nunca queda vacía.
 */

/** Lo que mide la columna de la lista acostada, como mucho */
private val ANCHO_LISTA_ACOSTADA = 400.dp

/** Cuánto asoma el velo del lienzo sobre la barra Núcleo, para que las filas no se corten en seco contra ella */
private val ALTO_VELO = 120.dp

/** Cuánto se corre la lista bajo la página que entra: la mitad de lo que ya sugiere la página de iOS */
private const val PARALAJE = 0.25f

/** Cuántos ms dura la materia del panel al cambiar de una a otra: un fundido, sin desplazamiento */
private const val FUNDIDO_PANEL_MS = 200

/**
 * `abajo` es lo que tapa la barra Núcleo (de pie) o el margen inferior
 * (acostada); `derecha`, el recorte seguro de ese lado. `navegacion` vive en
 * el interior: de ella dependen la barra y el Atrás del sistema.
 */
@Composable
fun VistaLista(
    estado: EstadoCarrera,
    navegacion: NavegacionLista,
    abajo: Dp,
    acostada: Boolean,
    derecha: Dp,
    alElegirElectiva: (casilla: String, aprobar: Boolean) -> Unit,
    alVerEnMapa: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alcance = rememberCoroutineScope()
    val sesion = remember(estado, navegacion) { SesionLista(estado, navegacion, alcance) }
    val simulador = estado.simulador
    LaunchedEffect(sesion, simulador) { sesion.medir() }
    // Elegir una electiva cambia las casillas y con ellas la estructura: se vuelve a ordenar
    LaunchedEffect(estado.casillas) { sesion.soltar() }

    val arriba = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val elegirElectiva by rememberUpdatedState(alElegirElectiva)
    val enAcostada by rememberUpdatedState(acostada)
    val acciones = remember(sesion) {
        AccionesFila(
            tocar = { f ->
                if (f.esCasilla) elegirElectiva(f.codigo, false)
                else {
                    navegacion.elegida = f.codigo
                    if (!enAcostada) navegacion.abierta = true
                }
            },
            deslizar = { f, paso -> sesion.marcar(mapOf(f.codigo to paso.marca)) },
            deshacer = sesion::deshacer,
        )
    }

    // Acostado se elige la primera de «Puedes inscribir» para que el panel nunca quede vacío
    val primera = sesion.primera
    LaunchedEffect(acostada, primera) {
        val elegida = navegacion.elegida
        if (acostada && (elegida == null || sesion.universo.materia(elegida) == null)) navegacion.elegida = primera
    }

    // Al volver de la página, la fila que se miraba se queda en su sitio con su realce
    val estabaAbierta = remember { booleanArrayOf(navegacion.abierta) }
    LaunchedEffect(navegacion.abierta) {
        if (estabaAbierta[0] && !navegacion.abierta) navegacion.elegida?.let(sesion::realzar)
        estabaAbierta[0] = navegacion.abierta
    }

    // Acostado, Atrás cierra la subpágina de «+N más» del panel; de pie lo hace el Atrás del interior
    ManejarAtras(acostada && navegacion.relacion != null) { navegacion.relacion = null }

    val etiquetaVolver = if (navegacion.porEstado) "Por hacer" else "Por semestre"

    Box(modifier.fillMaxSize().background(Paleta.lienzo)) {
        if (acostada) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth(0.55f).widthIn(max = ANCHO_LISTA_ACOSTADA).fillMaxHeight()) {
                    CuerpoDeLista(sesion, acciones, arriba, abajo, acostada = true)
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(top = arriba + 16.dp, bottom = abajo, start = 12.dp, end = derecha + 16.dp)) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(REALCE)) {
                        Crossfade(navegacion.elegida, animationSpec = tween(FUNDIDO_PANEL_MS), label = "panel") { codigo ->
                            if (codigo != null) PaginaDeMateria(
                                sesion, codigo, acostada = true, etiquetaVolver = etiquetaVolver, arriba = 0.dp, abajo = 0.dp,
                                alVolver = {}, alVerEnMapa = alVerEnMapa, alIrA = { navegacion.elegida = it },
                            )
                        }
                    }
                }
            }
        } else {
            PaginaEmpujada(sesion, acciones, arriba, abajo, etiquetaVolver, alVerEnMapa)
        }
    }
}

/**
 * De pie: la lista y, empujada encima, la página de la materia. El empuje es
 * una sola animación leída en la capa: nada se recompone por cuadro. La
 * página solo existe mientras se ve (entrando, quieta o saliendo).
 */
@Composable
private fun PaginaEmpujada(
    sesion: SesionLista,
    acciones: AccionesFila,
    arriba: Dp,
    abajo: Dp,
    etiquetaVolver: String,
    alVerEnMapa: (String) -> Unit,
) {
    val navegacion = sesion.navegacion
    // La barra Núcleo se va con la página abierta: su muelle solo deja sitio a los botones o gestos del sistema
    val abajoPagina = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    // Si se gira con la página abierta, nace ya empujada: no se vuelve a animar
    val empuje = remember { Animatable(if (navegacion.abierta) 1f else 0f) }
    LaunchedEffect(navegacion.abierta) {
        val meta = if (navegacion.abierta) 1f else 0f
        if (Movimiento.reducido) empuje.snapTo(meta)
        else empuje.animateTo(meta, tween(Movimiento.EMPUJE, easing = if (navegacion.abierta) Movimiento.ENTRA else Movimiento.ESTANDAR))
    }
    val montada by remember { derivedStateOf { empuje.value > 0f } }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { translationX = -empuje.value * size.width * PARALAJE }
            .drawWithContent {
                drawContent()
                // La lista se apaga un poco bajo la página que entra
                drawRect(Color.Black.copy(alpha = 0.18f * empuje.value))
            },
    ) {
        CuerpoDeLista(sesion, acciones, arriba, abajo, acostada = false)
        // Bajo la barra Núcleo las filas se funden con el lienzo
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(ALTO_VELO).drawBehind {
                val sin = Paleta.lienzo.copy(alpha = 0f)
                drawRect(Brush.verticalGradient(0f to sin, 0.7f to Paleta.lienzo, 1f to Paleta.lienzo))
            },
        )
    }
    if (montada) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { translationX = (1f - empuje.value) * size.width }
                .background(Paleta.lienzo)
                .drawBehind { drawLine(Paleta.tinta.copy(alpha = 0.06f), Offset(0.5f, 0f), Offset(0.5f, size.height), 1f) },
        ) {
            Crossfade(navegacion.elegida, animationSpec = tween(FUNDIDO_PANEL_MS), label = "pagina") { codigo ->
                if (codigo != null) PaginaDeMateria(
                    sesion, codigo, acostada = false, etiquetaVolver = etiquetaVolver, arriba = arriba, abajo = abajoPagina,
                    alVolver = { navegacion.abierta = false }, alVerEnMapa = alVerEnMapa, alIrA = { navegacion.elegida = it },
                )
            }
        }
    }
}

/** La cabecera, los encabezados y las filas en una sola `LazyColumn`; el menú «…» flota encima */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CuerpoDeLista(sesion: SesionLista, acciones: AccionesFila, arriba: Dp, abajo: Dp, acostada: Boolean) {
    val estado = sesion.estado
    val navegacion = sesion.navegacion
    val estructura = sesion.estructura
    val universo = sesion.universo
    val lista = rememberLazyListState()
    var menu by remember { mutableStateOf(false) }
    ManejarAtras(menu) { menu = false }

    // Cursando lleva al pie «Aprobé las N» cuando tiene dos o más: así se aprueba el semestre de un toque
    val conPie: (GrupoLista) -> Boolean = { g -> g.clave == SeccionEstado.CURSANDO.clave && g.codigos.size >= 2 }

    // Al cambiar de orden la lista es otra: por semestre se entra en el que vas; por estado, desde arriba
    val primera = remember { booleanArrayOf(true) }
    LaunchedEffect(navegacion.porEstado) {
        if (primera[0]) { primera[0] = false; return@LaunchedEffect }
        val destino = if (navegacion.porEstado) 0 else indiceDeGrupo(sesion.estructura, claveDeActual(sesion), navegacion.aprobadasAbiertas, conPie)
        lista.scrollToItem(destino)
    }

    val progreso = estado.progreso
    val subtitulo = subtituloDeLista(progreso.ucAprobadas, progreso.ucTitulo, progreso.aprobadas, estado.graduacion.periodoFinal)
    val cursandoAhora = universo.materias.filter { it.situacion == Situacion.CURSANDO }
    // Lo que no tiene margen va en su propia nube, encima del resto de «Puedes inscribir»
    val corte = if (navegacion.porEstado) sesion.corteSinMargen else 0

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = arriba, bottom = abajo + 16.dp),
        ) {
            item(key = "cabecera", contentType = "cabecera") {
                CabeceraDeLista(estado.pensum.nombreCorto, subtitulo, compacta = acostada) { menu = true }
            }
            var indice = 1
            for (g in estructura) {
                val propio = indice
                stickyHeader(key = "h-${g.clave}", contentType = "encabezado") {
                    EncabezadoDeSeccion(
                        titulo = tituloDeGrupo(g.clave),
                        meta = metaDeGrupo(g.clave, universo),
                        plegable = g.clave == SeccionEstado.APROBADAS.clave,
                        abierto = navegacion.aprobadasAbiertas,
                        alPlegar = { navegacion.aprobadasAbiertas = !navegacion.aprobadasAbiertas },
                        fijado = { lista.firstVisibleItemIndex > propio || (lista.firstVisibleItemIndex == propio && lista.firstVisibleItemScrollOffset > 0) },
                    )
                }
                val plegada = g.clave == SeccionEstado.APROBADAS.clave && !navegacion.aprobadasAbiertas
                val pie = conPie(g)
                indice += 1 + (if (plegada) 0 else g.codigos.size) + (if (pie) 1 else 0)
                if (plegada) continue
                val corteAqui = if (g.clave == SeccionEstado.INSCRIBIR.clave && corte in 1 until g.codigos.size) corte else 0
                itemsIndexed(g.codigos, key = { _, c -> c }, contentType = { _, _ -> "fila" }) { i, codigo ->
                    val base = universo.materia(codigo)?.let { filaDe(it, sesion.margenes[codigo]) }
                        ?: universo.casilla(codigo)?.let { filaDeCasilla(it, resumida = navegacion.porEstado) }
                        ?: return@itemsIndexed
                    val c = sesion.cambio
                    val marcada = c != null && codigo in c.codigos
                    val fila = if (marcada) base.copy(razon = razonTrasMarcar(estado.marcas[codigo])) else base
                    FilaDeLista(
                        fila = fila,
                        primera = i == 0 || (corteAqui > 0 && i == corteAqui),
                        ultima = (i == g.codigos.lastIndex && !pie) || (corteAqui > 0 && i == corteAqui - 1),
                        seleccionada = acostada && navegacion.elegida == codigo,
                        ofreceDeshacer = c != null && marcada && c.codigos.size == 1,
                        realce = sesion.realces[codigo] ?: 0,
                        acciones = acciones,
                        hueco = corteAqui > 0 && i == corteAqui,
                    )
                }
                if (pie) item(key = "pie-${g.clave}", contentType = "pie") {
                    val c = sesion.cambio
                    val n = cursandoAhora.size
                    when {
                        c != null && c.seccion == g.clave -> PieDeTarjeta("Deshacer", conSello = false, alTocar = sesion::deshacer)
                        n >= 1 -> PieDeTarjeta(
                            if (n == 1) "Aprobé la que queda" else "Aprobé las $n", conSello = true,
                            alTocar = { sesion.marcar(cursandoAhora.associate { it.codigo to Marca.APROBADA }, seccion = g.clave) },
                        )
                        else -> PieDeTarjeta("Ninguna en curso", conSello = false, alTocar = null)
                    }
                }
            }
        }
        if (menu) MenuDeLista(
            porEstado = navegacion.porEstado,
            alElegir = { menu = false; sesion.ordenar(!navegacion.porEstado) },
            alCerrar = { menu = false },
            arriba = arriba,
        )
    }
}

/** El grupo del semestre por el que vas, al ordenar por semestre */
private fun claveDeActual(sesion: SesionLista): String = claveDeSemestre(sesion.actual)

/** El lugar que ocupa un grupo en la columna: la cabecera, y por cada grupo anterior su encabezado, sus filas y su pie */
private fun indiceDeGrupo(estructura: List<GrupoLista>, clave: String, aprobadasAbiertas: Boolean, conPie: (GrupoLista) -> Boolean): Int {
    var indice = 1
    for (g in estructura) {
        if (g.clave == clave) return indice
        indice += 1 + (if (g.clave == SeccionEstado.APROBADAS.clave && !aprobadasAbiertas) 0 else g.codigos.size) + (if (conPie(g)) 1 else 0)
    }
    return 0
}
