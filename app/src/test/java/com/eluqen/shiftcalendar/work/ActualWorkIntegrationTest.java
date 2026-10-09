package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.core.*;
import com.eluqen.shiftcalendar.data.*;
import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActualWorkIntegrationTest {
    private final PersianDate date=new PersianDate(1405,7,12);
    private final TimeZone zone=TimeZone.getTimeZone("Asia/Tehran");
    private final Profile day=p("D",420,900), evening=p("E",900,1380), night=p("N",1380,420);
    private final Profile off=new Profile("O","استراحت","ا",-1,-1,0,false,false);
    private final List<Profile> profiles=Arrays.asList(day,evening,night);
    private static Profile p(String code,int start,int end) { return new Profile(code,ShiftEngine.label(code),ShiftEngine.shortLabel(code),start,end,0,true,true); }
    private ShiftPlan plan(String company) { ShiftPlan p=new ShiftPlan();p.company=company;p.team="A";p.pattern="D,E,N,O";p.anchorJdn=date.toJdn();return p; }
    private CardexRecord record(int mode) { CardexRecord r=new CardexRecord();r.date=date.toString();r.jdn=date.toJdn();r.rest=mode==1;r.leave=mode==2;r.swap=mode==3;r.overtime=mode==4;r.restFullShift=mode==1;r.leaveFullShift=mode==2;return r; }
    private DayWorkResolver.State resolve(Profile base,CardexRecord r) { return DayWorkResolver.resolve(date,plan("سفارشی"),base,r,profiles); }
    private List<ReminderScheduler.NextReminder> reminders(DayWorkResolver.State state) {
        return ReminderScheduler.remindersForDay(state,zone,30,Long.MIN_VALUE/4,true);
    }
    @Test public void normalResolvesBaseAndSchedulesAtItsRealStart() {
        DayWorkResolver.State s=resolve(day,null); assertEquals("D",s.effectiveCode);
        assertEquals(1,reminders(s).size()); assertEquals(DayWorkResolver.civilEpoch(date,390,zone),reminders(s).get(0).triggerAtMillis);
    }
    @Test public void daySwapEveningUsesOnlyReplacementAlarm() {
        CardexRecord r=record(3);r.swapShiftCode="E";DayWorkResolver.State s=resolve(day,r);
        assertEquals("E",s.effectiveCode);assertEquals(1,reminders(s).size());
        assertEquals(DayWorkResolver.civilEpoch(date,870,zone),reminders(s).get(0).triggerAtMillis);
        assertTrue(reminders(s).get(0).message.contains("تعویض شیفت"));
    }
    @Test public void eveningSwapNightUsesActualNight() {
        CardexRecord r=record(3);r.swapShiftCode="N";DayWorkResolver.State s=resolve(evening,r);
        assertEquals("N",s.effectiveCode);assertEquals(1350*60000L,reminders(s).get(0).triggerAtMillis-DayWorkResolver.civilEpoch(date,0,zone));
    }
    @Test public void leaveSuppressesAllWorkAndShowsLeaveEverywhere() {
        DayWorkResolver.State s=resolve(day,record(2));assertTrue(reminders(s).isEmpty());assertEquals("مرخصی روزانه",s.actualTitle());assertEquals("",s.hoursText());
    }
    @Test public void restSuppressesAllWorkAndShowsRestEverywhere() {
        DayWorkResolver.State s=resolve(night,record(1));assertTrue(reminders(s).isEmpty());assertEquals("استراحت روزانه",s.actualTitle());
    }
    @Test public void ownOvertimeProducesNormalAndOvertimeAlarm() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";r.overtimeStart="15:00";r.overtimeEnd="22:00";r.overtimeDayOffset=0;r.overtimeType=OvertimePolicy.TYPE_REPLACEMENT;DayWorkResolver.State s=resolve(day,r);
        List<ReminderScheduler.NextReminder> a=reminders(s);assertEquals(2,a.size());assertEquals("shift",a.get(0).kind);assertEquals("overtime",a.get(1).kind);
        assertEquals(s.shiftEndEpoch(null,zone)-1800000L,a.get(1).triggerAtMillis);assertTrue(a.get(1).message.contains("پس از نوبت اصلی ثبت شده است"));
    }
    @Test public void offDayOvertimeProducesOnlyGoToWorkAlarm() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";r.overtimeStart="15:00";r.overtimeEnd="22:00";r.overtimeDayOffset=0;r.overtimeType=OvertimePolicy.TYPE_REPLACEMENT;DayWorkResolver.State s=resolve(off,r);
        assertEquals(1,reminders(s).size());assertEquals("overtime",reminders(s).get(0).kind);assertTrue(reminders(s).get(0).message.contains("اضافه‌کاری برای عصرکار ثبت شده است"));
        assertEquals("اضافه‌کاری — عصرکار",s.actualTitle());
    }
    @Test public void overtimeTypeAppearsInReminderTitle() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";r.overtimeStart="15:00";r.overtimeEnd="22:00";r.overtimeDayOffset=0;r.overtimeType=OvertimePolicy.TYPE_NECESSITY;
        DayWorkResolver.State s=resolve(off,r);
        assertTrue(reminders(s).get(0).title.contains("ضرورت کاری"));
    }
    @Test public void explicitOvertimeCannotReuseOwnShift() {
        CardexRecord r=record(4);r.overtimeShiftCode="D";r.overtimeStart="07:00";r.overtimeEnd="15:00";r.overtimeDayOffset=0;r.overtimeType=OvertimePolicy.TYPE_REPLACEMENT;
        DayWorkResolver.State s=resolve(day,r);
        assertFalse(s.hasOvertime);
        assertEquals(1,reminders(s).size());
    }
    @Test public void nextTargetUsesProfileTimesRatherThanNames() {
        Profile d=p("D",900,1380),e=p("E",420,900),n=p("N",1380,420);
        assertEquals("N",DayWorkResolver.nextContinuousProfile(d,Arrays.asList(e,n,d)).code);
        assertEquals("D",DayWorkResolver.nextContinuousProfile(e,Arrays.asList(n,e,d)).code);
    }
    @Test public void ambiguousNextTargetIsNeverGuessed() {
        assertNull(DayWorkResolver.nextContinuousProfile(day,Arrays.asList(day,evening,p("N",900,420))));
    }
    @Test public void customOverlappingProfileDoesNotChangeCoreNextShift() {
        assertEquals("E",DayWorkResolver.nextContinuousProfile(day,Arrays.asList(p("C1",900,960),day,evening,night)).code);
    }
    @Test public void noncontiguousOvertimeTargetIsAllowedWhenExplicit() {
        CardexRecord r=record(4);r.overtimeShiftCode="N";r.overtimeStart="23:00";r.overtimeEnd="06:00";r.overtimeDayOffset=0;
        DayWorkResolver.State s=resolve(day,r);
        assertTrue(s.hasOvertime);assertEquals(2,reminders(s).size());
        assertEquals(DayWorkResolver.civilEpoch(date,1380,zone),s.overtimeStartEpoch(null,zone));
    }
    @Test public void overtimeBeforeOwnShiftUsesPreviousDayOccurrence() {
        CardexRecord r=record(4);r.overtimeShiftCode="N";r.overtimeStart="23:00";r.overtimeEnd="06:00";r.overtimeDayOffset=-1;
        DayWorkResolver.State s=resolve(day,r);
        assertEquals(date.plusDays(-1),s.overtimeStartDate(zone));
        assertEquals(DayWorkResolver.civilEpoch(date,360,zone),s.overtimeEndEpoch(null,zone));
        assertFalse(s.overtimeEndsAtOwnShiftStart(null,zone));
    }
    @Test public void offDayOvertimeCanUseAnyExplicitShiftAndSameDay() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";r.overtimeStart="15:00";r.overtimeEnd="22:00";r.overtimeDayOffset=0;
        DayWorkResolver.State s=resolve(off,r);
        assertTrue(s.hasOvertime);assertEquals(date,s.overtimeStartDate(zone));
    }
    @Test public void eveningCanContinueIntoNightAsExplicitOvertime() {
        CardexRecord r=record(4);r.overtimeShiftCode="N";r.overtimeStart="23:00";r.overtimeEnd="06:00";r.overtimeDayOffset=0;
        DayWorkResolver.State s=resolve(evening,r);
        assertTrue(s.overtimeContinuesWork(null,zone));
        assertEquals(date,s.overtimeStartDate(zone));
    }
    @Test public void crossMidnightNormalShiftEndsFollowingCivilDay() {
        DayWorkResolver.State s=resolve(night,null);assertEquals(DayWorkResolver.civilEpoch(date.plusDays(1),420,zone),s.shiftEndEpoch(null,zone));
    }
    @Test public void crossMidnightOwnOvertimeAfterNightStartsNextMorning() {
        CardexRecord r=record(4);r.overtimeShiftCode="D";DayWorkResolver.State s=resolve(night,r);
        assertEquals(date.plusDays(1),s.overtimeStartDate(zone));assertEquals(s.shiftEndEpoch(null,zone),s.overtimeStartEpoch(null,zone));
        assertEquals(date.plusDays(1),reminders(s).get(1).date);
    }
    @Test public void eveningOvertimeNightStartsTonightAndEndsTomorrow() {
        CardexRecord r=record(4);r.overtimeShiftCode="N";DayWorkResolver.State s=resolve(evening,r);
        assertEquals(date,s.overtimeStartDate(zone));assertEquals(DayWorkResolver.civilEpoch(date.plusDays(1),420,zone),s.overtimeEndEpoch(null,zone));
    }
    @Test public void presetNightKeepsPreviousEveningConvention() {
        DayWorkResolver.State s=DayWorkResolver.resolve(date,plan("فولاد مبارکه"),night,null,profiles);
        assertEquals(date.plusDays(-1),s.shiftStartDate());assertEquals(DayWorkResolver.civilEpoch(date,420,zone),s.shiftEndEpoch(null,zone));
    }
    @Test public void presetNightOvertimeDayStartsNominalMorning() {
        CardexRecord r=record(4);r.overtimeShiftCode="D";DayWorkResolver.State s=DayWorkResolver.resolve(date,plan("فولاد مبارکه"),night,r,profiles);
        assertEquals(date,s.overtimeStartDate(zone));assertEquals(DayWorkResolver.civilEpoch(date,420,zone),s.overtimeStartEpoch(null,zone));
    }
    @Test public void presetOffNightOvertimeStartsPreviousEvening() {
        CardexRecord r=record(4);r.overtimeShiftCode="N";DayWorkResolver.State s=DayWorkResolver.resolve(date,plan("فولاد مبارکه"),off,r,profiles);
        assertEquals(date.plusDays(-1),s.overtimeStartDate(zone));assertTrue(s.nextLabel(date.plusDays(-1),zone).startsWith("امشب:"));
    }
    @Test public void dstBoundaryUsesCivilProfileEndRatherThanFixedDuration() {
        TimeZone berlin=TimeZone.getTimeZone("Europe/Berlin");PersianDate d=JalaliCalendar.fromGregorian(2026,10,24);
        DayWorkResolver.State s=DayWorkResolver.resolve(d,plan("سفارشی"),night,null,profiles);
        assertEquals(DayWorkResolver.civilEpoch(d.plusDays(1),420,berlin),s.shiftEndEpoch(null,berlin));
        assertEquals(9*3600000L,s.shiftEndEpoch(null,berlin)-s.shiftStartEpoch(null,berlin));
    }
    @Test public void swapToSameBaseIsRejected() {
        CardexRecord r=record(3);r.swapShiftCode="D";DayWorkResolver.State s=resolve(day,r);assertFalse(s.swapApplied);assertTrue(reminders(s).isEmpty());
    }
    @Test public void offDayCannotBeSwappedIntoAWorkShift() {
        CardexRecord r=record(3);r.swapShiftCode="D";
        DayWorkResolver.State s=resolve(off,r);
        assertFalse(s.swapApplied);
        assertTrue(s.shiftSuppressed);
        assertTrue(reminders(s).isEmpty());
    }
    @Test public void unknownSwapCodeNeverFallsBackToBaseAlarm() {
        CardexRecord r=record(3);r.swapShiftCode="ZZ";assertTrue(reminders(resolve(day,r)).isEmpty());
    }
    @Test public void legacySwapTimeRecoversKnownProfile() {
        CardexRecord r=record(3);r.swapTime="15:00";assertEquals("E",resolve(day,r).effectiveCode);
    }
    @Test public void legacyUnknownSwapTimeKeepsWorkingOccurrenceWithoutCrash() {
        CardexRecord r=record(3);r.swapTime="16:10";DayWorkResolver.State s=resolve(day,r);
        assertTrue(s.swapApplied);assertEquals(970,s.effectiveProfile.startMinute);assertEquals(480,DayWorkResolver.durationMinutes(s.effectiveProfile.startMinute,s.effectiveProfile.endMinute));assertEquals(1,reminders(s).size());
    }
    @Test public void legacyOvertimeIntervalRemainsCompatible() {
        CardexRecord r=record(4);r.overtimeStart="23:00";r.overtimeEnd="07:00";DayWorkResolver.State s=resolve(off,r);
        assertTrue(s.hasOvertime);assertEquals(480,DayWorkResolver.durationMinutes(s.overtimeStartMinute,s.overtimeEndMinute));
    }
    @Test public void conflictingLegacyRowOnlyUsesRestEverywhere() {
        CardexRecord r=record(1);r.leave=true;r.swap=true;r.swapShiftCode="E";r.overtime=true;r.overtimeShiftCode="N";
        DayWorkResolver.State s=resolve(day,r);assertEquals("استراحت روزانه",s.actualTitle());assertTrue(reminders(s).isEmpty());assertFalse(r.summary().contains("تعویض"));assertEquals(0,CardexStats.calculate(Arrays.asList(r),false).swapCount);
    }
    @Test public void disabledStaleFieldsCannotSuppressNormalShift() {
        CardexRecord r=record(0);r.leaveStart="07:00";r.overtimeShiftCode="E";assertEquals(1,reminders(resolve(day,r)).size());
    }
    @Test public void switchingToLeaveClearsEveryOldTargetAndPreservesNote() {
        CardexRecord r=record(3);r.swapShiftCode="E";r.swapTime="15:00";r.swapNote="همکار";r.overtimeShiftCode="N";r.note="متن";
        r.clearOperational();r.leave=true;r.normalizeOperational();assertEquals("",r.swapShiftCode);assertEquals("",r.swapNote);assertEquals("",r.swapTime);assertEquals("",r.overtimeShiftCode);assertEquals("متن",r.note);
    }
    @Test public void normalizedNewSaveHasExactlyOneMode() {
        CardexRecord r=record(1);r.leave=true;r.swap=true;r.overtime=true;r.normalizeOperational();assertTrue(r.rest);assertFalse(r.leave||r.swap||r.overtime);assertTrue(r.restFullShift);
    }
    @Test public void normalizationOfDraftDoesNotMutateOriginal() {
        CardexRecord r=record(3);r.swapShiftCode="E";CardexRecord draft=r.copy();draft.clearOperational();assertEquals("E",r.swapShiftCode);assertTrue(r.swap);
    }
    @Test public void noOverrideMeansNoteCanCoexistWithBasePattern() {
        CardexRecord r=record(0);r.note="متن";assertEquals("D",resolve(day,r).effectiveCode);assertEquals(1,reminders(resolve(day,r)).size());
    }
    @Test public void normalAndOvertimeNotificationIdsCannotCollide() {
        Set<Integer> ids=new HashSet<>();for(int i=0;i<370;i++) {assertTrue(ids.add(ReminderScheduler.notificationId(date.plusDays(i),"shift:D")));assertTrue(ids.add(ReminderScheduler.notificationId(date.plusDays(i),"overtime:E")));}
    }
    @Test public void all128AlarmRequestIdsAreUnique() {
        Set<Integer> ids=new HashSet<>();for(int i=0;i<128;i++)assertTrue(ids.add(ReminderScheduler.alarmRequestCode(i)));assertEquals(128,ids.size());
    }
    @Test public void previewAndSchedulingUseExactSameDayCandidates() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";DayWorkResolver.State s=resolve(day,r);
        List<ReminderScheduler.NextReminder> all=reminders(s);
        long now=all.get(0).triggerAtMillis+1;
        List<ReminderScheduler.NextReminder> remaining=ReminderScheduler.remindersForDay(s,zone,30,now,true);
        assertEquals(1,remaining.size());assertEquals(all.get(1).message,remaining.get(0).message);assertEquals(all.get(1).triggerAtMillis,remaining.get(0).triggerAtMillis);
    }
    @Test public void widgetTodayAndTomorrowUseActualSwapAndLeaveTitles() {
        CardexRecord r=record(3);r.swapShiftCode="E";assertEquals("عصرکار (تعویض شیفت)",resolve(day,r).actualTitle());
        assertEquals("فردا: مرخصی روزانه",resolve(day,record(2)).nextLabel(date.plusDays(-1),zone));
    }
    @Test public void widgetOwnOvertimeShowsBaseAndTarget() {
        CardexRecord r=record(4);r.overtimeShiftCode="E";DayWorkResolver.State s=resolve(day,r);assertEquals("روزکار + اضافه‌کاری عصرکار",s.actualTitle());assertTrue(s.hoursText().contains("15:00 تا 23:00"));
    }
    @Test public void hourlyLeaveKeepsBaseWorkAndShowsItsRange() {
        CardexRecord r=record(2);r.leaveFullShift=false;r.leaveStart="10:00";r.leaveEnd="12:00";
        DayWorkResolver.State s=resolve(day,r);
        assertFalse(s.shiftSuppressed);assertTrue(s.hasBaseWork());assertTrue(s.isHourlyAbsence());
        assertEquals(120,s.absenceMinutes());assertEquals(360,s.baseWorkedMinutes());
        assertTrue(s.actualTitle().contains("مرخصی ساعتی 10:00–12:00"));
        assertEquals(1,reminders(s).size());
    }
    @Test public void hourlyRestAtShiftStartMovesReminderToReturnTime() {
        CardexRecord r=record(1);r.restFullShift=false;r.restStart="07:00";r.restEnd="11:00";
        DayWorkResolver.State s=resolve(day,r);
        assertFalse(s.shiftSuppressed);assertTrue(s.reminderStartsAfterAbsence());
        assertEquals(660,s.shiftReminderStartMinute());
        List<ReminderScheduler.NextReminder> a=reminders(s);
        assertEquals(1,a.size());
        assertEquals(DayWorkResolver.civilEpoch(date,630,zone),a.get(0).triggerAtMillis);
        assertTrue(a.get(0).message.contains("استراحت ساعتی"));
    }
    @Test public void timeParsingSupportsOldPersianAndArabicDigits() {
        assertEquals(1380,DayWorkResolver.parseMinute("۲۳:۰۰"));assertEquals(420,DayWorkResolver.parseMinute("٠٧:٠٠"));
    }
    @Test public void corruptEndTimeNeverCreatesAWorkAlarm() {
        assertTrue(reminders(resolve(p("D",420,420),null)).isEmpty());assertTrue(reminders(resolve(p("D",420,2000),null)).isEmpty());
    }
    @Test public void reminderPreferenceForReplacementIsHonored() {
        Profile muted=new Profile("E","عصرکار","ع",900,1380,0,true,false);CardexRecord r=record(3);r.swapShiftCode="E";
        DayWorkResolver.State s=DayWorkResolver.resolve(date,plan("سفارشی"),day,r,Arrays.asList(day,muted,night));assertTrue(reminders(s).isEmpty());assertEquals("E",s.effectiveCode);
    }
    @Test public void hourlyAbsenceMustStayInsideShiftAndCannotEqualWholeShift() {
        assertTrue(AbsencePolicy.hourlyRangeAllowed(day,480,600));
        assertFalse(AbsencePolicy.hourlyRangeAllowed(day,360,480));
        assertFalse(AbsencePolicy.hourlyRangeAllowed(day,420,900));
        assertFalse(AbsencePolicy.hourlyRangeAllowed(night,420,480));
        assertTrue(AbsencePolicy.hourlyRangeAllowed(night,1380,60));
    }
    @Test public void overtimePresenceAllowsFullTargetAndRequiresAtLeastOneHour() {
        assertTrue(OvertimePolicy.actualRangeAllowed(day,480,840));
        assertTrue(OvertimePolicy.actualRangeAllowed(day,420,900));
        assertTrue(OvertimePolicy.actualRangeAllowed(day,420,480));
        assertFalse(OvertimePolicy.actualRangeAllowed(day,420,479));
        assertFalse(OvertimePolicy.actualRangeAllowed(day,360,480));
        assertTrue(OvertimePolicy.actualRangeAllowed(night,1380,60));
        assertTrue(OvertimePolicy.actualRangeAllowed(night,1380,420));
    }
    @Test public void persistedFullTargetOvertimeRemainsReadableAndEditable() {
        CardexRecord r=record(4);
        r.overtimeShiftCode="E";
        r.overtimeStart="15:00";
        r.overtimeEnd="23:00";
        r.overtimeDayOffset=0;
        r.overtimeType=OvertimePolicy.TYPE_REPLACEMENT;
        DayWorkResolver.State s=resolve(day,r);
        assertTrue(s.hasOvertime);
        assertTrue(OvertimePolicy.recordedRangeAllowed(evening,900,1380));
        assertTrue(OvertimePolicy.actualRangeAllowed(evening,900,1380));
    }
    @Test public void premiumWorkCountsOnlyActualPresence() {
        assertEquals(480, PremiumWorkPolicy.workedMinutes(resolve(day,null)));
        assertEquals(0, PremiumWorkPolicy.workedMinutes(resolve(day,record(2))));
        assertEquals(0, PremiumWorkPolicy.workedMinutes(resolve(day,record(1))));

        CardexRecord leave=record(2);
        leave.leaveFullShift=false;
        leave.leaveStart="10:00";
        leave.leaveEnd="12:00";
        assertEquals(360, PremiumWorkPolicy.workedMinutes(resolve(day,leave)));

        CardexRecord rest=record(1);
        rest.restFullShift=false;
        rest.restStart="07:00";
        rest.restEnd="11:00";
        assertEquals(240, PremiumWorkPolicy.workedMinutes(resolve(day,rest)));
    }
}
