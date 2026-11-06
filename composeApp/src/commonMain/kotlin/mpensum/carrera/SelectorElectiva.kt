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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.datos.Asignatura
import mpensum.logica.EstadoMateria
import mpensum.mapa.tituloGrupo
import mpensum.tema.Paleta
import mpensum.tema.jost

/*
 * Elegir qué electiva va en una casilla (SelectorElectiva.jsx). Solo enseña
 * las de su grupo, en tres pestañas por lo que sirven para decidir: lo que
 * puedes cursar ya, lo que ya aprobaste y lo que aún no. Las fichas en
 * mosaico, no una lista larga: veinticinco filas iguales se leen como un
 * formulario.
 */

/** Con menos opciones el buscador estorba más de lo que ayuda */
private const val BUSCADOR_DESDE = 10

private enum class Pestana(val titulo: String, val entra: (EstadoMateria?) -> Boolean) {
    PUEDES("Puedes cursar", { it != EstadoMateria.APROBADA && it != EstadoMateria.BLOQUEADA }),
    APROBADAS("Aprobadas", { it == EstadoMateria.APROBADA }),
    BLOQUEADAS("Con prelaciones", { it == EstadoMateria.BLOQUEADA }),
}

private fun sinTildes(texto: String) = texto.lowercase()
    .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')

private val TITULO get() = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em, color = Paleta.tenue)

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
    val nodo = estado.nodos.firstOrNull { it.codigo == codigo }?.asignatura ?: return
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == nodo.grupo } ?: return
    val j = jost()

    HojaInferior(
        abierta = casilla != null,
        alCerrar = alCerrar,
        cabecera = {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    BasicText((if (nodo.semestre == null) tituloGrupo(grupo) else "Semestre ${nodo.semestre}").uppercase(), style = TITULO.copy(fontFamily = j))
                    BasicText(nodo.nombre, Modifier.padding(top = 4.dp), style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Paleta.tinta))
                }
                BotonCerrar(alCerrar)
            }
        },
    ) {
        Contenido(codigo, nodo, grupo.asignaturas, grupo.cuota, estado, aprobarAlElegir, alColocar, alCerrar)
    }
}

@Composable
fun BotonCerrar(alCerrar: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.08f)).clickable(role = Role.Button, onClick = alCerrar),
        contentAlignment = Alignment.Center,
    ) { IconoTrazo(Lucide.cerrar, Paleta.suave, 16.dp, 2.2f) }
}

