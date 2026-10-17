package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class Ui {
    public static int PRIMARY = 0xff203a63;
    public static int PRIMARY_DARK = 0xff122541;
    public static int ACCENT = 0xffffb457;
    public static int SECONDARY = 0xff108b86;
    public static int SURFACE = 0xfff5f8fc;
    public static int SURFACE_ALT = 0xffedf3fb;
    public static int CARD = 0xffffffff;
    public static int TEXT = 0xff182b45;
    public static int MUTED = 0xff63758d;
    public static int BORDER = 0xffdce5ed;
    public static final int HOLIDAY = 0xffc84a54;

    private Ui() {
    }

    public static int dp(Context context, float f) {
        return Math.round(f * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String str, float f, int i) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTextColor(i);
        textView.setGravity(21);
        textView.setTextDirection(4);
        return textView;
    }

    /** Consistent right-aligned Persian heading for Android AlertDialogs. */
    public static TextView rtlDialogTitle(Context context, String title) {
        TextView heading = text(context, title, 18f, TEXT);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        heading.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(context,24),dp(context,17),
                dp(context,24),dp(context,10));
        return heading;
    }

    public static TextView rtlDialogMessage(Context context, String body) {
        TextView message = text(context, body, 15f, TEXT);
        message.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.TOP);
        message.setLineSpacing(dp(context,3),1.1f);
        message.setPadding(dp(context,24),dp(context,8),
                dp(context,24),dp(context,20));
        return message;
    }

    public static TextView title(Context context, String str, float f) {
        TextView textViewText = text(context, str, f, TEXT);
        textViewText.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return textViewText;
    }

    public static TextView eyebrow(Context context, String str) {
        TextView textViewText = text(context, str, 12.0f, SECONDARY);
        textViewText.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return textViewText;
    }

    public static Button button(Context context, String str) {
        Button button = new Button(context);
        button.setText(str);
        button.setTextSize(14.0f);
        button.setTextColor(-1);
        button.setAllCaps(false);
        button.setGravity(17);
        int i = PRIMARY;
        button.setBackground(rounded(i, 16, context));
        button.setBackgroundTintList(ColorStateList.valueOf(i));
        button.setMinHeight(dp(context, 48.0f));
        DesignPolish.button(button, false);
        return button;
    }

    public static Button tonalButton(Context context, String str) {
        Button button = button(context, str);
        button.setTextColor(PRIMARY);
        int i = SURFACE_ALT;
        button.setBackground(rounded(i, 16, context));
        button.setBackgroundTintList(ColorStateList.valueOf(i));
        DesignPolish.button(button, true);
        return button;
    }

    public static EditText input(Context context, String str) {
        EditText editText = new EditText(context);
        editText.setHint(str);
        editText.setTextSize(16.0f);
        editText.setTextColor(TEXT);
        editText.setHintTextColor(Color.rgb(132, 146, 159));
        editText.setGravity(5);
        editText.setTextDirection(4);
        editText.setSingleLine(true);
        editText.setPadding(dp(context, 14.0f), dp(context, 10.0f), dp(context, 14.0f), dp(context, 10.0f));
        editText.setBackground(roundedStroke(CARD, BORDER, 12, context));
        return editText;
    }

    public static LinearLayout toolbar(Activity activity, String str, boolean z) {
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(activity, 8.0f), dp(activity, 4.0f), dp(activity, 12.0f), dp(activity, 4.0f));
        linearLayout.setBackgroundColor(PRIMARY);
        linearLayout.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        TextView textViewText = text(activity, str, 20.0f, -1);
        textViewText.setTypeface(Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        linearLayout.addView(textViewText, new LinearLayout.LayoutParams(0, dp(activity, 56.0f), 1.0f));
        if (z) {
            Button button = new Button(activity);
            button.setText("‹");
            button.setTextSize(30.0f);
            button.setTextColor(-1);
            button.setBackgroundColor(0);
            button.setOnClickListener(Ui.createOnClickCallback0(activity));
            linearLayout.addView(button, new LinearLayout.LayoutParams(dp(activity, 52.0f), dp(activity, 52.0f)));
        }
        DesignPolish.toolbar(linearLayout);
        HeaderPolish.trim(linearLayout);
        return linearLayout;
    }

    static void handleToolbarCallback0(Activity activity, View view) {
        activity.finish();
    }

    public static LinearLayout card(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(context, 16.0f), dp(context, 14.0f), dp(context, 16.0f), dp(context, 14.0f));
        linearLayout.setBackground(rounded(CARD, 20, context));
        linearLayout.setElevation(dp(context, 1.5f));
        DesignPolish.card(linearLayout);
        return linearLayout;
    }

    public static GradientDrawable rounded(int i, int i2, Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, i2));
        return gradientDrawable;
    }

    public static GradientDrawable roundedStroke(int i, int i2, int i3, Context context) {
        GradientDrawable gradientDrawableRounded = rounded(i, i3, context);
        gradientDrawableRounded.setStroke(dp(context, 1.0f), i2);
        return gradientDrawableRounded;
    }

    public static LinearLayout.LayoutParams margins(Context context, int i, int i2, int i3, int i4, int i5, int i6) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i, i2);
        layoutParams.setMargins(dp(context, i3), dp(context, i4), dp(context, i5), dp(context, i6));
        return layoutParams;
    }

    public static void toast(Context context, String str) {
        Toast.makeText(context, str, android.widget.Toast.LENGTH_LONG).show();
    }

    public static void disableParentClipping(View view) {
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if (viewGroup != null) {
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
        }
    }

    private static View.OnClickListener createOnClickCallback0(final Activity capture0) {
        return (view) -> {
        Ui.handleToolbarCallback0(capture0, view);
        };
    }
}
