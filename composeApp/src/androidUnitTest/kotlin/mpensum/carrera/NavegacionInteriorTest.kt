package mpensum.carrera

import androidx.compose.runtime.saveable.SaverScope
import mpensum.navegacion.Vista
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dónde está el usuario dentro de una carrera, sin pantalla: cada transición deja todo coherente en el mismo evento */
class NavegacionInteriorTest {

    private val ninguna: (String) -> Boolean = { false }
    private val vacia: (String) -> Boolean = { it == "hueco" }

    @Test
    fun cambiarDeVistaSueltaLaSeleccionEnElMismoEvento() {
        val n = NavegacionInterior()
        n.seleccionar("0723913", acostado = false, esCasillaVacia = ninguna)
        n.marcarEncuadrada("0723913")
        n.cambiarVista(Vista.LISTA)
        // Sin cuadro de por medio: ya no hay materia ni encuadre
        assertEquals(Vista.LISTA, n.vista)
        assertNull(n.seleccionado)
        assertNull(n.encuadrada)
    }

    @Test
    fun elegirLaMismaVistaNoTocaNada() {
        val n = NavegacionInterior()
        n.seleccionar("0723913", false, ninguna)
        n.cambiarVista(Vista.MAPA)
        assertEquals("0723913", n.seleccionado)
    }

    @Test
    fun abrirYCerrarLaFicha() {
        val n = NavegacionInterior()
        n.seleccionar("0723913", false, ninguna)
        assertEquals("0723913", n.seleccionado)
        n.seleccionar("0724001", false, ninguna)
        assertEquals("0724001", n.seleccionado)
        n.seleccionar(null, false, ninguna)
        assertNull(n.seleccionado)
    }

    @Test
    fun acostadoEnElMapaUnaCasillaVaciaAbreElSelectorYNoLaFicha() {
        val n = NavegacionInterior()
        n.seleccionar("hueco", acostado = true, esCasillaVacia = vacia)
        assertNull(n.seleccionado)
        assertEquals("hueco", n.casillaAbierta)
        assertFalse(n.aprobarAlElegir)
        assertTrue(n.hayHoja)
        n.cerrarCasilla()
        assertFalse(n.hayHoja)
    }

    @Test
    fun dePieOEnLaListaUnaCasillaVaciaSiAbreSuFicha() {
        val n = NavegacionInterior()
        n.seleccionar("hueco", acostado = false, esCasillaVacia = vacia)
        assertEquals("hueco", n.seleccionado)
        assertNull(n.casillaAbierta)
        n.cambiarVista(Vista.LISTA)
        n.seleccionar("hueco", acostado = true, esCasillaVacia = vacia)
        assertEquals("hueco", n.seleccionado)
        assertNull(n.casillaAbierta)
    }

    @Test
    fun girarConUnaCasillaVaciaAbiertaLaPasaAlSelector() {
        val n = NavegacionInterior()
        n.seleccionar("hueco", acostado = false, esCasillaVacia = vacia)
        n.marcarEncuadrada("hueco")
        n.girar(acostado = true, esCasillaVacia = vacia)
        assertNull(n.seleccionado)
        assertEquals("hueco", n.casillaAbierta)
        assertNull(n.encuadrada)
    }

    @Test
    fun girarOlvidaElEncuadrePeroNoLaSeleccion() {
        val n = NavegacionInterior()
        n.seleccionar("0723913", false, ninguna)
        n.marcarEncuadrada("0723913")
        n.girar(acostado = true, esCasillaVacia = ninguna)
        assertEquals("0723913", n.seleccionado)
        assertNull(n.encuadrada)
    }

    @Test
    fun elSelectorDeElectivasLlevaSiAprobarAlElegir() {
        val n = NavegacionInterior()
        n.abrirCasilla("hueco", aprobar = true)
        assertTrue(n.aprobarAlElegir)
        assertEquals("hueco", n.casillaAbierta)
        n.cerrarCasilla()
        assertNull(n.casillaAbierta)
    }

    @Test
    fun laHojaDelAvanceSeAbreYSeCierra() {
        val n = NavegacionInterior()
        n.abrirAvance()
        assertTrue(n.avanceAbierto)
        assertTrue(n.hayHoja)
        n.cerrarAvance()
        assertFalse(n.hayHoja)
    }

    @Test
    fun verEnElMapaDesdeLaHojaLaCierraYPideElEncuadre() {
        val n = NavegacionInterior(Vista.LISTA)
        n.abrirAvance()
        n.seleccionar("0723913", false, ninguna)
        n.verEnMapa(listOf("a", "b"), cerrandoAvance = true)
        assertEquals(Vista.MAPA, n.vista)
        assertFalse(n.avanceAbierto)
        assertNull(n.seleccionado)
        assertEquals(listOf("a", "b"), n.porEncuadrar)
        n.encuadreHecho()
        assertNull(n.porEncuadrar)
    }

    @Test
    fun verEnElMapaDesdeElMapaConservaLaFicha() {
        val n = NavegacionInterior(Vista.MAPA)
        n.seleccionar("0723913", false, ninguna)
        n.verEnMapa(listOf("0723913"))
        assertEquals("0723913", n.seleccionado)
    }

    @Test
    fun soloLaVistaSeGuardaAlRecrear() {
        val n = NavegacionInterior()
        n.cambiarVista(Vista.HORARIO)
        n.seleccionar("0723913", false, ninguna)
        n.abrirAvance()
        val guardado = with(NavegacionInterior.Guardable) { SaverScope { true }.save(n) }
        assertNotNull(guardado)
        val nuevo = NavegacionInterior.Guardable.restore(guardado)
        assertNotNull(nuevo)
        assertEquals(Vista.HORARIO, nuevo.vista)
        assertNull(nuevo.seleccionado)
        assertFalse(nuevo.avanceAbierto)
        // Un valor guardado que ya no existe vuelve al mapa
        assertEquals(Vista.MAPA, NavegacionInterior.Guardable.restore("CUADROS")?.vista)
    }
}
