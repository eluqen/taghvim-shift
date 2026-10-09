package com.eluqen.shiftcalendar.reminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ReminderActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DISMISS = "com.eluqen.shiftcalendar.action.REMINDER_DISMISS";
    public static final String ACTION_SNOOZE = "com.eluqen.shiftcalendar.action.REMINDER_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int intExtra = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 41000);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(intExtra);
        }
        if (ACTION_SNOOZE.equals(intent.getAction())) {
            ReminderScheduler.scheduleSnooze(context, intent, 10);
        }
    }
}
