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
- La carpeta de extracción se valida también antes de descargar, porque el
  producto final debe quedar en el SMB.

### 4.3 Extracción

El objetivo del producto es que **el contenido final extraído quede en el
SMB**. Por eso, por defecto:

- descargas y extracción se configuran en el recurso montado;
- el `.pkg` y la carpeta extraída pueden estar en el mismo share o en carpetas
  distintas del mismo recurso;
- la app no mueve el resultado a una carpeta local después de extraer.

Advertencia de rendimiento:

- pkg2zip escribe muchos archivos pequeños; en SMB puede ser lento.
- Mostrar una advertencia al guardar la configuración si ambas carpetas están
  en red: “La extracción en el recurso de red puede ser más lenta.”
- Mantener la misma carpeta SMB para el producto final; no se ofrece una
  extracción local en la primera versión.
- Una etapa de “descargar en local y copiar el resultado al SMB” queda fuera del
  alcance inicial: duplica espacio, requiere mover grandes volúmenes y complica
  la reanudación.

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
  “La extracción en red puede ser lenta; el producto final quedará en el
  recurso montado.”
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
- Descarga completa, pausa/reanudación, extracción en SMB y verificación del
  producto final en el recurso.
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
| Extracción muy lenta | Aviso de rendimiento y posibilidad de bajar concurrencia; el producto final se queda en el SMB. |
| Credenciales expuestas | Delegar en el SO y no almacenar secretos. |
| Ruta UNC no accesible desde el proceso | Mensaje “montá/autenticá el recurso” y validación. |
| Doble instancia sobre el mismo archivo | Documentar y no prometer bloqueo distribuido. |

## 12. Decisiones confirmadas

El 2026-11-21 el usuario confirmó:

1. Plataformas: **Windows, Linux y macOS** con montajes del sistema.
2. La carpeta de destino se elige **globalmente en Ajustes**.
3. El **producto final extraído debe quedar en el SMB**; descarga y extracción
   se configuran sobre el recurso montado.
4. Validación mediante **Probar conexión + validar al guardar**.
5. La app puede **crear subcarpetas** dentro del share.
6. Si el espacio libre no se puede consultar, **permitir con advertencia**.
7. Autenticación: **solo montajes existentes del sistema operativo**, sin pedir
   credenciales SMB.
8. Configuración, cola e historial permanecen en el equipo local.

Con esto se puede empezar por la fase 1 sin más preguntas de producto. La única
decisión técnica abierta es si en el futuro se añade un modo de staging local;
no es necesaria para la primera versión.
