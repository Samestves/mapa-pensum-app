package mpensum.portada

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mpensum.carrera.BotonCerrar
import mpensum.carrera.BotonRedondo
import mpensum.carrera.HojaInferior
import mpensum.carrera.IconosHoja
import mpensum.carrera.estiloTitulo
import mpensum.carrera.versal
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Silueta
import mpensum.componentes.iconoCarrera
import androidx.compose.foundation.isSystemInDarkTheme
import mpensum.componentes.esApaisado
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.datos.TIPOS
import mpensum.plataforma.versionApp

import mpensum.tema.Espacio
import mpensum.tema.Tipo
import mpensum.componentes.margenPantalla
import mpensum.carrera.empujeDePagina
import mpensum.tema.Movimiento
import mpensum.tema.Paleta
import mpensum.tema.Radio
import mpensum.tema.Tema
import mpensum.tema.colorDe
import mpensum.tema.inter
import mpensum.tema.jost

/*
 * "Ajustes y ayuda", lo que abre el engranaje (MenuPortada.jsx): lo que vale
 * para toda la app y se usa poco, por eso no está a la vista. Lo de cada
 * carrera (su avance, su carga, reiniciarla) vive en la hoja de avance, dentro
 * de ella; aquí solo:
 *
 *   - Tu carrera: cuál sale arriba en la portada.
 *   - Personalizar: tema, acento, texto y movimiento (subpantalla con tres páginas: Mapa, Inicio y horario, Accesibilidad).
 *   - Ayuda: la guía otra vez y reportar un error.
 *   - El proyecto: la web y el código.
 *   - Al pie, la versión, para saber qué tienes instalado al reportar.
 *
 * Filas de cristal como las de la portada, con etiquetas en versales de 11.
 * La flecha › es lo que se queda en la app; ↗, lo que abre el navegador.
 */

private const val REPO = "https://github.com/Samestves/mapa-pensum"
private const val WEB = "https://mapa-pensum.vercel.app"

/** El alto de una fila de cristal: crece si el subtítulo baja a dos líneas */
private val ALTO_FILA = 72.dp

/** Lo que enseña la hoja: el menú, o la lista para elegir tu carrera */
enum class PaginaAjustes { MENU, CARRERA, PERSONALIZAR, MAPA, INICIO, ACCESIBILIDAD }

