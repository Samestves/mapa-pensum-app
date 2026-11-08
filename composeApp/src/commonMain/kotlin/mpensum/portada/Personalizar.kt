package mpensum.portada

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.carrera.Vista
import mpensum.carrera.empujeDePagina
import mpensum.carrera.versal
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.PaletaTemas
import mpensum.componentes.PantallaTema
import mpensum.componentes.PantallasTema
import mpensum.componentes.VistaPreviaMapa
import mpensum.componentes.esApaisado
import mpensum.componentes.iconoCarrera
import mpensum.componentes.margenPantalla
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.plataforma.Apariencia
import mpensum.plataforma.Preferencias
import mpensum.plataforma.animacionesQuitadas
import mpensum.plataforma.telefonoModesto
import mpensum.tema.Acento
import mpensum.tema.Espacio
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Juego
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.pareja

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

/** La nota de la fila Mapa: «Rejilla · cables curvos» */
@Composable
private fun resumenMapa(): String {
    val fondo = when (Tema.fondo) { Fondo.REJILLA -> "Rejilla"; Fondo.PUNTOS -> "Puntos"; Fondo.LISO -> "Liso" }
    val cables = when (Tema.cables) { FormaCables.CURVOS -> "curvos"; FormaCables.RECTOS -> "rectos"; FormaCables.AL_TOCAR -> "al tocar" }
    return "$fondo · cables $cables"
}

/** La nota de la fila Inicio y horario: «Portada · 12 h» o «Sistemas · 24 h» */
@Composable
private fun resumenInicio(propia: Carrera?): String {
    val abrir by Preferencias.abrirEnCarrera.collectAsState()
    val h24 by Preferencias.hora24.collectAsState()
    return "${if (abrir && propia != null) propia.nombreCorto else "Portada"} · ${if (h24) "24 h" else "12 h"}"
}

/** La nota de la fila Accesibilidad: «Texto normal · ligero automático», más «· más contraste» si lo hay */
@Composable
private fun resumenAccesibilidad(): String {
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    val modo = when (ligero) { null -> "automático"; true -> "sí"; false -> "no" }
    return "Texto ${if (grande) "grande" else "normal"} · ligero $modo${if (contraste) " · más contraste" else ""}"
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

/** El nombre del juego que se está viendo en la paleta: el de la ranura activa con Automático */
@Composable
private fun juegoElegido(ranuras: Ranuras): Juego {
    val tema by Preferencias.tema.collectAsState()
    val dia by Preferencias.temaDia.collectAsState()
    val noche by Preferencias.temaNoche.collectAsState()
    return tema.juego ?: if (ranuras.noche) noche else dia
}

/** Elegir en la paleta: el tema, o el de la ranura activa si hay Automático */
private fun elegirJuego(juego: Juego, ranuras: Ranuras) {
    if (Preferencias.tema.value == Apariencia.SISTEMA) {
        if (ranuras.noche) {
            Preferencias.guardarTemaNoche(juego)
            // Elegir «De noche» propone su pareja para «De día», si ese no se ha tocado
            if (!ranuras.diaTocado && !juego.claro) Preferencias.guardarTemaDia(juego.pareja)
        } else {
            Preferencias.guardarTemaDia(juego)
            ranuras.diaTocado = true
        }
    } else Preferencias.guardarTema(aparienciaDe(juego))
}

private fun aparienciaDe(juego: Juego): Apariencia = Apariencia.entries.first { it.juego == juego }

/**
 * Las dos pantallas, el nombre del tema y la paleta. Acostado es el índice de
 * la izquierda: el rótulo TEMA con el nombre a la derecha, pantallas de 154 × 168
 * y toques de 50.
 */
@Composable
private fun BloqueTema(propia: Carrera?, ranuras: Ranuras, apaisado: Boolean, lado: Dp) {
    val tema by Preferencias.tema.collectAsState()
    val dia by Preferencias.temaDia.collectAsState()
    val noche by Preferencias.temaNoche.collectAsState()
    val automatico = tema == Apariencia.SISTEMA
    val elegido = juegoElegido(ranuras)
    val actual = tema.juego ?: Tema.juego
    val izquierda = if (automatico) PantallaTema(dia, false, "De día", false) else PantallaTema(actual, false, "Portada", null)
    val derecha = if (automatico) PantallaTema(noche, false, "De noche", true) else PantallaTema(actual, true, "Mapa", null)
    if (apaisado) Rotulo("TEMA", elegido.nombre, Modifier.padding(top = Espacio.xs))
    PantallasTema(
        izquierda, derecha, propia?.color, propia?.silueta,
        ranuraActiva = if (automatico) (if (ranuras.noche) 1 else 0) else null,
        modifier = Modifier.padding(horizontal = lado).padding(top = if (apaisado) 0.dp else Espacio.xs),
        anchoMaximo = if (apaisado) 154.dp else 160.dp,
        altoMaximo = if (apaisado) 168.dp else 208.dp,
        hueco = if (apaisado) 14.dp else 15.dp,
        alElegirRanura = { ranuras.noche = it == 1 },
    )
    if (!apaisado) {
        BasicText(
            elegido.nombre,
            Modifier.fillMaxWidth().padding(top = Espacio.s, bottom = Espacio.xs),
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, textAlign = TextAlign.Center, color = Paleta.tinta),
        )
    }
    PaletaTemas(
        elegido = elegido,
        otro = if (automatico) (if (ranuras.noche) dia else noche) else null,
        otroEsNoche = !ranuras.noche,
        modifier = Modifier.padding(horizontal = lado).padding(top = if (apaisado) Espacio.xs else 0.dp),
        alElegir = { elegirJuego(it, ranuras) },
    )
}

