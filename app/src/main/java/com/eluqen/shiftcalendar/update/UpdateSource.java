package com.eluqen.shiftcalendar.update;
public enum UpdateSource {
    GOOGLE_PLAY("google_play","گوگل‌پلی");
    public final String key;
    public final String label;
    UpdateSource(String key,String label){this.key=key;this.label=label;}
    public static UpdateSource fromKey(String value,UpdateSource fallback){return GOOGLE_PLAY;}
}
