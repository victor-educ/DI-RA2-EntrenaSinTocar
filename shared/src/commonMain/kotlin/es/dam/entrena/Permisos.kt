package es.dam.entrena

import androidx.compose.runtime.Composable

/** Los recursos que piden permiso en este curso. */
enum class Recurso(val nombre: String) { CAMARA("acceso a la cámara"), MICROFONO("acceso al micrófono") }

/** Igual que en el Tema 4: estado del permiso y las dos acciones posibles. */
class Permiso(val estado: EstadoPermiso, val pedir: () -> Unit, val abrirAjustes: () -> Unit)

@Composable
expect fun rememberPermiso(recurso: Recurso): Permiso

/** Muestra [contenido] solo con el permiso concedido; si no, la tarjeta de Figma del Tema 4. */
@Composable
fun ConPermiso(recurso: Recurso, motivo: String, contenido: @Composable () -> Unit) {
    val permiso = rememberPermiso(recurso)
    if (permiso.estado == EstadoPermiso.CONCEDIDO) contenido()
    else TarjetaPermiso(permiso.estado, recurso.nombre, motivo, permiso.pedir, permiso.abrirAjustes)
}
