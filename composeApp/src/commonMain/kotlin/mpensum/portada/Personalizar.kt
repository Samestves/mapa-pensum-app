package mpensum.portada

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.carrera.Vista
import mpensum.carrera.versal
import mpensum.componentes.CarruselTemas
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.VistaPrevia
import mpensum.componentes.esApaisado
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
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.Tipo
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * Personalizar: la subpantalla de Ajustes donde se elige cómo se ve la app.
 * Todo se guarda en el acto (Preferencias) y se ve en el acto, primero en la
 * vista previa fija de arriba. Los bloques, en este orden:
 *
 *   TEMA · ACENTO · MAPA · INICIO · HORARIO · LECTURA · MOVIMIENTO Y TACTO
 *
 * Lo que cambia la cara va primero; lo de accesibilidad y rendimiento, al final.
 */

/** El borde de los bloques: 20 dp, como el aire de la hoja */
private val LADO = Espacio.l

/** Lo que dice la fila de Ajustes: «Oscuro · tu carrera · texto normal» */
@Composable
internal fun resumenPersonalizar(): String {
    val tema by Preferencias.tema.collectAsState()
    val acento by Preferencias.acento.collectAsState()
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    return "${tema.nombre} · ${acento?.nombre?.lowercase() ?: "tu carrera"} · texto ${if (grande) "grande" else "normal"}${if (contraste) " · más contraste" else ""}"
}

/**
 * De pie, la vista previa va en la cabecera de la hoja (no se desplaza) y aquí
 * solo queda lo que se desplaza. Acostado no hay cabecera para tanto: el
 * índice de 330 lleva la vista previa y el carrusel, un hilo, y a la derecha
 * una columna que se desplaza.
 */
@Composable
internal fun Personalizar(colorCarrera: Color, silueta: List<Int>?) {
    if (esApaisado()) {
        Row(Modifier.fillMaxSize().padding(horizontal = LADO).padding(top = Espacio.xxs), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Column(Modifier.width(330.dp).fillMaxHeight()) {
                VistaPrevia(colorCarrera, silueta, 166.dp, Modifier.fillMaxWidth())
                BloqueTema(conDiaNoche = false, lado = 0.dp)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Paleta.borde))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = Espacio.xl)) {
                BloqueDiaNoche(0.dp)
                BloquesAjustes(colorCarrera, lado = 0.dp, primero = false)
            }
        }
    } else {
        Column(Modifier.padding(top = Espacio.xxs, bottom = Espacio.xl)) {
            BloqueTema(conDiaNoche = true, lado = LADO)
            BloquesAjustes(colorCarrera, lado = LADO, primero = true)
        }
    }
}

/** De ACENTO a MOVIMIENTO Y TACTO: lo mismo de pie y acostado */
@Composable
private fun BloquesAjustes(colorCarrera: Color, lado: Dp, primero: Boolean) {
    BloqueAcento(colorCarrera, lado, primero)
    BloqueMapa(lado)
    BloqueInicio(lado)
    BloqueHorario(lado)
    BloqueLectura(lado)
    BloqueMovimiento(lado)
}

@Composable
private fun BloqueTema(conDiaNoche: Boolean, lado: Dp) {
    val tema by Preferencias.tema.collectAsState()
    Rotulo("TEMA", tema.nombre, Modifier.padding(horizontal = lado).padding(top = Espacio.l))
    CarruselTemas(relleno = PaddingValues(horizontal = if (lado == 0.dp) 0.dp else 16.dp))
    if (conDiaNoche) BloqueDiaNoche(lado)
}

/** Con Sistema, cuál es el de día y cuál el de noche: divulgación progresiva, solo aparece al elegirlo */
@Composable
private fun BloqueDiaNoche(lado: Dp) {
    val tema by Preferencias.tema.collectAsState()
    if (tema != Apariencia.SISTEMA) return
    val dia by Preferencias.temaDia.collectAsState()
    val noche by Preferencias.temaNoche.collectAsState()
    Column(Modifier.padding(top = Espacio.m)) {
        FilaJuegos("De día", dia, lado, Preferencias::guardarTemaDia)
        FilaJuegos("De noche", noche, lado, Preferencias::guardarTemaNoche)
    }
}

/** Una fila de pastillas con los juegos para elegir uno; se desplaza, con 44 de alto */
@Composable
private fun FilaJuegos(titulo: String, actual: Juego, lado: Dp, alElegir: (Juego) -> Unit) {
    Nota(titulo, Modifier.padding(horizontal = lado, vertical = Espacio.xs))
    LazyRow(
        Modifier.semantics { selectableGroup() },
        contentPadding = PaddingValues(horizontal = if (lado == 0.dp) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        items(Juego.entries, key = { it.name }) { j ->
            val elegido = j == actual
            val forma = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .height(44.dp)
                    .clip(forma)
                    .background(Paleta.tinta.copy(alpha = 0.05f))
                    .border(if (elegido) 2.dp else 1.dp, if (elegido) Paleta.tinta else Paleta.tinta.copy(alpha = 0.065f), forma)
                    .selectable(elegido, role = Role.RadioButton) { alElegir(j) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(j.nombre, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, fontWeight = if (elegido) FontWeight.Medium else FontWeight.Normal, color = if (elegido) Paleta.tinta else Paleta.tenue))
            }
        }
    }
}

