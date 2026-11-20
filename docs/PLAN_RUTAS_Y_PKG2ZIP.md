# Plan · Rutas de descarga, extracción y pkg2zip configurable

**Estado:** propuesta para aprobación · **Base:** 1.0.0 · **Ámbito:** descargas, extracción y ajustes.

## 1. Objetivo

Permitir que cada usuario decida, desde la interfaz:

1. Dónde se guardan los `.pkg` descargados.
2. Dónde se guarda el resultado de la extracción.
3. Qué `pkg2zip` se usa, detectando primero si ya está disponible como comando del sistema y, si no lo está, dejando que el usuario indique la carpeta donde se encuentra.

La descarga debe seguir funcionando aunque `pkg2zip` no esté disponible. En ese caso se avisa con una acción clara para configurarlo, sin bloquear el flujo principal ni mostrar errores técnicos.

## 2. Situación actual

| Punto | Comportamiento actual |
|---|---|
| Destino del `.pkg` | `DownloadCoordinator` usa la ruta fija `packages/<archivo>` (`DownloadCoordinator.java:177`). |
| Resolución de rutas | `PathResolver` resuelve todo contra `AppPaths.dataDir()`. |
| pkg2zip en Windows | Se busca solo `appDir/lib/pkg2zip.exe` (`PackageService.java:55`). |
| pkg2zip en Linux/macOS | Solo se acepta si está en `PATH`; si no, falla (`PackageService.java:65`). |
| Directorio de extracción | No se configura: `ProcessBuilder` no establece `directory`, así que pkg2zip escribe en el directorio de trabajo del proceso. |
| UI | Ajustes no tiene sección de almacenamiento ni estado de pkg2zip. |
| Validación de disco | `SystemValidator` comprueba una ruta relativa resuelta contra `dataDir`, no la carpeta elegida. |

En este entorno **no hay binario `pkg2zip` incluido**; por eso el estado “no encontrado” debe ser un caso de primera clase, no una excepción.

