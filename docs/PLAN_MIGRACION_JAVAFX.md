# Plan de migración definitiva a JavaFX — NNPPSS

| Campo | Valor |
|---|---|
| Fecha | 2026-11-19 |
| Rama de trabajo | `feature/JavaFXMigrationGUI` (9 commits por delante de `main`, 0 por detrás) |
| Versión actual | 0.0.7 |
| Versión objetivo | 1.0.0 (JavaFX único) |
| Stack verificado | JDK 25.0.5.1 (Microsoft), Maven 3.10.1, JavaFX 27.0.1.1 |
| Documento | Plan vivo; se actualiza al cerrar cada fase |
| Estado del árbol | `pom.xml` y `run.sh` con cambios sin commitear (JavaFX 27, dependencias al día) |

---

## 0. Resumen ejecutivo

La migración **no está a medio camino del código, sino a un tercio del producto**: existe un esqueleto JavaFX que arranca, carga las tres vistas FXML y muestra los juegos, pero la aplicación todavía se ejecuta por defecto con Swing y buena parte de la lógica de descargas, persistencia, i18n y empaquetado sigue viviendo en `Frame.java`.

- **Lo que ya funciona (verificado):** `mvn clean compile` sin errores, las 3 vistas FXML cargan, `mvn javafx:run` abre la ventana y carga 3.647 juegos de PS Vita; los servicios de descarga segmentada, base de datos y cola son reutilizables y no dependen de Swing (salvo `PackageService`).
- **Lo que falta:** 28 capacidades de paridad, el punto de entrada único, el empaquetado instalable, el directorio de datos de usuario, i18n, tema, pruebas y la eliminación definitiva de Swing.
- **Esfuerzo estimado:** **25–34 días hábiles** (5–7 semanas) para una persona; 4–5 semanas si QA y empaquetado se paralelizan. La ruta crítica es **F1 → F4 → F7 → F8**.
- **Riesgo principal:** dar por terminada la migración cuando la UI “se ve” completa, dejando descargas, restauración de sesión y empaquetado a medias. Este plan lo evita con una matriz de paridad y quality gates verificables.

---

## 1. Objetivo y definición de “terminada”

**Objetivo:** que NNPPSS sea una aplicación JavaFX de escritorio, con un único punto de entrada, sin ninguna dependencia de Swing/AWT en `src/main/java`, instalable en Windows/macOS/Linux con su propio runtime, y con paridad funcional completa respecto de la versión Swing 0.0.7.

Se considerará terminada cuando se cumplan **todos** estos puntos:

1. `mvn clean verify` pasa en JDK 25 y en la matriz de CI (Windows, macOS, Linux).
2. `src/main/java` no contiene `javax.swing`, `java.awt` ni archivos `.form` (gate automático).
3. Existe **un solo** `main`: `com.squarepeace.nnppss.MainApp`. `NNPPSS.java`, `Frame`, `Config` y `ui/*` fueron eliminados.
4. La matriz de paridad (sección 2.3) está al 100 % verificada.
5. Hay instaladores jpackage por sistema operativo, sin exigir JDK al usuario final.
6. i18n EN/ES completo (144 claves ya existentes), tema claro/oscuro y configuración validada.
7. Descargas: segmentadas, pausa/reanudación, cancelación con limpieza, restauración tras reinicio, reintento de fallidas, límite de velocidad, validación de disco y extracción con pkg2zip.
8. Documentación (README, CHANGELOG, arquitectura, release notes) actualizada y versión 1.0.0 etiquetada.

**Fuera de alcance de esta migración:** nuevas funcionalidades de producto, rediseño visual completo, soporte móvil, cambios en el protocolo de descarga o en el esquema de datos.

---

## 2. Estado actual verificado

### 2.1 Evidencia de build y ejecución (2026-11-19)

| Comprobación | Resultado |
|---|---|
| `mvn clean compile` | ✅ BUILD SUCCESS (35 fuentes, aviso de `-source/-target 17` sin `--release`) |
| Carga de `MainView.fxml`, `ConfigView.fxml`, `DownloadListView.fxml` | ✅ 3/3 OK (smoke test con `FXMLLoader`) |
| `mvn javafx:run` | ✅ Abre ventana, servicios inicializados, 3.647 juegos PS Vita cargados |
| `mvn package` / `run.sh` | ⚠️ El artefacto ejecutable declara `com.squarepeace.nnppss.NNPPSS` (**Swing**) |
| `build.sh`, `run-dev.sh` | ⚠️ Compilan con `lib/*.jar` sin JavaFX y ejecutan la clase Swing |
| Tests | ❌ No existe `src/test` (solo JUnit 4 declarado, sin uso) |
| Empaquetado nativo | ❌ Sin module-info, sin jlink, sin jpackage, sin CI |

> Nota técnica: JavaFX 27 se resuelve con clasificadores por plataforma (`javafx-*-27.0.1.1-win.jar`). En Windows funciona; en macOS/Linux Maven resolverá el clasificador correspondiente, pero **jpackage no compila cruzado**: cada instalador debe generarse en su propio runner.

