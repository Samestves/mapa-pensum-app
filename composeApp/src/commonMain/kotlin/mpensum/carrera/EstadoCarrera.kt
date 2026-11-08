package mpensum.carrera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import mpensum.datos.leerPensumDelPaquete
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mpensum.datos.Asignatura
import mpensum.datos.Guardado
import mpensum.datos.Pensum
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.FuenteCarga
import mpensum.logica.Simulador
import mpensum.logica.cargaHabitual
import mpensum.logica.planesPorCarga
import mpensum.logica.MAX_CARGA
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.logica.Situacion
import mpensum.logica.avanceDeGrupos
import mpensum.logica.casillasEfectivas
import mpensum.logica.colocarElectiva
import mpensum.logica.conMarcas
import mpensum.logica.estadosDe
import mpensum.logica.progresoDe
import mpensum.logica.situacionDe
import mpensum.mapa.ALTO_ENCABEZADO
import mpensum.mapa.ALTO_ENCABEZADO_COMPACTO
import mpensum.mapa.Caja
import mpensum.mapa.Franja
import mpensum.mapa.FranjaColocada
import mpensum.mapa.LayoutMapa
import mpensum.mapa.Nodo
import mpensum.mapa.NodoMapa
import mpensum.mapa.calcularFranja
import mpensum.mapa.calcularLayout
import mpensum.tema.ColoresMapa
import mpensum.tema.Tema

/**
 * Todo lo de una carrera abierta: su geometría (fija), lo que marcaste y lo
 * que se deduce de ello. Lo deducido va en derivedStateOf: marcar una materia
 * recalcula una vez y solo repinta lo que lo lee.
 *
 * Con `demostracion` es un mapa de mentira (la guía de bienvenida): parte de
 * esas marcas y no guarda nada, para no tocar el avance de verdad.
 */
