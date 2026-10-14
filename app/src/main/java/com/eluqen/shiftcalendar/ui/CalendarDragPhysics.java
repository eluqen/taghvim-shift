package com.eluqen.shiftcalendar.ui;

/** Stateless month-drag release and frame pacing policy; separately unit-tested. */
final class CalendarDragPhysics {
    private CalendarDragPhysics() {}

    static boolean shouldCommit(float distancePx, float velocityPxSec,
                                int widthPx, float density, int initialDirection) {
        if (widthPx <= 0 || initialDirection == 0 || distancePx * initialDirection <= 0f)
            return false;
        if (Math.abs(distancePx) >= widthPx * 0.25f) return true;
        return Math.abs(distancePx) >= widthPx * 0.07f
                && velocityPxSec * initialDirection >= 560f * density;
    }

    static long durationMs(float from, float to, int widthPx) {
        float proportion = Math.min(1f, Math.abs(to - from) / Math.max(1, widthPx));
        return Math.max(105L, Math.min(230L, (long) (95f + 155f * proportion)));
    }
}
