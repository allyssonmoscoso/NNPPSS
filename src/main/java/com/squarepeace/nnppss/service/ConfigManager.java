package com.squarepeace.nnppss.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        properties.setProperty("autoCleanupPkg", "false");
        saveConfig();
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
}
