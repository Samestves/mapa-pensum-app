package mpensum.carrera

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mpensum.logica.Periodo
import mpensum.logica.Simulador
import mpensum.logica.Situacion
import mpensum.logica.fechaSiRepruebas

/*
 * Lo que responde la ficha de una materia («¿puede esperar?»), sin dibujar nada: la cuenta, el estado de
 * esa cuenta y la caché por simulador. La fila de la ficha (RespuestaFicha) está en PiezasFicha.
 */

/** Una nota con una fecha en medio de ella: «Si la atrasas, te gradúas en **Jun 2030**»; en rojo si la fecha es mala noticia */
internal class NotaRespuesta(val antes: String, val fecha: String? = null, val alerta: Boolean = false)

/** Lo que responde la fila: su pastilla, la frase y la nota; `para` dice a qué estado de la materia pertenece */
internal class Respuesta(val para: Situacion, val icono: IconoFila, val azul: Boolean, val titulo: String, val nota: NotaRespuesta?)

/**
 * Lo que cuesta la fila: hasta 8 simulaciones la primera vez, y una más por cada materia. Va fuera del hilo
 * principal. Null si la materia no está en tu plan (una electiva que no cuenta) o ya está aprobada.
 */
internal fun respuestaDe(codigo: String, s: Situacion, sim: Simulador): Respuesta? {
    val g = sim.base
    return when (s) {
        Situacion.HECHA -> null
        Situacion.CURSANDO -> {
            val fecha = fechaSiRepruebas(sim.pensum, sim.marcas, sim.casillas, g.carga, sim.hoy, codigo)
            val cambia = fecha != g.periodoFinal
            Respuesta(s, IconoFila.REINICIAR, false, "Si la repruebas", fecha?.let { NotaRespuesta(if (cambia) "Te gradúas en " else "Sigues en ", it.mesYAnio(), cambia) })
        }
        Situacion.INSCRIBIBLE -> {
            if (g.plan.none { codigo in it }) return null
            val margen = sim.margen(codigo)
            if (margen == 0) {
                val tarde: Periodo? = sim.conVeto(codigo, sim.indiceEnPlan(codigo) + 1).periodoFinal
                Respuesta(s, IconoFila.ESLABON, g.cuelloDeBotella.firstOrNull() == codigo, "No puede esperar", tarde?.let { NotaRespuesta("Si la atrasas, te gradúas en ", it.mesYAnio(), true) })
            } else Respuesta(s, IconoFila.RELOJ_ARENA, false, puedeEsperar(margen), NotaRespuesta("Sin mover tu fecha"))
        }
        Situacion.PROXIMA, Situacion.LEJANA -> {
            val i = g.plan.indexOfFirst { codigo in it }
            if (i < 0) return null
            val margen = sim.margen(codigo)
            Respuesta(s, if (codigo in g.cuelloDeBotella) IconoFila.ESLABON else IconoFila.CALENDARIO, false, "En tu plan, ${sim.hoy.mas(i + 1).rango()}", NotaRespuesta(if (margen == 0) "No puede esperar" else puedeEsperar(margen)))
        }
    }
}

/** Lo que se sabe de la respuesta de una materia: solo `Calculando` reserva sitio; `SinRespuesta` es que no hay nada que decir */
internal sealed interface EstadoRespuesta {
    data object Calculando : EstadoRespuesta
    data object SinRespuesta : EstadoRespuesta
    class Lista(val respuesta: Respuesta) : EstadoRespuesta
}

/**
 * Las respuestas ya calculadas de las materias, para el simulador de ahora (si
 * cambian las marcas, `EstadoCarrera.respuestas` crea otro). Se calculan fuera
 * del hilo principal y la ficha las pide antes de cambiar de materia: así la
 * tarjeta entra con su texto puesto y no se funde dos veces.
 */
internal class RespuestasFicha(estado: EstadoCarrera) {
    private val sim = estado.simulador
    private val calculadas = mutableStateMapOf<Pair<String, Situacion>, EstadoRespuesta>()

    fun de(codigo: String, s: Situacion): EstadoRespuesta = calculadas[codigo to s] ?: EstadoRespuesta.Calculando

    suspend fun calcular(codigo: String, s: Situacion, despachador: CoroutineDispatcher = Dispatchers.Default) {
        val clave = codigo to s
        if (clave in calculadas) return
        val r = withContext(despachador) { respuestaDe(codigo, s, sim) }
        calculadas[clave] = r?.let { EstadoRespuesta.Lista(it) } ?: EstadoRespuesta.SinRespuesta
    }
}
