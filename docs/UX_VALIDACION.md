# Validación UX/UI

**Fecha:** 2026-11-20 · **Versión:** 1.0.0 · **Rama:** `feature/JavaFXMigrationGUI`

Validación interna de las fases UX1–UX5. Las pruebas con usuarios externos
siguen pendientes y son el único criterio de UX6 que no se puede cerrar desde
este entorno.

## Resultado automatizado

```text
mvn clean verify
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La app image de Windows también se reconstruyó y se arrancó correctamente:
`dist/NNPPSS/NNPPSS.exe`.

## Contraste (WCAG AA, texto normal ≥ 4.5:1)

| Combinación | Ratio | Resultado |
|---|---:|---|
| Texto principal / superficie clara | 15.53:1 | ✅ |
| Texto secundario / superficie clara | 4.97:1 | ✅ |
| Blanco / primario claro | 5.17:1 | ✅ |
| Badge éxito claro | 4.56:1 | ✅ |
| Badge error claro | 4.92:1 | ✅ |
| Badge información claro | 5.85:1 | ✅ |
| Texto principal / superficie oscura | 13.77:1 | ✅ |
| Texto secundario / superficie oscura | 6.55:1 | ✅ |
| Primario oscuro / superficie oscura | 6.63:1 | ✅ |
| Badge éxito oscuro | 6.72:1 | ✅ |
| Badge error oscuro | 6.98:1 | ✅ |

El estado no depende solo del color: las tarjetas de descarga muestran texto
(`Paused`, `Completed`, `Error`) y los badges de detalle incluyen etiqueta.
La comprobación está automatizada en `ThemeContrastTest`, que lee los
stylesheets reales y falla si un token baja de 4.5:1.

## Recorrido de teclado

| Atajo | Acción |
|---|---|
| `Ctrl+F` | Enfocar búsqueda |
| `Ctrl+D` | Añadir el juego seleccionado a la cola |
| `Ctrl+L` | Abrir/cerrar la cola |
| `Espacio` | Pausar/reanudar las descargas objetivo |
| `Delete` | Cancelar las descargas seleccionadas |
| `F5` | Actualizar el catálogo |
| `Ctrl+,` | Abrir ajustes |
| `Alt+1/2/3` | Cambiar PS Vita / PSP / PSX |
| `Enter` | Descargar el juego seleccionado |
| `F1` | Mostrar esta ayuda |
| `Esc` | Cerrar el drawer de cola |
| `Tab` | Recorrer controles; foco visible en la tabla |

Al abrir la cola, el foco pasa a su tabla. Al cerrarla, vuelve al botón
**Queue**. Al abrir ajustes, el foco pasa a la primera URL.

## Estados comprobados

- **Catálogo cargando:** mensaje dentro de la tabla.
- **Sin datos:** “No games available” + “Open settings”.
- **Filtros sin coincidencias:** “No games match these filters” + “Clear
  filters”; probado con una búsqueda sin resultados y restauración de los
  3.647 juegos.
- **Error de carga:** título, causa y “Retry”.
- **Cola vacía:** placeholder y acciones deshabilitadas.
- **Descarga activa/pausada/completada/fallida:** texto de estado, barra y
  controles inline.
- **Dock de descargas:** expandido, colapsado y reexpandido al iniciar una
  sesión.

## Capturas

- [Vista principal](ux/redesign-final.png)
- [Drawer de cola](ux/queue-drawer.png)
- [Ajustes](ux/settings-panel.png)
- [Primera ejecución, paso 1](ux/first-run-step1.png)
- [Primera ejecución, paso 2](ux/first-run-step2.png)
- [Estado vacío por filtros](ux/empty-filtered.png)

## Limitaciones conocidas

- **Pruebas con 5 usuarios externos:** pendientes; requieren sesiones reales
  fuera de este entorno.
- **Extracción con `pkg2zip`:** en este entorno de desarrollo falta
  `lib/pkg2zip.exe`; la descarga y la UI funcionan, pero la extracción no se
  validó aquí. El instalador incluye el binario cuando está disponible.
- **Cambio de idioma:** se aplica al reiniciar, tal como indica el panel.
- **Ajustes:** se abren como diálogo modal moderno; no se convirtieron en
  drawer porque el formulario necesita más ancho que la cola.

## Próximo paso de UX6

Ejecutar el guion de prueba con 5 usuarios, registrar SUS y corregir los
hallazgos de fricción antes de cerrar la fase. El guion está en
[USER_TEST_GUIDE.md](ux/USER_TEST_GUIDE.md).
