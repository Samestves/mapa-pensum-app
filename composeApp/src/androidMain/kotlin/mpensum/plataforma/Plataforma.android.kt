package mpensum.plataforma

import android.app.ActivityManager
import android.content.Context
import android.os.Build
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
    private val ligero = MutableStateFlow<Boolean?>(null)
    internal var modesto = false
        private set
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
        ligero.value = prefs?.takeIf { it.contains("ligero") }?.getBoolean("ligero", false)
        modesto = medirModesto(contexto)
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

    actual val modoLigero: StateFlow<Boolean?> = ligero.asStateFlow()
    actual fun guardarModoLigero(ligero: Boolean?) {
        this.ligero.value = ligero
        prefs?.edit()?.apply { if (ligero == null) remove("ligero") else putBoolean("ligero", ligero) }?.apply()
    }
}

/** Con las animaciones quitadas (opciones de desarrollador o ahorro de batería) la escala del animador es 0 */
actual fun animacionesQuitadas(): Boolean {
    val c = Preferencias.animaciones ?: return false
    return runCatching { android.provider.Settings.Global.getFloat(c, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
}

/** Por debajo, la clase de memoria de la app es la de un teléfono justo: 192 MB o menos */
private const val MEMORIA_APP_MODESTA = 192
/** Menos de esto en total es un teléfono de menos de 4 GB: los de 4 anunciados dan unos 3,6 a 3,8 */
private const val RAM_MODESTA = 3_500L * 1024 * 1024
/** Con cuatro núcleos o menos, o ninguno por encima de 2 GHz, el dibujo no tiene holgura */
private const val NUCLEOS_MODESTOS = 4
private const val KHZ_MODESTOS = 2_000_000

/**
 * Gama baja, medida una vez al arrancar: basta con una señal. Sin RenderEffect
 * (API < 31) el cristal tampoco desenfoca, así que el modo ligero no quita nada.
 */
private fun medirModesto(contexto: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT < 31) return@runCatching true
    val am = contexto.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@runCatching false
    if (am.isLowRamDevice || am.memoryClass <= MEMORIA_APP_MODESTA) return@runCatching true
    val memoria = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
    if (memoria.totalMem in 1 until RAM_MODESTA) return@runCatching true
    val nucleos = Runtime.getRuntime().availableProcessors()
    if (nucleos <= NUCLEOS_MODESTOS) return@runCatching true
    val khz = frecuenciaMaxima(nucleos)
    khz in 1..KHZ_MODESTOS
}.getOrDefault(false)

/** La frecuencia más alta de la CPU, en kHz, o 0 si el sistema no la deja leer */
private fun frecuenciaMaxima(nucleos: Int): Int = (0 until nucleos).maxOfOrNull { i ->
    runCatching { java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toInt() }.getOrDefault(0)
} ?: 0

actual fun telefonoModesto(): Boolean = Preferencias.modesto

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
