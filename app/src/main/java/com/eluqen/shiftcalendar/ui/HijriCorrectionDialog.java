package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.hijri.HijriResolver;

final class HijriCorrectionDialog {
    private HijriCorrectionDialog() {}

    static void show(Activity activity, Runnable afterSave) {
        int savedOffset = Prefs.hijriManualOffset(activity);
        PersianDate today = PersianDate.todayTehran();
        EventRepository repository = EventRepository.get(activity);

        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        body.setBackground(Ui.roundedStroke(Ui.CARD, Ui.BORDER, 18, activity));

        TextView preview = new TextView(activity);
        preview.setGravity(Gravity.RIGHT);
        preview.setTextSize(15.5f);
        preview.setTextColor(Ui.TEXT);
        preview.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT, Ui.BORDER, 12, activity));
        preview.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        preview.setLineSpacing(0.0f, 1.15f);
        body.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(0);
        picker.setMaxValue(4);
        picker.setDisplayedValues(new String[]{"-۲", "-۱", "۰", "+۱", "+۲"});
        picker.setValue(savedOffset + 2);
        picker.setWrapSelectorWheel(false);
        FancyDialogs.styleWheel(activity, picker);
        body.addView(picker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 190)));

        Runnable refreshPreview = () -> {
            int selectedOffset = picker.getValue() - 2;
            PersianDate adjustedDate = today.plusDays(selectedOffset);
            boolean authoritative = repository.hasAuthoritativeHijri(adjustedDate);
            HijriDate result = HijriResolver.resolve(
                    today, selectedOffset, repository::resolveServerHijri);

            String text = "وضعیت خودکار: "
                    + (authoritative
                    ? "شروع ماه رسمی برای نتیجه انتخاب‌شده در بانک ثبت شده"
                    : "محاسباتی؛ شروع ماه رسمی برای نتیجه انتخاب‌شده ثبت نشده")
                    + "\nتنظیم ذخیره‌شده: " + signed(savedOffset) + " روز"
                    + "\nانتخاب فعلی: " + signed(selectedOffset) + " روز"
                    + "\nنتیجه برای امروز: " + result.displayLong()
                    + "\n\nتا زمانی که «ذخیره» را نزنید، تنظیم فعلی شما تغییر نمی‌کند.";
            preview.setText(PersianDigits.of(text));
        };

        picker.setOnValueChangedListener((numberPicker, oldValue, newValue) ->
                refreshPreview.run());
        refreshPreview.run();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("تنظیم دستی تاریخ قمری")
                .setView(body)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (d, which) -> {
                    Prefs.setHijriManualOffset(activity, picker.getValue() - 2);
                    if (afterSave != null) afterSave.run();
                })
                .create();
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(
                                android.graphics.Color.TRANSPARENT));
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.SECONDARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Ui.MUTED);
        });
        dialog.show();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
