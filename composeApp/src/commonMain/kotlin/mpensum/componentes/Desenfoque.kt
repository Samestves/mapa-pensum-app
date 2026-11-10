package mpensum.componentes

import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode

/**
 * El desenfoque de las entradas, puesto en la capa: así se anima sin
 * recomponer el texto. Sin efecto cuando ya casi no se nota.
 */
fun desenfoque(radio: Float): BlurEffect? = if (radio > 0.5f) BlurEffect(radio, radio, TileMode.Decal) else null
