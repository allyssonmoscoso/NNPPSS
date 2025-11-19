package com.squarepeace.nnppss.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadService {

    private final AtomicBoolean paused = new AtomicBoolean(false);

    public interface DownloadListener {
        void onProgress(long bytesDownloaded, long totalBytes);
        void onComplete(File file);
        void onError(Exception e);
    }

    public void setPaused(boolean paused) {
        this.paused.set(paused);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void downloadFile(String fileURL, String destinationPath, DownloadListener listener) {
        File destinationFile = new File(destinationPath);
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
            URL url = new URL(fileURL);
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

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(destinationFile, partial)) {
                byte[] buf = new byte[64 * 1024];
                int read;
                long downloaded = partial ? existingFileSize : 0L;
                while ((read = in.read(buf)) != -1) {
                    while (paused.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    out.write(buf, 0, read);
                    downloaded += read;
                    if (listener != null) {
                        listener.onProgress(downloaded, totalSize);
                    }
                }
            } finally {
                connection.disconnect();
            }

            if (listener != null) {
                listener.onComplete(destinationFile);
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
