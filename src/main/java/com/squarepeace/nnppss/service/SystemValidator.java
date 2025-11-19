package com.squarepeace.nnppss.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Service for validating system resources before downloads
 */
public class SystemValidator {
    private static final Logger log = LoggerFactory.getLogger(SystemValidator.class);
    private static final long MIN_FREE_SPACE_BYTES = 5L * 1024 * 1024 * 1024; // 5 GB default
    
    private final ConfigManager configManager;
    
    public SystemValidator(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Check if there's enough disk space for a download
     * @param downloadSizeBytes Size of the file to download
     * @param destinationPath Path where file will be saved
     * @return ValidationResult with status and message
     */
    public ValidationResult validateDiskSpace(long downloadSizeBytes, String destinationPath) {
        File destination = new File(destinationPath);
        File parentDir = destination.getParentFile();
        
        // If parent dir doesn't exist or is null, use current directory
        if (parentDir == null || !parentDir.exists()) {
            parentDir = new File(".");
        }
        
        long usableSpace = parentDir.getUsableSpace();
        long requiredSpace = downloadSizeBytes + MIN_FREE_SPACE_BYTES;
        
        log.debug("Disk space check: usable={}MB, required={}MB", 
                usableSpace / (1024 * 1024), requiredSpace / (1024 * 1024));
        
        if (usableSpace < requiredSpace) {
            String message = String.format(
                "Insufficient disk space. Available: %.2f GB, Required: %.2f GB (including %.2f GB buffer)",
                usableSpace / (1024.0 * 1024 * 1024),
                requiredSpace / (1024.0 * 1024 * 1024),
                MIN_FREE_SPACE_BYTES / (1024.0 * 1024 * 1024)
            );
            log.warn(message);
            return new ValidationResult(false, message);
        }
        
        return new ValidationResult(true, "Sufficient disk space available");
    }
    
    /**
     * Validate multiple downloads total size
     */
    public ValidationResult validateTotalDiskSpace(long totalBytes, String destinationPath) {
        return validateDiskSpace(totalBytes, destinationPath);
    }
    
    /**
     * Get formatted free space string
     */
    public String getFreeDiskSpaceFormatted(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir = new File(".");
        }
        
        long freeSpace = dir.getUsableSpace();
        return formatBytes(freeSpace);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Result of a validation check
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
