package com.squarepeace.nnppss.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.squarepeace.nnppss.app.AppPaths;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

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
    void createsDefaultConfigurationInDataDir() {
        ConfigManager manager = new ConfigManager();
        assertTrue(Files.exists(tempDir.resolve("config.properties")));
        assertEquals("en", manager.getLanguage());
        assertEquals(0, manager.getDownloadSpeedLimit());
        assertEquals(false, manager.isDarkModeEnabled());
        assertEquals(false, manager.isAutoCleanupEnabled());
    }

    @Test
    void appliesDefaultsWhenLegacyConfigIsMissingKeys() throws Exception {
        Files.writeString(tempDir.resolve("config.properties"),
                "psp.url=https://example.test/psp.tsv\n");
        ConfigManager manager = new ConfigManager();

        assertEquals("https://example.test/psp.tsv", manager.getPspUrl());
        assertEquals("en", manager.getLanguage());
        assertEquals(1, manager.getSimultaneousDownloads());
        assertEquals(0, manager.getDownloadSpeedLimit());
        assertEquals(false, manager.isDarkModeEnabled());
    }

    @Test
    void readsDarkModeAndLanguage() {
        ConfigManager manager = new ConfigManager();
        manager.setProperty("darkMode", "true");
        manager.setProperty("language", "es");

        assertTrue(manager.isDarkModeEnabled());
        assertEquals("es", manager.getLanguage());
    }

    @Test
    void savesAndReloadsProperties() {
        ConfigManager manager = new ConfigManager();
        manager.setProperty("psvita.url", "https://example.test/psv.tsv");
        manager.setProperty("simultaneousDownloads", "3");
        manager.setProperty("downloadSpeedLimit", "250");
        manager.setProperty("autoCleanupPkg", "true");
        manager.saveConfig();

        ConfigManager reloaded = new ConfigManager();
        assertEquals("https://example.test/psv.tsv", reloaded.getPsVitaUrl());
        assertEquals(3, reloaded.getSimultaneousDownloads());
        assertEquals(250, reloaded.getDownloadSpeedLimit());
        assertTrue(reloaded.isAutoCleanupEnabled());
    }

    @Test
    void clampsSimultaneousDownloadsToSupportedRange() {
        ConfigManager manager = new ConfigManager();

        manager.setProperty("simultaneousDownloads", "99");
        assertEquals(4, manager.getSimultaneousDownloads());

        manager.setProperty("simultaneousDownloads", "0");
        assertEquals(1, manager.getSimultaneousDownloads());

        manager.setProperty("simultaneousDownloads", "not-a-number");
        assertEquals(1, manager.getSimultaneousDownloads());
    }

    @Test
    void negativeSpeedLimitBecomesUnlimited() {
        ConfigManager manager = new ConfigManager();
        manager.setProperty("downloadSpeedLimit", "-10");
        assertEquals(0, manager.getDownloadSpeedLimit());
    }
}
