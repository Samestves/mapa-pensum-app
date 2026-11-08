package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.logica.CadenaExplicada
import mpensum.logica.Periodo
import mpensum.logica.cadenaExplicada
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * «No puedes atrasar» (docs/maquetas/proximo-y-cadena.html, opción A, «El
 * dominó»). Explica tres cosas sin párrafos:
 *
 *   1. Qué es: una frase («6 materias en fila: cada una abre la siguiente») y
 *      dos carriles de fichas por semestre. A tiempo caben antes de la línea
 *      del grado; si atrasas una, se corren como fichas de dominó y la última
 *      cae del otro lado, en rojo. Si la cadena tiene semestres libres, el
 *      hueco se los come y la fecha no se mueve: el dibujo lo enseña igual.
 *   2. Cuáles son: una fila por eslabón con su número (el de su ficha) y su
 *      período; la primera, la que toca inscribir, en azul.
 *   3. Qué hacer: inscribir la primera, repetir enseguida si repruebas y
 *      vigilar la rama que viene detrás.
 *
 * Todo sale de logica/Recomendacion.kt (cadenaExplicada), la misma cuenta que
 * da tu fecha. Acostado, el dominó va a lo ancho y debajo la cadena y qué
 * hacer, lado a lado. «Ver en el mapa» va en la cabecera.
 */

/** Las fichas: 28 dp de alto, radio 6, 4 dp entre columnas */
private val ALTO_FICHA = 28.dp
private val RADIO_FICHA = 6.dp
private val HUECO_FICHA = 4.dp

/** Lo que mide el dibujo: un renglón de rótulo por carril, las fichas y el eje de años */
private val RENGLON = 20.dp
private val ENTRE_CARRILES = 22.dp
private val ALTO_EJE = 26.dp

/** El número de cada eslabón en la lista: un círculo de 24 */
private val NUMERO = 24.dp

/** Cuántos períodos hay de `a` a `b` */
private fun entre(a: Periodo, b: Periodo): Int = (b.anio - a.anio) * 2 + (b.lapso - a.lapso)

@Composable
internal fun DetalleCadena(estado: EstadoCarrera, hoy: Periodo) {
    val sim = estado.simulador
    Calculado(clave = sim, listo = { sim.cadenaLista }, calculo = { sim.cadena }) { c ->
        ContenidoCadena(estado, c, hoy)
    }
}

@Composable
private fun ContenidoCadena(estado: EstadoCarrera, c: CadenaExplicada, hoy: Periodo) {
    if (c.eslabones.isEmpty()) return
    val nombre = remember(estado.pensum) { nombresDelPlan(estado.pensum) }

    CuerpoDetalle(
        arriba = { TarjetaDomino(c) },
        dosColumnas = true,
        izquierda = {
            val semestres = entre(c.periodos.first(), c.periodos.last()) + 1
            RotuloSeccion("LA CADENA", derecha = if (semestres == 1) "1 SEMESTRE" else "$semestres SEMESTRES")
            TarjetaLista {
                c.eslabones.forEachIndexed { k, codigo ->
                    val primera = k == 0
                    FilaLista(
                        titulo = nombre(codigo),
                        nota = nota(c.periodos[k].rango()),
                        colorNota = if (primera) Paleta.azul else Paleta.tenue,
                        pesoNota = if (primera) FontWeight.Medium else FontWeight.Normal,
                        primera = primera,
                        icono = { Numero(k + 1, primera) },
                    )
                }
            }
        },
        derecha = { QueHacer(c, hoy, nombre) },
    )
}

/** La tarjeta de arriba: la frase que dice qué es la cadena y el dominó que enseña qué pasa si atrasas una */
@Composable
private fun TarjetaDomino(c: CadenaExplicada) {
    val n = c.eslabones.size
    val fuerte = SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)
    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        BasicText(
            buildAnnotatedString {
                withStyle(fuerte) { append(if (n == 1) "1 materia fija tu fecha" else "$n materias en fila:") }
                if (n > 1) append(" cada una abre la siguiente")
                append(
                    when (c.holgura) {
                        0 -> if (n > 1) " y no queda semestre libre." else ": no queda semestre libre."
                        1 -> if (n > 1) "; te queda 1 semestre libre." else ": te queda 1 semestre libre."
                        else -> if (n > 1) "; te quedan ${c.holgura} semestres libres." else ": te quedan ${c.holgura} semestres libres."
                    },
                )
            },
            style = estiloCuerpo(),
        )
        Domino(c, Modifier.padding(top = Espacio.m))
    }
}

