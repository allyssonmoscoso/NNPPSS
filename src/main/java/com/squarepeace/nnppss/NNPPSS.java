/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.squarepeace.nnppss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.DownloadState;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import java.util.List;

public class NNPPSS {
    private static final Logger log = LoggerFactory.getLogger(NNPPSS.class);

    public static void main(String[] args) {
        log.info("Starting NNPPSS application");
        // Initialize services
        ConfigManager configManager = new ConfigManager();
        GameRepository gameRepository = new GameRepository();
        DownloadService downloadService = new DownloadService(configManager);
        PackageService packageService = new PackageService(configManager);
        DownloadStateManager downloadStateManager = new DownloadStateManager();

        // Check configuration
        String psvitaUrl = configManager.getPsVitaUrl();
        String pspUrl = configManager.getPspUrl();
        String psxUrl = configManager.getPsxUrl();

        if ((psvitaUrl == null || psvitaUrl.isEmpty()) && 
            (pspUrl == null || pspUrl.isEmpty()) && 
            (psxUrl == null || psxUrl.isEmpty())) {
            
            Config configFrame = new Config(configManager);
            configFrame.pack();
            configFrame.setLocationRelativeTo(null);
            configFrame.setResizable(false);
            configFrame.setVisible(true);
            configFrame.loadValues();
            return;
        }

        // Create and show Frame
        Frame frame = new Frame(configManager, gameRepository, downloadService, packageService, downloadStateManager);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Download TSV files if needed
        downloadDatabases(downloadService, configManager, frame);
        
        // Restore any saved download state from previous session
        List<DownloadState> savedStates = downloadStateManager.loadStates();
        if (!savedStates.isEmpty()) {
            SwingUtilities.invokeLater(() -> frame.restoreDownloads(savedStates));
        }
    }

    private static void downloadDatabases(DownloadService downloadService, ConfigManager configManager, Frame frame) {
        downloadDatabase(downloadService, configManager.getPsVitaUrl(), Console.PSVITA.getDbPath());
        downloadDatabase(downloadService, configManager.getPspUrl(), Console.PSP.getDbPath());
        downloadDatabase(downloadService, configManager.getPsxUrl(), Console.PSX.getDbPath());
        
        // Refresh table after downloads (optional, or user can click refresh)
        SwingUtilities.invokeLater(frame::fillTable);
    }

    private static void downloadDatabase(DownloadService downloadService, String url, String path) {
        if (url == null || url.isEmpty()) return;
        
        File file = new File(path);
        if (file.exists()) return; // Already exists

        System.out.println("Downloading database: " + path);
        CountDownLatch latch = new CountDownLatch(1);
        
        downloadService.downloadFile(url, path, new DownloadService.DownloadListener() {
            @Override
            public void onProgress(long bytesDownloaded, long totalBytes) {
                // Optional: show splash screen progress
            }

            @Override
            public void onComplete(File file) {
                System.out.println("Downloaded: " + path);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                log.error("Error downloading database: {}", path, e);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.warn("Database download interrupted: {}", path, e);
            Thread.currentThread().interrupt();
        }
    }
}