package com.eluqen.shiftcalendar.work;

import android.content.Context;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.CardexStats;
import java.util.ArrayList;
import java.util.List;

/**
 * Compensatory rest consumes overtime earned in the SAME Persian month.
 * New mutations may never worsen a negative monthly balance.
 */
public final class OvertimeBalancePolicy {
    private OvertimeBalancePolicy() {}

    public static Balance monthBalance(
            Context context, AppDatabase db, PersianDate date) {
        return monthBalance(context, db, date, null, false);
    }

    public static Balance monthBalanceAfter(
            Context context, AppDatabase db, PersianDate date,
            CardexRecord replacement, boolean deleteDate) {
        return monthBalance(context, db, date, replacement, deleteDate);
    }

    public static boolean mutationAllowed(
            Context context, AppDatabase db, PersianDate date,
            CardexRecord replacement, boolean deleteDate) {
        return requiredRestReleaseMinutes(
                context, db, date, replacement, deleteDate) == 0;
    }

    /**
     * Exact amount of compensatory rest that must be removed first for a
     * proposed mutation to become balance-safe. Existing legacy deficits are
     * preserved: only newly-worsened deficit is reported.
     */
    public static int requiredRestReleaseMinutes(
            Context context, AppDatabase db, PersianDate date,
            CardexRecord replacement, boolean deleteDate) {
        Balance before = monthBalance(context, db, date);
        Balance after = monthBalanceAfter(
                context, db, date, replacement, deleteDate);
        return requiredRestReleaseMinutes(
                before.grossOvertimeMinutes, before.restMinutes,
                after.grossOvertimeMinutes, after.restMinutes);
    }

    public static int requiredRestReleaseMinutes(
            int beforeGross, int beforeRest,
            int afterGross, int afterRest) {
        int beforeDeficit = Math.max(0, beforeRest - beforeGross);
        int afterDeficit = Math.max(0, afterRest - afterGross);
        return Math.max(0, afterDeficit - beforeDeficit);
    }

    public static boolean allowsChange(
            int beforeGross, int beforeRest,
            int afterGross, int afterRest) {
        return requiredRestReleaseMinutes(
                beforeGross, beforeRest, afterGross, afterRest) == 0;
    }

    private static Balance monthBalance(
            Context context, AppDatabase db, PersianDate date,
            CardexRecord replacement, boolean deleteDate) {
        PersianDate first = date.firstOfMonth();
        PersianDate last = first.plusMonths(1).plusDays(-1);
        List<CardexRecord> rows = new ArrayList<>(
                db.cardexBetween(first.toJdn(), last.toJdn()));

        if (replacement != null || deleteDate) {
            String key = date.toString();
            for (int i = rows.size() - 1; i >= 0; i--) {
                if (key.equals(rows.get(i).date)) rows.remove(i);
            }
            if (!deleteDate && replacement != null && !replacement.isEmpty()) {
                rows.add(replacement.copy());
            }
        }

        CardexStats stats = CardexStats.calculate(context, rows, false);
        return new Balance(
                stats.grossOvertimeMinutes,
                stats.restMinutes,
                stats.overtimeMinutes);
    }

    public static final class Balance {
        public final int grossOvertimeMinutes;
        public final int restMinutes;
        public final int netOvertimeMinutes;

        Balance(int gross, int rest, int net) {
            grossOvertimeMinutes = Math.max(0, gross);
            restMinutes = Math.max(0, rest);
            netOvertimeMinutes = Math.max(0, net);
        }

        public int deficitMinutes() {
            return Math.max(0, restMinutes - grossOvertimeMinutes);
        }
    }
}
