package com.eluqen.shiftcalendar.ui;

import static org.junit.Assert.*;
import org.junit.Test;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.hijri.HijriResolver;

public final class HijriAdjustmentOptionsTest {
    @Test public void allFiveManualCorrectionsHaveDistinctOptions() {
        java.util.Set<String> labels = new java.util.HashSet<>();
        for (int offset = HijriAdjustmentOptions.MIN;
                 offset <= HijriAdjustmentOptions.MAX; offset++) {
            assertTrue(labels.add(HijriAdjustmentOptions.optionLabel(offset)));
        }
        assertEquals(5, labels.size());
        assertTrue(HijriAdjustmentOptions.optionLabel(0).contains("بدون اصلاح"));
    }

    @Test public void savedZeroMeansNoManualCorrection() {
        assertEquals("بدون اصلاح دستی", HijriAdjustmentOptions.savedDescription(0));
        assertEquals("یک روز عقب‌تر", HijriAdjustmentOptions.savedDescription(-1));
        assertEquals("دو روز جلوتر", HijriAdjustmentOptions.savedDescription(2));
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidPositiveOffsetIsRejected() {
        HijriAdjustmentOptions.optionLabel(3);
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidNegativeOffsetIsRejected() {
        HijriAdjustmentOptions.optionLabel(-3);
    }

    @Test public void previewUsesChosenOffsetWithoutChangingBaselineConversion() {
        PersianDate date = new PersianDate(1405, 7, 24);
        HijriResolver.ServerCalendar server = d -> HijriDate.fromJdn(d.toJdn(), 0);
        HijriDate baseline = HijriResolver.resolve(date, 0, server);
        for (int offset = -2; offset <= 2; offset++) {
            HijriDate result = HijriResolver.resolve(date, offset, server);
            HijriDate expected = server.resolve(date.plusDays(offset));
            assertEquals(expected.year, result.year);
            assertEquals(expected.month, result.month);
            assertEquals(expected.day, result.day);
        }
        HijriDate after = HijriResolver.resolve(date, 0, server);
        assertEquals(baseline.year, after.year);
        assertEquals(baseline.month, after.month);
        assertEquals(baseline.day, after.day);
    }
}
