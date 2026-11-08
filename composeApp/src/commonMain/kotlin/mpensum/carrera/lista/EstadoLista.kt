package mpensum.carrera.lista

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpensum.carrera.EstadoCarrera
import mpensum.logica.GrupoLista
import mpensum.logica.Marca
import mpensum.logica.SeccionEstado
import mpensum.logica.Situacion
import mpensum.logica.UniversoLista
import mpensum.logica.estructuraPorEstado
import mpensum.logica.estructuraPorSemestre
import mpensum.logica.margenDeMateria
import mpensum.logica.semestreActualDeLista
import mpensum.logica.universoDeLista

/**
 * Lo que la lista recuerda al girar el teléfono: qué materia está elegida,
 * si su página está empujada (de pie), cómo se ordena y si Aprobadas está
 * desplegada. Vive en el interior, no en la vista: la barra Núcleo y el Atrás
 * del sistema dependen de ello, y la vista puede desmontarse mientras se
 * funde sin llevárselo.
 */
@Stable
class NavegacionLista(elegida: String? = null, abierta: Boolean = false, porEstado: Boolean = true, aprobadasAbiertas: Boolean = false) {
    /** La materia de la página (de pie) o del panel (acostado) */
    var elegida by mutableStateOf(elegida)

    /** De pie: la página de la materia está empujada sobre la lista */
    var abierta by mutableStateOf(abierta)

    /** Por estado (lo de por hacer) o por semestre, el del pensum */
    var porEstado by mutableStateOf(porEstado)

    /** Aprobadas es un renglón plegado hasta que se toca */
    var aprobadasAbiertas by mutableStateOf(aprobadasAbiertas)

    companion object {
        val Guardado = listSaver<NavegacionLista, Any?>(
            save = { listOf(it.elegida, it.abierta, it.porEstado, it.aprobadasAbiertas) },
            restore = { NavegacionLista(it[0] as String?, it[1] as Boolean, it[2] as Boolean, it[3] as Boolean) },
        )
    }
}

/** La navegación de la lista de una carrera: sobrevive a girar y a recrear la actividad */
@Composable
fun rememberNavegacionLista(slug: String): NavegacionLista = rememberSaveable(slug, saver = NavegacionLista.Guardado) { NavegacionLista() }

/** Lo último que se marcó, para devolverlo tal cual estaba. `seccion` es el encabezado que lo ofrece si fueron varias */
@Stable
internal class Cambio(val codigos: List<String>, val previas: Map<String, Marca?>, val seccion: String?)

/** Cuánto dura el realce de una fila recién marcada o recién abierta */
internal const val REALCE_MS = 1200

/**
 * Todo lo que la lista decide a partir del estado de la carrera, y lo poco
 * que recuerda mientras está en pantalla.
 *
 * La estructura (qué fila va en qué sección y en qué orden) se congela al
 * marcar la primera: la fila marcada se queda en su sitio con su realce y su
 * «Deshacer» en lugar de saltar a otra sección bajo el dedo. Lo que se pinta
 * de cada fila sí es siempre lo de ahora. La estructura se suelta al salir de
 * la lista (esta sesión muere con la vista) o al cambiar de orden o de casillas.
 */
@Stable
internal class SesionLista(
    val estado: EstadoCarrera,
    val navegacion: NavegacionLista,
    private val alcance: CoroutineScope,
) {
    /** Los semestres que puede esperar cada inscribible; llegan fuera del hilo principal y ordenan la sección */
    var margenes by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var congelada by mutableStateOf<List<GrupoLista>?>(null)

    /** El último cambio: ofrece «Deshacer» hasta que se sale de la lista */
    var cambio by mutableStateOf<Cambio?>(null)
        private set

    /** Las filas con el realce encendido: código → vez. Se apagan solas a los 1,2 s */
    val realces = mutableStateMapOf<String, Int>()
    private var vez = 0

    val universo: UniversoLista by derivedStateOf {
        universoDeLista(
            nodos = estado.nodos.map { it.asignatura },
            grupos = estado.pensum.grupos,
            enCasilla = estado::enCasilla,
            estados = estado.estados,
            situaciones = estado.situaciones,
            nombreDe = { estado.pensum.porCodigo[it]?.nombre },
        )
    }

    /** El semestre por el que vas, el mismo de la portada y la barra */
    val actual: Int? by derivedStateOf { semestreActualDeLista(estado.nodos.map { it.asignatura }, estado::enCasilla, estado.estados) }

    private val vivaPorEstado: List<GrupoLista> by derivedStateOf { estructuraPorEstado(universo, margenes, actual) }
    private val vivaPorSemestre: List<GrupoLista> by derivedStateOf { estructuraPorSemestre(universo) }

    /** Lo que se pinta: lo congelado si ya se marcó algo, y si no, lo de ahora */
    val estructura: List<GrupoLista> get() = congelada ?: if (navegacion.porEstado) vivaPorEstado else vivaPorSemestre

    /** La primera materia de «Puedes inscribir»: la que se elige al entrar acostado para que el panel no quede vacío */
    val primera: String? get() {
        val grupos = estructura
        val delPrimero = grupos.firstOrNull { it.clave == SeccionEstado.INSCRIBIR.clave } ?: grupos.firstOrNull()
        return (delPrimero?.codigos.orEmpty() + grupos.flatMap { it.codigos }).firstOrNull { universo.materia(it) != null }
    }

    /** Las UC que cursas ahora: el «llevas 4» del muelle */
    val ucCursando: Int get() = universo.materias.filter { it.situacion == Situacion.CURSANDO }.sumOf { it.uc ?: 0 }

    /** Mide el margen de cada inscribible con el simulador de la hoja de avance, fuera del hilo principal */
    suspend fun medir() {
        val sim = estado.simulador
        val codigos = universo.materias.filter { it.situacion == Situacion.INSCRIBIBLE }.map { it.codigo }
        margenes = withContext(Dispatchers.Default) {
            codigos.mapNotNull { c -> margenDeMateria(sim, c)?.let { c to it.margen } }.toMap()
        }
    }

    fun soltar() {
        congelada = null
    }

    fun ordenar(porEstado: Boolean) {
        navegacion.porEstado = porEstado
        soltar()
        cambio = null
    }

    /** Marca con los métodos de siempre del estado de la carrera, recuerda cómo estaba y enciende el realce */
    fun marcar(cambios: Map<String, Marca?>, seccion: String? = null) {
        if (cambios.isEmpty()) return
        if (congelada == null) congelada = estructura
        val previas = cambios.keys.associateWith { estado.marcas[it] }
        estado.marcarVarias(cambios)
        cambio = Cambio(cambios.keys.toList(), previas, seccion)
        // Con la página abierta la lista no se ve: el realce llega al volver (ver `realzar`)
        if (!navegacion.abierta) cambios.keys.forEach(::realzar)
    }

    fun deshacer() {
        val c = cambio ?: return
        estado.marcarVarias(c.previas)
        cambio = null
        if (!navegacion.abierta) c.codigos.forEach(::realzar)
    }

    /** Enciende el realce de una fila: se apaga solo en 1,2 s */
    fun realzar(codigo: String) {
        val mia = ++vez
        realces[codigo] = mia
        alcance.launch {
            delay(REALCE_MS + 100L)
            if (realces[codigo] == mia) realces.remove(codigo)
        }
    }
}
