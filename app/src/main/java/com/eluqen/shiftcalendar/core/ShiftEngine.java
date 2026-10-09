package com.eluqen.shiftcalendar.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ShiftEngine {
    public static final String CUSTOM1 = "C1";
    public static final String CUSTOM2 = "C2";
    public static final String DAY = "D";
    public static final String EVENING = "E";
    public static final String NIGHT = "N";
    public static final String OFF = "O";

    private ShiftEngine() {
    }

    public static String[] parsePattern(String str) {
        if (str == null || str.trim().isEmpty()) {
            return new String[]{OFF};
        }
        String[] strArrSplit = str.split("[,،;؛|\\s]+");
        String[] strArr = new String[strArrSplit.length];
        int i = 0;
        for (String str2 : strArrSplit) {
            if (!str2.trim().isEmpty()) {
                strArr[i] = normalize(str2);
                i++;
            }
        }
        if (i == 0) {
            return new String[]{OFF};
        }
        String[] strArr2 = new String[i];
        System.arraycopy(strArr, 0, strArr2, 0, i);
        return strArr2;
    }

    public static String normalize(String str) {
        String strTrim = str == null ? "" : str.trim();
        String upperCase = strTrim.toUpperCase(Locale.US);
        String str2 = DAY;
        if (!upperCase.equals(DAY) && !strTrim.equals("روز") && !strTrim.equals("روزکار")) {
            str2 = EVENING;
            if (!upperCase.equals(EVENING) && !strTrim.equals("عصر") && !strTrim.equals("عصرکار")) {
                str2 = NIGHT;
                if (!upperCase.equals(NIGHT) && !strTrim.equals("شب") && !strTrim.equals("شبکار")) {
                    str2 = OFF;
                    if (!upperCase.equals(OFF) && !upperCase.equals("R") && !strTrim.equals("استراحت") && !strTrim.equals("تعطیل")) {
                        String str3 = CUSTOM1;
                        if (!upperCase.equals(CUSTOM1) && !upperCase.equals("NEW1") && !strTrim.equals("جدید۱") && !strTrim.equals("جدید1")) {
                            str3 = CUSTOM2;
                            if (!upperCase.equals(CUSTOM2) && !upperCase.equals("NEW2") && !strTrim.equals("جدید۲") && !strTrim.equals("جدید2")) {
                                return strTrim.isEmpty() ? OFF : strTrim;
                            }
                        }
                        return str3;
                    }
                }
            }
        }
        return str2;
    }

    public static String tokenFor(long j, long j2, String str) {
        String[] pattern = parsePattern(str);
        long length = (j - j2) % ((long) pattern.length);
        if (length < 0) {
            length += (long) pattern.length;
        }
        return pattern[(int) length];
    }

    public static String label(String str) {
        String strNormalize = normalize(str);
        if (DAY.equals(strNormalize)) {
            return "روزکار";
        }
        if (EVENING.equals(strNormalize)) {
            return "عصرکار";
        }
        if (NIGHT.equals(strNormalize)) {
            return "شبکار";
        }
        if (OFF.equals(strNormalize)) {
            return "استراحت";
        }
        if (CUSTOM1.equals(strNormalize)) {
            return "سفارشی ۱";
        }
        return CUSTOM2.equals(strNormalize) ? "سفارشی ۲" : str;
    }

    public static String shortLabel(String str) {
        String strNormalize = normalize(str);
        if (DAY.equals(strNormalize)) {
            return "ر";
        }
        if (EVENING.equals(strNormalize)) {
            return "ع";
        }
        if (NIGHT.equals(strNormalize)) {
            return "ش";
        }
        if (OFF.equals(strNormalize)) {
            return "ا";
        }
        if (CUSTOM1.equals(strNormalize)) {
            return "۱";
        }
        if (CUSTOM2.equals(strNormalize)) {
            return "۲";
        }
        return str.length() > 2 ? str.substring(0, 2) : str;
    }

    public static Map<String, Integer> count(long j, long j2, long j3, String str) {
        long jMax = Math.max(j, j2);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (long jMin = Math.min(j, j2); jMin <= jMax; jMin++) {
            String strLabel = label(tokenFor(jMin, j3, str));
            Integer num = (Integer) linkedHashMap.get(strLabel);
            int iIntValue = 1;
            if (num != null) {
                iIntValue = 1 + num.intValue();
            }
            linkedHashMap.put(strLabel, Integer.valueOf(iIntValue));
        }
        return linkedHashMap;
    }
}
