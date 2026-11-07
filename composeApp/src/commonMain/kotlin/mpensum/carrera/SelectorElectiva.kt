package mpensum.carrera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.datos.Asignatura
import mpensum.datos.Grupo
import mpensum.logica.EstadoMateria
import mpensum.mapa.tituloGrupo
import mpensum.tema.Espacio
import mpensum.tema.Tipo
import mpensum.tema.Alto
import mpensum.componentes.margenPantalla
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.jost

/*
 * Elegir qué electiva va en una casilla (SelectorElectiva.jsx). Solo enseña
 * las de su grupo, en tres secciones por lo que sirven para decidir: lo que
 * puedes cursar ya, lo que ya aprobaste y lo que aún no, cada una en su
 * tarjeta de filas. Lo usan la hoja modal (desde la lista y los semestres) y
 * la ficha del mapa, donde una casilla vacía es el propio selector.
 */

/** Con menos opciones el buscador estorba más de lo que ayuda */
private const val BUSCADOR_DESDE = 10

/** Las opciones se agrupan por lo que sirven para decidir: lo que puedes cursar ya, lo ya aprobado y lo que aún no */
private enum class Seccion(val titulo: String, val entra: (EstadoMateria?) -> Boolean) {
    PUEDES("PUEDES CURSARLAS YA", { it != EstadoMateria.APROBADA && it != EstadoMateria.BLOQUEADA }),
    APROBADAS("YA APROBADAS", { it == EstadoMateria.APROBADA }),
    BLOQUEADAS("CON PRELACIONES", { it == EstadoMateria.BLOQUEADA }),
}

private fun sinTildes(texto: String) = texto.lowercase()
    .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')

private val TITULO get() = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, letterSpacing = 0.14.em, color = Paleta.tenue)

@Composable
fun BoxScope.SelectorElectiva(
    casilla: String?,
    estado: EstadoCarrera,
    aprobarAlElegir: Boolean,
    alColocar: (String, String?) -> Unit,
    alCerrar: () -> Unit,
) {
    // La última casilla abierta: al cerrarse, la hoja baja enseñando lo que tenía
    var ultima by remember { mutableStateOf(casilla) }
    if (casilla != null && casilla != ultima) ultima = casilla
    val codigo = ultima ?: return
    val elegida = casillaElegida(estado, codigo) ?: return
    val nodo = elegida.nodo
    val grupo = elegida.grupo
    val j = jost()

    HojaInferior(
        abierta = casilla != null,
        alCerrar = alCerrar,
        cabecera = {
            Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.m), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    BasicText((if (nodo.semestre == null) tituloGrupo(grupo) else "Semestre ${nodo.semestre}").uppercase(), style = TITULO.copy(fontFamily = j))
                    BasicText(nodo.nombre, Modifier.padding(top = Espacio.xxs), style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = Tipo.xl, color = Paleta.tinta))
                }
                BotonCerrar(alCerrar)
            }
        },
    ) {
        ContenidoElectiva(elegida, estado, aprobarAlElegir, conBarraDeCuota = true, alColocar = alColocar, alCerrar = alCerrar)
    }
}

/** El aspa de las hojas: el mismo círculo que el de cerrar de las demás pantallas */
@Composable
fun BotonCerrar(alCerrar: () -> Unit) = BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)

/** La casilla que se está eligiendo, con lo que el selector necesita saber de su grupo */
internal class CasillaElegida(val codigo: String, val nodo: Asignatura, val grupo: Grupo)

internal fun casillaElegida(estado: EstadoCarrera, codigo: String): CasillaElegida? {
    val nodo = estado.nodos.firstOrNull { it.codigo == codigo }?.asignatura ?: return null
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == nodo.grupo } ?: return null
    return CasillaElegida(codigo, nodo, grupo)
}

/** «Elige 1 de 14 · llevas 0 de 6 UC»: lo que dice la cabecera de la ficha cuando la casilla está vacía */
internal fun cuotaDeCasilla(c: CasillaElegida, estado: EstadoCarrera): String {
    val uc = c.grupo.asignaturas.filter { estado.casillaDe(it.codigo) != null }.sumOf { it.uc ?: 0 }
    return "Elige 1 de ${c.grupo.asignaturas.size}" + (c.grupo.cuota?.let { " · llevas $uc de $it UC" } ?: "")
}

