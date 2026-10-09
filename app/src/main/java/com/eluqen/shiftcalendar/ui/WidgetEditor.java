package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Single-size settings surface for the ELUQEN 4x1 widget. */
public final class WidgetEditor {
    private static final String BOARD_PREFS = "eluqen_widget_board";
    private static final String BOARD_THEME = "theme";
    private static final String BOARD_CUSTOM_COLOR = "custom_color";
    private static final int CUSTOM_THEME = 5;
    private static final String[] THEME_NAMES = {
            "ELUQEN", "آبی", "گرافیتی", "نیلی", "سبز", "سفارشی"
    };
    private static final int[] THEME_COLORS = {
            0xff0f5a5f, 0xff175c73, 0xff394a52,
            0xff3a3e6c, 0xff285e48
    };

    private WidgetEditor() {}
    private static int dp(Activity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private static TextView text(
            Activity a, String value, float size, int color, boolean bold) {
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        v.setPadding(dp(a, 10), dp(a, 6), dp(a, 10), dp(a, 6));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private static Button button(Activity a, String value) {
        Button b = new Button(a);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(13f);
        b.setTextColor(0xffffffff);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff0b8f7b);
        bg.setCornerRadius(dp(a, 12));
        b.setBackground(bg);
        return b;
    }
    private static int customColor(SharedPreferences board) {
        return board.getInt(BOARD_CUSTOM_COLOR, 0xff8a3ffc);
    }

    private static void styleThemeButton(
            Activity activity, Button button, int index, boolean selected,
            SharedPreferences board) {
        int color = index == CUSTOM_THEME
                ? customColor(board)
                : THEME_COLORS[index];
        button.setAllCaps(false);
        button.setText((selected ? "✓ " : "") + THEME_NAMES[index]);
        button.setTextSize(12f);
        button.setTextColor(ShiftVisuals.contrast(color));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(activity, 12));
        bg.setStroke(dp(activity, selected ? 3 : 1),
                selected ? 0xff00b8d4 : 0x66758a94);
        button.setBackground(bg);
        button.setContentDescription("رنگ ویجت " + THEME_NAMES[index]);
    }

