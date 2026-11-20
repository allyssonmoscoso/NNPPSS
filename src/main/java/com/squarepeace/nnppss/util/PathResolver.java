package com.squarepeace.nnppss.util;

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.app.AppPaths;

/**
 * Resolves application paths. All relative paths are resolved against the
 * per-user data directory (see {@link AppPaths#dataDir()}), which is writable
 * even when the application itself is installed in a read-only location.
 *
 * @deprecated use {@link AppPaths} directly in new code; this facade is kept
 *             so that existing services do not need to change at once.
 */
@Deprecated
public final class PathResolver {
    private static final Logger log = LoggerFactory.getLogger(PathResolver.class);

    private PathResolver() {
    }

    public static Path getBaseDirectory() {
        return AppPaths.dataDir();
    }

    public static Path resolve(String relativePath) {
        return AppPaths.resolve(relativePath);
    }

    public static File getFile(String relativePath) {
        return resolve(relativePath).toFile();
    }

    public static boolean ensureDirectory(String relativePath) {
        boolean created = AppPaths.ensureDirectory(relativePath);
        if (created) {
            log.debug("Directory ready: {}", AppPaths.resolve(relativePath));
        }
        return created;
    }
}
