package com.eluqen.shiftcalendar.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Settings host for the single authoritative ELUQEN 4x1 board widget. */
public final class WidgetSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "تنظیمات ویجت", true));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(
                Ui.dp(this, 14), Ui.dp(this, 14),
                Ui.dp(this, 14), Ui.dp(this, 28));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        WidgetEditor.build(this, body);
        setContentView(root);
    }
}