/** Sistema: un solo interruptor. Encendido, las dos pantallas pasan a ser las ranuras de día y de noche */
@Composable
private fun BloqueAutomatico(ranuras: Ranuras, lado: Dp, primero: Boolean) {
    val tema by Preferencias.tema.collectAsState()
    val automatico = tema == Apariencia.SISTEMA
    Tarjeta(lado, Modifier.padding(top = if (primero) Espacio.xs else Espacio.m)) {
        FilaInterruptor(
            "Automático", automatico,
            nota = if (automatico) "Claro de día · Oscuro de noche" else null,
            alCambiar = { encendido ->
                // Apagado guarda el juego que se está viendo, para que la pantalla no salte
                Preferencias.guardarTema(if (encendido) Apariencia.SISTEMA else aparienciaDe(Tema.juego))
            },
        )
    }
}

@Composable
private fun BloqueAcento(propia: Carrera?, lado: Dp) {
    val acento by Preferencias.acento.collectAsState()
    Rotulo("ACENTO", acento?.nombre ?: "Tu carrera", Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Row(Modifier.fillMaxWidth().padding(horizontal = if (lado == 0.dp) 0.dp else Espacio.xs).semantics { selectableGroup() }, horizontalArrangement = Arrangement.SpaceBetween) {
        CirculoAcento(null, acento == null, propia)
        Acento.entries.forEach { CirculoAcento(it, acento == it, propia) }
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

@Composable
private fun PaginaMapa(propia: Carrera?, lado: Dp) {
    val fondo = Tema.fondo
    val cables by Preferencias.cables.collectAsState()
    val grosor by Preferencias.grosorCables.collectAsState()
    val atenuar by Preferencias.atenuarAprobadas.collectAsState()
    val codigos by Preferencias.codigos.collectAsState()
    VistaPreviaMapa(propia?.color, Modifier.padding(horizontal = lado).padding(top = Espacio.xxs))
    Rotulo("FONDO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Baldosas(lado) {
        Baldosa("Rejilla", fondo == Fondo.REJILLA, { Preferencias.guardarFondoMapa(Fondo.REJILLA) }) { dibujoFondo(Fondo.REJILLA) }
        Baldosa("Puntos", fondo == Fondo.PUNTOS, { Preferencias.guardarFondoMapa(Fondo.PUNTOS) }) { dibujoFondo(Fondo.PUNTOS) }
        Baldosa("Liso", fondo == Fondo.LISO, { Preferencias.guardarFondoMapa(Fondo.LISO) }) { dibujoFondo(Fondo.LISO) }
    }
    Rotulo("CABLES", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Baldosas(lado) {
        Baldosa("Curvos", cables == FormaCables.CURVOS, { Preferencias.guardarCables(FormaCables.CURVOS) }) { dibujoCables(FormaCables.CURVOS) }
        Baldosa("Rectos", cables == FormaCables.RECTOS, { Preferencias.guardarCables(FormaCables.RECTOS) }) { dibujoCables(FormaCables.RECTOS) }
        Baldosa("Al tocar", cables == FormaCables.AL_TOCAR, { Preferencias.guardarCables(FormaCables.AL_TOCAR) }) { dibujoCables(FormaCables.AL_TOCAR) }
    }
    Rotulo("GROSOR", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Segmentado(Modifier.padding(horizontal = lado)) {
        Grosor.entries.forEachIndexed { i, g ->
            val nombre = when (g) { Grosor.FINO -> "Fino"; Grosor.NORMAL -> "Normal"; Grosor.GRUESO -> "Grueso" }
            OpcionTexto(nombre, null, grosor == g, Modifier.weight(1f), delante = { colorTexto ->
                // La línea que dibuja el grosor: 18 dp de largo y 1, 2 o 3 de alto
                Box(Modifier.padding(end = Espacio.xs).size(18.dp, (i + 1).dp).background(colorTexto, CircleShape))
            }) { Preferencias.guardarGrosorCables(g) }
        }
    }
    Tarjeta(lado, Modifier.padding(top = Espacio.xl)) {
        FilaInterruptor("Atenuar lo aprobado", atenuar, alCambiar = Preferencias::guardarAtenuarAprobadas)
        Divisor(Espacio.m)
        FilaInterruptor("Códigos de materia", codigos, alCambiar = Preferencias::guardarCodigos)
    }
}

@Composable
private fun PaginaInicio(propia: Carrera?, lado: Dp) {
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

@Composable
private fun PaginaAccesibilidad(lado: Dp) {
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val luz by Preferencias.luzFondo.collectAsState()
    val reducir by Preferencias.reducirMovimiento.collectAsState()
    val vibracion by Preferencias.vibracion.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    val exterior = Tema.juego.exterior
    Rotulo("LECTURA", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xxs))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Normal", 14, !grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(false) }
        OpcionTexto("Grande", 18, grande, Modifier.weight(1f)) { Preferencias.guardarTextoGrande(true) }
    }
    Tarjeta(lado, Modifier.padding(top = Espacio.m)) {
        // Un Exterior ya lo trae: se ve encendido y no se puede apagar
        FilaInterruptor(
            "Más contraste", contraste || exterior, habilitado = !exterior,
            nota = if (exterior) "Exterior ya lo trae" else null,
            alCambiar = Preferencias::guardarContraste,
        )
    }
    Rotulo("MOVIMIENTO Y TACTO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Tarjeta(lado) {
        FilaInterruptor("Luz de fondo", luz ?: (Tema.juego.aurora > 0f), alCambiar = Preferencias::guardarLuzFondo)
        Divisor(Espacio.m)
        FilaInterruptor("Reducir movimiento", reducir ?: animacionesQuitadas(), alCambiar = Preferencias::guardarReducirMovimiento)
        Divisor(Espacio.m)
        FilaInterruptor("Vibración", vibracion, alCambiar = Preferencias::guardarVibracion)
    }
    Rotulo("MODO LIGERO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Automático", null, ligero == null, Modifier.weight(1.4f)) { Preferencias.guardarModoLigero(null) }
        OpcionTexto("Sí", null, ligero == true, Modifier.weight(1f)) { Preferencias.guardarModoLigero(true) }
        OpcionTexto("No", null, ligero == false, Modifier.weight(1f)) { Preferencias.guardarModoLigero(false) }
    }
    val explica = when (ligero) {
        null -> if (telefonoModesto()) "Activo: sin desenfoque" else "Apagado"
        true -> "Activo: sin desenfoque"
        false -> "Todo el cristal y las luces"
    }
    Nota(explica, Modifier.padding(horizontal = lado).padding(top = Espacio.xs), Paleta.tenue)
}

/** El rótulo de un bloque en versales y, a la derecha, lo elegido: 8 dp debajo */
@Composable
private fun Rotulo(texto: String, valor: String?, modifier: Modifier) {
    Row(modifier.fillMaxWidth().padding(bottom = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        BasicText(texto, Modifier.weight(1f), style = versal(Paleta.tenue))
        if (valor != null) BasicText(valor, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, color = Paleta.suave))
    }
}

/** La nota de una opción: Inter 13/18 en `tenue` */
@Composable
private fun Nota(texto: String, modifier: Modifier = Modifier, color: Color = Paleta.tenue, maxLines: Int = Int.MAX_VALUE) {
    BasicText(texto, modifier, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = color), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/**
 * Un acento: un círculo de 24 en un área táctil de 44; el elegido lleva un
 * aro de 36. `null` es «tu carrera»: el icono de la carrera en tinta neutra,
 * en un disco de 28 al 12 %.
 */
@Composable
private fun CirculoAcento(acento: Acento?, elegido: Boolean, propia: Carrera?) {
    val nombre = acento?.nombre ?: "Tu carrera"
    Box(
        Modifier
            .size(44.dp)
            .selectable(elegido, role = Role.RadioButton) { Preferencias.guardarAcento(acento) }
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .then(if (elegido) Modifier.border(2.dp, Paleta.tinta, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (acento == null) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    IconoTrazo(iconoCarrera(propia?.slug), Paleta.tinta, 14.dp, 1.75f)
                }
            } else {
                Box(Modifier.size(24.dp).clip(CircleShape).background(acento.color))
            }
        }
    }
}

/** Las baldosas de una opción, repartidas a lo ancho con 8 dp entre ellas */
@Composable
private fun Baldosas(lado: Dp, contenido: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = lado).semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(Espacio.xs), content = contenido)
}

/**
 * Una baldosa de 76 de alto, radio 16, con su dibujo de 64×30 y su nombre; la
 * elegida lleva aro de 2 dp y el nombre en tinta.
 */
@Composable
private fun RowScope.Baldosa(nombre: String, elegida: Boolean, alPulsar: () -> Unit, dibujo: DrawScope.() -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .weight(1f)
            .height(76.dp)
            .clip(forma)
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .border(if (elegida) 2.dp else 1.dp, if (elegida) Paleta.tinta else Paleta.tinta.copy(alpha = 0.065f), forma)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar)
            .semantics { contentDescription = nombre },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(64.dp, 30.dp)) { dibujo() }
        BasicText(nombre, Modifier.padding(top = Espacio.xs), style = TextStyle(fontFamily = inter(), fontSize = Tipo.xs, lineHeight = 16.sp, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = if (elegida) Paleta.tinta else Paleta.tenue))
    }
}

