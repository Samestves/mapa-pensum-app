package mpensum.tema

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import mpensum.datos.ColorCarrera
import mpensum.recursos.Res
import mpensum.recursos.inter
import mpensum.recursos.jost
import mpensum.recursos.plex_mono_light
import org.jetbrains.compose.resources.Font

/**
 * Los siete juegos de color de la app (los tokens de tokens.py de las
 * maquetas). `claro` dice si va sobre papel (las barras del sistema y las
 * ramas [porTema] siguen a él) y `aurora` cuánta luz de fondo trae de serie:
 * Negro y Exterior, ninguna.
 */
enum class Juego(val claro: Boolean, val aurora: Float, val nombre: String) {
    OSCURO(false, 1f, "Oscuro"),
    GRAFITO(false, 0.5f, "Grafito"),
    NEGRO(false, 0f, "Negro"),
    MEDIANOCHE(false, 1f, "Medianoche"),
    CLARO(true, 0.35f, "Claro"),
    PAPEL(true, 0.35f, "Papel"),
    EXTERIOR(true, 0f, "Exterior"),
}

/** Los acentos que se pueden elegir; sin elegir ninguno, el de la carrera */
enum class Acento(val valor: String, val nombre: String, private val oscuro: Long, private val claro: Long) {
    NEUTRO("neutro", "Neutro", 0, 0),
    ARCILLA("arcilla", "Arcilla", 0xFFE08A6A, 0xFFA8492C),
    ROSA("rosa", "Rosa", 0xFFE596AE, 0xFFA8466A),
    LAGUNA("laguna", "Laguna", 0xFF45C4C8, 0xFF0B767C),
    ARENA("arena", "Arena", 0xFFCDBBA4, 0xFF75644F);

    /** El tono para el tema que se ve; Neutro es el gris suave de la paleta */
    val color: Color get() = if (this == NEUTRO) Paleta.suave else Color(if (Tema.claro) claro else oscuro)

    /** El tono para un tema dado, para dibujar la muestra de otro tema */
    fun colorEn(claroSobre: Boolean, suave: Color): Color = if (this == NEUTRO) suave else Color(if (claroSobre) claro else oscuro)

    companion object {
        fun de(valor: String?): Acento? = entries.firstOrNull { it.valor == valor }
    }
}

/**
 * El tema que se ve. Es un único estado de snapshot: todo lo que lee un color
 * de [Paleta] se suscribe a él solo, así que cambiar de tema es cambiar este
 * valor, sin pasar nada por parámetros ni reescribir pantallas. Lo pone
 * [TemaVivo]; las pruebas lo fijan a mano.
 *
 * Lo demás son las preferencias de Personalizar ya resueltas (con sus valores
 * por defecto): las pone App y las leen quienes dibujan.
 */
object Tema {
    var juego by mutableStateOf(Juego.OSCURO)
    val claro: Boolean get() = juego.claro

    /** Exterior: el de más contraste, con bordes más gruesos y cables más firmes */
    val fuerte: Boolean get() = juego == Juego.EXTERIOR

    /** null es el color de la carrera */
    var acento by mutableStateOf<Acento?>(null)
    var textoGrande by mutableStateOf(false)
    var luzFondo by mutableStateOf(true)
    /** Reducir movimiento: sin bucles ni latidos, y las entradas largas pasan a fundidos cortos */
    var movimiento by mutableStateOf(false)
    var codigos by mutableStateOf(true)
    /**
     * Modo ligero: para teléfonos modestos. Sin desenfoque (el cristal pasa a
     * un tinte liso), sin bucles, sin grano y con el mapa más sencillo de
     * pintar. Lo decide el teléfono al arrancar, salvo que se elija a mano.
     */
    var ligero by mutableStateOf(false)

    /** Sin luces, latidos ni ondas en bucle: con el movimiento reducido o en modo ligero */
    val sinBucles: Boolean get() = movimiento || ligero

    /** Cuánto pesa la aurora de la portada: la del juego, si la luz de fondo está encendida */
    val intensidadAurora: Float get() = juego.aurora * (if (luzFondo) 1f else 0f)

    /** El grosor de los bordes de cristales y tarjetas, en dp */
    val grosorBorde: Float get() = if (fuerte) 1.5f else 1f
}

/** El valor de cada tema: el primero en oscuro, el segundo en claro */
fun <T> porTema(oscuro: T, claro: T): T = if (Tema.claro) claro else oscuro

private fun par(oscuro: Long, claro: Long) = Color(if (Tema.claro) claro else oscuro)

/** El color de acento: el de la carrera si no se eligió otro, o el elegido */
fun acentoDe(colorCarrera: Color): Color = Tema.acento?.color ?: colorCarrera

/**
 * Los tokens de un juego, en el orden de tokens.py: lienzo, panel,
 * panelSuave, borde, tinta, suave, tenue, apagada, azul, azulClaro, verde,
 * verdeClaro, ambar, rojo, aurora, turquesa, rejilla, vidrio y sombra.
 */
