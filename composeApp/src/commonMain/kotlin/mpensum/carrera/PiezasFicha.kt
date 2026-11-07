package mpensum.carrera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mpensum.componentes.Lucide
import mpensum.componentes.margenPantalla
import mpensum.componentes.pulsable
import mpensum.componentes.ventana
import mpensum.datos.Asignatura
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.logica.Situacion
import mpensum.logica.fechaSiRepruebas
import mpensum.logica.seAbrenAlAprobar
import mpensum.mapa.nombreCasilla
import mpensum.tema.Espacio
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tipo
import mpensum.tema.plex

/*
 * Las piezas de la ficha de una materia (docs/ficha-y-lista.md §1). Cada una
 * va por separado porque la ficha de pie y la acostada las componen distinto:
 * la cabecera (quién es), la píldora de estado, la respuesta («¿puede
 * esperar?»), «Pide · Abre» y los botones de marcar. Ninguna sabe en qué hoja
 * está; todas reciben el estado de la carrera y el código de la materia.
 */

/** Lo que cambia de una materia a otra se funde en 150 ms, sin cambiar el alto de golpe a media animación */
private const val FUNDIDO_MS = 150

/** La fila de la respuesta reserva este alto desde el primer cuadro: la hoja no crece cuando llega el cálculo */
private val ALTO_RESPUESTA = 64.dp

/** Un estado de la materia como lo dice la píldora; «por elegir» es el de una casilla de electiva vacía */
internal enum class EstadoFicha(val palabra: String) {
    DISPONIBLE("DISPONIBLE"),
    CURSANDO("CURSANDO"),
    APROBADA("APROBADA"),
    BLOQUEADA("BLOQUEADA"),
    PROXIMA("PRÓXIMA"),
    POR_ELEGIR("POR ELEGIR"),
}

private fun estadoFichaDe(s: Situacion) = when (s) {
    Situacion.HECHA -> EstadoFicha.APROBADA
    Situacion.CURSANDO -> EstadoFicha.CURSANDO
    Situacion.INSCRIBIBLE -> EstadoFicha.DISPONIBLE
    Situacion.PROXIMA -> EstadoFicha.PROXIMA
    Situacion.LEJANA -> EstadoFicha.BLOQUEADA
}

private fun situacionDeFicha(e: EstadoFicha) = when (e) {
    EstadoFicha.APROBADA -> Situacion.HECHA
    EstadoFicha.CURSANDO -> Situacion.CURSANDO
    EstadoFicha.DISPONIBLE -> Situacion.INSCRIBIBLE
    EstadoFicha.PROXIMA -> Situacion.PROXIMA
    else -> Situacion.LEJANA
}

private fun nombreArea(area: String?) = when (area) {
    "generales" -> "Generales"
    "ciencias-basicas" -> "Ciencias básicas"
    "estadistica" -> "Estadística"
    "computacion" -> "Computación"
    "electronica" -> "Electrónica"
    "sistemas" -> "Sistemas"
    "gestion" -> "Gestión"
    "tesis" -> "Trabajo de grado"
    else -> null
}

/** Lo que dice la cabecera de una materia: se calcula una vez y la usan la ficha de pie y la acostada */
internal class EncabezadoFicha(
    val rotulo: String,
    /** Su código, salvo en una casilla vacía, que aún no es ninguna materia */
    val codigo: String?,
    val titulo: String,
    /** «2 UC · Gestión», o lo que lleva elegido el grupo de una casilla vacía */
    val datos: String,
    val estado: EstadoFicha,
    /** La casilla donde está puesta, si es una electiva: de ahí sale «Cambiar» */
    val casilla: String?,
)

