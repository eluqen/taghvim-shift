package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.WeakHashMap;

/**
 * Interactive pager for the five root pages.
 *
 * The LIVE current page tracks ACTION_MOVE exactly. Adjacent root pages are
 * always exact screenshots captured from their real canonical entry state.
 * RootPreviewWarmupActivity prepares all five before the first user swipe.
 * BackDock stays fixed and only its indicator follows swipe progress.
 */
final class MainPageSwipe {
    private static final WeakHashMap<Activity, Session> SESSIONS = new WeakHashMap<>();
    private static final Bitmap[] PAGE_CACHE = new Bitmap[5];
    static final String EXTRA_PREVIEW_WARMUP = "eluqen_root_preview_warmup";
    private static final String PREVIEW_DIR = "root-page-previews-v21";

    private static final class Session {
        float x, y, dx;
        int direction, width, height, fromTab, targetTab;
        boolean active, blocked, previewMissing;
        FrameLayout viewport;
        View targetView;
        View content;
    }

    private MainPageSwipe() {}

    static void dropCachedPreviews(android.content.Context context) {
        for (int i = 0; i < PAGE_CACHE.length; i++) {
            Bitmap b = PAGE_CACHE[i];
            PAGE_CACHE[i] = null;
            if (b != null && !b.isRecycled()) b.recycle();
        }
        // Disk previews are theme-keyed. Keep them so switching back to a
        // previously used theme can instantly reuse its exact real page UI.
    }

    static boolean isWarmup(Activity activity) {
        return activity != null && activity.getIntent() != null
                && activity.getIntent().getBooleanExtra(EXTRA_PREVIEW_WARMUP, false);
    }

    private static File previewFile(android.content.Context context, int tab) {
        String theme = AppTheme.currentKey(context);
        return new File(new File(context.getCacheDir(), PREVIEW_DIR),
                "tab-" + tab + "-" + theme + ".png");
    }

    static boolean previewReady(android.content.Context context, int tab) {
        if (tab < 0 || tab >= PAGE_CACHE.length) return false;
        Bitmap memory = PAGE_CACHE[tab];
        if (memory != null && !memory.isRecycled()
                && memory.getWidth() >= 32 && memory.getHeight() >= 32) {
            return true;
        }
        File file = previewFile(context, tab);
        if (!file.isFile() || file.length() < 1024) return false;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            return bounds.outWidth >= 32 && bounds.outHeight >= 32;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean previewsReady(android.content.Context context) {
        for (int tab = 0; tab < PAGE_CACHE.length; tab++) {
            if (!previewReady(context, tab)) return false;
        }
        return true;
    }

    static int firstMissingPreview(android.content.Context context) {
        for (int tab = 0; tab < PAGE_CACHE.length; tab++) {
            if (!previewReady(context, tab)) return tab;
        }
        return -1;
    }

    static void captureWarmupAndFinish(Activity activity) {
        captureWarmupAndFinish(activity, 0);
    }

    private static void captureWarmupAndFinish(Activity activity, int attempt) {
        int tab = BackDock.activeTab(activity);
        View content = BackDock.pageContent(activity);
        if (tab < 0 || content == null || content.getWidth() < 32 || content.getHeight() < 32) {
            if (attempt < 8) {
                activity.getWindow().getDecorView().postDelayed(
                        () -> captureWarmupAndFinish(activity, attempt + 1), 35);
                return;
            }
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
            activity.overridePendingTransition(0, 0);
            return;
        }
        try {
            Bitmap fresh = bitmap(content);
            Bitmap old = PAGE_CACHE[tab];
            PAGE_CACHE[tab] = fresh;
            if (old != null && old != fresh && !old.isRecycled()) old.recycle();
            persistPreview(activity, tab, fresh);
            activity.setResult(Activity.RESULT_OK);
        } catch (Throwable error) {
            android.util.Log.e("ELUQEN_PAGE_SWIPE", "Warmup preview failed", error);
            activity.setResult(Activity.RESULT_CANCELED);
        }
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    static boolean handle(Activity activity, MotionEvent event, View excluded) {
        if (isWarmup(activity) || BackDock.activeTab(activity) < 0) return false;
        try {
            int action = event.getActionMasked();
            Session s = SESSIONS.get(activity);

            if (action == MotionEvent.ACTION_DOWN) {
                if (s != null && s.active) return true;
                if (inside(excluded, event.getRawX(), event.getRawY())
                        || BackDock.inBottomNav(activity, event.getRawX(), event.getRawY())) {
                    SESSIONS.remove(activity);
                    return false;
                }
                FrameLayout viewport = BackDock.pageViewport(activity);
                View content = BackDock.pageContent(activity);
                if (viewport == null || content == null
                        || viewport.getWidth() < 32 || viewport.getHeight() < 32) return false;
                s = new Session();
                s.x = event.getRawX();
                s.y = event.getRawY();
                s.viewport = viewport;
                s.content = content;
                s.fromTab = BackDock.activeTab(activity);
                s.width = viewport.getWidth();
                s.height = viewport.getHeight();
                SESSIONS.put(activity, s);
                return false;
            }

            if (s == null) return false;

            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) {
                if (s.active) {
                    if (s.previewMissing) cleanup(activity, s, true);
                    else settle(activity, s, false);
                } else SESSIONS.remove(activity);
                return s.active;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - s.x;
                float dy = event.getRawY() - s.y;
                float slop = 12f * activity.getResources().getDisplayMetrics().density;

                if (!s.active && !s.blocked) {
                    if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx) * 1.15f) {
                        s.blocked = true;
                        return false;
                    }
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                        s.direction = dx < 0 ? -1 : 1;
                        s.targetTab = BackDock.targetTab(activity, s.direction);
                        if (s.targetTab < 0) {
                            SESSIONS.remove(activity);
                            return false;
                        }
                        cancelChild(s.content, event);
                        if (!begin(activity, s)) {
                            // A missing snapshot must never disable navigation.
                            // The destination will generate its real preview
                            // after entering; do not fabricate a placeholder.
                            s.previewMissing = true;
                            s.active = true;
                            BackDock.previewSwipe(activity, s.fromTab, s.targetTab, 0f);
                        }
                    }
                }

                if (s.active) {
                    if (dx * s.direction < 0) dx = 0;
                    if (s.previewMissing) {
                        BackDock.previewSwipe(activity, s.fromTab, s.targetTab,
                                Math.min(1f, Math.abs(dx) / Math.max(1f, s.width)));
                    } else {
                        move(activity, s, dx);
                    }
                    return true;
                }
                return false;
            }

