package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.core.PersianDate;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PlannerDateTimeTest {
    private static final TimeZone TEHRAN = TimeZone.getTimeZone("Asia/Tehran");
    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    @Test public void persianDateIsDisplayedNotGregorian() {
        PersianDate chosen = new PersianDate(1405, 7, 23);
        long t = PlannerDateTime.chooseDate(0, chosen, false, TEHRAN);
        assertEquals(chosen, PlannerDateTime.jalali(t, TEHRAN));
        assertTrue(PlannerDateTime.dateLabel(t, TEHRAN).contains("۱۴۰۵"));
        assertFalse(PlannerDateTime.dateLabel(t, TEHRAN).contains("2026"));
    }

    @Test public void selectingDateDoesNotLoseReminderHour() {
        PersianDate first = new PersianDate(1405, 2, 1);
        PersianDate second = new PersianDate(1405, 2, 2);
        long t = PlannerDateTime.chooseTime(
            PlannerDateTime.chooseDate(0, first, false, TEHRAN), 21, 35, TEHRAN);
        long changed = PlannerDateTime.chooseDate(t, second, true, TEHRAN);
        assertEquals(second, PlannerDateTime.jalali(changed, TEHRAN));
        assertEquals("۲۱:۳۵", PlannerDateTime.timeLabel(changed, TEHRAN));
    }

    @Test public void selectingTimeDoesNotLosePersianDate() {
        PersianDate date = new PersianDate(1405, 9, 19);
        long t = PlannerDateTime.chooseDate(0, date, false, BERLIN);
        t = PlannerDateTime.chooseTime(t, 7, 5, BERLIN);
        assertEquals(date, PlannerDateTime.jalali(t, BERLIN));
        assertEquals("۰۷:۰۵", PlannerDateTime.timeLabel(t, BERLIN));
    }

    @Test public void summaryReflectsUnsetFields() {
        long t = PlannerDateTime.chooseDate(0,new PersianDate(1405,6,4),false,TEHRAN);
        assertEquals("بدون تاریخ و ساعت", PlannerDateTime.summary(t,false,false,TEHRAN));
        assertTrue(PlannerDateTime.summary(t,true,false,TEHRAN).contains("بدون ساعت"));
        assertTrue(PlannerDateTime.summary(t,false,true,TEHRAN).contains("تاریخ انتخاب نشده"));
    }

    @Test public void removingTimePreservesDate() {
        PersianDate date=new PersianDate(1405,10,29);
        long t=PlannerDateTime.chooseTime(PlannerDateTime.chooseDate(0,date,false,TEHRAN),23,56,TEHRAN);
        long cleared=PlannerDateTime.withoutTime(t,TEHRAN);
        assertEquals(date,PlannerDateTime.jalali(cleared,TEHRAN));
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidHourIsRejected() { PlannerDateTime.chooseTime(0,24,0,TEHRAN); }

    @Test(expected=IllegalArgumentException.class)
    public void invalidMinuteIsRejected() { PlannerDateTime.chooseTime(0,23,60,TEHRAN); }
}
