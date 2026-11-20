package com.squarepeace.nnppss.service.download;

import com.squarepeace.nnppss.model.DownloadState;
import com.squarepeace.nnppss.model.Game;

import java.util.Locale;

/**
 * Mutable, thread-safe state of a single download. Network callbacks update it
 * from worker threads; the JavaFX controller only reads snapshots on the FX
 * thread.
 */
public final class DownloadTaskState {

    public enum Status {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    private final Game game;
    private final String destinationPath;
    private final Object lock = new Object();

    private Status status = Status.PENDING;
    private long downloadedBytes;
    private long totalBytes;
    private double speedBytesPerSecond;
    private long etaSeconds = -1;
    private int retryCount;
    private long lastSampleTimeMs;
    private long lastSampleBytes;
    private long lastProgressNotifyMs;

    public DownloadTaskState(Game game, String destinationPath) {
        this.game = game;
        this.destinationPath = destinationPath;
        this.totalBytes = Math.max(0L, game.getFileSize());
    }

    public Game getGame() {
        return game;
    }

    public String getUrl() {
        return game.getPkgUrl();
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized long getDownloadedBytes() {
        return downloadedBytes;
    }

    public synchronized long getTotalBytes() {
        return totalBytes;
    }

    public synchronized double getSpeedBytesPerSecond() {
        return speedBytesPerSecond;
    }

    public synchronized long getEtaSeconds() {
        return etaSeconds;
    }

    public synchronized int getRetryCount() {
        return retryCount;
    }

    public synchronized boolean isPaused() {
        return status == Status.PAUSED;
    }

    public synchronized boolean isActive() {
        return status == Status.PENDING || status == Status.DOWNLOADING || status == Status.PAUSED;
    }

    public synchronized void markStarted() {
        if (status != Status.PAUSED) {
            status = Status.DOWNLOADING;
        }
        lastSampleTimeMs = System.currentTimeMillis();
        lastSampleBytes = downloadedBytes;
    }

    public synchronized void markPaused() {
        if (isActive()) {
            status = Status.PAUSED;
            speedBytesPerSecond = 0;
        }
    }

    public synchronized void markResumed() {
        if (status == Status.PAUSED) {
            status = Status.DOWNLOADING;
            lastSampleTimeMs = System.currentTimeMillis();
            lastSampleBytes = downloadedBytes;
        }
    }

    public synchronized void markCompleted() {
        status = Status.COMPLETED;
        if (totalBytes > 0) {
            downloadedBytes = totalBytes;
        }
        speedBytesPerSecond = 0;
        etaSeconds = 0;
    }

    public synchronized void markFailed() {
        status = Status.FAILED;
        speedBytesPerSecond = 0;
        etaSeconds = -1;
    }

    public synchronized void markCancelled() {
        status = Status.CANCELLED;
        speedBytesPerSecond = 0;
        etaSeconds = -1;
    }

    public synchronized void setRetryCount(int retryCount) {
        this.retryCount = Math.max(0, retryCount);
    }

    /**
     * Updates byte counters, an exponentially smoothed speed and the ETA.
     *
     * @param downloaded bytes downloaded so far
     * @param total      total bytes, or a non-positive value when unknown
     */
    public synchronized void updateProgress(long downloaded, long total) {
        long now = System.currentTimeMillis();
        this.downloadedBytes = Math.max(0L, downloaded);
        if (total > 0) {
            this.totalBytes = total;
        }

        long elapsedMs = now - lastSampleTimeMs;
        if (elapsedMs >= 300) {
            long deltaBytes = this.downloadedBytes - lastSampleBytes;
            if (deltaBytes >= 0) {
                double instant = deltaBytes * 1000.0 / elapsedMs;
                speedBytesPerSecond = speedBytesPerSecond <= 0
                        ? instant
                        : 0.25 * instant + 0.75 * speedBytesPerSecond;
            }
            lastSampleTimeMs = now;
            lastSampleBytes = this.downloadedBytes;
        }

        if (totalBytes > 0 && downloadedBytes >= totalBytes) {
            etaSeconds = 0;
        } else if (totalBytes > 0 && speedBytesPerSecond > 0) {
            etaSeconds = (long) ((totalBytes - downloadedBytes) / speedBytesPerSecond);
        } else {
            etaSeconds = -1;
        }
    }

    /** Rate-limits progress notifications to one every {@code intervalMs}. */
    public synchronized boolean shouldNotifyProgress(long intervalMs) {
        long now = System.currentTimeMillis();
        if (now - lastProgressNotifyMs >= intervalMs) {
            lastProgressNotifyMs = now;
            return true;
        }
        return false;
    }

    /** Snapshot used by the persistence layer. */
    public synchronized DownloadState toDownloadState() {
        DownloadState state = new DownloadState();
        state.setTitle(game.getTitle());
        state.setRegion(game.getRegion());
        state.setConsole(game.getConsole() == null ? null : game.getConsole().getDisplayName());
        state.setPkgUrl(game.getPkgUrl());
        state.setzRif(game.getzRif());
        state.setBytesDownloaded(downloadedBytes);
        state.setTotalBytes(totalBytes);
        state.setFilePath(destinationPath);
        state.setPaused(status == Status.PAUSED);
        state.setStatus(status.name().toLowerCase(Locale.ROOT));
        state.setLastUpdateTimestamp(System.currentTimeMillis());
        return state;
    }

    @Override
    public synchronized String toString() {
        return "DownloadTaskState{" + game.getTitle() + ", " + status
                + ", " + downloadedBytes + "/" + totalBytes + "}";
    }
}
