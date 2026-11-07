package mpensum.logica

import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

/** Cuánto cuesta lo pesado de las pantallas internas: la hoja de avance y sus tres detalles. Imprime las cifras, no afirma tiempos */
class RendimientoTest {

    private fun pensum(slug: String) = leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText())
    private val sistemas by lazy { pensum("ingenieria-de-sistemas") }
    private val hoy = Periodo(2026, 2)

    private fun intermedio(p: Pensum) = p.obligatorias.filter { (it.semestre ?: 99) <= 3 }.associate { it.codigo to Marca.APROBADA } +
        p.obligatorias.filter { it.semestre == 4 }.associate { it.codigo to Marca.CURSANDO }

    /** La mediana de varias vueltas, en ms, tras calentar la JVM */
    private fun mediana(vueltas: Int = 9, trabajo: () -> Unit): Double {
        repeat(5) { trabajo() }
        return (1..vueltas).map { measureTime(trabajo).inWholeMicroseconds / 1000.0 }.sorted()[vueltas / 2]
    }

    @Test
    fun cuestaAbrirLaHojaYSusDetalles() {
        for ((nombre, marcas) in listOf("alumno nuevo" to emptyMap(), "intermedio" to intermedio(sistemas))) {
            val hoja = mediana { estimarGraduacion(sistemas, marcas, emptyMap(), null, hoy); planesPorCarga(sistemas, marcas, emptyMap(), hoy) }
            val graduacion = mediana { planesPorCarga(sistemas, marcas, emptyMap(), hoy); estimarGraduacion(sistemas, marcas, emptyMap(), null, hoy) }
            val proximo = mediana { proximoSemestre(sistemas, marcas, emptyMap(), null, hoy) }
            val cadena = mediana { cadenaExplicada(sistemas, marcas, emptyMap(), null, hoy) }
            println("RENDIMIENTO[$nombre] ANTES hoja=%.1f ms · ¿cuándo me gradúo?=%.1f ms · próximo=%.1f ms · cadena=%.1f ms · total=%.1f ms".format(hoja, graduacion, proximo, cadena, hoja + graduacion + proximo + cadena))

            // Ahora: los planes una vez (hoja y mando), y un solo simulador para el próximo semestre y la cadena
            val total = mediana {
                val planes = planesPorCarga(sistemas, marcas, emptyMap(), hoy)
                val (carga, _) = cargaHabitual(sistemas, marcas)
                val sim = Simulador(sistemas, marcas, emptyMap(), null, hoy, planes, planes[carga - 1])
                proximoSemestre(sistemas, marcas, emptyMap(), null, hoy, sim)
                cadenaExplicada(sistemas, marcas, emptyMap(), null, hoy, sim)
            }
            println("RENDIMIENTO[$nombre] AHORA total=%.1f ms".format(total))

            // Compartir no cambia ningún resultado
            val planes = planesPorCarga(sistemas, marcas, emptyMap(), hoy)
            val sim = Simulador(sistemas, marcas, emptyMap(), null, hoy, planes, planes[cargaHabitual(sistemas, marcas).first - 1])
            assertEquals(proximoSemestre(sistemas, marcas, emptyMap(), null, hoy), proximoSemestre(sistemas, marcas, emptyMap(), null, hoy, sim), nombre)
            assertEquals(cadenaExplicada(sistemas, marcas, emptyMap(), null, hoy), cadenaExplicada(sistemas, marcas, emptyMap(), null, hoy, sim), nombre)
        }
    }
}
