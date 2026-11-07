package mpensum.carrera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import mpensum.logica.Periodo
import mpensum.logica.abreElProximo
import mpensum.logica.inscribiblesDelProximo
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

/*
 * «Tu próximo semestre»: qué inscribir. Primero lo que te conviene meter a tu
 * ritmo (el primer período del plan con que se calcula tu fecha), cada una
 * con por qué: si está en tu cadena o cuántas abre. Debajo, lo demás que
 * también podrás inscribir. Acostado: lo que conviene y el botón a la
 * izquierda, lo demás a la derecha.
 */

@Composable
internal fun DetalleProximo(estado: EstadoCarrera, hoy: Periodo, alVerEnMapa: (List<String>) -> Unit) {
    val pensum = estado.pensum
    val g = estado.graduacion
    val plan = g.plan.firstOrNull().orEmpty()
    val nombre = remember(pensum) { nombresDelPlan(pensum) }
    val uc = remember(pensum) { ucsDelPlan(pensum) }
    val esCasilla = remember(pensum) { esCasillaDelPlan(pensum) }
    val cadena = g.cuelloDeBotella.toSet()
    val abre = remember(estado.marcas, plan) {
        val tambien = inscribiblesDelProximo(pensum, estado.marcas).map { it.codigo }.filter { it !in plan }
        (plan + tambien).associateWith { if (esCasilla(it)) 0 else abreElProximo(pensum, estado.marcas, it).size }
    }
    val tambien = abre.keys.filter { it !in plan }

    fun queAbre(codigo: String): String = when {
        esCasilla(codigo) -> "Tú eliges cuál"
        else -> when (val n = abre[codigo] ?: 0) {
            0 -> "No abre otras"
            else -> "Abre $n"
        }
    }

    val boton: @Composable () -> Unit = { BotonTinta("Verlas en el mapa", { alVerEnMapa(plan) }, Modifier.padding(top = Espacio.xl)) }

    CuerpoDetalle(
        izquierda = {
            RotuloSeccion("TE CONVIENE · ${plan.size}", Paleta.azul, derecha = "${plan.sumOf(uc)} UC", arriba = 0.dp)
            TarjetaLista {
                FilasPlegables(plan) { c, primera ->
                    val enCadena = c in cadena
                    FilaLista(
                        titulo = nombre(c),
                        nota = nota(if (enCadena) "En tu cadena" else queAbre(c)),
                        colorNota = if (enCadena) Paleta.tinta else Paleta.tenue,
                        pesoNota = if (enCadena) FontWeight.Medium else FontWeight.Normal,
                        primera = primera,
                        icono = { IconoDeFila(IconosHoja.candadoAbierto, Paleta.azul) },
                        valor = { CifraFila("${uc(c)}", "UC") },
                    )
                }
            }
        },
        derecha = {
            if (tambien.isNotEmpty()) {
                RotuloSeccion("TAMBIÉN PUEDES · ${tambien.size}")
                TarjetaLista {
                    FilasPlegables(tambien) { c, primera ->
                        FilaLista(
                            titulo = nombre(c),
                            nota = nota(queAbre(c)),
                            primera = primera,
                            icono = { IconoDeFila(IconosHoja.candadoAbierto, Paleta.apagada) },
                            valor = { CifraFila("${uc(c)}", "UC") },
                        )
                    }
                }
            }
            boton()
        },
    )
}
