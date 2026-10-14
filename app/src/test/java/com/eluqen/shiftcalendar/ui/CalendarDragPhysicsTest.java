package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CalendarDragPhysicsTest {
    @Test public void rightwardQuarterWidthCommitsNext() {
        assertTrue(CalendarDragPhysics.shouldCommit(270f, 0f, 1000, 3f, 1));
        assertFalse(CalendarDragPhysics.shouldCommit(270f, 0f, 1000, 3f, -1));
    }

    @Test public void leftwardQuarterWidthCommitsPrevious() {
        assertTrue(CalendarDragPhysics.shouldCommit(-270f, 0f, 1000, 3f, -1));
        assertFalse(CalendarDragPhysics.shouldCommit(-270f, 0f, 1000, 3f, 1));
    }

    @Test public void shortDragReturnsToOriginalMonth() {
        assertFalse(CalendarDragPhysics.shouldCommit(50f, 0f, 1000, 2f, 1));
        assertFalse(CalendarDragPhysics.shouldCommit(-50f, 0f, 1000, 2f, -1));
    }

    @Test public void deliberateFastFlickCommitsAndOppositeVelocityDoesNot() {
        assertTrue(CalendarDragPhysics.shouldCommit(100f, 1800f, 1000, 2f, 1));
        assertTrue(CalendarDragPhysics.shouldCommit(-100f, -1800f, 1000, 2f, -1));
        assertFalse(CalendarDragPhysics.shouldCommit(-100f, 1800f, 1000, 2f, -1));
    }

    @Test public void verySmallFlickDoesNotCommit() {
        assertFalse(CalendarDragPhysics.shouldCommit(30f, 5000f, 1000, 2f, 1));
    }

    @Test public void finishingAnimationAlwaysHasSmoothBoundedDuration() {
        assertTrue(CalendarDragPhysics.durationMs(250f, 1000f, 1000) >= 105L);
        assertTrue(CalendarDragPhysics.durationMs(0f, 1000f, 1000) <= 230L);
        assertTrue(CalendarDragPhysics.durationMs(0f, 0f, 1000) >= 105L);
    }
}
