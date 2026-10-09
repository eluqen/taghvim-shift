package com.eluqen.shiftcalendar.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.widget.RemoteViews;
import com.eluqen.shiftcalendar.R;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.ui.FourShiftActivity;
import com.eluqen.shiftcalendar.ui.GeneralWork;
import com.eluqen.shiftcalendar.ui.MainActivity;
import com.eluqen.shiftcalendar.ui.WidgetSettingsActivity;
import com.eluqen.shiftcalendar.ui.WidgetStudio;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

/**
 * ELUQEN Board widget.
 *
 * The previous text-stack widget is intentionally not rendered in this test
 * candidate. Medium/large sizes use a reference-inspired four-group board,
 * while the compact 2x2 size keeps a concise personal status card.
 */
public class ShiftWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH =
            "com.eluqen.shiftcalendar.widget.ACTION_BOARD_REFRESH";
    private static final String BOARD_PREFS = "eluqen_widget_board";
    private static final String BOARD_THEME = "theme";
    private static final String BOARD_CUSTOM_COLOR = "custom_color";
    private static final int CUSTOM_THEME = 5;
    private static final int[] BOARD_BACKGROUNDS = {
            R.drawable.widget_board_bg,
            R.drawable.widget_board_bg_blue,
            R.drawable.widget_board_bg_graphite,
            R.drawable.widget_board_bg_indigo,
            R.drawable.widget_board_bg_forest
    };
    private static final int[] ACTIVE_BACKGROUNDS = {
            R.drawable.widget_group_active,
            R.drawable.widget_group_active_blue,
            R.drawable.widget_group_active_graphite,
            R.drawable.widget_group_active_indigo,
            R.drawable.widget_group_active_forest
    };
    private static final int MODE_COMPACT = 0;
    private static final int MODE_MEDIUM = 1;
    private static final int MODE_LARGE = 2;

    private static final int[] GROUP_HEADERS = {
            R.id.widget_group_header_1,
            R.id.widget_group_header_2,
            R.id.widget_group_header_3,
            R.id.widget_group_header_4
    };
    private static final int[] GROUP_VALUES = {
            R.id.widget_group_value_1,
            R.id.widget_group_value_2,
            R.id.widget_group_value_3,
            R.id.widget_group_value_4
    };

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateComponent(context, manager, ShiftWidgetProviderLarge.class);
    }

    private static void updateComponent(
            Context context, AppWidgetManager manager, Class<?> providerClass) {
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, providerClass));
        for (int id : ids) update(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            updateAll(context.getApplicationContext());
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context, AppWidgetManager manager, int appWidgetId,
            android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions);
        WidgetStudio.captureOptions(context, appWidgetId, newOptions);
        update(context, manager, appWidgetId);
    }

    @Override
    public void onUpdate(
            Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) update(context, manager, id);
    }

    private static int modeFor(AppWidgetManager manager, int appWidgetId) {
        return MODE_LARGE;
    }

    private static int layoutFor(int mode) {
        return R.layout.widget_shift_large;
    }

    private static void update(
            Context context, AppWidgetManager manager, int appWidgetId) {
        int mode = modeFor(manager, appWidgetId);
        RemoteViews views =
                new RemoteViews(context.getPackageName(), layoutFor(mode));
        applyBoardTheme(context, views);

        PersianDate today = PersianDate.todayTehran();
        AppDatabase db = AppDatabase.get(context);
        ShiftPlan active = db.activePlan();

        views.setTextViewText(
                R.id.widget_date,
                PersianDigits.of(today.day + " " + today.monthName()));
        views.setTextViewText(R.id.widget_weekday, today.weekdayName());

        if (active == null) {
            views.setTextViewText(R.id.widget_shift, "الگوی فعال انتخاب نشده");
            views.setTextViewText(R.id.widget_hours, "");
            views.setTextViewText(R.id.widget_next, "");
            views.setTextViewText(R.id.widget_plan, "");
            if (mode != MODE_COMPACT) {
                clearGroupBoard(views);
            }
        } else {
            DayWorkResolver.State state =
                    DayWorkResolver.resolve(context, today, active);
            String hours = state.hoursText();

            views.setTextViewText(
                    R.id.widget_shift,
                    "شیفت شما • " + state.actualTitle());
            views.setTextViewText(
                    R.id.widget_hours,
                    hours.isEmpty()
                            ? "بدون ساعت کاری"
                            : PersianDigits.of(hours));
            views.setTextViewText(
                    R.id.widget_plan,
                    "گروه " + active.team + " • " + state.actualTitle());

            PersianDate nextDate = today.plusDays(1L);
            DayWorkResolver.State next =
                    DayWorkResolver.resolve(context, nextDate, active);
            String nextText = next.nextLabel(
                    today, TimeZone.getTimeZone("Asia/Tehran"));
            views.setTextViewText(
                    R.id.widget_next,
                    "بعدی • " + PersianDigits.of(nextText));

            if (mode != MODE_COMPACT) {
                applyGroupBoard(context, views, db, active, today);
            }
        }

        applyCandidateVisibility(context, views, mode);
        applyActions(context, views, appWidgetId, mode);
        manager.updateAppWidget(appWidgetId, views);
    }

    private static void applyCandidateVisibility(
            Context context, RemoteViews views, int mode) {
        views.setViewVisibility(R.id.widget_group_board, View.VISIBLE);
        views.setViewVisibility(R.id.widget_shift, View.GONE);
        views.setViewVisibility(R.id.widget_hours, View.GONE);
        views.setViewVisibility(R.id.widget_next, View.GONE);
        views.setViewVisibility(R.id.widget_groups, View.GONE);
        views.setViewVisibility(R.id.widget_reminder, View.GONE);
    }

    private static void applyActions(
            Context context, RemoteViews views, int appWidgetId, int mode) {
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
                context,
                appWidgetId,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, openPending);

        Intent settings = new Intent(context, WidgetSettingsActivity.class);
        settings.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent settingsPending = PendingIntent.getActivity(
                context,
                appWidgetId + 10000,
                settings,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_settings, settingsPending);

        Intent refresh = new Intent(
                context,
                ShiftWidgetProviderLarge.class);
        refresh.setAction(ACTION_REFRESH);
        refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId + 20000,
                refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending);

        Intent four = new Intent(context, FourShiftActivity.class);
        PendingIntent fourPending = PendingIntent.getActivity(
                context,
                appWidgetId + 30000,
                four,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_group_board, fourPending);
    }

    private static void clearGroupBoard(RemoteViews views) {
        for (int i = 0; i < 4; i++) {
            views.setTextViewText(GROUP_HEADERS[i], "—");
            views.setTextViewText(GROUP_VALUES[i], "—");
            views.setInt(
                    GROUP_HEADERS[i], "setBackgroundResource",
                    R.drawable.widget_group_cell);
            views.setInt(
                    GROUP_VALUES[i], "setBackgroundResource",
                    R.drawable.widget_group_cell);
            views.setTextColor(GROUP_HEADERS[i], 0xffbfe5e5);
            views.setTextColor(GROUP_VALUES[i], 0xffffffff);
        }
    }

    private static void applyGroupBoard(
            Context context,
            RemoteViews views,
            AppDatabase db,
            ShiftPlan active,
            PersianDate date) {
        ArrayList<ShiftPlan> plans = new ArrayList<>();
        for (ShiftPlan plan : db.plans()) {
            if (active.company.equals(plan.company)) {
                plans.add(plan);
            }
        }
        Collections.sort(plans, new Comparator<ShiftPlan>() {
            @Override
            public int compare(ShiftPlan a, ShiftPlan b) {
                return safe(a.team).compareTo(safe(b.team));
            }
        });

        for (int i = 0; i < 4; i++) {
            if (i >= plans.size()) {
                views.setTextViewText(GROUP_HEADERS[i], "—");
                views.setTextViewText(GROUP_VALUES[i], "—");
                views.setInt(
                        GROUP_HEADERS[i], "setBackgroundResource",
                        R.drawable.widget_group_cell);
                views.setInt(
                        GROUP_VALUES[i], "setBackgroundResource",
                        R.drawable.widget_group_cell);
                views.setTextColor(GROUP_HEADERS[i], 0xff759ca1);
                views.setTextColor(GROUP_VALUES[i], 0xff759ca1);
                continue;
            }

            ShiftPlan plan = plans.get(i);
            String code = GeneralWork.adjust(
                    context,
                    date,
                    plan,
                    ShiftEngine.tokenFor(
                            date.toJdn(), plan.anchorJdn, plan.pattern));
            String title = ShiftPreferences.title(context, code);
            if (title == null || title.trim().isEmpty()) title = "—";

            boolean selected = plan.id == active.id;
            views.setTextViewText(
                    GROUP_HEADERS[i],
                    selected ? "● " + safe(plan.team) : safe(plan.team));
            views.setTextViewText(GROUP_VALUES[i], title);

            int bg = selected
                    ? activeBackground(context)
                    : R.drawable.widget_group_cell;
            views.setInt(GROUP_HEADERS[i], "setBackgroundResource", bg);
            views.setInt(GROUP_VALUES[i], "setBackgroundResource", bg);
            views.setTextColor(
                    GROUP_HEADERS[i],
                    selected ? 0xffffffff : 0xffbfe5e5);
            views.setTextColor(
                    GROUP_VALUES[i],
                    selected ? 0xffffffff : 0xffffffff);
        }
    }

    private static int themeIndex(Context context) {
        int value = context.getSharedPreferences(BOARD_PREFS, 0)
                .getInt(BOARD_THEME, 0);
        return Math.max(0, Math.min(CUSTOM_THEME, value));
    }

    private static void applyBoardTheme(Context context, RemoteViews views) {
        int theme = themeIndex(context);
        if (theme == CUSTOM_THEME) {
            int color = context.getSharedPreferences(BOARD_PREFS, 0)
                    .getInt(BOARD_CUSTOM_COLOR, 0xff8a3ffc);
            views.setInt(
                    R.id.widget_content,
                    "setBackgroundResource",
                    android.R.color.transparent);
            views.setViewVisibility(R.id.widget_custom_background, View.VISIBLE);
            views.setImageViewBitmap(
                    R.id.widget_custom_background,
                    customBackground(color));
        } else {
            views.setViewVisibility(R.id.widget_custom_background, View.GONE);
            views.setInt(
                    R.id.widget_content,
                    "setBackgroundResource",
                    BOARD_BACKGROUNDS[theme]);
        }
    }

    private static Bitmap customBackground(int color) {
        final int width = 640;
        final int height = 144;
        Bitmap bitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(
                0f, 0f, width, 0f,
                new int[]{
                        shade(color, 0.46f),
                        shade(color, 0.70f),
                        shade(color, 0.88f)
                },
                null,
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(
                0f, 0f, width, height,
                36f, 36f, paint);
        return bitmap;
    }

    private static int shade(int color, float valueFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(
                0.08f,
                Math.min(1f, hsv[2] * valueFactor));
        return Color.HSVToColor(hsv);
    }

    private static int activeBackground(Context context) {
        int theme = themeIndex(context);
        return theme == CUSTOM_THEME
                ? R.drawable.widget_group_active
                : ACTIVE_BACKGROUNDS[theme];
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }
}
