package mpensum.carrera

import mpensum.navegacion.Vista
import mpensum.componentes.LocalFondoHoja
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import mpensum.componentes.fuenteCristal
import mpensum.componentes.esApaisado
import mpensum.datos.Carrera
import mpensum.logica.AccionSemestre
import mpensum.logica.Marca
import mpensum.logica.accionDeSemestre
import mpensum.carrera.lista.NavegacionLista
import mpensum.carrera.lista.VistaLista
import mpensum.carrera.lista.rememberNavegacionLista
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import kotlinx.coroutines.delay
import mpensum.mapa.MARGEN_AL_APROBAR
import mpensum.carrera.MapaCarrera
import mpensum.mapa.Margenes
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Espacio
import mpensum.tema.colorDe

/** Cuánto se separa la barra del borde de abajo */
private val SOBRE_EL_BORDE = 14.dp

/** En horizontal, el riel va a esto del recorte de cámara; su ancho (`ANCHO_RIEL_NAVEGACION`, 52 dp) sale de la barra */
private val SEPARA_RIEL = 14.dp

/** En horizontal, lo que tapa el riel a la izquierda (14 + 52 + 18): el mapa corre por detrás y la cámara lo descuenta */
private val RESERVA_RIEL = SEPARA_RIEL + ANCHO_RIEL_NAVEGACION + 18.dp

/** El velo tras el riel: los cables se funden hacia el borde en vez de cortarse contra el cristal */
private val ANCHO_VELO = 150.dp

/**
 * Dentro de una carrera, como el teléfono de la web: nada arriba, la vista a
 * pantalla completa y, abajo, la barra de cristal con inicio, las tres vistas
 * y el avance. La ficha de la materia flota encima de la barra (en el mapa y
 * en la lista); las hojas (electivas, avance, horario) suben por encima de todo.
 */
