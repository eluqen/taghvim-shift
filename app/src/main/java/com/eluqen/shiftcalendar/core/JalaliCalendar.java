package com.eluqen.shiftcalendar.core;

public final class JalaliCalendar {
    private static final int[] BREAKS = {-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178};

    private JalaliCalendar() {
    }

    public static GregorianDate toGregorian(PersianDate persianDate) {
        return toGregorian(persianDate.year, persianDate.month, persianDate.day);
    }

    public static GregorianDate toGregorian(int i, int i2, int i3) {
        validate(i, i2, i3);
        return fromGregorianJdn(toJdn(i, i2, i3));
    }

    public static PersianDate fromGregorian(int i, int i2, int i3) {
        validateGregorian(i, i2, i3);
        return fromJdn(toGregorianJdn(i, i2, i3));
    }

    public static long toJdn(int i, int i2, int i3) {
        JalCal jalCal = jalCal(i, false);
        long gregorianJdn = toGregorianJdn(jalCal.gregorianYear, 3, jalCal.marchDay);
        long j = i2;
        return (((gregorianJdn + ((j - 1) * 31)) - (div(j, 7L) * (j - 7))) + ((long) i3)) - 1;
    }

    public static PersianDate fromJdn(long j) {
        long j2;
        GregorianDate gregorianDateFromGregorianJdn = fromGregorianJdn(j);
        int i = gregorianDateFromGregorianJdn.year;
        int i2 = i - 621;
        JalCal jalCal = jalCal(i2, false);
        long gregorianJdn = j - toGregorianJdn(gregorianDateFromGregorianJdn.year, 3, jalCal.marchDay);
        if (gregorianJdn < 0) {
            i2 = i - 622;
            j2 = jalCal.leap == 1 ? gregorianJdn + 180 : 179 + gregorianJdn;
        } else {
            if (gregorianJdn <= 185) {
                return new PersianDate(i2, ((int) div(gregorianJdn, 31L)) + 1, ((int) mod(gregorianJdn, 31L)) + 1);
            }
            j2 = gregorianJdn - 186;
        }
        return new PersianDate(i2, ((int) div(j2, 30L)) + 7, ((int) mod(j2, 30L)) + 1);
    }

    public static int dayOfWeek(long j) {
        long j2 = (j + 2) % 7;
        if (j2 < 0) {
            j2 += 7;
        }
        return (int) j2;
    }

    public static int daysInMonth(int i, int i2) {
        if (i2 < 1 || i2 > 12) {
            throw new IllegalArgumentException("ماه نامعتبر است");
        }
        if (i2 <= 6) {
            return 31;
        }
        return (i2 > 11 && !isLeapYear(i)) ? 29 : 30;
    }

    public static boolean isLeapYear(int i) {
        return jalCal(i, false).leap == 0;
    }

    public static void validate(int i, int i2, int i3) {
        int[] iArr = BREAKS;
        if (i < iArr[0] || i >= iArr[iArr.length - 1]) {
            throw new IllegalArgumentException("سال جلالی خارج از بازهٔ پشتیبانی است: " + i);
        }
        if (i2 < 1 || i2 > 12) {
            throw new IllegalArgumentException("ماه نامعتبر است: " + i2);
        }
        int iDaysInMonth = daysInMonth(i, i2);
        if (i3 < 1 || i3 > iDaysInMonth) {
            throw new IllegalArgumentException("روز نامعتبر است: " + i3);
        }
    }

    public static long toGregorianJdn(int i, int i2, int i3) {
        long j = i;
        long j2 = i2;
        long j3 = j2 - 8;
        return ((((div(div((div(j3, 6L) + j) + 100100, 1L) * 1461, 4L) + div((mod(j2 + 9, 12L) * 153) + 2, 5L)) + ((long) i3)) - 34840408) - div(div((j + 100100) + div(j3, 6L), 100L) * 3, 4L)) + 752;
    }

    public static GregorianDate fromGregorianJdn(long j) {
        long j2 = j * 4;
        long jDiv = ((139361631 + j2) + (div(div(j2 + 183187720, 146097L) * 3, 4L) * 4)) - 3908;
        long jDiv2 = (div(mod(jDiv, 1461L), 4L) * 5) + 308;
        int iDiv = (int) (div(mod(jDiv2, 153L), 5L) + 1);
        int iMod = (int) (mod(div(jDiv2, 153L), 12L) + 1);
        return new GregorianDate((int) ((div(jDiv, 1461L) - 100100) + div(8 - ((long) iMod), 6L)), iMod, iDiv);
    }

    private static JalCal jalCal(int i, boolean z) {
        int[] iArr = BREAKS;
        int length = iArr.length;
        int i2 = i + 621;
        int i3 = iArr[0];
        if (i < i3 || i >= iArr[length - 1]) {
            throw new IllegalArgumentException("سال جلالی خارج از بازهٔ پشتیبانی است: " + i);
        }
        int iDiv = -14;
        int i4 = 1;
        int i5 = 0;
        while (true) {
            if (i4 >= length) {
                break;
            }
            int i6 = BREAKS[i4];
            int i7 = i6 - i3;
            if (i < i6) {
                i5 = i7;
                break;
            }
            iDiv += (div(i7, 33) * 8) + div(mod(i7, 33), 4);
            i4++;
            i3 = i6;
            i5 = i7;
        }
        int iDiv2 = i - i3;
        int iDiv3 = iDiv + (div(iDiv2, 33) * 8) + div(mod(iDiv2, 33) + 3, 4);
        if (mod(i5, 33) == 4 && i5 - iDiv2 == 4) {
            iDiv3++;
        }
        int iDiv4 = (iDiv3 + 20) - ((div(i2, 4) - div((div(i2, 100) + 1) * 3, 4)) - 150);
        if (z) {
            return new JalCal(0, i2, iDiv4);
        }
        if (i5 - iDiv2 < 6) {
            iDiv2 = (iDiv2 - i5) + (div(i5 + 4, 33) * 33);
        }
        int iMod = mod(mod(iDiv2 + 1, 33) - 1, 4);
        return new JalCal(iMod != -1 ? iMod : 4, i2, iDiv4);
    }

    private static void validateGregorian(int i, int i2, int i3) {
        if (i2 < 1 || i2 > 12) {
            throw new IllegalArgumentException("ماه میلادی نامعتبر است");
        }
        int[] iArr = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if ((i % 4 == 0 && i % 100 != 0) || i % 400 == 0) {
            iArr[1] = 29;
        }
        if (i3 < 1 || i3 > iArr[i2 - 1]) {
            throw new IllegalArgumentException("روز میلادی نامعتبر است");
        }
    }

    private static int div(int i, int i2) {
        return i / i2;
    }

    private static int mod(int i, int i2) {
        return i - ((i / i2) * i2);
    }

    private static long div(long j, long j2) {
        return j / j2;
    }

    private static long mod(long j, long j2) {
        return j - ((j / j2) * j2);
    }

    private static final class JalCal {
        final int gregorianYear;
        final int leap;
        final int marchDay;

        JalCal(int i, int i2, int i3) {
            this.leap = i;
            this.gregorianYear = i2;
            this.marchDay = i3;
        }
    }
}
