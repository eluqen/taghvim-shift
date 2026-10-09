package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.eluqen.shiftcalendar.data.Prefs;

/** Keeps the Settings/Features hub reminder card in sync with real state. */
public final class ReminderPolish {
    private ReminderPolish() {}

    private static TextView find(View view, String text) {
        if (view instanceof TextView
                && text.equals(((TextView) view).getText().toString())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = find(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void hub(Activity activity) {
        try {
            View root = activity.findViewById(android.R.id.content);
            TextView heading = find(root, "یادآوری شروع شیفت");
            if (heading == null
                    || !(heading.getParent() instanceof ViewGroup)) {
                return;
            }

            ViewGroup card = (ViewGroup) heading.getParent();
            boolean on = Prefs.reminderEnabled(activity);
            for (int i = 0; i < card.getChildCount(); i++) {
                View child = card.getChildAt(i);
                if (child instanceof Button) {
                    ((Button) child).setText(
                            on ? "مدیریت یادآوری فعال" : "تنظیم یادآوری");
                } else if (child instanceof TextView
                        && child != heading) {
                    ((TextView) child).setText(
                            on
                                    ? "یادآوری فعال است؛ برای ویرایش وارد شوید."
                                    : "یادآوری غیرفعال است؛ برای فعال‌سازی وارد شوید.");
                }
            }
        } catch (RuntimeException error) {
            android.util.Log.e(
                    "ELUQEN_REMINDER",
                    "Hub status refresh failed",
                    error);
        }
    }
}
