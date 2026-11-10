package mpensum.carrera

import mpensum.componentes.superficie
import mpensum.componentes.RotuloSeccion
import mpensum.componentes.TarjetaLista
import mpensum.componentes.icono
import mpensum.componentes.IconosHoja
import mpensum.componentes.DosALoAncho
import mpensum.componentes.CifraFila
import mpensum.componentes.FilaLista
import mpensum.componentes.nota
import mpensum.componentes.Pastilla
import mpensum.componentes.estiloRotulo
import mpensum.componentes.recorteCap
import mpensum.componentes.estiloNota
import mpensum.componentes.Fecha
import mpensum.componentes.IconoDeFila
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.CifraConUnidad
import mpensum.componentes.esApaisado
import mpensum.componentes.rememberPartesListas
import mpensum.logica.Motivo
import mpensum.logica.Periodo
import mpensum.logica.ProximoSemestre
import mpensum.logica.Recomendacion
import mpensum.tema.Espacio
import mpensum.tema.Paleta

/*
 * «Tu próximo semestre» (docs/maquetas/proximo-y-cadena.html, opción A):
 *
 *   1. La respuesta: cuántas materias, sus UC en cápsulas (una por materia,
 *      ancha según sus UC y con el relleno de su motivo) y tu fecha.
 *   2. Las materias agrupadas por motivo, dicho una vez en el rótulo, con la
 *      misma muestra de cápsula que en la barra: las que fijan tu fecha, las
 *      que te abren camino y las que completan tu carga.
 *   3. La salida si no puedes con todas y lo que también podrías inscribir.
 *
 * Todo sale de la misma cuenta que da tu fecha (logica/Recomendacion.kt): no
 * hay otro plan, se explica el mismo. Acostado, la respuesta va a lo ancho y
 * los grupos en dos columnas si caben. «Ver en el mapa» va en la cabecera.
 */

/** Las cápsulas: 14 dp de alto, radio 2,5; 2 dp entre materias y 5 entre motivos */
private val ALTO_CAPSULA = 14.dp
private const val RADIO_CAPSULA = 2.5f
private val HUECO_CAPSULA = 2.dp
private val HUECO_MOTIVO = 5.dp

/** La muestra del rótulo: la misma pieza, más pequeña */
private val MUESTRA_ANCHO = 14.dp
private val MUESTRA_ALTO = 10.dp

/** Con qué se pinta cada motivo, siempre en tinta: lleno la que fija tu fecha, a medias las que abren y en contorno las demás */
private fun DrawScope.capsula(motivo: Motivo, x: Float, ancho: Float, alto: Float, tinta: androidx.compose.ui.graphics.Color) {
    val r = CornerRadius(RADIO_CAPSULA * density)
    when (motivo) {
        Motivo.FIJA_FECHA -> drawRoundRect(tinta, Offset(x, 0f), Size(ancho, alto), r)
        Motivo.ABRE_CAMINO -> drawRoundRect(tinta.copy(alpha = 0.45f), Offset(x, 0f), Size(ancho, alto), r)
        else -> {
            val g = 1.5.dp.toPx()
            drawRoundRect(tinta.copy(alpha = 0.7f), Offset(x + g / 2, g / 2), Size(ancho - g, alto - g), CornerRadius(r.x - g / 2), style = Stroke(g))
        }
    }
}

@Composable
internal fun DetalleProximo(estado: EstadoCarrera, hoy: Periodo) {
    val sim = estado.simulador
    Calculado(clave = sim, listo = { sim.proximoListo }, calculo = { sim.proximo }) { proximo ->
        ContenidoProximo(estado, proximo)
    }
}

