package com.squarepeace.nnppss.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.util.PathResolver;

public class ConfigManager {
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        File file = PathResolver.getFile(CONFIG_FILE);
        if (!file.exists()) {
            log.info("Config file not found, creating default configuration");
            createDefaultConfig();
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
            applyMissingDefaults();
            log.info("Configuration loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Error loading configuration file: {}", CONFIG_FILE, e);
        }
    }

    private void createDefaultConfig() {
        log.debug("Creating default configuration");
        properties.setProperty("psp.url", "");
        properties.setProperty("psvita.url", "");
        properties.setProperty("psx.url", "");
        properties.setProperty("simultaneousDownloads", "1");
        properties.setProperty("downloadSpeedLimit", "0");
        properties.setProperty("autoCleanupPkg", "false");
        properties.setProperty("darkMode", "false");
        properties.setProperty("language", "en");
        properties.setProperty("downloads.directory", "");
        properties.setProperty("extraction.directory", "");
        properties.setProperty("pkg2zip.path", "");
        properties.setProperty("updates.checkOnStartup", "true");
        saveConfig();
    }

    private void applyMissingDefaults() {
        putIfMissing("psp.url", "");
        putIfMissing("psvita.url", "");
        putIfMissing("psx.url", "");
        putIfMissing("simultaneousDownloads", "1");
        putIfMissing("downloadSpeedLimit", "0");
        putIfMissing("autoCleanupPkg", "false");
        putIfMissing("darkMode", "false");
        putIfMissing("language", "en");
        putIfMissing("downloads.directory", "");
        putIfMissing("extraction.directory", "");
        putIfMissing("pkg2zip.path", "");
        putIfMissing("updates.checkOnStartup", "true");
    }

    private void putIfMissing(String key, String value) {
        if (properties.getProperty(key) == null) {
            properties.setProperty(key, value);
        }
    }

    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(PathResolver.getFile(CONFIG_FILE))) {
            properties.store(fos, "NNPPSS Configuration");
            log.info("Configuration saved to {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Error saving configuration file: {}", CONFIG_FILE, e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public String getPspUrl() {
        return getProperty("psp.url");
    }

    public String getPsVitaUrl() {
        return getProperty("psvita.url");
    }

    public String getPsxUrl() {
        return getProperty("psx.url");
    }

    public int getSimultaneousDownloads() {
        String value = getProperty("simultaneousDownloads");
        int n = 1;
        try {
            if (value != null) {
                n = Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException ignore) {
            n = 1;
        }
        if (n < 1) n = 1;
        if (n > 4) n = 4;
        return n;
    }

    public int getDownloadSpeedLimit() {
        String value = getProperty("downloadSpeedLimit");
        int limit = 0;
        try {
            if (value != null) {
                limit = Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException ignore) {
            limit = 0;
        }
        return limit < 0 ? 0 : limit;
    }

    public boolean isAutoCleanupEnabled() {
        String value = getProperty("autoCleanupPkg");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean isDarkModeEnabled() {
        return Boolean.parseBoolean(getProperty("darkMode"));
    }

    public String getLanguage() {
        String value = getProperty("language");
        return value == null || value.isBlank() ? "en" : value.trim();
    }

    public boolean isUpdateCheckOnStartupEnabled() {
        return !"false".equalsIgnoreCase(getProperty("updates.checkOnStartup"));
    }

    public long getLastUpdateCheckEpoch() {
        String value = getProperty("updates.lastCheck");
        try {
            return value == null ? 0L : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void markUpdateChecked(long epochSeconds) {
        setProperty("updates.lastCheck", String.valueOf(epochSeconds));
        saveConfig();
    }

    /** Folder where .pkg files are stored; legacy default is {@code dataDir/packages}. */
    public Path getDownloadsDirectory() {
        return resolveDirectory("downloads.directory", "packages");
    }

    /** Folder where pkg2zip writes its output; default is {@code dataDir/extracted}. */
    public Path getExtractionDirectory() {
        return resolveDirectory("extraction.directory", "extracted");
    }

    /** Configured pkg2zip executable, or null when automatic detection should run. */
    public Path getPkg2zipPath() {
        String value = getProperty("pkg2zip.path");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = Paths.get(value.trim());
            return path.isAbsolute() ? path.normalize() : AppPaths.dataDir().resolve(path).normalize();
        } catch (InvalidPathException e) {
            log.warn("Invalid pkg2zip.path, falling back to automatic detection: {}", value);
            return null;
        }
    }

    public void setDownloadsDirectory(Path directory) {
        setProperty("downloads.directory", toStoredPath(directory));
    }

    public void setExtractionDirectory(Path directory) {
        setProperty("extraction.directory", toStoredPath(directory));
    }

    public void setPkg2zipPath(Path executable) {
        setProperty("pkg2zip.path", executable == null ? "" : toStoredPath(executable));
    }

    private Path resolveDirectory(String key, String defaultName) {
        Path fallback = AppPaths.dataDir().resolve(defaultName).normalize();
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            Path path = Paths.get(value.trim());
            if (!path.isAbsolute()) {
                path = AppPaths.dataDir().resolve(path);
            }
            return path.normalize();
        } catch (InvalidPathException e) {
            log.warn("Invalid {} value, using default: {}", key, value);
            return fallback;
        }
    }

    private String toStoredPath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