/** Null si la materia no existe en este pensum */
internal fun encabezadoDe(codigo: String, estado: EstadoCarrera): EncabezadoFicha? {
    val nodo = estado.layout.porCodigo[codigo] ?: return null
    val materia = nodo.asignatura
    val grupo = estado.pensum.grupos.firstOrNull { it.clave == materia.grupo }
    if (materia.esHueco && estado.enCasilla(codigo) == null) {
        val vacia = casillaElegida(estado, codigo)
        return EncabezadoFicha(
            rotulo = (materia.semestre?.let { "SEMESTRE $it · " } ?: "") + "ELECTIVA",
            codigo = null,
            titulo = materia.nombre,
            datos = vacia?.let { cuotaDeCasilla(it, estado) } ?: "",
            estado = EstadoFicha.POR_ELEGIR,
            casilla = null,
        )
    }
    val casilla = estado.casillaDe(codigo)
    val semestre = materia.semestre ?: casilla?.let { c -> estado.nodos.firstOrNull { it.codigo == c }?.semestre }
    val esElectiva = materia.semestre == null
    val rotulo = listOfNotNull(
        semestre?.let { "SEMESTRE $it" },
        if (esElectiva) (grupo?.let { nombreCasilla(it).uppercase() } ?: "ELECTIVA") else null,
    ).joinToString(" · ")
    val datos = listOfNotNull("${materia.uc ?: 0} UC", nombreArea(materia.area)).joinToString(" · ")
    return EncabezadoFicha(rotulo, materia.codigo, materia.nombre, datos, estadoFichaDe(estado.situaciones[codigo] ?: Situacion.LEJANA), casilla)
}

/** El fundido de 150 ms al cambiar de materia: el alto cambia de golpe, no a media animación */
@Composable
internal fun FundidoDeMateria(codigo: String, modifier: Modifier = Modifier, contenido: @Composable (String) -> Unit) {
    AnimatedContent(
        targetState = codigo,
        modifier = modifier,
        transitionSpec = { (fadeIn(tween(FUNDIDO_MS)) togetherWith fadeOut(tween(FUNDIDO_MS))).using(null) },
        label = "materia",
    ) { contenido(it) }
}

// ─────────────────────────────────── Cabecera ──────────────────────────────────

