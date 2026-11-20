package com.squarepeace.nnppss.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Resolves the locations used by the application:
 *
 * <ul>
 *   <li>{@link #appDir()}: where the application (JAR or classes) lives. Read-only
 *       in an installed application; used to locate bundled tools such as pkg2zip.</li>
 *   <li>{@link #dataDir()}: per-user writable directory for configuration,
 *       databases, downloads, history and logs.</li>
 * </ul>
 *
 * The data directory can be overridden with the {@code nnppss.dataDir} system
 * property, which is also what the test suite does.
 */
public final class AppPaths {

    /** System property that overrides the per-user data directory. */
    public static final String DATA_DIR_PROPERTY = "nnppss.dataDir";

    private static final Logger LOG = Logger.getLogger(AppPaths.class.getName());
    private static final String APP_NAME = "NNPPSS";
    private static final String MIGRATION_MARKER = ".migrated-from-legacy";
    private static final long MAX_MIGRATED_PACKAGES_BYTES = 512L * 1024 * 1024;

    /** Files and directories copied from a legacy (next-to-the-JAR) layout. */
    private static final String[] MIGRATED_ENTRIES = {
            "config.properties",
            "download-queue.json",
            "download-history.json",
            "download-state.json",
            "db",
            "logs"
    };

    private static final Object LOCK = new Object();
    private static String cachedKey;
    private static Path cachedDataDir;

    private AppPaths() {
    }

    /**
     * Directory that contains the running application. When running from a JAR
     * this is the JAR's parent directory; in development it is the current
     * working directory.
     */
    public static Path appDir() {
        try {
            Path location = Paths.get(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.toAbsolutePath().normalize().getParent();
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not determine the application directory", e);
        }
        return Paths.get("").toAbsolutePath().normalize();
    }

    /**
     * Per-user writable directory. It is created if it does not exist.
     */
    public static Path dataDir() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        String key = override == null ? "" : override.trim();

        Path cached = cachedDataDir;
        if (cached != null && key.equals(cachedKey)) {
            return cached;
        }

        synchronized (LOCK) {
            if (cachedDataDir != null && key.equals(cachedKey)) {
                return cachedDataDir;
            }
            Path dir = key.isEmpty() ? defaultDataDir() : Paths.get(key);
            dir = dir.toAbsolutePath().normalize();
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create the data directory: " + dir, e);
            }
            cachedKey = key;
            cachedDataDir = dir;
            return dir;
        }
    }

    /**
     * Platform default data directory:
     * {@code %APPDATA%\NNPPSS}, {@code ~/Library/Application Support/NNPPSS}
     * or {@code $XDG_DATA_HOME/NNPPSS} (falling back to
     * {@code ~/.local/share/NNPPSS}).
     */
    public static Path defaultDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path home = Paths.get(System.getProperty("user.home", "."));

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = (appData != null && !appData.isBlank())
                    ? Paths.get(appData)
                    : home.resolve("AppData").resolve("Roaming");
            return base.resolve(APP_NAME);
        }
        if (os.contains("mac")) {
            return home.resolve("Library").resolve("Application Support").resolve(APP_NAME);
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        Path base = (xdg != null && !xdg.isBlank())
                ? Paths.get(xdg)
                : home.resolve(".local").resolve("share");
        return base.resolve(APP_NAME);
    }

    /** Resolves a path relative to {@link #dataDir()}. */
    public static Path resolve(String relativePath) {
        return dataDir().resolve(relativePath).normalize();
    }

    /** Ensures that a directory relative to {@link #dataDir()} exists. */
    public static boolean ensureDirectory(String relativePath) {
        Path dir = resolve(relativePath);
        try {
            Files.createDirectories(dir);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not create directory: " + dir, e);
            return false;
        }
    }

    /**
     * Copies configuration, databases and small legacy files from the
     * application directory into the data directory the first time the
     * application runs with the new layout.
     *
     * @return the names of the entries that were migrated
     */
    public static List<String> migrateLegacyData() {
        return migrateLegacyData(appDir(), dataDir());
    }

    static List<String> migrateLegacyData(Path source, Path target) {
        List<String> migrated = new ArrayList<>();
        source = source.toAbsolutePath().normalize();
        target = target.toAbsolutePath().normalize();

        if (source.equals(target)) {
            return migrated;
        }

        Path marker = target.resolve(MIGRATION_MARKER);
        if (Files.exists(marker)) {
            return migrated;
        }

        for (String name : MIGRATED_ENTRIES) {
            Path from = source.resolve(name);
            Path to = target.resolve(name);
            if (!Files.exists(from) || Files.exists(to)) {
                continue;
            }
            try {
                copyEntry(from, to);
                migrated.add(name);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not migrate " + from + " to " + to, e);
            }
        }

        // Packages can be very large: only migrate them when they are small
        // enough to be considered local test data, never multi-gigabyte downloads.
        Path legacyPackages = source.resolve("packages");
        Path targetPackages = target.resolve("packages");
        if (Files.isDirectory(legacyPackages) && !Files.exists(targetPackages)
                && directorySize(legacyPackages) <= MAX_MIGRATED_PACKAGES_BYTES) {
            try {
                copyEntry(legacyPackages, targetPackages);
                migrated.add("packages");
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not migrate " + legacyPackages, e);
            }
        }

        try {
            Files.writeString(marker,
                    "migrated-from=" + source + System.lineSeparator()
                            + "migrated-at=" + System.currentTimeMillis() + System.lineSeparator());
        } catch (IOException e) {
            LOG.log(Level.FINE, "Could not write the migration marker", e);
        }
        return migrated;
    }

    private static void copyEntry(Path from, Path to) throws IOException {
        if (Files.isDirectory(from)) {
            try (Stream<Path> walk = Files.walk(from)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    Path relative = from.relativize(src);
                    Path dst = to.resolve(relative);
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dst);
                    } else {
                        Files.createDirectories(dst.getParent());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } else {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static long directorySize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}
