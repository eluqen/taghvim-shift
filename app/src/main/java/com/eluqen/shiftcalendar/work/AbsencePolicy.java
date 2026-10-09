package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.ShiftPreferences;

/** Rules shared by hourly leave and hourly compensatory rest. */
public final class AbsencePolicy {
    private AbsencePolicy() {}

    public static boolean hourlyRangeAllowed(
            ShiftPreferences.Profile base, int start, int end) {
        if (!DayWorkResolver.validWork(base)) return false;
        if (start < 0 || end < 0 || start == end) return false;

        int shift = DayWorkResolver.durationMinutes(
                base.startMinute, base.endMinute);
        int absence = DayWorkResolver.durationMinutes(start, end);
        if (absence <= 0 || absence >= shift) return false;

        int relStart = forward(base.startMinute, start);
        int relEnd = relStart + absence;
        return relStart >= 0 && relStart < shift && relEnd <= shift;
    }

    public static int durationMinutes(
            boolean fullShift,
            ShiftPreferences.Profile base,
            String start,
            String end) {
        if (fullShift) {
            return DayWorkResolver.validWork(base)
                    ? DayWorkResolver.durationMinutes(
                            base.startMinute, base.endMinute)
                    : 0;
        }
        int from = DayWorkResolver.parseMinute(start);
        int to = DayWorkResolver.parseMinute(end);
        return hourlyRangeAllowed(base, from, to)
                ? DayWorkResolver.durationMinutes(from, to) : 0;
    }

    private static int forward(int origin, int minute) {
        return minute >= origin ? minute - origin : 1440 - origin + minute;
    }
}
