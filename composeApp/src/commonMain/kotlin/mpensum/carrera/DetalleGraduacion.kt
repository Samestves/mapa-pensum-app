package mpensum.carrera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mpensum.componentes.esApaisado
import mpensum.componentes.fundidoAbajo
import mpensum.componentes.margenPantalla
import mpensum.logica.MAX_CARGA
import mpensum.logica.Periodo
import mpensum.logica.cargaMasRapida
import mpensum.logica.estimarGraduacion
import mpensum.logica.planesPorCarga
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * «¿Cuándo me gradúo?»: el anillo, con una marca por materia, y debajo la
 * tarjeta de materias por semestre. Ocho chips (de 1 a 8): el elegido lleno
 * de tinta y tu ritmo de siempre con un punto. Debajo, tu fecha a tu ritmo
 * frente a la del ritmo elegido (o, si eliges el tuyo, frente a lo más rápido
 * que dejan las prelaciones). Acostado va en el panel del detalle: si cabe,
 * el anillo a la izquierda, entero y quieto, y la tarjeta a su lado, con los
 * chips en dos filas de cuatro; si no, uno debajo del otro.
 *
 * Los ocho planes se calculan una sola vez al abrir y solo se recalculan si
 * cambian las marcas o las casillas. Tocar un chip solo elige entre ellos: el
 * anillo y la comparación cambian al instante y la carga se guarda.
 */

/** El ancho del anillo acostado: lo que piden sus anotaciones a los lados */
private val ANCHO_ANILLO_APAISADO = 340.dp

/** Lo mínimo que pide la tarjeta del ritmo apilada junto al anillo: cuatro chips de 34 dp y sus márgenes */
private val ANCHO_RITMO_APILADO = 184.dp

/** La entrada: el anillo se llena en 600 ms, luego salen los rótulos y al final el resto */
private const val ENTRADA_MS = 1_100

@Composable
internal fun DetalleGraduacion(estado: EstadoCarrera, hoy: Periodo) {
    val pensum = estado.pensum
    // Una sola vez al abrir: cada plan con su anillo
    val planes = remember(estado.marcas, estado.casillas) { planesPorCarga(pensum, estado.marcas, estado.casillas, hoy) }
    val anillos = remember(planes) { planes.map { anilloDe(estado, it, hoy) } }
    val habitual = remember(estado.marcas, estado.casillas) { estimarGraduacion(pensum, estado.marcas, estado.casillas, null, hoy).carga.coerceIn(1, MAX_CARGA) }
    var elegida by remember { mutableIntStateOf((estado.carga ?: habitual).coerceIn(1, MAX_CARGA)) }
    val g = planes[elegida - 1]
    val porcentaje = estado.progreso.avance.roundToInt()
    val rapida = remember(planes) { cargaMasRapida(planes) }

    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(ENTRADA_MS, easing = LinearEasing)) }

    val anillo: @Composable (Modifier) -> Unit = { modifier ->
        AnilloGraduacion(anillos[elegida - 1], porcentaje, g.periodoFinal?.anio, hoy.anio, { entrada.value }, modifier)
    }
    // El ritmo entra al final, cuando el anillo ya se llenó
    val ritmo: @Composable (Boolean) -> Unit = { apilada ->
        Column(
            Modifier.graphicsLayer {
                val o = suavizar((entrada.value - 0.7f) / 0.3f)
                alpha = o
                translationY = (1f - o) * 16.dp.toPx()
            },
        ) {
            TarjetaRitmo(
                apilada = apilada,
                elegida = elegida,
                habitual = habitual,
                rapida = rapida,
                planes = planes.map { it.semestresRestantes to it.periodoFinal },
                alElegir = {
                    elegida = it
                    estado.elegirCarga(if (it == habitual) null else it)
                },
            )
            if (rapida < MAX_CARGA) {
                BasicText("Más de $rapida no adelanta: te frena la cadena.", Modifier.padding(start = Espacio.m, end = Espacio.m, top = Espacio.s), style = estiloNota())
            }
        }
    }

    if (esApaisado()) BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= ANCHO_ANILLO_APAISADO + Espacio.m + ANCHO_RITMO_APILADO) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                // El anillo mide 270 dp: centrado en el alto que hay, sin scroll
                Box(Modifier.width(ANCHO_ANILLO_APAISADO).fillMaxHeight(), contentAlignment = Alignment.Center) { anillo(Modifier) }
                val desplazo = rememberScrollState()
                Column(Modifier.weight(1f).fillMaxHeight().fundidoAbajo(desplazo).verticalScroll(desplazo).padding(top = ARRIBA_CUERPO_PANEL, bottom = Espacio.xl)) { ritmo(true) }
            }
        } else {
            val desplazo = rememberScrollState()
            Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo).padding(top = ARRIBA_CUERPO_PANEL, bottom = Espacio.xl)) {
                anillo(Modifier)
                Box(Modifier.padding(top = Espacio.m)) { ritmo(false) }
            }
        }
    } else {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xl, bottom = Espacio.xxl)) {
            anillo(Modifier)
            Box(Modifier.padding(top = Espacio.m)) { ritmo(false) }
        }
    }
}

