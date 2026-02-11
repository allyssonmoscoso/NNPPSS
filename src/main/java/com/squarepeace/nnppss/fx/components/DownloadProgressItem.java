package com.squarepeace.nnppss.fx.components;

import com.squarepeace.nnppss.model.download.Segment;
import com.squarepeace.nnppss.model.download.SegmentStatus;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.util.List;

/**
 * A UI component that displays the progress of an individual download.
 * Shows game title, a segmented progress bar, speed, ETA, and status.
 * The canvas-based bar resizes properly with the window.
 */
public class DownloadProgressItem extends VBox {

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#0.00");

    // Segment colors
    private static final Color COLOR_COMPLETED   = Color.web("#4CAF50");
    private static final Color COLOR_DOWNLOADING  = Color.web("#2196F3");
    private static final Color COLOR_PENDING      = Color.web("#9E9E9E");
    private static final Color COLOR_FAILED       = Color.web("#F44336");
    private static final Color COLOR_BACKGROUND   = Color.web("#E0E0E0");

    private static final double BAR_HEIGHT = 18;

    private final Label titleLabel;
    private final Label speedEtaLabel;
    private final Canvas progressCanvas;
    private final Pane canvasHolder;   // Resizable wrapper for the fixed-size Canvas
    private final Label statusLabel;
    private final Button cancelButton;

    private final String gameTitle;
    private Runnable onCancel;

    // State
    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long lastUpdateTimeMs = 0;
    private long lastBytesForSpeed = 0;
    private double currentSpeedBps = 0;
    private long startTimeMs;
    private boolean isSegmented = false;
    private List<Segment> lastSegments;
    private boolean isCompleted = false;
    private boolean isFailed = false;

    public DownloadProgressItem(String gameTitle, long totalBytes) {
        this.gameTitle = gameTitle;
        this.totalBytes = totalBytes;
        this.startTimeMs = System.currentTimeMillis();

        setSpacing(3);
        setPadding(new Insets(8, 10, 8, 10));
        setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 4; -fx-background-radius: 4;");
        setMaxWidth(Double.MAX_VALUE);

        // ── Row 1: Title (left) + Speed/ETA + Cancel (right) ──
        BorderPane topRow = new BorderPane();
        titleLabel = new Label(gameTitle);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setEllipsisString("...");
        BorderPane.setAlignment(titleLabel, Pos.CENTER_LEFT);

        HBox rightBox = new HBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(HBox.USE_PREF_SIZE); // don't shrink past content
        speedEtaLabel = new Label("");
        speedEtaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        speedEtaLabel.setMinWidth(Label.USE_PREF_SIZE);

        cancelButton = new Button("✕");
        cancelButton.setStyle("-fx-font-size: 10px; -fx-padding: 1 5 1 5; -fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        cancelButton.setTooltip(new Tooltip("Cancel Download"));
        cancelButton.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });
        rightBox.getChildren().addAll(speedEtaLabel, cancelButton);

        topRow.setLeft(titleLabel);
        topRow.setRight(rightBox);

        // ── Row 2: Resizable canvas bar ──
        // Wrap Canvas in a Pane so that the Pane participates in layout,
        // and we bind the Canvas size to the Pane.
        progressCanvas = new Canvas(0, BAR_HEIGHT);
        canvasHolder = new Pane(progressCanvas);
        canvasHolder.setMinHeight(BAR_HEIGHT);
        canvasHolder.setPrefHeight(BAR_HEIGHT);
        canvasHolder.setMaxHeight(BAR_HEIGHT);
        canvasHolder.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(canvasHolder, Priority.NEVER);

