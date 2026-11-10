package mpensum.portada

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import mpensum.carrera.Vista
import mpensum.datos.Carrera
import mpensum.logica.ResumenPortada

/**
 * Tu carrera, arriba del home: una tarjeta, un trabajo, volver a tu carrera.
 * Va en la superficie de la hoja del progreso (sin cristal, sin filo de color
 * y sin la silueta de puntos, que se queda en las otras carreras). Dentro,
 * la «cifra protagonista» (HeroeA).
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
        HeroeA(d, entrada, abrir, horario)
    }
}
