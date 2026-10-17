package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.data.UserEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PersonalEventItemsTest {
    @Test public void persistedIdentityUsesDatabaseRecordIdNotCalendarDateOrTitle() {
        UserEvent first = event(102, 0, 7, 12, "سالگرد", false);
        UserEvent second = event(103, 0, 7, 12, "سالگرد", false);
        assertEquals(102L, PersonalEventItems.persistedId(first));
        assertEquals(103L, PersonalEventItems.persistedId(second));
        assertNotEquals(PersonalEventItems.persistedId(first),
                PersonalEventItems.persistedId(second));
    }

    @Test public void listLabelMakesAnnualAndDatedEntriesDiscoverable() {
        assertEquals("هر سال — 7/12 — سالگرد",
                PersonalEventItems.label(event(1, 0, 7, 12, "سالگرد", false)));
        assertEquals("1405 — 7/12 — یادبود (تعطیل)",
                PersonalEventItems.label(event(2, 1405, 7, 12, "یادبود", true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void neverDeleteUnsavedEvent() {
        PersonalEventItems.persistedId(event(0, 1405, 1, 1, "تست", false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void neverDeleteNullEvent() {
        PersonalEventItems.persistedId(null);
    }

    @Test public void multipleIndependentEventsOnSameDayRemainInTheList() {
        java.util.List<UserEvent> events = java.util.Arrays.asList(
                event(10, 1405, 7, 12, "تولد اول", false),
                event(11, 1405, 7, 12, "تولد دوم", false),
                event(12, 0, 7, 12, "سالگرد", true));
        java.util.List<UserEvent> result = PersonalEventItems.sorted(events);
        assertEquals(3, result.size());
        assertEquals(3, result.stream().map(e -> e.f2id).distinct().count());
    }

    @Test public void sortedEventsAreGroupedByPersianMonthAndDay() {
        java.util.List<UserEvent> events = java.util.Arrays.asList(
                event(5, 1404, 10, 10, "پایان", false),
                event(2, 1405, 2, 7, "دوم", false),
                event(1, 0, 2, 7, "اول", true),
                event(3, 1403, 1, 1, "آغاز", false));
        java.util.List<UserEvent> ordered = PersonalEventItems.sorted(events);
        assertEquals(3L, ordered.get(0).f2id);
        assertEquals(1L, ordered.get(1).f2id);
        assertEquals(2L, ordered.get(2).f2id);
        assertEquals(5L, ordered.get(3).f2id);
    }

    @Test public void duplicateDetectionWarnsButDoesNotMergeOrLimitDay() {
        UserEvent existing = event(10, 0, 7, 12, "سالگرد", false);
        java.util.List<UserEvent> items = java.util.Collections.singletonList(existing);
        UserEvent same = event(0, 1405, 7, 12, " سالگرد ", false);
        assertSame(existing, PersonalEventItems.findDuplicate(items, same));
        assertNull(PersonalEventItems.findDuplicate(items,
                event(0, 1405, 7, 12, "تولد", false)));
        assertNull(PersonalEventItems.findDuplicate(items,
                event(0, 1405, 7, 13, "سالگرد", false)));
    }

    @Test public void editingSameRecordDoesNotFlagItselfAsDuplicate() {
        UserEvent existing = event(17, 1405, 7, 12, "تولد", false);
        assertNull(PersonalEventItems.findDuplicate(
                java.util.Collections.singletonList(existing),
                event(17, 1405, 7, 12, "تولد", true)));
    }

    private static UserEvent event(long id, int year, int month, int day,
                                   String title, boolean holiday) {
        UserEvent item = new UserEvent();
        item.f2id = id;
        item.year = year;
        item.month = month;
        item.day = day;
        item.title = title;
        item.holiday = holiday;
        return item;
    }
}
