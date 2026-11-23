package com.mydailygrowth.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lightweight dependency-free chart for the daily completion totals. */
@SuppressWarnings("serial")
public final class BarChartPanel extends JPanel {
    private Map<LocalDate, Integer> values = new LinkedHashMap<>();
    private LocalDate from = LocalDate.now();
    private LocalDate to = LocalDate.now();
    private int maxValue = 1;

    public BarChartPanel() {
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(600, 240));
    }

    public void setData(Map<LocalDate, Integer> values, LocalDate from, LocalDate to) {
        this.values = new LinkedHashMap<>(values);
        this.from = from;
        this.to = to;
        this.maxValue = Math.max(1, values.values().stream().mapToInt(Integer::intValue).max().orElse(1));
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 42, right = 16, top = 16, bottom = 34;
        int chartWidth = Math.max(1, getWidth() - left - right);
        int chartHeight = Math.max(1, getHeight() - top - bottom);
        int days = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        if (days <= 0) days = 1;
        if (values.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 13));
            g.setColor(Theme.MUTED);
            String message = "No check-ins yet — start with one small step ♡";
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(message, Math.max(12, (getWidth() - metrics.stringWidth(message)) / 2), getHeight() / 2);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i <= 4; i++) {
            int y = top + chartHeight - (i * chartHeight / 4);
            g.setColor(Theme.BORDER);
            g.drawLine(left, y, left + chartWidth, y);
            g.setColor(Theme.MUTED);
            g.drawString(String.valueOf(maxValue * i / 4), 9, y + 4);
        }
        int gap = days > 20 ? 2 : 9;
        int barWidth = Math.max(3, (chartWidth - gap * (days - 1)) / days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d");
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            int count = values.getOrDefault(date, 0);
            int barHeight = (int) Math.round(chartHeight * count / (double) maxValue);
            int x = left + i * (barWidth + gap);
            int y = top + chartHeight - barHeight;
            g.setColor(count == 0 ? Theme.BLUSH : Theme.ROSE);
            g.fill(new RoundRectangle2D.Double(x, y, barWidth, Math.max(3, barHeight), 7, 7));
            boolean showLabel = days <= 8 || i == 0 || (i + 1) % 5 == 0 || i == days - 1;
            if (showLabel) {
                g.setColor(Theme.MUTED);
                String label = days == 1 ? date.format(DateTimeFormatter.ofPattern("MMM d")) : date.format(formatter);
                int labelWidth = g.getFontMetrics().stringWidth(label);
                g.drawString(label, x + Math.max(0, (barWidth - labelWidth) / 2), top + chartHeight + 18);
            }
        }
        g.dispose();
    }
}
