package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mpensum.carrera.Velo
import mpensum.componentes.estiloCifra
import mpensum.datos.Asignatura
import mpensum.datos.Pensum
import mpensum.datos.rememberPensum
import mpensum.logica.EstadoCapsula
import mpensum.logica.Marca
import mpensum.logica.Periodo
import mpensum.logica.SemestreCapsulas
import mpensum.logica.capsulasDe
import mpensum.logica.estimarGraduacion
import mpensum.logica.resumenParaPortada
import mpensum.logica.semestreActual
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.inter
import mpensum.tema.interCifra
import mpensum.tema.jost
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Pantalla 3 (v2, docs/bienvenida-animaciones-v2.md): cuánto llevas y
 * cuándo te gradúas, contado con la tarjeta de arriba de la hoja «Tu avance»
 * tal como es hoy, para un estudiante de Sistemas que va por el quinto
 * semestre. Nace la barra de cápsulas (una por materia, ancha según sus UC)
 * con su regla de semestres; lo aprobado se llena de tinta en una ola de
 * izquierda a derecha mientras el 41 % y las UC cuentan de verdad y el
 * círculo de la regla avanza semestre a semestre hasta el que cursas. Lo que
 * cursas queda en contorno; luego una luz recorre lo que falta y la fecha de
 * la esquina pasa período a período hasta asentarse en el día del grado.
 * Queda vivo el contorno de lo que cursas, respirando.
 *
 * Todo sale de un solo reloj y se pinta en un Canvas, con los textos medidos
 * una vez: cada cuadro solo redibuja. Los datos salen del pensum con la
 * lógica de la app (la barra, la portada y la estimación del grado).
 */

private const val CARRERA = "ingenieria-de-sistemas"
/** El estudiante de la demostración: aprobó los cuatro primeros semestres y cursa dos materias del quinto */
private const val SEMESTRES_APROBADOS = 4
private const val MATERIAS_CURSANDO = 2
/** Lo que dice el título de la carrera de la demostración */
private const val CARRERA_TITULO = "Ingeniería de Sistemas"

/**
 * La historia, en ms del ciclo. La comparten los tres conceptos (A, B y C):
 * nacen las piezas, lo aprobado se llena mientras la cifra cuenta, lo que
 * cursas se marca, lo que falta se proyecta con la fecha corriendo y se asienta.
 */
internal object RitmoAvance {
    const val CICLO = 10_000
    const val NACE = 150f
    const val NACE_DURA = 650f
    const val CIFRA = 900f
    const val CUENTA = 1_100f
    const val DURA_CUENTA = 2_300f
    const val LLENA = 280f
    const val CURSANDO = 3_650f
    const val PROYECTA = 4_300f
    const val LLEGA = 7_000f
    const val SE_VA = 9_300f
    /** Con las animaciones apagadas o «Reducir movimiento»: todo puesto, con la fecha ya asentada */
    const val QUIETO = 8_400f
    /** La respiración de lo que cursas: el ritmo del punto «En curso» del horario */
    const val RESPIRA = 2_600f
}

private const val CICLO = RitmoAvance.CICLO
private const val NACE = RitmoAvance.NACE
private const val NACE_DURA = RitmoAvance.NACE_DURA
private const val CIFRA = RitmoAvance.CIFRA
private const val CUENTA = RitmoAvance.CUENTA
private const val DURA_CUENTA = RitmoAvance.DURA_CUENTA
private const val LLENA = RitmoAvance.LLENA
private const val CURSANDO = RitmoAvance.CURSANDO
private const val PROYECTA = RitmoAvance.PROYECTA
private const val LLEGA = RitmoAvance.LLEGA
private const val SE_VA = RitmoAvance.SE_VA
private const val QUIETO = RitmoAvance.QUIETO
private const val RESPIRA = RitmoAvance.RESPIRA
/** Un semestre sin nada aprobado no termina nunca: un tiempo lejano y finito (un infinito daría NaN al restar) */
private const val NUNCA = 1e9f

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/*
 * El plano (390 × 468, en dp del tablero): la tarjeta de la hoja con sus
 * medidas (relleno 16, radio 24), centrada. Cifra de 64 con su tope a 16 del
 * borde, lo aprobado a 11 bajo ella, la fecha arriba a la derecha y la barra
 * a 20 de lo aprobado, con la regla 6 por debajo.
 */
