# Changelog

All notable changes to NNPPSS are documented here.

## [2.0.0] — 2026-11-20

### Added

- Modern JavaFX application shell with app bar, console switch, search, theme
  toggle, queue counter and a persistent detail pane.
- “Download now” and “Add to queue” actions, with undo for queue additions.
- Non-modal download queue drawer with ordering, selection, export/import and
  start controls.
- Inline pause/resume and cancel controls on download cards.
- Collapsible download dock with global progress and ETA.
- Configurable download and extraction folders, free-space hints and folder
  pickers.
- pkg2zip auto-detection from configuration, bundled `lib`, or `PATH`, with a
  folder picker and actionable missing-tool dialog.
- Two-step first-run wizard for libraries, storage and preferences.
- “Free up space” cleanup for unextracted `.pkg` files, with confirmation,
  file list and recoverable-space estimate.
- Actionable loading, empty, filtered-empty and load-error states.
- Keyboard shortcuts, focus handling, accessible labels and WCAG AA contrast
  checks.
- Local `act` workflow support and Linux JavaFX runner dependencies.
- 51 automated tests, including paths, update/config storage, pkg2zip
  resolution, update checks and storage cleanup.

### Changed

- Maven development version is now `2.0.0-SNAPSHOT`; release builds will take
  their version from the Git tag.
- The JAR manifest exposes `Implementation-Version`.
- Settings are grouped into Libraries, Storage cleanup, Storage, Downloads and
  Appearance sections.

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
- UX6 closed with internal validation (38 tests, automated WCAG contrast,
  keyboard matrix and visual review); the 5-user SUS study is scheduled as
  post-release follow-up.
- Downloads and extraction folders are now configurable in Settings and the
  first-run wizard, with free-space hints and “Open” actions.
- pkg2zip is auto-detected from configuration, the bundled `lib` folder or
  `PATH`; users can pick the folder containing it. When missing, downloads are
  blocked unless “Download without extracting” is chosen explicitly.
- Settings now includes “Free up space”: it lists and deletes only `.pkg` files
  that have not gone through the app's extraction, with a confirmation dialog
  showing file count and recoverable space. Extracted files and active
  downloads are excluded.
- Added local `act` support (`scripts/act-local.ps1`, `scripts/act-local.sh`,
  `.actrc` and `docs/ACT_LOCAL.md`), Linux workflow dependencies, and a
  gitignore fix so `src/main/java/.../app` and message bundles are not omitted
  when reproducing the workflow locally.
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
