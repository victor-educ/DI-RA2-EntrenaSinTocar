package es.dam.entrena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/** Modelo de español de Vosk: lo descarga Gradle la primera vez que se compila desktopApp. */
val CARPETA_MODELO_VOZ = File(System.getProperty("user.home"), ".vosk/vosk-model-small-es-0.42")

val modeloVoz: ModeloVosk by lazy { ModeloVosk(CARPETA_MODELO_VOZ.path) }

/**
 * Gramática cerrada: Vosk solo puede devolver estas palabras o «[unk]» (desconocido).
 * Así acierta mucho más con las órdenes y casi no se confunde con el ruido de la sala.
 */
fun gramaticaOrdenes(): String =
    (PALABRAS_VOZ.values.flatten() + "[unk]").joinToString(",", "[", "]") { "\"$it\"" }

/**
 * Palabras de relleno: las más frecuentes del español (lista general, no sacada de las frases de prueba).
 * Con la gramática cerrada, cualquier cosa que se diga acaba convertida en la orden más parecida;
 * con estas palabras, lo que no es una orden tiene «otro sitio donde caer» y no se toma por orden.
 */
val PALABRAS_RELLENO = listOf(
    "de", "el", "en", "y", "a", "los", "se", "del", "las", "un", "por", "con", "no", "una", "su", "al", "lo",
    "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy",
    "sin", "sobre", "también", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos",
    "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "esto", "antes", "algunos", "unos",
    "yo", "otro", "otras", "otra", "tanto", "esa", "estos", "mucho", "nada", "muchos", "cual", "poco", "ella",
    "estar", "estas", "algo", "nosotros", "mi", "tu", "te", "ti", "dos", "tres", "año", "día", "vez", "casa",
    "tiempo", "mujer", "hombre", "cosa", "mundo", "vida", "mano", "parte", "ojo", "agua", "ver", "dar", "ir",
    "decir", "poder", "hacer", "saber", "querer", "llegar", "pasar", "deber", "poner", "creer", "hablar",
    "llevar", "dejar", "quedar", "encontrar", "llamar", "venir", "pensar", "salir", "volver", "tomar",
    "conocer", "vivir", "sentir", "tratar", "mirar", "contar", "esperar", "buscar", "entrar", "trabajar",
    "escribir", "perder", "entender", "pedir", "recibir", "recordar", "bueno", "grande", "nuevo", "mismo",
    "mejor", "ahora", "aquí", "así", "luego", "siempre", "nunca", "hoy", "mañana", "ayer", "tarde", "noche",
)

/** Gramática con relleno (la que usa la app): órdenes + palabras frecuentes + desconocido. */
fun gramaticaConRelleno(): String =
    (PALABRAS_VOZ.values.flatten() + PALABRAS_RELLENO + "[unk]").distinct().joinToString(",", "[", "]") { "\"$it\"" }

/** Vosk devuelve JSON como {"text" : "pausa"}. Esto saca el texto. */
fun textoVosk(json: String): String =
    Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1) ?: ""

/**
 * Escucha continua en el ordenador: un hilo lee el micrófono sin parar y cada vez que Vosk
 * detecta un silencio tras una frase, la entrega. No necesita Internet.
 */
private class EscuchaEscritorio : Escucha {
    override var estado by mutableStateOf(EstadoMicro.APAGADO)
    override var error by mutableStateOf<String?>(null)
    override var latenciaMs by mutableStateOf<Long?>(null)
    @Volatile private var seguir = false

    override fun empezar(alOir: (String) -> Unit) {
        if (seguir) return
        error = null
        if (!CARPETA_MODELO_VOZ.exists()) {
            error = "Falta el modelo de voz en ${CARPETA_MODELO_VOZ.path}"
            return
        }
        seguir = true
        thread(isDaemon = true, name = "voz") {
            try {
                val formato = AudioFormat(16000f, 16, 1, true, false)
                val micro = AudioSystem.getTargetDataLine(formato).apply { open(formato); start() }
                SwingUtilities.invokeLater { estado = EstadoMicro.ESCUCHANDO }
                ReconocedorVosk(modeloVoz, 16000f, gramaticaConRelleno()).use { rec ->
                    val trozo = ByteArray(3200)   // 100 ms de audio
                    while (seguir) {
                        val n = micro.read(trozo, 0, trozo.size)
                        if (n <= 0) continue
                        // Latencia: desde que llega el último trozo de la frase hasta tener el texto.
                        val antes = System.nanoTime()
                        if (rec.aceptar(trozo, n)) {
                            val dijo = textoVosk(rec.resultado)
                            val frase = dijo.replace("[unk]", "").trim()
                            val ms = (System.nanoTime() - antes) / 1_000_000
                            if (frase.isNotEmpty() || dijo.isNotEmpty()) {
                                SwingUtilities.invokeLater { latenciaMs = ms; if (seguir) alOir(frase) }
                            }
                        }
                    }
                }
                micro.stop(); micro.close()
            } catch (e: Exception) {
                seguir = false
                SwingUtilities.invokeLater { error = "No puedo usar el micrófono: ${e.message}" }
            }
            SwingUtilities.invokeLater { estado = EstadoMicro.APAGADO }
        }
    }

    override fun parar() { seguir = false }
}

@Composable
actual fun rememberEscucha(): Escucha {
    val e = remember { EscuchaEscritorio() }
    DisposableEffect(Unit) { onDispose { e.parar() } }
    return e
}

/** Voz del sistema operativo. [hablando] sigue a true hasta que termina el proceso y medio segundo más. */
private class HablanteEscritorio : Hablante {
    override var hablando by mutableStateOf(false)
    private var actual: Process? = null

    override fun decir(texto: String) {
        val so = System.getProperty("os.name").lowercase()
        val orden = when {
            so.contains("win") -> listOf(
                "powershell", "-Command",
                "Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('${texto.replace("'", "")}')",
            )
            so.contains("mac") -> listOf("say", "-v", "Monica", texto)
            else -> listOf("espeak-ng", "-v", "es", texto)
        }
        actual?.destroy()
        val proceso = runCatching { ProcessBuilder(orden).start() }.getOrNull() ?: return
        actual = proceso
        hablando = true
        thread(isDaemon = true) {
            proceso.waitFor()
            Thread.sleep(500)
            SwingUtilities.invokeLater { if (actual === proceso) hablando = false }
        }
    }
}

@Composable
actual fun rememberHablante(): Hablante = remember { HablanteEscritorio() }
