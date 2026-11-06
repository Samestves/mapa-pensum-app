package mpensum.mapa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mpensum.carrera.EstadoCarrera
import mpensum.logica.Situacion
import mpensum.logica.Tramo
import mpensum.logica.marcasDeSemestres
import mpensum.logica.tramoDe
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.avivar
import mpensum.tema.porTema
import kotlin.math.max
import kotlin.math.min

/*
 * El mapa de una carrera, plano a plano como en la web (GrafoPensum.jsx y
 * PlanosGrafo.jsx): la rejilla quieta detrás, y encima una capa que mueve la
 * cámara con los cables, la luz de la frontera, las cabeceras y las
 * tarjetas. Mover el mapa es mover esa capa: no recompone ni repinta nada.
 */

/** La opacidad de lo que queda fuera del foco en el teléfono (--opacidad-foco) */
private const val OPACIDAD_FOCO = 0.14f
private val CURVA_FOCO = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val LLEGADA = CubicBezierEasing(0.22f, 0.61f, 0.2f, 1f)
private val VIAJE_LUZ = CubicBezierEasing(0.45f, 0f, 0.3f, 1f)
private val DESCARGA = CubicBezierEasing(0.55f, 0f, 0.25f, 1f)
/** La descarga espera a que el mapa se corra para enseñar lo que se abre, y luego recorre el cable */
private const val ESPERA_DESCARGA = 380
private const val DURACION_DESCARGA = 1300
/** La rejilla del fondo: cuadros de 34 en píxeles de pantalla, quietos (DefsGrafo.jsx) */
private const val PASO_REJILLA = 34f

@Composable
fun MapaCarrera(
    estado: EstadoCarrera,
    camara: Camara,
    efectos: EfectosMapa,
    seleccionado: String?,
    congelado: Boolean,
    alTocarMateria: (String) -> Unit,
    alTocarVacio: () -> Unit,
    alAbrirCasilla: (String) -> Unit,
    alAlternarSemestre: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Sin fondo ni rejilla, el mapa flota sobre lo que haya detrás (la guía de bienvenida) */
    conFondo: Boolean = true,
    /** En horizontal el mapa pasa por detrás del riel: lo que ocupa a la izquierda (dp) se descuenta en el encuadre inicial; null en vertical */
    reservaIzquierda: Float? = null,
) {
    val layout = estado.layout
    val densidad = LocalDensity.current
    val arriba = with(densidad) { WindowInsets.statusBars.getTop(this).toDp().value }
    val abajo = with(densidad) { WindowInsets.navigationBars.getBottom(this).toDp().value }
    val tipos = rememberTiposMapa()
    val medidor = rememberTextMeasurer()

    // El foco: lo que mira la ficha abierta o la ruta fijada; lo demás se aparta
    val mirada = seleccionado ?: efectos.ruta
    val cadena = remember(mirada, layout) { mirada?.let { cadenaDe(it, layout.relaciones) } }
    var cadenaVisible by remember { mutableStateOf<Set<String>?>(null) }
    val foco = remember { Animatable(0f) }
    val trazado = remember { Animatable(1f) }
    LaunchedEffect(cadena) {
        if (cadena != null) {
            cadenaVisible = cadena
            foco.animateTo(1f, tween(320, easing = CURVA_FOCO))
        } else {
            foco.animateTo(0f, tween(320, easing = CURVA_FOCO))
            cadenaVisible = null
        }
    }
    LaunchedEffect(mirada) {
        if (mirada == null) return@LaunchedEffect
        trazado.snapTo(0f)
        trazado.animateTo(1f, tween(560, easing = CURVA_FOCO))
    }
    val descarga = remember { Animatable(1f) }
    LaunchedEffect(efectos.descarga?.n) {
        if (efectos.descarga == null) return@LaunchedEffect
        descarga.snapTo(0f)
        descarga.animateTo(1f, tween(ESPERA_DESCARGA + DURACION_DESCARGA, easing = LinearEasing))
    }
    LaunchedEffect(efectos.carga) {
        val c = efectos.carga ?: return@LaunchedEffect
        if (!c.hecha) return@LaunchedEffect
        delay(APAGADO_MS.toLong())
        efectos.apagarCarga(c.n)
    }
    val llegada = remember { Animatable(0f) }
    LaunchedEffect(camara.colocada) {
        if (camara.colocada) llegada.animateTo(1f, tween(620, easing = LLEGADA))
    }

    val opacidadDe = { codigo: String? ->
        {
            val visible = cadenaVisible
            if (visible == null || (codigo != null && codigo in visible)) 1f else 1f - (1f - OPACIDAD_FOCO) * foco.value
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .then(if (conFondo) Modifier.background(Paleta.lienzo) else Modifier)
            .clipToBounds()
            .onSizeChanged { t ->
                val d = densidad.density
                camara.medir(t.width / d, t.height / d, layout.ancho.toFloat(), estado.alto.toFloat())
                // Abre en tu semestre a escala de lectura, o donde lo dejaste en esta sesión
                camara.colocarInicial {
                    val columna = semestreFrente(estado)?.let { s -> layout.columnas.firstOrNull { it.semestre == s } } ?: layout.columnas.first()
                    if (reservaIzquierda != null) vistaDeAltura(columna.x.toFloat(), layout.finSemestres.toFloat(), camara.alto, arriba, abajo, reservaIzquierda)
                    else vistaDeColumna(columna.x.toFloat(), camara.ancho, arriba)
                }
            }
            .gestosDelMapa(camara, alTocarVacio),
    ) {
        if (conFondo) Canvas(Modifier.fillMaxSize()) { rejilla() }
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(layout.ancho.toFloat().dp, estado.alto.toFloat().dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = camara.x * density
                    translationY = (camara.y + 12f * (1f - llegada.value)) * density
                    scaleX = camara.escala
                    scaleY = camara.escala
                    alpha = llegada.value
                },
        ) {
            Cables(estado, cadena = { cadenaVisible }, foco = { foco.value }, trazado = { trazado.value }, descarga = { efectos.descarga?.codigo to descarga.value })
            if (!Tema.movimiento) LuzDeFrontera(estado, congelada = { camara.enGesto || congelado }, cadena = { cadenaVisible }, foco = { foco.value })

            CabecerasSemestres(estado, tipos, medidor, alAlternarSemestre)
            estado.franja?.let { RotulosFranja(it, estado, tipos) }

            TarjetasMapa(estado, camara, efectos, tipos, cadena, seleccionado, opacidadDe, alTocarMateria, alAbrirCasilla)
        }
    }
}