/** Rótulo y código a la izquierda, un texto-botón opcional («Cambiar») y la X (la burbuja no lleva): una fila de 44, el toque mínimo */
@Composable
internal fun RotuloFicha(enc: EncabezadoFicha, alCerrar: () -> Unit, alCambiar: (String) -> Unit, conCerrar: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (conCerrar) 44.dp else 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText(enc.rotulo, style = estiloRotulo(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (enc.codigo != null) {
                BasicText(enc.codigo, Modifier.padding(start = Espacio.xs), style = TextStyle(fontFamily = plex(), fontWeight = FontWeight.Light, fontSize = Tipo.xxs, lineHeight = 16.sp, letterSpacing = 0.04.em, color = Paleta.tenue), maxLines = 1)
            }
            if (enc.casilla != null) {
                BasicText(
                    "Cambiar",
                    Modifier.padding(start = Espacio.xs).heightIn(min = 44.dp).clickable(role = Role.Button) { alCambiar(enc.casilla) }.padding(horizontal = Espacio.xs).wrapCentrado(),
                    style = estiloNota(Paleta.tinta, FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
        if (conCerrar) BotonRedondo(Lucide.cerrar, "Cerrar", alCerrar)
    }
}

/** El texto de un botón de texto, centrado en su alto: `heightIn` solo no lo centra */
private fun Modifier.wrapCentrado(): Modifier = this.then(Modifier.padding(vertical = 14.dp))

/** El nombre: Jost 300, dos líneas como mucho, encogiéndose de 28 a 24 (en la burbuja, de 22 a 20) antes de partirse de más */
@Composable
internal fun TituloFicha(enc: EncabezadoFicha, modifier: Modifier = Modifier, compacto: Boolean = false) {
    val estilo = if (compacto) estiloTitulo().copy(lineHeight = 26.sp) else estiloTitulo()
    val tamanos = if (compacto) TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 22.sp, stepSize = 1.sp) else TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 28.sp, stepSize = 1.sp)
    BasicText(enc.titulo, modifier, style = estilo, maxLines = 2, overflow = TextOverflow.Ellipsis, autoSize = tamanos)
}

/** «2 UC · Gestión» y, a la derecha, la píldora de estado */
@Composable
internal fun DatosFicha(enc: EncabezadoFicha, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(enc.datos, Modifier.weight(1f).padding(end = Espacio.xs), style = estiloNota(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        FundidoDeEstado(enc.estado)
    }
}

/** La cabecera entera de pie: rótulo con la X, nombre y datos con la píldora */
@Composable
internal fun CabeceraFicha(enc: EncabezadoFicha, alCerrar: () -> Unit, alCambiar: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = margenPantalla())) {
        RotuloFicha(enc, alCerrar, alCambiar)
        TituloFicha(enc, Modifier.padding(top = 2.dp))
        DatosFicha(enc, Modifier.padding(top = Espacio.xs + 2.dp))
    }
}

// ─────────────────────────────────── Píldora ──────────────────────────────────

/** La píldora cambia de palabra y de icono con un fundido: marcar la materia no la parpadea */
@Composable
private fun FundidoDeEstado(e: EstadoFicha) {
    AnimatedContent(
        targetState = e,
        transitionSpec = { (fadeIn(tween(FUNDIDO_MS)) togetherWith fadeOut(tween(FUNDIDO_MS))).using(null) },
        label = "pildora",
    ) { PildoraEstado(it) }
}

/**
 * La palabra de estado, la única vez que se dice con letras (en las filas
 * basta el icono): 24 dp, radio 6, el icono de 14 y el rótulo.
 */
@Composable
internal fun PildoraEstado(e: EstadoFicha) {
    val forma = RoundedCornerShape(6.dp)
    val (fondo, texto) = when (e) {
        EstadoFicha.DISPONIBLE -> Paleta.azul.copy(alpha = Velo.pastillaAzul) to Paleta.azul
        EstadoFicha.CURSANDO -> Color.Transparent to Paleta.tinta
        EstadoFicha.APROBADA -> Paleta.tinta.copy(alpha = Velo.pastilla) to Paleta.tinta
        EstadoFicha.BLOQUEADA, EstadoFicha.PROXIMA -> Paleta.tinta.copy(alpha = Velo.pastilla) to Paleta.suave
        EstadoFicha.POR_ELEGIR -> Color.Transparent to Paleta.suave
    }
    val contorno = when (e) {
        EstadoFicha.CURSANDO -> Modifier.border(1.5.dp, Paleta.tinta, forma)
        EstadoFicha.POR_ELEGIR -> Modifier.drawBehind {
            val g = 1.dp.toPx()
            drawRoundRect(
                Paleta.tinta.copy(alpha = 0.3f), Offset(g / 2, g / 2), androidx.compose.ui.geometry.Size(size.width - g, size.height - g),
                CornerRadius(6.dp.toPx()), style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
        else -> Modifier
    }
    Row(
        Modifier.height(24.dp).clip(forma).background(fondo).then(contorno).padding(horizontal = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xxs),
    ) {
        if (e == EstadoFicha.POR_ELEGIR) Canvas(Modifier.size(14.dp)) {
            val g = 1.5.dp.toPx()
            drawCircle(Paleta.tinta.copy(alpha = 0.45f), size.minDimension / 2 - g, style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))))
        } else IconoSituacion(situacionDeFicha(e), 14.dp)
        BasicText(e.palabra, style = estiloRotulo(texto), maxLines = 1)
    }
}

/** El icono de estado (sello, libro, candados, reloj) de `lado`: el mismo de la lista y del mapa */
@Composable
internal fun IconoSituacion(s: Situacion, lado: Dp) {
    Canvas(Modifier.size(lado)) { iconoFila(s, size.minDimension) }
}

// ─────────────────────────────────── Respuesta ──────────────────────────────────

/** Una nota con una fecha en medio de ella: «Si la atrasas, te gradúas en **Jun 2030**»; en rojo si la fecha es mala noticia */
private class NotaRespuesta(val antes: String, val fecha: String? = null, val alerta: Boolean = false)

/** Lo que responde la fila: su pastilla, la frase y la nota; `para` dice a qué estado de la materia pertenece */
private class Respuesta(val para: Situacion, val icono: List<Path>, val azul: Boolean, val titulo: String, val nota: NotaRespuesta?)


/**
 * Lo que cuesta la fila: hasta 8 simulaciones la primera vez, y una más por cada materia. Va fuera del hilo
 * principal. Null si la materia no está en tu plan (una electiva que no cuenta) o ya está aprobada.
 */
