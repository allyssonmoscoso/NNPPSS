# NNPPSS

A tool for managing and downloading "Magic packages".    Built with Java, NNPPSS provides a robust download manager with persistence, retry capabilities, and automated package extraction.

NNPPSS is compatible with Windows, macOS, and Linux/Unix-based operating systems.

## Prerequisites

- **Java 17+** (required)
- **[pkg2zip](https://github.com/mmozeiko/pkg2zip)** (required for non-Windows systems, included for Windows)

## Installation

### Easiest (Download release)

The simplest way to install or run NNPPSS is to download the latest release from the project's GitHub Releases page:

https://github.com/allyssonmoscoso/NNPPSS/releases

Release assets include a pre-built `NNPPSS.jar` (all dependencies bundled). Download the release archive, extract (if needed) and run:

```bash
java -jar NNPPSS.jar
```

### Development Mode

For quick compilation and testing:
```bash
./run-dev.sh          # Compile and run
./run-dev.sh compile  # Compile only
./run-dev.sh run      # Run without recompiling
./run-dev.sh clean    # Clean build artifacts
```

### Configuration

- **Persistent Settings**: Configuration saved in `config.properties`
- **Customizable Options**:
  - Database URLs for each console
  - Simultaneous download limit (1-4)
- **Easy Setup**: Configuration wizard on first launch


- **Language selection**: change UI language; the choice is saved to `config.properties` and requires application restart to apply.
- **Dark Mode (experimental)**: an experimental dark theme can be enabled in Settings; the theme is applied after restart.
- **Download speed limit**: set an approximate download speed limit (KB/s). `0` means unlimited. The limiter works by throttling write loops and may be approximate.
- **Auto-cleanup**: enable or disable automatic deletion of `.pkg` files after extraction.

## Usage Tips

1. **First Launch**: Configure database URLs when prompted
2. **Adding Downloads**: Select games from table → "Add to Download List" button
3. **Managing Downloads**: Right-click on progress bars for pause/resume/cancel options
4. **Resuming Sessions**: Close and reopen app - downloads automatically resume
5. **Disk Space**: Ensure at least 5GB free space plus download size
6. **Extraction**: Games auto-extract after download

Notes on resume behavior:
- On startup the app detects previously in-progress downloads and will prompt the user to resume them — it does not silently resume everything without user confirmation.
- Download states older than a configured stale threshold (24 hours by default) are treated as stale and typically ignored to avoid attempting to resume very old partial downloads.

Notifications and UI tips:
- The app uses in-window toast-style notifications for success/error/info messages (auto-dismiss by default).
- Exported queue JSON follows the internal `Game` model structure (fields such as `title`, `region`, `console`, `pkgUrl`, `zRif`, `contentId`, `fileSize`). Import validates JSON and reports invalid files.

## Troubleshooting

- **Downloads won't start**: Check disk space and configuration
- **Extraction fails**: Ensure pkg2zip is installed (non-Windows)
- **Database empty**: Click "Refresh" button or check database URLs in settings
- **Logs**: Check `logs/nnppss-error.log` for detailed error information
