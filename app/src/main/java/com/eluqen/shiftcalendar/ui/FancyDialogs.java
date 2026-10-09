package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.NumberPicker;
import java.util.Arrays;
import java.util.List;

/**
 * Shared visual adapters for app-owned AlertDialog choice lists.
 * Behavior/callback ownership stays with each caller; this class only
 * standardizes typography, spacing, RTL alignment and theme-aware surfaces.
 */
public final class FancyDialogs {
    private FancyDialogs() {}

    public static ArrayAdapter<String> list(Context context, String[] items) {
        return new ChoiceAdapter(context, items, false);
    }

    public static ArrayAdapter<String> single(Context context, String[] items) {
        return new ChoiceAdapter(context, items, true);
    }

    public static ArrayAdapter<String> spinner(Context context, String[] items) {
        return spinner(context, Arrays.asList(items));
    }

    public static ArrayAdapter<String> spinner(Context context, List<String> items) {
        return new SpinnerAdapter(context, items);
    }

    public static void styleWheel(Context context, NumberPicker picker) {
        picker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        picker.setBackground(Ui.roundedStroke(
                Ui.SURFACE_ALT, Ui.BORDER, 14, context));
        picker.setPadding(
                Ui.dp(context, 4), Ui.dp(context, 4),
                Ui.dp(context, 4), Ui.dp(context, 4));
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            picker.setTextColor(Ui.TEXT);
            picker.setTextSize(Ui.dp(context, 18));
            picker.setSelectionDividerHeight(Ui.dp(context, 2));
        }
    }

    private static final class ChoiceAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final boolean single;

        ChoiceAdapter(Context context, String[] items, boolean single) {
            super(context,
                    single
                            ? android.R.layout.simple_list_item_single_choice
                            : android.R.layout.simple_list_item_1,
                    items);
            this.context = context;
            this.single = single;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            style(view);
            return view;
        }

        private void style(View view) {
            if (!(view instanceof TextView)) return;
            TextView text = (TextView) view;
            text.setTextSize(15.5f);
            text.setTextColor(Ui.TEXT);
            text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            text.setTextDirection(View.TEXT_DIRECTION_RTL);
            text.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            text.setMinHeight(Ui.dp(context, 54));
            text.setPadding(
                    Ui.dp(context, single ? 18 : 16),
                    Ui.dp(context, 7),
                    Ui.dp(context, 16),
                    Ui.dp(context, 7));
            text.setBackground(Ui.roundedStroke(
                    Ui.CARD, Ui.BORDER, 12, context));
            if (text instanceof CheckedTextView) {
                ((CheckedTextView) text).setCheckMarkTintList(
                        android.content.res.ColorStateList.valueOf(Ui.SECONDARY));
            }
        }
    }
    private static final class SpinnerAdapter extends ArrayAdapter<String> {
        private final Context context;

        SpinnerAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_spinner_item, items);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convertView, parent);
            style(text, false);
            return text;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getDropDownView(position, convertView, parent);
            style(text, true);
            return text;
        }

        private void style(TextView text, boolean dropdown) {
            text.setTextSize(dropdown ? 15.5f : 15f);
            text.setTextColor(Ui.TEXT);
            text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            text.setTextDirection(View.TEXT_DIRECTION_RTL);
            text.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            text.setMinHeight(Ui.dp(context, dropdown ? 52 : 48));
            text.setPadding(
                    Ui.dp(context, 14), Ui.dp(context, 6),
                    Ui.dp(context, 14), Ui.dp(context, 6));
            text.setBackground(Ui.roundedStroke(
                    dropdown ? Ui.CARD : Ui.SURFACE_ALT,
                    dropdown ? Ui.BORDER : Ui.SECONDARY,
                    12, context));
        }
    }

}