@Composable
private fun BloqueAcento(colorCarrera: Color, lado: Dp, primero: Boolean) {
    val acento by Preferencias.acento.collectAsState()
    Rotulo("ACENTO", acento?.nombre ?: "Tu carrera", Modifier.padding(horizontal = lado).padding(top = if (primero) Espacio.xl else 0.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = if (lado == 0.dp) 0.dp else Espacio.xs).semantics { selectableGroup() }, horizontalArrangement = Arrangement.SpaceBetween) {
        CirculoAcento(null, acento == null, colorCarrera)
        Acento.entries.forEach { CirculoAcento(it, acento == it, colorCarrera) }
    }
}

@Composable
private fun BloqueMapa(lado: Dp) {
    val fondo = Tema.fondo
    val cables by Preferencias.cables.collectAsState()
    val grosor by Preferencias.grosorCables.collectAsState()
    val atenuar by Preferencias.atenuarAprobadas.collectAsState()
    val codigos by Preferencias.codigos.collectAsState()
    Rotulo("MAPA", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Nota("Fondo", Modifier.padding(horizontal = lado).padding(bottom = Espacio.xs))
    Baldosas(lado) {
        Baldosa("Rejilla", fondo == Fondo.REJILLA, { Preferencias.guardarFondoMapa(Fondo.REJILLA) }) { dibujoFondo(Fondo.REJILLA) }
        Baldosa("Puntos", fondo == Fondo.PUNTOS, { Preferencias.guardarFondoMapa(Fondo.PUNTOS) }) { dibujoFondo(Fondo.PUNTOS) }
        Baldosa("Liso", fondo == Fondo.LISO, { Preferencias.guardarFondoMapa(Fondo.LISO) }) { dibujoFondo(Fondo.LISO) }
    }
    Nota("Cables", Modifier.padding(horizontal = lado).padding(top = Espacio.m, bottom = Espacio.xs))
    Baldosas(lado) {
        Baldosa("Curvos", cables == FormaCables.CURVOS, { Preferencias.guardarCables(FormaCables.CURVOS) }) { dibujoCables(FormaCables.CURVOS) }
        Baldosa("Rectos", cables == FormaCables.RECTOS, { Preferencias.guardarCables(FormaCables.RECTOS) }) { dibujoCables(FormaCables.RECTOS) }
        Baldosa("Al tocar", cables == FormaCables.AL_TOCAR, { Preferencias.guardarCables(FormaCables.AL_TOCAR) }) { dibujoCables(FormaCables.AL_TOCAR) }
    }
    Nota("Grosor de los cables", Modifier.padding(horizontal = lado).padding(top = Espacio.m, bottom = Espacio.xs))
    Baldosas(lado) {
        Baldosa("Fino", grosor == Grosor.FINO, { Preferencias.guardarGrosorCables(Grosor.FINO) }) { dibujoGrosor(Grosor.FINO) }
        Baldosa("Normal", grosor == Grosor.NORMAL, { Preferencias.guardarGrosorCables(Grosor.NORMAL) }) { dibujoGrosor(Grosor.NORMAL) }
        Baldosa("Grueso", grosor == Grosor.GRUESO, { Preferencias.guardarGrosorCables(Grosor.GRUESO) }) { dibujoGrosor(Grosor.GRUESO) }
    }
    Nota("Lo aprobado", Modifier.padding(horizontal = lado).padding(top = Espacio.m, bottom = Espacio.xs))
    Baldosas(lado) {
        Baldosa("Normal", !atenuar, { Preferencias.guardarAtenuarAprobadas(false) }) { dibujoAprobado(false) }
        Baldosa("Atenuado", atenuar, { Preferencias.guardarAtenuarAprobadas(true) }) { dibujoAprobado(true) }
    }
    Tarjeta(lado, Modifier.padding(top = Espacio.m)) {
        FilaInterruptor("Códigos en el mapa", codigos, alCambiar = Preferencias::guardarCodigos)
    }
}

@Composable
private fun BloqueInicio(lado: Dp) {
    val abrir by Preferencias.abrirEnCarrera.collectAsState()
    val vista by Preferencias.vistaInicial.collectAsState()
    Rotulo("INICIO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Nota("Al abrir la app", Modifier.padding(horizontal = lado).padding(bottom = Espacio.xs))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Portada", null, !abrir, Modifier.weight(1f)) { Preferencias.guardarAbrirEnCarrera(false) }
        OpcionTexto("Mi carrera", null, abrir, Modifier.weight(1f)) { Preferencias.guardarAbrirEnCarrera(true) }
    }
    Nota("Mi carrera se abre en", Modifier.padding(horizontal = lado).padding(top = Espacio.m, bottom = Espacio.xs))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("Mapa", null, vista != Vista.LISTA, Modifier.weight(1f)) { Preferencias.guardarVistaInicial(Vista.MAPA) }
        OpcionTexto("Lista", null, vista == Vista.LISTA, Modifier.weight(1f)) { Preferencias.guardarVistaInicial(Vista.LISTA) }
    }
}

