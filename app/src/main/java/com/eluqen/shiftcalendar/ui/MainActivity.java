package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.Event;
import com.eluqen.shiftcalendar.core.GregorianDate;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.data.UserEvent;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.hijri.HijriResolver;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.update.AppUpdateManager;
import com.eluqen.shiftcalendar.update.DataUpdateJobService;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class MainActivity extends BaseActivity {
    public static final String EXTRA_SKIP_DEFAULT = "skip_default_screen";
    private ShiftPlan activePlan;
    private GridLayout calendarGrid;
    private final ArrayList<DayCell> cells = new ArrayList<>();
    private AppDatabase database;
    private TextView detailText;
    private PersianDate displayedMonth;
    private TextView monthTitle;
    private Button todayButton;
    private PersianDate selected;
    private TextView selectedSubTitle;
    private TextView selectedTitle;
    private TextView shiftTitle;
    private final Map<String, CardexRecord> visibleCardex = new HashMap<>();
    private List<UserEvent> userEventsCache = new ArrayList<>();

    private void changeMonth(int i) {
        PersianDate persianDate = (PersianDate) MonthStep.next(this, this.selected, i);
        this.selected = persianDate;
        this.displayedMonth = persianDate.firstOfMonth();
        renderMonth();
        renderSelected();
    }

    @Override
    protected View mainPageSwipeExcludedView() {
        return calendarGrid;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (CalendarDrag.handle(this, motionEvent)) {
            return true;
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    static int access$000(MainActivity mainActivity, String str) {
        return mainActivity.shiftColor(str);
    }

    static void access$100(MainActivity mainActivity, PersianDate persianDate) {
        mainActivity.select(persianDate);
    }

    static void access$200(MainActivity mainActivity) {
        mainActivity.editSelected();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.database = AppDatabase.get(this);
        PersianDate persianDate = PersianDate.todayTehran();
        this.selected = persianDate;
        this.displayedMonth = persianDate.firstOfMonth();
        String stringExtra = getIntent() == null ? null : getIntent().getStringExtra(ReminderScheduler.EXTRA_DATE);
        if (stringExtra != null) {
            try {
                PersianDate persianDate2 = PersianDate.parse(stringExtra);
                this.selected = persianDate2;
                this.displayedMonth = persianDate2.firstOfMonth();
            } catch (RuntimeException unused) {
            }
        }
        if (bundle != null) {
            try {
                this.selected = PersianDate.parse(bundle.getString("selected", this.selected.toString()));
                this.displayedMonth = PersianDate.parse(bundle.getString("month", this.displayedMonth.toString())).firstOfMonth();
            } catch (RuntimeException unused2) {
            }
        }
        buildUi();
        if (MainPageSwipe.isWarmup(this)) {
            return;
        }
        Prefs.ensureInitialInstallSource(this);
        DataUpdateJobService.syncSchedule(this);
        
        if (Prefs.reminderEnabled(this)) {
            new Thread(MainActivity.createRunCallback0(this), "eluqen-shift-calendar-main-reminder").start();
        }
        if (Prefs.onboardingComplete(this)) {
            return;
        }
        FirstRunActivity.open(this);
    }

    void handleOnCreateCallback0() {
        ReminderScheduler.sync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageReturn.reset(this);
        CalendarEntryReset.top(this);
        CalendarScrollCue.show(this);
        this.activePlan = this.database.activePlan();
        renderMonth();
        renderSelected();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString("selected", this.selected.toString());
        bundle.putString("month", this.displayedMonth.toString());
        super.onSaveInstanceState(bundle);
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout3.setPadding(Ui.dp(this, 12.0f), Ui.dp(this, 10.0f), Ui.dp(this, 12.0f), Ui.dp(this, 14.0f));
        scrollView.addView(linearLayout3);
        LinearLayout linearLayoutCard = Ui.card(this);
        this.selectedTitle = Ui.title(this, "", 22.0f);
        this.selectedSubTitle = Ui.text(this, "", 13.0f, Ui.MUTED);
        this.shiftTitle = Ui.title(this, "", 17.0f);
        TextView textViewText = Ui.text(this, "", 14.0f, Ui.MUTED);
        this.detailText = textViewText;
        textViewText.setMinLines(2);
        linearLayoutCard.addView(this.selectedTitle);
        linearLayoutCard.addView(this.selectedSubTitle, Ui.margins(this, -1, -2, 0, 4, 0, 0));
        linearLayoutCard.addView(this.shiftTitle, Ui.margins(this, -1, -2, 0, 10, 0, 0));
        linearLayoutCard.addView(this.detailText, Ui.margins(this, -1, -2, 0, 5, 0, 0));
        Button button4 = Ui.button(this, "ثبت یا ویرایش کارکرد این روز");
        button4.setOnClickListener(MainActivity.createOnClickCallback4(this));
        linearLayoutCard.addView(button4, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 12, 0, 0));
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout4.setGravity(17);
        linearLayout4.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        Button button5 = Ui.tonalButton(this, "ماه بعد");
        button5.setOnClickListener(MainActivity.createOnClickCallback5(this));
        Button button6 = Ui.tonalButton(this, "ماه قبل");
        button6.setOnClickListener(MainActivity.createOnClickCallback6(this));
        TextView textViewTitle = Ui.title(this, "", 19.0f);
        this.monthTitle = textViewTitle;
        textViewTitle.setGravity(17);
        this.monthTitle.setOnClickListener(MainActivity.createOnClickCallback7(this));

        linearLayout4.addView(button5,
                new LinearLayout.LayoutParams(Ui.dp(this, 92.0f), Ui.dp(this, 46.0f)));
        linearLayout4.addView(this.monthTitle,
                new LinearLayout.LayoutParams(0, Ui.dp(this, 48.0f), 1.0f));
        linearLayout4.addView(button6,
                new LinearLayout.LayoutParams(Ui.dp(this, 92.0f), Ui.dp(this, 46.0f)));
        linearLayout3.addView(linearLayout4);
        GridLayout gridLayout = new GridLayout(this);
        this.calendarGrid = gridLayout;
        gridLayout.setColumnCount(7);
        this.calendarGrid.setRowCount(7);
        this.calendarGrid.setUseDefaultMargins(false);
        this.calendarGrid.setAlignmentMode(android.widget.GridLayout.ALIGN_BOUNDS);
        this.calendarGrid.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
        this.calendarGrid.setBackground(
                Ui.rounded(calendarCanvasFill(), 12, this));
        addWeekdayHeaders();
        addDayCells();
        linearLayout3.addView(this.calendarGrid,
                new LinearLayout.LayoutParams(-1, Ui.dp(this, 468.0f)));

        // Use one physical elevated surface for both the details content and
        // Today. Do not stack an unelevated overlay above a separately elevated
        // sibling card: Android 15+ may recompute child Z after the first
        // insets/layout pass. Keeping both inside one card surface makes the
        // Today position renderer-independent.
        FrameLayout detailsHost = new FrameLayout(this);
        detailsHost.setClipChildren(false);
        detailsHost.setClipToPadding(false);
        detailsHost.setBackground(
                Ui.roundedStroke(Ui.CARD, Ui.BORDER, 22, this));
        detailsHost.setElevation(Ui.dp(this, 2.0f));

        linearLayoutCard.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        linearLayoutCard.setElevation(0f);
        detailsHost.addView(linearLayoutCard,
                new FrameLayout.LayoutParams(-1, -2));

        this.todayButton = Ui.tonalButton(this, "امروز");
        this.todayButton.setTextSize(13.5f);
        this.todayButton.setMinHeight(0);
        this.todayButton.setMinimumWidth(0);
        this.todayButton.setPadding(
                Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f), 0);
        this.todayButton.setOnClickListener(v -> goToday());
        FrameLayout.LayoutParams todayLp = new FrameLayout.LayoutParams(
                Ui.dp(this, 80.0f), Ui.dp(this, 42.0f),
                android.view.Gravity.LEFT | android.view.Gravity.TOP);
        todayLp.leftMargin = Ui.dp(this, 7.0f);
        todayLp.topMargin = Ui.dp(this, 7.0f);
        this.todayButton.setVisibility(View.VISIBLE);
        this.todayButton.setEnabled(true);
        // Keep Today explicitly above the transparent content child.
        this.todayButton.setElevation(Ui.dp(this, 8.0f));
        this.todayButton.setTranslationZ(Ui.dp(this, 2.0f));
        detailsHost.addView(this.todayButton, todayLp);
        this.todayButton.bringToFront();

        linearLayout3.addView(detailsHost,
                Ui.margins(this, -1, -2, 0, 17, 0, 12));
        TextView textViewText2 = Ui.text(this, "لمس روز: نمایش جزئیات  •  نگه\u200cداشتن روز: ثبت سریع کارکرد", 12.0f, Ui.MUTED);
        textViewText2.setGravity(17);
        linearLayout3.addView(textViewText2, Ui.margins(this, -1, Ui.dp(this, 38.0f), 0, 4, 0, 0));
        setContentView(linearLayout);
    }

    void handleBuildUiCallback4(View view) {
        editSelected();
    }

    void handleBuildUiCallback5(View view) {
        CalendarMotion.navigate(this, 1);
    }

    void handleBuildUiCallback6(View view) {
        CalendarMotion.navigate(this, -1);
    }

    void handleBuildUiCallback8(View view) {
        DualDatePicker.show(this, this.selected, MainActivity.createOnPickedCallback15(this));
    }

    void handleBuildUiCallback7(PersianDate persianDate) {
        this.displayedMonth = persianDate.firstOfMonth();
        this.selected = persianDate;
        renderMonth();
        renderSelected();
    }

    private void addWeekdayHeaders() {
        String[] strArr = {"ش", "ی", "د", "س", "چ", "پ", "ج"};
        for (int i = 0; i < 7; i++) {
            TextView textViewTitle = Ui.title(this, strArr[i], 14.0f);
            textViewTitle.setGravity(17);
            if (i == 6) {
                textViewTitle.setTextColor(Ui.HOLIDAY);
            }
            this.calendarGrid.addView(textViewTitle, gridParams(0, 6 - i, Ui.dp(this, 32.0f)));
        }
    }

    private void addDayCells() {
        for (int i = 0; i < 42; i++) {
            DayCell dayCell = new DayCell();
            this.cells.add(dayCell);
            this.calendarGrid.addView(dayCell.root, gridParams((i / 7) + 1, 6 - (i % 7), Ui.dp(this, 70.0f)));
        }
    }

    private GridLayout.LayoutParams gridParams(int i, int i2, int i3) {
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(GridLayout.spec(i, GridLayout.FILL), GridLayout.spec(i2, GridLayout.FILL, 1.0f));
        layoutParams.width = 0;
        layoutParams.height = i3;
        layoutParams.setMargins(Ui.dp(this, 1.0f), Ui.dp(this, 1.0f), Ui.dp(this, 1.0f), Ui.dp(this, 1.0f));
        return layoutParams;
    }

    private void renderMonth() {
        if (this.calendarGrid == null) {
            return;
        }
        this.activePlan = this.database.activePlan();
        this.monthTitle.setText(PersianDigits.of(this.displayedMonth.monthName() + " " + this.displayedMonth.year));
        PersianDate persianDateFirstOfMonth = this.displayedMonth.firstOfMonth();
        long jdn = persianDateFirstOfMonth.toJdn() - ((long) persianDateFirstOfMonth.dayOfWeek());

        // One SQLite range read replaces 42 per-cell cardex() queries.
        this.visibleCardex.clear();
        for (CardexRecord record : this.database.cardexBetween(
                jdn, jdn + Math.max(0, this.cells.size() - 1))) {
            if (record != null && record.date != null) {
                this.visibleCardex.put(record.date, record);
            }
        }
        // User events are tiny and are loaded once per month render instead of
        // issuing one query for every calendar cell.
        this.userEventsCache = this.database.allUserEvents();

        PersianDate persianDate = PersianDate.todayTehran();
        EventRepository events = EventRepository.get(this);
        for (int i = 0; i < this.cells.size(); i++) {
            PersianDate persianDateFromJdn = JalaliCalendar.fromJdn(((long) i) + jdn);
            CardexRecord record = this.visibleCardex.get(persianDateFromJdn.toString());
            boolean holiday = events.isHoliday(persianDateFromJdn)
                    || hasHolidayUserEvent(persianDateFromJdn);
            this.cells.get(i).bind(
                    persianDateFromJdn,
                    persianDateFromJdn.year == this.displayedMonth.year
                            && persianDateFromJdn.month == this.displayedMonth.month,
                    holiday,
                    DayWorkResolver.resolve(
                            this, persianDateFromJdn, this.activePlan, record),
                    persianDateFromJdn.equals(this.selected),
                    persianDateFromJdn.equals(persianDate));
        }
        HolidayCalendar.month(this, this.displayedMonth, this.calendarGrid);
    }

    private void renderSelected() {
        String str;
        if (this.selectedTitle == null) {
            return;
        }
        this.activePlan = this.database.activePlan();
        GregorianDate gregorian = JalaliCalendar.toGregorian(this.selected);
        HijriDate hijriDateFromJdn = HijriResolver.resolve(this, this.selected);
        CardexRecord selectedRecord =
                this.visibleCardex.get(this.selected.toString());
        if (selectedRecord == null && !this.visibleCardex.containsKey(this.selected.toString())) {
            // Normally selected is inside the rendered 42-cell window. This
            // fallback preserves correctness for any external deep-link edge case.
            selectedRecord = this.database.cardex(this.selected.toString());
        }
        DayWorkResolver.State work =
                DayWorkResolver.resolve(
                        this, this.selected, this.activePlan, selectedRecord);
        this.selectedTitle.setText(PersianDigits.of(this.selected.displayLong()));
        this.selectedSubTitle.setText(PersianDigits.of("میلادی: " + gregorian + "  |  قمری: " + hijriDateFromJdn.displayLong()));
        ShiftPreferences.Profile base = work.baseProfile;
        String displayCode = base.code;
        String strTimeRange = DayWorkResolver.validWork(base)
                ? ShiftPreferences.formatMinute(base.startMinute)
                    + " تا " + ShiftPreferences.formatMinute(base.endMinute)
                : "";
        TextView textView = this.shiftTitle;
        StringBuilder sb = new StringBuilder("طبق برنامه: ").append(base.title)
                .append("  —  ").append(this.activePlan.displayName());
        if (!strTimeRange.isEmpty()) sb.append("\nساعت ").append(strTimeRange);
        textView.setText(PersianDigits.of(sb.toString()));
        this.shiftTitle.setTextColor(shiftColor(displayCode));
        ArrayList arrayList = new ArrayList();
        Iterator<Event> it = EventRepository.get(this).forDate(this.selected).iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().title);
        }
        Iterator<UserEvent> it2 = userEventsFor(this.selected).iterator();
        while (it2.hasNext()) {
            arrayList.add(it2.next().title);
        }
        this.detailText.setText(arrayList.isEmpty() ? "برای این روز مناسبتی ثبت نشده است." : join(arrayList, "\n"));
    }

    private List<UserEvent> userEventsFor(PersianDate date) {
        ArrayList<UserEvent> out = new ArrayList<>();
        for (UserEvent event : this.userEventsCache) {
            if (event.month == date.month
                    && event.day == date.day
                    && (event.year == 0 || event.year == date.year)) {
                out.add(event);
            }
        }
        return out;
    }

    private boolean hasHolidayUserEvent(PersianDate persianDate) {
        for (UserEvent event : userEventsFor(persianDate)) {
            if (event.holiday) return true;
        }
        return false;
    }

    private void select(PersianDate persianDate) {
        MonthStep.reset(this);
        this.selected = persianDate;
        if (persianDate.year != this.displayedMonth.year || persianDate.month != this.displayedMonth.month) {
            this.displayedMonth = persianDate.firstOfMonth();
        }
        renderMonth();
        renderSelected();
    }

    private void editSelected() {
        CardexEditor.show(this, this.selected, MainActivity.createOnSavedCallback16(this));
    }

    void handleEditSelectedCallback14() {
        renderMonth();
        renderSelected();
        ShiftWidgetProvider.updateAll(this);
    }

    private void goToday() {
        MonthStep.reset(this);
        PersianDate persianDate = PersianDate.todayTehran();
        this.selected = persianDate;
        this.displayedMonth = persianDate.firstOfMonth();
        renderMonth();
        renderSelected();
    }

    private void showOnboarding() {
        List<ShiftPlan> listPlans = this.database.plans();
        String[] strArr = new String[listPlans.size()];
        int i = 0;
        for (int i2 = 0; i2 < listPlans.size(); i2++) {
            strArr[i2] = listPlans.get(i2).displayName();
            if (listPlans.get(i2).company.equals("جنرال")) {
                i = i2;
            }
        }
        new AlertDialog.Builder(this).setTitle("الگوی کاری شما").setMessage("الگوی کاری اولیه را انتخاب کنید. هر زمان بخواهید می\u200cتوانید تاریخ شروع، چرخه و نام گروه را تغییر دهید یا یک الگوی کاملاً سفارشی بسازید.").setSingleChoiceItems(FancyDialogs.single(this, strArr), i,
                (DialogInterface.OnClickListener) null).setPositiveButton("شروع", MainActivity.createOnClickCallback9(this, i, listPlans)).setCancelable(false).show();
    }

    void handleCallback75(int i, List list, DialogInterface dialogInterface, int i2) {
        int checkedItemPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
        if (checkedItemPosition >= 0) {
            i = checkedItemPosition;
        }
        this.database.setActivePlan(((ShiftPlan) list.get(i)).id);
        Prefs.setOnboardingComplete(this, true);
        renderMonth();
        renderSelected();
        ShiftWidgetProvider.updateAll(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(MainActivity.createRunCallback8(this), "eluqen-shift-calendar-onboarding-reminder").start();
        }
    }

    void handleCallback74() {
        ReminderScheduler.sync(this);
    }

    private int shiftColor(String str) {
        return ShiftPreferences.color(this, str);
    }

    private int calendarShiftTextColor(boolean current, boolean redDay) {
        if (redDay) return 0xfffffbfc;
        return current ? Ui.PRIMARY : Ui.MUTED;
    }

    private GradientDrawable calendarCellBackground(
            boolean current, boolean selected, boolean today, boolean redDay) {
        int fill;
        int stroke;
        float width;

        if (today && selected) {
            // Today + selected must look different from plain Today. Keep the
            // amber Today fill but switch to a strong teal selection edge.
            fill = 0xffffd54f;
            stroke = 0xff008f8a;
            width = 5.5f;
        } else if (today) {
            fill = 0xffffd54f;
            stroke = 0xffff6f00;
            width = 4.5f;
        } else if (selected && redDay) {
            // Keep holiday semantics through a dark wine fill, while selection
            // gets a high-contrast cyan edge.
            fill = 0xff7a1730;
            stroke = 0xff00e5ff;
            width = 4.0f;
        } else if (selected) {
            fill = 0xff0b8f8a;
            stroke = 0xff004d40;
            width = 4.0f;
        } else if (redDay) {
            fill = 0xffc83b4f;
            stroke = 0xff9f2639;
            width = 1.2f;
        } else {
            fill = current ? Ui.CARD : calendarOutsideMonthFill();
            stroke = current ? Ui.BORDER : calendarOutsideMonthStroke();
            width = 1.0f;
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(Ui.dp(this, 10.0f));
        bg.setStroke(Ui.dp(this, width), stroke);
        return bg;
    }

    private int calendarCanvasFill() {
        // Deliberately theme-derived and slightly stronger than SURFACE_ALT so
        // the calendar canvas visibly follows Ocean/Violet/Forest/Rose themes.
        return mixCalendarColor(Ui.PRIMARY, Ui.SURFACE, 9);
    }

    private int calendarOutsideMonthFill() {
        if (AppTheme.DEFAULT.equals(AppTheme.currentKey(this))) {
            return Ui.SURFACE_ALT;
        }
        return mixCalendarColor(Ui.PRIMARY, Ui.SURFACE_ALT, 16);
    }

    private int calendarOutsideMonthStroke() {
        if (AppTheme.DEFAULT.equals(AppTheme.currentKey(this))) {
            return Ui.BORDER;
        }
        return mixCalendarColor(Ui.PRIMARY, Ui.BORDER, 22);
    }

    private int mixCalendarColor(int foreground, int background, int foregroundPercent) {
        int p = Math.max(0, Math.min(100, foregroundPercent));
        int q = 100 - p;
        return Color.rgb(
                (Color.red(foreground) * p + Color.red(background) * q) / 100,
                (Color.green(foreground) * p + Color.green(background) * q) / 100,
                (Color.blue(foreground) * p + Color.blue(background) * q) / 100);
    }

    private int themedFadedText(int foregroundPercent) {
        int p = Math.max(0, Math.min(100, foregroundPercent));
        int q = 100 - p;
        return Color.rgb(
                (Color.red(Ui.MUTED) * p + Color.red(Ui.SURFACE) * q) / 100,
                (Color.green(Ui.MUTED) * p + Color.green(Ui.SURFACE) * q) / 100,
                (Color.blue(Ui.MUTED) * p + Color.blue(Ui.SURFACE) * q) / 100);
    }

    private static String join(List<String> list, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : list) {
            if (sb.length() > 0) {
                sb.append(str);
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    final class DayCell {
        PersianDate date;
        final TextView day;
        final LinearLayout root;
        final TextView shift;

        DayCell() {
            LinearLayout linearLayout = new LinearLayout(MainActivity.this);
            this.root = linearLayout;
            linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            linearLayout.setGravity(17);
            linearLayout.setPadding(Ui.dp(MainActivity.this, 2.0f), Ui.dp(MainActivity.this, 3.0f), Ui.dp(MainActivity.this, 2.0f), Ui.dp(MainActivity.this, 2.0f));
            TextView textViewTitle = Ui.title(MainActivity.this, "", 15.0f);
            this.day = textViewTitle;
            textViewTitle.setGravity(17);
            TextView textViewText = Ui.text(MainActivity.this, "", 13.5f, Ui.MUTED);
            this.shift = textViewText;
            textViewText.setGravity(17);
            textViewText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            linearLayout.addView(textViewTitle, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            linearLayout.addView(textViewText, new LinearLayout.LayoutParams(-1, Ui.dp(MainActivity.this, 24.0f)));
            linearLayout.setOnClickListener(MainActivity.createDayCellOnClickCallback0(this));
            linearLayout.setOnLongClickListener(MainActivity.createDayCellOnLongClickCallback1(this));
        }

        void handleNewCallback0(View view) {
            MainActivity.access$100(MainActivity.this, this.date);
        }

        boolean handleNewCallback1(View view) {
            MainActivity.access$100(MainActivity.this, this.date);
            MainActivity.access$200(MainActivity.this);
            return true;
        }

        void bind(PersianDate persianDate, boolean z, boolean z2, DayWorkResolver.State work, boolean z3, boolean z4) {
            int iRgb;
            this.date = persianDate;
            this.day.setText(PersianDigits.of(Integer.valueOf(persianDate.day)));
            TextView textView = this.day;
            if (z) {
                iRgb = z2 ? Ui.HOLIDAY : Ui.TEXT;
            } else {
                iRgb = themedFadedText(58);
            }
            textView.setTextColor(iRgb);
            ShiftPreferences.Profile base = work.baseProfile;
            this.shift.setText(base.shortTitle);
            this.root.setContentDescription(persianDate.displayLong() + "، " + base.title);
            // z2 is already the authoritative Friday/official/personal-holiday
            // result prepared by renderMonth(). Never issue another DB lookup here.
            boolean redDay = z && z2;
            this.shift.setTextColor(
                    MainActivity.this.calendarShiftTextColor(z, redDay));
            this.shift.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            this.root.setBackground(
                    MainActivity.this.calendarCellBackground(z, z3, z4, redDay));

            MainActivity mainActivity = MainActivity.this;
            LinearLayout linearLayout = this.root;
            PersianDate persianDate2 = this.date;
            GregorianDay.attach(mainActivity, linearLayout, persianDate2);
            HolidayCalendar.cell(mainActivity, persianDate2, linearLayout, this.day);

            // Final visual pass owns the complete cell so official holidays/Fridays
            // are solid red (never hatched), and Today keeps a stronger border.
            this.root.setBackground(
                    MainActivity.this.calendarCellBackground(z, z3, z4, redDay));
            if (z4) {
                // TODAY uses amber fill, therefore dark ink is the strongest contrast.
                this.day.setTextColor(0xff1f2937);
                this.shift.setTextColor(0xff1f2937);
                GregorianDay.setTextColor(this.root, 0xff4b2e00);
            } else if (z3) {
                this.day.setTextColor(0xffffffff);
                this.shift.setTextColor(0xffffffff);
                GregorianDay.setTextColor(this.root, 0xfff5f7fb);
            } else if (redDay) {
                this.day.setTextColor(0xffffffff);
                this.shift.setTextColor(0xfffffbfc);
                GregorianDay.setTextColor(this.root, 0xfffff1f3);
            } else {
                this.day.setTextColor(z ? Ui.TEXT : themedFadedText(58));
                this.shift.setTextColor(
                        MainActivity.this.calendarShiftTextColor(z, false));
                GregorianDay.setTextColor(
                        this.root, z ? Ui.MUTED : themedFadedText(48));
            }
            this.root.setElevation(Ui.dp(MainActivity.this,
                    (z4 && z3) ? 10.0f : (z4 ? 8.0f : (z3 ? 5.0f : 0.0f))));
        }
    }

    private static Runnable createRunCallback8(final MainActivity capture0) {
        return () -> {
        capture0.handleCallback74();
        };
    }

    private static View.OnClickListener createDayCellOnClickCallback0(final MainActivity.DayCell capture0) {
        return (view) -> {
        capture0.handleNewCallback0(view);
        };
    }

    private static CardexEditor.Listener createOnSavedCallback16(final MainActivity capture0) {
        return () -> {
        capture0.handleEditSelectedCallback14();
        };
    }

    private static View.OnClickListener createOnClickCallback7(final MainActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback8(view);
        };
    }

    private static View.OnLongClickListener createDayCellOnLongClickCallback1(final MainActivity.DayCell capture0) {
        return (view) -> {
        return capture0.handleNewCallback1(view);
        };
    }

    private static View.OnClickListener createOnClickCallback5(final MainActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback5(view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback9(final MainActivity capture0, final int capture1, final List capture2) {
        return (dialogInterface, i) -> {
        capture0.handleCallback75(capture1, capture2, dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback4(final MainActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback4(view);
        };
    }

    private static Runnable createRunCallback0(final MainActivity capture0) {
        return () -> {
        capture0.handleOnCreateCallback0();
        };
    }

    private static View.OnClickListener createOnClickCallback6(final MainActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback6(view);
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback15(final MainActivity capture0) {
        return (persianDate) -> {
        capture0.handleBuildUiCallback7(persianDate);
        };
    }

}
