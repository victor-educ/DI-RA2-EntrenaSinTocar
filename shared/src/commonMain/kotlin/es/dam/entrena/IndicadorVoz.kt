package es.dam.entrena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Variantes del componente «Indicador de voz» de Figma (propiedad estado). */
enum class EstadoVoz(val color: Color, val icono: String) {
    INACTIVO(Color(0xFF9E9E9E), "–"),
    ESCUCHANDO(Color(0xFFD32F2F), "●"),
    RECONOCIDO(Color(0xFF2E7D32), "ok"),
    NO_ENTENDIDO(Color(0xFFE65100), "?"),
}

/** Texto de cada variante. Incluye siempre el estado en palabras, no solo el color (accesibilidad). */
fun textoVoz(estado: EstadoVoz, detalle: String): String = when (estado) {
    EstadoVoz.INACTIVO -> "Micrófono apagado"
    EstadoVoz.ESCUCHANDO -> "Te escucho…"
    EstadoVoz.RECONOCIDO -> "Entendido: $detalle"
    EstadoVoz.NO_ENTENDIDO -> "No te he entendido. Prueba: «pausa»"
}

/**
 * Componente «Indicador de voz». Es una región viva: TalkBack lee el cambio sin que el usuario toque nada.
 */
@Composable
fun IndicadorVoz(estado: EstadoVoz, detalle: String = "", modifier: Modifier = Modifier) {
    val texto = textoVoz(estado, detalle)
    Row(
        modifier
            .background(Color(0xCC000000), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = texto },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp).background(estado.color, CircleShape), contentAlignment = Alignment.Center) {
            Text(estado.icono, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(texto, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
