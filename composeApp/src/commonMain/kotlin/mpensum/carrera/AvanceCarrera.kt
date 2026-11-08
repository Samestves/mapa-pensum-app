package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.Lucide
import mpensum.componentes.SEPARACION_UNIDAD
import mpensum.componentes.dibujarCifraConUnidad
import mpensum.componentes.esApaisado
import mpensum.componentes.fundidoAbajo
import mpensum.componentes.estiloCifra
import mpensum.componentes.estiloUnidad
import mpensum.componentes.margenPantalla
import mpensum.componentes.ventana
import mpensum.datos.Grupo
import mpensum.datos.Pensum
import mpensum.logica.AvanceGrupo
import mpensum.logica.EstadoMateria
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.Periodo
import mpensum.logica.SemestreCapsulas
import mpensum.logica.SiApruebas
import mpensum.logica.avanceDeGrupos
import mpensum.logica.capsulasDe
import mpensum.logica.cargaMasRapida
import mpensum.logica.cursandoDe
import mpensum.logica.siApruebasLoQueCursas
import mpensum.logica.ucTipicaDe
import mpensum.mapa.nombreCasilla
import mpensum.mapa.tituloGrupo
import mpensum.plataforma.ManejarAtras
import mpensum.tema.Movimiento
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Tipo
import mpensum.tema.inter
import mpensum.tema.jost
import kotlin.math.roundToInt

/*
 * La hoja del progreso (docs/maquetas/progreso-refinado.html):
 *
 *   1. La tarjeta de arriba: el porcentaje con lo aprobado debajo, la fecha en
 *      la esquina, la barra de cápsulas con su regla de semestres y una frase
 *      calculada con lo que te da aprobar lo que cursas.
 *   2. Tres preguntas y la fecha en filas agrupadas: este semestre, el
 *      próximo, lo que no puedes atrasar y cuándo te gradúas.
 *   3. Las electivas con cuota, una fila por grupo.
 *   4. Reiniciar, callado.
 *
 * De pie, cada fila abre su pantalla interna dentro de la misma hoja: entra
 * deslizándose desde la derecha y el atrás del sistema vuelve a la hoja.
 * Acostada, la hoja ocupa la pantalla y es índice y detalle (AvanceIndice.kt).
 */

/** Del tope de la cifra héroe a «13 / 153 UC» */
private val BAJO_CIFRA = 11.dp

/** Los rótulos: mayúsculas semibold de 11 sp con 0,05 em de espaciado */
@Composable
internal fun versal(color: Color) = TextStyle(fontFamily = inter(), fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, lineHeight = 14.sp, letterSpacing = 0.05.em, color = color)

/** Un texto de la hoja: Inter, con el tamaño, el peso y el color que pida */
@Composable
internal fun estilo(tam: Int, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam * 1.35f).roundToInt()) =
    estilo(tam.sp, color, peso, interlinea)

/** Lo mismo con el tamaño de un token de [Tipo]: la interlínea por defecto es 1,35 veces el tamaño */
@Composable
internal fun estilo(tam: TextUnit, color: Color, peso: FontWeight = FontWeight.Normal, interlinea: Int = (tam.value * 1.35f).roundToInt()) =
    TextStyle(fontFamily = inter(), fontWeight = peso, fontSize = tam, lineHeight = interlinea.sp, color = color, fontFeatureSettings = "tnum")

private const val ENTRADA_CIFRA = 700

/** Un número que cuenta hasta su valor en 600 ms, saliendo rápido y frenando (useNumeroAnimado) */
@Composable
fun numeroAnimado(destino: Float): Float {
    val valor = remember { Animatable(destino) }
    LaunchedEffect(destino) { valor.animateTo(destino, tween(600, easing = Movimiento.ESTANDAR)) }
    return valor.value
}

/** Lo que hay dentro de cada pantalla interna, de pie o en el panel del detalle acostado */
@Composable
internal fun ContenidoDetalle(d: DetalleAvance, estado: EstadoCarrera, hoy: Periodo, alVerEnMapa: (List<String>) -> Unit, alAbrirCasilla: (String) -> Unit) {
    when (d) {
        DetalleAvance.EsteSemestre -> DetalleEsteSemestre(estado, hoy)
        DetalleAvance.Proximo -> DetalleProximo(estado, hoy)
        DetalleAvance.Cadena -> DetalleCadena(estado, hoy)
        DetalleAvance.Graduacion -> DetalleGraduacion(estado, hoy)
        is DetalleAvance.Electivas -> DetalleElectivas(estado, d.clave, alAbrirCasilla)
    }
}

