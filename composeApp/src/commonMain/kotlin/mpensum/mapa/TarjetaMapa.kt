package mpensum.mapa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.componentes.aspecto
import mpensum.componentes.iconoEstado
import mpensum.datos.Asignatura
import mpensum.logica.Situacion
import mpensum.tema.PESO_NOMBRE
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.avivar
import mpensum.tema.jost
import mpensum.tema.plex

/*
 * La tarjeta de una materia en el mapa, la misma de la web (CaraTarjeta.jsx
 * y NodoAsignatura.jsx): 224×96 en unidades del plano, código arriba, nombre
 * en Jost, el punto de su color y las UC abajo, y el icono de su situación
 * arriba a la derecha. Los textos van en dp y no en sp: son parte del dibujo
 * y escalan con el mapa.
 */

/** Los estilos de texto del mapa, medidos en unidades del plano y con los colores y el peso del tema */
@Immutable
class TiposMapa(private val densidad: Density, jost: FontFamily, plex: FontFamily) {
    private fun tam(v: Double) = with(densidad) { v.toFloat().dp.toSp() }
    val codigo = TextStyle(fontFamily = plex, fontWeight = FontWeight.Light, fontSize = tam(TextoTarjeta.CODIGO), letterSpacing = 0.04.em, color = Paleta.tenue)
    val nombre = TextStyle(fontFamily = jost, fontWeight = PESO_NOMBRE, fontSize = tam(TextoTarjeta.NOMBRE), lineHeight = tam(TextoTarjeta.ALTO_LINEA))
    val meta = TextStyle(fontFamily = plex, fontWeight = FontWeight.Light, fontSize = tam(TextoTarjeta.META), color = Paleta.tenue)
    val hueco = TextStyle(fontFamily = jost, fontWeight = PESO_NOMBRE, fontSize = tam(TextoTarjeta.NOMBRE), color = Paleta.tenue, textAlign = TextAlign.Center)
    // La cabecera de cada semestre (RotulosGrafo.jsx), en la letra de la interfaz de la web: Jost
    val titulo = TextStyle(fontFamily = jost, fontWeight = FontWeight.Medium, fontSize = tam(11.5), letterSpacing = 0.3.em, color = Paleta.tinta)
    val datos = TextStyle(fontFamily = plex, fontWeight = FontWeight.Light, fontSize = tam(10.0), letterSpacing = 0.04.em, color = Paleta.tenue)
    val cifra = TextStyle(fontFamily = jost, fontWeight = FontWeight.ExtraLight, fontSize = tam(32.0), letterSpacing = (-0.02).em)
    val unidad = TextStyle(fontFamily = jost, fontWeight = FontWeight.Light, fontSize = tam(14.0), color = Paleta.tenue)
    val pie = TextStyle(fontFamily = jost, fontWeight = FontWeight.Medium, fontSize = tam(9.5), letterSpacing = 0.22.em)
    val cuenta = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = tam(11.0))
}

@Composable
fun rememberTiposMapa(): TiposMapa {
    val densidad = LocalDensity.current
    val j = jost()
    val p = plex()
    val claro = Tema.claro
    return remember(densidad, j, p, claro) { TiposMapa(densidad, j, p) }
}

/** El color "fuerte" de cada situación: el borde de la elegida, de su cadena y su aura */
fun fuerte(s: Situacion): Color = when (s) {
    Situacion.HECHA -> Paleta.verde
    Situacion.CURSANDO -> Paleta.cursando
    Situacion.INSCRIBIBLE -> Paleta.azul
    else -> Paleta.tinta.copy(alpha = 0.62f)
}

/** El icono mide 17,5 (dibujado en una rejilla de 20) y su centro queda a la altura del código */
private const val LADO_ICONO = 17.5f
private val X_ICONO = (Nodo.ANCHO - Nodo.PAD_DER - LADO_ICONO).toFloat()
private const val Y_ICONO = 10.5f

private val ANILLO = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val DESTELLO = CubicBezierEasing(0.22f, 0.61f, 0.2f, 1f)
private val CARGA = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/** Lo que la tarjeta está animando ahora mismo, si algo */
@Immutable
data class AnimacionesTarjeta(val toque: Int? = null, val destello: Int? = null, val carga: Carga? = null)

