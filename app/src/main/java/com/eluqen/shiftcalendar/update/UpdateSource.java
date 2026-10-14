package com.eluqen.shiftcalendar.update;

import com.eluqen.shiftcalendar.data.Prefs;

public enum UpdateSource {
    GOOGLE_PLAY("google_play", "گوگل\u200cپلی"),
    BAZAAR("bazaar", "بازار"),
    MYKET("myket", "مایکت"),
    GALAXY("galaxy", "Galaxy Store"),
    DIRECT("direct", "دانلود مستقیم"),
    ASK(Prefs.NETWORK_ASK, "هر بار سؤال شود");

    public final String key;
    public final String label;

    UpdateSource(String str, String str2) {
        this.key = str;
        this.label = str2;
    }

    public static UpdateSource fromKey(String str, UpdateSource updateSource) {
        for (UpdateSource updateSource2 : values()) {
            if (updateSource2.key.equals(str)) {
                return updateSource2;
            }
        }
        return updateSource;
    }
}
