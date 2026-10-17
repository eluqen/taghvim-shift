package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.eluqen.shiftcalendar.core.Event;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.CardexStats;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.data.UserEvent;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import com.eluqen.shiftcalendar.work.PremiumWorkPolicy;

/**
 * Monthly work ledger. The header stays fixed while all days of the month scroll below it.
 * Operational truth is always resolved through DayWorkResolver; this screen never reimplements
 * shift-state rules.
 */
public final class CardexActivity extends BaseActivity {
    private static final int BG_NOTE = 0xfffff2cf;
    private static final int FG_NOTE = 0xff8a5a00;
    private static final int BG_SWAP = 0xffeee7ff;
    private static final int FG_SWAP = 0xff6845a8;
    private static final int BG_LEAVE = 0xffffeadc;
    private static final int FG_LEAVE = 0xffa85421;
    private static final int BG_REST = 0xffe6f1ff;
    private static final int FG_REST = 0xff376ea8;
    private static final int BG_OT = 0xffe3f6ea;
    private static final int FG_OT = 0xff24724a;
    private static final int BG_EMPTY = 0xfff4f7f9;
    private static final int GRID = 0xffd9e2e8;

    private AppDatabase database;
    private PersianDate selected = PersianDate.todayTehran();
    private TextView monthTitle;
    private LinearLayout monthSummary;
    private LinearLayout rows;
    private ScrollView scroll;
    private LinearLayout monthStatsPanel;
    private Button todayButton;
    private View todayRowView;
    private PersianDate pickedDateFocus;
    private boolean scrollToPickedDate;
    private boolean autoScrollToToday = true;
    private boolean initialTodayPositionPending = true;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        database = AppDatabase.get(this);
        String date = getIntent().getStringExtra(ReminderScheduler.EXTRA_DATE);
        initialTodayPositionPending = bundle == null
                && (date == null || date.trim().isEmpty());
        if (date != null) {
            try { selected = PersianDate.parse(date); } catch (RuntimeException ignored) {}
        }
        if (bundle != null) {
            try { selected = PersianDate.parse(bundle.getString("selected", selected.toString())); }
            catch (RuntimeException ignored) {}
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageReturn.reset(this);
        refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String requested = intent == null ? null
                : intent.getStringExtra(ReminderScheduler.EXTRA_DATE);
        if (requested == null || requested.trim().isEmpty()) {
            selected = PersianDate.todayTehran();
            MonthStep.reset(this);
            autoScrollToToday = true;
        } else {
            try {
                selected = PersianDate.parse(requested);
                autoScrollToToday = selected.year == PersianDate.todayTehran().year
                        && selected.month == PersianDate.todayTehran().month;
            } catch (RuntimeException ignored) {
                selected = PersianDate.todayTehran();
                autoScrollToToday = true;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("selected", selected.toString());
        super.onSaveInstanceState(out);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3));
        nav.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT, Ui.BORDER, 10, this));

        Button next = Ui.tonalButton(this, "ماه بعد");
        next.setTextSize(11.5f);
        next.setOnClickListener(v -> moveMonth(1));

        todayButton = Ui.tonalButton(this, "امروز");
        todayButton.setTextSize(11.5f);
        todayButton.setVisibility(View.VISIBLE);
        todayButton.setEnabled(true);
        todayButton.setOnClickListener(v -> goToday());

        monthTitle = Ui.title(this, "", 16f);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setBackground(Ui.roundedStroke(Ui.CARD, Ui.BORDER, 10, this));
        monthTitle.setOnClickListener(v ->
                PersianDatePicker.show(this, selected, picked -> {
                    selected = picked;
                    pickedDateFocus = picked;
                    scrollToPickedDate = true;
                    initialTodayPositionPending = false;
                    autoScrollToToday = false;
                    MonthStep.reset(this);
                    refresh();
                }));

        Button previous = Ui.tonalButton(this, "ماه قبل");
        previous.setTextSize(11.5f);
        previous.setOnClickListener(v -> moveMonth(-1));

        Button bulkLeave = Ui.tonalButton(this, "مرخصی گروهی");
        bulkLeave.setTextSize(10.5f);
        bulkLeave.setOnClickListener(v -> showBulkDailyLeave());

        // RTL timeline: previous on the right, next on the left.
        LinearLayout rightGroup = new LinearLayout(this);
        rightGroup.setOrientation(LinearLayout.HORIZONTAL);
        rightGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rightGroup.setGravity(Gravity.CENTER);
        rightGroup.addView(previous,
                new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 40)));
        rightGroup.addView(todayButton,
                new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 40)));

        LinearLayout leftGroup = new LinearLayout(this);
        leftGroup.setOrientation(LinearLayout.HORIZONTAL);
        leftGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        leftGroup.setGravity(Gravity.CENTER);
        leftGroup.addView(bulkLeave,
                new LinearLayout.LayoutParams(Ui.dp(this, 86), Ui.dp(this, 40)));
        leftGroup.addView(next,
                new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 40)));

        nav.addView(rightGroup,
                new LinearLayout.LayoutParams(Ui.dp(this, 130), Ui.dp(this, 40)));
        LinearLayout.LayoutParams monthLp =
                new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1f);
        monthLp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        nav.addView(monthTitle, monthLp);
        nav.addView(leftGroup,
                new LinearLayout.LayoutParams(Ui.dp(this, 158), Ui.dp(this, 40)));
        root.addView(nav, Ui.margins(this, -1, Ui.dp(this, 46), 6, 3, 6, 3));

        monthStatsPanel = new LinearLayout(this);
        monthStatsPanel.setOrientation(LinearLayout.VERTICAL);
        monthStatsPanel.setVisibility(View.VISIBLE);
        monthStatsPanel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        monthStatsPanel.setPadding(Ui.dp(this, 7), Ui.dp(this, 7),
                Ui.dp(this, 7), Ui.dp(this, 7));
        monthStatsPanel.setBackground(Ui.roundedStroke(Ui.CARD, Ui.BORDER, 16, this));

        monthSummary = new LinearLayout(this);
        monthSummary.setOrientation(LinearLayout.VERTICAL);
        monthSummary.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        monthStatsPanel.addView(monthSummary, new LinearLayout.LayoutParams(-1, -2));

        // Sticky/fixed header: only the rows below this point scroll.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));
        header.setBackgroundColor(0xffe8f0f2);

        header.addView(headerTextCell("روز", 1.30f));
        header.addView(headerTextCell("وضعیت", 1.35f));
        header.addView(headerIconCell("یادداشت", WorkStatusGlyph.NOTE, .80f));
        header.addView(headerIconCell("تعویض", WorkStatusGlyph.SWAP, .80f));
        header.addView(headerIconCell("مرخصی", WorkStatusGlyph.LEAVE, .80f));
        header.addView(headerIconCell("استراحت", WorkStatusGlyph.REST, .80f));
        header.addView(headerIconCell("اضافه‌کار", WorkStatusGlyph.OVERTIME, .80f));
        root.addView(header, new LinearLayout.LayoutParams(-1, Ui.dp(this, 50)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setScrollbarFadingEnabled(false);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 18));
        scroll.addView(rows);
        scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                updateTodayButtonState());
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void updateTodayButtonState() {
        if (todayButton == null) return;

        PersianDate today = PersianDate.todayTehran();
        boolean currentMonth = selected.year == today.year && selected.month == today.month;
        boolean todayVisible = false;

        if (currentMonth && todayRowView != null && scroll != null
                && scroll.getHeight() > 0 && todayRowView.getHeight() > 0) {
            int[] scrollPos = new int[2];
            int[] rowPos = new int[2];
            scroll.getLocationOnScreen(scrollPos);
            todayRowView.getLocationOnScreen(rowPos);

            int viewportTop = scrollPos[1];
            int viewportBottom = viewportTop + scroll.getHeight();
            int rowTop = rowPos[1];
            int rowBottom = rowTop + todayRowView.getHeight();

            // Any visible portion of today's row counts as "today is shown".
            todayVisible = rowBottom > viewportTop && rowTop < viewportBottom;
        }

        boolean attention = !todayVisible;
        todayButton.animate().cancel();
        todayButton.setVisibility(View.VISIBLE);
        todayButton.setEnabled(true);
        todayButton.setAlpha(1f);
        todayButton.setTypeface(
                Typeface.DEFAULT, attention ? Typeface.BOLD : Typeface.NORMAL);
        todayButton.setTextColor(attention ? 0xffffffff : Ui.PRIMARY);
        todayButton.setBackgroundTintList(null);
        todayButton.setBackground(Ui.roundedStroke(
                attention ? Ui.SECONDARY : Ui.SURFACE_ALT,
                attention ? Ui.PRIMARY_DARK : Ui.BORDER,
                14, this));
        todayButton.setElevation(Ui.dp(this, attention ? 4 : 0));
        todayButton.setContentDescription(attention
                ? "امروز خارج از محدوده نمایش است؛ برای بازگشت به امروز لمس کنید"
                : "امروز؛ برای بازگشت به ردیف امروز لمس کنید");
    }

    private void positionTodayBeforeFirstDraw(View target) {
        if (scroll == null || target == null) return;
        final ViewTreeObserver.OnPreDrawListener listener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        ViewTreeObserver observer = scroll.getViewTreeObserver();
                        if (observer.isAlive()) observer.removeOnPreDrawListener(this);
                        scroll.scrollTo(0, Math.max(0, target.getTop() - Ui.dp(CardexActivity.this, 8)));
                        initialTodayPositionPending = false;
                        autoScrollToToday = false;
                        scroll.post(CardexActivity.this::updateTodayButtonState);
                        return true;
                    }
                };
        scroll.getViewTreeObserver().addOnPreDrawListener(listener);
    }

    private void moveMonth(int delta) {
        selected = (PersianDate) MonthStep.next(this, selected, delta);
        pickedDateFocus = null;
        scrollToPickedDate = false;
        autoScrollToToday = false;
        refresh();
    }

    private void goToday() {
        selected = PersianDate.todayTehran();
        pickedDateFocus = null;
        scrollToPickedDate = false;
        MonthStep.reset(this);
        autoScrollToToday = true;
        refresh();
    }

    private void refresh() {
        if (rows == null) return;
        rows.removeAllViews();
        monthTitle.setText(PersianDigits.of(selected.monthName() + " " + selected.year));

        int notes = 0, swaps = 0, leaves = 0, rests = 0, overtime = 0;
        int applicableFridays = 0;
        int applicableHolidays = 0;
        PersianDate first = selected.firstOfMonth();
        PersianDate today = PersianDate.todayTehran();
        int days = JalaliCalendar.daysInMonth(selected.year, selected.month);
        ShiftPlan plan = database.activePlan();
        List<CardexRecord> monthRecords = database.cardexBetween(
                first.toJdn(), first.plusDays(days - 1).toJdn());
        Map<String, CardexRecord> recordsByDate = new HashMap<>();
        for (CardexRecord record : monthRecords) {
            if (record != null && record.date != null) {
                recordsByDate.put(record.date, record);
            }
        }
        List<UserEvent> userEvents = database.allUserEvents();
        View todayRow = null;
        View pickedRow = null;
        todayRowView = null;

        for (int i = 0; i < days; i++) {
            PersianDate date = first.plusDays(i);
            CardexRecord record = recordsByDate.get(date.toString());
            int mode = DayWorkResolver.mode(record);
            if (hasNote(record)) notes++;
            if (mode == DayWorkResolver.MODE_SWAP) swaps++;
            if (mode == DayWorkResolver.MODE_LEAVE) leaves++;
            if (mode == DayWorkResolver.MODE_REST) rests++;
            if (mode == DayWorkResolver.MODE_OVERTIME) overtime++;

            DayWorkResolver.State state = plan == null
                    ? null : DayWorkResolver.resolve(this, date, plan, record);
            boolean friday = date.dayOfWeek() == 6;
            boolean official = officialHoliday(date, userEvents);
            View row = dayRow(date, record, state, friday, official);
            rows.addView(row, Ui.margins(this, -1, Ui.dp(this, 72), 0, 0, 0, 4));
            if (date.equals(today)) {
                todayRow = row;
                todayRowView = row;
            }
            if (pickedDateFocus != null && date.equals(pickedDateFocus)) {
                pickedRow = row;
            }

            // Count the whole selected month, including future dates.
            // Only normally scheduled workdays that were NOT marked as leave count.
            // Friday takes precedence if a date is also an official holiday.
            if (PremiumWorkPolicy.scheduledPremiumDayEligible(state)) {
                if (friday) applicableFridays++;
                else if (official) applicableHolidays++;
            }
        }

        CardexStats stats = CardexStats.calculate(this, monthRecords, false);
        renderWorkStats(notes, swaps, leaves, rests, overtime,
                stats, applicableFridays, applicableHolidays);

        if (monthStatsPanel.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) monthStatsPanel.getParent()).removeView(monthStatsPanel);
        }
        rows.addView(monthStatsPanel,
                Ui.margins(this, -1, -2, 0, 10, 0, 8));

        final View todayTarget = todayRow;
        final View pickedTarget = pickedRow;
        if (scrollToPickedDate && pickedTarget != null && scroll != null) {
            scroll.post(() -> {
                scroll.smoothScrollTo(
                        0, Math.max(0, pickedTarget.getTop() - Ui.dp(this, 8)));
                scroll.postDelayed(this::updateTodayButtonState, 260);
            });
            scrollToPickedDate = false;
        } else if (initialTodayPositionPending && todayTarget != null && scroll != null) {
            positionTodayBeforeFirstDraw(todayTarget);
        } else if (autoScrollToToday && todayTarget != null && scroll != null) {
            scroll.post(() -> {
                scroll.smoothScrollTo(
                        0, Math.max(0, todayTarget.getTop() - Ui.dp(this, 8)));
                scroll.postDelayed(this::updateTodayButtonState, 260);
            });
            autoScrollToToday = false;
        } else if (scroll != null) {
            scroll.post(this::updateTodayButtonState);
        }
    }

    private boolean officialHoliday(
            PersianDate date, List<UserEvent> personalEvents) {
        for (Event event : EventRepository.get(this).forDate(date)) {
            if (event.holiday
                    && event.title != null
                    && !"جمعه".equals(event.title.trim())) {
                return true;
            }
        }
        for (UserEvent event : personalEvents) {
            if (event.holiday
                    && event.month == date.month
                    && event.day == date.day
                    && (event.year == 0 || event.year == date.year)
                    && event.title != null
                    && !"جمعه".equals(event.title.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Six statistics only, two compact RTL rows with three equal-width cells.
     * First row: overtime, leave, rest, each HH:MM with no recorded-day count.
     * Second row: Friday, holiday, swap, numbers ONLY with no unit suffix.
     * Data and existing calculations remain untouched.
     */
    private void renderWorkStats(int notes, int swaps, int leaves, int rests,
            int overtime, CardexStats stats,
            int applicableFridays, int applicableHolidays) {
        if (monthSummary == null) return;
        monthSummary.removeAllViews();

        monthSummary.addView(statsTripleRow(
                statLabel("اضافه‌کاری", PersianDigits.of(
                        CardexSummaryPresentation.clockDuration(
                                stats.grossOvertimeMinutes))), FG_OT,
                statLabel("مرخصی", PersianDigits.of(
                        CardexSummaryPresentation.clockDuration(
                                stats.leaveMinutes))), 0xff007c91,
                statLabel("استراحت", PersianDigits.of(
                        CardexSummaryPresentation.clockDuration(
                                stats.restMinutes))), 0xff176ab0));

        // Fridays override coincident official holidays. These are relevant
        // scheduled workdays with leave already excluded, including future days.
        monthSummary.addView(statsTripleRow(
                statLabel("جمعه‌کاری", PersianDigits.of(
                        CardexSummaryPresentation.numberOnly(applicableFridays))),
                0xffc62828,
                statLabel("تعطیل‌کاری", PersianDigits.of(
                        CardexSummaryPresentation.numberOnly(applicableHolidays))),
                0xffc62828,
                statLabel("تعویض شیفت", PersianDigits.of(
                        CardexSummaryPresentation.numberOnly(swaps))),
                FG_SWAP));
    }

    private String statLabel(String title, String value) {
        // Keep HH:MM in the correct visual order in a Persian RTL sentence.
        if (value.matches("[0-9\u06F0-\u06F9]+:[0-9\u06F0-\u06F9]{2}")) {
            value = "\u200e" + value + "\u200e";
        }
        return title + ": " + value;
    }

    private View statsTripleRow(String first, int firstColor,
            String second, int secondColor, String third, int thirdColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(statsTripleCell(first, firstColor),
                new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(statsTripleCell(second, secondColor),
                new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(statsTripleCell(third, thirdColor),
                new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View statsTripleCell(String text, int labelColor) {
        // On real phone screenshots, 24sp values and titles placed BESIDE each
        // other in a one-third-wide cell overlap or crush the title. Each of
        // the six metrics therefore gets TWO lines INSIDE its allocated third:
        // a readable title on top and a large centered value below. The overall
        // summary remains exactly 2 ROWS of 3 metrics and has no extra cards.
        int divider = text.indexOf(':');
        String title = divider > 0 ? text.substring(0, divider) : text;
        String value = divider > 0 ? text.substring(divider + 1).trim() : "";

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        cell.setPadding(Ui.dp(this, 2), Ui.dp(this, 7),
                Ui.dp(this, 2), Ui.dp(this, 7));

        TextView label = Ui.text(this, title, 12.5f, labelColor);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        label.setTextDirection(View.TEXT_DIRECTION_RTL);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setIncludeFontPadding(false);
        // Autosize the TITLE only when needed for particularly narrow phones.
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                label, 11, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        cell.addView(label, new LinearLayout.LayoutParams(-1, -2));

        TextView number = Ui.text(this, value, 22f, Ui.TEXT);
        number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        number.setTextDirection(View.TEXT_DIRECTION_LTR);
        number.setGravity(Gravity.CENTER);
        number.setSingleLine(true);
        number.setIncludeFontPadding(false);
        // Full-width number cell, not side-by-side with the title; no squashing.
        cell.addView(number, Ui.margins(this, -1, -2, 0, 4, 0, 0));

        cell.setContentDescription(title + ": " + value);
        return cell;
    }

    private GradientDrawable todayRowBackground() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xffffedb5, 0xfffff8dc, 0xfffff0bf});
        bg.setCornerRadius(Ui.dp(this, 12));
        bg.setStroke(Ui.dp(this, 3), 0xffffa000);
        return bg;
    }

    private View dayRow(PersianDate date, CardexRecord record, DayWorkResolver.State state, boolean friday, boolean officialHoliday) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1));

        boolean today = date.equals(PersianDate.todayTehran());
        int mode = DayWorkResolver.mode(record);
        int rowBg = friday ? 0xffffeef1
                : (officialHoliday ? 0xfffff5df : 0xffffffff);
        int rowStroke = friday ? 0xffe39aa4
                : (officialHoliday ? 0xffddb56a : 0xffe2e9ee);
        row.setBackground(today
                ? todayRowBackground()
                : Ui.roundedStroke(rowBg, rowStroke, 12, this));
        if (today) row.setElevation(Ui.dp(this, 4));
        boolean picked = pickedDateFocus != null && date.equals(pickedDateFocus);
        if (picked) {
            GradientDrawable focus = new GradientDrawable();
            focus.setColor((Ui.SECONDARY & 0x00ffffff) | 0x24000000);
            focus.setCornerRadius(Ui.dp(this, 12));
            focus.setStroke(Ui.dp(this, 3), Ui.SECONDARY);
            row.setForeground(focus);
            row.setElevation(Ui.dp(this, 6));
            row.setContentDescription("روز انتخاب‌شده " + date.displayLong());
        }

        String dayTag = today ? "امروز" : (officialHoliday && !friday ? "تعطیل" : "");
        String dayText = date.weekdayName() + "\n" + PersianDigits.of(String.valueOf(date.day))
                + (dayTag.isEmpty() ? "" : "\n" + dayTag);
        TextView day = cell(dayText,
                11.5f, friday || officialHoliday ? Ui.HOLIDAY : Ui.TEXT,
                today ? 0xffffe9a6 : (friday ? 0xffffe4e8 : (officialHoliday ? 0xffffefd0 : 0xffffffff)),
                1.30f, true);
        if (today) {
            day.setBackground(Ui.roundedStroke(0xffffe9a6, 0xffffa000, 10, this));
            day.setTextColor(0xff6b4a00);
            day.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        day.setContentDescription("روز " + date.displayLong() + "؛ برای ثبت یا ویرایش لمس کنید");
        day.setOnClickListener(v -> edit(date, CardexEditor.OPEN_KEEP));
        row.addView(day);

        View stateCell = statusCell(state, mode);
        stateCell.setOnClickListener(v -> edit(date, CardexEditor.OPEN_KEEP));
        row.addView(stateCell);

        boolean note = hasNote(record);
        row.addView(actionCell(WorkStatusGlyph.NOTE, "یادداشت", note,
                BG_NOTE, FG_NOTE, .80f, v -> openNote(date)));

        boolean swap = mode == DayWorkResolver.MODE_SWAP;
        row.addView(actionCell(WorkStatusGlyph.SWAP, "تعویض شیفت", swap,
                BG_SWAP, FG_SWAP, .80f, v -> edit(date, DayWorkResolver.MODE_SWAP)));

        boolean leave = mode == DayWorkResolver.MODE_LEAVE;
        row.addView(actionCell(WorkStatusGlyph.LEAVE, "مرخصی", leave,
                BG_LEAVE, FG_LEAVE, .80f, v -> edit(date, DayWorkResolver.MODE_LEAVE)));

        boolean rest = mode == DayWorkResolver.MODE_REST;
        row.addView(actionCell(WorkStatusGlyph.REST, "استراحت", rest,
                BG_REST, FG_REST, .80f, v -> edit(date, DayWorkResolver.MODE_REST)));

        boolean ot = mode == DayWorkResolver.MODE_OVERTIME;
        row.addView(actionCell(WorkStatusGlyph.OVERTIME, "اضافه‌کاری", ot,
                BG_OT, FG_OT, .80f, v -> edit(date, DayWorkResolver.MODE_OVERTIME)));

        return row;
    }

    private View actionCell(int glyph, String label, boolean active,
                            int activeBg, int activeFg, float weight,
                            View.OnClickListener listener) {
        FrameLayout cell = new FrameLayout(this);
        cell.setBackground(Ui.roundedStroke(
                active ? activeBg : 0x44f4f7f9,
                active ? GRID : 0x55d9e2e8, 9, this));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
        cell.setLayoutParams(params);

        WorkStatusGlyph icon = new WorkStatusGlyph(
                this, glyph, 0xffc9d0d6, active);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 44),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        iconParams.rightMargin = Ui.dp(this, 3);
        cell.addView(icon, iconParams);
        cell.setContentDescription(active
                ? label + " ثبت شده؛ برای مشاهده یا ویرایش لمس کنید"
                : label + "؛ برای ثبت لمس کنید");
        cell.setOnClickListener(listener);
        return cell;
    }

    private View headerTextCell(String text, float weight) {
        TextView cell = cell(text, 10f, 0xff29434e, 0xffe8f0f2, weight, true);
        cell.setMaxLines(2);
        return cell;
    }

    private View headerIconCell(String label, int glyph, float weight) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(Ui.roundedStroke(0xffe8f0f2, GRID, 9, this));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
        cell.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(8.4f);
        title.setTextColor(0xff29434e);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        cell.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 18)));

        WorkStatusGlyph icon = new WorkStatusGlyph(this, glyph, 0xff87939c, true);
        cell.addView(icon, new LinearLayout.LayoutParams(
                Ui.dp(this, 27), Ui.dp(this, 27)));
        cell.setContentDescription(label);
        return cell;
    }

    private TextView cell(String text, float size, int fg, int bg, float weight, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(fg);
        view.setGravity(Gravity.CENTER);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setIncludeFontPadding(false);
        view.setPadding(Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3));
        view.setBackground(Ui.roundedStroke(bg, GRID, 9, this));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
        view.setLayoutParams(params);
        return view;
    }

    private int statusBackground(int mode) {
        if (mode == DayWorkResolver.MODE_SWAP) return BG_SWAP;
        if (mode == DayWorkResolver.MODE_LEAVE) return BG_LEAVE;
        if (mode == DayWorkResolver.MODE_REST) return BG_REST;
        if (mode == DayWorkResolver.MODE_OVERTIME) return BG_OT;
        return 0xffedf6f5;
    }

    private View statusCell(DayWorkResolver.State state, int mode) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3));
        box.setBackground(Ui.roundedStroke(statusBackground(mode), GRID, 9, this));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1.35f);
        params.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
        box.setLayoutParams(params);

        if (state == null) {
            box.addView(statusLine("تعیین نشده", 0xffeef2f4, 0xff65727b));
            box.setContentDescription("تعیین نشده");
            return box;
        }

        if (mode == DayWorkResolver.MODE_OVERTIME && state.hasOvertime) {
            if (state.baseProfile != null && state.baseProfile.working) {
                TextView pattern = statusLine(
                        state.baseProfile.title,
                        0xffe3f2ff, 0xff24628c);
                String otText = "اضافه‌کار: " + state.overtimeTitle()
                        + "  "
                        + ShiftPreferences.formatMinute(state.overtimeStartMinute)
                        + "–"
                        + ShiftPreferences.formatMinute(state.overtimeEndMinute);
                TextView overtime = statusLine(otText, 0xffe2f7e9, 0xff24724a);

                if (overtimeBeforePattern(state)) {
                    box.addView(overtime);
                    box.addView(pattern);
                } else {
                    box.addView(pattern);
                    box.addView(overtime);
                }
            } else {
                box.addView(statusLine(
                        safeTitle(state.baseProfile),
                        0xffe3f2ff, 0xff24628c));
                box.addView(statusLine(
                        "اضافه‌کار: " + state.overtimeTitle()
                                + "  "
                                + ShiftPreferences.formatMinute(state.overtimeStartMinute)
                                + "–"
                                + ShiftPreferences.formatMinute(state.overtimeEndMinute),
                        0xffe2f7e9, 0xff24724a));
            }
        } else if (mode == DayWorkResolver.MODE_SWAP) {
            box.addView(statusLine(
                    safeTitle(state.baseProfile),
                    0xffe3f2ff, 0xff24628c));
            box.addView(statusLine(
                    state.swapApplied
                            ? "تعویض: " + safeTitle(state.effectiveProfile)
                            : "تعویض نامعتبر",
                    0xffeee7ff, 0xff6845a8));
        } else if (mode == DayWorkResolver.MODE_LEAVE) {
            box.addView(statusLine(
                    safeTitle(state.baseProfile),
                    0xffe3f2ff, 0xff24628c));
            boolean hourlyLeave = state.isHourlyAbsence();
            String leave = hourlyLeave
                    ? "مرخصی ساعتی\n"
                        + ShiftPreferences.formatMinute(state.absenceStartMinute())
                        + "–"
                        + ShiftPreferences.formatMinute(state.absenceEndMinute())
                    : "مرخصی روزانه";
            box.addView(hourlyLeave
                    ? statusLineCompact(leave, BG_LEAVE, FG_LEAVE)
                    : statusLine(leave, BG_LEAVE, FG_LEAVE));
        } else if (mode == DayWorkResolver.MODE_REST) {
            box.addView(statusLine(
                    safeTitle(state.baseProfile),
                    0xffe3f2ff, 0xff24628c));
            boolean hourlyRest = state.isHourlyAbsence();
            String rest = hourlyRest
                    ? "استراحت ساعتی\n"
                        + ShiftPreferences.formatMinute(state.absenceStartMinute())
                        + "–"
                        + ShiftPreferences.formatMinute(state.absenceEndMinute())
                    : "استراحت روزانه";
            box.addView(hourlyRest
                    ? statusLineCompact(rest, BG_REST, FG_REST)
                    : statusLine(rest, BG_REST, FG_REST));
        } else {
            box.addView(statusLine(
                    safeTitle(state.effectiveProfile),
                    0xffe3f2ff, 0xff24628c));
        }

        box.setContentDescription(statusDescription(state, mode)
                + "؛ برای مشاهده یا ویرایش لمس کنید");
        return box;
    }

    private TextView statusLine(String text, int bg, int fg) {
        TextView line = Ui.text(this, text, 9.2f, fg);
        line.setGravity(Gravity.CENTER);
        line.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        line.setSingleLine(true);
        line.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line.setIncludeFontPadding(false);
        line.setPadding(Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2));
        line.setBackground(Ui.roundedStroke(bg, 0x22000000, 7, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.setMargins(0, Ui.dp(this, 1), 0, Ui.dp(this, 1));
        line.setLayoutParams(lp);
        return line;
    }

    private TextView statusLineCompact(String text, int bg, int fg) {
        TextView line = Ui.text(this, text, 7.8f, fg);
        line.setGravity(Gravity.CENTER);
        line.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        line.setSingleLine(false);
        line.setMaxLines(2);
        line.setIncludeFontPadding(false);
        line.setPadding(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
        line.setBackground(Ui.roundedStroke(bg, 0x22000000, 7, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.setMargins(0, Ui.dp(this, 1), 0, Ui.dp(this, 1));
        line.setLayoutParams(lp);
        return line;
    }

    private String safeTitle(ShiftPreferences.Profile profile) {
        if (profile == null || profile.title == null || profile.title.trim().isEmpty()) {
            return "بدون شیفت";
        }
        return profile.title;
    }

    private String statusDescription(DayWorkResolver.State state, int mode) {
        if (state == null) return "تعیین نشده";
        if (mode == DayWorkResolver.MODE_OVERTIME && state.hasOvertime) {
            String pattern = safeTitle(state.baseProfile);
            String ot = "اضافه‌کاری " + state.overtimeTitle();
            if (state.baseProfile != null && state.baseProfile.working) {
                return overtimeBeforePattern(state)
                        ? ot + "، سپس " + pattern
                        : pattern + "، سپس " + ot;
            }
            return pattern + "، " + ot;
        }
        if (mode == DayWorkResolver.MODE_SWAP) {
            return safeTitle(state.baseProfile)
                    + "، تعویض به " + safeTitle(state.effectiveProfile);
        }
        if (mode == DayWorkResolver.MODE_LEAVE) {
            return safeTitle(state.baseProfile) + "، "
                    + state.absenceTitle()
                    + (state.isHourlyAbsence()
                    ? " " + ShiftPreferences.formatMinute(state.absenceStartMinute())
                    + " تا " + ShiftPreferences.formatMinute(state.absenceEndMinute())
                    : "");
        }
        if (mode == DayWorkResolver.MODE_REST) {
            return safeTitle(state.baseProfile) + "، "
                    + state.absenceTitle()
                    + (state.isHourlyAbsence()
                    ? " " + ShiftPreferences.formatMinute(state.absenceStartMinute())
                    + " تا " + ShiftPreferences.formatMinute(state.absenceEndMinute())
                    : "");
        }
        return safeTitle(state.effectiveProfile);
    }

    private boolean overtimeBeforePattern(DayWorkResolver.State state) {
        int overtimeRank = coreShiftRank(
                state.overtimeProfile == null ? "" : state.overtimeProfile.code);
        int baseRank = coreShiftRank(
                state.baseProfile == null ? "" : state.baseProfile.code);
        if (overtimeRank >= 0 && baseRank >= 0) {
            // Organizational order is always shift 1=N, shift 2=D, shift 3=E,
            // regardless of the civil date on which a night interval begins.
            return overtimeRank < baseRank;
        }
        long baseStart = state.shiftStartEpoch(
                this, java.util.TimeZone.getTimeZone("Asia/Tehran"));
        long overtimeStart = state.overtimeStartEpoch(
                this, java.util.TimeZone.getTimeZone("Asia/Tehran"));
        return overtimeStart < baseStart;
    }

    private int coreShiftRank(String code) {
        if ("N".equals(code)) return 0;
        if ("D".equals(code)) return 1;
        if ("E".equals(code)) return 2;
        return -1;
    }

    private boolean hasNote(CardexRecord record) {
        return record != null
                && ((!blank(record.note)) || (!blank(record.noteTime)) || (!blank(record.noteInk)));
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showBulkDailyLeave() {
        ShiftPlan plan = database.activePlan();
        if (plan == null) {
            Ui.toast(this, "الگوی کاری فعال پیدا نشد");
            return;
        }

        ScrollView selectorScroll = new ScrollView(this);
        selectorScroll.setFillViewport(false);
        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.VERTICAL);
        selector.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        selector.setPadding(
                Ui.dp(this, 16), Ui.dp(this, 10),
                Ui.dp(this, 16), Ui.dp(this, 12));
        selectorScroll.addView(selector);

        TextView intro = Ui.text(
                this,
                "روزهای مرخصی را تیک بزنید؛ برداشتن تیک، مرخصی روزانه ثبت‌شده را لغو می‌کند.",
                12.5f, Ui.MUTED);
        intro.setGravity(Gravity.RIGHT);
        selector.addView(intro, Ui.margins(
                this, -1, -2, 0, 0, 0, 8));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button selectAll = Ui.tonalButton(this, "همه");
        Button clearAll = Ui.tonalButton(this, "هیچ‌کدام");
        quick.addView(selectAll, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 40), 1f));
        quick.addView(clearAll, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 40), 1f));
        selector.addView(quick, Ui.margins(
                this, -1, Ui.dp(this, 40), 0, 0, 0, 8));

        List<PersianDate> editableDates = new ArrayList<>();
        List<CheckBox> editableChecks = new ArrayList<>();
        List<Boolean> initialChecked = new ArrayList<>();
        PersianDate first = selected.firstOfMonth();
        int days = JalaliCalendar.daysInMonth(selected.year, selected.month);

        for (int i = 0; i < days; i++) {
            PersianDate date = first.plusDays(i);
            ShiftPreferences.Profile base =
                    DayWorkResolver.baseProfile(this, date, plan);
            CardexRecord record = database.cardex(date.toString());
            int mode = DayWorkResolver.mode(record);
            boolean working = DayWorkResolver.validWork(base);
            boolean dailyLeave = working
                    && mode == DayWorkResolver.MODE_LEAVE
                    && record != null
                    && record.leaveFullShift;
            boolean editable = working
                    && (mode == DayWorkResolver.MODE_NORMAL || dailyLeave);

            String suffix = "";
            if (!working) {
                suffix = "  •  استراحت الگو";
            } else if (dailyLeave) {
                suffix = "  •  مرخصی روزانه";
            } else if (mode == DayWorkResolver.MODE_LEAVE && record != null) {
                suffix = record.leaveFullShift
                        ? "  •  مرخصی روزانه"
                        : "  •  مرخصی ساعتی";
            } else if (mode == DayWorkResolver.MODE_REST && record != null) {
                suffix = record.restFullShift
                        ? "  •  استراحت روزانه"
                        : "  •  استراحت ساعتی";
            } else if (mode == DayWorkResolver.MODE_SWAP) {
                suffix = "  •  تعویض شیفت";
            } else if (mode == DayWorkResolver.MODE_OVERTIME) {
                suffix = "  •  اضافه‌کاری";
            }

            CheckBox check = new CheckBox(this);
            check.setText(PersianDigits.of(
                    date.weekdayName() + "  " + date.day + " " + date.monthName()
                            + "  —  " + (base == null ? "" : base.title) + suffix));
            check.setTextSize(13.5f);
            check.setTextColor(editable ? Ui.TEXT : 0xff9aa4ad);
            check.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            check.setEnabled(editable);
            check.setChecked(dailyLeave);
            check.setPadding(
                    Ui.dp(this, 8), Ui.dp(this, 4),
                    Ui.dp(this, 8), Ui.dp(this, 4));
            check.setBackground(Ui.roundedStroke(
                    editable ? 0xfffffbf7 : 0xfff4f6f7,
                    editable ? 0xffffc79f : 0xffdfe5e9,
                    10, this));
            selector.addView(check, Ui.margins(
                    this, -1, Ui.dp(this, 46), 0, 2, 0, 3));

            if (editable) {
                editableDates.add(date);
                editableChecks.add(check);
                initialChecked.add(dailyLeave);
            }
        }

        selectAll.setOnClickListener(v -> {
            for (CheckBox check : editableChecks) check.setChecked(true);
        });
        clearAll.setOnClickListener(v -> {
            for (CheckBox check : editableChecks) check.setChecked(false);
        });

        if (editableDates.isEmpty()) {
            Ui.toast(this, "در این ماه روز قابل ویرایشی برای مرخصی گروهی وجود ندارد");
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(Ui.rtlDialogTitle(this,"مرخصی گروهی — "
                        + selected.monthName() + " "
                        + PersianDigits.of(String.valueOf(selected.year))))
                .setView(selectorScroll)
                .setPositiveButton("اعمال تغییرات", null)
                .setNegativeButton("انصراف", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    List<CardexRecord> targets = new ArrayList<>();

                    for (int i = 0; i < editableDates.size(); i++) {
                        boolean wanted = editableChecks.get(i).isChecked();
                        boolean wasDailyLeave = initialChecked.get(i);
                        if (wanted == wasDailyLeave) continue;

                        PersianDate date = editableDates.get(i);
                        CardexRecord current = database.cardex(date.toString());
                        ShiftPreferences.Profile base =
                                DayWorkResolver.baseProfile(this, date, plan);
                        if (!DayWorkResolver.validWork(base)) continue;

                        int currentMode = DayWorkResolver.mode(current);
                        boolean currentDailyLeave = currentMode == DayWorkResolver.MODE_LEAVE
                                && current != null && current.leaveFullShift;
                        if (wanted
                                && currentMode != DayWorkResolver.MODE_NORMAL
                                && !currentDailyLeave) {
                            continue;
                        }
                        if (!wanted && !currentDailyLeave) {
                            continue;
                        }

                        CardexRecord target = current == null
                                ? new CardexRecord() : current.copy();
                        target.date = date.toString();
                        target.jdn = date.toJdn();
                        target.clearOperational();

                        if (wanted) {
                            target.leave = true;
                            target.leaveFullShift = true;
                            target.leaveStart =
                                    ShiftPreferences.formatMinute(base.startMinute);
                            target.leaveEnd =
                                    ShiftPreferences.formatMinute(base.endMinute);
                        }
                        targets.add(target);
                    }

                    if (targets.isEmpty()) {
                        Ui.toast(this, "تغییری برای اعمال وجود ندارد");
                        return;
                    }

                    try {
                        database.saveCardexBatch(targets);
                    } catch (RuntimeException error) {
                        Ui.toast(this, error.getMessage() == null
                                ? "ثبت گروهی مرخصی انجام نشد"
                                : error.getMessage());
                        return;
                    }

                    dialog.dismiss();
                    autoScrollToToday = false;
                    refresh();
                    Ui.toast(this, PersianDigits.of(
                            targets.size() + " تغییر اعمال شد"));
                }));
        dialog.show();
    }

    private void openNote(PersianDate date) {
        Intent intent = new Intent(this, WorkNoteActivity.class);
        intent.putExtra(WorkNoteActivity.EXTRA_DATE, date.toString());
        startActivity(intent);
    }

    private void edit(PersianDate date, int preferred) {
        CardexEditor.show(this, date, () -> refresh(), preferred);
    }

    private LinearLayout.LayoutParams navSideParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), .85f);
        p.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
        return p;
    }

    private LinearLayout.LayoutParams navCenterParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1.30f);
        p.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
        return p;
    }
}