// Los dibujos de las baldosas: 64×30 dp, con la Paleta del tema que se ve

private fun DrawScope.dibujoFondo(fondo: Fondo) {
    val dp = density
    val linea = Paleta.tenue.copy(alpha = 0.5f)
    when (fondo) {
        Fondo.REJILLA -> {
            for (i in 1..3) drawLine(linea, Offset(i * 16 * dp, 0f), Offset(i * 16 * dp, size.height), dp)
            for (j in 1..1) drawLine(linea, Offset(0f, j * 15 * dp), Offset(size.width, j * 15 * dp), dp)
            drawLine(linea, Offset(0f, 0f), Offset(size.width, 0f), dp)
        }
        Fondo.PUNTOS -> {
            val puntos = ArrayList<Offset>()
            for (i in 0..3) for (j in 0..1) puntos.add(Offset(8 * dp + i * 16 * dp, 7.5f * dp + j * 15 * dp))
            drawPoints(puntos, PointMode.Points, Paleta.tenue, 3f * dp, StrokeCap.Round)
        }
        Fondo.LISO -> drawRoundRect(Paleta.tenue.copy(alpha = 0.18f), cornerRadius = CornerRadius(6 * dp))
    }
}

private fun DrawScope.dibujoCables(forma: FormaCables) {
    val dp = density
    val a = Offset(8 * dp, 22 * dp)
    val b = Offset(56 * dp, 8 * dp)
    val trazo = Stroke(2 * dp, cap = StrokeCap.Round)
    val ruta = Path().apply {
        moveTo(a.x, a.y)
        when (forma) {
            FormaCables.RECTOS -> { lineTo(32 * dp, a.y); lineTo(32 * dp, b.y); lineTo(b.x, b.y) }
            else -> cubicTo(32 * dp, a.y, 32 * dp, b.y, b.x, b.y)
        }
    }
    drawPath(ruta, Paleta.suave.copy(alpha = if (forma == FormaCables.AL_TOCAR) 0.3f else 0.9f), style = trazo)
    drawCircle(Paleta.suave, 3.5f * dp, a)
    drawCircle(if (forma == FormaCables.AL_TOCAR) Paleta.tinta else Paleta.suave, 3.5f * dp, b)
}

