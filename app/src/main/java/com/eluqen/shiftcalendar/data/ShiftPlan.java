package com.eluqen.shiftcalendar.data;

public final class ShiftPlan {
    public boolean active;
    public long anchorJdn;
    public String company;

    public long id;
    public String pattern;
    public String team;

    public String displayName() {
        return this.company + " — " + this.team;
    }
}
