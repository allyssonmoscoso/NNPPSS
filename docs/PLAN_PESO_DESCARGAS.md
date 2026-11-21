# Plan · Peso de las descargas pendientes

**Estado:** propuesta · **Rama:** `develop` · **Fecha:** 2026-11-21.

## 1. Objetivo

Que el usuario vea, antes de descargar, **cuánto pesa cada juego pendiente** y
cuánto suma la cola completa. Debe aplicar a:

- la lista/columna de la cola de descargas;
- el panel de descargas activas mientras están pendientes;
- el resumen de la cola;
- la tabla principal, cuando un juego ya está en la lista de espera.

## 2. Lo que ya existe

La base de datos y el modelo ya tienen el dato:

- `Game.fileSize` (`long`, bytes).
- La tabla principal muestra `%table.column.size` y formatea `fileSize`.
- La cola (`DownloadListView.fxml`, `DownloadListController`) ya declara una
  columna `colSize` enlazada a `fileSize`.
- El resumen inferior de la cola suma los bytes y muestra el total en GB.
- `DownloadProgressItem` muestra `descargado / total` durante la descarga.

Por tanto, **no hace falta cambiar el modelo ni el backend**. El trabajo es de
presentación, consistencia y visibilidad.

### Posible causa de que no se vea

La cola mide 420 px y la tabla tiene cinco columnas con un ancho preferido total
mayor que el panel. La columna de peso es la última y puede quedar comprimida o
poco visible. Además, el panel de descargas activas no muestra el peso de una
entrada pendiente hasta que empieza a descargar.

## 3. Experiencia propuesta

### 3.1 Cola de descargas

- Columna **Peso** visible, alineada a la derecha, con ancho mínimo de 80–90 px.
- Formato humano: `850 MB`, `4.2 GB`; `—` si el dato es desconocido.
- Ordenable por tamaño (el valor subyacente sigue siendo `long`).
- Cabecera de resumen:
  - `3 juegos · 12.4 GB pendientes`.
- Al seleccionar filas:
  - `2 seleccionados · 5.1 GB`.
- Tooltip/accesible con el valor exacto en bytes para lectores de pantalla.

Para que quepa, considerar:

- reducir el ancho de `Región` o moverla a la segunda línea del título;
- aumentar el drawer a 460–480 px;
- mantener `TableColumn` con `CONSTRAINED_RESIZE_POLICY`.

### 3.2 Panel de descargas activas/pendientes

Cada `DownloadProgressItem` ya recibe `totalBytes`; mostrar desde el inicio:

```text
Pendiente · 4.2 GB
Descargando · 1.1 GB / 4.2 GB
```

No esperar al primer progreso para mostrar el peso.

### 3.3 Tabla principal

Mantener la columna actual, pero usar el mismo formateador y el mismo criterio de
unidades para que la cola y la tabla principal no muestren valores distintos.

### 3.4 Unidades

- Usar formato binario (1 GB = 1024 MB) porque es el que ya usa la app.
- Mostrar `GB`/`MB` con un decimal para tamaños grandes y sin decimales para
  bytes/KB.
- No mostrar `0 GB` para un dato desconocido: mostrar `—`.

## 4. Cambios técnicos propuestos

### 4.1 Extraer `SizeFormatter`

Hoy hay tres implementaciones duplicadas:

- `MainController.formatSize`;
- `DownloadListController.formatSize`;
- `DownloadProgressItem.formatSize`.

Crear `com.squarepeace.nnppss.util.SizeFormatter`:

```java
public static String format(long bytes);       // 4.2 GB, 850 MB, 512 KB, 12 B, —
public static String exact(long bytes);        // "4,509,715,456 bytes" para tooltip
```

Con tests de:

- 0, negativos y desconocidos;
- límites 1023/1024, 1 MB, 1 GB;
- redondeo y locale.

### 4.2 Cola

- `DownloadListView.fxml`: ajustar anchos y alineación; considerar drawer 460 px.
- `DownloadListController`:
  - usar `SizeFormatter`;
  - alinear la celda a la derecha;
  - actualizar el resumen con total y total seleccionado;
  - mantener el cálculo existente de espacio libre.
- Añadir claves i18n para “Pendiente”, “seleccionados” y unidades.

### 4.3 Panel activo

- `DownloadProgressItem`: usar `SizeFormatter`;
- añadir estado inicial `Pendiente` con el total;
- conservar el formato actual durante la descarga.

### 4.4 Accesibilidad

- `accessibleText` en la celda con nombre + peso.
- Tooltip con el valor exacto.
- No depender solo del color/estado.

## 5. Indicador de juego ya encolado

### 5.1 Problema

