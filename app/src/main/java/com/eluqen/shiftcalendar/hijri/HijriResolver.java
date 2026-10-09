package com.eluqen.shiftcalendar.hijri;

import android.content.Context;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;

/**
 * Single source of truth for Hijri conversion.
 *
 * The signed event bank may contain authoritative Hijri month starts. The
 * user's manual correction is applied by shifting the requested civil day,
 * never by mutating or resetting the server data.
 */
public final class HijriResolver {
    private static final int REVERSE_SEARCH_DAYS = 10;

    public interface ServerCalendar {
        HijriDate resolve(PersianDate date);
    }

    private HijriResolver() {}

    public static HijriDate resolve(Context context, PersianDate date) {
        EventRepository repository = EventRepository.get(context);
        return resolve(
                date,
                Prefs.hijriManualOffset(context),
                repository::resolveServerHijri);
    }

    public static HijriDate resolve(PersianDate date, int manualOffset,
                                    ServerCalendar serverCalendar) {
        PersianDate adjusted = date.plusDays(manualOffset);
        return serverCalendar.resolve(adjusted);
    }

    public static int manualOffset(Context context) {
        return Prefs.hijriManualOffset(context);
    }

    public static PersianDate fromHijri(Context context, int year, int month, int day) {
        EventRepository repository = EventRepository.get(context);
        return fromHijri(
                year,
                month,
                day,
                Prefs.hijriManualOffset(context),
                repository::resolveServerHijri);
    }

    public static PersianDate fromHijri(int year, int month, int day,
                                       int manualOffset,
                                       ServerCalendar serverCalendar) {
        if (month < 1 || month > 12 || day < 1 || day > 30) {
            throw new IllegalArgumentException("تاریخ قمری نامعتبر است");
        }

        long tabularCandidate = HijriDate.toJdn(year, month, day) - manualOffset;
        PersianDate best = null;
        long bestDistance = Long.MAX_VALUE;

        for (int delta = -REVERSE_SEARCH_DAYS; delta <= REVERSE_SEARCH_DAYS; delta++) {
            PersianDate candidate = JalaliCalendar.fromJdn(tabularCandidate + delta);
            HijriDate resolved = resolve(candidate, manualOffset, serverCalendar);
            if (same(resolved, year, month, day)) {
                long distance = Math.abs(candidate.toJdn() - tabularCandidate);
                if (best == null || distance < bestDistance
                        || (distance == bestDistance
                        && candidate.toJdn() < best.toJdn())) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }

        if (best == null) {
            throw new IllegalArgumentException(
                    "تاریخ قمری انتخاب‌شده با اطلاعات فعلی قابل تبدیل نیست");
        }
        return best;
    }

    public static int daysInMonth(Context context, int year, int month) {
        EventRepository repository = EventRepository.get(context);
        return daysInMonth(
                year,
                month,
                Prefs.hijriManualOffset(context),
                repository::resolveServerHijri);
    }

    public static int daysInMonth(int year, int month, int manualOffset,
                                  ServerCalendar serverCalendar) {
        try {
            PersianDate day30 = fromHijri(
                    year, month, 30, manualOffset, serverCalendar);
            HijriDate resolved = resolve(day30, manualOffset, serverCalendar);
            if (same(resolved, year, month, 30)) return 30;
        } catch (RuntimeException ignored) {
        }
        return 29;
    }

    private static boolean same(HijriDate value, int year, int month, int day) {
        return value.year == year && value.month == month && value.day == day;
    }
}
