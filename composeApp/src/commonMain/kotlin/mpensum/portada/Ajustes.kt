package mpensum.portada

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import mpensum.carrera.HojaInferior
import mpensum.carrera.IconosHoja
import mpensum.carrera.versal
import mpensum.componentes.IconoTrazo
import mpensum.componentes.Lucide
import mpensum.componentes.Silueta
import mpensum.componentes.pulsable
import mpensum.datos.Carrera
import mpensum.datos.TIPOS
import mpensum.plataforma.versionApp
import mpensum.tema.Paleta
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
 *   - Personalizar: tema, acento, texto y movimiento (subpantalla; el tema es el mismo de la hoja de avance).
 *   - Ayuda: la guía otra vez y reportar un error.
 *   - El proyecto: la web y el código.
 *   - Al pie, la versión, para saber qué tienes instalado al reportar.
 *
 * Filas de cristal como las de la portada, con etiquetas en versales de 11.
 * La flecha › es lo que se queda en la app; ↗, lo que abre el navegador.
 */

private const val REPO = "https://github.com/Samestves/mapa-pensum"
private const val WEB = "https://mapa-pensum.vercel.app"

/** Lo que enseña la hoja: el menú, o la lista para elegir tu carrera */
enum class PaginaAjustes { MENU, CARRERA, PERSONALIZAR }

@Composable
fun BoxScope.HojaAjustes(
    pagina: PaginaAjustes?,
    carreras: List<Carrera>,
    propia: Carrera?,
    alCerrar: () -> Unit,
    alElegir: (Carrera) -> Unit,
    alVerGuia: () -> Unit,
) {
    // La última página abierta sigue pintándose mientras la hoja baja
    var ultima by remember { mutableStateOf(pagina ?: PaginaAjustes.MENU) }
    var actual by remember { mutableStateOf(ultima) }
    if (pagina != null && pagina != ultima) {
        ultima = pagina
        actual = pagina
    }
    val enlaces = LocalUriHandler.current
    HojaInferior(
        abierta = pagina != null,
        alCerrar = alCerrar,
        cabecera = {
            // La de la hoja de avance: etiqueta en versales y el título debajo
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                // La flecha de volver solo si se llegó desde el menú; abierta directo en la lista, se cierra y ya
                val conVolver = actual != PaginaAjustes.MENU && ultima == PaginaAjustes.MENU
                if (conVolver) {
                    Box(Modifier.padding(end = 8.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).pulsable({ actual = PaginaAjustes.MENU }), contentAlignment = Alignment.Center) {
                        IconoTrazo(Lucide.atras, Paleta.tinta, 20.dp, 2f)
                    }
                }
                Column(Modifier.weight(1f)) {
                    BasicText(if (actual != PaginaAjustes.MENU) "AJUSTES" else "MAPA DE PENSUM", style = versal(Paleta.tenue))
                    BasicText(
                        when (actual) {
                            PaginaAjustes.CARRERA -> "Elige tu carrera"
                            PaginaAjustes.PERSONALIZAR -> "Personalizar"
                            PaginaAjustes.MENU -> "Ajustes y ayuda"
                        },
                        Modifier.padding(top = 4.dp),
                        style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp, color = Paleta.tinta),
                    )
                }
                BotonCerrar(alCerrar)
            }
        },
    ) {
        AnimatedContent(
            targetState = actual,
            transitionSpec = {
                val adelante = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(320)) { if (adelante) it / 4 else -it / 4 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(280)) { if (adelante) -it / 4 else it / 4 } + fadeOut(tween(160)))
            },
            label = "ajustes",
        ) { p ->
            if (p == PaginaAjustes.CARRERA) {
                ListaCarreras(carreras, propia) { c ->
                    alElegir(c)
                    alCerrar()
                }
            } else if (p == PaginaAjustes.PERSONALIZAR) {
                Personalizar(propia?.let { colorDe(it.color) } ?: Paleta.azul)
            } else {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
                    FilaCarrera(propia) { actual = PaginaAjustes.CARRERA }

                    Spacer(Modifier.height(8.dp))
                    FilaCristal({ actual = PaginaAjustes.PERSONALIZAR }) {
                        MiniVistaTema()
                        Textos("Personalizar", resumenPersonalizar(), Modifier.padding(start = 16.dp, end = 12.dp).weight(1f))
                        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
                    }

                    Etiqueta("AYUDA")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Renglon(Lucide.ayuda, "Ver la guía otra vez", "Cómo funciona, en cuatro pasos") {
                            alCerrar()
                            alVerGuia()
                        }
                        Renglon(Lucide.bandera, "Reportar un error", "Una materia o prelación mal puesta", fuera = true) { abrir(enlaces, enlaceReporte(propia?.nombre)) }
                    }

                    Etiqueta("EL PROYECTO")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Renglon(Lucide.mundo, "Abrir la versión web", "mapa-pensum.vercel.app", fuera = true) { abrir(enlaces, WEB) }
                        Renglon(Lucide.codigo, "Código abierto", "Revísalo en GitHub", fuera = true) { abrir(enlaces, REPO) }
                    }

                    versionApp()?.let {
                        BasicText(
                            "VERSIÓN $it",
                            Modifier.fillMaxWidth().padding(top = 28.dp),
                            style = versal(Paleta.apagada).copy(textAlign = TextAlign.Center),
                        )
                    }
                }
            }
        }
    }
}