/** Las pantallas internas a las que lleva cada fila de la hoja */
internal sealed interface DetalleAvance {
    data object EsteSemestre : DetalleAvance
    data object Proximo : DetalleAvance
    data object Cadena : DetalleAvance
    data object Graduacion : DetalleAvance
    data class Electivas(val clave: String) : DetalleAvance
}

/** La pastilla, el título y el subtítulo de cada pantalla interna */
internal fun cabeceraDe(d: DetalleAvance, estado: EstadoCarrera, g: EstimacionGraduacion, hoy: Periodo): DatosCabecera = when (d) {
    DetalleAvance.EsteSemestre -> DatosCabecera(IconosHoja.libroAbierto, "Este semestre")
    DetalleAvance.Proximo -> DatosCabecera(IconosHoja.calendario, hoy.mas(1).rango(), azul = true)
    DetalleAvance.Cadena -> DatosCabecera(IconosHoja.eslabon, "No puedes atrasar")
    DetalleAvance.Graduacion -> DatosCabecera(Lucide.birrete, "¿Cuándo me gradúo?")
    is DetalleAvance.Electivas -> {
        val grupo = estado.pensum.grupos.firstOrNull { it.clave == d.clave }
        DatosCabecera(grupo?.let(::iconoDeGrupo) ?: IconosHoja.libroAbierto, grupo?.let(::tituloGrupo) ?: "Electivas")
    }
}

/** Lo que se enciende en el mapa desde una pantalla interna: las que te conviene inscribir o tu cadena; las demás no llevan mapa */
internal fun codigosDelMapa(d: DetalleAvance, g: EstimacionGraduacion): List<String>? = when (d) {
    DetalleAvance.Proximo -> g.plan.firstOrNull()?.takeIf { it.isNotEmpty() }
    DetalleAvance.Cadena -> g.cuelloDeBotella.takeIf { it.isNotEmpty() }
    else -> null
}

@Composable
fun BoxScope.HojaAvance(abierta: Boolean, estado: EstadoCarrera, alCerrar: () -> Unit, alVerEnMapa: (List<String>) -> Unit) {
    // Cada vez que se abre, se abre en la hoja y no en el detalle donde se dejó
    var apertura by remember { mutableIntStateOf(0) }
    var estabaAbierta by remember { mutableStateOf(abierta) }
    if (abierta != estabaAbierta) {
        estabaAbierta = abierta
        if (abierta) apertura++
    }
    var detalle by remember(apertura) { mutableStateOf<DetalleAvance?>(null) }
    // La casilla de electiva que se está eligiendo: su selector sube por encima de esta hoja
    var casilla by remember(apertura) { mutableStateOf<String?>(null) }
    val apaisado = esApaisado()
    remember(apertura) { estado.actualizarHoy() }
    val hoy = estado.hoy

    if (apaisado) {
        // Acostada: índice y detalle a pantalla completa; lo elegido es el mismo detalle de pie, así girar no pierde dónde estabas
        HojaInferior(abierta = abierta, alCerrar = alCerrar, desplazable = false, cabecera = {}) {
            IndiceYDetalle(estado, hoy, detalle, alElegir = { detalle = it }, alCerrar = alCerrar, alVerEnMapa = alVerEnMapa, alAbrirCasilla = { casilla = it })
        }
    } else {
        PaginasDelProgreso(abierta, apertura, estado, hoy, detalle, alCambiar = { detalle = it }, alCerrar = alCerrar, alVerEnMapa = alVerEnMapa, alAbrirCasilla = { casilla = it })
    }
    // Montado después de la hoja, el selector queda encima y su atrás se atiende primero
    SelectorElectiva(
        casilla = casilla,
        estado = estado,
        aprobarAlElegir = false,
        alColocar = { c, electiva ->
            estado.colocar(c, electiva)
            casilla = null
        },
        alCerrar = { casilla = null },
    )
}

/**
 * La hoja de pie: alto fijo y, dentro, dos páginas (el índice y cada pantalla
 * interna) con un solo reloj (patrón A, docs/qa/transiciones.md). La cabecera
 * y el cuerpo son el mismo `Transition`; cada página lleva su desplazamiento,
 * que se guarda al salir: al volver, el índice está donde lo dejaste.
 */
