package mpensum.carrera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import mpensum.datos.Asignatura
import mpensum.datos.HorarioGuardado
import mpensum.logica.Ahora
import mpensum.logica.FalloLectura
import mpensum.plataforma.LectorCaptura
import mpensum.logica.MateriaLeida
import mpensum.logica.diaDelMes
import mpensum.logica.Sesion
import mpensum.logica.clases
import mpensum.logica.franjaNueva
import mpensum.logica.idNuevo
import mpensum.logica.sinRepetidas
import mpensum.logica.momentoDe
import mpensum.logica.porDia
import mpensum.logica.rangoDeClases
import mpensum.plataforma.HoraLocal
import mpensum.plataforma.horaLocal
import mpensum.plataforma.rememberLectorCaptura
import mpensum.tema.Paleta

/** La clase que se está creando o editando; sin `codigo` no se puede guardar */
@Immutable
data class Borrador(
    val id: String?,
    val codigo: String?,
    val dia: Int,
    val inicio: Int,
    val fin: Int,
    val aula: String = "",
    val seccion: String = "",
    val profesor: String = "",
)

/** Qué hoja del horario está abierta */
sealed interface HojaHorario {
    data class Detalle(val id: String) : HojaHorario
    data class Editar(val borrador: Borrador) : HojaHorario
    data object Menu : HojaHorario
    data object Borrar : HojaHorario
    /** «Antes de guardar»: las materias leídas de la captura, para revisarlas */
    data object Confirmar : HojaHorario
}

/** Por dónde va la lectura de una captura */
enum class FaseLectura { ELEGIR, LEYENDO, VOLANDO, CONFIRMAR }

/**
 * Una captura en lectura: la foto, cuántas materias ve el lector y las que ya
 * leyó. Nada se guarda hasta que se confirma: hasta entonces es provisional.
 */
@Stable
class Lectura {
    var foto by mutableStateOf<ImageBitmap?>(null)
    var fase by mutableStateOf(FaseLectura.ELEGIR)
    var total by mutableIntStateOf(0)

    /** Por qué no se pudo abrir o leer la captura; se dice dentro de la propia pantalla de lectura */
    var fallo by mutableStateOf<FalloLectura?>(null)
    val leidas = mutableStateListOf<MateriaLeida>()
    val sesiones: List<Sesion> get() = leidas.flatMap { it.sesiones }
}

/**
 * El horario de una carrera abierta: sus clases (guardadas por carrera), la
 * hora de ahora y lo que se mira (qué día, qué hoja). Lo
 * comparten la vista y sus hojas, que se montan en sitios distintos del
 * interior. `reloj` es el del teléfono; las pruebas inyectan uno fijo.
 */
@Stable
class EstadoHorario(val carrera: EstadoCarrera, private val reloj: () -> HoraLocal = ::horaLocal) {
    private val porCodigo: Map<String, Asignatura> = carrera.pensum.porCodigo

    var sesiones by mutableStateOf(HorarioGuardado.leer(carrera.pensum.slug, carrera.pensum.todas.mapTo(HashSet()) { it.codigo }))
        private set

    private var epoca = reloj().epoca

    var ahora by mutableStateOf(reloj().let { Ahora(it.dia, it.minuto) })
        private set

    var dia by mutableStateOf(if (ahora.dia < 5) ahora.dia else 0)
    var hoja by mutableStateOf<HojaHorario?>(null)

    /** Quien lee las capturas; sin él (la plataforma no puede abrir la galería) «Subir mi horario» lleva a crearlo a mano */
    var lector: LectorCaptura? = null

    /** La lectura en curso, si hay una */
    var lectura by mutableStateOf<Lectura?>(null)

    /** El día del mes de cada columna de la semana en curso (lunes a viernes), o null si el reloj no lo da */
    val numerosDia: List<Int>? get() = if (epoca > 0) (0 until 5).map { diaDelMes(epoca - ahora.dia + it) } else null

    /** Las clases de cada día, de lunes a domingo, en orden */
    val dias by derivedStateOf { porDia(sesiones) }
    val momento by derivedStateOf { momentoDe(dias, ahora) }

