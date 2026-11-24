package es.dam.entrena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Variantes del componente «Marcador de encuadre» de Figma. */
enum class Encuadre(val texto: String, val color: Color) {
    SIN_PERSONA("No te veo: ponte delante de la cámara", Color(0xFFE53935)),
    PARCIAL("Aléjate: necesito verte de la cabeza a los pies", Color(0xFFFFB300)),
    CORRECTO("Te veo bien", Color(0xFF26A69A)),
}

/** Decide la variante a partir de la postura. */
fun encuadre(p: Postura?): Encuadre {
    if (p == null || p.puntos.values.none { it.confianza > 0.6f }) return Encuadre.SIN_PERSONA
    val necesarias = listOf(Parte.HOMBRO_IZQ, Parte.HOMBRO_DER, Parte.CADERA_IZQ, Parte.CADERA_DER, Parte.TOBILLO_IZQ, Parte.TOBILLO_DER)
    return if (necesarias.all { p.visible(it) != null }) Encuadre.CORRECTO else Encuadre.PARCIAL
}

/** Componente «Marcador de encuadre»: una pastilla de color con una instrucción. */
@Composable
fun MarcadorEncuadre(encuadre: Encuadre, modifier: Modifier = Modifier) {
    Text(
        encuadre.texto,
        modifier = modifier.background(encuadre.color, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
}
