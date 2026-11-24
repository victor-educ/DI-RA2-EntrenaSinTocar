import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)          // Compose para el sistema en el que compilas
    implementation(libs.kotlinx.coroutines.swing)      // hilo principal de la ventana
}

compose.desktop {
    application {
        mainClass = "es.dam.entrena.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "EntrenaSinTocar"
            packageVersion = "1.0.0"
        }
    }
}

// Modelos de visión para escritorio (OpenCV Zoo y OpenCV). Se descargan una vez en ~/.di-modelos.
val descargarModelos = tasks.register("descargarModelos") {
    val carpeta = File(System.getProperty("user.home"), ".di-modelos")
    val archivos = mapOf(
        "pose_estimation_mediapipe_2023mar.onnx" to "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/pose_estimation_mediapipe/pose_estimation_mediapipe_2023mar.onnx",
    )
    doLast {
        carpeta.mkdirs()
        for ((nombre, url) in archivos) {
            val destino = File(carpeta, nombre)
            if (!destino.exists() || destino.length() == 0L) {
                // Se descarga a un archivo .part y solo al terminar se renombra:
                // si la descarga se corta, no queda un modelo a medias que parezca bueno.
                val parcial = File(carpeta, "$nombre.part")
                uri(url).toURL().openStream().use { entrada -> parcial.outputStream().use { entrada.copyTo(it) } }
                destino.delete()
                check(parcial.renameTo(destino)) { "No se pudo guardar ${destino.path}" }
            }
        }
    }
}
// Modelo de voz en español de Vosk (sin Internet al usar la app). Se descarga una vez en ~/.vosk.
val descargarModeloVoz = tasks.register("descargarModeloVoz") {
    val carpeta = File(System.getProperty("user.home"), ".vosk")
    val destino = File(carpeta, "vosk-model-small-es-0.42")
    outputs.dir(destino)
    doLast {
        // Gradle crea la carpeta de salida vacía antes de ejecutar la tarea: se comprueba que tenga el modelo.
        if (!File(destino, "am/final.mdl").exists()) {
            destino.deleteRecursively()
            carpeta.mkdirs()
            val zip = File(carpeta, "modelo-es.zip")
            uri("https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip").toURL().openStream().use { entrada ->
                zip.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            project.copy { from(project.zipTree(zip)); into(carpeta) }
            zip.delete()
        }
    }
}
tasks.named("compileKotlin") { dependsOn(descargarModelos, descargarModeloVoz) }

// Medidas del informe en el ordenador (ver Medidas.kt): ./gradlew :desktopApp:medidas
tasks.register<JavaExec>("medidas") {
    group = "documentacion"
    mainClass.set("es.dam.entrena.MedidasKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(rootProject.file("medidas").path)
}

// Capturas de las pantallas para el informe (ver Capturas.kt): ./gradlew :desktopApp:capturas
tasks.register<JavaExec>("capturas") {
    group = "documentacion"
    mainClass.set("es.dam.entrena.CapturasKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(rootProject.file("capturas").path)
}
