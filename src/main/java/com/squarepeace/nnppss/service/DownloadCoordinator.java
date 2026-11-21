package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.DownloadHistory;
import com.squarepeace.nnppss.model.DownloadState;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.model.download.Segment;
import com.squarepeace.nnppss.service.download.DownloadClient;
import com.squarepeace.nnppss.service.download.DownloadTaskState;
import com.squarepeace.nnppss.util.PathResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Orchestrates the lifecycle of downloads: queue start, concurrency, pause and
 * resume, cancellation, history, extraction, state persistence and retry of
 * failed downloads. It contains no UI code; the controller subscribes through
 * {@link Listener}.
 *
 * <p>Callbacks are invoked on the calling thread for control methods (usually
 * the JavaFX thread) and on worker threads for progress/completion events. The
 * controller must hop to the FX thread before touching nodes.</p>
 */
public class DownloadCoordinator {
    private static final Logger log = LoggerFactory.getLogger(DownloadCoordinator.class);
    private static final long STATE_SAVE_INTERVAL_MS = 10_000;
    private static final long PROGRESS_NOTIFY_INTERVAL_MS = 200;

    /** Event sink for the UI. All methods are optional. */
    public interface Listener {
        default void onTaskStarted(DownloadTaskState task) {
        }

        default void onTaskProgress(DownloadTaskState task) {
        }

        default void onTaskSegments(DownloadTaskState task, List<Segment> segments) {
        }

        default void onTaskCompleted(DownloadTaskState task, File file) {
        }

        default void onTaskFailed(DownloadTaskState task, Exception error) {
        }

        default void onTaskCancelled(DownloadTaskState task) {
        }

        default void onExtractionFinished(DownloadTaskState task, boolean success) {
        }

        default void onAllFinished() {
        }
    }

    private final ConfigManager configManager;
    private final DownloadClient downloadClient;
    private final PackageExtractor packageExtractor;
    private final DownloadStateManager stateManager;
    private final DownloadHistoryManager historyManager;
    private final SystemValidator systemValidator;
    private final GameRepository gameRepository;

    private final Object taskLock = new Object();
    private final Object historyLock = new Object();
    private final Map<String, DownloadTaskState> tasks = new LinkedHashMap<>();

    private volatile Listener listener = new Listener() {
    };
    private volatile boolean downloading;
    private volatile boolean extractionEnabled = true;
    private volatile boolean shuttingDown;
    private volatile int failedThisSession;
    private ExecutorService executor;
    private volatile long lastStateSaveMs = System.currentTimeMillis();
    private volatile String lastStartError = "";

    public DownloadCoordinator(ConfigManager configManager,
                               DownloadClient downloadClient,
                               PackageExtractor packageExtractor,
                               DownloadStateManager stateManager,
                               DownloadHistoryManager historyManager,
                               SystemValidator systemValidator,
                               GameRepository gameRepository) {
        this.configManager = configManager;
        this.downloadClient = downloadClient;
        this.packageExtractor = packageExtractor;
        this.stateManager = stateManager;
        this.historyManager = historyManager;
        this.systemValidator = systemValidator;
        this.gameRepository = gameRepository;
    }

