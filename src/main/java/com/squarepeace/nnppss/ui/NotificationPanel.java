package com.squarepeace.nnppss.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Non-blocking notification panel that displays toast-style messages
 */
public class NotificationPanel extends JPanel {
    
    public enum NotificationType {
        INFO(new Color(33, 150, 243), "ℹ"),      // Blue
        SUCCESS(new Color(76, 175, 80), "✓"),    // Green
        WARNING(new Color(255, 152, 0), "⚠"),    // Orange
        ERROR(new Color(244, 67, 54), "✕");      // Red
        
        private final Color color;
        private final String icon;
        
        NotificationType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }
        
        public Color getColor() { return color; }
        public String getIcon() { return icon; }
    }
    
    private static final int NOTIFICATION_HEIGHT = 50;
    private static final int NOTIFICATION_WIDTH = 400;
    private static final int AUTO_DISMISS_MS = 5000;
    private static final int ANIMATION_STEP_MS = 20;
    private static final int MAX_VISIBLE_NOTIFICATIONS = 3;
    
    private final Queue<NotificationMessage> messageQueue = new LinkedList<>();
    private final JPanel notificationsContainer;
    private int visibleCount = 0;
    
    public NotificationPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        notificationsContainer = new JPanel();
        notificationsContainer.setLayout(new BoxLayout(notificationsContainer, BoxLayout.Y_AXIS));
        notificationsContainer.setOpaque(false);
        
        add(notificationsContainer, BorderLayout.NORTH);
    }
    
    /**
     * Show a notification message
     */
    public void showNotification(String message, NotificationType type) {
        showNotification(message, type, AUTO_DISMISS_MS);
    }
    
    /**
     * Show a notification message with custom duration
     */
    public void showNotification(String message, NotificationType type, int durationMs) {
        SwingUtilities.invokeLater(() -> {
            if (visibleCount >= MAX_VISIBLE_NOTIFICATIONS) {
                // Queue the message
                messageQueue.offer(new NotificationMessage(message, type, durationMs));
            } else {
                displayNotification(message, type, durationMs);
            }
        });
    }
    
    private void displayNotification(String message, NotificationType type, int durationMs) {
        JPanel notification = createNotificationPanel(message, type);
        
        // Slide-in animation
        notification.setPreferredSize(new Dimension(NOTIFICATION_WIDTH, 0));
        notification.setMaximumSize(new Dimension(NOTIFICATION_WIDTH, 0));
        
        notificationsContainer.add(notification, 0); // Add at top
        visibleCount++;
        
        // Animate slide-in
        Timer slideInTimer = new Timer(ANIMATION_STEP_MS, null);
        slideInTimer.addActionListener(new ActionListener() {
            int currentHeight = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                currentHeight += 5;
                if (currentHeight >= NOTIFICATION_HEIGHT) {
                    currentHeight = NOTIFICATION_HEIGHT;
                    slideInTimer.stop();
                    
                    // Schedule auto-dismiss
                    if (durationMs > 0) {
                        Timer dismissTimer = new Timer(durationMs, ev -> dismissNotification(notification));
                        dismissTimer.setRepeats(false);
                        dismissTimer.start();
                    }
                }
                
                notification.setPreferredSize(new Dimension(NOTIFICATION_WIDTH, currentHeight));
                notification.setMaximumSize(new Dimension(NOTIFICATION_WIDTH, currentHeight));
                notification.revalidate();
                notificationsContainer.revalidate();
                notificationsContainer.repaint();
            }
        });
        slideInTimer.start();
    }
    
    private JPanel createNotificationPanel(String message, NotificationType type) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(type.getColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, type.getColor().darker()),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Icon
        JLabel iconLabel = new JLabel(type.getIcon());
        iconLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        iconLabel.setForeground(Color.WHITE);
        panel.add(iconLabel, BorderLayout.WEST);
        
        // Message
        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        panel.add(messageLabel, BorderLayout.CENTER);
        
        // Close button
        JButton closeBtn = new JButton("×");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(type.getColor().darker());
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setFont(new Font("Dialog", Font.BOLD, 20));
        closeBtn.setPreferredSize(new Dimension(30, 30));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dismissNotification(panel));
        panel.add(closeBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private void dismissNotification(JPanel notification) {
        // Slide-out animation
        Timer slideOutTimer = new Timer(ANIMATION_STEP_MS, null);
        slideOutTimer.addActionListener(new ActionListener() {
            int currentHeight = NOTIFICATION_HEIGHT;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                currentHeight -= 5;
                if (currentHeight <= 0) {
                    slideOutTimer.stop();
                    notificationsContainer.remove(notification);
                    visibleCount--;
                    
                    // Show next queued notification
                    NotificationMessage queued = messageQueue.poll();
                    if (queued != null) {
                        displayNotification(queued.message, queued.type, queued.durationMs);
                    }
                    
                    notificationsContainer.revalidate();
                    notificationsContainer.repaint();
                    return;
                }
                
                notification.setPreferredSize(new Dimension(NOTIFICATION_WIDTH, currentHeight));
                notification.setMaximumSize(new Dimension(NOTIFICATION_WIDTH, currentHeight));
                notification.revalidate();
                notificationsContainer.revalidate();
                notificationsContainer.repaint();
            }
        });
        slideOutTimer.start();
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
