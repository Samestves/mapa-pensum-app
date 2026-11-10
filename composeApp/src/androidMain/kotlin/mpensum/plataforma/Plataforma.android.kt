package mpensum.plataforma

import mpensum.plataforma.iniciar
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
import mpensum.tema.Paleta
import mpensum.tema.Tema

/** SharedPreferences como almacén: los mismos nombres y tipos que siempre */
private class AlmacenAndroid(private val prefs: SharedPreferences) : Almacen {
    override fun texto(clave: String): String? = prefs.getString(clave, null)
    override fun booleano(clave: String): Boolean? = if (prefs.contains(clave)) prefs.getBoolean(clave, false) else null
    override fun guardarTexto(clave: String, valor: String?) {
        prefs.edit().apply { if (valor == null) remove(clave) else putString(clave, valor) }.apply()
    }
    override fun guardarBooleano(clave: String, valor: Boolean?) {
        prefs.edit().apply { if (valor == null) remove(clave) else putBoolean(clave, valor) }.apply()
    }
}

/** Lo que solo se sabe en Android y se mide al arrancar: ni se guarda ni lo elige nadie */
internal object DatosDelTelefono {
    var modesto = false
    var version: String? = null
    var animaciones: android.content.ContentResolver? = null
}

/** Lo llama MainActivity al arrancar */
fun Preferencias.iniciar(contexto: Context) {
    usar(AlmacenAndroid(contexto.getSharedPreferences("mapa-pensum", Context.MODE_PRIVATE)))
    DatosDelTelefono.modesto = medirModesto(contexto)
    DatosDelTelefono.animaciones = contexto.applicationContext.contentResolver
    DatosDelTelefono.version = runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull()
}

/** Con las animaciones quitadas (opciones de desarrollador o ahorro de batería) la escala del animador es 0 */
actual fun animacionesQuitadas(): Boolean {
    val c = DatosDelTelefono.animaciones ?: return false
    return runCatching { android.provider.Settings.Global.getFloat(c, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
}

/** Por debajo, la clase de memoria de la app es la de un teléfono justo: 192 MB o menos */
private const val MEMORIA_APP_MODESTA = 192
/** Menos de esto en total es un teléfono de menos de 4 GB: los de 4 anunciados dan unos 3,6 a 3,8 */
private const val RAM_MODESTA = 3_500L * 1024 * 1024
/** Con cuatro núcleos o menos, o ninguno por encima de 2,2 GHz con poca memoria, el dibujo no tiene holgura */
private const val NUCLEOS_MODESTOS = 4
private const val KHZ_MODESTOS = 2_200_000
/** Por debajo de 6 GB, un núcleo rápido de menos de 2,2 GHz es un teléfono flojo */
private const val RAM_JUSTA = 6_000L * 1024 * 1024

/**
 * Los SoC de gama baja, por el comienzo de `Build.SOC_MODEL` (Helio G y P, Snapdragon 4xx y 6xx bajos, Unisoc,
 * Exynos 850). Lista hecha a mano: se amplía cuando aparezca un teléfono flojo que no entra solo.
 */
private val SOC_MODESTOS = listOf(
    "MT676", "MT6765", "MT6762", "MT6761", "MT6768", "MT6769", "MT6781", "SM4", "SM6115", "SM6125", "SM6225",
    "SDM4", "SDM6", "SDM450", "UMS", "T6", "T7", "S5E3830",
)

/** Los 60 Hz, que es lo que un teléfono flojo sostiene: a 120 Hz tendría la mitad de tiempo por cuadro */
private const val REFRESCO_LIGERO = 60f

/**
 * Gama baja, medida una vez al arrancar: basta con una señal. Sin RenderEffect
 * (API < 31) el cristal tampoco desenfoca, así que el modo ligero no quita nada.
 */
private fun medirModesto(contexto: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT < 31) return@runCatching true
    // Certificado por el fabricante como gama media o alta: no es gama baja, diga lo que diga la memoria
    if (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S) return@runCatching false
    if (esSocModesto(Build.SOC_MODEL)) return@runCatching true
    val am = contexto.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@runCatching false
    if (am.isLowRamDevice || am.memoryClass <= MEMORIA_APP_MODESTA) return@runCatching true
    val memoria = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
    if (memoria.totalMem in 1 until RAM_MODESTA) return@runCatching true
    val nucleos = Runtime.getRuntime().availableProcessors()
    if (nucleos <= NUCLEOS_MODESTOS) return@runCatching true
    val khz = frecuenciaMaxima(nucleos)
    khz in 1 until KHZ_MODESTOS && memoria.totalMem < RAM_JUSTA
}.getOrDefault(false)

/** Si el modelo del SoC ("MT6769", "SM6225"...) es de la lista de los modestos; "UNKNOWN" y vacío no lo son */
internal fun esSocModesto(modelo: String): Boolean {
    val soc = modelo.trim().uppercase()
    return soc.isNotEmpty() && SOC_MODESTOS.any { soc.startsWith(it) }
}

/**
 * En modo ligero la ventana pide 60 Hz (el modo de la misma resolución con el refresco más cercano); fuera de él
 * suelta la petición y el sistema decide. Solo toca la ventana si cambia algo.
 */
fun ajustarRefresco(actividad: ComponentActivity, ligero: Boolean) {
    val ventana = actividad.window
    val modoId = if (ligero) modoDe60Hz(actividad) else 0
    if (ventana.attributes.preferredDisplayModeId == modoId) return
    ventana.attributes = ventana.attributes.also { it.preferredDisplayModeId = modoId }
}

/** El id del modo de 60 Hz de la pantalla actual, o 0 (sin preferencia) si no hay o no se puede saber */
@Suppress("DEPRECATION")
private fun modoDe60Hz(actividad: ComponentActivity): Int = runCatching {
    val pantalla = actividad.windowManager.defaultDisplay
    val actual = pantalla.mode
    pantalla.supportedModes
        .filter { it.physicalWidth == actual.physicalWidth && it.physicalHeight == actual.physicalHeight }
        .minByOrNull { kotlin.math.abs(it.refreshRate - REFRESCO_LIGERO) }
        ?.modeId ?: 0
}.getOrDefault(0)

/** La frecuencia más alta de la CPU, en kHz, o 0 si el sistema no la deja leer */
private fun frecuenciaMaxima(nucleos: Int): Int = (0 until nucleos).maxOfOrNull { i ->
    runCatching { java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toInt() }.getOrDefault(0)
} ?: 0

actual fun telefonoModesto(): Boolean = DatosDelTelefono.modesto

actual fun versionApp(): String? = DatosDelTelefono.version

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
        // Si el modo ligero cambia a mano en Personalizar, los 60 Hz lo siguen
        ajustarRefresco(actividad, Tema.ligero)
    }
}

actual fun versionSdk(): Int = android.os.Build.VERSION.SDK_INT
