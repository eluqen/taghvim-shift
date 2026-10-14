package com.eluqen.shiftcalendar.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.eluqen.shiftcalendar.update.InstallSourceDetector;
import com.eluqen.shiftcalendar.update.UpdateSource;

public final class Prefs {
    private static final String AUTO_APP_UPDATE = "auto_app_update";
    private static final String AUTO_DATA_UPDATE = "auto_data_update";
    private static final String DEFAULT_SCREEN = "default_screen";
    private static final String EXCLUDE_LUNCH_HOUR = "exclude_lunch_hour";
    private static final String FILE = StorageIdentity.GENERAL_PREFS;
    private static final String HIJRI_OFFSET = "hijri_offset";
    private static final String INITIAL_SOURCE_KEY = "initial_source_key";
    private static final String INITIAL_SOURCE_LABEL = "initial_source_label";
    private static final String INITIAL_SOURCE_PACKAGE = "initial_source_package";
    private static final String INITIAL_SOURCE_SAVED = "initial_source_saved";
    private static final String LAST_APP_CHECK = "last_app_check";
    private static final String LAST_DATA_CHECK = "last_data_check";
    private static final String LAST_DATA_STATUS = "last_data_status";
    private static final String LAST_DATA_SUCCESS = "last_data_success";
    private static final String LAST_REMINDER_SCHEDULE = "last_reminder_schedule";
    private static final String LAST_REMINDER_STATUS = "last_reminder_status";
    public static final String NETWORK_ANY = "any";
    public static final String NETWORK_ASK = "ask";
    public static final String NETWORK_WIFI = "wifi";
    private static final String ONBOARDING = "onboarding_complete";
    private static final String PENDING_APP_CHANGELOG = "pending_app_changelog";
    private static final String PENDING_APP_DOWNLOAD_URL = "pending_app_download_url";
    private static final String PENDING_APP_VERSION_CODE = "pending_app_version_code";
    private static final String PENDING_APP_VERSION_NAME = "pending_app_version_name";
    private static final String REMINDER_ENABLED = "reminder_enabled";
    private static final String REMINDER_LEAD_MINUTES = "reminder_lead_minutes";
    private static final String REMINDER_SOUND = "reminder_sound";
    private static final String REMINDER_VIBRATION = "reminder_vibration";
    private static final String REMINDER_ZONE = "reminder_zone";
    public static final String REMINDER_ZONE_DEVICE = "device";
    public static final String REMINDER_ZONE_TEHRAN = "tehran";
    public static final String SCREEN_CALENDAR = "calendar";
    public static final String SCREEN_CARDEX = "cardex";
    public static final String SCREEN_FEATURES = "features";
    public static final String SCREEN_FOUR_SHIFT = "four_shift";
    public static final String SCREEN_GRAPHIC = "graphic";
    private static final String UPDATE_NETWORK = "update_network";
    private static final String UPDATE_SOURCE = "update_source";
    public static final String WIDGET_DARK = "dark";
    public static final String WIDGET_LIGHT = "light";
    private static final String WIDGET_SHOW_GROUPS = "widget_show_groups";
    private static final String WIDGET_SHOW_NEXT = "widget_show_next";
    public static final String WIDGET_TEAL = "teal";
    private static final String WIDGET_THEME = "widget_theme";

    public static UpdateSource updateSource(Context context) {
        ensureInitialInstallSource(context);
        SharedPreferences sharedPreferencesValues = values(context);
        return UpdateSource.fromKey(
                sharedPreferencesValues.getString(UPDATE_SOURCE, ""),
                UpdateSource.DIRECT);
    }

    private Prefs() {
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(FILE, 0);
    }

    public static int hijriManualOffset(Context context) {
        return Math.max(-2, Math.min(2, values(context).getInt(HIJRI_OFFSET, 0)));
    }

    public static void setHijriManualOffset(Context context, int offset) {
        values(context).edit().putInt(
                HIJRI_OFFSET,
                Math.max(-2, Math.min(2, offset))).apply();
    }

    /**
     * Compatibility aliases. The persisted key remains hijri_offset so existing
     * installs and backups keep their user-selected manual correction.
     */
    public static int hijriOffset(Context context) {
        return hijriManualOffset(context);
    }

    public static void setHijriOffset(Context context, int offset) {
        setHijriManualOffset(context, offset);
    }

    public static boolean onboardingComplete(Context context) {
        return values(context).getBoolean(ONBOARDING, false);
    }

    public static void setOnboardingComplete(Context context, boolean z) {
        values(context).edit().putBoolean(ONBOARDING, z).apply();
    }

