package mpensum.mapa

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import mpensum.logica.Marca
import mpensum.logica.adoptarMarcadas
import mpensum.logica.casillasLibres
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * El layout del mapa contra el de la web: cada tarjeta en su sitio, cada
 * nombre partido igual y cada cable con el mismo trazado, hasta el decimal.
 * Las fixtures salen de herramientas/fixturesMapa.mjs.
 */
class MapaDoradoTest {

    private val carpetaFixtures = File("src/androidUnitTest/fixtures/mapa")
    private val carpetaCarreras = File("src/commonMain/composeResources/files/carreras")

    private fun carreras(): List<Pair<Pensum, JsonObject>> {
        val archivos = carpetaFixtures.listFiles { f -> f.extension == "json" }?.sortedBy { it.name }
            ?: fail("No hay fixtures del mapa: corre herramientas/fixturesMapa.mjs")
        assertEquals(9, archivos.size)
        return archivos.map { leerPensum(File(carpetaCarreras, it.name).readText()) to Json.parseToJsonElement(it.readText()).jsonObject }
    }

    @Test
    fun tarjetasYColumnas() = carreras().forEach { (pensum, e) ->
        val layout = calcularLayout(pensum.asignaturas, pensum.grupos)
        val nodos = e.getValue("nodos").jsonArray.map { it.jsonArray }
        assertEquals(nodos.size, layout.nodos.size, "${pensum.slug}: cuántas tarjetas")
        nodos.zip(layout.nodos).forEach { (esperado, real) ->
            val que = "${pensum.slug} · ${real.codigo}"
            assertEquals(esperado[0].jsonPrimitive.content, real.codigo, "$que: orden")
            assertEquals(esperado[1].jsonPrimitive.double, real.x, "$que: x")
            assertEquals(esperado[2].jsonPrimitive.double, real.y, "$que: y")
            assertEquals(esperado[3].jsonArray.map { it.jsonPrimitive.content }, real.lineasNombre, "$que: líneas del nombre")
        }
        e.getValue("electivas").jsonArray.map { it.jsonArray }.zip(layout.electivas).forEach { (esperado, real) ->
            assertEquals(esperado[0].jsonPrimitive.content, real.codigo)
            assertEquals(esperado[1].jsonArray.map { it.jsonPrimitive.content }, real.lineasNombre, "${pensum.slug} · ${real.codigo}: líneas")
        }
        assertEquals(
            e.getValue("columnas").jsonArray.map { c -> c.jsonArray.let { listOf(it[0].jsonPrimitive.int, it[1].jsonPrimitive.double, it[2].jsonPrimitive.int, it[3].jsonPrimitive.int) } },
            layout.columnas.map { listOf(it.semestre, it.x, it.cantidad, it.uc) },
            "${pensum.slug}: columnas",
        )
        assertEquals(e.getValue("finSemestres").jsonPrimitive.double, layout.finSemestres, "${pensum.slug}: fin de los semestres")
        assertEquals(e.getValue("maxFilas").jsonPrimitive.int, layout.maxFilas, "${pensum.slug}: filas")
        assertEquals(e.getValue("ancho").jsonPrimitive.double, layout.ancho, "${pensum.slug}: ancho")
        assertEquals(e.getValue("alto").jsonPrimitive.double, layout.alto, "${pensum.slug}: alto")
    }

    @Test
    fun cablesRuteados() = carreras().forEach { (pensum, e) ->
        val layout = calcularLayout(pensum.asignaturas, pensum.grupos)
        val esperadas = e.getValue("aristas").jsonArray.map { it.jsonArray }
        assertEquals(esperadas.size, layout.aristas.size, "${pensum.slug}: cuántos cables")
        esperadas.zip(layout.aristas).forEach { (esperada, real) ->
            val que = "${pensum.slug} · ${real.id}"
            assertEquals(esperada[0].jsonPrimitive.content, real.id, "$que: orden")
            assertEquals(if (esperada[1] is JsonNull) null else esperada[1].jsonPrimitive.content, real.area, "$que: área")
            assertEquals(esperada[2].jsonPrimitive.boolean, real.cruzaColumnas, "$que: cruza columnas")
            assertEquals(esperada[3].jsonPrimitive.content, comoSvg(real.trazo), "$que: trazado")
        }
    }

    @Test
    fun cadenas() = carreras().forEach { (pensum, e) ->
        val relaciones = calcularLayout(pensum.asignaturas, pensum.grupos).relaciones
        e.getValue("cadenas").jsonObject.forEach { (codigo, cadena) ->
            assertEquals(cadena.jsonArray.map { it.jsonPrimitive.content }, cadenaDe(codigo, relaciones).sorted(), "${pensum.slug} · $codigo: cadena")
        }
    }

    @Test
    fun franjaDeElectivas() = carreras().forEach { (pensum, e) ->
        val layout = calcularLayout(pensum.asignaturas, pensum.grupos)
        e.getValue("franjas").jsonArray.map { it.jsonObject }.forEach { f ->
            val que = "${pensum.slug} · ${f.getValue("caso").jsonPrimitive.content}"
            val marcas = f.getValue("marcas").jsonObject.mapValues { Marca.de(it.value.jsonPrimitive.content)!! }
            val aMano = f.getValue("aMano").jsonObject.mapValues { it.value.jsonPrimitive.content }
            val elegidas = adoptarMarcadas(aMano, pensum.grupos, marcas)
            assertEquals(f.getValue("elegidas").jsonObject.mapValues { it.value.jsonPrimitive.content }, elegidas, "$que: electivas adoptadas")
            assertEquals(f.getValue("libres").jsonPrimitive.int, casillasLibres(pensum.grupos).size, "$que: casillas libres posibles")

            val franja = calcularFranja(pensum.grupos, elegidas, layout.columnas.map { it.x }, layout.finSemestres + Franja.CORREDOR)
            assertEquals(
                f.getValue("nodos").jsonArray.map { n -> n.jsonArray.let { listOf(it[0].jsonPrimitive.content, it[1].jsonPrimitive.content, it[2].jsonPrimitive.content, it[3].jsonPrimitive.double, it[4].jsonPrimitive.double) } },
                franja.nodos.map { listOf(it.codigo, it.asignatura.nombre, it.asignatura.grupo, it.x, it.y) },
                "$que: casillas de la franja",
            )
            assertEquals(
                f.getValue("filas").jsonArray.map { n -> n.jsonArray.let { listOf(it[0].jsonPrimitive.content, it[1].jsonPrimitive.content, it[2].jsonPrimitive.intOrNull, it[3].jsonPrimitive.int, it[4].jsonPrimitive.int, it[5].jsonPrimitive.double, it[6].jsonPrimitive.double) } },
                franja.filas.map { listOf(it.clave, it.titulo, it.cuota, it.opciones, it.elegidas, it.x, it.y) },
                "$que: rótulos de la franja",
            )
            assertEquals(f.getValue("alto").jsonPrimitive.double, franja.alto, "$que: alto de la franja")
        }
    }

    /** El trazado en el formato del atributo d de la web, con los números como los escribe JS */
    private fun comoSvg(trazo: List<Segmento>): String = trazo.joinToString("") { s ->
        when (s) {
            is Segmento.Mover -> "M ${js(s.x)} ${js(s.y)}"
            is Segmento.Linea -> " L ${js(s.x)} ${js(s.y)}"
            is Segmento.Curva -> " C ${js(s.x1)} ${js(s.y1)}, ${js(s.x2)} ${js(s.y2)}, ${js(s.x)} ${js(s.y)}"
        }
    }

    private fun js(v: Double): String = if (v == kotlin.math.floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()

}
