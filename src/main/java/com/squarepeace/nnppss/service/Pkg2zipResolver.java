package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.app.AppPaths;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Resolves the pkg2zip executable without executing it. The lookup order is:
 * configured path, bundled binary under {@code app/lib}, then {@code PATH}.
 */
public final class Pkg2zipResolver {

    public enum Source {
        CONFIGURED, BUNDLED, PATH, NOT_FOUND, INVALID
    }

    public record Resolution(Source source, Path executable, String detail) {
    }

    private final ConfigManager configManager;
    private final Path appDir;
    private final String pathEnvironment;
    private final boolean windows;

    public Pkg2zipResolver(ConfigManager configManager) {
        this(configManager, AppPaths.appDir(), System.getenv("PATH"), isWindows());
    }

    Pkg2zipResolver(ConfigManager configManager, Path appDir,
                    String pathEnvironment, boolean windows) {
        this.configManager = configManager;
        this.appDir = appDir;
        this.pathEnvironment = pathEnvironment == null ? "" : pathEnvironment;
        this.windows = windows;
    }

    public Resolution resolve() {
        Path configured = configManager == null ? null : configManager.getPkg2zipPath();
        if (configured != null) {
            Path executable = executableIn(configured);
            if (executable != null) {
                return new Resolution(Source.CONFIGURED, executable,
                        executable.toString());
            }
            return new Resolution(Source.INVALID,
                    Files.isDirectory(configured) ? null : configured,
                    configured.toString());
        }

        if (appDir != null) {
            Path bundled = appDir.resolve("lib").resolve(executableName());
            if (isUsable(bundled)) {
                return new Resolution(Source.BUNDLED, bundled, bundled.toString());
            }
        }

        Path fromPath = findOnPath();
        if (fromPath != null) {
            return new Resolution(Source.PATH, fromPath, fromPath.toString());
        }

        return new Resolution(Source.NOT_FOUND, null, "");
    }

    /** Finds the executable in a directory chosen by the user. */
    public Path findInDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }
        return executableIn(directory);
    }

    public static String executableName() {
        return isWindows() ? "pkg2zip.exe" : "pkg2zip";
    }

    private Path executableIn(Path path) {
        if (path == null) {
            return null;
        }
        if (Files.isDirectory(path)) {
            Path candidate = path.resolve(executableName());
            return isUsable(candidate) ? candidate : null;
        }
        return isUsable(path) ? path : null;
    }

    private Path findOnPath() {
        for (String entry : pathEnvironment.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path candidate = Paths.get(entry).resolve(executableName());
            if (isUsable(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private boolean isUsable(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        return windows || Files.isExecutable(path);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win");
    }
}
