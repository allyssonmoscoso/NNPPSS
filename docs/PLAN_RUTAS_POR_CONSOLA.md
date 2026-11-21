# Plan · Carpeta de extracción por consola

**Estado:** implementado en `develop` · **Rama:** `develop` · **Fecha:** 2026-11-21.

## 1. Objetivo

Permitir que PS Vita, PSP y PSX tengan **carpetas de extracción distintas**, para
que el usuario pueda separar su catálogo. Ejemplo:

```text
PS Vita  → \\10.0.0.33\Public\NNPPSS\PSVita
PSP      → \\10.0.0.33\Public\NNPPSS\PSP
PSX      → \\10.0.0.33\Public\NNPPSS\PSX
```

También debe funcionar con carpetas locales y con las rutas SMB del plan de
[SMB montado](PLAN_SMB.md).

## 2. Situación actual

- Existe una sola carpeta global: `extraction.directory`.
- `ConfigManager.getExtractionDirectory()` devuelve esa ruta.
- `DownloadCoordinator.extractIfNeeded()` usa la misma carpeta para cualquier
  consola.
- `PackageService` recibe la carpeta de salida y no necesita cambios de modelo.
- Ajustes tiene un único campo **Carpeta de extracción**.
- El historial solo guarda si se extrajo, no dónde se extrajo.

## 3. Modelo propuesto

### 3.1 Configuración

Mantener la carpeta global como valor de respaldo y añadir tres claves opcionales:

```properties
extraction.directory=
extraction.psvita.directory=
extraction.psp.directory=
extraction.psx.directory=
```

Reglas:

- Si la clave de la consola está vacía → se usa `extraction.directory`.
- Si la clave de la consola tiene valor → se usa esa ruta.
- Si la carpeta global está vacía → se usa el valor por defecto actual
  (`<dataDir>/extracted`).
- No se crean subcarpetas automáticamente en la primera versión; el usuario
  decide si separa o comparte carpeta.
- La carpeta de **descargas** sigue siendo global. Este cambio es solo de
  extracción.

### 3.2 Resolución

`ConfigManager`:

```java
Path getExtractionDirectory();
Path getExtractionDirectory(Console console); // override o global
Path getConfiguredExtractionDirectory(Console console); // texto explícito o null
```

`DownloadCoordinator`:

```java
Path extractionDirectory =
        configManager.getExtractionDirectory(game.getConsole());
```

`PackageService` recibe esa ruta como hasta ahora. No hay cambios en pkg2zip ni
en el formato del `.pkg`.

### 3.3 Historial

- `extracted` sigue siendo suficiente para saber si se extrajo.
- Opcional: guardar `extractionPath` en el historial para diagnóstico y para
  “Abrir carpeta” desde el historial. No es necesario para el MVP, pero es útil.
- No se migran archivos ya extraídos: cada nueva extracción usa la ruta vigente.

## 4. Interfaz

### 4.1 Ajustes → Almacenamiento

Mantener:

- **Carpeta de descargas** (global).
- **Carpeta de extracción** (global, usada como respaldo).

Añadir una subsección **Carpetas por consola**:

| Consola | Campo | Acciones |
|---|---|---|
| PS Vita | ruta o vacío | Elegir, Abrir, Usar global |
| PSP | ruta o vacío | Elegir, Abrir, Usar global |
| PSX | ruta o vacío | Elegir, Abrir, Usar global |

Cada fila debe mostrar:

- `Global` si hereda;
- `Personalizada` si tiene override;
- ruta efectiva resuelta;
- estado de validación: `Válida`, `No disponible`, `Solo lectura`;
- espacio libre si el sistema lo informa.

El texto de ayuda:

> Deja una carpeta vacía para usar la carpeta global de extracción.

### 4.2 Primera ejecución

- Mantener el wizard simple: solo la carpeta global.
- Las carpetas por consola se configuran después en Ajustes.
- Si el usuario ya tiene una carpeta global, no se cambia nada.

### 4.3 Acciones

- **Elegir** abre `DirectoryChooser` y rellena la fila.
- **Abrir** abre la carpeta efectiva.
- **Usar global** borra el override y deja la fila vacía.
- **Probar conexión** por fila si la ruta es SMB.
- Al guardar:
  - validar todas las rutas no vacías;
  - no validar la global tres veces si las tres heredan;
  - mostrar el primer error con la consola afectada:
    “PSP: la carpeta de extracción no está disponible.”

