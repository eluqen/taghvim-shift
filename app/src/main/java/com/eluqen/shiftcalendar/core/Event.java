package com.eluqen.shiftcalendar.core;

public final class Event {
    public final String category;
    public final boolean holiday;
    public final String title;

    public Event(String str, boolean z) {
        this(str, z, "");
    }

    public Event(String str, boolean z, String str2) {
        this.title = str;
        this.holiday = z;
        this.category = str2 == null ? "" : str2;
    }
}
