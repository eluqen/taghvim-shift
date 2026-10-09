package com.eluqen.shiftcalendar.work;

import android.content.Context;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

/**
 * Cross-day fatigue guard for operational work records.
 *
 * Business rule supplied by owner:
 * - no more than 16 hours of actual work may accumulate without a real rest break;
 * - once that limit is reached, another work block requires at least 8 hours rest.
 *
 * The rule intentionally spans nominal day boundaries because night shift N belongs to
 * a nominal day while normally starting on the previous civil evening.
 */
public final class WorkRestPolicy {
    public static final long HOUR_MS = 60L * 60L * 1000L;
    public static final long MAX_WORK_MS = 16L * HOUR_MS;
    public static final long MIN_REST_MS = 8L * HOUR_MS;

    private WorkRestPolicy() {}

    public static final class Interval {
        public final long start;
        public final long end;
        public final boolean candidate;

        public Interval(long start, long end, boolean candidate) {
            this.start = start;
            this.end = end;
            this.candidate = candidate;
        }

        long duration() { return Math.max(0L, end - start); }
    }

    public static boolean canSave(
            Context context,
            AppDatabase database,
            ShiftPlan plan,
            PersianDate nominalDate,
            CardexRecord candidateRecord) {
        if (context == null || database == null || plan == null
                || nominalDate == null || candidateRecord == null) {
            return true;
        }

        TimeZone zone = TimeZone.getTimeZone("Asia/Tehran");
        DayWorkResolver.State candidateState =
                DayWorkResolver.resolve(context, nominalDate, plan, candidateRecord);
        if (!candidateState.hasOvertime) return true;

        long candidateStart = candidateState.overtimeStartEpoch(context, zone);
        long candidateEnd = candidateState.overtimeEndEpoch(context, zone);
        if (candidateEnd <= candidateStart) return false;

        List<Interval> intervals = new ArrayList<>();
        for (int delta = -2; delta <= 2; delta++) {
            PersianDate day = nominalDate.plusDays(delta);
            CardexRecord row = day.equals(nominalDate)
                    ? candidateRecord
                    : database.cardex(day.toString());
            DayWorkResolver.State state =
                    DayWorkResolver.resolve(context, day, plan, row);
            addStateIntervals(
                    intervals,
                    state,
                    context,
                    zone,
                    day.equals(nominalDate),
                    candidateStart,
                    candidateEnd);
        }
        return respectsRestWindow(intervals);
    }

    /**
     * Used by the editor while building the list of shift choices. It tests the
     * normal/full interval of a possible target before displaying it.
     */
    public static boolean fullShiftChoiceAllowed(
            Context context,
            AppDatabase database,
            ShiftPlan plan,
            PersianDate nominalDate,
            CardexRecord sourceRecord,
            ShiftPreferences.Profile target,
            String overtimeType) {
        if (!DayWorkResolver.validWork(target)) return false;
        CardexRecord candidate =
                sourceRecord == null ? new CardexRecord() : sourceRecord.copy();
        candidate.date = nominalDate.toString();
        candidate.jdn = nominalDate.toJdn();
        candidate.clearOperational();
        candidate.overtime = true;
        candidate.overtimeShiftCode = target.code;
        candidate.overtimeStart =
                ShiftPreferences.formatMinute(target.startMinute);
        candidate.overtimeEnd =
                ShiftPreferences.formatMinute(target.endMinute);
        candidate.overtimeDayOffset =
                OvertimePolicy.actualStartOffset(
                        plan, nominalDate, target, target.startMinute);
        candidate.overtimeType = OvertimePolicy.normalizeType(overtimeType);
        return canSave(context, database, plan, nominalDate, candidate);
    }

