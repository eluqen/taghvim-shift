package com.eluqen.shiftcalendar.data;

import android.content.Context;

public final class CardexRecord {
    public String date;
    public long jdn;
    public boolean leave;
    public boolean leaveFullShift;
    public boolean overtime;
    public boolean rest;
    public boolean restFullShift;
    public boolean swap;
    public String note = "";
    public String noteTime = "";
    public String noteInk = "";
    public String restStart = "";
    public String restEnd = "";
    public String leaveStart = "";
    public String leaveEnd = "";
    public String swapNote = "";
    public String swapTime = "";
    public String swapShiftCode = "";
    public String overtimeStart = "";
    public String overtimeEnd = "";
    public String overtimeShiftCode = "";
    public String overtimeType = "";
    public int overtimeDayOffset = 99;

    public CardexRecord copy() {
        CardexRecord r = new CardexRecord();
        r.date=date; r.jdn=jdn; r.note=note; r.noteTime=noteTime; r.noteInk=noteInk;
        r.rest=rest; r.restFullShift=restFullShift; r.restStart=restStart; r.restEnd=restEnd;
        r.leave=leave; r.leaveFullShift=leaveFullShift; r.leaveStart=leaveStart; r.leaveEnd=leaveEnd;
        r.swap=swap; r.swapNote=swapNote; r.swapTime=swapTime; r.swapShiftCode=swapShiftCode;
        r.overtime=overtime; r.overtimeStart=overtimeStart; r.overtimeEnd=overtimeEnd; r.overtimeShiftCode=overtimeShiftCode; r.overtimeDayOffset=overtimeDayOffset; r.overtimeType=overtimeType;
        return r;
    }

    public void clearOperational() {
        rest=false; restFullShift=false; restStart=""; restEnd="";
        leave=false; leaveFullShift=false; leaveStart=""; leaveEnd="";
        swap=false; swapNote=""; swapTime=""; swapShiftCode="";
        overtime=false; overtimeStart=""; overtimeEnd=""; overtimeShiftCode=""; overtimeDayOffset=99; overtimeType="";
    }

    public void normalizeOperational() {
        int mode = com.eluqen.shiftcalendar.work.DayWorkResolver.mode(this);
        if (mode != 1) { rest=false; restFullShift=false; restStart=""; restEnd=""; }
        if (mode != 2) { leave=false; leaveFullShift=false; leaveStart=""; leaveEnd=""; }
        if (mode != 3) { swap=false; swapNote=""; swapTime=""; swapShiftCode=""; }
        if (mode != 4) { overtime=false; overtimeStart=""; overtimeEnd=""; overtimeShiftCode=""; overtimeDayOffset=99; overtimeType=""; }
    }

    public boolean isEmpty() {
        return blank(this.note)
                && blank(this.noteTime)
                && blank(this.noteInk)
                && !this.rest
                && !this.leave
                && !this.swap
                && !this.overtime;
    }

    public String summary() {
        return summary((String) null, (String) null);
    }

    public String summary(Context context) {
        String swapTarget = "";
        String overtimeTarget = "";
        if (context != null && !blank(this.swapShiftCode)) {
            swapTarget = ShiftPreferences.title(context, this.swapShiftCode);
        }
        if (context != null && !blank(this.overtimeShiftCode)) {
            overtimeTarget = ShiftPreferences.title(context, this.overtimeShiftCode);
        }
        return summary(swapTarget, overtimeTarget);
    }

    private String summary(String swapTargetTitle, String overtimeTargetTitle) {
        StringBuilder sb = new StringBuilder();
        add(sb, join(" ", this.note, this.noteTime));
        if (blank(this.note) && !blank(this.noteInk)) add(sb, "دست‌نویس");
        int mode = com.eluqen.shiftcalendar.work.DayWorkResolver.mode(this);
        if (mode == 1) {
            boolean hourly = !this.restFullShift
                    && !blank(this.restStart) && !blank(this.restEnd);
            add(sb, hourly
                    ? "استراحت ساعتی " + range(this.restStart, this.restEnd)
                    : "استراحت روزانه");
        }
        if (mode == 2) {
            boolean hourly = !this.leaveFullShift
                    && !blank(this.leaveStart) && !blank(this.leaveEnd);
            add(sb, hourly
                    ? "مرخصی استحقاقی ساعتی " + range(this.leaveStart, this.leaveEnd)
                    : "مرخصی استحقاقی روزانه");
        }
        if (mode == 3) {
            String target;
            if (!blank(swapTargetTitle)) {
                target = "→ " + swapTargetTitle;
            } else if (!blank(this.swapShiftCode)) {
                target = "→ " + this.swapShiftCode;
            } else {
                target = "";
            }
            add(sb, "تعویض شیفت " + join(
                    " ", target, this.swapNote, this.swapTime));
        }
        if (mode == 4) {
            String target = !blank(overtimeTargetTitle)
                    ? "→ " + overtimeTargetTitle
                    : (!blank(this.overtimeShiftCode)
                    ? "→ " + this.overtimeShiftCode : "");
            add(sb, "اضافه‌کاری " + join(" ",
                    overtimeTypeTitle(this.overtimeType), target,
                    range(this.overtimeStart, this.overtimeEnd)));
        }
        return sb.toString();
    }

    public static String overtimeTypeTitle(String code) {
        if ("REPLACEMENT".equals(code)) return "پوشش شیفت";
        if ("NECESSITY".equals(code)) return "ضرورت کاری";
        return "";
    }

    private static String range(String str, String str2) {
        if (blank(str)) {
            return blank(str2) ? "" : str2;
        }
        if (blank(str2)) {
            return str;
        }
        return str + "–" + str2;
    }

    private static String join(String str, String... strArr) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : strArr) {
            if (!blank(str2)) {
                if (sb.length() > 0) {
                    sb.append(str);
                }
                sb.append(str2.trim());
            }
        }
        return sb.toString();
    }

    private static void add(StringBuilder sb, String str) {
        if (blank(str)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" • ");
        }
        sb.append(str.trim());
    }

    private static boolean blank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
