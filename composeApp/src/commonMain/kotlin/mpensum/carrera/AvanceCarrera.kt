package mpensum.carrera

import mpensum.componentes.IndiceRetenido
import mpensum.componentes.empujeDePagina
import mpensum.componentes.CabeceraDeAltoEstable
import mpensum.componentes.HojaInferior
import mpensum.componentes.BotonMapa
import mpensum.componentes.CabeceraInterna
import mpensum.componentes.trazos
import mpensum.componentes.estiloRotulo
import mpensum.componentes.BotonRedondo
import mpensum.componentes.IconoFila
import mpensum.componentes.estiloTitulo
import mpensum.componentes.DatosCabecera
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.componentes.esApaisado
import mpensum.componentes.fundidoAbajo
import mpensum.componentes.margenPantalla
import mpensum.componentes.rememberPartesListas
import mpensum.componentes.ventana
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Periodo
import mpensum.mapa.tituloGrupo
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.tema.Tipo

/*
 * La hoja del progreso (docs/maquetas/progreso-refinado.html):
 *
 *   1. La tarjeta de arriba: el porcentaje con lo aprobado debajo, la fecha en
 *      la esquina, la barra de cápsulas con su regla de semestres y una frase
 *      calculada con lo que te da aprobar lo que cursas.
 *   2. Tres preguntas y la fecha en filas agrupadas: este semestre, el
 *      próximo, lo que no puedes atrasar y cuándo te gradúas.
 *   3. Las electivas con cuota, una fila por grupo.
 *   4. Reiniciar, callado.
 *
 * De pie, cada fila abre su pantalla interna dentro de la misma hoja: entra
 * deslizándose desde la derecha y el atrás del sistema vuelve a la hoja.
 * Acostada, la hoja ocupa la pantalla y es índice y detalle (AvanceIndice.kt).
 */

/** Lo que hay dentro de cada pantalla interna, de pie o en el panel del detalle acostado */
@Composable
internal fun ContenidoDetalle(d: DetalleAvance, estado: EstadoCarrera, hoy: Periodo, alVerEnMapa: (List<String>) -> Unit, alAbrirCasilla: (String) -> Unit) {
    when (d) {
        DetalleAvance.EsteSemestre -> DetalleEsteSemestre(estado, hoy)
        DetalleAvance.Proximo -> DetalleProximo(estado, hoy)
        DetalleAvance.Cadena -> DetalleCadena(estado, hoy)
        DetalleAvance.Graduacion -> DetalleGraduacion(estado, hoy)
        is DetalleAvance.Electivas -> DetalleElectivas(estado, d.clave, alAbrirCasilla)
    }
}

/** Las pantallas internas a las que lleva cada fila de la hoja */
internal sealed interface DetalleAvance {
    data object EsteSemestre : DetalleAvance
    data object Proximo : DetalleAvance
    data object Cadena : DetalleAvance
    data object Graduacion : DetalleAvance
    data class Electivas(val clave: String) : DetalleAvance
}

/** La clave con que cada página guarda su estado (su desplazamiento) mientras no está a la vista */
internal fun DetalleAvance?.clave(): String = when (this) {
    null -> "indice"
    DetalleAvance.EsteSemestre -> "este-semestre"
    DetalleAvance.Proximo -> "proximo"
    DetalleAvance.Cadena -> "cadena"
    DetalleAvance.Graduacion -> "graduacion"
    is DetalleAvance.Electivas -> "electivas-$clave"
}

/** La pastilla, el título y el subtítulo de cada pantalla interna */
internal fun cabeceraDe(d: DetalleAvance, estado: EstadoCarrera, g: EstimacionGraduacion, hoy: Periodo): DatosCabecera = when (d) {
    DetalleAvance.EsteSemestre -> DatosCabecera(IconoFila.LIBRO_ABIERTO.trazos(), "Este semestre")
    DetalleAvance.Proximo -> DatosCabecera(IconoFila.CALENDARIO.trazos(), hoy.mas(1).rango(), azul = true)
    DetalleAvance.Cadena -> DatosCabecera(IconoFila.ESLABON.trazos(), "No puedes atrasar")
    DetalleAvance.Graduacion -> DatosCabecera(IconoFila.BIRRETE.trazos(), "¿Cuándo me gradúo?")
    is DetalleAvance.Electivas -> {
        val grupo = estado.pensum.grupos.firstOrNull { it.clave == d.clave }
        DatosCabecera((grupo?.let(::iconoDeGrupo) ?: IconoFila.LIBRO_ABIERTO).trazos(), grupo?.let(::tituloGrupo) ?: "Electivas")
    }
}

