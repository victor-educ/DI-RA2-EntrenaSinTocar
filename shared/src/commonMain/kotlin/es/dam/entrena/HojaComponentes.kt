package es.dam.entrena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Las variantes de los tres componentes de Figma juntas, para comparar con la página «Componentes». */
@Composable
fun HojaComponentes() {
    Column(Modifier.fillMaxSize().background(Colores.Fondo).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tarjeta de permiso", fontSize = 18.sp)
        for (e in EstadoPermiso.entries) TarjetaPermiso(e, Recurso.MICROFONO.nombre, "El micrófono se usa para tus órdenes de voz.", {}, {})
        Text("Marcador de encuadre", fontSize = 18.sp)
        for (e in Encuadre.entries) MarcadorEncuadre(e)
        Text("Indicador de voz", fontSize = 18.sp)
        for (e in EstadoVoz.entries) IndicadorVoz(e, "pausa")
    }
}