/** La cabecera de cada semestre: su número, lo que cuenta y el aro que lo aprueba entero */
@Composable
private fun CabecerasSemestres(estado: EstadoCarrera, tipos: TiposMapa, medidor: TextMeasurer, alAlternarSemestre: (Int) -> Unit) {
    val layout = estado.layout
    val sit = estado.situaciones
    val cuentas = marcasDeSemestres(layout.nodos.map { it.asignatura }, estado::enCasilla, estado.estados)
    layout.columnas.forEach { columna ->
        val delSemestre = layout.nodos.filter { it.semestre == columna.semestre }
        val materias = delSemestre.map { estado.materiaDe(it) }
        val situaciones = materias.map { m -> m?.let { sit[it.codigo] } ?: Situacion.LEJANA }
        CabeceraSemestre(
            DatosCabecera(
                semestre = columna.semestre,
                x = columna.x.toFloat(),
                uc = materias.sumOf { it?.uc ?: 0 },
                total = delSemestre.size,
                hechas = situaciones.count { it == Situacion.HECHA },
                cursando = situaciones.count { it == Situacion.CURSANDO },
                inscribibles = situaciones.count { it == Situacion.INSCRIBIBLE },
                casilla = cuentas[columna.semestre],
            ),
            tipos, medidor,
            compacta = layout.altoEncabezado < ALTO_ENCABEZADO,
            alAlternar = { alAlternarSemestre(columna.semestre) },
        )
    }
}

