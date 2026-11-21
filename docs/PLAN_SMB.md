# Plan · Soporte para guardar descargas en SMB montado

**Estado:** propuesta · **Rama:** `develop` · **Fecha:** 2026-11-21.

## 1. Objetivo

Permitir que el usuario elija una carpeta dentro de un recurso SMB **ya montado
por el sistema operativo** para guardar descargas y/o extracciones.

La app **no implementa un cliente SMB**, no pide usuario/contraseña y no monta
recursos. La autenticación, el montaje y la reconexión son responsabilidad del
sistema operativo. La app usa la ruta local que ese montaje expone.

Ejemplos válidos:

| Sistema | Ruta |
|---|---|
| Windows UNC | `\\10.0.0.33\Public\NNPPSS\descargas` |
| Windows con unidad mapeada | `Z:\NNPPSS\descargas` |
| macOS | `/Volumes/Public/NNPPSS/descargas` |
| Linux | `/mnt/smb/Public/NNPPSS/descargas` |

`smb://10.0.0.33/Public` **no** es una ruta de archivos Java; se rechaza con un
mensaje que indica montar primero el recurso.

## 2. Situación actual

La app ya tiene:

- Carpeta de descargas configurable (`downloads.directory`).
- Carpeta de extracción configurable (`extraction.directory`).
- Selector de carpetas en Ajustes y validación de escritura.
- Descargas segmentadas/reanudables y extracción con pkg2zip.
- Historial con `filePath` y estado de extracción.
- Limpieza de `.pkg` no extraídos.

Pero hay supuestos de disco local que hay que revisar:

- `ConfigController.validateDirectory()` ejecuta `Files.createDirectories`,
  `isDirectory` e `isWritable` en el hilo de JavaFX. En un SMB desconectado eso
  puede tardar y congelar la ventana.
- `SystemValidator` usa `File.getUsableSpace()` y exige 5 GB libres. Algunos
  montajes SMB devuelven 0 o un valor desconocido.
- `StorageCleanupService` recorre la carpeta de descargas en el hilo de la UI.
- El arranque y el guardado no distinguen “ruta local” de “ruta de red”.
- La app no avisa de que el recurso puede desaparecer si se desconecta la red.

## 3. Principios de diseño

1. **El montaje lo hace el SO.** La app solo usa rutas existentes.
2. **No guardar credenciales SMB.** Nunca usuario, contraseña ni tokens en
   `config.properties`, logs o historial.
3. **No caer silenciosamente a una carpeta local.** Si la ruta SMB no está
   disponible, se avisa y se bloquea el inicio hasta corregirla.
4. **Nunca validar una ruta de red en el hilo de JavaFX.**
5. **Mantener configuración, cola e historial en el directorio local de datos.**
   Solo las descargas/extracciones van al SMB.
6. **UNC es más estable que una letra de unidad mapeada** en Windows; se puede
   preferir `\\servidor\recurso` sobre `Z:\`.

## 4. Experiencia de usuario

### 4.1 Ajustes → Almacenamiento

- Campo de texto editable para pegar una ruta UNC o un punto de montaje.
- Botón **Elegir** con `DirectoryChooser`.
- Botón **Probar conexión**:
  - comprueba que existe y es carpeta;
  - crea un archivo temporal, escribe bytes, fuerza `flush`/cierre y lo borra;
  - mide latencia aproximada;
  - muestra `Conectada`, `Solo lectura`, `No disponible` o `Tiempo agotado`.
- Etiqueta de tipo:
  - `Red · SMB` si se detecta;
  - `Montaje local` si no se puede determinar;
  - `No disponible` si falla.
- Espacio libre:
  - `12.4 GB disponibles` si el sistema lo informa;
  - `Espacio libre no disponible en este recurso` si no.
- Aviso al guardar una ruta de red:
  - “Si el servidor se desconecta, las descargas se detendrán. La app no
    guarda credenciales SMB; el montaje debe existir en el sistema.”
- Botón **Abrir carpeta** para comprobar que el SO puede abrirla.

### 4.2 Inicio y cola

- Si la carpeta configurada no está disponible al arrancar:
  - notificación no modal;
  - acciones **Abrir ajustes** y **Reintentar**;
  - las descargas no se inician hasta que la ruta vuelva.
- Si la ruta desaparece durante una descarga:
  - se comporta como un fallo de descarga normal;
  - los reintentos existentes se agotan;
  - queda el estado `failed` y el botón **Reintentar fallidas**;
  - no se borra el `.pkg` parcial.
- No se inicia una descarga si la validación de escritura falla.

### 4.3 Extracción

La extracción puede ser en el SMB o local. Recomendación:

- Permitir ambas, pero mostrar una advertencia de rendimiento si la extracción
  va al SMB.
- Valor por defecto: mantener la carpeta que el usuario elija; no cambiarla
  automáticamente.
- Si la extracción es local y las descargas SMB, el `.pkg` se lee por red y se
  escribe localmente.

### 4.4 Limpieza

- “Liberar espacio” debe escanear el SMB en segundo plano.
- Mostrar progreso/“Calculando…” y permitir cancelar.
- Si el recurso desaparece, no borrar nada y mostrar error.
- No recorrer recursivamente todo el recurso si la carpeta elegida es la raíz
  del share; limitar a `.pkg` del nivel configurado.

## 5. Detección y validación de rutas

### 5.1 Clasificación

Crear `StoragePathService` (sin JavaFX) con:

```java
enum StorageKind { LOCAL, WINDOWS_UNC, WINDOWS_MAPPED, MAC_MOUNT, LINUX_MOUNT, UNKNOWN }