Referencia de uso de pkg2zip: [pkg2zip (mmozeiko)](https://context7.com/mmozeiko/pkg2zip). La invocación `-x` y los argumentos se validarán contra el binario real antes de cerrar R3.

## 3. Decisiones de producto propuestas

| Decisión | Propuesta |
|---|---|
| Carpetas | Dos rutas independientes: **Descargas** y **Extracción**. Se recomienda no usar la misma carpeta, pero se permite con advertencia. |
| Valores por defecto | Descargas: `<dataDir>/packages` (compatibilidad). Extracción: `<dataDir>/extracted`. |
| pkg2zip | Orden: ruta configurada → binario incluido con la app → comando en `PATH`. |
| Disponible en `PATH` | Se informa “Detectado en el sistema” y se usa automáticamente; el usuario puede cambiarlo. |
| No disponible | Aviso no bloqueante + botón para elegir la carpeta que contiene `pkg2zip.exe`/`pkg2zip`. |
| Cambio de rutas con descargas activas | Se aplica a las próximas descargas; no se mueven archivos ni se alteran tareas en curso. |
| Archivos existentes | No se mueven ni se borran. Las rutas antiguas siguen siendo legibles. |
| Selector | Selector de **carpeta** para pkg2zip (busca dentro el ejecutable), para que coincida con “elegir el directorio”. Si no se encuentra, error inline sin cerrar el diálogo. |

## 4. Modelo de configuración

Nuevas claves en `config.properties`:

```properties
downloads.directory=<ruta absoluta>
extraction.directory=<ruta absoluta>
pkg2zip.path=<ruta absoluta al ejecutable, opcional>
```

Reglas:

- Valores vacíos o ausentes → defaults de la sección 3.
- Guardar rutas normalizadas y absolutas.
- `pkg2zip.path` vacío significa “detectar automáticamente”.
- No guardar el resultado de la detección como si fuera una ruta elegida: el estado se calcula al abrir ajustes o al extraer.
- Si el usuario elige una carpeta para pkg2zip, se guarda la ruta del ejecutable encontrado (`pkg2zip.exe` en Windows; `pkg2zip` en Linux/macOS), no la carpeta, para evitar ambigüedad si el contenido cambia.

### Compatibilidad

- Sin claves nuevas: descargas siguen en `<dataDir>/packages`; extracción nueva en `<dataDir>/extracted`.
- Los `download-history.json` y `download-state.json` antiguos con rutas relativas se resuelven contra `dataDir` al cargarlos.
- Las tareas nuevas guardan rutas absolutas.
- No se migran automáticamente carpetas grandes de extracción; el usuario puede elegir la carpeta antigua si la tiene.

## 5. Detección de pkg2zip

Nuevo servicio `Pkg2zipResolver` (sin JavaFX) con resultado:

```text
Pkg2zipResolution
  source: CONFIGURED | BUNDLED | PATH | NOT_FOUND | INVALID
  executable: Path | null
  detail: String
```

### Orden de búsqueda

1. **Configurado:** `pkg2zip.path` existe, es archivo y es ejecutable.
2. **Incluido:** `AppPaths.appDir()/lib/pkg2zip.exe` en Windows o `.../pkg2zip` en Unix.
3. **PATH:** buscar el comando `pkg2zip` en las entradas de `PATH`, sin depender de `where`/`which` para facilitar pruebas.
4. **No encontrado.**

### Reglas

- En Windows debe ser `.exe`; en Unix debe tener permiso de ejecución.
- La detección no descarga binarios ni ejecuta nada automáticamente.
- La comprobación se hace en segundo plano al abrir ajustes; mientras tanto se muestra “Comprobando…”.
- Si el binario está en `PATH`, se muestra la ruta resuelta y el texto “Se usará automáticamente”.
- Si el usuario elige una carpeta y no contiene el ejecutable, se mantiene el diálogo abierto y se muestra un error inline.
- Si el binario desaparece después de guardarlo, se muestra `NOT_FOUND` y se permite volver a elegir.

## 6. Cambios de ejecución

### Descarga

- `DownloadCoordinator` obtiene la carpeta configurada y construye:
  `downloadsDir.resolve(game.getFileName()).toAbsolutePath()`.
- `DownloadService` acepta la ruta absoluta; `PathResolver` ya devuelve una ruta absoluta sin cambios, pero se elimina la dependencia implícita de `packages/`.
- `SystemValidator` valida espacio libre sobre la **carpeta de descargas** elegida.
- Historial y estado guardan la ruta absoluta; al cargar estados antiguos se resuelven rutas relativas contra `dataDir`.
- Si la carpeta no existe, se crea al iniciar la descarga; si no se puede crear/escribir, se cancela con un mensaje accionable (“Abrir ajustes”).

### Extracción

- `PackageService` cambia su API interna para recibir:
  - ruta absoluta del `.pkg`;
  - carpeta de extracción;
  - consola y zRIF.
- Se ejecuta con `ProcessBuilder` y una **lista de argumentos**, sin `cmd /c` ni `sh -lc`, para soportar rutas con espacios y evitar inyección:
  - Windows/Linux/macOS: `[pkg2zip, "-x", pkgPath, zRif]` para PS Vita;
  - `[pkg2zip, pkgPath]` para PSP/PSX.
- `ProcessBuilder.directory(extractionDir.toFile())` define dónde escribe pkg2zip.
- Se crea la carpeta de extracción antes de ejecutar.
- El zRIF no se registra en logs.
- Auto-cleanup usa la ruta absoluta y solo se ejecuta si la extracción terminó bien.
- Si pkg2zip no está disponible: no se intenta extraer, se registra un resultado `NOT_CONFIGURED` y se notifica con acción “Configurar”.

### Resultado de extracción

- `DownloadCoordinator` recibe un resultado más rico que `boolean`:
  - `SUCCESS`, `FAILED`, `TOOL_MISSING`, `INVALID_OUTPUT_DIR`.
- La UI muestra:
  - éxito: notificación breve;
  - herramienta ausente: aviso persistente con “Configurar”;
  - fallo: error con “Ver detalle”/“Abrir ajustes”.

## 7. UI/UX

### 7.1 Ajustes → nueva sección “Almacenamiento”

```text
┌ Almacenamiento ──────────────────────────────────────────────┐
│ Carpeta de descargas                                        │
│ [C:\...\packages                              ] [Elegir] [Abrir]
│ Los archivos .pkg se guardan aquí.  258 GB libres           │
│                                                              │
│ Carpeta de extracción                                       │
│ [C:\...\extracted                             ] [Elegir] [Abrir]
│ pkg2zip escribe aquí el resultado.                          │
│                                                              │
│ ⚠ Descargas y extracción están en la misma carpeta.         │
│   Se recomienda separarlas.                                  │
└──────────────────────────────────────────────────────────────┘
```

- Campos de solo lectura + botón “Elegir…” (DirectoryChooser).
- “Abrir” usa el gestor de archivos del sistema (sin AWT).
- “Restablecer carpetas predeterminadas” como enlace secundario.
- Espacio libre calculado sobre la carpeta elegida, no sobre `dataDir`.
- Validación inline:
  - ruta vacía;
  - no se puede crear;
  - sin permisos de escritura;
  - es un archivo, no una carpeta;
  - misma carpeta para descargas y extracción (advertencia, no bloqueo).
- Si hay descargas activas: aviso “Se aplicará a las próximas descargas”; no se mueve nada.

### 7.2 Ajustes → nueva sección “Extracción”

```text
┌ pkg2zip ─────────────────────────────────────────────────────┐
│ ● Detectado en PATH                                           │
│ C:\Tools\pkg2zip.exe                                          │
│ Se usará automáticamente.                    [Volver a detectar]│
│                                                               │
│ ○ No encontrado                                               │
│ Las descargas funcionarán, pero no se extraerán.              │
│ [Elegir carpeta de pkg2zip…]                                  │
└───────────────────────────────────────────────────────────────┘
```

Estados visuales:

| Estado | Texto | Acción |
|---|---|---|
| `PATH` | “Detectado en el sistema (PATH)” | “Cambiar” / “Volver a detectar” |
| `BUNDLED` | “Incluido con NNPPSS” | “Cambiar” |
| `CONFIGURED` | “Configurado manualmente” | “Cambiar” |
| `NOT_FOUND` | “No encontrado” | “Elegir carpeta de pkg2zip…” |
| `INVALID` | “El archivo no es ejecutable” | “Elegir otra carpeta” |
| `CHECKING` | “Comprobando…” | — |

- No depender solo del color: icono + texto.
- El botón de elegir carpeta abre un `DirectoryChooser`; al aceptar se busca `pkg2zip.exe`/`pkg2zip` dentro y se valida.
- Si falta, el diálogo sigue abierto y el error aparece bajo el estado.
- La ruta se muestra con elipsis y tooltip completo.

### 7.3 Primera ejecución

- En el paso 2 del asistente se añade un bloque compacto “Almacenamiento” con las rutas por defecto y el estado de pkg2zip.
- No se bloquea “Guardar” si pkg2zip no está: se puede configurar después.
- Si está en `PATH`, se muestra el aviso informativo sin pedir nada.
- “Configurar después” sigue disponible.

### 7.4 Flujo principal

- Antes de iniciar una descarga, si la carpeta de descargas no es escribible:
  - notificación con acción **Abrir ajustes**;
  - no se inicia la descarga.
- Si pkg2zip no está disponible:
  - la descarga continúa;
  - al terminar, notificación persistente: “pkg2zip no está configurado. El archivo se descargó, pero no se extrajo.” con **Configurar**.
- Al completar una descarga/extracción:
  - acción **Abrir carpeta de descargas** o **Abrir carpeta de extracción**.
- Los errores de extracción no borran el `.pkg` automáticamente.

### 7.5 Accesibilidad e i18n

- Etiquetas accesibles en ambos selectores y en el botón de pkg2zip.
- Navegación completa con `Tab`; foco visible.
- `Esc` cierra el diálogo sin guardar.
- Todas las cadenas en `messages.properties`, `messages_es.properties` y `messages_en.properties`.
- No usar solo color para el estado de pkg2zip.

## 8. i18n (claves nuevas)

```text
config.section.storage
config.section.storage.hint
config.label.downloadsdir
config.label.extractiondir
config.label.samefolder
config.label.freespace
button.choosefolder
button.openfolder
button.resetfolders
config.section.pkg2zip
config.pkg2zip.checking
config.pkg2zip.path
config.pkg2zip.bundled
config.pkg2zip.configured
config.pkg2zip.notfound
config.pkg2zip.invalid
config.pkg2zip.pathhint
config.pkg2zip.choose
config.pkg2zip.notfoundhint
config.pkg2zip.applynext
config.error.pathempty
config.error.pathnotwritable
config.error.pathnotdirectory
notify.pkg2zip.missing
notify.pkg2zip.invalid
notify.extraction.failed
notify.downloadpath.invalid
```

## 9. Archivos afectados

| Archivo | Cambio |
|---|---|
| `ConfigManager` | Defaults y getters/setters de rutas y pkg2zip. |
| `Pkg2zipResolver` (nuevo) | Detección configurada/incluida/PATH y validación. |
| `PathSettings`/`StorageSettings` (nuevo, opcional) | Normalización y validación de carpetas. |
| `DownloadCoordinator` | Ruta de destino configurable; estados y extracción con resultado. |
| `DownloadService` | Aceptar rutas absolutas; sin cambio de protocolo. |
| `PackageService` | API con ruta absoluta + carpeta de salida; `ProcessBuilder` con lista de argumentos y `directory`. |
| `SystemValidator` | Validar espacio en la carpeta elegida. |
| `ConfigView.fxml` / `ConfigController` | Secciones Almacenamiento y pkg2zip; selector de carpetas; validación. |
| `MainView`/`MainController` | Notificaciones accionables y apertura de carpetas. |
| `messages*.properties` | Claves ES/EN. |
| Tests | Resolver, validación, comando, coordinador y FXML. |

## 10. Fases de implementación

| Fase | Entregable | Estimación |
|---|---|---:|
| R0 | Decisiones de producto y claves de configuración | 0,5 día |
| R1 | `Pkg2zipResolver`, validación de rutas y defaults | 1,5 días |
| R2 | Propagación de rutas a descarga, estado, historial y disco | 1,5 días |
| R3 | `PackageService` con ruta absoluta y carpeta de salida | 1 día |
| R4 | Ajustes: Almacenamiento + pkg2zip, selectores y validación inline | 2 días |
| R5 | Primera ejecución, notificaciones y “Abrir carpeta” | 1 día |
| R6 | Tests, migración, i18n y QA manual | 1,5 días |
| **Total** | | **~9 días** |

## 11. Pruebas

### Automatizadas

- `Pkg2zipResolverTest`:
  - configurado válido;
  - ruta configurada inválida;
  - incluido en `lib/`;
  - encontrado en un `PATH` temporal;
  - no encontrado.
- `PackageServiceTest`:
  - comando con rutas con espacios;
  - argumentos separados, sin shell;
  - `directory` de extracción;
  - auto-cleanup solo tras éxito.
- `PathSettingsTest`: normalización, carpeta inexistente creable, archivo en lugar de carpeta, sin permisos (cuando el sistema lo permita).
- `DownloadCoordinatorTest`: destino bajo la carpeta configurada; rutas antiguas compatibles.
- `FxmlSmokeTest`: nuevos `fx:id` de ConfigView.
- `mvn clean verify` verde.

### Manual

1. Elegir carpetas nuevas, guardar, reiniciar y comprobar persistencia.
2. Descargar un juego pequeño y verificar `.pkg` en Descargas.
3. Verificar extracción en Extracción.
4. Quitar `pkg2zip` del `PATH` y de `lib/`: aparece “No encontrado”; la descarga sigue.
5. Elegir carpeta con `pkg2zip`; se detecta y extrae.
6. Poner `pkg2zip` en `PATH`: se informa y se usa.
7. Ruta de solo lectura: error inline, no se guarda.
8. Misma carpeta para ambas: advertencia y confirmación.
9. Cambiar ruta con descarga activa: no se mueve la tarea actual.
10. Reiniciar con una descarga pausada: se restaura con su ruta original.

## 12. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| pkg2zip escribe en el directorio de trabajo, no en un flag de salida | Usar `ProcessBuilder.directory(extractionDir)`; verificar con el binario real en Windows/Linux. |
| Windows y Unix difieren en nombre/permisos | `Pkg2zipResolver` encapsula la diferencia; tests por plataforma. |
| Rutas con espacios o caracteres especiales | Lista de argumentos, nunca concatenar shell. |
| Mover rutas rompe descargas activas | Solo aplicar a nuevas tareas; no mover archivos. |
| El usuario elige un binario no confiable | Advertir que solo use un pkg2zip de confianza; no descargar automáticamente. |
| Espacio insuficiente en la carpeta elegida | Chequeo antes de descargar y aviso antes de extraer. |
| Historial antiguo con rutas relativas | Resolver contra `dataDir` al cargar; probar migración. |
| pkg2zip desaparece después de configurarlo | Revalidar al extraer y mostrar acción “Configurar”. |

## 13. Criterios de aceptación

- [ ] El usuario puede elegir carpeta de descargas y de extracción; se guardan y persisten.
- [ ] Las descargas nuevas usan la carpeta elegida; los `.pkg` existentes no se mueven.
- [ ] La extracción escribe en la carpeta elegida.
- [ ] Si `pkg2zip` está en `PATH`, la UI lo informa y lo usa.
- [ ] Si no está, la UI permite elegir la carpeta y valida el ejecutable.
- [ ] La falta de pkg2zip no bloquea la descarga; el aviso ofrece “Configurar”.
- [ ] Los errores de ruta son inline y accionables.
- [ ] El flujo funciona con teclado y en tema claro/oscuro.
- [ ] Tests y build verdes; sin regresiones en cola, pausa ni restauración.
- [ ] No se registra el zRIF en logs ni se concatenan comandos de shell.

## 14. Decisiones pendientes de confirmar

1. ¿Dos carpetas independientes (recomendado) o una carpeta base con subcarpetas `packages/` y `extracted/`?
2. ¿El selector de pkg2zip debe pedir la **carpeta** (propuesto, coincide con tu petición) o directamente el ejecutable?
3. ¿La carpeta de extracción por defecto debe ser `<dataDir>/extracted` o conservar el comportamiento actual basado en el directorio de trabajo?

Con estas tres respuestas se puede pasar a R0/R1 sin rehacer UI.