/** Las tarjetas de las materias y las casillas, con el foco de la cadena y las animaciones de toque, descarga y carga */
@Composable
private fun TarjetasMapa(
    estado: EstadoCarrera,
    camara: Camara,
    efectos: EfectosMapa,
    tipos: TiposMapa,
    cadena: Set<String>?,
    seleccionado: String?,
    opacidadDe: (String?) -> () -> Float,
    alTocarMateria: (String) -> Unit,
    alAbrirCasilla: (String) -> Unit,
) {
    val layout = estado.layout
    val sit = estado.situaciones
    val haptica = LocalHapticFeedback.current
    val descargado = efectos.descarga
    estado.nodos.forEach { nodo ->
        val materia = estado.materiaDe(nodo)
        val codigo = materia?.codigo
        val situacion = codigo?.let { sit[it] }
        TarjetaMapa(
            nodo = nodo,
            materia = materia,
            lineas = codigo?.let { layout.porCodigo[it]?.lineasNombre } ?: nodo.lineasNombre,
            situacion = situacion,
            acento = materia?.let(estado.colores::de) ?: Paleta.suave,
            seleccionada = codigo != null && codigo == seleccionado,
            resaltada = codigo != null && cadena?.contains(codigo) == true,
            opacidad = opacidadDe(codigo),
            animaciones = AnimacionesTarjeta(
                toque = efectos.toque?.takeIf { it.codigo == codigo }?.n,
                destello = descargado?.takeIf { situacion == Situacion.INSCRIBIBLE && materia?.prerrequisitos?.contains(it.codigo) == true }?.n,
                carga = efectos.carga?.takeIf { it.codigo == codigo },
            ),
            escala = { camara.escala },
            tipos = tipos,
            camara = camara,
            alTocar = { if (codigo != null) alTocarMateria(codigo) else alAbrirCasilla(nodo.codigo) },
            alEmpezarCarga = { if (codigo != null) efectos.empezarCarga(codigo) },
            alSoltarCarga = { efectos.soltarCarga() },
            alFijar = {
                if (codigo != null) {
                    efectos.fijarRuta(codigo)
                    haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
        )
    }
}

/** El semestre por el que vas: lo que puedes inscribir o cursas; si no, lo primero pendiente (layout/camara.js) */
private fun semestreFrente(estado: EstadoCarrera): Int? {
    val reales = estado.layout.nodos.filter { !it.asignatura.esHueco }
    if (reales.isEmpty()) return null
    val sit = estado.situaciones
    reales.filter { sit[it.codigo] == Situacion.INSCRIBIBLE || sit[it.codigo] == Situacion.CURSANDO }.mapNotNull { it.semestre }.minOrNull()?.let { return it }
    reales.filter { sit[it.codigo] != Situacion.HECHA }.mapNotNull { it.semestre }.minOrNull()?.let { return it }
    return reales.mapNotNull { it.semestre }.maxOrNull()
}

private fun DrawScope.rejilla() {
    val paso = PASO_REJILLA * density
    val color = Paleta.rejilla
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
        x += paso
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
        y += paso
    }
}

/* La fuerza de cada cable según lo que dice (Arista.jsx) */
private fun opacidad(t: Tramo): Float {
    val base = when (t) {
        Tramo.FRONTERA -> 0.55f
        Tramo.RECORRIDO -> 0.5f
        Tramo.PROXIMA -> 0.32f
        Tramo.LEJANA -> porTema(0.12f, 0.2f)
    }
    // En Exterior los cables se firman más: a pleno sol los finos se pierden
    return if (Tema.fuerte) (base * 1.5f).coerceAtMost(0.9f) else base
}

private fun grosor(t: Tramo) = when (t) {
    Tramo.FRONTERA -> 1.75f
    Tramo.RECORRIDO -> 1.5f
    Tramo.PROXIMA -> 1.25f
    Tramo.LEJANA -> 1f
}

/** Un cable ya convertido a Path, medido, con sus extremos y sus colores */
private class CableDibujo(val arista: Arista, val ruta: Path, val desde: Color, val hacia: Color, val inicio: Offset, val fin: Offset) {
    private val medida = PathMeasure().apply { setPath(ruta, false) }
    val largo = medida.length
    private val parte = Path()

    /** El trozo del principio hasta la fracción `f` del largo. Reutiliza siempre el mismo Path */
    fun hasta(f: Float): Path {
        parte.reset()
        medida.getSegment(0f, largo * f.coerceIn(0f, 1f), parte, true)
        return parte
    }
}

private fun cablesDe(estado: EstadoCarrera): List<CableDibujo> {
    val porCodigo = estado.layout.porCodigo
    return estado.layout.aristas.map { a ->
        val ruta = Path().apply {
            for (s in a.trazo) when (s) {
                is Segmento.Mover -> moveTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Linea -> lineTo(s.x.toFloat(), s.y.toFloat())
                is Segmento.Curva -> cubicTo(s.x1.toFloat(), s.y1.toFloat(), s.x2.toFloat(), s.y2.toFloat(), s.x.toFloat(), s.y.toFloat())
            }
        }
        val inicio = a.trazo.first() as Segmento.Mover
        val fin = a.trazo.last() as Segmento.Curva
        CableDibujo(
            a, ruta,
            porCodigo[a.origen]?.asignatura?.let(estado.colores::de) ?: Paleta.suave,
            porCodigo[a.destino]?.asignatura?.let(estado.colores::de) ?: Paleta.suave,
            Offset(inicio.x.toFloat(), inicio.y.toFloat()), Offset(fin.x.toFloat(), fin.y.toFloat()),
        )
    }
}

/**
 * La luz de la frontera no es de un color: sale con el del área de la que
 * viene, pasa por ese mismo color girado medio tono y llega con el de la
 * materia que puedes inscribir (el oklch de la web, aquí en HSL).
 */
private fun girar(color: Color, grados: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    if (mx == mn) return color
    val l = (mx + mn) / 2
    val d = mx - mn
    val s = if (l > 0.5f) d / (2 - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } * 60f
    return Color.hsl((h + grados) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun degradado(c: CableDibujo, vivo: Boolean): Brush {
    val medio = girar(c.desde, 48f)
    val colores = if (vivo) listOf(avivar(c.desde), avivar(medio), avivar(c.hacia)) else listOf(c.desde, medio, c.hacia)
    return Brush.linearGradient(colores, c.inicio, c.fin)
}

/**
 * Los cables: la base de cada uno con la fuerza de lo que dice, la cadena
 * de la materia que se mira dibujándose de origen a destino, y la descarga
 * verde de la que se acaba de aprobar. Las rutas se arman una vez; en cada
 * cuadro solo cambian opacidades y trozos.
 */
@Composable
private fun Cables(
    estado: EstadoCarrera,
    cadena: () -> Set<String>?,
    foco: () -> Float,
    trazado: () -> Float,
    descarga: () -> Pair<String?, Float>,
) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val cables = cablesDe(estado)
            onDrawBehind {
                scale(density, Offset.Zero) {
                    val sit = estado.situaciones
                    val enFoco = cadena()
                    val f = foco()
                    val (aprobada, tiempo) = descarga()
                    for (c in cables) {
                        val a = c.arista
                        val tramo = tramoDe(sit[a.origen] ?: Situacion.LEJANA, sit[a.destino] ?: Situacion.LEJANA)
                        val frontera = tramo == Tramo.FRONTERA
                        val enCadena = enFoco != null && a.origen in enFoco && a.destino in enFoco
                        val apartado = if (enFoco != null && !enCadena) 1f - (1f - OPACIDAD_FOCO) * f else 1f
                        // Dentro del foco la base baja a 0,2 y la cadena se dibuja encima, en su color
                        val alfa = if (enCadena) opacidad(tramo) + (0.2f - opacidad(tramo)) * f else opacidad(tramo) * apartado
                        val estilo = Stroke(grosor(tramo), cap = StrokeCap.Round)
                        if (frontera) drawPath(c.ruta, degradado(c, false), alpha = alfa, style = estilo)
                        else drawPath(c.ruta, c.desde.copy(alpha = alfa), style = estilo)
                        if (enCadena && f > 0f) {
                            val trozo = c.hasta(trazado())
                            val linea = Stroke(2.25f, cap = StrokeCap.Round)
                            if (frontera) drawPath(trozo, degradado(c, false), alpha = 0.95f * f, style = linea)
                            else drawPath(trozo, c.desde.copy(alpha = 0.95f * f), style = linea)
                        }
                        // El punto de llegada: el de la frontera siempre, el de la cadena con el foco puesto
                        if (frontera) drawCircle(c.hacia.copy(alpha = (if (enCadena) 1f else 0.9f) * apartado), 3f, c.fin)
                        else if (enCadena && f > 0f) drawCircle(c.desde.copy(alpha = f), 3f, c.fin)
                        if (aprobada != null && a.origen == aprobada && tiempo < 1f) descargaPor(c, tiempo)
                    }
                }
            }
        },
    )
}

