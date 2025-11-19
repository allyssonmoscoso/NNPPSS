# NNPPSS

A professional tool for managing and downloading PlayStation (PSP, PS Vita, PSX) game packages. Built with Java and Swing, NNPPSS provides a robust download manager with persistence, retry capabilities, and automated package extraction.

NNPPSS is compatible with Windows, macOS, and Linux/Unix-based operating systems.

## Prerequisites

- **Java 17+** (required)
- **[pkg2zip](https://github.com/mmozeiko/pkg2zip)** (required for non-Windows systems, included for Windows)

## Installation

### Quick Start (Recommended)

1. Clone the repository:
   ```bash
   git clone https://github.com/allyssonmoscoso/NNPPSS.git
   cd NNPPSS
   ```

2. Build the application:
   ```bash
   ./build.sh
   ```

3. Run the application:
   ```bash
   ./run.sh
   ```
   Or directly:
   ```bash
   java -jar dist/NNPPSS-fat.jar
   ```

### Development Mode

For quick compilation and testing:
```bash
./run-dev.sh          # Compile and run
./run-dev.sh compile  # Compile only
./run-dev.sh run      # Run without recompiling
./run-dev.sh clean    # Clean build artifacts
```

## Features

### Download Management

- **Multi-threaded Downloads**: Configure up to 4 simultaneous downloads
- **HTTP Resume Support**: Resume interrupted downloads from where they left off
- **Automatic Retry**: Failed downloads automatically retry up to 3 times with exponential backoff (2s, 4s, 8s)
- **Download Persistence**: Downloads automatically resume when restarting the application
- **Progress Tracking**: Real-time progress bars with:
  - Download percentage
  - Download speed (MiB/s with smoothing algorithm)
  - ETA (estimated time remaining)
  - Pause/Resume status

### Download Controls

- **Individual Download Management**:
  - Pause/resume specific downloads via context menu
  - Cancel downloads with confirmation dialog
  - View download status and progress in real-time
- **Batch Selection**:
  - Click to select single game
  - Ctrl+Click for multiple selection
  - Shift+Click for range selection
  - Context menu for paused downloads

### Game Database

- **Multi-console Support**: PSP, PS Vita (PSV), and PSX games
- **Advanced Search**: Real-time text filtering across game titles
- **Region Filter**: Filter games by region (US, EU, JP, etc.)
- **Auto-sync**: Game databases automatically download on first launch
- **Refresh**: Manual database refresh button available

### Package Management

- **Automatic Extraction**: Games are automatically extracted after download
- **Console-specific Handling**:
  - PS Vita: Requires zRIF key for extraction
  - PSP/PSX: Direct extraction without additional keys
- **Background Processing**: Extraction runs in background without blocking UI

### System Validation

- **Disk Space Check**: Validates sufficient disk space before starting downloads
- **5GB Safety Buffer**: Ensures system stability by maintaining free space buffer
- **User Notifications**: Clear warnings when disk space is insufficient

### Configuration

- **Persistent Settings**: Configuration saved in `config.properties`
- **Customizable Options**:
  - Database URLs for each console
  - Simultaneous download limit (1-4)
- **Easy Setup**: Configuration wizard on first launch

### Professional Logging

- **SLF4J + Logback Integration**:
  - Main log: `logs/nnppss.log` (7-day rotation)
  - Error log: `logs/nnppss-error.log` (30-day rotation)
  - Console output for immediate feedback
- **Comprehensive Coverage**:
  - Download operations and errors
  - Retry attempts and success/failure
  - Database loading and parsing
  - Configuration changes
  - Package extraction status
  - System validation checks

## Architecture

NNPPSS follows Clean Code principles with a modular service-oriented architecture.

## File Structure

```
NNPPSS/
├── config.properties          # User configuration
├── download-state.json        # Download persistence (auto-managed)
├── logs/                      # Application logs
│   ├── nnppss.log            # Main log file
│   └── nnppss-error.log      # Error log file
├── games/                     # Downloaded games
├── db/                        # Game databases (TSV format)
│   ├── PSP_GAMES.tsv
│   ├── PSV_GAMES.tsv
│   └── PSX_GAMES.tsv
├── lib/                       # Dependencies (auto-downloaded)
└── dist/                      # Compiled JARs
    ├── NNPPSS.jar            # Regular JAR (requires lib/)
    └── NNPPSS-fat.jar        # Fat JAR (all dependencies included)
```

## Usage Tips

1. **First Launch**: Configure database URLs when prompted
2. **Adding Downloads**: Select games from table → "Add to Download List" button
3. **Managing Downloads**: Right-click on progress bars for pause/resume/cancel options
4. **Resuming Sessions**: Close and reopen app - downloads automatically resume
5. **Disk Space**: Ensure at least 5GB free space plus download size
6. **Extraction**: Games auto-extract after download (PS Vita requires valid zRIF)

## Troubleshooting

- **Downloads won't start**: Check disk space and configuration
- **Extraction fails**: Ensure pkg2zip is installed (non-Windows) or verify zRIF for PS Vita
- **Database empty**: Click "Refresh" button or check database URLs in settings
- **Logs**: Check `logs/nnppss-error.log` for detailed error information

## Building from Source

Dependencies are automatically downloaded during build:
- jsoup 1.15.3
- Gson 2.10.1
- SLF4J 2.0.9
- Logback 1.4.11

The build scripts (`build.sh` and `run-dev.sh`) handle all dependency management.