/** Lo que se enciende en el mapa desde una pantalla interna: las que te conviene inscribir o tu cadena; las demás no llevan mapa */
internal fun codigosDelMapa(d: DetalleAvance, g: EstimacionGraduacion): List<String>? = when (d) {
    DetalleAvance.Proximo -> g.plan.firstOrNull()?.takeIf { it.isNotEmpty() }
    DetalleAvance.Cadena -> g.cuelloDeBotella.takeIf { it.isNotEmpty() }
    else -> null
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
    remember(apertura) { estado.actualizarHoy() }
    val hoy = estado.hoy
    // Las páginas de pie van con un solo reloj, nuevo en cada apertura: no arrastra la página donde se cerró
    val transicion = key(apertura) { updateTransition(detalle, label = "progreso") }

    // Una sola hoja de pie y acostada: girar no la vuelve a subir ni rehace su atrás, que sigue por debajo del
    // del selector. Solo cambia lo de dentro; lo elegido es el mismo detalle, así girar no pierde dónde estabas
    HojaInferior(
        abierta = abierta,
        alCerrar = alCerrar,
        desplazable = false,
        altoFijo = true,
        // Acostada, cada columna lleva su cabecera (AvanceIndice.kt)
        cabecera = { if (!apaisado) CabeceraDelProgreso(transicion, estado, alCambiar = { detalle = it }, alCerrar, alVerEnMapa) },
    ) {
        if (apaisado) {
            IndiceYDetalle(estado, hoy, detalle, alElegir = { detalle = it }, alCerrar = alCerrar, alVerEnMapa = alVerEnMapa, alAbrirCasilla = { casilla = it })
        } else {
            PaginasDelProgreso(transicion, estado, hoy, alCambiar = { detalle = it }, alVerEnMapa = alVerEnMapa, alAbrirCasilla = { casilla = it })
        }
    }
    // De pie, atrás vuelve del detalle a la hoja; ya en la hoja, la cierra (HojaInferior). Va entre la hoja y el
    // selector, siempre compuesto: así se atiende antes que la hoja y después que el selector
    ManejarAtras(abierta && !apaisado && detalle != null) { detalle = null }
    // Montado después de la hoja, el selector queda encima y su atrás se atiende primero
    SelectorElectiva(
        casilla = casilla,
        estado = estado,
        aprobarAlElegir = false,
        sobreHoja = true,
        alColocar = { c, electiva ->
            estado.colocar(c, electiva)
            casilla = null
        },
        alCerrar = { casilla = null },
    )
}

/** La cabecera de pie: en el índice, «Tu avance» con el aspa; en una pantalla interna, volver y «Ver en el mapa» */
@Composable
private fun CabeceraDelProgreso(
    transicion: Transition<DetalleAvance?>,
    estado: EstadoCarrera,
    alCambiar: (DetalleAvance?) -> Unit,
    alCerrar: () -> Unit,
    alVerEnMapa: (List<String>) -> Unit,
) {
    val desplazoPx = with(LocalDensity.current) { Movimiento.DESPLAZO.roundToPx() }
    CabeceraDeAltoEstable {
        transicion.AnimatedContent(transitionSpec = { empujeDePagina(targetState != null, desplazoPx) }, contentAlignment = Alignment.TopStart) { d ->
            if (d == null) {
                CabeceraHoja(estado.pensum.nombre, alCerrar)
            } else {
                Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonRedondo(Lucide.atras, "Volver") { alCambiar(null) }
                    Spacer(Modifier.weight(1f))
                    codigosDelMapa(d, estado.graduacion)?.let { codigos -> BotonMapa { alVerEnMapa(codigos) } }
                }
            }
        }
    }
}

/**
 * El cuerpo de pie: dos páginas (el índice y cada pantalla interna) con el
 * mismo reloj que la cabecera (patrón A, docs/qa/transiciones.md). Cada
 * página lleva su desplazamiento, que se guarda al salir: al volver, el índice
 * está donde lo dejaste.
 */