@Composable
fun BoxScope.HojaAjustes(
    pagina: PaginaAjustes?,
    carreras: List<Carrera>,
    propia: Carrera?,
    alCerrar: () -> Unit,
    alElegir: (Carrera) -> Unit,
    alVerGuia: () -> Unit,
) {
    val nocheDelSistema = isSystemInDarkTheme()
    // La hoja siempre está compuesta (la portada solo la abre y la cierra), así que sus remember viven
    // entre aperturas. Cada apertura cuenta una vez: la página pedida manda aunque sea la misma de la vez
    // anterior, y no se arrastra la subpágina donde se cerró (mismo patrón que HojaAvance)
    val abierta = pagina != null
    var apertura by remember { mutableIntStateOf(0) }
    var estabaAbierta by remember { mutableStateOf(abierta) }
    // La página pedida al abrir: decide si la cabecera ofrece volver al menú
    var ultima by remember { mutableStateOf(pagina ?: PaginaAjustes.MENU) }
    // La que se ve; mientras la hoja baja sigue pintándose la última que hubo
    var actual by remember { mutableStateOf(ultima) }
    // Al abrir, o si piden otra página con la hoja abierta, se parte de la pedida; lo demás lo mueve la
    // navegación interna (volver, entrar en una fila)
    if (pagina != null && (!estabaAbierta || pagina != ultima)) {
        ultima = pagina
        actual = pagina
    }
    if (abierta != estabaAbierta) {
        estabaAbierta = abierta
        if (abierta) apertura++
    }
    val enlaces = LocalUriHandler.current
    // Qué ranura de Automático se edita: vive aquí para sobrevivir a entrar y salir de las subpáginas
    val ranuras = remember { Ranuras(noche = nocheDelSistema) }
    // Si el sistema pasa a noche o a día con la hoja montada, la ranura que se edita lo sigue. No se rehace
    // el objeto: así no se olvida que ya se tocó el tema de día
    LaunchedEffect(nocheDelSistema) { ranuras.noche = nocheDelSistema }
    // Un solo reloj para la cabecera y el cuerpo: cambian de página a la vez, y la vista previa llega con su
    // página. Uno nuevo en cada apertura, para que la hoja suba ya en su página, sin el empuje desde la vieja
    val transicion = key(apertura) { updateTransition(actual, label = "ajustes") }
    val desplazoPx = with(LocalDensity.current) { Movimiento.DESPLAZO.roundToPx() }
    val empuje: AnimatedContentTransitionScope<PaginaAjustes>.() -> ContentTransform = {
        empujeDePagina(targetState.ordinal > initialState.ordinal, desplazoPx, animarAlto = true)
    }
    HojaInferior(
        abierta = abierta,
        alCerrar = alCerrar,
        // Acostado, las dos columnas piden más que los 560 dp de siempre
        cabecera = {
          transicion.AnimatedContent(transitionSpec = empuje, contentAlignment = Alignment.TopStart) { p ->
          Column {
            // El título de la hoja; las páginas internas llevan encima «AJUSTES» para saber de dónde se vino
            Row(Modifier.fillMaxWidth().padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = if (esApaisado()) Espacio.xs else Espacio.m), verticalAlignment = Alignment.CenterVertically) {
                // Las subpáginas de Personalizar siempre vuelven a ella; las demás, solo si se llegó desde el menú
                val conVolver = p.esDePersonalizar && p != PaginaAjustes.PERSONALIZAR || (p != PaginaAjustes.MENU && ultima == PaginaAjustes.MENU)
                if (conVolver) {
                    val destino = if (p.esDePersonalizar && p != PaginaAjustes.PERSONALIZAR) PaginaAjustes.PERSONALIZAR else PaginaAjustes.MENU
                    Box(Modifier.padding(end = Espacio.xs)) { BotonRedondo(Lucide.atras, "Volver") { actual = destino } }
                }
                Column(Modifier.weight(1f)) {
                    // El rótulo ocupa su sitio también en el menú (transparente): así la cabecera no cambia de alto al entrar en una página
                    val rotulo = when {
                        p == PaginaAjustes.MENU -> " "
                        p.esDePersonalizar && p != PaginaAjustes.PERSONALIZAR -> "PERSONALIZAR"
                        else -> "AJUSTES"
                    }
                    BasicText(rotulo, style = versal(if (p == PaginaAjustes.MENU) Color.Transparent else Paleta.tenue))
                    BasicText(
                        when (p) {
                            PaginaAjustes.CARRERA -> "Elige tu carrera"
                            PaginaAjustes.PERSONALIZAR -> "Personalizar"
                            PaginaAjustes.MAPA -> "Mapa"
                            PaginaAjustes.INICIO -> "Inicio y horario"
                            PaginaAjustes.ACCESIBILIDAD -> "Accesibilidad"
                            PaginaAjustes.MENU -> "Ajustes"
                        },
                        Modifier.padding(top = Espacio.xxs),
                        style = estiloTitulo(),
                    )
                }
                BotonCerrar(alCerrar)
            }
          }
          }
        },
        // Acostado la hoja nunca desplaza: cada página trae su propio desplazamiento. Si dependiera de la
        // página actual, al volver de Personalizar la hoja volvería a desplazar mientras esa página aún
        // sale con su columna desplazable dentro, y un desplazamiento dentro de otro no se puede medir
        desplazable = !esApaisado(),
    ) {
        // Acostado, las cuatro páginas de Personalizar son una sola: el índice no se mueve y cambia la columna de la derecha
        val apaisado = esApaisado()
        transicion.AnimatedContent(
            transitionSpec = empuje,
            contentAlignment = Alignment.TopStart,
            contentKey = { if (apaisado && it.esDePersonalizar) PaginaAjustes.PERSONALIZAR else it },
        ) { p ->
          // Personalizar acostado reparte su alto y desplaza su columna derecha; las demás desplazan enteras
          Column(if (apaisado && !p.esDePersonalizar) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier) {
            if (p == PaginaAjustes.CARRERA) {
                ListaCarreras(carreras, propia) { c ->
                    alElegir(c)
                    alCerrar()
                }
            } else if (p.esDePersonalizar) {
                Personalizar(p, propia, ranuras) { actual = it }
            } else {
                val tuCarrera = { actual = PaginaAjustes.CARRERA }
                val personalizar = { actual = PaginaAjustes.PERSONALIZAR }
                val guia = {
                    alCerrar()
                    alVerGuia()
                }
                val reporte = { abrir(enlaces, enlaceReporte(propia?.nombre)) }
                val web = { abrir(enlaces, WEB) }
                val codigo = { abrir(enlaces, REPO) }
                if (esApaisado()) {
                    // Acostado, dos filas que se leen como una rejilla: lo tuyo junto a la ayuda arriba, el
                    // proyecto a todo el ancho debajo y la versión al pie. Cada bloque abre con su rótulo,
                    // así las dos columnas empiezan a la misma altura
                    val rotuloArriba = Modifier.padding(start = Espacio.xxs, bottom = Espacio.s)
                    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.l)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Column(Modifier.weight(1f)) {
                                Etiqueta("TU MAPA", rotuloArriba)
                                FilaCarrera(propia, tuCarrera)
                                Spacer(Modifier.height(Espacio.xs))
                                FilaPersonalizar(personalizar)
                            }
                            Column(Modifier.weight(1f)) {
                                BloqueAyuda(guia, reporte, rotuloArriba)
                            }
                        }
                        Etiqueta("EL PROYECTO", Modifier.padding(start = Espacio.xxs, top = Espacio.xl, bottom = Espacio.s))
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Box(Modifier.weight(1f)) { Renglon(Lucide.mundo, "Abrir la versión web", "mapa-pensum.vercel.app", fuera = true, alPulsar = web) }
                            Box(Modifier.weight(1f)) { Renglon(Lucide.codigo, "Código abierto", "GitHub", fuera = true, alPulsar = codigo) }
                        }
                        Version(Modifier.padding(top = Espacio.xl))
                    }
                } else {
                    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.xl)) {
                        FilaCarrera(propia, tuCarrera)
                        Spacer(Modifier.height(Espacio.xs))
                        FilaPersonalizar(personalizar)
                        BloqueAyuda(guia, reporte)
                        BloqueProyecto(web, codigo)
                        Version(Modifier.padding(top = 28.dp))
                    }
                }
            }
          }
        }
    }
}

