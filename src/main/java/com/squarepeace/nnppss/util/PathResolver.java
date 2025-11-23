package com.squarepeace.nnppss.util;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to resolve file paths relative to the JAR location.
 * This ensures that the application creates files and directories in the same
 * location as the JAR file, regardless of the working directory from which
 * the application is executed.
 */
public class PathResolver {
    private static final Logger log = LoggerFactory.getLogger(PathResolver.class);
    private static Path baseDirectory;
    
    static {
        initializeBaseDirectory();
    }
    
    /**
     * Initializes the base directory by detecting where the JAR is located.
     */
    private static void initializeBaseDirectory() {
        try {
            File jarFile = new File(PathResolver.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            
            if (jarFile.isFile()) {
                // Running from JAR file - use JAR's parent directory
                baseDirectory = jarFile.getParentFile().toPath();
                log.info("Base directory set to JAR location: {}", baseDirectory.toAbsolutePath());
            } else {
                // Running from classes directory (dev mode) - use current directory
                baseDirectory = Paths.get(".").toAbsolutePath().normalize();
                log.info("Base directory set to current directory (dev mode): {}", baseDirectory);
            }
        } catch (URISyntaxException e) {
            // Fallback to current directory
            baseDirectory = Paths.get(".").toAbsolutePath().normalize();
            log.warn("Could not determine JAR location, using current directory: {}", baseDirectory, e);
        }
    }
    
    /**
     * Gets the base directory where the JAR is located.
     * 
     * @return the base directory path
     */
    public static Path getBaseDirectory() {
        return baseDirectory;
    }
    
    /**
     * Resolves a relative path against the base directory.
     * 
     * @param relativePath the relative path to resolve
     * @return the resolved absolute path
     */
    public static Path resolve(String relativePath) {
        return baseDirectory.resolve(relativePath).normalize();
    }
    
    /**
     * Gets a File object for a relative path resolved against the base directory.
     * 
     * @param relativePath the relative path to resolve
     * @return a File object for the resolved path
     */
    public static File getFile(String relativePath) {
        return resolve(relativePath).toFile();
    }
    
    /**
     * Ensures a directory exists, creating it if necessary.
     * 
     * @param relativePath the relative path to the directory
     * @return true if the directory exists or was created successfully
     */
    public static boolean ensureDirectory(String relativePath) {
        File dir = getFile(relativePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Created directory: {}", dir.getAbsolutePath());
            } else {
                log.error("Failed to create directory: {}", dir.getAbsolutePath());
            }
            return created;
        }
        return true;
    }
}
