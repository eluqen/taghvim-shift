package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public abstract class BaseActivity extends Activity {
    private String appliedThemeKey;
    private boolean rootEntryPreviewScheduled;
    private boolean previewWarmup;
    private boolean warmupCaptureScheduled;

    protected View mainPageSwipeExcludedView() {
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!previewWarmup && MainPageSwipe.handle(this, event, mainPageSwipeExcludedView())) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
    @Override
    public void onBackPressed() {
        if (previewWarmup) {
            finish();
            return;
        }
        if (BackDock.handleBack(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentTheme = AppTheme.currentKey(this);
        if (appliedThemeKey != null && !appliedThemeKey.equals(currentTheme)) {
            AppTheme.apply(this);
            appliedThemeKey = currentTheme;
        }
        if (previewWarmup) {
            if (!warmupCaptureScheduled) {
                warmupCaptureScheduled = true;
                // Subclass onResume continues immediately after super.onResume()
                // and applies its canonical entry scroll. Capture shortly after;
                // captureWarmupAndFinish() also retries if layout is not ready.
                getWindow().getDecorView().postDelayed(
                        () -> MainPageSwipe.captureWarmupAndFinish(this), 140);
            }
            return;
        }
        BackDock.refresh(this);
        if (!rootEntryPreviewScheduled && BackDock.activeTab(this) >= 0) {
            rootEntryPreviewScheduled = true;
            MainPageSwipe.captureSoon(this);
        }
    }

    @Override
    protected void onDestroy() {
        MainPageSwipe.abort(this);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        previewWarmup = MainPageSwipe.isWarmup(this);
        if (previewWarmup) {
            setTheme(com.eluqen.shiftcalendar.R.style.PreviewWarmupTheme);
        }
        super.onCreate(bundle);
        AppTheme.apply(this);
        appliedThemeKey = AppTheme.currentKey(this);
        if (previewWarmup) {
            android.view.WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.alpha = 0f;
            getWindow().setAttributes(attrs);
        }
        getWindow().getDecorView().setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        if (Build.VERSION.SDK_INT >= 35) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsAppearance(android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            getWindow().setStatusBarColor(Ui.PRIMARY_DARK);
            getWindow().setNavigationBarColor(Ui.SURFACE);
            if (Build.VERSION.SDK_INT >= 26) {
                getWindow().getDecorView().setSystemUiVisibility(16);
            }
        }
        if (!previewWarmup) BackDock.registerSystemBack(this);
    }

    @Override
    public void setContentView(View view) {
        if (Build.VERSION.SDK_INT >= 35) {
            view.setOnApplyWindowInsetsListener(BaseActivity.createOnApplyWindowInsetsCallback0(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom()));
        }
        super.setContentView(view);
        if (Build.VERSION.SDK_INT >= 35) {
            view.requestApplyInsets();
        }
        BackDock.install(this, view);
    }

    @android.annotation.TargetApi(35)
    static WindowInsets handleSetContentViewCallback0(int i, int i2, int i3, int i4, View view, WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
        view.setPadding(i + insets.left, i2 + insets.top, i3 + insets.right, i4 + insets.bottom);
        return windowInsets;
    }

    @android.annotation.TargetApi(35)
    private static View.OnApplyWindowInsetsListener createOnApplyWindowInsetsCallback0(final int capture0, final int capture1, final int capture2, final int capture3) {
        return (view, windowInsets) -> {
        return BaseActivity.handleSetContentViewCallback0(capture0, capture1, capture2, capture3, view, windowInsets);
        };
    }
}
