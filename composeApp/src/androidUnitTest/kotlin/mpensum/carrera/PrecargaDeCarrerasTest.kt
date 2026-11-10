package mpensum.carrera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** La precarga de carreras: lo que sale bien se guarda, lo que falla se vuelve a intentar y nunca tumba a quien espera */
class PrecargaDeCarrerasTest {

    private val pensum: Pensum by lazy {
        leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText())
    }

    /** Una precarga que lee con `lectura` y cuenta cuántas veces se le pidió leer */
    private class Cuenta(val fallos: Int) {
        var lecturas = 0
    }

    private fun precarga(c: Cuenta, pensum: () -> Pensum) = PrecargaDeCarreras(Dispatchers.Unconfined) {
        c.lecturas++
        if (c.lecturas <= c.fallos) error("el paquete no se pudo leer")
        pensum()
    }

    @Test
    fun loQueSaleBienSeGuardaYNoSeLeeDosVeces() = runBlocking {
        val c = Cuenta(fallos = 0)
        val p = precarga(c) { pensum }
        val primera = p.pedir("sistemas", false, false)
        assertSame(primera, p.pedir("sistemas", false, false))
        assertNotNull(p.cargar("sistemas", false, false))
        assertEquals(1, c.lecturas)
        // Otro layout de la misma carrera es otra petición
        assertNotSame(primera, p.pedir("sistemas", true, false))
    }

    @Test
    fun unaCargaFallidaNoSeQuedaGuardada() = runBlocking {
        val c = Cuenta(fallos = 1)
        val p = precarga(c) { pensum }
        val fallida = p.pedir("sistemas", false, false)
        assertTrue(fallida.isCancelled, "la lectura falló")
        // La siguiente petición ya no es la que falló, y esa sí sale
        val nueva = p.pedir("sistemas", false, false)
        assertNotSame(fallida, nueva)
        assertNotNull(nueva.await())
        assertSame(nueva, p.pedir("sistemas", false, false))
        assertEquals(2, c.lecturas)
    }

    @Test
    fun conUnSoloIntentoUnFalloDaNullSinLanzar() = runBlocking {
        val c = Cuenta(fallos = 1)
        val p = precarga(c) { pensum }
        assertNull(p.cargar("sistemas", false, false, intentos = 1))
        assertNotNull(p.cargar("sistemas", false, false, intentos = 1))
        assertEquals(2, c.lecturas)
    }

    @Test
    fun cargarReintentaUnaVezAntesDeRendirse() = runBlocking {
        val c = Cuenta(fallos = 1)
        val p = precarga(c) { pensum }
        // Con los dos intentos de serie, el segundo sale bien
        assertNotNull(p.cargar("sistemas", false, false))
        assertEquals(2, c.lecturas)
    }

    @Test
    fun siSiempreFallaDevuelveNullSinLanzar() = runBlocking {
        val c = Cuenta(fallos = Int.MAX_VALUE)
        val p = precarga(c) { pensum }
        assertNull(p.cargar("sistemas", false, false))
        assertEquals(2, c.lecturas)
    }
}
