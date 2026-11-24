package es.dam.entrena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- piezas comunes

@Composable
fun BotonGrande(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, color: Color = Colores.Primario, activo: Boolean = true) {
    Button(
        onClick = alPulsar, enabled = activo,
        modifier = modifier.heightIn(min = ALTO_BOTON),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
    ) { Text(texto, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
fun BotonBorde(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = alPulsar, modifier = modifier.heightIn(min = ALTO_BOTON), shape = RoundedCornerShape(16.dp),
    ) { Text(texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Colores.Texto, textAlign = TextAlign.Center) }
}

private fun mmss(ms: Long): String {
    val s = ms / 1000
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------- Inicio

@Composable
fun PantallaInicio(
    rutina: Rutina,
    alElegir: (Rutina) -> Unit,
    alEmpezar: () -> Unit,
    voz: EstadoVoz,
    detalleVoz: String,
) {
    val camara = rememberPermiso(Recurso.CAMARA)
    val micro = rememberPermiso(Recurso.MICROFONO)
    Column(
        Modifier.fillMaxSize().background(Colores.Fondo).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Entrena sin tocar", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text("Apoya el móvil en el suelo a 2–3 m y colócate delante. La app cuenta tus repeticiones y te obedece con la voz y con los brazos en cruz.",
            fontSize = 18.sp, color = Colores.TextoSuave)

        // Tarjetas de permiso del Tema 4 (desaparecen al conceder).
        if (camara.estado != EstadoPermiso.CONCEDIDO)
            TarjetaPermiso(camara.estado, Recurso.CAMARA.nombre, "La cámara se usa para ver tu postura y contar. Las imágenes se analizan en el móvil y no se guardan.", camara.pedir, camara.abrirAjustes)
        if (micro.estado != EstadoPermiso.CONCEDIDO)
            TarjetaPermiso(micro.estado, Recurso.MICROFONO.nombre, "El micrófono se usa para tus órdenes de voz («pausa», «sigue»…). Sin él puedes usar los botones y la postura.", micro.pedir, micro.abrirAjustes)

        Text("Elige rutina", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Rutina.entries.forEach { r ->
            val elegida = r == rutina
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp)
                    .background(Colores.Superficie, RoundedCornerShape(16.dp))
                    .border(2.dp, if (elegida) Colores.Primario else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { alElegir(r) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = elegida, onClick = { alElegir(r) })
                Column {
                    Text(r.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(r.series.joinToString(" · ") { "${it.ejercicio.nombre} ${it.objetivo}" }, fontSize = 16.sp, color = Colores.TextoSuave)
                }
            }
        }
        BotonGrande("Empezar entrenamiento", alEmpezar, Modifier.fillMaxWidth(), activo = camara.estado == EstadoPermiso.CONCEDIDO)
        if (micro.estado == EstadoPermiso.CONCEDIDO) IndicadorVoz(voz, detalleVoz)
        Text("Puedes decir «empezar» para ir al entrenamiento.", fontSize = 16.sp, color = Colores.TextoSuave)
    }
}

// ---------------------------------------------------------------- Entrenamiento

/**
 * @param postura última postura (de la cámara o del maniquí), para dibujar el esqueleto.
 * @param medidas texto con las medidas en vivo (fps y latencia de voz), si se quieren ver.
 */
@Composable
fun PantallaEntrenamiento(
    control: ControlEntrenamiento,
    ahoraMs: Long,
    postura: Postura?,
    alDetectar: (Postura) -> Unit,
    voz: EstadoVoz,
    detalleVoz: String,
    alOrden: (Accion) -> Unit,
    alEscribir: (String) -> Unit,
    medidas: String,
    sinCamaraInicial: Boolean = false,
    herramientasInicial: Boolean = false,
) {
    var sinCamara by remember { mutableStateOf(sinCamaraInicial) }
    var herramientas by remember { mutableStateOf(herramientasInicial) }
    var confirmarFin by remember { mutableStateOf(false) }
    var escrita by remember { mutableStateOf("") }

    val contando = control.fase == Fase.EN_MARCHA && control.encuadre == Encuadre.CORRECTO
    val colorEstado = when (control.fase) {
        Fase.EN_MARCHA -> if (contando) Colores.Primario else Colores.Aviso
        Fase.PAUSA -> Colores.Aviso
        Fase.CAMBIO -> Colores.Primario
        else -> Colores.TextoSuave
    }

    Column(Modifier.fillMaxSize().background(Colores.Fondo).safeDrawingPadding()) {
        // Cámara con esqueleto, marcador de encuadre e indicador de voz encima.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CamaraConEsqueleto(postura, sinCamara, if (contando) Colores.Primario else Colores.Aviso, Modifier, alDetectar)
            IndicadorVoz(voz, detalleVoz, Modifier.align(Alignment.BottomCenter).padding(10.dp))
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Ejercicio, objetivo, contador grande y tiempo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(control.serie.ejercicio.nombre, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Serie ${control.indice + 1} de ${control.rutina.series.size} · Tiempo ${mmss(control.tiempoMs(ahoraMs))}", fontSize = 20.sp, color = Colores.TextoSuave)
                }
                Text(
                    "${control.hechas}", fontSize = 88.sp, fontWeight = FontWeight.Black, color = colorEstado,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(" / ${control.serie.objetivo}", fontSize = 32.sp, color = Colores.TextoSuave)
            }
            // Una sola instrucción, con color y texto.
            Text(
                control.instruccion(ahoraMs), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colorEstado,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            // Barra de la postura de pausa (brazos en cruz 2 s).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Brazos en cruz", fontSize = 16.sp, color = Colores.TextoSuave)
                LinearProgressIndicator(progress = { control.progresoCruz }, modifier = Modifier.weight(1f).height(10.dp), color = Colores.Aviso, trackColor = Color(0xFF37474F))
            }
            // Alternativa táctil de todas las órdenes.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (control.fase == Fase.EN_MARCHA || control.fase == Fase.CAMBIO)
                    BotonGrande("Pausa", { alOrden(Accion.PAUSA) }, Modifier.weight(1f), Colores.Aviso)
                else
                    BotonGrande(if (control.fase == Fase.PREPARADO) "Empezar" else "Seguir", { alOrden(Accion.EMPEZAR) }, Modifier.weight(1f))
                BotonBorde("Siguiente", { alOrden(Accion.SIGUIENTE) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonBorde("Terminar", { confirmarFin = true }, Modifier.weight(1f))
                BotonBorde("Ayuda", { alOrden(Accion.AYUDA) }, Modifier.weight(1f))
            }
            // Herramientas de prueba: maniquí y orden escrita (para probar sin cámara o sin micrófono).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modo prueba", fontSize = 14.sp, color = Colores.TextoSuave, modifier = Modifier.weight(1f))
                Switch(checked = herramientas, onCheckedChange = { herramientas = it })
            }
            if (herramientas) {
                Text(medidas, fontSize = 14.sp, color = Colores.TextoSuave)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = sinCamara, onCheckedChange = { sinCamara = it })
                    Text("  Sin cámara: usar el maniquí", fontSize = 14.sp)
                }
                if (sinCamara) Maniqui(alDetectar)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(escrita, { escrita = it }, Modifier.weight(1f), singleLine = true, label = { Text("Orden escrita") })
                    TextButton(onClick = { alEscribir(escrita); escrita = "" }) { Text("Enviar") }
                }
            }
        }
    }

    if (control.ayudaVisible) PanelAyuda(alCerrar = { control.cerrarAyuda() })
    if (confirmarFin) AlertDialog(
        onDismissRequest = { confirmarFin = false },
        title = { Text("¿Terminar el entrenamiento?", fontSize = 22.sp) },
        text = { Text("Irás al resumen con lo que llevas hecho.", fontSize = 18.sp) },
        confirmButton = { BotonGrande("Terminar", { confirmarFin = false; alOrden(Accion.TERMINAR) }) },
        dismissButton = { BotonBorde("Seguir entrenando", { confirmarFin = false }) },
    )
}