@Composable
private fun Contenido(
    casilla: String,
    nodo: Asignatura,
    opciones: List<Asignatura>,
    cuota: Int?,
    estado: EstadoCarrera,
    aprobarAlElegir: Boolean,
    alColocar: (String, String?) -> Unit,
    alCerrar: () -> Unit,
) {
    val j = jost()
    val estados = estado.estados
    var busqueda by remember(casilla) { mutableStateOf("") }
    val puesta = opciones.firstOrNull { estado.casillaDe(it.codigo) == casilla }
    val enFranja = nodo.semestre == null
    // Una electiva aprobada o en curso no sale de la franja: volvería a entrar sola
    val puestaFija = enFranja && puesta != null && (estados[puesta.codigo] == EstadoMateria.APROBADA || estados[puesta.codigo] == EstadoMateria.CURSANDO)
    // La cuota se cuenta en UC, no en casillas: con dos de 3 se cumplen 6, aunque sobre una casilla
    val ucColocadas = opciones.filter { estado.casillaDe(it.codigo) != null }.sumOf { it.uc ?: 0 }
    val pestanas = Pestana.entries.map { p -> p to opciones.filter { p.entra(estados[it.codigo]) }.sortedBy { sinTildes(it.nombre) } }.filter { it.second.isNotEmpty() }
    var elegida by remember(casilla) { mutableStateOf(pestanas.firstOrNull()?.first) }
    val pestana = pestanas.firstOrNull { it.first == elegida } ?: pestanas.firstOrNull()
    val q = sinTildes(busqueda.trim())
    val visibles = if (q.isNotEmpty()) opciones.filter { sinTildes(it.nombre).contains(q) || it.codigo.contains(q) }.sortedBy { sinTildes(it.nombre) } else pestana?.second.orEmpty()

    fun elegir(o: Asignatura) {
        val aqui = estado.casillaDe(o.codigo) == casilla
        // En la franja, una que ya está en tu mapa no se mueve de casilla: solo cambiaría de orden
        if (enFranja && estado.casillaDe(o.codigo) != null && !aqui) alCerrar() else alColocar(casilla, if (aqui) null else o.codigo)
    }

    Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (aprobarAlElegir && puesta == null) {
            BasicText("✓  Es lo único que le falta a este semestre. La que elijas queda aprobada.", style = TextStyle(fontFamily = j, fontSize = 13.sp, color = Paleta.verde))
        }
        cuota?.let { CuotaGrupo("UC elegidas de este grupo", ucColocadas, it) }
        puesta?.let { p ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Paleta.tinta.copy(alpha = 0.04f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FichaUc(p, estado, apagada = false)
                Column(Modifier.weight(1f)) {
                    BasicText(if (enFranja) "EN TU MAPA" else "EN ESTA CASILLA", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.1.em, color = Paleta.verde))
                    BasicText(p.nombre, style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!puestaFija) {
                    Box(
                        Modifier.height(32.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.06f)).clickable(role = Role.Button) { alColocar(casilla, null) }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { BasicText("Quitar", style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = Paleta.suave)) }
                }
            }
        }
        if (opciones.size > BUSCADOR_DESDE) {
            Box(Modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.05f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (busqueda.isEmpty()) BasicText("Buscar por nombre o código", style = TextStyle(fontFamily = j, fontSize = 14.sp, color = Paleta.tenue))
                BasicTextField(
                    busqueda, { busqueda = it }, Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(fontFamily = j, fontSize = 14.sp, color = Paleta.tinta), cursorBrush = SolidColor(Paleta.tinta),
                )
            }
        }
        if (q.isEmpty() && pestanas.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pestanas.forEach { (p, lista) ->
                    val activa = p == pestana?.first
                    Row(
                        Modifier.height(32.dp).clip(CircleShape).background(if (activa) Paleta.tinta else Paleta.tinta.copy(alpha = 0.05f))
                            .clickable(role = Role.Tab) { elegida = p }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BasicText(p.titulo, style = TextStyle(fontFamily = j, fontSize = 12.5.sp, color = if (activa) Paleta.panel else Paleta.suave))
                        BasicText("${lista.size}", style = TextStyle(fontFamily = j, fontSize = 12.5.sp, color = if (activa) Paleta.panel.copy(alpha = 0.6f) else Paleta.tenue))
                    }
                }
            }
        }
        if (visibles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibles.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { o ->
                            val donde = estado.casillaDe(o.codigo)
                            FichaElectiva(o, estado, estados[o.codigo], aqui = donde == casilla, enOtra = donde != null && donde != casilla, enFranja = enFranja, modifier = Modifier.weight(1f)) { elegir(o) }
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else if (q.isNotEmpty()) {
            BasicText("Ninguna coincide con «$busqueda»", Modifier.fillMaxWidth().padding(vertical = 24.dp), style = TextStyle(fontFamily = j, fontSize = 13.sp, color = Paleta.tenue))
        }
        // El semestre de la casilla es la ruta que sugiere la UDO, no un registro de cuándo la cursaste
        BasicText(
            if (enFranja) "La UDO no publica en qué semestre va cada electiva de esta carrera. Añade las que vayas a cursar y quedan en tu mapa, debajo de los semestres."
            else "El semestre es la ruta que sugiere la UDO. Si la cursaste en otro, ponla igual: para el título cuentan las UC, no el semestre.",
            style = TextStyle(fontFamily = j, fontSize = 12.sp, lineHeight = 16.sp, color = Paleta.tenue),
        )
    }
}

/** Las UC de una electiva en un cuadrito de su color */
@Composable
private fun FichaUc(materia: Asignatura, estado: EstadoCarrera, apagada: Boolean) {
    val color = estado.colores.de(materia)
    val j = jost()
    Column(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.16f * (if (apagada) 0.5f else 1f))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val tinta = lerp(color, Paleta.tinta, 0.28f).copy(alpha = if (apagada) 0.5f else 1f)
        BasicText("${materia.uc ?: 0}", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 15.sp, color = tinta))
        BasicText("UC", style = TextStyle(fontFamily = j, fontWeight = FontWeight.SemiBold, fontSize = 8.5.sp, lineHeight = 9.sp, letterSpacing = 0.08.em, color = tinta))
    }
}

@Composable
private fun FichaElectiva(
    materia: Asignatura,
    estado: EstadoCarrera,
    e: EstadoMateria?,
    aqui: Boolean,
    enOtra: Boolean,
    enFranja: Boolean,
    modifier: Modifier,
    alElegir: () -> Unit,
) {
    val j = jost()
    val bloqueada = e == EstadoMateria.BLOQUEADA
    val nota: Pair<String, Color>? = when {
        e == EstadoMateria.APROBADA -> "Aprobada" to Paleta.verde
        e == EstadoMateria.CURSANDO -> "Cursando" to Paleta.ambar
        enOtra -> (if (enFranja) "Ya en tu mapa" else "En otra casilla") to Paleta.tenue
        else -> null
    }
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (aqui) Paleta.verde.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (aqui) Paleta.verde.copy(alpha = 0.5f) else BORDE_PANEL, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = alElegir)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            FichaUc(materia, estado, bloqueada)
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (aqui) Paleta.verde else Color.Transparent)
                    .border(2.dp, if (aqui) Color.Transparent else Paleta.tinta.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (aqui) BasicText("✓", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Paleta.lienzo)) }
        }
        BasicText(
            materia.nombre,
            style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 17.sp, color = if (bloqueada) Paleta.suave else Paleta.tinta),
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(materia.codigo, style = TextStyle(fontFamily = j, fontSize = 11.5.sp, color = Paleta.tenue))
            nota?.let { (texto, color) -> BasicText(texto, style = TextStyle(fontFamily = j, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, color = color)) }
        }
    }
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
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Paleta.tinta.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth((uc.toFloat() / meta).coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(Paleta.verde))
            }
        }
    }
}
