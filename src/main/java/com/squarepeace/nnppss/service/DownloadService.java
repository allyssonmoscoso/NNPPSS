package com.squarepeace.nnppss.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadService {
    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds

    private final AtomicBoolean paused = new AtomicBoolean(false); // legacy global pause
    private final ConcurrentMap<String, AtomicBoolean> pausedByUrl = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicBoolean> cancelledByUrl = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> retryCountByUrl = new ConcurrentHashMap<>();
    private final ConfigManager configManager;

    public DownloadService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Legacy constructor for backward compatibility if needed, 
     * but we should update callers to pass ConfigManager.
     * For now, we can create a default one or throw.
     * Better to update callers.
     */
    public DownloadService() {
        this(new ConfigManager());
    }

    public interface DownloadListener {
        void onProgress(long bytesDownloaded, long totalBytes);
        void onComplete(File file);
        void onError(Exception e);
        default void onCancelled() {}
    }

    public void setPaused(boolean paused) { // legacy global
        this.paused.set(paused);
    }

    public boolean isPaused() { // legacy global
        return paused.get();
    }

    public void pauseUrl(String url) {
        pausedByUrl.computeIfAbsent(url, k -> new AtomicBoolean(false)).set(true);
    }

    public void resumeUrl(String url) {
        AtomicBoolean flag = pausedByUrl.get(url);
        if (flag != null) flag.set(false);
    }

    public boolean isPaused(String url) {
        AtomicBoolean flag = pausedByUrl.get(url);
        return flag != null && flag.get();
    }

    public void cancelUrl(String url) {
        cancelledByUrl.computeIfAbsent(url, k -> new AtomicBoolean(false)).set(true);
    }

    public boolean isCancelled(String url) {
        AtomicBoolean flag = cancelledByUrl.get(url);
        return flag != null && flag.get();
    }

    public void downloadFile(String fileURL, String destinationPath, DownloadListener listener) {
        retryCountByUrl.put(fileURL, 0);
        downloadFileWithRetry(fileURL, destinationPath, listener, 0);
    }

    private void downloadFileWithRetry(String fileURL, String destinationPath, DownloadListener listener, int attempt) {
        File destinationFile = new File(destinationPath);
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
            log.info("Starting download: {} (attempt {}/{})", fileURL, attempt + 1, MAX_RETRIES + 1);
            URL url = new URL(fileURL);
            pausedByUrl.computeIfAbsent(fileURL, k -> new AtomicBoolean(false));
            cancelledByUrl.computeIfAbsent(fileURL, k -> new AtomicBoolean(false));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            long existingFileSize = destinationFile.exists() ? destinationFile.length() : 0L;
            if (existingFileSize > 0) {
                connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
            }

            int status = connection.getResponseCode();
            // Handle basic redirects (already enabled) and errors
            if (status >= 400) {
                throw new IOException("HTTP error: " + status);
            }

            long reportedLength = connection.getContentLengthLong();
            boolean partial = status == HttpURLConnection.HTTP_PARTIAL;
            long totalSize = (reportedLength > 0)
                    ? (partial ? existingFileSize + reportedLength : reportedLength)
                    : -1L;

            boolean wasCancelled = false;
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(destinationFile, partial)) {
                byte[] buf = new byte[64 * 1024];
                int read;
                long downloaded = partial ? existingFileSize : 0L;
                while (true) {
                    long startChunkTime = System.currentTimeMillis();
                    read = in.read(buf);
                    if (read == -1) break;
                    
                    if (isCancelled(fileURL)) { wasCancelled = true; break; }
                    while (paused.get() || isPaused(fileURL)) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    if (isCancelled(fileURL)) { wasCancelled = true; break; }
                    out.write(buf, 0, read);
                    
                    // Speed Limiting
                    int speedLimitKBps = configManager.getDownloadSpeedLimit();
                    if (speedLimitKBps > 0) {
                        long expectedTimeMs = (read * 1000L) / (speedLimitKBps * 1024L);
                        long actualTimeMs = System.currentTimeMillis() - startChunkTime;
                        if (actualTimeMs < expectedTimeMs) {
                            try {
                                Thread.sleep(expectedTimeMs - actualTimeMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }

                    downloaded += read;
                    if (listener != null) {
                        listener.onProgress(downloaded, totalSize);
                    }
                }
            } finally {
                connection.disconnect();
                boolean cancelled = isCancelled(fileURL);
                pausedByUrl.remove(fileURL);
                cancelledByUrl.remove(fileURL);
                if (cancelled && destinationFile.exists()) {
                    // best-effort delete of partial file
                    try { destinationFile.delete(); } catch (Exception ignore) {}
                }
            }

            if (wasCancelled) {
                if (listener != null) {
                    try { listener.onCancelled(); } catch (Throwable t) { /* ignore */ }
                }
                return;
            } else {
                if (listener != null) {
                    listener.onComplete(destinationFile);
                }
            }

        } catch (IOException e) {
            log.error("Download failed for URL: {} (attempt {}/{})", fileURL, attempt + 1, MAX_RETRIES + 1, e);
            
            // Retry logic with exponential backoff
            if (attempt < MAX_RETRIES && !isCancelled(fileURL)) {
                long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
                log.info("Retrying download in {}ms...", delay);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted for URL: {}", fileURL);
                    if (listener != null) {
                        listener.onError(e);
                    }
                    return;
                }
                
                // Retry the download
                retryCountByUrl.put(fileURL, attempt + 1);
                downloadFileWithRetry(fileURL, destinationPath, listener, attempt + 1);
            } else {
                // Max retries exceeded or cancelled
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries exceeded for URL: {}", fileURL);
                }
                retryCountByUrl.remove(fileURL);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        } finally {
            retryCountByUrl.remove(fileURL);
        }
    }
    
    public long getFileSize(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        return conn.getContentLengthLong();
    }
}
