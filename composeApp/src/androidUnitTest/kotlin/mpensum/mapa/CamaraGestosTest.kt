package mpensum.mapa

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La escala de lectura, los dos primeros encuadres del mapa y la velocidad con que se suelta un arrastre */
class CamaraGestosTest {

    @Test
    fun laEscalaDeLecturaTieneTopeDe0coma8() {
        assertEquals(0.8f, escalaDeLectura(1000f))
        assertEquals(0.8f, escalaDeLectura(600f))
    }

    @Test
    fun enUnTelefonoEstrechoLaEscalaBajaConElAncho() {
        val ancho360 = escalaDeLectura(360f)
        val ancho400 = escalaDeLectura(400f)
        assertEquals((360f - 24f) / (Nodo.ANCHO.toFloat() * 1.55f + Espaciado.COLUMNA.toFloat()), ancho360, 1e-6f)
        assertTrue(ancho360 < ancho400 && ancho400 < 0.8f)
    }

    @Test
    fun laVistaDeColumnaPegaElSemestreALaIzquierda() {
        val e = escalaDeLectura(400f)
        val v = vistaDeColumna(xColumna = 48f, ancho = 400f, arriba = 30f)
        assertEquals(e, v.escala)
        // El borde de la columna (48 en el plano) cae a 16 dp de la pantalla
        assertEquals(16f, v.x + 48f * e, 1e-4f)
        // Y la cabecera (24 en el plano) a 12 dp por debajo de la barra de estado
        assertEquals(30f + 12f, v.y + Margen.ARRIBA.toFloat() * e, 1e-4f)
    }

    @Test
    fun laVistaDeAlturaLlenaElAltoLibre() {
        // Libre: 400 - (20 + 6) - (10 + 14) = 350, y la columna mide 374 - 24 = 350
        val v = vistaDeAltura(xColumna = 48f, finColumna = 374f, alto = 400f, arriba = 20f, abajo = 10f, izq = 80f)
        assertEquals(1f, v.escala, 1e-6f)
        assertEquals(80f, v.x + 48f * v.escala, 1e-4f)
        assertEquals(26f, v.y + Margen.ARRIBA.toFloat() * v.escala, 1e-4f)
    }

    @Test
    fun laVistaDeAlturaNuncaPasaDeLosTopesDeZoom() {
        assertEquals(ZOOM_MIN, vistaDeAltura(0f, 1_000_000f, 400f, 0f, 0f, 0f).escala)
        assertEquals(ZOOM_MAX, vistaDeAltura(0f, 25f, 400f, 0f, 0f, 0f).escala)
    }

    private fun m(t: Long, x: Float, y: Float) = Muestra(t, x, y)

    @Test
    fun sinDosMuestrasNoHayVelocidad() {
        assertNull(velocidadAlSoltar(emptyList(), 0))
        assertNull(velocidadAlSoltar(listOf(m(0, 0f, 0f)), 10))
    }

    @Test
    fun siElDedoSeQuedoQuietoAntesDeLevantarseNoHayLanzamiento() {
        assertNull(velocidadAlSoltar(listOf(m(0, 0f, 0f), m(50, 50f, 0f)), ahora = 111))
        assertNotNull(velocidadAlSoltar(listOf(m(0, 0f, 0f), m(50, 50f, 0f)), ahora = 110))
    }

    @Test
    fun laVelocidadEsLaDeLosUltimos80ms() {
        val v = velocidadAlSoltar(listOf(m(0, 0f, 0f), m(100, 10f, 0f), m(180, 190f, 40f)), ahora = 190)
        // Se parte de la muestra de t = 100, la primera dentro de la ventana
        assertEquals(Offset(2.25f, 0.5f), v)
    }

    @Test
    fun dosMuestrasEnElMismoInstanteNoDanVelocidad() {
        assertNull(velocidadAlSoltar(listOf(m(5, 0f, 0f), m(5, 9f, 9f)), ahora = 5))
    }
}
