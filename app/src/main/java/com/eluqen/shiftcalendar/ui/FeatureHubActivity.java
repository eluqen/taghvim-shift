package com.eluqen.shiftcalendar.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;

public final class FeatureHubActivity extends BaseActivity {
    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.addView(Ui.toolbar(this, "همه امکانات", true));
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(android.widget.LinearLayout.VERTICAL);
        scrollView.addView(linearLayout2);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        HubSections.hub(this, linearLayout);
        setContentView(linearLayout);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (HubGeneral.result(this, i, i2, intent) || ToolActions.result(this, i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ReminderPolish.hub(this);
        if (!MainPageSwipe.isWarmup(this) && Prefs.reminderEnabled(this)) {
            new Thread(FeatureHubActivity.createRunCallback0(this), "eluqen-shift-calendar-hub-reminder").start();
        }
        ScrollTopReset.top(this);
    }

    void handleOnResumeCallback0() {
        ReminderScheduler.sync(this);
    }

    private static Runnable createRunCallback0(final FeatureHubActivity capture0) {
        return () -> {
        capture0.handleOnResumeCallback0();
        };
    }
}
