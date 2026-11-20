package com.squarepeace.nnppss.service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squarepeace.nnppss.model.DownloadHistory;
import com.squarepeace.nnppss.util.JsonStore;
import com.squarepeace.nnppss.util.PathResolver;

public class DownloadHistoryManager {
    private static final Logger log = LoggerFactory.getLogger(DownloadHistoryManager.class);
    private static final String HISTORY_FILE = "download-history.json";
    private final Gson gson;

    public DownloadHistoryManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public List<DownloadHistory> loadHistory() {
        Path path = PathResolver.resolve(HISTORY_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<DownloadHistory>>() {
            }.getType();
            List<DownloadHistory> history = JsonStore.read(path, listType, gson);

            if (history == null) {
                log.warn("Download history file is empty or invalid: {}", HISTORY_FILE);
                return new ArrayList<>();
            }
            return history;
        } catch (IOException | RuntimeException e) {
            log.error("Error loading download history from: {}", HISTORY_FILE, e);
            return new ArrayList<>();
        }
    }

    public void saveHistory(List<DownloadHistory> history) {
        if (history == null) {
            history = new ArrayList<>();
        }

        try {
            JsonStore.write(PathResolver.resolve(HISTORY_FILE), history, gson);
            log.debug("Saved {} download history entries to: {}", history.size(), HISTORY_FILE);
        } catch (IOException e) {
            log.error("Error saving download history to: {}", HISTORY_FILE, e);
        }
    }

    public void addEntry(DownloadHistory entry) {
        if (entry == null) {
            return;
        }
        List<DownloadHistory> history = loadHistory();

        // Remove a previous entry with the same pkgUrl, if any.
        history.removeIf(existing -> existing != null
                && java.util.Objects.equals(existing.getPkgUrl(), entry.getPkgUrl()));

        history.add(entry);
        saveHistory(history);
    }

    public DownloadHistory findByUrl(String pkgUrl) {
        return loadHistory().stream()
                .filter(entry -> java.util.Objects.equals(entry.getPkgUrl(), pkgUrl))
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

    /** Marks the history entry as successfully extracted. */
    public void markExtracted(String pkgUrl) {
        if (pkgUrl == null) {
            return;
        }
        List<DownloadHistory> history = loadHistory();
        boolean changed = false;
        for (DownloadHistory entry : history) {
            if (entry != null && java.util.Objects.equals(entry.getPkgUrl(), pkgUrl)
                    && !entry.isExtracted()) {
                entry.setExtracted(true);
                changed = true;
            }
        }
        if (changed) {
            saveHistory(history);
        }
    }

    /**
     * True when the resolved file belongs to a history entry that was
     * successfully extracted by the app.
     */
    public boolean isExtractedFile(Path file) {
        if (file == null) {
            return false;
        }
        Path normalized = file.toAbsolutePath().normalize();
        for (DownloadHistory entry : loadHistory()) {
            if (entry == null || !entry.isExtracted() || entry.getFilePath() == null) {
                continue;
            }
            Path recorded = PathResolver.resolve(entry.getFilePath()).toAbsolutePath().normalize();
            if (recorded.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