internal class Tokens(hex: String) {
    private val v = hex.split(' ').map { Color(0xFF000000 or it.toLong(16)) }
    val lienzo = v[0]; val panel = v[1]; val panelSuave = v[2]; val borde = v[3]; val tinta = v[4]
    val suave = v[5]; val tenue = v[6]; val apagada = v[7]; val azul = v[8]; val azulClaro = v[9]
    val verde = v[10]; val verdeClaro = v[11]; val ambar = v[12]; val rojo = v[13]; val aurora = v[14]
    val turquesa = v[15]; val rejilla = v[16]; val vidrio = v[17]; val sombra = v[18]
}

/** En el orden del enum [Juego] */
private val TABLA = arrayOf(
    Tokens("0B0C0E 121316 16181B 262A2F ECEEF1 B6BBC2 858B94 5D636C 5B8AF5 9DB8FF 2EC27E 96E0BE F0A53A E5403A 6094FF 2FE8C6 15171A 15171A 000000"),
    Tokens("2A2B2E 323438 383A3E 474A50 F0F1F3 CACDD2 A6AAB1 8A8F97 7FA3F8 B6CBFF 4CCF93 A8E7CB F2AE4E F27A72 7FA5FF 4FE2C6 303236 35373B 111214"),
    Tokens("000000 0B0B0C 101113 202328 ECEEF1 B6BBC2 858B94 5D636C 5B8AF5 9DB8FF 2EC27E 96E0BE F0A53A E5403A 6094FF 2FE8C6 0C0D0F 0D0E10 000000"),
    Tokens("0D1320 141B29 192132 28324A E8ECF4 B4BDCD 8893A8 646E84 7AA2FF ADC6FF 3CCB8B A0E4C6 F0AA48 F06560 5E8CFF 3BE0C8 141B29 151C2B 03060C"),
    Tokens("F4F4F4 FFFFFF F6F6F7 DCDDE0 171A20 393C41 5C5E62 717479 3E6AE1 2F57C9 0F8A5F 0B7350 A86200 CC1F24 2751CC 0A9781 E4E5E7 FFFFFF 171A20"),
    Tokens("F2EEE6 FBF8F2 EFEAE0 DBD3C5 2A251E 4A4339 6A6256 756D60 2F5DC4 284FA8 1C7A4F 166541 975800 B3302A 8A7458 2A8577 E7E1D5 FBF8F2 3A2E20"),
    Tokens("FFFFFF FFFFFF F0F1F3 84898F 050608 1C1F23 34383E 52565C 1A47C2 133A9F 00603F 00573A 874700 A80F1A 1A47C2 006B5C E2E4E7 FFFFFF 000000"),
)

internal fun tokensDe(j: Juego): Tokens = TABLA[j.ordinal]

/**
 * La paleta Acero de la web (src/estilos/tema.css) en sus siete juegos. Cada
 * color es un getter que lee el juego actual, así que se recompone o se
 * redibuja solo donde se use. El único azul sigue siendo lo inscribible, y
 * los estados son los tonos profundos en los juegos claros (los vivos del
 * oscuro se lavan sobre blanco).
 */
object Paleta {
    private val t get() = TABLA[Tema.juego.ordinal]

    val lienzo get() = t.lienzo
    val panel get() = t.panel
    val panelSuave get() = t.panelSuave
    val borde get() = t.borde
    val tinta get() = t.tinta
    val suave get() = t.suave
    /** Sobre panel blanco, más claro que esto queda lavado */
    val tenue get() = t.tenue
    /** Lo deshabilitado y lo de poco peso; en claro aguanta 4,6:1 sobre la hoja blanca */
    val apagada get() = t.apagada
    val azul get() = t.azul
    val verde get() = t.verde
    /** «Cursando» no tiene color propio: es la tinta, y se distingue por la forma (contorno, peso 500) */
    val cursando get() = t.tinta
    val rojo get() = t.rojo
    /** La aurora: en claro el azul profundo de la carrera, que tiñe en vez de brillar */
    val aurora get() = t.aurora
    val turquesa get() = t.turquesa

    /** La rejilla del fondo del mapa (--rejilla) */
    val rejilla get() = t.rejilla
    /** La base del cristal: grafito en oscuro, blanco lechoso en claro */
    val vidrio get() = t.vidrio
    /** Las sombras: negro en oscuro; en claro, la tinta, que ensucia menos que el negro puro */
    val sombra get() = t.sombra
}

/** Un color de carrera en "#rrggbb", como viene del índice */
fun colorDe(hex: String): Color {
    val v = hex.removePrefix("#").toLong(16)
    return Color(0xFF000000 or v)
}

/** El color de una carrera en el tema que se ve: el índice trae los dos, ya ajustados */
fun colorDe(c: ColorCarrera): Color = colorDe(if (Tema.claro) c.claro else c.oscuro)

