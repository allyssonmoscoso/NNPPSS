package com.squarepeace.nnppss.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateServiceTest {

    @Test
    void comparesSemanticVersions() {
        assertTrue(UpdateService.isNewer("2.0.0", "1.9.9"));
        assertTrue(UpdateService.isNewer("2.0.1", "2.0.0"));
        assertTrue(UpdateService.isNewer("2.0.1", "2.0.0-SNAPSHOT"));
        assertFalse(UpdateService.isNewer("2.0.0", "2.0.0"));
        assertFalse(UpdateService.isNewer("2.0.0", "2.0.1"));
        assertFalse(UpdateService.isNewer("dev", "2.0.0"));
    }

    @Test
    void readsNewerVersionFromManifest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/update-manifest.json", exchange -> {
            byte[] body = ("{\"version\":\"2.0.0\","
                    + "\"notesUrl\":\"https://example.test/release\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/update-manifest.json");
            UpdateService service = new UpdateService(uri, HttpClient.newHttpClient());

            Optional<UpdateService.UpdateInfo> update = service.check("1.0.0");

            assertTrue(update.isPresent());
            assertEquals("2.0.0", update.get().version());
            assertEquals("https://example.test/release", update.get().notesUrl());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void treatsMissingManifestAsNoUpdate() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/update-manifest.json", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/update-manifest.json");
            UpdateService service = new UpdateService(uri, HttpClient.newHttpClient());

            assertTrue(service.check("2.0.0").isEmpty());
        } finally {
            server.stop(0);
        }
    }
}