    public static void ensureInitialInstallSource(Context context) {
        SharedPreferences sharedPreferencesValues = values(context);
        if (sharedPreferencesValues.getBoolean(INITIAL_SOURCE_SAVED, false)) {
            return;
        }
        InstallSourceDetector.Detection detectionDetect = InstallSourceDetector.detect(context);
        sharedPreferencesValues.edit().putBoolean(INITIAL_SOURCE_SAVED, true).putString(INITIAL_SOURCE_LABEL, detectionDetect.label).putString(INITIAL_SOURCE_PACKAGE, detectionDetect.packageName).putString(INITIAL_SOURCE_KEY, detectionDetect.suggestedSource.key).putString(UPDATE_SOURCE, detectionDetect.suggestedSource.key).apply();
    }

    public static String initialInstallSourceLabel(Context context) {
        ensureInitialInstallSource(context);
        return values(context).getString(INITIAL_SOURCE_LABEL, "منبع نامشخص");
    }

    public static String initialInstallSourcePackage(Context context) {
        ensureInitialInstallSource(context);
        return values(context).getString(INITIAL_SOURCE_PACKAGE, "");
    }

    public static void setUpdateSource(Context context, UpdateSource updateSource) {
        values(context).edit().putString(UPDATE_SOURCE, updateSource.key).apply();
    }

    public static boolean autoAppUpdate(Context context) {
        return values(context).getBoolean(AUTO_APP_UPDATE, true);
    }

    public static void setAutoAppUpdate(Context context, boolean z) {
        values(context).edit().putBoolean(AUTO_APP_UPDATE, z).apply();
    }

    public static boolean autoDataUpdate(Context context) {
        return values(context).getBoolean(AUTO_DATA_UPDATE, true);
    }

    public static void setAutoDataUpdate(Context context, boolean z) {
        values(context).edit().putBoolean(AUTO_DATA_UPDATE, z).apply();
    }

    public static String updateNetwork(Context context) {
        String string = values(context).getString(UPDATE_NETWORK, NETWORK_ANY);
        return (NETWORK_WIFI.equals(string) || NETWORK_ASK.equals(string)) ? string : NETWORK_ANY;
    }

    public static void setUpdateNetwork(Context context, String str) {
        if (!NETWORK_WIFI.equals(str) && !NETWORK_ASK.equals(str)) {
            str = NETWORK_ANY;
        }
        values(context).edit().putString(UPDATE_NETWORK, str).apply();
    }

    public static long lastDataCheck(Context context) {
        return values(context).getLong(LAST_DATA_CHECK, 0L);
    }

    public static long lastDataSuccess(Context context) {
        return values(context).getLong(LAST_DATA_SUCCESS, 0L);
    }

    public static String lastDataStatus(Context context) {
        return values(context).getString(LAST_DATA_STATUS, "هنوز بررسی نشده است");
    }

    public static void recordDataCheck(Context context, String str, boolean z) {
        SharedPreferences.Editor editorPutLong = values(context).edit().putLong(LAST_DATA_CHECK, System.currentTimeMillis());
        if (str == null) {
            str = "";
        }
        SharedPreferences.Editor editorPutString = editorPutLong.putString(LAST_DATA_STATUS, str);
        if (z) {
            editorPutString.putLong(LAST_DATA_SUCCESS, System.currentTimeMillis());
        }
        editorPutString.apply();
    }

    public static long lastAppCheck(Context context) {
        return values(context).getLong(LAST_APP_CHECK, 0L);
    }

    public static void setLastAppCheck(Context context, long j) {
        values(context).edit().putLong(LAST_APP_CHECK, j).apply();
    }

    public static void setPendingAppUpdate(Context context, int i, String str, String str2, String str3) {
        SharedPreferences.Editor editorPutInt = values(context).edit().putInt(PENDING_APP_VERSION_CODE, i);
        if (str == null) {
            str = "";
        }
        SharedPreferences.Editor editorPutString = editorPutInt.putString(PENDING_APP_VERSION_NAME, str);
        if (str2 == null) {
            str2 = "";
        }
        SharedPreferences.Editor editorPutString2 = editorPutString.putString(PENDING_APP_CHANGELOG, str2);
        if (str3 == null) {
            str3 = "";
        }
        editorPutString2.putString(PENDING_APP_DOWNLOAD_URL, str3).apply();
    }

    public static void clearPendingAppUpdate(Context context) {
        values(context).edit().remove(PENDING_APP_VERSION_CODE).remove(PENDING_APP_VERSION_NAME).remove(PENDING_APP_CHANGELOG).remove(PENDING_APP_DOWNLOAD_URL).apply();
    }

    public static int pendingAppVersionCode(Context context) {
        return values(context).getInt(PENDING_APP_VERSION_CODE, 0);
    }

    public static String pendingAppVersionName(Context context) {
        return values(context).getString(PENDING_APP_VERSION_NAME, "");
    }

    public static String pendingAppChangelog(Context context) {
        return values(context).getString(PENDING_APP_CHANGELOG, "");
    }

    public static String pendingAppDownloadUrl(Context context) {
        return values(context).getString(PENDING_APP_DOWNLOAD_URL, "");
    }