@Stable
class EstadoCarrera(
    val pensum: Pensum,
    private val demostracion: Map<String, Marca>? = null,
    compacto: Boolean = false,
    /** El layout ya calculado (fuera del hilo principal) para `compactoInicial`; si no, se calcula al leerlo */
    private val layoutInicial: LayoutMapa? = null,
    private val compactoInicial: Boolean = compacto,
    /** Si `layoutInicial` se calculó con cables rectos */
    private val rectosInicial: Boolean = false,
) {
    /**
     * Horizontal: cabecera de una línea. Girar el teléfono cambia solo esto y,
     * con ello, el layout; las marcas, el horario y la lista siguen donde estaban.
     */
    var compacto by mutableStateOf(compacto)

    /**
     * La geometría no depende del avance: solo se recalcula si gira el teléfono
     * o si cambian los cables a rectos (cuyo trazado sale del layout). Se compara
     * por identidad: dos layouts nunca se comparan campo a campo.
     */
    val layout: LayoutMapa by derivedStateOf(referentialEqualityPolicy()) {
        val rectos = Tema.rectos
        if (layoutInicial != null && this.compacto == compactoInicial && rectos == rectosInicial) layoutInicial else layoutDe(pensum, this.compacto, rectos)
    }

    /**
     * El período de hoy, uno solo para la fecha de la carrera y para todas las
     * pantallas: se relee al abrir la hoja de avance, no a media sesión de otro.
     */
    var hoy by mutableStateOf(Periodo.actual())
        private set

    fun actualizarHoy() {
        hoy = Periodo.actual()
    }

    var marcas by mutableStateOf(demostracion ?: Guardado.marcas(pensum))
        private set
    var casillas by mutableStateOf(if (demostracion != null) emptyMap() else Guardado.casillas(pensum))
        private set

    val estados by derivedStateOf { estadosDe(pensum.todas, marcas) }

    val situaciones: Map<String, Situacion> by derivedStateOf {
        pensum.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
    }

    val progreso by derivedStateOf {
        progresoDe(pensum.obligatorias, estados, pensum.creditos, avanceDeGrupos(pensum.grupos, marcas))
    }

    /** Las materias por semestre que eligió en el mando de la hoja de avance; null es la habitual, la de sus marcas */
    var carga by mutableStateOf(if (demostracion != null) null else Guardado.carga(pensum.slug))
        private set

    /**
     * Los ocho planes (de 1 a 8 materias por semestre), calculados una sola vez
     * por cada marca, casilla o período que cambie. La hoja, el mando de carga,
     * la fecha más rápida y los detalles leen estos mismos: nadie los repite.
     * Solo se calculan si algo los lee (la hoja de avance).
     */
    val planes: List<EstimacionGraduacion> by derivedStateOf { planesPorCarga(pensum, marcas, casillas, hoy) }

    private val habitualYFuente by derivedStateOf { cargaHabitual(pensum, marcas) }

    /** Cuántas materias por semestre salen de tus marcas, acotada al tope del mando */
    val habitual: Int get() = habitualYFuente.first

    /** Cuándo te gradúas: el plan de la carga que elegiste o, si no, el de tu habitual */
    val graduacion: EstimacionGraduacion by derivedStateOf {
        val (usada, fuente) = carga?.let { it.coerceIn(1, MAX_CARGA) to FuenteCarga.ELEGIDA } ?: habitualYFuente
        planes[usada - 1].copy(fuenteCarga = fuente)
    }

    /** Las simulaciones con veto que comparten «Tu próximo semestre» y «No puedes atrasar» */
    val simulador: Simulador by derivedStateOf { Simulador(pensum, marcas, casillas, carga, hoy, planes, graduacion) }

    /** Dónde está cada electiva: la misma cuenta que usa la fecha (casillasEfectivas) */
    private val elegidas by derivedStateOf { casillasEfectivas(pensum, casillas, marcas) }

    /** La franja de electivas, debajo de los semestres; null donde las casillas van en los semestres */
    val franja: FranjaColocada? by derivedStateOf {
        if (pensum.electivasEnCasillas || pensum.grupos.isEmpty()) null
        else calcularFranja(pensum.grupos, elegidas, layout.columnas.map { it.x }, layout.finSemestres + Franja.CORREDOR)
    }

    /** Lo que se dibuja: las materias de los semestres y las casillas de la franja */
    val nodos: List<NodoMapa> by derivedStateOf { layout.nodos + franja?.nodos.orEmpty() }

    val alto: Double by derivedStateOf { franja?.let { layout.finSemestres + Franja.CORREDOR + it.alto } ?: layout.alto }

    /** La electiva puesta en una casilla, o null si sigue vacía */
    fun enCasilla(casilla: String): Asignatura? = elegidas[casilla]?.let { layout.porCodigo[it]?.asignatura }

    /** La casilla donde está puesta una electiva, si lo está */
    fun casillaDe(codigo: String): String? = elegidas.entries.firstOrNull { it.value == codigo }?.key

    /** Lo que de verdad hay en un nodo: la materia, o la electiva puesta en su casilla */
    fun materiaDe(nodo: NodoMapa): Asignatura? = if (nodo.asignatura.esHueco) enCasilla(nodo.codigo) else nodo.asignatura

    /** El color de cada materia: el de su área, o su tono de la carrera */
    val colores = ColoresMapa(pensum)

    /** El nodo donde se dibuja una materia: el suyo, o la casilla donde se puso si es una electiva */
    fun nodoDe(codigo: String): NodoMapa? = nodos.firstOrNull { (materiaDe(it)?.codigo ?: it.codigo) == codigo }

    /** El rectángulo que abarca unas materias en el plano, o null si ninguna está dibujada */
    fun cajaDe(codigos: Collection<String>): Caja? {
        val dibujadas = codigos.mapNotNull(::nodoDe)
        if (dibujadas.isEmpty()) return null
        return Caja(
            dibujadas.minOf { it.x }.toFloat(), dibujadas.minOf { it.y }.toFloat(),
            dibujadas.maxOf { it.x + Nodo.ANCHO }.toFloat(), dibujadas.maxOf { it.y + Nodo.ALTO }.toFloat(),
        )
    }

    fun marcar(codigo: String, marca: Marca?) = marcarVarias(mapOf(codigo to marca))

    /** Varias marcas en un solo cambio: aprobar o desmarcar un semestre entero */
    fun marcarVarias(cambios: Map<String, Marca?>) {
        marcas = conMarcas(marcas, cambios)
        if (demostracion == null) Guardado.guardarMarcas(pensum.slug, marcas)
    }

    /** Si hay algo que reiniciar: una marca, una electiva puesta o una carga elegida */
    val hayAvance: Boolean get() = marcas.isNotEmpty() || casillas.isNotEmpty() || carga != null

    /** De vuelta al 0 %: sin marcas, sin electivas en las casillas y con la carga habitual */
    fun reiniciar() {
        marcas = emptyMap()
        casillas = emptyMap()
        carga = null
        if (demostracion == null) Guardado.borrarAvance(pensum.slug)
    }

    /**
     * null vuelve a la carga habitual. Se guarda siempre lo pulsado, aunque
     * coincida con la habitual de hoy: la habitual cambia al marcar más y la
     * elegida no debe moverse sola. Acotada al mismo tope que la cuenta.
     */
    fun elegirCarga(nueva: Int?) {
        carga = nueva?.coerceIn(1, MAX_CARGA)
        if (demostracion == null) Guardado.guardarCarga(pensum.slug, carga)
    }

    fun colocar(casilla: String, electiva: String?) {
        casillas = colocarElectiva(casillas, casilla, electiva)
        if (demostracion == null) Guardado.guardarCasillas(pensum.slug, casillas)
    }
}

