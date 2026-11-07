package mpensum

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import mpensum.bienvenida.Bienvenida
import mpensum.carrera.Interior
import mpensum.carrera.Vista
import mpensum.componentes.LocalVentana
import mpensum.componentes.ProveerTacto
import mpensum.componentes.medirVentana
import mpensum.datos.rememberCarreras
import mpensum.plataforma.BarrasDelSistema
import mpensum.plataforma.ManejarAtras
import mpensum.plataforma.Preferencias
import mpensum.plataforma.animacionesQuitadas
import mpensum.plataforma.telefonoModesto
import mpensum.portada.Portada
import mpensum.tema.Paleta
import mpensum.tema.Tema
import mpensum.tema.LocalFuentes
import mpensum.tema.TemaVivo
import mpensum.tema.rememberFuentes

/** Las tres pantallas de la app; dentro de una carrera, sus vistas */
private sealed interface Pantalla {
    data object Bienvenida : Pantalla
    data object Portada : Pantalla
    data class Carrera(val slug: String) : Pantalla
}

/**
 * La pantalla como texto, para que sobreviva a que Android recree la actividad
 * (cambio de tema, de idioma o de tamaño de letra con la app abierta): sin
 * esto, quien estaba dentro de su carrera volvía a la portada.
 */
private val GuardarPantalla = Saver<Pantalla, String>(
    save = {
        when (it) {
            Pantalla.Bienvenida -> "bienvenida"
            Pantalla.Portada -> "portada"
            is Pantalla.Carrera -> "carrera:${it.slug}"
        }
    },
    restore = {
        when {
            it == "bienvenida" -> Pantalla.Bienvenida
            it.startsWith("carrera:") -> Pantalla.Carrera(it.removePrefix("carrera:"))
            else -> Pantalla.Portada
        }
    },
)

/** Lo que cambia sin salir de pantalla arranca quieto y frena largo (Material, enfatizada) */
private val ENFATICA = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * La primera vez, la guía; después, la portada. La guía acaba en la portada
 * con la carrera elegida ya puesta como «Tu carrera»: elegir es configurar, y
 * entrar al mapa es un gesto aparte (el toque en la tarjeta).
 *
 * Entrar es ir hacia dentro: el mapa nace desde la tarjeta o el mosaico que
 * tocaste y, al volver, se recoge sobre él. La portada guarda su estado
 * (hasta dónde bajaste) mientras estás dentro.
 */
@Composable
fun App() {
    // Una sola apariencia para toda la app; «Sistema» sigue al teléfono mientras la app está abierta
    val apariencia by Preferencias.tema.collectAsState()
    val nocheDelSistema = isSystemInDarkTheme()
    val deDia by Preferencias.temaDia.collectAsState()
    val deNoche by Preferencias.temaNoche.collectAsState()
    val juego = apariencia.juego(nocheDelSistema, deDia, deNoche)
    // El resto de lo elegido en Personalizar se vuelca al Tema al componer: lo que dibuja lo lee de ahí
    val acento by Preferencias.acento.collectAsState()
    val grande by Preferencias.textoGrande.collectAsState()
    val luz by Preferencias.luzFondo.collectAsState()
    val reducir by Preferencias.reducirMovimiento.collectAsState()
    val codigos by Preferencias.codigos.collectAsState()
    val ligero by Preferencias.modoLigero.collectAsState()
    val fondoMapa by Preferencias.fondoMapa.collectAsState()
    val cables by Preferencias.cables.collectAsState()
    val grosor by Preferencias.grosorCables.collectAsState()
    val atenuar by Preferencias.atenuarAprobadas.collectAsState()
    val contraste by Preferencias.contraste.collectAsState()
    val hora24 by Preferencias.hora24.collectAsState()
    val vibracion by Preferencias.vibracion.collectAsState()
    Tema.contraste = contraste
    Tema.fondo = fondoMapa ?: juego.fondo
    Tema.cables = cables
    Tema.grosor = grosor
    Tema.atenuarAprobadas = atenuar
    Tema.hora24 = hora24
    Tema.vibracion = vibracion
    Tema.acento = acento
    Tema.textoGrande = grande
    Tema.luzFondo = luz ?: (juego.aurora > 0f)
    Tema.movimiento = reducir ?: animacionesQuitadas()
    Tema.codigos = codigos
    Tema.ligero = ligero ?: telefonoModesto()
    BarrasDelSistema(juego.claro)
    // El texto grande es la escala de letra del sistema por 1,15: se lee en sp, no toca los dp
    val densidad = LocalDensity.current
    val escala = if (grande) 1.15f else 1f
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, densidad.fontScale * escala), LocalFuentes provides rememberFuentes()) {
        // La ventana se mide una vez aquí, ya con la letra final, y las pantallas la leen con ventana()
        CompositionLocalProvider(LocalVentana provides medirVentana()) {
            ProveerTacto { TemaVivo(juego) { Pantallas() } }
        }
    }
}

