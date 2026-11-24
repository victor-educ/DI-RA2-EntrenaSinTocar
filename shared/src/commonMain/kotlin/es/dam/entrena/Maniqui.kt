package es.dam.entrena

import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Un cuerpo «de mentira» para practicar sin cámara.
 * Devuelve la misma Postura que daría el modelo, con la rodilla doblada [rodilla] grados
 * (180 = de pie, 90 = sentadilla) y los brazos levantados [brazos] grados (0 = abajo, 90 = en cruz).
 * Así el ángulo, el contador y la pausa se pueden probar en un ordenador sin cámara.
 */
fun posturaManiqui(rodilla: Float, brazos: Float): Postura {
    val tramo = 0.17f                               // largo de muslo, pierna, brazo… (en alto de la imagen)
    val r = rodilla * PI.toFloat() / 180f
    val b = brazos * PI.toFloat() / 180f
    val puntos = mutableMapOf<Parte, Punto>()

    // Piernas: rodilla debajo de la cadera; el tobillo gira según el ángulo de la rodilla.
    // x se divide entre 0.75 por lo mismo que en angulo(): la imagen es 3:4.
    val caderaY = 0.5f
    val rodillaY = caderaY + tramo
    val tobilloY = rodillaY - tramo * cos(r)
    val bajar = 0.9f - tobilloY                     // se baja todo para que los pies queden en el suelo (y = 0.9)
    for ((lado, cadera, rodillaP, tobillo) in listOf(
        Lado(-1f, Parte.CADERA_DER, Parte.RODILLA_DER, Parte.TOBILLO_DER),
        Lado(1f, Parte.CADERA_IZQ, Parte.RODILLA_IZQ, Parte.TOBILLO_IZQ),
    )) {
        val x = 0.5f + lado * 0.07f
        puntos[cadera] = Punto(x, caderaY + bajar, 1f)
        puntos[rodillaP] = Punto(x, rodillaY + bajar, 1f)
        puntos[tobillo] = Punto(x + lado * tramo * sin(r) / 0.75f, tobilloY + bajar, 1f)
    }

    // Brazos: desde el hombro, girados [brazos] grados hacia fuera.
    val hombroY = caderaY + bajar - 0.25f
    for ((lado, hombro, codo, muneca) in listOf(
        Lado(-1f, Parte.HOMBRO_DER, Parte.CODO_DER, Parte.MUNECA_DER),
        Lado(1f, Parte.HOMBRO_IZQ, Parte.CODO_IZQ, Parte.MUNECA_IZQ),
    )) {
        val x = 0.5f + lado * 0.10f
        puntos[hombro] = Punto(x, hombroY, 1f)
        puntos[codo] = Punto(x + lado * 0.5f * 0.26f * sin(b) / 0.75f, hombroY + 0.5f * 0.26f * cos(b), 1f)
        puntos[muneca] = Punto(x + lado * 0.26f * sin(b) / 0.75f, hombroY + 0.26f * cos(b), 1f)
    }
    return Postura(puntos)
}

/** Las tres partes de un brazo o una pierna, y hacia qué lado va (−1 izquierda de la imagen, 1 derecha). */
private data class Lado(val signo: Float, val arriba: Parte, val medio: Parte, val abajo: Parte)

/** Deslizadores del maniquí. Cada 100 ms manda la postura, como haría la cámara con cada imagen. */
@Composable
fun Maniqui(alDetectar: (Postura) -> Unit) {
    var rodilla by remember { mutableFloatStateOf(175f) }
    var brazos by remember { mutableFloatStateOf(0f) }
    val avisar by rememberUpdatedState(alDetectar)
    val r by rememberUpdatedState(rodilla)
    val b by rememberUpdatedState(brazos)

    Text("Rodilla: ${rodilla.toInt()}°  (180 = de pie, 90 = abajo)")
    Slider(value = rodilla, onValueChange = { rodilla = it }, valueRange = 60f..180f)
    Text("Brazos: ${brazos.toInt()}°  (0 = abajo, 90 = en cruz)")
    Slider(value = brazos, onValueChange = { brazos = it }, valueRange = 0f..90f)

    LaunchedEffect(Unit) {
        while (true) {
            avisar(posturaManiqui(r, b))
            delay(100)
        }
    }
}
