package mpensum.plataforma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpensum.navegacion.Vista
import mpensum.tema.Acento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Juego

/**
 * Lo único que la plataforma tiene que dar para guardar: pares clave-valor de texto y de sí/no.
 * Un valor null borra la clave. Las claves y los valores guardados son los de siempre: quien actualiza
 * la app no pierde su configuración (PreferenciasTest los fija).
 */
interface Almacen {
    fun texto(clave: String): String?

    /** Null si no hay clave: así se distingue «no elegido» de «apagado» */
    fun booleano(clave: String): Boolean?
    fun guardarTexto(clave: String, valor: String?)
    fun guardarBooleano(clave: String, valor: Boolean?)
}

/**
 * Una preferencia: su valor observable, cómo se lee del almacén (null es «no hay o no vale»: sale el
 * valor por defecto) y cómo se escribe. Mientras no haya almacén (antes de [Preferencias.usar]) vale
 * su valor por defecto y lo que se guarde no se escribe en ningún sitio.
 */
class Preferencia<T> internal constructor(
    private val clave: String,
    private val defecto: T,
    private val leer: (Almacen, String) -> T?,
    private val escribir: (Almacen, String, T) -> Unit,
) {
    private val estado = MutableStateFlow(defecto)
    val flujo: StateFlow<T> = estado.asStateFlow()

    internal fun cargar(almacen: Almacen?) {
        estado.value = almacen?.let { leer(it, clave) } ?: defecto
    }

    internal fun guardar(almacen: Almacen?, valor: T) {
        estado.value = valor
        almacen?.let { escribir(it, clave, valor) }
    }

    internal companion object {
        fun booleano(clave: String, defecto: Boolean) = Preferencia(clave, defecto, Almacen::booleano, Almacen::guardarBooleano)

        /** Sí/no que puede no estar elegido: null es «que mande otra cosa» (el tema, el sistema o el teléfono) */
        fun booleanoOpcional(clave: String) = Preferencia<Boolean?>(clave, null, Almacen::booleano, Almacen::guardarBooleano)

        /** Un enum guardado por su nombre; un nombre que ya no existe es el valor por defecto */
        fun <E : Enum<E>> enumeracion(clave: String, defecto: E, valores: List<E>) = Preferencia(
            clave, defecto,
            { a, k -> a.texto(k)?.let { n -> valores.firstOrNull { it.name == n } } },
            { a, k, v -> a.guardarTexto(k, v.name) },
        )

        fun <E : Enum<E>> enumeracionOpcional(clave: String, valores: List<E>) = Preferencia<E?>(
            clave, null,
            { a, k -> a.texto(k)?.let { n -> valores.firstOrNull { it.name == n } } },
            { a, k, v -> a.guardarTexto(k, v?.name) },
        )
    }
}

/** Lo poco que la app guarda en el teléfono */
object Preferencias {
    private var almacen: Almacen? = null

    private val _tema = Preferencia(
        "tema", Apariencia.OSCURO,
        { a, k -> Apariencia.de(a.texto(k)) },
        { a, k, v -> a.guardarTexto(k, v.valor) },
    )
    private val _acento = Preferencia<Acento?>(
        "acento", null,
        { a, k -> Acento.de(a.texto(k)) },
        { a, k, v -> a.guardarTexto(k, v?.valor) },
    )
    private val _textoGrande = Preferencia(
        "texto", false,
        { a, k -> a.texto(k) == "grande" },
        { a, k, v -> a.guardarTexto(k, if (v) "grande" else "normal") },
    )
    private val _luzFondo = Preferencia.booleanoOpcional("luzFondo")
    private val _reducirMovimiento = Preferencia.booleanoOpcional("movimiento")
    private val _modoLigero = Preferencia.booleanoOpcional("ligero")
    private val _codigos = Preferencia.booleano("codigos", true)
    private val _fondoMapa = Preferencia.enumeracionOpcional("fondoMapa", Fondo.entries)
    private val _cables = Preferencia.enumeracion("cables", FormaCables.CURVOS, FormaCables.entries)
    private val _grosorCables = Preferencia.enumeracion("grosorCables", Grosor.NORMAL, Grosor.entries)
    private val _atenuarAprobadas = Preferencia.booleano("atenuarAprobadas", false)
    private val _contraste = Preferencia.booleano("contraste", false)
    private val _temaDia = juego("temaDia", Juego.CLARO)
    private val _temaNoche = juego("temaNoche", Juego.OSCURO)
    private val _abrirEnCarrera = Preferencia.booleano("abrirEnCarrera", false)
    private val _vistaInicial = Preferencia(
        "vistaInicial", Vista.MAPA,
        // El horario no es una vista con la que se abra una carrera
        { a, k -> a.texto(k)?.let { n -> Vista.entries.firstOrNull { it.name == n } }?.takeIf { it != Vista.HORARIO } },
        { a, k, v -> a.guardarTexto(k, v.name) },
    )
    private val _hora24 = Preferencia.booleano("hora24", false)
    private val _vibracion = Preferencia.booleano("vibracion", true)

    /** El Morichal de antes pasa a Medianoche */
    private fun juego(clave: String, defecto: Juego) = Preferencia(
        clave, defecto,
        { a, k -> a.texto(k)?.let { n -> if (n == "MORICHAL") Juego.MEDIANOCHE else Juego.entries.firstOrNull { it.name == n } } },
        { a, k, v -> a.guardarTexto(k, v.name) },
    )

    private val todas get() = listOf<Preferencia<*>>(
        _tema, _acento, _textoGrande, _luzFondo, _reducirMovimiento, _modoLigero, _codigos, _fondoMapa, _cables,
        _grosorCables, _atenuarAprobadas, _contraste, _temaDia, _temaNoche, _abrirEnCarrera, _vistaInicial, _hora24, _vibracion,
    )

