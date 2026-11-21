# NNPPSS

A JavaFX download manager for "Magic packages" (PS Vita, PSP and PSX), with
segmented downloads, pause/resume, retries, queue persistence, pkg2zip
extraction and automatic cleanup.

## Features

- PS Vita, PSP and PSX catalogues from configurable TSV URLs.
- Search and filters (region, size) with debounced filtering.
- Download queue with reordering, JSON export/import and free-space checks.
- Segmented downloads, pause/resume, cancel with partial-file cleanup.
- Download history, failed-download retry and row colouring.
- Automatic session restore after restart (including paused downloads).
- pkg2zip extraction and optional `.pkg` auto-cleanup.
- Configurable download and extraction folders.
- “Free up space” cleanup for `.pkg` files that have not been extracted,
  showing the file count and recoverable space before deleting anything.
- Update checks against the latest stable GitHub release, with the current
  version shown in Settings and a link to the release page.
- Status tags in the catalogue for **Downloaded**, **Queued**, **Paused** and
  **Failed** games, instead of colouring entire rows.
- pkg2zip auto-detection (configured, bundled or `PATH`) with folder picker.
- English/Spanish UI, light/dark theme.
- Per-user data directory, so the app works when installed read-only.
- Self-contained app image built with jpackage (no JDK required).

## Requirements

- **pkg2zip** for extraction: detected from `pkg2zip.path`, the bundled
  `lib\pkg2zip.exe`/`lib/pkg2zip`, or `PATH` on any platform. If it is missing,
  choose its folder in Settings; downloads are blocked unless you explicitly
  choose “Download without extracting”.
- To build from source: **JDK 25** and **Maven 3.9+**.

The distributed app image bundles its own Java runtime.

## Run the packaged app

```powershell
# Windows
.\scripts\package-windows.ps1
.\dist\NNPPSS\NNPPSS.exe
```

```bash
# macOS / Linux
sh scripts/package-unix.sh
# Linux: dist/NNPPSS/bin/NNPPSS
# macOS: open dist/NNPPSS.app
```

Set `JPACKAGE_TYPE=msi` (Windows), `dmg` (macOS) or `deb` (Linux) to build an
installer instead of the portable app image, provided the platform tools
(WiX, Xcode tools, dpkg) are installed.

## Development

```bash
./run-dev.sh            # run the JavaFX application
./run-dev.sh compile    # compile only
./run-dev.sh test       # run the automated tests
./run-dev.sh package    # build target/NNPPSS-<version>.jar
./run-dev.sh clean      # clean build artifacts
```

Direct Maven equivalents:

```bash
mvn javafx:run
mvn test
mvn clean package
```

Development runs use `.local/` as the data directory; pass
`-Dnnppss.dataDir=/path` to override it.

## Data directory

Configuration, databases, downloads, history and logs live outside the
application folder:

| OS | Default directory |
|---|---|
| Windows | `%APPDATA%\NNPPSS` |
| macOS | `~/Library/Application Support/NNPPSS` |
| Linux | `$XDG_DATA_HOME/NNPPSS` or `~/.local/share/NNPPSS` |

Existing files next to the application are migrated once on first run.

`config.properties` keys:

- `psvita.url`, `psp.url`, `psx.url`: database URLs per console
- `simultaneousDownloads`: 1–4
- `downloadSpeedLimit`: approximate KB/s, `0` means unlimited
- `autoCleanupPkg`: delete `.pkg` files after successful extraction
- `downloads.directory`: folder for downloaded `.pkg` files (default
  `<dataDir>/packages`)
- `extraction.directory`: folder where pkg2zip writes its output (default
  `<dataDir>/extracted`)
- `pkg2zip.path`: optional path to a pkg2zip executable; empty means automatic
  detection
- `darkMode`: enable the dark theme
- `language`: `en` or `es` (applied on restart)

## Project layout

```
src/main/java/com/squarepeace/nnppss/
├── Launcher.java         jpackage entry point
├── MainApp.java          JavaFX application
├── app/                  AppContext, AppPaths, lifecycle, theme
├── fx/                   controllers, components, view utilities
├── model/                data model
├── service/              downloads, coordinator, databases, persistence
└── util/                 i18n, paths, atomic JSON storage
src/main/resources/
├── fxml/                 views
├── styles/               base and dark CSS
└── messages*.properties  English/Spanish translations
```

## Testing

```bash
mvn test
```

The suite covers application paths and migration, configuration defaults,
i18n key parity, game filters, FXML loading and theming, the download
coordinator (start, progress, pause/resume, cancel, history, extraction,
restart persistence), queue persistence and a real segmented download against a
local HTTP server.

To run the Linux GitHub workflow locally with `act`:

```powershell
pwsh -NoProfile -File scripts/act-local.ps1
```

See [docs/ACT_LOCAL.md](docs/ACT_LOCAL.md) for requirements and limitations
(Windows/macOS runners cannot be emulated by act).

## Troubleshooting

- **Downloads do not start:** check free disk space (the app requires the
  download size plus a 5 GB buffer) and the console URLs.
- **Extraction fails:** verify pkg2zip is installed or bundled.
- **Database empty:** use Refresh, or check the console URLs.
- **Logs:** `nnppss.log` and `nnppss-error.log` in the data directory.

## History

The last Swing-only release is tagged `legacy-swing-0.0.7`; version 1.0.0 is the
definitive JavaFX edition.

## Roadmap

- [Plan de migración a JavaFX](docs/PLAN_MIGRACION_JAVAFX.md)
- [Plan de mejora UI/UX](docs/PLAN_UX_UI.md)
- [Plan de versionado y actualizaciones](docs/PLAN_VERSIONADO_ACTUALIZACIONES.md)
- [Plan de peso en descargas pendientes](docs/PLAN_PESO_DESCARGAS.md)

## Versioning and releases

Release versions come from Git tags named `vMAJOR.MINOR.PATCH`. The release
workflow injects the tag version into Maven, the JAR, `jpackage` and the update
manifest. Pushing a tag creates a **draft** GitHub release; a maintainer
reviews and publishes it. The app only checks published stable releases and
opens the release page; it never installs anything silently.

Current release: **2.0.2**. See [the versioning and update plan](docs/PLAN_VERSIONADO_ACTUALIZACIONES.md)
and the [release checklist](docs/RELEASING.md).

## License

See [LICENSE](LICENSE).
