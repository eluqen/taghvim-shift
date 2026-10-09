package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import com.eluqen.shiftcalendar.core.PersianDate;

/**
 * Compatibility facade for callers that only need a Persian date.
 * The actual picker UI is DualDatePicker, the single authoritative
 * polished date-selection surface in the app.
 */
public final class PersianDatePicker {
    public interface Listener {
        void onPicked(PersianDate persianDate);
    }

    private PersianDatePicker() {}

    public static void show(
            Activity activity, PersianDate current, Listener listener) {
        DualDatePicker.show(activity, current, listener);
    }
}
