package com.example.phonedir;

import java.awt.Color;

public class Theme {
    public final boolean dark;
    public final Color base;
    public final Color baseLight;
    public final Color baseDark;
    public final Color bg;
    public final Color bgAlt;
    public final Color text;
    public final Color textMuted;
    public final Color headerStart;
    public final Color headerEnd;

    public Theme(boolean dark,
                 Color base,
                 Color baseLight,
                 Color baseDark,
                 Color bg,
                 Color bgAlt,
                 Color text,
                 Color textMuted,
                 Color headerStart,
                 Color headerEnd) {
        this.dark = dark;
        this.base = base;
        this.baseLight = baseLight;
        this.baseDark = baseDark;
        this.bg = bg;
        this.bgAlt = bgAlt;
        this.text = text;
        this.textMuted = textMuted;
        this.headerStart = headerStart;
        this.headerEnd = headerEnd;
    }

    public static Theme ocean(boolean dark) {
        return make(new Color(0x2D7FF9), dark);
    }
    public static Theme emerald(boolean dark) {
        return make(new Color(0x10B981), dark); // teal/green
    }
    public static Theme amethyst(boolean dark) {
        return make(new Color(0x8B5CF6), dark); // purple
    }
    public static Theme coral(boolean dark) {
        return make(new Color(0xF97360), dark); // coral
    }

    private static Theme make(Color base, boolean dark) {
        Color baseLight = lighten(base, 0.25f);
        Color baseDark = darken(base, 0.15f);
        Color bg = dark ? new Color(0x1E1F22) : new Color(0xF6F8FA);
        Color bgAlt = dark ? new Color(0x2A2C30) : new Color(0xFFFFFF);
        Color text = dark ? new Color(0xE6E6E6) : new Color(0x16171A);
        Color textMuted = dark ? new Color(0xB4B6BA) : new Color(0x3F434A);
        Color headerStart = blend(base, bg, dark ? 0.35f : 0.2f);
        Color headerEnd = base;
        return new Theme(dark, base, baseLight, baseDark, bg, bgAlt, text, textMuted, headerStart, headerEnd);
    }

    // Public factory to create a theme from any base color
    public static Theme fromBase(Color base, boolean dark) {
        return make(base, dark);
    }

    public static Color lighten(Color c, float amount) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[2] = clamp(hsb[2] + amount, 0f, 1f); // brightness
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
    public static Color darken(Color c, float amount) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[2] = clamp(hsb[2] - amount, 0f, 1f);
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
    public static Color blend(Color a, Color b, float t) {
        t = clamp(t, 0f, 1f);
        int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}