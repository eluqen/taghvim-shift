package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import java.util.List;

/** Company-specific Cardex limits that must be enforced consistently in UI and persistence. */
public final class CardexCompanyPolicy {
    private static final int ZOB_MONTHLY_SWAP_LIMIT = 2;
    private static final int ZOB_MONTHLY_HOURLY_LEAVE_LIMIT = 2;
    private static final int ZOB_MONTHLY_HOURLY_REST_LIMIT = 2;

    private CardexCompanyPolicy() {}

    public static boolean isZobAhan(ShiftPlan plan) {
        if (plan == null || plan.company == null) return false;
        String company = plan.company
                .replace('\u200c', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        return "ذوب آهن".equals(company);
    }

    public static boolean swapOptionAllowed(
            AppDatabase db, PersianDate date, ShiftPlan plan, CardexRecord current) {
        if (!isZobAhan(plan)) return true;
        return existingOrBelowLimit(
                db, date, current,
                ZOB_MONTHLY_SWAP_LIMIT,
                CardexCompanyPolicy::isSwap);
    }

    public static boolean hourlyLeaveAllowed(
            AppDatabase db, PersianDate date, ShiftPlan plan, CardexRecord current) {
        if (!isZobAhan(plan)) return true;
        return existingOrBelowLimit(
                db, date, current,
                ZOB_MONTHLY_HOURLY_LEAVE_LIMIT,
                CardexCompanyPolicy::isHourlyLeave);
    }

    public static boolean hourlyRestAllowed(
            AppDatabase db, PersianDate date, ShiftPlan plan, CardexRecord current) {
        if (!isZobAhan(plan)) return true;
        return existingOrBelowLimit(
                db, date, current,
                ZOB_MONTHLY_HOURLY_REST_LIMIT,
                CardexCompanyPolicy::isHourlyRest);
    }

    /**
     * Returns a Persian validation message when a new mutation would exceed
     * a Zob Ahan monthly limit. Existing/legacy over-limit records may be edited
     * without increasing the number of limited records.
     */
    public static String validationError(
            AppDatabase db, PersianDate date, ShiftPlan plan, CardexRecord replacement) {
        if (!isZobAhan(plan) || replacement == null) return null;
        CardexRecord before = db.cardex(date.toString());

        if (wouldIncreasePastLimit(
                db, date, before, replacement,
                ZOB_MONTHLY_SWAP_LIMIT,
                CardexCompanyPolicy::isSwap)) {
            return "در شرکت ذوب آهن سقف تعویض شیفت در هر ماه ۲ بار است";
        }
        if (wouldIncreasePastLimit(
                db, date, before, replacement,
                ZOB_MONTHLY_HOURLY_LEAVE_LIMIT,
                CardexCompanyPolicy::isHourlyLeave)) {
            return "در شرکت ذوب آهن سقف مرخصی ساعتی در هر ماه ۲ بار است";
        }
        if (wouldIncreasePastLimit(
                db, date, before, replacement,
                ZOB_MONTHLY_HOURLY_REST_LIMIT,
                CardexCompanyPolicy::isHourlyRest)) {
            return "در شرکت ذوب آهن سقف استراحت ساعتی در هر ماه ۲ بار است";
        }
        return null;
    }

    interface Matcher {
        boolean matches(CardexRecord record);
    }

    static boolean transitionAllowed(
            int otherCount, boolean beforeMatches, boolean afterMatches, int limit) {
        if (!afterMatches || afterMatches == beforeMatches) return true;
        return otherCount + 1 <= limit;
    }

    private static boolean existingOrBelowLimit(
            AppDatabase db, PersianDate date, CardexRecord current,
            int limit, Matcher matcher) {
        if (current != null && matcher.matches(current)) return true;
        return countOtherMonthRecords(db, date, matcher) < limit;
    }

    private static boolean wouldIncreasePastLimit(
            AppDatabase db, PersianDate date,
            CardexRecord before, CardexRecord after,
            int limit, Matcher matcher) {
        int others = countOtherMonthRecords(db, date, matcher);
        return !transitionAllowed(
                others,
                before != null && matcher.matches(before),
                matcher.matches(after),
                limit);
    }

    private static int countOtherMonthRecords(
            AppDatabase db, PersianDate date, Matcher matcher) {
        PersianDate first = date.firstOfMonth();
        PersianDate last = first.plusMonths(1).plusDays(-1);
        List<CardexRecord> rows = db.cardexBetween(first.toJdn(), last.toJdn());
        int count = 0;
        String targetDate = date.toString();
        for (CardexRecord row : rows) {
            if (row == null || targetDate.equals(row.date)) continue;
            if (matcher.matches(row)) count++;
        }
        return count;
    }

    private static boolean isSwap(CardexRecord record) {
        return record != null
                && DayWorkResolver.mode(record) == DayWorkResolver.MODE_SWAP;
    }

    private static boolean isHourlyLeave(CardexRecord record) {
        return record != null
                && DayWorkResolver.mode(record) == DayWorkResolver.MODE_LEAVE
                && !record.leaveFullShift
                && DayWorkResolver.parseMinute(record.leaveStart) >= 0
                && DayWorkResolver.parseMinute(record.leaveEnd) >= 0;
    }

    private static boolean isHourlyRest(CardexRecord record) {
        return record != null
                && DayWorkResolver.mode(record) == DayWorkResolver.MODE_REST
                && !record.restFullShift
                && DayWorkResolver.parseMinute(record.restStart) >= 0
                && DayWorkResolver.parseMinute(record.restEnd) >= 0;
    }
}
