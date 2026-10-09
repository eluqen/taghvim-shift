package com.eluqen.shiftcalendar.data;

import com.eluqen.shiftcalendar.core.PersianDate;
import org.json.JSONObject;
import org.json.JSONException;

public final class CardexCodec {
    public static final String[] COLUMNS = {"date", "jdn", "note", "note_time", "note_ink", "rest_enabled", "rest_start", "rest_end", "leave_enabled", "leave_start", "leave_end", "swap_enabled", "swap_note", "swap_time", "overtime_enabled", "overtime_start", "overtime_end", "swap_shift_code", "rest_full_shift", "leave_full_shift", "overtime_shift_code", "overtime_day_offset", "overtime_type"};
    private static final String[] FIELDS = {"date", "jdn", "note", "noteTime", "noteInk", "rest", "restStart", "restEnd", "leave", "leaveStart", "leaveEnd", "swap", "swapNote", "swapTime", "overtime", "overtimeStart", "overtimeEnd", "swapShiftCode", "restFullShift", "leaveFullShift", "overtimeShiftCode", "overtimeDayOffset", "overtimeType"};
    private CardexCodec() {}
    public static CardexRecord fromJson(JSONObject j) throws JSONException { return decode(j, true); }
    private static CardexRecord decode(JSONObject j, boolean normalize) throws JSONException {
        CardexRecord r = new CardexRecord();
        r.date = j.getString("date");
        try { r.jdn = PersianDate.parse(r.date).toJdn(); }
        catch (RuntimeException e) { throw new JSONException("تاریخ کارکرد نامعتبر است"); }
        r.note=j.optString("note"); r.noteTime=j.optString("noteTime"); r.noteInk=j.optString("noteInk");
        r.rest=j.optBoolean("rest"); r.restFullShift=j.optBoolean("restFullShift",r.rest);
        r.restStart=j.optString("restStart"); r.restEnd=j.optString("restEnd");
        r.leave=j.optBoolean("leave"); r.leaveFullShift=j.optBoolean("leaveFullShift",r.leave);
        r.leaveStart=j.optString("leaveStart"); r.leaveEnd=j.optString("leaveEnd");
        r.swap=j.optBoolean("swap"); r.swapNote=j.optString("swapNote"); r.swapTime=j.optString("swapTime");
        r.swapShiftCode=j.optString("swapShiftCode");
        r.overtime=j.optBoolean("overtime"); r.overtimeStart=j.optString("overtimeStart");
        r.overtimeEnd=j.optString("overtimeEnd"); r.overtimeShiftCode=j.optString("overtimeShiftCode");
        r.overtimeDayOffset=j.optInt("overtimeDayOffset",99); r.overtimeType=j.optString("overtimeType");
        if (normalize) r.normalizeOperational();
        return r;
    }
    public static JSONObject toJson(CardexRecord r) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("date",r.date); j.put("jdn",r.jdn); j.put("note",r.note); j.put("noteTime",r.noteTime); j.put("noteInk",r.noteInk);
        j.put("rest",r.rest); j.put("restFullShift",r.restFullShift); j.put("restStart",r.restStart); j.put("restEnd",r.restEnd);
        j.put("leave",r.leave); j.put("leaveFullShift",r.leaveFullShift); j.put("leaveStart",r.leaveStart); j.put("leaveEnd",r.leaveEnd);
        j.put("swap",r.swap); j.put("swapNote",r.swapNote); j.put("swapTime",r.swapTime); j.put("swapShiftCode",r.swapShiftCode);
        j.put("overtime",r.overtime); j.put("overtimeStart",r.overtimeStart); j.put("overtimeEnd",r.overtimeEnd);
        j.put("overtimeShiftCode",r.overtimeShiftCode); j.put("overtimeDayOffset",r.overtimeDayOffset); j.put("overtimeType",r.overtimeType);
        return j;
    }
    public static java.util.Map<String,Object> sqlValues(CardexRecord r) {
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();
        try {
            JSONObject j=toJson(r);
            for(int i=0;i<COLUMNS.length;i++) {
                Object v=j.opt(FIELDS[i]);
                if(v instanceof Boolean) v=(Boolean)v?1:0;
                out.put(COLUMNS[i],v==null||v==JSONObject.NULL?"":v);
            }
        } catch(JSONException e) { throw new IllegalArgumentException(e); }
        return out;
    }
    public static CardexRecord fromSql(java.util.Map<String,Object> values) {
        try {
            JSONObject j=new JSONObject();
            for(int i=0;i<COLUMNS.length;i++) {
                Object v=values.get(COLUMNS[i]);
                if(COLUMNS[i].endsWith("_enabled")||COLUMNS[i].endsWith("_full_shift"))
                    v=v instanceof Number&&((Number)v).intValue()!=0;
                j.put(FIELDS[i],v);
            }
            return decode(j,false);
        } catch(JSONException e) { throw new IllegalArgumentException(e); }
    }
}
