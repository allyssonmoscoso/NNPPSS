package com.squarepeace.nnppss.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.squarepeace.nnppss.app.AppPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real segmented download path against a local HTTP server with
 * Range support, including the temporary metadata file cleanup.
 */
class DownloadServiceIntegrationTest {

    @TempDir
    Path tempDir;

    private HttpServer server;
    private DownloadService downloadService;
    private byte[] payload;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, tempDir.toString());

        payload = new byte[1024 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 251);
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payload.bin", this::handle);
        server.start();

        ConfigManager configManager = new ConfigManager();
        downloadService = new DownloadService(configManager);
    }

    @AfterEach
    void tearDown() {
        if (downloadService != null) {
            downloadService.shutdown();
        }
        if (server != null) {
            server.stop(0);
        }
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void downloadsFileThroughTheSegmentedPath() throws Exception {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/payload.bin";
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        downloadService.downloadFile(url, "downloads/payload.bin",
                new DownloadService.DownloadListener() {
                    @Override
                    public void onProgress(long bytesDownloaded, long totalBytes) {
                        // Progress is exercised by the coordinator tests; here we
                        // only care about the final file.
                    }

                    @Override
                    public void onComplete(File file) {
                        finished.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        error.set(e);
                        finished.countDown();
                    }
                });

        assertTrue(finished.await(20, TimeUnit.SECONDS), "download did not finish");
        if (error.get() != null) {
            throw new AssertionError("download failed", error.get());
        }

        Path downloaded = tempDir.resolve("downloads").resolve("payload.bin");
        assertTrue(Files.exists(downloaded));
        assertArrayEquals(payload, Files.readAllBytes(downloaded));
        assertFalse(Files.exists(downloaded.resolveSibling("payload.bin.part.json")),
                "segment metadata must be removed after success");
    }

    private void handle(HttpExchange exchange) throws IOException {
        String range = exchange.getRequestHeaders().getFirst("Range");
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(payload.length));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        int start = 0;
        int end = payload.length - 1;
        int status = 200;
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring("bytes=".length()).split("-", 2);
            start = Integer.parseInt(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Integer.parseInt(parts[1]);
            }
            status = 206;
            exchange.getResponseHeaders().set("Content-Range",
                    "bytes " + start + "-" + end + "/" + payload.length);
        }

        int length = end - start + 1;
        exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
        exchange.sendResponseHeaders(status, length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload, start, length);
        }
        exchange.close();
    }
}
