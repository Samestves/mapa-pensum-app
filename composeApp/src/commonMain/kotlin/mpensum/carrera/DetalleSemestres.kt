package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.logica.CuentaSemestre
import mpensum.logica.marcasDeSemestres
import mpensum.logica.materiasDeSemestre
import mpensum.tema.Alto
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo

/*
 * «Por semestres»: un renglón por semestre con su anillito (lo aprobado), lo
 * que llevas y lo que cursas. Tocar uno lleva al mapa, encuadrando sus materias.
 * Acostado se parte en dos columnas de renglones.
 */

/** Cómo va cada semestre, en orden: las casillas de electiva cuentan como una materia más */
internal fun cuentasPorSemestre(estado: EstadoCarrera): List<Pair<Int, CuentaSemestre>> =
    marcasDeSemestres(estado.pensum.asignaturas, estado::enCasilla, estado.estados).toList().sortedBy { it.first }

@Composable
internal fun DetalleSemestres(estado: EstadoCarrera, alVerEnMapa: (List<String>) -> Unit) {
    val cuentas = remember(estado.marcas, estado.casillas) { cuentasPorSemestre(estado) }
    val renglon: @Composable (Pair<Int, CuentaSemestre>) -> Unit = { (k, c) ->
        RenglonSemestre(k, c) { alVerEnMapa(materiasDeSemestre(estado.pensum.asignaturas, k, estado::enCasilla)) }
    }
    if (esApaisado()) {
        val mitad = (cuentas.size + 1) / 2
        CuerpoDetalle(
            izquierda = { cuentas.take(mitad).forEach { renglon(it) } },
            derecha = { cuentas.drop(mitad).forEach { renglon(it) } },
        )
    } else {
        CuerpoDetalle(izquierda = { cuentas.forEach { renglon(it) } }, derecha = {})
    }
}

@Composable
private fun RenglonSemestre(semestre: Int, cuenta: CuentaSemestre, alTocar: () -> Unit) {
    val fraccion = if (cuenta.total > 0) cuenta.hechas.toFloat() / cuenta.total else 0f
    Row(
        Modifier.fillMaxWidth().pulsable(alTocar, escala = 0.99f).heightIn(min = Alto.fila + Espacio.xs).padding(vertical = Espacio.xs, horizontal = Espacio.xxs).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        AnilloSemestre(fraccion)
        Column(Modifier.weight(1f)) {
            BasicText("Semestre $semestre", style = estilo(Tipo.l, Paleta.tinta, interlinea = 20))
            BasicText(
                buildAnnotatedString {
                    append("${cuenta.hechas} de ${cuenta.total} ${if (cuenta.total == 1) "aprobada" else "aprobadas"}")
                    if (cuenta.cursando > 0) {
                        append(" · ")
                        withStyle(SpanStyle(color = Paleta.tinta)) { append("${cuenta.cursando} cursando") }
                    }
                },
                Modifier.padding(top = Espacio.hilo),
                style = estilo(Tipo.xs, Paleta.tenue, interlinea = 16),
            )
        }
        IconoTrazo(Lucide.siguiente, Paleta.tenue, 16.dp, 2.2f)
    }
}

/** El anillito de 26 dp: pista al 10 % y el arco de lo aprobado, de 3 dp con puntas redondas */
@Composable
private fun AnilloSemestre(fraccion: Float) {
    val tinta = Paleta.tinta
    Canvas(Modifier.size(26.dp)) {
        val grueso = 3.dp.toPx()
        val lado = size.width - grueso
        val arriba = Offset(grueso / 2f, grueso / 2f)
        drawArc(tinta.copy(alpha = 0.10f), 0f, 360f, false, arriba, Size(lado, lado), style = Stroke(grueso))
        if (fraccion > 0f) drawArc(tinta, -90f, 360f * fraccion, false, arriba, Size(lado, lado), style = Stroke(grueso, cap = StrokeCap.Round))
    }
}
