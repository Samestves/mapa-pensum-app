package mpensum.carrera.lista

import mpensum.componentes.BotonesDePaso
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mpensum.logica.Marca
import mpensum.logica.PlanMuelle
import mpensum.logica.Situacion
import mpensum.tema.Movimiento

/*
 * El muelle: lo que se decide va fijo abajo, al alcance del pulgar. Son los
 * mismos botones de la ficha del mapa (`BotonesDePaso`): los dos pasos lado a
 * lado, cada uno con su icono, sin texto de más.
 */

/**
 * El muelle de pie, al pie de la página. La página no tiene scroll, así que
 * nada pasa por debajo y no lleva velo. `abajo` es lo que ocupan los gestos o
 * los botones del sistema.
 */
@Composable
internal fun MuelleDePie(situacion: Situacion, plan: PlanMuelle, abajo: Dp, alMarcar: (Marca?) -> Unit, modifier: Modifier = Modifier) {
    Crossfade(
        situacion, modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp + abajo),
        animationSpec = tween(Movimiento.CORTA), label = "muelle",
    ) { _ -> BotonesDePaso(plan.principal, plan.secundaria, alMarcar) }
}

/** El muelle acostado, en el pie del panel */
@Composable
internal fun MuelleAcostado(situacion: Situacion, plan: PlanMuelle, alMarcar: (Marca?) -> Unit, modifier: Modifier = Modifier) {
    Crossfade(situacion, modifier, animationSpec = tween(Movimiento.CORTA), label = "muelle") { _ ->
        BotonesDePaso(plan.principal, plan.secundaria, alMarcar, alto = 48.dp)
    }
}
