package com.squarepeace.nnppss.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * Global progress indicator showing overall download status
 */
public class GlobalProgressPanel extends JPanel {
    
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel etaLabel;
    
    private int totalGames = 0;
    private int completedGames = 0;
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long startTimeMs = 0;
    
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#0.00");
    
    public GlobalProgressPanel() {
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 25));
        progressBar.setFont(new Font("Dialog", Font.BOLD, 12));
        
        // Status label (games count)
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        
        // ETA label
        etaLabel = new JLabel("");
        etaLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        etaLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(etaLabel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        
        setVisible(false); // Hidden by default
    }
    
    /**
     * Start tracking downloads
     */
    public void startDownloads(int totalGames, long totalBytes) {
        this.totalGames = totalGames;
        this.completedGames = 0;
        this.totalBytes = totalBytes;
        this.downloadedBytes = 0;
        this.startTimeMs = System.currentTimeMillis();
        
        updateDisplay();
        setVisible(true);
    }
    
    /**
     * Update progress
     */
    public void updateProgress(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
        updateDisplay();
    }
    
    /**
     * Mark a game as completed
     */
    public void gameCompleted() {
        this.completedGames++;
        updateDisplay();
    }
    
    /**
     * Reset and hide
     */
    public void reset() {
        totalGames = 0;
        completedGames = 0;
        totalBytes = 0;
        downloadedBytes = 0;
        startTimeMs = 0;
        
        progressBar.setValue(0);
        progressBar.setString("Ready");
        statusLabel.setText("Ready");
        etaLabel.setText("");
        setVisible(false);
    }
    
    private void updateDisplay() {
        // Calculate percentage
        int percentBytes = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : 0;
        int percentGames = totalGames > 0 ? (int) ((completedGames * 100) / totalGames) : 0;
        
        // Use average of both percentages for smoother display
        int percent = (percentBytes + percentGames) / 2;
        
        progressBar.setValue(percent);
        progressBar.setString(percent + "%");
        
        // Status text
        String sizeText = formatSize(downloadedBytes) + " / " + formatSize(totalBytes);
        statusLabel.setText(String.format("Downloading: %d / %d games  •  %s", 
            completedGames, totalGames, sizeText));
        
        // ETA calculation
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        if (elapsedMs > 1000 && downloadedBytes > 0 && downloadedBytes < totalBytes) {
            long remainingBytes = totalBytes - downloadedBytes;
            double bytesPerMs = (double) downloadedBytes / elapsedMs;
            long etaMs = (long) (remainingBytes / bytesPerMs);
            
            etaLabel.setText("ETA: " + formatTime(etaMs));
        } else if (completedGames >= totalGames) {
            etaLabel.setText("Complete!");
        } else {
            etaLabel.setText("Calculating...");
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
    
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long mins = seconds / 60;
            long secs = seconds % 60;
            return String.format("%dm %ds", mins, secs);
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            return String.format("%dh %dm", hours, mins);
        }
    }
}
