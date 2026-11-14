package mpensum.carrera

import mpensum.componentes.HojaInferior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.componentes.margenPantalla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mpensum.componentes.ventana
import mpensum.logica.Marca
import mpensum.logica.Situacion
import mpensum.tema.Espacio

/*
 * La ficha de una materia en el mapa (docs/ficha-y-lista.md §1, «Siguiente
 * paso»): una `HojaInferior` sin velo, pegada abajo y a todo el ancho, con tope
 * en el 60 % de la pantalla. Las piezas viven en PiezasFicha.kt; aquí solo se
 * eligen y se ordenan. Acostada, la hoja ocupa la pantalla y las mismas piezas
 * van en dos columnas (después se decide entre burbuja y pantalla completa).
 */

/** Hasta dónde crece la hoja de pie: el resto se desplaza dentro */
private const val TOPE_FICHA = 0.6f

/** El asa de la hoja sin velo: 8 arriba, 4 de alto y 8 abajo */
private val ASA_FICHA = 20.dp

@Composable
fun BoxScope.FichaMateria(
    codigo: String?,
    estado: EstadoCarrera,
    alCerrar: () -> Unit,
    alMarcar: (String, Marca?) -> Unit,
    alIrA: (String) -> Unit,
    alCambiarElectiva: (String) -> Unit,
    /** Una vez por materia, con el alto de la hoja ya medido: de ahí sale dónde queda la tarjeta en el mapa */
    alEncuadrar: (String, Dp) -> Unit,
) {
    val acostado = ventana().apaisado
    val densidad = LocalDensity.current
    val topePx = with(densidad) { (ventana().alto * TOPE_FICHA).toPx() }
    val medidas = remember { MedidasFicha() }
    // Lo que la hoja suma a sus dos contenidos: el asa (8 + 4 + 8) y la barra de gestos de abajo
    val restoPx = with(densidad) { (ASA_FICHA + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).roundToPx() }
    // Lo que se enseña va un paso por detrás de lo que se pide: al cambiar de materia con la hoja arriba se espera
    // (150 ms como mucho) a que su respuesta esté calculada, y así su alto se anima una sola vez, hacia el final
    var actual by remember { mutableStateOf(codigo) }
    // Al abrirse (no al cambiar de materia con la hoja arriba) no hay nada que esperar: la hoja sube ya con la
    // nueva, sin que la anterior asome mientras entra. Un reloj nuevo por apertura, que no anima de una a otra
    var apertura by remember { mutableIntStateOf(0) }
    var estabaAbierta by remember { mutableStateOf(codigo != null) }
    if ((codigo != null) != estabaAbierta) {
        estabaAbierta = codigo != null
        if (codigo != null) {
            apertura++
            medidas.olvidar(codigo)
            actual = codigo
        }
    }
    val despachador = LocalCalculo.current
    LaunchedEffect(codigo) {
        // Cerrada no hay nada que enseñar; recién abierta ya se puso arriba, en composición
        if (codigo == null || actual == codigo) return@LaunchedEffect
        val situacion = estado.situaciones[codigo]
        if (actual != null && actual != codigo && situacion != null && situacion != Situacion.HECHA) {
            withTimeoutOrNull(150) { estado.respuestas.calcular(codigo, situacion, despachador) }
        }
        medidas.olvidar(codigo)
        actual = codigo
    }
    // Una vez por materia, con el alto al que va la hoja (no al que tiene ahora): la cámara y la hoja se mueven hacia el mismo sitio
    LaunchedEffect(codigo, actual) {
        if (codigo == null || actual != codigo) return@LaunchedEffect
        withFrameNanos { }
        val destino = snapshotFlow { medidas.destino(codigo, topePx, restoPx, acostado) }.first { it != null }!!
        alEncuadrar(codigo, with(densidad) { destino.toDp() })
    }
    val vista = actual ?: return
    val transicion = key(apertura) { updateTransition(vista, label = "materia") }

    HojaInferior(
        abierta = codigo != null,
        alCerrar = alCerrar,
        modal = false,
        tope = TOPE_FICHA,
        alMedir = { medidas.hoja = with(densidad) { it.roundToPx() } },
        cabecera = {
            transicion.FundidoDeMateria(alMedir = { c, h -> medidas.cabeceras[c] = h }) { c ->
                encabezadoDe(c, estado)?.let { CabeceraFicha(it, alCerrar, alCambiarElectiva) }
            }
        },
    ) {
        transicion.FundidoDeMateria(alMedir = { c, h -> medidas.cuerpos[c] = h }) { c ->
            val enc = encabezadoDe(c, estado)
            val vacia = if (enc?.estado == EstadoFicha.POR_ELEGIR) casillaElegida(estado, c) else null
            when {
                enc == null -> Unit
                vacia != null -> ContenidoElectiva(
                    elegida = vacia, estado = estado, aprobarAlElegir = false, conBarraDeCuota = false,
                    alColocar = { casilla, electiva -> estado.colocar(casilla, electiva); alCerrar() },
                    alCerrar = alCerrar,
                )
                acostado -> CuerpoAcostado(c, estado, alMarcar, alIrA)
                else -> CuerpoDePie(c, estado, alMarcar, alIrA)
            }
        }
    }
}

/**
 * Lo medido de la hoja de la ficha, en px. El alto de destino de un cambio de
 * materia es lo que miden la cabecera y el cuerpo de la materia nueva más lo
 * que la hoja pone aparte (`resto`), sin pasar del tope de la hoja.
 */
private class MedidasFicha {
    var hoja by mutableIntStateOf(0)
    val cabeceras = mutableStateMapOf<String, Int>()
    val cuerpos = mutableStateMapOf<String, Int>()
    fun olvidar(codigo: String) {
        cabeceras.remove(codigo)
        cuerpos.remove(codigo)
    }

    /** Null mientras falte alguna medida */
    fun destino(codigo: String, tope: Float, resto: Int, acostado: Boolean): Int? {
        if (hoja <= 0) return null
        // Acostada la hoja ocupa la pantalla y no se anima su alto: vale el que tiene
        if (acostado) return hoja
        val cabecera = cabeceras[codigo] ?: return null
        val cuerpo = cuerpos[codigo] ?: return null
        return minOf(cabecera + cuerpo + resto, tope.toInt())
    }
}

/** De pie, en orden de lectura: lo que necesita antes, lo que abre y los botones */
@Composable
private fun CuerpoDePie(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, alIrA: (String) -> Unit) {
    Column(Modifier.padding(horizontal = margenPantalla()).padding(bottom = Espacio.m)) {
        PideAbre(codigo, estado, alIrA, Modifier.padding(top = Espacio.s))
        BotonesMarca(codigo, estado, alMarcar, Modifier.padding(top = Espacio.s))
    }
}

/** Acostada, dos columnas: a la izquierda lo que se marca; a la derecha, lo que necesita antes y lo que abre */
@Composable
private fun CuerpoAcostado(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, alIrA: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = margenPantalla()).padding(top = Espacio.s, bottom = Espacio.m),
        horizontalArrangement = Arrangement.spacedBy(Espacio.l),
        verticalAlignment = Alignment.Top,
    ) {
        BotonesMarca(codigo, estado, alMarcar, Modifier.weight(1f))
        PideAbre(codigo, estado, alIrA, Modifier.weight(1f))
    }
}