@Composable
fun Interior(carrera: Carrera, alVolver: () -> Unit, vistaInicial: Vista = Vista.MAPA) {
    var vista by rememberSaveable { mutableStateOf(vistaInicial) }
    // Se sabe antes de medir porque de ello depende la geometría del mapa
    val horizontal = esApaisado()
    val estado = rememberEstadoCarrera(carrera.slug, horizontal)
    // El horario vive aquí y no en su vista: sus hojas suben por encima de la barra
    val horario = estado?.let { rememberEstadoHorario(it) }
    val color = colorDe(carrera.color)
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Lo que tapa la barra, medido desde el borde: las vistas con lista dejan ese hueco al final
    val tapaBarra = abajo + SOBRE_EL_BORDE + ALTO_BARRA
    val arriba = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    // Lo que pasa por debajo de la barra se ve a través de ella, desenfocado
    val cristal = remember { HazeState() }

    val camara = remember(carrera.slug) { Camara(carrera.slug) }
    val efectos = remember(carrera.slug) { EfectosMapa() }
    val alcance = rememberCoroutineScope()
    var seleccionado by remember(carrera.slug) { mutableStateOf<String?>(null) }
    // La lista vive aquí: la barra Núcleo y el Atrás dependen de si su página está abierta, y sobrevive al giro
    val lista = rememberNavegacionLista(carrera.slug)
    var casillaAbierta by remember(carrera.slug) { mutableStateOf<String?>(null) }
    var aprobarAlElegir by remember { mutableStateOf(false) }
    var avanceAbierto by remember { mutableStateOf(false) }
    // Las materias que pidió «Verlas en el mapa» la hoja del avance: se encuadran cuando el mapa ya se ve
    var porEncuadrar by remember { mutableStateOf<List<String>?>(null) }
    // La materia a la que ya se le hizo sitio: la ficha cambia de alto al animarse y no debe empujar el mapa cada vez
    var encuadrada by remember { mutableStateOf<String?>(null) }

    // Una hoja encima (electivas, avance o clase del horario) se cierra con su propio Atrás
    val hojaAbierta = casillaAbierta != null || avanceAbierto || horario?.hoja != null
    // De pie, la página de la materia de la lista se cierra con Atrás. Cada Atrás tiene su condición propia y excluyente
    // (hoja, página, ruta del mapa): ninguno depende del orden en que se monten
    val paginaDeLista = vista == Vista.LISTA && !horizontal && lista.abierta
    // Con la subpágina de «+N más» abierta, el primer Atrás cierra esa y el siguiente la página
    ManejarAtras(paginaDeLista && !hojaAbierta) { if (lista.relacion != null) lista.relacion = null else lista.abierta = false }
    ManejarAtras(vista == Vista.MAPA && !hojaAbierta && seleccionado == null && efectos.ruta != null) { efectos.ruta = null }
    // Con otra orientación el panel de la ficha cambia de sitio: la materia se vuelve a encuadrar
    LaunchedEffect(horizontal) { encuadrada = null }
    // Cambiar de vista suelta la materia abierta: la ficha es de la vista en la que se pidió
    LaunchedEffect(vista) { seleccionado = null }
    LaunchedEffect(seleccionado) { if (seleccionado == null) encuadrada = null }
    LaunchedEffect(porEncuadrar, vista, estado) {
        val codigos = porEncuadrar ?: return@LaunchedEffect
        if (vista != Vista.MAPA || estado == null) return@LaunchedEffect
        // Espera a que la hoja se haya ido y el mapa esté a la vista
        delay(Movimiento.SALIDA_HOJA + 120L)
        estado.cajaDe(codigos)?.let { camara.mostrar(alcance, it, MARGEN_AL_APROBAR) }
        porEncuadrar = null
    }

    fun abrirCasilla(casilla: String, aprobar: Boolean) {
        aprobarAlElegir = aprobar
        casillaAbierta = casilla
    }

    // Acostado, una casilla de electiva vacía no cabe en la burbuja: abre el selector a pantalla completa
    LaunchedEffect(seleccionado, horizontal, vista) {
        val c = seleccionado ?: return@LaunchedEffect
        if (horizontal && vista == Vista.MAPA && estado != null && estado.layout.porCodigo[c]?.asignatura?.esHueco == true && estado.enCasilla(c) == null) {
            seleccionado = null
            abrirCasilla(c, aprobar = false)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Paleta.lienzo)
            .drawBehind {
                val r = size.width * 0.9f
                val c = Offset(size.width * 0.15f, -size.width * 0.1f)
                drawCircle(Brush.radialGradient(0f to color.copy(alpha = 0.14f), 0.7f to Color.Transparent, center = c, radius = r), r, c)
            },
    ) {
        // En horizontal la barra pasa a riel y la lista se parte en dos
        val seguro = WindowInsets.safeDrawing.asPaddingValues()
        val izquierda = seguro.calculateLeftPadding(LayoutDirection.Ltr)
        val derecha = seguro.calculateRightPadding(LayoutDirection.Ltr)
        // En horizontal la lista y el horario empiezan donde la cámara del mapa, tras el riel; el mapa pasa por detrás
        val inicio = if (horizontal) izquierda + RESERVA_RIEL else 0.dp
        val tapa = if (horizontal) abajo + Espacio.m else tapaBarra
        // Acostado, sobre el mapa la ficha es una burbuja pegada a su tarjeta; la lista lleva su propia página
        val burbuja = horizontal && vista == Vista.MAPA
        CapaVista(
            vista = vista,
            estado = estado,
            horario = horario,
            camara = camara,
            efectos = efectos,
            cristal = cristal,
            horizontal = horizontal,
            izquierda = izquierda,
            inicio = inicio,
            tapa = tapa,
            derecha = derecha,
            lista = lista,
            seleccionado = seleccionado,
            hojaAbierta = hojaAbierta,
            alSeleccionar = { seleccionado = it },
            alAbrirCasilla = ::abrirCasilla,
            alVerEnMapa = { codigo ->
                vista = Vista.MAPA
                porEncuadrar = listOf(codigo)
            },
        )

        NavegacionCarrera(
            horizontal = horizontal,
            vista = vista,
            estado = estado,
            cristal = cristal,
            color = color,
            izquierda = izquierda,
            abajo = abajo,
            alVolver = alVolver,
            alCambiar = { vista = it },
            alAvance = { if (estado != null) avanceAbierto = true },
            // La página de la materia de la lista ocupa el sitio de la barra, igual que la ficha del mapa
            oculta = (seleccionado != null && vista != Vista.HORARIO && !horizontal) || paginaDeLista,
        )

        if (estado != null) {
            val alMarcar: (String, Marca?) -> Unit = { codigo, marca ->
                // Aprobar enseña lo que abre: el mapa se corre hasta que se vea todo, y entonces descarga.
                // En la lista no hay cámara que mover: solo se marca
                if (vista == Vista.MAPA && marca == Marca.APROBADA && estado.marcas[codigo] != Marca.APROBADA) {
                    val abre = estado.layout.relaciones.adelante[codigo].orEmpty()
                    estado.cajaDe(abre + codigo)?.let { camara.mostrar(alcance, it, MARGEN_AL_APROBAR) }
                    seleccionado = null
                    estado.marcar(codigo, marca)
                    efectos.descargar(codigo)
                } else {
                    estado.marcar(codigo, marca)
                }
                efectos.tocar(codigo)
            }
            val alCambiarElectiva: (String) -> Unit = { casilla ->
                seleccionado = null
                abrirCasilla(casilla, aprobar = false)
            }
            // Encima de la barra: la ficha esconde la barra y ocupa su sitio. Acostada en el mapa, la burbuja
            BurbujaFicha(
                codigo = seleccionado.takeIf { burbuja },
                estado = estado,
                camara = camara,
                zona = ZonaSegura(
                    izq = inicio.value + 16f, arriba = arriba.value + 12f,
                    der = maxWidth.value - derecha.value - 16f, abajo = maxHeight.value - abajo.value - 20f,
                ),
                ancho = maxWidth.value,
                alto = maxHeight.value,
                alCerrar = { seleccionado = null },
                alMarcar = alMarcar,
                alIrA = { seleccionado = it },
                alCambiarElectiva = alCambiarElectiva,
            )
            FichaMateria(
                codigo = seleccionado.takeIf { vista != Vista.HORARIO && !burbuja },
                estado = estado,
                alCerrar = { seleccionado = null },
                alMarcar = alMarcar,
                alIrA = { seleccionado = it },
                alCambiarElectiva = alCambiarElectiva,
                alEncuadrar = { codigo, alto ->
                    // Solo de pie: acostada la ficha ocupa la pantalla (o es la burbuja) y no hay hueco donde centrar nada
                    if (vista == Vista.MAPA && !horizontal && codigo == seleccionado && codigo != encuadrada) {
                        estado.cajaDe(listOf(codigo))?.let {
                            // Centrada en el hueco libre: 16 bajo la barra de estado y 16 sobre la hoja
                            camara.mostrar(alcance, it, Margenes(arriba = arriba.value + 16f, abajo = alto.value + 16f, izq = 20f, der = 20f), centrar = true)
                            encuadrada = codigo
                        }
                    }
                },
            )
        }

        HojasCarrera(
            estado = estado,
            horario = horario,
            cristal = cristal,
            efectos = efectos,
            casillaAbierta = casillaAbierta,
            aprobarAlElegir = aprobarAlElegir,
            avanceAbierto = avanceAbierto,
            alCerrarCasilla = { casillaAbierta = null },
            alCerrarAvance = { avanceAbierto = false },
            alVerEnMapa = { codigos ->
                avanceAbierto = false
                vista = Vista.MAPA
                porEncuadrar = codigos
            },
        )
    }
}

