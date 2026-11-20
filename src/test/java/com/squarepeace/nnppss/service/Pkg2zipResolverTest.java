package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.app.AppPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class Pkg2zipResolverTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;
    private boolean windows;

    @BeforeEach
    void setUp() {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.resolve("data").toString());
        configManager = new ConfigManager();
        windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void findsConfiguredExecutableFirst() throws Exception {
        Path executable = executable(tempDir.resolve("custom"));
        configManager.setPkg2zipPath(executable);

        Pkg2zipResolver.Resolution result = new Pkg2zipResolver(
                configManager, tempDir.resolve("app"), "", windows).resolve();

        assertEquals(Pkg2zipResolver.Source.CONFIGURED, result.source());
        assertEquals(executable, result.executable());
    }

    @Test
    void findsBundledExecutable() throws Exception {
        Path appDir = tempDir.resolve("app");
        Path executable = executable(appDir.resolve("lib"));

        Pkg2zipResolver.Resolution result = new Pkg2zipResolver(
                configManager, appDir, "", windows).resolve();

        assertEquals(Pkg2zipResolver.Source.BUNDLED, result.source());
        assertEquals(executable, result.executable());
    }

    @Test
    void findsExecutableOnPath() throws Exception {
        Path binDir = tempDir.resolve("bin");
        Path executable = executable(binDir);

        Pkg2zipResolver.Resolution result = new Pkg2zipResolver(
                configManager, tempDir.resolve("app"), binDir.toString(), windows).resolve();

        assertEquals(Pkg2zipResolver.Source.PATH, result.source());
        assertEquals(executable.toAbsolutePath().normalize(), result.executable());
    }

    @Test
    void reportsInvalidConfiguredPathInsteadOfFallingBack() {
        Path missing = tempDir.resolve("missing").resolve("pkg2zip.exe");
        configManager.setPkg2zipPath(missing);

        Pkg2zipResolver.Resolution result = new Pkg2zipResolver(
                configManager, tempDir.resolve("app"), "", windows).resolve();

        assertEquals(Pkg2zipResolver.Source.INVALID, result.source());
        assertNotNull(result.detail());
    }

    @Test
    void reportsNotFound() {
        Pkg2zipResolver.Resolution result = new Pkg2zipResolver(
                configManager, tempDir.resolve("app"), "", windows).resolve();

        assertEquals(Pkg2zipResolver.Source.NOT_FOUND, result.source());
        assertNull(result.executable());
    }

    private Path executable(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path executable = directory.resolve(windows ? "pkg2zip.exe" : "pkg2zip");
        Files.createFile(executable);
        if (!windows) {
            executable.toFile().setExecutable(true);
        }
        return executable;
    }
}
