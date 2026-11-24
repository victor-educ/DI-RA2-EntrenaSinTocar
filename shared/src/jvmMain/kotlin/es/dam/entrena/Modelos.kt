package es.dam.entrena

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File

/** Carpeta donde Gradle deja los modelos al compilar desktopApp (ver desktopApp/build.gradle.kts). */
internal val CARPETA_MODELOS = File(System.getProperty("user.home"), ".di-modelos")

internal fun modelo(nombre: String): String {
    val f = File(CARPETA_MODELOS, nombre)
    check(f.exists()) { "Falta el modelo ${f.path}: compila desktopApp con conexión a Internet" }
    return f.path
}

/** Imagen cuadrada lista para un modelo: se rellena con negro hasta ser cuadrada y se escala a [lado]. */
internal class Cuadrada(imagen: Mat, lado: Int) {
    val ladoOriginal = maxOf(imagen.width(), imagen.height())
    val izquierda = (ladoOriginal - imagen.width()) / 2
    val arriba = (ladoOriginal - imagen.height()) / 2
    /** Entrada del modelo: 1 × lado × lado × 3, RGB, valores de 0 a 1. */
    val entrada: Mat

    init {
        val cuadrada = Mat()
        Core.copyMakeBorder(
            imagen, cuadrada, arriba, ladoOriginal - imagen.height() - arriba,
            izquierda, ladoOriginal - imagen.width() - izquierda, Core.BORDER_CONSTANT, Scalar(0.0, 0.0, 0.0),
        )
        entrada = aEntrada(cuadrada, lado)
    }
}

/** Escala a lado × lado, pasa de BGR a RGB y de 0..255 a 0..1, con la forma 1 × lado × lado × 3. */
internal fun aEntrada(imagen: Mat, lado: Int): Mat {
    val pequena = Mat()
    Imgproc.resize(imagen, pequena, Size(lado.toDouble(), lado.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
    Imgproc.cvtColor(pequena, pequena, Imgproc.COLOR_BGR2RGB)
    val decimal = Mat()
    pequena.convertTo(decimal, CvType.CV_32FC3, 1.0 / 255)
    return decimal.reshape(1, intArrayOf(1, lado, lado, 3))
}

/** Todos los números de una salida del modelo, en orden. */
internal fun numeros(m: Mat): FloatArray {
    val datos = FloatArray(m.total().toInt() * m.channels())
    m.get(IntArray(m.dims()), datos)
    return datos
}

internal fun sigmoide(v: Float) = (1.0 / (1.0 + Math.exp(-v.toDouble()))).toFloat()