/** Una tarjeta de filas: velo de tinta, filo de 1 y radio 24 */
@Composable
private fun Tarjeta(lado: Dp, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier
            .padding(horizontal = lado)
            .fillMaxWidth()
            .drawBehind {
                val r = Radio.Grande.toPx()
                val g = Tema.grosorBorde.dp.toPx()
                drawRoundRect(Paleta.tinta.copy(alpha = 0.035f), cornerRadius = CornerRadius(r))
                drawRoundRect(Paleta.tinta.copy(alpha = 0.065f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r - g / 2), style = Stroke(g))
            },
    ) { contenido() }
}

/** La cápsula de un segmentado: las opciones en fila sobre un fondo de tinta muy floja */
@Composable
private fun Segmentado(modifier: Modifier, opciones: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Paleta.tinta.copy(alpha = 0.05f))
            .border(1.dp, Paleta.tinta.copy(alpha = 0.065f), RoundedCornerShape(24.dp))
            .padding(Espacio.xxs)
            .semantics { selectableGroup() },
        content = opciones,
    )
}

/**
 * Una opción del segmentado. La elegida pasa a tinta al 11 % con un filo de
 * tinta al 12 % y texto 500 (en oscuro, el `panel` con sombra quedaba más
 * oscuro que la pista). Con `tamAa`, la muestra «Aa» delante a ese tamaño; con
 * `delante`, lo que se dibuje, que recibe el color del texto.
 */
