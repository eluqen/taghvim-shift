package com.eluqen.shiftcalendar.update;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class CalendarAssetContractTest {
    @Test
    public void bundledBankUsesCanonicalSchema() throws Exception {
        File file = new File("src/main/assets/event_bank.json");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) out.write(buffer, 0, read);
            }
        }
        JSONObject root = new JSONObject(
                new String(out.toByteArray(), StandardCharsets.UTF_8));

        assertEquals(1, root.getInt("schemaVersion"));
        assertEquals(2, root.getInt("dataVersion"));
        assertEquals(1405, root.getInt("coverageFrom"));
        assertEquals(1409, root.getInt("coverageTo"));
        assertTrue(root.getString("publishedAt").endsWith("Z"));

        JSONObject hijri = root.getJSONObject("hijri");
        assertEquals("tabular", hijri.getString("algorithm"));
        assertEquals(2, hijri.getInt("correctionVersion"));
        assertEquals(13, hijri.getJSONArray("monthStarts").length());

        JSONArray events = root.getJSONArray("events");
        JSONArray exact = root.getJSONArray("exactEvents");
        assertTrue(events.length() > 0);
        assertNotNull(exact);

        assertFalse(root.has("revision"));
        assertFalse(root.has("version"));
        assertFalse(root.has("coverage_from"));
        assertFalse(root.has("coverage_to"));
        assertFalse(root.has("exact_events"));
    }
}
