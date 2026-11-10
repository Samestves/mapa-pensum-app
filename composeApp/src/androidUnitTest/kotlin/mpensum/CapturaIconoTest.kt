package mpensum

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.github.takahirom.roborazzi.captureRoboImage
import com.mapapensum.app.R
import mpensum.componentes.LogoMosaico
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * El icono del teléfono pintado desde sus recursos de verdad (el fondo y el
 * mapa del icono adaptable). De izquierda a derecha: las capas enteras de
 * 108 dp con el círculo seguro de 66 marcado, el icono a 56 dp con máscara
 * redonda y con la de squircle, el monocromo de Android 13 teñido, y el
 * mosaico de la cabecera a 34 dp, que debe verse como el mismo objeto.
 * Va a build/capturas/icono-lanzador.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w420dp-h844dp-xxhdpi")
class CapturaIconoTest {
    @get:Rule
    val regla = createComposeRule()

    /** Una capa del icono (108 dp) pintada a `px` de lado */
    private fun capa(id: Int, px: Int): Bitmap {
        val res = RuntimeEnvironment.getApplication().resources
        val d = ResourcesCompat.getDrawable(res, id, null)!!
        return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also { b ->
            d.setBounds(0, 0, px, px)
            d.draw(Canvas(b))
        }
    }

    /** El fondo y el mapa juntos */
    private fun icono(px: Int): Bitmap = capa(R.drawable.ic_launcher_background, px).also { b ->
        Canvas(b).drawBitmap(capa(R.drawable.ic_launcher_foreground, px), 0f, 0f, null)
    }

    @Test
    fun lanzador() {
        System.setProperty("roborazzi.test.record", "true")
        val lado = 108 * 3
        val entero = icono(lado).asImageBitmap()
        val mono = capa(R.drawable.ic_launcher_monochrome, lado).asImageBitmap()
        regla.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF15171B))) {
                for (claro in listOf(false, true)) {
                    Row(
                        Modifier.fillMaxWidth().height(150.dp).background(if (claro) Color(0xFFE9EBEF) else Color(0xFF15171B)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // La máscara del lanzador recorta los 18 dp de cada lado: lo visible son los 72 del centro
                        Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                            Image(entero, null, Modifier.size(108.dp))
                            Box(Modifier.size(66.dp).border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.7f), CircleShape))
                        }
                        Visible(entero, Modifier.size(56.dp).clip(CircleShape))
                        Visible(entero, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
                        Box(Modifier.size(56.dp).clip(CircleShape).background(if (claro) Color(0xFFD3E3FD) else Color(0xFF1F2A3D)), contentAlignment = Alignment.Center) {
                            Image(mono, null, Modifier.requiredSize(56.dp * 108f / 72f), colorFilter = ColorFilter.tint(if (claro) Color(0xFF0B57D0) else Color(0xFFA8C7FA)))
                        }
                        LogoMosaico(34.dp, canto = true)
                    }
                }
            }
        }
        regla.waitForIdle()
        regla.onRoot().captureRoboImage("build/capturas/icono-lanzador.png")
    }

    /** El icono como lo enseña el lanzador: solo los 72 dp del centro, escalados a la máscara */
    @androidx.compose.runtime.Composable
    private fun Visible(img: androidx.compose.ui.graphics.ImageBitmap, modifier: Modifier) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Image(img, null, Modifier.requiredSize(56.dp * 108f / 72f))
        }
    }
}