@Composable
private fun BoxScope.PaginasDelProgreso(
    abierta: Boolean,
    apertura: Int,
    estado: EstadoCarrera,
    hoy: Periodo,
    detalle: DetalleAvance?,
    alCambiar: (DetalleAvance?) -> Unit,
    alCerrar: () -> Unit,
    alVerEnMapa: (List<String>) -> Unit,
    alAbrirCasilla: (String) -> Unit,
) {
    // Un reloj nuevo en cada apertura: no arrastra la página donde se cerró
    val transicion = key(apertura) { updateTransition(detalle, label = "progreso") }
    val desplazoPx = with(LocalDensity.current) { Movimiento.DESPLAZO.roundToPx() }
    val empuje: AnimatedContentTransitionScope<DetalleAvance?>.() -> ContentTransform = { empujeDePagina(targetState != null, desplazoPx) }
    HojaInferior(
        abierta = abierta,
        alCerrar = alCerrar,
        desplazable = false,
        altoFijo = true,
        cabecera = {
            CabeceraDeAltoEstable {
                transicion.AnimatedContent(transitionSpec = empuje, contentAlignment = Alignment.TopStart) { d ->
                    if (d == null) {
                        CabeceraHoja(estado.pensum.nombre, alCerrar)
                    } else {
                        Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            BotonRedondo(Lucide.atras, "Volver") { alCambiar(null) }
                            Spacer(Modifier.weight(1f))
                            codigosDelMapa(d, estado.graduacion)?.let { codigos -> BotonMapa { alVerEnMapa(codigos) } }
                        }
                    }
                }
            }
        },
    ) {
        // Atrás vuelve del detalle a la hoja; ya en la hoja, la cierra (HojaInferior)
        ManejarAtras(detalle != null) { alCambiar(null) }
        val guardado = rememberSaveableStateHolder()
        transicion.AnimatedContent(Modifier.fillMaxSize(), transitionSpec = empuje, contentAlignment = Alignment.TopStart) { d ->
            guardado.SaveableStateProvider(d.clave()) {
                val desplazo = rememberScrollState()
                Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo)) {
                    if (d == null) {
                        ContenidoHoja(estado, hoy) { alCambiar(it) }
                    } else {
                        CabeceraInterna(cabeceraDe(d, estado, estado.graduacion, hoy), Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.m))
                        ContenidoDetalle(d, estado, hoy, alVerEnMapa, alAbrirCasilla)
                    }
                }
            }
        }
    }
}

/** «TU AVANCE», el nombre de la carrera en Jost Light 28 y el botón de cerrar */
@Composable
private fun CabeceraHoja(nombre: String, alCerrar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = Espacio.s)) {
            BasicText("TU AVANCE", style = estiloRotulo())
            // En ventana compacta el título se encoge antes que saltar de línea
            if (ventana().compacta) {
                BasicText(nombre, Modifier.padding(top = Espacio.xxs), style = estiloTitulo(), maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = Tipo.xl, maxFontSize = 28.sp))
            } else {
                BasicText(nombre, Modifier.padding(top = Espacio.xxs), style = estiloTitulo())
            }
        }
        BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
    }
}

/**
 * El cuerpo de una pantalla interna: la respuesta `arriba` y debajo las dos
 * partes, en una columna. De pie la desplaza la hoja y empieza 24 dp bajo la
 * cabecera; acostado va en el panel del detalle, que ya trae sus márgenes, y
 * se desplaza por su cuenta bajo su cabecera fija. Con `dosColumnas`, si el
 * panel pasa de 500 dp, las dos partes van lado a lado bajo la respuesta.
 */
@Composable
internal fun CuerpoDetalle(
    arriba: (@Composable ColumnScope.() -> Unit)? = null,
    dosColumnas: Boolean = false,
    izquierda: @Composable ColumnScope.() -> Unit,
    derecha: @Composable ColumnScope.() -> Unit,
) {
    if (!esApaisado()) {
        Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xl, bottom = Espacio.xxl)) {
            arriba?.invoke(this)
            izquierda()
            derecha()
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val lado = dosColumnas && maxWidth >= ANCHO_DOS_COLUMNAS
        val desplazo = rememberScrollState()
        Column(Modifier.fillMaxSize().fundidoAbajo(desplazo).verticalScroll(desplazo).padding(top = ARRIBA_CUERPO_PANEL, bottom = Espacio.xl)) {
            arriba?.invoke(this)
            if (lado) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Column(Modifier.weight(1f), content = izquierda)
                    Column(Modifier.weight(1f), content = derecha)
                }
            } else {
                izquierda()
                derecha()
            }
        }
    }
}