/** "6 meses", "1 año", "1 año y medio", "8 años": lo que separa dos fechas, en semestres */
internal fun diferencia(semestres: Int): String {
    val medios = abs(semestres)
    if (medios == 1) return "6 meses"
    val anios = medios / 2
    return "$anios ${if (anios == 1) "año" else "años"}" + if (medios % 2 == 1) " y medio" else ""
}

/**
 * La tarjeta del ritmo: el rótulo, los ocho chips y la comparación. `planes`
 * trae, por carga, los semestres que faltan y la fecha.
 */
@Composable
private fun TarjetaRitmo(apilada: Boolean, elegida: Int, habitual: Int, rapida: Int, planes: List<Pair<Int, Periodo?>>, alElegir: (Int) -> Unit) {
    // Con tu ritmo elegido, la comparación es contra lo más rápido; si no, contra el elegido
    val contraRapida = elegida == habitual
    val otra = if (contraRapida) rapida else elegida
    val (semTuyo, fechaTuya) = planes[habitual - 1]
    val (semOtra, fechaOtra) = planes[otra - 1]
    val d = semOtra - semTuyo
    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        // Junto al anillo no cabe entero en una línea: basta con «por semestre», los chips dicen el resto
        BasicText(if (apilada) "POR SEMESTRE" else "MATERIAS POR SEMESTRE", style = estiloRotulo(), maxLines = 1)
        // Junto al anillo acostado, los chips van en dos filas de cuatro y las fechas una debajo de la otra
        val porFila = if (apilada) MAX_CARGA / 2 else MAX_CARGA
        Column(Modifier.padding(top = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
            (1..MAX_CARGA).chunked(porFila).forEach { fila ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
                    for (n in fila) Chip(n, n == elegida, n == habitual, Modifier.weight(1f)) { alElegir(n) }
                }
            }
        }
        val tuya: @Composable ColumnScope.() -> Unit = { ColumnaComparacion("A TU RITMO · $habitual", null) { Fecha(fechaTuya?.mesYAnio() ?: "Ya") } }
        val contra: @Composable ColumnScope.() -> Unit = {
            ColumnaComparacion(
                if (contraRapida) "LO MÁS RÁPIDO · $otra" else "A $otra",
                when {
                    d == 0 -> if (contraRapida) "Ya vas lo más rápido" else "La misma fecha"
                    d < 0 -> "${diferencia(d)} antes"
                    else -> "${diferencia(d)} después"
                },
            ) { Fecha(fechaOtra?.mesYAnio() ?: "Ya") }
        }
        if (apilada) {
            Column(Modifier.padding(top = Espacio.l), content = tuya)
            Box(Modifier.padding(vertical = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = Velo.hilo)))
            Column(content = contra)
        } else {
            Comparacion(Modifier.padding(top = Espacio.l), relleno = 0.dp, izquierda = tuya, derecha = contra)
        }
    }
}

/** Un chip: 36 dp, radio 8; el elegido lleno de tinta y el de tu ritmo con un punto de 4 dp abajo */
@Composable
private fun Chip(n: Int, elegido: Boolean, tuyo: Boolean, modifier: Modifier, alTocar: () -> Unit) {
    val tinta = Paleta.tinta
    val panel = Paleta.panel
    Box(
        modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (elegido) tinta else tinta.copy(alpha = Velo.pastilla))
            .clickable(role = Role.RadioButton, onClick = alTocar)
            .semantics { contentDescription = "$n materias por semestre" + if (tuyo) ", tu ritmo" else "" },
        contentAlignment = Alignment.Center,
    ) {
        // El texto lleva su aire: con letra grande el chip crece y el punto no lo pisa
        BasicText("$n", Modifier.padding(vertical = Espacio.xs), style = estiloFila(if (elegido) panel else Paleta.suave), maxLines = 1)
        if (tuyo) Canvas(Modifier.align(Alignment.BottomCenter).padding(bottom = Espacio.xxs).size(4.dp)) { drawCircle(if (elegido) panel else tinta) }
    }
}
