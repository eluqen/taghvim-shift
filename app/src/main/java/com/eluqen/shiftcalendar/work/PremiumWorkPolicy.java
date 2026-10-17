package com.eluqen.shiftcalendar.work;

/**
 * Premium-day worked minutes (Friday/official holiday) are based on actual presence,
 * not nominal pattern duration.
 */
public final class PremiumWorkPolicy {
    private PremiumWorkPolicy() {}

    /**
     * Whether a Friday/holiday belongs to the user's regular shift this month
     * after recorded absence overrides. Future scheduled dates count immediately.
     *
     * Any leave (full-day or hourly) excludes the day, as requested. A full rest
     * override also excludes it; partial rest counts if work time remains.
     * Overtime on a normally-off day never invents an eligible shift date.
     */
    public static boolean scheduledPremiumDayEligible(DayWorkResolver.State state) {
        return state != null
                && DayWorkResolver.validWork(state.baseProfile)
                && state.mode != DayWorkResolver.MODE_LEAVE
                && state.hasBaseWork()
                && state.baseWorkedMinutes() > 0;
    }

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