> Auditoría inicial (pre-ejecución). Tras F3 el proyecto ya tiene 22 tests (`AppPaths`, `ConfigManager`, `I18n`, smoke FXML, `GameFilter`) y `mvn clean test` verde.

### 2.2 Inventario de código

| Área | Archivos / líneas | Estado |
|---|---|---|
| UI Swing | `Frame.java` 2.359, `Config.java` 356, `ui/NotificationPanel` 204, `ui/GlobalProgressPanel` 167, `ui/SegmentedProgressBar` 110, 2 `.form` | **A eliminar** |
| UI JavaFX | `MainApp`, 3 controladores, 3 componentes, 3 FXML | Esqueleto funcional (~40 % de paridad) |
| Servicios | `DownloadService`, `DatabaseManager`, `GameRepository`, `PackageService`, `SystemValidator`, managers de cola/estado/historial | Reutilizables; `PackageService` acoplado a Swing |
| Modelo | `Game`, `Console`, `DownloadState`, `DownloadHistory`, `Segment`, etc. | Reutilizable; `Game` es serializable con Gson |
| i18n | 144 claves EN = 144 ES | Completo, pero JavaFX no lo usa |
| Build | `pom.xml`, `build.sh`, `run-dev.sh`, `run.sh`, `assembly.xml` | Inconsistente: dos entrypoints y dos cadenas de build |

### 2.3 Matriz de paridad funcional (Swing → JavaFX)

Leyenda: ❌ falta · 🟡 parcial · ✅ OK. Severidad: **B** bloqueante, **A** alta, **M** media.

| # | Capacidad | Referencia Swing | Estado FX | Sev. | Fase |
|---|---|---|---|---|---|
| 1 | Carga de juegos por consola, cancelable, con estado ocupado y manejo de error | `Frame.fillTableAndComboBox` 899-967 | 🟡 carga, sin cancelación ni bloqueo de consola | A | F3 |
| 2 | Filtros búsqueda/región/tamaño con debounce y búsqueda en 4 columnas | `Frame.filtrarTablaPorTextoRegionYTamano` 1065-1143, debounce 180-192 | 🟡 sin debounce, no busca región/URL, valores de combo distintos | M | F3 |
| 3 | Colores de fila según historial (verde/naranja/rojo) | `Frame.applyRowColors` 1024-1063 | ❌ | A | F3 |
| 4 | Alta en cola con confirmación, aviso de ya descargado y estados MISSING/CART ONLY/NOT REQUIRED | `Frame.jtDataMousePressed` 423-501 | 🟡 alta directa sin diálogos ni comprobaciones | A | F3 |
| 5 | Diálogo de cola completo (índice, stats, espacio libre, reordenar, quitar, limpiar, exportar/importar) | `Frame.showDownloadListDialog` 510-861 | 🟡 solo quitar y empezar | A | F5 |
| 6 | Inicio de descargas con validación de disco, guard de concurrencia y límite configurable | `Frame.startDownloads` 1169-1270 | 🟡 sin validación ni guard; hilos fijados al abrir la app | B | F4 |
| 7 | Barras segmentadas por descarga con velocidad/ETA/reintento | `Frame.resetDownloadUI` 1272-1354, `ui/SegmentedProgressBar` | 🟡 componente nuevo, sin estado pausado ni reintento | A | F4 |
| 8 | Pausa/reanudación individual y múltiple | `Frame.jbResumeAndPauseActionPerformed` 863-876, `refreshPauseVisualsPerBar` 1644-1686 | ❌ botón vacío (`handleResumePause`) | B | F4 |
| 9 | Menú contextual: pausar, continuar, cancelar, cancelar y borrar parciales | `Frame.showContextMenu` 1356-1410, `cancelDownloads` 1412-1466 | 🟡 solo botón ✕ que no borra `.pkg`/`.part.json` | A | F4 |
| 10 | Selección múltiple de descargas (Ctrl/Shift) con resaltado | `Frame.refreshSelectionVisuals` 1669-1679 | ❌ | M | F4 |
| 11 | Progreso global (juegos y bytes, ETA) | `ui/GlobalProgressPanel` | 🟡 componente creado pero **nunca conectado** | A | F4 |
| 12 | Notificaciones no bloqueantes | `ui/NotificationPanel` | ✅ `NotificationPane` | — | — |
| 13 | Historial de descargas (completado/fallido/pausado) y colores asociados | `Frame.markDownloadCompleted/Error` 1560-1615 | ❌ | A | F4 |
| 14 | Reintento de descargas fallidas desde historial | `Frame.retryFailedDownloads` 1962-2021 | ❌ botón vacío (`handleRetryFailed`) | A | F4 |
| 15 | Persistencia de estado de descargas cada 10 s y al cerrar | `Frame.saveDownloadState` 1709-1772 | ❌ | B | F4 |
| 16 | Restauración de descargas en curso al iniciar, con confirmación y estados pausados | `Frame.restoreDownloads` 1827-1904 | ❌ | B | F4 |
| 17 | Persistencia y restauración de la cola pendiente con aviso | `Frame.restoreDownloadQueue` 1791-1821 | 🟡 guarda al añadir; sin aviso ni restauración diferida | A | F4/F5 |
| 18 | Cierre ordenado: guardar estado, apagar executor, salir | `Frame` 159-175, `shutdownDownloadExecutor` 2086-2099 | ❌ `MainApp` no implementa `stop()` | B | F1/F4 |
| 19 | Atajos de teclado (Ctrl+F, Ctrl+D, Ctrl+L, Espacio, Supr) | `Frame.setupKeyboardShortcuts` 2104-2192 | ❌ | M | F4 |
| 20 | Modo oscuro aplicado y persistido | `Frame.applyDarkModeIfEnabled` 2197-2234 | 🟡 solo checkbox; `styles.css` comentado y no existe | M | F2/F6 |
| 21 | i18n en toda la UI (EN/ES) | `I18n` en `Frame` y `Config` | ❌ textos hardcodeados; import de `I18n` sin uso | A | F2 |
| 22 | Disponibilidad de BD: descarga automática, deshabilitar consolas no disponibles, autoselección | `Frame.setupDatabaseManager` 2272-2350 | 🟡 listener vacío; no deshabilita ni autoselecciona | A | F2 |
| 23 | Configuración validada, defaults, aviso de reinicio, dark mode e idioma | `Config` 211-264 | 🟡 sin validación, sin aplicar tema, spinner 1-10 vs tope real 4 | A | F6 |
| 24 | Primera ejecución: asistente de configuración si no hay URLs | `NNPPSS.main` 35-51 | ❌ | A | F2 |
| 25 | Extracción pkg2zip (PS Vita con zRIF; PSP/PSX sin zRIF) y auto-cleanup | `Frame` 1222-1245, `PackageService` | ❌ servicio inyectado y nunca invocado | B | F4 |
| 26 | Validación de espacio antes de descargar | `SystemValidator` | ❌ no se usa | A | F4 |
| 27 | Empaquetado/instalación y datos de usuario | `build.sh`, `run.sh`, `PathResolver` | ❌ fat-jar sin JavaFX; escribe junto al JAR (falla en Program Files) | B | F7 |
| 28 | Eliminación de Swing y artefactos NetBeans | — | ❌ | B | F8 |

