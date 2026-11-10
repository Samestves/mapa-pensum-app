package mpensum.plataforma

import android.content.Context
import android.content.SharedPreferences
import mpensum.carrera.Vista
import mpensum.tema.Acento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Juego
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fija lo que la app escribe en el teléfono: cada clave, cada valor y cada valor por defecto. Si cambia
 * una, quien actualiza la app pierde su configuración: esta prueba tiene que pasar antes y después de
 * tocar cómo se guardan las preferencias.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferenciasTest {

    private lateinit var contexto: Context
    private lateinit var crudas: SharedPreferences

    @BeforeTest
    fun preparar() {
        contexto = RuntimeEnvironment.getApplication()
        crudas = contexto.getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE)
        crudas.edit().clear().commit()
        Preferencias.iniciar(contexto)
    }

    /** Como al abrir la app otra vez: se vuelve a leer lo guardado */
    private fun reabrir() = Preferencias.iniciar(contexto)

    private fun vaciar() {
        crudas.edit().clear().commit()
        reabrir()
    }

    @Test
    fun porDefectoConElTelefonoVirgen() {
        assertFalse(Preferencias.bienvenidaVista())
        assertNull(Preferencias.carrera())
        assertNull(Preferencias.leerTexto("nada"))
        assertEquals(Apariencia.OSCURO, Preferencias.tema.value)
        assertNull(Preferencias.acento.value)
        assertFalse(Preferencias.textoGrande.value)
        assertNull(Preferencias.luzFondo.value)
        assertNull(Preferencias.reducirMovimiento.value)
        assertNull(Preferencias.modoLigero.value)
        assertTrue(Preferencias.codigos.value)
        assertNull(Preferencias.fondoMapa.value)
        assertEquals(FormaCables.CURVOS, Preferencias.cables.value)
        assertEquals(Grosor.NORMAL, Preferencias.grosorCables.value)
        assertFalse(Preferencias.atenuarAprobadas.value)
        assertFalse(Preferencias.contraste.value)
        assertEquals(Juego.CLARO, Preferencias.temaDia.value)
        assertEquals(Juego.OSCURO, Preferencias.temaNoche.value)
        assertFalse(Preferencias.abrirEnCarrera.value)
        assertEquals(Vista.MAPA, Preferencias.vistaInicial.value)
        assertFalse(Preferencias.hora24.value)
        assertTrue(Preferencias.vibracion.value)
        // Y no escribe nada solo por arrancar
        assertTrue(crudas.all.isEmpty(), "arrancar no guarda nada: ${crudas.all}")
    }

    @Test
    fun cadaPreferenciaEscribeSuClaveYSuValor() {
        Preferencias.marcarBienvenidaVista()
        assertEquals(true, crudas.all["bienvenida"])
        Preferencias.guardarCarrera("ingenieria-de-sistemas")
        assertEquals("ingenieria-de-sistemas", crudas.all["carrera"])
        Preferencias.guardarTexto("otra-clave", "hola")
        assertEquals("hola", crudas.all["otra-clave"])
        assertEquals("hola", Preferencias.leerTexto("otra-clave"))

        Preferencias.guardarTema(Apariencia.GRAFITO)
        assertEquals("grafito", crudas.all["tema"])
        Preferencias.guardarAcento(Acento.LAGUNA)
        assertEquals("laguna", crudas.all["acento"])
        Preferencias.guardarTextoGrande(true)
        assertEquals("grande", crudas.all["texto"])
        Preferencias.guardarTextoGrande(false)
        assertEquals("normal", crudas.all["texto"])
        Preferencias.guardarLuzFondo(false)
        assertEquals(false, crudas.all["luzFondo"])
        Preferencias.guardarReducirMovimiento(true)
        assertEquals(true, crudas.all["movimiento"])
        Preferencias.guardarCodigos(false)
        assertEquals(false, crudas.all["codigos"])
        Preferencias.guardarModoLigero(true)
        assertEquals(true, crudas.all["ligero"])
        Preferencias.guardarFondoMapa(Fondo.PUNTOS)
        assertEquals("PUNTOS", crudas.all["fondoMapa"])
        Preferencias.guardarCables(FormaCables.AL_TOCAR)
        assertEquals("AL_TOCAR", crudas.all["cables"])
        Preferencias.guardarGrosorCables(Grosor.GRUESO)
        assertEquals("GRUESO", crudas.all["grosorCables"])
        Preferencias.guardarAtenuarAprobadas(true)
        assertEquals(true, crudas.all["atenuarAprobadas"])
        Preferencias.guardarContraste(true)
        assertEquals(true, crudas.all["contraste"])
        Preferencias.guardarTemaDia(Juego.PAPEL)
        assertEquals("PAPEL", crudas.all["temaDia"])
        Preferencias.guardarTemaNoche(Juego.CIRUELA)
        assertEquals("CIRUELA", crudas.all["temaNoche"])
        Preferencias.guardarAbrirEnCarrera(true)
        assertEquals(true, crudas.all["abrirEnCarrera"])
        Preferencias.guardarVistaInicial(Vista.LISTA)
        assertEquals("LISTA", crudas.all["vistaInicial"])
        Preferencias.guardarHora24(true)
        assertEquals(true, crudas.all["hora24"])
        Preferencias.guardarVibracion(false)
        assertEquals(false, crudas.all["vibracion"])
    }

    @Test
    fun loGuardadoVuelveTalCualAlReabrir() {
        Preferencias.marcarBienvenidaVista()
        Preferencias.guardarCarrera("ingenieria-ambiental")
        Preferencias.guardarTema(Apariencia.SISTEMA)
        Preferencias.guardarAcento(Acento.ROSA)
        Preferencias.guardarTextoGrande(true)
        Preferencias.guardarLuzFondo(false)
        Preferencias.guardarReducirMovimiento(true)
        Preferencias.guardarCodigos(false)
        Preferencias.guardarModoLigero(false)
        Preferencias.guardarFondoMapa(Fondo.LISO)
        Preferencias.guardarCables(FormaCables.RECTOS)
        Preferencias.guardarGrosorCables(Grosor.FINO)
        Preferencias.guardarAtenuarAprobadas(true)
        Preferencias.guardarContraste(true)
        Preferencias.guardarTemaDia(Juego.NIEBLA)
        Preferencias.guardarTemaNoche(Juego.MEDIANOCHE)
        Preferencias.guardarAbrirEnCarrera(true)
        Preferencias.guardarVistaInicial(Vista.LISTA)
        Preferencias.guardarHora24(true)
        Preferencias.guardarVibracion(false)
        reabrir()
        assertTrue(Preferencias.bienvenidaVista())
        assertEquals("ingenieria-ambiental", Preferencias.carrera())
        assertEquals(Apariencia.SISTEMA, Preferencias.tema.value)
        assertEquals(Acento.ROSA, Preferencias.acento.value)
        assertTrue(Preferencias.textoGrande.value)
        assertEquals(false, Preferencias.luzFondo.value)
        assertEquals(true, Preferencias.reducirMovimiento.value)
        assertFalse(Preferencias.codigos.value)
        assertEquals(false, Preferencias.modoLigero.value)
        assertEquals(Fondo.LISO, Preferencias.fondoMapa.value)
        assertEquals(FormaCables.RECTOS, Preferencias.cables.value)
        assertEquals(Grosor.FINO, Preferencias.grosorCables.value)
        assertTrue(Preferencias.atenuarAprobadas.value)
        assertTrue(Preferencias.contraste.value)
        assertEquals(Juego.NIEBLA, Preferencias.temaDia.value)
        assertEquals(Juego.MEDIANOCHE, Preferencias.temaNoche.value)
        assertTrue(Preferencias.abrirEnCarrera.value)
        assertEquals(Vista.LISTA, Preferencias.vistaInicial.value)
        assertTrue(Preferencias.hora24.value)
        assertFalse(Preferencias.vibracion.value)
    }

    @Test
    fun losOpcionalesSeQuitanAlVolverAAutomatico() {
        Preferencias.guardarAcento(Acento.LILA)
        Preferencias.guardarAcento(null)
        assertFalse(crudas.contains("acento"))
        Preferencias.guardarModoLigero(true)
        Preferencias.guardarModoLigero(null)
        assertFalse(crudas.contains("ligero"))
        Preferencias.guardarFondoMapa(Fondo.PUNTOS)
        Preferencias.guardarFondoMapa(null)
        assertFalse(crudas.contains("fondoMapa"))
        reabrir()
        assertNull(Preferencias.acento.value)
        assertNull(Preferencias.modoLigero.value)
        assertNull(Preferencias.fondoMapa.value)
    }

    @Test
    fun cambiarDeTemaSueltaElFondoElegidoAMano() {
        Preferencias.guardarTema(Apariencia.CLARO)
        Preferencias.guardarFondoMapa(Fondo.LISO)
        // El mismo tema no lo suelta
        Preferencias.guardarTema(Apariencia.CLARO)
        assertEquals(Fondo.LISO, Preferencias.fondoMapa.value)
        // Otro tema sí
        Preferencias.guardarTema(Apariencia.PAPEL)
        assertNull(Preferencias.fondoMapa.value)
        assertFalse(crudas.contains("fondoMapa"))
    }

    @Test
    fun lasClavesViejasDeLaAparienciaSiguenLeyendose() {
        for ((guardado, esperado) in listOf(
            "claro" to Apariencia.CLARO,
            "oscuro" to Apariencia.OSCURO,
            "exterior" to Apariencia.EXTERIOR,
            "sistema" to Apariencia.SISTEMA,
            "morichal" to Apariencia.MEDIANOCHE,
            "algo-que-ya-no-existe" to Apariencia.OSCURO,
        )) {
            vaciar()
            crudas.edit().putString("tema", guardado).commit()
            reabrir()
            assertEquals(esperado, Preferencias.tema.value, "tema «$guardado»")
        }
    }

    @Test
    fun migracionesDeLosJuegosYLaVista() {
        crudas.edit().putString("temaDia", "MORICHAL").putString("temaNoche", "MORICHAL").commit()
        reabrir()
        assertEquals(Juego.MEDIANOCHE, Preferencias.temaDia.value)
        assertEquals(Juego.MEDIANOCHE, Preferencias.temaNoche.value)

        vaciar()
        crudas.edit().putString("temaDia", "NO_EXISTE").putString("temaNoche", "").commit()
        reabrir()
        assertEquals(Juego.CLARO, Preferencias.temaDia.value)
        assertEquals(Juego.OSCURO, Preferencias.temaNoche.value)

        // El horario no es una vista con la que se abra una carrera
        vaciar()
        crudas.edit().putString("vistaInicial", "HORARIO").commit()
        reabrir()
        assertEquals(Vista.MAPA, Preferencias.vistaInicial.value)
        vaciar()
        crudas.edit().putString("vistaInicial", "LISTA").commit()
        reabrir()
        assertEquals(Vista.LISTA, Preferencias.vistaInicial.value)
    }

    @Test
    fun valoresDesconocidosCaenEnElPorDefecto() {
        crudas.edit()
            .putString("acento", "turquesa")
            .putString("texto", "enorme")
            .putString("fondoMapa", "ESTRELLAS")
            .putString("cables", "ZIGZAG")
            .putString("grosorCables", "GORDO")
            .putString("vistaInicial", "CUADROS")
            .commit()
        reabrir()
        assertNull(Preferencias.acento.value)
        assertFalse(Preferencias.textoGrande.value)
        assertNull(Preferencias.fondoMapa.value)
        assertEquals(FormaCables.CURVOS, Preferencias.cables.value)
        assertEquals(Grosor.NORMAL, Preferencias.grosorCables.value)
        assertEquals(Vista.MAPA, Preferencias.vistaInicial.value)
    }

    @Test
    fun luzMovimientoYLigeroSoloValenSiHayClave() {
        // Sin clave: null (manda el tema, el sistema o el teléfono); con ella, lo que diga
        crudas.edit().putBoolean("luzFondo", true).putBoolean("movimiento", false).putBoolean("ligero", true).commit()
        reabrir()
        assertEquals(true, Preferencias.luzFondo.value)
        assertEquals(false, Preferencias.reducirMovimiento.value)
        assertEquals(true, Preferencias.modoLigero.value)
    }

    @Test
    fun esSocModestoReconoceLosDeLaLista() {
        assertTrue(esSocModesto("MT6769"))
        assertTrue(esSocModesto(" mt6765 "))
        assertTrue(esSocModesto("SM6225"))
        assertTrue(esSocModesto("S5E3830"))
        assertFalse(esSocModesto(""))
        assertFalse(esSocModesto("UNKNOWN"))
        assertFalse(esSocModesto("SM8650"))
        assertFalse(esSocModesto("Tensor G3"))
    }
}
