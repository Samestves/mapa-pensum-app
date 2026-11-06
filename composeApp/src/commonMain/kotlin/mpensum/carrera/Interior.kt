package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import mpensum.componentes.esApaisado
import mpensum.componentes.IconoDoble
import mpensum.componentes.Phosphor
import mpensum.datos.Carrera
import mpensum.datos.rememberPensum
import mpensum.logica.AccionSemestre
import mpensum.logica.Marca
import mpensum.logica.accionDeSemestre
import mpensum.mapa.Camara
import mpensum.mapa.EfectosMapa
import kotlinx.coroutines.delay
import mpensum.mapa.MARGEN_AL_APROBAR
import mpensum.mapa.MapaCarrera
import mpensum.mapa.Margenes
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Paleta
import mpensum.tema.colorDe

/** Las tres formas de mirar la misma carrera, en el orden de data/vistas.js */
enum class Vista(val etiqueta: String, val icono: IconoDoble) {
    MAPA("Mapa", Phosphor.mapa),
    LISTA("Lista", Phosphor.lista),
    HORARIO("Horario", Phosphor.horario),
}

/** Cuánto se separa la barra del borde de abajo */
private val SOBRE_EL_BORDE = 14.dp

/** En horizontal, el riel va a esto del recorte de cámara y mide 52 dp de ancho */
private val SEPARA_RIEL = 14.dp
private val ANCHO_RIEL = 52.dp

/** En horizontal, lo que tapa el riel a la izquierda (14 + 52 + 18): el mapa corre por detrás y la cámara lo descuenta */
private val RESERVA_RIEL = SEPARA_RIEL + ANCHO_RIEL + 18.dp

/** El velo tras el riel: los cables se funden hacia el borde en vez de cortarse contra el cristal */
private val ANCHO_VELO = 150.dp

/**
 * Dentro de una carrera, como el teléfono de la web: nada arriba, la vista a
 * pantalla completa y, abajo, la barra de cristal con inicio, las tres vistas
 * y el avance. La ficha de la materia flota encima de la barra (en el mapa y
 * en la lista); las hojas (electivas, avance, horario) suben por encima de todo.
 */
