package com.eluqen.shiftcalendar.work;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorkRestPolicyTest {
    private static long h(long hour) {
        return hour * WorkRestPolicy.HOUR_MS;
    }

    private static WorkRestPolicy.Interval block(
            long startHour, long endHour, boolean candidate) {
        return new WorkRestPolicy.Interval(
                h(startHour), h(endHour), candidate);
    }

    @Test public void threeBackToBackEightHourShiftsAreRejected() {
        assertFalse(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, false),
                block(16, 24, true))));
    }

    @Test public void twoEightHourShiftsRemainAllowed() {
        assertTrue(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, true))));
    }

    @Test public void eightHoursRestResetsTheWorkBlock() {
        assertTrue(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, false),
                block(24, 32, true))));
    }

    @Test public void lessThanEightHoursRestDoesNotResetAfterSixteenHours() {
        assertFalse(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, false),
                block(23, 24, true))));
    }

    @Test public void shortenedWorkMayReachButNotExceedSixteenHours() {
        assertTrue(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 15, false),
                block(15, 16, true))));
        assertFalse(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 15, false),
                block(15, 17, true))));
    }

    @Test public void previousDayEveningThenOwnedNightThenSameDayDayIsAlwaysThreeShifts() {
        // Exact owner regression:
        // Mon E overtime 15:00-23:00, Tue owned N 23:00-07:00,
        // Tue D overtime 07:00-15:00 => 24h continuous and forbidden.
        long monday15 = h(0);
        assertFalse(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                new WorkRestPolicy.Interval(monday15, monday15 + h(8), false),
                new WorkRestPolicy.Interval(monday15 + h(8), monday15 + h(16), false),
                new WorkRestPolicy.Interval(monday15 + h(16), monday15 + h(24), true))));
        // Tue E 15:00 starts after 8h rest following N end at 07:00.
        assertTrue(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                new WorkRestPolicy.Interval(monday15, monday15 + h(8), false),
                new WorkRestPolicy.Interval(monday15 + h(8), monday15 + h(16), false),
                new WorkRestPolicy.Interval(monday15 + h(24), monday15 + h(32), true))));
    }

    @Test public void mondayEveningPlusTuesdayNightBlocksTuesdayDayButNotEvening() {
        // Monday E overtime 15-23 => normalized here as 0-8.
        // Tuesday N shift 23-07 => immediately follows, normalized as 8-16.
        // Tuesday D 07-15 would be third consecutive work block => rejected.
        assertFalse(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, false),
                block(16, 24, true))));

        // Tuesday E starts after 8h of actual rest (07-15) => allowed.
        assertTrue(WorkRestPolicy.respectsRestWindow(Arrays.asList(
                block(0, 8, false),
                block(8, 16, false),
                block(24, 32, true))));
    }
}
