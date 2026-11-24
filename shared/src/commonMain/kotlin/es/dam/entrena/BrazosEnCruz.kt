package es.dam.entrena

import kotlin.math.abs

/** ¿Están los dos brazos estirados a la altura de los hombros? */
fun brazosEnCruz(p: Postura): Boolean {
    val hi = p.visible(Parte.HOMBRO_IZQ) ?: return false
    val hd = p.visible(Parte.HOMBRO_DER) ?: return false
    val mi = p.visible(Parte.MUNECA_IZQ) ?: return false
    val md = p.visible(Parte.MUNECA_DER) ?: return false
    val alturaOk = abs(mi.y - hi.y) < 0.08f && abs(md.y - hd.y) < 0.08f
    val separadas = abs(mi.x - md.x) > abs(hi.x - hd.x) * 2.5f   // muñecas mucho más abiertas que los hombros
    return alturaOk && separadas
}

/** Cuenta cuánto tiempo seguido se mantiene una postura. Avisa una vez al llegar a [duracionMs]. */
class PosturaMantenida(private val duracionMs: Long = 2000) {
    private var desde: Long? = null
    private var avisado = false
    var progreso = 0f
        private set

    /** Devuelve true solo en el instante en que se completan los 2 segundos. */
    fun actualizar(seCumple: Boolean, ahoraMs: Long): Boolean {
        if (!seCumple) { desde = null; avisado = false; progreso = 0f; return false }
        val inicio = desde ?: ahoraMs.also { desde = it }
        progreso = ((ahoraMs - inicio).toFloat() / duracionMs).coerceAtMost(1f)
        if (progreso >= 1f && !avisado) { avisado = true; return true }
        return false
    }
}