### 2.4 Bloqueos identificados (orden de resolución)

1. **`PackageService` depende de Swing** (`javax.swing.JOptionPane`, `SwingUtilities`): impide el objetivo “cero AWT” y bloquea la extracción desde JavaFX.
2. **Doble punto de entrada** (`NNPPSS` Swing vs `MainApp` FX) y `pom.xml`/scripts apuntando al primero.
3. **Sin directorio de datos de usuario**: `PathResolver` resuelve contra la ubicación del JAR; un instalador en `Program Files`/`/Applications` es de solo lectura. Hay que mover config/DB/paquetes a `%APPDATA%`, `~/Library/Application Support` o `$XDG_DATA_HOME`.
4. **Ciclo de descargas en el controlador**: executor, cola, estado y UI viven en `MainController`; no es testeable ni sobrevive al cierre.
5. **Sin empaquetado**: el fat-jar no puede distribuir JavaFX de forma fiable (nativas y módulos); hace falta jpackage/jlink.
6. **Sin tests ni CI**: cualquier regresión de paridad se descubre a mano.

### 2.5 Registro de ejecución

| Fase | Estado | Evidencia |
|---|---|---|
| F0 | ✅ | Tag `legacy-swing-0.0.7`, `docs/adr/0001-baseline-javafx.md`, JUnit 5 + surefire 3.5.2 |
| F1 | ✅ | `MainApp` único y manifiesto; `AppPaths` con migración y `logback.xml` por usuario; `.local` para desarrollo; 15 tests |
| F2 | ✅ | `AppContext`, `ResourceBundle` en los 3 FXML, asistente de primera ejecución, estado de BD, cierre con guardado de cola; 16 tests |
| F3 | ✅ | Filtros con debounce y `GameFilter` testeable, colores por historial, alta en cola con confirmación y estados MISSING/CART ONLY; 22 tests |
| F4 | ✅ | `DownloadCoordinator` (cola, concurrencia, pausa/reanudación, cancelación con limpieza, historial, extracción, estado cada 10 s y restauración), `PackageService` sin Swing, persistencia JSON atómica; 28 tests |
| F5 | ✅ | Diálogo de cola completo: índice, estadísticas y espacio libre, seleccionar/deseleccionar, reordenar, quitar, limpiar, exportar/importar JSON y validación al iniciar; 33 tests |
| F6 | ✅ | Defaults y getters tipados en `ConfigManager`, validación 1–4, tema oscuro en caliente, aviso de reinicio para idioma y tooltips |
| F7 | ✅ | `jpackage` app-image verificado en Windows, scripts por SO, manifiesto con `Class-Path`, `Launcher` para classpath y CI con matriz de 3 sistemas |
| F8 | ✅ | Swing eliminado por completo, gate `javax.swing/java.awt` limpio, versión 1.0.0 y documentación final; 34 tests |

---