    private static void repaintThemeButtons(
            Activity activity, Button[] buttons, int selected,
            SharedPreferences board) {
        for (int i = 0; i < buttons.length; i++) {
            styleThemeButton(activity, buttons[i], i, i == selected, board);
        }
    }
    private static void showCustomColorPicker(
            Activity activity, SharedPreferences board,
            Button[] buttons) {
        int initial = customColor(board);
        float[] hsv = new float[3];
        Color.colorToHSV(initial, hsv);
        int[] chosen = {initial};

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 18), dp(activity, 10),
                dp(activity, 18), dp(activity, 4));

        TextView preview = text(
                activity, "رنگ سفارشی", 15f, ShiftVisuals.contrast(initial), true);
        preview.setGravity(Gravity.CENTER);
        preview.setMinHeight(dp(activity, 54));
        box.addView(preview, new LinearLayout.LayoutParams(-1, dp(activity, 54)));

        TextView hint = text(activity,
                "رنگ را آزادانه تنظیم کنید؛ هیچ پالت ازپیش‌تعیین‌شده‌ای محدودتان نمی‌کند.",
                12.5f, 0xff5f7480, false);
        hint.setGravity(Gravity.CENTER);
        box.addView(hint);

        SeekBar hue = slider(activity, 360, Math.round(hsv[0]));
        SeekBar sat = slider(activity, 100, Math.round(hsv[1] * 100f));
        SeekBar val = slider(activity, 100, Math.round(hsv[2] * 100f));
        addSliderRow(activity, box, "ته‌رنگ", hue);
        addSliderRow(activity, box, "اشباع", sat);
        addSliderRow(activity, box, "روشنایی", val);

        Runnable paint = () -> {
            float[] current = {
                    hue.getProgress(),
                    sat.getProgress() / 100f,
                    Math.max(0.08f, val.getProgress() / 100f)
            };
            chosen[0] = Color.HSVToColor(current);
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{shade(chosen[0], .58f), chosen[0], shade(chosen[0], .82f)});
            bg.setCornerRadius(dp(activity, 14));
            preview.setBackground(bg);
            preview.setTextColor(ShiftVisuals.contrast(chosen[0]));
            preview.setText(String.format("رنگ سفارشی  #%06X", chosen[0] & 0xffffff));
        };
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) { paint.run(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
        hue.setOnSeekBarChangeListener(listener);
        sat.setOnSeekBarChangeListener(listener);
        val.setOnSeekBarChangeListener(listener);
        paint.run();

        new AlertDialog.Builder(activity)
                .setTitle("رنگ سفارشی ویجت")
                .setView(box)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("اعمال", (dialog, which) -> {
                    board.edit()
                            .putInt(BOARD_THEME, CUSTOM_THEME)
                            .putInt(BOARD_CUSTOM_COLOR, chosen[0])
                            .apply();
                    repaintThemeButtons(activity, buttons, CUSTOM_THEME, board);
                    WidgetStudio.refresh(activity);
                })
                .show();
    }

    private static SeekBar slider(Activity activity, int max, int value) {
        SeekBar bar = new SeekBar(activity);
        bar.setMax(max);
        bar.setProgress(value);
        bar.setMinimumHeight(dp(activity, 36));
        return bar;
    }

    private static void addSliderRow(
            Activity activity, LinearLayout parent, String label, SeekBar bar) {
        TextView title = text(activity, label, 13f, 0xff203a63, true);
        title.setPadding(dp(activity, 2), dp(activity, 5), dp(activity, 2), 0);
        parent.addView(title);
        parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(activity, 42)));
    }

    private static int shade(int color, float valueFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0.08f, Math.min(1f, hsv[2] * valueFactor));
        return Color.HSVToColor(hsv);
    }
    public static void build(Activity activity, LinearLayout root) {
        root.removeAllViews();

        TextView title = text(
                activity, "ELUQEN Shift Board", 21f, 0xff163d4a, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView note = text(
                activity,
                "ویجت برنامه فقط در اندازه ۴×۱ ارائه می‌شود.",
                13f, 0xff5f7480, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note);

        TextView colorTitle = text(
                activity, "رنگ ویجت", 15f, 0xff203a63, true);
        colorTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        root.addView(colorTitle);

        TextView colorHint = text(
                activity,
                "تغییر رنگ بلافاصله روی ویجت اعمال می‌شود.",
                12.5f, 0xff5f7480, false);
        colorHint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        root.addView(colorHint);

        SharedPreferences board = activity.getSharedPreferences(BOARD_PREFS, 0);
        int savedTheme = Math.max(0, Math.min(CUSTOM_THEME,
                board.getInt(BOARD_THEME, 0)));
        Button[] themeButtons = new Button[THEME_NAMES.length];
        GridLayout themes = new GridLayout(activity);
        themes.setColumnCount(3);
        themes.setRowCount(2);
        themes.setUseDefaultMargins(false);
        themes.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        themes.setPadding(dp(activity, 8), dp(activity, 4),
                dp(activity, 8), dp(activity, 8));

        for (int i = 0; i < THEME_NAMES.length; i++) {
            final int index = i;
            Button choice = new Button(activity);
            themeButtons[i] = choice;
            styleThemeButton(activity, choice, i, i == savedTheme, board);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(activity, 48);
            lp.columnSpec = GridLayout.spec(i % 3, 1f);
            lp.rowSpec = GridLayout.spec(i / 3);
            lp.setMargins(dp(activity, 3), dp(activity, 3),
                    dp(activity, 3), dp(activity, 3));
            themes.addView(choice, lp);

            choice.setOnClickListener(v -> {
                if (index == CUSTOM_THEME) {
                    showCustomColorPicker(activity, board, themeButtons);
                    return;
                }
                board.edit().putInt(BOARD_THEME, index).apply();
                repaintThemeButtons(activity, themeButtons, index, board);
                WidgetStudio.refresh(activity);
            });
        }
        root.addView(themes, new LinearLayout.LayoutParams(-1, -2));
        TextView status = text(
                activity,
                "انتخاب فعلی ذخیره می‌شود و برای همه نمونه‌های ویجت اعمال خواهد شد.",
                12.5f, 0xff50717a, false);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        Button add = button(activity, "افزودن ویجت ۴×۱");
        LinearLayout.LayoutParams addLp =
                new LinearLayout.LayoutParams(-1, dp(activity, 54));
        addLp.setMargins(
                dp(activity, 8), dp(activity, 10),
                dp(activity, 8), dp(activity, 8));
        root.addView(add, addLp);

        add.setOnClickListener(v ->
                WidgetSizePicker.pin(activity, "single"));
    }
}
