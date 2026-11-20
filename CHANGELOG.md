# Changelog

All notable changes to NNPPSS are documented here.

## [1.0.0] — 2026-11-20

### Added

- Complete JavaFX user interface replacing the former Swing UI.
- `DownloadCoordinator`: download queue, concurrency, pause/resume, cancel,
  history, extraction, retry of failed downloads and session persistence.
- Per-user data directory (`%APPDATA%`, `Library/Application Support`,
  `$XDG_DATA_HOME`) with one-time migration from the legacy layout.
- Atomic JSON persistence for queue, history and in-progress download state.
- English/Spanish localization in every view, with restart notice for language
  changes.
- Light/dark CSS theme applied without restart.
- Self-contained jpackage app image (Windows, macOS, Linux) and GitHub Actions
  build matrix.
- JUnit 5 test suite: application paths, configuration, i18n parity, filters,
  FXML loading, theming, download coordinator, queue persistence and a real
  segmented download against a local HTTP server.

### Changed

- Single entry point: `Launcher` for jpackage and `MainApp` for the JavaFX
  runtime; Maven now builds a runnable JavaFX application by default.
- Segmented-download UI updates are throttled at the source and in the
  coordinator (5 Hz), avoiding thousands of repaint events per large download.
- Failed-download retry resolves games off the JavaFX thread; the retry button
  uses the in-memory history cache instead of reading JSON on every update.
- Download row and global progress styling moved from inline styles to CSS
  classes with dark-theme overrides.
- New application shell: app bar with console switch and queue counter,
  filter bar with result count, and a detail pane with “Descargar” and
  “Añadir a la cola”; adding to the queue can be undone from the snackbar.
- The download queue is now a non-modal side drawer with statistics, compact
  actions and JSON export/import, instead of a blocking dialog.
- Settings redesigned into Libraries, Downloads and Appearance sections with
  inline validation and per-URL “Test” checks that run in the background;
  first-run configuration explains what is required and refuses to save with
  no library configured.
- Each download card now has an inline pause/resume button; visible result
  counters use correct singular/plural forms and icon-only actions expose
  accessible labels.
- The catalogue now shows actionable empty/loading/error states (Open settings,
  Clear filters, Retry) and supports F5, Ctrl+, and Alt+1/2/3 shortcuts.
- Internal UX validation documented (contrast, keyboard matrix, states and
  screenshots); dead UI code removed and the status bar reflects active filters.
- First-run settings can be closed with “Configure later”; F1 opens an in-app
  keyboard-shortcut reference, and WCAG contrast is now guarded by a test.
- First-run configuration is now a two-step wizard (libraries → preferences)
  with validation before advancing and a “Configure later” escape hatch.
- The download dock can be collapsed to “Show downloads” from the status bar
  and expands again automatically when a new session starts.
- Content ID moved out of the table into the detail pane; console names are
  localized.
- Configuration validated (1–4 simultaneous downloads, non-negative speed
  limit) with defaults for every key.
- Logs are written to the data directory instead of the working directory.
- README, architecture notes and build scripts updated for the JavaFX edition.

### Removed

- Swing entry point, `Frame`, `Config`, `ui` package, NetBeans `.form` files and
  the legacy `build.sh` / `run.sh` / assembly descriptor.
- `PackageService` dependency on Swing dialogs.

### Fixed

- Download state no longer races with readers (atomic replacement).
- Session state is preserved on exit even while workers are being cancelled.
- Global progress uses bytes instead of averaging bytes and game counts.
- Retry-failed and history lookups no longer block the JavaFX Application
  Thread.

## [0.0.7] — legacy

Last Swing-only release, preserved under the `legacy-swing-0.0.7` tag.