/**
 * Una materia del mapa en su sitio. `materia` es lo que hay en el nodo (la
 * electiva puesta, si es una casilla); null si es una casilla vacía.
 * `opacidad` baja con el foco puesto en otra cadena: se lee al dibujar, así
 * que apagar el mapa entero no recompone ninguna tarjeta.
 */
@Composable
fun TarjetaMapa(
    nodo: NodoMapa,
    materia: Asignatura?,
    lineas: List<String>,
    situacion: Situacion?,
    acento: Color,
    seleccionada: Boolean,
    resaltada: Boolean,
    opacidad: () -> Float,
    animaciones: AnimacionesTarjeta,
    escala: () -> Float,
    tipos: TiposMapa,
    camara: Camara,
    alTocar: () -> Unit,
    alEmpezarCarga: () -> Unit,
    alSoltarCarga: () -> Unit,
    alFijar: () -> Unit,
) {
    val anillo = remember { Animatable(1f) }
    val destello = remember { Animatable(1f) }
    val carga = remember { Animatable(0f) }
    val apagado = remember { Animatable(1f) }
    // Casi ninguna tarjeta anima nada a la vez: los efectos solo existen mientras hay algo que animar
    // (60 tarjetas × 4 corrutinas quietas eran unas 240 al abrir el mapa). Al irse, la animación se cancela
    // igual que al cambiar de clave; cada una vuelve a empezar desde cero la próxima vez.
    if (animaciones.toque != null) LaunchedEffect(animaciones.toque) {
        anillo.snapTo(0f)
        anillo.animateTo(1f, tween(620, easing = LinearEasing))
    }
    if (animaciones.destello != null) LaunchedEffect(animaciones.destello) {
        destello.snapTo(0f)
        // Espera a que la luz llegue por el cable: encenderse antes que el cable se ve mal
        destello.animateTo(1f, tween(1100, delayMillis = 1000, easing = LinearEasing))
    }
    val cargando = animaciones.carga
    if (cargando != null) LaunchedEffect(cargando.n, cargando.hecha) {
        if (!cargando.hecha) {
            apagado.snapTo(1f)
            carga.snapTo(0f)
            carga.animateTo(1f, tween(CARGA_MS, delayMillis = ESPERA_MS, easing = CARGA))
        } else {
            carga.snapTo(1f)
            apagado.animateTo(0f, tween(APAGADO_MS, easing = DESTELLO))
        }
    }

    val base = Modifier
        .offset(nodo.x.toFloat().dp, nodo.y.toFloat().dp)
        .size(Nodo.ANCHO.toFloat().dp, Nodo.ALTO.toFloat().dp)
        .graphicsLayer {
            alpha = opacidad()
            // En modo ligero, apagada por el foco no se pinta aparte para fundirla: cada trazo baja su alfa
            if (Tema.ligero) compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        .toqueYMantener(nodo.codigo, camara, alEmpezarCarga, alSoltarCarga, alTocar, alFijar)
        .drawWithContent {
            drawContent()
            val u = density
            if (animaciones.toque != null && anillo.value < 1f) {
                val p = ANILLO.transform(anillo.value)
                bordeRedondeado(Paleta.tinta.copy(alpha = 0.7f * (1 - p)), (2 + 12 * p) * u)
            }
            if (animaciones.destello != null && destello.value > 0f && destello.value < 1f) {
                val t = destello.value
                val p = DESTELLO.transform(t)
                val alfa = if (t < 0.18f) 0.8f * (t / 0.18f) else 0.8f * (1 - (p - 0.18f) / 0.82f)
                scale(1 + 0.06f * p, 1 + 0.28f * p) { bordeRedondeado(Paleta.azul.copy(alpha = alfa.coerceIn(0f, 1f)), 1.5f * u) }
            }
            if (cargando != null && carga.value > 0f && apagado.value > 0f) contornoDeCarga(carga.value, avivar(acento).copy(alpha = apagado.value), 1.6f / escala())
        }

    if (materia == null || situacion == null) {
        CasillaVacia(nodo.asignatura.nombre, base, tipos)
        return
    }
    val a = aspecto(situacion)
    Box(
        base.drawBehind {
            val u = density
            val radio = CornerRadius(Nodo.RADIO.toFloat() * u)
            // Solo la elegida lleva aura: es la única que necesita separarse del resto
            if (seleccionada) {
                drawRoundRect(
                    fuerte(situacion).copy(alpha = 0.3f), Offset(-4 * u, -4 * u), Size(size.width + 8 * u, size.height + 8 * u),
                    CornerRadius((Nodo.RADIO.toFloat() + 4) * u), style = Stroke(2 * u),
                )
            }
            // La sombra de papel de los temas claros; en modo ligero la tarjeta se separa solo con su borde
            if (a.sombra.alpha > 0f && !Tema.ligero) drawRoundRect(a.sombra, Offset(0f, 2 * u), size, radio)
            drawRoundRect(a.fondo, cornerRadius = radio)
            val grosor = (if (seleccionada) a.grosor + 0.75f else a.grosor) * u
            drawRoundRect(
                if (seleccionada || resaltada) fuerte(situacion) else a.borde, Offset(grosor / 2, grosor / 2), Size(size.width - grosor, size.height - grosor),
                radio, style = Stroke(grosor),
            )
            iconoEstado(situacion, X_ICONO * u, Y_ICONO * u, LADO_ICONO * u, a.fondo)
            drawCircle(acento, 3 * u, Offset((Nodo.PAD_IZQ.toFloat() + 3) * u, (Nodo.ALTO.toFloat() - 16) * u))
        },
    ) {
        if (Tema.codigos) BasicText(materia.codigo, Modifier.offset(Nodo.PAD_IZQ.toFloat().dp, 13.dp), style = tipos.codigo)
        // Las líneas se centran en el alto de la tarjeta, como en la web (línea base de la primera, menos su ascenso)
        val primera = TextoTarjeta.CENTRO_NOMBRE - (lineas.size - 1) * TextoTarjeta.ALTO_LINEA / 2 - 12.2
        BasicText(
            lineas.joinToString("\n"),
            Modifier.offset(Nodo.PAD_IZQ.toFloat().dp, primera.toFloat().dp).width(Nodo.ANCHO_TEXTO.toFloat().dp),
            style = tipos.nombre.copy(color = a.nombre),
            softWrap = false,
        )
        BasicText("${materia.uc ?: 0} UC", Modifier.offset((Nodo.PAD_IZQ.toFloat() + 12).dp, 72.5.dp), style = tipos.meta)
    }
}

/** Un borde con la forma de la tarjeta, centrado en su canto */
private fun DrawScope.bordeRedondeado(color: Color, grosor: Float) {
    drawRoundRect(color, cornerRadius = CornerRadius(Nodo.RADIO.toFloat() * density), style = Stroke(grosor))
}

/** La línea que recorre el borde mientras el dedo se queda quieto (ContornoCarga.jsx) */
private fun DrawScope.contornoDeCarga(progreso: Float, color: Color, grosorDp: Float) {
    val u = density
    val contorno = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(Nodo.RADIO.toFloat() * u))) }
    val largo = PathMeasure().apply { setPath(contorno, false) }.length
    drawPath(
        contorno, color,
        style = Stroke(grosorDp * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(largo * progreso, largo), 0f)),
    )
}

/** La casilla de una electiva sin elegir: un hueco punteado con su "+" */
@Composable
private fun CasillaVacia(nombre: String, modifier: Modifier, tipos: TiposMapa) {
    Box(
        modifier.drawBehind {
            val u = density
            val g = 1.5f * u
            drawRoundRect(
                Paleta.tinta.copy(alpha = 0.22f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(Nodo.RADIO.toFloat() * u),
                style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * u, 5 * u))),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(30.dp).drawBehind { masCasilla() })
            BasicText(nombre, Modifier.offset(y = 6.dp), style = tipos.hueco)
        }
    }
}

private fun DrawScope.masCasilla() {
    val u = density
    drawRoundRect(Paleta.tinta.copy(alpha = 0.22f), cornerRadius = CornerRadius(8 * u), style = Stroke(1 * u))
    val c = center
    val l = 6 * u
    drawLine(Paleta.suave, Offset(c.x - l, c.y), Offset(c.x + l, c.y), 1.5f * u)
    drawLine(Paleta.suave, Offset(c.x, c.y - l), Offset(c.x, c.y + l), 1.5f * u)
}
