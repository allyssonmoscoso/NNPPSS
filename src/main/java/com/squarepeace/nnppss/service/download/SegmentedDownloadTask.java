package com.squarepeace.nnppss.service.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.model.download.DownloadMetadata;
import com.squarepeace.nnppss.model.download.Segment;
import com.squarepeace.nnppss.model.download.SegmentStatus;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DownloadService.DownloadListener;

/**
 * Orchestrates the segmented download of a single file.
 */
public class SegmentedDownloadTask implements Callable<File> {
    private static final Logger log = LoggerFactory.getLogger(SegmentedDownloadTask.class);
    private static final int DEFAULT_SEGMENT_COUNT = 8;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES_PER_SEGMENT = 5;

    private final DownloadMetadata metadata;
    private final DownloadListener listener;
    private final ConfigManager configManager;
    private final SegmentStateManager stateManager;
    private final ExecutorService executor; // Provided by service to limit global threads
    
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicLong totalBytesDownloaded = new AtomicLong(0);

    public SegmentedDownloadTask(DownloadMetadata metadata, DownloadListener listener, 
                                 ConfigManager configManager, SegmentStateManager stateManager,
                                 ExecutorService executor) {
        this.metadata = metadata;
        this.listener = listener;
        this.configManager = configManager;
        this.stateManager = stateManager;
        this.executor = executor;
    }

    @Override
    public File call() throws Exception {
        File destinationFile = new File(metadata.getDestinationPath());

        // Phase 3: Segmentation logic
        initializeSegments(destinationFile);

        // Pre-allocate file if needed (Phase 6)
        try (RandomAccessFile raf = new RandomAccessFile(destinationFile, "rw")) {
            if (raf.length() != metadata.getTotalSize()) {
                raf.setLength(metadata.getTotalSize());
            }
        }

        // Calculate initial progress
        long initialDownloaded = metadata.getTotalBytesDownloaded();
        totalBytesDownloaded.set(initialDownloaded);
        listener.onProgress(initialDownloaded, metadata.getTotalSize());

        // Phase 4 & 5: Concurrency and Segment Download
        // Main loop to handle pause/resume
        while (!metadata.isCompleted() && !cancelled.get()) {
            
            if (paused.get()) {
                stateManager.saveState(metadata);
                // Blocking wait for resume
                while (paused.get() && !cancelled.get()) {
                    Thread.sleep(500);
                }
                if (cancelled.get()) break;
                // Resumed: continue to spawn workers
            }

            List<Future<Boolean>> futures = new ArrayList<>();
            List<SegmentWorker> workers = new ArrayList<>();

            long incompleteCount = metadata.getSegments().stream()
                    .filter(s -> s.getStatus() != SegmentStatus.COMPLETED)
                    .count();
            
            if (incompleteCount == 0) {
                metadata.setCompleted(true);
                break;
            }

            for (Segment segment : metadata.getSegments()) {
                if (segment.getStatus() != SegmentStatus.COMPLETED) {
                    SegmentWorker worker = new SegmentWorker(segment, destinationFile);
                    workers.add(worker);
                    futures.add(executor.submit(worker));
                }
            }

            // Monitor workers
            boolean batchSuccess = true;
            for (Future<Boolean> future : futures) {
                try {
                    if (!future.get()) {
                        batchSuccess = false;
                    }
                } catch (ExecutionException e) {
                    batchSuccess = false;
                    log.error("Segment execution failed", e);
                }
            }

            if (cancelled.get()) break;
            
            if (paused.get()) {
                // Workers exited because of pause. Loop back to handle pause state.
                continue;
            }

            if (!batchSuccess) {
                // If not paused and not cancelled, and workers failed -> Error
                stateManager.saveState(metadata);
                throw new IOException("Download failed for some segments. Check logs.");
            }
            
            // Check if all done
            if (metadata.getSegments().stream().allMatch(s -> s.getStatus() == SegmentStatus.COMPLETED)) {
                metadata.setCompleted(true);
            }
        }

        if (cancelled.get()) {
            listener.onCancelled();
            return null;
        }
        
        // Success
        stateManager.clearState(metadata.getDestinationPath());
        metadata.setCompleted(true);
        listener.onComplete(destinationFile);
        return destinationFile;
    }

