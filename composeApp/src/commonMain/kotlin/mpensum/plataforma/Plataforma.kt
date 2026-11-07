package mpensum.plataforma

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import mpensum.carrera.Vista
import mpensum.tema.Acento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Juego
import mpensum.tema.Tema

/**
 * Cómo quiere ver la app: uno de los trece juegos o lo que diga el teléfono.
 * Es una sola para toda la app, se cambie desde Ajustes o desde la hoja de
 * avance. Se guarda con el nombre del juego en minúsculas, así que lo viejo
 * («claro», «oscuro», «exterior», «sistema») sigue leyéndose.
 */
enum class Apariencia(val juego: Juego?) {
    OSCURO(Juego.OSCURO),
    NEGRO(Juego.NEGRO),
    GRAFITO(Juego.GRAFITO),
    MEDIANOCHE(Juego.MEDIANOCHE),
    CIRUELA(Juego.CIRUELA),
    MORICHAL(Juego.MORICHAL),
    CLARO(Juego.CLARO),
    PAPEL(Juego.PAPEL),
    NIEBLA(Juego.NIEBLA),
    ALUMINIO(Juego.ALUMINIO),
    CUARZO(Juego.CUARZO),
    EXTERIOR_OSCURO(Juego.EXTERIOR_OSCURO),
    EXTERIOR(Juego.EXTERIOR),
    SISTEMA(null);

    val valor: String get() = name.lowercase()

    /** El nombre que se enseña */
    val nombre: String get() = juego?.nombre ?: "Sistema"

    /** El juego que se pinta: el propio, o el de día o el de noche según la noche del teléfono */
    fun juego(nocheDelSistema: Boolean, deDia: Juego, deNoche: Juego): Juego = juego ?: if (nocheDelSistema) deNoche else deDia

    companion object {
        /** Lo guardado, o la oscura: es la que existe hoy y con la que nació la app */
        fun de(valor: String?): Apariencia = entries.firstOrNull { it.valor == valor } ?: OSCURO
    }
}

/** Lo poco que la app guarda en el teléfono */
expect object Preferencias {
    fun bienvenidaVista(): Boolean
    fun marcarBienvenidaVista()
    fun carrera(): String?
    fun guardarCarrera(slug: String)

    /** Un texto guardado con su clave, o null si no hay */
    fun leerTexto(clave: String): String?
    fun guardarTexto(clave: String, texto: String)

    /**
     * La apariencia elegida, observable: quien pinte la app la recoge
     * (collectAsState) y cambia en el acto, sin reabrir nada.
     */
    val tema: StateFlow<Apariencia>
    fun guardarTema(apariencia: Apariencia)

    /** El acento: null es el color de la carrera */
    val acento: StateFlow<Acento?>
    fun guardarAcento(acento: Acento?)

    /** El tamaño del texto («texto»: normal o grande) */
    val textoGrande: StateFlow<Boolean>
    fun guardarTextoGrande(grande: Boolean)

    /** La luz de fondo de la portada; null mientras no se haya elegido (entonces manda el tema) */
    val luzFondo: StateFlow<Boolean?>
    fun guardarLuzFondo(encendida: Boolean)

    /** Reducir movimiento; null mientras no se haya elegido (entonces manda el sistema) */
    val reducirMovimiento: StateFlow<Boolean?>
    fun guardarReducirMovimiento(reducir: Boolean)

    /** El modo ligero; null es automático (entonces manda [telefonoModesto]) */
    val modoLigero: StateFlow<Boolean?>
    fun guardarModoLigero(ligero: Boolean?)

    /** El código de 7 cifras en las tarjetas del mapa */
    val codigos: StateFlow<Boolean>
    fun guardarCodigos(visibles: Boolean)

    /** El fondo del mapa; null es el del tema, y vuelve a null al cambiar de tema */
    val fondoMapa: StateFlow<Fondo?>
    fun guardarFondoMapa(fondo: Fondo?)

    val cables: StateFlow<FormaCables>
    fun guardarCables(forma: FormaCables)

    val grosorCables: StateFlow<Grosor>
    fun guardarGrosorCables(grosor: Grosor)

    /** Lo aprobado se dibuja al 45 % */
    val atenuarAprobadas: StateFlow<Boolean>
    fun guardarAtenuarAprobadas(atenuar: Boolean)

    /** Más contraste: textos, bordes y rejilla hacia la tinta */
    val contraste: StateFlow<Boolean>
    fun guardarContraste(activo: Boolean)

    /** Con Sistema: el tema de día y el de noche (por defecto Claro y Oscuro) */
    val temaDia: StateFlow<Juego>
    fun guardarTemaDia(juego: Juego)
    val temaNoche: StateFlow<Juego>
    fun guardarTemaNoche(juego: Juego)

    /** Al abrir la app, entrar directo a la carrera elegida (si hay) */
    val abrirEnCarrera: StateFlow<Boolean>
    fun guardarAbrirEnCarrera(abrir: Boolean)

    /** La vista con que se abre una carrera: el mapa o la lista */
    val vistaInicial: StateFlow<Vista>
    fun guardarVistaInicial(vista: Vista)

    /** Las horas del horario en 24 h */
    val hora24: StateFlow<Boolean>
    fun guardarHora24(activo: Boolean)

    /** La vibración de la lista, la barra, el mapa y la ficha */
    val vibracion: StateFlow<Boolean>
    fun guardarVibracion(activa: Boolean)
}

