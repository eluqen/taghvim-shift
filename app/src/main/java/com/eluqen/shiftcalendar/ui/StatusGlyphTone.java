package com.eluqen.shiftcalendar.ui;

/** Shared immutable tone contract for owner-approved status illustrations. */
final class StatusGlyphTone {
    static final int REGISTERED_ALPHA = 255;
    static final int UNREGISTERED_ALPHA = 90;
    private StatusGlyphTone() {}

    static int opacity(boolean registered) {
        return registered ? REGISTERED_ALPHA : UNREGISTERED_ALPHA;
    }
}
