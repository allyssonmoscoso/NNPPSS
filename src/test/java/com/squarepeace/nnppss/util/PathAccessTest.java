package com.squarepeace.nnppss.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathAccessTest {

    @TempDir
    Path tempDir;

    @Test
    void writableDirectoryAcceptsARealProbe() throws Exception {
        assertTrue(PathAccess.isWritableDirectory(tempDir));
    }

    @Test
    void missingDirectoryIsNotWritable() {
        assertFalse(PathAccess.isWritableDirectory(tempDir.resolve("missing")));
    }

    @Test
    void probeLeavesNoTemporaryFileBehind() throws Exception {
        assertTrue(PathAccess.isWritableDirectory(tempDir));
        try (var files = Files.list(tempDir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString()
                    .startsWith(".nnppss-write-")));
        }
    }
}
