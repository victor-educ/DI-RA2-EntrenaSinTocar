package es.dam.entrena

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Ángulo en grados (0 a 180) en el punto [b], formado por a-b-c.
 * Rodilla: angulo(cadera, rodilla, tobillo) → unos 175° de pie y menos de 100° agachado.
 * x se multiplica por 3/4 porque la imagen es más alta que ancha (3:4).
 */
fun angulo(a: Punto, b: Punto, c: Punto): Float {
    val ux = (a.x - b.x) * 0.75f; val uy = a.y - b.y
    val vx = (c.x - b.x) * 0.75f; val vy = c.y - b.y
    val coseno = (ux * vx + uy * vy) / (sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy))
    return (acos(coseno.coerceIn(-1f, 1f)) * 180 / PI).toFloat()
}

/** Ángulo de una rodilla, o null si no se ven la cadera, la rodilla y el tobillo de ese lado. */
fun anguloRodilla(p: Postura, izquierda: Boolean = true): Float? {
    val cadera = p.visible(if (izquierda) Parte.CADERA_IZQ else Parte.CADERA_DER) ?: return null
    val rodilla = p.visible(if (izquierda) Parte.RODILLA_IZQ else Parte.RODILLA_DER) ?: return null
    val tobillo = p.visible(if (izquierda) Parte.TOBILLO_IZQ else Parte.TOBILLO_DER) ?: return null
    return angulo(cadera, rodilla, tobillo)
}