private const val MX = 24f
private const val W = 342f
private const val IX = MX + 16f
private const val IW = W - 32f
private const val TARJETA_Y = 116f
private const val TARJETA_ALTO = 170f
private const val TOPE = TARJETA_Y + 16f
/** La línea base de la cifra: el tope más el alto de las mayúsculas de Inter (0,727 em) */
private const val BASE_CIFRA = TOPE + 64f * 0.727f
private const val Y_UC = BASE_CIFRA + 11f
private const val BASE_FECHA = TOPE + 24f * 0.727f
private const val Y_TE_GRADUAS = BASE_FECHA + 8f
private const val BARRA_Y = Y_UC + 16f + 20f
private const val ALTO_CAPSULA = 14f
private const val Y_REGLA = BARRA_Y + ALTO_CAPSULA + 6f
private val RADIO_CAPSULA = CornerRadius(2.5f)
private val CONTORNO = Stroke(1.5f)

/*
 * La toma, de un solo travelling: empieza un poco más cerca y baja, sobre la
 * barra mientras nace y se llena (la tarjeta entera siempre dentro), se aleja
 * para ver la cifra y se va a la derecha con la luz hasta la fecha.
 *                      ms     x     y    zoom  incl  giro
 */
private val TOMA = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 195f, 222f, 1.12f, 10f, -1.0f),
        floatArrayOf(1_200f, 192f, 214f, 1.10f, 5f, -0.4f),
        floatArrayOf(3_400f, 185f, 205f, 1.06f, 2f, 0f),
        floatArrayOf(4_300f, 195f, 203f, 1.04f, 2f, 0.2f),
        floatArrayOf(7_000f, 210f, 201f, 1.08f, 3f, 0.4f),
        floatArrayOf(8_800f, 198f, 203f, 1.04f, 1f, 0f),
        floatArrayOf(CICLO.toFloat(), 195f, 222f, 1.12f, 10f, -1.0f),
    ),
)

/** En modo ligero no hay toma: la tarjeta de frente y en el centro */
private val CENTRO_FIJO = Offset(195f, 201f)

/** El hueco del tablero entre la escena y el título */
private val HUECO_TABLERO = 68.dp

/** Lo que cuenta la escena, sacado del pensum con la misma lógica que la hoja: nada escrito a mano */
internal class AvanceDemo(
    val semestres: List<SemestreCapsulas>,
    /** El número del semestre por el que vas, con la regla de la barra */
    val actual: Int?,
    val porcentaje: Int,
    val llevas: Int,
    val de: Int,
    val unidad: String,
    /** Los períodos de hoy al del grado, los dos incluidos; vacío si el plan no cierra */
    val periodos: List<Periodo>,
    /** Por materia, en el orden de la barra: si va en tu próximo semestre, lo que inscribes (el azul del anillo) */
    val proximo: BooleanArray,
)

