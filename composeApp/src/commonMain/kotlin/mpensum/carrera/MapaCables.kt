package mpensum.carrera

import mpensum.mapa.fuerte
import mpensum.mapa.Segmento
import mpensum.mapa.Arista
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import mpensum.carrera.EstadoCarrera
import mpensum.logica.Situacion
import mpensum.logica.Tramo
import mpensum.logica.tramoDe
import mpensum.tema.Movimiento
import mpensum.tema.FormaCables
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.avivar
import mpensum.tema.porTema
import kotlin.math.max
import kotlin.math.min

/* Los cables de prelación del mapa: su trazado, su color y la descarga que baja por ellos */

/* La fuerza de cada cable según lo que dice (Arista.jsx) */
private fun opacidad(t: Tramo): Float {
    val base = when (t) {
        Tramo.FRONTERA -> 0.55f
        Tramo.RECORRIDO -> 0.5f
        Tramo.PROXIMA -> 0.32f
        Tramo.LEJANA -> porTema(0.12f, 0.2f)
    }
    // En Exterior los cables se firman más: a pleno sol los finos se pierden
    return if (Tema.fuerte) (base * 1.5f).coerceAtMost(0.9f) else base
}

private fun grosor(t: Tramo) = when (t) {
    Tramo.FRONTERA -> 1.75f
    Tramo.RECORRIDO -> 1.5f
    Tramo.PROXIMA -> 1.25f
    Tramo.LEJANA -> 1f
}

/** Un cable ya convertido a Path, medido, con sus extremos y sus colores */
internal class CableDibujo(val arista: Arista, val ruta: Path, val desde: Color, val hacia: Color, val inicio: Offset, val fin: Offset) {
    private val medida = PathMeasure().apply { setPath(ruta, false) }
    val largo = medida.length
    private val parte = Path()

    /** El trozo del principio hasta la fracción `f` del largo. Reutiliza siempre el mismo Path */
    fun hasta(f: Float): Path {
        parte.reset()
        medida.getSegment(0f, largo * f.coerceIn(0f, 1f), parte, true)
        return parte
    }

    /** El trozo entre dos distancias, acotadas al largo del cable. Reutiliza siempre el mismo Path */
    fun trozo(desde: Float, hasta: Float): Path {
        parte.reset()
        val ini = desde.coerceIn(0f, largo)
        val fin = hasta.coerceIn(0f, largo)
        if (fin > ini) medida.getSegment(ini, fin, parte, true)
        return parte
    }
}

internal fun cablesDe(estado: EstadoCarrera): List<CableDibujo> {
    val porCodigo = estado.layout.porCodigo
    return estado.layout.aristas.map { a ->
        val ruta = Path().apply {
            for (s in a.trazo) when (s) {
                is Segmento.Mover -> moveTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Linea -> lineTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Curva -> cubicTo(s.x1.toFloat(), s.y1.toFloat(), s.x2.toFloat(), s.y2.toFloat(), s.x.toFloat(), s.y.toFloat())
            }
        }
        val inicio = a.trazo.first()
        val fin = a.trazo.last()
        CableDibujo(
            a, ruta,
            porCodigo[a.origen]?.asignatura?.let(estado.colores::de)?.let(::colorDeCable) ?: Paleta.suave,
            porCodigo[a.destino]?.asignatura?.let(estado.colores::de)?.let(::colorDeCable) ?: Paleta.suave,
            Offset(inicio.x.toFloat(), inicio.y.toFloat()), Offset(fin.x.toFloat(), fin.y.toFloat()),
        )
    }
}

/** El color de un cable: el de su área, más o menos tirado a la tinta según el tema (`Juego.cables`: 1 el del área, 0 la tinta) */
private fun colorDeCable(area: Color): Color {
    val color = Tema.juego.cables
    return if (color >= 1f) area else lerp(Paleta.tinta, area, color)
}

/**
 * La luz de la frontera no es de un color: sale con el del área de la que
 * viene, pasa por ese mismo color girado medio tono y llega con el de la
 * materia que puedes inscribir (el oklch de la web, aquí en HSL).
 */
