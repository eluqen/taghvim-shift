package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * Touch-following month pager for the calendar grid ONLY.
 *
 * The old implementation rebuilt 42 live day cells TWICE at the instant a
 * swipe crossed touch slop. Here adjacent snapshots are prepared after the
 * screen is idle and reused during drag. Preparation suppresses peripheral
 * month details and never renders an intermediate month to a visible frame.
 * Root-page navigation and its preview cache are completely independent.
 */
public final class CalendarDrag {
    private static final WeakHashMap<Activity, Session> sessions = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Cache> caches = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Boolean> preparing = new WeakHashMap<>();
    private static final long WARMUP_DELAY_MS = 560;
    private static final long SECOND_PREVIEW_DELAY_MS = 96;

    private CalendarDrag() {}

    private static final class Cache {
        final String key;
        final int width, height;
        Bitmap nextMonth, previousMonth;
        Runnable first, second;
        Cache(String k, int w, int h) { key = k; width = w; height = h; }
    }

    private static final class Session {
        float x, y, dx;
        int direction, width, height;
        boolean active, settling, finished;
        Bitmap oldImage, newImage;
        FrameLayout layer, root;
        ImageView oldView, newView;
        GridLayout grid;
        VelocityTracker velocity;
        ValueAnimator animator;
    }

    static boolean isPreparing(Activity activity) {
        return Boolean.TRUE.equals(preparing.get(activity));
    }

    private static GridLayout grid(Activity a) throws Exception {
        Field f = a.getClass().getDeclaredField("calendarGrid");
        f.setAccessible(true);
        return (GridLayout) f.get(a);
    }

    private static Object field(Activity a, String name) throws Exception {
        Field f = a.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(a);
    }

    private static void field(Activity a, String name, Object value) throws Exception {
        Field f = a.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(a, value);
    }

    private static void month(Activity a, int delta) throws Exception {
        Method m = a.getClass().getDeclaredMethod("changeMonth", int.class);
        m.setAccessible(true);
        m.invoke(a, delta);
    }

    private static void restoreGrid(Activity a) throws Exception {
        Method m = a.getClass().getDeclaredMethod("renderMonth");
        m.setAccessible(true);
        m.invoke(a);
    }

    private static Bitmap bitmap(View v) {
        if (v.getWidth() <= 0 || v.getHeight() <= 0)
            throw new IllegalStateException("Calendar not laid out");
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        return b;
    }

    /** Render one neighbor without changing persistent selection/day-of-month. */
    private static Bitmap neighbor(Activity a, GridLayout g, int direction) throws Exception {
        if (isPreparing(a)) throw new IllegalStateException("Nested month preview");
        Object selected = field(a, "selected");
        Object displayed = field(a, "displayedMonth");
        Object[] desired = MonthStep.snapshot(a);
        preparing.put(a, true);
        Bitmap result = null;
        try {
            month(a, CalendarMonthDirection.fromDirection(direction));
            result = bitmap(g);
            return result;
        } finally {
            try {
                field(a, "selected", selected);
                field(a, "displayedMonth", displayed);
                MonthStep.restore(a, desired);
                restoreGrid(a);
            } catch (Exception error) {
                if (result != null && !result.isRecycled()) result.recycle();
                throw error;
            } finally {
                preparing.remove(a);
            }
        }
    }

    private static String key(Activity a, GridLayout g) throws Exception {
        return field(a, "selected") + "|" + field(a, "displayedMonth")
                + "|" + AppTheme.currentKey(a) + "|" + g.getWidth() + "x" + g.getHeight();
    }

    private static void recycle(Bitmap b) {
        if (b != null && !b.isRecycled()) b.recycle();
    }

    private static void removeTasks(Activity a, Cache c) {
        if (c == null || a.getWindow() == null) return;
        View decor = a.getWindow().getDecorView();
        if (c.first != null) decor.removeCallbacks(c.first);
        if (c.second != null) decor.removeCallbacks(c.second);
    }

    private static void forget(Activity a) {
        Cache c = caches.remove(a);
        if (c != null) {
            removeTasks(a, c);
            recycle(c.nextMonth);
            recycle(c.previousMonth);
            c.nextMonth = null;
            c.previousMonth = null;
        }
    }

