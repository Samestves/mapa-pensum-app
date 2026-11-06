package mpensum.plataforma

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import mpensum.tema.Acento
import mpensum.tema.Juego
import mpensum.tema.Tema

/**
 * Cómo quiere ver la app: uno de los siete juegos o lo que diga el teléfono.
 * Es una sola para toda la app, se cambie desde Ajustes o desde la hoja de
 * avance. Se guarda con el nombre del juego en minúsculas, así que lo viejo
 * («claro», «oscuro», «sistema») sigue leyéndose.
 */
enum class Apariencia(val juego: Juego?) {
    OSCURO(Juego.OSCURO),
    GRAFITO(Juego.GRAFITO),
    NEGRO(Juego.NEGRO),
    MEDIANOCHE(Juego.MEDIANOCHE),
    CLARO(Juego.CLARO),
    PAPEL(Juego.PAPEL),
    EXTERIOR(Juego.EXTERIOR),
    SISTEMA(null);

    val valor: String get() = name.lowercase()

    /** El nombre que se enseña */
    val nombre: String get() = juego?.nombre ?: "Sistema"

    /** El juego que se pinta: el propio, o Claro u Oscuro según la noche del teléfono */
    fun juego(nocheDelSistema: Boolean): Juego = juego ?: if (nocheDelSistema) Juego.OSCURO else Juego.CLARO

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
    val juego = Preferencias.tema.value.juego(nocheDelSistema)
    Tema.juego = juego
    ponerPreferenciasEnTema(juego)
}

/** Todo menos el juego (que App cambia con fundido): acento, texto, luz, movimiento, códigos y modo ligero */
fun ponerPreferenciasEnTema(juego: Juego) {
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