        // When the holder resizes, resize the canvas and repaint
        canvasHolder.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w > 0) {
                progressCanvas.setWidth(w);
                repaintCanvas();
            }
        });
        canvasHolder.heightProperty().addListener((obs, oldH, newH) -> {
            double h = newH.doubleValue();
            if (h > 0) {
                progressCanvas.setHeight(h);
                repaintCanvas();
            }
        });

        // ── Row 3: Status ──
        statusLabel = new Label("Preparing...");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(topRow, canvasHolder, statusLabel);
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    /** Update for a non-segmented (legacy) download. */
    public void updateProgress(long downloaded, long total) {
        if (isCompleted || isFailed) return;
        this.downloadedBytes = downloaded;
        if (total > 0) this.totalBytes = total;
        this.isSegmented = false;
        updateSpeed(downloaded);

        Platform.runLater(() -> {
            repaintCanvas();
            updateStatusLabel();
            updateSpeedLabel();
        });
    }

    /** Update for a segmented download — shows each segment individually. */
    public void updateSegments(List<Segment> segments) {
        if (isCompleted || isFailed) return;
        this.lastSegments = segments;
        this.isSegmented = true;

        long downloaded = segments.stream().mapToLong(Segment::bytesDownloaded).sum();
        this.downloadedBytes = downloaded;
        updateSpeed(downloaded);

        Platform.runLater(() -> {
            repaintCanvas();
            updateStatusLabel();
            updateSpeedLabel();
        });
    }

    public void setCompleted() {
        this.isCompleted = true;
        this.downloadedBytes = this.totalBytes;
        Platform.runLater(() -> {
            setStyle("-fx-background-color: #E8F5E9; -fx-border-color: #A5D6A7; -fx-border-radius: 4; -fx-background-radius: 4;");
            statusLabel.setText("✓ Completed — " + formatSize(totalBytes));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32;");
            speedEtaLabel.setText("Done");
            cancelButton.setVisible(false);
            repaintCanvas();
        });
    }

    public void setFailed(String error) {
        this.isFailed = true;
        Platform.runLater(() -> {
            setStyle("-fx-background-color: #FFEBEE; -fx-border-color: #EF9A9A; -fx-border-radius: 4; -fx-background-radius: 4;");
            statusLabel.setText("✗ Failed: " + (error != null ? error : "Unknown error"));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #C62828;");
            speedEtaLabel.setText("");
            cancelButton.setVisible(false);
            repaintCanvas();
        });
    }

    public void setCancelled() {
        this.isFailed = true;
        Platform.runLater(() -> {
            setStyle("-fx-background-color: #FFF3E0; -fx-border-color: #FFCC02; -fx-border-radius: 4; -fx-background-radius: 4;");
            statusLabel.setText("⚠ Cancelled");
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #E65100;");
            speedEtaLabel.setText("");
            cancelButton.setVisible(false);
            repaintCanvas();
        });
    }

    // ═══════════════════════  Canvas Painting  ═══════════════════════

    private void repaintCanvas() {
        double w = progressCanvas.getWidth();
        double h = progressCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = progressCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Background
        gc.setFill(COLOR_BACKGROUND);
        gc.fillRoundRect(0, 0, w, h, 6, 6);

        if (isCompleted) {
            gc.setFill(COLOR_COMPLETED);
            gc.fillRoundRect(0, 0, w, h, 6, 6);
            drawPercentageText(gc, w, h, 100.0);
            return;
        }

        if (isSegmented && lastSegments != null && !lastSegments.isEmpty()) {
            paintSegmented(gc, w, h);
        } else {
            paintLegacy(gc, w, h);
        }
    }

    private void paintSegmented(GraphicsContext gc, double w, double h) {
        if (totalBytes <= 0) return;

        // Clip to rounded rect
        gc.save();
        gc.beginPath();
        roundRect(gc, 0, 0, w, h, 6);
        gc.clip();

        for (Segment seg : lastSegments) {
            double segStartFrac = (double) seg.getStartByte() / totalBytes;
            double segEndFrac   = (double) (seg.getEndByte() + 1) / totalBytes;
            double segX = segStartFrac * w;
            double segW = (segEndFrac - segStartFrac) * w;

            // Segment background
            gc.setFill(COLOR_PENDING);
            gc.fillRect(segX, 0, segW, h);

            // Downloaded portion
            double dlFrac = seg.length() > 0 ? (double) seg.bytesDownloaded() / seg.length() : 0;

            Color segColor;
            switch (seg.getStatus()) {
                case COMPLETED:  segColor = COLOR_COMPLETED;  dlFrac = 1.0; break;
                case DOWNLOADING: segColor = COLOR_DOWNLOADING; break;
                case FAILED:     segColor = COLOR_FAILED;     break;
                default:         segColor = COLOR_PENDING;    break;
            }

            if (dlFrac > 0) {
                gc.setFill(segColor);
                gc.fillRect(segX, 0, segW * dlFrac, h);
            }

            // Thin separator line
            gc.setStroke(Color.rgb(255, 255, 255, 0.5));
            gc.setLineWidth(1);
            gc.strokeLine(segX + segW, 0, segX + segW, h);
        }

        gc.restore(); // pop clip

        // Percentage text
        double pct = totalBytes > 0 ? (double) downloadedBytes / totalBytes * 100.0 : 0;
        drawPercentageText(gc, w, h, pct);
    }

    private void paintLegacy(GraphicsContext gc, double w, double h) {
        double fraction = totalBytes > 0 ? Math.min(1.0, (double) downloadedBytes / totalBytes) : 0;

        if (fraction > 0) {
            gc.save();
            gc.beginPath();
            roundRect(gc, 0, 0, w, h, 6);
            gc.clip();
            gc.setFill(COLOR_DOWNLOADING);
            gc.fillRect(0, 0, w * fraction, h);
            gc.restore();
        }

        drawPercentageText(gc, w, h, fraction * 100.0);
    }

    /** Helper: trace a rounded-rectangle path on the GraphicsContext. */
    private void roundRect(GraphicsContext gc, double x, double y, double w, double h, double r) {
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.arcTo(x + w, y, x + w, y + r, r);
        gc.lineTo(x + w, y + h - r);
        gc.arcTo(x + w, y + h, x + w - r, y + h, r);
        gc.lineTo(x + r, y + h);
        gc.arcTo(x, y + h, x, y + h - r, r);
        gc.lineTo(x, y + r);
        gc.arcTo(x, y, x + r, y, r);
        gc.closePath();
    }

    private void drawPercentageText(GraphicsContext gc, double w, double h, double pct) {
        String text = String.format("%.1f%%", pct);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));

        double textWidth = text.length() * 6.5;
        double x = (w - textWidth) / 2;
        double y = h / 2 + 4;

        // Shadow for readability
        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillText(text, x + 0.5, y + 0.5);
        gc.setFill(Color.WHITE);
        gc.fillText(text, x, y);
    }

    // ═══════════════════════  Speed / ETA  ═══════════════════════

    private void updateSpeed(long downloaded) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdateTimeMs;
        if (elapsed > 500) {
            long byteDelta = downloaded - lastBytesForSpeed;
            currentSpeedBps = byteDelta * 1000.0 / elapsed;
            lastBytesForSpeed = downloaded;
            lastUpdateTimeMs = now;
        }
    }

    private void updateSpeedLabel() {
        if (isCompleted || isFailed) return;

        StringBuilder sb = new StringBuilder();
        if (currentSpeedBps > 0) {
            sb.append(formatSize((long) currentSpeedBps)).append("/s");
        }
        if (currentSpeedBps > 0 && downloadedBytes < totalBytes) {
            long remaining = totalBytes - downloadedBytes;
            long etaSec = (long) (remaining / currentSpeedBps);
            sb.append("  •  ETA: ").append(formatTime(etaSec));
        }
        speedEtaLabel.setText(sb.toString());
    }

    private void updateStatusLabel() {
        if (isCompleted || isFailed) return;

        String sizeText = formatSize(downloadedBytes) + " / " + formatSize(totalBytes);
        if (isSegmented && lastSegments != null) {
            long completed = lastSegments.stream().filter(s -> s.getStatus() == SegmentStatus.COMPLETED).count();
            long downloading = lastSegments.stream().filter(s -> s.getStatus() == SegmentStatus.DOWNLOADING).count();
            statusLabel.setText(String.format("Segments: %d/%d completed, %d active  •  %s",
                    completed, lastSegments.size(), downloading, sizeText));
        } else {
            statusLabel.setText("Downloading: " + sizeText);
        }
    }

    // ═══════════════════════  Formatting  ═══════════════════════

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        else if (bytes < 1024L * 1024 * 1024) return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        else return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }

    private static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        else if (seconds < 3600) return String.format("%dm %ds", seconds / 60, seconds % 60);
        else return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }
}
