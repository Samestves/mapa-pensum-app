package mpensum.carrera

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import mpensum.navegacion.Vista

/**
 * Dónde está el usuario dentro de una carrera: la vista, la materia abierta, la casilla de electiva que se
 * está eligiendo, la hoja del avance y lo que hay que encuadrar en el mapa. Cada cambio es una función que
 * deja todo coherente en el mismo evento: nada se arregla un cuadro después con un efecto.
 *
 * No sabe de pantalla ni de cámara: lo que depende del estado de la carrera (si una casilla está vacía) se
 * le pasa como función, y por eso se prueba en la JVM sin dibujar nada. De todo ello solo la vista se
 * guarda al recrear la actividad ([Guardado]); lo demás empieza limpio, como siempre.
 */
@Stable
class NavegacionInterior(vistaInicial: Vista = Vista.MAPA) {
    var vista by mutableStateOf(vistaInicial)
        private set

    /** La materia cuya ficha (o burbuja, o página de la lista) está abierta */
    var seleccionado by mutableStateOf<String?>(null)
        private set

    /** La casilla de electiva cuyo selector está abierto, y si elegir en ella también aprueba la materia */
    var casillaAbierta by mutableStateOf<String?>(null)
        private set
    var aprobarAlElegir by mutableStateOf(false)
        private set
    var avanceAbierto by mutableStateOf(false)
        private set

    /** Las materias que pidió «Verlas en el mapa» la hoja del avance: se encuadran cuando el mapa ya se ve */
    var porEncuadrar by mutableStateOf<List<String>?>(null)
        private set

    /** La materia a la que ya se le hizo sitio: la ficha cambia de alto al animarse y no debe empujar el mapa cada vez */
    var encuadrada by mutableStateOf<String?>(null)
        private set

    /** Cambiar de vista suelta la materia abierta: la ficha es de la vista en la que se pidió. Elegir la misma no toca nada */
    fun cambiarVista(nueva: Vista) {
        if (nueva == vista) return
        vista = nueva
        soltarSeleccion()
    }

    /**
     * Abrir (o cerrar con null) la ficha de una materia. Acostado, sobre el mapa, una casilla de electiva vacía
     * no cabe en la burbuja: se abre el selector a pantalla completa en vez de la ficha.
     */
    fun seleccionar(codigo: String?, acostado: Boolean, esCasillaVacia: (String) -> Boolean) {
        if (codigo == null) return soltarSeleccion()
        seleccionado = codigo
        casillaVaciaAcostado(acostado, esCasillaVacia)
    }

    /** Girar el teléfono: el panel de la ficha cambia de sitio, así que la materia se vuelve a encuadrar */
    fun girar(acostado: Boolean, esCasillaVacia: (String) -> Boolean) {
        encuadrada = null
        casillaVaciaAcostado(acostado, esCasillaVacia)
    }

    private fun casillaVaciaAcostado(acostado: Boolean, esCasillaVacia: (String) -> Boolean) {
        val c = seleccionado ?: return
        if (acostado && vista == Vista.MAPA && esCasillaVacia(c)) {
            soltarSeleccion()
            abrirCasilla(c, aprobar = false)
        }
    }

    fun soltarSeleccion() {
        seleccionado = null
        encuadrada = null
    }

    fun abrirCasilla(casilla: String, aprobar: Boolean) {
        aprobarAlElegir = aprobar
        casillaAbierta = casilla
    }

    fun cerrarCasilla() {
        casillaAbierta = null
    }

    fun abrirAvance() {
        avanceAbierto = true
    }

    fun cerrarAvance() {
        avanceAbierto = false
    }

    /** Llevar al mapa unas materias (desde la lista o desde la hoja del avance, que se cierra) y pedir que se encuadren */
    fun verEnMapa(codigos: List<String>, cerrandoAvance: Boolean = false) {
        if (cerrandoAvance) avanceAbierto = false
        cambiarVista(Vista.MAPA)
        porEncuadrar = codigos
    }

    /** El mapa ya encuadró lo que se pidió */
    fun encuadreHecho() {
        porEncuadrar = null
    }

    /** La ficha de `codigo` ya tiene su sitio en el mapa */
    fun marcarEncuadrada(codigo: String) {
        encuadrada = codigo
    }

    /** Si hay una hoja encima (electivas o avance): lo que hace que el mapa se congele y la lista se dé por tapada */
    val hayHoja: Boolean get() = casillaAbierta != null || avanceAbierto

    companion object {
        /** Solo se guarda la vista: lo demás empieza limpio al recrearse, como cuando era cada cosa suelta */
        val Guardable: Saver<NavegacionInterior, String> = Saver(
            save = { it.vista.name },
            restore = { NavegacionInterior(Vista.entries.firstOrNull { v -> v.name == it } ?: Vista.MAPA) },
        )
    }
}
