# Mapa de Pensum — app nativa

App Android de **Mapa de Pensum** para las carreras de la UDO Monagas: el mapa de la carrera con sus prelaciones, la lista de materias, el avance hacia la graduación y el horario de clases.

Es la versión nativa de la web: [github.com/Samestves/mapa-pensum](https://github.com/Samestves/mapa-pensum).

## Stack

- Kotlin 2.3 + Compose Multiplatform 1.10
- [Haze](https://github.com/chrisbanes/haze) para el vidrio esmerilado
- Android minSdk 26, con perfil base (baseline profile) para que arranque fluido en gama baja

## Compilar

```bash
./gradlew :composeApp:assembleRelease
```

El APK queda en `composeApp/build/outputs/apk/`. Para Android Studio basta con abrir la carpeta; `local.properties` apunta al SDK y no se sube.

## Pruebas

Las capturas de pantalla se generan con Roborazzi (Robolectric):

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*CapturaResponsive*"
```

Las imágenes quedan en `build/capturas/`.

## Documentación

- [`docs/sistema-diseno.md`](docs/sistema-diseno.md): medidas, radios, tipografía y la paleta.
- [`docs/revision-clean-code.md`](docs/revision-clean-code.md): cómo está organizado el código.
