package mpensum.carrera

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mpensum.datos.Asignatura
import mpensum.datos.Guardado
import mpensum.datos.Pensum
import mpensum.logica.EstimacionGraduacion
import mpensum.logica.MAX_CARGA
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.logica.Situacion
import mpensum.logica.avanceDeGrupos
import mpensum.logica.casillasEfectivas
import mpensum.logica.colocarElectiva
import mpensum.logica.conMarcas
import mpensum.logica.estadosDe
import mpensum.logica.estimarGraduacion
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

/**
 * Todo lo de una carrera abierta: su geometría (fija), lo que marcaste y lo
 * que se deduce de ello. Lo deducido va en derivedStateOf: marcar una materia
 * recalcula una vez y solo repinta lo que lo lee.
 *
 * Con `demostracion` es un mapa de mentira (la guía de bienvenida): parte de
 * esas marcas y no guarda nada, para no tocar el avance de verdad.
 */
@Stable
class EstadoCarrera(val pensum: Pensum, private val demostracion: Map<String, Marca>? = null, compacto: Boolean = false) {
    /** La geometría no depende del avance: se calcula una sola vez. Compacto (horizontal): cabecera de una línea */
    val layout: LayoutMapa = calcularLayout(pensum.asignaturas, pensum.grupos, if (compacto) ALTO_ENCABEZADO_COMPACTO else ALTO_ENCABEZADO)

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
     * Cuándo te gradúas. Solo se calcula si algo lo lee (la hoja de avance), y
     * entonces una vez por cada marca, casilla o carga que cambie.
     */
    val graduacion: EstimacionGraduacion by derivedStateOf { estimarGraduacion(pensum, marcas, casillas, carga, Periodo.actual()) }

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