@Composable
internal fun ContenidoElectiva(
    elegida: CasillaElegida,
    estado: EstadoCarrera,
    aprobarAlElegir: Boolean,
    /** Si la cuota del grupo va en su barra (la hoja modal) o ya la dice la cabecera (la ficha) */
    conBarraDeCuota: Boolean,
    alColocar: (String, String?) -> Unit,
    alCerrar: () -> Unit,
) {
    val j = jost()
    val casilla = elegida.codigo
    val nodo = elegida.nodo
    val opciones = elegida.grupo.asignaturas
    val estados = estado.estados
    var busqueda by remember(casilla) { mutableStateOf("") }
    val puesta = opciones.firstOrNull { estado.casillaDe(it.codigo) == casilla }
    val enFranja = nodo.semestre == null
    // Una electiva aprobada o en curso no sale de la franja: volvería a entrar sola
    val puestaFija = enFranja && puesta != null && (estados[puesta.codigo] == EstadoMateria.APROBADA || estados[puesta.codigo] == EstadoMateria.CURSANDO)
    // La cuota se cuenta en UC, no en casillas: con dos de 3 se cumplen 6, aunque sobre una casilla
    val ucColocadas = opciones.filter { estado.casillaDe(it.codigo) != null }.sumOf { it.uc ?: 0 }
    val q = sinTildes(busqueda.trim())
    val porNombre = remember(opciones) { opciones.sortedBy { sinTildes(it.nombre) } }
    val coinciden = if (q.isEmpty()) porNombre else porNombre.filter { sinTildes(it.nombre).contains(q) || it.codigo.contains(q) }
    val secciones = Seccion.entries.map { s -> s to coinciden.filter { s.entra(estados[it.codigo]) } }.filter { it.second.isNotEmpty() }

    fun elegir(o: Asignatura) {
        val aqui = estado.casillaDe(o.codigo) == casilla
        // En la franja, una que ya está en tu mapa no se mueve de casilla: solo cambiaría de orden
        if (enFranja && estado.casillaDe(o.codigo) != null && !aqui) alCerrar() else alColocar(casilla, if (aqui) null else o.codigo)
    }

    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), bottom = Espacio.xxl)) {
        if (aprobarAlElegir && puesta == null) {
            BasicText("✓  Es lo único que le falta a este semestre. La que elijas queda aprobada.", Modifier.padding(bottom = Espacio.m), style = TextStyle(fontFamily = j, fontSize = Tipo.s, color = Paleta.verde))
        }
        if (conBarraDeCuota) elegida.grupo.cuota?.let { CuotaGrupo("UC elegidas de este grupo", ucColocadas, it) }
        puesta?.let { p ->
            Row(
                Modifier.padding(top = if (conBarraDeCuota) Espacio.m else 0.dp).fillMaxWidth().clip(RoundedCornerShape(Radio.Tarjeta)).background(Paleta.tinta.copy(alpha = 0.04f)).padding(Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
            ) {
                FichaUc(p, estado, apagada = false)
                Column(Modifier.weight(1f)) {
                    BasicText(if (enFranja) "EN TU MAPA" else "EN ESTA CASILLA", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = Tipo.xxs, letterSpacing = 0.1.em, color = Paleta.verde))
                    BasicText(p.nombre, style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = Tipo.m, color = Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!puestaFija) {
                    Box(
                        Modifier.height(Alto.toque).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.06f)).clickable(role = Role.Button) { alColocar(casilla, null) }.padding(horizontal = Espacio.s),
                        contentAlignment = Alignment.Center,
                    ) { BasicText("Quitar", style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = Paleta.suave)) }
                }
            }
        }
        if (opciones.size > BUSCADOR_DESDE) Buscador(busqueda, { busqueda = it }, Modifier.padding(top = Espacio.m))
        secciones.forEach { (seccion, lista) ->
            RotuloSeccion("${seccion.titulo} · ${lista.size}", arriba = Espacio.m)
            TarjetaLista {
                lista.forEachIndexed { k, o ->
                    val donde = estado.casillaDe(o.codigo)
                    FilaElectiva(o, estados[o.codigo], primera = k == 0, aqui = donde == casilla, enOtra = donde != null && donde != casilla, enFranja = enFranja) { elegir(o) }
                }
            }
        }
        if (coinciden.isEmpty()) {
            BasicText("Ninguna coincide con «$busqueda»", Modifier.fillMaxWidth().padding(vertical = Espacio.xl), style = TextStyle(fontFamily = j, fontSize = Tipo.s, color = Paleta.tenue))
        }
        // El semestre de la casilla es la ruta que sugiere la UDO, no un registro de cuándo la cursaste
        BasicText(
            if (enFranja) "La UDO no publica en qué semestre va cada electiva de esta carrera. Añade las que vayas a cursar y quedan en tu mapa, debajo de los semestres."
            else "El semestre es la ruta que sugiere la UDO. Si la cursaste en otro, ponla igual: para el título cuentan las UC, no el semestre.",
            Modifier.padding(top = Espacio.m),
            style = TextStyle(fontFamily = j, fontSize = Tipo.xs, lineHeight = 16.sp, color = Paleta.tenue),
        )
    }
}