/** La fila que abre Personalizar, con la mini vista del tema y el resumen de lo elegido */
@Composable
private fun FilaPersonalizar(alPulsar: () -> Unit) {
    FilaCristal(alPulsar) {
        TeclaIcono(Lucide.muestrario, Paleta.suave)
        Textos("Personalizar", resumenPersonalizar(), Modifier.padding(start = Espacio.m, end = Espacio.s).weight(1f))
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

@Composable
private fun BloqueAyuda(alVerGuia: () -> Unit, alReportar: () -> Unit, etiqueta: Modifier = Modifier.padding(start = Espacio.xxs, top = 28.dp, bottom = Espacio.s)) {
    Etiqueta("AYUDA", etiqueta)
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Renglon(Lucide.ayuda, "Ver la guía", null, alPulsar = alVerGuia)
        Renglon(Lucide.bandera, "Reportar un error", null, fuera = true, alPulsar = alReportar)
    }
}

@Composable
private fun BloqueProyecto(alAbrirWeb: () -> Unit, alAbrirCodigo: () -> Unit) {
    Etiqueta("EL PROYECTO")
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Renglon(Lucide.mundo, "Abrir la versión web", "mapa-pensum.vercel.app", fuera = true, alPulsar = alAbrirWeb)
        Renglon(Lucide.codigo, "Código abierto", "GitHub", fuera = true, alPulsar = alAbrirCodigo)
    }
}

