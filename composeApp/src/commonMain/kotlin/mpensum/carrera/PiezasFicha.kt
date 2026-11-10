package mpensum.carrera

import mpensum.componentes.TarjetaLista
import mpensum.componentes.icono
import mpensum.componentes.trazos
import mpensum.componentes.Velo
import mpensum.componentes.FilaLista
import mpensum.componentes.nota
import mpensum.componentes.Pastilla
import mpensum.componentes.estiloRotulo
import mpensum.componentes.estilo
import mpensum.componentes.estiloNota
import mpensum.componentes.BotonRedondo
import mpensum.componentes.estiloTitulo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.iconoEstado
import mpensum.componentes.colorEstado
import mpensum.componentes.Lucide
import mpensum.componentes.margenPantalla
import mpensum.logica.Situacion
import mpensum.mapa.nombreCasilla
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.plex

/*
 * Las piezas de la ficha de una materia (docs/ficha-y-lista.md §1). Cada una
 * va por separado porque la ficha de pie y la acostada las componen distinto:
 * la cabecera (quién es), la píldora de estado, la respuesta («¿puede
 * esperar?»), «Pide · Abre» y los botones de marcar. Ninguna sabe en qué hoja
 * está; todas reciben el estado de la carrera y el código de la materia.
 */

/** Lo que cambia de una materia a otra se funde en 150 ms, sin cambiar el alto de golpe a media animación */
/** Sale la vieja (100 ms) y después entra la nueva (150 ms): nunca dos textos encimados */
internal fun fundidoEnSerie(): ContentTransform =
    if (Movimiento.reducido) fadeIn(tween(Movimiento.CORTA)) togetherWith fadeOut(tween(Movimiento.CORTA))
    else fadeIn(tween(Movimiento.CORTA, Movimiento.SALIDA_RAPIDA, Movimiento.ENTRA)) togetherWith fadeOut(tween(Movimiento.SALIDA_RAPIDA, easing = Movimiento.SALE))

/** La fila de la respuesta reserva este alto desde el primer cuadro: la hoja no crece cuando llega el cálculo */
private val ALTO_RESPUESTA = 64.dp

/** Un estado de la materia como lo dice la píldora; «por elegir» es el de una casilla de electiva vacía */
internal enum class EstadoFicha(val palabra: String) {
    DISPONIBLE("DISPONIBLE"),
    CURSANDO("CURSANDO"),
    APROBADA("APROBADA"),
    BLOQUEADA("BLOQUEADA"),
    PROXIMA("PRÓXIMA"),
    POR_ELEGIR("POR ELEGIR"),
}

private fun estadoFichaDe(s: Situacion) = when (s) {
    Situacion.HECHA -> EstadoFicha.APROBADA
    Situacion.CURSANDO -> EstadoFicha.CURSANDO
    Situacion.INSCRIBIBLE -> EstadoFicha.DISPONIBLE
    Situacion.PROXIMA -> EstadoFicha.PROXIMA
    Situacion.LEJANA -> EstadoFicha.BLOQUEADA
}

private fun situacionDeFicha(e: EstadoFicha) = when (e) {
    EstadoFicha.APROBADA -> Situacion.HECHA
    EstadoFicha.CURSANDO -> Situacion.CURSANDO
    EstadoFicha.DISPONIBLE -> Situacion.INSCRIBIBLE
    EstadoFicha.PROXIMA -> Situacion.PROXIMA
    else -> Situacion.LEJANA
}

internal fun nombreArea(area: String?) = when (area) {
    "generales" -> "Generales"
    "ciencias-basicas" -> "Ciencias básicas"
    "estadistica" -> "Estadística"
    "computacion" -> "Computación"
    "electronica" -> "Electrónica"
    "sistemas" -> "Sistemas"
    "gestion" -> "Gestión"
    "tesis" -> "Trabajo de grado"
    else -> null
}

/** Lo que dice la cabecera de una materia: se calcula una vez y la usan la ficha de pie y la acostada */
internal class EncabezadoFicha(
    val rotulo: String,
    /** Su código, salvo en una casilla vacía, que aún no es ninguna materia */
    val codigo: String?,
    val titulo: String,
    /** «2 UC · Gestión», o lo que lleva elegido el grupo de una casilla vacía */
    val datos: String,
    val estado: EstadoFicha,
    /** La casilla donde está puesta, si es una electiva: de ahí sale «Cambiar» */
    val casilla: String?,
)

