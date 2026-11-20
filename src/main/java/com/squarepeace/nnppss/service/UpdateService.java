package com.squarepeace.nnppss.service;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Checks the stable update manifest published with GitHub Releases. It never
 * downloads or executes installers; the UI only offers the release page.
 */
public class UpdateService {

    public static final String MANIFEST_URL =
            "https://github.com/allyssonmoscoso/NNPPSS/releases/latest/download/update-manifest.json";

    private final URI manifestUri;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public UpdateService() {
        this(URI.create(MANIFEST_URL), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    UpdateService(URI manifestUri, HttpClient client) {
        this.manifestUri = manifestUri;
        this.client = client;
    }

    public Optional<UpdateInfo> check(String currentVersion)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(manifestUri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return Optional.empty();
        }

        UpdateManifest manifest = gson.fromJson(response.body(), UpdateManifest.class);
        if (manifest == null || manifest.version == null || manifest.version.isBlank()) {
            return Optional.empty();
        }
        if (!isNewer(manifest.version, currentVersion)) {
            return Optional.empty();
        }
        String notes = manifest.notesUrl == null || manifest.notesUrl.isBlank()
                ? "https://github.com/allyssonmoscoso/NNPPSS/releases"
                : manifest.notesUrl;
        return Optional.of(new UpdateInfo(manifest.version, notes));
    }

    static boolean isNewer(String latest, String current) {
        try {
            ModuleDescriptor.Version newer = ModuleDescriptor.Version.parse(latest);
            ModuleDescriptor.Version installed = ModuleDescriptor.Version.parse(current);
            return newer.compareTo(installed) > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public record UpdateInfo(String version, String notesUrl) {
    }

    private static final class UpdateManifest {
        String version;
        String notesUrl;
    }
}
