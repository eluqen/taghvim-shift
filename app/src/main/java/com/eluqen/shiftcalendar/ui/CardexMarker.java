package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import java.util.ArrayList;
import java.util.List;

public final class CardexMarker {
    private static final String FOUR_MARKERS = "eluqen_four_markers";

    private CardexMarker() {}

    private static final class Mark {
        final int glyph;
        final String symbol;
        final String label;
        final int color;

        Mark(int glyph, String symbol, String label, int color) {
            this.glyph = glyph;
            this.symbol = symbol;
            this.label = label;
            this.color = color;
        }
    }

    private static List<Mark> marks(CardexRecord record) {
        ArrayList<Mark> out = new ArrayList<>();
        if (record == null) return out;

        if (hasNote(record)) {
            out.add(new Mark(
                    WorkStatusGlyph.NOTE,
                    "✎",
                    "یادداشت",
                    0xff687686));
        }

        int mode = DayWorkResolver.mode(record);
        if (mode == DayWorkResolver.MODE_SWAP) {
            out.add(new Mark(
                    WorkStatusGlyph.SWAP,
                    "↔",
                    "تعویض شیفت",
                    0xff7955a5));
        } else if (mode == DayWorkResolver.MODE_LEAVE) {
            out.add(new Mark(
                    WorkStatusGlyph.LEAVE,
                    "م",
                    !record.leaveFullShift
                            && text(record.leaveStart)
                            && text(record.leaveEnd)
                            ? "مرخصی ساعتی"
                            : "مرخصی روزانه",
                    0xffd9822b));
        } else if (mode == DayWorkResolver.MODE_REST) {
            out.add(new Mark(
                    WorkStatusGlyph.REST,
                    "ا",
                    !record.restFullShift
                            && text(record.restStart)
                            && text(record.restEnd)
                            ? "استراحت ساعتی"
                            : "استراحت روزانه",
                    0xff2f80b7));
        } else if (mode == DayWorkResolver.MODE_OVERTIME) {
            out.add(new Mark(
                    WorkStatusGlyph.OVERTIME,
                    "+",
                    "اضافه‌کاری",
                    0xff25865c));
        }
        return out;
    }

    private static List<Mark> marks(
            Activity activity, Object date) {
        return marks(AppDatabase.get(activity)
                .cardex(String.valueOf(date)));
    }

    private static boolean hasNote(CardexRecord record) {
        return text(record.note)
                || text(record.noteTime)
                || text(record.noteInk);
    }

    private static boolean text(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Legacy calendar marker stays compact; FourShift uses the real illustrated glyphs. */
    private static void applyCalendar(
            Activity activity, Object date, TextView view) {
        List<Mark> marks = marks(activity, date);
        if (marks.isEmpty()) return;
        Mark mark = marks.get(marks.size() - 1);
        String base = String.valueOf(view.getText())
                .replace("  •", "")
                .trim();
        String suffix = "  " + mark.symbol + " ";
        SpannableString span =
                new SpannableString(base + suffix);
        int start = base.length() + 2;
        int end = span.length();
        span.setSpan(
                new BackgroundColorSpan(mark.color),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(
                new ForegroundColorSpan(Color.WHITE),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(
                new StyleSpan(Typeface.BOLD),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(
                new RelativeSizeSpan(.9f),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(span);
        view.setContentDescription(
                base + "، " + mark.label + " ثبت شده");
    }

    public static void calendar(
            Activity activity, Object date, TextView shift) {
        applyCalendar(activity, date, shift);
    }

    /**
     * FourShift intentionally uses the same illustrated WorkStatusGlyph set as
     * the Work page. No single-letter Cardex markers are rendered here.
     */
    public static void four(
            Activity activity, Object date, LinearLayout row) {
        CardexRecord record = AppDatabase.get(activity)
                .cardex(String.valueOf(date));
        four(activity, date, row, record);
    }

    public static void four(
            Activity activity, Object date, LinearLayout row,
            CardexRecord record) {
        if (row == null || row.getChildCount() == 0) return;
        View first = row.getChildAt(0);
        if (!(first instanceof FrameLayout)) return;
        FrameLayout dateCell = (FrameLayout) first;

        List<Mark> marks = marks(record);
        if (marks.isEmpty()) return;

        for (int i = 0; i < dateCell.getChildCount(); i++) {
            View child = dateCell.getChildAt(i);
            if (child instanceof TextView
                    && "four_date_text".equals(child.getTag())) {
                TextView label = (TextView) child;
                label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                label.setPadding(
                        Ui.dp(activity, 2), Ui.dp(activity, 3),
                        Ui.dp(activity, 2), Ui.dp(activity, 29));
                break;
            }
        }

        LinearLayout strip = new LinearLayout(activity);
        strip.setTag(FOUR_MARKERS);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(
                Ui.dp(activity, 2), 0,
                Ui.dp(activity, 2), 0);

        StringBuilder description = new StringBuilder();
        for (Mark mark : marks) {
            WorkStatusGlyph glyph = new WorkStatusGlyph(
                    activity, mark.glyph, mark.color, true);
            LinearLayout.LayoutParams gp =
                    new LinearLayout.LayoutParams(
                            Ui.dp(activity, 27),
                            Ui.dp(activity, 27));
            gp.setMargins(
                    Ui.dp(activity, 1), 0,
                    Ui.dp(activity, 1), 0);
            strip.addView(glyph, gp);
            if (description.length() > 0) description.append("، ");
            description.append(mark.label);
        }

        FrameLayout.LayoutParams sp =
                new FrameLayout.LayoutParams(
                        -2,
                        Ui.dp(activity, 29),
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        sp.bottomMargin = Ui.dp(activity, 1);
        dateCell.addView(strip, sp);

        CharSequence old = dateCell.getContentDescription();
        dateCell.setContentDescription(
                (old == null ? "" : old + "، ")
                        + description + " ثبت شده");
    }
}
