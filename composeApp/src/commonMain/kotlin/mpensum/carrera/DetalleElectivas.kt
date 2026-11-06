package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.componentes.pulsable
import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import mpensum.logica.avanceDeGrupos
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tipo
import kotlin.math.ceil

/*
 * El detalle de un grupo de electivas: lo que llevas de lo que pide el título,
 * cuántas electivas te faltan más o menos y las casillas del plan donde van.
 * Tocar una casilla abre el selector de siempre, montado por la hoja encima de
 * ella. Si el grupo no tiene casillas en el plan, se listan las aprobadas.
 */

private val ROMANOS = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

/** Las UC más repetidas entre las electivas del grupo: con ellas se estima cuántas faltan */
private fun ucTipicas(grupo: Grupo): Int =
    grupo.asignaturas.mapNotNull { it.uc }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 3

@Composable
internal fun DetalleElectivas(estado: EstadoCarrera, clave: String, alElegir: (String) -> Unit) {
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == clave } ?: return
    val avance = remember(estado.marcas, grupo) { avanceDeGrupos(listOf(grupo), estado.marcas)[clave] } ?: return
    val meta = avance.meta ?: 0
    val tipicas = remember(grupo) { ucTipicas(grupo) }
    val faltan = (meta - avance.uc).coerceAtLeast(0)
    val electivas = ceil(faltan.toFloat() / tipicas).toInt()
    val casillas = remember(estado.pensum, clave) { estado.pensum.asignaturas.filter { it.esHueco && it.grupo == clave }.sortedBy { it.semestre ?: 99 } }
    val desde = casillas.mapNotNull { it.semestre }.minOrNull()

    CuerpoDetalle(
        izquierda = {
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText("${avance.uc}", style = estilo(44, Paleta.tinta, FontWeight.Light, 48).copy(letterSpacing = (-0.03).em))
                BasicText("de $meta UC", Modifier.padding(start = Espacio.xs, bottom = Espacio.xs), style = estilo(Tipo.xl, Paleta.tenue, interlinea = 22))
            }
            Box(Modifier.padding(top = Espacio.xs)) { BarraDeUc(avance.uc, meta, 6.dp) }
            BasicText(
                buildAnnotatedString {
                    if (faltan == 0) append("Ya cubres las UC que pide el título.")
                    else {
                        append(if (electivas == 1) "Te falta " else "Te faltan unas ")
                        withStyle(SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.SemiBold)) { append("$electivas ${if (electivas == 1) "electiva" else "electivas"}") }
                        append(" de $tipicas UC.")
                        if (desde != null) append(" Se abren desde el semestre $desde.")
                    }
                },
                Modifier.padding(top = Espacio.s),
                style = estilo(Tipo.m, Paleta.suave, interlinea = 20),
            )
        },
        derecha = {
            if (casillas.isNotEmpty()) {
                BasicText("EN TU PLAN", Modifier.padding(top = Espacio.l, bottom = Espacio.xs), style = versal(Paleta.tenue))
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    casillas.forEachIndexed { k, c ->
                        val puesta = estado.enCasilla(c.codigo)
                        val nombre = puesta?.nombre ?: if (casillas.size > 1) "${c.nombre} ${ROMANOS.getOrElse(k) { "${k + 1}" }}" else c.nombre
                        FichaElectiva(
                            uc = puesta?.uc ?: c.uc ?: tipicas,
                            titulo = nombre,
                            subtitulo = c.semestre?.let { "Semestre $it" } ?: "Sin semestre fijo",
                            accion = if (puesta == null) "Elegir ›" else "Cambiar ›",
                            alTocar = { alElegir(c.codigo) },
                        )
                    }
                }
            } else {
                BasicText("APROBADAS", Modifier.padding(top = Espacio.l, bottom = Espacio.xs), style = versal(Paleta.tenue))
                if (avance.elegidas.isEmpty()) {
                    BasicText("Aún no has aprobado ninguna.", style = estilo(Tipo.m, Paleta.suave, interlinea = 20))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                        avance.elegidas.forEach { a: Asignatura -> FichaElectiva(a.uc ?: tipicas, a.nombre, a.codigo, null, null) }
                    }
                }
                BasicText(
                    "Este plan no fija en qué semestre llevas estas electivas. Cuentan las que marques como aprobadas en el mapa o en la lista, hasta completar las UC del título; lo que pase de ahí se muestra, pero no suma.",
                    Modifier.padding(top = Espacio.m),
                    style = estilo(Tipo.s, Paleta.tenue, interlinea = 18),
                )
            }
        },
    )
}

/** Una ficha: cuadrito con las UC, el nombre con su semestre debajo y, si se puede tocar, a qué lleva */
@Composable
private fun FichaElectiva(uc: Int, titulo: String, subtitulo: String, accion: String?, alTocar: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (alTocar != null) Modifier.pulsable(alTocar, escala = 0.985f) else Modifier)
            .clip(RoundedCornerShape(Radio.Tarjeta))
            .background(Paleta.tinta.copy(alpha = 0.045f))
            .heightIn(min = Alto.grande)
            .padding(Espacio.s)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Column(
            Modifier.size(Alto.icono).clip(RoundedCornerShape(Radio.Chip)).background(Paleta.lienzo),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText("$uc", style = estilo(Tipo.m, Paleta.tinta, FontWeight.SemiBold, 15))
            BasicText("UC", style = estilo(8, Paleta.tenue, FontWeight.SemiBold, 9))
        }
        Column(Modifier.weight(1f)) {
            BasicText(titulo, style = estilo(Tipo.l, Paleta.tinta, interlinea = 20), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(subtitulo, Modifier.padding(top = Espacio.hilo), style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16))
        }
        if (accion != null) BasicText(accion, style = estilo(Tipo.s, Paleta.suave))
    }
}
