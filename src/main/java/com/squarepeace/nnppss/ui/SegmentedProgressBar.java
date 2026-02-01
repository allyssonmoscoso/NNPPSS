package com.squarepeace.nnppss.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JProgressBar;

import com.squarepeace.nnppss.model.download.Segment;
import com.squarepeace.nnppss.model.download.SegmentStatus;

/**
 * A custom progress bar that visualizes download segments.
 */
public class SegmentedProgressBar extends JProgressBar {
    private List<Segment> segments;
    private long totalSize;

    // Colors
    private static final Color COLOR_BG = new Color(230, 230, 230);
    private static final Color COLOR_COMPLETED = new Color(76, 175, 80); // Green
    private static final Color COLOR_DOWNLOADING = new Color(33, 150, 243); // Blue
    private static final Color COLOR_PENDING = Color.LIGHT_GRAY;
    private static final Color COLOR_FAILED = new Color(244, 67, 54); // Red

    public SegmentedProgressBar() {
        super();
        setStringPainted(true);
        // setPreferredSize(new Dimension(100, 20)); // Let layout handle it or use default
        setOpaque(true);
    }

    public void setSegments(List<Segment> segments, long totalSize) {
        this.segments = segments;
        this.totalSize = totalSize;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (segments == null || segments.isEmpty() || totalSize <= 0) {
            super.paintComponent(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(COLOR_BG);
        g2.fillRect(0, 0, width, height);

        double scale = (double) width / totalSize;

        for (Segment segment : segments) {
            // Calculate pixel positions
            int xStart = (int) (segment.getStartByte() * scale);
            int xEnd = (int) (segment.getEndByte() * scale);
            // ... (rest of logic)
            int wTotal = xEnd - xStart;
            if (wTotal < 1) wTotal = 1;

            long downloaded = segment.getCurrentOffset() - segment.getStartByte();
            int wDownloaded = (int) (downloaded * scale);

            // Draw downloaded part
            if (wDownloaded > 0) {
                if (segment.getStatus() == SegmentStatus.FAILED) {
                    g2.setColor(COLOR_FAILED);
                } else if (segment.getStatus() == SegmentStatus.COMPLETED || segment.isFinished()) {
                    g2.setColor(COLOR_COMPLETED);
                    // Force full width if completed to avoid gaps due to rounding
                    wDownloaded = wTotal + 1; 
                } else {
                    g2.setColor(COLOR_DOWNLOADING);
                }
                g2.fillRect(xStart, 0, wDownloaded, height);
            }
            
            // Draw header/current cursor of segment
            if (segment.getStatus() == SegmentStatus.DOWNLOADING) {
                 g2.setColor(Color.WHITE);
                 g2.fillRect(xStart + wDownloaded - 1, 0, 2, height);
            }
        }
        
        // Draw segment separators
        g2.setColor(Color.WHITE);
        for (Segment segment : segments) {
             int xEnd = (int) (segment.getEndByte() * scale);
             g2.drawLine(xEnd, 0, xEnd, height);
        }
        
        // Draw border
        g2.setColor(Color.GRAY);
        g2.drawRect(0, 0, width - 1, height - 1);
        
        // Paint String
        if (isStringPainted()) {
            paintString(g2, width, height);
        }
    }
    
    private void paintString(Graphics2D g, int width, int height) {
        String text = getString();
        if (text == null || text.isEmpty()) return;

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        java.awt.geom.Rectangle2D bounds = fm.getStringBounds(text, g);
        
        int x = (int) ((width - bounds.getWidth()) / 2);
        int y = (int) ((height - bounds.getHeight()) / 2 + fm.getAscent());
        
        // Draw outline for better visibility? Or just contrast color.
        // Simple contrast: Black.
        g.setColor(Color.BLACK);
        g.drawString(text, x, y);
    }
}
