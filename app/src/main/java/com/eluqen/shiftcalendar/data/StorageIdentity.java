package com.eluqen.shiftcalendar.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Canonical storage/runtime identifiers for the current ELUQEN Shift Calendar
 * identity, plus a narrow one-time compatibility bridge for pre-final builds.
 */
public final class StorageIdentity {
    public static final String GENERAL_PREFS =
            "eluqen_shift_calendar_preferences";
    public static final String SHIFT_TYPE_PREFS =
            "eluqen_shift_calendar_shift_types";
    public static final String DATABASE_NAME =
            "eluqen_shift_calendar.db";
    public static final String BACKUP_FORMAT =
            "eluqen-shift-calendar-backup";
    public static final String CALENDAR_UPDATE_WORK =
            "eluqen-shift-calendar-data-update";
    public static final String ICS_UID_DOMAIN =
            "shiftcalendar.eluqen.local";

    // Pre-final compatibility only. New runtime data must never be written
    // under these identifiers after migrateLegacyStorage() completes.
    static final String LEGACY_GENERAL_PREFS =
            "taghvimeshift_preferences";
    static final String LEGACY_SHIFT_TYPE_PREFS =
            "taghvimeshift_shift_types";
    static final String LEGACY_DATABASE_NAME =
            "taghvimeshift.db";
    public static final String LEGACY_BACKUP_FORMAT =
            "taghvimeshift-backup";
    public static final String LEGACY_CALENDAR_UPDATE_WORK =
            "taghvimeshift-calendar-data-update";

    private StorageIdentity() {}

    public static void migrateLegacyStorage(Context context) {
        Context app = context.getApplicationContext();
        migratePreferences(
                app, LEGACY_GENERAL_PREFS, GENERAL_PREFS);
        migratePreferences(
                app, LEGACY_SHIFT_TYPE_PREFS, SHIFT_TYPE_PREFS);
        migrateDatabase(
                app, LEGACY_DATABASE_NAME, DATABASE_NAME);
    }

    private static void migratePreferences(
            Context context, String legacyName, String currentName) {
        SharedPreferences legacy =
                context.getSharedPreferences(legacyName, 0);
        Map<String, ?> all = legacy.getAll();
        if (all == null || all.isEmpty()) return;

        SharedPreferences current =
                context.getSharedPreferences(currentName, 0);
        SharedPreferences.Editor editor = current.edit();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (current.contains(key)) continue;
            Object value = entry.getValue();
            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> strings = (Set<String>) value;
                editor.putStringSet(
                        key,
                        new java.util.HashSet<>(strings));
            }
        }

        if (!editor.commit()) {
            throw new IllegalStateException(
                    "Could not migrate ELUQEN preferences");
        }

        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(legacyName);
        } else {
            legacy.edit().clear().commit();
        }
    }

    private static void migrateDatabase(
            Context context, String legacyName, String currentName) {
        File legacy = context.getDatabasePath(legacyName);
        if (!legacy.exists()) return;

        File current = context.getDatabasePath(currentName);
        if (current.exists()) return;

        File parent = current.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create database directory");
        }
        if (!legacy.renameTo(current)) {
            throw new IllegalStateException(
                    "Could not migrate ELUQEN database");
        }

        migrateSidecar(legacy, current, "-wal");
        migrateSidecar(legacy, current, "-shm");
        migrateSidecar(legacy, current, "-journal");
    }

    private static void migrateSidecar(
            File legacy, File current, String suffix) {
        File oldSidecar =
                new File(legacy.getAbsolutePath() + suffix);
        if (!oldSidecar.exists()) return;
        File newSidecar =
                new File(current.getAbsolutePath() + suffix);
        if (!newSidecar.exists()) {
            oldSidecar.renameTo(newSidecar);
        }
    }
}
