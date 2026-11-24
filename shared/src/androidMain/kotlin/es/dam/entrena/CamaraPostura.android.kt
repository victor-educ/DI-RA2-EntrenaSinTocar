package es.dam.entrena

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

// Qué constante de ML Kit corresponde a cada parte.
private val PARTES = mapOf(
    Parte.HOMBRO_IZQ to PoseLandmark.LEFT_SHOULDER, Parte.HOMBRO_DER to PoseLandmark.RIGHT_SHOULDER,
    Parte.CODO_IZQ to PoseLandmark.LEFT_ELBOW, Parte.CODO_DER to PoseLandmark.RIGHT_ELBOW,
    Parte.MUNECA_IZQ to PoseLandmark.LEFT_WRIST, Parte.MUNECA_DER to PoseLandmark.RIGHT_WRIST,
    Parte.CADERA_IZQ to PoseLandmark.LEFT_HIP, Parte.CADERA_DER to PoseLandmark.RIGHT_HIP,
    Parte.RODILLA_IZQ to PoseLandmark.LEFT_KNEE, Parte.RODILLA_DER to PoseLandmark.RIGHT_KNEE,
    Parte.TOBILLO_IZQ to PoseLandmark.LEFT_ANKLE, Parte.TOBILLO_DER to PoseLandmark.RIGHT_ANKLE,
)

private class AnalizadorPostura : Analizador<Postura> {
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    )

    override fun analizar(imagen: Bitmap, instanteMs: Long): Postura {
        val pose = Tasks.await(detector.process(InputImage.fromBitmap(imagen, 0)))
        val puntos = PARTES.mapNotNull { (parte, tipo) ->
            pose.getPoseLandmark(tipo)?.let {
                parte to Punto(it.position.x / imagen.width, it.position.y / imagen.height, it.inFrameLikelihood)
            }
        }.toMap()
        return Postura(puntos)
    }

    override fun cerrar() = detector.close()
}

@Composable
actual fun CamaraPostura(modifier: Modifier, alDetectar: (Postura) -> Unit) =
    CamaraAnalizada(modifier, { AnalizadorPostura() }, alDetectar)
