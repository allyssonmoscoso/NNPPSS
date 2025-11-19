package com.squarepeace.nnppss.service;

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

    private final AtomicBoolean paused = new AtomicBoolean(false); // legacy global pause
    private final ConcurrentMap<String, AtomicBoolean> pausedByUrl = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicBoolean> cancelledByUrl = new ConcurrentHashMap<>();

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
        File destinationFile = new File(destinationPath);
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
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
                while ((read = in.read(buf)) != -1) {
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
            if (listener != null) {
                listener.onError(e);
            }
        }
    }
    
    public long getFileSize(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        return conn.getContentLengthLong();
    }
}
