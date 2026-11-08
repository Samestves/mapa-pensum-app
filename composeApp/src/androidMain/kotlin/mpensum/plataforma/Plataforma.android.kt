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
import mpensum.carrera.Vista
import mpensum.tema.Acento
import mpensum.tema.Fondo
import mpensum.tema.FormaCables
import mpensum.tema.Grosor
import mpensum.tema.Juego
import mpensum.tema.Paleta
import mpensum.tema.Tema
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
    private val fondo = MutableStateFlow<Fondo?>(null)
    private val forma = MutableStateFlow(FormaCables.CURVOS)
    private val grosor = MutableStateFlow(Grosor.NORMAL)
    private val atenuar = MutableStateFlow(false)
    private val masContraste = MutableStateFlow(false)
    private val dia = MutableStateFlow(Juego.CLARO)
    private val noche = MutableStateFlow(Juego.OSCURO)
    private val entrarEnCarrera = MutableStateFlow(false)
    private val vista = MutableStateFlow(Vista.MAPA)
    private val horas24 = MutableStateFlow(false)
    private val vibra = MutableStateFlow(true)
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
        fondo.value = leerEnum<Fondo>("fondoMapa")
        forma.value = leerEnum<FormaCables>("cables") ?: FormaCables.CURVOS
        grosor.value = leerEnum<Grosor>("grosorCables") ?: Grosor.NORMAL
        atenuar.value = prefs?.getBoolean("atenuarAprobadas", false) ?: false
        masContraste.value = prefs?.getBoolean("contraste", false) ?: false
        dia.value = leerEnum<Juego>("temaDia") ?: Juego.CLARO
        noche.value = leerEnum<Juego>("temaNoche") ?: Juego.OSCURO
        entrarEnCarrera.value = prefs?.getBoolean("abrirEnCarrera", false) ?: false
        // El horario no es una vista con la que se abra una carrera
        vista.value = leerEnum<Vista>("vistaInicial")?.takeIf { it != Vista.HORARIO } ?: Vista.MAPA
        horas24.value = prefs?.getBoolean("hora24", false) ?: false
        vibra.value = prefs?.getBoolean("vibracion", true) ?: true
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
        // Cada tema trae su fondo: elegir otro tema suelta el fondo que se hubiera elegido a mano
        if (apariencia != this.apariencia.value) guardarFondoMapa(null)
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

    actual val fondoMapa: StateFlow<Fondo?> = fondo.asStateFlow()
    actual fun guardarFondoMapa(fondo: Fondo?) {
        this.fondo.value = fondo
        prefs?.edit()?.apply { if (fondo == null) remove("fondoMapa") else putString("fondoMapa", fondo.name) }?.apply()
    }

    actual val cables: StateFlow<FormaCables> = forma.asStateFlow()
    actual fun guardarCables(forma: FormaCables) {
        this.forma.value = forma
        prefs?.edit()?.putString("cables", forma.name)?.apply()
    }

    actual val grosorCables: StateFlow<Grosor> = grosor.asStateFlow()
    actual fun guardarGrosorCables(grosor: Grosor) {
        this.grosor.value = grosor
        prefs?.edit()?.putString("grosorCables", grosor.name)?.apply()
    }

    actual val atenuarAprobadas: StateFlow<Boolean> = atenuar.asStateFlow()
    actual fun guardarAtenuarAprobadas(atenuar: Boolean) {
        this.atenuar.value = atenuar
        prefs?.edit()?.putBoolean("atenuarAprobadas", atenuar)?.apply()
    }

    actual val contraste: StateFlow<Boolean> = masContraste.asStateFlow()
    actual fun guardarContraste(activo: Boolean) {
        masContraste.value = activo
        prefs?.edit()?.putBoolean("contraste", activo)?.apply()
    }

    actual val temaDia: StateFlow<Juego> = dia.asStateFlow()
    actual fun guardarTemaDia(juego: Juego) {
        dia.value = juego
        prefs?.edit()?.putString("temaDia", juego.name)?.apply()
    }

    actual val temaNoche: StateFlow<Juego> = noche.asStateFlow()
    actual fun guardarTemaNoche(juego: Juego) {
        noche.value = juego
        prefs?.edit()?.putString("temaNoche", juego.name)?.apply()
    }

    actual val abrirEnCarrera: StateFlow<Boolean> = entrarEnCarrera.asStateFlow()
    actual fun guardarAbrirEnCarrera(abrir: Boolean) {
        entrarEnCarrera.value = abrir
        prefs?.edit()?.putBoolean("abrirEnCarrera", abrir)?.apply()
    }

    actual val vistaInicial: StateFlow<Vista> = vista.asStateFlow()
    actual fun guardarVistaInicial(vista: Vista) {
        this.vista.value = vista
        prefs?.edit()?.putString("vistaInicial", vista.name)?.apply()
    }

    actual val hora24: StateFlow<Boolean> = horas24.asStateFlow()
    actual fun guardarHora24(activo: Boolean) {
        horas24.value = activo
        prefs?.edit()?.putBoolean("hora24", activo)?.apply()
    }

    actual val vibracion: StateFlow<Boolean> = vibra.asStateFlow()
    actual fun guardarVibracion(activa: Boolean) {
        vibra.value = activa
        prefs?.edit()?.putBoolean("vibracion", activa)?.apply()
    }

    /** Un enum guardado por su nombre, o null si no hay o ya no existe */
    private inline fun <reified E : Enum<E>> leerEnum(clave: String): E? {
        val nombre = prefs?.getString(clave, null) ?: return null
        return enumValues<E>().firstOrNull { it.name == nombre }
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
        // Si el modo ligero cambia a mano en Personalizar, los 60 Hz lo siguen
        ajustarRefresco(actividad, Tema.ligero)
    }
}

actual fun versionSdk(): Int = android.os.Build.VERSION.SDK_INT
