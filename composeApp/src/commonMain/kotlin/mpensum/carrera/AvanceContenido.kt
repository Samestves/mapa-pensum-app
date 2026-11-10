package mpensum.carrera

import mpensum.componentes.superficie
import mpensum.componentes.RotuloSeccion
import mpensum.componentes.estiloCuerpo
import mpensum.componentes.TarjetaLista
import mpensum.componentes.icono
import mpensum.componentes.trazos
import mpensum.componentes.DosALoAncho
import mpensum.componentes.CifraFila
import mpensum.componentes.FilaLista
import mpensum.componentes.nota
import mpensum.componentes.Pastilla
import mpensum.componentes.altoMayuscula
import mpensum.componentes.estiloNota
import mpensum.componentes.Fecha
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.componentes.SEPARACION_UNIDAD
import mpensum.componentes.dibujarCifraConUnidad
import mpensum.componentes.estiloCifra
import mpensum.componentes.estiloUnidad
import mpensum.componentes.margenPantalla
import mpensum.componentes.rememberPartesListas
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.SemestreCapsulas
import mpensum.logica.SiApruebas
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import kotlin.math.roundToInt

/* El contenido del índice de «Tu avance»: la cifra héroe, la fecha, las filas y las electivas */

/** Del tope de la cifra héroe a «13 / 153 UC» */
private val BAJO_CIFRA = 11.dp

private const val ENTRADA_CIFRA = 700

/** Desde este ancho de panel, dos columnas de filas caben sin cortar los nombres */
internal val ANCHO_DOS_COLUMNAS = 500.dp

// ─────────────────────────────────── La hoja ──────────────────────────────────

/**
 * El índice. Lo que cuenta ya viene calculado (`EstadoCarrera.resumen`) y se
 * compone en cuatro partes, una por cuadro: la cifra y la fecha al tocar, y
 * la barra, las filas y el resto mientras la hoja todavía sube desde abajo.
 */
@Composable
internal fun ContenidoHoja(estado: EstadoCarrera, abrir: (DetalleAvance) -> Unit) {
    val resumen = estado.resumen
    val partes = rememberPartesListas(4)

    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.l, bottom = Espacio.xl)) {
        TarjetaHeroe(estado, estado.graduacion, resumen.semestres, resumen.si, resumen.cursando.size, conBarra = partes >= 2)
        if (partes < 3) return@Column
        Spacer(Modifier.height(Espacio.m))
        ListaDeFilas(resumen.preguntas, abrir)
        if (partes < 4) return@Column
        TarjetaElectivas(resumen.electivas, abrir)
        Spacer(Modifier.height(Espacio.xl))
        BotonReinicio(estado)
    }
}

// ─────────────────────────────────── La tarjeta de arriba ──────────────────────────────────

/**
 * Lo que comparten la tarjeta de pie y el resumen del índice acostado: la
 * `cifra` a la izquierda con lo aprobado debajo («13 / 153 UC»), a `bajoCifra`
 * de ella; la fecha arriba a la derecha, con su tope a la altura del de la
 * cifra, y «Te gradúas» debajo. Si no caben a lo ancho, la fecha baja bajo la
 * cifra.
 */
@Composable
internal fun CifraYFecha(estado: EstadoCarrera, g: EstimacionGraduacion, bajoCifra: Dp, cifra: @Composable () -> Unit) {
    val progreso = estado.progreso
    val conUc = progreso.porcentaje != null
    val llevas = if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas
    val de = if (conUc) progreso.ucTitulo!! else progreso.total
    DosALoAncho(
        hueco = Espacio.m,
        vertical = Espacio.m,
        izquierda = {
            Column {
                cifra()
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)) { append("$llevas") }
                        append(" / $de ${if (conUc) "UC" else "materias"}")
                    },
                    Modifier.padding(top = bajoCifra),
                    style = estiloNota(),
                )
            }
        },
        derecha = {
            val final = g.periodoFinal
            if (!g.terminado && final != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Fecha(final.mesYAnio())
                    BasicText("Te gradúas", Modifier.padding(top = Espacio.xs), style = estiloNota())
                }
            }
        },
    )
}

/**
 * La tarjeta de pie: la cifra y la fecha, luego la barra de cápsulas y la frase, que
 * sin `conBarra` todavía no se componen.
 */
