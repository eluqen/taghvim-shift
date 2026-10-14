package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class CalendarMonthDirectionTest {
    @Test public void rightwardDragShowsNextMonth() {
        assertEquals(1, CalendarMonthDirection.fromDrag(210f));
        assertEquals(1, CalendarMonthDirection.fromDrag(1f));
    }

    @Test public void leftwardDragShowsPreviousMonth() {
        assertEquals(-1, CalendarMonthDirection.fromDrag(-210f));
        assertEquals(-1, CalendarMonthDirection.fromDrag(-1f));
    }

    @Test public void neutralDragDoesNotNavigate() {
        assertEquals(0, CalendarMonthDirection.fromDrag(0f));
    }

    @Test public void previewAndCommitUseTheSameMapping() {
        assertEquals(1, CalendarMonthDirection.fromDirection(1));
        assertEquals(-1, CalendarMonthDirection.fromDirection(-1));
        assertEquals(0, CalendarMonthDirection.fromDirection(0));
    }

    @Test public void buttonAnimationsMatchGestureDirection() {
        assertEquals(1, CalendarMonthDirection.animationDirectionForMonthDelta(1));
        assertEquals(-1, CalendarMonthDirection.animationDirectionForMonthDelta(-1));
        assertEquals(0, CalendarMonthDirection.animationDirectionForMonthDelta(0));
    }
}
