package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class PersonalStore {
    private static final String FILE = "eluqen_personal_planner";

    private PersonalStore() {
    }

    private static SharedPreferences p(Context context) {
        return context.getSharedPreferences(FILE, 0);
    }

    public static synchronized JSONArray all(Context context) {
        try {
            return new JSONArray(p(context).getString("items", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static synchronized JSONObject get(Context context, int i) {
        JSONArray jSONArrayAll = all(context);
        for (int i2 = 0; i2 < jSONArrayAll.length(); i2++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayAll.optJSONObject(i2);
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optInt("id") == i) {
                return jSONObjectOptJSONObject;
            }
        }
        return null;
    }

    public static synchronized int save(Context context, JSONObject jSONObject) {
        int iOptInt;
        String strTrim = jSONObject.optString("title", "").trim();
        if (strTrim.length() == 0 || strTrim.length() > 140) {
            throw new IllegalArgumentException("عنوان نامعتبر");
        }
        JSONArray jSONArrayAll = all(context);
        JSONArray jSONArray = new JSONArray();
        iOptInt = jSONObject.optInt("id", 0);
        if (iOptInt <= 0 && (iOptInt = p(context).getInt("next", 1)) > 500000) {
            throw new IllegalStateException("ID overflow");
        }
        if (jSONArrayAll.length() >= 500 && jSONObject.optInt("id", 0) <= 0) {
            throw new IllegalStateException("Too many tasks");
        }
        try {
            jSONObject.put("id", iOptInt);
            jSONObject.put("title", strTrim);
            for (int i = 0; i < jSONArrayAll.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayAll.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optInt("id") != iOptInt) {
                    jSONArray.put(jSONObjectOptJSONObject);
                }
            }
            jSONArray.put(jSONObject);
            if (!p(context).edit().putString("items", jSONArray.toString()).putInt("next", Math.max(iOptInt + 1, p(context).getInt("next", 1))).commit()) {
                throw new IllegalStateException("Persist failed");
            }
            PersonalAlarm.syncOne(context, jSONObject);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return iOptInt;
    }

    public static synchronized void remove(Context context, int i) {
        PersonalAlarm.cancel(context, i);
        JSONArray jSONArrayAll = all(context);
        JSONArray jSONArray = new JSONArray();
        for (int i2 = 0; i2 < jSONArrayAll.length(); i2++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayAll.optJSONObject(i2);
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optInt("id") != i) {
                jSONArray.put(jSONObjectOptJSONObject);
            }
        }
        p(context).edit().putString("items", jSONArray.toString()).commit();
    }

    public static synchronized void setDone(Context context, int i, boolean z) {
        JSONObject jSONObject = get(context, i);
        if (jSONObject == null) {
            return;
        }
        try {
            jSONObject.put("done", z);
            save(context, jSONObject);
        } catch (Exception e) {
            Log.e("ELUQEN_PERSONAL", "Done", e);
        }
    }

    public static synchronized void markFired(Context context, int i, long j) {
        JSONArray jSONArrayAll = all(context);
        for (int i2 = 0; i2 < jSONArrayAll.length(); i2++) {
            try {
                JSONObject jSONObject = jSONArrayAll.getJSONObject(i2);
                if (jSONObject.optInt("id") == i && jSONObject.optLong("when") == j) {
                    jSONObject.put("fired", true);
                    p(context).edit().putString("items", jSONArrayAll.toString()).commit();
                    return;
                }
            } catch (Exception e) {
            }
        }
    }

    public static synchronized JSONObject exportJson(Context context) {
        JSONObject jSONObject;
        jSONObject = new JSONObject();
        try {
            jSONObject.put("items", all(context));
            jSONObject.put("next", p(context).getInt("next", 1));
            JSONObject jSONObject2 = new JSONObject();
            for (Map.Entry<String, ?> entry : context.getSharedPreferences("eluqen_shift_audio", 0).getAll().entrySet()) {
                if (entry.getValue() instanceof String) {
                    jSONObject2.put(entry.getKey(), entry.getValue());
                }
            }
            jSONObject.put("shiftAudio", jSONObject2);
        } catch (Exception e) {
        }
        return jSONObject;
    }

    public static synchronized void importJson(Context context, JSONObject jSONObject) {
        int iMax;
        if (jSONObject == null) {
            return;
        }
        JSONArray jSONArrayAll = all(context);
        for (int i = 0; i < jSONArrayAll.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayAll.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                PersonalAlarm.cancel(context, jSONObjectOptJSONObject.optInt("id"));
            }
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("items");
        JSONArray jSONArray = new JSONArray();
        if (jSONArrayOptJSONArray != null) {
            iMax = 0;
            for (int i2 = 0; i2 < Math.min(500, jSONArrayOptJSONArray.length()); i2++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i2);
                if (jSONObjectOptJSONObject2 != null && jSONObjectOptJSONObject2.optInt("id") > 0 && jSONObjectOptJSONObject2.optString("title").trim().length() > 0 && jSONObjectOptJSONObject2.optString("title").length() <= 140) {
                    jSONArray.put(jSONObjectOptJSONObject2);
                    iMax = Math.max(iMax, jSONObjectOptJSONObject2.optInt("id"));
                }
            }
        } else {
            iMax = 0;
        }
        p(context).edit().putString("items", jSONArray.toString()).putInt("next", Math.max(iMax + 1, jSONObject.optInt("next", 1))).commit();
        JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("shiftAudio");
        if (jSONObjectOptJSONObject3 != null) {
            SharedPreferences.Editor editorClear = context.getSharedPreferences("eluqen_shift_audio", 0).edit().clear();
            Iterator<String> itKeys = jSONObjectOptJSONObject3.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                editorClear.putString(next, jSONObjectOptJSONObject3.optString(next));
            }
            editorClear.commit();
        }
        PersonalAlarm.reschedule(context);
    }
}
