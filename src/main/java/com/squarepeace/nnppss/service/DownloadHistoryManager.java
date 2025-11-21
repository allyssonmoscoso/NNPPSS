package com.squarepeace.nnppss.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squarepeace.nnppss.model.DownloadHistory;

public class DownloadHistoryManager {
    private static final Logger log = LoggerFactory.getLogger(DownloadHistoryManager.class);
    private static final String HISTORY_FILE = "download-history.json";
    private final Gson gson;

    public DownloadHistoryManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public List<DownloadHistory> loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            //log.debug("No download history file found: {}", HISTORY_FILE);
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<DownloadHistory>>(){}.getType();
            List<DownloadHistory> history = gson.fromJson(reader, listType);
            
            if (history == null) {
                log.warn("Download history file is empty or invalid: {}", HISTORY_FILE);
                return new ArrayList<>();
            }
            
            //log.info("Loaded {} download history entries", history.size());
            return history;
        } catch (IOException e) {
            log.error("Error loading download history from: {}", HISTORY_FILE, e);
            return new ArrayList<>();
        }
    }

    public void saveHistory(List<DownloadHistory> history) {
        if (history == null) {
            history = new ArrayList<>();
        }

        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            gson.toJson(history, writer);
            log.debug("Saved {} download history entries to: {}", history.size(), HISTORY_FILE);
        } catch (IOException e) {
            log.error("Error saving download history to: {}", HISTORY_FILE, e);
        }
    }

    public void addEntry(DownloadHistory entry) {
        List<DownloadHistory> history = loadHistory();
        
        // Remover entrada anterior con mismo pkgUrl si existe
        history.removeIf(h -> h.getPkgUrl().equals(entry.getPkgUrl()));
        
        history.add(entry);
        saveHistory(history);
    }

    public DownloadHistory findByUrl(String pkgUrl) {
        List<DownloadHistory> history = loadHistory();
        return history.stream()
                .filter(h -> h.getPkgUrl().equals(pkgUrl))
                .findFirst()
                .orElse(null);
    }

    public boolean isDownloaded(String pkgUrl) {
        DownloadHistory entry = findByUrl(pkgUrl);
        return entry != null && "completed".equals(entry.getStatus());
    }

    public boolean isFailed(String pkgUrl) {
        DownloadHistory entry = findByUrl(pkgUrl);
        return entry != null && "failed".equals(entry.getStatus());
    }

    public boolean isPaused(String pkgUrl) {
        DownloadHistory entry = findByUrl(pkgUrl);
        return entry != null && "paused".equals(entry.getStatus());
    }
}
