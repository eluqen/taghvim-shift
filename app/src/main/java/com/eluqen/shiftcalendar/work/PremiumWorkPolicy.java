package com.eluqen.shiftcalendar.work;

/**
 * Premium-day worked minutes (Friday/official holiday) are based on actual presence,
 * not nominal pattern duration.
 */
public final class PremiumWorkPolicy {
    private PremiumWorkPolicy() {}

    public static int workedMinutes(DayWorkResolver.State state) {
        if (state == null) return 0;
        int worked = 0;
        if (state.hasBaseWork()) {
            worked += state.baseWorkedMinutes();
        }
        if (state.hasOvertime) {
            worked += DayWorkResolver.durationMinutes(
                    state.overtimeStartMinute, state.overtimeEndMinute);
        }
        return Math.max(0, worked);
    }
}
