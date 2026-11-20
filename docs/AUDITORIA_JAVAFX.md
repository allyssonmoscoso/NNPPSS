# Auditoría JavaFX — rendimiento, hilo de UI, Canvas y tema

Fecha: 2026-11-20 · Base: NNPPSS 1.0.0 · Skills usadas:
`javafx-concurrency-services`, `javafx-animation-canvas-render-loop`,
`javafx-devtools-inspection-debugging`, `javafx-theming-icons-styling`.

## Alcance

Revisión de `MainController`, `DownloadCoordinator`, `DownloadService`,
`SegmentedDownloadTask`, `DownloadTaskState`, `DownloadProgressItem`,
`GlobalProgressIndicator`, `DownloadListController` y los CSS de `styles/`.

## Hallazgos y cambios

| # | Severidad | Hallazgo | Cambio aplicado |
|---|---|---|---|
| 1 | Alta | `SegmentedDownloadTask` emitía `onProgress` y `onSegmentProgress` por cada buffer de 8 KB; en 1 GB eran ~130.000 eventos hacia el hilo de FX. | Throttle en origen a 5 Hz (`SegmentedDownloadTask`), segunda barrera en `DownloadCoordinator` (`shouldNotifySegments`) y actualización directa cuando ya estamos en FX (`runOnFx`). |
| 2 | Alta | `updateRetryButton()` leía `download-history.json` desde el hilo de FX tras cada descarga; `handleRetryFailed()` además resolvía juegos contra los TSV en el hilo de FX. | El botón usa la caché `historyByUrl`; la resolución de fallidos se movió a un `Task` con hilo daemon. |
| 3 | Media | `DownloadProgressItem` y `GlobalProgressIndicator` usaban estilos inline, bloqueando el cambio de tema. | Clases CSS semánticas (`download-item`, `download-title`, `download-meta`, `download-status`, `download-cancel`, `global-progress`) y overrides para `dark.css`. |
| 4 | Media | `DownloadProgressItem` hacía `Platform.runLater` incluso cuando el callback ya estaba en el hilo de FX, duplicando saltos de cola. | Helper `runOnFx(...)`: ejecuta directo en FX y solo encola desde workers. |
| 5 | Baja | La etiqueta de estadísticas de cola usaba `setStyle` para el aviso de espacio insuficiente. | Clase `.stats-warning` y override oscuro. |
| 6 | Baja | `DownloadService` (ruta legacy) notificaba progreso por cada bloque de 64 KB. | Throttle de progreso a 5 Hz; el estado final sigue llegando por `onComplete`. |
| 7 | Pendiente | `cancel()` borra archivos y `saveState()` escribe JSON en el hilo de FX. | Son operaciones de metadatos pequeñas; si se observan pausas, mover a un executor de persistencia. |
| 8 | Pendiente | `refreshHistoryCache()` lee el historial en FX tras completar. | El archivo es pequeño; valorar caché en `DownloadHistoryManager` si crece. |

## Verificación

- `mvn clean verify`: **36 tests, 0 fallos**.
- Nuevo test: 20 eventos de segmento seguidos producen una sola notificación a la UI
  dentro de la ventana de throttle.
- Test de descarga segmentada real contra un servidor HTTP local con `Range`.
- Arranque de la app sin errores de CSS ni excepciones en el log.
- Smoke test del diálogo de cola con las firmas actuales.

## Recomendaciones de inspección

- **Scenic View** para inspeccionar el scene graph y detectar nodos que crecen.
- **CssFX** para depurar selectores y precedencia de `base.css` / `dark.css` en caliente.
- **JFR** (`-XX:StartFlightRecording`) si se detectan pausas; revisar asignaciones de
  `Canvas` y el hilo `JavaFX Application Thread`.
- Mantener las herramientas de diagnóstico detrás de flags de desarrollo, nunca en
  producción.
