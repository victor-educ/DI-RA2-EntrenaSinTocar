package es.dam.entrena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

enum class Pantalla { INICIO, ENTRENAMIENTO, RESUMEN }

/**
 * Raíz de la app, igual en Android y en el ordenador.
 * Aquí se juntan las tres entradas (voz, postura y toque) y todas acaban en ControlEntrenamiento.orden().
 */
@Composable
fun App() = TemaEntrena {
    val reloj = remember { TimeSource.Monotonic.markNow() }
    fun ahora(): Long = reloj.elapsedNow().inWholeMilliseconds

    val hablante = rememberHablante()
    val escucha = rememberEscucha()
    val micro = rememberPermiso(Recurso.MICROFONO)

    var pantalla by remember { mutableStateOf(Pantalla.INICIO) }
    var rutina by remember { mutableStateOf(Rutina.SUAVE) }
    var control by remember { mutableStateOf(ControlEntrenamiento(rutina, hablante::decir)) }
    var ahoraMs by remember { mutableLongStateOf(0L) }
    var postura by remember { mutableStateOf<Postura?>(null) }

    // Indicador de voz: estado y texto. Vuelve a «escuchando» 2 s después de reconocer algo.
    var voz by remember { mutableStateOf(EstadoVoz.INACTIVO) }
    var detalleVoz by remember { mutableStateOf("") }
    var vozHastaMs by remember { mutableLongStateOf(0L) }
    var ultimoNoEntendidoMs by remember { mutableLongStateOf(-10_000L) }

    // Medidas en vivo (para el informe): imágenes analizadas por segundo.
    var fps by remember { mutableIntStateOf(0) }
    var cuentaImagenes by remember { mutableIntStateOf(0) }

    // La cámara y Vosk avisan desde otros hilos: se pasa todo por canales y se trata en el hilo de la interfaz.
    val posturas = remember { Channel<Postura>(Channel.CONFLATED) }
    val frases = remember { Channel<String>(Channel.UNLIMITED) }

    fun empezarEntrenamiento() {
        control = ControlEntrenamiento(rutina, hablante::decir)
        pantalla = Pantalla.ENTRENAMIENTO
    }

    /** Todas las órdenes pasan por aquí, vengan de donde vengan. */
    fun orden(accion: Accion, origen: Origen) {
        when (pantalla) {
            Pantalla.INICIO -> if (accion == Accion.EMPEZAR) empezarEntrenamiento()
            Pantalla.ENTRENAMIENTO, Pantalla.RESUMEN -> control.orden(accion, origen, ahora())
        }
    }

    /** Una frase oída (o escrita en el modo prueba). */
    fun frase(texto: String) {
        val t = ahora()
        val accion = interpretarVoz(texto)
        if (accion == null) {
            voz = EstadoVoz.NO_ENTENDIDO; detalleVoz = ""; vozHastaMs = t + 2500
            // Se dice como mucho cada 5 s, para no ser pesada si hay ruido.
            if (t - ultimoNoEntendidoMs > 5000) { ultimoNoEntendidoMs = t; hablante.decir(Frases.NO_ENTENDIDO) }
        } else {
            voz = EstadoVoz.RECONOCIDO; detalleVoz = accion.nombre.lowercase(); vozHastaMs = t + 2000
            orden(accion, Origen.VOZ)
        }
    }

    // Escucha continua mientras haya permiso de micrófono.
    val alOir by rememberUpdatedState<(String) -> Unit>({ frases.trySend(it) })
    DisposableEffect(micro.estado) {
        if (micro.estado == EstadoPermiso.CONCEDIDO) escucha.empezar { alOir(it) }
        onDispose { escucha.parar() }
    }
    LaunchedEffect(Unit) {
        for (texto in frases) {
            // Mientras la app habla (y medio segundo después) no se hace caso al micrófono:
            // si no, se oiría a sí misma.
            if (!hablante.hablando) frase(texto)
        }
    }
    LaunchedEffect(Unit) {
        for (p in posturas) {
            postura = p
            cuentaImagenes++
            if (pantalla == Pantalla.ENTRENAMIENTO) control.postura(p, ahora())
        }
    }
    // Reloj de 100 ms: tiempo en pantalla, avance automático y vuelta del indicador de voz.
    LaunchedEffect(Unit) {
        var ultimoSegundo = 0L
        while (true) {
            ahoraMs = ahora()
            control.tick(ahoraMs)
            if (ahoraMs / 1000 != ultimoSegundo) { ultimoSegundo = ahoraMs / 1000; fps = cuentaImagenes; cuentaImagenes = 0 }
            if (ahoraMs > vozHastaMs) {
                voz = if (escucha.estado == EstadoMicro.APAGADO) EstadoVoz.INACTIVO else EstadoVoz.ESCUCHANDO
                detalleVoz = ""
            }
            delay(100)
        }
    }
    // Al terminar la rutina (por voz, botón o porque se acaba) se va al resumen.
    LaunchedEffect(control.fase) {
        if (control.fase == Fase.TERMINADO && pantalla == Pantalla.ENTRENAMIENTO) pantalla = Pantalla.RESUMEN
    }

    val medidas = buildString {
        append("Postura: $fps imágenes/s")
        append(" · Voz: ").append(escucha.latenciaMs?.let { "$it ms" } ?: "sin medir")
        escucha.error?.let { append("\n").append(it) }
    }

    when (pantalla) {
        Pantalla.INICIO -> PantallaInicio(rutina, { rutina = it }, ::empezarEntrenamiento, voz, detalleVoz)
        Pantalla.ENTRENAMIENTO -> PantallaEntrenamiento(
            control = control, ahoraMs = ahoraMs, postura = postura,
            alDetectar = { posturas.trySend(it) },
            voz = voz, detalleVoz = detalleVoz,
            alOrden = { orden(it, Origen.TOQUE) },
            alEscribir = { frase(it) },
            medidas = medidas,
        )
        Pantalla.RESUMEN -> PantallaResumen(
            control, ahoraMs, voz, detalleVoz,
            alFavorito = { orden(Accion.FAVORITO, Origen.TOQUE) },
            alVolver = { pantalla = Pantalla.INICIO },
        )
    }
}
