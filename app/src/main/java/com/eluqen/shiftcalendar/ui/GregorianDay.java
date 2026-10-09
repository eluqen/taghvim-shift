package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;

public final class GregorianDay {
    private static final String META_TAG = "eluqen_calendar_numeric_meta";

    private GregorianDay() {}

    private static final class BaselineLowerSpan extends MetricAffectingSpan {
        private final int px;
        BaselineLowerSpan(int px) { this.px = px; }
        @Override public void updateDrawState(TextPaint paint) { paint.baselineShift += px; }
        @Override public void updateMeasureState(TextPaint paint) { paint.baselineShift += px; }
    }

    private static String persianDigits(int value) {
        String s = String.valueOf(value);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            b.append(ch >= '0' && ch <= '9'
                    ? (char) (0x06f0 + ch - '0') : ch);
        }
        return b.toString();
    }

    private static int dp(Context c, int n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }

    public static void setTextColor(LinearLayout root, int color) {
        TextView meta = root == null ? null : root.findViewWithTag(META_TAG);
        if (meta != null) meta.setTextColor(color);
    }

    public static void attach(Context c, LinearLayout root, Object date) {
        try {
            Class<?> pd = Class.forName("com.eluqen.shiftcalendar.core.PersianDate");
            Object g = Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar")
                    .getMethod("toGregorian", pd).invoke(null, date);
            int gd = g.getClass().getField("day").getInt(g);
            Object h = Class.forName("com.eluqen.shiftcalendar.hijri.HijriResolver")
                    .getMethod("resolve", Context.class, pd).invoke(null, c, date);
            int hd = h.getClass().getField("day").getInt(h);

            boolean current = true;
            try {
                Field f = c.getClass().getDeclaredField("displayedMonth");
                f.setAccessible(true);
                Object shown = f.get(c);
                current = date.getClass().getField("year").getInt(date)
                        == shown.getClass().getField("year").getInt(shown)
                        && date.getClass().getField("month").getInt(date)
                        == shown.getClass().getField("month").getInt(shown);
            } catch (Exception ignored) {}

            TextView meta = root.findViewWithTag(META_TAG);
            if (meta == null) {
                meta = new TextView(c);
                meta.setTag(META_TAG);
                meta.setTextSize(9.5f);
                meta.setIncludeFontPadding(false);
                meta.setGravity(Gravity.CENTER);
                meta.setSingleLine(true);
                root.addView(meta, new LinearLayout.LayoutParams(-1, dp(c, 22)));
            }
            String gregorian = String.valueOf(gd);
            String gap = "    ";
            String hijri = persianDigits(hd);
            SpannableString dates = new SpannableString(gregorian + gap + hijri);
            int hijriStart = gregorian.length() + gap.length();
            dates.setSpan(new RelativeSizeSpan(1.33f), hijriStart, dates.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            dates.setSpan(new StyleSpan(Typeface.BOLD), hijriStart, dates.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            dates.setSpan(new BaselineLowerSpan(dp(c, 2)), hijriStart, dates.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            meta.setText(dates);
            meta.setTextColor(current ? 0xff53626d : 0xffb2b7bb);
        } catch (Exception e) {
            android.util.Log.e("ELUQEN_DATE", "Cell calendar days failed", e);
        }
    }
}
