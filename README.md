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
- English/Spanish UI, light/dark theme.
- Per-user data directory, so the app works when installed read-only.
- Self-contained app image built with jpackage (no JDK required).

## Requirements

- **pkg2zip** for extraction: `lib\pkg2zip.exe` on Windows, or `pkg2zip` on
  `PATH` on macOS/Linux.
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

## License

See [LICENSE](LICENSE).
