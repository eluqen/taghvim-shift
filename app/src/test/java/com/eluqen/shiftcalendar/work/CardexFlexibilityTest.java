package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.CardexStats;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class CardexFlexibilityTest {
    @Test
    public void noteCanCoexistWithSingleOperationalStatus() {
        CardexRecord record = new CardexRecord();
        record.note = "یادداشت";
        record.swap = true;
        record.swapShiftCode = "E";

        assertEquals(
                DayWorkResolver.MODE_SWAP,
                DayWorkResolver.mode(record));
        assertTrue(record.summary().contains("یادداشت"));
        assertTrue(record.summary().contains("تعویض شیفت"));
    }

    @Test
    public void swapShiftCode_isCountedByStats() {
        CardexRecord record = new CardexRecord();
        record.swap = true;
        record.swapShiftCode = "N";
        CardexStats stats = CardexStats.calculate(
                Arrays.asList(record), false);
        assertEquals(1, stats.swapCount);
    }

    @Test
    public void wholeDayLeaveStoredRange_contributesToStats() {
        CardexRecord record = new CardexRecord();
        record.leave = true;
        record.leaveFullShift = true;
        record.leaveStart = "07:00";
        record.leaveEnd = "15:00";

        CardexStats stats = CardexStats.calculate(
                Arrays.asList(record), false);
        assertEquals(8 * 60, stats.leaveMinutes);
    }

    @Test
    public void overtimeAcrossMidnight_contributesEightHours() {
        CardexRecord record = new CardexRecord();
        record.overtime = true;
        record.overtimeShiftCode = "N";
        record.overtimeStart = "23:00";
        record.overtimeEnd = "07:00";

        CardexStats stats = CardexStats.calculate(
                Arrays.asList(record), false);
        assertEquals(8 * 60, stats.overtimeMinutes);
    }

    @Test
    public void hourlyLeaveAndRestKeepExactDurations() {
        CardexRecord leave = new CardexRecord();
        leave.leave = true;
        leave.leaveFullShift = false;
        leave.leaveStart = "09:00";
        leave.leaveEnd = "11:30";

        CardexRecord rest = new CardexRecord();
        rest.rest = true;
        rest.restFullShift = false;
        rest.restStart = "12:00";
        rest.restEnd = "14:00";

        CardexStats stats = CardexStats.calculate(
                Arrays.asList(leave, rest), false);
        assertEquals(150, stats.leaveMinutes);
        assertEquals(120, stats.restMinutes);
    }

    @Test
    public void compensatoryRestReducesOvertimeHourForHour() {
        CardexRecord overtime = new CardexRecord();
        overtime.overtime = true;
        overtime.overtimeStart = "07:00";
        overtime.overtimeEnd = "15:00";

        CardexRecord rest = new CardexRecord();
        rest.rest = true;
        rest.restFullShift = false;
        rest.restStart = "07:00";
        rest.restEnd = "11:00";

        CardexStats stats = CardexStats.calculate(
                Arrays.asList(overtime, rest), false);
        assertEquals(480, stats.grossOvertimeMinutes);
        assertEquals(240, stats.restMinutes);
        assertEquals(240, stats.overtimeMinutes);
    }

    @Test
    public void overtimeBalanceNeverLetsRestExceedSameMonthEarnedOvertime() {
        assertTrue(OvertimeBalancePolicy.allowsChange(480, 0, 480, 240));
        assertTrue(OvertimeBalancePolicy.allowsChange(480, 240, 480, 480));
        assertFalse(OvertimeBalancePolicy.allowsChange(480, 240, 0, 240));
        // Legacy deficit may remain unchanged by unrelated edits, but cannot worsen.
        assertTrue(OvertimeBalancePolicy.allowsChange(0, 240, 0, 240));
        assertFalse(OvertimeBalancePolicy.allowsChange(0, 240, 0, 300));
    }

    @Test
    public void overtimeDeleteReportsExactRestThatMustBeReleasedFirst() {
        assertEquals(240,
                OvertimeBalancePolicy.requiredRestReleaseMinutes(
                        480, 240, 0, 240));
        assertEquals(0,
                OvertimeBalancePolicy.requiredRestReleaseMinutes(
                        600, 240, 480, 240));
        assertEquals(180,
                OvertimeBalancePolicy.requiredRestReleaseMinutes(
                        0, 120, 0, 300));
    }

    @Test
    public void absencePolicyHandlesCrossMidnightNightShift() {
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile night =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "N", "شبکار", "ش", 1380, 420, 0, true, true);
        assertTrue(AbsencePolicy.hourlyRangeAllowed(night, 60, 180));
        assertTrue(AbsencePolicy.hourlyRangeAllowed(night, 1380, 60));
        assertFalse(AbsencePolicy.hourlyRangeAllowed(night, 1380, 420));
        assertFalse(AbsencePolicy.hourlyRangeAllowed(night, 900, 1000));
    }

    @Test
    public void legacyConflictingFlags_doNotDefineMultipleActiveModes() {
        CardexRecord record = new CardexRecord();
        record.leave = true;
        record.swap = true;
        record.overtime = true;

        assertEquals(
                DayWorkResolver.MODE_LEAVE,
                DayWorkResolver.mode(record));
    }
}
