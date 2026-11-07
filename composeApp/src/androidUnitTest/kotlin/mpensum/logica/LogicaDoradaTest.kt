package mpensum.logica

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.datos.leerPensum
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Compara la lógica de la app con la de la web, carrera por carrera y avance
 * por avance. Las fixtures salen de ejecutar el JS de la web
 * (herramientas/fixtures.mjs): si esto pasa, la app y la web dicen lo mismo
 * de cada materia, de cada cable y del porcentaje.
 */
class LogicaDoradaTest {

    private val carpetaFixtures = File("src/androidUnitTest/fixtures")
    private val carpetaCarreras = File("src/commonMain/composeResources/files/carreras")

    private fun casos(): List<Triple<Pensum, String, JsonObject>> {
        val archivos = carpetaFixtures.listFiles { f -> f.extension == "json" }?.sortedBy { it.name }
            ?: fail("No hay fixtures en ${carpetaFixtures.absolutePath}: corre herramientas/fixtures.mjs")
        assertEquals(9, archivos.size, "Faltan carreras en las fixtures")
        return archivos.flatMap { archivo ->
            val pensum = sinUcMinimas(leerPensum(File(carpetaCarreras, archivo.name).readText()))
            Json.parseToJsonElement(archivo.readText()).jsonObject.getValue("casos").jsonArray.map {
                val caso = it.jsonObject
                Triple(pensum, "${pensum.slug} · ${caso.texto("nombre")}", caso)
            }
        }
    }

    /**
     * La web no mira las «120 UC aprobadas» de Proyecto de Trabajo de Grado
     * (Ambiental): lo da por inscribible desde el primer día. La app sí lo
     * respeta (QaLogicaTest), así que aquí se compara sin ese requisito.
     */
    private fun sinUcMinimas(p: Pensum) = p.copy(asignaturas = p.asignaturas.map { it.copy(requisitoEspecial = null) })

    @Test
    fun estadosSituacionesYCables() = casos().forEach { (pensum, nombre, caso) ->
        val marcas = depurarMarcas(caso.mapaDeTexto("crudas"), pensum.todas.map { it.codigo }.toSet())
        assertEquals(caso.mapaDeTexto("marcas"), marcas.mapValues { it.value.valor }, "$nombre: marcas depuradas")

        val estados = estadosDe(pensum.todas, marcas)
        assertEquals(caso.mapaDeTexto("estados"), estados.mapValues { it.value.name.lowercase() }, "$nombre: estados")

        val situaciones = pensum.todas.associate { it.codigo to situacionDe(it.codigo, it.prerrequisitos, estados) }
        assertEquals(caso.mapaDeTexto("situaciones"), situaciones.mapValues { it.value.name.lowercase() }, "$nombre: situaciones")

        caso.getValue("tramos").jsonArray.forEach { t ->
            val (origen, destino, esperado) = t.jsonArray.map { it.jsonPrimitive.content }
            assertEquals(esperado, tramoDe(situaciones.getValue(origen), situaciones.getValue(destino)).name.lowercase(), "$nombre: cable $origen → $destino")
        }
    }

    @Test
    fun avanceYGrupos() = casos().forEach { (pensum, nombre, caso) ->
        val marcas = depurarMarcas(caso.mapaDeTexto("crudas"), pensum.todas.map { it.codigo }.toSet())
        val grupos = avanceDeGrupos(pensum.grupos, marcas)
        caso.getValue("grupos").jsonObject.forEach { (clave, g) ->
            val real = grupos.getValue(clave)
            val o = g.jsonObject
            assertEquals(o.getValue("uc").jsonPrimitive.int, real.uc, "$nombre: UC del grupo $clave")
            assertEquals(o.getValue("completa").jsonPrimitive.content.toBoolean(), real.completa, "$nombre: grupo $clave completo")
            assertEquals(o.getValue("excedente").jsonPrimitive.int, real.excedente, "$nombre: excedente de $clave")
            assertEquals(o.getValue("elegidas").jsonArray.map { it.jsonPrimitive.content }, real.elegidas.map { it.codigo }, "$nombre: elegidas de $clave")
        }

        val p = progresoDe(pensum.obligatorias, estadosDe(pensum.todas, marcas), pensum.creditos, grupos)
        val e = caso.getValue("progreso").jsonObject
        assertEquals(e.entero("ucAprobadas"), p.ucAprobadas, "$nombre: UC aprobadas")
        assertEquals(e.entero("ucTotales"), p.ucTotales, "$nombre: UC totales")
        assertEquals(e.entero("ucTitulo"), p.ucTitulo, "$nombre: UC del título")
        assertEquals(e.entero("ucElectivas"), p.ucElectivas, "$nombre: UC electivas")
        casiIgual(e.decimal("porcentaje"), p.porcentaje, "$nombre: porcentaje")
        casiIgual(e.decimal("porcentajeObligatorias"), p.porcentajeObligatorias, "$nombre: porcentaje de obligatorias")
        assertEquals(e.entero("aprobadas"), p.aprobadas, "$nombre: aprobadas")
        assertEquals(e.entero("cursando"), p.cursando, "$nombre: cursando")
        assertEquals(e.entero("disponibles"), p.disponibles, "$nombre: disponibles")
        assertEquals(e.entero("bloqueadas"), p.bloqueadas, "$nombre: bloqueadas")
        assertEquals(e.entero("total"), p.total, "$nombre: total")
        assertEquals(e.getValue("paraInscribir").jsonArray.map { it.jsonPrimitive.content }, p.paraInscribir.map { it.codigo }, "$nombre: orden de lo inscribible")
        casiIgual(caso.getValue("avance").jsonPrimitive.doubleOrNull, p.avance, "$nombre: avance")
    }