@Composable
private fun ContenidoProximo(estado: EstadoCarrera, proximo: ProximoSemestre) {
    val pensum = estado.pensum
    val nombre = remember(pensum) { nombresDelPlan(pensum) }
    val uc = remember(pensum) { ucsDelPlan(pensum) }
    val esCasilla = remember(pensum) { esCasillaDelPlan(pensum) }
    val final = estado.graduacion.periodoFinal
    val grupos = remember(proximo) {
        listOf(Motivo.FIJA_FECHA, Motivo.ABRE_CAMINO, Motivo.COMPLETA_CARGA)
            .map { m -> m to proximo.recomendadas.filter { it.motivo == m } }
            .filter { it.second.isNotEmpty() }
    }

    CuerpoDetalle(
        arriba = { TarjetaProximo(proximo, grupos, final, uc) },
        dosColumnas = true,
        izquierda = {
            // Un grupo por cuadro: son la mayor parte de las filas de la pantalla
            val listos = rememberPartesListas(grupos.size)
            grupos.take(listos).forEach { (motivo, recomendadas) -> GrupoMotivo(motivo, recomendadas, nombre, uc, esCasilla) }
        },
        derecha = {
            SiNoPuedes(proximo, estado.graduacion.carga, final, nombre)
            if (proximo.opcionales.isNotEmpty()) {
                RotuloSeccion("TAMBIÉN PUEDES")
                TarjetaLista {
                    proximo.opcionales.forEachIndexed { k, r ->
                        FilaLista(
                            titulo = nombre(r.codigo),
                            nota = nota(queAbre(r.abre, nombre)),
                            primera = k == 0,
                            icono = { IconoDeFila(IconosHoja.candadoAbierto, Paleta.apagada) },
                            valor = { CifraFila("${uc(r.codigo)}", "UC") },
                        )
                    }
                }
            }
        },
    )
}

/**
 * La respuesta: «6 materias» y tu fecha, «16 UC» y «Te gradúas» debajo, y la
 * barra de cápsulas. Acostado va en una sola fila: cifra, barra y fecha.
 */
@Composable
private fun TarjetaProximo(proximo: ProximoSemestre, grupos: List<Pair<Motivo, List<Recomendacion>>>, final: Periodo?, uc: (String) -> Int) {
    val n = proximo.recomendadas.size
    val total = proximo.recomendadas.sumOf { uc(it.codigo) }
    val cifra: @Composable () -> Unit = {
        Column {
            CifraConUnidad("$n", if (n == 1) "materia" else "materias", 40.sp, Modifier.recorteCap(40.sp))
            BasicText(ucTexto(total), Modifier.padding(top = Espacio.s), style = estiloNota())
        }
    }
    val fecha: @Composable () -> Unit = {
        if (final != null) {
            Column(horizontalAlignment = Alignment.End) {
                Fecha(final.mesYAnio())
                BasicText("Te gradúas", Modifier.padding(top = Espacio.xs), style = estiloNota())
            }
        }
    }
    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        if (esApaisado()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                cifra()
                BarraProximo(grupos, uc, Modifier.weight(1f).padding(horizontal = Espacio.l))
                fecha()
            }
        } else {
            DosALoAncho(hueco = Espacio.m, vertical = Espacio.m, izquierda = cifra, derecha = fecha)
            BarraProximo(grupos, uc, Modifier.padding(top = Espacio.m))
        }
    }
}

/** «16 UC» con la cifra en tinta */
@Composable
private fun ucTexto(uc: Int): AnnotatedString {
    val tinta = Paleta.tinta
    return buildAnnotatedString {
        withStyle(SpanStyle(color = tinta, fontWeight = FontWeight.Medium)) { append("$uc") }
        append(" UC")
    }
}

/** Una cápsula por materia, ancha según sus UC, agrupadas por motivo */
@Composable
private fun BarraProximo(grupos: List<Pair<Motivo, List<Recomendacion>>>, uc: (String) -> Int, modifier: Modifier) {
    val piezas = grupos.flatMap { (m, rs) -> rs.mapIndexed { k, r -> Triple(m, uc(r.codigo).coerceAtLeast(1), k == 0) } }
    if (piezas.isEmpty()) return
    val tinta = Paleta.tinta
    Canvas(modifier.fillMaxWidth().height(ALTO_CAPSULA).semantics { contentDescription = "${piezas.size} materias, ${piezas.sumOf { it.second }} UC" }) {
        val hueco = HUECO_CAPSULA.toPx()
        val entreMotivos = HUECO_MOTIVO.toPx()
        val separaciones = (piezas.size - 1) * hueco + (grupos.size - 1) * (entreMotivos - hueco)
        val porUc = (size.width - separaciones) / piezas.sumOf { it.second }
        var x = 0f
        piezas.forEachIndexed { k, (motivo, ucs, abreGrupo) ->
            if (k > 0) x += if (abreGrupo) entreMotivos else hueco
            val ancho = ucs * porUc
            capsula(motivo, x, ancho, size.height, tinta)
            x += ancho
        }
    }
}

