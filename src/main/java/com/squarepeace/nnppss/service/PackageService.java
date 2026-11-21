package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.model.Console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs pkg2zip and reports the result through a callback. This service is
 * UI-agnostic: it never opens dialogs and never touches Swing/JavaFX, so it can
 * be used from the JavaFX coordinator and from tests.
 */
public class PackageService implements PackageExtractor {
    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    private final Pkg2zipResolver resolver;

    public interface ExtractionListener {
        void onExtractionComplete(boolean success);
    }

    public PackageService(ConfigManager configManager) {
        this(configManager, new Pkg2zipResolver(configManager));
    }

    PackageService(ConfigManager configManager, Pkg2zipResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void extractPackage(Path pkgPath, Path outputDirectory, String zRifKey,
                               Console console, ExtractionListener listener) {
        if (pkgPath == null || !Files.isRegularFile(pkgPath)) {
            log.warn("Package file does not exist: {}", pkgPath);
            notifyComplete(listener, false);
            return;
        }
        if (outputDirectory == null) {
            log.warn("No extraction directory configured");
            notifyComplete(listener, false);
            return;
        }
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            log.error("Could not create extraction directory: {}", outputDirectory, e);
            notifyComplete(listener, false);
            return;
        }

        Pkg2zipResolver.Resolution resolution = resolver.resolve();
        if (resolution.source() == Pkg2zipResolver.Source.NOT_FOUND
                || resolution.source() == Pkg2zipResolver.Source.INVALID) {
            log.warn("pkg2zip is not available: {}", resolution.detail());
            notifyComplete(listener, false);
            return;
        }

        List<String> command = buildCommand(
                resolution.executable(), pkgPath, console, zRifKey);
        log.info("Starting package extraction with {} in {}",
                resolution.source(), outputDirectory);
        runCommand(command, outputDirectory, listener);
    }

    private List<String> buildCommand(Path executable, Path pkgPath,
                                      Console console, String zRifKey) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        if (console == Console.PSVITA) {
            command.add("-x");
            command.add(pkgPath.toAbsolutePath().normalize().toString());
            if (zRifKey != null && !zRifKey.isBlank()) {
                command.add(zRifKey);
            }
        } else {
            // Preserve the existing PSP/PSX behaviour: pkg2zip creates a zip.
            command.add(pkgPath.toAbsolutePath().normalize().toString());
        }
        return command;
    }

    private void runCommand(List<String> command, Path outputDirectory,
                            ExtractionListener listener) {
        Thread worker = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(outputDirectory.toFile());
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
}
