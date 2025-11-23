package com.squarepeace.nnppss.service;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.util.PathResolver;

/**
 * Manages database files for game consoles.
 * Handles checking availability and downloading database files.
 */
public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_FOLDER = "db";
    
    private final ConfigManager configManager;
    private final DownloadService downloadService;
    private final Map<Console, Boolean> consoleAvailability = new HashMap<>();
    private final List<DatabaseListener> listeners = new ArrayList<>();
    
    public interface DatabaseListener {
        void onAvailabilityChanged(Console console, boolean available);
        void onDownloadComplete(Console console, boolean success);
        void onDownloadProgress(Console console, String message);
    }
    
    public DatabaseManager(ConfigManager configManager, DownloadService downloadService) {
        this.configManager = configManager;
        this.downloadService = downloadService;
        for (Console console : Console.values()) {
            consoleAvailability.put(console, false);
        }
    }
    
    public void addListener(DatabaseListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(DatabaseListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Check availability of all consoles asynchronously
     */
    public CompletableFuture<Void> checkAllConsolesAvailability() {
        log.info("Checking availability for all consoles");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Console console : Console.values()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                boolean available = checkConsoleAvailability(console);
                consoleAvailability.put(console, available);
                notifyAvailabilityChanged(console, available);
            });
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Check if a console database is available (exists locally)
     */
    private boolean checkConsoleAvailability(Console console) {
        String dbFileName = getDbFileName(console);
        File dbFile = new File(PathResolver.getFile(DB_FOLDER), dbFileName);
        
        // Only return true if file exists locally
        if (dbFile.exists() && dbFile.length() > 0) {
            log.debug("Database file exists locally: {}", dbFileName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a URL is accessible
     */
    private boolean isUrlAccessible(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            log.debug("URL not accessible: {}", urlString, e);
            return false;
        }
    }
    
    /**
     * Check if a specific console is available
     */
    public boolean isConsoleAvailable(Console console) {
        return consoleAvailability.getOrDefault(console, false);
    }
    
    /**
     * Download database for a specific console
     */
    public CompletableFuture<Boolean> downloadDatabase(Console console) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String url = getConsoleUrl(console);
        
        if (url == null || url.trim().isEmpty()) {
            log.warn("No URL configured for console: {}. Please configure it in Settings.", console);
            future.complete(false);
            return future;
        }
        
        String dbFileName = getDbFileName(console);
        PathResolver.ensureDirectory(DB_FOLDER);
        
        String destPath = PathResolver.getFile(DB_FOLDER).getAbsolutePath() + File.separator + dbFileName;
        notifyDownloadProgress(console, "Starting download for " + console.getDisplayName());
        log.info("Downloading database for {}: {} -> {}", console, url, destPath);
        
        downloadService.downloadFile(url, destPath, new DownloadService.DownloadListener() {
            @Override
            public void onProgress(long bytesDownloaded, long totalBytes) {
                if (totalBytes > 0) {
                    int percent = (int) ((bytesDownloaded * 100) / totalBytes);
                    notifyDownloadProgress(console, 
                        String.format("Downloading %s: %d%%", console.getDisplayName(), percent));
                }
            }
            
            @Override
            public void onComplete(File file) {
                log.info("Database download completed for {}: {}", console, file.getAbsolutePath());
                consoleAvailability.put(console, true);
                notifyAvailabilityChanged(console, true);
                notifyDownloadComplete(console, true);
                future.complete(true);
            }
            
            @Override
            public void onCancelled() {
                log.warn("Database download cancelled for {}", console);
                notifyDownloadComplete(console, false);
                future.complete(false);
            }
            
            @Override
            public void onError(Exception e) {
                log.error("Database download failed for {}", console, e);
                notifyDownloadComplete(console, false);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Download databases for all unavailable consoles
     */
    public CompletableFuture<Map<Console, Boolean>> downloadAllDatabases() {
        log.info("Starting download for all missing databases");
        Map<Console, CompletableFuture<Boolean>> futures = new HashMap<>();
        
        for (Console console : Console.values()) {
            if (!isConsoleAvailable(console)) {
                futures.put(console, downloadDatabase(console));
            }
        }
        
        if (futures.isEmpty()) {
            log.info("All databases are already available");
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.values().toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> {
            Map<Console, Boolean> results = new HashMap<>();
            futures.forEach((console, future) -> {
                try {
                    results.put(console, future.join());
                } catch (Exception e) {
                    log.error("Error getting download result for {}", console, e);
                    results.put(console, false);
                }
            });
            return results;
        });
    }
    
    private String getConsoleUrl(Console console) {
        switch (console) {
            case PSP: return configManager.getPspUrl();
            case PSVITA: return configManager.getPsVitaUrl();
            case PSX: return configManager.getPsxUrl();
            default: return null;
        }
    }
    
    private String getDbFileName(Console console) {
        switch (console) {
            case PSP: return "PSP_GAMES.tsv";
            case PSVITA: return "PSV_GAMES.tsv";
            case PSX: return "PSX_GAMES.tsv";
            default: return null;
        }
    }
    
    private void notifyAvailabilityChanged(Console console, boolean available) {
        log.debug("Console availability changed: {} = {}", console, available);
        for (DatabaseListener listener : listeners) {
            try {
                listener.onAvailabilityChanged(console, available);
            } catch (Exception e) {
                log.error("Error notifying listener of availability change", e);
            }
        }
    }
    
    private void notifyDownloadComplete(Console console, boolean success) {
        log.debug("Database download complete: {} = {}", console, success);
        for (DatabaseListener listener : listeners) {
            try {
                listener.onDownloadComplete(console, success);
            } catch (Exception e) {
                log.error("Error notifying listener of download completion", e);
            }
        }
    }
    
    private void notifyDownloadProgress(Console console, String message) {
        for (DatabaseListener listener : listeners) {
            try {
                listener.onDownloadProgress(console, message);
            } catch (Exception e) {
                log.error("Error notifying listener of download progress", e);
            }
        }
    }
}
