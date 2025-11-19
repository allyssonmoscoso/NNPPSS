package com.squarepeace.nnppss.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            createDefaultConfig();
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultConfig() {
        properties.setProperty("psp.url", "");
        properties.setProperty("psvita.url", "");
        properties.setProperty("psx.url", "");
        properties.setProperty("simultaneousDownloads", "1");
        saveConfig();
    }

    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "NNPPSS Configuration");
        } catch (IOException e) {
            e.printStackTrace();
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
}
