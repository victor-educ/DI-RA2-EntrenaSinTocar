package es.dam.entrena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HUESOS = listOf(
    Parte.HOMBRO_IZQ to Parte.HOMBRO_DER, Parte.CADERA_IZQ to Parte.CADERA_DER,
    Parte.HOMBRO_IZQ to Parte.CADERA_IZQ, Parte.HOMBRO_DER to Parte.CADERA_DER,
    Parte.HOMBRO_IZQ to Parte.CODO_IZQ, Parte.CODO_IZQ to Parte.MUNECA_IZQ,
    Parte.HOMBRO_DER to Parte.CODO_DER, Parte.CODO_DER to Parte.MUNECA_DER,
    Parte.CADERA_IZQ to Parte.RODILLA_IZQ, Parte.RODILLA_IZQ to Parte.TOBILLO_IZQ,
    Parte.CADERA_DER to Parte.RODILLA_DER, Parte.RODILLA_DER to Parte.TOBILLO_DER,
)

/**
 * Cámara frontal (3:4) con el esqueleto y el «Marcador de encuadre» encima (Tema 6).
 * Con [sinCamara] no se enciende la cámara: la postura llega del maniquí y aquí solo se dibuja.
 * [colorEsqueleto]: verde contando, naranja si no se cuenta (estados de la UD3).
 */
@Composable
fun CamaraConEsqueleto(
    postura: Postura?,
    sinCamara: Boolean,
    colorEsqueleto: Color,
    modifier: Modifier = Modifier,
    alDetectar: (Postura) -> Unit,
) {
    Box(modifier.aspectRatio(3f / 4f, matchHeightConstraintsFirst = true).background(Color(0xFF263238))) {
        if (!sinCamara) CamaraPostura(Modifier.fillMaxSize(), alDetectar)
        Canvas(Modifier.fillMaxSize()) {
            val p = postura ?: return@Canvas
            fun Punto.enPantalla() = Offset(x * size.width, y * size.height)
            for ((a, b) in HUESOS) {
                val pa = p.visible(a) ?: continue
                val pb = p.visible(b) ?: continue
                drawLine(colorEsqueleto, pa.enPantalla(), pb.enPantalla(), strokeWidth = 10f)
            }
            p.puntos.values.filter { it.confianza > 0.6f }.forEach { drawCircle(Color.White, 9f, it.enPantalla()) }
        }
        MarcadorEncuadre(encuadre(postura), Modifier.align(Alignment.TopCenter).padding(8.dp))
    }
}