/** Desde este ancho de panel, dos columnas de filas caben sin cortar los nombres */
private val ANCHO_DOS_COLUMNAS = 500.dp

// ─────────────────────────────────── La hoja ──────────────────────────────────

@Composable
private fun ContenidoHoja(estado: EstadoCarrera, hoy: Periodo, abrir: (DetalleAvance) -> Unit) {
    val pensum = estado.pensum
    val g = estado.graduacion
    val semestres = remember(estado.marcas, estado.casillas) { capsulasDe(pensum, estado.marcas, estado::enCasilla) }
    val cursando = remember(estado.marcas) { cursandoDe(pensum, estado.marcas).size }
    val si = remember(estado.marcas) { if (cursando > 0) siApruebasLoQueCursas(pensum, estado.marcas) else null }
    val preguntas = rememberPreguntas(estado, hoy)

    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.l, bottom = Espacio.xl)) {
        TarjetaHeroe(estado, g, semestres, si, cursando)
        Spacer(Modifier.height(Espacio.m))
        ListaDeFilas(preguntas, abrir)
        TarjetaElectivas(estado, abrir)
        Spacer(Modifier.height(Espacio.xl))
        BotonReinicio(estado)
    }
}

// ─────────────────────────────────── La tarjeta de arriba ──────────────────────────────────

/**
 * La cifra a la izquierda con lo aprobado debajo; la fecha arriba a la
 * derecha, con su tope a la altura del de la cifra, y «Te gradúas» debajo. Si
 * no caben a lo ancho, la fecha baja bajo la cifra. Luego la barra de
 * cápsulas y la frase.
 */
