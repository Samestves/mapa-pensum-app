package mpensum.bienvenida

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import mpensum.carrera.Velo
import mpensum.componentes.Lucide
import mpensum.logica.EstadoCapsula
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Pantalla 3, concepto B: «El anillo del grado», el de «¿Cuándo me gradúo?».
 * Una marca radial por materia, en sentido horario desde arriba; el hueco de
 * arriba es la meta, con el birrete. Nacen todas en gris; lo aprobado se
 * enciende en verde (más vivo cuanto más cerca de hoy) mientras el 41 % del
 * centro cuenta; lo que cursas pasa a tinta y aparece HOY; una luz recorre lo
 * que falta (tu próximo semestre en azul, lo demás en tinta cada vez más
 * apagada hacia la meta) mientras la fecha de abajo corre hasta la del grado,
 * y el birrete se enciende. Queda viva la respiración de lo que cursas.
 *
 * Mismo reloj y mismos datos que la tarjeta (RitmoAvance, GeometriaAvance):
 * solo cambia el dibujo. Nada se crea por cuadro.
 */

private val ESTANDAR = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** El anillo en el plano: centro, radios de las marcas y el hueco de la meta (en marcas) */
private const val CX = 195f
private const val CY = 212f
private const val RO = 128f
private const val RI = 106f
private const val HUECO = 8
private val META = Offset(CX, CY - (RO + RI) / 2)
private const val BASE_FECHA = CY + RO + 40f
private val TRAZO_BIRRETE = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val ARO = Stroke(1.5f)

/*
 * La toma: cerca e inclinada al nacer, se aleja mientras se llena y se
 * asienta de frente cuando la meta se enciende.
 *                      ms     x     y    zoom  incl  giro
 */
internal val TOMA_ANILLO = TomaEnBucle(
    listOf(
        floatArrayOf(0f, 195f, 236f, 1.08f, 10f, -1.2f),
        floatArrayOf(1_400f, 195f, 234f, 1.04f, 5f, -0.4f),
        floatArrayOf(3_800f, 193f, 238f, 1.00f, 3f, 0f),
        floatArrayOf(7_000f, 195f, 232f, 1.03f, 2f, 0.4f),
        floatArrayOf(8_800f, 195f, 236f, 1.02f, 1f, 0f),
        floatArrayOf(RitmoAvance.CICLO.toFloat(), 195f, 236f, 1.08f, 10f, -1.2f),
    ),
)

/** En modo ligero, el anillo de frente y en el centro */
internal val CENTRO_ANILLO = Offset(195f, 238f)

