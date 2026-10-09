package com.eluqen.shiftcalendar.work;

import android.content.Context;
import com.eluqen.shiftcalendar.core.GregorianDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import com.eluqen.shiftcalendar.ui.GeneralWork;
import com.eluqen.shiftcalendar.ui.ShiftExtras;
import com.eluqen.shiftcalendar.ui.ShiftOccurrence;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** The one operational truth for a nominal Persian date; the pattern is never mutated. */
public final class DayWorkResolver {
    public static final int MODE_NORMAL=0, MODE_REST=1, MODE_LEAVE=2, MODE_SWAP=3, MODE_OVERTIME=4;
    private DayWorkResolver() {}

    public static Profile baseProfile(Context c, PersianDate d, ShiftPlan p) {
        return ShiftPreferences.get(c, GeneralWork.adjust(c,d,p,
                ShiftEngine.tokenFor(d.toJdn(),p.anchorJdn,p.pattern)));
    }
    public static String baseCode(Context c, PersianDate d, ShiftPlan p) { return baseProfile(c,d,p).code; }
    public static int mode(CardexRecord r) {
        if(r==null) return MODE_NORMAL;
        // Only enabled flags are operational; disabled fields in old rows are metadata.
        if(r.rest) return MODE_REST;
        if(r.leave) return MODE_LEAVE;
        if(r.swap) return MODE_SWAP;
        if(r.overtime) return MODE_OVERTIME;
        return MODE_NORMAL;
    }
    public static String[] workingCodes(Context c) {
        java.util.LinkedHashSet<String> codes=new java.util.LinkedHashSet<>();
        codes.add("D"); codes.add("E"); codes.add("N");
        for(String code:ShiftExtras.visibleCodes(c)) if(ShiftPreferences.get(c,code).working) codes.add(code);
        return codes.toArray(new String[0]);
    }
    private static List<Profile> profiles(Context c) {
        List<Profile> out=new ArrayList<>();
        for(String code:workingCodes(c)) out.add(ShiftPreferences.get(c,code));
        return out;
    }
    public static boolean validWork(Profile p) {
        return p!=null && p.working && p.startMinute>=0 && p.startMinute<1440
                && p.endMinute>=0 && p.endMinute<1440 && p.startMinute!=p.endMinute;
    }
    /** The three core profiles determine temporal order; names/titles never do. */
    public static Profile nextContinuousProfile(Profile base,List<Profile> profiles) {
        if(!validWork(base)) return null;
        Profile found=null;
        for(Profile p:profiles) {
            if(!("D".equals(p.code)||"E".equals(p.code)||"N".equals(p.code))) continue;
            if(validWork(p) && !p.code.equals(base.code) && p.startMinute==base.endMinute) {
                if(found!=null) return null; // ambiguous schedules require an explicit choice
                found=p;
            }
        }
        return found;
    }
    public static Profile nextContinuousProfile(Context c,Profile base) { return nextContinuousProfile(base,profiles(c)); }
    private static Profile find(List<Profile> profiles,String code) {
        for(Profile p:profiles) if(p.code.equals(clean(code))) return p;
        return null;
    }
    public static State resolve(Context c,PersianDate date,ShiftPlan plan) {
        return resolve(c,date,plan,AppDatabase.get(c).cardex(date.toString()));
    }
    public static State resolve(Context c,PersianDate date,ShiftPlan plan,CardexRecord record) {
        return resolve(date,plan,baseProfile(c,date,plan),record,profiles(c));
    }
    /** Pure resolver entry point, also used by integration tests with real model fixtures. */
    public static State resolve(PersianDate date,ShiftPlan plan,Profile base,CardexRecord record,List<Profile> profiles) {
        int mode=mode(record);
        Profile effective=base, overtime=null;
        boolean swap=false, continues=false;
        if(mode==MODE_SWAP && validWork(base)) {
            if(!clean(record.swapShiftCode).isEmpty()) {
                Profile target=find(profiles,record.swapShiftCode);
                if(validWork(target) && !target.code.equals(base.code)) { effective=target; swap=true; }
            } else {
                int start=parseMinute(record.swapTime);
                if(start>=0) {
                    // Recover a real legacy destination when uniquely identifiable.
                    Profile match=null;
                    boolean ambiguous=false;
                    for(Profile target:profiles) if(validWork(target) && target.startMinute==start && !target.code.equals(base.code)) {
                        if(match!=null) ambiguous=true; else match=target;
                    }
                    if(match!=null&&!ambiguous) effective=match;
                    else {
                        int duration=validWork(base)?durationMinutes(base.startMinute,base.endMinute):480;
                        effective=new Profile("LEGACY_SWAP","تعویض شیفت ثبت‌شده","ت",start,(start+duration)%1440,base.color,true,base.reminder);
                    }
                    swap=true;
                }
            }
        }
        if(mode==MODE_OVERTIME) {
            String code=clean(record.overtimeShiftCode);
            int start=parseMinute(record.overtimeStart),end=parseMinute(record.overtimeEnd);
            Profile target=code.isEmpty()?null:find(profiles,code);
            if(validWork(target)) {
                if(record.overtimeDayOffset!=99) {
                    // Read-side compatibility: old persisted rows may cover the full
                    // target shift. New/editor saves use the stricter actualRangeAllowed().
                    if(OvertimePolicy.targetAllowed(record.overtimeType,base,target)
                            && OvertimePolicy.recordedRangeAllowed(target,start,end)) {
                        overtime=new Profile(target.code,target.title,target.shortTitle,
                                start,end,target.color,true,target.reminder);
                    }
                } else if(start>=0 && end>=0 && start!=end) {
                    // Legacy target record keeps its historical custom interval.
                    overtime=new Profile(target.code,target.title,target.shortTitle,
                            start,end,target.color,true,target.reminder);
                } else {
                    overtime=target;
                }
            } else if(start>=0 && end>=0 && start!=end && record.overtimeDayOffset==99) {
                overtime=new Profile("LEGACY_OT","اضافه‌کاری ثبت‌شده","ا",start,end,base.color,true,true);
            } else if(base.working && record.overtimeDayOffset==99) {
                // Historical auto-next behavior is retained only for old rows.
                overtime=nextContinuousProfile(base,profiles);
            }
            if(overtime!=null) {
                if(record.overtimeDayOffset==99) {
                    continues=validWork(base)&&overtime.startMinute==base.endMinute;
                } else if(validWork(base)) {
                    PersianDate baseStart=(PersianDate)ShiftOccurrence.startDate(plan,date,base);
                    PersianDate baseEnd=baseStart.plusDays(base.endMinute<=base.startMinute?1:0);
                    PersianDate otStart=date.plusDays(record.overtimeDayOffset);
                    continues=baseEnd.equals(otStart)&&base.endMinute==overtime.startMinute;
                }
            }
        }
        boolean fullRest=mode==MODE_REST&&fullDayAbsence(record,base,true);
        boolean fullLeave=mode==MODE_LEAVE&&fullDayAbsence(record,base,false);
        boolean suppressed=fullRest||fullLeave||(mode==MODE_SWAP&&!swap);
        String reason=fullRest?"استراحت روزانه":fullLeave?"مرخصی روزانه":
                (mode==MODE_SWAP&&!swap)?"تعویض شیفت نامعتبر":"";
        return new State(date,plan,record,mode,base.code,base,effective.code,effective,swap,
                suppressed,reason,overtime!=null,overtime,continues);
    }
    private static boolean fullDayAbsence(
            CardexRecord record, Profile base, boolean rest) {
        if (record == null) return false;
        boolean full = rest ? record.restFullShift : record.leaveFullShift;
        if (full) return true;
        String from = rest ? record.restStart : record.leaveStart;
        String to = rest ? record.restEnd : record.leaveEnd;
        int start = parseMinute(from);
        int end = parseMinute(to);
        // Legacy enabled rows without a valid hourly range remain full-day.
        return !AbsencePolicy.hourlyRangeAllowed(base, start, end);
    }