/** Un grupo: el rótulo con su muestra y sus UC, y una fila por materia con el porqué que le toca a cada una */
@Composable
private fun ColumnScope.GrupoMotivo(motivo: Motivo, recomendadas: List<Recomendacion>, nombre: (String) -> String, uc: (String) -> Int, esCasilla: (String) -> Boolean) {
    val (rotulo, icono) = when (motivo) {
        Motivo.FIJA_FECHA -> "FIJA TU FECHA" to IconosHoja.eslabon
        Motivo.ABRE_CAMINO -> "TE ABREN CAMINO" to IconosHoja.candadoAbierto
        else -> "COMPLETAN TU CARGA" to IconosHoja.candadoAbierto
    }
    val tinta = Paleta.tinta
    Row(Modifier.fillMaxWidth().padding(start = Espacio.m, end = Espacio.m, top = Espacio.xl, bottom = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(MUESTRA_ANCHO, MUESTRA_ALTO)) { capsula(motivo, 0f, size.width, size.height, tinta) }
        BasicText(rotulo, Modifier.weight(1f).padding(start = Espacio.xs), style = estiloRotulo(), maxLines = 1)
        BasicText("${recomendadas.sumOf { uc(it.codigo) }} UC", style = estiloRotulo(), maxLines = 1)
    }
    TarjetaLista {
        recomendadas.forEachIndexed { k, r ->
            val porque = when {
                esCasilla(r.codigo) -> "Tú eliges cuál"
                motivo == Motivo.FIJA_FECHA -> "No puede esperar"
                motivo == Motivo.ABRE_CAMINO -> queAbre(r.abre, nombre)
                else -> puedeEsperar(r.margen)
            }
            FilaLista(
                titulo = nombre(r.codigo),
                nota = nota(porque),
                colorNota = if (motivo == Motivo.FIJA_FECHA) Paleta.tinta else Paleta.tenue,
                pesoNota = if (motivo == Motivo.FIJA_FECHA) FontWeight.Medium else FontWeight.Normal,
                primera = k == 0,
                icono = { IconoDeFila(icono, Paleta.azul) },
                valor = { CifraFila("${uc(r.codigo)}", "UC") },
            )
        }
    }
}

/** Lo que abre una materia: «→ Optimización de Operaciones», o la primera «y 2 más» */
private fun queAbre(abre: List<String>, nombre: (String) -> String): String = when (abre.size) {
    0 -> "No abre otras"
    1 -> "→ ${nombre(abre[0])}"
    else -> "→ ${nombre(abre[0])} y ${abre.size - 1} más"
}

/** «Puede esperar 1 semestre», «Puede esperar 3 semestres» */
internal fun puedeEsperar(margen: Int): String = if (margen == 1) "Puede esperar 1 semestre" else "Puede esperar $margen semestres"

/**
 * ¿SOLO PUEDES CON 5?: la que puedes dejar sin mover tu fecha (comprobado
 * simulando) y tu fecha si metes una menos cada semestre. Solo lo que haya.
 */
@Composable
private fun SiNoPuedes(proximo: ProximoSemestre, carga: Int, final: Periodo?, nombre: (String) -> String) {
    val dejar = proximo.dejar?.takeIf { final != null }
    val conUnaMenos = proximo.conUnaMenos?.takeIf { carga > 1 }
    if (proximo.recomendadas.size < 2 || (dejar == null && conUnaMenos == null)) return
    val fuerte = SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)
    RotuloSeccion("¿SOLO PUEDES CON ${proximo.recomendadas.size - 1}?")
    TarjetaLista {
        if (dejar != null && final != null) {
            FilaLista(
                titulo = "Deja ${nombre(dejar)}",
                nota = buildAnnotatedString {
                    append("Sigues en ")
                    withStyle(fuerte) { append(final.mesYAnio()) }
                },
                primera = true,
                icono = { Pastilla(IconosHoja.calendarioListo) },
            )
        }
        if (conUnaMenos != null) {
            FilaLista(
                titulo = "Con ${carga - 1} cada semestre",
                nota = buildAnnotatedString {
                    append("Te gradúas en ")
                    withStyle(fuerte) { append(conUnaMenos.mesYAnio()) }
                },
                primera = dejar == null,
                icono = { Pastilla(IconosHoja.relojArena) },
            )
        }
    }
}
