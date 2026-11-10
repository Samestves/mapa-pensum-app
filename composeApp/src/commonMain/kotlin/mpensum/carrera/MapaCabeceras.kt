package mpensum.carrera

import mpensum.mapa.TiposMapa
import mpensum.mapa.Margen
import mpensum.mapa.FranjaColocada
import mpensum.mapa.Franja
import mpensum.mapa.DatosCabecera
import mpensum.mapa.CabeceraSemestre
import mpensum.mapa.ALTO_ENCABEZADO
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import mpensum.carrera.EstadoCarrera
import mpensum.logica.Situacion
import mpensum.logica.marcasDeSemestres
import mpensum.tema.Paleta

/* Las cabeceras de cada semestre y los rótulos de la franja de electivas */

/** La cabecera de cada semestre: su número, lo que cuenta y el aro que lo aprueba entero */
@Composable
internal fun CabecerasSemestres(estado: EstadoCarrera, tipos: TiposMapa, medidor: TextMeasurer, entrada: EntradaDelMapa, listos: Int, alAlternarSemestre: (Int) -> Unit) {
    val layout = estado.layout
    val sit = estado.situaciones
    val porColumna = remember(layout) { layout.nodos.groupBy { it.semestre } }
    val cuentas = remember(layout, estado.estados, estado.casillas) { marcasDeSemestres(layout.nodos.map { it.asignatura }, estado::enCasilla, estado.estados) }
    layout.columnas.forEach { columna ->
        // Cada cabecera entra con la primera tarjeta de su semestre
        if (entrada.loteDe(columna.semestre) >= listos) return@forEach
        val delSemestre = porColumna[columna.semestre].orEmpty()
        val materias = delSemestre.map { estado.materiaDe(it) }
        val situaciones = materias.map { m -> m?.let { sit[it.codigo] } ?: Situacion.LEJANA }
        CabeceraSemestre(
            DatosCabecera(
                semestre = columna.semestre,
                x = columna.x.toFloat(),
                uc = materias.sumOf { it?.uc ?: 0 },
                total = delSemestre.size,
                hechas = situaciones.count { it == Situacion.HECHA },
                cursando = situaciones.count { it == Situacion.CURSANDO },
                inscribibles = situaciones.count { it == Situacion.INSCRIBIBLE },
                casilla = cuentas[columna.semestre],
            ),
            tipos, medidor,
            compacta = layout.altoEncabezado < ALTO_ENCABEZADO,
            alAlternar = { alAlternarSemestre(columna.semestre) },
        )
    }
}

/** El rótulo de cada grupo en la franja de electivas, y la raya que la separa de los semestres */
@Composable
internal fun RotulosFranja(franja: FranjaColocada, estado: EstadoCarrera, tipos: TiposMapa) {
    val primera = franja.filas.firstOrNull() ?: return
    Spacer(
        Modifier
            .offset(Margen.IZQUIERDA.toFloat().dp, (primera.y - Franja.CORREDOR / 2).toFloat().dp)
            .size((estado.layout.ancho - Margen.IZQUIERDA - Margen.DERECHA).toFloat().dp, 1.dp)
            .background(Paleta.tinta.copy(alpha = 0.08f)),
    )
    franja.filas.forEach { f ->
        val datos = listOfNotNull(
            if (f.elegidas > 0) "${f.elegidas} EN TU MAPA" else "ELIGE LAS TUYAS",
            "${f.opciones} ${if (f.opciones == 1) "OPCIÓN" else "OPCIONES"}",
            f.cuota?.let { "ELIGE $it UC" },
        )
        Column(Modifier.offset(f.x.toFloat().dp, (f.y + 14 - 11.5 * 0.93).toFloat().dp)) {
            BasicText(f.titulo.uppercase(), style = tipos.titulo)
            BasicText(datos.joinToString(" · "), Modifier.offset(y = 6.dp), style = tipos.datos)
        }
    }
}