    public static String defaultScreen(Context context) {
        String string = values(context).getString(DEFAULT_SCREEN, SCREEN_CALENDAR);
        if (SCREEN_GRAPHIC.equals(string)) {
            string = "settings";
        }
        return (SCREEN_FEATURES.equals(string) || SCREEN_FOUR_SHIFT.equals(string) || SCREEN_CARDEX.equals(string) || "settings".equals(string)) ? string : SCREEN_CALENDAR;
    }

    public static void setDefaultScreen(Context context, String str) {
        if (SCREEN_GRAPHIC.equals(str)) {
            str = "settings";
        }
        if (!SCREEN_FEATURES.equals(str) && !SCREEN_FOUR_SHIFT.equals(str) && !SCREEN_CARDEX.equals(str) && !"settings".equals(str)) {
            str = SCREEN_CALENDAR;
        }
        values(context).edit().putString(DEFAULT_SCREEN, str).apply();
    }

    public static boolean excludeLunchHour(Context context) {
        return values(context).getBoolean(EXCLUDE_LUNCH_HOUR, false);
    }

    public static void setExcludeLunchHour(Context context, boolean z) {
        values(context).edit().putBoolean(EXCLUDE_LUNCH_HOUR, z).apply();
    }

    public static boolean reminderEnabled(Context context) {
        return values(context).getBoolean(REMINDER_ENABLED, false);
    }

    public static void setReminderEnabled(Context context, boolean z) {
        values(context).edit().putBoolean(REMINDER_ENABLED, z).apply();
    }

    public static int reminderLeadMinutes(Context context) {
        return Math.max(0, Math.min(1440, values(context).getInt(REMINDER_LEAD_MINUTES, 30)));
    }

    public static void setReminderLeadMinutes(Context context, int i) {
        values(context).edit().putInt(REMINDER_LEAD_MINUTES, Math.max(0, Math.min(1440, i))).apply();
    }

    public static boolean reminderSound(Context context) {
        return values(context).getBoolean(REMINDER_SOUND, true);
    }

    public static void setReminderSound(Context context, boolean z) {
        values(context).edit().putBoolean(REMINDER_SOUND, z).apply();
    }

    public static boolean reminderVibration(Context context) {
        return values(context).getBoolean(REMINDER_VIBRATION, true);
    }

    public static void setReminderVibration(Context context, boolean z) {
        values(context).edit().putBoolean(REMINDER_VIBRATION, z).apply();
    }

    public static String reminderZone(Context context) {
        return REMINDER_ZONE_TEHRAN.equals(values(context).getString(REMINDER_ZONE, REMINDER_ZONE_DEVICE)) ? REMINDER_ZONE_TEHRAN : REMINDER_ZONE_DEVICE;
    }

    public static void setReminderZone(Context context, String str) {
        SharedPreferences.Editor editorEdit = values(context).edit();
        String str2 = REMINDER_ZONE_TEHRAN;
        if (!REMINDER_ZONE_TEHRAN.equals(str)) {
            str2 = REMINDER_ZONE_DEVICE;
        }
        editorEdit.putString(REMINDER_ZONE, str2).apply();
    }

    public static long lastReminderSchedule(Context context) {
        return values(context).getLong(LAST_REMINDER_SCHEDULE, 0L);
    }

    public static String lastReminderStatus(Context context) {
        return values(context).getString(LAST_REMINDER_STATUS, "هنوز زمان\u200cبندی نشده است");
    }

    public static void recordReminderSchedule(Context context, String str) {
        SharedPreferences.Editor editorPutLong = values(context).edit().putLong(LAST_REMINDER_SCHEDULE, System.currentTimeMillis());
        if (str == null) {
            str = "";
        }
        editorPutLong.putString(LAST_REMINDER_STATUS, str).apply();
    }

    public static String widgetTheme(Context context) {
        String string = values(context).getString(WIDGET_THEME, WIDGET_DARK);
        return (WIDGET_LIGHT.equals(string) || WIDGET_TEAL.equals(string)) ? string : WIDGET_DARK;
    }

    public static void setWidgetTheme(Context context, String str) {
        if (!WIDGET_LIGHT.equals(str) && !WIDGET_TEAL.equals(str)) {
            str = WIDGET_DARK;
        }
        values(context).edit().putString(WIDGET_THEME, str).apply();
    }

    public static boolean widgetShowGroups(Context context) {
        return values(context).getBoolean(WIDGET_SHOW_GROUPS, true);
    }

    public static void setWidgetShowGroups(Context context, boolean z) {
        values(context).edit().putBoolean(WIDGET_SHOW_GROUPS, z).apply();
    }

    public static boolean widgetShowNext(Context context) {
        return values(context).getBoolean(WIDGET_SHOW_NEXT, true);
    }

    public static void setWidgetShowNext(Context context, boolean z) {
        values(context).edit().putBoolean(WIDGET_SHOW_NEXT, z).apply();
    }

}
