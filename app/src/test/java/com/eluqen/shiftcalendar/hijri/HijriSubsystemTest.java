package com.eluqen.shiftcalendar.hijri;

import static org.junit.Assert.*;

import com.eluqen.shiftcalendar.core.Event;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.events.EventRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class HijriSubsystemTest {
    private JSONObject bank() throws Exception {
        File file = new File("src/main/assets/event_bank.json");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) output.write(buffer, 0, read);
            }
        }
        return new JSONObject(
                new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    private EventRepository.EventPack validate(JSONObject root) throws Exception {
        return EventRepository.validateCanonical(
                root.toString().getBytes(StandardCharsets.UTF_8),
                root.getInt("dataVersion"),
                root.getInt("coverageFrom"),
                root.getInt("coverageTo"),
                root.getString("publishedAt"));
    }

    private JSONObject policy(JSONArray starts) throws Exception {
        return new JSONObject()
                .put("algorithm", "tabular")
                .put("correctionVersion", 7)
                .put("monthStarts", starts);
    }

    private static JSONObject start(HijriDate hijri, PersianDate persian)
            throws Exception {
        return new JSONObject()
                .put("hijriYear", hijri.year)
                .put("hijriMonth", hijri.month)
                .put("persianDate", persian.toString());
    }

    private static HijriDate nextMonth(HijriDate value) {
        return value.month == 12
                ? new HijriDate(value.year + 1, 1, 1)
                : new HijriDate(value.year, value.month + 1, 1);
    }

    private static Fixture fixture(int firstMonthLength) throws Exception {
        PersianDate reference = new PersianDate(1405, 6, 15);
        HijriDate atReference = HijriDate.fromJdn(reference.toJdn(), 0);
        long tabularMonthStart =
                HijriDate.toJdn(atReference.year, atReference.month, 1);
        PersianDate first = JalaliCalendar.fromJdn(tabularMonthStart);
        HijriDate firstHijri =
                HijriDate.fromJdn(tabularMonthStart, 0);
        HijriDate secondHijri = nextMonth(firstHijri);
        PersianDate second = first.plusDays(firstMonthLength);

        JSONArray starts = new JSONArray()
                .put(start(firstHijri, first))
                .put(start(secondHijri, second));

        JSONObject root = new HijriSubsystemTest().bank();
        root.put("hijri", new HijriSubsystemTest().policy(starts));
        return new Fixture(
                new HijriSubsystemTest().validate(root),
                first,
                firstHijri,
                second,
                secondHijri,
                root);
    }

    @Test
    public void bundledBankHasVerified1405OfficialMonthStarts() throws Exception {
        JSONObject root = bank();
        JSONObject hijri = root.getJSONObject("hijri");
        assertEquals("tabular", hijri.getString("algorithm"));
        assertEquals(2, hijri.getInt("correctionVersion"));
        assertEquals(13, hijri.getJSONArray("monthStarts").length());

        EventRepository.EventPack pack = validate(root);
        HijriDate current = pack.resolveServerHijri(new PersianDate(1405, 7, 10));
        assertHijri(new HijriDate(1448, 4, 20), current);

        List<Event> events = pack.eventsForDate(new PersianDate(1405, 7, 24), 0);
        assertTrue(hasTitle(events, "میلاد حضرت زینب و روز پرستار"));
    }

    @Test
    public void missingHijriBlockFallsBackToTabularZero() throws Exception {
        JSONObject root = bank();
        root.remove("hijri");
        EventRepository.EventPack pack = validate(root);
        PersianDate date = new PersianDate(1405, 8, 10);
        HijriDate expected = HijriDate.fromJdn(date.toJdn(), 0);
        HijriDate actual = pack.resolveServerHijri(date);
        assertHijri(expected, actual);
        assertEquals(0, pack.hijriCorrectionVersion());
        assertFalse(pack.hasAuthoritativeHijri(date));
    }

    @Test
    public void outsideAnchorsAlwaysFallsBackToTabularZero() throws Exception {
        JSONObject root = bank();
        root.put("hijri", policy(new JSONArray()));
        EventRepository.EventPack pack = validate(root);
        PersianDate date = new PersianDate(1405, 8, 10);
        assertHijri(
                HijriDate.fromJdn(date.toJdn(), 0),
                pack.resolveServerHijri(date));
    }

    @Test
    public void authoritativeMonthStartProducesDayOne() throws Exception {
        Fixture f = fixture(30);
        assertHijri(f.firstHijri, f.pack.resolveServerHijri(f.first));
        assertTrue(f.pack.hasAuthoritativeHijri(f.first));
    }

    @Test
    public void thirtyDayOfficialMonthProducesDayThirty() throws Exception {
        Fixture f = fixture(30);
        HijriDate value = f.pack.resolveServerHijri(f.first.plusDays(29));
        assertEquals(f.firstHijri.year, value.year);
        assertEquals(f.firstHijri.month, value.month);
        assertEquals(30, value.day);
        assertTrue(f.pack.hasAuthoritativeHijri(f.first.plusDays(29)));
    }

    @Test
    public void thirtyDayMonthTransitionsToNextDayOne() throws Exception {
        Fixture f = fixture(30);
        assertHijri(f.secondHijri, f.pack.resolveServerHijri(f.second));
    }

    @Test
    public void twentyNineDayMonthTransitionsToNextDayOne() throws Exception {
        Fixture f = fixture(29);
        HijriDate day29 = f.pack.resolveServerHijri(f.first.plusDays(28));
        assertEquals(29, day29.day);
        assertHijri(f.secondHijri, f.pack.resolveServerHijri(f.second));
    }

    @Test
    public void manualCorrectionShiftsCivilDayWithoutMutatingServerPolicy()
            throws Exception {
        Fixture f = fixture(30);
        HijriDate value = HijriResolver.resolve(
                f.first, 1, f.pack::resolveServerHijri);
        assertEquals(2, value.day);
        assertHijri(f.firstHijri, f.pack.resolveServerHijri(f.first));
    }

    @Test
    public void reverseConversionRoundTripsWithNoManualCorrection()
            throws Exception {
        Fixture f = fixture(30);
        PersianDate expected = f.first.plusDays(14);
        HijriDate target = f.pack.resolveServerHijri(expected);
        PersianDate actual = HijriResolver.fromHijri(
                target.year, target.month, target.day,
                0, f.pack::resolveServerHijri);
        assertEquals(expected, actual);
    }

    @Test
    public void reverseConversionRoundTripsWithManualCorrection()
            throws Exception {
        Fixture f = fixture(30);
        PersianDate expected = f.first.plusDays(10);
        HijriDate target = HijriResolver.resolve(
                expected, 1, f.pack::resolveServerHijri);
        PersianDate actual = HijriResolver.fromHijri(
                target.year, target.month, target.day,
                1, f.pack::resolveServerHijri);
        assertEquals(expected, actual);
    }

    @Test
    public void reverseConversionSupportsOfficialDayThirty()
            throws Exception {
        Fixture f = fixture(30);
        PersianDate expected = f.first.plusDays(29);
        PersianDate actual = HijriResolver.fromHijri(
                f.firstHijri.year, f.firstHijri.month, 30,
                0, f.pack::resolveServerHijri);
        assertEquals(expected, actual);
    }

    @Test
    public void monthLengthUsesOfficialThirtyDayBoundary() throws Exception {
        Fixture f = fixture(30);
        assertEquals(30, HijriResolver.daysInMonth(
                f.firstHijri.year, f.firstHijri.month,
                0, f.pack::resolveServerHijri));
    }

    @Test
    public void monthLengthUsesOfficialTwentyNineDayBoundary() throws Exception {
        Fixture f = fixture(29);
        assertEquals(29, HijriResolver.daysInMonth(
                f.firstHijri.year, f.firstHijri.month,
                0, f.pack::resolveServerHijri));
    }

    @Test
    public void recurringHijriEventUsesAuthoritativeResolver() throws Exception {
        Fixture f = fixture(30);
        JSONObject event = new JSONObject()
                .put("calendar", "hijri")
                .put("month", f.firstHijri.month)
                .put("day", 30)
                .put("title", "QA-HIJRI-30")
                .put("holiday", false)
                .put("category", "qa");
        f.root.getJSONArray("events").put(event);
        EventRepository.EventPack pack = validate(f.root);
        List<Event> events = pack.eventsForDate(f.first.plusDays(29), 0);
        assertTrue(hasTitle(events, "QA-HIJRI-30"));
    }

    @Test
    public void exactEventRemainsBoundToPersianDate() throws Exception {
        Fixture f = fixture(30);
        JSONObject exact = new JSONObject()
                .put("year", f.first.year)
                .put("month", f.first.month)
                .put("day", f.first.day)
                .put("title", "QA-EXACT")
                .put("holiday", true)
                .put("category", "qa");
        f.root.getJSONArray("exactEvents").put(exact);
        EventRepository.EventPack pack = validate(f.root);
        assertTrue(hasTitle(pack.eventsForDate(f.first, 0), "QA-EXACT"));
        assertFalse(hasTitle(pack.eventsForDate(f.first.plusDays(1), 0), "QA-EXACT"));
    }

    @Test
    public void invalidAlgorithmRejected() throws Exception {
        JSONObject root = bank();
        root.getJSONObject("hijri").put("algorithm", "unknown");
        rejects(() -> validate(root));
    }

    @Test
    public void invalidCorrectionVersionRejected() throws Exception {
        JSONObject root = bank();
        root.getJSONObject("hijri").put("correctionVersion", 0);
        rejects(() -> validate(root));
    }

    @Test
    public void missingMonthStartsRejected() throws Exception {
        JSONObject root = bank();
        root.getJSONObject("hijri").remove("monthStarts");
        rejects(() -> validate(root));
    }

    @Test
    public void invalidPersianMonthStartRejected() throws Exception {
        JSONObject root = bank();
        JSONArray starts = new JSONArray().put(
                new JSONObject()
                        .put("hijriYear", 1448)
                        .put("hijriMonth", 1)
                        .put("persianDate", "1405/13/01"));
        root.put("hijri", policy(starts));
        rejects(() -> validate(root));
    }

    @Test
    public void monthStartOutsideCoverageRejected() throws Exception {
        JSONObject root = bank();
        JSONArray starts = new JSONArray().put(
                new JSONObject()
                        .put("hijriYear", 1448)
                        .put("hijriMonth", 1)
                        .put("persianDate", "1410/01/01"));
        root.put("hijri", policy(starts));
        rejects(() -> validate(root));
    }

    @Test
    public void duplicateMonthStartRejected() throws Exception {
        Fixture f = fixture(30);
        JSONArray starts = f.root.getJSONObject("hijri").getJSONArray("monthStarts");
        starts.put(new JSONObject(starts.getJSONObject(1).toString()));
        rejects(() -> validate(f.root));
    }

    @Test
    public void descendingMonthStartRejected() throws Exception {
        Fixture f = fixture(30);
        JSONArray starts = f.root.getJSONObject("hijri").getJSONArray("monthStarts");
        JSONObject a = starts.getJSONObject(0);
        JSONObject b = starts.getJSONObject(1);
        JSONArray reversed = new JSONArray().put(b).put(a);
        f.root.getJSONObject("hijri").put("monthStarts", reversed);
        rejects(() -> validate(f.root));
    }

    @Test
    public void consecutiveMonthStartMustBeTwentyNineOrThirtyDays()
            throws Exception {
        Fixture f = fixture(30);
        JSONArray starts = f.root.getJSONObject("hijri").getJSONArray("monthStarts");
        JSONObject second = starts.getJSONObject(1);
        second.put("persianDate", f.first.plusDays(31).toString());
        rejects(() -> validate(f.root));
    }

    @Test
    public void monthStartMoreThanTwoDaysFromTabularIsRejected() throws Exception {
        PersianDate reference = new PersianDate(1405, 6, 15);
        HijriDate h = HijriDate.fromJdn(reference.toJdn(), 0);
        long base = HijriDate.toJdn(h.year, h.month, 1);
        PersianDate tooFar = JalaliCalendar.fromJdn(base + 3);
        JSONObject root = bank();
        JSONArray starts = new JSONArray().put(start(
                new HijriDate(h.year, h.month, 1), tooFar));
        root.put("hijri", policy(starts));
        rejects(() -> validate(root));
    }

    @Test
    public void legacyBankStillParsesWithoutHijriPolicy() throws Exception {
        JSONObject legacy = new JSONObject()
                .put("schema", 1)
                .put("revision", 1)
                .put("version", "legacy")
                .put("coverage_from", 1405)
                .put("coverage_to", 1409)
                .put("published_at", "legacy")
                .put("events", new JSONArray())
                .put("exact_events", new JSONArray());
        EventRepository.EventPack pack = EventRepository.validate(
                legacy.toString().getBytes(StandardCharsets.UTF_8));
        PersianDate date = new PersianDate(1405, 1, 1);
        assertHijri(
                HijriDate.fromJdn(date.toJdn(), 0),
                pack.resolveServerHijri(date));
    }

    private static boolean hasTitle(List<Event> events, String title) {
        for (Event event : events) {
            if (title.equals(event.title)) return true;
        }
        return false;
    }

    private static void assertHijri(HijriDate expected, HijriDate actual) {
        assertEquals(expected.year, actual.year);
        assertEquals(expected.month, actual.month);
        assertEquals(expected.day, actual.day);
    }

    private interface Throwing {
        void run() throws Exception;
    }

    private static void rejects(Throwing action) throws Exception {
        try {
            action.run();
            fail("Expected rejection");
        } catch (Exception expected) {
            // expected
        }
    }

    private static final class Fixture {
        final EventRepository.EventPack pack;
        final PersianDate first;
        final HijriDate firstHijri;
        final PersianDate second;
        final HijriDate secondHijri;
        final JSONObject root;

        Fixture(EventRepository.EventPack pack,
                PersianDate first,
                HijriDate firstHijri,
                PersianDate second,
                HijriDate secondHijri,
                JSONObject root) {
            this.pack = pack;
            this.first = first;
            this.firstHijri = firstHijri;
            this.second = second;
            this.secondHijri = secondHijri;
            this.root = root;
        }
    }
}