private fun girar(color: Color, grados: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    if (mx == mn) return color
    val l = (mx + mn) / 2
    val d = mx - mn
    val s = if (l > 0.5f) d / (2 - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } * 60f
    return Color.hsl((h + grados) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

internal fun degradado(c: CableDibujo, vivo: Boolean): Brush {
    val medio = girar(c.desde, 48f)
    val colores = if (vivo) listOf(avivar(c.desde), avivar(medio), avivar(c.hacia)) else listOf(c.desde, medio, c.hacia)
    return Brush.linearGradient(colores, c.inicio, c.fin)
}

/**
 * Los cables: la base de cada uno con la fuerza de lo que dice, la cadena
 * de la materia que se mira dibujándose de origen a destino, y la descarga
 * verde de la que se acaba de aprobar. Las rutas se arman una vez; en cada
 * cuadro solo cambian opacidades y trozos. En modo ligero la frontera va del
 * color de su origen, sin degradado: un shader por cable menos que pintar.
 */
@Composable
internal fun Cables(
    estado: EstadoCarrera,
    cables: List<CableDibujo>,
    cadena: () -> Set<String>?,
    foco: () -> Float,
    trazado: () -> Float,
    descarga: () -> Pair<String?, Float>,
) {
    Spacer(
        // En su propia capa: la luz de la frontera, las cabeceras o el foco no la repintan, y moverla no la toca
        Modifier.fillMaxSize().graphicsLayer {}.drawWithCache {
            // Degradados y trazos se arman una vez: en cada cuadro solo se eligen
            val degradados = cables.map { degradado(it, false) }
            // El grosor elegido es un factor del trazo que ya se crea: no cuesta nada más
            val factor = Tema.grosor.factor
            val trazos = Tramo.entries.associateWith { Stroke(grosor(it) * factor, cap = StrokeCap.Round) }
            val linea = Stroke(2.25f * factor, cap = StrokeCap.Round)
            onDrawBehind {
                scale(density, Offset.Zero) {
                    val sit = estado.situaciones
                    val enFoco = cadena()
                    val f = foco()
                    val (aprobada, tiempo) = descarga()
                    val conDegradado = !Tema.ligero
                    // Al tocar: solo los cables de la materia señalada (la cadena), que entran con el foco
                    val soloTocar = Tema.cables == FormaCables.AL_TOCAR
                    val atenuar = Tema.atenuarAprobadas
                    for ((i, c) in cables.withIndex()) {
                        val a = c.arista
                        val tramo = tramoDe(sit[a.origen] ?: Situacion.LEJANA, sit[a.destino] ?: Situacion.LEJANA)
                        val frontera = tramo == Tramo.FRONTERA
                        val enCadena = enFoco != null && a.origen in enFoco && a.destino in enFoco
                        if (!soloTocar || enCadena) {
                            val apartado = if (enFoco != null && !enCadena) 1f - (1f - OPACIDAD_FOCO) * f else 1f
                            // Dentro del foco la base baja a 0,2 y la cadena se dibuja encima, en su color
                            val base = if (enCadena) opacidad(tramo) + (0.2f - opacidad(tramo)) * f else opacidad(tramo) * apartado
                            // Lo aprobado, atenuado, y al tocar la base entra con el foco
                            val atenuado = if (atenuar && tramo == Tramo.RECORRIDO) ATENUADO else 1f
                            val alfa = base * atenuado * (if (soloTocar) f else 1f)
                            val estilo = trazos.getValue(tramo)
                            if (frontera && conDegradado) drawPath(c.ruta, degradados[i], alpha = alfa, style = estilo)
                            else drawPath(c.ruta, c.desde.copy(alpha = alfa), style = estilo)
                            if (enCadena && f > 0f) {
                                val trozo = c.hasta(trazado())
                                if (frontera && conDegradado) drawPath(trozo, degradados[i], alpha = 0.95f * f, style = linea)
                                else drawPath(trozo, c.desde.copy(alpha = 0.95f * f), style = linea)
                            }
                            // El punto de llegada: el de la frontera siempre, el de la cadena con el foco puesto
                            if (frontera) drawCircle(c.hacia.copy(alpha = (if (enCadena) 1f else 0.9f) * apartado), 3f, c.fin)
                            else if (enCadena && f > 0f) drawCircle(c.desde.copy(alpha = f), 3f, c.fin)
                        }
                        if (aprobada != null && a.origen == aprobada && tiempo < 1f) descargaPor(c, tiempo)
                    }
                }
            }
        },
    )
}

/**
 * La luz al aprobar: el cable entero se enciende en verde de origen a
 * destino, se queda encendido un instante y se apaga. Con un halo ancho y
 * flojo debajo que hace de resplandor sin filtro.
 */
private fun DrawScope.descargaPor(c: CableDibujo, tiempo: Float) {
    val total = (ESPERA_DESCARGA + DURACION_DESCARGA).toFloat()
    val t = ((tiempo * total) - ESPERA_DESCARGA) / DURACION_DESCARGA
    if (t <= 0f) return
    val p = Movimiento.Mapa.DESCARGA.transform(t.coerceIn(0f, 1f))
    val lleno = min(1f, p / 0.58f)
    val alfa = if (p < 0.58f) 1f else 1f - (p - 0.58f) / 0.42f
    val trozo = c.hasta(lleno)
    drawPath(trozo, Paleta.verde.copy(alpha = 0.18f * alfa), style = Stroke(9f, cap = StrokeCap.Round))
    drawPath(trozo, Paleta.verde.copy(alpha = alfa), style = Stroke(2.5f, cap = StrokeCap.Round))
}
