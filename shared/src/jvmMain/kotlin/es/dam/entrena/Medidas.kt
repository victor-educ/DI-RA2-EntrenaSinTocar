package es.dam.entrena

import nu.pattern.OpenCV
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.File
import javax.sound.sampled.AudioSystem
import kotlin.random.Random

/**
 * Medidas del informe de herramientas en el ordenador (./gradlew :desktopApp:medidas).
 *
 * 1. Voz (Vosk, con gramática cerrada y con habla libre): se generan las órdenes con varias voces del sistema
 *    (`say`, macOS) y se pasan al reconocedor en dos condiciones: «cerca» (audio limpio) y «lejos»
 *    (volumen al 30 % y ruido de fondo, para imitar los 2-3 m). Se mide acierto, falsas órdenes y tiempo de respuesta
 *    (desde la última palabra hasta tener el texto: incluye el silencio que Vosk espera para cerrar la frase).
 * 2. Postura: tiempo de inferencia del modelo MediaPipe Pose (ONNX con OpenCV) sobre 640×480.
 *
 * Resultado en la carpeta que se pasa como argumento: voz.csv, postura.csv y resumen.txt.
 */
fun main(args: Array<String>) {
    val salida = File(args.firstOrNull() ?: "medidas").apply { mkdirs() }
    val resumen = StringBuilder()
    medirVoz(salida, resumen)
    medirPostura(salida, resumen)
    File(salida, "resumen.txt").writeText(resumen.toString())
    println(resumen)
}

private val VOCES = listOf("Mónica", "Eddy (Español (España))", "Flo (Español (España))", "Reed (Español (España))",
    "Sandy (Español (España))", "Paulina")

/** Frases de prueba y la acción que se espera (null = no es una orden y no debe hacer nada). */
private val PRUEBAS: List<Pair<String, Accion?>> =
    PALABRAS_VOZ.flatMap { (a, palabras) -> palabras.map { it to a } } +
        listOf("hola qué tal", "me duele la pierna", "vamos bien", "qué calor hace", "esto cuesta mucho", "qué hora es", "ya casi está", "venga una más")
            .map { it to null }

private fun leerPcm(wav: File): ShortArray = AudioSystem.getAudioInputStream(wav).use { e ->
    val b = e.readAllBytes()
    ShortArray(b.size / 2) { i -> ((b[2 * i + 1].toInt() shl 8) or (b[2 * i].toInt() and 0xFF)).toShort() }
}

private fun aBytes(s: ShortArray): ByteArray = ByteArray(s.size * 2).also { b ->
    s.forEachIndexed { i, v -> b[2 * i] = (v.toInt() and 0xFF).toByte(); b[2 * i + 1] = (v.toInt() shr 8).toByte() }
}

/** Medio segundo de silencio antes y después, como al hablar de verdad. */
/** Medio segundo de silencio antes y 1,5 s después, como al hablar de verdad. */
private const val ANTES = 8000
private fun conSilencio(s: ShortArray) = ShortArray(ANTES) + s + ShortArray(24000)

/** «Lejos»: voz al 30 % y ruido de fondo, para imitar la voz a 2-3 m en una habitación. */
private fun lejos(s: ShortArray, r: Random): ShortArray =
    ShortArray(s.size) { i -> (s[i] * 0.3 + r.nextDouble(-1.0, 1.0) * 300).toInt().coerceIn(-32768, 32767).toShort() }

/** Dónde acaba la voz dentro del audio de `say` (último trozo con algo de volumen). */
private fun finDeVoz(s: ShortArray): Int {
    var i = s.size - 1
    while (i > 0 && kotlin.math.abs(s[i].toInt()) < 300) i--
    return i
}

private enum class Modo(val texto: String) { GRAMATICA("gramática cerrada"), RELLENO("gramática con relleno"), LIBRE("habla libre") }