/**
 * Los dos carriles. Cada columna es un semestre desde el primer eslabón. Arriba
 * las fichas en su sitio, la primera en azul, y el birrete pasada la línea del
 * grado. Abajo, el hueco del semestre perdido y las fichas corridas: cada una
 * se corre si la de antes la empuja, y un semestre libre absorbe el empujón.
 * Las que caen pasada la línea, en rojo, como la fecha de ese carril.
 */
@Composable
private fun Domino(c: CadenaExplicada, modifier: Modifier) {
    val final = c.final ?: return
    val inicio = c.periodos.first()
    val antes = c.periodos.map { entre(inicio, it) }
    // La primera se corre uno; cada una de las demás, solo si la de antes la alcanza
    val despues = mutableListOf<Int>().apply {
        antes.forEachIndexed { k, i -> add(if (k == 0) i + 1 else maxOf(i, last() + 1)) }
    }
    val limite = entre(inicio, final)
    val columnas = maxOf(despues.last(), limite + 1) + 1
    val tarde = c.siAtrasas?.let { it > final } ?: false

    // Rótulos, una ficha por eslabón en cada carril y los años: más que la caché de 8 de serie, que se vaciaría en cada dibujo
    val medidor = rememberTextMeasurer(cacheSize = 48)
    val tinta = Paleta.tinta
    val azul = Paleta.azul
    val rojo = Paleta.rojo
    val panel = Paleta.panel
    val apagada = Paleta.apagada
    val rotulo = estiloNota()
    val fecha = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Light, fontSize = 15.sp, color = tinta, fontFeatureSettings = "tnum")
    val numero = TextStyle(fontFamily = inter(), fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFeatureSettings = "tnum")
    val anio = TextStyle(fontFamily = jost(), fontSize = 11.sp, color = apagada)
    val alto = RENGLON * 2 + ALTO_FICHA * 2 + ENTRE_CARRILES + ALTO_EJE

    Canvas(
        modifier.fillMaxWidth().height(alto).semantics {
            contentDescription = "A tiempo te gradúas en ${final.mesYAnio()}; si atrasas una, en ${c.siAtrasas?.mesYAnio() ?: final.mesYAnio()}"
        },
    ) {
        val u = 1.dp.toPx()
        val g = HUECO_FICHA.toPx()
        val ancho = (size.width - g * (columnas - 1)) / columnas
        val th = ALTO_FICHA.toPx()
        val r = CornerRadius(RADIO_FICHA.toPx())
        fun x(i: Int) = i * (ancho + g)
        val y1 = RENGLON.toPx()
        val y2 = y1 + th + ENTRE_CARRILES.toPx() + RENGLON.toPx()
        val ye = y2 + th + 8 * u

        fun texto(t: String, estilo: TextStyle, izq: Float, base: Float, derecha: Boolean = false, centro: Boolean = false) {
            val m = medidor.measure(t, estilo)
            val px = when {
                derecha -> izq - m.size.width
                centro -> izq - m.size.width / 2f
                else -> izq
            }
            drawText(m, topLeft = Offset(px, base - m.firstBaseline))
        }

        // Los rótulos de cada carril y su fecha
        texto("A tiempo", rotulo, 0f, y1 - 6 * u)
        texto(final.mesYAnio(), fecha, size.width, y1 - 6 * u, derecha = true)
        texto("Si atrasas una", rotulo, 0f, y2 - 6 * u)
        texto((c.siAtrasas ?: final).mesYAnio(), fecha.copy(color = if (tarde) rojo else tinta, fontWeight = if (tarde) FontWeight.Medium else FontWeight.Light), size.width, y2 - 6 * u, derecha = true)

        // La línea del grado, punteada, entre el último semestre a tiempo y el siguiente
        val xl = x(limite + 1) - g / 2
        val punteada = PathEffect.dashPathEffect(floatArrayOf(2 * u, 3 * u))
        listOf(y1, y2).forEach { y -> drawLine(tinta.copy(alpha = 0.3f), Offset(xl, y - 4 * u), Offset(xl, y + th + 4 * u), u, pathEffect = punteada) }

        // Carril de arriba: las fichas en su sitio y el birrete pasada la línea
        antes.forEachIndexed { k, i ->
            val color = if (k == 0) azul else tinta
            drawRoundRect(color, Offset(x(i), y1), Size(ancho, th), r)
            texto("${k + 1}", numero.copy(color = panel), x(i) + ancho / 2, y1 + th / 2 + 4.5f * u, centro = true)
        }
        val lado = 18 * u
        icono(Lucide.birrete, tinta, x(limite + 1) + (ancho - lado) / 2, y1 + (th - lado) / 2, lado, 1.5f * 24f / 18f)

        // Carril de abajo: el hueco del semestre perdido y las fichas corridas
        val trazo = 1.5f * u
        drawRoundRect(
            tinta.copy(alpha = 0.35f), Offset(x(antes[0]) + trazo / 2, y2 + trazo / 2), Size(ancho - trazo, th - trazo),
            CornerRadius(r.x - trazo / 2), style = Stroke(trazo, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u))),
        )
        despues.forEachIndexed { k, i ->
            val fuera = i > limite
            drawRoundRect(if (fuera) rojo else tinta.copy(alpha = 0.2f), Offset(x(i), y2), Size(ancho, th), r)
            texto("${k + 1}", numero.copy(color = if (fuera) Color.White else tinta), x(i) + ancho / 2, y2 + th / 2 + 4.5f * u, centro = true)
        }

        // El eje: un hilo y, bajo cada año, su número; el año que solo existe por atrasarte, en rojo
        drawLine(tinta.copy(alpha = 0.09f), Offset(0f, ye), Offset(size.width, ye), u)
        (0 until columnas).groupBy { inicio.mas(it).anio }.forEach { (a, cols) ->
            val cx = (x(cols.first()) + x(cols.last()) + ancho) / 2
            drawLine(tinta.copy(alpha = 0.3f), Offset(cx, ye), Offset(cx, ye + 4 * u), u)
            val soloTarde = cols.all { it > limite }
            texto("$a", anio.copy(color = if (soloTarde && tarde) rojo else apagada), cx, ye + 17 * u, centro = true)
        }
    }
}

