package es.dam.entrena

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Escucha continua en Android: SpeechRecognizer del sistema, que se vuelve a lanzar en cuanto
 * termina cada frase (o salta un silencio). El permiso se pide antes, en la tarjeta de Inicio.
 */
private class EscuchaAndroid(private val context: Context) : Escucha, RecognitionListener {
    override var estado by mutableStateOf(EstadoMicro.APAGADO)
    override var error by mutableStateOf<String?>(null)
    override var latenciaMs by mutableStateOf<Long?>(null)

    private val hilo = Handler(Looper.getMainLooper())
    private var sistema: SpeechRecognizer? = null
    private var alOir: (String) -> Unit = {}
    private var activo = false
    private var finFraseMs = 0L

    private val peticion = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    override fun empezar(alOir: (String) -> Unit) {
        this.alOir = alOir
        if (activo) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "Este dispositivo no tiene reconocimiento de voz"
            return
        }
        activo = true
        error = null
        sistema = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
        escuchar()
    }

    private fun escuchar() { if (activo) sistema?.startListening(peticion) }

    private fun otraVez(esperaMs: Long = 150) {
        hilo.removeCallbacksAndMessages(null)
        hilo.postDelayed({ escuchar() }, esperaMs)
    }

    override fun parar() {
        activo = false
        hilo.removeCallbacksAndMessages(null)
        sistema?.destroy(); sistema = null
        estado = EstadoMicro.APAGADO
    }

    override fun onReadyForSpeech(params: Bundle?) { estado = EstadoMicro.ESCUCHANDO }
    override fun onEndOfSpeech() { finFraseMs = SystemClock.elapsedRealtime(); estado = EstadoMicro.PROCESANDO }
    override fun onResults(results: Bundle?) {
        if (finFraseMs > 0) latenciaMs = SystemClock.elapsedRealtime() - finFraseMs
        val opciones = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        // De las alternativas, la primera que contenga una orden; si ninguna, la más probable.
        val frase = opciones.firstOrNull { interpretarVoz(it) != null } ?: opciones.firstOrNull().orEmpty()
        estado = EstadoMicro.ESCUCHANDO
        if (frase.isNotBlank()) alOir(frase)
        otraVez()
    }
    override fun onError(codigo: Int) {
        when (codigo) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { error = "Falta el permiso de micrófono"; parar() }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { sistema?.cancel(); otraVez(400) }
            // Silencio o nada reconocible: se sigue escuchando sin avisar.
            else -> otraVez(if (codigo == SpeechRecognizer.ERROR_NO_MATCH || codigo == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) 150 else 1000)
        }
    }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

@Composable
actual fun rememberEscucha(): Escucha {
    val context = LocalContext.current
    val e = remember { EscuchaAndroid(context) }
    DisposableEffect(Unit) { onDispose { e.parar() } }
    return e
}

/** TextToSpeech con aviso de inicio y fin, para que la app no se escuche a sí misma. */
private class HablanteAndroid(context: Context) : Hablante {
    override var hablando by mutableStateOf(false)
    private val hilo = Handler(Looper.getMainLooper())
    val tts: TextToSpeech = TextToSpeech(context) { ok ->
        if (ok == TextToSpeech.SUCCESS) iniciar()
    }

    private fun iniciar() {
        tts.language = Locale.forLanguageTag("es-ES")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { hilo.post { hilo.removeCallbacksAndMessages(null); hablando = true } }
            override fun onDone(id: String?) { hilo.postDelayed({ hablando = false }, 600) }
            @Deprecated("Obligatorio en API antiguas") override fun onError(id: String?) { hilo.post { hablando = false } }
        })
    }

    override fun decir(texto: String) {
        hablando = true
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "frase")
    }
}

@Composable
actual fun rememberHablante(): Hablante {
    val context = LocalContext.current
    val h = remember { HablanteAndroid(context) }
    DisposableEffect(Unit) { onDispose { h.tts.shutdown() } }
    return h
}
