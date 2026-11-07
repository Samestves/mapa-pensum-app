package mpensum.carrera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import mpensum.componentes.ventana
import mpensum.logica.Marca
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
    // La última que se enseñó sigue pintándose mientras la hoja baja
    var ultima by remember { mutableStateOf(codigo) }
    if (codigo != null) ultima = codigo
    val actual = ultima ?: return
    var alto by remember { mutableStateOf(0.dp) }
    // Espera un cuadro: con otra materia el alto viejo aún vale hasta que se mide el nuevo
    LaunchedEffect(codigo) {
        if (codigo == null) return@LaunchedEffect
        withFrameNanos { }
        alEncuadrar(codigo, snapshotFlow { alto }.first { it > 0.dp })
    }
    val acostado = ventana().apaisado

    HojaInferior(
        abierta = codigo != null,
        alCerrar = alCerrar,
        modal = false,
        tope = TOPE_FICHA,
        alMedir = { alto = it },
        cabecera = {
            FundidoDeMateria(actual) { c ->
                encabezadoDe(c, estado)?.let { CabeceraFicha(it, alCerrar, alCambiarElectiva) }
            }
        },
    ) {
        FundidoDeMateria(actual) { c ->
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

/** De pie, en orden de lectura: la respuesta, lo que pide y abre, y los botones */
@Composable
private fun CuerpoDePie(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, alIrA: (String) -> Unit) {
    Column(Modifier.padding(horizontal = margenPantalla()).padding(bottom = Espacio.m)) {
        RespuestaFicha(codigo, estado, Modifier.padding(top = Espacio.m))
        PideAbre(codigo, estado, alIrA, Modifier.padding(top = Espacio.s))
        BotonesMarca(codigo, estado, alMarcar, Modifier.padding(top = Espacio.m))
    }
}

/** Acostada, dos columnas: a la izquierda lo que responde y lo que se marca; a la derecha, lo que pide y lo que abre */
@Composable
private fun CuerpoAcostado(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, alIrA: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = margenPantalla()).padding(top = Espacio.s, bottom = Espacio.m),
        horizontalArrangement = Arrangement.spacedBy(Espacio.l),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            RespuestaFicha(codigo, estado)
            BotonesMarca(codigo, estado, alMarcar)
        }
        PideAbre(codigo, estado, alIrA, Modifier.weight(1f))
    }
}
