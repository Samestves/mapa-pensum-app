package mpensum

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import mpensum.plataforma.aplicarAlTema
import mpensum.plataforma.Preferencias
import mpensum.plataforma.estiloBarras
import mpensum.tema.Paleta
import mpensum.tema.Tema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Preferencias.iniciar(applicationContext)
        // El tema se sabe antes del primer cuadro: barras, ventana y app nacen ya en el suyo, sin destello
        val nocheDelSistema = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        aplicarAlTema(nocheDelSistema)
        val claro = Tema.claro
        // Barras del sistema transparentes, con iconos claros sobre lo oscuro y oscuros sobre lo claro
        enableEdgeToEdge(statusBarStyle = estiloBarras(claro), navigationBarStyle = estiloBarras(claro))
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Paleta.lienzo.toArgb()))
        setContent { App() }
    }
}