@Composable
private fun TarjetaHeroe(estado: EstadoCarrera, g: EstimacionGraduacion, semestres: List<SemestreCapsulas>, si: SiApruebas?, cursando: Int) {
    val progreso = estado.progreso
    val conUc = progreso.porcentaje != null
    val llevas = if (conUc) progreso.ucAprobadas + progreso.ucElectivas else progreso.aprobadas
    val de = if (conUc) progreso.ucTitulo!! else progreso.total
    val porcentaje = progreso.avance.roundToInt()
    val fuerte = SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)

    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        DosALoAncho(
            hueco = Espacio.m,
            vertical = Espacio.m,
            izquierda = {
                Column {
                    CifraHeroe(porcentaje)
                    BasicText(
                        buildAnnotatedString {
                            withStyle(fuerte) { append("$llevas") }
                            append(" / $de ${if (conUc) "UC" else "materias"}")
                        },
                        Modifier.padding(top = BAJO_CIFRA),
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

/** Una fila de la hoja: su pastilla, el título, la nota, la cifra (con su unidad) y la pantalla a la que lleva */
internal class Pregunta(
    val icono: List<Path>,
    val azul: Boolean,
    val titulo: String,
    val nota: String,
    val valor: String?,
    val unidad: String?,
    val detalle: DetalleAvance,
)

/** Las preguntas de la hoja: solo las que tienen algo que decir */
internal fun preguntasDe(g: EstimacionGraduacion, hoy: Periodo, cursando: Int, seAbren: Int, rapida: Periodo?, nombre: (String) -> String): List<Pregunta> = buildList {
    if (cursando > 0) {
        val abren = when (seAbren) {
            0 -> "No te abren otras"
            1 -> "Te abren 1"
            else -> "Te abren $seAbren"
        }
        add(Pregunta(IconosHoja.libroAbierto, false, "Este semestre", abren, "$cursando", null, DetalleAvance.EsteSemestre))
    }
    g.plan.firstOrNull()?.let { add(Pregunta(IconosHoja.calendario, true, "Tu próximo semestre", hoy.mas(1).rango(), "${it.size}", null, DetalleAvance.Proximo)) }
    g.cuelloDeBotella.firstOrNull()?.let { add(Pregunta(IconosHoja.eslabon, false, "No puedes atrasar", "Empieza en ${nombre(it)}", "${g.cuelloDeBotella.size}", null, DetalleAvance.Cadena)) }
    if (!g.terminado) add(Pregunta(Lucide.birrete, false, "¿Cuándo me gradúo?", rapida?.let {
        // Si tu fecha ya es la más rápida posible, decirlo en vez de repetirla
        if (it == g.periodoFinal) "Ya vas lo más rápido" else "Lo más rápido: ${it.mesYAnio()}"
    } ?: "A tu ritmo", null, null, DetalleAvance.Graduacion))
}

/** Las preguntas con lo que piden calculado: lo mismo de pie que en el índice acostado */
@Composable
internal fun rememberPreguntas(estado: EstadoCarrera, hoy: Periodo): List<Pregunta> {
    val pensum = estado.pensum
    val cursando = remember(estado.marcas) { cursandoDe(pensum, estado.marcas).size }
    val seAbren = remember(estado.marcas) { if (cursando > 0) siApruebasLoQueCursas(pensum, estado.marcas).seAbren.size else 0 }
    val nombre = remember(pensum) { nombresDelPlan(pensum) }
    val rapida = rememberRapida(estado)
    return preguntasDe(estado.graduacion, hoy, cursando, seAbren, rapida, nombre)
}

/** Las electivas con cuota: una fila por grupo, con lo que llevas de lo que pide el título */
internal fun electivasDe(estado: EstadoCarrera): List<Pregunta> =
    avanceDeGrupos(estado.pensum.grupos, estado.marcas).values.filter { it.meta != null }.map { c ->
        val grupo = estado.pensum.grupos.first { it.clave == c.clave }
        Pregunta(iconoDeGrupo(grupo), false, nombreCorto(c.titulo), subtituloDeGrupo(estado, grupo, c), "${c.uc}", "/${c.meta} UC", DetalleAvance.Electivas(c.clave))
    }

/** Lo más rápido que dejan las prelaciones, probando de 1 a 8 materias por semestre */
@Composable
internal fun rememberRapida(estado: EstadoCarrera): Periodo? = remember(estado.planes) {
    estado.planes[cargaMasRapida(estado.planes) - 1].periodoFinal
}

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
                icono = { Pastilla(f.icono, f.azul) },
                chevron = Lucide.siguiente,
                alTocar = { abrir(f.detalle) },
                valor = f.valor?.let { v -> { CifraFila(v, f.unidad) } },
            )
        }
    }
}

/** El icono de un grupo de electivas: la llave para las técnicas, las personas para las sociohumanísticas y el libro para las demás */
internal fun iconoDeGrupo(g: Grupo): List<Path> = when {
    g.clave == "tecnica" || g.titulo.contains("Técnica", ignoreCase = true) -> IconosHoja.llave
    g.clave == "humanistica" || g.titulo.contains("Sociohuman", ignoreCase = true) -> IconosHoja.usuarios
    else -> IconosHoja.libroAbierto
}

/** «Electivas Técnicas» → «Técnicas» */
private fun nombreCorto(titulo: String): String = titulo.removePrefix("Electivas ").replaceFirstChar { it.uppercase() }

/** Lo útil de un grupo: si ya cubres su cuota, cuántas puedes cursar ya o desde qué semestre se abren */
private fun subtituloDeGrupo(estado: EstadoCarrera, grupo: Grupo, avance: AvanceGrupo): String {
    if (avance.completa) return "Ya cubres las ${avance.meta} UC"
    val abiertas = grupo.asignaturas.count { estado.estados[it.codigo] == EstadoMateria.DISPONIBLE }
    if (abiertas > 0) return "Ya puedes cursar $abiertas"
    val desde = estado.pensum.asignaturas.filter { it.esHueco && it.grupo == grupo.clave }.mapNotNull { it.semestre }.minOrNull()
    return if (desde != null) "Desde el semestre $desde" else "Aún no se abre ninguna"
}

/** ELECTIVAS: una fila por grupo con cuota */
@Composable
private fun TarjetaElectivas(estado: EstadoCarrera, abrir: (DetalleAvance) -> Unit) {
    val electivas = remember(estado.marcas) { electivasDe(estado) }
    if (electivas.isEmpty()) return
    RotuloSeccion("ELECTIVAS")
    ListaDeFilas(electivas, abrir)
}