    private void initializeSegments(File destinationFile) {
        if (metadata.getSegments() == null || metadata.getSegments().isEmpty()) {
            // First time logic
            long totalSize = metadata.getTotalSize();
            int segmentCount = DEFAULT_SEGMENT_COUNT;
            
            // Adjust segment count based on size (simple heuristic)
            if (totalSize < 10 * 1024 * 1024) { // < 10MB
                segmentCount = 1;
            } else if (totalSize < 50 * 1024 * 1024) { // < 50MB
                segmentCount = 4;
            }

            long segmentSize = totalSize / segmentCount;
            List<Segment> segments = new ArrayList<>();
            for (int i = 0; i < segmentCount; i++) {
                long start = i * segmentSize;
                long end = (i == segmentCount - 1) ? totalSize - 1 : (start + segmentSize - 1);
                segments.add(new Segment(i, start, end));
            }
            metadata.setSegments(segments);
            stateManager.saveState(metadata);
        } else {
             // Resume logic: Validate if file still matches expected size?
             // Should ideally verify ETag if possible, but ResourceAnalyzer did that phase.
        }
    }
    
    public void pause() {
        paused.set(true);
        // Workers check this flag
    }

    public void resume() {
        paused.set(false);
    }

    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Inner class to handle individual segment download independent of others.
     */
    private class SegmentWorker implements Callable<Boolean> {
        private final Segment segment;
        private final File file;
        private volatile boolean workerCancelled = false;

        public SegmentWorker(Segment segment, File file) {
            this.segment = segment;
            this.file = file;
        }
        
        public void cancel() {
            workerCancelled = true;
        }

        @Override
        public Boolean call() {
            if (segment.getStatus() == SegmentStatus.COMPLETED) return true;

            segment.setStatus(SegmentStatus.DOWNLOADING);
            int retries = 0;

            while (retries < MAX_RETRIES_PER_SEGMENT && !workerCancelled && !cancelled.get() && !paused.get()) {
                try {
                    downloadSegment();
                    if (segment.isFinished()) {
                        segment.setStatus(SegmentStatus.COMPLETED);
                        return true;
                    }
                } catch (IOException e) {
                    retries++;
                    segment.incrementRetryCount();
                    log.warn("Segment {} failed (attempt {}): {}", segment.getId(), retries, e.getMessage());
                    try {
                        Thread.sleep(1000L * retries); // Exponential backoffish
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            
            if (paused.get()) {
                segment.setStatus(SegmentStatus.PENDING); // Mark as pending for resume
            } else if (!segment.isFinished()) {
                segment.setStatus(SegmentStatus.FAILED);
            }
            
            return segment.getStatus() == SegmentStatus.COMPLETED;
        }

        private void downloadSegment() throws IOException {
            long start = segment.getCurrentOffset();
            long end = segment.getEndByte();
            if (start > end) return; // Already done

            URL url = new URL(metadata.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_PARTIAL && status != HttpURLConnection.HTTP_OK) {
                 // Fallback? If server returns 200 OK instead of 206, it means it DOESN'T support ranges properly for this request
                 // or it ignored it. If it returns 200, it's sending the whole file. Determine what to do.
                 // For safety, fail this segment logic if we expect partial.
                 throw new IOException("Server did not return partial content: " + status);
            }

            try (InputStream in = connection.getInputStream();
                 RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                
                raf.seek(start);
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (workerCancelled || cancelled.get() || paused.get()) {
                        break;
                    }

                    raf.write(buffer, 0, bytesRead);
                    segment.setCurrentOffset(segment.getCurrentOffset() + bytesRead);
                    
                    // Atomic update of global progress and notification
                    long total = totalBytesDownloaded.addAndGet(bytesRead);
                    
                    // Update listeners
                    // We should probably throttle this if performance is an issue, but standard IDM updates fast.
                    listener.onProgress(total, metadata.getTotalSize());
                    listener.onSegmentProgress(metadata.getSegments());
                    
                    // Synchronization/Speed Limit logic could go here (Phase 8)
                    applySpeedLimit(bytesRead);
                }
            }
        }
        
        private void applySpeedLimit(int bytesRead) {
            // Simple placeholder for speed limiting
            int limitKB = configManager.getDownloadSpeedLimit();
            if (limitKB > 0) {
                // Determine fair share for this thread?
                // Or use a global token bucket. 
                // Given the existing architecture, we might just sleep a bit.
                // Current simple logic:
                // Global limit is tricky without a coordinator.
                // Let's rely on the fact that the OS/Network stack balances connections.
                // If we want accurate limiting, we need a shared limiter passed to the task.
                // Leaving simple sleep for now for compatibility with legacy style if needed,
                // but IDM usually tries to go fast.
            }
        }
    }
}
