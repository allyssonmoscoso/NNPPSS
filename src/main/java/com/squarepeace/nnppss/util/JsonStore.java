package com.squarepeace.nnppss.util;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Small helper for JSON persistence with atomic replacement, so that a reader
 * never observes a half-written file (state is saved from worker threads while
 * the UI reads it).
 */
public final class JsonStore {

    private JsonStore() {
    }

    public static <T> T read(Path path, Type type, Gson gson) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }

    public static void write(Path path, Object value, Gson gson) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                gson.toJson(value, writer);
            }
            try {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