    public static int parseMinute(String value) {
        String s=clean(value); StringBuilder normalized=new StringBuilder();
        for(int i=0;i<s.length();i++) { char ch=s.charAt(i);
            if(ch>='۰'&&ch<='۹') ch=(char)('0'+ch-'۰');
            else if(ch>='٠'&&ch<='٩') ch=(char)('0'+ch-'٠');
            normalized.append(ch);
        }
        s=normalized.toString();
        if(!s.matches("[0-9]{1,2}:[0-9]{2}")) return -1;
        try { String[] parts=s.split(":"); int h=Integer.parseInt(parts[0]),m=Integer.parseInt(parts[1]);
            return h>=0&&h<24&&m>=0&&m<60?h*60+m:-1;
        } catch(RuntimeException e) { return -1; }
    }
    public static int durationMinutes(int start,int end) {
        if(start<0||end<0||start>=1440||end>=1440||start==end) return 0;
        return end>start?end-start:1440-start+end;
    }
    public static long civilEpoch(PersianDate date,int minute,TimeZone zone) {
        GregorianDate g=JalaliCalendar.toGregorian(date);
        Calendar c=Calendar.getInstance(zone,Locale.US); c.clear();
        c.set(g.year,g.month-1,g.day,minute/60,minute%60,0);
        return c.getTimeInMillis();
    }
    public static PersianDate dateAt(long millis,TimeZone zone) {
        Calendar c=Calendar.getInstance(zone,Locale.US); c.setTimeInMillis(millis);
        return JalaliCalendar.fromGregorian(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
    }
    public static final class State {
        public final PersianDate date;
        public final ShiftPlan plan;
        public final CardexRecord cardex;
        public final int mode;
        public final String baseCode,effectiveCode,suppressionReason;
        public final Profile baseProfile,effectiveProfile,overtimeProfile;
        public final boolean swapApplied,shiftSuppressed,hasOvertime,overtimeFromOwnShift;
        public final int overtimeStartMinute,overtimeEndMinute,overtimeDayOffset;
        State(PersianDate date,ShiftPlan plan,CardexRecord cardex,int mode,String baseCode,Profile base,
                String effectiveCode,Profile effective,boolean swap,boolean suppressed,String reason,
                boolean hasOvertime,Profile overtime,boolean continues) {
            this.date=date;this.plan=plan;this.cardex=cardex;this.mode=mode;this.baseCode=baseCode;this.baseProfile=base;
            this.effectiveCode=effectiveCode;this.effectiveProfile=effective;this.swapApplied=swap;
            this.shiftSuppressed=suppressed;this.suppressionReason=reason;this.hasOvertime=hasOvertime;
            this.overtimeProfile=overtime;this.overtimeFromOwnShift=continues;
            this.overtimeStartMinute=overtime==null?-1:overtime.startMinute;
            this.overtimeEndMinute=overtime==null?-1:overtime.endMinute;
            this.overtimeDayOffset=cardex==null?99:cardex.overtimeDayOffset;
        }
        public boolean shouldScheduleShift() { return !shiftSuppressed&&validWork(effectiveProfile)&&effectiveProfile.reminder; }
        public boolean hasBaseWork() { return !shiftSuppressed&&validWork(effectiveProfile); }
        public boolean isAbsenceMode() { return mode==MODE_REST||mode==MODE_LEAVE; }
        public boolean isFullDayAbsence() { return isAbsenceMode()&&shiftSuppressed; }
        public boolean isHourlyAbsence() { return isAbsenceMode()&&!shiftSuppressed&&cardex!=null; }
        public int absenceStartMinute() {
            if(!isHourlyAbsence()) return -1;
            return parseMinute(mode==MODE_REST?cardex.restStart:cardex.leaveStart);
        }
        public int absenceEndMinute() {
            if(!isHourlyAbsence()) return -1;
            return parseMinute(mode==MODE_REST?cardex.restEnd:cardex.leaveEnd);
        }
        public int absenceMinutes() {
            int start=absenceStartMinute(),end=absenceEndMinute();
            return AbsencePolicy.hourlyRangeAllowed(effectiveProfile,start,end)
                    ? durationMinutes(start,end):0;
        }
        public String absenceTitle() {
            if(mode==MODE_REST) return isFullDayAbsence()?"استراحت روزانه":"استراحت ساعتی";
            if(mode==MODE_LEAVE) return isFullDayAbsence()?"مرخصی روزانه":"مرخصی ساعتی";
            return "";
        }
        public int baseWorkedMinutes() {
            if(!hasBaseWork()) return 0;
            int total=durationMinutes(effectiveProfile.startMinute,effectiveProfile.endMinute);
            return Math.max(0,total-absenceMinutes());
        }
        public Profile displayProfile() { return hasOvertime&&!baseProfile.working?overtimeProfile:effectiveProfile; }
        public String effectiveTitle() { return effectiveProfile.title+(swapApplied?" (تعویض شیفت)":""); }
        public String overtimeTitle() { return overtimeProfile==null?"اضافه‌کاری":overtimeProfile.title; }
        public String actualTitle() {
            if(shiftSuppressed) return suppressionReason;
            String title=hasOvertime&&!baseProfile.working?"اضافه‌کاری — "+overtimeTitle():effectiveTitle();
            if(isHourlyAbsence()) {
                title+=" • "+absenceTitle()+" "
                        +ShiftPreferences.formatMinute(absenceStartMinute())
                        +"–"+ShiftPreferences.formatMinute(absenceEndMinute());
            }
            if(hasOvertime&&baseProfile.working) title+=" + اضافه‌کاری "+overtimeTitle();
            if(mode==MODE_OVERTIME&&!hasOvertime) title+=" (شیفت اضافه‌کاری نامعتبر)";
            return title;
        }
        public String shortTitle() {
            if(shiftSuppressed) return suppressionReason;
            Profile p=displayProfile();
            String text=p.shortTitle+(isHourlyAbsence()?" • ساعتی":"");
            return text+(hasOvertime?" +":"");
        }
        public String hoursText() {
            if(shiftSuppressed) return "";
            Profile p=displayProfile(); if(!validWork(p)) return "";
            String text=ShiftPreferences.formatMinute(p.startMinute)+" تا "+ShiftPreferences.formatMinute(p.endMinute);
            if(isHourlyAbsence()) text+=" • "+absenceTitle()+" "
                    +ShiftPreferences.formatMinute(absenceStartMinute())+"–"
                    +ShiftPreferences.formatMinute(absenceEndMinute());
            if(hasOvertime&&baseProfile.working) text+=" + "+ShiftPreferences.formatMinute(overtimeStartMinute)+" تا "+ShiftPreferences.formatMinute(overtimeEndMinute);
            return text;
        }
        public String nextLabel(PersianDate today,TimeZone zone) {
            if(shiftSuppressed) return "فردا: "+actualTitle();
            long start=hasOvertime&&!baseProfile.working?overtimeStartEpoch(null,zone):shiftStartEpoch(null,zone);
            PersianDate civil=dateAt(start,zone);
            return (civil.equals(today)?"امشب: ":"فردا: ")+actualTitle();
        }
        public long shiftStartEpoch(Context context,TimeZone zone) { return ShiftOccurrence.startEpoch(context,plan,date,effectiveProfile,zone); }
        public boolean reminderStartsAfterAbsence() {
            return isHourlyAbsence()
                    && absenceStartMinute()==effectiveProfile.startMinute
                    && absenceMinutes()>0;
        }
        public int shiftReminderStartMinute() {
            return reminderStartsAfterAbsence()
                    ? absenceEndMinute() : effectiveProfile.startMinute;
        }
        public long shiftReminderStartEpoch(Context context,TimeZone zone) {
            long start=shiftStartEpoch(context,zone);
            if(!reminderStartsAfterAbsence()) return start;
            int offset=absenceMinutes();
            return start+offset*60000L;
        }
        public long shiftEndEpoch(Context context,TimeZone zone) {
            PersianDate start=shiftStartDate();
            return civilEpoch(start.plusDays(effectiveProfile.endMinute<=effectiveProfile.startMinute?1:0),effectiveProfile.endMinute,zone);
        }
        public PersianDate shiftStartDate() { return (PersianDate)ShiftOccurrence.startDate(plan,date,effectiveProfile); }
        public PersianDate overtimeStartDate() { return overtimeStartDate(TimeZone.getTimeZone("Asia/Tehran")); }
        public PersianDate overtimeStartDate(TimeZone zone) { return hasOvertime?dateAt(overtimeStartEpoch(null,zone),zone):date; }
        public long overtimeStartEpoch(Context context,TimeZone zone) {
            if(!hasOvertime) return Long.MIN_VALUE/4;
            if(overtimeDayOffset!=99) {
                return civilEpoch(date.plusDays(overtimeDayOffset),overtimeStartMinute,zone);
            }
            if(overtimeFromOwnShift) return shiftEndEpoch(context,zone);
            long start=ShiftOccurrence.startEpoch(context,plan,date,overtimeProfile,zone);
            // Legacy rows keep their historical occurrence semantics.
            if(validWork(baseProfile)) {
                long end=shiftEndEpoch(context,zone);
                while(start<end) start=civilEpoch(dateAt(start,zone).plusDays(1),overtimeStartMinute,zone);
            }
            return start;
        }
        public long overtimeEndEpoch(Context context,TimeZone zone) {
            PersianDate start=dateAt(overtimeStartEpoch(context,zone),zone);
            return civilEpoch(start.plusDays(overtimeEndMinute<=overtimeStartMinute?1:0),overtimeEndMinute,zone);
        }
        public boolean overtimeContinuesWork(Context context,TimeZone zone) {
            return hasOvertime && overtimeStartEpoch(context,zone)==shiftEndEpoch(context,zone);
        }
        public boolean overtimeEndsAtOwnShiftStart(Context context,TimeZone zone) {
            return hasOvertime && validWork(baseProfile)
                    && overtimeEndEpoch(context,zone)==shiftStartEpoch(context,zone);
        }
    }
    private static String clean(String s) { return s==null?"":s.trim(); }
}
