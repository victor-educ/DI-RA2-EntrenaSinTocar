package es.dam.entrena

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VozTest {
    @Test fun ordenesDelMapa() {
        assertEquals(Accion.EMPEZAR, interpretarVoz("Empezar"))
        assertEquals(Accion.EMPEZAR, interpretarVoz("sigue"))
        assertEquals(Accion.PAUSA, interpretarVoz("¡Pausa!"))
        assertEquals(Accion.PAUSA, interpretarVoz("para"))
        assertEquals(Accion.SIGUIENTE, interpretarVoz("siguiente"))
        assertEquals(Accion.TERMINAR, interpretarVoz("terminar"))
        assertEquals(Accion.AYUDA, interpretarVoz("ayuda"))
        assertEquals(Accion.FAVORITO, interpretarVoz("favorito"))
    }
    @Test fun dentroDeUnaFrase() = assertEquals(Accion.PAUSA, interpretarVoz("vale, pausa un momento"))
    @Test fun trozosDePalabraNoCuentan() {
        assertNull(interpretarVoz("seguimos con la parada"))
        assertNull(interpretarVoz("hola qué tal"))
        assertNull(interpretarVoz(""))
    }
    @Test fun ganaLaPrimera() = assertEquals(Accion.PAUSA, interpretarVoz("pausa no sigue"))

    /** Requisito 5: la app nunca dice una orden, así no se obedece a sí misma. */
    @Test fun ningunaFraseContieneUnaOrden() {
        for (f in Frases.todas()) assertNull(interpretarVoz(f), "La frase «$f» contiene una orden")
    }
}

class ControlTest {
    private val dichas = mutableListOf<String>()
    private fun control(rutina: Rutina = Rutina.SUAVE) = ControlEntrenamiento(rutina) { dichas += it }
    private val dePie = posturaManiqui(175f, 0f)
    private val agachado = posturaManiqui(90f, 0f)

    /** Hace [n] sentadillas con el maniquí a partir del instante [desde] (una postura cada 100 ms). */
    private fun ControlEntrenamiento.sentadillas(n: Int, desde: Long): Long {
        var t = desde
        repeat(n) { postura(agachado, t); t += 100; postura(dePie, t); t += 100 }
        return t
    }

    @Test fun mismaOrdenEnMenosDeUnSegundoSeIgnora() {
        val c = control()
        assertTrue(c.orden(Accion.EMPEZAR, Origen.TOQUE, 0))
        assertTrue(c.orden(Accion.PAUSA, Origen.VOZ, 100))
        assertFalse(c.orden(Accion.PAUSA, Origen.POSTURA, 600))       // repetida: ignorada
        assertEquals(Fase.PAUSA, c.fase)
        assertTrue(c.orden(Accion.EMPEZAR, Origen.VOZ, 1200))
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun historialGuardaElOrigen() {
        val c = control()
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        c.orden(Accion.PAUSA, Origen.VOZ, 2000)
        assertEquals(listOf(Origen.VOZ, Origen.TOQUE), c.historial.map { it.origen })
    }

    @Test fun cuentaSoloEnMarchaYBienEncuadrado() {
        val c = control()
        c.sentadillas(2, 0)                                            // aún no ha empezado
        assertEquals(0, c.hechas)
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 1000)
        var t = c.sentadillas(2, 1100)
        assertEquals(2, c.hechas)
        val sinPies = Postura(agachado.puntos - Parte.TOBILLO_IZQ - Parte.TOBILLO_DER)
        c.postura(sinPies, t); t += 100; c.postura(dePie, t); t += 100  // fuera de encuadre: no cuenta
        assertEquals(2, c.hechas)
        c.orden(Accion.PAUSA, Origen.TOQUE, t)
        c.sentadillas(2, t + 100)                                      // en pausa: no cuenta
        assertEquals(2, c.hechas)
    }

    @Test fun avanceAutomaticoTresSegundos() {
        val c = control()
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        val conseguido = c.sentadillas(8, 100) - 100                    // la octava se completa al subir
        assertEquals(Fase.CAMBIO, c.fase)
        c.tick(conseguido + 2900); assertEquals(0, c.indice)
        c.tick(conseguido + 3000); assertEquals(1, c.indice)
        assertEquals(Fase.EN_MARCHA, c.fase)
        assertEquals(Ejercicio.ZANCADAS, c.serie.ejercicio)
    }

    @Test fun pausaDuranteElCambioLoDetiene() {
        val c = control()
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        val t = c.sentadillas(8, 100)
        c.orden(Accion.PAUSA, Origen.VOZ, t + 1000)
        c.tick(t + 10_000)
        assertEquals(0, c.indice)                                      // no ha avanzado
        c.orden(Accion.EMPEZAR, Origen.VOZ, t + 11_000)
        assertEquals(1, c.indice)                                      // al seguir pasa al siguiente
    }

    @Test fun brazosEnCruzEmpiezanYPausan() {
        val c = control()
        val cruz = posturaManiqui(175f, 90f)
        for (t in 0L..2000L step 100) c.postura(cruz, t)
        assertEquals(Fase.EN_MARCHA, c.fase)
        c.postura(dePie, 2100)                                         // los baja
        for (t in 3000L..5000L step 100) c.postura(cruz, t)
        assertEquals(Fase.PAUSA, c.fase)
        assertEquals(listOf(Origen.POSTURA, Origen.POSTURA), c.historial.map { it.origen })
    }

    @Test fun terminarYFavorito() {
        val c = control()
        assertFalse(c.orden(Accion.FAVORITO, Origen.VOZ, 0))           // solo en el resumen
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        c.orden(Accion.TERMINAR, Origen.TOQUE, 5000)
        assertEquals(Fase.TERMINADO, c.fase)
        assertEquals(5000, c.tiempoMs(99_999))
        assertTrue(c.orden(Accion.FAVORITO, Origen.VOZ, 7000))
        assertTrue(c.favorito)
    }

    @Test fun laAppSoloDiceFrasesDeLaLista() {
        val c = control(Rutina.INTENSA)
        c.orden(Accion.EMPEZAR, Origen.TOQUE, 0)
        c.orden(Accion.PAUSA, Origen.TOQUE, 2000)
        c.orden(Accion.AYUDA, Origen.TOQUE, 3000)
        c.orden(Accion.TERMINAR, Origen.TOQUE, 4000)
        assertTrue(dichas.isNotEmpty())
        dichas.forEach { assertTrue(it in Frases.todas(), "«$it» no está en Frases") }
    }
}
