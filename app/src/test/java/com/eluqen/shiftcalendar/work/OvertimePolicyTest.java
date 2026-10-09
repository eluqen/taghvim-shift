package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.core.PersianDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class OvertimePolicyTest {
    private static Profile work(String code,int start,int end) {
        return new Profile(code,code,code,start,end,0,true,true);
    }
    private static final Profile N=work("N",1380,420);
    private static final Profile D=work("D",420,900);
    private static final Profile E=work("E",900,1380);
    private static final Profile O=new Profile("O","O","O",-1,-1,0,false,false);
    private static final Profile C=work("C1",480,960);

    @Test public void nightMayOnlyUseDayOrEvening() {
        assertTrue(OvertimePolicy.targetAllowed(N,D));
        assertTrue(OvertimePolicy.targetAllowed(N,E));
        assertFalse(OvertimePolicy.targetAllowed(N,N));
        assertFalse(OvertimePolicy.targetAllowed(N,C));
    }
    @Test public void dayMayOnlyUseNightOrEvening() {
        assertTrue(OvertimePolicy.targetAllowed(D,N));
        assertTrue(OvertimePolicy.targetAllowed(D,E));
        assertFalse(OvertimePolicy.targetAllowed(D,D));
    }
    @Test public void eveningMayOnlyUseNightOrDay() {
        assertTrue(OvertimePolicy.targetAllowed(E,N));
        assertTrue(OvertimePolicy.targetAllowed(E,D));
        assertFalse(OvertimePolicy.targetAllowed(E,E));
    }
    @Test public void offDayMayUseAnyCoreShift() {
        assertTrue(OvertimePolicy.targetAllowed(O,N));
        assertTrue(OvertimePolicy.targetAllowed(O,D));
        assertTrue(OvertimePolicy.targetAllowed(O,E));
        assertFalse(OvertimePolicy.targetAllowed(O,C));
    }
    @Test public void actualPresenceMayUseFullTargetButMustStayInsideAndBeAtLeastOneHour() {
        assertTrue(OvertimePolicy.actualRangeAllowed(D,420,900));
        assertTrue(OvertimePolicy.actualRangeAllowed(D,420,480));
        assertFalse(OvertimePolicy.actualRangeAllowed(D,420,479));
        assertFalse(OvertimePolicy.actualRangeAllowed(D,360,900));
        assertFalse(OvertimePolicy.actualRangeAllowed(D,420,960));
    }
    @Test public void nightRangeHandlesMidnightAndAllowsFullTarget() {
        assertTrue(OvertimePolicy.actualRangeAllowed(N,1380,420));
        assertTrue(OvertimePolicy.actualRangeAllowed(N,0,420));
        assertTrue(OvertimePolicy.actualRangeAllowed(N,1380,360));
        assertTrue(OvertimePolicy.actualRangeAllowed(N,1380,0));
        assertFalse(OvertimePolicy.actualRangeAllowed(N,1380,1439));
        assertFalse(OvertimePolicy.actualRangeAllowed(N,1320,420));
        assertFalse(OvertimePolicy.actualRangeAllowed(N,1380,480));
    }
    @Test public void nominalDayMappingMatchesThreeShiftOperation() {
        PersianDate date=new PersianDate(1405,7,12);
        ShiftPlan plan=new ShiftPlan();
        assertEquals(-1,OvertimePolicy.occurrenceOffset(plan,date,N));
        assertEquals(0,OvertimePolicy.occurrenceOffset(plan,date,D));
        assertEquals(0,OvertimePolicy.occurrenceOffset(plan,date,E));
    }
    @Test public void lateArrivalToNightAfterMidnightUsesActualCivilDay() {
        PersianDate date=new PersianDate(1405,7,12);
        ShiftPlan plan=new ShiftPlan();
        assertEquals(0,OvertimePolicy.actualStartOffset(plan,date,N,60));
        assertEquals(-1,OvertimePolicy.actualStartOffset(plan,date,N,1380));
    }
    @Test public void unspecifiedTypeRemainsUnspecified() {
        assertEquals("",OvertimePolicy.normalizeType(""));
        assertEquals("",OvertimePolicy.normalizeType("UNKNOWN"));
        assertEquals(OvertimePolicy.TYPE_REPLACEMENT,
                OvertimePolicy.normalizeType(OvertimePolicy.TYPE_REPLACEMENT));
        assertEquals(OvertimePolicy.TYPE_NECESSITY,
                OvertimePolicy.normalizeType(OvertimePolicy.TYPE_NECESSITY));
    }
    @Test public void necessityNeverOffersNightShift() {
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,N,N));
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,D,N));
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,E,N));
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,O,N));
    }
    @Test public void necessityUsesOnlyDayAndEveningWithBaseSpecificExclusion() {
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,N,D));
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,N,E));
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,D,D));
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,D,E));
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,E,D));
        assertFalse(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,E,E));
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,O,D));
        assertTrue(OvertimePolicy.targetAllowed(OvertimePolicy.TYPE_NECESSITY,O,E));
    }
    @Test public void typeNamesAreStableAndCompanyNeutral() {
        assertEquals("پوشش شیفت",OvertimePolicy.typeTitle(OvertimePolicy.TYPE_REPLACEMENT));
        assertEquals("ضرورت کاری",OvertimePolicy.typeTitle(OvertimePolicy.TYPE_NECESSITY));
    }
    @Test public void oneOvertimeTargetMeansAtMostOwnShiftPlusOneExtraShift() {
        assertFalse(OvertimePolicy.targetAllowed(N,N));
        assertFalse(OvertimePolicy.targetAllowed(D,D));
        assertFalse(OvertimePolicy.targetAllowed(E,E));
        assertTrue(OvertimePolicy.targetAllowed(N,D));
        assertTrue(OvertimePolicy.targetAllowed(D,E));
        assertTrue(OvertimePolicy.targetAllowed(E,N));
    }

}
