package es.dam.entrena

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** En qué momento está el entrenamiento. */
enum class Fase { PREPARADO, EN_MARCHA, PAUSA, CAMBIO, TERMINADO }

/**
 * Único punto de entrada de las órdenes (requisito 1): voz, postura y toque llaman a [orden].
 * No sabe nada de cámaras ni micrófonos, así que se puede probar con tests y funciona igual
 * en Android y en el ordenador. El tiempo se pasa siempre como parámetro (ms) para poder simularlo.
 *
 * Sus propiedades son estado de Compose: si cambian, la pantalla se repinta sola.
 *
 * @param decir lo que la app tiene que decir en voz alta (siempre frases de [Frases]).
 */
class ControlEntrenamiento(val rutina: Rutina, private val decir: (String) -> Unit = {}) {

    var fase by mutableStateOf(Fase.PREPARADO)
        private set
    /** Serie (ejercicio) actual. */
    var indice by mutableIntStateOf(0)
        private set
    /** Repeticiones hechas en cada serie. */
    val repeticiones = mutableStateListOf<Int>().apply { repeat(rutina.series.size) { add(0) } }
    /** Últimas órdenes aplicadas, la más reciente primero. */
    val historial = mutableStateListOf<Orden>()
    var ayudaVisible by mutableStateOf(false)
        private set
    var favorito by mutableStateOf(false)
        private set
    var encuadre by mutableStateOf(Encuadre.SIN_PERSONA)
        private set
    /** Progreso (0 a 1) de los brazos en cruz mantenidos, para la barra de la pantalla. */
    var progresoCruz by mutableStateOf(0f)
        private set
    /** Cuándo termina la cuenta atrás del avance automático (solo en la fase CAMBIO). */
    var cambioHastaMs by mutableStateOf<Long?>(null)
        private set
    var abajo by mutableStateOf(false)
        private set

    /** Instante de la primera orden de empezar (null si aún no ha empezado). */
    var inicioMs: Long? = null
        private set
    private var finMs: Long? = null
    private val ultimaVez = mutableMapOf<Accion, Long>()
    private val cruz = PosturaMantenida(2000)
    private var contador = contadorPara(rutina.series[0].ejercicio)

    val serie: Serie get() = rutina.series[indice.coerceAtMost(rutina.series.lastIndex)]
    val hechas: Int get() = repeticiones[indice.coerceAtMost(rutina.series.lastIndex)]

    /** Tiempo de entrenamiento en ms (desde la primera orden de empezar). */
    fun tiempoMs(ahoraMs: Long): Long = inicioMs?.let { (finMs ?: ahoraMs) - it } ?: 0

    /**
     * Aplica una orden venga de donde venga. Devuelve true si se ha aplicado.
     * La misma acción repetida en menos de 1 s se ignora (requisito 1): así, si el usuario dice
     * «pausa» y a la vez cruza los brazos, no se pausa y se reanuda en el mismo instante.
     */
    fun orden(accion: Accion, origen: Origen, ahoraMs: Long): Boolean {
        val anterior = ultimaVez[accion]
        if (anterior != null && ahoraMs - anterior < 1000) return false
        ultimaVez[accion] = ahoraMs

        val aplicada = when (accion) {
            Accion.EMPEZAR -> when (fase) {
                Fase.PREPARADO -> { inicioMs = ahoraMs; empezarSerie(); true }
                Fase.PAUSA -> { reanudar(); true }
                else -> false
            }
            Accion.PAUSA -> when (fase) {
                Fase.EN_MARCHA, Fase.CAMBIO -> { fase = Fase.PAUSA; cambioHastaMs = null; decir(Frases.DESCANSO); true }
                else -> false
            }
            Accion.SIGUIENTE -> when (fase) {
                Fase.EN_MARCHA, Fase.PAUSA, Fase.CAMBIO -> { avanzar(ahoraMs); true }
                else -> false
            }
            Accion.TERMINAR -> if (fase != Fase.TERMINADO) { terminar(ahoraMs); true } else false
            Accion.AYUDA -> {
                ayudaVisible = !ayudaVisible
                if (ayudaVisible) decir(Frases.VER_PANTALLA)
                true
            }
            Accion.FAVORITO -> if (fase == Fase.TERMINADO && !favorito) { favorito = true; decir(Frases.GUARDADA); true } else false
        }
        if (aplicada) {
            historial.add(0, Orden(accion, origen, ahoraMs))
            while (historial.size > 12) historial.removeAt(historial.lastIndex)
        }
        return aplicada
    }

