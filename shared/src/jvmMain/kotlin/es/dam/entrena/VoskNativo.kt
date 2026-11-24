package es.dam.entrena

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.io.File
import java.nio.file.Files

/**
 * Enlace mínimo con la librería nativa de Vosk (la que viene dentro de com.alphacephei:vosk:0.3.45).
 *
 * Por qué no se usa org.vosk.LibVosk directamente: en macOS la librería de ese .jar no trae la función
 * vosk_recognizer_set_grm y LibVosk intenta registrarla al cargarse, así que falla con
 * UnsatisfiedLinkError antes de hacer nada. Aquí solo se declaran las funciones que usa la app.
 */
internal interface VoskC : Library {
    fun vosk_set_log_level(nivel: Int)
    fun vosk_model_new(ruta: String): Pointer?
    fun vosk_recognizer_new(modelo: Pointer, frecuencia: Float): Pointer?
    fun vosk_recognizer_new_grm(modelo: Pointer, frecuencia: Float, gramatica: String): Pointer?
    fun vosk_recognizer_accept_waveform(rec: Pointer, datos: ByteArray, largo: Int): Int
    fun vosk_recognizer_result(rec: Pointer): String
    fun vosk_recognizer_final_result(rec: Pointer): String
    fun vosk_recognizer_free(rec: Pointer)
}

internal val vosk: VoskC by lazy {
    val so = System.getProperty("os.name").lowercase()
    val (carpeta, archivos) = when {
        so.contains("mac") -> "darwin" to listOf("libvosk.dylib")
        so.contains("win") -> "win32-x86-64" to listOf("libgcc_s_seh-1.dll", "libwinpthread-1.dll", "libstdc++-6.dll", "libvosk.dll")
        else -> "linux-x86-64" to listOf("libvosk.so")
    }
    // Se copian del .jar a una carpeta temporal y se cargan desde ahí.
    val dir = Files.createTempDirectory("vosk").toFile().apply { deleteOnExit() }
    for (n in archivos) {
        val entrada = VoskC::class.java.getResourceAsStream("/$carpeta/$n") ?: error("No encuentro $carpeta/$n en el .jar de Vosk")
        entrada.use { e -> File(dir, n).apply { deleteOnExit() }.outputStream().use { e.copyTo(it) } }
    }
    archivos.dropLast(1).forEach { System.load(File(dir, it).path) }   // dependencias (solo Windows)
    Native.load(File(dir, archivos.last()).path, VoskC::class.java, mapOf(Library.OPTION_STRING_ENCODING to "UTF-8"))
        .also { it.vosk_set_log_level(-1) }
}

/** Modelo de idioma cargado en memoria (se carga una vez, tarda un par de segundos). */
class ModeloVosk(ruta: String) {
    internal val puntero: Pointer = vosk.vosk_model_new(ruta) ?: error("No se ha podido cargar el modelo de voz $ruta")
}

/** Reconocedor. Con [gramatica] solo devuelve esas palabras; con null, habla libre. Hay que cerrarlo (use { }). */
class ReconocedorVosk(modelo: ModeloVosk, frecuencia: Float, gramatica: String? = null) : AutoCloseable {
    private val rec: Pointer = (if (gramatica == null) vosk.vosk_recognizer_new(modelo.puntero, frecuencia)
        else vosk.vosk_recognizer_new_grm(modelo.puntero, frecuencia, gramatica))
        ?: error("No se ha podido crear el reconocedor")
    /** true cuando detecta el silencio del final de una frase: entonces [resultado] tiene la frase. */
    fun aceptar(datos: ByteArray, largo: Int): Boolean = vosk.vosk_recognizer_accept_waveform(rec, datos, largo) != 0
    val resultado: String get() = vosk.vosk_recognizer_result(rec)
    val resultadoFinal: String get() = vosk.vosk_recognizer_final_result(rec)
    override fun close() = vosk.vosk_recognizer_free(rec)
}
