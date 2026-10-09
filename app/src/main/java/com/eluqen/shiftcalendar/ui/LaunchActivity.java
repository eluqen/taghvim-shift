package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import android.view.Gravity;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.reminder.ReminderNotifications;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.update.AppUpdateManager;
import com.eluqen.shiftcalendar.update.UpdateSource;
import java.util.List;
import com.eluqen.shiftcalendar.update.DataUpdateJobService;

public final class LaunchActivity extends Activity {
    private static final int REQUEST_PREVIEW_WARMUP = 7310;
    private Class<?> pendingTarget;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        AppTheme.apply(this);
        Prefs.ensureInitialInstallSource(this);
        ReminderNotifications.ensureChannels(this);
        DataUpdateJobService.syncSchedule(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(LaunchActivity.createRunCallback0(this),
                    "eluqen-shift-calendar-launch-reminder").start();
        }

        pendingTarget = targetActivity();
        continueLaunch();
    }



    private void continueLaunch() {
        if (pendingTarget == FirstRunActivity.class) {
            openTarget();
            return;
        }

        if (MainPageSwipe.previewsReady(this)) {
            openTarget();
            return;
        }

        showWarmupSurface();
        RootPreviewWarmupActivity.startForResult(this, REQUEST_PREVIEW_WARMUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PREVIEW_WARMUP) {
            openTarget();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showWarmupSurface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.SURFACE);
        TextView brand = new TextView(this);
        brand.setText("ELUQEN");
        brand.setTextColor(Ui.PRIMARY);
        brand.setTextSize(18f);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private void openTarget() {
        Class<?> target = pendingTarget != null ? pendingTarget : targetActivity();
        Intent intent = new Intent(this, target);
        if (target == MainActivity.class) {
            intent.putExtra(MainActivity.EXTRA_SKIP_DEFAULT, true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    void handleOnCreateCallback0() {
        ReminderScheduler.sync(this);
    }

    private Class<?> targetActivity() {
        if (!Prefs.onboardingComplete(this)) {
            return FirstRunActivity.class;
        }
        String strDefaultScreen = Prefs.defaultScreen(this);
        if (Prefs.SCREEN_FEATURES.equals(strDefaultScreen)) {
            return FeatureHubActivity.class;
        }
        if (Prefs.SCREEN_FOUR_SHIFT.equals(strDefaultScreen)) {
            return FourShiftActivity.class;
        }
        if (Prefs.SCREEN_CARDEX.equals(strDefaultScreen)) {
            return CardexActivity.class;
        }
        return "settings".equals(strDefaultScreen) ? GeneralSettingsActivity.class : MainActivity.class;
    }

    private static Runnable createRunCallback0(final LaunchActivity capture0) {
        return () -> {
        capture0.handleOnCreateCallback0();
        };
    }
}
