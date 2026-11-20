# ADR 0001 — Baseline JavaFX y arquitectura de la migración

- **Estado:** aceptada
- **Fecha:** 2026-11-19
- **Contexto:** NNPPSS 0.0.7 es una app Swing con un esqueleto JavaFX parcial. El plan `docs/PLAN_MIGRACION_JAVAFX.md` fija la migración definitiva.

## Decisiones

| ID | Decisión | Motivo |
|---|---|---|
| D1 | JDK 25 LTS + JavaFX 27, `maven.compiler.release=25` | Es el stack ya verificado en el entorno; jpackage incluye el runtime y elimina la dependencia de Java en el usuario final. |
| D2 | `com.squarepeace.nnppss.MainApp` como único entrypoint | Elimina la ambigüedad entre `NNPPSS` (Swing) y `MainApp` (JavaFX). Swing queda solo como referencia hasta F8. |
| D3 | Empaquetado con jpackage no modular (app-image + instalador por SO) | JavaFX no se distribuye de forma fiable en fat-jars; jpackage soporta classpath y nativas. |
| D4 | MVC/MVVM ligero: `AppContext`, `DownloadCoordinator`, controladores finos y view models observables | La lógica de descargas deja de vivir en el controlador y se vuelve testeable. |
| D5 | i18n con `ResourceBundle` en `FXMLLoader` y `%claves` en FXML; cambio de idioma con reinicio asistido | Aprovecha las 144 claves EN/ES existentes y reduce el riesgo frente a la recarga en caliente. |
| D6 | Tema por CSS (`styles/base.css`, `styles/dark.css`) aplicado en caliente | El modo oscuro actual es experimental y no se aplica en JavaFX. |
| D7 | Datos de usuario en `AppPaths.dataDir()` con migración desde la carpeta del JAR | Un instalador en `Program Files`/`/Applications` no es escribible. |
| D8 | JUnit 5 + surefire 3.x; smoke test de FXML; TestFX opcional | No existe `src/test` y cualquier regresión se detecta a mano. |
| D9 | PRs por fase sobre `develop`; etiqueta `legacy-swing-0.0.7` antes de borrar Swing | Permite rollback y revisión incremental. |

## Consecuencias

- El README debe indicar JDK 25 para desarrollo y “sin JDK” para instalación.
- `PathResolver` pasa a delegar en `AppPaths`; `pkg2zip` se resuelve contra `appDir()`, no contra el directorio de datos.
- Cualquier import de `javax.swing`/`java.awt` en `src/main/java` es un fallo de build a partir de F8.
- El cambio de idioma en caliente queda en el backlog posterior a 1.0.0.

## Alternativas descartadas

- **JavaFX 21 + JDK 17:** válida si se exige compatibilidad con Java 17, pero obliga a cambiar versiones y revalidar el entorno; se pospone hasta que exista ese requisito.
- **Fat-jar con JavaFX:** no soporta nativas/módulos de forma fiable.
- **jlink modular:** más limpio, pero exige `module-info` y `opens` de FXML; se puede adoptar después de 1.0.0.