internal fun avanceDeDemostracion(pensum: Pensum): AvanceDemo {
    val obligatorias = pensum.obligatorias
    val marcas = buildMap<String, Marca> {
        obligatorias.filter { (it.semestre ?: 99) <= SEMESTRES_APROBADOS }.forEach { put(it.codigo, Marca.APROBADA) }
        obligatorias.filter { it.semestre == SEMESTRES_APROBADOS + 1 }.take(MATERIAS_CURSANDO).forEach { put(it.codigo, Marca.CURSANDO) }
    }
    val resumen = resumenParaPortada(pensum, marcas.mapValues { it.value.valor })
    val semestres = capsulasDe(pensum, marcas) { null }
    val hoy = Periodo.actual()
    // La misma estimación de la hoja, a la carga de siempre: pura y barata, se calcula una vez
    val estimacion = estimarGraduacion(pensum, marcas, emptyMap(), hoy = hoy)
    val final = estimacion.periodoFinal
    val periodos = if (final == null || final < hoy) emptyList() else generateSequence(hoy) { it.mas(1) }.takeWhile { it <= final }.toList()
    // Los códigos en el orden de capsulasDe: por semestre y, dentro, por estado (aprobadas, cursando, pendientes), estable.
    // Así cada cápsula sabe si va en el próximo semestre
    val siguiente = estimacion.plan.firstOrNull().orEmpty().toSet()
    fun estadoDe(a: Asignatura) = when (if (a.esHueco) null else marcas[a.codigo]) {
        Marca.APROBADA -> EstadoCapsula.APROBADA
        Marca.CURSANDO -> EstadoCapsula.CURSANDO
        null -> EstadoCapsula.PENDIENTE
    }
    val codigos = pensum.asignaturas.filter { it.semestre != null }.groupBy { it.semestre!! }.toSortedMap().values
        .flatMap { nodos -> nodos.sortedBy { estadoDe(it).ordinal } }.map { it.codigo }
    val proximo = BooleanArray(codigos.size) { codigos[it] in siguiente }
    return AvanceDemo(semestres, semestreActual(semestres), resumen.porcentaje, resumen.llevas, resumen.de, resumen.unidad, periodos, proximo)
}

@Composable
fun PasoAvance(alSeguir: () -> Unit) {
    val pensum = rememberPensum(CARRERA)
    val avance = remember(pensum) { pensum?.let(::avanceDeDemostracion) }
    // Por encima de la bifurcación de pie o acostado; parado mientras llega el pensum, así la historia empieza en 0
    val reloj = rememberRelojEscena(CICLO, QUIETO, parado = Movimiento.reducido || avance == null)
    val textos = rememberTextos(avance)
    val geometria = remember(avance) { avance?.let(::GeometriaAvance) }
    // Los tres conceptos comparten datos, textos y ritmo; cada uno trae su toma y su dibujo
    val variante = remember { Variantes.avance }
    val firma = remember { FirmaDemo() }
    val ligero = Tema.ligero
    val respira = !Tema.sinBucles
    val (toma, centro) = when (variante) {
        VarianteAvance.TARJETA -> TOMA to CENTRO_FIJO
        VarianteAvance.ANILLO -> TOMA_ANILLO to CENTRO_ANILLO
        VarianteAvance.TITULO -> TOMA_TITULO to CENTRO_TITULO
    }
    PasoConEscena("Cuánto llevas,", "cuándo te gradúas.", "Continuar", alSeguir, hueco = HUECO_TABLERO) { modifier ->
        EscenaEnBucle(
            reloj, toma, arriba = 0.08f, abajo = 0.12f, lados = 0.04f, modifier = modifier,
            fija = if (ligero) centro else null,
        ) { t ->
            if (avance != null && textos != null && geometria != null) {
                when (variante) {
                    VarianteAvance.TARJETA -> pintar(t, avance, geometria, textos, respira)
                    VarianteAvance.ANILLO -> pintarAnillo(t, avance, geometria, textos, respira)
                    VarianteAvance.TITULO -> pintarTitulo(t, avance, geometria, textos, firma, respira)
                }
            }
        }
    }
}

/**
 * Lo que no cambia entre cuadros: dónde va cada cápsula (con el reparto de
 * BarraCapsulas: 2 dp entre materias, 7 entre semestres, el ancho por UC) y
 * cuándo nace, se llena o pasa a contorno.
 */