/** El número de un eslabón: el de su ficha. El primero, lleno de azul: es el que toca inscribir */
@Composable
private fun Numero(n: Int, primero: Boolean) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(NUMERO).then(
                if (primero) Modifier.background(Paleta.azul, CircleShape) else Modifier.border(1.5.dp, Paleta.tinta.copy(alpha = 0.28f), CircleShape),
            ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("$n", style = estiloNota(if (primero) Paleta.panel else Paleta.tinta, FontWeight.Medium).copy(fontSize = 12.sp))
        }
    }
}

/** QUÉ HACER: la primera, repetir enseguida si repruebas y la rama que viene detrás */
@Composable
private fun QueHacer(c: CadenaExplicada, hoy: Periodo, nombre: (String) -> String) {
    val primera = c.periodos.first()
    RotuloSeccion("QUÉ HACER")
    TarjetaLista {
        FilaLista(
            titulo = "Inscribe ${nombre(c.eslabones.first())}",
            nota = nota(if (primera == hoy.mas(1)) primera.rango() else "En ${primera.rango()}"),
            colorNota = Paleta.azul,
            pesoNota = FontWeight.Medium,
            primera = true,
            icono = { Pastilla(IconosHoja.calendarioListo, azul = true) },
        )
        FilaLista(
            titulo = "Si repruebas una, repítela ya",
            nota = nota(
                when (c.holgura) {
                    0 -> "Cada semestre de espera, 6 meses más"
                    1 -> "Tienes 1 semestre de margen"
                    else -> "Tienes ${c.holgura} semestres de margen"
                },
            ),
            primera = false,
            icono = { Pastilla(IconosHoja.reiniciar) },
        )
        c.vigilar?.let { (codigo, margen) ->
            FilaLista(
                titulo = "Vigila ${nombre(codigo)}",
                nota = nota(if (margen == 1) "Su rama tiene 1 semestre de margen" else "Su rama tiene $margen semestres de margen"),
                primera = false,
                icono = { Pastilla(IconosHoja.relojArena) },
            )
        }
    }
}
