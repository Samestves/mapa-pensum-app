package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.logica.Periodo
import mpensum.logica.periodosDeCadena
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/*
 * «No puedes atrasar»: la cadena que fija tu fecha. Arriba, lo que cuesta
 * atrasar una (tu fecha a tiempo frente a la de atrasarla). Debajo, la vía:
 * cada eslabón en su período, el primero en azul (inscríbela ya) y el último
 * con el birrete. Acostado: la comparación y el botón a la izquierda, la vía
 * a la derecha.
 */

/** La columna del período, a la izquierda de la vía */
private val ANCHO_CUANDO = 64.dp

/** La vía: su columna, su nodo y su trazo */
private val ANCHO_VIA = 24.dp
private val NODO = 12.dp
private val TRAZO_VIA = 1.5.dp

@Composable
internal fun DetalleCadena(estado: EstadoCarrera, hoy: Periodo, alVerEnMapa: (List<String>) -> Unit) {
    val g = estado.graduacion
    val cadena = g.cuelloDeBotella
    val nombre = remember(estado.pensum) { nombresDelPlan(estado.pensum) }
    val periodos = remember(g, hoy) { periodosDeCadena(g, hoy) }
    // Si el plan dura más que la cadena, hay margen: atrasar una no mueve la fecha hasta gastarlo
    val holgura = (g.plan.size - cadena.size).coerceAtLeast(0)
    val final = g.periodoFinal
    val boton: @Composable () -> Unit = { BotonTinta("Verla en el mapa", { alVerEnMapa(cadena) }, Modifier.padding(top = Espacio.xl)) }

    CuerpoDetalle(
        izquierda = {
            if (final != null) {
                Comparacion(Modifier.fillMaxWidth().superficie(), izquierda = {
                    ColumnaComparacion("A TIEMPO", null) { Fecha(final.mesYAnio()) }
                }, derecha = {
                    if (holgura == 0) {
                        ColumnaComparacion("SI ATRASAS UNA", "6 meses después") { Fecha(final.mas(1).mesYAnio()) }
                    } else {
                        ColumnaComparacion("SI ATRASAS UNA", if (holgura == 1) "Tienes 1 semestre de margen" else "Tienes $holgura semestres de margen") { Fecha(final.mesYAnio()) }
                    }
                })
            }
        },
        derecha = {
            Column(Modifier.padding(top = Espacio.xl)) {
                cadena.forEachIndexed { k, c ->
                    val primera = k == 0
                    val ultima = k == cadena.lastIndex && k > 0
                    val periodo = periodos[k]
                    PasoCadena(
                        periodo = periodo,
                        nombre = nombre(c),
                        nota = when {
                            primera -> if (periodo == hoy.mas(1)) "Inscríbela ya" else "Inscríbela en ${periodo.rango()}"
                            ultima -> "Y te gradúas"
                            k == 1 -> "La abre ${nombre(cadena[0])}"
                            else -> null
                        },
                        tipo = when {
                            primera -> TipoPaso.PRIMERO
                            ultima -> TipoPaso.ULTIMO
                            else -> TipoPaso.MEDIO
                        },
                        sigue = k < cadena.lastIndex,
                    )
                }
            }
            boton()
        },
    )
}

private enum class TipoPaso { PRIMERO, MEDIO, ULTIMO }

/**
 * Un paso de la vía: el período a la derecha de su columna, el nodo sobre la
 * vía y el nombre con su nota. La vía baja desde debajo del nodo hasta el
 * siguiente paso.
 */
@Composable
private fun PasoCadena(periodo: Periodo, nombre: String, nota: String?, tipo: TipoPaso, sigue: Boolean) {
    val azul = Paleta.azul
    val tinta = Paleta.tinta
    val panel = Paleta.panel
    val primera = tipo == TipoPaso.PRIMERO
    // Con letra grande, el período («Ene – Jun») pide su columna más ancha para no partirse en tres
    val anchoCuando = ANCHO_CUANDO * maxOf(1f, LocalDensity.current.fontScale)
    val rango = periodo.rango()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 52.dp).semantics(mergeDescendants = true) {}) {
        BasicText(
            rango.substringBeforeLast(' ') + "\n" + rango.substringAfterLast(' '),
            Modifier.width(anchoCuando).padding(top = Espacio.hilo),
            style = estiloNota(if (primera) azul else Paleta.tenue, if (primera) FontWeight.Medium else FontWeight.Normal)
                .copy(fontSize = 12.sp, textAlign = TextAlign.End),
        )
        Box(
            Modifier.padding(horizontal = Espacio.s).width(ANCHO_VIA).fillMaxHeight().drawBehind {
                if (sigue) {
                    val x = size.width / 2f
                    drawLine(tinta.copy(alpha = 0.16f), Offset(x, 18.dp.toPx()), Offset(x, size.height + 2.dp.toPx()), TRAZO_VIA.toPx())
                }
            },
            contentAlignment = Alignment.TopCenter,
        ) {
            when (tipo) {
                TipoPaso.ULTIMO -> IconoLucide(Lucide.birrete, tinta, 18.dp, Modifier.padding(top = 1.dp).background(panel))
                else -> Canvas(Modifier.padding(top = Espacio.xxs).size(NODO)) {
                    val r = size.minDimension / 2f
                    if (primera) {
                        drawCircle(azul.copy(alpha = 0.18f), r + 4.dp.toPx())
                        drawCircle(azul, r)
                    } else {
                        val t = TRAZO_VIA.toPx()
                        drawCircle(panel, r)
                        drawCircle(tinta.copy(alpha = 0.42f), r - t / 2f, style = Stroke(t))
                    }
                }
            }
        }
        Column(Modifier.weight(1f).padding(bottom = Espacio.m)) {
            BasicText(nombre, style = estiloFila())
            if (nota != null) BasicText(nota, style = estiloNota(if (primera) azul else Paleta.tenue))
        }
    }
}
