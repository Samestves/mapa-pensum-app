package mpensum.plataforma

import androidx.compose.runtime.Composable
import mpensum.datos.Asignatura
import mpensum.logica.LectorCaptura

/**
 * El lector real de capturas de la plataforma: galería del sistema, imagen
 * reducida y lectura en el servidor de la web. null si el entorno no puede
 * abrir la galería; entonces «Subir mi horario» lleva a crearlo a mano.
 */
@Composable
expect fun rememberLectorCaptura(materias: List<Asignatura>): LectorCaptura?
