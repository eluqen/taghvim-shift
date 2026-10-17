package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;

/** Assign the nearest valid upcoming occurrence when yearly repetition is disabled. */
public final class PersonalEventRecurrence {
    private PersonalEventRecurrence() {}

    public static int nextSingleYear(int month, int day, PersianDate today) {
        if (today == null) throw new IllegalArgumentException("Current date missing");
        if (month < 1 || month > 12 || day < 1 || day > 31)
            throw new IllegalArgumentException("Invalid recurring date");
        for (int year = today.year; year <= today.year + 10; year++) {
            if (day > JalaliCalendar.daysInMonth(year, month)) continue;
            if (new PersianDate(year, month, day).compareTo(today) >= 0)
                return year;
        }
        throw new IllegalArgumentException("No valid upcoming occurrence");
    }
}
