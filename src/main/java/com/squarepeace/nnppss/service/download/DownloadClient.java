package com.squarepeace.nnppss.service.download;

import com.squarepeace.nnppss.service.DownloadService;

/**
 * The subset of {@link DownloadService} used by the download coordinator. It
 * allows tests to provide a fake implementation without touching the network.
 */
public interface DownloadClient {

    void downloadFile(String fileURL, String destinationPath,
                      DownloadService.DownloadListener listener);

    void pauseUrl(String url);

    void resumeUrl(String url);

    boolean isPaused(String url);

    void cancelUrl(String url);

    boolean isCancelled(String url);

    int getRetryCount(String url);
}