StorageKind classify(Path path);
```

Heurísticas:

- Windows: prefijo `\\` → UNC; letra de unidad no se puede clasificar como SMB
  sin consultar el sistema. Se puede usar `Get-SmbConnection` solo como dato
  informativo opcional.
- macOS: `/Volumes/*` es candidato; `mount` puede confirmar `smbfs`.
- Linux: `/proc/mounts` puede confirmar `cifs`/`smb3`; si no, `UNKNOWN`.
- Nunca bloquear ni fallar por no poder clasificar: es una etiqueta, no un
  requisito.

### 5.2 Validación asíncrona

`StorageValidationService`:

- `CompletableFuture<Validation>` con timeout de UI de 10 s.
- Ejecutar en un hilo daemon; no cancelar a la fuerza una llamada nativa.
- Pasos:
  1. normalizar ruta absoluta;
  2. comprobar existencia y directorio;
  3. escribir un archivo temporal de 4–8 KB;
  4. cerrar y borrar;
  5. consultar espacio libre con `Files.getFileStore` y `File.getUsableSpace`
     como respaldo;
  6. medir latencia.
- Resultado con `writable`, `freeBytes` opcional, `latencyMs`,
  `kind`, `message`.
- No ejecutar `createDirectories` en el hilo FX.
- Si la carpeta no existe, preguntar antes de crearla; en SMB crear una carpeta
  nueva debe ser una acción explícita.

### 5.3 Espacio libre

- `Files.getFileStore(path).getUsableSpace()`.
- Si devuelve 0/negativo o lanza excepción:
  - no rechazar automáticamente;
  - mostrar `Espacio no disponible`;
  - permitir continuar bajo responsabilidad del usuario.
- Antes de descargar, el chequeo de 5 GB debe degradar a:
  - “No se pudo verificar el espacio libre en el recurso SMB. ¿Continuar?”
  - no bloquear por un dato no disponible.

## 6. Descarga, escritura y bloqueo

- La escritura actual (`RandomAccessFile` por segmentos, `FileOutputStream` en
  modo clásico) funciona sobre SMB, pero puede ser más lenta.
- No usar `Files.move` atómico para el `.pkg`: se escribe directamente en la
  ruta final.
- Mantener el archivo `.part.json` junto al `.pkg` o en el directorio de datos
  local? **Recomendación:** mantenerlo junto al `.pkg` para que la reanudación
  funcione aunque la app se ejecute en otro equipo; si el share no permite
  escribir archivos ocultos, usar el directorio local como respaldo.
- Cerrar todos los handles antes de extraer y antes de borrar:
  - ya existe el patrón de reintentos para el auto-cleanup;
  - extenderlo a fallos transitorios de SMB.
- Evitar `fsync` por cada bloque; SMB ya es lento y el archivo se valida al
  completar.
- Si dos instancias de la app escriben el mismo `.pkg`, no hay coordinación
  distribuida: documentar que no se debe abrir la misma cola en dos equipos.
- Los segmentos concurrentes pueden saturar enlaces lentos; evaluar bajar
  `simultaneousDownloads` o el número de segmentos cuando se detecte SMB.

## 7. Extracción y limpieza en SMB

- pkg2zip se ejecuta con `ProcessBuilder.directory(extractionDirectory)`; un
  working directory SMB puede ser lento.
- La salida de pkg2zip puede contener muchos archivos pequeños; el rendimiento
  en SMB puede ser pobre.
- Mostrar advertencia si extracción y descargas están en SMB:
  “La extracción en red puede ser lenta; considera una carpeta local.”
- El auto-cleanup ya reintenta `deleteIfExists`; añadir prueba específica con
  archivo temporal en un directorio con latencia simulada no es posible en
  unit tests, pero sí probar la lógica de reintentos.
- La limpieza manual se ejecuta en background y nunca borra si la ruta no está
  disponible.

## 8. Seguridad y privacidad

- No almacenar credenciales SMB.
- No ejecutar comandos de montaje ni `net use` automáticamente.
- No registrar contraseñas; las rutas pueden aparecer en logs, pero no
  credenciales.
- Advertir que la seguridad/autenticación depende del montaje del SO.
- Validar que la ruta no sea un enlace simbólico a un destino no deseado; al
  menos normalizar y mostrar la ruta real.
- No permitir rutas `smb://` como si fueran archivos locales.

## 9. Pruebas

### Automatizadas

- Clasificación de rutas (UNC, `/Volumes`, `/mnt`, local) con cadenas
  independientes del SO.
- Validación de escritura con `@TempDir`.
- Espacio libre desconocido → resultado no bloqueante.
- Reintentos de borrado.
- `GameFilter`/UI no afectados.
- FXML de Ajustes con nuevos controles.

### Manuales / integración

- Windows con **UNC** y con **unidad mapeada** (el ejemplo
  `\\10.0.0.33\Public`).
- Windows sin conexión al share: no debe congelar la UI.
- Linux con montaje `cifs`; macOS con `/Volumes`.
- Descarga completa, pausa/reanudación, extracción local y extracción en SMB.
- Desconectar el servidor a mitad de descarga y reconectar.
- “Liberar espacio” con el share desconectado.
- Permisos de solo lectura.
- Nombres de archivo largos o caracteres especiales.

## 10. Fases y estimación

| Fase | Entregable | Días |
|---|---|---:|
| 1 | Clasificación de rutas y servicio de validación asíncrona | 1 |
| 2 | Ajustes: probar conexión, tipo, espacio y avisos | 1,5 |
| 3 | Preflight de descargas y errores de red | 1 |
| 4 | Descarga/reanudación/extracción/limpieza en SMB | 1,5 |
| 5 | Tests, documentación y prueba manual Windows/Linux | 1 |
| **Total** |  | **~6 días** |

## 11. Riesgos

| Riesgo | Mitigación |
|---|---|
| La UI se congela al validar un share caído | Validación en background con timeout. |
| `File.getUsableSpace()` devuelve 0 | No bloquear; mostrar desconocido y pedir confirmación. |
| SMB desconectado durante la descarga | Fallo normal, estado failed, reintentos y reanudación. |
| Escritura/borrado bloqueado por SMB | Reintentos y cierre explícito de handles. |
| Extracción muy lenta | Aviso y opción de carpeta local. |
| Credenciales expuestas | Delegar en el SO y no almacenar secretos. |
| Ruta UNC no accesible desde el proceso | Mensaje “montá/autenticá el recurso” y validación. |
| Doble instancia sobre el mismo archivo | Documentar y no prometer bloqueo distribuido. |

## 12. Decisiones pendientes

1. ¿El soporte debe ser **solo Windows** o también Linux/macOS con montajes
   `cifs`/`smbfs`?
2. ¿La extracción por defecto debe quedarse en la carpeta SMB elegida o
   recomendar una carpeta local?
3. ¿Quieres un botón **Probar conexión** explícito o validar solo al guardar?
4. ¿Se permite crear una carpeta nueva en el share desde la app, o solo elegir
   una existente?
5. ¿Aceptas que, si el espacio libre no se puede consultar, la app permita
   continuar con una advertencia?