private fun respuestaDe(codigo: String, s: Situacion, estado: EstadoCarrera): Respuesta? {
    val sim = estado.simulador
    val g = sim.base
    return when (s) {
        Situacion.HECHA -> null
        Situacion.CURSANDO -> {
            val fecha = fechaSiRepruebas(sim.pensum, sim.marcas, sim.casillas, g.carga, sim.hoy, codigo)
            val cambia = fecha != g.periodoFinal
            Respuesta(s, IconosHoja.reiniciar, false, "Si la repruebas", fecha?.let { NotaRespuesta(if (cambia) "Te gradúas en " else "Sigues en ", it.mesYAnio(), cambia) })
        }
        Situacion.INSCRIBIBLE -> {
            if (g.plan.none { codigo in it }) return null
            val margen = sim.margen(codigo)
            if (margen == 0) {
                val tarde: Periodo? = sim.conVeto(codigo, sim.indiceEnPlan(codigo) + 1).periodoFinal
                Respuesta(s, IconosHoja.eslabon, g.cuelloDeBotella.firstOrNull() == codigo, "No puede esperar", tarde?.let { NotaRespuesta("Si la atrasas, te gradúas en ", it.mesYAnio(), true) })
            } else Respuesta(s, IconosHoja.relojArena, false, puedeEsperar(margen), NotaRespuesta("Sin mover tu fecha"))
        }
        Situacion.PROXIMA, Situacion.LEJANA -> {
            val i = g.plan.indexOfFirst { codigo in it }
            if (i < 0) return null
            val margen = sim.margen(codigo)
            Respuesta(s, if (codigo in g.cuelloDeBotella) IconosHoja.eslabon else IconosHoja.calendario, false, "En tu plan, ${sim.hoy.mas(i + 1).rango()}", NotaRespuesta(if (margen == 0) "No puede esperar" else puedeEsperar(margen)))
        }
    }
}

private fun NotaRespuesta.texto(): AnnotatedString = buildAnnotatedString {
    append(antes)
    if (fecha != null) withStyle(SpanStyle(color = if (alerta) Paleta.rojo else Paleta.tinta, fontWeight = FontWeight.Medium)) { append(fecha) }
}

/**
 * «¿Puede esperar?»: una sola fila, la que importa para tu fecha. La tarjeta
 * reserva sus 64 dp desde el primer cuadro y el texto entra con un fundido
 * cuando llega el cálculo; una aprobada no tiene fila (no hay nada que decidir).
 */
@Composable
internal fun RespuestaFicha(codigo: String, estado: EstadoCarrera, modifier: Modifier = Modifier) {
    val situacion = estado.situaciones[codigo] ?: return
    if (situacion == Situacion.HECHA) return
    val marcas = estado.marcas
    val calculada by produceState<Respuesta?>(null, codigo, situacion, marcas, estado.casillas, estado.carga, estado.hoy) {
        value = withContext(Dispatchers.Default) { respuestaDe(codigo, situacion, estado) }
    }
    val lista = calculada?.takeIf { it.para == situacion }
    // Mientras llega la siguiente respuesta se sigue pintando la anterior, que se apaga: sin parpadeo ni salto
    var mostrada by remember { mutableStateOf<Respuesta?>(null) }
    if (lista != null) mostrada = lista
    val opacidad = animateFloatAsState(if (lista != null) 1f else 0f, tween(FUNDIDO_MS), label = "respuesta")
    TarjetaLista(modifier.heightIn(min = ALTO_RESPUESTA)) {
        mostrada?.let { r ->
            Box(Modifier.graphicsLayer { alpha = opacidad.value }) {
                FilaLista(
                    titulo = r.titulo,
                    nota = r.nota?.texto(),
                    primera = true,
                    icono = { Pastilla(r.icono, r.azul) },
                )
            }
        }
    }
}

// ─────────────────────────────────── Pide · Abre ──────────────────────────────────

private val ANCHO_ROTULO = 36.dp

/**
 * Lo que pide y lo que abre, cada una con su estado y un toque que lleva a
 * ella. El rótulo va solo en la primera fila de cada grupo, y un hilo parte
 * los dos grupos.
 */