private fun medirVoz(salida: File, resumen: StringBuilder) {
    val tmp = File(salida, "audios").apply { mkdirs() }
    val csv = StringBuilder("modo;voz;condicion;frase;esperado;reconocido;accion;acierto;latencia_ms\n")
    data class Fila(val modo: Modo, val cond: String, val esOrden: Boolean, val acierto: Boolean, val ms: Long)
    val filas = mutableListOf<Fila>()
    modeloVoz // se carga antes de medir
    for (modo in Modo.entries) {
        val r = Random(7)
        for (voz in VOCES) for ((frase, esperada) in PRUEBAS) {
            val wav = File(tmp, "${voz.take(6).replace(' ', '_')}_${frase.replace(' ', '_')}.wav")
            if (!wav.exists()) {
                val ok = ProcessBuilder("say", "-v", voz, "-o", wav.path, "--file-format=WAVE", "--data-format=LEI16@16000", frase)
                    .redirectErrorStream(true).start().waitFor() == 0
                if (!ok) continue
            }
            val voces = leerPcm(wav)
            val finVoz = ANTES + finDeVoz(voces)                    // muestra donde se deja de hablar
            val limpio = conSilencio(voces)
            for ((cond, audio) in listOf("cerca" to limpio, "lejos" to lejos(limpio, r))) {
                ReconocedorVosk(modeloVoz, 16000f, when (modo) { Modo.GRAMATICA -> gramaticaOrdenes(); Modo.RELLENO -> gramaticaConRelleno(); Modo.LIBRE -> null }).use { rec ->
                    val bytes = aBytes(audio)
                    var texto = ""
                    var i = 0
                    var proceso = 0L                                 // ms de cálculo desde el final de la voz
                    var latencia = -1L
                    while (i < bytes.size) {
                        val n = minOf(3200, bytes.size - i)          // trozos de 100 ms, como el micrófono
                        val t0 = System.nanoTime()
                        val fin = rec.aceptar(bytes.copyOfRange(i, i + n), n)
                        if ((i + n) / 2 > finVoz) proceso += (System.nanoTime() - t0) / 1_000_000
                        i += n
                        if (fin) {
                            texto = textoVosk(rec.resultado)
                            if (texto.replace("[unk]", "").isNotBlank()) {
                                // Audio que ha tenido que pasar tras la última palabra + lo que tarda en calcular.
                                latencia = ((i / 2 - finVoz) * 1000L / 16000).coerceAtLeast(0) + proceso
                                break
                            }
                        }
                    }
                    if (latencia < 0) { texto = textoVosk(rec.resultadoFinal); latencia = ((i / 2 - finVoz) * 1000L / 16000) + proceso }
                    val accion = interpretarVoz(texto.replace("[unk]", ""))
                    val acierto = accion == esperada
                    filas += Fila(modo, cond, esperada != null, acierto, latencia)
                    csv.append("${modo.name};$voz;$cond;$frase;${esperada?.name ?: "-"};$texto;${accion?.name ?: "-"};${if (acierto) 1 else 0};$latencia\n")
                }
            }
        }
    }
    File(salida, "voz.csv").writeText(csv.toString())
    resumen.append("VOZ (Vosk small-es 0.42) · ${VOCES.size} voces × ${PRUEBAS.size} frases (${PALABRAS_VOZ.values.flatten().size} órdenes y ${PRUEBAS.count { it.second == null }} que no lo son)\n")
    for (modo in Modo.entries) for (cond in listOf("cerca", "lejos")) {
        val f = filas.filter { it.modo == modo && it.cond == cond }
        val ordenes = f.filter { it.esOrden }
        val otras = f.filter { !it.esOrden }
        val lat = ordenes.filter { it.acierto }.map { it.ms }.sorted()
        resumen.append("  ${modo.texto}, $cond: órdenes bien ${ordenes.count { it.acierto }}/${ordenes.size}" +
            " (${100 * ordenes.count { it.acierto } / ordenes.size.coerceAtLeast(1)} %)" +
            " · frases normales tomadas por orden ${otras.count { !it.acierto }}/${otras.size}" +
            " · latencia media ${if (lat.isEmpty()) "-" else lat.average().toInt().toString()} ms" +
            " · p90 ${lat.getOrElse(lat.size * 9 / 10) { 0 }} ms\n")
    }
}

private fun medirPostura(salida: File, resumen: StringBuilder) {
    OpenCV.loadLocally()
    val analizador = AnalizadorPosturaEscritorio()
    val imagen = Mat(480, 640, CvType.CV_8UC3)
    org.opencv.core.Core.randu(imagen, 0.0, 255.0)
    repeat(10) { analizador.analizar(imagen) } // calentamiento
    val tiempos = (1..100).map { val t0 = System.nanoTime(); analizador.analizar(imagen); (System.nanoTime() - t0) / 1e6 }
    File(salida, "postura.csv").writeText("iteracion;ms\n" + tiempos.mapIndexed { i, t -> "${i + 1};${"%.2f".format(t)}" }.joinToString("\n"))
    val media = tiempos.average()
    val p90 = tiempos.sorted()[89]
    resumen.append("POSTURA (MediaPipe Pose ONNX + OpenCV DNN, 640×480 → 256×256) · 100 imágenes\n")
    resumen.append("  inferencia media ${"%.1f".format(media)} ms · p90 ${"%.1f".format(p90)} ms · máximo teórico ${(1000 / media).toInt()} imágenes/s\n")
    resumen.append("  Equipo: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}, ${Runtime.getRuntime().availableProcessors()} núcleos\n")
}
