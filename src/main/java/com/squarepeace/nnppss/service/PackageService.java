package com.squarepeace.nnppss.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.util.PathResolver;

/**
 * Runs pkg2zip and reports the result through a callback. This service is
 * UI-agnostic: it never opens dialogs and never touches Swing/JavaFX, so it can
 * be used from the JavaFX coordinator and from tests.
 */
public class PackageService implements PackageExtractor {
    private static final Logger log = LoggerFactory.getLogger(PackageService.class);
    private final ConfigManager configManager;

    public interface ExtractionListener {
        void onExtractionComplete(boolean success);
    }

    public PackageService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void extractPackage(String pkgName, String zRifKey, Console console) {
        extractPackage(pkgName, zRifKey, console, null);
    }

    @Override
    public void extractPackage(String pkgName, String zRifKey, Console console, ExtractionListener listener) {
        log.info("Starting package extraction: {} (console: {})", pkgName, console);
        String command = buildCommand(pkgName, zRifKey, console);
        if (command == null || command.isEmpty()) {
            log.warn("No command generated for package extraction: {}", pkgName);
            notifyComplete(listener, false);
            return;
        }
        runCommand(command, pkgName, listener);
    }

    private String buildCommand(String pkgName, String zRifKey, Console console) {
        String fileSeparator = System.getProperty("file.separator");
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("Building extraction command for OS: {}", os);

        if (os.contains("win")) {
            String exePath = AppPaths.appDir().resolve("lib").resolve("pkg2zip.exe").toString();
            String pkgPath = PathResolver.getFile("packages" + fileSeparator + pkgName).getAbsolutePath();

            if (console == Console.PSVITA) {
                return "\"" + exePath + "\" -x \"" + pkgPath + "\" " + zRifKey;
            }
            return "\"" + exePath + "\" \"" + pkgPath + "\"";
        }

        if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            if (!isCommandInstalled("pkg2zip")) {
                log.error("pkg2zip is not installed on the system PATH");
                return null;
            }
            String pkgPath = PathResolver.getFile("packages" + fileSeparator + pkgName).getAbsolutePath();
            if (console == Console.PSVITA) {
                return "pkg2zip -x \"" + pkgPath + "\" " + zRifKey;
            }
            return "pkg2zip \"" + pkgPath + "\"";
        }

        log.error("Unsupported operating system: {}", os);
        return null;
    }

    private boolean isCommandInstalled(String command) {
        String checkCommand = System.getProperty("os.name").toLowerCase().contains("win")
                ? "where " + command
                : "which " + command;
        try {
            Process process = Runtime.getRuntime().exec(checkCommand);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        process.waitFor();
                        return true;
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.warn("Error checking if command is installed: {}", command, e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private void runCommand(String command, String pkgName, ExtractionListener listener) {
        Thread worker = new Thread(() -> {
            try {
                List<String> cmd;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    cmd = List.of("cmd", "/c", command);
                } else {
                    cmd = List.of("sh", "-lc", command);
                }
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("pkg2zip output: {}", line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    log.error("pkg2zip failed with exit code: {}", exitCode);
                    notifyComplete(listener, false);
                    return;
                }

                log.info("Package extraction completed successfully");
                if (configManager.isAutoCleanupEnabled()) {
                    cleanupPkgFile(pkgName);
                }
                notifyComplete(listener, true);
            } catch (Exception e) {
                log.error("Error running pkg2zip command", e);
                notifyComplete(listener, false);
            }
        }, "pkg2zip-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void notifyComplete(ExtractionListener listener, boolean success) {
        if (listener != null) {
            listener.onExtractionComplete(success);
        }
    }

    private void cleanupPkgFile(String pkgName) {
        String fileSeparator = System.getProperty("file.separator");
        String pkgPath = "packages" + fileSeparator + pkgName;
        File pkgFile = PathResolver.getFile(pkgPath);

        if (pkgFile.exists()) {
            if (pkgFile.delete()) {
                log.info("Auto-cleanup: deleted package file {}", pkgPath);
            } else {
                log.warn("Auto-cleanup: could not delete package file {}", pkgPath);
            }
        } else {
            log.debug("Auto-cleanup: package file not found: {}", pkgPath);
        }
    }
}
