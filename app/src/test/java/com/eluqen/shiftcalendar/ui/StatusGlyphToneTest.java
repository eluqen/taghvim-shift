package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class StatusGlyphToneTest {
    @Test public void savedStatusIsFullyVisible() {
        assertEquals(255, StatusGlyphTone.opacity(true));
    }

    @Test public void missingStatusIsFadedButStillIdentifiable() {
        int faded = StatusGlyphTone.opacity(false);
        assertTrue(faded >= 70);
        assertTrue(faded <= 115);
        assertTrue(faded < StatusGlyphTone.opacity(true));
    }
}
