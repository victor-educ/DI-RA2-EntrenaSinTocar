package es.dam.entrena

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Capturas PNG de las pantallas (360 × 800 dp a 2x) sin abrir ventanas ni cámara, con el maniquí.
 * Sirven para compararlas con Figma y para el informe. Uso: ./gradlew :desktopApp:capturas
 */
fun main(args: Array<String>) {
    val carpeta = File(args.firstOrNull() ?: "capturas").apply { mkdirs() }
    val dePie = posturaManiqui(175f, 0f)
    val agachado = posturaManiqui(95f, 0f)
    val sinPies = Postura(dePie.puntos - Parte.TOBILLO_IZQ - Parte.TOBILLO_DER)

    fun ControlEntrenamiento.sentadillas(n: Int, desde: Long): Long {
        var t = desde
        repeat(n) { postura(agachado, t); t += 100; postura(dePie, t); t += 100 }
        return t
    }

    // Entrenamiento en marcha: 5 de 8, bajando.
    val enMarcha = ControlEntrenamiento(Rutina.SUAVE).apply {
        orden(Accion.EMPEZAR, Origen.VOZ, 0)
        val t = sentadillas(5, 100)
        postura(agachado, t)
    }
    // Fuera de encuadre: no cuenta.
    val fuera = ControlEntrenamiento(Rutina.SUAVE).apply {
        orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        val t = sentadillas(3, 100)
        postura(sinPies, t)
    }
    // En pausa con los brazos en cruz a medias.
    val pausa = ControlEntrenamiento(Rutina.SUAVE).apply {
        orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        val t = sentadillas(2, 100)
        orden(Accion.PAUSA, Origen.VOZ, t)
        postura(posturaManiqui(175f, 90f), t + 100)
        postura(posturaManiqui(175f, 90f), t + 1300)
    }
    // Objetivo conseguido: cuenta atrás del avance automático.
    val cambio = ControlEntrenamiento(Rutina.SUAVE).apply {
        orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        sentadillas(8, 100)
    }
    val ayuda = ControlEntrenamiento(Rutina.SUAVE).apply {
        orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        sentadillas(1, 100)
        orden(Accion.AYUDA, Origen.VOZ, 2000)
    }
    // Resumen: rutina hecha con órdenes de los tres orígenes.
    val resumen = ControlEntrenamiento(Rutina.SUAVE).apply {
        for (x in listOf(0L, 100L, 2000L)) postura(posturaManiqui(175f, 90f), x)      // empieza con postura
        var t = sentadillas(8, 2100) + 3000; tick(t)
        orden(Accion.PAUSA, Origen.VOZ, t + 5000)
        orden(Accion.EMPEZAR, Origen.VOZ, t + 20_000)
        t = sentadillas(8, t + 20_100) + 3000; tick(t)
        t = sentadillas(5, t + 100)
        orden(Accion.TERMINAR, Origen.TOQUE, t + 60_000)
        orden(Accion.FAVORITO, Origen.VOZ, t + 62_000)
    }

    @Composable
    fun entreno(c: ControlEntrenamiento, ahora: Long, p: Postura, voz: EstadoVoz, detalle: String = "", prueba: Boolean = false) =
        PantallaEntrenamiento(c, ahora, p, {}, voz, detalle, {}, {}, "Postura: 28 imágenes/s · Voz: 850 ms",
            sinCamaraInicial = true, herramientasInicial = prueba)

    val pantallas: List<Pair<String, @Composable () -> Unit>> = listOf(
        "01 Inicio" to { PantallaInicio(Rutina.SUAVE, {}, {}, EstadoVoz.ESCUCHANDO, "") },
        "02 Entrenamiento contando" to { entreno(enMarcha, 42_000, agachado, EstadoVoz.ESCUCHANDO) },
        "03 Entrenamiento sin encuadre" to { entreno(fuera, 30_000, sinPies, EstadoVoz.NO_ENTENDIDO) },
        "04 Entrenamiento en pausa" to { entreno(pausa, 25_000, posturaManiqui(175f, 90f), EstadoVoz.RECONOCIDO, "pausa") },
        "05 Objetivo y avance automatico" to { entreno(cambio, 1800, dePie, EstadoVoz.ESCUCHANDO) },
        "06 Ayuda" to { entreno(ayuda, 2100, dePie, EstadoVoz.RECONOCIDO, "ayuda") },
        "07 Modo prueba" to { entreno(enMarcha, 42_000, agachado, EstadoVoz.ESCUCHANDO, prueba = true) },
        "08 Resumen" to { PantallaResumen(resumen, 0, EstadoVoz.RECONOCIDO, "favorito", {}, {}) },
        "09 Componentes" to { HojaComponentes() },
    )
    for ((nombre, pantalla) in pantallas) {
        val alto = if (nombre.startsWith("07") || nombre.startsWith("09")) 2400 else 1600
        val escena = ImageComposeScene(720, alto, Density(2f)) { TemaEntrena { pantalla() } }
        // Varios fotogramas para que terminen las animaciones (diálogos).
        for (ms in 0L..1500L step 100) escena.render(ms * 1_000_000)
        val imagen = escena.render(2_000_000_000)
        File(carpeta, "App - $nombre.png").writeBytes(imagen.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        escena.close()
        println("Captura: $nombre")
    }
}
