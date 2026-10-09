package com.eluqen.shiftcalendar.data;

import java.util.List;

public final class CardexStats {
    public int leaveMinutes;
    public int noteCount;
    public int grossOvertimeMinutes;
    /** Net overtime remaining after compensatory rest is deducted. */
    public int overtimeMinutes;
    public int recordedDays;
    public int restMinutes;
    public int swapCount;

    private CardexStats() {
    }

    public static CardexStats calculate(
            android.content.Context context,
            List<CardexRecord> rows,
            boolean excludeLunch) {
        java.util.Map<String,java.util.ArrayList<CardexRecord>> months =
                new java.util.LinkedHashMap<>();
        ShiftPlan plan = AppDatabase.get(context).activePlan();

        for (CardexRecord original : rows) {
            CardexRecord r = original.copy();
            com.eluqen.shiftcalendar.core.PersianDate date =
                    com.eluqen.shiftcalendar.core.PersianDate.parse(r.date);
            com.eluqen.shiftcalendar.work.DayWorkResolver.State state =
                    com.eluqen.shiftcalendar.work.DayWorkResolver.resolve(
                            context, date, plan, r);

            if (state.mode == 4 && state.hasOvertime) {
                r.overtimeStart =
                        ShiftPreferences.formatMinute(state.overtimeStartMinute);
                r.overtimeEnd =
                        ShiftPreferences.formatMinute(state.overtimeEndMinute);
            }
            if (state.mode == 1) {
                if (state.isFullDayAbsence()
                        && com.eluqen.shiftcalendar.work.DayWorkResolver.validWork(
                                state.baseProfile)) {
                    r.restStart = ShiftPreferences.formatMinute(
                            state.baseProfile.startMinute);
                    r.restEnd = ShiftPreferences.formatMinute(
                            state.baseProfile.endMinute);
                }
            }
            if (state.mode == 2) {
                if (state.isFullDayAbsence()
                        && com.eluqen.shiftcalendar.work.DayWorkResolver.validWork(
                                state.baseProfile)) {
                    r.leaveStart = ShiftPreferences.formatMinute(
                            state.baseProfile.startMinute);
                    r.leaveEnd = ShiftPreferences.formatMinute(
                            state.baseProfile.endMinute);
                }
            }

            String key = date.year + "-" + date.month;
            java.util.ArrayList<CardexRecord> bucket = months.get(key);
            if (bucket == null) {
                bucket = new java.util.ArrayList<>();
                months.put(key, bucket);
            }
            bucket.add(r);
        }

        CardexStats out = new CardexStats();
        for (java.util.ArrayList<CardexRecord> bucket : months.values()) {
            merge(out, calculate(bucket, excludeLunch));
        }
        return out;
    }

    public static CardexStats calculate(List<CardexRecord> list, boolean z) {
        CardexStats cardexStats = new CardexStats();
        for (CardexRecord cardexRecord : list) {
            cardexStats.recordedDays++;
            if (!blank(cardexRecord.note) || !blank(cardexRecord.noteTime) || !blank(cardexRecord.noteInk)) {
                cardexStats.noteCount++;
            }
            int mode = com.eluqen.shiftcalendar.work.DayWorkResolver.mode(cardexRecord);
            if (mode == 3) {
                cardexStats.swapCount++;
            }
            if (mode == 1) {
                // Compensatory rest is exchanged against overtime minute-for-minute.
                // Lunch-exclusion preferences must never change that balance.
                cardexStats.restMinutes += duration(
                        cardexRecord.restStart, cardexRecord.restEnd, false);
            }
            if (mode == 2) {
                cardexStats.leaveMinutes += duration(
                        cardexRecord.leaveStart, cardexRecord.leaveEnd, z);
            }
            if (mode == 4) {
                cardexStats.grossOvertimeMinutes += duration(
                        cardexRecord.overtimeStart,
                        cardexRecord.overtimeEnd,
                        false);
            }
        }
        cardexStats.overtimeMinutes = Math.max(
                0, cardexStats.grossOvertimeMinutes - cardexStats.restMinutes);
        return cardexStats;
    }

    private static void merge(CardexStats into, CardexStats part) {
        into.leaveMinutes += part.leaveMinutes;
        into.noteCount += part.noteCount;
        into.grossOvertimeMinutes += part.grossOvertimeMinutes;
        into.overtimeMinutes += part.overtimeMinutes;
        into.recordedDays += part.recordedDays;
        into.restMinutes += part.restMinutes;
        into.swapCount += part.swapCount;
    }

    public static int duration(String str, String str2, boolean z) {
        int minute = parseMinute(str);
        int minute2 = parseMinute(str2);
        if (minute < 0 || minute2 < 0 || minute == minute2) {
            return 0;
        }
        if (minute2 < minute) {
            minute2 += 1440;
        }
        int iOverlap = minute2 - minute;
        if (z) {
            iOverlap = (iOverlap - overlap(minute, minute2, 720, 780)) - overlap(minute, minute2, 2160, 2220);
        }
        return Math.max(0, iOverlap);
    }

    public static String formatDuration(int i) {
        int iMax = Math.max(0, i);
        int i2 = iMax / 60;
        int i3 = iMax % 60;
        if (i2 == 0) {
            return i3 + " دقیقه";
        }
        if (i3 == 0) {
            return i2 + " ساعت";
        }
        return i2 + " ساعت و " + i3 + " دقیقه";
    }

    private static int parseMinute(String str) {
        if (blank(str)) {
            return -1;
        }
        try {
            String[] strArrSplit = str.trim().replace((char) 1776, '0').replace((char) 1777, '1').replace((char) 1778, '2').replace((char) 1779, '3').replace((char) 1780, '4').replace((char) 1781, '5').replace((char) 1782, '6').replace((char) 1783, '7').replace((char) 1784, '8').replace((char) 1785, '9').split(":");
            if (strArrSplit.length != 2) {
                return -1;
            }
            int i = Integer.parseInt(strArrSplit[0]);
            int i2 = Integer.parseInt(strArrSplit[1]);
            if (i >= 0 && i <= 23 && i2 >= 0 && i2 <= 59) {
                return (i * 60) + i2;
            }
        } catch (RuntimeException unused) {
        }
        return -1;
    }

    private static int overlap(int i, int i2, int i3, int i4) {
        return Math.max(0, Math.min(i2, i4) - Math.max(i, i3));
    }

    private static boolean blank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
