package es.dam.entrena

enum class Ejercicio(val nombre: String) { SENTADILLAS("Sentadillas"), ZANCADAS("Zancadas") }

/** Un ejercicio con su objetivo de repeticiones. */
data class Serie(val ejercicio: Ejercicio, val objetivo: Int)

enum class Rutina(val nombre: String, val series: List<Serie>) {
    SUAVE("Suave", listOf(Serie(Ejercicio.SENTADILLAS, 8), Serie(Ejercicio.ZANCADAS, 8), Serie(Ejercicio.SENTADILLAS, 8))),
    INTENSA("Intensa", listOf(Serie(Ejercicio.SENTADILLAS, 15), Serie(Ejercicio.ZANCADAS, 16), Serie(Ejercicio.SENTADILLAS, 20))),
}