/** Al pie, para saber qué tienes instalado al reportar */
@Composable
private fun Version(modifier: Modifier) {
    versionApp()?.let {
        BasicText("VERSIÓN $it", modifier.fillMaxWidth(), style = versal(Paleta.apagada).copy(textAlign = TextAlign.Center))
    }
}

/** Sin navegador instalado (o con uno deshabilitado) openUri lanza; un enlace que no abre no debe tumbar la app */
private fun abrir(enlaces: UriHandler, uri: String) {
    runCatching { enlaces.openUri(uri) }
}

/** La etiqueta de cada bloque, en versales: 28 dp sobre ella, 10 debajo */
@Composable
internal fun Etiqueta(texto: String, modifier: Modifier = Modifier.padding(start = Espacio.xxs, top = 28.dp, bottom = Espacio.s)) {
    BasicText(texto, modifier, style = versal(Paleta.tenue))
}

/**
 * Una fila de cristal, la misma de "Otras carreras" en la portada: 72 de
 * alto, un velo de tinta casi nada, su filo de 1 px y radio 16.
 */
@Composable
internal fun FilaCristal(alPulsar: () -> Unit, contenido: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_FILA)
            .pulsable(alPulsar, escala = 0.98f)
            .drawBehind {
                val r = 16.dp.toPx()
                val g = Tema.grosorBorde.dp.toPx()
                drawRoundRect(Paleta.tinta.copy(alpha = 0.035f), cornerRadius = CornerRadius(r))
                drawRoundRect(Paleta.tinta.copy(alpha = 0.065f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r - g / 2), style = Stroke(g))
            }
            .padding(start = Espacio.m, end = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido,
    )
}

/** Lo que hace la fila en Jost, como los nombres de la portada, y su porqué debajo en Inter */
@Composable
internal fun Textos(titulo: String, texto: String?, modifier: Modifier) {
    Column(modifier) {
        BasicText(
            titulo,
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (texto != null) BasicText(texto, Modifier.padding(top = Espacio.hilo), style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.tenue), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** La tecla de siempre: 40×40, radio 12, fondo de tinta al 6 % (o el que se pida) y el icono de 20 con trazo 1,75 */
@Composable
private fun TeclaIcono(icono: List<Path>, color: androidx.compose.ui.graphics.Color, fondo: androidx.compose.ui.graphics.Color = Paleta.tinta.copy(alpha = 0.06f)) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(Radio.Chip)).background(fondo), contentAlignment = Alignment.Center) {
        IconoTrazo(icono, color, 20.dp, 1.75f)
    }
}

