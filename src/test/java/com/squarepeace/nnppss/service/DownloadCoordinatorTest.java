package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.DownloadHistory;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.service.download.DownloadClient;
import com.squarepeace.nnppss.service.download.DownloadTaskState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadCoordinatorTest {

    @TempDir
    Path tempDir;

    private ConfigManager configManager;
    private FakeDownloadClient downloadClient;
    private FakePackageExtractor packageExtractor;
    private DownloadStateManager stateManager;
    private DownloadHistoryManager historyManager;
    private RecordingListener listener;
    private FakeSystemValidator validator;
    private DownloadCoordinator coordinator;

    @BeforeEach
    void setUp() {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.toString());
        configManager = new ConfigManager();
        configManager.setProperty("simultaneousDownloads", "1");

        downloadClient = new FakeDownloadClient();
        packageExtractor = new FakePackageExtractor();
        stateManager = new DownloadStateManager();
        historyManager = new DownloadHistoryManager();
        listener = new RecordingListener();
        validator = new FakeSystemValidator(configManager, true);

        GameRepository repository = new GameRepository() {
            @Override
            public List<Game> loadGames(Console console) {
                return List.of(game("Lookup", "https://example.test/lookup.pkg", 2048));
            }
        };

        coordinator = new DownloadCoordinator(configManager, downloadClient, packageExtractor,
                stateManager, historyManager, validator, repository);
        coordinator.setListener(listener);
    }

    @AfterEach
    void tearDown() {
        coordinator.shutdown();
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void startsReportsProgressAndCompletes() throws Exception {
        Game game = game("Alpha", "https://example.test/alpha.pkg", 1000);

        assertTrue(coordinator.start(List.of(game), Set.of()));
        assertTrue(coordinator.isDownloading());
        assertEquals(1, listener.started.size());
        assertEquals(1, stateManager.loadStates().size(), "state must be saved on start");

        DownloadService.DownloadListener clientListener = downloadClient.await(game.getPkgUrl());
        clientListener.onProgress(500, 1000);

        assertEquals(500, coordinator.getTotalDownloaded());
        assertTrue(listener.progressCount.get() > 0);

        File file = tempDir.resolve("alpha.pkg").toFile();
        clientListener.onComplete(file);

        DownloadTaskState task = coordinator.getTask(game.getPkgUrl());
        assertEquals(DownloadTaskState.Status.COMPLETED, task.getStatus());
        assertEquals(1, listener.completed.size());
        assertEquals(1, listener.allFinished.get());
        assertFalse(coordinator.isDownloading());
        assertTrue(packageExtractor.calls > 0, "PSVITA downloads must be extracted");
        assertEquals("zrif", packageExtractor.lastZRif);
        assertTrue(stateManager.loadStates().isEmpty(), "state must be cleared after completion");

        List<DownloadHistory> history = historyManager.loadHistory();
        assertEquals(1, history.size());
        assertEquals("completed", history.get(0).getStatus());
    }

    @Test
    void pauseResumeAndCancelKeepStateConsistent() throws Exception {
        Game game = game("Beta", "https://example.test/beta.pkg", 4096);
        assertTrue(coordinator.start(List.of(game), Set.of()));
        DownloadService.DownloadListener clientListener = downloadClient.await(game.getPkgUrl());

        coordinator.pause(game.getPkgUrl());
        assertTrue(downloadClient.paused.contains(game.getPkgUrl()));
        assertTrue(coordinator.getTask(game.getPkgUrl()).isPaused());
        assertEquals(1, stateManager.loadStates().size());
        assertTrue(stateManager.loadStates().get(0).isPaused());

        coordinator.resume(game.getPkgUrl());
        assertFalse(downloadClient.paused.contains(game.getPkgUrl()));
        assertFalse(coordinator.getTask(game.getPkgUrl()).isPaused());

        Path packageDir = tempDir.resolve("packages");
        Files.createDirectories(packageDir);
        Path partial = packageDir.resolve("beta.pkg");
        Path partState = packageDir.resolve("beta.pkg.part.json");
        Files.writeString(partial, "partial");
        Files.writeString(partState, "{}");

        coordinator.cancel(game.getPkgUrl(), true);

        assertTrue(downloadClient.cancelled.contains(game.getPkgUrl()));
        assertEquals(DownloadTaskState.Status.CANCELLED,
                coordinator.getTask(game.getPkgUrl()).getStatus());
        assertFalse(Files.exists(partial), "partial package must be deleted");
        assertFalse(Files.exists(partState), "segment state must be deleted");
        assertEquals(1, listener.allFinished.get());
        assertFalse(coordinator.isDownloading());

        // The cancelled task is terminal, so the listener clean-up is harmless.
        clientListener.onProgress(1, 10);
    }

    @Test
    void rejectsStartWhenDiskSpaceValidationFails() {
        validator.valid = false;
        validator.message = "not enough space";

        assertFalse(coordinator.start(List.of(game("Gamma", "https://example.test/gamma.pkg", 10)), Set.of()));

        assertFalse(coordinator.isDownloading());
        assertTrue(listener.started.isEmpty());
        assertEquals("not enough space", coordinator.getLastStartError());
    }

    @Test
    void takeFailedGamesUsesRepositoryLookupAndClearsHistory() throws IOException {
        DownloadHistory failed = new DownloadHistory();
        failed.setPkgUrl("https://example.test/lookup.pkg");
        failed.setTitle("Lookup");
        failed.setConsole(Console.PSVITA.getDisplayName());
        failed.setStatus("failed");
        failed.setDownloadDate(System.currentTimeMillis());
        historyManager.addEntry(failed);

        List<Game> games = coordinator.takeFailedGames();

        assertEquals(1, games.size());
        assertEquals("Lookup", games.get(0).getTitle());
        assertTrue(historyManager.loadHistory().isEmpty());
        assertFalse(coordinator.hasFailedDownloads());
    }

    @Test
    void stateSnapshotUsesHistoryCompatibleStatusNames() throws Exception {
        Game game = game("Delta", "https://example.test/delta.pkg", 2048);
        assertTrue(coordinator.start(List.of(game), Set.of()));
        downloadClient.await(game.getPkgUrl());
        coordinator.saveState();

        var saved = stateManager.loadStates();
        assertEquals(1, saved.size());
        assertEquals("downloading", saved.get(0).getStatus());
        assertNotNull(saved.get(0).getFilePath());
    }

    @Test
    void throttlesSegmentNotificationsFromTheDownloadWorker() throws Exception {
        Game game = game("Zeta", "https://example.test/zeta.pkg", 2048);
        assertTrue(coordinator.start(List.of(game), Set.of()));
        DownloadService.DownloadListener clientListener = downloadClient.await(game.getPkgUrl());

        for (int i = 0; i < 20; i++) {
            clientListener.onSegmentProgress(List.of());
        }

        assertEquals(1, listener.segmentCount.get(),
                "only the first segment update should reach the UI within the throttle window");
    }

    @Test
    void shutdownKeepsActiveStateForTheNextSession() throws Exception {
        Game game = game("Epsilon", "https://example.test/epsilon.pkg", 4096);
        assertTrue(coordinator.start(List.of(game), Set.of()));
        downloadClient.await(game.getPkgUrl());

        coordinator.shutdown();

        var saved = stateManager.loadStates();
        assertEquals(1, saved.size());
        assertTrue(saved.get(0).getStatus().equals("downloading")
                || saved.get(0).getStatus().equals("pending"));
        assertFalse(coordinator.isDownloading());
    }

    private static Game game(String title, String url, long size) {
        return new Game(title, "USA", Console.PSVITA, url, "zrif", size);
    }

    private static final class FakeSystemValidator extends SystemValidator {
        private boolean valid;
        private String message;

        private FakeSystemValidator(ConfigManager configManager, boolean valid) {
            super(configManager);
            this.valid = valid;
            this.message = valid ? "ok" : "invalid";
        }

        @Override
        public ValidationResult validateTotalDiskSpace(long totalBytes, String destinationPath) {
            return new ValidationResult(valid, message);
        }
    }

    private static final class FakeDownloadClient implements DownloadClient {
        private final Map<String, DownloadService.DownloadListener> listeners = new HashMap<>();
        private final Set<String> paused = new HashSet<>();
        private final Set<String> cancelled = new HashSet<>();

        private DownloadService.DownloadListener await(String url) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline) {
                DownloadService.DownloadListener listener = listeners.get(url);
                if (listener != null) {
                    return listener;
                }
                Thread.sleep(10);
            }
            throw new AssertionError("downloadFile was not called for " + url);
        }

        @Override
        public void downloadFile(String fileURL, String destinationPath,
                                 DownloadService.DownloadListener listener) {
            listeners.put(fileURL, listener);
        }

        @Override
        public void pauseUrl(String url) {
            paused.add(url);
        }

        @Override
        public void resumeUrl(String url) {
            paused.remove(url);
        }

        @Override
        public boolean isPaused(String url) {
            return paused.contains(url);
        }

        @Override
        public void cancelUrl(String url) {
            cancelled.add(url);
        }

        @Override
        public boolean isCancelled(String url) {
            return cancelled.contains(url);
        }

        @Override
        public int getRetryCount(String url) {
            return 0;
        }
    }

    private static final class FakePackageExtractor implements PackageExtractor {
        private int calls;
        private String lastZRif;

        @Override
        public void extractPackage(String pkgName, String zRifKey, Console console,
                                   PackageService.ExtractionListener listener) {
            calls++;
            lastZRif = zRifKey;
            if (listener != null) {
                listener.onExtractionComplete(true);
            }
        }
    }

    private static final class RecordingListener implements DownloadCoordinator.Listener {
        private final List<DownloadTaskState> started = new ArrayList<>();
        private final List<DownloadTaskState> completed = new ArrayList<>();
        private final java.util.concurrent.atomic.AtomicInteger progressCount =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger segmentCount =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger allFinished =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public void onTaskStarted(DownloadTaskState task) {
            started.add(task);
        }

        @Override
        public void onTaskProgress(DownloadTaskState task) {
            progressCount.incrementAndGet();
        }

        @Override
        public void onTaskSegments(DownloadTaskState task, List<com.squarepeace.nnppss.model.download.Segment> segments) {
            segmentCount.incrementAndGet();
        }

        @Override
        public void onTaskCompleted(DownloadTaskState task, File file) {
            completed.add(task);
        }

        @Override
        public void onAllFinished() {
            allFinished.incrementAndGet();
        }
    }
}
