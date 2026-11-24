package es.dam.entrena

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// Arranque de escritorio: una ventana con la misma interfaz común que el móvil (App() de commonMain).
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Entrena sin tocar",
        state = rememberWindowState(width = 480.dp, height = 900.dp),
    ) {
        App()
    }
}
