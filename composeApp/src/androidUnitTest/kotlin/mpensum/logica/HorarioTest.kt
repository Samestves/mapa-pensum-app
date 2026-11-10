package mpensum.logica

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La lógica del horario con la semana de ejemplo de Sistemas (la del tablero aprobado) */
class HorarioTest {
    private fun h(x: Int) = x * 60

    private val ejemplo = listOf(
        Sesion("a", "0624622", 0, h(7), h(9), "01", "Aula 12"),
        Sesion("b", "0624622", 2, h(7), h(9), "01", "Aula 12"),
        Sesion("c", "0713122", 1, h(9), h(11), "01", "Lab 3"),
        Sesion("d", "0713122", 3, h(9), h(11), "01", "Lab 3"),
        Sesion("e", "0713172", 0, h(10), h(12), "01", "Aula 5"),
        Sesion("f", "0713463", 1, h(13), h(15), "01", "Aula 8"),
        Sesion("g", "0713463", 3, h(13), h(15), "01", "Aula 8"),
        Sesion("i", "0723913", 2, h(10), h(12), "01", "Aula 2"),
        Sesion("j", "0723913", 4, h(8), h(10), "01", "Aula 2"),
        Sesion("k", "0713632", 4, h(13), h(15), "01", "Aula 9"),
    )
    private val dias = porDia(ejemplo)

    @Test
    fun lunesALas1040HayClaseEnCurso() {
        val m = momentoDe(dias, Ahora(0, 640))
        assertIs<Momento.EnCurso>(m)
        assertEquals("e", m.clase.id)
        assertEquals(80, m.quedan)
        assertEquals("1 h 20 min", duracion(m.quedan))
        assertNull(m.despues)
    }

    @Test
    fun antesDeLaPrimeraEsLaProxima() {
        val m = momentoDe(dias, Ahora(0, 400))
        assertIs<Momento.Luego>(m)
        assertEquals("a", m.clase.id)
        assertEquals("e", m.despues?.id)
    }

    @Test
    fun acabadoElDiaMiraMananaYDaLaVuelta() {
        val lunes = momentoDe(dias, Ahora(0, 780))
        assertIs<Momento.Libre>(lunes)
        assertTrue(lunes.terminado)
        assertEquals("Mañana", cuandoViene(lunes.proxima!!))
        val viernes = momentoDe(dias, Ahora(4, 1000))
        assertIs<Momento.Libre>(viernes)
        assertEquals("El lunes", cuandoViene(viernes.proxima!!))
        assertEquals("Fin de semana", tituloLibre(momentoDe(dias, Ahora(5, 600)) as Momento.Libre, Ahora(5, 600)))
        assertEquals(Momento.Vacio, momentoDe(porDia(emptyList()), Ahora(0, 600)))
    }

    @Test
    fun horasYTramos() {
        assertEquals("7 – 9 AM", tramoCorto(h(7), h(9), false))
        assertEquals("11 AM – 1 PM", tramoCorto(h(11), h(13), false))
        assertEquals("8:15 – 9:50 AM", tramoCorto(495, 590, false))
        assertEquals("12:00 PM", enDoceHoras(h(12), false))
        assertEquals("10:40", horaDe(640, false))
        assertEquals(h(7) to h(15), rangoDeClases(ejemplo))
        assertEquals("10 clases · 20 h a la semana", resumenSemana(ejemplo))
    }

    @Test
    fun laHoraDelAlmuerzoSePliega() {
        // De 7 a 15: solo de 12 a 1 no hay clase ningún día
        val ocupadas = horasOcupadas(ejemplo, h(7), h(15))
        assertEquals(8, ocupadas.size)
        assertEquals(listOf(5), ocupadas.indices.filter { !ocupadas[it] })
        // Una clase que acaba a y media ocupa la hora en la que acaba, no la siguiente
        assertEquals(listOf(true, true, false), horasOcupadas(listOf(Sesion("x", "c", 0, h(7), h(8) + 30)), h(7), h(10)))
        assertEquals("1 materia", materias(1))
    }