internal class GeometriaAvance(a: AvanceDemo) {
    val n = a.semestres.sumOf { it.capsulas.size }
    val x = FloatArray(n)
    val w = FloatArray(n)
    val uc = IntArray(n)
    val estado = Array(n) { EstadoCapsula.PENDIENTE }
    val nace = FloatArray(n)
    val llena = FloatArray(n)
    val cursa = FloatArray(n)
    val centros = FloatArray(a.semestres.size)
    /** Cuándo termina de llenarse lo aprobado de cada semestre: ahí la regla avanza */
    val finSemestre = FloatArray(a.semestres.size) { NUNCA }
    /** El índice del semestre por el que vas, o -1 */
    val actual = a.semestres.indexOfFirst { it.numero == a.actual }
    val ucAprobadas = a.semestres.sumOf { s -> s.capsulas.filter { it.estado == EstadoCapsula.APROBADA }.sumOf { it.uc } }
    /** Por dónde corre la luz de lo que falta: de la primera cápsula sin aprobar del semestre actual al final de la barra */
    val barridoDesde: Float
    val barridoHasta = IX + IW + 26f

    init {
        val s = a.semestres.size
        val total = a.semestres.sumOf { sem -> sem.capsulas.sumOf { it.uc } }.coerceAtLeast(1)
        var gM = 2f
        var gS = 7f
        // En un teléfono muy estrecho los huecos se afinan antes de que una materia de 1 UC desaparezca, como en la app
        if ((IW - gM * (n - s) - gS * (s - 1)) / total < 0.5f) {
            gM = 1f
            gS = 4f
        }
        val k = ((IW - gM * (n - s) - gS * (s - 1)) / total).coerceAtLeast(0.1f)
        val aprobadas = a.semestres.sumOf { sem -> sem.capsulas.count { it.estado == EstadoCapsula.APROBADA } }.coerceAtLeast(1)
        var i = 0
        var ia = 0
        var ic = 0
        var px = IX
        var desde = Float.NaN
        a.semestres.forEachIndexed { si, sem ->
            val x0 = px
            sem.capsulas.forEachIndexed { j, c ->
                x[i] = px
                w[i] = c.uc * k
                uc[i] = c.uc
                estado[i] = c.estado
                nace[i] = NACE + i * NACE_DURA / n
                when (c.estado) {
                    EstadoCapsula.APROBADA -> {
                        llena[i] = CUENTA + ia * DURA_CUENTA / aprobadas
                        finSemestre[si] = llena[i] + LLENA
                        ia++
                    }
                    EstadoCapsula.CURSANDO -> {
                        cursa[i] = CURSANDO + ic * 90f
                        ic++
                    }
                    EstadoCapsula.PENDIENTE -> Unit
                }
                if (desde.isNaN() && si >= actual.coerceAtLeast(0) && c.estado != EstadoCapsula.APROBADA) desde = px
                px += w[i] + if (j < sem.capsulas.lastIndex) gM else 0f
                i++
            }
            centros[si] = (x0 + px) / 2f
            px += gS
        }
        barridoDesde = (if (desde.isNaN()) IX else desde) - 26f
    }
}

/** Cuánto se ha llenado la materia `i` (de 0 a 1), si está aprobada */
internal fun GeometriaAvance.llenaDe(i: Int, t: Float) = ESTANDAR.transform(tramo(t, llena[i], llena[i] + LLENA))

/** Lo aprobado que ya se llenó, de 0 a 1 y en UC: la cifra y lo aprobado cuentan con ello */
internal fun GeometriaAvance.llenado(t: Float): Float {
    if (ucAprobadas <= 0) return 1f
    var llenas = 0f
    for (i in 0 until n) if (estado[i] == EstadoCapsula.APROBADA) llenas += uc[i] * llenaDe(i, t)
    return llenas / ucAprobadas
}

/** Cuándo pasa lo que cursas a su marca, y desde cuándo respira */
internal fun GeometriaAvance.cursaDe(i: Int, t: Float) = ESTANDAR.transform(tramo(t, cursa[i], cursa[i] + 420f))

/** Los textos de la escena, medidos una vez: en cada cuadro solo se pintan */
internal class TextosAvance(
    /** Una por cada valor que la cuenta puede tocar: contar no mide nada */
    val cifras: List<TextLayoutResult>,
    val signo: TextLayoutResult,
    val ucs: List<TextLayoutResult>,
    val deTotal: TextLayoutResult,
    val numeros: List<TextLayoutResult>,
    val numerosFuertes: List<TextLayoutResult>,
    val fechas: List<TextLayoutResult>,
    val teGraduas: TextLayoutResult,
    /** B y C: la cifra de 40 con su % de 16, los rótulos del anillo y el título */
    val cifras40: List<TextLayoutResult>,
    val signo40: TextLayoutResult,
    val hoy: TextLayoutResult,
    val universidad: TextLayoutResult,
    val nucleo: TextLayoutResult,
    val otorga: TextLayoutResult,
    val carrera: TextLayoutResult,
)

