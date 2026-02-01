# NNPPSS

A tool for managing and downloading "Magic packages".    Built with Java, NNPPSS provides a robust download manager with persistence, segmented Downloads, retry capabilities, etc.

NNPPSS is compatible with Windows, macOS, and Linux/Unix-based operating systems.

## Prerequisites

- **Java 17+** (required)
- **[pkg2zip](https://github.com/mmozeiko/pkg2zip)**

## Installation

### Easiest (Download release)

The simplest way to run NNPPSS is to download the latest **[Releases](https://github.com/allyssonmoscoso/NNPPSS/releases)**.

  

Release assets include a pre-built `.jar` (all dependencies bundled). Download the release archive, extract and run:

```bash
java -jar NNPPSS-{version}.jar
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

1. **First Launch**: Configure database URLs when prompted, save configurations, close and run .jar again.
2. **Adding Downloads**: Select games from table → "Add to Download List" button
3. **Managing Downloads**: Right-click on progress bars for pause/resume/cancel options
4. **Resuming Sessions**: Close and reopen app - downloads automatically resume
4. **Extraction**: Games auto-extract after download

## Troubleshooting

- **Downloads won't start**: Check disk space and configuration
- **Extraction fails**: Ensure pkg2zip is installed (add pkg2zip the environment path)
- **Database empty**: Click "Refresh" button or check database URLs in settings
- **Logs**: Check `logs/nnppss-error.log` for detailed error information, Please report any errors in issues.