/**
 * La luz al aprobar: el cable entero se enciende en verde de origen a
 * destino, se queda encendido un instante y se apaga. Con un halo ancho y
 * flojo debajo que hace de resplandor sin filtro.
 */
private fun DrawScope.descargaPor(c: CableDibujo, tiempo: Float) {
    val total = (ESPERA_DESCARGA + DURACION_DESCARGA).toFloat()
    val t = ((tiempo * total) - ESPERA_DESCARGA) / DURACION_DESCARGA
    if (t <= 0f) return
    val p = DESCARGA.transform(t.coerceIn(0f, 1f))
    val lleno = min(1f, p / 0.58f)
    val alfa = if (p < 0.58f) 1f else 1f - (p - 0.58f) / 0.42f
    val trozo = c.hasta(lleno)
    drawPath(trozo, Paleta.verde.copy(alpha = 0.18f * alfa), style = Stroke(9f, cap = StrokeCap.Round))
    drawPath(trozo, Paleta.verde.copy(alpha = alfa), style = Stroke(2.5f, cap = StrokeCap.Round))
}

/**
 * La luz que viaja por la frontera hacia lo que puedes inscribir: un guion
 * del 9 % del cable que nace y muere apagado. Cada luz a su ritmo, entre 2,6
 * y 3,2 s, y nacida a media marcha. Va en su propia capa, y se congela
 * mientras el mapa se mueve o hay una hoja encima: en un teléfono modesto
 * la luz corriendo durante un gesto le quita cuadros.
 */