@Composable
fun Interior(carrera: Carrera, alVolver: () -> Unit) {
    var vista by rememberSaveable { mutableStateOf(Vista.MAPA) }
    val pensum = rememberPensum(carrera.slug)
    // Se sabe antes de medir porque de ello depende la geometría del mapa
    val horizontal = esApaisado()
    val estado = remember(pensum, horizontal) { pensum?.let { EstadoCarrera(it, compacto = horizontal) } }
    // El horario vive aquí y no en su vista: sus hojas suben por encima de la barra
    val horario = estado?.let { rememberEstadoHorario(it) }
    val color = colorDe(carrera.color)
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Lo que tapa la barra, medido desde el borde: las vistas con lista dejan ese hueco al final
    val tapaBarra = abajo + SOBRE_EL_BORDE + ALTO_BARRA
    val arriba = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    // Lo que pasa por debajo de la barra se ve a través de ella, desenfocado
    val cristal = remember { HazeState() }

    val camara = remember(carrera.slug, horizontal) { Camara(carrera.slug) }
    val efectos = remember(carrera.slug) { EfectosMapa() }
    val alcance = rememberCoroutineScope()
    var seleccionado by remember(carrera.slug) { mutableStateOf<String?>(null) }
    var casillaAbierta by remember(carrera.slug) { mutableStateOf<String?>(null) }
    var aprobarAlElegir by remember { mutableStateOf(false) }
    var avanceAbierto by remember { mutableStateOf(false) }
    // Las materias que pidió «Verlas en el mapa» la hoja del avance: se encuadran cuando el mapa ya se ve
    var porEncuadrar by remember { mutableStateOf<List<String>?>(null) }
    // La materia a la que ya se le hizo sitio: la ficha cambia de alto al animarse y no debe empujar el mapa cada vez
    var encuadrada by remember { mutableStateOf<String?>(null) }

    // Atrás suelta primero la materia elegida, luego la ruta fijada; después sale de la carrera
    ManejarAtras(efectos.ruta != null) { efectos.ruta = null }
    ManejarAtras(seleccionado != null) { seleccionado = null }
    // Cambiar de vista suelta la materia abierta: la ficha es de la vista en la que se pidió
    LaunchedEffect(vista) { seleccionado = null }
    LaunchedEffect(seleccionado) { if (seleccionado == null) encuadrada = null }
    LaunchedEffect(porEncuadrar, vista, estado) {
        val codigos = porEncuadrar ?: return@LaunchedEffect
        if (vista != Vista.MAPA || estado == null) return@LaunchedEffect
        // Espera a que la hoja se haya ido y el mapa esté a la vista
        delay(380)
        estado.cajaDe(codigos)?.let { camara.mostrar(alcance, it, MARGEN_AL_APROBAR) }
        porEncuadrar = null
    }

    fun abrirCasilla(casilla: String, aprobar: Boolean) {
        aprobarAlElegir = aprobar
        casillaAbierta = casilla
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
        val tapa = if (horizontal) abajo + 16.dp else tapaBarra
        val enLista = horizontal && vista == Vista.LISTA
        AnimatedContent(
            targetState = vista,
            modifier = Modifier.fillMaxSize().hazeSource(cristal),
            transitionSpec = { fadeIn(tween(260, 80)) togetherWith fadeOut(tween(160)) },
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
                    congelado = casillaAbierta != null || avanceAbierto,
                    alTocarMateria = { codigo ->
                        efectos.ruta = null
                        seleccionado = if (seleccionado == codigo) null else codigo
                    },
                    alTocarVacio = {
                        seleccionado = null
                        efectos.ruta = null
                    },
                    alAbrirCasilla = { abrirCasilla(it, aprobar = false) },
                    alAlternarSemestre = { s ->
                        when (val accion = accionDeSemestre(estado.layout.nodos.map { it.asignatura }, s, estado::enCasilla, estado.estados)) {
                            is AccionSemestre.Aprobar -> estado.marcarVarias(accion.codigos.associateWith { Marca.APROBADA })
                            is AccionSemestre.Desmarcar -> estado.marcarVarias(accion.codigos.associateWith { null })
                            is AccionSemestre.Elegir -> abrirCasilla(accion.casilla, aprobar = true)
                            null -> Unit
                        }
                    },
                )
                // El Box suelta el mínimo que fija AnimatedContent: sin él el ancho de la lista se ignora
                Vista.LISTA -> Box(Modifier.fillMaxSize().padding(start = inicio)) { VistaLista(
                    estado = estado,
                    abajo = tapa,
                    modifier = if (enLista) Modifier.width(376.dp) else Modifier,
                    tapada = seleccionado != null || casillaAbierta != null || avanceAbierto,
                    alAbrirFicha = { seleccionado = it },
                    alElegirElectiva = ::abrirCasilla,
                ) }
                Vista.HORARIO -> if (horario != null) VistaHorario(horario, abajo = tapa, modifier = Modifier.padding(start = inicio), horizontal = horizontal)
            }
        }

        if (estado != null) {
            FichaMateria(
                codigo = seleccionado.takeIf { vista != Vista.HORARIO },
                estado = estado,
                reservaAbajo = if (horizontal) abajo + 16.dp else abajo + SOBRE_EL_BORDE + ALTO_BARRA + 10.dp,
                lateral = if (enLista) PanelLateral(inicio + 376.dp + 8.dp, derecha + 16.dp, arriba + 16.dp) else null,
                horizontal = horizontal,
                alCerrar = { seleccionado = null },
                alMarcar = { codigo, marca ->
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
                },
                alIrA = { seleccionado = it },
                alCambiarElectiva = { casilla ->
                    seleccionado = null
                    abrirCasilla(casilla, aprobar = false)
                },
                alTapar = { tapa ->
                    val codigo = seleccionado
                    if (vista == Vista.MAPA && codigo != null && codigo != encuadrada) {
                        estado.cajaDe(listOf(codigo))?.let {
                            camara.mostrar(alcance, it, Margenes(arriba = 24f + arriba.value, abajo = tapa + 16f, izq = 20f, der = 20f))
                            encuadrada = codigo
                        }
                    }
                },
            )
        }

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
            alCambiar = { vista = it },
            alAvance = { if (estado != null) avanceAbierto = true },
            color = color,
            modifier = if (horizontal) Modifier.align(Alignment.CenterStart).padding(start = izquierda + SEPARA_RIEL)
            else Modifier.align(Alignment.BottomCenter).padding(bottom = abajo + SOBRE_EL_BORDE),
            vertical = horizontal,
        )

        if (estado != null) {
            // Las hojas difuminan por encima de ellas lo mismo que recoge la barra: el contenido de la vista
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
                        casillaAbierta = null
                    },
                    alCerrar = { casillaAbierta = null },
                )
                HojaAvance(
                    abierta = avanceAbierto,
                    estado = estado,
                    alCerrar = { avanceAbierto = false },
                    alVerEnMapa = { codigos ->
                        avanceAbierto = false
                        vista = Vista.MAPA
                        porEncuadrar = codigos
                    },
                )
                if (horario != null) HojasHorario(horario, cristal)
            }
        }
    }
}
