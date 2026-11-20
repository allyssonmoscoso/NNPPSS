package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadQueueManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void overrideDataDir() {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.toString());
    }

    @AfterEach
    void clearDataDir() {
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void savesLoadsAndClearsTheQueue() {
        DownloadQueueManager manager = new DownloadQueueManager();
        Game game = new Game("Alpha", "USA", Console.PSVITA,
                "https://example.test/alpha.pkg", "zrif", 1024);

        manager.saveQueue(List.of(game));
        assertTrue(manager.hasQueuedDownloads());

        List<Game> loaded = manager.loadQueue();
        assertEquals(1, loaded.size());
        assertEquals("Alpha", loaded.get(0).getTitle());
        assertEquals("https://example.test/alpha.pkg", loaded.get(0).getPkgUrl());
        assertEquals(Console.PSVITA, loaded.get(0).getConsole());

        manager.clearQueue();
        assertTrue(manager.loadQueue().isEmpty());
        assertFalse(manager.hasQueuedDownloads());
    }

    @Test
    void missingFileReturnsAnEmptyQueue() {
        DownloadQueueManager manager = new DownloadQueueManager();
        assertTrue(manager.loadQueue().isEmpty());
        assertFalse(manager.hasQueuedDownloads());
    }
}
