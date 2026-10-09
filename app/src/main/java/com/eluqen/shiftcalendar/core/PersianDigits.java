package com.eluqen.shiftcalendar.core;

public final class PersianDigits {
    private static final char[] DIGITS = {1776, 1777, 1778, 1779, 1780, 1781, 1782, 1783, 1784, 1785};

    private PersianDigits() {
    }

    public static String of(Object obj) {
        String strValueOf = String.valueOf(obj);
        StringBuilder sb = new StringBuilder(strValueOf.length());
        for (int i = 0; i < strValueOf.length(); i++) {
            char cCharAt = strValueOf.charAt(i);
            if (cCharAt >= '0' && cCharAt <= '9') {
                cCharAt = DIGITS[cCharAt - '0'];
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }
}