    /** Lo llama la plataforma al arrancar: desde aquí se lee y se escribe en este almacén */
    fun usar(nuevo: Almacen) {
        almacen = nuevo
        todas.forEach { it.cargar(nuevo) }
    }

    fun bienvenidaVista(): Boolean = almacen?.booleano("bienvenida") ?: false
    fun marcarBienvenidaVista() {
        almacen?.guardarBooleano("bienvenida", true)
    }

    fun carrera(): String? = almacen?.texto("carrera")
    fun guardarCarrera(slug: String) {
        almacen?.guardarTexto("carrera", slug)
    }

    /** Un texto guardado con su clave, o null si no hay */
    fun leerTexto(clave: String): String? = almacen?.texto(clave)
    fun guardarTexto(clave: String, texto: String) {
        almacen?.guardarTexto(clave, texto)
    }

    /**
     * La apariencia elegida, observable: quien pinte la app la recoge
     * (collectAsState) y cambia en el acto, sin reabrir nada.
     */
    val tema: StateFlow<Apariencia> get() = _tema.flujo
    fun guardarTema(apariencia: Apariencia) {
        // Cada tema trae su fondo: elegir otro tema suelta el fondo que se hubiera elegido a mano
        if (apariencia != _tema.flujo.value) guardarFondoMapa(null)
        _tema.guardar(almacen, apariencia)
    }

    /** El acento: null es el color de la carrera */
    val acento: StateFlow<Acento?> get() = _acento.flujo
    fun guardarAcento(acento: Acento?) = _acento.guardar(almacen, acento)

    /** El tamaño del texto («texto»: normal o grande) */
    val textoGrande: StateFlow<Boolean> get() = _textoGrande.flujo
    fun guardarTextoGrande(grande: Boolean) = _textoGrande.guardar(almacen, grande)

    /** La luz de fondo de la portada; null mientras no se haya elegido (entonces manda el tema) */
    val luzFondo: StateFlow<Boolean?> get() = _luzFondo.flujo
    fun guardarLuzFondo(encendida: Boolean) = _luzFondo.guardar(almacen, encendida)

    /** Reducir movimiento; null mientras no se haya elegido (entonces manda el sistema) */
    val reducirMovimiento: StateFlow<Boolean?> get() = _reducirMovimiento.flujo
    fun guardarReducirMovimiento(reducir: Boolean) = _reducirMovimiento.guardar(almacen, reducir)

    /** El modo ligero; null es automático (entonces manda [telefonoModesto]) */
    val modoLigero: StateFlow<Boolean?> get() = _modoLigero.flujo
    fun guardarModoLigero(ligero: Boolean?) = _modoLigero.guardar(almacen, ligero)

    /** El código de 7 cifras en las tarjetas del mapa */
    val codigos: StateFlow<Boolean> get() = _codigos.flujo
    fun guardarCodigos(visibles: Boolean) = _codigos.guardar(almacen, visibles)

    /** El fondo del mapa; null es el del tema, y vuelve a null al cambiar de tema */
    val fondoMapa: StateFlow<Fondo?> get() = _fondoMapa.flujo
    fun guardarFondoMapa(fondo: Fondo?) = _fondoMapa.guardar(almacen, fondo)

    val cables: StateFlow<FormaCables> get() = _cables.flujo
    fun guardarCables(forma: FormaCables) = _cables.guardar(almacen, forma)

    val grosorCables: StateFlow<Grosor> get() = _grosorCables.flujo
    fun guardarGrosorCables(grosor: Grosor) = _grosorCables.guardar(almacen, grosor)

    /** Lo aprobado se dibuja al 45 % */
    val atenuarAprobadas: StateFlow<Boolean> get() = _atenuarAprobadas.flujo
    fun guardarAtenuarAprobadas(atenuar: Boolean) = _atenuarAprobadas.guardar(almacen, atenuar)

    /** Más contraste: textos, bordes y rejilla hacia la tinta */
    val contraste: StateFlow<Boolean> get() = _contraste.flujo
    fun guardarContraste(activo: Boolean) = _contraste.guardar(almacen, activo)

    /** Con Sistema: el tema de día y el de noche (por defecto Claro y Oscuro) */
    val temaDia: StateFlow<Juego> get() = _temaDia.flujo
    fun guardarTemaDia(juego: Juego) = _temaDia.guardar(almacen, juego)
    val temaNoche: StateFlow<Juego> get() = _temaNoche.flujo
    fun guardarTemaNoche(juego: Juego) = _temaNoche.guardar(almacen, juego)

    /** Al abrir la app, entrar directo a la carrera elegida (si hay) */
    val abrirEnCarrera: StateFlow<Boolean> get() = _abrirEnCarrera.flujo
    fun guardarAbrirEnCarrera(abrir: Boolean) = _abrirEnCarrera.guardar(almacen, abrir)

    /** La vista con que se abre una carrera: el mapa o la lista */
    val vistaInicial: StateFlow<Vista> get() = _vistaInicial.flujo
    fun guardarVistaInicial(vista: Vista) = _vistaInicial.guardar(almacen, vista)

    /** Las horas del horario en 24 h */
    val hora24: StateFlow<Boolean> get() = _hora24.flujo
    fun guardarHora24(activo: Boolean) = _hora24.guardar(almacen, activo)

    /** La vibración de la lista, la barra, el mapa y la ficha */
    val vibracion: StateFlow<Boolean> get() = _vibracion.flujo
    fun guardarVibracion(activa: Boolean) = _vibracion.guardar(almacen, activa)
}