/**
 * La vista que se ve (mapa, lista u horario), con el fundido entre ellas. Es
 * lo que desenfoca la barra, por eso lleva el `hazeSource`.
 */
@Composable
private fun CapaVista(
    vista: Vista,
    estado: EstadoCarrera?,
    horario: EstadoHorario?,
    camara: Camara,
    efectos: EfectosMapa,
    cristal: HazeState,
    horizontal: Boolean,
    izquierda: Dp,
    inicio: Dp,
    tapa: Dp,
    /** El recorte seguro de la derecha, para el panel de la lista acostada */
    derecha: Dp,
    /** La lista: qué materia está elegida y si su página está empujada; vive aquí para sobrevivir al giro */
    lista: NavegacionLista,
    seleccionado: String?,
    /** Si hay una hoja abierta encima: el mapa se congela y la lista se da por tapada */
    hojaAbierta: Boolean,
    alSeleccionar: (String?) -> Unit,
    alAbrirCasilla: (String, Boolean) -> Unit,
    alVerEnMapa: (String) -> Unit,
) {
AnimatedContent(
    targetState = vista,
    modifier = Modifier.fillMaxSize().fuenteCristal(cristal),
    // Cambio de vista en serie (100 sale, 200 entra): nunca dos vistas a medias a la vez
    transitionSpec = {
        if (Movimiento.reducido) fadeIn(tween(Movimiento.CORTA)) togetherWith fadeOut(tween(Movimiento.CORTA))
        else fadeIn(tween(Movimiento.CAMBIO_VISTA - Movimiento.SALIDA_RAPIDA, Movimiento.SALIDA_RAPIDA, Movimiento.ENTRA)) togetherWith fadeOut(tween(Movimiento.SALIDA_RAPIDA, easing = Movimiento.SALE))
    },
    label = "vista",
) { v ->
    // Mientras el pensum carga no se pinta nada
    if (estado == null) return@AnimatedContent
    when (v) {
        Vista.MAPA -> MapaCarrera(
            estado = estado,
            camara = camara,
            efectos = efectos,
            reservaIzquierda = if (horizontal) (izquierda + RESERVA_RIEL).value else null,
            seleccionado = seleccionado,
            congelado = hojaAbierta,
            alTocarMateria = { codigo ->
                efectos.ruta = null
                alSeleccionar(if (seleccionado == codigo) null else codigo)
            },
            alTocarVacio = {
                alSeleccionar(null)
                efectos.ruta = null
            },
            alAbrirCasilla = { alAbrirCasilla(it, false) },
            alAlternarSemestre = { s ->
                when (val accion = accionDeSemestre(estado.layout.nodos.map { it.asignatura }, s, estado::enCasilla, estado.estados)) {
                    is AccionSemestre.Aprobar -> estado.marcarVarias(accion.codigos.associateWith { Marca.APROBADA })
                    is AccionSemestre.Desmarcar -> estado.marcarVarias(accion.codigos.associateWith { null })
                    is AccionSemestre.Elegir -> alAbrirCasilla(accion.casilla, true)
                    null -> Unit
                }
            },
        )
        Vista.LISTA -> {
            // La página empujada es de la lista: al irse ella (cuando acaba su fundido), la página se cierra
            DisposableEffect(Unit) { onDispose { lista.abierta = false } }
            // El Box suelta el mínimo que fija AnimatedContent: sin él el ancho de la lista se ignora
            Box(Modifier.fillMaxSize().padding(start = inicio)) {
                VistaLista(
                    estado = estado,
                    navegacion = lista,
                    abajo = tapa,
                    acostada = horizontal,
                    derecha = derecha,
                    alElegirElectiva = alAbrirCasilla,
                    alVerEnMapa = alVerEnMapa,
                )
            }
        }
        Vista.HORARIO -> if (horario != null) VistaHorario(horario, abajo = tapa, modifier = Modifier.padding(start = inicio), horizontal = horizontal)
    }
}
}

