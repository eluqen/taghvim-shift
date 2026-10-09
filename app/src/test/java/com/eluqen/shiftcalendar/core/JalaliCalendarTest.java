package com.eluqen.shiftcalendar.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class JalaliCalendarTest {
    @Test
    public void jalaliJdnRoundTrip() {
        PersianDate[] samples = {
                new PersianDate(1405, 1, 1),
                new PersianDate(1405, 6, 31),
                new PersianDate(1405, 12, 29),
                new PersianDate(1409, 1, 1),
                new PersianDate(1399, 12, 30)
        };
        for (PersianDate d : samples) {
            assertEquals(d, JalaliCalendar.fromJdn(d.toJdn()));
        }
    }

    @Test
    public void gregorianRoundTrip() {
        PersianDate[] samples = {
                new PersianDate(1405, 1, 1),
                new PersianDate(1405, 10, 11),
                new PersianDate(1409, 12, 29)
        };
        for (PersianDate d : samples) {
            GregorianDate g = JalaliCalendar.toGregorian(d);
            assertEquals(d, JalaliCalendar.fromGregorian(g.year, g.month, g.day));
        }
    }

    @Test
    public void monthLengthsStayValid() {
        for (int y = 1405; y <= 1409; y++) {
            for (int m = 1; m <= 12; m++) {
                int days = JalaliCalendar.daysInMonth(y, m);
                assertTrue(days >= 29 && days <= 31);
                JalaliCalendar.validate(y, m, days);
            }
        }
    }
    @Test
    public void knownGregorianAnchors() {
        int[][] anchors = {
            {1405, 7, 7, 2026, 9, 29},
            {1404, 1, 1, 2025, 3, 21},
            {1399, 12, 30, 2021, 3, 20},
            {1400, 1, 1, 2021, 3, 21}
        };
        for (int[] a : anchors) {
            GregorianDate result = JalaliCalendar.toGregorian(new PersianDate(a[0], a[1], a[2]));
            assertEquals(a[3], result.year);
            assertEquals(a[4], result.month);
            assertEquals(a[5], result.day);
        }
    }

    @Test
    public void leapYearTransitionIsOneDay() {
        assertEquals(1L, new PersianDate(1400, 1, 1).toJdn() - new PersianDate(1399, 12, 30).toJdn());
    }
}
