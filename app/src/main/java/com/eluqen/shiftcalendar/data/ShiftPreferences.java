package com.eluqen.shiftcalendar.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.ui.ShiftExtras;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ShiftPreferences {
    private static final String[] CODES = {ShiftEngine.DAY, ShiftEngine.EVENING, ShiftEngine.NIGHT, ShiftEngine.OFF, ShiftEngine.CUSTOM1, ShiftEngine.CUSTOM2, "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11", "C12"};
    private static final String FILE = StorageIdentity.SHIFT_TYPE_PREFS;

    private ShiftPreferences() {
    }

    public static String[] codes() {
        return (String[]) CODES.clone();
    }

    public static Profile get(Context context, String str) {
        String strNormalize = ShiftEngine.normalize(str);
        int iIndexOf = indexOf(strNormalize);
        if (iIndexOf < 0) {
            return new Profile(strNormalize, ShiftEngine.label(strNormalize), defaultShort(ShiftEngine.label(strNormalize)), -1, -1, Color.rgb(80, 96, 112), true, false);
        }
        Profile profileDefaults = defaults(iIndexOf);
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE, 0);
        String str2 = strNormalize + ".";
        String strCleanTitle = cleanTitle(sharedPreferences.getString(str2 + "title", profileDefaults.title), profileDefaults.title);
        String strCleanShort = cleanShort(sharedPreferences.getString(str2 + "short", profileDefaults.shortTitle), profileDefaults.shortTitle);
        int iClampMinute = clampMinute(sharedPreferences.getInt(str2 + "start", profileDefaults.startMinute));
        int iClampMinute2 = clampMinute(sharedPreferences.getInt(str2 + "end", profileDefaults.endMinute));
        int i = sharedPreferences.getInt(str2 + "color", profileDefaults.color);
        boolean z = sharedPreferences.getBoolean(str2 + "working", profileDefaults.working);
        boolean z2 = sharedPreferences.getBoolean(str2 + "reminder", profileDefaults.reminder);
        if (!z) {
            z2 = false;
        }
        return new Profile(strNormalize, strCleanTitle, strCleanShort, iClampMinute, iClampMinute2, i, z, z2);
    }

    public static void save(Context context, Profile profile) {
        int iIndexOf = indexOf(profile.code);
        if (iIndexOf < 0) {
            throw new IllegalArgumentException("نوع شیفت شناخته\u200cشده نیست");
        }
        Profile profileDefaults = defaults(iIndexOf);
        boolean z = false;
        SharedPreferences.Editor editorPutBoolean = context.getSharedPreferences(FILE, 0).edit().putString(profile.code + ".title", cleanTitle(profile.title, profileDefaults.title)).putString(profile.code + ".short", cleanShort(profile.shortTitle, profileDefaults.shortTitle)).putInt(profile.code + ".start", clampMinute(profile.startMinute)).putInt(profile.code + ".end", clampMinute(profile.endMinute)).putInt(profile.code + ".color", profile.color).putBoolean(profile.code + ".working", profile.working);
        String str = profile.code + ".reminder";
        if (profile.working && profile.reminder) {
            z = true;
        }
        editorPutBoolean.putBoolean(str, z).apply();
    }

    public static void reset(Context context) {
        ShiftExtras.restoreBase(context);
        context.getSharedPreferences(FILE, 0).edit().clear().apply();
    }

    public static String title(Context context, String str) {
        return get(context, str).title;
    }

    public static String shortTitle(Context context, String str) {
        return get(context, str).shortTitle;
    }

    public static int color(Context context, String str) {
        return get(context, str).color;
    }

    public static String timeRange(Context context, String str) {
        Profile profile = get(context, str);
        if (!profile.working || profile.startMinute < 0 || profile.endMinute < 0) {
            return "";
        }
        return formatMinute(profile.startMinute) + " تا " + formatMinute(profile.endMinute);
    }

    public static JSONObject exportJson(Context context) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("version", 1);
        JSONArray jSONArray = new JSONArray();
        for (String str : ShiftExtras.visibleCodes(context)) {
            Profile profile = get(context, str);
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("code", profile.code);
            jSONObject2.put("title", profile.title);
            jSONObject2.put("shortTitle", profile.shortTitle);
            jSONObject2.put("startMinute", profile.startMinute);
            jSONObject2.put("endMinute", profile.endMinute);
            jSONObject2.put("color", profile.color);
            jSONObject2.put("working", profile.working);
            jSONObject2.put("reminder", profile.reminder);
            jSONArray.put(jSONObject2);
        }
        jSONObject.put("profiles", jSONArray);
        return jSONObject;
    }

    public static void importJson(Context context, JSONObject jSONObject) throws JSONException {
        JSONArray jSONArrayOptJSONArray;
        String strNormalize;
        int iIndexOf;
        if (jSONObject == null || (jSONArrayOptJSONArray = jSONObject.optJSONArray("profiles")) == null) {
            return;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (iIndexOf = indexOf((strNormalize = ShiftEngine.normalize(jSONObjectOptJSONObject.optString("code"))))) >= 0) {
                Profile profileDefaults = defaults(iIndexOf);
                Profile profile = new Profile(strNormalize, jSONObjectOptJSONObject.optString("title", profileDefaults.title), jSONObjectOptJSONObject.optString("shortTitle", profileDefaults.shortTitle), jSONObjectOptJSONObject.optInt("startMinute", profileDefaults.startMinute), jSONObjectOptJSONObject.optInt("endMinute", profileDefaults.endMinute), jSONObjectOptJSONObject.optInt("color", profileDefaults.color), jSONObjectOptJSONObject.optBoolean("working", profileDefaults.working), jSONObjectOptJSONObject.optBoolean("reminder", profileDefaults.reminder));
                save(context, profile);
                ShiftExtras.enableImported(context, profile.code);
            }
        }
    }

    public static String formatMinute(int i) {
        if (i < 0) {
            return "—";
        }
        int i2 = i % 1440;
        return String.format(Locale.US, "%02d:%02d", Integer.valueOf(i2 / 60), Integer.valueOf(i2 % 60));
    }

    private static int indexOf(String str) {
        int i = 0;
        while (true) {
            String[] strArr = CODES;
            if (i >= strArr.length) {
                return -1;
            }
            if (strArr[i].equals(str)) {
                return i;
            }
            i++;
        }
    }

    private static int clampMinute(int i) {
        if (i < 0) {
            return -1;
        }
        return Math.min(1439, i);
    }

    private static String cleanTitle(String str, String str2) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.isEmpty()) {
            return str2;
        }
        return strTrim.length() > 24 ? strTrim.substring(0, 24) : strTrim;
    }

    private static String cleanShort(String str, String str2) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.isEmpty()) {
            return str2;
        }
        return strTrim.length() > 3 ? strTrim.substring(0, 3) : strTrim;
    }

    private static String defaultShort(String str) {
        if (str == null || str.isEmpty()) {
            return "؟";
        }
        return str.substring(0, 1);
    }

    private static Profile defaults(int i) {
        if (i >= 6) {
            int i2 = i - 3;
            return new Profile("C" + i2, "سفارشی " + i2, String.valueOf(i2), 480, 960, -8497214, true, true);
        }
        if (i == 0) {
            return new Profile(ShiftEngine.DAY, "روزکار", "ر", 420, 900, Color.rgb(216, 148, 47), true, true);
        }
        if (i == 1) {
            return new Profile(ShiftEngine.EVENING, "عصرکار", "ع", 900, 1380, Color.rgb(201, 83, 122), true, true);
        }
        if (i == 2) {
            return new Profile(ShiftEngine.NIGHT, "شبکار", "ش", 1380, 420, Color.rgb(83, 103, 199), true, true);
        }
        if (i == 3) {
            return new Profile(ShiftEngine.OFF, "استراحت", "ا", -1, -1, Color.rgb(23, 126, 121), false, false);
        }
        if (i == 4) {
            return new Profile(ShiftEngine.CUSTOM1, "سفارشی ۱", "۱", 480, 960, Color.rgb(126, 87, 194), true, true);
        }
        return new Profile(ShiftEngine.CUSTOM2, "سفارشی ۲", "۲", 960, 0, Color.rgb(0, 137, 123), true, true);
    }

    public static final class Profile {
        public final String code;
        public final int color;
        public final int endMinute;
        public final boolean reminder;
        public final String shortTitle;
        public final int startMinute;
        public final String title;
        public final boolean working;

        public Profile(String str, String str2, String str3, int i, int i2, int i3, boolean z, boolean z2) {
            this.code = ShiftEngine.normalize(str);
            this.title = str2;
            this.shortTitle = str3;
            this.startMinute = i;
            this.endMinute = i2;
            this.color = i3;
            this.working = z;
            this.reminder = z2;
        }
    }
}
