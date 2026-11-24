package es.dam.entrena

/**
 * Todo lo que la app dice en voz alta. Está junto en un sitio para poder comprobar con un test
 * que ninguna frase contiene una orden de voz: si no, la app se oiría a sí misma y se obedecería.
 * Por eso no se usa nunca «para», «sigue», «siguiente», «pausa», «ayuda»…
 */
object Frases {
    fun empezarSerie(serie: Serie): String =
        "Vamos con ${serie.ejercicio.nombre.lowercase()}: ${serie.objetivo} repeticiones"
    fun objetivoConseguido(proxima: Serie?): String =
        if (proxima == null) "Objetivo conseguido. Era la última serie"
        else "Objetivo conseguido. Ahora toca ${proxima.ejercicio.nombre.lowercase()}, en tres segundos"
    const val DESCANSO = "Descanso. Cuando quieras, cruza los brazos"
    const val REANUDAR = "Vamos allá"
    const val FIN = "Rutina completada. Buen trabajo"
    const val NO_ENTENDIDO = "No te he entendido"
    const val VER_PANTALLA = "Mira la pantalla: ahí tienes las órdenes"
    const val GUARDADA = "Rutina guardada como preferida"

    /** Todas las frases posibles, para el test. */
    fun todas(): List<String> {
        val series = Rutina.entries.flatMap { it.series }
        return series.map { empezarSerie(it) } + series.map { objetivoConseguido(it) } + objetivoConseguido(null) +
            listOf(DESCANSO, REANUDAR, FIN, NO_ENTENDIDO, VER_PANTALLA, GUARDADA)
    }
}
