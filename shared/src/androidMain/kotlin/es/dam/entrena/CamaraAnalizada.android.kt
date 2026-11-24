package es.dam.entrena

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/** Un modelo que mira una imagen y devuelve lo que ha encontrado. */
internal interface Analizador<T> {
    fun analizar(imagen: Bitmap, instanteMs: Long): T
    fun cerrar()
}

/**
 * Cámara frontal que pasa cada imagen a un [Analizador] y entrega el resultado en la pantalla.
 * La imagen se gira y se pone en espejo antes de analizarla, para que los puntos que devuelve
 * el modelo coincidan con lo que el usuario ve. Proporción 3:4 en vertical.
 */
@Composable
internal fun <T> CamaraAnalizada(
    modifier: Modifier,
    crearAnalizador: (Context) -> Analizador<T>,
    alResultado: (T) -> Unit,
) {
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val avisar by rememberUpdatedState(alResultado)

    val controlador = remember {
        val proporcion = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            previewResolutionSelector = proporcion
            imageAnalysisResolutionSelector = proporcion
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        }
    }

    DisposableEffect(ciclo) {
        val hilo = Executors.newSingleThreadExecutor()   // los modelos trabajan fuera del hilo de la interfaz
        val principal = ContextCompat.getMainExecutor(context)
        val analizador = crearAnalizador(context)
        controlador.setImageAnalysisAnalyzer(hilo) { proxy ->
            val girada = proxy.use {
                val original = it.toBitmap()
                val m = Matrix().apply {
                    postRotate(it.imageInfo.rotationDegrees.toFloat())
                    postScale(-1f, 1f)   // espejo, como la vista previa frontal
                }
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
            }
            val resultado = analizador.analizar(girada, SystemClock.uptimeMillis())
            principal.execute { avisar(resultado) }
        }
        controlador.bindToLifecycle(ciclo)
        onDispose {
            controlador.clearImageAnalysisAnalyzer()
            controlador.unbind()
            hilo.execute { analizador.cerrar() }
            hilo.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PreviewView(it).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                controller = controlador
            }
        },
    )
}
