package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CardexSummaryPresentationTest {
    @Test public void secondRowShowsOnlyPersianNumbersNoDaySuffix() {
        assertEquals("۲", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.numberOnly(2)));
        assertEquals("۱", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.numberOnly(1)));
        assertEquals("۳", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.numberOnly(3)));
        assertEquals("۰", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.numberOnly(-1)));
    }
    @Test public void attendanceAndCountsAreSingleInlineStatistics() {
        assertEquals("جمعه‌کاری: 2 روز", CardexSummaryPresentation.inlineMetric("جمعه‌کاری", 2, "روز"));
        assertEquals("تعطیل‌کاری: 1 روز", CardexSummaryPresentation.inlineMetric("تعطیل‌کاری", 1, "روز"));
        assertEquals("یادداشت‌ها: 0 مورد", CardexSummaryPresentation.inlineMetric("یادداشت‌ها", 0, "مورد"));
        assertEquals("جمعه‌کاری: 0 روز", CardexSummaryPresentation.inlineMetric("جمعه‌کاری", -2, "روز"));
    }
    @Test public void overtimeIsGrossAndRestOnlyAddsToRestDisplayInMonthlySummary() {
        com.eluqen.shiftcalendar.data.CardexRecord overtime =
                new com.eluqen.shiftcalendar.data.CardexRecord();
        overtime.overtime = true;
        overtime.overtimeStart = "07:00";
        overtime.overtimeEnd = "15:00";
        com.eluqen.shiftcalendar.data.CardexRecord rest =
                new com.eluqen.shiftcalendar.data.CardexRecord();
        rest.rest = true;
        rest.restStart = "07:00";
        rest.restEnd = "09:30";
        com.eluqen.shiftcalendar.data.CardexStats stats =
                com.eluqen.shiftcalendar.data.CardexStats.calculate(
                        java.util.Arrays.asList(overtime, rest), false);
        assertEquals(480, stats.grossOvertimeMinutes);
        assertEquals(150, stats.restMinutes);
        // The existing rest-balance accounting can remain internal; UI shows
        // gross registered overtime, which does NOT decrease on taking rest.
        assertEquals("۰۸:۰۰", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(stats.grossOvertimeMinutes)));
        assertEquals("۰۲:۳۰", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(stats.restMinutes)));
    }

    @Test public void durationUsesPersianNumeralsIncludingZeroAndOver24Hours() {
        assertEquals("۰۰:۰۰", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(0)));
        assertEquals("۰۲:۳۰", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(150)));
        assertEquals("۲۷:۲۵", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(1645)));
        assertEquals("۱۲۲:۰۵", com.eluqen.shiftcalendar.core.PersianDigits.of(
                CardexSummaryPresentation.clockDuration(7325)));
    }

    @Test public void clockDurationPaddedUnwrappedAndDefensive() {
        assertEquals("00:00", CardexSummaryPresentation.clockDuration(0));
        assertEquals("02:30", CardexSummaryPresentation.clockDuration(150));
        assertEquals("08:00", CardexSummaryPresentation.clockDuration(480));
        assertEquals("27:25", CardexSummaryPresentation.clockDuration(1645));
        assertEquals("120:05", CardexSummaryPresentation.clockDuration(7205));
        assertEquals("00:00", CardexSummaryPresentation.clockDuration(-1));
    }
    @Test public void zeroShowsBothHoursAndMinutes() {
        assertEquals("0 ساعت", CardexSummaryPresentation.hoursLabel(0));
        assertEquals("0 دقیقه", CardexSummaryPresentation.minutesLabel(0));
    }
    @Test public void fullHoursStaySeparateFromDays() {
        assertEquals("16 ساعت", CardexSummaryPresentation.hoursLabel(960));
        assertEquals("0 دقیقه", CardexSummaryPresentation.minutesLabel(960));
        assertEquals("2 روز", CardexSummaryPresentation.countLabel(2,"روز"));
    }
    @Test public void mixedMinutesHaveNoTwentyFourHourWrap() {
        assertEquals("27 ساعت", CardexSummaryPresentation.hoursLabel(1645));
        assertEquals("25 دقیقه", CardexSummaryPresentation.minutesLabel(1645));
    }
    @Test public void partialHourAndNegativeDefensiveFormatting() {
        assertEquals("0 ساعت", CardexSummaryPresentation.hoursLabel(45));
        assertEquals("45 دقیقه", CardexSummaryPresentation.minutesLabel(45));
        assertEquals("0 ساعت", CardexSummaryPresentation.hoursLabel(-10));
        assertEquals("0 دقیقه", CardexSummaryPresentation.minutesLabel(-10));
        assertEquals("0 مورد", CardexSummaryPresentation.countLabel(-1,"مورد"));
    }
}
