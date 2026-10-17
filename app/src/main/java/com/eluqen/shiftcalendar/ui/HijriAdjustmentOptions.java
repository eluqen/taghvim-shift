package com.eluqen.shiftcalendar.ui;

/** Pure, UI-independent mapping of the supported local Hijri correction. */
public final class HijriAdjustmentOptions {
    public static final int MIN = -2;
    public static final int MAX = 2;

    private HijriAdjustmentOptions() {}

    public static String optionLabel(int offset) {
        switch (offset) {
            case -2: return "دو روز عقب‌تر";
            case -1: return "یک روز عقب‌تر";
            case 0: return "بدون اصلاح (صفر)";
            case 1: return "یک روز جلوتر";
            case 2: return "دو روز جلوتر";
            default: throw new IllegalArgumentException("Offset outside supported range");
        }
    }

    public static String savedDescription(int offset) {
        if (offset == 0) return "بدون اصلاح دستی";
        return optionLabel(offset);
    }
}
