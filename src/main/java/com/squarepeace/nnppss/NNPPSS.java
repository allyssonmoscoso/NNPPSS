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
        frame.downloadDatabases();
        
        // Restore any saved download state from previous session
        List<DownloadState> savedStates = downloadStateManager.loadStates();
        if (!savedStates.isEmpty()) {
            SwingUtilities.invokeLater(() -> frame.restoreDownloads(savedStates));
        }
    }
}