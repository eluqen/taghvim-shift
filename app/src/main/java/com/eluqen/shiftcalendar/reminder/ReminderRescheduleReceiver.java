package com.eluqen.shiftcalendar.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.eluqen.shiftcalendar.data.Prefs;

public final class ReminderRescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        boolean supported = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action);
        if (!supported) {
            return;
        }
        if (Prefs.reminderEnabled(context)) {
            new Thread(ReminderRescheduleReceiver.createRunCallback0(context, goAsync()), "eluqen-shift-calendar-reminder-reschedule").start();
        }
    }

    static void handleOnReceiveCallback0(Context context, BroadcastReceiver.PendingResult pendingResult) {
        try {
            ReminderScheduler.sync(context);
        } finally {
            pendingResult.finish();
        }
    }

    private static Runnable createRunCallback0(final Context capture0, final BroadcastReceiver.PendingResult capture1) {
        return () -> {
        ReminderRescheduleReceiver.handleOnReceiveCallback0(capture0, capture1);
        };
    }
}
