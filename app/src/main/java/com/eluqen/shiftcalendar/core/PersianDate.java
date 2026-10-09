package com.eluqen.shiftcalendar.core;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class PersianDate implements Comparable<PersianDate> {
    public static final String[] MONTH_NAMES = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
    public static final String[] WEEKDAY_NAMES = {"شنبه", "یکشنبه", "دوشنبه", "سه\u200cشنبه", "چهارشنبه", "پنجشنبه", "جمعه"};
    public final int day;
    public final int month;
    public final int year;

    @Override
    public int compareTo(PersianDate persianDate) {
        return compareTo2(persianDate);
    }

    public PersianDate(int i, int i2, int i3) {
        JalaliCalendar.validate(i, i2, i3);
        this.year = i;
        this.month = i2;
        this.day = i3;
    }

    public static PersianDate todayTehran() {
        return today(TimeZone.getTimeZone("Asia/Tehran"));
    }

    public static PersianDate today(TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone, Locale.US);
        return JalaliCalendar.fromGregorian(calendar.get(1), calendar.get(2) + 1, calendar.get(5));
    }

    public static PersianDate parse(String str) {
        if (str == null) {
            throw new IllegalArgumentException("تاریخ خالی است");
        }
        String[] strArrSplit = str.trim().replace('-', '/').replace('.', '/').replace((char) 1776, '0').replace((char) 1777, '1').replace((char) 1778, '2').replace((char) 1779, '3').replace((char) 1780, '4').replace((char) 1781, '5').replace((char) 1782, '6').replace((char) 1783, '7').replace((char) 1784, '8').replace((char) 1785, '9').split("/");
        if (strArrSplit.length != 3) {
            throw new IllegalArgumentException("قالب تاریخ باید سال/ماه/روز باشد");
        }
        return new PersianDate(Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1]), Integer.parseInt(strArrSplit[2]));
    }

    public long toJdn() {
        return JalaliCalendar.toJdn(this.year, this.month, this.day);
    }

    public PersianDate plusDays(long j) {
        return JalaliCalendar.fromJdn(toJdn() + j);
    }

    public PersianDate firstOfMonth() {
        return new PersianDate(this.year, this.month, 1);
    }

    public PersianDate plusMonths(int i) {
        long j = (((long) this.year) * 12) + (((long) this.month) - 1) + ((long) i);
        long j2 = ((j % 12) + 12) % 12;
        int i2 = (int) ((j - j2) / 12);
        int i3 = ((int) j2) + 1;
        return new PersianDate(i2, i3, Math.min(this.day, JalaliCalendar.daysInMonth(i2, i3)));
    }

    public int dayOfWeek() {
        return JalaliCalendar.dayOfWeek(toJdn());
    }

    public String weekdayName() {
        return WEEKDAY_NAMES[dayOfWeek()];
    }

    public String monthName() {
        return MONTH_NAMES[this.month - 1];
    }

    public String displayLong() {
        return weekdayName() + " " + this.day + " " + monthName() + " " + this.year;
    }

    public String toString() {
        return String.format(Locale.US, "%04d/%02d/%02d", Integer.valueOf(this.year), Integer.valueOf(this.month), Integer.valueOf(this.day));
    }

    public int compareTo2(PersianDate persianDate) {
        return Long.compare(toJdn(), persianDate.toJdn());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PersianDate)) {
            return false;
        }
        PersianDate persianDate = (PersianDate) obj;
        return this.year == persianDate.year && this.month == persianDate.month && this.day == persianDate.day;
    }

    public int hashCode() {
        return (this.year * 372) + (this.month * 31) + this.day;
    }
}
