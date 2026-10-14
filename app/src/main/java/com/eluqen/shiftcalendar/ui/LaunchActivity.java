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
        UpdateSource launchSource = AppUpdateManager.selectedSource(this);
        AppUpdateManager.maybeCheckDirectInBackground(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(LaunchActivity.createRunCallback0(this),
                    "eluqen-shift-calendar-launch-reminder").start();
        }

        pendingTarget = targetActivity();
        if (AppUpdateManager.isMarketPolicyRestricted(this) && launchSource == UpdateSource.ASK) {
            askMarketSourceOnce();
            return;
        }
        continueLaunch();
    }

    private void askMarketSourceOnce() {
        List<UpdateSource> choices = AppUpdateManager.selectableSources(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 18));
        TextView heading = new TextView(this);
        heading.setText("فروشگاه دریافت به‌روزرسانی");
        heading.setTextSize(19f);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Ui.TEXT);
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, Ui.dp(this, 55)));
        AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).setCancelable(false).create();
        for (UpdateSource option : choices) {
            TextView item = new TextView(this);
            item.setText(option.label);
            item.setTextSize(17f);
            item.setTextColor(Ui.TEXT);
            item.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
            item.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            item.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Ui.SURFACE);
            bg.setCornerRadius(Ui.dp(this, 14));
            bg.setStroke(Ui.dp(this, 1), 0xffd9e3e8);
            item.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 58));
            lp.bottomMargin = Ui.dp(this, 10);
            panel.addView(item, lp);
            item.setOnClickListener(v -> {
                Prefs.setUpdateSource(this, option);
                dialog.dismiss();
                continueLaunch();
            });
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            GradientDrawable surround = new GradientDrawable();
            surround.setColor(Ui.SURFACE);
            surround.setCornerRadius(Ui.dp(this, 20));
            dialog.getWindow().setBackgroundDrawable(surround);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88f), -2);
        }
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