@Composable
private fun rememberTextos(a: AvanceDemo?): TextosAvance? {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val j = jost()
    val i = inter()
    val c = interCifra()
    return remember(a, densidad, j, i, c) { a?.let { medir(it, medidor, densidad, j, i, c) } }
}

private fun medir(a: AvanceDemo, medidor: TextMeasurer, d: Density, jost: FontFamily, inter: FontFamily, interCifra: FontFamily): TextosAvance {
    // En dp y no en sp: son parte del dibujo y escalan con la cámara, no con la letra del sistema
    fun tam(v: Float) = with(d) { v.dp.toSp() }
    fun m(texto: String, e: TextStyle) = medidor.measure(texto, e, softWrap = false, density = d)
    // Las letras de la hoja: la cifra héroe de 64 (Inter Display 200) con su % de 26 en 300, la nota de 13/16 y la fecha de 24 en 300
    val cifra = estiloCifra(interCifra, tam(64f))
    val signo = TextStyle(fontFamily = inter, fontWeight = FontWeight.Light, fontSize = tam(26f), fontFeatureSettings = "tnum")
    val nota = TextStyle(
        fontFamily = inter, fontSize = tam(13f), lineHeight = tam(16f), letterSpacing = (-0.003).em, fontFeatureSettings = "tnum",
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val fecha = estiloCifra(interCifra, tam(24f), peso = FontWeight.Light).copy(letterSpacing = (-0.015).em)
    val numero = TextStyle(fontFamily = jost, fontSize = tam(11f))
    // Las mayúsculas finas y espaciadas de las etiquetas
    val versal = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = tam(11f), letterSpacing = 0.14.em, fontFeatureSettings = "tnum")
    return TextosAvance(
        cifras = (0..a.porcentaje).map { m("$it", cifra) },
        signo = m("%", signo),
        ucs = (0..a.llevas).map { m("$it", nota.copy(fontWeight = FontWeight.Medium)) },
        deTotal = m(" / ${a.de} ${a.unidad}", nota),
        numeros = a.semestres.map { m("${it.numero}", numero) },
        numerosFuertes = a.semestres.map { m("${it.numero}", numero.copy(fontWeight = FontWeight.Medium)) },
        fechas = a.periodos.map { m(it.mesYAnio(), fecha) },
        teGraduas = m("Te gradúas", nota),
        cifras40 = (0..a.porcentaje).map { m("$it", estiloCifra(interCifra, tam(40f))) },
        signo40 = m("%", signo.copy(fontSize = tam(16f))),
        hoy = m("HOY", versal),
        universidad = m("UNIVERSIDAD DE ORIENTE", versal.copy(color = Paleta.suave)),
        nucleo = m("NÚCLEO DE MONAGAS", versal),
        otorga = m("otorga el título de", nota),
        carrera = m(CARRERA_TITULO, TextStyle(fontFamily = jost, fontWeight = FontWeight.Light, fontSize = tam(30f), letterSpacing = (-0.01).em)),
    )
}

/** La escena entera en el instante `t`, en las unidades del plano */
private fun DrawScope.pintar(t: Float, a: AvanceDemo, g: GeometriaAvance, x: TextosAvance, respira: Boolean) {
    // Al final de cada vuelta todo se apaga a la vez: la siguiente arranca de cero, sin salto
    val velo = 1f - Movimiento.ESTANDAR.transform(tramo(t, SE_VA, SE_VA + 700f))
    if (velo <= 0.004f) return

    // La superficie de la hoja: la tinta al 5 %, sin filo
    val tarjeta = Movimiento.ASIENTA.transform(tramo(t, NACE - 100f, NACE + 500f)) * velo
    drawRoundRect(Paleta.tinta, Offset(MX, TARJETA_Y), Size(W, TARJETA_ALTO), CornerRadius(24f), alpha = Velo.superficie * tarjeta)

    // Lo que ya se llenó, en UC: la cifra y lo aprobado cuentan con la barra
    val f = g.llenado(t)
    cifra(t, a, x, f, velo)
    fecha(t, x, velo)
    capsulas(t, g, velo, respira)
    regla(t, g, x, velo)
}