/** Cómo se llama cada código del plan: la materia, la casilla, o "Electiva técnica" para lo que pide la cuota de un grupo */
internal fun nombresDelPlan(pensum: Pensum): (String) -> String {
    val grupos = pensum.grupos.associateBy { it.clave }
    return { codigo ->
        pensum.porCodigo[codigo]?.nombre
            ?: grupos[codigo.substringBefore('#')]?.let(::nombreCasilla)
            ?: codigo
    }
}

/** Las UC de cada código del plan: las de la materia o, en una casilla sin elegir, las típicas de su grupo */
internal fun ucsDelPlan(pensum: Pensum): (String) -> Int {
    val tipicas = pensum.grupos.associate { it.clave to ucTipicaDe(it) }
    return { codigo -> pensum.porCodigo[codigo]?.uc ?: tipicas[pensum.porCodigo[codigo]?.grupo ?: codigo.substringBefore('#')] ?: 0 }
}

/** Si un código del plan es una casilla de electiva sin elegir */
internal fun esCasillaDelPlan(pensum: Pensum): (String) -> Boolean {
    val casillas = pensum.asignaturas.filter { it.esHueco }.map { it.codigo }.toSet()
    return { codigo -> codigo in casillas || '#' in codigo }
}

// ──────────────────────────────── Reiniciar ───────────────────────────────

/**
 * Reiniciar el avance de la carrera: lo único de la hoja que no se deshace, y
 * por eso lo más callado hasta que se pide. Te deja en el 0 %: sin marcas,
 * sin las electivas que pusiste y con la carga habitual. Pulsarlo no borra
 * nada: dice qué se va a perder y deja Cancelar del lado del pulgar. Sin caja
 * roja: el rojo solo va en el botón que borra.
 */
@Composable
internal fun BotonReinicio(estado: EstadoCarrera) {
    val hay = estado.hayAvance
    var confirmando by remember { mutableStateOf(false) }
    if (!confirmando || !hay) {
        val color = Paleta.suave.copy(alpha = if (hay) 1f else 0.5f)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).superficie(24.dp).clickable(enabled = hay, role = Role.Button) { confirmando = true }.padding(horizontal = Espacio.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hay) IconoLucide(IconosHoja.reiniciar, color, 16.dp, Modifier.padding(end = Espacio.xs))
            BasicText(if (hay) "Reiniciar mi avance" else "Nada que reiniciar todavía", style = estiloFila(color))
        }
        return
    }
    Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
        BasicText("¿Empezar de cero?", style = estiloFila())
        BasicText(
            "Se borra${loQueSeBorra(estado)} en esta carrera. Vuelves al 0% y no se puede deshacer.",
            Modifier.padding(top = Espacio.xxs),
            style = estiloNota(Paleta.suave).copy(lineHeight = 18.sp),
        )
        Row(Modifier.padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Paleta.tinta.copy(alpha = Velo.pastilla)).clickable(role = Role.Button) { confirmando = false },
                contentAlignment = Alignment.Center,
            ) { BasicText("Cancelar", style = estiloFila()) }
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Paleta.rojo.copy(alpha = 0.16f)).clickable(role = Role.Button) {
                    estado.reiniciar()
                    confirmando = false
                },
                contentAlignment = Alignment.Center,
            ) { BasicText("Borrar todo", style = estiloFila(ROJO_LETRA)) }
        }
    }
}

/** " tu materia marcada, las electivas que pusiste y tu carga por semestre": solo lo que de verdad hay */
private fun loQueSeBorra(estado: EstadoCarrera): String {
    val cuantas = estado.marcas.size
    val partes = listOfNotNull(
        when (cuantas) {
            0 -> null
            1 -> "tu materia marcada"
            else -> "tus $cuantas materias marcadas"
        },
        if (estado.casillas.isNotEmpty()) "las electivas que pusiste" else null,
        if (estado.carga != null) "tu carga por semestre" else null,
    )
    // "Se borran" con más de una cosa o con varias materias; "se borra" con una sola
    val plural = partes.size > 1 || cuantas > 1
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
    return "${if (plural) "n" else ""} $lista"
}

/** El rojo mezclado un cuarto con la tinta: se lee sobre su propio velo (AA) */
private val ROJO_LETRA get() = lerp(Paleta.rojo, Paleta.tinta, 0.3f)
