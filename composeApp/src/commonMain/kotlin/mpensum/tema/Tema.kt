package mpensum.tema

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
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

/** El fondo del mapa */
enum class Fondo { REJILLA, PUNTOS, LISO }

/** Cómo se dibujan los cables de prelación */
enum class FormaCables { CURVOS, RECTOS, AL_TOCAR }

/** Lo gruesos que van los cables: un factor sobre el trazo de cada uno */
enum class Grosor(val factor: Float) {
    FINO(0.7f), NORMAL(1f), GRUESO(1.5f)
}

/**
 * Los doce juegos de color de la app (los tokens de tokens.py de las
 * maquetas). `claro` dice si va sobre papel (las barras del sistema y las
 * ramas [porTema] siguen a él), `aurora` cuánta luz de fondo trae de serie,
 * `fondo` el fondo del mapa que pone al elegirlo y `cables` cuánto color de su
 * área llevan los cables (1 el del área, 0 la tinta). `exterior` es para el
 * sol: bordes firmes y cristal en tinte liso.
 */
enum class Juego(
    val claro: Boolean,
    val aurora: Float,
    val nombre: String,
    val fondo: Fondo,
    val cables: Float,
    val exterior: Boolean = false,
) {
    OSCURO(false, 1f, "Oscuro", Fondo.REJILLA, 1f),
    NEGRO(false, 0f, "Negro", Fondo.LISO, 1f),
    GRAFITO(false, 0.45f, "Grafito", Fondo.PUNTOS, 0.5f),
    MEDIANOCHE(false, 1.15f, "Medianoche", Fondo.REJILLA, 1f),
    CIRUELA(false, 0.9f, "Ciruela", Fondo.PUNTOS, 0.9f),
    CLARO(true, 0.35f, "Claro", Fondo.REJILLA, 1f),
    PAPEL(true, 0f, "Papel", Fondo.PUNTOS, 0.15f),
    NIEBLA(true, 0.45f, "Niebla", Fondo.REJILLA, 0.9f),
    ALUMINIO(true, 0.7f, "Aluminio", Fondo.REJILLA, 0.7f),
    CUARZO(true, 0.35f, "Cuarzo", Fondo.PUNTOS, 0.8f),
    EXTERIOR_OSCURO(false, 0f, "Exterior oscuro", Fondo.LISO, 1f, exterior = true),
    EXTERIOR(true, 0f, "Exterior", Fondo.REJILLA, 1f, exterior = true),
}

/** La pareja de cada juego, para proponer el otro de Sistema */
val Juego.pareja: Juego get() = when (this) {
    Juego.OSCURO -> Juego.CLARO
    Juego.NEGRO -> Juego.CLARO
    Juego.GRAFITO -> Juego.ALUMINIO
    Juego.MEDIANOCHE -> Juego.NIEBLA
    Juego.CIRUELA -> Juego.CUARZO
    Juego.CLARO -> Juego.OSCURO
    Juego.PAPEL -> Juego.GRAFITO
    Juego.NIEBLA -> Juego.MEDIANOCHE
    Juego.ALUMINIO -> Juego.GRAFITO
    Juego.CUARZO -> Juego.CIRUELA
    Juego.EXTERIOR_OSCURO -> Juego.EXTERIOR
    Juego.EXTERIOR -> Juego.EXTERIOR_OSCURO
}

/** Los acentos que se pueden elegir; sin elegir ninguno, el de la carrera. Uno guardado que ya no existe vuelve a «Tu carrera» */
enum class Acento(val valor: String, val nombre: String, private val oscuro: Long, private val claro: Long) {
    NEUTRO("neutro", "Neutro", 0, 0),
    LAGUNA("laguna", "Laguna", 0xFF45C4C8, 0xFF0A6A70),
    LILA("lila", "Lila", 0xFFA99CF6, 0xFF5644C2),
    ORQUIDEA("orquidea", "Orquídea", 0xFFD98BE3, 0xFF8E3A9C),
    ROSA("rosa", "Rosa", 0xFFEE93B0, 0xFF9C3860);

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

    /** Más contraste: textos, bordes y rejilla hacia la tinta (otra tabla de tokens) */
    var contraste by mutableStateOf(false)

    /** El de más contraste, con bordes más gruesos y cables más firmes: el interruptor o un Exterior */
    val fuerte: Boolean get() = contraste || juego.exterior

