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
            long fileSize = connection.getContentLengthLong();
            
            // Check if file exists and resume? 
            // The original code checks if file exists and skips bytes.
            long existingFileSize = 0;
            if (destinationFile.exists()) {
                 existingFileSize = destinationFile.length();
                 if (existingFileSize >= fileSize && fileSize > 0) {
                     listener.onComplete(destinationFile);
                     return;
                 }
                 // Resume logic if server supports it, but original code just skipped bytes from stream which is inefficient but let's stick to it or improve it.
                 // Original code: in.skip(bytesDownloaded);
                 // Better: connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
            }

            // Let's try to use Range header for proper resume if supported, otherwise fallback or overwrite.
            // For now, to be safe and consistent with original logic (which was skipping), I will implement the loop.
            
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(destinationFile, false)) { // Overwrite for now as original logic was a bit weird with skip but new FileOutputStream(path) overwrites unless append is true.
                 // Wait, original code: new FileOutputStream(localFilePath) -> overwrites.
                 // But it did: if (file.exists()) { bytesDownloaded = file.length(); in.skip(bytesDownloaded); }
                 // This implies it was trying to resume but writing to a NEW stream (overwriting). This is a BUG in original code. It would write the TAIL of the file to the beginning of the file.
                 // I will FIX this by using append mode if resuming, or just overwrite if not.
                 
                 // Actually, let's just implement a clean download.
                 
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    while (paused.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (listener != null) {
                        listener.onProgress(totalBytesRead, fileSize);
                    }
                }
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
