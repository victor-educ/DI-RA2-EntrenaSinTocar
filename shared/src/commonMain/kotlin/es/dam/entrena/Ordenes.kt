package es.dam.entrena

/** Las acciones del mapa de órdenes. Todas se pueden pedir también tocando un botón. */
enum class Accion(val nombre: String) {
    EMPEZAR("Empezar / seguir"),
    PAUSA("Pausa"),
    SIGUIENTE("Siguiente ejercicio"),
    TERMINAR("Terminar"),
    AYUDA("Ayuda"),
    FAVORITO("Favorito"),
}

/** De dónde llega una orden. */
enum class Origen(val nombre: String) { VOZ("voz"), POSTURA("postura"), TOQUE("toque") }

/** Una orden ya aplicada, con su origen y el instante (ms desde que empezó la sesión). */
data class Orden(val accion: Accion, val origen: Origen, val instanteMs: Long)

/** Palabras de voz admitidas para cada acción (tal como salen en el mapa de órdenes). */
val PALABRAS_VOZ: Map<Accion, List<String>> = mapOf(
    Accion.EMPEZAR to listOf("empezar", "empieza", "sigue", "seguir"),
    Accion.PAUSA to listOf("pausa", "para", "parar"),
    Accion.SIGUIENTE to listOf("siguiente"),
    Accion.TERMINAR to listOf("terminar", "termina"),
    Accion.AYUDA to listOf("ayuda"),
    Accion.FAVORITO to listOf("favorito"),
)

/** Minúsculas, sin tildes y sin signos: «¡Pausa!» → «pausa». */
fun normalizar(texto: String): String {
    val sinTildes = texto.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
    return sinTildes.map { if (it.isLetter() || it == 'ñ') it else ' ' }.joinToString("").trim()
}

/**
 * Convierte una frase reconocida en una acción, o null si no contiene ninguna orden.
 * Se busca palabra a palabra (no trozos de palabra): «seguimos» no es «sigue» y «parada» no es «para».
 * Si la frase tiene varias órdenes, gana la primera que se dijo.
 */
fun interpretarVoz(frase: String): Accion? {
    for (palabra in normalizar(frase).split(' ').filter { it.isNotBlank() }) {
        for ((accion, palabras) in PALABRAS_VOZ) if (palabra in palabras) return accion
    }
    return null
}
