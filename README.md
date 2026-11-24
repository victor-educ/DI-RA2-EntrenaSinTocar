# Entrena sin tocar

Práctica de la UP2 (RA2, interfaces naturales de usuario) de Desarrollo de Interfaces, 2º DAM.
App de entrenamiento en casa para Android y escritorio (Kotlin Multiplatform + Compose) que se maneja
sin tocar la pantalla: con la voz, con la postura (brazos en cruz) y, si hace falta, con botones grandes.
Cuenta las repeticiones con la cámara y solo cuenta cuando la persona está bien encuadrada.

Autor: Víctor Sellés Estevan (trabajo individual, he hecho yo todas las partes: diseño en Figma,
código, pruebas, medidas, informe y vídeo).

- Repositorio: https://github.com/victor-educ/DI-RA2-EntrenaSinTocar
- Diseño en Figma: https://www.figma.com/design/JPCeH8JbT22ENefbJLK1GY/DI-RA2---Entrena-sin-tocar
- Informe de herramientas y vídeo de demostración: en el ZIP de entrega (`DI_RA2_Selles_Estevan.zip`).

## Cómo ejecutarlo

Requisitos: JDK 21, Android SDK (para la parte Android) y conexión a Internet la primera vez
(Gradle descarga el modelo de postura en `~/.di-modelos` y el de voz en `~/.vosk`).

```bash
# Escritorio (Windows, Linux o macOS)
./gradlew :desktopApp:run

# Android: instalar en el móvil conectado por USB
./gradlew :androidApp:installDebug

# Tests (lógica común: órdenes, voz, contador, encuadre, control del entrenamiento)
./gradlew :shared:jvmTest

# Medidas del informe en el ordenador (voz y postura) -> carpeta medidas/
./gradlew :desktopApp:medidas

# Capturas de todas las pantallas sin abrir la app -> carpeta capturas/
./gradlew :desktopApp:capturas
```

En el móvil hay que darle permiso de cámara y micrófono desde las tarjetas de la pantalla de inicio.
El reconocimiento de voz de Android necesita un móvil con los servicios de Google (en el emulador,
una imagen con Google Play).

## Mapa de órdenes

| Acción | Voz | Postura | Toque |
|---|---|---|---|
| Empezar / seguir | «empezar», «sigue» | brazos en cruz 2 s (al inicio o en pausa) | botón Empezar / Seguir |
| Pausa | «pausa», «para» | brazos en cruz 2 s (en marcha) | botón Pausa |
| Siguiente | «siguiente» | - | botón Siguiente |
| Terminar | «terminar» | - | botón Terminar (pide confirmación) |
| Ayuda | «ayuda» | - | botón Ayuda |
| Favorito | «favorito» (en el resumen) | - | botón Guardar como favorita |

Rutinas: Suave (sentadillas 8, zancadas 8, sentadillas 8) e Intensa (15, 16, 20).

## Cómo está hecho

Todo lo importante está en `shared/src/commonMain`, así Android y escritorio comparten el mismo código:

- `ControlEntrenamiento.kt`: único punto de entrada de las órdenes. Voz, postura y toque llaman a
  `orden(accion, origen, ahoraMs)`. La misma acción repetida en menos de 1 s se ignora (así no se pausa
  y se reanuda a la vez si digo «pausa» y cruzo los brazos). Hace el avance automático 3 s después de
  llegar al objetivo, salvo que el usuario pause, y solo cuenta en marcha y con el encuadre correcto.
- `Ordenes.kt`: acciones, orígenes y `interpretarVoz()`, que busca palabras completas («parada» no es «para»).
- `Frases.kt`: todo lo que dice la app. Un test comprueba que ninguna frase contiene una orden,
  para que la app no se obedezca a sí misma. Además, mientras habla se ignora el micrófono.
- `Contador.kt`, `Angulo.kt`, `BrazosEnCruz.kt`, `MarcadorEncuadre.kt`: lo del Tema 6 (contador con dos
  umbrales, ángulo de rodilla, postura mantenida y encuadre).
- `Pantallas.kt`, `App.kt`: Inicio, Entrenamiento y Resumen, y la unión de las tres entradas.
- `TarjetaPermiso.kt`, `MarcadorEncuadre.kt`, `IndicadorVoz.kt`: los tres componentes de Figma.
- `Voz.kt` (expect) con `Voz.android.kt` (SpeechRecognizer relanzado tras cada frase + TextToSpeech)
  y `Voz.jvm.kt` (Vosk sin Internet + voz del sistema).

Para probar sin cámara o sin micrófono, en la pantalla de entrenamiento hay un interruptor
«Modo prueba» con el maniquí (deslizadores de rodilla y brazos), un campo de orden escrita (entra por el
mismo camino que la voz) y las medidas en vivo (imágenes por segundo y latencia de la voz).

### Dos decisiones que salen de las medidas

1. Vosk en macOS: la librería nativa que trae `com.alphacephei:vosk:0.3.45` no tiene la función
   `vosk_recognizer_set_grm` y la clase `LibVosk` falla al cargarse (UnsatisfiedLinkError). Por eso
   `VoskNativo.kt` enlaza con JNA solo las funciones que uso. Funciona igual en Windows y Linux.
2. Gramática de voz: con gramática cerrada (solo las órdenes) Vosk acierta todas las órdenes, pero
   convierte frases normales en órdenes (8 de 48). Con habla libre no hay falsas órdenes pero falla mucho
   de lejos. Uso una gramática con las órdenes y unas 150 palabras frecuentes del español «de relleno»:
   97 % de órdenes bien de cerca y solo 1 de 48 frases normales tomada por orden. Las cifras están en
   `medidas/resumen.txt` y en el informe.

## Licencias de terceros

| Componente | Uso | Licencia |
|---|---|---|
| Kotlin, Compose Multiplatform, AndroidX, CameraX | base de la app | Apache 2.0 |
| ML Kit Pose Detection (Google) | postura en Android | Términos de ML Kit (gratuito, en el dispositivo) |
| MediaPipe Pose (ONNX de OpenCV Zoo) | postura en escritorio | Apache 2.0 |
| OpenCV (org.openpnp:opencv) | cámara web y red neuronal en escritorio | Apache 2.0 |
| Vosk y modelo vosk-model-small-es-0.42 | voz en escritorio, sin Internet | Apache 2.0 |
| JNA | enlace con la librería de Vosk | Apache 2.0 / LGPL 2.1 |
| SpeechRecognizer y TextToSpeech | voz en Android | parte del sistema Android |

El proyecto parte de la solución del Tema 6 del profesor (cámara, permisos, maniquí, encuadre),
que he ampliado y reorganizado para la práctica.
