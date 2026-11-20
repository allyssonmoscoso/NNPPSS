# Arquitectura — NNPPSS 1.0 (JavaFX)

## Puntos de entrada

- `com.squarepeace.nnppss.Launcher`: clase `main` que no extiende
  `Application`; la usa jpackage cuando JavaFX viaja en el classpath.
- `com.squarepeace.nnppss.MainApp`: `Application` de JavaFX; inicializa
  servicios, carga `MainView.fxml`, aplica el tema y arranca el controlador.

## Capas

```
MainApp
  └── AppContext (servicios de larga vida)
        ├── ConfigManager
        ├── GameRepository
        ├── DownloadService / DownloadCoordinator
        ├── PackageService
        ├── DatabaseManager
        ├── DownloadQueueManager / DownloadStateManager / DownloadHistoryManager
        └── SystemValidator

MainController ──► DownloadCoordinator ──► DownloadClient (DownloadService)
ConfigController ──► ConfigManager            PackageExtractor (PackageService)
DownloadListController ──► DownloadQueueManager
```

- `AppContext` se crea una vez y se inyecta en los controladores; los
  controladores no construyen servicios.
- `DownloadCoordinator` concentra la lógica de descargas y no conoce JavaFX.
  Emite eventos a un `Listener`; el controlador los marshalea al hilo de FX.
- `FXMLLoader` recibe el `ResourceBundle` de `I18n`, de modo que los FXML usan
  `%claves` y no literales.
- `GameFilter` contiene la lógica pura de filtrado y se prueba sin UI.

## Hilos

- La UI solo se modifica en el *JavaFX Application Thread*.
- Las descargas corren en un `ExecutorService` del coordinador, dimensionado por
  `simultaneousDownloads` (1–4).
- Las cargas de catálogo usan `Task` con un `Thread` daemon y comprueban una
  generación para descartar resultados obsoletos.
- `PackageService` ejecuta pkg2zip en un hilo daemon y notifica por callback.
- `MainApp.stop()` guarda el estado, apaga el coordinador y libera los servicios.

## Persistencia

| Archivo | Responsable | Escritura |
|---|---|---|
| `config.properties` | `ConfigManager` | directa |
| `download-queue.json` | `DownloadQueueManager` | atómica |
| `download-state.json` | `DownloadStateManager` | atómica |
| `download-history.json` | `DownloadHistoryManager` | atómica |
| `logs/*.log` | logback | rotativa |

`JsonStore` escribe en un temporal y reemplaza con `ATOMIC_MOVE`, evitando que
un lector vea un archivo a medio escribir.

## Rutas

`AppPaths` distingue:

- `appDir()`: ubicación del JAR (o directorio de trabajo en desarrollo); es de
  solo lectura en una instalación y se usa para localizar `lib/pkg2zip`.
- `dataDir()`: `%APPDATA%\NNPPSS`, `~/Library/Application Support/NNPPSS` o
  `$XDG_DATA_HOME/NNPPSS`; se puede sobrescribir con `-Dnnppss.dataDir`.

La primera ejecución copia configuración, bases de datos y archivos pequeños
del layout antiguo, y deja `.migrated-from-legacy` para no repetirlo.

## Empaquetado

`scripts/package-windows.ps1` y `scripts/package-unix.sh`:

1. `mvn -DskipTests package` genera el JAR y copia dependencias a
   `target/app/lib`.
2. Copian el JAR a `target/app/NNPPSS.jar` y pkg2zip cuando existe.
3. Ejecutan `jpackage` con `Launcher` como clase principal y
   `--enable-native-access=ALL-UNNAMED`.

El JAR incluye `Class-Path: lib/...` para funcionar tanto en la imagen de
jpackage como en una ejecución manual con dependencias.
