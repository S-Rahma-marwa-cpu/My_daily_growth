package com.mydailygrowth;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.ui.AppFrame;
import javax.swing.*;
import java.awt.*;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                applyPrettyGirlyTheme();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Database db = new Database();
                AppFrame frame = new AppFrame(db);
                frame.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "DB Error: " + ex.getMessage());
            }
        });
    }

    private static void applyPrettyGirlyTheme() {
        String fontName = "Segoe UI Light";
        Font prettyRegular = new Font(fontName, Font.PLAIN, 13);
        Font prettyItalic = new Font(fontName, Font.ITALIC, 13);
        Font prettyBoldItalic = new Font(fontName, Font.ITALIC | Font.BOLD, 13);

        UIManager.put("Label.font", prettyItalic);
        UIManager.put("Button.font", prettyRegular);
        UIManager.put("Table.font", prettyRegular);
        UIManager.put("TableHeader.font", prettyBoldItalic);
        UIManager.put("TableHeader.background", new Color(255, 210, 220));
        UIManager.put("TableHeader.foreground", new Color(70, 30, 40));
        UIManager.put("Table.selectionBackground", new Color(255, 228, 235));
    }
}