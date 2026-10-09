package com.eluqen.shiftcalendar.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ShiftReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ReminderScheduler.ACTION_FIRE.equals(intent.getAction())) {
            return;
        }
        if (ReminderScheduler.isCurrent(context, intent)) {
            ReminderNotifications.show(context, intent);
        }
        new Thread(ShiftReminderReceiver.createRunCallback0(context, goAsync()), "eluqen-shift-calendar-reminder-refresh").start();
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
        ShiftReminderReceiver.handleOnReceiveCallback0(capture0, capture1);
        };
    }
}
