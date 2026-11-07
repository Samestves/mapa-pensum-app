package mpensum.carrera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpensum.componentes.CifraConUnidad
import mpensum.componentes.Lucide
import mpensum.logica.EstadoMateria
import mpensum.logica.Marca
import mpensum.logica.avanceDeGrupos
import mpensum.logica.ucTipicaDe
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import kotlin.math.ceil

/*
 * El detalle de un grupo de electivas: lo que llevas de lo que pide el título
 * con una ranura por electiva que hace falta (llena la aprobada, en contorno
 * la que cursas) y las casillas del plan donde van. Tocar una casilla abre el
 * selector de siempre, montado por la hoja encima de ella. Si el grupo no
 * tiene casillas en el plan, se listan las aprobadas.
 */

private val ROMANOS = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

/** Las ranuras: 8 dp de alto, radio 2, 4 dp entre ellas */
private val ALTO_RANURA = 8.dp
private val HUECO_RANURA = 4.dp

@Composable
internal fun DetalleElectivas(estado: EstadoCarrera, clave: String, alElegir: (String) -> Unit) {
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == clave } ?: return
    val avance = remember(estado.marcas, grupo) { avanceDeGrupos(listOf(grupo), estado.marcas)[clave] } ?: return
    val meta = avance.meta ?: 0
    val tipicas = remember(grupo) { ucTipicaDe(grupo) }
    val necesarias = if (meta > 0) ceil(meta.toFloat() / tipicas).toInt() else 0
    val cursando = grupo.asignaturas.count { estado.marcas[it.codigo] == Marca.CURSANDO }
    val abiertas = grupo.asignaturas.count { estado.estados[it.codigo] == EstadoMateria.DISPONIBLE }
    val casillas = remember(estado.pensum, clave) { estado.pensum.asignaturas.filter { it.esHueco && it.grupo == clave }.sortedBy { it.semestre ?: 99 } }

    CuerpoDetalle(
        izquierda = {
            Column(Modifier.fillMaxWidth().superficie().padding(Espacio.m)) {
                DosALoAncho(
                    hueco = Espacio.m,
                    vertical = Espacio.xs,
                    porAbajo = true,
                    izquierda = { CifraConUnidad("${avance.uc}", if (meta > 0) "/$meta UC" else "UC", 40.sp, Modifier.recorteCap(40.sp)) },
                    derecha = {
                        if (necesarias > 0) {
                            BasicText(
                                "$necesarias ${if (necesarias == 1) "electiva" else "electivas"} de $tipicas UC",
                                style = estiloNota().copy(textAlign = TextAlign.End),
                            )
                        }
                    },
                )
                if (necesarias > 0) Ranuras(necesarias, avance.elegidas.size, cursando, Modifier.padding(top = Espacio.m))
            }
        },
        derecha = {
            if (casillas.isNotEmpty()) {
                RotuloSeccion("EN TU PLAN")
                TarjetaLista {
                    FilasPlegables(casillas.withIndex().toList()) { (k, c), primera ->
                        val puesta = estado.enCasilla(c.codigo)
                        val nombre = puesta?.nombre ?: if (casillas.size > 1) "${c.nombre} ${ROMANOS.getOrElse(k) { "${k + 1}" }}" else c.nombre
                        val semestre = c.semestre?.let { "Semestre $it" } ?: "Sin semestre fijo"
                        val cuando = when {
                            puesta != null -> when (estado.estados[puesta.codigo]) {
                                EstadoMateria.APROBADA -> "Aprobada"
                                EstadoMateria.CURSANDO -> "Cursando"
                                EstadoMateria.DISPONIBLE -> "Puedes inscribirla"
                                else -> "Aún no se abre"
                            }
                            k == 0 && abiertas == 0 -> "aún no se abre"
                            else -> null
                        }
                        FilaLista(
                            titulo = nombre,
                            nota = nota(if (cuando != null) "$semestre · $cuando" else semestre),
                            primera = primera,
                            icono = { if (puesta != null) Pastilla(Lucide.listo) else PastillaVacia() },
                            chevron = Lucide.siguiente,
                            alTocar = { alElegir(c.codigo) },
                            valor = { BasicText(if (puesta == null) "Elegir" else "Cambiar", style = estiloNota().copy(fontSize = 15.sp, lineHeight = 20.sp)) },
                        )
                    }
                }
            } else {
                RotuloSeccion("APROBADAS")
                if (avance.elegidas.isEmpty()) {
                    BasicText("Aún no has aprobado ninguna.", Modifier.padding(horizontal = Espacio.m), style = estiloCuerpo())
                } else {
                    TarjetaLista {
                        avance.elegidas.forEachIndexed { k, a ->
                            FilaLista(
                                titulo = a.nombre,
                                nota = nota(a.codigo),
                                primera = k == 0,
                                icono = { Pastilla(Lucide.listo) },
                                valor = { CifraFila("${a.uc ?: tipicas}", "UC") },
                            )
                        }
                    }
                }
                BasicText(
                    "Este plan no fija en qué semestre llevas estas electivas. Cuentan las que marques como aprobadas en el mapa o en la lista, hasta completar las UC del título; lo que pase de ahí se muestra, pero no suma.",
                    Modifier.padding(start = Espacio.m, end = Espacio.m, top = Espacio.s),
                    style = estiloNota(),
                )
            }
        },
    )
}

/** Una ranura por electiva que pide el título: llenas las aprobadas, en contorno las que cursas y el resto pendientes */
@Composable
private fun Ranuras(total: Int, aprobadas: Int, cursando: Int, modifier: Modifier) {
    val tinta = Paleta.tinta
    val pendiente = tinta.copy(alpha = Velo.pendiente)
    Canvas(modifier.fillMaxWidth().height(ALTO_RANURA).semantics { contentDescription = "$aprobadas de $total electivas aprobadas" }) {
        val hueco = HUECO_RANURA.toPx()
        val ancho = (size.width - hueco * (total - 1)) / total
        val radio = CornerRadius(2.dp.toPx())
        val trazo = 1.5.dp.toPx()
        for (i in 0 until total) {
            val x = i * (ancho + hueco)
            when {
                i < aprobadas -> drawRoundRect(tinta, Offset(x, 0f), Size(ancho, size.height), radio)
                i < aprobadas + cursando -> drawRoundRect(tinta, Offset(x + trazo / 2, trazo / 2), Size(ancho - trazo, size.height - trazo), radio, style = Stroke(trazo))
                else -> drawRoundRect(pendiente, Offset(x, 0f), Size(ancho, size.height), radio)
            }
        }
    }
}
