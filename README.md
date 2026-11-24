# Entrena sin tocar

Práctica del RA2 de Desarrollo de Interfaces (2º DAM). Es una app para entrenar en casa sin tocar la pantalla:
cuenta sentadillas y zancadas con la cámara y se maneja con la voz, con los brazos en cruz o con botones.
Funciona en Android y en el ordenador (Kotlin Multiplatform + Compose).

Autor: Víctor Sellés Estevan. Lo he hecho solo, así que todas las partes (diseño, código, pruebas, informe y vídeo) son mías.

- Figma: https://www.figma.com/design/JPCeH8JbT22ENefbJLK1GY/DI-RA2---Entrena-sin-tocar
- El informe de herramientas y el vídeo van en el ZIP de la entrega.

## Cómo ejecutarlo

Hace falta JDK 21 y, para Android, el SDK. La primera vez necesita Internet porque Gradle descarga
los modelos de postura y de voz.

```bash
./gradlew :desktopApp:run            # ordenador
./gradlew :androidApp:installDebug   # móvil conectado por USB
./gradlew :shared:jvmTest            # tests
./gradlew :desktopApp:medidas        # medidas del informe (carpeta medidas/)
```

En el móvil hay que dar permiso de cámara y micrófono en la pantalla de inicio. Si no hay cámara o micrófono,
el «Modo prueba» de la pantalla de entrenamiento tiene un maniquí y un campo para escribir órdenes.

## Órdenes

| Acción | Voz | Postura | Toque |
|---|---|---|---|
| Empezar / seguir | «empezar», «sigue» | brazos en cruz 2 s | botón |
| Pausa | «pausa», «para» | brazos en cruz 2 s | botón |
| Siguiente | «siguiente» | - | botón |
| Terminar | «terminar» | - | botón (pide confirmación) |
| Ayuda | «ayuda» | - | botón |
| Favorito | «favorito» (en el resumen) | - | botón |

## Organización

Casi todo está en `shared/src/commonMain`. `ControlEntrenamiento.kt` recibe todas las órdenes con su origen
e ignora la misma orden repetida en menos de 1 s. `Pantallas.kt` y `App.kt` tienen las pantallas. La voz y la
cámara de cada plataforma están en `androidMain` y `jvmMain`.

Nota: en macOS la librería de Vosk 0.3.45 falla al cargarse (le falta una función nativa), por eso hay un
`VoskNativo.kt` que la enlaza a mano.

## Licencias

Kotlin, Compose, AndroidX, CameraX, OpenCV, Vosk y su modelo en español, y el modelo MediaPipe Pose son
Apache 2.0. ML Kit tiene los términos de Google ML Kit. El proyecto parte de la solución del Tema 6 del profesor.