    /** El fondo del mapa: el elegido, o el del tema */
    var fondo by mutableStateOf(Fondo.REJILLA)
    var cables by mutableStateOf(FormaCables.CURVOS)
    /** Si los cables van de codos: lo único de [cables] que cambia el layout, así que quien lo lea solo se entera cuando cambia esto */
    val rectos: Boolean by derivedStateOf { cables == FormaCables.RECTOS }
    var grosor by mutableStateOf(Grosor.NORMAL)
    /** Lo aprobado se dibuja al 45 %: el ojo va a lo que falta */
    var atenuarAprobadas by mutableStateOf(false)
    /** Las horas del horario en 24 h, sin meridiano */
    var hora24 by mutableStateOf(false)
    /** Con ella apagada no suena ningún tacto: todo pasa por [mpensum.componentes.ProveerTacto] */
    var vibracion by mutableStateOf(true)

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

    /** Sin desenfoque: el cristal pasa a un tinte liso en modo ligero y en los Exterior (a pleno sol el desenfoque solo resta) */
    val sinDesenfoque: Boolean get() = ligero || juego.exterior

    /** Sin luces, latidos ni ondas en bucle: con el movimiento reducido o en modo ligero */
    val sinBucles: Boolean get() = movimiento || ligero

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
 * verdeClaro, rojo, aurora, aurora2, rejilla, vidrio y sombra.
 */
internal class Tokens(hex: String) {
    private val v = hex.split(' ').map { Color(0xFF000000 or it.toLong(16)) }
    val lienzo = v[0]; val panel = v[1]; val panelSuave = v[2]; val borde = v[3]; val tinta = v[4]
    val suave = v[5]; val tenue = v[6]; val apagada = v[7]; val azul = v[8]; val azulClaro = v[9]
    val verde = v[10]; val verdeClaro = v[11]; val rojo = v[12]; val aurora = v[13]; val aurora2 = v[14]
    val rejilla = v[15]; val vidrio = v[16]; val sombra = v[17]
}

/** En el orden del enum [Juego] */
private val TABLA = arrayOf(
    Tokens("0B0C0E 121316 16181B 262A2F ECEEF1 B6BBC2 858B94 5D636C 5B8AF5 9DB8FF 2EC27E 96E0BE E5403A 6094FF 2FE8C6 15171A 15171A 000000"), // Oscuro
    Tokens("000000 0A0A0B 0F1012 202328 F2F3F5 BEC2C8 8A9099 5F656E 5B8AF5 9DB8FF 2EC27E 96E0BE EA4A43 6094FF 2FE8C6 0F1012 0C0D0F 000000"), // Negro
    Tokens("27292C 303236 36393D 494C52 F1F2F4 CDD0D5 A9AEB5 8B9098 86A9FA B8CCFF 52D197 A8E7CB F47E76 C9D2DE 7FA5FF 3A3D42 34373B 121315"), // Grafito
    Tokens("0A1020 10192D 151F37 24304B E9EEF8 B4BFD3 8794AE 5F6C88 7DA4FF AEC6FF 3FCF8E A0E4C6 F26A63 3A78FF 8C6BFF 141E36 121B30 02050C"), // Medianoche
    Tokens("100D15 17131E 1C1825 2D2738 EFEBF4 BEB6CA 9088A1 6B6380 6F96FF A6BEFF 37C886 9DE2C2 EE5F58 A57BFF F27BB5 2A2433 1A1622 040206"), // Ciruela
    Tokens("F3F4F6 FFFFFF F7F8F9 DADDE1 171A20 393C42 5B5F66 72767D 3A66DC 2F57C9 0F8A5F 0B7350 CC1F24 2751CC 0A9781 E3E5E8 FFFFFF 171A20"), // Claro
    Tokens("EEEFEF F8F9F9 F2F3F3 D3D6D9 141A2A 343B4D 515A6B 6A7282 2F5DC4 284FA8 1C7A4F 166541 B3302A 2F5DC4 6A7282 C3C8CF F8F9F9 141A2A"), // Papel
    Tokens("E6EBF1 F5F7FA EDF1F5 CED6E0 0E1626 323F58 4C5973 66728B 2E5ED0 254FB4 0E7F57 0A6B49 C0242A 3B6EE0 7FA6FF D3DBE5 F5F7FA 0E1626"), // Niebla
    Tokens("D8DBDF E8EAED E1E4E7 BFC4CA 0D1015 2A2F36 434952 5B616A 2A55BF 22479F 0A6B45 07593A A81E22 FFFFFF B4C2D6 C8CCD1 EDEFF1 0D1015"), // Aluminio
    Tokens("F4EDF0 FDF9FA F8F2F4 E2D5DA 1E1419 45343F 65525E 7E6A76 3A62D6 2D51B8 0D7F55 0A6A47 C21F2F D9568F 8E6FE8 DCCDD4 FDF9FA 1E1419"), // Cuarzo
    Tokens("000000 0E0F11 16181B 8A8F96 FFFFFF E8EAED C9CDD3 A3A8AF 84ADFF B8CFFF 45E096 A8F0CC FF7A72 000000 000000 26292D 0E0F11 000000"), // Exterior oscuro
    Tokens("FFFFFF FFFFFF F0F1F3 84898F 050608 1C1F23 34383E 52565C 1A47C2 133A9F 00603F 00573A A80F1A 1A47C2 006B5C E2E4E7 FFFFFF 000000"), // Exterior
)

/** Lo mismo con Más contraste: textos, bordes y rejilla hacia la tinta. Calculado aquí, no en cada cuadro */
private val TABLA_CONTRASTE = arrayOf(
    Tokens("0B0C0E 121316 16181B 65686C F7F8F9 D6DADE B3B7BC 8B9096 5B8AF5 9DB8FF 2EC27E 96E0BE E5403A 6094FF 2FE8C6 232427 15171A 000000"), // Oscuro
    Tokens("000000 0A0A0B 0F1012 616467 FAFAFB DCDEE2 B7BAC0 8E9298 5B8AF5 9DB8FF 2EC27E 96E0BE EA4A43 6094FF 2FE8C6 1D1E20 0C0D0F 000000"), // Negro
    Tokens("27292C 303236 36393D 7E8085 F9FAFB E3E5E8 C9CCD1 ACB0B6 86A9FA B8CCFF 52D197 A8E7CB F47E76 C9D2DE 7FA5FF 45484D 34373B 121315"), // Grafito
    Tokens("0A1020 10192D 151F37 636C80 F6F8FC D5DCE8 B3BCCD 8C96AB 7DA4FF AEC6FF 3FCF8E A0E4C6 F26A63 3A78FF 8C6BFF 222B42 121B30 02050C"), // Medianoche
    Tokens("100D15 17131E 1C1825 6A6572 F9F7FB DCD6E2 BAB4C5 968FA5 6F96FF A6BEFF 37C886 9DE2C2 EE5F58 A57BFF F27BB5 36313F 1A1622 040206"), // Ciruela
    Tokens("F3F4F6 FFFFFF F7F8F9 9B9EA1 090A0D 212328 3A3D42 52565B 3A66DC 2F57C9 0F8A5F 0B7350 CC1F24 2751CC 0A9781 D6D8DB FFFFFF 171A20"), // Claro
    Tokens("EEEFEF F8F9F9 F2F3F3 96999D 080A11 1E222F 343A47 4D5360 2F5DC4 284FA8 1C7A4F 166541 B3302A 2F5DC4 6A7282 B8BDC4 F8F9F9 141A2A"), // Papel
    Tokens("E6EBF1 F5F7FA EDF1F5 9298A1 06090F 1C2434 30394B 495266 2E5ED0 254FB4 0E7F57 0A6B49 C0242A 3B6EE0 7FA6FF C7CED8 F5F7FA 0E1626"), // Niebla
    Tokens("D8DBDF E8EAED E1E4E7 878B90 050608 181A1F 2A2E34 41464D 2A55BF 22479F 0A6B45 07593A A81E22 FFFFFF B4C2D6 BCC0C5 EDEFF1 0D1015"), // Aluminio
    Tokens("F4EDF0 FDF9FA F8F2F4 A2989C 0C080A 281E24 41343C 5C4D56 3A62D6 2D51B8 0D7F55 0A6A47 C21F2F D9568F 8E6FE8 D0C1C8 FDF9FA 1E1419"), // Cuarzo
    // Los dos Exterior ya son el máximo: repiten su fila
    TABLA[Juego.EXTERIOR_OSCURO.ordinal],
    TABLA[Juego.EXTERIOR.ordinal],
)

/** Los tokens de un juego; con `contraste`, los de Más contraste, como los pinta [Paleta] */
internal fun tokensDe(j: Juego, contraste: Boolean = false): Tokens = (if (contraste) TABLA_CONTRASTE else TABLA)[j.ordinal]

/**
 * La paleta Acero de la web (src/estilos/tema.css) en sus doce juegos. Cada
 * color es un getter que lee el juego actual, así que se recompone o se
 * redibuja solo donde se use. El único azul sigue siendo lo inscribible, y
 * los estados son los tonos profundos en los juegos claros (los vivos del
 * oscuro se lavan sobre blanco).
 */
object Paleta {
    private val t get() = (if (Tema.contraste) TABLA_CONTRASTE else TABLA)[Tema.juego.ordinal]

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
    /** La segunda luz de la aurora: turquesa en Oscuro, rosa en Ciruela, plata en Aluminio */
    val aurora2 get() = t.aurora2

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
