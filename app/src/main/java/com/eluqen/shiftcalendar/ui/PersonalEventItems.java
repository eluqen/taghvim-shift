package com.eluqen.shiftcalendar.ui;

import com.eluqen.shiftcalendar.data.UserEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure mappings for rendering and identifying saved personal events. */
public final class PersonalEventItems {
    private PersonalEventItems() {}

    public static long persistedId(UserEvent event) {
        if (event == null || event.f2id <= 0L) {
            throw new IllegalArgumentException("An unsaved event cannot be deleted");
        }
        return event.f2id;
    }

    /** Keep all occasions independently addressable, even on the same day. */
    public static List<UserEvent> sorted(List<UserEvent> events) {
        ArrayList<UserEvent> out = new ArrayList<>(events);
        // Collections.sort and explicit Comparator work on minSdk 23.
        // Preserve EXACT ordering: month, day, year, normalized-null title, f2id.
        Collections.sort(out, new Comparator<UserEvent>() {
            @Override public int compare(UserEvent left, UserEvent right) {
                int c = Integer.compare(left.month, right.month);
                if (c != 0) return c;
                c = Integer.compare(left.day, right.day);
                if (c != 0) return c;
                c = Integer.compare(left.year, right.year);
                if (c != 0) return c;
                String lt = left.title == null ? "" : left.title;
                String rt = right.title == null ? "" : right.title;
                c = lt.compareTo(rt);
                return c != 0 ? c : Long.compare(left.f2id, right.f2id);
            }
        });
        return out;
    }

    /**
     * Advisory duplicate check only. A repeating annual entry and a
     * year-specific entry can overlap without either being prohibited.
     */
    public static UserEvent findDuplicate(List<UserEvent> events, UserEvent proposed) {
        for (UserEvent existing : events) {
            if (existing.f2id == proposed.f2id && proposed.f2id > 0) continue;
            if (existing.month == proposed.month && existing.day == proposed.day
                    && (existing.year == proposed.year
                        || existing.year == 0 || proposed.year == 0)
                    && normalize(existing.title).equals(normalize(proposed.title))) {
                return existing;
            }
        }
        return null;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String label(UserEvent event) {
        if (event == null) throw new IllegalArgumentException("Event required");
        String year = event.year == 0 ? "هر سال" : String.valueOf(event.year);
        return year + " — " + event.month + "/" + event.day + " — "
                + event.title + (event.holiday ? " (تعطیل)" : "");
    }
}
