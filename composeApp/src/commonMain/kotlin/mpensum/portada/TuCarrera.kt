package mpensum.portada

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.navegacion.Vista
import mpensum.datos.Carrera
import mpensum.logica.ResumenPortada
import mpensum.tema.Espacio

/**
 * Tu carrera, arriba del home: una tarjeta, un trabajo, volver a tu carrera.
 * Va en la superficie de la hoja del progreso (sin cristal, sin filo de color
 * y sin la silueta de puntos, que se queda en las otras carreras). Dentro,
 * la «cifra protagonista»: un foco, el 41 % grande. Encima, el nombre; debajo,
 * una barra fina de extremo a extremo y la misma frase de la lista. Luego la
 * acción. Sin carrera o sin marcas no hay cifra que enseñar: queda el nombre,
 * la invitación y el botón.
 *
 * Tocar la tarjeta abre el mapa desde ella; el atajo del horario abre la
 * carrera en esa vista.
 */
@Composable
internal fun TuCarrera(
    c: Carrera?,
    resumen: ResumenPortada?,
    cargando: Boolean,
    modifier: Modifier,
    alPulsar: (TransformOrigin) -> Unit,
    alAbrirVista: (TransformOrigin, Vista) -> Unit = { _, _ -> },
) {
    val origen = rememberOrigen()
    val d = DatosHeroe(c, resumen?.takeIf { it.conMarcas }, resumen, cargando)
    val entrada = rememberEntradaHeroe(c?.slug, cargando)
    val abrir = { alPulsar(origen.valor) }
    val horario = { alAbrirVista(origen.valor, Vista.HORARIO) }
    Box(modifier.tarjetaHeroe(origen, abrir)) {
        Column(Modifier.fillMaxWidth().padding(RELLENO_HEROE)) {
            NombreHeroe(d)
            val r = d.resumen
            if (r != null) {
                CifraHeroe(r.porcentaje, 64.sp, entrada, Modifier.padding(top = Espacio.l))
                BarraHeroe(d.fraccion, entrada, Modifier.padding(top = Espacio.l))
            }
            DatoHeroe(d.linea(), Modifier.padding(top = if (r != null) Espacio.s else 2.dp))
            AccionesHeroe(d, abrir, horario, Modifier.padding(top = Espacio.l))
        }
    }
}