    public void setListener(Listener listener) {
        this.listener = listener == null ? new Listener() {
        } : listener;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public String getLastStartError() {
        return lastStartError;
    }

    /** Snapshot of the current download rows, in insertion order. */
    public List<DownloadTaskState> getTasks() {
        synchronized (taskLock) {
            return new ArrayList<>(tasks.values());
        }
    }

    public DownloadTaskState getTask(String url) {
        synchronized (taskLock) {
            return tasks.get(url);
        }
    }

    public long getTotalDownloaded() {
        synchronized (taskLock) {
            return tasks.values().stream()
                    .mapToLong(DownloadTaskState::getDownloadedBytes)
                    .sum();
        }
    }

    /**
     * Starts the given games.
     *
     * @param pausedUrls URLs that must start paused (restored session)
     * @return false when a download session is already running, the queue is
     *         empty or there is not enough disk space
     */
    public boolean start(List<Game> games, Set<String> pausedUrls) {
        return start(games, pausedUrls, true);
    }

    /**
     * Starts the given games, optionally skipping extraction for this session.
     */
    public boolean start(List<Game> games, Set<String> pausedUrls, boolean extract) {
        if (games == null || games.isEmpty()) {
            return false;
        }

        List<DownloadTaskState> newTasks = new ArrayList<>();
        synchronized (taskLock) {
            if (downloading) {
                return false;
            }

            long totalBytes = games.stream().mapToLong(Game::getFileSize).sum();
            Path downloadsDirectory = configManager.getDownloadsDirectory();
            try {
                Files.createDirectories(downloadsDirectory);
            } catch (IOException e) {
                lastStartError = "Could not create download directory: " + downloadsDirectory;
                log.error(lastStartError, e);
                return false;
            }
            SystemValidator.ValidationResult validation =
                    systemValidator.validateTotalDiskSpace(
                            totalBytes, downloadsDirectory.toString());
            if (!validation.isValid()) {
                lastStartError = validation.getMessage();
                log.warn("Download rejected: {}", validation.getMessage());
                return false;
            }

            downloading = true;
            extractionEnabled = extract;
            failedThisSession = 0;
            lastStartError = "";
            tasks.clear();

            Set<String> seen = new HashSet<>();
            for (Game game : games) {
                if (game == null || game.getPkgUrl() == null || game.getPkgUrl().isBlank()
                        || !seen.add(game.getPkgUrl())) {
                    continue;
                }
                String destinationPath = downloadsDirectory.resolve(game.getFileName())
                        .toAbsolutePath().normalize().toString();
                DownloadTaskState task = new DownloadTaskState(game, destinationPath);
                tasks.put(task.getUrl(), task);
                newTasks.add(task);
            }

            if (newTasks.isEmpty()) {
                downloading = false;
                return false;
            }

            int threads = Math.max(1, Math.min(4, configManager.getSimultaneousDownloads()));
            executor = Executors.newFixedThreadPool(threads, runnable -> {
                Thread thread = new Thread(runnable, "download-worker");
                thread.setDaemon(true);
                return thread;
            });
        }

        for (DownloadTaskState task : newTasks) {
            listener.onTaskStarted(task);
            executor.submit(() -> runDownload(task));
        }

        if (pausedUrls != null) {
            for (String url : pausedUrls) {
                pause(url);
            }
        }
        saveState();
        return true;
    }

    private void runDownload(DownloadTaskState task) {
        task.markStarted();
        notifyProgress(task, true);
        try {
            downloadClient.downloadFile(task.getUrl(), task.getDestinationPath(),
                    new DownloadService.DownloadListener() {
                        @Override
                        public void onProgress(long bytesDownloaded, long totalBytes) {
                            task.updateProgress(bytesDownloaded, totalBytes);
                            task.setRetryCount(downloadClient.getRetryCount(task.getUrl()));
                            notifyProgress(task, false);
                        }

                        @Override
                        public void onSegmentProgress(List<Segment> segments) {
                            if (task.shouldNotifySegments(PROGRESS_NOTIFY_INTERVAL_MS)) {
                                listener.onTaskSegments(task, segments);
                            }
                        }

                        @Override
                        public void onComplete(File file) {
                            handleCompleted(task, file);
                        }

                        @Override
                        public void onError(Exception e) {
                            handleFailed(task, e);
                        }

                        @Override
                        public void onCancelled() {
                            handleCancelled(task);
                        }
                    });
        } catch (Exception e) {
            handleFailed(task, e);
        }
    }

    private void handleCompleted(DownloadTaskState task, File file) {
        if (shuttingDown) {
            return;
        }
        task.markCompleted();
        addHistory(task, "completed");
        listener.onTaskCompleted(task, file);
        extractIfNeeded(task);
        saveState();
        checkAllFinished();
    }

    private void handleFailed(DownloadTaskState task, Exception error) {
        if (shuttingDown) {
            return;
        }
        task.markFailed();
        synchronized (taskLock) {
            failedThisSession++;
        }
        addHistory(task, "failed");
        listener.onTaskFailed(task, error);
        saveState();
        checkAllFinished();
    }

    /** Number of downloads that failed during the current session. */
    public int getFailedThisSessionCount() {
        return failedThisSession;
    }

    private void handleCancelled(DownloadTaskState task) {
        if (shuttingDown) {
            return;
        }
        task.markCancelled();
        listener.onTaskCancelled(task);
        saveState();
        checkAllFinished();
    }

    private void extractIfNeeded(DownloadTaskState task) {
        if (!extractionEnabled) {
            log.info("Extraction disabled for this session; keeping .pkg for {}",
                    task.getGame().getTitle());
            return;
        }
        Game game = task.getGame();
        if (game == null || task.getDestinationPath() == null) {
            return;
        }

        Path pkgPath = Path.of(task.getDestinationPath());
        Path extractionDirectory = configManager.getExtractionDirectory();
        if (game.getConsole() == Console.PSVITA) {
            if (game.getzRif() == null || game.getzRif().isBlank()) {
                log.info("PSVITA package {} has no zRIF; skipping extraction", game.getTitle());
                return;
            }
            extract(task, pkgPath, extractionDirectory, game.getzRif(), Console.PSVITA);
        } else if (game.getConsole() != null) {
            extract(task, pkgPath, extractionDirectory, null, game.getConsole());
        }
    }

    private void extract(DownloadTaskState task, Path pkgPath, Path extractionDirectory,
                         String zRif, Console console) {
        packageExtractor.extractPackage(pkgPath, extractionDirectory, zRif, console, success -> {
            if (success) {
                historyManager.markExtracted(task.getUrl());
                log.info("Extraction completed for {}", task.getGame().getTitle());
                if (configManager.isAutoCleanupEnabled()) {
                    log.info("Auto-cleanup enabled; deleting package {}", pkgPath);
                    cleanupPackageFile(pkgPath);
                }
            } else {
                log.warn("Extraction failed for {}", task.getGame().getTitle());
            }
            listener.onExtractionFinished(task, success);
        });
    }

    /**
     * Deletes a successfully extracted package. Windows can briefly keep a
     * handle open after pkg2zip exits, so retry instead of silently leaving the
     * .pkg behind.
     */
    private void cleanupPackageFile(Path pkgPath) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                if (Files.deleteIfExists(pkgPath)) {
                    log.info("Auto-cleanup: deleted package file {}", pkgPath);
                } else {
                    log.debug("Auto-cleanup: package file not found: {}", pkgPath);
                }
                return;
            } catch (IOException e) {
                if (attempt == 5) {
                    log.error("Auto-cleanup: could not delete package file {}", pkgPath, e);
                    return;
                }
                try {
                    Thread.sleep(200L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    log.warn("Auto-cleanup interrupted for {}", pkgPath);
                    return;
                }
            }
        }
    }

    public void pause(String url) {
        DownloadTaskState task = getTask(url);
        if (task == null || !task.isActive() || task.isPaused()) {
            return;
        }
        downloadClient.pauseUrl(url);
        task.markPaused();
        listener.onTaskProgress(task);
        saveState();
    }

    public void pauseAll() {
        for (DownloadTaskState task : getTasks()) {
            pause(task.getUrl());
        }
    }

    public void resume(String url) {
        DownloadTaskState task = getTask(url);
        if (task == null || !task.isPaused()) {
            return;
        }
        downloadClient.resumeUrl(url);
        task.markResumed();
        listener.onTaskProgress(task);
        saveState();
    }

    public void resumeAll() {
        for (DownloadTaskState task : getTasks()) {
            resume(task.getUrl());
        }
    }

    public boolean isPaused(String url) {
        DownloadTaskState task = getTask(url);
        return task != null && task.isPaused();
    }

    public boolean anyPaused(Collection<String> urls) {
        for (String url : urls) {
            if (isPaused(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels a download.
     *
     * @param deleteFiles also deletes the partial .pkg and .part.json files
     */
    public void cancel(String url, boolean deleteFiles) {
        DownloadTaskState task = getTask(url);
        if (task == null || !task.isActive()) {
            return;
        }
        downloadClient.cancelUrl(url);
        task.markCancelled();
        if (deleteFiles) {
            deletePartialFiles(task);
        }
        listener.onTaskCancelled(task);
        saveState();
        checkAllFinished();
    }

    private void deletePartialFiles(DownloadTaskState task) {
        File file = PathResolver.getFile(task.getDestinationPath());
        deleteQuietly(file);
        deleteQuietly(new File(file.getAbsolutePath() + ".part.json"));
    }

    private void deleteQuietly(File file) {
        try {
            if (file.exists() && file.delete()) {
                log.info("Deleted partial download file: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Could not delete partial download file: {}", file.getAbsolutePath(), e);
        }
    }

    private void checkAllFinished() {
        boolean hasActive;
        synchronized (taskLock) {
            hasActive = tasks.values().stream().anyMatch(DownloadTaskState::isActive);
        }
        if (hasActive) {
            return;
        }

        synchronized (this) {
            if (!downloading) {
                return;
            }
            downloading = false;
            if (executor != null) {
                executor.shutdown();
            }
        }
        saveState();
        listener.onAllFinished();
    }

    private void notifyProgress(DownloadTaskState task, boolean force) {
        if (force || task.shouldNotifyProgress(PROGRESS_NOTIFY_INTERVAL_MS)) {
            listener.onTaskProgress(task);
        }
        maybeSaveState();
    }

    private void maybeSaveState() {
        if (System.currentTimeMillis() - lastStateSaveMs >= STATE_SAVE_INTERVAL_MS) {
            saveState();
        }
    }

    /** Persists active downloads (pending/downloading/paused). */
    public synchronized void saveState() {
        List<DownloadState> states = new ArrayList<>();
        synchronized (taskLock) {
            for (DownloadTaskState task : tasks.values()) {
                if (task.isActive()) {
                    states.add(task.toDownloadState());
                }
            }
        }
        if (states.isEmpty()) {
            stateManager.clearStates();
        } else {
            stateManager.saveStates(states);
        }
        lastStateSaveMs = System.currentTimeMillis();
    }

    /**
     * Stops the coordinator, saving the current state first. Used on
     * application shutdown; downloads are interrupted, not cancelled.
     */
    public synchronized void shutdown() {
        shuttingDown = true;
        saveState();
        downloading = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void addHistory(DownloadTaskState task, String status) {
        Game game = task.getGame();
        DownloadHistory entry = new DownloadHistory();
        entry.setPkgUrl(task.getUrl());
        entry.setTitle(game.getTitle());
        entry.setConsole(game.getConsole() == null ? null : game.getConsole().getDisplayName());
        entry.setStatus(status);
        entry.setDownloadDate(System.currentTimeMillis());
        entry.setFilePath(task.getDestinationPath());
        synchronized (historyLock) {
            historyManager.addEntry(entry);
        }
    }

    /** True when at least one history entry is in the failed state. */
    public boolean hasFailedDownloads() {
        return getFailedCount() > 0;
    }

    /** Number of failed entries currently stored in the history. */
    public int getFailedCount() {
        synchronized (historyLock) {
            return (int) historyManager.loadHistory().stream()
                    .filter(entry -> "failed".equals(entry.getStatus()))
                    .count();
        }
    }

    /**
     * Converts failed history entries into games and removes them from the
     * history, so they can be queued again without duplicating entries.
     */
    public List<Game> takeFailedGames() {
        synchronized (historyLock) {
            List<DownloadHistory> history = historyManager.loadHistory();
            List<DownloadHistory> failed = history.stream()
                    .filter(entry -> "failed".equals(entry.getStatus()))
                    .collect(Collectors.toList());
            if (failed.isEmpty()) {
                return List.of();
            }

            history.removeIf(entry -> "failed".equals(entry.getStatus()));
            historyManager.saveHistory(history);

            Map<Console, List<Game>> cache = new EnumMap<>(Console.class);
            List<Game> games = new ArrayList<>();
            for (DownloadHistory entry : failed) {
                Console console = parseConsole(entry.getConsole());
                if (console == null || entry.getPkgUrl() == null) {
                    continue;
                }
                Game game = findGame(cache, console, entry.getPkgUrl());
                if (game == null) {
                    game = new Game(entry.getTitle(), null, console, entry.getPkgUrl(), null, 0L);
                }
                games.add(game);
            }
            return games;
        }
    }

    private Console parseConsole(String displayName) {
        if (displayName == null) {
            return null;
        }
        try {
            return Console.fromDisplayName(displayName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown console in history: {}", displayName);
            return null;
        }
    }

    private Game findGame(Map<Console, List<Game>> cache, Console console, String pkgUrl) {
        List<Game> games = cache.computeIfAbsent(console, key -> {
            try {
                return gameRepository.loadGames(key);
            } catch (Exception e) {
                log.warn("Could not load games for {} while looking up a failed download", key, e);
                return List.of();
            }
        });
        return games.stream()
                .filter(game -> pkgUrl.equals(game.getPkgUrl()))
                .findFirst()
                .orElse(null);
    }
}
