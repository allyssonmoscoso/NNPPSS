package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.model.DownloadHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageCleanupServiceTest {

    @TempDir
    Path tempDir;

    private Path downloads;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.resolve("data").toString());
        downloads = tempDir.resolve("downloads");
        Files.createDirectories(downloads);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void selectsOnlyUnextractedPkgFiles() throws Exception {
        Path extracted = Files.writeString(downloads.resolve("extracted.pkg"), "done");
        Path pending = Files.writeString(downloads.resolve("pending.pkg"), "pending-data");
        Path unrelated = Files.writeString(downloads.resolve("notes.txt"), "ignore");

        DownloadHistoryManager history = new DownloadHistoryManager();
        DownloadHistory entry = new DownloadHistory();
        entry.setPkgUrl("https://example.test/extracted.pkg");
        entry.setFilePath(extracted.toString());
        entry.setStatus("completed");
        entry.setExtracted(true);
        history.addEntry(entry);

        StorageCleanupService service = new StorageCleanupService();
        StorageCleanupService.ScanResult result = service.scan(downloads, Set.of(), history);

        assertEquals(1, result.candidates().size());
        assertTrue(result.candidates().get(0).path().endsWith("pending.pkg"));
        assertEquals("pending-data".length(), result.totalBytes());

        long freed = service.delete(result.candidates());
        assertFalse(Files.exists(pending));
        assertTrue(Files.exists(extracted));
        assertTrue(Files.exists(unrelated));
        assertEquals("pending-data".length(), freed);
    }

    @Test
    void skipsFilesThatAreActiveOrExtractedByHistory() throws Exception {
        Path active = Files.writeString(downloads.resolve("active.pkg"), "1234");
        Path pending = Files.writeString(downloads.resolve("pending.pkg"), "12");

        DownloadHistoryManager history = new DownloadHistoryManager();
        StorageCleanupService.ScanResult result = new StorageCleanupService()
                .scan(downloads, Set.of(active.toAbsolutePath().normalize()), history);

        assertEquals(1, result.candidates().size());
        assertTrue(result.candidates().get(0).path().endsWith("pending.pkg"));
    }
}
