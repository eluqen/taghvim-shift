package com.eluqen.shiftcalendar.update;

import android.content.Context;

/**
 * Compatibility bridge retained for existing call sites. Scheduling is now
 * implemented by WorkManager through CalendarUpdateScheduler.
 */
public final class DataUpdateJobService {
    private DataUpdateJobService() {}

    public static void syncSchedule(Context context) {
        CalendarUpdateScheduler.syncSchedule(context);
    }
}
