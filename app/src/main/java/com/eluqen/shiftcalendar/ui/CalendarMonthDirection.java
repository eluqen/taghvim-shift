package com.eluqen.shiftcalendar.ui;

/**
 * Month navigation follows the owner-approved Persian RTL temporal direction:
 * a rightward finger movement moves to the NEXT month; leftward to PREVIOUS.
 * The sign is independent of the root-tab swipe direction.
 */
final class CalendarMonthDirection {
    private CalendarMonthDirection() {}

    /** Horizontal displacement on the screen, positive is finger moving right. */
    static int fromDrag(float horizontalDx) {
        return horizontalDx > 0f ? 1 : horizontalDx < 0f ? -1 : 0;
    }

    /** Direction already determined from the horizontal gesture. */
    static int fromDirection(int fingerDirection) {
        return fingerDirection > 0 ? 1 : fingerDirection < 0 ? -1 : 0;
    }

    /** Which way the OLD calendar slides during a button-triggered month move. */
    static int animationDirectionForMonthDelta(int delta) {
        return fromDirection(delta);
    }
}
