package com.eluqen.shiftcalendar.ui;

/** Presentation-only numbers for monthly work statistics.
 * Existing work-day counting and CardexStats calculations remain authoritative.
 */
public final class CardexSummaryPresentation {
    private CardexSummaryPresentation() {}

    public static int hours(int minutes) {
        return Math.max(0, minutes) / 60;
    }

    public static int minutesPart(int minutes) {
        return Math.max(0, minutes) % 60;
    }

    /** Total hours, not a clock-of-day. Keep >24h and zero-pad HH and MM. */
    public static String clockDuration(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d",
                hours(minutes), minutesPart(minutes));
    }

    public static String hoursLabel(int minutes) {
        return hours(minutes) + " ساعت";
    }

    public static String minutesLabel(int minutes) {
        return minutesPart(minutes) + " دقیقه";
    }

    /** A single self-contained RTL statistic row: title and value together. */
    public static String inlineMetric(String title, int count, String unit) {
        return title + ": " + countLabel(count, unit);
    }

    /** Bare nonnegative count for the compact monthly Friday/holiday/swap row. */
    public static String numberOnly(int count) {
        return String.valueOf(Math.max(0, count));
    }

    public static String countLabel(int count, String unit) {
        return Math.max(0, count) + " " + unit;
    }
}
