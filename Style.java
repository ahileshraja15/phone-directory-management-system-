package com.example.phonedir;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Enumeration;

public final class Style {
    private Style() {}

    public static void installGlobalFont(String family, int size) {
        try {
            FontUIResource f = new FontUIResource(new Font(family, Font.PLAIN, size));
            Enumeration<?> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object val = UIManager.get(key);
                if (val instanceof FontUIResource) {
                    UIManager.put(key, f);
                }
            }
        } catch (Exception ignored) {}
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    public static void softenButtons(AbstractButton... buttons) {
        for (AbstractButton b : buttons) {
            if (b == null) continue;
            b.setFocusPainted(false);
            b.setMargin(new Insets(4, 10, 4, 10));
        }
    }
}