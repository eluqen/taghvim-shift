package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import com.eluqen.shiftcalendar.ui.ShiftOccurrence;

public final class OvertimePolicy {
    public static final String TYPE_REPLACEMENT = "REPLACEMENT";
    public static final String TYPE_NECESSITY = "NECESSITY";
    private OvertimePolicy() {}

    public static String typeTitle(String code) {
        return CardexRecord.overtimeTypeTitle(code);
    }
    public static String normalizeType(String code) {
        if (TYPE_REPLACEMENT.equals(code)) return TYPE_REPLACEMENT;
        if (TYPE_NECESSITY.equals(code)) return TYPE_NECESSITY;
        return "";
    }
    public static boolean isCoreShift(String code) {
        return "N".equals(code) || "D".equals(code) || "E".equals(code);
    }
    public static boolean targetAllowed(Profile base, Profile target) {
        if (!DayWorkResolver.validWork(target)) return false;
        if (base == null || !base.working) return isCoreShift(target.code);
        if (isCoreShift(base.code)) {
            return isCoreShift(target.code) && !target.code.equals(base.code);
        }
        return !target.code.equals(base.code);
    }
    public static boolean targetAllowed(
            String type, Profile base, Profile target) {
        if (TYPE_NECESSITY.equals(type)) {
            return necessityTargetAllowed(base, target);
        }
        return targetAllowed(base, target);
    }
    public static boolean necessityTargetAllowed(Profile base, Profile target) {
        if (!DayWorkResolver.validWork(target)) return false;
        if (!"D".equals(target.code) && !"E".equals(target.code)) return false;
        if (base == null || !base.working) return true;
        if ("D".equals(base.code)) return "E".equals(target.code);
        if ("E".equals(base.code)) return "D".equals(target.code);
        return true;
    }
    /**
     * New/editable overtime presence must stay inside the selected target shift
     * and last at least 60 minutes. The full target-shift range is valid.
     */
    public static boolean actualRangeAllowed(Profile target, int actualStart, int actualEnd) {
        if (!recordedRangeAllowed(target, actualStart, actualEnd)) return false;
        int actual = DayWorkResolver.durationMinutes(actualStart, actualEnd);
        return actual >= 60;
    }

    /**
     * Read-side compatibility for already-persisted overtime rows. Historical rows
     * that used the complete target shift remain visible after the stricter editor rule.
     */
    public static boolean recordedRangeAllowed(Profile target, int actualStart, int actualEnd) {
        if (!DayWorkResolver.validWork(target)) return false;
        if (actualStart < 0 || actualEnd < 0 || actualStart >= 1440 || actualEnd >= 1440
                || actualStart == actualEnd) return false;
        int full = DayWorkResolver.durationMinutes(target.startMinute, target.endMinute);
        if (full <= 0) return false;
        int actual = DayWorkResolver.durationMinutes(actualStart, actualEnd);
        if (actual <= 0 || actual > full) return false;
        int startDelta = forward(target.startMinute, actualStart);
        int endDelta = startDelta + actual;
        return startDelta >= 0 && startDelta < full && endDelta <= full;
    }
    public static int occurrenceOffset(ShiftPlan plan, PersianDate date, Profile target) {
        if (date == null || !DayWorkResolver.validWork(target)) return 0;
        if ("N".equals(target.code)) return -1;
        if ("D".equals(target.code) || "E".equals(target.code)) return 0;
        PersianDate start = (PersianDate) ShiftOccurrence.startDate(plan, date, target);
        long delta = start.toJdn() - date.toJdn();
        if (delta < -2) return -2;
        if (delta > 2) return 2;
        return (int) delta;
    }
    public static int actualStartOffset(
            ShiftPlan plan, PersianDate date, Profile target, int actualStart) {
        int offset = occurrenceOffset(plan, date, target);
        if (DayWorkResolver.validWork(target)
                && target.endMinute <= target.startMinute
                && actualStart >= 0
                && actualStart < target.startMinute) {
            offset++;
        }
        return offset;
    }
    private static int forward(int from, int to) {
        int value = (to - from) % 1440;
        return value < 0 ? value + 1440 : value;
    }
}