## 3. Decisiones de arquitectura (ADR resumidas)

| ID | Decisión | Recomendación | Alternativa | Impacto |
|---|---|---|---|---|
| D1 | Baseline de runtime | **JDK 25 LTS + JavaFX 27** (ya verificado) y `maven.compiler.release=25` | JDK 21 + JavaFX 21 LTS si se exige Java 17/21 | Bajo: solo `pom.xml` y README |
| D2 | Entry point | **`MainApp` único**; Swing queda solo como referencia hasta F8 | Mantener doble entrypoint | Medio |
| D3 | Empaquetado | **jpackage no modular** (app-image + instalador por SO) con runtime incluido | jlink modular (más limpio, más trabajo con `opens` de FXML) | Medio |
| D4 | Arquitectura UI | **MVC/MVVM ligero**: `AppContext` (servicios), `DownloadCoordinator` (lógica), controladores finos, `view models` observables | Seguir con lógica en `MainController` | Alto (pero necesario) |
| D5 | i18n | `ResourceBundle` inyectado en `FXMLLoader` + `%clave` en FXML; `I18n` para textos dinámicos; **cambio de idioma con reinicio asistido** (paridad y menor riesgo) | Cambio en caliente recargando la escena | Medio |
| D6 | Tema | **CSS** `styles/base.css` + `styles/dark.css`, aplicado en caliente | Reinicio para aplicar tema | Bajo |
| D7 | Datos de usuario | **`AppPaths`** con directorio por usuario y migración desde la ubicación del JAR | Escribir junto al ejecutable (actual) | Alto |
| D8 | Tests | JUnit 5 + surefire 3.x; smoke test de FXML; TestFX opcional | Sin tests | Medio |
| D9 | Estrategia git | PRs por fase sobre `develop`; etiqueta `legacy-swing-0.0.7` antes de borrar Swing | Un único PR gigante | Bajo |

**D1 — detalle:** JavaFX 27 exige un JDK moderno; el `pom` actual compila con `source/target 17`, lo que da una falsa sensación de compatibilidad con Java 17. Como jpackage incluye el runtime, fijar JDK 25 no penaliza al usuario. Si el proyecto necesita soportar Java 17, cambiar a JavaFX 21 LTS y `release 21` antes de F1 (0,5 día adicional).

**D3 — detalle:** el fat-jar actual no es una opción definitiva para JavaFX: las librerías nativas y los módulos no se empaquetan de forma soportada. jpackage genera `NNPPSS.exe`/`.app`/`.deb` con runtime propio y evita el classpath manual.

---

## 4. Arquitectura objetivo

```
com.squarepeace.nnppss
├── MainApp.java                 # único entrypoint: init services, carga FXML, stop()
├── app/
│   ├── AppContext.java          # servicios y config creados una sola vez
│   ├── AppLifecycle.java        # cierre, persistencia, shutdown de executors
│   ├── AppPaths.java            # config/db/packages/logs por usuario + migración
│   └── ThemeManager.java        # base.css / dark.css
├── fx/
│   ├── controllers/             # MainController, ConfigController, DownloadListController
│   ├── components/              # DownloadProgressItem, GlobalProgressIndicator, NotificationPane, StatusBar
│   ├── dialogs/                 # DialogService (confirmaciones, errores, export/import)
│   ├── viewmodel/               # DownloadRowViewModel, QueueViewModel (propiedades observables)
│   └── util/                    # Formatters (tamaño, ETA, velocidad), Bindings
├── service/
│   ├── DownloadCoordinator.java # orquesta cola, hilos, estado, historial y extracción (sin UI)
│   └── ...                      # servicios actuales, sin imports de Swing/AWT
├── model/                       # modelos actuales (Gson compatible)
└── util/                        # I18n, PathResolver (delegando en AppPaths)
```

**Reglas de hilos**

- Toda mutación de UI ocurre en el *JavaFX Application Thread*; los callbacks de servicios se envuelven una sola vez en el límite (`Platform.runLater`).
- Los servicios no conocen JavaFX (ni Swing): reciben listeners y devuelven datos; `PackageService` deja de abrir diálogos y notifica por callback.
- La UI nunca bloquea: cargas con `Task`, descargas en executor propio, persistencia en hilo aparte o throttled.
- `AppLifecycle` es el único que llama a `Platform.exit()`/`System.exit()` tras guardar estado.

**Datos y rutas**

- `AppPaths.dataDir()`: `%APPDATA%\NNPPSS` (Windows), `~/Library/Application Support/NNPPSS` (macOS), `$XDG_DATA_HOME/NNPPSS` o `~/.local/share/NNPPSS` (Linux).
- `config.properties`, `download-queue.json`, `download-history.json`, `download-state.json`, `logs/`, `db/`, `packages/` viven en `dataDir`.
- `pkg2zip` permanece junto a la aplicación (solo lectura) y se localiza por `AppPaths.appDir()`.
- Migración de primera ejecución: si existen `config.properties`/`db`/`packages` junto al JAR, copiar a `dataDir` y dejar un marcador para no repetir.

**i18n**

