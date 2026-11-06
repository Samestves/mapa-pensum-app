package mpensum.plataforma

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import mpensum.tema.Acento
import mpensum.tema.Paleta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object Preferencias {
    private var prefs: SharedPreferences? = null
    private val apariencia = MutableStateFlow(Apariencia.OSCURO)
    private val acentoElegido = MutableStateFlow<Acento?>(null)
    private val grande = MutableStateFlow(false)
    private val luz = MutableStateFlow<Boolean?>(null)
    private val reducir = MutableStateFlow<Boolean?>(null)
    private val codigosVisibles = MutableStateFlow(true)
    internal var version: String? = null
        private set
    internal var animaciones: android.content.ContentResolver? = null

    /** Lo llama MainActivity al arrancar */
    fun iniciar(contexto: Context) {
        prefs = contexto.getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE)
        apariencia.value = Apariencia.de(prefs?.getString("tema", null))
        acentoElegido.value = Acento.de(prefs?.getString("acento", null))
        grande.value = prefs?.getString("texto", null) == "grande"
        luz.value = prefs?.takeIf { it.contains("luzFondo") }?.getBoolean("luzFondo", true)
        reducir.value = prefs?.takeIf { it.contains("movimiento") }?.getBoolean("movimiento", false)
        codigosVisibles.value = prefs?.getBoolean("codigos", true) ?: true
        animaciones = contexto.applicationContext.contentResolver
        version = runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull()
    }

    actual fun bienvenidaVista(): Boolean = prefs?.getBoolean("bienvenida", false) ?: false
    actual fun marcarBienvenidaVista() {
        prefs?.edit()?.putBoolean("bienvenida", true)?.apply()
    }
    actual fun carrera(): String? = prefs?.getString("carrera", null)
    actual fun guardarCarrera(slug: String) {
        prefs?.edit()?.putString("carrera", slug)?.apply()
    }

    actual fun leerTexto(clave: String): String? = prefs?.getString(clave, null)
    actual fun guardarTexto(clave: String, texto: String) {
        prefs?.edit()?.putString(clave, texto)?.apply()
    }

    actual val tema: StateFlow<Apariencia> = apariencia.asStateFlow()
    actual fun guardarTema(apariencia: Apariencia) {
        this.apariencia.value = apariencia
        prefs?.edit()?.putString("tema", apariencia.valor)?.apply()
    }

    actual val acento: StateFlow<Acento?> = acentoElegido.asStateFlow()
    actual fun guardarAcento(acento: Acento?) {
        acentoElegido.value = acento
        prefs?.edit()?.apply { if (acento == null) remove("acento") else putString("acento", acento.valor) }?.apply()
    }

    actual val textoGrande: StateFlow<Boolean> = grande.asStateFlow()
    actual fun guardarTextoGrande(grande: Boolean) {
        this.grande.value = grande
        prefs?.edit()?.putString("texto", if (grande) "grande" else "normal")?.apply()
    }

    actual val luzFondo: StateFlow<Boolean?> = luz.asStateFlow()
    actual fun guardarLuzFondo(encendida: Boolean) {
        luz.value = encendida
        prefs?.edit()?.putBoolean("luzFondo", encendida)?.apply()
    }

    actual val reducirMovimiento: StateFlow<Boolean?> = reducir.asStateFlow()
    actual fun guardarReducirMovimiento(reducir: Boolean) {
        this.reducir.value = reducir
        prefs?.edit()?.putBoolean("movimiento", reducir)?.apply()
    }

    actual val codigos: StateFlow<Boolean> = codigosVisibles.asStateFlow()
    actual fun guardarCodigos(visibles: Boolean) {
        codigosVisibles.value = visibles
        prefs?.edit()?.putBoolean("codigos", visibles)?.apply()
    }
}

/** Con las animaciones quitadas (opciones de desarrollador o ahorro de batería) la escala del animador es 0 */
actual fun animacionesQuitadas(): Boolean {
    val c = Preferencias.animaciones ?: return false
    return runCatching { android.provider.Settings.Global.getFloat(c, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
}

actual fun versionApp(): String? = Preferencias.version

@Composable
actual fun ManejarAtras(activo: Boolean, alVolver: () -> Unit) = BackHandler(activo, alVolver)

/** El estilo de las barras para un tema: transparentes, con los iconos que se leen encima */
fun estiloBarras(claro: Boolean): SystemBarStyle =
    if (claro) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT)

@Composable
actual fun BarrasDelSistema(claro: Boolean) {
    val actividad = LocalContext.current as? ComponentActivity ?: return
    val lienzo = Paleta.lienzo.toArgb()
    SideEffect {
        actividad.enableEdgeToEdge(statusBarStyle = estiloBarras(claro), navigationBarStyle = estiloBarras(claro))
        // El fondo de la ventana es el que asoma al rotar o al abrir el teclado: el del tema
        actividad.window.setBackgroundDrawable(ColorDrawable(lienzo))
    }
}

actual fun versionSdk(): Int = android.os.Build.VERSION.SDK_INT
