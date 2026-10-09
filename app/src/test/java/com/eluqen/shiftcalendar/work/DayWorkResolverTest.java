package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.CardexRecord;
import org.junit.Test;
import static org.junit.Assert.*;

public class DayWorkResolverTest {
    @Test
    public void parseMinute_acceptsValid24HourTime() {
        assertEquals(0, DayWorkResolver.parseMinute("00:00"));
        assertEquals(435, DayWorkResolver.parseMinute("07:15"));
        assertEquals(1439, DayWorkResolver.parseMinute("23:59"));
    }

    @Test
    public void parseMinute_rejectsInvalidTime() {
        assertEquals(-1, DayWorkResolver.parseMinute(""));
        assertEquals(-1, DayWorkResolver.parseMinute("24:00"));
        assertEquals(-1, DayWorkResolver.parseMinute("07:60"));
        assertEquals(-1, DayWorkResolver.parseMinute("x"));
    }

    @Test
    public void durationMinutes_handlesCrossMidnight() {
        assertEquals(480, DayWorkResolver.durationMinutes(420, 900));
        assertEquals(480, DayWorkResolver.durationMinutes(1380, 420));
    }

    @Test
    public void noOperationalSelection_meansNormalPattern() {
        assertEquals(
                DayWorkResolver.MODE_NORMAL,
                DayWorkResolver.mode(new CardexRecord()));
    }

    @Test
    public void eachOperationalSelection_mapsToOneMode() {
        CardexRecord rest = new CardexRecord();
        rest.rest = true;
        assertEquals(DayWorkResolver.MODE_REST, DayWorkResolver.mode(rest));

        CardexRecord leave = new CardexRecord();
        leave.leave = true;
        assertEquals(DayWorkResolver.MODE_LEAVE, DayWorkResolver.mode(leave));

        CardexRecord swap = new CardexRecord();
        swap.swap = true;
        assertEquals(DayWorkResolver.MODE_SWAP, DayWorkResolver.mode(swap));

        CardexRecord overtime = new CardexRecord();
        overtime.overtime = true;
        assertEquals(
                DayWorkResolver.MODE_OVERTIME,
                DayWorkResolver.mode(overtime));
    }

    @Test
    public void legacyConflictingRow_resolvesDeterministicallyToSingleMode() {
        CardexRecord record = new CardexRecord();
        record.rest = true;
        record.leave = true;
        record.swap = true;
        record.overtime = true;

        assertEquals(
                DayWorkResolver.MODE_REST,
                DayWorkResolver.mode(record));
    }

    @Test
    public void staleLegacyFields_doNotOverrideAuthoritativeFlag() {
        CardexRecord record = new CardexRecord();
        record.restStart = "07:00";
        record.restEnd = "15:00";
        record.leaveStart = "07:00";
        record.leaveEnd = "15:00";
        record.swap = true;
        record.swapShiftCode = "E";

        assertEquals(
                DayWorkResolver.MODE_SWAP,
                DayWorkResolver.mode(record));
    }

    @Test
    public void staleLegacyFields_withoutFlag_meanNormalDay() {
        CardexRecord record = new CardexRecord();
        record.restStart = "07:00";
        record.restEnd = "15:00";
        record.overtimeStart = "15:00";
        record.overtimeEnd = "23:00";

        assertEquals(
                DayWorkResolver.MODE_NORMAL,
                DayWorkResolver.mode(record));
        assertTrue(record.isEmpty());
    }

    @Test
    public void operationalFlags_makeTargetRecordsNonEmpty() {
        CardexRecord swap = new CardexRecord();
        swap.swap = true;
        swap.swapShiftCode = "E";
        assertFalse(swap.isEmpty());

        CardexRecord overtime = new CardexRecord();
        overtime.overtime = true;
        overtime.overtimeShiftCode = "N";
        assertFalse(overtime.isEmpty());
    }

    @Test
    public void staleTargetCodes_withoutFlags_areIgnored() {
        CardexRecord record = new CardexRecord();
        record.swapShiftCode = "E";
        record.overtimeShiftCode = "N";
        assertTrue(record.isEmpty());
        assertEquals(
                DayWorkResolver.MODE_NORMAL,
                DayWorkResolver.mode(record));
    }

    @Test
    public void wholeDaySummaries_areExplicit() {
        CardexRecord leave = new CardexRecord();
        leave.leave = true;
        leave.leaveFullShift = true;
        assertTrue(leave.summary().contains("مرخصی استحقاقی"));

        CardexRecord rest = new CardexRecord();
        rest.rest = true;
        rest.restFullShift = true;
        assertTrue(rest.summary().contains("استراحت"));
    }

