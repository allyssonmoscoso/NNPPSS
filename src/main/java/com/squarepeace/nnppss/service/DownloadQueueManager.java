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
import com.squarepeace.nnppss.model.Game;

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
        File file = new File(QUEUE_FILE);
        if (!file.exists()) {
            log.debug("No download queue file found: {}", QUEUE_FILE);
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Game>>(){}.getType();
            List<Game> queue = gson.fromJson(reader, listType);
            
            if (queue == null) {
                log.warn("Download queue file is empty or invalid: {}", QUEUE_FILE);
                return new ArrayList<>();
            }
            
            log.info("Loaded {} games from download queue", queue.size());
            return queue;
        } catch (IOException e) {
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

        try (FileWriter writer = new FileWriter(QUEUE_FILE)) {
            gson.toJson(queue, writer);
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
        File file = new File(QUEUE_FILE);
        return file.exists() && file.length() > 0;
    }
}
