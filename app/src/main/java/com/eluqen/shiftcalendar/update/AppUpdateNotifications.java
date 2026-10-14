package com.eluqen.shiftcalendar.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import com.eluqen.shiftcalendar.R;
import com.eluqen.shiftcalendar.ui.UpdateCenterActivity;

final class AppUpdateNotifications {
    private static final String CHANNEL = "direct_app_updates";
    private static final int NOTIFICATION_ID = 47001;

    private AppUpdateNotifications() {}

    static void show(Context context, AppUpdateManager.Result result) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL,
                    "به‌روزرسانی برنامه",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(
                    "اطلاع از انتشار نسخه جدید تقویم شیفت");
            manager.createNotificationChannel(channel);
        }

        Intent centerIntent = new Intent(context, UpdateCenterActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent center = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                centerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);

        StringBuilder detail = new StringBuilder(
                "نسخه " + result.versionName + " آماده دریافت است.");
        if (!result.changelog.trim().isEmpty()) {
            detail.append("\n").append(result.changelog.trim());
        }

        builder.setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(23, 126, 121))
                .setContentTitle("نسخه جدید تقویم شیفت آماده است")
                .setContentText(
                        "نسخه " + result.versionName + " آماده دریافت است.")
                .setStyle(new Notification.BigTextStyle().bigText(detail.toString()))
                .setContentIntent(center)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(
                        0, "مرکز به‌روزرسانی", center).build());

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
