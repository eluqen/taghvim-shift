package com.eluqen.shiftcalendar.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class FourShiftActivity extends BaseActivity {
    private AppDatabase database;
    private PersianDate month = PersianDate.todayTehran().firstOfMonth();
    private TextView companyTitle;
    private TextView monthTitle;
    private LinearLayout headerHost;
    private LinearLayout table;
    private ScrollView scroll;
    private Button todayButton;
    private PersianDate selectedDateFocus;
    private boolean scrollToSelectedDate;
    private boolean initialTodayPositionPending = true;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        database = AppDatabase.get(this);
        if (bundle != null) {
            initialTodayPositionPending = false;
            try {
                month = PersianDate.parse(
                        bundle.getString("month", month.toString()))
                        .firstOfMonth();
                String focus = bundle.getString("selectedDateFocus", null);
                if (focus != null) selectedDateFocus = PersianDate.parse(focus);
            } catch (RuntimeException ignored) {
            }
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ShiftMonth.reset(this);
        PageReturn.reset(this);
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString("month", month.toString());
        if (selectedDateFocus != null) {
            bundle.putString("selectedDateFocus", selectedDateFocus.toString());
        }
        super.onSaveInstanceState(bundle);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "تقویم چهارشیفت", true));

        // One compact group-selector surface. ShiftMonth.picker() makes this
        // whole card clickable; no nested full-width button is added.
        LinearLayout companyBar = new LinearLayout(this);
        companyBar.setOrientation(LinearLayout.HORIZONTAL);
        companyBar.setGravity(Gravity.CENTER_VERTICAL);
        companyBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        companyBar.setPadding(
                Ui.dp(this, 12), Ui.dp(this, 5),
                Ui.dp(this, 12), Ui.dp(this, 5));
        companyBar.setBackground(Ui.roundedStroke(
                Ui.CARD, Ui.SECONDARY, 16, this));
        companyBar.setElevation(Ui.dp(this, 2));

        companyTitle = Ui.title(this, "", 14.5f);
        companyTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        companyTitle.setSingleLine(true);

        companyBar.addView(companyTitle,
                new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(companyBar, Ui.margins(
                this, -1, Ui.dp(this, 52), 10, 5, 10, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.setBackground(Ui.roundedStroke(
                Ui.SURFACE_ALT, Ui.BORDER, 12, this));

        Button next = Ui.tonalButton(this, "ماه بعد");
        next.setOnClickListener(FourShiftActivity.createOnClickCallback1(this));
        monthTitle = Ui.title(this, "", 18f);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setOnClickListener(FourShiftActivity.createOnClickCallback2(this));
        Button previous = Ui.tonalButton(this, "ماه قبل");
        previous.setOnClickListener(FourShiftActivity.createOnClickCallback3(this));

        todayButton = Ui.tonalButton(this, "امروز");
        todayButton.setAllCaps(false);
        todayButton.setTextSize(12.5f);
        todayButton.setTextColor(0xffffffff);
        todayButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        todayButton.setMinHeight(0);
        todayButton.setMinimumWidth(0);
        todayButton.setBackground(Ui.roundedStroke(
                Ui.SECONDARY, Ui.PRIMARY_DARK, 14, this));
        todayButton.setContentDescription("بازگشت به امروز");
        todayButton.setOnClickListener(v -> goToday());

        // Previous/next keep fixed widths. Month/year owns the exact center area;
        // Today overlays that area and therefore can appear/disappear without moving it.
        // RTL time navigation: previous month sits on the physical right.
        nav.addView(previous,
                new LinearLayout.LayoutParams(
                        Ui.dp(this, 72), Ui.dp(this, 42)));

        FrameLayout navCenter = new FrameLayout(this);
        nav.addView(navCenter,
                new LinearLayout.LayoutParams(
                        0, Ui.dp(this, 44), 1f));

        monthTitle.setPadding(
                Ui.dp(this, 70), 0,
                Ui.dp(this, 70), 0);
        FrameLayout.LayoutParams monthLp =
                new FrameLayout.LayoutParams(
                        -1, -1, Gravity.CENTER);
        navCenter.addView(monthTitle, monthLp);

        FrameLayout.LayoutParams todayNavLp =
                new FrameLayout.LayoutParams(
                        Ui.dp(this, 64), Ui.dp(this, 38),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        todayNavLp.leftMargin = Ui.dp(this, 3);
        todayButton.setVisibility(View.INVISIBLE);
        todayButton.setEnabled(false);
        navCenter.addView(todayButton, todayNavLp);

        nav.addView(next,
                new LinearLayout.LayoutParams(
                        Ui.dp(this, 72), Ui.dp(this, 42)));
        root.addView(nav, Ui.margins(
                this, -1, Ui.dp(this, 46), 10, 1, 10, 2));

        // Work-page style grid: one fixed header and the maximum possible
        // scrolling height for the month. No floating button over the bottom.
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        headerHost = new LinearLayout(this);
        headerHost.setOrientation(LinearLayout.VERTICAL);
        headerHost.setPadding(
                Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        headerHost.setBackgroundColor(Ui.SURFACE_ALT);
        grid.addView(headerHost,
                new LinearLayout.LayoutParams(-1, Ui.dp(this, 45)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        grid.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1f));

        table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(
                Ui.dp(this, 8), 0,
                Ui.dp(this, 8), Ui.dp(this, 8));
        table.setBackgroundColor(Ui.SURFACE_ALT);
        scroll.addView(table);
        scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                updateTodayButtonVisibility());

        setContentView(root);
    }

    private void positionTodayBeforeFirstDraw() {
        if (!initialTodayPositionPending || scroll == null || table == null) return;
        PersianDate today = PersianDate.todayTehran();
        if (month.year != today.year || month.month != today.month) {
            initialTodayPositionPending = false;
            table.post(this::updateTodayButtonVisibility);
            return;
        }
        int index = today.day - 1;
        if (index < 0 || index >= table.getChildCount()) {
            initialTodayPositionPending = false;
            return;
        }
        final View target = table.getChildAt(index);
        final ViewTreeObserver.OnPreDrawListener listener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        ViewTreeObserver observer = scroll.getViewTreeObserver();
                        if (observer.isAlive()) observer.removeOnPreDrawListener(this);
                        int y = Math.max(0, target.getTop() - Ui.dp(FourShiftActivity.this, 6));
                        scroll.scrollTo(0, y);
                        initialTodayPositionPending = false;
                        scroll.post(FourShiftActivity.this::updateTodayButtonVisibility);
                        // scrollTo() is applied before this frame is drawn; allow
                        // the same frame to render instead of blinking/cancelling it.
                        return true;
                    }
                };
        scroll.getViewTreeObserver().addOnPreDrawListener(listener);
    }

    private void updateTodayButtonVisibility() {
        if (todayButton == null) return;
        PersianDate today = PersianDate.todayTehran();
        boolean show = month.year != today.year || month.month != today.month;

        if (!show && scroll != null && table != null) {
            int index = today.day - 1;
            if (index < 0 || index >= table.getChildCount()) {
                show = true;
            } else {
                View row = table.getChildAt(index);
                int[] scrollPos = new int[2];
                int[] rowPos = new int[2];
                scroll.getLocationOnScreen(scrollPos);
                row.getLocationOnScreen(rowPos);

                int viewportTop = scrollPos[1];
                int viewportBottom = viewportTop + scroll.getHeight();
                int rowTop = rowPos[1];
                int rowBottom = rowTop + row.getHeight();

                // Show Today only after today's row has actually left the
                // visible ScrollView viewport. Partial visibility still counts
                // as "inside the frame".
                show = rowBottom <= viewportTop || rowTop >= viewportBottom;
            }
        }

        todayButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        todayButton.setEnabled(show);
    }

    private void goToday() {
        PersianDate today = PersianDate.todayTehran();
        month = today.firstOfMonth();
        selectedDateFocus = null;
        scrollToSelectedDate = false;
        render();
        scrollToToday();
    }

    private void scrollToToday() {
        if (scroll == null || table == null) return;
        PersianDate today = PersianDate.todayTehran();
        if (month.year != today.year || month.month != today.month) return;
        int index = today.day - 1;
        if (index < 0 || index >= table.getChildCount()) return;
        View target = table.getChildAt(index);
        table.post(() -> scroll.smoothScrollTo(
                0, Math.max(0, target.getTop() - Ui.dp(this, 6))));
    }

    void handleBuildUiCallback0(View view) {
        month = month.plusMonths(1).firstOfMonth();
        selectedDateFocus = null;
        scrollToSelectedDate = false;
        initialTodayPositionPending = false;
        render();
    }

    void handleBuildUiCallback2(View view) {
        PersianDatePicker.show(
                this, PersianDate.todayTehran(),
                FourShiftActivity.createOnPickedCallback0(this));
    }

    void handleBuildUiCallback1(PersianDate date) {
        month = date.firstOfMonth();
        selectedDateFocus = date;
        scrollToSelectedDate = true;
        initialTodayPositionPending = false;
        render();
    }

    void handleBuildUiCallback3(View view) {
        month = month.plusMonths(-1).firstOfMonth();
        selectedDateFocus = null;
        scrollToSelectedDate = false;
        initialTodayPositionPending = false;
        render();
    }

    private void render() {
        ShiftPlan active = (ShiftPlan) ShiftMonth.selected(
                this, database.activePlan());
        ArrayList<ShiftPlan> sameCompany = new ArrayList<>();
        for (ShiftPlan item : database.plans()) {
            if (item.company.equals(active.company)) sameCompany.add(item);
        }
        Collections.sort(
                sameCompany, FourShiftActivity.createCompareCallback5());
        if (sameCompany.size() > 4) {
            sameCompany = new ArrayList<>(sameCompany.subList(0, 4));
        }
        @SuppressWarnings("unchecked")
        List<ShiftPlan> plans =
                (List<ShiftPlan>) (List<?>) ShiftMonth.ensureIncluded(
                        this, sameCompany, active);

        companyTitle.setText(
                active.company + "  •  گروه فعال: " + active.team);
        monthTitle.setText(PersianDigits.of(
                month.monthName() + " " + month.year));
        headerHost.removeAllViews();
        headerHost.addView(
                header(plans, active.id), rowParams(43));

        table.removeAllViews();
        int days = JalaliCalendar.daysInMonth(month.year, month.month);
        PersianDate first = month.firstOfMonth();
        Map<String, CardexRecord> recordsByDate = new HashMap<>();
        for (CardexRecord record : database.cardexBetween(
                first.toJdn(), first.plusDays(days - 1).toJdn())) {
            if (record != null && record.date != null) {
                recordsByDate.put(record.date, record);
            }
        }
        for (int day = 1; day <= days; day++) {
            PersianDate date = new PersianDate(month.year, month.month, day);
            table.addView(
                    dayRow(
                            date,
                            plans,
                            active.id,
                            recordsByDate.get(date.toString())),
                    rowParams(54));
        }
        FourShiftPage.afterRender(this, month, table);
        if (scrollToSelectedDate && selectedDateFocus != null
                && selectedDateFocus.year == month.year
                && selectedDateFocus.month == month.month
                && selectedDateFocus.day >= 1
                && selectedDateFocus.day <= table.getChildCount()) {
            View target = table.getChildAt(selectedDateFocus.day - 1);
            table.post(() -> {
                scroll.smoothScrollTo(
                        0, Math.max(0, target.getTop() - Ui.dp(this, 6)));
                scroll.postDelayed(this::updateTodayButtonVisibility, 260);
            });
            scrollToSelectedDate = false;
        } else if (initialTodayPositionPending) positionTodayBeforeFirstDraw();
        else table.post(this::updateTodayButtonVisibility);
    }

    static int handleRenderCallback4(ShiftPlan left, ShiftPlan right) {
        return left.team.compareTo(right.team);
    }

    private View header(List<ShiftPlan> plans, long activeId) {
        LinearLayout row = row();
        TextView day = cell(
                "روز", Ui.SURFACE_ALT, Ui.TEXT, true);
        day.setBackground(Ui.roundedStroke(
                Ui.SURFACE_ALT, Ui.BORDER, 9, this));
        row.addView(day, dateParams());

        for (ShiftPlan item : plans) {
            boolean active = item.id == activeId;
            TextView team = cell(
                    active ? "● " + item.team : item.team,
                    active ? Ui.SECONDARY : Ui.SURFACE_ALT,
                    active ? 0xffffffff : Ui.TEXT,
                    true);
            team.setBackground(Ui.roundedStroke(
                    active ? Ui.SECONDARY : Ui.SURFACE_ALT,
                    active ? Ui.PRIMARY_DARK : Ui.BORDER,
                    9, this));
            team.setContentDescription(
                    active ? "گروه فعال " + item.team : "گروه " + item.team);
            row.addView(team, shiftParams());
        }
        return row;
    }

    private View dayRow(
            PersianDate date,
            List<ShiftPlan> plans,
            long activeId,
            CardexRecord record) {
        LinearLayout row = row();
        row.addView(dateCell(date), dateParams());

        for (ShiftPlan plan : plans) {
            String code = GeneralWork.adjust(
                    this,
                    date,
                    plan,
                    ShiftEngine.tokenFor(
                            date.toJdn(), plan.anchorJdn, plan.pattern));
            boolean active = plan.id == activeId;
            row.addView(
                    shiftVisualCell(plan.team, code, active),
                    shiftParams());
        }

        row.setOnClickListener(
                FourShiftActivity.createOnClickCallback4(this, date));
        FourShiftPolish.decorate(this, date, row);
        CardexMarker.four(this, date, row, record);
        if (selectedDateFocus != null && date.equals(selectedDateFocus)) {
            android.graphics.drawable.GradientDrawable focus =
                    new android.graphics.drawable.GradientDrawable();
            // Selected date uses the active theme's dark primary language.
            // Today deliberately uses a fixed warm amber language in
            // FourShiftPolish, so the two states remain visually unmistakable.
            focus.setColor((Ui.PRIMARY & 0x00ffffff) | 0x30000000);
            focus.setCornerRadius(Ui.dp(this, 12));
            focus.setStroke(Ui.dp(this, 4), Ui.PRIMARY_DARK);
            row.setForeground(focus);
            row.setElevation(Ui.dp(this, 7));
            row.setContentDescription("روز انتخاب‌شده " + date.displayLong());
        }
        return row;
    }

    private View dateCell(PersianDate date) {
        FrameLayout box = new FrameLayout(this);
        box.setTag("four_date_cell");
        box.setBackground(Ui.roundedStroke(
                Ui.CARD, Ui.BORDER, 9, this));

        TextView text = Ui.text(
                this,
                PersianDigits.of(
                        date.day + " " + date.weekdayName()),
                11.7f,
                date.dayOfWeek() == 6 ? Ui.HOLIDAY : Ui.TEXT);
        text.setTag("four_date_text");
        text.setGravity(Gravity.CENTER);
        text.setTypeface(
                Typeface.DEFAULT,
                date.dayOfWeek() == 6 ? Typeface.BOLD : Typeface.NORMAL);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(-1, -1);
        lp.setMargins(
                Ui.dp(this, 2), Ui.dp(this, 1),
                Ui.dp(this, 2), Ui.dp(this, 1));
        box.addView(text, lp);
        return box;
    }

    private View shiftVisualCell(
            String team,
            String code,
            boolean activeColumn) {
        ShiftPreferences.Profile profile =
                ShiftPreferences.get(this, code);

        FrameLayout outer = new FrameLayout(this);
        int fill = blendWithThemeCard(profile.color);
        if (activeColumn) {
            // The picker-selected group owns a clearly framed column across
            // the whole month, not only a colored header.
            int selectedFill = mix(Ui.SECONDARY, fill, 16);
            android.graphics.drawable.GradientDrawable selected =
                    new android.graphics.drawable.GradientDrawable();
            selected.setColor(selectedFill);
            selected.setCornerRadius(Ui.dp(this, 9));
            selected.setStroke(Ui.dp(this, 3), Ui.SECONDARY);
            outer.setBackground(selected);
            outer.setElevation(Ui.dp(this, 3));
        } else {
            outer.setBackground(Ui.roundedStroke(
                    fill, Ui.BORDER, 9, this));
        }

        // In the FourShift chart, literal shift names are clearer than
        // decorative symbols or one-letter abbreviations.
        TextView text = Ui.text(
                this, fourShiftLabel(code, profile), 10.2f, Ui.TEXT);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setMaxLines(2);
        text.setIncludeFontPadding(false);
        text.setPadding(
                Ui.dp(this, 1), Ui.dp(this, 1),
                Ui.dp(this, 1), Ui.dp(this, 1));
        outer.addView(
                text,
                new FrameLayout.LayoutParams(-1, -1));

        outer.setContentDescription(
                (activeColumn ? "گروه فعال " + team + "، " : "")
                        + fourShiftLabel(code, profile));
        return outer;
    }

    private String fourShiftLabel(
            String code,
            ShiftPreferences.Profile profile) {
        String normalized = ShiftEngine.normalize(code);
        if (ShiftEngine.NIGHT.equals(normalized)) return "شبکار";
        if (ShiftEngine.DAY.equals(normalized)) return "روزکار";
        if (ShiftEngine.EVENING.equals(normalized)) return "عصرکار";
        if (ShiftEngine.OFF.equals(normalized)) return "استراحت";
        return profile.title;
    }

    void handleDayRowCallback5(PersianDate date, View view) {
        CardexOpen.four(this, date);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return row;
    }

    private TextView cell(
            String text,
            int background,
            int foreground,
            boolean bold) {
        TextView view = Ui.text(this, text, 12.5f, foreground);
        view.setGravity(Gravity.CENTER);
        view.setBackground(Ui.rounded(
                background, 9, this));
        view.setTypeface(
                Typeface.DEFAULT,
                bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private LinearLayout.LayoutParams dateParams() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, -1, 1.72f);
        lp.setMargins(
                Ui.dp(this, 2), Ui.dp(this, 2),
                Ui.dp(this, 2), Ui.dp(this, 2));
        return lp;
    }

    private LinearLayout.LayoutParams shiftParams() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(
                Ui.dp(this, 2), Ui.dp(this, 2),
                Ui.dp(this, 2), Ui.dp(this, 2));
        return lp;
    }

    private LinearLayout.LayoutParams rowParams(int dp) {
        return new LinearLayout.LayoutParams(
                -1, Ui.dp(this, dp));
    }

    private int blendWithThemeCard(int color) {
        // Shift identity remains semantic; the larger share comes from the selected
        // application theme so FourShift visibly follows the chosen palette.
        int base = mix(Ui.SURFACE_ALT, Ui.CARD, 42);
        return mix(color, base, 18);
    }

    private int mix(int foreground, int background, int foregroundPercent) {
        int p = Math.max(0, Math.min(100, foregroundPercent));
        int q = 100 - p;
        return android.graphics.Color.rgb(
                (android.graphics.Color.red(foreground) * p
                        + android.graphics.Color.red(background) * q) / 100,
                (android.graphics.Color.green(foreground) * p
                        + android.graphics.Color.green(background) * q) / 100,
                (android.graphics.Color.blue(foreground) * p
                        + android.graphics.Color.blue(background) * q) / 100);
    }

    private static PersianDatePicker.Listener createOnPickedCallback0(
            final FourShiftActivity capture0) {
        return date -> capture0.handleBuildUiCallback1(date);
    }

    private static View.OnClickListener createOnClickCallback1(
            final FourShiftActivity capture0) {
        return view -> capture0.handleBuildUiCallback0(view);
    }

    private static Comparator createCompareCallback5() {
        return (left, right) -> FourShiftActivity.handleRenderCallback4(
                (ShiftPlan) left, (ShiftPlan) right);
    }

    private static View.OnClickListener createOnClickCallback3(
            final FourShiftActivity capture0) {
        return view -> capture0.handleBuildUiCallback3(view);
    }

    private static View.OnClickListener createOnClickCallback2(
            final FourShiftActivity capture0) {
        return view -> capture0.handleBuildUiCallback2(view);
    }

    private static View.OnClickListener createOnClickCallback4(
            final FourShiftActivity capture0,
            final PersianDate capture1) {
        return view -> capture0.handleDayRowCallback5(capture1, view);
    }
}
