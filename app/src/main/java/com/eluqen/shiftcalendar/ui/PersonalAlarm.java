package com.eluqen.shiftcalendar.ui;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public final class PersonalAlarm {
    public static final String ACTION = "com.eluqen.shiftcalendar.PERSONAL_ALERT";

    private PersonalAlarm() {
    }

    private static PendingIntent pending(Context context, int i, int i2) {
        return PendingIntent.getBroadcast(context, i + 100000, new Intent(context, (Class<?>) PersonalReceiver.class).setAction(ACTION).putExtra("id", i), i2 | android.app.PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancel(Context context, int i) {
        PendingIntent pendingIntentPending;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE);
        if (alarmManager == null || i <= 0 || (pendingIntentPending = pending(context, i, 536870912)) == null) {
            return;
        }
        alarmManager.cancel(pendingIntentPending);
        pendingIntentPending.cancel();
    }

    public static void syncOne(Context context, JSONObject jSONObject) {
        AlarmManager alarmManager;
        int iOptInt = jSONObject.optInt("id");
        cancel(context, iOptInt);
        long jOptLong = jSONObject.optLong("when", 0L);
        if (iOptInt <= 0 || jSONObject.optBoolean("done") || !jSONObject.optBoolean("alert") || jSONObject.optBoolean("fired") || jOptLong <= System.currentTimeMillis() + 1000 || (alarmManager = (AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE)) == null) {
            return;
        }
        PendingIntent pendingIntentPending = pending(context, iOptInt, 134217728);
        try {
            if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, jOptLong, pendingIntentPending);
            } else {
                alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, jOptLong, pendingIntentPending);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, jOptLong, pendingIntentPending);
        }
    }

    public static void reschedule(Context context) {
        JSONArray jSONArrayAll = PersonalStore.all(context);
        for (int i = 0; i < jSONArrayAll.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayAll.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                syncOne(context, jSONObjectOptJSONObject);
            }
        }
    }

    public static String channel(Context context, int i, String str, long j) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String str2 = "eluqen_personal_" + i + "_" + Math.abs((str + j).hashCode());
        if (notificationManager != null && Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(str2, "رخداد شخصی " + i, android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("زنگ مستقل برای یک یادآوری شخصی");
            notificationChannel.enableVibration(true);
            Uri uri = SoundPicker.uri(str);
            if ("silent".equals(str)) {
                notificationChannel.setSound(null, null);
            } else {
                notificationChannel.setSound(uri, new AudioAttributes.Builder().setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
            }
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return str2;
    }

    public static void fire(Context context, int i) {
        JSONObject jSONObject = PersonalStore.get(context, i);
        if (jSONObject == null || jSONObject.optBoolean("done") || !jSONObject.optBoolean("alert") || jSONObject.optBoolean("fired")) {
            return;
        }
        long jOptLong = jSONObject.optLong("when");
        if (jOptLong <= 0) {
            return;
        }
        String strOptString = jSONObject.optString("sound", "default_notification");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String strChannel = channel(context, i, strOptString, jSONObject.optLong("updated"));
        PendingIntent activity = PendingIntent.getActivity(context, 110000 + i, new Intent(context, (Class<?>) PlannerActivity.class).putExtra("focus", i).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP), android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, strChannel) : new Notification.Builder(context);
        builder.setSmallIcon(context.getResources().getIdentifier("ic_notification", "drawable", context.getPackageName())).setContentTitle(jSONObject.optString("title", "یادآوری شخصی")).setContentText(jSONObject.optString("note", "زمان انجام این کار رسیده است.")).setContentIntent(activity).setAutoCancel(true).setPriority(android.app.Notification.PRIORITY_HIGH).setCategory("reminder");
        if (Build.VERSION.SDK_INT < 26) {
            if ("silent".equals(strOptString)) {
                builder.setSound(null);
            } else {
                builder.setSound(SoundPicker.uri(strOptString));
            }
            builder.setVibrate(new long[]{0, 250, 160, 250});
        }
        notificationManager.notify(120000 + i, builder.build());
        PersonalStore.markFired(context, i, jOptLong);
    }
}