@Composable
private fun OpcionTexto(nombre: String, tamAa: Int?, elegida: Boolean, modifier: Modifier, delante: (@Composable (Color) -> Unit)? = null, alPulsar: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(forma)
            .then(if (elegida) Modifier.background(Paleta.tinta.copy(alpha = 0.11f)).border(1.dp, Paleta.tinta.copy(alpha = 0.12f), forma) else Modifier)
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (elegida) Paleta.tinta else Paleta.tenue
        if (tamAa != null) BasicText("Aa", Modifier.padding(end = Espacio.xs), style = TextStyle(fontFamily = jost(), fontSize = tamAa.sp, color = color))
        delante?.invoke(color)
        BasicText(nombre, style = TextStyle(fontFamily = inter(), fontSize = Tipo.l, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** El separador de una tarjeta, que arranca en `desde` (a la altura del texto si hay una pastilla) */
@Composable
private fun Divisor(desde: Dp) {
    Box(Modifier.padding(start = desde).fillMaxWidth().height(1.dp).background(Paleta.borde))
}

/**
 * Una fila con su interruptor, de 64 de alto: el título en Inter 500 (15/20,
 * como las demás opciones) y, si hace falta, una nota debajo. Deshabilitada
 * se atenúa y no responde.
 */
@Composable
private fun FilaInterruptor(titulo: String, activo: Boolean, habilitado: Boolean = true, nota: String? = null, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(activo, enabled = habilitado, role = Role.Switch, onValueChange = alCambiar)
            .padding(start = Espacio.m, end = Espacio.m, top = Espacio.xs, bottom = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Espacio.s)) {
            BasicText(titulo, style = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = Tipo.l, lineHeight = 20.sp, color = Paleta.tinta))
            if (nota != null) BasicText(nota, Modifier.padding(top = Espacio.hilo), style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.tenue))
        }
        Interruptor(activo, habilitado)
    }
}

/** El interruptor de iOS, 51×31: la pista en tinta y el pomo en el panel (nada de verde); apagado, pista de borde y pomo blanco */
@Composable
private fun Interruptor(activo: Boolean, habilitado: Boolean) {
    val x by animateDpAsState(if (activo) 22.dp else 2.dp, tween(if (Tema.movimiento) 0 else 200), label = "pomo")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background((if (activo) Paleta.tinta else Paleta.borde).let { if (habilitado) it else it.copy(alpha = 0.55f) }),
    ) {
        Box(Modifier.offset(x, 2.dp).size(27.dp).shadow(2.dp, CircleShape).background(if (activo) Paleta.panel else Color.White, CircleShape))
    }
}