/** El 41 % con su signo en la línea base y, debajo, «62 / 153 UC», contando a la par de la barra */
private fun DrawScope.cifra(t: Float, a: AvanceDemo, x: TextosAvance, f: Float, velo: Float) {
    val e = Movimiento.ASIENTA.transform(tramo(t, CIFRA, CIFRA + 500f))
    val op = e * velo
    if (op <= 0.004f) return
    val dy = (1f - e) * 8f
    val cifra = x.cifras[(a.porcentaje * f).roundToInt().coerceIn(0, a.porcentaje)]
    textoPorBase(cifra, IX, BASE_CIFRA + dy, Paleta.tinta, op)
    textoPorBase(x.signo, IX + anchoPlano(cifra) + 2f, BASE_CIFRA + dy, Paleta.tenue, op)
    val uc = x.ucs[(a.llevas * f).roundToInt().coerceIn(0, a.llevas)]
    textoPlano(uc, IX, Y_UC + dy, Paleta.tinta, op)
    textoPlano(x.deTotal, IX + anchoPlano(uc), Y_UC + dy, Paleta.tenue, op)
}

/**
 * La fecha de la esquina: llega con la luz y pasa período a período (el que
 * se va sube y se apaga, el que llega sube desde abajo) hasta el del grado,
 * que se asienta en tinta. «Te gradúas» queda debajo desde el principio.
 */
private fun DrawScope.fecha(t: Float, x: TextosAvance, velo: Float) {
    val np = x.fechas.size
    if (np == 0) return
    val e = Movimiento.ASIENTA.transform(tramo(t, PROYECTA - 250f, PROYECTA + 300f))
    val op = e * velo
    if (op <= 0.004f) return
    val dy = (1f - e) * 8f
    val derecha = IX + IW
    textoPlano(x.teGraduas, derecha, Y_TE_GRADUAS + dy, Paleta.tenue, op, ancla = 1f)
    fechaQueCorre(t, x.fechas, derecha, BASE_FECHA + dy, 1f, PROYECTA, LLEGA, op)
}

/** Las cápsulas: nacen pendientes, lo aprobado se llena de tinta en ola, lo que cursas pasa a contorno y lo que falta deja pasar la luz */
private fun DrawScope.capsulas(t: Float, g: GeometriaAvance, velo: Float, respira: Boolean) {
    val pendiente = Velo.pendiente
    // La luz que recorre lo que falta, a paso constante: cada cápsula se aclara cuando pasa y vuelve a su gris
    val luz = if (t >= PROYECTA && t <= LLEGA + 600f) mezcla(g.barridoDesde, g.barridoHasta, tramo(t, PROYECTA, LLEGA)) else Float.NaN
    for (i in 0 until g.n) {
        val nace = ESTANDAR.transform(tramo(t, g.nace[i], g.nace[i] + 360f))
        if (nace <= 0f) continue
        val h = ALTO_CAPSULA * (0.4f + 0.6f * nace)
        val o = Offset(g.x[i], BARRA_Y + (ALTO_CAPSULA - h) / 2)
        val tam = Size(g.w[i], h)
        val op = nace * velo
        when (g.estado[i]) {
            EstadoCapsula.APROBADA -> {
                val k = ESTANDAR.transform(tramo(t, g.llena[i], g.llena[i] + LLENA))
                drawRoundRect(Paleta.tinta, o, tam, RADIO_CAPSULA, alpha = mezcla(pendiente, 1f, k) * op)
            }
            EstadoCapsula.CURSANDO -> {
                val k = ESTANDAR.transform(tramo(t, g.cursa[i], g.cursa[i] + 420f))
                if (k < 1f) drawRoundRect(Paleta.tinta, o, tam, RADIO_CAPSULA, alpha = pendiente * (1f - k) * op)
                if (k > 0f) {
                    var aro = k
                    // Respira al ritmo del punto «En curso»: arranca en su máximo, así no salta
                    val desde = g.cursa[i] + 420f
                    if (respira && t > desde) aro *= 0.7f + 0.3f * cos(2f * PI.toFloat() * (t - desde) / RESPIRA)
                    drawRoundRect(
                        Paleta.tinta, Offset(o.x + 0.75f, o.y + 0.75f), Size((tam.width - 1.5f).coerceAtLeast(0.5f), tam.height - 1.5f),
                        RADIO_CAPSULA, style = CONTORNO, alpha = aro * op,
                    )
                }
            }
            EstadoCapsula.PENDIENTE -> {
                val brillo = if (luz.isNaN()) 0f else 0.42f * suave(1f - min(1f, abs(luz - (g.x[i] + g.w[i] / 2)) / 26f))
                drawRoundRect(Paleta.tinta, o, tam, RADIO_CAPSULA, alpha = (pendiente + brillo) * op)
            }
        }
    }
}

