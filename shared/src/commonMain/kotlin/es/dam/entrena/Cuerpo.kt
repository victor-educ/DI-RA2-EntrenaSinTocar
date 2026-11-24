package es.dam.entrena

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Las partes del cuerpo que usan los ejemplos. */
enum class Parte { HOMBRO_IZQ, HOMBRO_DER, CODO_IZQ, CODO_DER, MUNECA_IZQ, MUNECA_DER, CADERA_IZQ, CADERA_DER, RODILLA_IZQ, RODILLA_DER, TOBILLO_IZQ, TOBILLO_DER }

/** Un punto del cuerpo: x e y de 0 a 1 sobre la imagen, y la confianza del modelo (0 a 1). */
class Punto(val x: Float, val y: Float, val confianza: Float)

/** Lo que devuelve el modelo en cada imagen. Vacío si no hay nadie. */
class Postura(val puntos: Map<Parte, Punto>) {
    /** El punto solo si el modelo está bastante seguro de que se ve. */
    fun visible(parte: Parte): Punto? = puntos[parte]?.takeIf { it.confianza > 0.6f }
}

/** Cámara frontal que avisa con la postura de cada imagen. En Android: CameraX + ML Kit. */
@Composable
expect fun CamaraPostura(modifier: Modifier, alDetectar: (Postura) -> Unit)
