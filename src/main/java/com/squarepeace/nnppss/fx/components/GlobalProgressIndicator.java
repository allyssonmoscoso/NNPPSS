package com.squarepeace.nnppss.fx.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;

public class GlobalProgressIndicator extends VBox {

    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label etaLabel;

    private int totalGames = 0;
    private int completedGames = 0;
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long startTimeMs = 0;

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#0.00");

    public GlobalProgressIndicator() {
        setSpacing(5);
        setPadding(new Insets(10));
        setStyle("-fx-border-color: #c8c8c8; -fx-border-width: 2 0 0 0;");

        // Top Row: Status (Left) and ETA (Right)
        BorderPane topPane = new BorderPane();
        statusLabel = new Label("Ready");
        etaLabel = new Label("");
        topPane.setLeft(statusLabel);
        topPane.setRight(etaLabel);

        // Middle: ProgressBar
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(25);

        // Overlay text on progress bar is tricky in JavaFX without a StackPane,
        // let's just put it below or use a StackPane if we want it inside.
        // For simplicity, let's put percentage on the right of the bar or inside.
        // Let's us a simple VBox layout for now.
        
        // Let's try a StackPane for the progress text if we want specific styling,
        // but standard ProgressBar doesn't support text inside easily.
        // We'll place percentage in the center? No, let's just keep it simple.
        
        getChildren().addAll(topPane, progressBar);
        
        setVisible(false);
    }

    public void startDownloads(int totalGames, long totalBytes) {
        this.totalGames = totalGames;
        this.completedGames = 0;
        this.totalBytes = totalBytes;
        this.downloadedBytes = 0;
        this.startTimeMs = System.currentTimeMillis();

        updateDisplay();
        setVisible(true);
    }

    public void updateProgress(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
        Platform.runLater(this::updateDisplay);
    }

    public void gameCompleted() {
        this.completedGames++;
        Platform.runLater(this::updateDisplay);
    }

    public void reset() {
        totalGames = 0;
        completedGames = 0;
        totalBytes = 0;
        downloadedBytes = 0;
        startTimeMs = 0;

        Platform.runLater(() -> {
            progressBar.setProgress(0);
            statusLabel.setText("Ready");
            etaLabel.setText("");
            setVisible(false);
        });
    }

    private void updateDisplay() {
        // Calculate percentage
        double percentBytes = totalBytes > 0 ? (double) downloadedBytes / totalBytes : 0;
        double percentGames = totalGames > 0 ? (double) completedGames / totalGames : 0;

        // Averaging both for smoothness, or just use bytes if available
        double progress = (percentBytes + percentGames) / 2.0;

        progressBar.setProgress(progress);

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
            progressBar.setProgress(1.0);
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
