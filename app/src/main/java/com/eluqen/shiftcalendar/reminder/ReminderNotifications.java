package com.eluqen.shiftcalendar.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;
import com.eluqen.shiftcalendar.R;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.ui.MainActivity;
import com.eluqen.shiftcalendar.ui.ShiftAudio;

public final class ReminderNotifications {
    private static final String CHANNEL_SILENT = "shift_reminder_silent";
    private static final String CHANNEL_SOUND = "shift_reminder_sound";
    private static final String CHANNEL_SOUND_VIBRATE = "shift_reminder_sound_vibrate";
    private static final String CHANNEL_VIBRATE = "shift_reminder_vibrate";

private ReminderNotifications() {
    }

    public static void ensureChannels(Context context) {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= 26 && (notificationManager = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)) != null) {
            notificationManager.createNotificationChannel(channel(CHANNEL_SOUND_VIBRATE, "یادآوری شیفت — صدا و لرزش", true, true));
            notificationManager.createNotificationChannel(channel(CHANNEL_SOUND, "یادآوری شیفت — فقط صدا", true, false));
            notificationManager.createNotificationChannel(channel(CHANNEL_VIBRATE, "یادآوری شیفت — فقط لرزش", false, true));
            notificationManager.createNotificationChannel(channel(CHANNEL_SILENT, "یادآوری شیفت — بی\u200cصدا", false, false));
        }
    }

    public static void show(Context context, Intent intent) {
        String str;
        Notification.Builder builder;
        String str2;
        int i;
        ensureChannels(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        int intExtra = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 41000);
        String strSafe = safe(intent.getStringExtra(ReminderScheduler.EXTRA_SHIFT_TITLE), "شیفت کاری");
        String str3 = "";
        String strSafe2 = safe(intent.getStringExtra(ReminderScheduler.EXTRA_SHIFT_TIME), "");
        String strSafe3 = safe(intent.getStringExtra(ReminderScheduler.EXTRA_DATE), "");
        String strSafe4 = safe(intent.getStringExtra(ReminderScheduler.EXTRA_PLAN), "");
        String customMessage = safe(
                intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE), "");
        String kind = safe(
                intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_KIND),
                ReminderScheduler.KIND_SHIFT);
        StringBuilder sb = new StringBuilder();
        sb.append(strSafe);
        if (strSafe2.isEmpty()) {
            str = "";
        } else {
            str = " ساعت " + PersianDigits.of(strSafe2);
        }
        sb.append(str);
        sb.append(" شروع می\u200cشود.");
        String string = customMessage.isEmpty() ? sb.toString() : customMessage;
        String notificationTitle = ReminderScheduler.KIND_OVERTIME.equals(kind)
                ? "یادآوری اضافه‌کاری"
                : "یادآوری شیفت";
        PendingIntent activity = PendingIntent.getActivity(context, intExtra, new Intent(context, (Class<?>) MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(ReminderScheduler.EXTRA_DATE, strSafe3).putExtra(MainActivity.EXTRA_SKIP_DEFAULT, true), (android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
        Intent action = new Intent(context, (Class<?>) ReminderActionReceiver.class).setAction(ReminderActionReceiver.ACTION_SNOOZE);
        copyExtras(intent, action);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, intExtra + 20000, action, (android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
        PendingIntent broadcast2 = PendingIntent.getBroadcast(context, 40000 + intExtra, new Intent(context, (Class<?>) ReminderActionReceiver.class).setAction(ReminderActionReceiver.ACTION_DISMISS).putExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, intExtra), (android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(context, ShiftAudio.channel(context, intent, selectedChannel(context)));
        } else {
            builder = new Notification.Builder(context);
        }
        Notification.Builder contentText = builder.setSmallIcon(R.drawable.ic_notification).setColor(Color.rgb(23, 126, 121)).setContentTitle(notificationTitle).setContentText(string);
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(string);
        if (strSafe3.isEmpty()) {
            str2 = "";
        } else {
            str2 = "\nتاریخ: " + PersianDigits.of(strSafe3);
        }
        sb2.append(str2);
        if (!strSafe4.isEmpty()) {
            str3 = "\nالگو: " + strSafe4;
        }
        sb2.append(str3);
        contentText.setStyle(bigTextStyle.bigText(sb2.toString())).setContentIntent(activity).setAutoCancel(true).setCategory("reminder").setVisibility(android.app.Notification.VISIBILITY_PRIVATE).setPriority(android.app.Notification.PRIORITY_HIGH).addAction(new Notification.Action.Builder(0, "۱۰ دقیقه بعد", broadcast).build()).addAction(new Notification.Action.Builder(0, "متوجه شدم", broadcast2).build());
        if (Build.VERSION.SDK_INT < 26) {
            int defaults = 0;
            if (Prefs.reminderSound(context)) defaults |= Notification.DEFAULT_SOUND;
            if (Prefs.reminderVibration(context)) defaults |= Notification.DEFAULT_VIBRATE;
            builder.setDefaults(defaults);
        }
        notificationManager.notify(intExtra, builder.build());
    }

    public static void showTest(Context context) {
        Intent intent = new Intent();
        intent.putExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 40999);
        intent.putExtra(ReminderScheduler.EXTRA_SHIFT_TITLE, "اعلان آزمایشی شیفت");
        intent.putExtra(ReminderScheduler.EXTRA_SHIFT_TIME, "07:00");
        intent.putExtra(ReminderScheduler.EXTRA_DATE, PersianDate.todayTehran().toString());
        intent.putExtra(ReminderScheduler.EXTRA_PLAN, "تنظیمات فعلی تقویم شیفت");
        show(context, intent);
    }

    @android.annotation.TargetApi(26)
    private static NotificationChannel channel(String str, String str2, boolean z, boolean z2) {
        NotificationChannel notificationChannel = new NotificationChannel(str, str2, android.app.NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription("اعلان\u200cهای کاربرمحور قبل از شروع نوبت کاری");
        notificationChannel.enableVibration(z2);
        if (!z) {
            notificationChannel.setSound(null, null);
        } else {
            notificationChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, new AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        }
        notificationChannel.setLockscreenVisibility(0);
        return notificationChannel;
    }

    private static String selectedChannel(Context context) {
        boolean zReminderSound = Prefs.reminderSound(context);
        boolean zReminderVibration = Prefs.reminderVibration(context);
        if (zReminderSound && zReminderVibration) {
            return CHANNEL_SOUND_VIBRATE;
        }
        if (zReminderSound) {
            return CHANNEL_SOUND;
        }
        if (zReminderVibration) {
            return CHANNEL_VIBRATE;
        }
        return CHANNEL_SILENT;
    }

    private static void copyExtras(Intent intent, Intent intent2) {
        intent2.putExtra(ReminderScheduler.EXTRA_DATE, intent.getStringExtra(ReminderScheduler.EXTRA_DATE));
        intent2.putExtra(ReminderScheduler.EXTRA_NOMINAL_DATE, intent.getStringExtra(ReminderScheduler.EXTRA_NOMINAL_DATE));
        intent2.putExtra(ReminderScheduler.EXTRA_TRIGGER_AT, intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER_AT, Long.MIN_VALUE));
        intent2.putExtra(ReminderScheduler.EXTRA_SHIFT_CODE, intent.getStringExtra(ReminderScheduler.EXTRA_SHIFT_CODE));
        intent2.putExtra(ReminderScheduler.EXTRA_SHIFT_TITLE, intent.getStringExtra(ReminderScheduler.EXTRA_SHIFT_TITLE));
        intent2.putExtra(ReminderScheduler.EXTRA_SHIFT_TIME, intent.getStringExtra(ReminderScheduler.EXTRA_SHIFT_TIME));
        intent2.putExtra(ReminderScheduler.EXTRA_PLAN, intent.getStringExtra(ReminderScheduler.EXTRA_PLAN));
        intent2.putExtra(ReminderScheduler.EXTRA_REMINDER_KIND, intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_KIND));
        intent2.putExtra(ReminderScheduler.EXTRA_MESSAGE, intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE));
        intent2.putExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 41000));
    }

    private static String safe(String str, String str2) {
        return (str == null || str.trim().isEmpty()) ? str2 : str.trim();
    }
}
