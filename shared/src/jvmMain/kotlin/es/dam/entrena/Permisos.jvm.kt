package es.dam.entrena

import androidx.compose.runtime.Composable

// En escritorio la app no pide permisos: la cámara lo autoriza el sistema operativo.
@Composable
actual fun rememberPermiso(recurso: Recurso): Permiso = Permiso(EstadoPermiso.CONCEDIDO, pedir = {}, abrirAjustes = {})