    /** De qué hora a qué hora va la semana: la miniatura y la rejilla usan la misma */
    val rango by derivedStateOf { rangoDeClases(sesiones) }

    /** Relee el reloj y devuelve cuánto falta para el próximo minuto, en ms */
    fun actualizarAhora(): Long {
        val h = reloj()
        ahora = Ahora(h.dia, h.minuto)
        epoca = h.epoca
        return (60 - h.segundo) * 1000L
    }

    fun sesion(id: String): Sesion? = sesiones.firstOrNull { it.id == id }
    fun materia(codigo: String): Asignatura? = porCodigo[codigo]
    fun nombre(codigo: String): String = porCodigo[codigo]?.nombre ?: codigo

    /** El mismo color que la materia tiene en el mapa */
    fun color(codigo: String): Color = porCodigo[codigo]?.let(carrera.colores::de) ?: Paleta.suave

    fun enCurso(s: Sesion) = s.dia == ahora.dia && s.inicio <= ahora.minuto && ahora.minuto < s.fin
    fun pasada(s: Sesion) = s.dia == ahora.dia && s.fin <= ahora.minuto

    /** «Subir mi horario»: abre la lectura si hay lector; si no, lo único posible es crear la primera clase */
    fun subir() {
        if (lector == null) abrirNueva(if (ahora.dia < 5) ahora.dia else 0, 7 * 60) else lectura = Lectura()
    }

    fun cancelarLectura() {
        lectura = null
        hoja = null
    }

    /** Lo que guardaría la lectura: sin las clases que ya tenías idénticas */
    val nuevasDeLaLectura: List<Sesion> get() = lectura?.let { sinRepetidas(it.sesiones, sesiones) }.orEmpty()

    /** Confirma la lectura: las clases leídas que aún no tenías se suman, con ids propios, y se mira hoy (o el lunes en fin de semana) */
    fun guardarLectura() {
        if (lectura == null) return
        val usados = sesiones.map { it.id }.toMutableSet()
        val nuevas = nuevasDeLaLectura.map { s -> s.copy(id = idNuevo(s.codigo, s.dia, s.inicio, usados).also { usados += it }) }
        cambiar(sesiones + nuevas)
        lectura = null
        hoja = null
        dia = if (ahora.dia < 5) ahora.dia else 0
    }

    fun abrirNueva(dia: Int, inicio: Int) {
        val (desde, hasta) = franjaNueva(dias[dia], inicio)
        hoja = HojaHorario.Editar(Borrador(null, null, dia, desde, hasta))
    }

    fun editar(s: Sesion) {
        hoja = HojaHorario.Editar(Borrador(s.id, s.codigo, s.dia, s.inicio, s.fin, s.aula, s.seccion, s.profesor))
    }

    /** Crea o reemplaza: el id decide cuál de las dos. Al guardar, se mira el día de la clase */
    fun guardar(b: Borrador) {
        val codigo = b.codigo ?: return
        val id = b.id ?: idNuevo(codigo, b.dia, b.inicio, sesiones.map { it.id }.toSet())
        val nueva = Sesion(id, codigo, b.dia, b.inicio, b.fin, b.seccion.trim(), b.aula.trim(), b.profesor.trim())
        cambiar(sesiones.filter { it.id != id } + nueva)
        dia = b.dia
        hoja = null
    }

    fun quitar(id: String) {
        cambiar(sesiones.filter { it.id != id })
        hoja = null
    }

    fun vaciar() {
        cambiar(emptyList())
        hoja = null
    }

    private fun cambiar(nuevas: List<Sesion>) {
        sesiones = nuevas
        HorarioGuardado.guardar(carrera.pensum.slug, nuevas)
    }
}

/** El horario de la carrera, el mismo mientras no cambie de carrera */
@Composable
fun rememberEstadoHorario(estado: EstadoCarrera): EstadoHorario {
    val lector = rememberLectorCaptura(estado.pensum.todas)
    return remember(estado) { EstadoHorario(estado) }.also { it.lector = lector }
}