/** Un renglón: su icono en una tecla, lo que hace y la flecha (› si se queda en la app, ↗ si sale) */
@Composable
private fun Renglon(icono: List<Path>, titulo: String, texto: String?, fuera: Boolean = false, alPulsar: () -> Unit) {
    FilaCristal(alPulsar) {
        TeclaIcono(icono, Paleta.suave)
        Textos(titulo, texto, Modifier.padding(start = Espacio.m, end = Espacio.s).weight(1f))
        IconoTrazo(if (fuera) IconosHoja.fuera else Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

/**
 * Tu carrera: su silueta en su color, como en la portada, y su nombre. Lo
 * primero de la hoja porque es lo único que cambia lo que ves al volver.
 */
@Composable
private fun FilaCarrera(propia: Carrera?, alPulsar: () -> Unit) {
    FilaCristal(alPulsar) {
        // Su icono en tinta neutra: el color de la carrera es del acento, no de las filas
        TeclaIcono(iconoCarrera(propia?.slug), Paleta.suave)
        // La etiqueta arriba y el valor debajo, como una ficha: "Tu carrera / Sistemas"
        Column(Modifier.padding(start = Espacio.m, end = Espacio.s).weight(1f)) {
            BasicText("Tu carrera", style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.tenue))
            BasicText(
                propia?.nombreCorto ?: "Elige la tuya",
                Modifier.padding(top = Espacio.hilo),
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicText(if (propia != null) "Cambiar" else "Elegir", Modifier.padding(end = Espacio.xxs), style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, color = Paleta.tenue))
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

/** Elegir tu carrera: cada una en su fila, con su silueta, y la tuya marcada */
@Composable
private fun ListaCarreras(carreras: List<Carrera>, propia: Carrera?, alElegir: (Carrera) -> Unit) {
    Column(Modifier.padding(start = margenPantalla(), end = margenPantalla(), top = Espacio.xxs, bottom = Espacio.xl), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        BasicText(
            "Sale primera en la portada.",
            Modifier.padding(start = Espacio.xxs, end = Espacio.xxs, bottom = Espacio.xs),
            style = TextStyle(fontFamily = inter(), fontSize = Tipo.s, lineHeight = 18.sp, color = Paleta.tenue),
        )
        // En el orden de la portada: ingenierías, licenciaturas y gerencia
        carreras.sortedBy { TIPOS.indexOf(it.tipo) }.forEach { c ->
            val elegida = c.slug == propia?.slug
            FilaCristal({ alElegir(c) }) {
                Box(Modifier.size(56.dp, 36.dp)) {
                    Silueta(
                        c.silueta, colorDe(c.color).copy(alpha = if (elegida) 1f else 0.7f),
                        hechas = c.silueta,
                        punto = 2.5.dp,
                        hueco = (56f / c.silueta.size - 2.5f).dp,
                        huecoFila = (36f / 7f - 2.5f).dp,
                    )
                }
                Column(Modifier.padding(start = Espacio.m, end = Espacio.s).weight(1f)) {
                    BasicText(
                        c.nombreCorto,
                        style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = Tipo.xl, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(c.tipo.uppercase(), Modifier.padding(top = Espacio.xxs), style = versal(Paleta.tenue), maxLines = 1)
                }
                if (elegida) IconoTrazo(Lucide.listo, Paleta.tinta, 20.dp, 2.2f)
            }
        }
    }
}

/**
 * Un reporte que solo dice "hay un error en Sistemas" no se puede arreglar:
 * el issue de GitHub se abre con el formulario ya redactado (data/proyecto.js).
 */
private fun enlaceReporte(carrera: String?): String {
    val cuerpo = listOf(
        "**Carrera:** ${carrera ?: "(¿cuál?)"}", "",
        "**Materia:** (código y nombre, si aplica)", "",
        "**Qué está mal o qué falta**", "", "",
        "**Qué dice el pensum oficial**", "", "",
        "**De dónde lo sacas** (captura, PDF de la DACE, control de estudios…)", "",
    ).joinToString("\n")
    val titulo = if (carrera != null) "Corrección en $carrera" else "Corrección en los datos"
    return "$REPO/issues/new?title=${codificar(titulo)}&body=${codificar(cuerpo)}"
}

/** Codifica un texto para la URL, byte a byte en UTF-8 */
private fun codificar(texto: String): String = buildString {
    texto.encodeToByteArray().forEach { b ->
        val c = b.toInt().toChar()
        if (c.isLetterOrDigit() && b >= 0 || c in "-_.~") append(c)
        else append('%').append(((b.toInt() and 0xFF) + 0x100).toString(16).substring(1).uppercase())
    }
}
