package com.eluqen.shiftcalendar.ui;

/** Consistent completion-state colors, deliberately independent of the nine theme palettes. */
final class PlannerCompletionVisual {
    static final int EDIT = 0, TOGGLE = 1, DELETE = 2;
    static final int DONE_CARD = 0xffe3f5eb;
    static final int DONE_BORDER = 0xff087649;
    static final int DONE_TEXT = 0xff134e35;
    static final int DONE_BADGE = 0xff08633f;
    static final int RETURN_FILL = 0xffffc15a;
    static final int RETURN_TEXT = 0xff382500;
    static final int DELETE_TEXT = 0xffa12839;
    private PlannerCompletionVisual() {}

    static String badge(boolean done) { return done ? "✓  انجام‌شده" : "○  در انتظار"; }
    static int cardFill(boolean done, int normal) { return done ? DONE_CARD : normal; }
    static int cardOutline(boolean done, int normal) { return done ? DONE_BORDER : normal; }
    static int badgeFill(boolean done, int neutral) { return done ? DONE_BADGE : neutral; }
    static int badgeInk(boolean done, int themedText) { return done ? 0xffffffff : themedText; }
    static int actionFill(boolean done, int role, int primary, int neutral) {
        if (done) return role == TOGGLE ? RETURN_FILL : role == DELETE ? 0xfffff2f2 : 0xffffffff;
        return role == TOGGLE ? primary : neutral;
    }
    static int actionInk(boolean done, int role, int themedText) {
        if (role == DELETE) return DELETE_TEXT;
        if (done) return role == TOGGLE ? RETURN_TEXT : DONE_TEXT;
        return role == TOGGLE ? 0xffffffff : themedText;
    }
    static int actionOutline(boolean done, int role, int primary, int neutral) {
        if (role == DELETE) return 0xffb93e50;
        if (done) return role == TOGGLE ? 0xffab6813 : DONE_BORDER;
        return role == TOGGLE ? primary : neutral;
    }

    static double contrast(int a, int b) {
        double one=luminance(a), two=luminance(b);
        return (Math.max(one,two)+0.05)/(Math.min(one,two)+0.05);
    }
    private static double luminance(int color) {
        double r=linear((color>>>16)&255),g=linear((color>>>8)&255),b=linear(color&255);
        return .2126*r+.7152*g+.0722*b;
    }
    private static double linear(int channel) {
        double c=channel/255.0;
        return c<=0.04045 ? c/12.92 : Math.pow((c+0.055)/1.055,2.4);
    }
}