internal fun DrawScope.pintarAnillo(t: Float, a: AvanceDemo, g: GeometriaAvance, x: TextosAvance, respira: Boolean) {
    val velo = 1f - Movimiento.ESTANDAR.transform(tramo(t, RitmoAvance.SE_VA, RitmoAvance.SE_VA + 700f))
    if (velo <= 0.004f) return
    val n = g.n
    if (n == 0) return
    val paso = 2f * PI.toFloat() / (n + HUECO)

    // Dónde empieza lo que falta: la primera materia sin aprobar del semestre por el que vas
    var inicio = 0
    for (c in 0 until g.actual.coerceAtLeast(0)) inicio += a.semestres[c].capsulas.size
    while (inicio < n && g.estado[inicio] == EstadoCapsula.APROBADA) inicio++
    // La luz que recorre lo que falta, en marcas: sale de hoy y llega a la meta
    val luz = mezcla(inicio - 1.5f, n + 1.5f, tramo(t, RitmoAvance.PROYECTA, RitmoAvance.LLEGA))
    val proyecta = t >= RitmoAvance.PROYECTA
    val aprobadas = g.estado.count { it == EstadoCapsula.APROBADA }.coerceAtLeast(1)
    var ia = 0
    var hoy = -1

    for (i in 0 until n) {
        val nace = ESTANDAR.transform(tramo(t, g.nace[i], g.nace[i] + 360f))
        if (nace <= 0f) continue
        val angulo = -PI.toFloat() / 2 + (HUECO / 2f + i + 0.5f) * paso
        val c = cos(angulo)
        val s = sin(angulo)
        val ro = RI + (RO - RI) * nace
        val color = when (g.estado[i]) {
            EstadoCapsula.APROBADA -> {
                // Más vivo cuanto más cerca de hoy, como en el anillo de la app
                val vivo = mezcla(0.5f, 1f, ia.toFloat() / aprobadas)
                ia++
                mezclarColor(Paleta.tinta.copy(alpha = Velo.pendiente), Paleta.verde.copy(alpha = vivo), g.llenaDe(i, t))
            }
            EstadoCapsula.CURSANDO -> {
                if (hoy < 0) hoy = i
                val k = g.cursaDe(i, t)
                var tinta = 1f
                val desde = g.cursa[i] + 420f
                if (respira && t > desde) tinta = 0.7f + 0.3f * cos(2f * PI.toFloat() * (t - desde) / RitmoAvance.RESPIRA)
                mezclarColor(Paleta.tinta.copy(alpha = Velo.pendiente), Paleta.tinta.copy(alpha = tinta), k)
            }
            EstadoCapsula.PENDIENTE -> {
                if (!proyecta || i < inicio) {
                    Paleta.tinta.copy(alpha = Velo.pendiente)
                } else {
                    // Al pasar la luz: el próximo semestre en azul, lo demás en tinta que se apaga hacia la meta
                    val paso2 = ESTANDAR.transform(((luz - i) / 2f).coerceIn(0f, 1f))
                    val brillo = 0.35f * suave(1f - (kotlin.math.abs(luz - i) / 2.5f).coerceAtMost(1f))
                    val destino = if (a.proximo.getOrElse(i) { false }) Paleta.azul
                    else Paleta.tinta.copy(alpha = mezcla(0.5f, 0.22f, (i - inicio).toFloat() / (n - inicio).coerceAtLeast(1)))
                    val base = mezclarColor(Paleta.tinta.copy(alpha = Velo.pendiente), destino, paso2)
                    base.copy(alpha = (base.alpha + brillo).coerceAtMost(1f))
                }
            }
        }
        drawLine(color, Offset(CX + c * RI, CY + s * RI), Offset(CX + c * ro, CY + s * ro), strokeWidth = 3f, cap = StrokeCap.Round, alpha = nace * velo)
    }

    // HOY, por fuera, junto a lo que cursas: una rayita y su rótulo
    val alfaHoy = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.CURSANDO + 200f, RitmoAvance.CURSANDO + 700f)) * velo
    if (hoy >= 0 && alfaHoy > 0.004f) {
        val angulo = -PI.toFloat() / 2 + (HUECO / 2f + hoy + 0.5f) * paso
        val c = cos(angulo)
        val s = sin(angulo)
        drawLine(Paleta.tinta, Offset(CX + c * (RO + 6f), CY + s * (RO + 6f)), Offset(CX + c * (RO + 13f), CY + s * (RO + 13f)), strokeWidth = 1.5f, cap = StrokeCap.Round, alpha = alfaHoy)
        val r = RO + 26f
        textoPlano(x.hoy, CX + c * r, CY + s * r - 8f, Paleta.tinta, alfaHoy, ancla = 0.5f)
    }

    // La cifra en el centro, anclada a su ancho final para que no baile mientras cuenta
    val f = g.llenado(t)
    val e = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.CIFRA, RitmoAvance.CIFRA + 500f))
    val op = e * velo
    if (op > 0.004f) {
        val dy = (1f - e) * 8f
        val final = x.cifras[a.porcentaje]
        val izq = CX - (anchoPlano(final) + 2f + anchoPlano(x.signo)) / 2
        val cifra = x.cifras[(a.porcentaje * f).roundToInt().coerceIn(0, a.porcentaje)]
        textoPorBase(cifra, izq + anchoPlano(final) - anchoPlano(cifra), CY + 14f + dy, Paleta.tinta, op)
        textoPorBase(x.signo, izq + anchoPlano(final) + 2f, CY + 14f + dy, Paleta.tenue, op)
        val ucFinal = x.ucs[a.llevas]
        val izqUc = CX - (anchoPlano(ucFinal) + anchoPlano(x.deTotal)) / 2
        val uc = x.ucs[(a.llevas * f).roundToInt().coerceIn(0, a.llevas)]
        textoPlano(uc, izqUc + anchoPlano(ucFinal) - anchoPlano(uc), CY + 26f + dy, Paleta.tinta, op)
        textoPlano(x.deTotal, izqUc + anchoPlano(ucFinal), CY + 26f + dy, Paleta.tenue, op)
    }

    // La meta: el birrete se ve tenue desde el principio y se enciende al llegar la luz, con una sola onda
    val alfaMeta = ESTANDAR.transform(tramo(t, RitmoAvance.NACE + 300f, RitmoAvance.NACE + 900f)) * velo
    if (alfaMeta > 0.004f) {
        val llega = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.LLEGA - 100f, RitmoAvance.LLEGA + 500f))
        val onda = tramo(t, RitmoAvance.LLEGA, RitmoAvance.LLEGA + 900f)
        if (onda > 0f && onda < 1f) {
            val q = Movimiento.ASIENTA.transform(onda)
            drawCircle(Paleta.tinta, 16f * (1f + 0.8f * q), META, style = ARO, alpha = 0.45f * (1f - onda) * velo)
        }
        translate(META.x - 11f, META.y - 11f) {
            scale(22f / 24f, 22f / 24f, Offset.Zero) {
                Lucide.birrete.forEach { drawPath(it, Paleta.tinta, alpha = mezcla(0.3f, 1f, llega) * alfaMeta, style = TRAZO_BIRRETE) }
            }
        }
    }

    // La fecha debajo del anillo, que corre período a período hasta el del grado
    val sale = Movimiento.ASIENTA.transform(tramo(t, RitmoAvance.PROYECTA - 250f, RitmoAvance.PROYECTA + 300f))
    val opFecha = sale * velo
    if (opFecha > 0.004f) {
        val dy = (1f - sale) * 8f
        fechaQueCorre(t, x.fechas, CX, BASE_FECHA + dy, 0.5f, RitmoAvance.PROYECTA, RitmoAvance.LLEGA, opFecha)
        textoPlano(x.teGraduas, CX, BASE_FECHA + 8f + dy, Paleta.tenue, opFecha, ancla = 0.5f)
    }
}