## 5. Flujo de descarga y extracción

1. El usuario inicia una descarga.
2. La app resuelve la carpeta de extracción de **cada consola**.
3. Antes de iniciar, valida las carpetas de las consolas involucradas.
4. Si una ruta personalizada está configurada pero no disponible:
   - no se inicia la descarga;
   - se muestra un error accionable con **Abrir ajustes**;
   - **no se cae silenciosamente a la carpeta global**.
5. Si la ruta es válida, `pkg2zip` se ejecuta con esa carpeta de salida.
6. El `.pkg` se borra solo si el auto-cleanup está activo y la extracción fue
   exitosa.

### SMB

Cada consola puede apuntar a:

- el mismo share y una subcarpeta distinta;
- un share distinto;
- una carpeta local;
- una combinación de local y SMB.

Cada ruta se valida de forma independiente con el servicio asíncrono del plan
SMB. Si una consola está en SMB y otra en local, ambas deben funcionar antes de
iniciar una sesión que las incluya.

## 6. Compatibilidad y migración

- Las claves nuevas faltantes se tratan como vacías.
- Los usuarios existentes siguen usando la carpeta global sin cambios.
- No se mueven archivos ya extraídos.
- Si un usuario configura PS Vita en otra ruta, las extracciones nuevas de Vita
  van allí; PSP/PSX siguen en la global.
- Un cambio de ruta no afecta al historial de extracciones anteriores.
- No se duplican descargas ni se vuelve a extraer automáticamente.

## 7. Casos límite

- **Misma carpeta para dos consolas:** permitido; la app no fuerza separación.
- **Carpeta de consola igual a la global:** permitido, se muestra como
  personalizada si el texto no está vacío.
- **Ruta relativa:** se resuelve contra el directorio de datos, igual que hoy.
- **Ruta inexistente:** se puede crear si el usuario confirma; en SMB, crear
  carpetas requiere permiso de escritura.
- **Ruta SMB desconectada:** error al validar; no se inicia la descarga.
- **Un lote mezcla consolas:** se validan todas las rutas antes de empezar; si
  una falla, no se inicia ninguna descarga del lote.
- **Auto-cleanup:** no depende de la carpeta de extracción; sigue borrando el
  `.pkg` de descargas.
- **“Liberar espacio”:** no se ve afectado; solo mira la carpeta de descargas.

## 8. Tests

- `ConfigManager`:
  - sin override usa la global;
  - override de Vita no afecta a PSP/PSX;
  - override vacío hereda;
  - valores ausentes no rompen la configuración antigua.
- `DownloadCoordinator`:
  - extracción de Vita usa la carpeta de Vita;
  - PSP/PSX usan la suya;
  - un lote mixto usa la carpeta correcta por tarea.
- `ConfigController` / FXML:
  - cargar y guardar las tres rutas;
  - “Usar global” limpia el override;
  - validación con una ruta inválida muestra la consola afectada.
- SMB:
  - validación independiente de cada ruta;
  - una ruta caída bloquea el lote sin fallback.
- Tests de no regresión: 56 actuales.

## 9. Fases y estimación

| Fase | Entregable | Horas |
|---|---|---:|
| 1 | ConfigManager + resolución por consola + tests | 2 |
| 2 | Ajustes: tres filas, heredar, validar | 3 |
| 3 | DownloadCoordinator: ruta por tarea y preflight | 2 |
| 4 | Integración SMB y mensajes | 2 |
| 5 | Tests, documentación y prueba manual | 2 |
| **Total** |  | **~11 h** |

## 10. Decisiones pendientes

1. ¿Las tres rutas deben ser **overrides opcionales** que heredan de la global,
   o carpetas obligatorias independientes?
2. ¿Quieres que al activar “separar por consola” se creen automáticamente
   subcarpetas `PSVita`, `PSP` y `PSX` dentro de la carpeta global?
3. ¿Las **descargas** también deben poder separarse por consola o solo la
   extracción?
4. Si una ruta personalizada no está disponible, ¿prefieres **bloquear** la
   descarga (recomendado) o caer a la global con aviso?
5. ¿Guardamos `extractionPath` en el historial para diagnóstico? (recomendado,
   pero no imprescindible para el MVP).