@Composable
private fun TarjetaHeroe(estado: EstadoCarrera, g: EstimacionGraduacion, semestres: List<SemestreCapsulas>, si: SiApruebas?, cursando: Int, conBarra: Boolean) {
    val progreso = estado.progreso
    val fuerte = SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)

    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        CifraYFecha(estado, g, BAJO_CIFRA) { CifraHeroe(progreso.avance.roundToInt()) }
        if (!conBarra) return@Column
        BarraCapsulas(semestres, Modifier.padding(top = Espacio.l))
        BasicText(
            buildAnnotatedString {
                when {
                    g.terminado -> append("Completaste la carrera.")
                    si != null && cursando > 0 -> {
                        append("Si apruebas ")
                        withStyle(fuerte) { append(if (cursando == 1) "la que cursas" else "las $cursando que cursas") }
                        append(":\nllegas al ")
                        withStyle(fuerte) { append("${si.avance.roundToInt()}%") }
                        when (val n = si.seAbren.size) {
                            0 -> append(".")
                            1 -> {
                                append(" y se te abre ")
                                withStyle(fuerte) { append("1") }
                                append(".")
                            }
                            else -> {
                                append(" y se te abren ")
                                withStyle(fuerte) { append("$n") }
                                append(".")
                            }
                        }
                    }
                    progreso.disponibles > 0 -> {
                        append("Puedes inscribir ")
                        withStyle(fuerte) { append("${progreso.disponibles} ${if (progreso.disponibles == 1) "materia" else "materias"}") }
                        append(" ahora.")
                    }
                    else -> append("Por ahora no hay materias que inscribir.")
                }
            },
            Modifier.padding(top = Espacio.m),
            style = estiloCuerpo(),
        )
    }
}

/**
 * El porcentaje héroe (Inter Display 200 a 64 sp) con su % en la línea base,
 * recortado a sus mayúsculas: su tope es el tope del número. Cuenta al
 * dibujar, sin recomponer.
 */
@Composable
private fun CifraHeroe(porcentaje: Int) {
    // Cuenta una vez por apertura: al volver de una pantalla interna el 41 % ya está puesto (se guarda con la página)
    var yaContó by rememberSaveable { mutableStateOf(false) }
    val cuenta = remember { Animatable(if (yaContó || Movimiento.reducido) 1f else 0f) }
    LaunchedEffect(porcentaje) {
        if (cuenta.value >= 1f) return@LaunchedEffect
        yaContó = true
        cuenta.animateTo(1f, tween(ENTRADA_CIFRA, easing = Movimiento.ESTANDAR))
    }
    val medidor = rememberTextMeasurer()
    val numero = estiloCifra(64.sp, interlinea = 64.sp)
    val signo = estiloUnidad(64.sp)
    val densidad = LocalDensity.current
    val (ancho, alto) = with(densidad) {
        val final = medidor.measure("$porcentaje", numero)
        val por = medidor.measure("%", signo)
        (final.size.width + SEPARACION_UNIDAD.toPx() + por.size.width).toDp() to altoMayuscula(64.sp.toPx()).toDp()
    }
    Canvas(Modifier.size(ancho, alto).semantics { contentDescription = "$porcentaje por ciento aprobado" }) {
        val cifra = medidor.measure("${(porcentaje * cuenta.value).roundToInt()}", numero)
        val por = medidor.measure("%", signo)
        dibujarCifraConUnidad(cifra, por, 0f, size.height)
    }
}

// ─────────────────────────────────── Las filas ──────────────────────────────────

/** Las filas agrupadas de la hoja de pie: cada una abre su pantalla */
@Composable
private fun ListaDeFilas(filas: List<Pregunta>, abrir: (DetalleAvance) -> Unit) {
    if (filas.isEmpty()) return
    TarjetaLista {
        filas.forEachIndexed { k, f ->
            FilaLista(
                titulo = f.titulo,
                nota = nota(f.nota),
                primera = k == 0,
                icono = { Pastilla(f.icono.trazos(), f.azul) },
                chevron = Lucide.siguiente,
                alTocar = { abrir(f.detalle) },
                valor = f.valor?.let { v -> { CifraFila(v, f.unidad) } },
            )
        }
    }
}

/** ELECTIVAS: una fila por grupo con cuota */
@Composable
private fun TarjetaElectivas(electivas: List<Pregunta>, abrir: (DetalleAvance) -> Unit) {
    if (electivas.isEmpty()) return
    RotuloSeccion("ELECTIVAS")
    ListaDeFilas(electivas, abrir)
}

// ──────────────────────────────── Reiniciar ───────────────────────────────
