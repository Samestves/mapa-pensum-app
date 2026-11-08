package mpensum.carrera.lista

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.iconoEstado
import mpensum.logica.Situacion
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost
import mpensum.tema.plex
import mpensum.tema.porTema

/*
 * Lo que comparten las piezas de la lista: los tintes de la Paleta Acero (la
 * raya, el realce y el fondo de los botones), las letras de las maquetas y los
 * iconos de estado. Los tintes son getters: leen el tema al dibujar y se
 * actualizan solos al cambiar de juego.
 */

/** La raya fina bajo el texto de cada fila y bajo los datos */
internal val RAYA: Color get() = Paleta.tinta.copy(alpha = porTema(0.07f, 0.08f))

/** El realce de la fila elegida o recién marcada, y el fondo de los controles suaves */
internal val REALCE: Color get() = Paleta.tinta.copy(alpha = porTema(0.045f, 0.035f))
internal val BOTON: Color get() = Paleta.tinta.copy(alpha = porTema(0.08f, 0.07f))

/** El color de cada estado en la lista: el azul solo para lo inscribible, y cursando en tinta */
internal fun colorDeEstado(s: Situacion): Color = when (s) {
    Situacion.HECHA -> Paleta.verde
    Situacion.CURSANDO -> Paleta.cursando
    Situacion.INSCRIBIBLE -> Paleta.azul
    Situacion.PROXIMA -> Paleta.suave
    Situacion.LEJANA -> Paleta.rojo
}

/** El icono de estado de la fila (sello, libro, candados, reloj de arena); null es la casilla de electiva sin elegir, un aro punteado */
@Composable
internal fun IconoDeEstado(s: Situacion?, lado: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(lado)) {
        if (s == null) {
            val g = 1.5.dp.toPx()
            drawCircle(
                Paleta.tenue, size.minDimension * 0.45f - g / 2, Offset(size.width / 2, size.height / 2),
                style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))),
            )
        } else iconoEstado(s, 0f, 0f, size.minDimension, Paleta.lienzo, color = colorDeEstado(s))
    }
}

/** Un icono de estado con un color y un fondo de recorte dados: el del botón lleno y el del gesto */
@Composable
internal fun IconoDeEstadoSobre(s: Situacion, color: Color, sobre: Color, lado: Dp = 20.dp) {
    Canvas(Modifier.size(lado)) { iconoEstado(s, 0f, 0f, size.minDimension, sobre, color = color) }
}

private fun trazo(d: String): Path = PathParser().parsePathString(d).toPath()

/** Los iconos de trazo (rejilla de 24) que solo usa la lista */
internal object TrazosLista {
    val orden = listOf(trazo("M3 6h18M7 12h10M10 18h4"))
    val chevronAbajo = listOf(trazo("m6 9 6 6 6-6"))
    val chevronArriba = listOf(trazo("m18 15-6-6-6 6"))
}

/** Los tres puntos del botón «…» de la cabecera */
@Composable
internal fun IconoMas(color: Color, lado: Dp = 20.dp) {
    Canvas(Modifier.size(lado)) {
        val e = size.minDimension / 24f
        for (x in listOf(5.5f, 12f, 18.5f)) drawCircle(color, 1.25f * e, Offset(x * e, 12f * e))
    }
}

// ─────────────────────────────────── Letras ──────────────────────────────────

/** El nombre de una materia en la fila: Jost 16,5/21 */
@Composable
internal fun estiloNombre(color: Color = Paleta.tinta) =
    TextStyle(fontFamily = jost(), fontWeight = FontWeight.Normal, fontSize = 16.5.sp, lineHeight = 21.sp, letterSpacing = (-0.003).em, color = color)

/** La razón bajo el nombre: Inter 13/18, tenue */
@Composable
internal fun estiloRazon() =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.tenue)

/** Las UC y los códigos: Plex Light 12 */
@Composable
internal fun estiloPlex(color: Color = Paleta.tenue, tam: Float = 12f) =
    TextStyle(fontFamily = plex(), fontWeight = FontWeight.Light, fontSize = tam.sp, lineHeight = 14.sp, color = color)

/** Lo que se toca dentro de una fila o un encabezado («Deshacer», «Elegir»): Inter 500, 13 */
@Composable
internal fun estiloAccion() =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 14.sp, color = Paleta.tinta)

/** Mayúsculas finas y espaciadas: el rótulo de una sección */
@Composable
internal fun estiloSeccion() =
    TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em, color = Paleta.suave)

/** Un texto con «**negrita**»: la lógica marca lo que destaca y aquí se le da el peso y la tinta */
@Composable
internal fun conNegritas(texto: String, tinta: Color = Paleta.tinta): AnnotatedString = remember(texto, tinta) {
    if ("**" !in texto) AnnotatedString(texto)
    else buildAnnotatedString {
        texto.split("**").forEachIndexed { i, trozo ->
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = tinta)) { append(trozo) } else append(trozo)
        }
    }
}