    @Test
    public void restAndLeave_neverScheduleBaseShiftReminder() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "جنرال";
        plan.team = "A";
        com.eluqen.shiftcalendar.core.PersianDate date =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile day =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "D", "روزکار", "ر", 420, 900, 0, true, true);

        DayWorkResolver.State rest = new DayWorkResolver.State(
                date, plan, new CardexRecord(),
                DayWorkResolver.MODE_REST,
                "D", day, "D", day,
                false, true, "استراحت",
                false, null, false);
        DayWorkResolver.State leave = new DayWorkResolver.State(
                date, plan, new CardexRecord(),
                DayWorkResolver.MODE_LEAVE,
                "D", day, "D", day,
                false, true, "مرخصی",
                false, null, false);

        assertFalse(rest.shouldScheduleShift());
        assertFalse(leave.shouldScheduleShift());
    }

    @Test
    public void swapSchedulesOnlyReplacementShiftProfile() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "جنرال";
        plan.team = "A";
        com.eluqen.shiftcalendar.core.PersianDate date =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile day =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "D", "روزکار", "ر", 420, 900, 0, true, true);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile evening =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "E", "عصرکار", "ع", 900, 1380, 0, true, true);

        DayWorkResolver.State swap = new DayWorkResolver.State(
                date, plan, new CardexRecord(),
                DayWorkResolver.MODE_SWAP,
                "D", day, "E", evening,
                true, false, "",
                false, null, false);

        assertTrue(swap.shouldScheduleShift());
        assertEquals("E", swap.effectiveCode);
        assertEquals(900, swap.effectiveProfile.startMinute);
    }

    @Test
    public void offDayOvertime_doesNotScheduleBaseOffReminder() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "جنرال";
        plan.team = "A";
        com.eluqen.shiftcalendar.core.PersianDate date =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile off =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "O", "استراحت", "ا", -1, -1, 0, false, false);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile day =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "D", "روزکار", "ر", 420, 900, 0, true, true);

        DayWorkResolver.State overtime = new DayWorkResolver.State(
                date, plan, new CardexRecord(),
                DayWorkResolver.MODE_OVERTIME,
                "O", off, "O", off,
                false, false, "",
                true, day, false);

        assertFalse(overtime.shouldScheduleShift());
        assertTrue(overtime.hasOvertime);
    }

    @Test
    public void ownShiftPlusOvertime_keepsOwnShiftReminder() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "جنرال";
        plan.team = "A";
        com.eluqen.shiftcalendar.core.PersianDate date =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile day =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "D", "روزکار", "ر", 420, 900, 0, true, true);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile evening =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "E", "عصرکار", "ع", 900, 1380, 0, true, true);

        DayWorkResolver.State overtime = new DayWorkResolver.State(
                date, plan, new CardexRecord(),
                DayWorkResolver.MODE_OVERTIME,
                "D", day, "D", day,
                false, false, "",
                true, evening, true);

        assertTrue(overtime.shouldScheduleShift());
        assertTrue(overtime.hasOvertime);
        assertTrue(overtime.overtimeFromOwnShift);
    }

    @Test
    public void offDayNightOvertime_usesPresetPreviousEveningStartDate() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "فولاد مبارکه";
        plan.team = "A";

        com.eluqen.shiftcalendar.core.PersianDate nominal =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile off =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "O", "استراحت", "ا", -1, -1, 0, false, false);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile night =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "N", "شبکار", "ش", 1380, 420, 0, true, true);

        DayWorkResolver.State state = new DayWorkResolver.State(
                nominal, plan, new CardexRecord(),
                DayWorkResolver.MODE_OVERTIME,
                "O", off,
                "O", off,
                false,
                false, "",
                true, night, false);

        assertEquals(
                nominal.plusDays(-1L),
                state.overtimeStartDate());
    }

    @Test
    public void eveningThenNightOvertime_startsSameNominalDay() {
        com.eluqen.shiftcalendar.data.ShiftPlan plan =
                new com.eluqen.shiftcalendar.data.ShiftPlan();
        plan.company = "فولاد مبارکه";
        plan.team = "A";

        com.eluqen.shiftcalendar.core.PersianDate nominal =
                new com.eluqen.shiftcalendar.core.PersianDate(1405, 7, 10);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile evening =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "E", "عصرکار", "ع", 900, 1380, 0, true, true);
        com.eluqen.shiftcalendar.data.ShiftPreferences.Profile night =
                new com.eluqen.shiftcalendar.data.ShiftPreferences.Profile(
                        "N", "شبکار", "ش", 1380, 420, 0, true, true);

        DayWorkResolver.State state = new DayWorkResolver.State(
                nominal, plan, new CardexRecord(),
                DayWorkResolver.MODE_OVERTIME,
                "E", evening,
                "E", evening,
                false,
                false, "",
                true, night, true);

        assertEquals(nominal, state.overtimeStartDate());
    }
}
