package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SettingsThemeRefreshTest {
    @Test public void unchangedThemeNeverRepaints() {
        for (String key : new String[]{"eluqen","ocean","violet","forest","rose",
                "graphite","sand","ice","sunset"})
            assertFalse(SettingsThemeRefresh.needsRepaint(key,key));
    }

    @Test public void changedThemeRepaintsWithoutActivityRestart() {
        for (String key : new String[]{"ocean","violet","forest","rose",
                "graphite","sand","ice","sunset"})
            assertTrue(SettingsThemeRefresh.needsRepaint("eluqen",key));
    }

    @Test public void initialUnrenderedScreenDoesNotDoubleBuild() {
        assertFalse(SettingsThemeRefresh.needsRepaint(null,"ocean"));
    }

    @Test public void unknownSavedKeyDoesNotTriggerAnUnsafeRefresh() {
        assertFalse(SettingsThemeRefresh.needsRepaint("ocean",null));
    }
}