/**
 * Si el teléfono es de gama baja: poca memoria, pocos núcleos o lentos, o sin
 * desenfoque nativo. Se mide una vez al arrancar; con él decide el modo
 * ligero mientras esté en automático.
 */
expect fun telefonoModesto(): Boolean

/** Si el teléfono tiene las animaciones quitadas (en Android, la escala del animador a 0) */
expect fun animacionesQuitadas(): Boolean

/** Vuelca las preferencias ya leídas al [Tema]: lo que se pinta lee de ahí */
fun aplicarAlTema(nocheDelSistema: Boolean) {
    val juego = Preferencias.tema.value.juego(nocheDelSistema, Preferencias.temaDia.value, Preferencias.temaNoche.value)
    Tema.juego = juego
    ponerPreferenciasEnTema(juego)
}

/** Todo menos el juego (que App cambia con fundido): lo elegido en Personalizar, ya resuelto */
fun ponerPreferenciasEnTema(juego: Juego) {
    Tema.contraste = Preferencias.contraste.value
    Tema.fondo = Preferencias.fondoMapa.value ?: juego.fondo
    Tema.cables = Preferencias.cables.value
    Tema.grosor = Preferencias.grosorCables.value
    Tema.atenuarAprobadas = Preferencias.atenuarAprobadas.value
    Tema.hora24 = Preferencias.hora24.value
    Tema.vibracion = Preferencias.vibracion.value
    Tema.acento = Preferencias.acento.value
    Tema.textoGrande = Preferencias.textoGrande.value
    Tema.luzFondo = Preferencias.luzFondo.value ?: (juego.aurora > 0f)
    Tema.movimiento = Preferencias.reducirMovimiento.value ?: animacionesQuitadas()
    Tema.codigos = Preferencias.codigos.value
    Tema.ligero = Preferencias.modoLigero.value ?: telefonoModesto()
}

/** La versión instalada ("0.1"), o null si el sistema no la da */
expect fun versionApp(): String?

/** El gesto o botón de volver del sistema, mientras `activo` */
@Composable
expect fun ManejarAtras(activo: Boolean, alVolver: () -> Unit)

/**
 * Los iconos de las barras del sistema (hora, batería, gestos): claros sobre
 * la app oscura y oscuros sobre la clara. Las barras siguen transparentes.
 */
@Composable
expect fun BarrasDelSistema(claro: Boolean)

/**
 * La versión del sistema (en Android, el SDK: 31 es Android 12). El desenfoque
 * de la cabecera solo se pide desde ahí: antes cae en RenderScript, que no pinta.
 */
expect fun versionSdk(): Int
