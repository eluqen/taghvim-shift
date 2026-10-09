package com.eluqen.shiftcalendar.core;

import java.util.Locale;

public final class GregorianDate {
    public final int day;
    public final int month;
    public final int year;

    public GregorianDate(int i, int i2, int i3) {
        this.year = i;
        this.month = i2;
        this.day = i3;
    }

    public String toString() {
        return String.format(Locale.US, "%04d-%02d-%02d", Integer.valueOf(this.year), Integer.valueOf(this.month), Integer.valueOf(this.day));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof GregorianDate)) {
            return false;
        }
        GregorianDate gregorianDate = (GregorianDate) obj;
        return this.year == gregorianDate.year && this.month == gregorianDate.month && this.day == gregorianDate.day;
    }

    public int hashCode() {
        return (this.year * 372) + (this.month * 31) + this.day;
    }
}