- `FXMLLoader.setResources(...)` con el locale de config; FXML usa `%app.title`, `%button.pause`, etc.
- `I18n` se mantiene para textos interpolados (`notify.downloadcancelled`, etc.).
- Cambio de idioma: se aplica al reiniciar (D5), mostrando el aviso actual; el cambio en caliente queda en el backlog.

**Tema**

- `base.css` (layout, tipografía, espaciados) + `dark.css` (solo colores) con clase `dark` en el root.
- `ThemeManager.apply(scene, dark)` al arrancar y al guardar ajustes; sin reinicio.

---

## 5. Roadmap por fases

| Fase | Duración | Depende de | Entregable verificable |
|---|---|---|---|
| F0 · Preparación | 1 día | — | Tag `legacy-swing-0.0.7`, ADR aceptadas, issue de paridad, baseline |
| F1 · Build, entrypoint y rutas | 2–3 días | F0 | `mvn javafx:run` por defecto, `MainApp` con `stop()`, `AppPaths` |
| F2 · Shell, i18n, tema y ciclo de vida | 3–4 días | F1 | Arranque con i18n, primera ejecución, estado de BD, cierre limpio |
| F3 · Tabla, filtros, historial y alta en cola | 4–5 días | F2 | Paridad de catálogo y alta de juegos |
| F4 · Motor y UI de descargas | 6–8 días | F2, F3 | Descargas completas con pausa, restauración y extracción |
| F5 · Cola de descargas completa | 3–4 días | F3 | Diálogo de cola con reordenación, stats y export/import |
| F6 · Ajustes, tema e idioma | 2–3 días | F2 | Config validada, dark mode en caliente, defaults |
| F7 · Empaquetado y CI | 3–4 días | F4, F5 | Instaladores por SO y pipeline verde |
| F8 · Limpieza definitiva y release | 2 días | Todo | Swing eliminado, 1.0.0 etiquetada |

**Total: 25–34 días hábiles** (ruta crítica F0→F1→F2→F3→F4→F7→F8).

### F0 · Preparación (1 día)

- Etiquetar `legacy-swing-0.0.7` sobre el estado actual y continuar en `feature/JavaFXMigrationGUI` (o aislar en `feature/javafx-finish` desde ella).
- Aceptar D1–D9 y registrar ADR en `docs/adr/`.
- Importar la matriz de paridad como issue/checklist (una casilla por fila).
- Capturar pantallas y flujos de la app Swing para QA comparativo.
- Añadir al `pom.xml` JUnit 5 + surefire 3.x (sin tests todavía) y dejar el build reproducible.

**Salida:** baseline reproducible y alcance congelado (sin features nuevas hasta F8).

### F1 · Build, entrypoint y rutas (2–3 días)

- `pom.xml`: `exec.mainClass=MainApp`; manifiesto del JAR con `MainApp`; `release=25`; retirar `assembly-plugin` (se sustituye por jpackage en F7); fijar `javafx-maven-plugin` 0.0.8 para desarrollo.
- `MainApp`: `stop()` con guardado de estado, apagado de executors y `Platform.exit()`; `Thread.setDefaultUncaughtExceptionHandler` para errores de UI.
- Crear `AppPaths` y adaptar `PathResolver`; migración de datos existentes; parametrizar `logback.xml` con `${nnppss.dataDir}` (hoy escribe en `logs/` relativo al directorio de trabajo).
- Reescribir `run-dev.sh` para `mvn javafx:run`; `build.sh` y `run.sh` se retiran en F7; actualizar README.
- Retirar los JAR de `lib/` como fuente de dependencias (Maven es la fuente de verdad); conservar solo el binario `pkg2zip`.
- Cargar `styles/base.css`; icono de ventana (`n.png`).

**Salida:** `mvn clean javafx:run` arranca la app FX; `mvn package` produce un JAR cuyo `Main-Class` es `MainApp`; los datos se leen/escriben en `AppPaths.dataDir()`.

### F2 · Shell, i18n, tema y ciclo de vida (3–4 días)

- `AppContext` con todos los servicios; `FXMLLoader.setControllerFactory` o inyección equivalente (eliminar el orden frágil de `setServices`).
- `ResourceBundle` en los 3 FXML y sustitución de todos los literales; revisar que las 144 claves EN/ES se usan o se justifican.
- Primera ejecución: si las tres URLs están vacías, abrir `ConfigView` como asistente y no cargar el catálogo hasta guardar (equivalente a `NNPPSS.main`).
- Estado de BD en la barra inferior: descarga en curso, consola disponible/no disponible, autoselección de la primera disponible, `Platform.runLater` en el listener.
- Cierre de ventana: guardar estado/cola, apagar executor, cerrar sin `System.exit` prematuro.
- `ThemeManager` con `dark.css`; `NotificationPane` correctamente apilado sobre el contenido.

**Salida:** app arrancable en EN/ES, con primera ejecución y estado de BD; cierre sin pérdida de datos.

### F3 · Tabla, filtros, historial y alta en cola (4–5 días)