@Composable
private fun LuzDeFrontera(estado: EstadoCarrera, congelada: () -> Boolean, cadena: () -> Set<String>?, foco: () -> Float) {
    val reloj by rememberInfiniteTransition(label = "luz").animateFloat(
        0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "segundos",
    )
    val congeladaEn = remember { floatArrayOf(0f) }
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val cables = cablesDe(estado)
            onDrawBehind {
                val sit = estado.situaciones
                val enFoco = cadena()
                val f = foco()
                val fronteras = cables.withIndex().filter { (_, c) ->
                    tramoDe(sit[c.arista.origen] ?: Situacion.LEJANA, sit[c.arista.destino] ?: Situacion.LEJANA) == Tramo.FRONTERA
                }
                if (fronteras.isEmpty()) return@onDrawBehind
                // Congelada, sin leer el reloj: la capa no se invalida y la luz se queda donde estaba
                val segundos = if (congelada()) congeladaEn[0] else reloj.also { congeladaEn[0] = it }
                scale(density, Offset.Zero) {
                    for ((i, c) in fronteras) {
                        val enCadena = enFoco != null && c.arista.origen in enFoco && c.arista.destino in enFoco
                        val apagado = if (enFoco != null && !enCadena) 1f - f else 1f
                        if (apagado <= 0f) continue
                        val duracion = 2.6f + ((i * 5) % 7) * 0.1f
                        val retraso = ((i * 7) % 11) * 0.26f
                        val bruto = ((segundos + retraso) / duracion) % 1f
                        val p = VIAJE_LUZ.transform(bruto)
                        val alfa = when {
                            bruto < 0.14f -> bruto / 0.14f
                            bruto > 0.82f -> (1f - bruto) / 0.18f
                            else -> 1f
                        } * apagado
                        val guion = c.largo * 0.09f
                        // El guion entra por el origen y sale por el destino (dashoffset de 100 a 0)
                        val fase = (c.largo * (1f - p)) % c.largo
                        drawPath(
                            c.ruta, degradado(c, true), alpha = alfa.coerceIn(0f, 1f),
                            style = Stroke(2.25f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(guion, c.largo - guion), fase)),
                        )
                    }
                }
            }
        },
    )
}

/** El rótulo de cada grupo en la franja de electivas, y la raya que la separa de los semestres */
@Composable
private fun RotulosFranja(franja: FranjaColocada, estado: EstadoCarrera, tipos: TiposMapa) {
    val primera = franja.filas.firstOrNull() ?: return
    Spacer(
        Modifier
            .offset(Margen.IZQUIERDA.toFloat().dp, (primera.y - Franja.CORREDOR / 2).toFloat().dp)
            .size((estado.layout.ancho - Margen.IZQUIERDA - Margen.DERECHA).toFloat().dp, 1.dp)
            .background(Paleta.tinta.copy(alpha = 0.08f)),
    )
    franja.filas.forEach { f ->
        val datos = listOfNotNull(
            if (f.elegidas > 0) "${f.elegidas} EN TU MAPA" else "ELIGE LAS TUYAS",
            "${f.opciones} ${if (f.opciones == 1) "OPCIÓN" else "OPCIONES"}",
            f.cuota?.let { "ELIGE $it UC" },
        )
        Column(Modifier.offset(f.x.toFloat().dp, (f.y + 14 - 11.5 * 0.93).toFloat().dp)) {
            BasicText(f.titulo.uppercase(), style = tipos.titulo)
            BasicText(datos.joinToString(" · "), Modifier.offset(y = 6.dp), style = tipos.datos)
        }
    }
}
