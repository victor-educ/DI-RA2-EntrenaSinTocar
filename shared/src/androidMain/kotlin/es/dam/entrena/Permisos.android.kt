package es.dam.entrena

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

// Es el ejemplo 3 del Tema 4, generalizado a cámara y micrófono.
@Composable
actual fun rememberPermiso(recurso: Recurso): Permiso {
    val context = LocalContext.current
    val nombre = when (recurso) {
        Recurso.CAMARA -> Manifest.permission.CAMERA
        Recurso.MICROFONO -> Manifest.permission.RECORD_AUDIO
    }
    fun yaConcedido() = ContextCompat.checkSelfPermission(context, nombre) == PackageManager.PERMISSION_GRANTED

    var estado by remember { mutableStateOf(if (yaConcedido()) EstadoPermiso.CONCEDIDO else EstadoPermiso.SIN_PEDIR) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { si ->
        estado = if (si) EstadoPermiso.CONCEDIDO else EstadoPermiso.DENEGADO
    }
    LifecycleResumeEffect(Unit) {
        if (yaConcedido()) estado = EstadoPermiso.CONCEDIDO
        onPauseOrDispose { }
    }
    return Permiso(
        estado = estado,
        pedir = { lanzador.launch(nombre) },
        abrirAjustes = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        },
    )
}