    private static void addStateIntervals(
            List<Interval> out,
            DayWorkResolver.State state,
            Context context,
            TimeZone zone,
            boolean candidateNominalDate,
            long candidateStart,
            long candidateEnd) {
        if (state == null) return;

        if (state.hasBaseWork()) {
            /*
             * Fatigue accounting follows organizational shift ownership, not the
             * calendar label of the Cardex row. Core N/shift-1 belongs to the
             * nominal day but physically starts on the previous evening.
             *
             * Do not delegate this safety rule to company/preset presentation
             * semantics in ShiftOccurrence: otherwise a valid N row can be
             * placed one civil day too late and a real E -> N -> D three-shift
             * chain can slip through the 16h/8h guard.
             */
            long start = fatigueBaseStartEpoch(state, zone);
            long end = fatigueBaseEndEpoch(state, zone, start);
            if (end > start) {
                if (state.isHourlyAbsence() && state.absenceMinutes() > 0) {
                    int baseMinute = state.effectiveProfile.startMinute;
                    int absenceStart = state.absenceStartMinute();
                    int offsetMinutes = absenceStart >= baseMinute
                            ? absenceStart - baseMinute
                            : 1440 - baseMinute + absenceStart;
                    long gapStart = start + offsetMinutes * 60000L;
                    long gapEnd = gapStart + state.absenceMinutes() * 60000L;
                    if (gapStart > start) out.add(new Interval(start, gapStart, false));
                    if (gapEnd < end) out.add(new Interval(gapEnd, end, false));
                } else {
                    out.add(new Interval(start, end, false));
                }
            }
        }

        if (state.hasOvertime) {
            long start = state.overtimeStartEpoch(context, zone);
            long end = state.overtimeEndEpoch(context, zone);
            if (end > start) {
                boolean candidate = candidateNominalDate
                        && start == candidateStart
                        && end == candidateEnd;
                out.add(new Interval(start, end, candidate));
            }
        }
    }

    static long fatigueBaseStartEpoch(
            DayWorkResolver.State state, TimeZone zone) {
        if (state == null || !state.hasBaseWork()) return Long.MIN_VALUE / 4;
        int offset = OvertimePolicy.occurrenceOffset(
                state.plan, state.date, state.effectiveProfile);
        return DayWorkResolver.civilEpoch(
                state.date.plusDays(offset),
                state.effectiveProfile.startMinute,
                zone);
    }

    static long fatigueBaseEndEpoch(
            DayWorkResolver.State state, TimeZone zone, long startEpoch) {
        if (state == null || !state.hasBaseWork()) return Long.MIN_VALUE / 4;
        int duration = DayWorkResolver.durationMinutes(
                state.effectiveProfile.startMinute,
                state.effectiveProfile.endMinute);
        return duration <= 0 ? startEpoch : startEpoch + duration * 60L * 1000L;
    }

    /**
     * Pure interval checker, intentionally public for deterministic unit tests.
     * A rest gap of 8h or more starts a fresh work block.
     * Gaps shorter than 8h do not reset accumulated actual work.
     */
    public static boolean respectsRestWindow(List<Interval> source) {
        if (source == null || source.isEmpty()) return true;

        List<Interval> intervals = new ArrayList<>();
        for (Interval item : source) {
            if (item != null && item.end > item.start) intervals.add(item);
        }
        if (intervals.isEmpty()) return true;

        Collections.sort(intervals, new Comparator<Interval>() {
            @Override public int compare(Interval a, Interval b) {
                return Long.compare(a.start, b.start);
            }
        });

        long cumulativeWork = 0L;
        long lastEnd = Long.MIN_VALUE;
        boolean chainContainsCandidate = false;

        for (Interval item : intervals) {
            if (lastEnd == Long.MIN_VALUE
                    || item.start - lastEnd >= MIN_REST_MS) {
                cumulativeWork = item.duration();
                lastEnd = item.end;
                chainContainsCandidate = item.candidate;
            } else {
                chainContainsCandidate |= item.candidate;
                if (item.start >= lastEnd) {
                    cumulativeWork += item.duration();
                    lastEnd = item.end;
                } else if (item.end > lastEnd) {
                    cumulativeWork += item.end - lastEnd;
                    lastEnd = item.end;
                }
            }

            if (chainContainsCandidate && cumulativeWork > MAX_WORK_MS) {
                return false;
            }
        }
        return true;
    }
}
