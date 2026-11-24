package es.dam.entrena

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnguloTest {
    @Test fun recto() = assertEquals(180f, angulo(Punto(0f, 0f, 1f), Punto(0f, 0.5f, 1f), Punto(0f, 1f, 1f)), 0.5f)
    @Test fun maniquiDePie() = assertEquals(175f, anguloRodilla(posturaManiqui(175f, 0f))!!, 1f)
    @Test fun maniquiAbajo() = assertEquals(90f, anguloRodilla(posturaManiqui(90f, 0f))!!, 1f)
    @Test fun sinTobilloNoHayAngulo() =
        assertEquals(null, anguloRodilla(Postura(posturaManiqui(175f, 0f).puntos - Parte.TOBILLO_IZQ)))
}

class ContadorTest {
    private fun contar(vararg grados: Float) = ContadorRepeticiones(100f, 160f).also { c -> grados.forEach { c.nuevoAngulo(it) } }.repeticiones
    @Test fun una() = assertEquals(1, contar(175f, 90f, 175f))
    @Test fun medioBajarNoCuenta() = assertEquals(0, contar(175f, 120f, 175f))
    @Test fun tembloresNoCuentan() = assertEquals(1, contar(175f, 99f, 101f, 99f, 159f, 161f, 159f, 161f))
    @Test fun tres() = assertEquals(3, contar(170f, 80f, 170f, 85f, 165f, 95f, 170f))
}

class BrazosTest {
    @Test fun enCruz() = assertTrue(brazosEnCruz(posturaManiqui(175f, 90f)))
    @Test fun abajo() = assertFalse(brazosEnCruz(posturaManiqui(175f, 0f)))
    @Test fun aMedias() = assertFalse(brazosEnCruz(posturaManiqui(175f, 45f)))
    @Test fun mantenida() {
        val m = PosturaMantenida(2000)
        assertFalse(m.actualizar(true, 0)); assertFalse(m.actualizar(true, 1900))
        assertTrue(m.actualizar(true, 2000)); assertFalse(m.actualizar(true, 2500))   // avisa una sola vez
    }
    @Test fun soltarReinicia() {
        val m = PosturaMantenida(2000)
        m.actualizar(true, 0); m.actualizar(false, 1500)
        assertFalse(m.actualizar(true, 2000))                                         // vuelve a empezar
    }
}

class EncuadreTest {
    @Test fun maniquiSeVeEntero() = assertEquals(Encuadre.CORRECTO, encuadre(posturaManiqui(175f, 0f)))
    @Test fun maniquiAbajoSeVeEntero() = assertEquals(Encuadre.CORRECTO, encuadre(posturaManiqui(60f, 90f)))
    @Test fun nadie() = assertEquals(Encuadre.SIN_PERSONA, encuadre(Postura(emptyMap())))
    @Test fun sinPies() = assertEquals(Encuadre.PARCIAL,
        encuadre(Postura(posturaManiqui(175f, 0f).puntos - Parte.TOBILLO_IZQ - Parte.TOBILLO_DER)))
    @Test fun maniquiDentroDeLaImagen() {
        for (r in listOf(60f, 90f, 180f)) for (b in listOf(0f, 90f))
            posturaManiqui(r, b).puntos.values.forEach { assertTrue(it.x in 0f..1f && it.y in 0f..1f, "fuera: $r $b ${it.x} ${it.y}") }
    }
}
