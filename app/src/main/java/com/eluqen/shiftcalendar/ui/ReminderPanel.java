package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Read-only reminder diagnostics and future-preview helpers. */
public final class ReminderPanel {
    private ReminderPanel() {}

    public static String scheduleStatusText(Context context) {
        long stamp = Prefs.lastReminderSchedule(context);
        String last = Prefs.lastReminderStatus(context);
        if (!Prefs.reminderEnabled(context)) {
            last = "یادآوری اصلی غیرفعال است؛ اعلان زمان‌بندی‌شده‌ای نباید باقی بماند.";
        }
        String date = stamp > 0
                ? DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT,
                        new Locale("fa"))
                    .format(new Date(stamp))
                : "ثبت نشده";

        return PersianDigits.of(
                last
                + "\nآخرین محاسبه: " + date
                + "\nتغییر الگو یا کارکرد، زمان‌بندی آینده را دوباره محاسبه می‌کند.");
    }

    public static void openUpcoming(Activity activity) {
        if (!Prefs.reminderEnabled(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle("یادآوری غیرفعال است")
                    .setMessage("ابتدا یادآوری شروع شیفت را روشن کنید.")
                    .setPositiveButton("بستن", null)
                    .show();
            return;
        }

        Toast.makeText(
                activity,
                "در حال محاسبه یادآوری‌های آینده…",
                Toast.LENGTH_SHORT)
                .show();

        Context app = activity.getApplicationContext();
        new Thread(() -> {
            try {
                List<ReminderScheduler.NextReminder> items =
                        ReminderScheduler.previewUpcoming(app, 10);
                String result = futureText(items);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()
                            || activity.isDestroyed()) return;

                    ScrollView scroll = new ScrollView(activity);
                    TextView body =
                            Ui.text(activity, result, 14f, Ui.TEXT);
                    body.setPadding(
                            Ui.dp(activity, 18),
                            Ui.dp(activity, 12),
                            Ui.dp(activity, 18),
                            Ui.dp(activity, 18));
                    scroll.addView(body);

                    new AlertDialog.Builder(activity)
                            .setTitle("۱۰ یادآوری بعدی")
                            .setView(scroll)
                            .setPositiveButton("بستن", null)
                            .show();
                });
            } catch (RuntimeException error) {
                android.util.Log.e(
                        "ELUQEN_REMINDER",
                        "Future preview error",
                        error);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()
                            || activity.isDestroyed()) return;
                    new AlertDialog.Builder(activity)
                            .setTitle("پیش‌نمایش در دسترس نیست")
                            .setMessage(
                                    "محاسبه فهرست فعلاً انجام نشد. "
                                    + "زمان‌بندی اصلی تغییری نکرده است.")
                            .setPositiveButton("بستن", null)
                            .show();
                });
            }
        }, "eluqen-reminder-future-preview").start();
    }

    private static String futureText(
            List<ReminderScheduler.NextReminder> items) {
        StringBuilder out = new StringBuilder();
        if (items == null || items.isEmpty()) {
            out.append(
                    "با وضعیت فعلی کارکرد و شیفت‌ها "
                    + "یادآوری آینده‌ای پیدا نشد.");
            return out.toString();
        }

        int index = 0;
        for (ReminderScheduler.NextReminder item : items) {
            index++;
            out.append(index)
                    .append(". ")
                    .append(item.date.displayLong())
                    .append(" · ")
                    .append(item.title)
                    .append("\n")
                    .append(item.message)
                    .append("\n")
                    .append("زمان اعلان: ")
                    .append(item.triggerDisplay());
            if (index < items.size()) out.append("\n\n");
        }
        return PersianDigits.of(out.toString());
    }
}
