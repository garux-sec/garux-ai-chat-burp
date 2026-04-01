package com.aireport.burp.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Custom tab title component that shows a red dot badge
 * when there is a new unread message — mimics Burp's native tab indicators.
 */
public class TabBadgeComponent extends JPanel {

    private final JLabel titleLabel;
    private final JLabel dotLabel;

    public TabBadgeComponent(String title) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        setOpaque(false);

        titleLabel = new JLabel(title);

        dotLabel = new JLabel("●");
        dotLabel.setForeground(new Color(0xE53935)); // Burp-style red
        dotLabel.setFont(dotLabel.getFont().deriveFont(Font.BOLD, 10f));
        dotLabel.setVisible(false);

        add(titleLabel);
        add(dotLabel);
    }

    /** Show or hide the red dot. */
    public void showBadge(boolean show) {
        SwingUtilities.invokeLater(() -> {
            dotLabel.setVisible(show);
            revalidate();
            repaint();
        });
    }

    public boolean isBadgeVisible() {
        return dotLabel.isVisible();
    }
}