/** El color de cada área del pensum (--area-* de tema.css): en claro, los mismos tonos más profundos */
fun colorArea(area: String?): Color = when (area) {
    "ciencias-basicas" -> par(0xFF5BC0EB, 0xFF1B7FB0)
    "estadistica" -> par(0xFFA78BFA, 0xFF6F42C9)
    "computacion" -> par(0xFF3CCFA8, 0xFF0F8A75)
    "electronica" -> par(0xFFF2A65A, 0xFFB8610F)
    "sistemas" -> par(0xFF5B8AF5, 0xFF3E6AE1)
    "gestion" -> par(0xFFF0739B, 0xFFC2326A)
    "tesis" -> par(0xFFE8CF4F, 0xFF957200)
    else -> par(0xFF8B9199, 0xFF6B6E73)
}

/**
 * El peso de los nombres de materia en el mapa (--peso-nombre). Cambia con el
 * tema: el trazo claro sobre negro se derrama y se ve más grueso, así que en
 * claro hace falta un punto más para leerse igual de firme.
 */
val PESO_NOMBRE: FontWeight get() = if (Tema.claro) PESO_NOMBRE_CLARO else PESO_NOMBRE_OSCURO
private val PESO_NOMBRE_OSCURO = FontWeight(380)
private val PESO_NOMBRE_CLARO = FontWeight(440)

private fun variable(peso: Int) = FontVariation.Settings(FontVariation.weight(peso))

/**
 * Las cuatro familias de la app, hechas una sola vez en la raíz. Cada `Font()`
 * de recurso es un composable con su `remember` y su carga: armar la familia
 * en cada llamada (más de 70 sitios, varias veces por fila de la lista) era
 * trabajo repetido. `jost()` y compañía leen de aquí sin cambiar su firma.
 */
@Immutable
class Fuentes(val jost: FontFamily, val inter: FontFamily, val interCifra: FontFamily, val plex: FontFamily)

val LocalFuentes = staticCompositionLocalOf<Fuentes?> { null }

/** Las fuentes de la app, para proveerlas en la raíz */
@Composable
fun rememberFuentes(): Fuentes {
    val j = nuevaJost()
    val i = nuevaInter()
    val c = nuevaInterCifra()
    val p = nuevaPlex()
    return remember(j, i, c, p) { Fuentes(j, i, c, p) }
}

/** Jost: títulos ligeros y nombres, la "voz que nombra" de la web */
@Composable
fun jost(): FontFamily = LocalFuentes.current?.jost ?: nuevaJost()

/** Inter: el texto */
@Composable
fun inter(): FontFamily = LocalFuentes.current?.inter ?: nuevaInter()

/** Inter «Display» (opsz 32) en 200 y 300: las cifras grandes. Úsese con fontFeatureSettings = "tnum" */
@Composable
fun interCifra(): FontFamily = LocalFuentes.current?.interCifra ?: nuevaInterCifra()

/** IBM Plex Mono: solo códigos y UC, como en las tarjetas del mapa */
@Composable
fun plex(): FontFamily = LocalFuentes.current?.plex ?: nuevaPlex()

@Composable
private fun nuevaJost() = FontFamily(
    Font(Res.font.jost, FontWeight.Thin, variationSettings = variable(100)),
    Font(Res.font.jost, FontWeight.ExtraLight, variationSettings = variable(200)),
    Font(Res.font.jost, FontWeight.Light, variationSettings = variable(300)),
    // El peso de los nombres en las tarjetas del mapa (--peso-nombre de la web)
    Font(Res.font.jost, PESO_NOMBRE_OSCURO, variationSettings = variable(380)),
    Font(Res.font.jost, PESO_NOMBRE_CLARO, variationSettings = variable(440)),
    Font(Res.font.jost, FontWeight.Normal, variationSettings = variable(400)),
    Font(Res.font.jost, FontWeight.Medium, variationSettings = variable(500)),
)

@Composable
private fun nuevaInter() = FontFamily(
    Font(Res.font.inter, FontWeight.Light, variationSettings = variable(300)),
    Font(Res.font.inter, FontWeight.Normal, variationSettings = variable(400)),
    Font(Res.font.inter, FontWeight.Medium, variationSettings = variable(500)),
    Font(Res.font.inter, FontWeight.SemiBold, variationSettings = variable(600)),
)

@Composable
private fun nuevaInterCifra() = FontFamily(
    Font(Res.font.inter, FontWeight.ExtraLight, variationSettings = FontVariation.Settings(FontVariation.weight(200), FontVariation.Setting("opsz", 32f))),
    Font(Res.font.inter, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300), FontVariation.Setting("opsz", 32f))),
)

@Composable
private fun nuevaPlex() = FontFamily(Font(Res.font.plex_mono_light, FontWeight.Light))