/** Lo que navega: el riel lateral con su velo en horizontal, o la barra de abajo en vertical */
@Composable
private fun BoxScope.NavegacionCarrera(
    horizontal: Boolean,
    vista: Vista,
    estado: EstadoCarrera?,
    cristal: HazeState,
    color: Color,
    izquierda: Dp,
    abajo: Dp,
    alVolver: () -> Unit,
    alCambiar: (Vista) -> Unit,
    alAvance: () -> Unit,
    /** Con la ficha abierta la barra baja y se funde (solo `graphicsLayer`), y vuelve al cerrarla */
    oculta: Boolean,
) {
    val fuera by animateFloatAsState(
        if (oculta) 1f else 0f,
        // Se va con la ficha y vuelve cuando la ficha ya casi salió (la hoja tarda más en irse que la barra en volver)
        if (oculta) tween(Movimiento.CORTA, easing = Movimiento.SALE) else tween(Movimiento.CORTA, delayMillis = 120, easing = Movimiento.ENTRA),
        label = "barra",
    )
    if (horizontal) Box(
        Modifier.align(Alignment.CenterStart).fillMaxHeight().width(ANCHO_VELO).background(
            Brush.horizontalGradient(0.4f to Paleta.lienzo.copy(alpha = 0.92f), 1f to Color.Transparent),
        ),
    )

    BarraCarrera(
        vista = vista,
        progreso = estado?.progreso,
        cristal = cristal,
        alInicio = alVolver,
        alCambiar = alCambiar,
        alAvance = alAvance,
        color = color,
        modifier = (if (horizontal) Modifier.align(Alignment.CenterStart).padding(start = izquierda + SEPARA_RIEL)
        else Modifier.align(Alignment.BottomCenter).padding(bottom = abajo + SOBRE_EL_BORDE))
            .graphicsLayer {
                alpha = 1f - fuera
                if (!horizontal) translationY = 72.dp.toPx() * fuera
            },
        vertical = horizontal,
    )
}

/** Las hojas que suben por encima de todo: electivas, avance y horario. Difuminan lo mismo que recoge la barra: el contenido de la vista */
@Composable
private fun BoxScope.HojasCarrera(
    estado: EstadoCarrera?,
    horario: EstadoHorario?,
    cristal: HazeState,
    efectos: EfectosMapa,
    casillaAbierta: String?,
    aprobarAlElegir: Boolean,
    avanceAbierto: Boolean,
    alCerrarCasilla: () -> Unit,
    alCerrarAvance: () -> Unit,
    alVerEnMapa: (List<String>) -> Unit,
) {
    if (estado == null) return
    CompositionLocalProvider(LocalFondoHoja provides cristal) {
        SelectorElectiva(
            casilla = casillaAbierta,
            estado = estado,
            aprobarAlElegir = aprobarAlElegir,
            alColocar = { casilla, electiva ->
                estado.colocar(casilla, electiva)
                if (aprobarAlElegir && electiva != null) {
                    estado.marcar(electiva, Marca.APROBADA)
                    efectos.descargar(electiva)
                    efectos.tocar(electiva)
                }
                alCerrarCasilla()
            },
            alCerrar = alCerrarCasilla,
        )
        HojaAvance(abierta = avanceAbierto, estado = estado, alCerrar = alCerrarAvance, alVerEnMapa = alVerEnMapa)
        if (horario != null) HojasHorario(horario, cristal)
    }
}
