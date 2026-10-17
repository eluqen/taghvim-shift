package com.eluqen.shiftcalendar.ui;

/** Decide if a previously rendered settings view needs a new palette. */
final class SettingsThemeRefresh {
    private SettingsThemeRefresh() {}
    static boolean needsRepaint(String renderedKey, String savedKey) {
        return renderedKey != null && savedKey != null
                && !renderedKey.equals(savedKey);
    }
}