/** El campo de buscar por nombre o código, con su etiqueta para el lector de pantalla */
@Composable
private fun Buscador(busqueda: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    val j = jost()
    Box(
        modifier.fillMaxWidth().heightIn(min = Alto.toque).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.05f)).padding(horizontal = Espacio.m),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (busqueda.isEmpty()) BasicText("Buscar por nombre o código", style = TextStyle(fontFamily = j, fontSize = Tipo.m, color = Paleta.tenue))
        BasicTextField(
            busqueda, alCambiar, Modifier.fillMaxWidth().semantics { contentDescription = "Buscar por nombre o código" }, singleLine = true,
            textStyle = TextStyle(fontFamily = j, fontSize = Tipo.m, color = Paleta.tinta), cursorBrush = SolidColor(Paleta.tinta),
        )
    }
}

/** Las UC de una electiva en un cuadrito de su color */
@Composable
private fun FichaUc(materia: Asignatura, estado: EstadoCarrera, apagada: Boolean) {
    val color = estado.colores.de(materia)
    val j = jost()
    Column(
        Modifier.size(40.dp).clip(RoundedCornerShape(Radio.Chip)).background(color.copy(alpha = 0.16f * (if (apagada) 0.5f else 1f))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val tinta = lerp(color, Paleta.tinta, 0.28f).copy(alpha = if (apagada) 0.5f else 1f)
        BasicText("${materia.uc ?: 0}", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = Tipo.l, lineHeight = 15.sp, color = tinta))
        BasicText("UC", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = 8.5.sp, lineHeight = 9.sp, letterSpacing = 0.08.em, color = tinta))
    }
}

/** Una opción de la lista: el aro de elegir (con el visto si es la de esta casilla), su nombre, lo que ya es y sus UC */
@Composable
private fun FilaElectiva(
    materia: Asignatura,
    e: EstadoMateria?,
    primera: Boolean,
    aqui: Boolean,
    enOtra: Boolean,
    enFranja: Boolean,
    alElegir: () -> Unit,
) {
    val nota: Pair<String, Color>? = when {
        e == EstadoMateria.APROBADA -> "Aprobada" to Paleta.verde
        e == EstadoMateria.CURSANDO -> "Cursando" to Paleta.cursando
        enOtra -> (if (enFranja) "Ya en tu mapa" else "En otra casilla") to Paleta.tenue
        else -> null
    }
    FilaLista(
        titulo = materia.nombre,
        nota = nota?.let { nota(it.first) },
        colorNota = nota?.second ?: Paleta.tenue,
        primera = primera,
        estiloTitulo = estiloFila(if (e == EstadoMateria.BLOQUEADA) Paleta.suave else Paleta.tinta),
        icono = {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(if (aqui) Paleta.verde else Color.Transparent)
                        .border(1.5.dp, if (aqui) Color.Transparent else Paleta.tinta.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (aqui) IconoLucide(Lucide.listo, Paleta.lienzo, 14.dp) }
            }
        },
        valor = { CifraFila("${materia.uc ?: 0}", "UC") },
        alTocar = alElegir,
    )
}

/** Cuánto llevas de una cuota de electivas, en UC, con su barra */
@Composable
fun CuotaGrupo(titulo: String, uc: Int, meta: Int?) {
    val j = jost()
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(titulo, Modifier.weight(1f), style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val completa = meta != null && uc >= meta
            BasicText(
                (if (completa) "✓ " else "") + "$uc" + (meta?.let { " de $it" } ?: "") + " UC",
                style = TextStyle(fontFamily = j, fontSize = 12.5.sp, color = if (completa) Paleta.verde else Paleta.tenue),
            )
        }
        if (meta != null) {
            Box(Modifier.padding(top = Espacio.xs).fillMaxWidth().height(6.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth((uc.toFloat() / meta).coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(Paleta.verde))
            }
        }
    }
}
