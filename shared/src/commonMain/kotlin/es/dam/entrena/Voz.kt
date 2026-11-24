package es.dam.entrena

import androidx.compose.runtime.Composable

/** Estado del micrófono. */
enum class EstadoMicro { APAGADO, ESCUCHANDO, PROCESANDO }

/**
 * Escucha continua (Tema 5, ampliado): en cuanto termina una frase vuelve a escuchar,
 * así el usuario no tiene que tocar nada.
 *   Android    → SpeechRecognizer del sistema, relanzado tras cada frase.
 *   Escritorio → Vosk leyendo el micrófono sin parar, sin Internet.
 */
interface Escucha {
    val estado: EstadoMicro
    /** Motivo si algo falla (sin permiso, sin micrófono, sin modelo…). */
    val error: String?
    /** Último tiempo de respuesta medido: del final de la frase al texto reconocido (ms). */
    val latenciaMs: Long?
    fun empezar(alOir: (frase: String) -> Unit)
    fun parar()
}

/** La «boca» de la app. [hablando] sirve para no hacer caso al micrófono mientras habla ella. */
interface Hablante {
    val hablando: Boolean
    fun decir(texto: String)
}

@Composable
expect fun rememberEscucha(): Escucha

@Composable
expect fun rememberHablante(): Hablante
