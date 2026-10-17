package com.eluqen.shiftcalendar.ui;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Locally stored, explicitly selected telephone numbers. Never queries address books. */
public final class ColleagueNumbers {
    private ColleagueNumbers() {}

    public static final class NumberEntry {
        public final String label;
        public final String number;
        public NumberEntry(String label, String number) {
            this.label = label == null ? "" : label.trim();
            this.number = number == null ? "" : number.trim();
        }
    }

    public static JSONArray decode(String raw) {
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (Exception ex) { return new JSONArray(); }
    }

    public static String importJson(Object raw) {
        if (raw instanceof JSONArray) return raw.toString();
        if (raw instanceof String) return decode((String)raw).toString();
        return "[]";
    }

    public static List<NumberEntry> entries(String raw) {
        List<NumberEntry> result = new ArrayList<>();
        JSONArray arr = decode(raw);
        for (int i=0;i<arr.length();i++) {
            JSONObject item=arr.optJSONObject(i);
            if (item == null) continue;
            String number=item.optString("number","").trim();
            if (!number.isEmpty()) result.add(new NumberEntry(
                 item.optString("label","شماره دیگر"),number));
        }
        return result;
    }

    public static String encode(List<NumberEntry> numbers) {
        JSONArray arr = new JSONArray();
        for (NumberEntry e : numbers) {
            if (e == null || e.number.isEmpty()) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("label",e.label);
                item.put("number",e.number);
                arr.put(item);
            } catch (Exception ignored) { }
        }
        return arr.toString();
    }

    public static String key(String phone) {
        if (phone == null) return "";
        StringBuilder b=new StringBuilder();
        String raw=phone.trim();
        for(int i=0;i<raw.length();i++){
            char c=raw.charAt(i);
            int n=Character.digit(c,10);
            if (n>=0) b.append(n);
            else if (c=='+' && b.length()==0) b.append('+');
        }
        return b.toString();
    }

    public static boolean has(List<String> saved,String number){
        String target=key(number);
        if(target.isEmpty())return true;
        for(String s:saved)if(key(s).equals(target))return true;
        return false;
    }
}
