package mpensum.portada

import mpensum.plataforma.Apariencia
import mpensum.tema.Juego
import mpensum.tema.pareja

/**
 * Lo que hay que guardar al elegir un juego en la paleta de Personalizar. Cada campo null es «no tocar».
 * Es la regla de Sistema con su pareja, sin escribir en ningún sitio: la interfaz solo la aplica.
 */
internal class EleccionDeTema(
    val tema: Apariencia? = null,
    val dia: Juego? = null,
    val noche: Juego? = null,
    /** Si «De día» queda elegido a mano: entonces elegir «De noche» ya no le propone su pareja */
    val diaTocado: Boolean,
)

/**
 * Elegir en la paleta: el tema, o el de la ranura activa si hay Automático (Sistema). Elegir «De noche» un
 * tema oscuro propone su pareja para «De día», si ese no se ha tocado.
 */
internal fun elegirJuego(juego: Juego, apariencia: Apariencia, ranuraNoche: Boolean, diaTocado: Boolean): EleccionDeTema =
    if (apariencia != Apariencia.SISTEMA) EleccionDeTema(tema = aparienciaDe(juego), diaTocado = diaTocado)
    else if (ranuraNoche) EleccionDeTema(noche = juego, dia = if (!diaTocado && !juego.claro) juego.pareja else null, diaTocado = diaTocado)
    else EleccionDeTema(dia = juego, diaTocado = true)

internal fun aparienciaDe(juego: Juego): Apariencia = Apariencia.entries.first { it.juego == juego }