/** Null si la materia no existe en este pensum */
internal fun encabezadoDe(codigo: String, estado: EstadoCarrera): EncabezadoFicha? {
    val nodo = estado.layout.porCodigo[codigo] ?: return null
    val materia = nodo.asignatura
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == materia.grupo }
    if (materia.esHueco && estado.enCasilla(codigo) == null) {
        val vacia = casillaElegida(estado, codigo)
        return EncabezadoFicha(
            rotulo = (materia.semestre?.let { "SEMESTRE $it · " } ?: "") + "ELECTIVA",
            codigo = null,
            titulo = materia.nombre,
            datos = vacia?.let { cuotaDeCasilla(it, estado) } ?: "",
            estado = EstadoFicha.POR_ELEGIR,
            casilla = null,
        )
    }
    val casilla = estado.casillaDe(codigo)
    val semestre = materia.semestre ?: casilla?.let { c -> estado.nodos.firstOrNull { it.codigo == c }?.semestre }
    val esElectiva = materia.semestre == null
    val rotulo = listOfNotNull(
        semestre?.let { "SEMESTRE $it" },
        if (esElectiva) (grupo?.let { nombreCasilla(it).uppercase() } ?: "ELECTIVA") else null,
    ).joinToString(" · ")
    val datos = listOfNotNull("${materia.uc ?: 0} UC", nombreArea(materia.area)).joinToString(" · ")
    return EncabezadoFicha(rotulo, materia.codigo, materia.nombre, datos, estadoFichaDe(estado.situaciones[codigo] ?: Situacion.LEJANA), casilla)
}

/**
 * El cambio de materia (patrón B): la que sale se funde rápido, después entra la
 * nueva, y el alto se anima una sola vez hacia el de la nueva con el resorte de
 * las hojas. Nunca se leen las dos a la vez. `alMedir` recibe el alto de cada
 * contenido ya medido: de ahí sale el alto de destino que usa la cámara.
 */
@Composable
internal fun Transition<String>.FundidoDeMateria(modifier: Modifier = Modifier, alMedir: (String, Int) -> Unit = { _, _ -> }, contenido: @Composable (String) -> Unit) {
    AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            fundidoEnSerie().using(SizeTransform(clip = false) { _, _ -> Movimiento.ALTO })
        },
    ) { c -> Box(Modifier.onSizeChanged { alMedir(c, it.height) }) { contenido(c) } }
}

/** Lo mismo con su propio reloj, para quien no comparte cabecera y cuerpo (la burbuja) */
@Composable
internal fun FundidoDeMateria(codigo: String, modifier: Modifier = Modifier, contenido: @Composable (String) -> Unit) {
    updateTransition(codigo, label = "materia").FundidoDeMateria(modifier, contenido = contenido)
}

// ─────────────────────────────────── Cabecera ──────────────────────────────────

/** Rótulo y código a la izquierda, un texto-botón opcional («Cambiar») y la X (la burbuja no lleva): una fila de 44, el toque mínimo */
@Composable
internal fun RotuloFicha(enc: EncabezadoFicha, alCerrar: () -> Unit, alCambiar: (String) -> Unit, conCerrar: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (conCerrar) 44.dp else 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText(enc.rotulo, style = estiloRotulo(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (enc.codigo != null) {
                BasicText(enc.codigo, Modifier.padding(start = Espacio.xs), style = TextStyle(fontFamily = plex(), fontWeight = FontWeight.Light, fontSize = Tipo.xxs, lineHeight = 16.sp, letterSpacing = 0.04.em, color = Paleta.tenue), maxLines = 1)
            }
            if (enc.casilla != null) {
                BasicText(
                    "Cambiar",
                    Modifier.padding(start = Espacio.xs).heightIn(min = 44.dp).clickable(role = Role.Button) { alCambiar(enc.casilla) }.padding(horizontal = Espacio.xs).wrapCentrado(),
                    style = estiloNota(Paleta.tinta, FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
        if (conCerrar) BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
    }
}

/** El texto de un botón de texto, centrado en su alto: `heightIn` solo no lo centra */
private fun Modifier.wrapCentrado(): Modifier = this.then(Modifier.padding(vertical = 14.dp))

/** El nombre: Jost 300, dos líneas como mucho, encogiéndose de 28 a 24 (en la burbuja, de 22 a 20) antes de partirse de más */
@Composable
internal fun TituloFicha(enc: EncabezadoFicha, modifier: Modifier = Modifier, compacto: Boolean = false) {
    val estilo = if (compacto) estiloTitulo().copy(lineHeight = 26.sp) else estiloTitulo()
    val tamanos = if (compacto) TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 22.sp, stepSize = 1.sp) else TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 28.sp, stepSize = 1.sp)
    BasicText(enc.titulo, modifier, style = estilo, maxLines = 2, overflow = TextOverflow.Ellipsis, autoSize = tamanos)
}

/** «2 UC · Gestión» y, a la derecha, la píldora de estado */
@Composable
internal fun DatosFicha(enc: EncabezadoFicha, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(enc.datos, Modifier.weight(1f).padding(end = Espacio.xs), style = estiloNota(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        FundidoDeEstado(enc.estado)
    }
}

/** La cabecera entera de pie: rótulo con la X, nombre y datos con la píldora */
@Composable
internal fun CabeceraFicha(enc: EncabezadoFicha, alCerrar: () -> Unit, alCambiar: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = margenPantalla())) {
        RotuloFicha(enc, alCerrar, alCambiar)
        TituloFicha(enc, Modifier.padding(top = 2.dp))
        DatosFicha(enc, Modifier.padding(top = Espacio.xs + 2.dp))
    }
}

// ─────────────────────────────────── Píldora ──────────────────────────────────

/** La píldora cambia de palabra y de icono con un fundido: marcar la materia no la parpadea */
@Composable
private fun FundidoDeEstado(e: EstadoFicha) {
    AnimatedContent(
        targetState = e,
        transitionSpec = { fundidoEnSerie().using(null) },
        label = "pildora",
    ) { PildoraEstado(it) }
}

/**
 * La palabra de estado, la única vez que se dice con letras (en las filas
 * basta el icono): 24 dp, radio 6, el icono de 14 y el rótulo.
 */
@Composable
internal fun PildoraEstado(e: EstadoFicha) {
    val forma = RoundedCornerShape(6.dp)
    val (fondo, texto) = when (e) {
        EstadoFicha.DISPONIBLE -> Paleta.azul.copy(alpha = Velo.pastillaAzul) to Paleta.azul
        EstadoFicha.CURSANDO -> Color.Transparent to Paleta.tinta
        EstadoFicha.APROBADA -> Paleta.tinta.copy(alpha = Velo.pastilla) to Paleta.tinta
        EstadoFicha.BLOQUEADA, EstadoFicha.PROXIMA -> Paleta.tinta.copy(alpha = Velo.pastilla) to Paleta.suave
        EstadoFicha.POR_ELEGIR -> Color.Transparent to Paleta.suave
    }
    val contorno = when (e) {
        EstadoFicha.CURSANDO -> Modifier.border(1.5.dp, Paleta.tinta, forma)
        EstadoFicha.POR_ELEGIR -> Modifier.drawBehind {
            val g = 1.dp.toPx()
            drawRoundRect(
                Paleta.tinta.copy(alpha = 0.3f), Offset(g / 2, g / 2), androidx.compose.ui.geometry.Size(size.width - g, size.height - g),
                CornerRadius(6.dp.toPx()), style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
        else -> Modifier
    }
    Row(
        Modifier.height(24.dp).clip(forma).background(fondo).then(contorno).padding(horizontal = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xxs),
    ) {
        if (e == EstadoFicha.POR_ELEGIR) Canvas(Modifier.size(14.dp)) {
            val g = 1.5.dp.toPx()
            drawCircle(Paleta.tinta.copy(alpha = 0.45f), size.minDimension / 2 - g, style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))))
        } else IconoSituacion(situacionDeFicha(e), 14.dp)
        BasicText(e.palabra, style = estiloRotulo(texto), maxLines = 1)
    }
}

