package com.squarepeace.nnppss.service.download;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.model.download.DownloadMetadata;

/**
 * Phase 1: Analyzes the remote resource to determine size and range support.
 */
public class ResourceAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ResourceAnalyzer.class);

    public DownloadMetadata analyze(String fileURL, String destinationPath) throws IOException {
        log.info("Analyzing resource: {}", fileURL);
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);

        int status = connection.getResponseCode();
        if (status >= 400) {
            throw new IOException("HTTP error analyzing resource: " + status);
        }

        DownloadMetadata metadata = new DownloadMetadata();
        metadata.setUrl(fileURL);
        metadata.setDestinationPath(destinationPath);
        
        // 1. Detect Total Size
        long length = connection.getContentLengthLong();
        metadata.setTotalSize(length);
        
        // 2. Detect Range Support
        String acceptRanges = connection.getHeaderField("Accept-Ranges");
        boolean supportsRanges = "bytes".equalsIgnoreCase(acceptRanges);
        // Sometimes Accept-Ranges is missing but Content-Range is present, 
        // but typically HEAD response just has Accept-Ranges.
        // Also check if content-length is known, otherwise can't segment easily.
        if (length <= 0) {
            supportsRanges = false;
        }
        metadata.setResumable(supportsRanges);

        // 3. Detect Integrity Identifiers
        metadata.setEtag(connection.getHeaderField("ETag"));
        metadata.setLastModified(connection.getLastModified());

        log.info("Analysis complete. Size: {}, Resumable: {}, ETag: {}", 
                 length, supportsRanges, metadata.getEtag());

        connection.disconnect();
        return metadata;
    }
}