/**
 * La regla de semestres: un hilo que se traza, una marca y un número por
 * semestre, y el círculo de tinta del actual, que avanza semestre a semestre
 * a medida que se llenan. Lo completo pasa a tinta; el número que el círculo
 * cubre, al color del panel.
 */
private fun DrawScope.regla(t: Float, g: GeometriaAvance, x: TextosAvance, velo: Float) {
    val hilo = ESTANDAR.transform(tramo(t, NACE + 200f, NACE + 900f))
    if (hilo <= 0f) return
    drawRect(Paleta.tinta, Offset(IX, Y_REGLA - 0.5f), Size(IW * hilo, 1f), alpha = Velo.hilo * velo)
    val ultimo = g.centros.size - 1
    // Dónde va el círculo, en semestres: suma un paso por cada semestre que termina antes del actual
    var m = 0f
    for (c in 0 until g.actual) m += ESTANDAR.transform(tramo(t, g.finSemestre[c], g.finSemestre[c] + 420f))
    val marca = if (g.actual >= 0) ESTANDAR.transform(tramo(t, CUENTA - 200f, CUENTA + 200f)) else 0f
    if (marca > 0f) {
        val i0 = m.toInt().coerceIn(0, ultimo)
        val xm = mezcla(g.centros[i0], g.centros[(i0 + 1).coerceAtMost(ultimo)], m - i0)
        drawCircle(Paleta.tinta, 8.5f, Offset(xm, Y_REGLA + 15f), alpha = marca * velo)
    }
    for (c in 0..ultimo) {
        val aparece = ESTANDAR.transform(tramo(t, NACE + 300f + 50f * c, NACE + 700f + 50f * c)) * velo
        if (aparece <= 0.004f) continue
        val cx = g.centros[c]
        val hecho = ESTANDAR.transform(tramo(t, g.finSemestre[c], g.finSemestre[c] + 300f))
        val bajoMarca = (1f - abs(m - c) / 0.35f).coerceIn(0f, 1f) * marca
        val fuerte = if (c == g.actual) bajoMarca else 0f
        drawLine(
            mezclarColor(Paleta.tinta.copy(alpha = Velo.hilo), Paleta.tinta, maxOf(hecho, fuerte)),
            Offset(cx, Y_REGLA), Offset(cx, Y_REGLA + 4f), strokeWidth = 1f, alpha = aparece,
        )
        val color = mezclarColor(mezclarColor(Paleta.apagada, Paleta.tinta, hecho), Paleta.panel, bajoMarca)
        val numero = if (hecho > 0.5f || bajoMarca > 0.5f) x.numerosFuertes[c] else x.numeros[c]
        textoPorBase(numero, cx, Y_REGLA + 19f, color, aparece, ancla = 0.5f)
    }
}
