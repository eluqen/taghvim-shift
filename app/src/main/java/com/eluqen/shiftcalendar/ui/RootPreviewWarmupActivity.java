package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;

/**
 * Pre-renders the five root pages invisibly so MainPageSwipe always has an
 * exact real-page preview before the user can perform the first swipe.
 */
public final class RootPreviewWarmupActivity extends Activity {
    private static final int REQUEST_ROOT = 7301;
    private int launchCount;

    public static void startDetached(Activity caller) {
        Intent intent = new Intent(caller, RootPreviewWarmupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        caller.startActivity(intent);
        caller.overridePendingTransition(0, 0);
    }

    public static void startForResult(Activity caller, int requestCode) {
        Intent intent = new Intent(caller, RootPreviewWarmupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        caller.startActivityForResult(intent, requestCode);
        caller.overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(Bundle state) {
        setTheme(com.eluqen.shiftcalendar.R.style.PreviewWarmupTheme);
        super.onCreate(state);
        AppTheme.apply(this);
        // This Activity has no content and uses a translucent theme, so the
        // Launch/Theme screen remains visible underneath while it orchestrates.
        getWindow().getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        startNext();
    }

    private void startNext() {
        int tab = MainPageSwipe.firstMissingPreview(this);
        if (tab < 0 || launchCount >= 7) {
            setResult(RESULT_OK);
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        launchCount++;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                this, getPackageName() + rootAlias(tab)));
        intent.putExtra(MainPageSwipe.EXTRA_PREVIEW_WARMUP, true);
        intent.putExtra(BackDock.EXTRA_ROOT_ENTRY, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (tab == 0) intent.putExtra(MainActivity.EXTRA_SKIP_DEFAULT, true);
        if (tab == 3) intent.putExtra(GeneralSettingsActivity.EXTRA_RESET_SCROLL, true);
        startActivityForResult(intent, REQUEST_ROOT);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ROOT) {
            startNext();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static String rootAlias(int tab) {
        switch (tab) {
            case 0: return ".ui.WarmupCalendarAlias";
            case 1: return ".ui.WarmupFourShiftAlias";
            case 2: return ".ui.WarmupCardexAlias";
            case 3: return ".ui.WarmupSettingsAlias";
            default: return ".ui.WarmupFeaturesAlias";
        }
    }
}