            if (action == MotionEvent.ACTION_UP) {
                if (!s.active) {
                    SESSIONS.remove(activity);
                    return false;
                }
                float dx = event.getRawX() - s.x;
                boolean commit = Math.abs(dx) > s.width * .25f;
                if (s.previewMissing) {
                    int direction = s.direction;
                    cleanup(activity, s, true);
                    if (commit) BackDock.navigateRelative(activity, direction);
                } else {
                    settle(activity, s, commit);
                }
                return true;
            }
        } catch (Throwable error) {
            android.util.Log.e("ELUQEN_PAGE_SWIPE", "Root-page swipe failed", error);
            Session s = SESSIONS.get(activity);
            if (s != null) cleanup(activity, s, true);
        }
        return false;
    }

    static void captureSoon(Activity activity) {
        if (isWarmup(activity) || BackDock.activeTab(activity) < 0) return;
        View content = BackDock.pageContent(activity);
        if (content == null) return;
        // Entry positioning uses OnPreDraw. Capture only after that has settled,
        // and never again from later arbitrary user scroll positions.
        content.postDelayed(() -> captureCurrent(activity, false), 90);
        content.postDelayed(() -> captureCurrent(activity, true), 260);
    }

    private static void captureCurrent(Activity activity, boolean persist) {
        int tab = BackDock.activeTab(activity);
        View content = BackDock.pageContent(activity);
        if (tab < 0 || tab >= PAGE_CACHE.length || content == null
                || content.getWidth() < 32 || content.getHeight() < 32
                || Math.abs(content.getTranslationX()) > .5f) return;
        try {
            Bitmap fresh = bitmap(content);
            Bitmap old = PAGE_CACHE[tab];
            PAGE_CACHE[tab] = fresh;
            if (old != null && old != fresh && !old.isRecycled()) old.recycle();
            if (persist) persistPreview(activity, tab, fresh);
        } catch (Throwable error) {
            android.util.Log.w("ELUQEN_PAGE_SWIPE", "Preview cache skipped", error);
        }
    }

    private static void persistPreview(Activity activity, int tab, Bitmap source) {
        try {
            Bitmap copy = source.copy(Bitmap.Config.ARGB_8888, false);
            File out = previewFile(activity, tab);
            File dir = out.getParentFile();
            new Thread(() -> {
                try {
                    if (!dir.exists() && !dir.mkdirs()) return;
                    try (FileOutputStream stream = new FileOutputStream(out)) {
                        copy.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    }
                } catch (Throwable error) {
                    android.util.Log.w("ELUQEN_PAGE_SWIPE", "Persist preview skipped", error);
                } finally {
                    if (!copy.isRecycled()) copy.recycle();
                }
            }, "root-preview-" + tab).start();
        } catch (Throwable error) {
            android.util.Log.w("ELUQEN_PAGE_SWIPE", "Preview copy skipped", error);
        }
    }

    private static Bitmap cachedPreview(Activity activity, int tab) {
        if (tab < 0 || tab >= PAGE_CACHE.length) return null;
        Bitmap memory = PAGE_CACHE[tab];
        if (memory != null && !memory.isRecycled()) return memory;
        File file = previewFile(activity, tab);
        if (!file.isFile()) return null;
        try {
            Bitmap loaded = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (loaded != null) PAGE_CACHE[tab] = loaded;
            return loaded;
        } catch (Throwable error) {
            return null;
        }
    }

    static void abort(Activity activity) {
        Session s = SESSIONS.get(activity);
        if (s != null) cleanup(activity, s, true);
    }

    private static boolean begin(Activity activity, Session s) {
        Bitmap cached = cachedPreview(activity, s.targetTab);
        if (cached == null) {
            android.util.Log.e("ELUQEN_PAGE_SWIPE",
                    "Missing prewarmed root preview for tab " + s.targetTab);
            return false;
        }
        ImageView image = new ImageView(activity);
        image.setImageBitmap(cached);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        image.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        s.targetView = image;

        s.targetView.setTranslationX(-s.direction * s.width);
        s.viewport.addView(s.targetView, 0);
        s.content.bringToFront();
        s.content.setTranslationX(0f);
        s.active = true;
        BackDock.previewSwipe(activity, s.fromTab, s.targetTab, 0f);
        return true;
    }

    private static void move(Activity activity, Session s, float dx) {
        s.dx = Math.max(-s.width, Math.min(s.width, dx));
        s.content.setTranslationX(s.dx);
        s.targetView.setTranslationX(s.dx - s.direction * s.width);
        BackDock.previewSwipe(activity, s.fromTab, s.targetTab,
                Math.min(1f, Math.abs(s.dx) / Math.max(1f, s.width)));
    }

    private static void settle(Activity activity, Session s, boolean commit) {
        float target = commit ? s.direction * s.width : 0f;
        float next = commit ? 0f : -s.direction * s.width;
        long duration = Math.min(210, Math.max(80,
                (long) (170f * Math.abs(target - s.dx) / Math.max(1, s.width))));
        BackDock.settleSwipe(activity, s.fromTab, s.targetTab, commit, duration);

        s.content.animate().cancel();
        s.targetView.animate().cancel();

        s.content.animate().translationX(target).setDuration(duration)
                .setInterpolator(new DecelerateInterpolator()).start();
        s.targetView.animate().translationX(next).setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (commit) {
                        BackDock.navigateRelative(activity, s.direction);
                        activity.getWindow().getDecorView().postDelayed(
                                () -> cleanup(activity, s, false), 90);
                    } else {
                        cleanup(activity, s, false);
                    }
                }).start();
    }

    private static void cleanup(Activity activity, Session s, boolean resetNav) {
        try {
            if (s.content != null) {
                s.content.animate().cancel();
                s.content.setTranslationX(0f);
            }
            if (s.targetView != null) {
                s.targetView.animate().cancel();
                if (s.targetView instanceof ImageView) {
                    ((ImageView) s.targetView).setImageDrawable(null);
                }
                if (s.viewport != null && s.targetView.getParent() == s.viewport) {
                    s.viewport.removeView(s.targetView);
                }
            }
        } finally {
            SESSIONS.remove(activity);
            if (resetNav) BackDock.resetSwipePreview(activity);
        }
    }

    private static void cancelChild(View content, MotionEvent source) {
        MotionEvent cancel = MotionEvent.obtain(source);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        content.dispatchTouchEvent(cancel);
        cancel.recycle();
    }

    private static Bitmap bitmap(View view) {
        Bitmap b = Bitmap.createBitmap(
                Math.max(1, view.getWidth()), Math.max(1, view.getHeight()),
                Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(b));
        return b;
    }

    private static int dp(Activity a, float value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private static boolean inside(View view, float rawX, float rawY) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) return false;
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        return rawX >= pos[0] && rawX < pos[0] + view.getWidth()
                && rawY >= pos[1] && rawY < pos[1] + view.getHeight();
    }
}
