package mpensum.logica

import mpensum.datos.leerPensum
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** La caché de márgenes del simulador: varios hilos piden los mismos márgenes y todos reciben lo mismo */
class SimuladorConcurrenciaTest {

    private val hoy = Periodo(2026, 2)
    private val sistemas by lazy { leerPensum(File("src/commonMain/composeResources/files/carreras/ingenieria-de-sistemas.json").readText()) }

    private val marcas by lazy {
        buildMap {
            sistemas.obligatorias.filter { (it.semestre ?: 99) <= 4 }.forEach { put(it.codigo, Marca.APROBADA) }
            sistemas.obligatorias.filter { it.semestre == 5 }.take(2).forEach { put(it.codigo, Marca.CURSANDO) }
        }
    }

    private fun simulador() = Simulador(sistemas, marcas, emptyMap(), 6, hoy)

    @Test
    fun dosSimuladoresMidenLoMismo() {
        val a = simulador()
        val b = simulador()
        val codigos = a.base.plan.flatten()
        assertTrue(codigos.isNotEmpty())
        assertEquals(codigos.associateWith { a.margen(it) }, codigos.associateWith { b.margen(it) })
    }

    @Test
    fun variosHilosPidenLosMismosMargenesYObtienenLoMismo() {
        val referencia = simulador().let { s -> s.base.plan.flatten().associateWith { s.margen(it) } }
        val compartido = simulador()
        val codigos = referencia.keys.toList()
        val hilos = 8
        val pool = Executors.newFixedThreadPool(hilos)
        try {
            val salida = CountDownLatch(1)
            val resultados = (0 until hilos).map { n ->
                pool.submit<Map<String, Int>> {
                    salida.await()
                    // Cada hilo recorre los códigos en un orden distinto para que choquen en las mismas materias
                    val orden = if (n % 2 == 0) codigos else codigos.reversed()
                    orden.associateWith { compartido.margen(it) }
                }
            }
            salida.countDown()
            resultados.forEach { assertEquals(referencia, it.get(60, TimeUnit.SECONDS)) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun unaMateriaFueraDelPensumNoRompeLaCache() {
        val s = simulador()
        // No está en el mapa de márgenes: se mide al momento, como antes
        val margen = s.margen("no-existe")
        assertTrue(margen in 0..TOPE_MARGEN)
    }
}