@Composable
internal fun PideAbre(codigo: String, estado: EstadoCarrera, alIrA: (String) -> Unit, modifier: Modifier = Modifier) {
    val relaciones = estado.layout.relaciones
    val porCodigo = estado.layout.porCodigo
    val pide = relaciones.atras[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val abre = relaciones.adelante[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val especial = porCodigo[codigo]?.asignatura?.requisitoEspecial
    TarjetaLista(modifier) {
        GrupoPideAbre("PIDE", pide, vacio = especial == null, estado, alIrA)
        // Una condición del pensum que no es una materia: no se toca, solo se dice
        if (especial != null) {
            BasicText("Además: $especial", Modifier.padding(start = Espacio.m + ANCHO_ROTULO + Espacio.xs, end = Espacio.m, bottom = Espacio.xs), style = estiloNota(Paleta.suave))
        }
        Box(Modifier.padding(start = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = Velo.hilo)))
        GrupoPideAbre("ABRE", abre, vacio = true, estado, alIrA)
    }
}

@Composable
private fun GrupoPideAbre(rotulo: String, materias: List<Asignatura>, vacio: Boolean, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    if (materias.isEmpty()) {
        if (vacio) FilaPideAbre(rotulo, null, estado, alIrA)
        return
    }
    materias.forEachIndexed { k, m -> FilaPideAbre(if (k == 0) rotulo else null, m, estado, alIrA) }
}

/** Una fila de 48: rótulo (36), icono de estado (16), nombre a dos líneas y chevrón; sin materia dice «Ninguna» */
@Composable
private fun FilaPideAbre(rotulo: String?, materia: Asignatura?, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val puedeIr = materia != null && estado.nodoDe(materia.codigo) != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (puedeIr) Modifier.pulsable({ alIrA(materia!!.codigo) }, escala = 0.985f) else Modifier)
            .padding(horizontal = Espacio.m, vertical = Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = ANCHO_ROTULO)) { if (rotulo != null) BasicText(rotulo, style = estiloRotulo(), maxLines = 1, softWrap = false) }
        if (materia == null) {
            Spacer(Modifier.width(Espacio.xs + 16.dp))
            BasicText("Ninguna", style = estiloFila(Paleta.tenue, FontWeight.Normal))
            return@Row
        }
        val s = estado.situaciones[materia.codigo] ?: Situacion.LEJANA
        Spacer(Modifier.width(Espacio.xs))
        IconoSituacion(s, 16.dp)
        BasicText(
            materia.nombre, Modifier.weight(1f).padding(horizontal = Espacio.xs),
            style = estiloFila(if (s == Situacion.HECHA) Paleta.suave else Paleta.tinta), maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (puedeIr) Chevron()
    }
}

/**
 * Pide · Abre en una línea cada uno, para la burbuja: la primera materia y «y N más».
 * Tocar la fila lleva a esa primera; sin materias dice «Ninguna».
 */
@Composable
internal fun PideAbreResumen(codigo: String, estado: EstadoCarrera, alIrA: (String) -> Unit, modifier: Modifier = Modifier) {
    val relaciones = estado.layout.relaciones
    val porCodigo = estado.layout.porCodigo
    val pide = relaciones.atras[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    val abre = relaciones.adelante[codigo].orEmpty().mapNotNull { porCodigo[it]?.asignatura }
    TarjetaLista(modifier) {
        FilaResumen("PIDE", pide, estado, alIrA)
        Box(Modifier.padding(start = Espacio.m).fillMaxWidth().height(1.dp).background(Paleta.tinta.copy(alpha = Velo.hilo)))
        FilaResumen("ABRE", abre, estado, alIrA)
    }
}

/** Una fila de 44: rótulo (36), icono de estado (16), el nombre a una línea con «y N más» y el chevrón */
@Composable
private fun FilaResumen(rotulo: String, materias: List<Asignatura>, estado: EstadoCarrera, alIrA: (String) -> Unit) {
    val primera = materias.firstOrNull()
    val puedeIr = primera != null && estado.nodoDe(primera.codigo) != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .then(if (puedeIr) Modifier.pulsable({ alIrA(primera!!.codigo) }, escala = 0.985f) else Modifier)
            .padding(horizontal = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = ANCHO_ROTULO)) { BasicText(rotulo, style = estiloRotulo(), maxLines = 1, softWrap = false) }
        if (primera == null) {
            BasicText("Ninguna", Modifier.padding(start = Espacio.xs + 16.dp), style = estiloFila(Paleta.tenue, FontWeight.Normal))
            return@Row
        }
        val s = estado.situaciones[primera.codigo] ?: Situacion.LEJANA
        Spacer(Modifier.width(Espacio.xs))
        IconoSituacion(s, 16.dp)
        BasicText(
            primera.nombre, Modifier.weight(1f, fill = false).padding(start = Espacio.xs),
            style = estiloFila(if (s == Situacion.HECHA) Paleta.suave else Paleta.tinta), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (materias.size > 1) BasicText(" y ${materias.size - 1} más", style = estiloNota(), maxLines = 1, softWrap = false)
        Spacer(Modifier.weight(1f))
        if (puedeIr) Chevron()
    }
}

