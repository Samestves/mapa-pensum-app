package mpensum.portada

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.navegacion.Vista
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.datos.Carrera
import mpensum.plataforma.Preferencias
import mpensum.tema.Espacio

/* La página «Inicio» de Personalizar */

/** La nota de la fila Inicio y horario: «Portada · 12 h» o «Sistemas · 24 h» */
@Composable
internal fun resumenInicio(propia: Carrera?): String {
    val abrir by Preferencias.abrirEnCarrera.collectAsState()
    val h24 by Preferencias.hora24.collectAsState()
    return "${if (abrir && propia != null) propia.nombreCorto else "Portada"} · ${if (h24) "24 h" else "12 h"}"
}

@Composable
internal fun PaginaInicio(propia: Carrera?, lado: Dp) {
    val abrir by Preferencias.abrirEnCarrera.collectAsState()
    val vista by Preferencias.vistaInicial.collectAsState()
    val h24 by Preferencias.hora24.collectAsState()
    var primero = true
    // Sin carrera no hay a dónde entrar directo: el bloque no aparece
    if (propia != null) {
        primero = false
        Rotulo("AL ABRIR", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xxs))
        Segmentado(Modifier.padding(horizontal = lado)) {
            OpcionTexto("Portada", null, !abrir, Modifier.weight(1f)) { Preferencias.guardarAbrirEnCarrera(false) }
            OpcionTexto(propia.nombreCorto, null, abrir, Modifier.weight(1f)) { Preferencias.guardarAbrirEnCarrera(true) }
        }
    }
    Rotulo("VISTA INICIAL", null, Modifier.padding(horizontal = lado).padding(top = if (primero) Espacio.xxs else Espacio.xl))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Mapa", null, vista != Vista.LISTA, Modifier.weight(1f), delante = { IconoTrazo(Lucide.mapa3, it, 18.dp, 1.75f, Modifier.padding(end = Espacio.xs)) }) { Preferencias.guardarVistaInicial(Vista.MAPA) }
        OpcionTexto("Lista", null, vista == Vista.LISTA, Modifier.weight(1f), delante = { IconoTrazo(Lucide.lista, it, 18.dp, 1.75f, Modifier.padding(end = Espacio.xs)) }) { Preferencias.guardarVistaInicial(Vista.LISTA) }
    }
    Rotulo("HORARIO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Tarjeta(lado) {
        // La nota enseña el formato elegido
        FilaInterruptor("Hora de 24 h", h24, nota = if (h24) "13:00" else "1:00 p. m.", alCambiar = Preferencias::guardarHora24)
    }
}
