package com.eluqen.shiftcalendar.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

/**
 * Persistent reminder tone policy.
 *
 * Global tone is the default. A shift type only stores a key when it explicitly
 * overrides the global tone; absence of that key means true inheritance.
 */
public final class ShiftAudio {
    private static final String FILE = "eluqen_shift_audio";
    private static final String GLOBAL = "global";
    private static final String TYPE_PREFIX = "type_";

    private ShiftAudio() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, 0);
    }

    public static String globalChoice(Context context) {
        return prefs(context).getString(GLOBAL, "default_notification");
    }

    public static void setGlobalChoice(Context context, String choice) {
        prefs(context).edit()
                .putString(GLOBAL, normalize(choice))
                .apply();
    }

    public static boolean hasTypeOverride(Context context, String code) {
        return prefs(context).contains(TYPE_PREFIX + code);
    }

    public static String typeChoice(Context context, String code) {
        return prefs(context).getString(
                TYPE_PREFIX + code, globalChoice(context));
    }

    public static String effectiveChoice(Context context, String code) {
        return hasTypeOverride(context, code)
                ? typeChoice(context, code)
                : globalChoice(context);
    }

    public static void setTypeChoice(
            Context context, String code, String choice) {
        prefs(context).edit()
                .putString(TYPE_PREFIX + code, normalize(choice))
                .apply();
    }

    public static void clearTypeChoice(Context context, String code) {
        prefs(context).edit().remove(TYPE_PREFIX + code).apply();
    }

    public static int overrideCount(Context context) {
        int count = 0;
        for (String code : ShiftExtras.visibleCodes(context)) {
            if (hasTypeOverride(context, code)) count++;
        }
        return count;
    }

    public static String typeSummary(Context context, String code) {
        if (!hasTypeOverride(context, code)) {
            return "صدای عمومی · "
                    + SoundPicker.title(globalChoice(context));
        }
        return "صدای مستقل · "
                + SoundPicker.title(typeChoice(context, code));
    }

    public static String channel(
            Context context, Intent event, String fallback) {
        String code = event == null
                ? "" : safe(event.getStringExtra("shift_code"));
        boolean typeOverride =
                !code.isEmpty() && hasTypeOverride(context, code);
        String choice = typeOverride
                ? typeChoice(context, code)
                : globalChoice(context);
        String channelScope = typeOverride ? code : "global";

        SharedPreferences options =
                context.getSharedPreferences(
                        "eluqen_shift_calendar_preferences", 0);
        if (!options.getBoolean("reminder_sound", true)
                || "default_notification".equals(choice)) {
            return fallback;
        }

        boolean vibrate =
                options.getBoolean("reminder_vibration", true);
        String key = dynamicChannelKey(channelScope, choice, vibrate);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel =
                        new NotificationChannel(
                                key,
                                "یادآوری شیفت - زنگ انتخابی",
                                NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(
                        "صدای انتخابی کاربر برای یادآوری شیفت");
                channel.enableVibration(vibrate);

                Uri sound = SoundPicker.uri(choice);
                if ("silent".equals(choice)) {
                    channel.setSound(null, null);
                } else {
                    channel.setSound(
                            sound,
                            new AudioAttributes.Builder()
                                    .setContentType(
                                            AudioAttributes
                                                    .CONTENT_TYPE_SONIFICATION)
                                    .setUsage(
                                            AudioAttributes
                                                    .USAGE_NOTIFICATION_RINGTONE)
                                    .build());
                }
                manager.createNotificationChannel(channel);
            }
        }
        return key;
    }

    static String dynamicChannelKey(
            String code, String choice, boolean vibrate) {
        return "eluqen_shift_tone_"
                + Math.abs((safe(code)
                + "|" + normalize(choice)
                + "|v=" + vibrate).hashCode());
    }

    private static String normalize(String choice) {
        if (choice == null || choice.trim().isEmpty()) {
            return "default_notification";
        }
        return choice.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
