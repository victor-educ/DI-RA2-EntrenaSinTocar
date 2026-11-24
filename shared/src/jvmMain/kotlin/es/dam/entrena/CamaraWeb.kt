package es.dam.entrena

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.videoio.VideoCapture
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

/** Un modelo que mira una imagen de la cámara web (formato BGR de OpenCV) y devuelve lo que ha encontrado. */
internal interface AnalizadorEscritorio<T> {
    fun analizar(imagen: Mat): T
}

/**
 * Cámara web del ordenador con OpenCV, para escritorio.
 * Hace lo mismo que CamaraAnalizada en Android: pone la imagen en espejo, la recorta a 3:4 en vertical,
 * se la pasa a un analizador (si hay) y entrega el resultado a la pantalla.
 * @param espejo true como la cámara frontal del móvil; false como la trasera (la RA del Tema 8,
 *   porque un marcador impreso visto en espejo ya no se reconoce).
 */
@Composable
internal fun <T> CamaraWeb(
    modifier: Modifier,
    numeroCamara: Int = 0,
    espejo: Boolean = true,
    crearAnalizador: (() -> AnalizadorEscritorio<T>)? = null,
    alResultado: (T) -> Unit = {},
) {
    var imagen by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val avisar by rememberUpdatedState(alResultado)

    LaunchedEffect(numeroCamara) {
        withContext(Dispatchers.IO) {                     // la cámara y los modelos, fuera del hilo de la ventana
            OpenCV.loadLocally()                          // carga la librería nativa de OpenCV que viene en el .jar
            val camara = VideoCapture(numeroCamara)
            if (!camara.isOpened) { error = "No se encuentra la cámara web $numeroCamara"; return@withContext }
            val analizador = try { crearAnalizador?.invoke() } catch (e: Exception) { error = e.message; camara.release(); return@withContext }
            val fotograma = Mat()
            try {
                while (isActive && camara.read(fotograma)) {
                    if (espejo) Core.flip(fotograma, fotograma, 1)   // espejo, como la cámara frontal del móvil
                    val ancho = minOf(fotograma.width(), fotograma.height() * 3 / 4)
                    val vertical = Mat(fotograma, Rect((fotograma.width() - ancho) / 2, 0, ancho, fotograma.height()))
                    val resultado = analizador?.analizar(vertical)
                    val bitmap = aBufferedImage(vertical).toComposeImageBitmap()
                    withContext(Dispatchers.Main) {
                        imagen = bitmap
                        if (resultado != null) avisar(resultado)
                    }
                }
            } finally {
                camara.release()                          // al salir de la pantalla, se apaga la cámara
            }
        }
    }

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        imagen?.let { Image(it, contentDescription = "Cámara", contentScale = ContentScale.FillBounds) }
        error?.let { Text(it, color = Color.White) }
    }
}

/** Convierte una imagen de OpenCV (BGR) en una imagen de Java que Compose sabe pintar. */
private fun aBufferedImage(mat: Mat): BufferedImage {
    val copia = mat.clone()                               // el recorte no es continuo en memoria: se copia
    val salida = BufferedImage(copia.width(), copia.height(), BufferedImage.TYPE_3BYTE_BGR)
    copia.get(0, 0, (salida.raster.dataBuffer as DataBufferByte).data)
    return salida
}