    /**
     * Called after a real (not offscreen) month render.
     * Work is intentionally delayed beyond root-page preview capture (90/260ms).
     */
    static void onMonthRendered(Activity a) {
        if (isPreparing(a)) return;
        forget(a);
        if (MainPageSwipe.isWarmup(a) || a.isFinishing() || a.isDestroyed()) return;
        try {
            GridLayout g = grid(a);
            if (g == null) return;
            if (g.getWidth() < 32 || g.getHeight() < 32) {
                // onResume can render before the first layout. Retry ONCE
                // after layout rather than caching an unusable 0x0 grid.
                g.post(() -> {
                    if (g.getWidth() >= 32 && g.getHeight() >= 32
                            && !a.isFinishing() && !a.isDestroyed()) {
                        onMonthRendered(a);
                    }
                });
                return;
            }
            Cache c = new Cache(key(a, g), g.getWidth(), g.getHeight());
            caches.put(a, c);
            c.first = () -> warm(a, c, 1);
            a.getWindow().getDecorView().postDelayed(c.first, WARMUP_DELAY_MS);
        } catch (Exception error) {
            android.util.Log.w("ELUQEN_MONTH_DRAG", "Preview scheduling unavailable", error);
        }
    }

    private static void warm(Activity a, Cache c, int direction) {
        if (caches.get(a) != c || a.isFinishing() || a.isDestroyed()
                || !a.hasWindowFocus() || sessions.containsKey(a)) return;
        try {
            if (direction > 0 && c.nextMonth != null) return;
            if (direction < 0 && c.previousMonth != null) return;
            GridLayout g = grid(a);
            if (g == null || g.getWidth() < 32 || g.getHeight() < 32) return;
            if (!c.key.equals(key(a, g))) return;
            Bitmap rendered = neighbor(a, g, direction);
            if (caches.get(a) != c || !c.key.equals(key(a, g))) {
                recycle(rendered);
                return;
            }
            if (direction > 0) c.nextMonth = rendered;
            else c.previousMonth = rendered;
            if (direction > 0) {
                c.second = () -> warm(a, c, -1);
                a.getWindow().getDecorView().postDelayed(c.second, SECOND_PREVIEW_DELAY_MS);
            }
        } catch (Exception error) {
            android.util.Log.w("ELUQEN_MONTH_DRAG", "Neighbor preview unavailable", error);
        }
    }

    private static void rearmAfterTapOrCancel(Activity a) {
        Cache c = caches.get(a);
        if (c == null || a.isFinishing() || a.isDestroyed()
                || (c.nextMonth != null && c.previousMonth != null)) return;
        removeTasks(a, c);
        c.first = () -> warm(a, c, c.nextMonth == null ? 1 : -1);
        a.getWindow().getDecorView().postDelayed(c.first, 250);
    }

    static void onPause(Activity a) {
        Session s = sessions.get(a);
        if (s != null) {
            if (s.animator != null) s.animator.cancel();
            cleanup(a, s);
        }
        forget(a);
    }

    private static ImageView image(Activity a, Bitmap b, int w, int h) {
        ImageView v = new ImageView(a);
        v.setImageBitmap(b);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        v.setLayoutParams(new FrameLayout.LayoutParams(w, h, Gravity.LEFT | Gravity.TOP));
        return v;
    }

    private static void preview(Activity a, Session s, int direction) throws Exception {
        s.direction = direction;
        Cache c = caches.get(a);
        if (c != null) removeTasks(a, c);
        s.oldImage = bitmap(s.grid);

        Bitmap ready = null;
        if (c != null && c.width == s.width && c.height == s.height
                && c.key.equals(key(a, s.grid))) {
            ready = direction > 0 ? c.nextMonth : c.previousMonth;
            if (ready != null && ready.isRecycled()) ready = null;
            if (direction > 0) c.nextMonth = null;
            else c.previousMonth = null;
        }
        s.newImage = ready != null ? ready : neighbor(a, s.grid, direction);
        s.root = a.findViewById(android.R.id.content);
        int[] gp = new int[2], rp = new int[2];
        s.grid.getLocationOnScreen(gp);
        s.root.getLocationOnScreen(rp);
        s.layer = new FrameLayout(a);
        s.layer.setClipChildren(true);
        s.layer.setClipToPadding(true);
        FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(
                s.width, s.height, Gravity.TOP | Gravity.LEFT);
        pos.leftMargin = gp[0] - rp[0];
        pos.topMargin = gp[1] - rp[1];
        s.oldView = image(a, s.oldImage, s.width, s.height);
        s.newView = image(a, s.newImage, s.width, s.height);
        s.layer.addView(s.oldView);
        s.layer.addView(s.newView);
        s.newView.setTranslationX(-direction * s.width);
        s.grid.setAlpha(0f);
        s.root.addView(s.layer, pos);
        s.active = true;
    }

    private static void move(Session s, float dx) {
        s.dx = Math.max(-s.width, Math.min(s.width, dx));
        s.oldView.setTranslationX(s.dx);
        s.newView.setTranslationX(s.dx - s.direction * s.width);
    }