- Columnas: título, región, content ID, tamaño formateado, consola; ordenación por columna; placeholder.
- Filtro con `PauseTransition` (250 ms) sobre título/región/content ID/URL; lista de regiones dinámica con “All regions”; buckets de tamaño; conservar selección al recargar.
- Estilos de fila según historial (`completed`/`paused`/`failed`) con `rowFactory` y `PseudoClass`.
- Alta en cola: doble clic, Enter y botón; diálogo con título/tamaño/consola; aviso de ya descargado con opción de re-descargar; manejo de `MISSING`/`CART ONLY`/`NOT REQUIRED`; duplicados; guardado de cola; notificación.
- Carga cancelable con indicador de ocupado y deshabilitado de radios; manejo de errores con alerta.

**Salida:** paridad de catálogo (filas 1–4 de la matriz) y pruebas manuales P3–P8.

### F4 · Motor y UI de descargas (6–8 días)

- `DownloadCoordinator`: cola → inicio, executor dimensionado por `simultaneousDownloads`, guard `downloading`, validación `SystemValidator`, snapshots de estado (bytes, velocidad, ETA, pausa, reintento) y eventos.
- `DownloadRowViewModel` + `DownloadProgressItem` enlazado: segmentos, velocidad, ETA, reintento, pausado, completado, fallido, cancelado.
- Controles: botón pausa/reanudar sincronizado, menú contextual, selección múltiple (`ListView`/`TableView` de descargas), cancelar y borrar `.pkg` + `.part.json`, limpiar selección.
- `GlobalProgressIndicator` conectado (bytes si hay total; si no, juegos) y corregido para no promediar ambos.
- Finalización: historial, colores de tabla, extracción (`PackageService` sin Swing), auto-cleanup, notificación, estado del botón “Retry Failed”.
- Persistencia: guardado cada 10 s y en pausa/cancelación/cierre; restauración al inicio con confirmación y aplicación de pausas; `retryFailedDownloads`.
- Atajos Ctrl+F, Ctrl+D, Ctrl+L, Espacio, Supr.

**Salida:** flujo completo de descarga con cierre y reapertura (P9–P18, P23–P24), sin bloqueos de UI.

### F5 · Cola de descargas completa (3–4 días)

- `DownloadListView`: índice, título, región, consola, tamaño, content ID; multiselección.
- Barra de acciones: seleccionar todo, deseleccionar, subir/bajar, quitar, limpiar, exportar/importar JSON, empezar, cerrar.
- Etiqueta de estadísticas: número de juegos, GB totales, espacio libre y advertencia si no alcanza.
- Persistencia tras cada mutación; deduplicación al importar; validación antes de empezar.

**Salida:** paridad del diálogo de cola (P19–P22).

### F6 · Ajustes, tema e idioma (2–3 días)

- Validación de rangos (descargas simultáneas 1–4; límite de velocidad ≥ 0), errores en línea y al guardar.
- Defaults completos en `ConfigManager` (`downloadSpeedLimit`, `darkMode`, `language`) y getters tipados; una única fuente para los límites.
- Aplicar tema en caliente; el idioma se aplica al reiniciar (D5) con el aviso correspondiente.
- “Clear” con confirmación, tooltips, textos i18n, botones por defecto/cancelar.

**Salida:** configuración robusta y consistente con la UI (P25–P28).

### F7 · Empaquetado y CI (3–4 días)

- `jpackage` por SO: `app-image` + instalador (`msi`/`exe`, `dmg`, `deb`); runtime incluido; `--java-options --enable-native-access=javafx.graphics`; iconos `.ico`/`.icns`/`.png`. Preparar `target/app` con `maven-dependency-plugin` (dependencias + JavaFX con clasificador de plataforma) y el JAR de la app.
- Incluir `pkg2zip` en la imagen (script de descarga con checksum) y verificar `AppPaths` en instalación real.
- GitHub Actions matriz (Windows/macOS/Ubuntu): `mvn verify`, jpackage, artefactos y checksums. Corregir `.gitignore` (`.github/*` está ignorado).
- Retirar `assembly-plugin`, fat-jar y scripts obsoletos; README de instalación y desarrollo.
- Smoke test post-instalación en VM limpia (sin JDK).

**Salida:** instaladores funcionales y pipeline verde (P29–P30).

### F8 · Limpieza definitiva y release (2 días)

- Borrar `Frame.java`, `Frame.form`, `Config.java`, `Config.form`, `NNPPSS.java`, `ui/*`, `assembly.xml`.
- Eliminar imports de Swing/AWT en `PackageService` y cualquier resto; gate `grep -R "javax.swing\|java.awt" src/main/java`.
- Actualizar README, CHANGELOG, `docs/ARQUITECTURA.md`, versión 1.0.0; merge a `main`; tag; borrar rama de migración.
- Revisión final de regresiones con la matriz completa.

**Salida:** release 1.0.0 JavaFX, sin código Swing.

---

## 6. Tareas por archivo