/** Sin navegador instalado (o con uno deshabilitado) openUri lanza; un enlace que no abre no debe tumbar la app */
private fun abrir(enlaces: UriHandler, uri: String) {
    runCatching { enlaces.openUri(uri) }
}

/** La etiqueta de cada bloque, en versales: 28 dp sobre ella, 10 debajo */
@Composable
internal fun Etiqueta(texto: String, modifier: Modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 10.dp)) {
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
            .height(72.dp)
            .pulsable(alPulsar, escala = 0.98f)
            .drawBehind {
                val r = 16.dp.toPx()
                val g = Tema.grosorBorde.dp.toPx()
                drawRoundRect(Paleta.tinta.copy(alpha = 0.035f), cornerRadius = CornerRadius(r))
                drawRoundRect(Paleta.tinta.copy(alpha = 0.065f), Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r - g / 2), style = Stroke(g))
            }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido,
    )
}

/** Lo que hace la fila en Jost, como los nombres de la portada, y su porqué debajo en Inter */
@Composable
internal fun Textos(titulo: String, texto: String, modifier: Modifier) {
    Column(modifier) {
        BasicText(
            titulo,
            style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(texto, Modifier.padding(top = 2.dp), style = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.tenue), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Un renglón: su icono en una tecla, lo que hace y la flecha (› si se queda en la app, ↗ si sale) */
@Composable
private fun Renglon(icono: List<Path>, titulo: String, texto: String, fuera: Boolean = false, alPulsar: () -> Unit) {
    FilaCristal(alPulsar) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Paleta.tinta.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            IconoTrazo(icono, Paleta.suave, 20.dp, 1.75f)
        }
        Textos(titulo, texto, Modifier.padding(start = 16.dp, end = 12.dp).weight(1f))
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
        if (propia != null) {
            Box(Modifier.size(56.dp, 36.dp)) {
                Silueta(
                    propia.silueta, colorDe(propia.color),
                    hechas = propia.silueta,
                    punto = 2.5.dp,
                    hueco = (56f / propia.silueta.size - 2.5f).dp,
                    huecoFila = (36f / 7f - 2.5f).dp,
                )
            }
        } else {
            Box(Modifier.size(56.dp, 40.dp), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Paleta.tinta.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    IconoTrazo(Lucide.birrete, Paleta.suave, 20.dp, 1.75f)
                }
            }
        }
        // La etiqueta arriba y el valor debajo, como una ficha: "Tu carrera / Sistemas"
        Column(Modifier.padding(start = 16.dp, end = 12.dp).weight(1f)) {
            BasicText("Tu carrera", style = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.tenue))
            BasicText(
                propia?.nombreCorto ?: "Elige la tuya",
                Modifier.padding(top = 2.dp),
                style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicText(if (propia != null) "Cambiar" else "Elegir", Modifier.padding(end = 4.dp), style = TextStyle(fontFamily = inter(), fontSize = 13.sp, color = Paleta.tenue))
        IconoTrazo(Lucide.siguiente, Paleta.apagada, 16.dp, 1.75f)
    }
}

/** Elegir tu carrera: cada una en su fila, con su silueta, y la tuya marcada */
@Composable
private fun ListaCarreras(carreras: List<Carrera>, propia: Carrera?, alElegir: (Carrera) -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            "La tuya sale primero en la portada, con tu avance. Las demás las abres cuando quieras.",
            Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
            style = TextStyle(fontFamily = inter(), fontSize = 13.sp, lineHeight = 18.sp, color = Paleta.tenue),
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
                Column(Modifier.padding(start = 16.dp, end = 12.dp).weight(1f)) {
                    BasicText(
                        c.nombreCorto,
                        style = TextStyle(fontFamily = jost(), fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em, color = Paleta.tinta),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(c.tipo.uppercase(), Modifier.padding(top = 4.dp), style = versal(Paleta.tenue), maxLines = 1)
                }
                if (elegida) IconoTrazo(Lucide.listo, Paleta.azul, 20.dp, 2.2f)
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