/** El icono de estado (sello, libro, candados, reloj) de `lado`: el mismo de la lista y del mapa */
@Composable
internal fun IconoSituacion(s: Situacion, lado: Dp) {
    Canvas(Modifier.size(lado)) {
        iconoEstado(s, 0f, 0f, size.minDimension, Paleta.panel, color = if (s == Situacion.PROXIMA) Paleta.tenue else colorEstado(s))
    }
}

// ─────────────────────────────────── Respuesta ──────────────────────────────────

private fun NotaRespuesta.texto(): AnnotatedString = buildAnnotatedString {
    append(antes)
    if (fecha != null) withStyle(SpanStyle(color = if (alerta) Paleta.rojo else Paleta.tinta, fontWeight = FontWeight.Medium)) { append(fecha) }
}

/**
 * «¿Puede esperar?»: una sola fila, la que importa para tu fecha. La tarjeta
 * reserva sus 64 dp solo mientras se calcula y el texto entra con un fundido;
 * una aprobada o una materia fuera del plan no tienen fila (no hay nada que decidir).
 */
@Composable
internal fun RespuestaFicha(codigo: String, estado: EstadoCarrera, modifier: Modifier = Modifier) {
    val situacion = estado.situaciones[codigo] ?: return
    if (situacion == Situacion.HECHA) return
    val respuestas = estado.respuestas
    val despachador = LocalCalculo.current
    LaunchedEffect(respuestas, codigo, situacion) { respuestas.calcular(codigo, situacion, despachador) }
    val actual = respuestas.de(codigo, situacion)
    if (actual == EstadoRespuesta.SinRespuesta) return
    // Nace con lo ya calculado: si la ficha lo pidió antes de cambiar de materia, no hay segundo fundido.
    // Mientras llega la siguiente respuesta (tras marcar) se sigue pintando la anterior, que se apaga
    var mostrada by remember { mutableStateOf((actual as? EstadoRespuesta.Lista)?.respuesta) }
    if (actual is EstadoRespuesta.Lista) mostrada = actual.respuesta
    val opacidad = animateFloatAsState(if (actual is EstadoRespuesta.Lista) 1f else 0f, tween(Movimiento.MEDIA, easing = Movimiento.ESTANDAR), label = "respuesta")
    TarjetaLista(modifier.heightIn(min = ALTO_RESPUESTA)) {
        mostrada?.let { r ->
            Box(Modifier.graphicsLayer { alpha = opacidad.value }) {
                FilaLista(
                    titulo = r.titulo,
                    nota = r.nota?.texto(),
                    primera = true,
                    icono = { Pastilla(r.icono.trazos(), r.azul) },
                )
            }
        }
    }
}

// ─────────────────────────────────── Pide · Abre ──────────────────────────────────