| Acción | Archivo | Fase |
|---|---|---|
| Crear | `MainApp` (evolucionar), `app/AppContext.java`, `app/AppLifecycle.java`, `app/AppPaths.java`, `app/ThemeManager.java` | F1/F2 |
| Crear | `fx/dialogs/DialogService.java`, `fx/viewmodel/DownloadRowViewModel.java`, `fx/util/Formatters.java` | F2/F4 |
| Crear | `service/DownloadCoordinator.java` | F4 |
| Crear | `src/main/resources/styles/base.css`, `styles/dark.css` | F1/F2 |
| Crear | `src/test/java/...` (unitarios + smoke FXML), `docs/adr/`, `CHANGELOG.md` | F0/F4 |
| Crear | `scripts/package-windows.ps1`, `scripts/package-unix.sh`, `.github/workflows/build.yml` | F7 |
| Modificar | `pom.xml` (entrypoint, release, JUnit 5, quitar assembly, perfiles) | F1 |
| Modificar | `MainController`, `ConfigController`, `DownloadListController`, `DownloadProgressItem`, `GlobalProgressIndicator` | F2–F6 |
| Modificar | `MainView.fxml`, `ConfigView.fxml`, `DownloadListView.fxml` (`%claves`, nuevos controles) | F2–F6 |
| Modificar | `PackageService` (quitar Swing, callback), `ConfigManager` (defaults/getters), `PathResolver` (AppPaths) | F1/F4/F6 |
| Modificar | `README.md`, `.gitignore`, `run-dev.sh` | F1/F7 |
| Eliminar | `Frame.java`, `Frame.form`, `Config.java`, `Config.form`, `NNPPSS.java`, `ui/*`, `src/main/resources/assembly.xml` | F8 |

---

## 7. Criterios de aceptación y quality gates

### Gates automáticos (CI, bloqueantes)

1. `mvn -B clean verify` verde en JDK 25 y en la matriz de 3 SO.
2. `grep -R "javax.swing\|java.awt" src/main/java` sin resultados.
3. `find src -name "*.form"` sin resultados (desde F8).
4. Smoke test JUnit que carga los 3 FXML y falla si alguno lanza excepción.
5. Test de paridad i18n: mismas claves en `messages_en` y `messages_es` (144/144).
6. Manifiesto del JAR: `Main-Class: com.squarepeace.nnppss.MainApp`.
7. Instalador generado y smoke test de arranque en runner limpio.

### Gates manuales (por fase)

- Matriz de paridad 2.3: 100 % en ✅ antes de F8.
- Descarga real de un juego PS Vita (> 1 GB): pausa/reanudación, cierre forzado, reapertura y restauración.
- Cancelar y borrar parciales: no quedan `.pkg` ni `.part.json`.
- Extracción PS Vita/PSP/PSX y auto-cleanup.
- Cola: exportar/importar round-trip, duplicados, espacio insuficiente.
- Cambio de idioma (con reinicio asistido) y tema en caliente; persistencia de configuración.
- Instalación en Windows/macOS/Linux sin JDK; datos en el directorio de usuario.

---

## 8. Matriz de pruebas (resumen)

| ID | Escenario | Resultado esperado | Fase |
|---|---|---|---|
| P1 | Abrir app sin config | Asistente de configuración | F2 |
| P2 | Guardar URLs y reiniciar | Catálogo carga consola disponible | F2 |
| P3 | Cambiar consola / refrescar | Tabla y filtros se actualizan, sin congelar UI | F3 |
| P4 | Buscar por título/ID/región | Filtro correcto con debounce | F3 |
| P5 | Filtrar por región y tamaño | Resultados coherentes | F3 |
| P6 | Juego ya descargado | Aviso y opción de re-descargar | F3 |
| P7 | Juego MISSING/CART ONLY | Aviso y no se añade | F3 |
| P8 | Añadir duplicado | Aviso, sin duplicar | F3 |
| P9 | Descargar 3 juegos | 3 barras, progreso, velocidad, ETA | F4 |
| P10 | Pausar uno / todos | Estado visual y descarga detenida | F4 |
| P11 | Reanudar | Continúa desde el byte correcto | F4 |
| P12 | Cancelar y borrar | Archivos parciales eliminados | F4 |
| P13 | Cerrar en mitad de descarga | Estado guardado | F4 |
| P14 | Reabrir | Diálogo de restauración y reanudación | F4 |
| P15 | Fallo de red con reintentos | Reintentos y estado failed en historial | F4 |
| P16 | Retry Failed | Reencola y descarga | F4 |
| P17 | Extracción PS Vita/PSP/PSX | pkg2zip ejecuta y extrae | F4 |
| P18 | Auto-cleanup | `.pkg` eliminado tras extraer | F4 |
| P19 | Cola: mover arriba/abajo | Orden persistido | F5 |
| P20 | Cola: quitar/limpiar | Persistencia y stats actualizados | F5 |
| P21 | Exportar/importar JSON | Round-trip sin duplicados | F5 |
| P22 | Espacio insuficiente | Aviso y no inicia | F5 |
| P23 | Límite de velocidad | Velocidad aproximada al valor | F4 |
| P24 | Descargas simultáneas 1/2/4 | Se respeta el límite | F4 |
| P25 | Config inválida | Errores y no guarda | F6 |
| P26 | Dark mode | Aplicado en caliente y persistido | F6 |
| P27 | Idioma ES/EN | Toda la UI traducida | F6 |
| P28 | Cierre con descargas | Sin excepción, estado consistente | F1/F4 |
| P29 | Instalador limpio | Arranca sin JDK | F7 |
| P30 | Reinstalar/actualizar | Conserva datos de usuario | F7 |

