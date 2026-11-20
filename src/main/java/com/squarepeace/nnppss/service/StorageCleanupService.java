package com.squarepeace.nnppss.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds and deletes .pkg files that the app has not extracted yet. Extracted
 * files, active downloads and non-pkg files are never touched.
 */
public final class StorageCleanupService {
    private static final Logger log = LoggerFactory.getLogger(StorageCleanupService.class);

    public record Candidate(Path path, long bytes) {
    }

    public record ScanResult(List<Candidate> candidates, long totalBytes) {
        public boolean isEmpty() {
            return candidates.isEmpty();
        }
    }

    public ScanResult scan(Path downloadsDirectory, Set<Path> excluded,
                           DownloadHistoryManager historyManager) {
        if (downloadsDirectory == null || !Files.isDirectory(downloadsDirectory)) {
            return new ScanResult(List.of(), 0);
        }

        Set<Path> ignored = new HashSet<>();
        if (excluded != null) {
            for (Path path : excluded) {
                if (path != null) {
                    ignored.add(path.toAbsolutePath().normalize());
                }
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        long total = 0;
        try (var stream = Files.list(downloadsDirectory)) {
            for (Path path : stream.toList()) {
                if (!Files.isRegularFile(path)
                        || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pkg")) {
                    continue;
                }
                Path normalized = path.toAbsolutePath().normalize();
                if (ignored.contains(normalized)) {
                    continue;
                }
                if (historyManager != null && historyManager.isExtractedFile(normalized)) {
                    continue;
                }
                long bytes = Files.size(normalized);
                candidates.add(new Candidate(normalized, bytes));
                total += bytes;
            }
        } catch (IOException e) {
            log.error("Could not scan downloads directory: {}", downloadsDirectory, e);
        }

        candidates.sort(Comparator.comparing(candidate -> candidate.path().toString()));
        return new ScanResult(List.copyOf(candidates), total);
    }

    /** Deletes the candidates and returns the actually freed bytes. */
    public long delete(List<Candidate> candidates) {
        long freed = 0;
        if (candidates == null) {
            return 0;
        }
        for (Candidate candidate : candidates) {
            try {
                if (Files.deleteIfExists(candidate.path())) {
                    freed += candidate.bytes();
                }
            } catch (IOException e) {
                log.warn("Could not delete unextracted package: {}", candidate.path(), e);
            }
        }
        return freed;
    }
}
