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
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.util.JsonStore;
import com.squarepeace.nnppss.util.PathResolver;

/**
 * Manager for persisting the download queue (pending downloads)
 */
public class DownloadQueueManager {
    private static final Logger log = LoggerFactory.getLogger(DownloadQueueManager.class);
    private static final String QUEUE_FILE = "download-queue.json";
    private final Gson gson;

    public DownloadQueueManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Load the download queue from disk
     */
    public List<Game> loadQueue() {
        Path path = PathResolver.resolve(QUEUE_FILE);
        if (!Files.exists(path)) {
            log.debug("No download queue file found: {}", QUEUE_FILE);
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<Game>>() {
            }.getType();
            List<Game> queue = JsonStore.read(path, listType, gson);

            if (queue == null) {
                log.warn("Download queue file is empty or invalid: {}", QUEUE_FILE);
                return new ArrayList<>();
            }

            log.info("Loaded {} games from download queue", queue.size());
            return queue;
        } catch (IOException | RuntimeException e) {
            log.error("Error loading download queue from: {}", QUEUE_FILE, e);
            return new ArrayList<>();
        }
    }

    /**
     * Save the download queue to disk
     */
    public void saveQueue(List<Game> queue) {
        if (queue == null) {
            queue = new ArrayList<>();
        }

        try {
            JsonStore.write(PathResolver.resolve(QUEUE_FILE), queue, gson);
            log.debug("Saved {} games to download queue: {}", queue.size(), QUEUE_FILE);
        } catch (IOException e) {
            log.error("Error saving download queue to: {}", QUEUE_FILE, e);
        }
    }

    /**
     * Clear the download queue file
     */
    public void clearQueue() {
        saveQueue(new ArrayList<>());
        log.info("Download queue cleared");
    }

    /**
     * Check if queue file exists and has content
     */
    public boolean hasQueuedDownloads() {
        Path path = PathResolver.resolve(QUEUE_FILE);
        try {
            return Files.exists(path) && Files.size(path) > 2;
        } catch (IOException e) {
            log.warn("Could not inspect the download queue file", e);
            return false;
        }
    }
}
