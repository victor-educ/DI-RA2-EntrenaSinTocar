package es.dam.entrena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Las tres variantes del componente «Tarjeta de permiso» de Figma. */
enum class EstadoPermiso { SIN_PEDIR, CONCEDIDO, DENEGADO }

/**
 * Componente «Tarjeta de permiso» de Figma.
 * Propiedades de Figma → parámetros: Estado → [estado], Recurso → [recurso], Motivo → [motivo].
 */
@Composable
fun TarjetaPermiso(
    estado: EstadoPermiso,
    recurso: String,
    motivo: String,
    alPedir: () -> Unit,
    alAbrirAjustes: () -> Unit,
) {
    // Variables de color de Figma: fondo de cada variante.
    val fondo = when (estado) {
        EstadoPermiso.SIN_PEDIR -> Color(0xFFFFF3E0)   // naranja claro
        EstadoPermiso.CONCEDIDO -> Color(0xFFE0F7F4)   // verde agua
        EstadoPermiso.DENEGADO -> Color(0xFFFDE7E7)    // rojo claro
    }
    val titulo = when (estado) {
        EstadoPermiso.SIN_PEDIR -> "Necesito el $recurso"
        EstadoPermiso.CONCEDIDO -> "Ya tengo el $recurso"
        EstadoPermiso.DENEGADO -> "Has rechazado el $recurso"
    }
    Column(
        Modifier.fillMaxWidth().background(fondo, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),   // auto layout vertical, separación 8
    ) {
        Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        if (estado != EstadoPermiso.CONCEDIDO) Text(motivo, fontSize = 16.sp, color = Color(0xFF111827))
        when (estado) {
            EstadoPermiso.SIN_PEDIR -> Button(onClick = alPedir) { Text("Permitir $recurso") }
            EstadoPermiso.DENEGADO -> {
                // Tras dos rechazos Android ya no vuelve a preguntar: queda la vía de Ajustes.
                Button(onClick = alPedir) { Text("Volver a pedirlo") }
                Text("Si no aparece la pregunta, pulsa «Abrir Ajustes» y permítelo allí.", fontSize = 16.sp, color = Color(0xFF111827))
                Button(onClick = alAbrirAjustes) { Text("Abrir Ajustes") }
            }
            EstadoPermiso.CONCEDIDO -> {}
        }
    }
}
