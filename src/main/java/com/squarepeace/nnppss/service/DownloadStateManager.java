package com.squarepeace.nnppss.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squarepeace.nnppss.model.DownloadState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadStateManager {
    private static final String STATE_FILE = "download-state.json";
    private static final long STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000; // 24 hours
    private final Gson gson;

    public DownloadStateManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Load download states from JSON file
     * @return List of DownloadState objects, or empty list if file doesn't exist or error occurs
     */
    public List<DownloadState> loadStates() {
        File file = new File(STATE_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<DownloadState>>(){}.getType();
            List<DownloadState> states = gson.fromJson(reader, listType);
            
            if (states == null) {
                return new ArrayList<>();
            }
            
            // Filter out completed downloads and stale downloads
            return states.stream()
                    .filter(state -> !"completed".equals(state.getStatus()))
                    .filter(state -> !isStale(state))
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            System.err.println("Error loading download states: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Save download states to JSON file
     * @param states List of DownloadState objects to save
     */
    public void saveStates(List<DownloadState> states) {
        if (states == null) {
            states = new ArrayList<>();
        }

        try (FileWriter writer = new FileWriter(STATE_FILE)) {
            gson.toJson(states, writer);
        } catch (IOException e) {
            System.err.println("Error saving download states: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if a download state is stale (older than 24 hours)
     * @param state DownloadState to check
     * @return true if state is older than threshold
     */
    private boolean isStale(DownloadState state) {
        long now = System.currentTimeMillis();
        return (now - state.getLastUpdateTimestamp()) > STALE_THRESHOLD_MS;
    }

    /**
     * Delete the state file
     */
    public void clearStates() {
        File file = new File(STATE_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
