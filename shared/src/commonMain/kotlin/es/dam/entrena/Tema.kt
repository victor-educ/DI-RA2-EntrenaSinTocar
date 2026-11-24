package es.dam.entrena

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Variables de color de Figma. Fondo oscuro y texto claro: contraste AA leyendo a 2-3 m. */
object Colores {
    val Fondo = Color(0xFF101418)
    val Superficie = Color(0xFF1E252B)
    val Texto = Color(0xFFFFFFFF)
    val TextoSuave = Color(0xFFB0BEC5)
    val Primario = Color(0xFF26A69A)     // verde agua: en marcha, correcto
    val Aviso = Color(0xFFFFB300)        // ámbar: pausa, encuadre parcial
    val Error = Color(0xFFE53935)
}

/** Botones de 56 dp o más (requisito de accesibilidad). */
val ALTO_BOTON = 64.dp

@Composable
fun TemaEntrena(contenido: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Colores.Primario, onPrimary = Color.Black,
        secondary = Colores.Aviso, onSecondary = Color.Black,
        background = Colores.Fondo, surface = Colores.Fondo, surfaceVariant = Colores.Superficie,
        onBackground = Colores.Texto, onSurface = Colores.Texto, onSurfaceVariant = Colores.TextoSuave,
        error = Colores.Error,
        surfaceContainerHigh = Colores.Superficie, surfaceContainer = Colores.Superficie,
    ),
) {
    // Surface pone el color de texto claro por defecto a todo lo que hay dentro.
    Surface(color = Colores.Fondo, contentColor = Colores.Texto) { contenido() }
}
