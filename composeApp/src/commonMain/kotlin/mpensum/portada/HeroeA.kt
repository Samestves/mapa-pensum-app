package mpensum.portada

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.tema.Espacio

/**
 * La tarjeta de tu carrera, «cifra protagonista». Un foco: el 41 % grande. Encima, el nombre; debajo,
 * una barra fina de extremo a extremo y la misma frase de la lista. Luego la
 * acción. Sin carrera o sin marcas no hay cifra que enseñar: queda el nombre,
 * la invitación y el botón.
 */
@Composable
internal fun HeroeA(d: DatosHeroe, entrada: () -> Float, alPulsar: () -> Unit, alHorario: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(RELLENO_HEROE)) {
        NombreHeroe(d)
        val r = d.resumen
        if (r != null) {
            CifraHeroe(r.porcentaje, 64.sp, entrada, Modifier.padding(top = Espacio.l))
            BarraHeroe(d.fraccion, entrada, Modifier.padding(top = Espacio.l))
        }
        DatoHeroe(d.linea(), Modifier.padding(top = if (r != null) Espacio.s else 2.dp))
        AccionesHeroe(d, alPulsar, alHorario, Modifier.padding(top = Espacio.l))
    }
}