// ─────────────────────────────────── Botones ──────────────────────────────────

/** Qué botones lleva cada estado: el lleno es siempre el siguiente paso */
private enum class Botones { DISPONIBLE, CURSANDO, BLOQUEADA, APROBADA }

private fun botonesDe(marca: Marca?, bloqueada: Boolean) = when {
    marca == Marca.APROBADA -> Botones.APROBADA
    marca == Marca.CURSANDO -> Botones.CURSANDO
    bloqueada -> Botones.BLOQUEADA
    else -> Botones.DISPONIBLE
}

/**
 * Los botones de marcar. Con 320 dp o letra grande se apilan; si no, van en
 * dos columnas. «Se te abre N» solo sale si aprobarla abre algo. Cada marca
 * lleva su háptico, y los textos se funden en 150 ms al cambiar de estado.
 */
@Composable
internal fun BotonesMarca(codigo: String, estado: EstadoCarrera, alMarcar: (String, Marca?) -> Unit, modifier: Modifier = Modifier, compacto: Boolean = false) {
    val haptica = LocalHapticFeedback.current
    val marca = estado.marcas[codigo]
    val bloqueada = estado.situaciones[codigo].let { it == Situacion.PROXIMA || it == Situacion.LEJANA }
    val abre = remember(estado.marcas, codigo) { seAbrenAlAprobar(estado.pensum, estado.marcas, listOf(codigo)).size }
    val apilados = !compacto && ventana().let { it.compacta || it.letraGrande }
    val alto = if (compacto) 48.dp else 52.dp
    val radio = if (compacto) 16.dp else Radio.Barra
    fun marcar(m: Marca?) {
        haptica.performHapticFeedback(HapticFeedbackType.SegmentTick)
        alMarcar(codigo, m)
    }
    val nota = when (abre) {
        0 -> null
        1 -> "Se te abre 1"
        else -> "Se te abren $abre"
    }
    AnimatedContent(
        targetState = botonesDe(marca, bloqueada),
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { (fadeIn(tween(FUNDIDO_MS)) togetherWith fadeOut(tween(FUNDIDO_MS))).using(null) },
        label = "botones",
    ) { modo ->
        val cursando = @Composable { m: Modifier, lleno: Boolean -> BotonTinta("La estoy cursando", { marcar(Marca.CURSANDO) }, m, superficie = !lleno, alto = alto, radio = radio) }
        val aprobada = @Composable { m: Modifier, lleno: Boolean ->
            // Sobre el botón lleno la nota sigue la tinta del panel; en uno de superficie, azul: es lo que se te abre
            BotonTinta("Ya la aprobé", { marcar(Marca.APROBADA) }, m, nota = nota, colorNota = if (lleno) null else Paleta.azul, superficie = !lleno, alto = alto, radio = radio)
        }
        val desmarcar = @Composable { m: Modifier -> BotonTinta("Desmarcar", { marcar(null) }, m, superficie = true, alto = alto, radio = radio) }
        val dos = modo != Botones.APROBADA
        val primero: @Composable (Modifier) -> Unit = { m ->
            when (modo) {
                Botones.DISPONIBLE -> cursando(m, true)
                Botones.CURSANDO -> aprobada(m, true)
                Botones.BLOQUEADA -> cursando(m, false)
                Botones.APROBADA -> desmarcar(m)
            }
        }
        val segundo: @Composable (Modifier) -> Unit = { m ->
            when (modo) {
                Botones.DISPONIBLE, Botones.BLOQUEADA -> aprobada(m, false)
                else -> desmarcar(m)
            }
        }
        if (apilados) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                primero(Modifier)
                if (dos) segundo(Modifier)
            }
        } else if (dos) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                primero(Modifier.weight(1f))
                segundo(Modifier.weight(1f))
            }
        } else primero(Modifier)
    }
}
