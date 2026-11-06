package mpensum.plataforma

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mpensum.datos.Asignatura
import mpensum.logica.CALIDAD_JPEG
import mpensum.logica.DOMINIO
import mpensum.logica.FalloLectura
import mpensum.logica.LADO_MAXIMO
import mpensum.logica.LectorCaptura
import mpensum.logica.MateriaLeida
import mpensum.logica.PESO_MAXIMO
import mpensum.logica.URL_LECTURA
import mpensum.logica.cuerpoDePeticion
import mpensum.logica.filasDeRespuesta
import mpensum.logica.materiasLeidas
import mpensum.logica.materiasParaLeer

/** Lo que se espera entre una materia leída y la siguiente: el contador avanza sobre lo leído, no de golpe */
private const val RITMO_MS = 120L

private const val TIMEOUT_MS = 60_000

@Composable
actual fun rememberLectorCaptura(materias: List<Asignatura>): LectorCaptura? {
    // Sin registro de resultados (una prueba sin actividad) no hay galería que abrir
    if (LocalActivityResultRegistryOwner.current == null) return null
    val contexto = LocalContext.current.applicationContext
    val lector = remember(materias) { LectorAndroid(contexto, materiasParaLeer(materias)) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { lector.elegida(it) }
    lector.abrirGaleria = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    return lector
}

/**
 * Lee la captura con el servidor de la web: el Photo Picker del sistema (sin
 * permisos), la imagen reducida a JPEG fuera del hilo principal y un POST con
 * HttpURLConnection. El servidor responde de una vez; las materias se entregan
 * de una en una con un ritmo corto para que el contador de la pantalla avance.
 */
private class LectorAndroid(private val contexto: Context, private val materias: List<Asignatura>) : LectorCaptura {
    var abrirGaleria: () -> Unit = {}
    private var pendiente: CompletableDeferred<Uri?>? = null

    fun elegida(uri: Uri?) {
        pendiente?.complete(uri)
    }

    override suspend fun elegir(): ImageBitmap? {
        pendiente?.complete(null)
        val espera = CompletableDeferred<Uri?>().also { pendiente = it }
        abrirGaleria()
        val uri = espera.await() ?: return null
        return withContext(Dispatchers.IO) { abrirImagen(contexto.contentResolver, uri) }.asImageBitmap()
    }

    override suspend fun leer(foto: ImageBitmap, alTotal: (Int) -> Unit, alLeer: (MateriaLeida) -> Unit) {
        val base64 = withContext(Dispatchers.Default) {
            val bytes = ByteArrayOutputStream()
            foto.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, bytes)
            Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
        }
        val cuerpo = cuerpoDePeticion(base64, materias)
        val leidas = withContext(Dispatchers.IO) {
            val (estado, esJson, texto) = publicar(cuerpo)
            materiasLeidas(filasDeRespuesta(estado, esJson, texto), materias)
        }
        alTotal(leidas.size)
        for (m in leidas) {
            delay(RITMO_MS)
            alLeer(m)
        }
    }
}

/** Abre la imagen elegida: reducida a `LADO_MAXIMO`, derecha según su EXIF y sobre fondo blanco (un PNG transparente en JPEG sería negro) */
private fun abrirImagen(cr: ContentResolver, uri: Uri): Bitmap {
    if (cr.getType(uri)?.startsWith("image/") == false) throw FalloLectura("no-es-imagen")
    val peso = runCatching { cr.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
    if (peso > PESO_MAXIMO) throw FalloLectura("pesada")

    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
    } catch (e: IOException) {
        throw FalloLectura("no-se-abre", e.message)
    }
    if (medidas.outWidth <= 0 || medidas.outHeight <= 0) throw FalloLectura("no-se-abre")

    // inSampleSize es potencia de 2: se baja hasta quedar entre una y dos veces el lado máximo y se afina después
    var muestra = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    val leida = try {
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    } catch (e: IOException) {
        throw FalloLectura("no-se-abre", e.message)
    } ?: throw FalloLectura("no-se-abre")

    val orientacion = runCatching {
        cr.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrNull()
    val grados = when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(leida.width, leida.height))
    val matriz = Matrix().apply { postScale(escala, escala); postRotate(grados) }
    val lista = Bitmap.createBitmap(leida, 0, 0, leida.width, leida.height, matriz, true)

    val salida = Bitmap.createBitmap(lista.width, lista.height, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(salida)
    lienzo.drawColor(android.graphics.Color.WHITE)
    lienzo.drawBitmap(lista, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
    return salida
}

/** El POST al servidor de la web, con las cabeceras de la propia web: (estado, ¿es JSON?, cuerpo). Sin red es «red» */
private fun publicar(cuerpo: String): Triple<Int, Boolean, String> {
    val c = URL(URL_LECTURA).openConnection() as HttpURLConnection
    try {
        c.requestMethod = "POST"
        c.connectTimeout = TIMEOUT_MS
        c.readTimeout = TIMEOUT_MS
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Accept", "application/json")
        // El servidor solo acepta peticiones con el mismo host que la web (mismaCasa)
        c.setRequestProperty("Origin", DOMINIO)
        c.setRequestProperty("Referer", "$DOMINIO/")
        c.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        val estado = c.responseCode
        val flujo = if (estado in 200..299) c.inputStream else c.errorStream
        val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return Triple(estado, c.contentType?.contains("application/json") == true, texto)
    } catch (e: IOException) {
        throw FalloLectura("red", e.message)
    } finally {
        c.disconnect()
    }
}
