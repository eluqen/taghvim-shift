package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppTheme {
    public static final String PREFS = "eluqen_shift_calendar_preferences";
    public static final String KEY = "app_theme";
    public static final String DEFAULT = "eluqen";
    public static final String[] KEYS = {
            "eluqen", "ocean", "violet", "forest", "rose",
            "graphite", "sand", "ice", "sunset"
    };
    public static final String[] LABELS = {
            "ELUQEN", "اقیانوسی", "بنفش آرام", "سبز جنگلی", "رز خاکی",
            "گرافیتی", "شنی", "یخی", "غروب"
    };

    private AppTheme() {}

    public static final class Palette {
        public final String key, label;
        public final int primary, primaryDark, secondary, accent;
        public final int surface, surfaceAlt, card, text, muted, border;
        Palette(String key, String label,
                int primary, int primaryDark, int secondary, int accent,
                int surface, int surfaceAlt, int card,
                int text, int muted, int border) {
            this.key = key;
            this.label = label;
            this.primary = primary;
            this.primaryDark = primaryDark;
            this.secondary = secondary;
            this.accent = accent;
            this.surface = surface;
            this.surfaceAlt = surfaceAlt;
            this.card = card;
            this.text = text;
            this.muted = muted;
            this.border = border;
        }
    }

    public static String currentKey(Context context) {
        String stored = context.getSharedPreferences(PREFS, 0)
                .getString(KEY, DEFAULT);
        return palette(stored).key;
    }
    public static Palette current(Context context) {
        return palette(currentKey(context));
    }

    public static void set(Context context, String key) {
        Palette selected = palette(key);
        context.getSharedPreferences(PREFS, 0)
                .edit().putString(KEY, selected.key).apply();
        apply(context);
        MainPageSwipe.dropCachedPreviews(context);
    }

    public static void apply(Context context) {
        Palette p = current(context);
        Ui.PRIMARY = p.primary;
        Ui.PRIMARY_DARK = p.primaryDark;
        Ui.ACCENT = p.accent;
        Ui.SECONDARY = p.secondary;
        Ui.SURFACE = p.surface;
        Ui.SURFACE_ALT = p.surfaceAlt;
        Ui.CARD = p.card;
        Ui.TEXT = p.text;
        Ui.MUTED = p.muted;
        Ui.BORDER = p.border;
        DesignPolish.applyTheme(p);
        SettingsFeaturesUi.applyTheme(p);
        BackDock.applyTheme(p);
    }
    public static Palette palette(String key) {
        if ("ocean".equals(key)) return new Palette(
                "ocean", "اقیانوسی",
                0xff155f82, 0xff0e3a55, 0xff0c8fa0, 0xff63cad7,
                0xffe3f2f6, 0xffdceff4, 0xffeaf6f9,
                0xff15384b, 0xff56707d, 0xffc2dde5);
        if ("violet".equals(key)) return new Palette(
                "violet", "بنفش آرام",
                0xff56457d, 0xff352850, 0xff6d5a9a, 0xffc6a6ff,
                0xfff0eaf5, 0xffe8e0f2, 0xfff1ebf8,
                0xff2d2740, 0xff6a6078, 0xffd4c7e2);
        if ("forest".equals(key)) return new Palette(
                "forest", "سبز جنگلی",
                0xff2f6658, 0xff1e433a, 0xff3d7b68, 0xff9accb8,
                0xffe8f2ec, 0xffdfeee6, 0xffeaf4ee,
                0xff213b33, 0xff5b7168, 0xffc9e0d3);
        if ("rose".equals(key)) return new Palette(
                "rose", "رز خاکی",
                0xff825366, 0xff553543, 0xff9b6277, 0xffe5a7be,
                0xfff5eaef, 0xfff1e0e6, 0xfff8eaef,
                0xff402b33, 0xff725f67, 0xffe3c8d2);
        if ("graphite".equals(key)) return new Palette(
                "graphite", "گرافیتی",
                0xff455a64, 0xff263238, 0xff607d8b, 0xff90a4ae,
                0xffeceff1, 0xffe3e8eb, 0xfff7f9fa,
                0xff263238, 0xff546e7a, 0xffcfd8dc);
        if ("sand".equals(key)) return new Palette(
                "sand", "شنی",
                0xff6d4c41, 0xff4e342e, 0xff8d6e63, 0xffd7b377,
                0xfff6f0e7, 0xffefe5d8, 0xfffffaf4,
                0xff4a3828, 0xff756657, 0xffdccfbe);
        if ("ice".equals(key)) return new Palette(
                "ice", "یخی",
                0xff2b5d73, 0xff183a49, 0xff4e8ea6, 0xff9acbdc,
                0xffe8f3f7, 0xffdcecf2, 0xfff2f9fb,
                0xff203b4a, 0xff5a6f78, 0xffc6dde5);
        if ("sunset".equals(key)) return new Palette(
                "sunset", "غروب",
                0xff8a4b3f, 0xff5e3129, 0xffa86150, 0xfff2a66d,
                0xfff8ece8, 0xfff3ded7, 0xfffff6f2,
                0xff4b302b, 0xff75615c, 0xffe7c9c0);
        return new Palette(
                DEFAULT, "ELUQEN",
                0xff203a63, 0xff122541, 0xff108b86, 0xffffb457,
                0xffeff4f9, 0xffedf3fb, 0xfffbfdff,
                0xff182b45, 0xff5f7188, 0xffdce5ed);
    }

    public static int indexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i;
        }
        return 0;
    }
}