@Composable
private fun BloqueHorario(lado: Dp) {
    val h24 by Preferencias.hora24.collectAsState()
    Rotulo("HORARIO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Nota("Formato de hora", Modifier.padding(horizontal = lado).padding(bottom = Espacio.xs))
    Segmentado(Modifier.padding(horizontal = lado)) {
        OpcionTexto("12 h", null, !h24, Modifier.weight(1f)) { Preferencias.guardarHora24(false) }
        OpcionTexto("24 h", null, h24, Modifier.weight(1f)) { Preferencias.guardarHora24(true) }
    }
}

@Composable
private fun BloqueLectura(lado: Dp) {
    val grande by Preferencias.textoGrande.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val exterior = Tema.juego.exterior
    Rotulo("LECTURA", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
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
}

@Composable
private fun BloqueMovimiento(lado: Dp) {
    val luz by Preferencias.luzFondo.collectAsState()
    val reducir by Preferencias.reducirMovimiento.collectAsState()
    val vibracion by Preferencias.vibracion.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    Rotulo("MOVIMIENTO Y TACTO", null, Modifier.padding(horizontal = lado).padding(top = Espacio.xl))
    Tarjeta(lado) {
        FilaInterruptor("Luz de fondo", luz ?: (Tema.juego.aurora > 0f), alCambiar = Preferencias::guardarLuzFondo)
        Divisor()
        FilaInterruptor("Reducir movimiento", reducir ?: animacionesQuitadas(), alCambiar = Preferencias::guardarReducirMovimiento)
        Divisor()
        FilaInterruptor("Vibración", vibracion, alCambiar = Preferencias::guardarVibracion)
    }
    Nota("Modo ligero", Modifier.padding(horizontal = lado).padding(top = Espacio.m, bottom = Espacio.xs))
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

/** La nota de una opción: Inter 13/16 en `tenue` */
@Composable
private fun Nota(texto: String, modifier: Modifier = Modifier, color: Color = Paleta.tenue) {
    BasicText(texto, modifier, style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = color))
}

/**
 * Un acento: un círculo de 24 en un área táctil de 44; el elegido lleva un
 * aro de 36. `null` es «tu carrera»: la universidad calada en un disco del
 * color de la carrera.
 */
@Composable
private fun CirculoAcento(acento: Acento?, elegido: Boolean, colorCarrera: Color) {
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
                Box(Modifier.size(24.dp).clip(CircleShape).background(colorCarrera), contentAlignment = Alignment.Center) {
                    IconoTrazo(Lucide.universidad, Paleta.panel, 14.dp, 2.4f)
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

private fun DrawScope.dibujoGrosor(grosor: Grosor) {
    val dp = density
    drawLine(Paleta.suave, Offset(4 * dp, size.height / 2), Offset(60 * dp, size.height / 2), 2.2f * dp * grosor.factor, StrokeCap.Round)
}

private fun DrawScope.dibujoAprobado(atenuado: Boolean) {
    val dp = density
    val alpha = if (atenuado) 0.45f else 1f
    for (i in 0..1) {
        val o = Offset(2 * dp + i * 32 * dp, 6 * dp)
        val u = Size(28 * dp, 18 * dp)
        drawRoundRect(Paleta.verde.copy(alpha = 0.2f * alpha), o, u, CornerRadius(5 * dp))
        drawRoundRect(Paleta.verde.copy(alpha = alpha), o, u, CornerRadius(5 * dp), style = Stroke(1.5f * dp))
    }
}

/** Una tarjeta de filas de interruptor: velo de tinta, filo de 1 y radio 16 */
@Composable
private fun Tarjeta(lado: Dp, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier
            .padding(horizontal = lado)
            .fillMaxWidth()
            .drawBehind {
                val r = 16.dp.toPx()
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

/** Una opción del segmentado; con `tamAa`, la muestra «Aa» delante a ese tamaño */
@Composable
private fun OpcionTexto(nombre: String, tamAa: Int?, elegida: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .then(if (elegida) Modifier.shadow(1.dp, RoundedCornerShape(20.dp)).background(Paleta.panel, RoundedCornerShape(20.dp)) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .selectable(elegida, role = Role.RadioButton, onClick = alPulsar),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (elegida) Paleta.tinta else Paleta.tenue
        if (tamAa != null) BasicText("Aa", Modifier.padding(end = Espacio.xs), style = TextStyle(fontFamily = jost(), fontSize = tamAa.sp, color = color))
        BasicText(nombre, style = TextStyle(fontFamily = inter(), fontSize = Tipo.l, fontWeight = if (elegida) FontWeight.Medium else FontWeight.Normal, color = color))
    }
}

@Composable
private fun Divisor() {
    Box(Modifier.padding(start = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.borde))
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
