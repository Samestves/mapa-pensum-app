package mpensum

import mpensum.plataforma.iniciar
import mpensum.carrera.EstadoCarrera
import mpensum.guardado.Guardado
import mpensum.guardado.HorarioGuardado
import mpensum.datos.leerPensum
import mpensum.logica.MAX_CARGA
import mpensum.logica.Marca
import mpensum.plataforma.Preferencias
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lo guardado en el teléfono puede venir roto, a medias o de otra versión de
 * la app (o de la web). Nada de eso debe tumbar la app: arranca en limpio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QaDatosTest {

    private val slug = "ingenieria-de-sistemas"
    private val pensum by lazy { leerPensum(File("src/commonMain/composeResources/files/carreras/$slug.json").readText()) }

    @BeforeTest
    fun preparar() {
        Preferencias.iniciar(RuntimeEnvironment.getApplication())
    }

    private val basura = listOf("", " ", "null", "[]", "{}", "[1,2", "{\"a\":", "\"texto\"", "42", "true", "{\"a\":{\"b\":1}}", "[null,1,\"x\",{}]", "\u0000", "{\"k\":null}")

    @Test
    fun marcasRotasArrancanEnLimpio() {
        for (b in basura) {
            Preferencias.guardarTexto("mapa-pensum:marcas:$slug", b)
            assertTrue(Guardado.marcas(pensum).isEmpty(), "marcas con «$b»")
            // La portada solo lee crudo: tampoco revienta
            Guardado.marcasCrudas(slug)
            // Y el estado de la carrera se levanta igual
            EstadoCarrera(pensum).progreso
        }
    }

    @Test
    fun marcasDeOtraVersionSeDepuran() {
        val cod = pensum.obligatorias.first().codigo
        Preferencias.guardarTexto("mapa-pensum:marcas:$slug", "{\"$cod\":\"aprobada\",\"9999999\":\"aprobada\",\"${pensum.obligatorias[1].codigo}\":\"pendiente\",\"${pensum.obligatorias[2].codigo}\":3}")
        assertEquals(mapOf(cod to Marca.APROBADA), Guardado.marcas(pensum))
    }

    @Test
    fun casillasRotasArrancanEnLimpio() {
        for (b in basura) {
            Preferencias.guardarTexto("mapa-pensum:casillas:$slug", b)
            assertTrue(Guardado.casillas(pensum).isEmpty(), "casillas con «$b»")
        }
    }

    @Test
    fun cargaRotaOFueraDeRango() {
        for (b in basura + listOf("{\"unidad\":\"materias\"}", "{\"unidad\":\"uc\",\"valor\":5}", "{\"unidad\":\"materias\",\"valor\":\"x\"}", "{\"unidad\":\"materias\",\"valor\":2.5}")) {
            Preferencias.guardarTexto("mapa-pensum:carga:$slug", b)
            assertNull(Guardado.carga(slug), "carga con «$b»")
        }
        // Fuera de rango se acota: con cero no te graduarías
        Preferencias.guardarTexto("mapa-pensum:carga:$slug", "{\"unidad\":\"materias\",\"valor\":0}")
        assertEquals(1, Guardado.carga(slug))
        Preferencias.guardarTexto("mapa-pensum:carga:$slug", "{\"unidad\":\"materias\",\"valor\":9999}")
        assertEquals(MAX_CARGA, Guardado.carga(slug))
        Preferencias.guardarTexto("mapa-pensum:carga:$slug", "{\"unidad\":\"materias\",\"valor\":-4}")
        assertEquals(1, Guardado.carga(slug))
        // Guardar y leer da lo mismo, y null vuelve a la habitual
        Guardado.guardarCarga(slug, 5)
        assertEquals(5, Guardado.carga(slug))
        Guardado.guardarCarga(slug, null)
        assertNull(Guardado.carga(slug))
    }

    @Test
    fun laCargaElegidaFueraDeRangoSeAcotaTambienEnMemoria() {
        // La habitual puede pasar de 9 (quien cursa 12) y el mando lo deja en 11: lo mostrado y lo guardado deben coincidir
        val estado = EstadoCarrera(pensum, demostracion = null)
        estado.elegirCarga(11)
        assertEquals(Guardado.carga(slug), estado.carga)
    }

    @Test
    fun horarioRotoOImposibleSeDescarta() {
        val codigos = pensum.todas.map { it.codigo }.toSet()
        val cod = pensum.obligatorias.first().codigo
        for (b in basura) {
            Preferencias.guardarTexto("mapa-pensum:horario:$slug", b)
            assertTrue(HorarioGuardado.leer(slug, codigos).isEmpty(), "horario con «$b»")
        }
        fun fila(extra: String) = "[{\"codigo\":\"$cod\"$extra}]"
        val casos = mapOf(
            // Un día fuera de lunes–viernes, un fin antes del inicio, fuera de jornada, una materia que no existe
            fila(",\"dia\":5,\"inicio\":420,\"fin\":480") to 0,
            fila(",\"dia\":-1,\"inicio\":420,\"fin\":480") to 0,
            fila(",\"dia\":0,\"inicio\":480,\"fin\":420") to 0,
            fila(",\"dia\":0,\"inicio\":480,\"fin\":480") to 0,
            fila(",\"dia\":0,\"inicio\":60,\"fin\":120") to 0,
            fila(",\"dia\":0,\"inicio\":1100,\"fin\":1300") to 0,
            "[{\"codigo\":\"0000000\",\"dia\":0,\"inicio\":420,\"fin\":480}]" to 0,
            // Tipos cambiados
            fila(",\"dia\":\"0\",\"inicio\":420,\"fin\":480") to 0,
            fila(",\"dia\":0,\"inicio\":420.5,\"fin\":480") to 0,
            fila(",\"dia\":0,\"inicio\":null,\"fin\":480") to 0,
            fila(",\"dia\":0,\"inicio\":420") to 0,
            // Una fila buena, sin id ni textos opcionales: entra, con el id de la web
            fila(",\"dia\":0,\"inicio\":420,\"fin\":480") to 1,
            // Dos con el mismo id: queda una
            "[{\"id\":\"a\",\"codigo\":\"$cod\",\"dia\":0,\"inicio\":420,\"fin\":480},{\"id\":\"a\",\"codigo\":\"$cod\",\"dia\":1,\"inicio\":420,\"fin\":480}]" to 1,
            // Una mala entre buenas no tira las demás
            "[1,{\"codigo\":\"$cod\",\"dia\":0,\"inicio\":420,\"fin\":480},null,\"x\"]" to 1,
        )
        for ((json, esperadas) in casos) {
            Preferencias.guardarTexto("mapa-pensum:horario:$slug", json)
            assertEquals(esperadas, HorarioGuardado.leer(slug, codigos).size, json)
        }
    }

    @Test
    fun horarioGuardarYLeerDaLoMismo() {
        val cod = pensum.obligatorias.first().codigo
        val s = mpensum.logica.Sesion("x", cod, 2, 450, 540, "01", "Aula \"3\"", "Ñandú \\ Pérez")
        HorarioGuardado.guardar(slug, listOf(s))
        assertEquals(listOf(s), HorarioGuardado.leer(slug, setOf(cod)))
    }
}
