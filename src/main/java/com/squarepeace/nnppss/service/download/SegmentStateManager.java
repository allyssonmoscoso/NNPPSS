package com.squarepeace.nnppss.service.download;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squarepeace.nnppss.model.download.DownloadMetadata;

/**
 * Manages persistence of download metadata (segments, progress).
 */
public class SegmentStateManager {
    private static final Logger log = LoggerFactory.getLogger(SegmentStateManager.class);
    private final Gson gson;

    public SegmentStateManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveState(DownloadMetadata metadata) {
        if (metadata == null || metadata.getDestinationPath() == null) return;
        
        File metaFile = getMetadataFile(metadata.getDestinationPath());
        try (FileWriter writer = new FileWriter(metaFile)) {
            gson.toJson(metadata, writer);
            // log.debug("Saved metadata to {}", metaFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to save download state for {}", metadata.getDestinationPath(), e);
        }
    }

    public DownloadMetadata loadState(String destinationPath) {
        File metaFile = getMetadataFile(destinationPath);
        if (!metaFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(metaFile)) {
            DownloadMetadata metadata = gson.fromJson(reader, DownloadMetadata.class);
            if (metadata != null) {
                // Validate if file matches? Maybe in a later phase.
                // For now, assume if metadata exists, we try to resume.
                return metadata;
            }
        } catch (IOException e) {
            log.warn("Failed to load download state from {}", metaFile.getAbsolutePath(), e);
        }
        return null;
    }

    public void clearState(String destinationPath) {
        File metaFile = getMetadataFile(destinationPath);
        if (metaFile.exists()) {
            if (!metaFile.delete()) {
                log.warn("Could not delete metadata file {}", metaFile.getAbsolutePath());
            }
        }
    }

    private File getMetadataFile(String destinationPath) {
        return new File(destinationPath + ".part.json");
    }
}
