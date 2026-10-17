package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.core.GregorianDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Pure presentation/epoch arithmetic for existing personal reminder records. */
public final class PlannerDateTime {
    private PlannerDateTime() {}

    public static PersianDate jalali(long epoch, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.US);
        c.setTimeInMillis(epoch);
        return JalaliCalendar.fromGregorian(c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String dateLabel(long epoch, TimeZone zone) {
        return PersianDigits.of(jalali(epoch, zone).displayLong());
    }

    public static String timeLabel(long epoch, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.US);
        c.setTimeInMillis(epoch);
        return PersianDigits.of(String.format(Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
    }

    public static String summary(long epoch, boolean hasDate, boolean hasTime, TimeZone zone) {
        if (!hasDate) return hasTime
                ? "تاریخ انتخاب نشده • ساعت " + timeLabel(epoch, zone)
                : "بدون تاریخ و ساعت";
        return dateLabel(epoch, zone)
                + (hasTime ? " • ساعت " + timeLabel(epoch, zone) : " • بدون ساعت");
    }

    public static long chooseDate(long epoch, PersianDate date, boolean hasTime,
                                  TimeZone zone) {
        GregorianDate g = JalaliCalendar.toGregorian(date);
        Calendar c = Calendar.getInstance(zone, Locale.US);
        c.setTimeInMillis(epoch > 0 ? epoch : System.currentTimeMillis());
        c.setLenient(false);
        if (!hasTime) {
            c.set(Calendar.HOUR_OF_DAY, 12); // Noon avoids nonexistent DST midnight.
            c.set(Calendar.MINUTE, 0);
        }
        c.set(Calendar.YEAR, g.year);
        c.set(Calendar.MONTH, g.month - 1);
        c.set(Calendar.DAY_OF_MONTH, g.day);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long chooseTime(long epoch, int hour, int minute, TimeZone zone) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59)
            throw new IllegalArgumentException("Invalid reminder time");
        Calendar c = Calendar.getInstance(zone, Locale.US);
        c.setTimeInMillis(epoch > 0 ? epoch : System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long withoutTime(long epoch, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.US);
        c.setTimeInMillis(epoch > 0 ? epoch : System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
