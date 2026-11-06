package mpensum.carrera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La geometría del anillo de graduación y la colocación de sus notas, sin dibujar nada */
class AnilloGeometriaTest {

    private fun cerca(esperado: Float, real: Float) = assertEquals(esperado, real, 1e-3f)

    @Test
    fun laDistanciaEntreAngulosDaLaVueltaCorta() {
        cerca(20f, distancia(350f, 10f))
        cerca(20f, distancia(10f, 350f))
        cerca(180f, distancia(0f, 180f))
        cerca(0f, distancia(90f, 90f))
    }

    @Test
    fun lasMarcasSeReparten360GradosConUnHuecoArriba() {
        // Con 54 casillas y 6 de hueco, cada marca ocupa 6 grados y la primera cae pasado el medio hueco
        val casillas = 54
        cerca((0f + 3f + 0.5f) * 6f, anguloDeMarca(0f, casillas))
        cerca(6f, anguloDeMarca(1f, casillas) - anguloDeMarca(0f, casillas))
        // La última queda antes de cerrar la vuelta: deja el hueco del birrete
        assertTrue(anguloDeMarca((casillas - 1).toFloat(), casillas) < 360f - 6f)
    }

    @Test
    fun elPlanoPonePuntosAPartirDelCentroYDeArriba() {
        val plano = PlanoAnillo(cx = 100f, cy = 100f, u = 2f)
        // 0 grados es arriba; 90, a la derecha; 180, abajo
        val arriba = plano.xy(10f, 0f)
        cerca(100f, arriba.x)
        cerca(80f, arriba.y)
        val derecha = plano.xy(10f, 90f)
        cerca(120f, derecha.x)
        cerca(100f, derecha.y)
        cerca(120f, plano.xy(10f, 180f).y)
    }

    private fun anillo(vararg tipos: Pair<Int, Int>): Anillo {
        val fichas = tipos.map { (k, tipo) -> Ficha(k, tipo) }
        return Anillo(fichas, casillas = 54, anios = emptyList(), hoyK = 20f, aprobadas = fichas.count { it.tipo == 0 }, porAprobar = fichas.count { it.tipo == 3 })
    }

    private val plano = PlanoAnillo(cx = 180f, cy = 132f, u = 1f)

    private fun elegir(a: Anillo, lado: Int, preferido: Float, usados: List<Float> = emptyList(), holgado: Boolean = false, margen: Float = 64f) =
        elegirAnguloNota(a, plano, anchoLienzo = 360f, tipos = listOf(0), lado = lado, preferido = preferido, holgado = holgado, margen = margen, gradosHoy = anguloDeMarca(20f, 54), angulosAnio = emptyList(), usados = usados)

    @Test
    fun laNotaDeLaDerechaCaeEnUnaMarcaDeEseLado() {
        // Aprobadas de la casilla 5 a la 14: todas a la derecha del anillo
        val a = anillo(*(5..14).map { it to 0 }.toTypedArray())
        val g = assertNotNull(elegir(a, lado = 1, preferido = 58f))
        // Una marca de verdad: es el ángulo de alguna de las aprobadas
        assertTrue((5..14).any { kotlin.math.abs(anguloDeMarca(it.toFloat(), 54) - g) < 1e-3f })
        // Y está en el lado derecho
        assertTrue(plano.xy(113f, g).x > plano.cx)
    }

    @Test
    fun laCajaDeLaIzquierdaTambienTieneSuMargen() {
        val a = anillo(*(5..14).map { it to 0 }.toTypedArray())
        // Con margen normal cabe; con uno que se come la caja, ninguna marca queda dentro
        assertNotNull(elegir(a, lado = -1, preferido = 225f))
        assertNull(elegir(a, lado = -1, preferido = 225f, margen = 400f))
    }

    @Test
    fun noPisaOtraNota() {
        val a = anillo(5 to 0, 6 to 0)
        val primera = assertNotNull(elegir(a, lado = 1, preferido = 0f))
        val segunda = elegir(a, lado = 1, preferido = 0f, usados = listOf(primera))
        // Las dos marcas están a 6 grados: con 40 de separación mínima la segunda ya no cabe
        assertNull(segunda)
    }

    @Test
    fun conMenosRestriccionAceptaLoQueSinEllaNo() {
        // A 2 casillas de HOY (12 grados) no vale ni holgado
        val cercaDeHoy = anillo(22 to 0)
        assertNull(elegir(cercaDeHoy, lado = 1, preferido = 58f))
        val aMedias = anillo(24 to 0)
        // A 4 casillas (24 grados) de HOY: pisa el mínimo normal (34) pero no el holgado (20)
        assertNull(elegir(aMedias, lado = 1, preferido = 58f))
        assertNotNull(elegir(aMedias, lado = 1, preferido = 58f, holgado = true))
    }

    @Test
    fun siLaNotaNoCabeEnLaCajaNoSeElige() {
        val a = anillo(*(5..14).map { it to 0 }.toTypedArray())
        // Un margen enorme deja la caja sin sitio a la derecha
        assertNull(elegir(a, lado = 1, preferido = 58f, margen = 400f))
    }
}
