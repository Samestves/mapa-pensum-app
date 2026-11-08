package mpensum.carrera

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import mpensum.componentes.CifraConUnidad
import mpensum.datos.Asignatura
import mpensum.logica.Periodo
import mpensum.logica.abreAlAprobar
import mpensum.tema.Paleta
import kotlin.math.roundToInt

/*
 * «Este semestre»: qué te juegas con lo que cursas. Arriba la suma (a qué
 * avance llegas y cuántas se te abren), luego cada materia con lo que abre y
 * al final qué pasa con tu fecha si repruebas una. Lo que «abre» sale de las
 * prelaciones reales: lo que pasa a estar disponible cuando lo que cursas
 * queda aprobado. Acostado: la suma a la izquierda y las listas a la derecha.
 */

@Composable
internal fun DetalleEsteSemestre(estado: EstadoCarrera, hoy: Periodo) {
    // Reprobar una la vuelve a poner en lo que falta: si eso mueve la fecha, está en tu cadena. Una simulación por materia, precalculada
    val sim = estado.simulador
    Calculado(clave = sim, listo = { sim.retrasosListos }, calculo = { sim.retrasos }) { retrasos ->
        ContenidoEsteSemestre(estado, hoy, retrasos)
    }
}

@Composable
private fun ContenidoEsteSemestre(estado: EstadoCarrera, hoy: Periodo, retrasos: List<Pair<Asignatura, Periodo>>) {
    val resumen = estado.resumen
    val cursando = resumen.cursando
    val si = resumen.si ?: return
    val conUc = estado.progreso.porcentaje != null
    val fuerte = SpanStyle(color = Paleta.tinta, fontWeight = FontWeight.Medium)
    val abren = si.seAbren.size

    CuerpoDetalle(
        izquierda = {
            Comparacion(Modifier.fillMaxWidth().superficie(), izquierda = {
                ColumnaComparacion("SI APRUEBAS", "+${si.suma} ${if (conUc) "UC" else if (si.suma == 1) "materia" else "materias"}") {
                    CifraConUnidad("${si.avance.roundToInt()}", "%", 40.sp, Modifier.recorteCap(40.sp))
                }
            }, derecha = {
                ColumnaComparacion("SE TE ABREN", hoy.mas(1).rango(), Paleta.azul) {
                    CifraConUnidad("$abren", if (abren == 1) "materia" else "materias", 40.sp, Modifier.recorteCap(40.sp), color = Paleta.azul)
                }
            })
        },
        derecha = {
            RotuloSeccion("CURSAS")
            TarjetaLista {
                cursando.forEachIndexed { k, c ->
                    val abre = abreAlAprobar(c.codigo, si.seAbren)
                    val uc = c.uc?.let { " · $it\u00A0UC" }.orEmpty()
                    FilaLista(
                        titulo = c.nombre,
                        nota = buildAnnotatedString {
                            when (abre.size) {
                                0 -> append("No abre otras$uc")
                                else -> {
                                    append("Abre ")
                                    withStyle(fuerte) { append(abre.first().nombre) }
                                    if (abre.size > 1) append(" y ${abre.size - 1} más")
                                    append(uc)
                                }
                            }
                        },
                        primera = k == 0,
                        icono = { IconoDeFila(IconosHoja.libroAbierto, Paleta.cursando) },
                    )
                }
            }
            RotuloSeccion("SI REPRUEBAS UNA")
            TarjetaLista {
                if (retrasos.isEmpty()) {
                    FilaLista(
                        titulo = "Tu fecha no cambia",
                        nota = nota(if (cursando.size == 1) "No está en tu cadena" else "Ninguna está en tu cadena"),
                        primera = true,
                        icono = { IconoDeFila(IconosHoja.calendarioListo, Paleta.tinta) },
                    )
                } else {
                    retrasos.forEachIndexed { k, (c, f) ->
                        FilaLista(
                            titulo = "Reprobar ${c.nombre}",
                            nota = nota("Corre tu fecha a ${f.mesYAnio()}"),
                            primera = k == 0,
                            icono = { IconoDeFila(IconosHoja.eslabon, Paleta.tinta) },
                        )
                    }
                }
            }
        },
    )
}
