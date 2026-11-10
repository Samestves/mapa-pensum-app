package mpensum.portada

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.empujeDePagina
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.esApaisado
import mpensum.componentes.margenPantalla
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.tema.Espacio
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.inter

/*
 * Personalizar (docs/personalizar-v2.md, opción C «Primero la cara»): lo que
 * cambia la cara está a cero toques en la página y lo demás, a uno.
 *
 *   Personalizar   Tema (dos pantallas y la paleta) · Automático · Acento · tres filas
 *   Mapa           Fondo · Cables · Grosor · Atenuar lo aprobado · Códigos de materia
 *   Inicio y horario   Al abrir · Vista inicial · Hora de 24 h
 *   Accesibilidad  Texto · Más contraste · Luz de fondo · Reducir movimiento · Vibración · Modo ligero
 *
 * Todo se guarda en el acto (Preferencias) y se ve en el acto. Las pantallas y
 * la paleta son Canvas sin animación: solo se redibujan al cambiar algo.
 */

/** Las páginas que cuelgan de Personalizar */
internal val PaginaAjustes.esDePersonalizar: Boolean
    get() = this == PaginaAjustes.PERSONALIZAR || this == PaginaAjustes.MAPA || this == PaginaAjustes.INICIO || this == PaginaAjustes.ACCESIBILIDAD

/**
 * Qué ranura de Automático se está editando (la de noche o la de día) y si ya
 * se tocó la otra. Vive en la hoja de Ajustes para que sobreviva a entrar y
 * salir de las subpáginas.
 */
internal class Ranuras(noche: Boolean) {
    var noche by mutableStateOf(noche)
    var diaTocado = false
}

/** Lo que dice la fila de Ajustes: «Oscuro · acento tu carrera» */
@Composable
internal fun resumenPersonalizar(): String {
    val tema by Preferencias.tema.collectAsState()
    val acento by Preferencias.acento.collectAsState()
    return "${if (tema == Apariencia.SISTEMA) "Automático" else tema.nombre} · acento ${acento?.nombre?.lowercase() ?: "tu carrera"}"
}

/**
 * Personalizar y sus tres páginas. De pie, `pagina` dice cuál se pinta. Acostado
 * el índice de la izquierda (tema) no se mueve y a la derecha se sustituye la
 * columna por la página elegida: maestro y detalle.
 */
@Composable
internal fun Personalizar(pagina: PaginaAjustes, propia: Carrera?, ranuras: Ranuras, alAbrir: (PaginaAjustes) -> Unit) {
    val lado = margenPantalla()
    if (esApaisado()) {
        Row(Modifier.fillMaxSize().padding(horizontal = lado).padding(top = Espacio.xxs), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Column(Modifier.width(330.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = Espacio.m)) {
                BloqueTema(propia, ranuras, apaisado = true, lado = 0.dp)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Paleta.borde))
            val desplazoPx = with(LocalDensity.current) { Movimiento.DESPLAZO.roundToPx() }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    pagina,
                    transitionSpec = { empujeDePagina(targetState.ordinal > initialState.ordinal, desplazoPx) },
                    contentAlignment = Alignment.TopStart,
                ) { p ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Espacio.xxl)) {
                        ContenidoDePagina(p, propia, ranuras, 0.dp, alAbrir)
                    }
                }
                // El desvanecido de 40 dp: lo que se desplaza se va por debajo sin cortarse en seco
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Paleta.panel))))
            }
        }
    } else {
        Column(Modifier.padding(top = Espacio.xxs, bottom = Espacio.xl)) {
            ContenidoDePagina(pagina, propia, ranuras, lado, alAbrir)
        }
    }
}

/** Lo que lleva cada página; la raíz, de pie, abre con el tema */
@Composable
private fun ContenidoDePagina(pagina: PaginaAjustes, propia: Carrera?, ranuras: Ranuras, lado: Dp, alAbrir: (PaginaAjustes) -> Unit) {
    when (pagina) {
        PaginaAjustes.MAPA -> PaginaMapa(propia, lado)
        PaginaAjustes.INICIO -> PaginaInicio(propia, lado)
        PaginaAjustes.ACCESIBILIDAD -> PaginaAccesibilidad(lado)
        else -> {
            if (!esApaisado()) BloqueTema(propia, ranuras, apaisado = false, lado = lado)
            BloqueAutomatico(ranuras, lado, primero = esApaisado())
            BloqueAcento(propia, lado)
            BloqueGrupos(propia, lado, alAbrir)
        }
    }
}

/** Mapa, Inicio y horario, y Accesibilidad: una tarjeta de tres filas de 64 */
@Composable
private fun BloqueGrupos(propia: Carrera?, lado: Dp, alAbrir: (PaginaAjustes) -> Unit) {
    Tarjeta(lado, Modifier.padding(top = Espacio.xl)) {
        FilaGrupo(Lucide.mapa, "Mapa", resumenMapa()) { alAbrir(PaginaAjustes.MAPA) }
        Divisor(60.dp)
        FilaGrupo(Lucide.casa, "Inicio y horario", resumenInicio(propia)) { alAbrir(PaginaAjustes.INICIO) }
        Divisor(60.dp)
        FilaGrupo(Lucide.persona, "Accesibilidad y movimiento", resumenAccesibilidad()) { alAbrir(PaginaAjustes.ACCESIBILIDAD) }
    }
}

/** Una fila que abre su página: pastilla de 32 con el icono, el título, su resumen y el chevrón */
@Composable
private fun FilaGrupo(icono: List<Path>, titulo: String, resumen: String, alPulsar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pulsable(alPulsar, escala = 0.98f)
            .padding(horizontal = Espacio.m, vertical = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Paleta.tinta.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            IconoTrazo(icono, Paleta.suave, 18.dp, 1.75f)
        }
        Column(Modifier.weight(1f).padding(horizontal = Espacio.s)) {
            BasicText(titulo, style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = Tipo.l, lineHeight = 20.sp, color = Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Nota(resumen, Modifier.padding(top = Espacio.hilo), maxLines = 1)
        }
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

// Las tres páginas
