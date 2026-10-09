package com.eluqen.shiftcalendar.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ThemeSettingsActivity extends BaseActivity {
    private boolean previewWarmupScheduled;
    private ScrollView themeScroll;
    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private GradientDrawable shape(int color, int border, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        if (border != 0) g.setStroke(dp(1), border);
        return g;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi(0);
    }

    private void buildUi(int restoreScrollY) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);

        ScrollView scroll = new ScrollView(this);
        themeScroll = scroll;
        scroll.setClipToPadding(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(28));
        scroll.addView(body);
        TextView title = text(
                "تم برنامه", 22f, Ui.TEXT, true);
        body.addView(title);

        TextView intro = text(
                "هر تم با رنگ‌های هماهنگ و خوانایی کنترل‌شده طراحی شده است. "
                        + "تغییر تم بلافاصله روی رابط برنامه اعمال می‌شود.",
                13.5f, Ui.MUTED, false);
        intro.setLineSpacing(dp(2), 1.12f);
        body.addView(intro, margins(0, 5, 0, 12));

        String current = AppTheme.currentKey(this);
        for (String key : AppTheme.KEYS) {
            body.addView(themeCard(key, key.equals(current)),
                    margins(0, 0, 0, 10));
        }

        TextView note = text(
                "رنگ وضعیت‌های مهم مثل تعطیلی، مرخصی و اضافه‌کاری "
                        + "برای جلوگیری از ابهام مستقل از تم باقی می‌ماند.",
                12.5f, Ui.MUTED, false);
        note.setLineSpacing(dp(2), 1.1f);
        body.addView(note, margins(4, 5, 4, 0));

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        if (restoreScrollY > 0) {
            scroll.post(() -> scroll.scrollTo(0, restoreScrollY));
        }
    }
    private LinearLayout themeCard(String key, boolean selected) {
        AppTheme.Palette p = AppTheme.palette(key);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(shape(
                p.card,
                selected ? p.secondary : p.border,
                16));
        card.setElevation(dp(selected ? 3 : 1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView name = text(p.label, 16f, p.text, true);
        header.addView(name, new LinearLayout.LayoutParams(0, dp(38), 1f));
        TextView state = text(
                selected ? "✓ انتخاب شده" : "انتخاب",
                12.5f,
                selected ? p.secondary : p.muted,
                selected);
        state.setGravity(Gravity.CENTER);
        header.addView(state, new LinearLayout.LayoutParams(dp(92), dp(38)));
        card.addView(header);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setGravity(Gravity.CENTER);
        swatches.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int[] colors = {p.primary, p.secondary, p.accent, p.surfaceAlt};
        for (int color : colors) {
            View sample = new View(this);
            sample.setBackground(shape(color, 0, 9));
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, dp(34), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            swatches.addView(sample, lp);
        }
        card.addView(swatches, margins(0, 3, 0, 8));
        LinearLayout readability = new LinearLayout(this);
        readability.setOrientation(LinearLayout.VERTICAL);
        readability.setPadding(dp(10), dp(8), dp(10), dp(8));
        readability.setBackground(shape(p.surface, p.border, 11));
        TextView sampleTitle = text(
                "نمونه متن اصلی", 13.5f, p.text, true);
        TextView sampleDetail = text(
                "متن توضیحی با کنتراست کنترل‌شده", 12f, p.muted, false);
        readability.addView(sampleTitle);
        readability.addView(sampleDetail);
        card.addView(readability);

        card.setContentDescription(
                "تم " + p.label + (selected ? "، انتخاب شده" : ""));
        card.setOnClickListener(v -> {
            if (key.equals(AppTheme.currentKey(this))) return;
            int restoreScrollY =
                    themeScroll == null ? 0 : themeScroll.getScrollY();
            AppTheme.set(this, key);
            buildUi(restoreScrollY);
        });
        return card;
    }

    // Theme selection updates the visible view in place. Never launch hidden
    // preview Activities from this screen: those produced a black transition.
    // Previews are regenerated by the normal root-entry flow when needed.

    private LinearLayout.LayoutParams margins(
            int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return lp;
    }
}
