package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PersonalEventRecurrenceTest {
    @Test public void upcomingDayUsesThisYear() {
        assertEquals(1405, PersonalEventRecurrence.nextSingleYear(7, 27, new PersianDate(1405,7,26)));
    }
    @Test public void pastDayUsesNextYearNotPastOccurrence() {
        assertEquals(1406, PersonalEventRecurrence.nextSingleYear(7, 25, new PersianDate(1405,7,26)));
    }
    @Test public void currentDayCanBeUsed() {
        assertEquals(1405, PersonalEventRecurrence.nextSingleYear(7, 26, new PersianDate(1405,7,26)));
    }
    @Test public void leapYearOccasionSkipsInvalidYear() {
        int nowYear=1405;int expected=nowYear;
        while (!JalaliCalendar.isLeapYear(expected)) expected++;
        assertEquals(expected, PersonalEventRecurrence.nextSingleYear(12,30,new PersianDate(nowYear,7,26)));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidMonth() {
        PersonalEventRecurrence.nextSingleYear(13, 12, new PersianDate(1405,7,26));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidDay() {
        PersonalEventRecurrence.nextSingleYear(1, 32, new PersianDate(1405,7,26));
    }
}
