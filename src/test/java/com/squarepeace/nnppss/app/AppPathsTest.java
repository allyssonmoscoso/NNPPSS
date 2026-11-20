package com.squarepeace.nnppss.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void overrideDataDir() {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.toString());
    }

    @AfterEach
    void clearDataDir() {
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void resolvesRelativePathsUnderDataDir() {
        assertEquals(tempDir.toAbsolutePath().normalize(),
                AppPaths.dataDir());
        assertEquals(tempDir.resolve("db").resolve("PSV_GAMES.tsv").normalize(),
                AppPaths.resolve("db/PSV_GAMES.tsv"));
    }

    @Test
    void createsDataDirectoryAndSubdirectories() {
        Path nested = AppPaths.resolve("packages/PSVITA");
        assertFalse(Files.exists(nested));

        assertTrue(AppPaths.ensureDirectory("packages/PSVITA"));
        assertTrue(Files.isDirectory(nested));
    }

    @Test
    void defaultDataDirIsNamedAfterTheApplication() {
        assertEquals("NNPPSS", AppPaths.defaultDataDir().getFileName().toString());
        assertTrue(AppPaths.defaultDataDir().isAbsolute());
    }

    @Test
    void appDirExists() {
        assertTrue(Files.isDirectory(AppPaths.appDir()));
    }

    @Test
    void migratesLegacyEntriesOnlyOnce() throws IOException {
        Path source = tempDir.resolve("legacy");
        Path target = tempDir.resolve("data");
        Files.createDirectories(source.resolve("db"));
        Files.createDirectories(source.resolve("packages"));
        Files.writeString(source.resolve("config.properties"), "language=en\n");
        Files.writeString(source.resolve("db/PSV_GAMES.tsv"), "header\n");
        Files.writeString(source.resolve("packages/game.pkg"), "payload");

        List<String> migrated = AppPaths.migrateLegacyData(source, target);

        assertTrue(migrated.contains("config.properties"));
        assertTrue(migrated.contains("db"));
        assertTrue(migrated.contains("packages"));
        assertTrue(Files.exists(target.resolve("config.properties")));
        assertTrue(Files.exists(target.resolve("db/PSV_GAMES.tsv")));
        assertTrue(Files.exists(target.resolve("packages/game.pkg")));
        assertTrue(Files.exists(target.resolve(".migrated-from-legacy")));

        assertTrue(AppPaths.migrateLegacyData(source, target).isEmpty());
    }

    @Test
    void doesNotOverwriteExistingData() throws IOException {
        Path source = tempDir.resolve("legacy");
        Path target = tempDir.resolve("data");
        Files.createDirectories(source);
        Files.createDirectories(target);
        Files.writeString(source.resolve("config.properties"), "legacy\n");
        Files.writeString(target.resolve("config.properties"), "current\n");

        List<String> migrated = AppPaths.migrateLegacyData(source, target);

        assertFalse(migrated.contains("config.properties"));
        assertEquals("current\n", Files.readString(target.resolve("config.properties")));
    }

    @Test
    void legacyPathResolverDelegatesToAppPaths() {
        @SuppressWarnings("deprecation")
        Path resolved = com.squarepeace.nnppss.util.PathResolver
                .resolve("download-queue.json");
        assertEquals(tempDir.resolve("download-queue.json").normalize(), resolved);
    }
}