@Composable
private fun PaginasDelProgreso(
    transicion: Transition<DetalleAvance?>,
    estado: EstadoCarrera,
    hoy: Periodo,
    alCambiar: (DetalleAvance?) -> Unit,
    alVerEnMapa: (List<String>) -> Unit,
    alAbrirCasilla: (String) -> Unit,
) {
    val desplazoPx = with(LocalDensity.current) { Movimiento.DESPLAZO.roundToPx() }
    val empuje: AnimatedContentTransitionScope<DetalleAvance?>.() -> ContentTransform = { empujeDePagina(targetState != null, desplazoPx) }
    Box(Modifier.fillMaxSize()) {
        // El índice no se descompone al entrar a una pantalla: al volver ya está, con su desplazamiento
        transicion.IndiceRetenido(desplazoPx) {
            val desplazo = rememberScrollState()
            Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo)) {
                ContenidoHoja(estado) { alCambiar(it) }
            }
        }
        val guardado = rememberSaveableStateHolder()
        transicion.AnimatedContent(Modifier.fillMaxSize(), transitionSpec = empuje, contentAlignment = Alignment.TopStart) { d ->
            if (d == null) return@AnimatedContent
            guardado.SaveableStateProvider(d.clave()) {
                val desplazo = rememberScrollState()
                Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo)) {
                    CabeceraInterna(cabeceraDe(d, estado, estado.graduacion, hoy), Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.m))
                    ContenidoDetalle(d, estado, hoy, alVerEnMapa, alAbrirCasilla)
                }
            }
        }
    }
}

/** «TU AVANCE», el nombre de la carrera en Jost Light 28 y el botón de cerrar */
@Composable
private fun CabeceraHoja(nombre: String, alCerrar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = Espacio.s)) {
            BasicText("TU AVANCE", style = estiloRotulo())
            // En ventana compacta el título se encoge antes que saltar de línea
            if (ventana().compacta) {
                BasicText(nombre, Modifier.padding(top = Espacio.xxs), style = estiloTitulo(), maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = Tipo.xl, maxFontSize = 28.sp))
            } else {
                BasicText(nombre, Modifier.padding(top = Espacio.xxs), style = estiloTitulo())
            }
        }
        BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
    }
}

/**
 * El cuerpo de una pantalla interna: la respuesta `arriba` y debajo las dos
 * partes, en una columna. De pie la desplaza la hoja y empieza 24 dp bajo la
 * cabecera; acostado va en el panel del detalle, que ya trae sus márgenes, y
 * se desplaza por su cuenta bajo su cabecera fija. Con `dosColumnas`, si el
 * panel pasa de 500 dp, las dos partes van lado a lado bajo la respuesta.
 *
 * Se compone por partes, una por cuadro: la pantalla entra 100 ms después de
 * salir la anterior (`empujeDePagina`) y en ese hueco ya está entera, sin que
 * un solo cuadro cargue con todas sus filas.
 */
@Composable
internal fun CuerpoDetalle(
    arriba: (@Composable ColumnScope.() -> Unit)? = null,
    dosColumnas: Boolean = false,
    izquierda: @Composable ColumnScope.() -> Unit,
    derecha: @Composable ColumnScope.() -> Unit,
) {
    // La respuesta (si hay) entra con el toque; cada una de las dos partes, un cuadro después
    val primera = if (arriba != null) 1 else 0
    val partes = rememberPartesListas(primera + 2)
    val conIzquierda = partes > primera
    val conDerecha = partes > primera + 1
    if (!esApaisado()) {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xl, bottom = Espacio.xxl)) {
            arriba?.invoke(this)
            if (conIzquierda) izquierda()
            if (conDerecha) derecha()
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val lado = dosColumnas && maxWidth >= ANCHO_DOS_COLUMNAS
        val desplazo = rememberScrollState()
        Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo).padding(top = ARRIBA_CUERPO_PANEL, bottom = Espacio.xl)) {
            arriba?.invoke(this)
            if (lado) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Column(Modifier.weight(1f)) { if (conIzquierda) izquierda() }
                    Column(Modifier.weight(1f)) { if (conDerecha) derecha() }
                }
            } else {
                if (conIzquierda) izquierda()
                if (conDerecha) derecha()
            }
        }
    }
}
