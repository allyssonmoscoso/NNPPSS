package com.squarepeace.nnppss.fx.components;

import com.squarepeace.nnppss.util.I18n;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;

/**
 * Aggregate progress of the whole download session (games and bytes).
 */
public class GlobalProgressIndicator extends VBox {

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#0.00");

    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label etaLabel;

    private int totalGames = 0;
    private int completedGames = 0;
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long startTimeMs = 0;

    public GlobalProgressIndicator() {
        setSpacing(5);
        setPadding(new Insets(10));
        setStyle("-fx-border-color: #c8c8c8; -fx-border-width: 2 0 0 0;");

        BorderPane topPane = new BorderPane();
        statusLabel = new Label(I18n.get("status.ready"));
        etaLabel = new Label("");
        topPane.setLeft(statusLabel);
        topPane.setRight(etaLabel);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(25);
        progressBar.setProgress(0);

        getChildren().addAll(topPane, progressBar);
        setVisible(false);
    }

    public void startDownloads(int totalGames, long totalBytes) {
        this.totalGames = Math.max(0, totalGames);
        this.completedGames = 0;
        this.totalBytes = Math.max(0, totalBytes);
        this.downloadedBytes = 0;
        this.startTimeMs = System.currentTimeMillis();

        updateDisplay();
        setVisible(true);
    }

    public void updateProgress(long downloadedBytes) {
        this.downloadedBytes = Math.max(0, downloadedBytes);
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
            statusLabel.setText(I18n.get("status.ready"));
            etaLabel.setText("");
            setVisible(false);
        });
    }

    private void updateDisplay() {
        double progress;
        if (totalBytes > 0) {
            progress = (double) downloadedBytes / totalBytes;
        } else if (totalGames > 0) {
            progress = (double) completedGames / totalGames;
        } else {
            progress = 0;
        }
        progress = Math.max(0, Math.min(1, progress));
        progressBar.setProgress(progress);

        statusLabel.setText(I18n.get("progress.globalinfo",
                completedGames, totalGames,
                formatSize(downloadedBytes), formatSize(totalBytes)));

        if (totalGames > 0 && completedGames >= totalGames) {
            etaLabel.setText(I18n.get("progress.complete"));
            progressBar.setProgress(1.0);
        } else if (totalBytes > 0 && downloadedBytes > 0) {
            long elapsedMs = System.currentTimeMillis() - startTimeMs;
            if (elapsedMs > 1000) {
                double bytesPerMs = (double) downloadedBytes / elapsedMs;
                long remainingBytes = Math.max(0, totalBytes - downloadedBytes);
                long etaSeconds = (long) (remainingBytes / bytesPerMs / 1000.0);
                etaLabel.setText(I18n.get("progress.eta", formatTime(etaSeconds)));
            } else {
                etaLabel.setText(I18n.get("progress.calculating"));
            }
        } else {
            etaLabel.setText("");
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

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
    }
}