    @Test
    fun cuandoJuntaLosDiasConLaMismaHora() {
        assertEquals("Lunes y miércoles · 7 – 9 AM", cuandoEs(ejemplo[0], ejemplo, false))
        // Métodos va a horas distintas: solo su día
        assertEquals("Miércoles · 10 AM – 12 PM", cuandoEs(ejemplo[7], ejemplo, false))
    }

    @Test
    fun laAgendaDejaElHuecoLibre() {
        val lunes = agendaDe(dias[0])
        assertEquals(3, lunes.size)
        assertEquals(Renglon.Libre(h(9), h(10)), lunes[1])
    }

    @Test
    fun lasQueSePisanVanLadoALado() {
        val pisadas = listOf(
            Sesion("x", "1", 0, h(8), h(10)),
            Sesion("y", "2", 0, h(9), h(11)),
            Sesion("z", "3", 0, h(10), h(12)),
            Sesion("w", "4", 0, h(13), h(14)),
        )
        val c = enCarriles(pisadas).associateBy { it.sesion.id }
        assertEquals(0, c.getValue("x").carril)
        assertEquals(1, c.getValue("y").carril)
        // z cabe en el carril que dejó x
        assertEquals(0, c.getValue("z").carril)
        assertEquals(2, c.getValue("z").carriles)
        assertEquals(1, c.getValue("w").carriles)
    }

    @Test
    fun choqueYFranjaNueva() {
        val candidata = Sesion("", "0723913", 0, h(11), h(13))
        assertEquals("e", choqueCon(ejemplo, candidata)?.id)
        assertNull(choqueCon(ejemplo, ejemplo[4]))
        // Desde el hueco libre de las 9 del lunes, la propuesta se corta donde empieza la siguiente
        assertEquals(h(9) to h(10), franjaNueva(dias[0], h(9)))
        assertEquals(h(12) to h(14), franjaNueva(dias[0], h(12)))
        assertEquals("0713172-0-600-b", idNuevo("0713172", 0, 600, setOf("0713172-0-600")))
    }

    @Test
    fun abreviaSinPartirPalabras() {
        // Un "ancho" de 9 letras: lo que cabe en un bloque de 64 dp a 12 sp
        val cabe = { t: String -> t.length <= 9 }
        assertEquals(listOf("Estad. II"), abreviarNombre("Estadística II", 2, cabe))
        assertEquals(listOf("Sistemas", "de Costo"), abreviarNombre("Sistemas de Costo", 2, cabe))
        assertEquals(listOf("Teoría de", "Sistemas"), abreviarNombre("Teoría de Sistemas", 2, cabe))
        val larga = abreviarNombre("Inferencia y Diseño de Experimentos", 2, cabe)
        assertTrue(larga.size <= 2 && larga.all(cabe), "$larga")
        assertEquals(listOf("Circuitos"), abreviarNombre("Circuitos y Sistemas", 1, cabe))
        // En tres renglones la «y» no se queda sola en el suyo
        assertEquals(listOf("Circuitos", "Sistemas"), abreviarNombre("Circuitos y Sistemas", 3, cabe))
        // Ninguna palabra queda cortada sin su punto
        abreviarNombre("Métodos Numéricos", 2, cabe).flatMap { it.split(' ') }.forEach { p ->
            assertTrue(p in setOf("Métodos", "Numéricos") || p.endsWith("."), p)
        }
    }

    @Test
    fun loImposibleNoEntra() {
        assertTrue(esValida(ejemplo[0]))
        assertTrue(!esValida(Sesion("m", "1", 5, h(8), h(9))))
        assertTrue(!esValida(Sesion("m", "1", 0, h(9), h(8))))
        assertTrue(!esValida(Sesion("m", "1", 0, h(5), h(8))))
    }
}