    /** Cierra la ayuda sin contar como orden (botón «Cerrar» del panel). */
    fun cerrarAyuda() { ayudaVisible = false }

    /**
     * Llega una postura nueva de la cámara (o del maniquí).
     * 1) Brazos en cruz 2 s: empieza o sigue si está parado, pausa si está en marcha.
     * 2) Solo se cuenta en marcha y con el encuadre correcto (requisito 4).
     */
    fun postura(p: Postura, ahoraMs: Long) {
        encuadre = encuadre(p)
        val enCruz = brazosEnCruz(p)
        if (cruz.actualizar(enCruz, ahoraMs)) {
            when (fase) {
                Fase.PREPARADO, Fase.PAUSA -> orden(Accion.EMPEZAR, Origen.POSTURA, ahoraMs)
                Fase.EN_MARCHA, Fase.CAMBIO -> orden(Accion.PAUSA, Origen.POSTURA, ahoraMs)
                Fase.TERMINADO -> {}
            }
        }
        progresoCruz = cruz.progreso
        if (fase != Fase.EN_MARCHA || encuadre != Encuadre.CORRECTO || enCruz) return

        val grados = anguloParaContar(p) ?: return
        contador.nuevoAngulo(grados)
        abajo = contador.abajo
        repeticiones[indice] = contador.repeticiones
        if (contador.repeticiones >= serie.objetivo) {
            fase = Fase.CAMBIO
            cambioHastaMs = ahoraMs + 3000
            decir(Frases.objetivoConseguido(rutina.series.getOrNull(indice + 1)))
        }
    }

    /** Se llama a menudo (cada 100 ms): hace el avance automático cuando acaba la cuenta atrás. */
    fun tick(ahoraMs: Long) {
        val hasta = cambioHastaMs
        if (fase == Fase.CAMBIO && hasta != null && ahoraMs >= hasta) avanzar(ahoraMs)
    }

    /** Segundos que faltan para el avance automático (para la pantalla). */
    fun segundosCambio(ahoraMs: Long): Int = cambioHastaMs?.let { ((it - ahoraMs + 999) / 1000).toInt().coerceAtLeast(0) } ?: 0

    private fun empezarSerie() {
        contador = contadorPara(serie.ejercicio)
        abajo = false
        fase = Fase.EN_MARCHA
        decir(Frases.empezarSerie(serie))
    }

    private fun reanudar() {
        // Si se pausó durante la cuenta atrás, la serie ya estaba completa: se pasa a la siguiente.
        if (hechas >= serie.objetivo) avanzar(null)
        else { fase = Fase.EN_MARCHA; decir(Frases.REANUDAR) }
    }

    private fun avanzar(ahoraMs: Long?) {
        cambioHastaMs = null
        if (indice + 1 >= rutina.series.size) terminar(ahoraMs)
        else { indice++; empezarSerie() }
    }

    private fun terminar(ahoraMs: Long?) {
        if (inicioMs != null) finMs = ahoraMs ?: finMs ?: inicioMs
        fase = Fase.TERMINADO
        cambioHastaMs = null
        ayudaVisible = false
        decir(Frases.FIN)
    }

    /** Una sola instrucción en pantalla, la más importante en cada momento. */
    fun instruccion(ahoraMs: Long): String = when (fase) {
        Fase.PREPARADO -> "Cruza los brazos 2 s o di «empezar»"
        Fase.PAUSA -> "En pausa. Brazos en cruz 2 s o di «sigue»"
        Fase.CAMBIO -> rutina.series.getOrNull(indice + 1)
            ?.let { "¡Objetivo! ${it.ejercicio.nombre} en ${segundosCambio(ahoraMs)} s. Di «pausa» para esperar" }
            ?: "¡Objetivo! Terminando en ${segundosCambio(ahoraMs)} s"
        Fase.TERMINADO -> "Rutina terminada"
        Fase.EN_MARCHA -> when {
            encuadre != Encuadre.CORRECTO -> "No cuento. ${encuadre.texto}"
            abajo -> "Bien, ahora sube"
            else -> "Baja"
        }
    }
}
