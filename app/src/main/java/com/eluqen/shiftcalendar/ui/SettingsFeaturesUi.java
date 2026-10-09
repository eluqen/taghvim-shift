package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Presentation confined to the Settings and Features routes. */
final class SettingsFeaturesUi {
    static int INK = 0xff203a63, MUTED = 0xff60758b, ACCENT = 0xff087e80;
    static int SURFACE = 0xfff4f7fb, BORDER = 0xffdce5ed, CARD = 0xffffffff;
    private SettingsFeaturesUi() {}
    static void applyTheme(AppTheme.Palette p) {
        INK = p.text;
        MUTED = p.muted;
        ACCENT = p.secondary;
        SURFACE = p.surface;
        BORDER = p.border;
        CARD = p.card;
    }
    static int dp(Context c, float n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }
    static GradientDrawable shape(Context c, int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp(c, radius));
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }
    static TextView text(Context c, String value, float size, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size);
        t.setTextColor(color); t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL); t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    static LinearLayout section(Activity a, LinearLayout body, String title) {
        TextView h = text(a, title, 14, MUTED, true);
        h.setPadding(dp(a, 4), dp(a, 18), dp(a, 4), dp(a, 8));
        body.addView(h);
        LinearLayout box = new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a, 4), dp(a, 4), dp(a, 4), dp(a, 4));
        box.setBackground(shape(a, CARD, BORDER, 16)); box.setElevation(dp(a, 1));
        body.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return box;
    }
    static void divider(Activity a, LinearLayout box) {
        View d = new View(a); d.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 1));
        lp.setMargins(dp(a, 64), 0, dp(a, 12), 0); box.addView(d, lp);
    }
    static final class Row extends LinearLayout {
        final TextView title, detail, indicator, arrow;
        final Glyph icon;
        Row(Activity a, String heading, String description, int kind, Runnable action) {
            super(a); setOrientation(HORIZONTAL); setGravity(Gravity.CENTER_VERTICAL);
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
            setMinimumHeight(dp(a, 88));
            icon = new Glyph(a, kind); icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LayoutParams ip = new LayoutParams(dp(a, 40), dp(a, 40));
            ip.setMarginEnd(dp(a, 12)); addView(icon, ip);
            LinearLayout words = new LinearLayout(a); words.setOrientation(VERTICAL);
            title = text(a, heading, 15.5f, INK, true);
            detail = text(a, description, 12.5f, MUTED, false);
            // Description remains allocated when absent. State changes never alter row geometry.
            detail.setMinLines(2); detail.setPadding(0, dp(a, 5), 0, 0);
            detail.setVisibility(description.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            words.addView(title); words.addView(detail);
            addView(words, new LayoutParams(0, -2, 1));
            indicator = text(a, "", 10.5f, ACCENT, true);
            indicator.setGravity(Gravity.CENTER); indicator.setVisibility(View.INVISIBLE);
            indicator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LayoutParams bp = new LayoutParams(dp(a, 36), dp(a, 28));
            bp.setMarginStart(dp(a, 6)); addView(indicator, bp);
            arrow = text(a, "‹", 26, ACCENT, false); arrow.setGravity(Gravity.CENTER);
            arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(arrow, new LayoutParams(dp(a, 22), dp(a, 48)));
            setFocusable(true); setClickable(true);
            setOnClickListener(v -> action.run());
            active(false);
        }
        void active(boolean enabled) {
            Context c = getContext();
            setBackground(new RippleDrawable(ColorStateList.valueOf((ACCENT & 0x00ffffff) | 0x24000000),
                    shape(c, enabled ? Ui.SURFACE_ALT : CARD, 0, 12),
                    shape(c, CARD, 0, 12)));
            title.setTextColor(enabled ? ACCENT : INK);
            detail.setTextColor(enabled ? ACCENT : MUTED);
            indicator.setText(enabled ? "فعال" : "");
            indicator.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
            setContentDescription(title.getText() + "، " + detail.getText()
                    + (enabled ? "، فعال" : ""));
        }
    }
    static Row row(Activity a, LinearLayout box, String title, String detail, int kind, Runnable action) {
        Row r = new Row(a, title, detail, kind, action);
        box.addView(r, new LinearLayout.LayoutParams(-1, -2)); return r;
    }
    static int kind(String title) {
        if (title.contains("یادآور") || title.contains("ساعت")) return 1;
        if (title.contains("همکار") || title.contains("بازخورد")) return 3;
        if (title.contains("تم")) return 9;
        if (title.contains("ویجت") || title.contains("شروع")) return 4;
        if (title.contains("قمری")) return 5;
        if (title.contains("بازیابی")) return 7;
        if (title.contains("پشتیبان")) return 6;
        if (title.contains("به‌روزرسانی")) return 7;
        if (title.contains("درباره")) return 8;
        if (title.contains("تقویم") || title.contains("مناسبت")) return 4;
        return 0;
    }
    private static final class Glyph extends View {
        final Paint paint = new Paint(3); final int kind;
        Glyph(Context c, int kind) { super(c); this.kind = kind; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); canvas.save(); canvas.scale(getWidth()/40f, getHeight()/40f);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Ui.SURFACE_ALT);
            canvas.drawRoundRect(0,0,40,40,12,12,paint);
            paint.setColor(ACCENT); paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            if (kind == 1) {
                canvas.drawCircle(20,20,10,paint); canvas.drawLine(20,13,20,20,paint);
                canvas.drawLine(20,20,25,23,paint);
            } else if (kind == 2) {
                canvas.drawRoundRect(11,9,29,31,2,2,paint);
                canvas.drawLine(15,16,25,16,paint); canvas.drawLine(15,21,25,21,paint);
                canvas.drawLine(15,26,22,26,paint);
            } else if (kind == 3) {
                canvas.drawCircle(20,15,5,paint);
                Path path=new Path(); path.moveTo(11,30); path.cubicTo(11,20,29,20,29,30);
                canvas.drawPath(path,paint);
            } else if (kind == 4) {
                canvas.drawRoundRect(10,11,30,30,3,3,paint);
                canvas.drawLine(10,17,30,17,paint); canvas.drawLine(15,9,15,14,paint);
                canvas.drawLine(25,9,25,14,paint);
                canvas.drawCircle(16,23,1,paint); canvas.drawCircle(24,23,1,paint);
            } else if (kind == 5) {
                Path moon=new Path(); moon.moveTo(24,10); moon.cubicTo(8,8,7,31,23,30);
                moon.cubicTo(32,29,32,22,31,20); moon.cubicTo(20,25,16,16,24,10);
                canvas.drawPath(moon,paint);
            } else if (kind == 6 || kind == 7) {
                canvas.drawLine(11,27,11,31,paint); canvas.drawLine(11,31,29,31,paint);
                canvas.drawLine(29,31,29,27,paint); canvas.drawLine(20,10,20,25,paint);
                int y=kind==6?25:10, other=kind==6?20:15;
                canvas.drawLine(15,other,20,y,paint); canvas.drawLine(25,other,20,y,paint);
            } else if (kind == 8) {
                canvas.drawCircle(20,20,10,paint); canvas.drawLine(20,18,20,26,paint);
                canvas.drawCircle(20,14,0.7f,paint);
            } else if (kind == 9) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(14,16,3.2f,paint);
                canvas.drawCircle(25,16,3.2f,paint);
                canvas.drawCircle(20,26,3.2f,paint);
            } else {
                canvas.drawRoundRect(10,10,30,30,3,3,paint);
                canvas.drawLine(15,17,17,19,paint); canvas.drawLine(17,19,21,14,paint);
                canvas.drawLine(15,25,25,25,paint);
            }
            canvas.restore();
        }
    }
}