`MainController.isQueued(game)` ya existe y el panel de detalle muestra
`detail.status.queued`; el botón cambia a “Quitar de la cola”. Sin embargo, la
fila de la tabla principal no tiene una señal visual permanente, por lo que el
usuario puede perder de vista que ese juego ya está en la lista de espera.

### 5.2 UX propuesta

En la tabla principal:

- Una etiqueta compacta **“En cola”** junto al título, o una columna
  **Estado** si se prefiere no tocar la celda del título.
- Estilo de fila con un acento sutil (borde izquierdo y fondo suave), sin
  depender solo del color.
- Tooltip: “Ya está en la lista de espera”.
- En el panel de detalle, mantener el estado **“En cola”** y el botón
  **“Quitar de la cola”**.

En la cola:

- Cada fila sigue mostrando su posición (`#`).
- La etiqueta puede ser **“Pendiente”** o **“En espera”** para diferenciarla de
  una descarga activa.

### 5.3 Comportamiento

- Al añadir a la cola, la fila se actualiza inmediatamente.
- Al quitar, el indicador desaparece al instante.
- Al iniciar las descargas, el juego sale de la cola y pasa al panel de
  descargas activas; la tabla principal deja de mostrar “En cola”.
- La identidad se resuelve por `pkgUrl`, no por título, para evitar falsos
  positivos con juegos homónimos.
- La búsqueda/filtro no debe perder el indicador: se calcula contra la cola
  actual, no contra las filas visibles.

### 5.4 Implementación

- Añadir un `ObservableSet<String> queuedUrls` o reutilizar
  `downloadQueue`/`isQueued` en `MainController`.
- Actualizar el indicador cuando cambie `downloadQueue` (lista observable).
- `TableRow<Game>` con pseudo-clase `queued` + CSS.
- Celda de título personalizada o columna `Estado` con badge.
- Claves i18n:
  - `table.column.status`
  - `status.queued.badge=En cola`
  - `status.queued.tooltip=Ya está en la lista de espera`
- Accesibilidad: incluir “En cola” en el `accessibleText` de la fila/celda.

## 6. Criterios de aceptación

- [ ] La cola muestra el peso de cada juego pendiente.
- [ ] La cola muestra el total pendiente y el total seleccionado.
- [ ] El panel de descargas pendientes muestra el peso antes de iniciar.
- [ ] Un tamaño desconocido/0 se muestra como `—`, no como `0 B`.
- [ ] La tabla principal, la cola y el panel usan el mismo formateador.
- [ ] La columna de peso es visible a 420 px y a 460 px.
- [ ] Un juego en la cola queda marcado como **“En cola”** en la tabla principal.
- [ ] Al quitarlo o iniciar la descarga, el indicador desaparece.
- [ ] Tests de formateador, resumen, estado encolado y carga FXML en verde.
- [ ] Sin cambios en el formato de `download-queue.json` (sigue guardando `fileSize`).

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| La columna se sigue cortando en ventanas estrechas | Ancho mínimo + drawer 460 px + prueba visual; mover Región a segunda línea si hace falta. |
| Valores distintos entre tabla y cola | Un solo `SizeFormatter` y tests compartidos. |
| `fileSize` desconocido en datos antiguos | Mostrar `—` y no incluir esos juegos en el total, o contarlos como 0 y avisar. |
| Cola muy grande | El total ya se calcula en O(n); actualizar solo al cambiar la lista. |
| Locale español/inglés | Claves i18n y tests de paridad. |
| Indicador de cola desactualizado tras filtros | Calcularlo contra `downloadQueue`, no contra `filteredData`. |
| Juegos con el mismo título | Usar `pkgUrl` como identidad. |

## 8. Fases y estimación

| Fase | Entregable | Horas |
|---|---|---:|
| 1 | Auditoría visual de la cola y elección de layout | 0,5 |
| 2 | `SizeFormatter` + tests | 1 |
| 3 | Cola: columna, total y selección | 1,5 |
| 4 | Panel de descargas pendientes | 1 |
| 5 | Indicador **“En cola”** en tabla/detalle | 1 |
| 6 | i18n, tooltips, accesibilidad | 0,5 |
| 7 | Tests, captura y validación | 1 |
| **Total** |  | **~6,5 h** |

## 9. Decisiones pendientes

1. ¿La prioridad es la **cola** (drawer), el **panel de descargas activas**, o ambos?
2. ¿Aceptas aumentar el drawer a 460 px para que la columna se vea bien?
3. ¿Mostrar también el total de las filas seleccionadas?
4. ¿Unidades binarias (`GB` de 1024 MB) o decimales (`GB` de 1000 MB)?
5. ¿Quieres que los juegos con tamaño desconocido cuenten como 0 o queden fuera
   del total con un aviso?
6. ¿Prefieres una etiqueta **“En cola”** junto al título o una columna **Estado**
   independiente?
