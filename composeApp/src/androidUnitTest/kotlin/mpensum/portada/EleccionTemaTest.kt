package mpensum.portada

import mpensum.plataforma.Apariencia
import mpensum.tema.Juego
import mpensum.tema.pareja
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La regla de la paleta de Personalizar: qué se guarda al elegir un juego, sin pantalla */
class EleccionTemaTest {

    @Test
    fun sinSistemaElegirEsCambiarElTema() {
        val e = elegirJuego(Juego.PAPEL, Apariencia.OSCURO, ranuraNoche = false, diaTocado = false)
        assertEquals(Apariencia.PAPEL, e.tema)
        assertNull(e.dia)
        assertNull(e.noche)
        assertFalse(e.diaTocado)
        // La ranura no importa si no hay Sistema
        assertEquals(Apariencia.NEGRO, elegirJuego(Juego.NEGRO, Apariencia.CLARO, ranuraNoche = true, diaTocado = true).tema)
    }

    @Test
    fun conSistemaLaRanuraDeDiaGuardaElDiaYLoDaPorTocado() {
        val e = elegirJuego(Juego.NIEBLA, Apariencia.SISTEMA, ranuraNoche = false, diaTocado = false)
        assertNull(e.tema)
        assertEquals(Juego.NIEBLA, e.dia)
        assertNull(e.noche)
        assertTrue(e.diaTocado)
    }

    @Test
    fun conSistemaLaNocheProponeLaParejaAlDiaSiNoSeTocoYEsOscura() {
        val e = elegirJuego(Juego.CIRUELA, Apariencia.SISTEMA, ranuraNoche = true, diaTocado = false)
        assertEquals(Juego.CIRUELA, e.noche)
        assertEquals(Juego.CIRUELA.pareja, e.dia)
        assertFalse(e.diaTocado)
    }

    @Test
    fun conSistemaLaNocheNoPisaUnDiaYaElegidoNiProponeParaUnClaro() {
        val tocado = elegirJuego(Juego.CIRUELA, Apariencia.SISTEMA, ranuraNoche = true, diaTocado = true)
        assertEquals(Juego.CIRUELA, tocado.noche)
        assertNull(tocado.dia)
        assertTrue(tocado.diaTocado)
        // Un juego claro en la ranura de noche no tiene pareja que proponer
        val claro = elegirJuego(Juego.CLARO, Apariencia.SISTEMA, ranuraNoche = true, diaTocado = false)
        assertEquals(Juego.CLARO, claro.noche)
        assertNull(claro.dia)
    }
}
