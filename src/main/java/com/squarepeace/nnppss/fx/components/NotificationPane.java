package com.squarepeace.nnppss.fx.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

public class NotificationPane extends StackPane {

    public enum NotificationType {
        INFO(Color.rgb(33, 150, 243), "ℹ"),
        SUCCESS(Color.rgb(76, 175, 80), "✓"),
        WARNING(Color.rgb(255, 152, 0), "⚠"),
        ERROR(Color.rgb(244, 67, 54), "✕");

        private final Color color;
        private final String icon;

        NotificationType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public Color getColor() { return color; }
        public String getIcon() { return icon; }
    }

    private static final int MAX_VISIBLE_NOTIFICATIONS = 3;
    private static final double NOTIFICATION_WIDTH = 350;
    private static final Duration ANIMATION_DURATION = Duration.millis(300);

    private final VBox container;
    private final Queue<NotificationMessage> messageQueue = new LinkedList<>();
    private int visibleCount = 0;

    public NotificationPane() {
        setPickOnBounds(false); // Allow clicks to pass through empty areas
        setAlignment(Pos.TOP_RIGHT);
        setPadding(new Insets(20));

        container = new VBox(10); // Spacing between notifications
        container.setAlignment(Pos.TOP_RIGHT);
        container.setPickOnBounds(false);
        
        getChildren().add(container);
    }

    public void showNotification(String message, NotificationType type) {
        showNotification(message, type, 5000);
    }

    public void showNotification(String message, NotificationType type, int durationMs) {
        Platform.runLater(() -> {
            if (visibleCount >= MAX_VISIBLE_NOTIFICATIONS) {
                messageQueue.offer(new NotificationMessage(message, type, durationMs));
            } else {
                displayNotification(message, type, durationMs);
            }
        });
    }

    private void displayNotification(String message, NotificationType type, int durationMs) {
        HBox notification = createNotificationNode(message, type);
        notification.setOpacity(0);
        notification.setTranslateX(NOTIFICATION_WIDTH); // Start off-screen

        container.getChildren().add(0, notification); // Add to top
        visibleCount++;

        // Animate slide-in
        Timeline slideIn = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(notification.opacityProperty(), 1.0),
                        new KeyValue(notification.translateXProperty(), 0.0)
                )
        );
        slideIn.play();

        // Auto-dismiss
        if (durationMs > 0) {
            Timeline dismissTimer = new Timeline(new KeyFrame(Duration.millis(durationMs), e -> dismissNotification(notification)));
            dismissTimer.setCycleCount(1);
            dismissTimer.play();
        }
    }

    private HBox createNotificationNode(String message, NotificationType type) {
        HBox hbox = new HBox(10);
        hbox.setPrefWidth(NOTIFICATION_WIDTH);
        hbox.setMaxWidth(NOTIFICATION_WIDTH);
        hbox.setPadding(new Insets(10));
        hbox.setAlignment(Pos.CENTER_LEFT);
        
        // Convert Color to hex string for CSS
        String colorHex = toHexString(type.getColor());
        String bgColor = "rgba(" + (int)(type.getColor().getRed()*255) + "," + 
                                   (int)(type.getColor().getGreen()*255) + "," + 
                                   (int)(type.getColor().getBlue()*255) + ", 0.9)";
        
        hbox.setStyle("-fx-background-color: " + bgColor + "; " +
                      "-fx-background-radius: 5; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        // Icon
        Label iconLabel = new Label(type.getIcon());
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Message
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        msgLabel.setWrapText(true);
        HBox.setHgrow(msgLabel, Priority.ALWAYS);

        // Close Button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        closeBtn.setPadding(new Insets(0, 5, 0, 5));
        closeBtn.setOnAction(e -> dismissNotification(hbox));

        hbox.getChildren().addAll(iconLabel, msgLabel, closeBtn);
        return hbox;
    }

    private void dismissNotification(HBox notification) {
        if (!container.getChildren().contains(notification)) return;

        Timeline slideOut = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(notification.opacityProperty(), 0.0),
                        new KeyValue(notification.translateXProperty(), NOTIFICATION_WIDTH)
                )
        );
        slideOut.setOnFinished(e -> {
            container.getChildren().remove(notification);
            visibleCount--;

            NotificationMessage next = messageQueue.poll();
            if (next != null) {
                displayNotification(next.message, next.type, next.durationMs);
            }
        });
        slideOut.play();
    }
    
    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255));
    }

    private static class NotificationMessage {
        String message;
        NotificationType type;
        int durationMs;

        NotificationMessage(String message, NotificationType type, int durationMs) {
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
        }
    }
}
