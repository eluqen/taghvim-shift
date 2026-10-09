package com.eluqen.shiftcalendar.core;

import java.util.Locale;

public final class HijriDate {
    private static final long ISLAMIC_EPOCH = 1948440;
    public static final String[] MONTH_NAMES = {"محرم", "صفر", "ربیع\u200cالاول", "ربیع\u200cالثانی", "جمادی\u200cالاول", "جمادی\u200cالثانی", "رجب", "شعبان", "رمضان", "شوال", "ذی\u200cالقعده", "ذی\u200cالحجه"};
    public final int day;
    public final int month;
    public final int year;

    public HijriDate(int i, int i2, int i3) {
        this.year = i;
        this.month = i2;
        this.day = i3;
    }

    public static HijriDate fromJdn(long j, int i) {
        long j2 = j + ((long) i);
        int iFloor = (int) Math.floor((((j2 - ISLAMIC_EPOCH) * 30.0d) + 10646.0d) / 10631.0d);
        int iMin = (int) Math.min(12.0d, Math.ceil(((j2 - 29.0d) - toJdn(iFloor, 1, 1)) / 29.5d) + 1.0d);
        if (iMin < 1) {
            iMin = 1;
        }
        return new HijriDate(iFloor, iMin, (int) ((j2 - toJdn(iFloor, iMin, 1)) + 1));
    }

    public static long toJdn(int i, int i2, int i3) {
        return ((long) i3) + ((long) Math.ceil(((double) (i2 - 1)) * 29.5d)) + ((((long) i) - 1) * 354) + ((long) Math.floor(((((double) i) * 11.0d) + 3.0d) / 30.0d)) + 1948439;
    }

    public String monthName() {
        return MONTH_NAMES[Math.max(1, Math.min(12, this.month)) - 1];
    }

    public String displayLong() {
        return this.day + " " + monthName() + " " + this.year;
    }

    public String toString() {
        return String.format(Locale.US, "%04d/%02d/%02d", Integer.valueOf(this.year), Integer.valueOf(this.month), Integer.valueOf(this.day));
    }
}
