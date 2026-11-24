package es.dam.entrena

/**
 * Cuenta repeticiones con dos umbrales (Tema 6, paso 3):
 *   ARRIBA --(ángulo < bajar)--> ABAJO --(ángulo > subir)--> ARRIBA y suma 1
 * Entre los dos umbrales no cambia nada, así el temblor del modelo no cuenta repeticiones falsas.
 */
class ContadorRepeticiones(private val bajar: Float, private val subir: Float) {
    var repeticiones = 0
        private set
    var abajo = false
        private set

    fun nuevoAngulo(grados: Float) {
        if (!abajo && grados < bajar) abajo = true
        else if (abajo && grados > subir) { abajo = false; repeticiones++ }
    }
}

/** Sentadilla: rodilla por debajo de 100° y vuelta por encima de 160° (valores del Tema 6). */
fun contadorPara(ejercicio: Ejercicio) = when (ejercicio) {
    Ejercicio.SENTADILLAS -> ContadorRepeticiones(bajar = 100f, subir = 160f)
    // En la zancada baja sobre todo la rodilla de delante y el ángulo mínimo es algo mayor.
    Ejercicio.ZANCADAS -> ContadorRepeticiones(bajar = 110f, subir = 160f)
}

/**
 * Ángulo que se usa para contar: la rodilla más doblada de las que se ven.
 * En la sentadilla las dos bajan igual; en la zancada, alternando la pierna, siempre hay una que baja.
 */
fun anguloParaContar(p: Postura): Float? = listOfNotNull(anguloRodilla(p, izquierda = true), anguloRodilla(p, izquierda = false)).minOrNull()