/**
 * Calcula los ocho planes de graduación fuera del hilo principal y los deja en la caché de `planes` y `graduacion`:
 * cuando se abre «Tu avance» ya están listos y la hoja no los calcula en el primer cuadro de su subida.
 * Se repite tras cada cambio de marcas, casillas o período, con una pausa para no calcular a cada toque.
 */
private suspend fun EstadoCarrera.precalentarGraduacion() {
    snapshotFlow { Triple(marcas, casillas, hoy) }.collectLatest {
        delay(PAUSA_PRECALENTADO_MS)
        withContext(Dispatchers.Default) { graduacion }
    }
}

private const val PAUSA_PRECALENTADO_MS = 600L

/** La geometría de una carrera; compacto (horizontal) lleva la cabecera de una línea */
private fun layoutDe(pensum: Pensum, compacto: Boolean, rectos: Boolean): LayoutMapa =
    calcularLayout(pensum.asignaturas, pensum.grupos, if (compacto) ALTO_ENCABEZADO_COMPACTO else ALTO_ENCABEZADO, rectos)

/** Lo que llega junto de fuera del hilo principal: el pensum y su geometría */
private class CarreraLista(val pensum: Pensum, val layout: LayoutMapa, val compacto: Boolean, val rectos: Boolean)

/**
 * El estado de una carrera al abrirla. El JSON se decodifica y la geometría se
 * calcula en otro hilo, para no robarle cuadros a la animación de entrada; null
 * mientras llega. Girar el teléfono no lo recrea: solo cambia `compacto`.
 */
@Composable
fun rememberEstadoCarrera(slug: String, horizontal: Boolean): EstadoCarrera? {
    val lista = produceState<CarreraLista?>(null, slug) {
        val compacto = horizontal
        val rectos = Tema.rectos
        value = withContext(Dispatchers.Default) {
            val pensum = leerPensumDelPaquete(slug)
            CarreraLista(pensum, layoutDe(pensum, compacto, rectos), compacto, rectos)
        }
    }.value
    val estado = remember(lista) {
        lista?.let { EstadoCarrera(it.pensum, compacto = horizontal, layoutInicial = it.layout, compactoInicial = it.compacto, rectosInicial = it.rectos) }
    }
    // Girar solo cambia el layout: el estado, el horario, la cámara y la lista siguen vivos
    estado?.compacto = horizontal
    if (estado != null) LaunchedEffect(estado) { estado.precalentarGraduacion() }
    return estado
}