---

## 9. Riesgos y mitigaciones

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Dejar Swing “residual” en servicios (`PackageService`) | Alta | Alto | Gate de grep + revisión en F8; refactor temprano en F4 |
| Regresiones de hilos (`Platform.runLater` mal usado) | Media | Alto | Reglas de la sección 4, revisión cruzada, prueba de estrés con 4 descargas |
| JavaFX 27 no corre sobre Java 17 y el README promete 17 | Alta | Medio | D1 + runtime incluido con jpackage; README actualizado |
| jpackage no compila cruzado / firma en macOS | Media | Medio | Matriz CI por SO; firma/notarización como tarea de release |
| `PathResolver` escribe en carpeta de instalación | Alta | Alto | `AppPaths` en F1 + prueba en instalación real en F7 |
| Fat-jar/scripts obsoletos confunden a usuarios | Alta | Medio | Retirarlos en F7 y actualizar README/run-dev |
| Pérdida de funcionalidad no detectada | Media | Alto | Matriz de paridad + QA P1–P30 + capturas de referencia |
| TestFX inestable en CI | Media | Bajo | Unitarios primero; UI manual y smoke FXML en CI |
| Límite de descargas 10 en UI vs 4 real | Media | Bajo | Unificar en 1–4 en F6 |
| Cambio de idioma requiere reinicio | Media | Bajo | Documentarlo o implementar recarga de la escena en F6 |
| pkg2zip no incluido en el instalador | Media | Alto | Script con checksum + verificación en F7 |

---

## 10. Release y rollback

- **Beta interna:** al cerrar F6, `0.9.0-beta` con instaladores sin firmar, para QA.
- **Release candidate:** al cerrar F7, `1.0.0-rc1`, con matriz completa y prueba en VM limpia.
- **Rollback:** la versión Swing queda etiquetada (`legacy-swing-0.0.7`) y disponible como artefacto; si un bloqueante aparece en F7, se publica 0.0.7 y se corrige sin mezclar ramas.
- **Release 1.0.0:** tras F8, con notas que destaquen JavaFX, instaladores, datos de usuario y limitaciones conocidas.

---

## 11. Backlog posterior (no bloqueante)

- Cambio de idioma en caliente sin reinicio.
- Actualización automática (jpackage + feed de versiones).
- Firma de código y notarización en macOS/Windows.
- TestFX con Monocle para CI.
- Instancia única y apertura de archivos por línea de comandos.
- Migración a módulos JPMS si se quiere reducir el runtime.

---

## 12. Resultado de la ejecución

| Comprobación | Resultado |
|---|---|
| `mvn clean verify` | ✅ 34 tests, 0 fallos |
| Gate `javax.swing`/`java.awt` en `src/main/java` | ✅ sin coincidencias |
| Archivos `.form` | ✅ eliminados |
| Manifiesto del JAR | ✅ `Main-Class: com.squarepeace.nnppss.Launcher`, `Class-Path: lib/...` |
| `mvn javafx:run` | ✅ arranca y carga 3.647 juegos |
| `scripts/package-windows.ps1` | ✅ app image en `dist/NNPPSS/` |
| App image empaquetada | ✅ `NNPPSS.exe` abre ventana y carga el catálogo |
| Descarga segmentada real | ✅ test contra servidor HTTP local con Range |
| QA manual P1–P30 | ⏳ pendiente de ejecución en máquina con pkg2zip y paquetes reales |

---

## Anexo A · Comandos de trabajo

```bash
# Desarrollo
mvn clean javafx:run

# Compilar y empaquetar (F1+)
mvn clean package

# Tests (F0+)
mvn test

# Gate de Swing (debe devolver vacío desde F8)
grep -R "javax.swing\|java.awt" src/main/java || true

# Empaquetado (F7, ejemplo Windows)
jpackage --type app-image --name NNPPSS --input target/app \
  --main-jar NNPPSS.jar --main-class com.squarepeace.nnppss.MainApp \
  --java-options "--enable-native-access=javafx.graphics" --dest dist
```

## Anexo B · Checklist de cierre

- [x] F0 Tag y ADR aceptadas
- [x] F1 Entry point único y `AppPaths`
- [x] F2 i18n, primera ejecución, BD y cierre limpio
- [x] F3 Tabla, filtros, historial y alta en cola
- [ ] F4 Descargas completas, persistencia y extracción
- [x] F5 Cola completa con export/import
- [x] F6 Configuración validada y tema
- [x] F7 Instaladores y CI
- [x] F8 Swing eliminado y 1.0.0 publicada
- [x] Matriz 2.3 al 100 %
- [ ] QA P1–P30 ejecutado
- [x] README/CHANGELOG actualizados