    private static void cleanup(Activity a, Session s) {
        if (s == null || s.finished) return;
        s.finished = true;
        if (s.layer != null && s.root != null) s.root.removeView(s.layer);
        if (s.grid != null) s.grid.setAlpha(1f);
        if (s.oldView != null) s.oldView.setImageDrawable(null);
        if (s.newView != null) s.newView.setImageDrawable(null);
        recycle(s.oldImage);
        recycle(s.newImage);
        if (s.velocity != null) s.velocity.recycle();
        sessions.remove(a);
    }

    /** One animator drives both images in the SAME frame, including cancellation. */
    private static void settle(Activity a, Session s, boolean commit) {
        if (s.settling) return;
        s.settling = true;
        float target = commit ? s.direction * s.width : 0f;
        ValueAnimator animator = ValueAnimator.ofFloat(s.dx, target);
        s.animator = animator;
        animator.setDuration(CalendarDragPhysics.durationMs(s.dx, target, s.width));
        animator.setInterpolator(new DecelerateInterpolator(1.3f));
        animator.addUpdateListener(animation -> {
            if (!s.finished) move(s, (Float) animation.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            boolean handled;
            boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (handled) return;
                handled = true;
                try {
                    if (!cancelled && commit && !s.finished)
                        month(a, CalendarMonthDirection.fromDirection(s.direction));
                } catch (Exception error) {
                    android.util.Log.e("ELUQEN_MONTH_DRAG", "Month commit failed", error);
                } finally {
                    cleanup(a, s);
                    if (cancelled || !commit) rearmAfterTapOrCancel(a);
                }
            }
        });
        animator.start();
    }

    public static boolean handle(Activity a, MotionEvent e) {
        try {
            int act = e.getActionMasked();
            Session s = sessions.get(a);
            if (s != null && s.settling) return true;

            if (act == MotionEvent.ACTION_DOWN) {
                if (s != null && s.active) return true;
                GridLayout g = grid(a);
                if (g == null || g.getWidth() < 32) return false;
                int[] p = new int[2];
                g.getLocationOnScreen(p);
                if (e.getRawX() < p[0] || e.getRawX() > p[0] + g.getWidth()
                        || e.getRawY() < p[1] || e.getRawY() > p[1] + g.getHeight())
                    return false;

                Cache c = caches.get(a);
                if (c != null) removeTasks(a, c);
                s = new Session();
                s.x = e.getRawX();
                s.y = e.getRawY();
                s.grid = g;
                s.width = g.getWidth();
                s.height = g.getHeight();
                s.velocity = VelocityTracker.obtain();
                s.velocity.addMovement(e);
                sessions.put(a, s);
                return false;
            }

            if (s == null) return false;
            if (s.velocity != null) s.velocity.addMovement(e);

            if (act == MotionEvent.ACTION_POINTER_DOWN || act == MotionEvent.ACTION_CANCEL) {
                boolean wasActive = s.active;
                if (wasActive) settle(a, s, false);
                else {
                    cleanup(a, s);
                    rearmAfterTapOrCancel(a);
                }
                return wasActive;
            }

            if (act == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - s.x;
                float dy = e.getRawY() - s.y;
                float slop = 12f * a.getResources().getDisplayMetrics().density;
                if (!s.active && Math.abs(dx) > slop
                        && Math.abs(dx) > Math.abs(dy) * 1.20f && s.width > 32) {
                    int direction = CalendarMonthDirection.fromDrag(dx);
                    MotionEvent cancel = MotionEvent.obtain(e);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    s.grid.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    preview(a, s, direction);
                }
                if (s.active) {
                    if (dx * s.direction < 0f) dx = 0f;
                    move(s, dx);
                    return true;
                }
                return false;
            }

            if (act == MotionEvent.ACTION_UP) {
                if (!s.active) {
                    cleanup(a, s);
                    rearmAfterTapOrCancel(a);
                    return false;
                }
                float dx = e.getRawX() - s.x;
                float velocityX = 0f;
                if (s.velocity != null) {
                    s.velocity.computeCurrentVelocity(1000);
                    velocityX = s.velocity.getXVelocity();
                }
                boolean commit = CalendarDragPhysics.shouldCommit(
                        dx, velocityX, s.width,
                        a.getResources().getDisplayMetrics().density, s.direction);
                settle(a, s, commit);
                return true;
            }
        } catch (Throwable error) {
            android.util.Log.e("ELUQEN_MONTH_DRAG", "Drag error", error);
            Session s = sessions.get(a);
            if (s != null) {
                if (s.animator != null) s.animator.cancel();
                cleanup(a, s);
            }
        }
        return false;
    }
}
