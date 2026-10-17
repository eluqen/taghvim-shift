package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PremiumScheduledDayTest {
    private final PersianDate date = new PersianDate(1405, 7, 26);
    private final Profile day = new Profile("D", "روزکار", "ر", 420, 900, 0, true, true);
    private final Profile eve = new Profile("E", "عصرکار", "ع", 900, 1380, 0, true, true);
    private final Profile off = new Profile("O", "استراحت", "ا", -1, -1, 0, false, false);
    private DayWorkResolver.State resolve(Profile base, CardexRecord record) {
        ShiftPlan plan = new ShiftPlan();
        plan.pattern = "D,E,N,O";
        plan.anchorJdn = date.toJdn();
        return DayWorkResolver.resolve(date, plan, base, record, Arrays.asList(day, eve));
    }
    private CardexRecord record() {
        CardexRecord r = new CardexRecord();
        r.date = date.toString();
        r.jdn = date.toJdn();
        return r;
    }
    @Test public void scheduledWorkingDayCountsEvenIfDateIsFutureAndNoRecordExists() {
        assertTrue(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, null)));
    }
    @Test public void ordinaryDayOffDoesNotCount() {
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(off, null)));
    }
    @Test public void fullDayLeaveSubtractsScheduledDay() {
        CardexRecord r = record(); r.leave = true; r.leaveFullShift = true;
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, r)));
    }
    @Test public void hourlyLeaveAlsoSubtractsDay() {
        CardexRecord r = record(); r.leave = true; r.leaveStart = "07:00"; r.leaveEnd = "08:00";
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, r)));
    }
    @Test public void fullRestOverrideSubtractsScheduledDay() {
        CardexRecord r = record(); r.rest = true; r.restFullShift = true;
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, r)));
    }
    @Test public void partialRestWithWorkRemainingStillCounts() {
        CardexRecord r = record(); r.rest = true; r.restStart = "07:00"; r.restEnd = "08:00";
        assertTrue(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, r)));
    }
    @Test public void aShiftSwapWithinWorkingDayStillCounts() {
        CardexRecord r = record(); r.swap = true; r.swapShiftCode = "E";
        assertTrue(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(day, r)));
    }
    @Test public void offDayOvertimeNeverCountsAsPatternFridayOrHoliday() {
        CardexRecord r = record(); r.overtime = true; r.overtimeShiftCode = "E";
        r.overtimeStart = "15:00"; r.overtimeEnd = "22:00"; r.overtimeDayOffset = 0;
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(resolve(off, r)));
    }
    @Test public void emptyStateNeverCounts() {
        assertFalse(PremiumWorkPolicy.scheduledPremiumDayEligible(null));
    }
}