/**
 * Con qué pantalla abre la app: la guía la primera vez; después la portada o,
 * si se pidió «Mi carrera» y hay una elegida, directo dentro de ella (Atrás
 * vuelve a la portada). Una carrera que el índice ya no trae vuelve sola a la portada.
 */
private fun pantallaDeArranque(): Pantalla {
    if (!Preferencias.bienvenidaVista()) return Pantalla.Bienvenida
    val propia = Preferencias.carrera()
    return if (Preferencias.abrirEnCarrera.value && propia != null) Pantalla.Carrera(propia) else Pantalla.Portada
}

@Composable
private fun Pantallas() {
    val carreras = rememberCarreras()
    var pantalla by rememberSaveable(stateSaver = GuardarPantalla) {
        mutableStateOf<Pantalla>(pantallaDeArranque())
    }
    var propia by remember { mutableStateOf(Preferencias.carrera()) }
    var origen by remember { mutableStateOf(TransformOrigin.Center) }
    // La vista con la que nace la carrera que se abre (el atajo del horario); null es el mapa
    var vistaInicial by remember { mutableStateOf<Vista?>(null) }
    val guardado = rememberSaveableStateHolder()

    ManejarAtras(pantalla is Pantalla.Carrera || (pantalla == Pantalla.Bienvenida && Preferencias.bienvenidaVista())) {
        pantalla = Pantalla.Portada
    }

    Box(Modifier.fillMaxSize().background(Paleta.lienzo)) {
        // El índice se lee del paquete en un instante; mientras, solo el lienzo
        if (carreras.isEmpty()) return@Box
        AnimatedContent(
            targetState = pantalla,
            transitionSpec = {
                when {
                    // Con el movimiento reducido, un fundido de 150 ms en vez de entrar y salir con escala
                    Tema.movimiento -> fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    targetState is Pantalla.Carrera ->
                        (scaleIn(tween(420, easing = ENFATICA), 0.9f, origen) + fadeIn(tween(200, 40, LinearEasing))) togetherWith
                            (scaleOut(tween(420, easing = ENFATICA), 1.06f, origen) + fadeOut(tween(180, easing = LinearEasing)))
                    initialState is Pantalla.Carrera ->
                        (scaleIn(tween(380, easing = ENFATICA), 1.06f, origen) + fadeIn(tween(200, easing = LinearEasing))) togetherWith
                            (scaleOut(tween(380, easing = ENFATICA), 0.9f, origen) + fadeOut(tween(180, 60, LinearEasing)))
                    // De la guía a la portada: la guía se disuelve y la portada nace bajo ella, que hace su propia entrada
                    targetState == Pantalla.Portada -> fadeIn(tween(360, 80, ENFATICA)) togetherWith fadeOut(tween(320, easing = LinearEasing))
                    else -> fadeIn(tween(500, 120)) togetherWith fadeOut(tween(200))
                }
            },
            label = "pantalla",
        ) { p ->
            when (p) {
                Pantalla.Bienvenida -> Bienvenida(carreras) { elegida ->
                    Preferencias.marcarBienvenidaVista()
                    if (elegida != null) {
                        Preferencias.guardarCarrera(elegida.slug)
                        propia = elegida.slug
                    }
                    origen = TransformOrigin.Center
                    // La guía termina en la portada, con «Tu carrera» ya puesta; el mapa queda a un toque
                    pantalla = Pantalla.Portada
                }
                Pantalla.Portada -> guardado.SaveableStateProvider("portada") {
                    Portada(
                        carreras,
                        propia = carreras.find { it.slug == propia },
                        alAbrir = { c, desde, vista ->
                            origen = desde
                            vistaInicial = vista
                            pantalla = Pantalla.Carrera(c.slug)
                        },
                        alVerGuia = { pantalla = Pantalla.Bienvenida },
                        alElegirPropia = { c ->
                            Preferencias.guardarCarrera(c.slug)
                            propia = c.slug
                        },
                    )
                }
                is Pantalla.Carrera -> {
                    val carrera = carreras.firstOrNull { it.slug == p.slug }
                    // Una carrera que el índice ya no trae (un estado guardado de otra versión) no tiene a dónde entrar
                    if (carrera == null) LaunchedEffect(Unit) { pantalla = Pantalla.Portada }
                    else Interior(carrera, alVolver = { pantalla = Pantalla.Portada }, vistaInicial = vistaInicial ?: Preferencias.vistaInicial.value)
                }
            }
        }
    }
}