    @Test
    fun resumenDeLaPortada() = casos().forEach { (pensum, nombre, caso) ->
        val r = resumenParaPortada(pensum, caso.mapaDeTexto("crudas"))
        val e = caso.getValue("portada").jsonObject
        assertEquals(e.getValue("hechas").jsonArray.map { it.jsonPrimitive.int }, r.hechas, "$nombre: aprobadas por semestre")
        assertEquals(e.entero("porcentaje"), r.porcentaje, "$nombre: porcentaje redondeado")
        assertEquals(e.entero("llevas"), r.llevas, "$nombre: cuánto llevas")
        assertEquals(e.entero("de"), r.de, "$nombre: de cuánto")
        assertEquals(e.texto("unidad"), r.unidad, "$nombre: unidad")
        assertEquals(e.entero("disponibles"), r.disponibles, "$nombre: disponibles")
    }

    @Test
    fun semestresYSuCasilla() = casos().forEach { (pensum, nombre, caso) ->
        val marcas = depurarMarcas(caso.mapaDeTexto("crudas"), pensum.todas.map { it.codigo }.toSet())
        val estados = estadosDe(pensum.todas, marcas)
        val porCodigo = pensum.todas.associateBy { it.codigo }
        val casillas = caso.mapaDeTexto("casillas")
        val enCasilla: (String) -> Asignatura? = { casillas[it]?.let(porCodigo::get) }

        val cuentas = marcasDeSemestres(pensum.asignaturas, enCasilla, estados)
        caso.getValue("semestres").jsonArray.forEach { s ->
            val o = s.jsonObject
            val c = cuentas.getValue(o.entero("semestre")!!)
            val esperado = listOf(o.entero("total"), o.entero("hechas"), o.entero("cursando"), o.entero("huecos"), o.texto("marca"))
            assertEquals(esperado, listOf(c.total, c.hechas, c.cursando, c.huecos, c.marca.name.lowercase().replace('_', '-')), "$nombre: semestre ${o.entero("semestre")}")
        }

        caso.getValue("acciones").jsonArray.forEach { a ->
            val o = a.jsonObject
            val semestre = o.entero("semestre")!!
            val real = accionDeSemestre(pensum.asignaturas, semestre, enCasilla, estados)
            val esperada = o.getValue("accion")
            val texto = when (real) {
                null -> "null"
                is AccionSemestre.Aprobar -> "aprobar:" + real.codigos.joinToString(",")
                is AccionSemestre.Desmarcar -> "desmarcar:" + real.codigos.joinToString(",")
                is AccionSemestre.Elegir -> "elegir:" + real.casilla
            }
            val textoEsperado = if (esperada is JsonNull) "null" else esperada.jsonObject.let { x ->
                val tipo = x.texto("tipo")
                tipo + ":" + (x["casilla"]?.jsonPrimitive?.content ?: x.getValue("codigos").jsonArray.joinToString(",") { it.jsonPrimitive.content })
            }
            assertEquals(textoEsperado, texto, "$nombre: acción del semestre $semestre")
        }
    }

    @Test
    fun ordenEnEspanol() {
        val nombres = listOf("Óptica", "Ñame", "nube", "Álgebra", "algoritmos", "Oro", "Nutrición")
        assertEquals(listOf("Álgebra", "algoritmos", "nube", "Nutrición", "Ñame", "Óptica", "Oro"), nombres.sortedWith(::compararEs))
    }

    private fun casiIgual(esperado: Double?, real: Double?, que: String) {
        if (esperado == null || real == null) return assertEquals(esperado, real, que)
        assertTrue(abs(esperado - real) < 1e-9, "$que: esperaba $esperado y salió $real")
    }

    private fun JsonObject.texto(clave: String) = getValue(clave).jsonPrimitive.content
    private fun JsonObject.entero(clave: String): Int? = (get(clave) as? JsonElement)?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
    private fun JsonObject.decimal(clave: String): Double? = (get(clave) as? JsonElement)?.takeIf { it !is JsonNull }?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.mapaDeTexto(clave: String): Map<String, String> =
        getValue(clave).jsonObject.mapValues { it.value.jsonPrimitive.content }

}