/** Ayuda de órdenes: el mapa completo, siempre a mano. */
@Composable
fun PanelAyuda(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Órdenes", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Empezar / seguir" to "«empezar», «sigue» · brazos en cruz 2 s",
                    "Pausa" to "«pausa», «para» · brazos en cruz 2 s",
                    "Siguiente ejercicio" to "«siguiente»",
                    "Terminar" to "«terminar»",
                    "Ayuda" to "«ayuda»",
                    "Favorito (en el resumen)" to "«favorito»",
                ).forEach { (accion, como) ->
                    Column {
                        Text(accion, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(como, fontSize = 18.sp, color = Colores.TextoSuave)
                    }
                }
                Text("Todo se puede hacer también con los botones.", fontSize = 16.sp)
            }
        },
        confirmButton = { BotonGrande("Cerrar", alCerrar) },
    )
}

// ---------------------------------------------------------------- Resumen

@Composable
fun PantallaResumen(control: ControlEntrenamiento, ahoraMs: Long, voz: EstadoVoz, detalleVoz: String, alFavorito: () -> Unit, alVolver: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Colores.Fondo).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Resumen", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text("Rutina ${control.rutina.nombre} · Tiempo ${mmss(control.tiempoMs(ahoraMs))}", fontSize = 22.sp)
        control.rutina.series.forEachIndexed { i, s ->
            val hechas = control.repeticiones[i]
            Row(
                Modifier.fillMaxWidth().background(Colores.Superficie, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}. ${s.ejercicio.nombre}", fontSize = 22.sp, modifier = Modifier.weight(1f))
                Text("$hechas / ${s.objetivo}", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = if (hechas >= s.objetivo) Colores.Primario else Colores.Aviso)
            }
        }
        Text("Últimas órdenes", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        if (control.historial.isEmpty()) Text("Ninguna", fontSize = 18.sp, color = Colores.TextoSuave)
        control.historial.forEach { o ->
            Text("${mmss((o.instanteMs - (control.inicioMs ?: o.instanteMs)).coerceAtLeast(0))} · ${o.accion.nombre} · por ${o.origen.nombre}",
                fontSize = 18.sp, color = Colores.TextoSuave)
        }
        if (control.favorito) Text("Guardada como favorita", fontSize = 22.sp, color = Colores.Aviso, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        else BotonGrande("Guardar como favorita", alFavorito, Modifier.fillMaxWidth(), Colores.Aviso)
        Text("También puedes decir «favorito».", fontSize = 16.sp, color = Colores.TextoSuave)
        IndicadorVoz(voz, detalleVoz)
        Spacer(Modifier.height(4.dp))
        BotonBorde("Volver al inicio", alVolver, Modifier.fillMaxWidth())
    }
}
