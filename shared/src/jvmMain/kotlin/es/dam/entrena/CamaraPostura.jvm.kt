package es.dam.entrena

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.opencv.core.Mat
import org.opencv.dnn.Dnn

// Número de cada articulación en el modelo MediaPipe Pose (33 puntos).
private val PARTES = mapOf(
    Parte.HOMBRO_IZQ to 11, Parte.HOMBRO_DER to 12, Parte.CODO_IZQ to 13, Parte.CODO_DER to 14,
    Parte.MUNECA_IZQ to 15, Parte.MUNECA_DER to 16, Parte.CADERA_IZQ to 23, Parte.CADERA_DER to 24,
    Parte.RODILLA_IZQ to 25, Parte.RODILLA_DER to 26, Parte.TOBILLO_IZQ to 27, Parte.TOBILLO_DER to 28,
)

/**
 * Escritorio: el mismo modelo que usa MediaPipe (versión ONNX de OpenCV Zoo), ejecutado con OpenCV.
 * Funciona bien si la persona ocupa casi toda la imagen, como al entrenar delante de la cámara.
 */
internal class AnalizadorPosturaEscritorio : AnalizadorEscritorio<Postura> {
    private val red = Dnn.readNet(modelo("pose_estimation_mediapipe_2023mar.onnx"))

    override fun analizar(imagen: Mat): Postura {
        val c = Cuadrada(imagen, 256)                          // 256 × 256, como pide el modelo
        red.setInput(c.entrada)
        val salidas = ArrayList<Mat>()
        red.forward(salidas, red.unconnectedOutLayersNames)
        if (numeros(salidas[1])[0] < 0.5f) return Postura(emptyMap())   // no hay nadie
        val p = numeros(salidas[0])                           // 39 puntos × (x, y, z, visibilidad, presencia)
        val puntos = PARTES.mapValues { (_, i) ->
            // De píxeles de la imagen de 256 a 0..1 de la imagen original.
            val x = (p[i * 5] / 256f * c.ladoOriginal - c.izquierda) / imagen.width()
            val y = (p[i * 5 + 1] / 256f * c.ladoOriginal - c.arriba) / imagen.height()
            Punto(x, y, sigmoide(p[i * 5 + 3]))
        }
        return Postura(puntos)
    }
}

@Composable
actual fun CamaraPostura(modifier: Modifier, alDetectar: (Postura) -> Unit) =
    CamaraWeb(modifier, crearAnalizador = { AnalizadorPosturaEscritorio() }, alResultado = alDetectar)
