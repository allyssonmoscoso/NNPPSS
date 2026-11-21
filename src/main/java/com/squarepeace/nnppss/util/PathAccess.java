package com.squarepeace.nnppss.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Filesystem checks that work on SMB/mapped network shares.
 *
 * <p>{@link Files#isWritable(Path)} can return {@code false} on a Windows
 * mapped drive even when the share is writable. A short create/write/delete
 * probe is the reliable check.</p>
 */
public final class PathAccess {

    private PathAccess() {
    }

    public static boolean isWritableDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return false;
        }

        Path probe = null;
        try {
            probe = Files.createTempFile(directory, ".nnppss-write-", ".tmp");
            try (OutputStream out = Files.newOutputStream(probe)) {
                out.write('N');
            }
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        } finally {
            if (probe != null) {
                try {
                    Files.deleteIfExists(probe);
                } catch (IOException ignored) {
                    // The probe is harmless if the share keeps a transient lock.
                }
            }
        }
    }
}
