package mpensum.plataforma

import androidx.compose.runtime.Composable
import mpensum.datos.Asignatura
import androidx.compose.ui.graphics.ImageBitmap
import mpensum.logica.MateriaLeida

/**
 * El lector real de capturas de la plataforma: galería del sistema, imagen
 * reducida y lectura en el servidor de la web. null si el entorno no puede
 * abrir la galería; entonces «Subir mi horario» lleva a crearlo a mano.
 */
@Composable
expect fun rememberLectorCaptura(materias: List<Asignatura>): LectorCaptura?

/**
 * Quien lee la captura de INTRADACE. `elegir` abre la galería y devuelve la
 * foto (null si se cancela); `leer` avisa primero de cuántas materias ve y
 * después de cada una según la va leyendo, para que el contador sea el real.
 */
interface LectorCaptura {
    suspend fun elegir(): ImageBitmap?
    suspend fun leer(foto: ImageBitmap, alTotal: (Int) -> Unit, alLeer: (MateriaLeida) -> Unit)
}
