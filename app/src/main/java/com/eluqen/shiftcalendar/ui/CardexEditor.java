package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.CardexStats;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.work.CardexCompanyPolicy;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import com.eluqen.shiftcalendar.work.AbsencePolicy;
import com.eluqen.shiftcalendar.work.OvertimeBalancePolicy;
import com.eluqen.shiftcalendar.work.OvertimePolicy;
import com.eluqen.shiftcalendar.work.WorkRestPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CardexEditor {
    public interface Listener {
        void onSaved();
    }

    public static final int OPEN_KEEP = -100;

    private CardexEditor() {}

    public static void show(
            Activity activity, PersianDate date, Listener listener) {
        show(activity, date, listener, OPEN_KEEP);
    }

    public static void show(
            Activity activity, PersianDate date, Listener listener, int preferredOpen) {
        AppDatabase database = AppDatabase.get(activity);
        CardexRecord stored = database.cardex(date.toString());
        CardexRecord record = stored == null ? new CardexRecord() : stored.copy();
        record.date = date.toString();
        record.jdn = date.toJdn();

        ShiftPlan plan = database.activePlan();
        ShiftPreferences.Profile base =
                DayWorkResolver.baseProfile(activity, date, plan);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(
                Ui.dp(activity, 18), Ui.dp(activity, 8),
                Ui.dp(activity, 18), Ui.dp(activity, 8));
        scroll.addView(body);

        TextView dateHeader = Ui.text(
                activity,
                PersianDigits.of(
                        date.weekdayName() + "  "
                        + date.day + " " + date.monthName() + " " + date.year),
                19, 0xff145f69);
        dateHeader.setGravity(android.view.Gravity.CENTER);
        dateHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dateHeader.setPadding(
                Ui.dp(activity, 10), Ui.dp(activity, 10),
                Ui.dp(activity, 10), Ui.dp(activity, 10));
        dateHeader.setBackground(Ui.roundedStroke(
                0xffe9fbf6, 0xff6fc7b8, 15, activity));
        body.addView(dateHeader, Ui.margins(
                activity, -1, -2, 0, 0, 0, 8));

        TextView scheduled = Ui.text(
                activity,
                base.working
                        ? "الگوی این روز: " + base.title + "  •  "
                        + ShiftPreferences.formatMinute(base.startMinute)
                        + " تا "
                        + ShiftPreferences.formatMinute(base.endMinute)
                        : "الگوی این روز: " + base.title,
                12.5f, 0xff2d6b87);
        scheduled.setGravity(android.view.Gravity.CENTER);
        scheduled.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scheduled.setPadding(
                Ui.dp(activity, 8), Ui.dp(activity, 7),
                Ui.dp(activity, 8), Ui.dp(activity, 7));
        scheduled.setBackground(Ui.roundedStroke(
                0xffeef8ff, 0xffa8d4e8, 12, activity));
        body.addView(scheduled, Ui.margins(
                activity, -1, -2, 0, 0, 0, 10));

        int existingMode = DayWorkResolver.mode(record);
        int initialMode = preferredOpen >= DayWorkResolver.MODE_NORMAL
                && preferredOpen <= DayWorkResolver.MODE_OVERTIME
                ? preferredOpen : existingMode;

        RadioGroup modes = new RadioGroup(activity);
        modes.setOrientation(LinearLayout.VERTICAL);
        body.addView(modes);

        boolean workingDay = DayWorkResolver.validWork(base);
        boolean hourlyRestAllowed = CardexCompanyPolicy.hourlyRestAllowed(
                database, date, plan, record);
        boolean hourlyLeaveAllowed = CardexCompanyPolicy.hourlyLeaveAllowed(
                database, date, plan, record);
        boolean restAllowed = workingDay
                && restOptionAllowed(activity, database, date, base, record);
        boolean leaveAllowed = workingDay;
        boolean swapAllowed = workingDay
                && CardexCompanyPolicy.swapOptionAllowed(
                        database, date, plan, record);

        RadioButton rest = option(
                activity, modes, "استراحت",
                initialMode == DayWorkResolver.MODE_REST,
                0xffeef5ff, 0xff376ea8);
        LinearLayout restBox = subMenu(activity);
        modes.addView(restBox);
        AbsenceFields restFields = buildAbsenceFields(
                activity, restBox, "استراحت", base,
                record.restFullShift, record.restStart, record.restEnd,
                true, database, date, hourlyRestAllowed);
        if (!restAllowed) {
            rest.setChecked(false);
            rest.setEnabled(false);
            rest.setVisibility(View.GONE);
            restBox.setVisibility(View.GONE);
        }

        RadioButton leave = option(
                activity, modes, "مرخصی استحقاقی",
                initialMode == DayWorkResolver.MODE_LEAVE,
                0xfffff0e6, 0xffa85421);
        LinearLayout leaveBox = subMenu(activity);
        modes.addView(leaveBox);
        AbsenceFields leaveFields = buildAbsenceFields(
                activity, leaveBox, "مرخصی", base,
                record.leaveFullShift, record.leaveStart, record.leaveEnd,
                false, database, date, hourlyLeaveAllowed);
        if (!leaveAllowed) {
            leave.setChecked(false);
            leave.setEnabled(false);
            leave.setVisibility(View.GONE);
            leaveBox.setVisibility(View.GONE);
        }

        RadioButton swap = option(
                activity, modes, "تعویض شیفت",
                initialMode == DayWorkResolver.MODE_SWAP,
                0xfff2edff, 0xff6845a8);
        LinearLayout swapBox = subMenu(activity);
        modes.addView(swapBox);
        SwapFields swapFields = buildSwapFields(
                activity, swapBox, base, record);
        if (!swapAllowed) {
            swap.setChecked(false);
            swap.setEnabled(false);
            swap.setVisibility(View.GONE);
            swapBox.setVisibility(View.GONE);
        }

        RadioButton overtime = option(
                activity, modes, "اضافه‌کاری",
                initialMode == DayWorkResolver.MODE_OVERTIME,
                0xffeaf8ef, 0xff24724a);
        LinearLayout overtimeBox = subMenu(activity);
        modes.addView(overtimeBox);
        OvertimeFields overtimeFields = buildOvertimeFields(
                activity, overtimeBox, database, plan, date, base, record);

        if (modes.getCheckedRadioButtonId() == -1) {
            RadioButton[] candidates = {rest, leave, swap, overtime};
            for (RadioButton candidate : candidates) {
                if (candidate.getVisibility() == View.VISIBLE
                        && candidate.isEnabled()) {
                    candidate.setChecked(true);
                    break;
                }
            }
        }

        Runnable refresh = () -> {
            restFields.setVisible(rest.isChecked() && restAllowed);
            leaveFields.setVisible(leave.isChecked() && leaveAllowed);
            swapFields.setVisible(swap.isChecked() && swapAllowed);
            overtimeFields.setVisible(overtime.isChecked());
        };
        modes.setOnCheckedChangeListener((group, id) -> refresh.run());
        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(scroll)
                .setPositiveButton("ذخیره", null)
                .setNeutralButton("حذف وضعیت", null)
                .setNegativeButton("انصراف", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setTextColor(0xff07895b);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xffa56a17);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xffb24545);
            saveButton.setEnabled(modes.getCheckedRadioButtonId() != -1);
            modes.setOnCheckedChangeListener((group, id) -> {
                refresh.run();
                saveButton.setEnabled(id != -1);
            });
            saveButton.setOnClickListener(v -> {
                        if (modes.getCheckedRadioButtonId() == -1) {
                            Ui.toast(activity, "یک وضعیت را انتخاب کنید");
                            return;
                        }
                        record.clearOperational();

                        if (rest.isChecked()) {
                            if (!restFields.applyTo(record, true)) return;
                        } else if (leave.isChecked()) {
                            if (!leaveFields.applyTo(record, false)) return;
                        } else if (swap.isChecked()) {
                            if (!DayWorkResolver.validWork(base)) {
                                Ui.toast(activity,
                                        "در روز استراحت الگو، تعویض شیفت قابل ثبت نیست");
                                return;
                            }
                            String targetCode = swapFields.selectedCode();
                            if (targetCode.isEmpty()) {
                                Ui.toast(activity,
                                        "شیفت جایگزین را انتخاب کنید");
                                return;
                            }
                            record.swap = true;
                            if ("@legacy".equals(targetCode) && stored != null) {
                                record.swapTime = stored.swapTime;
                                record.swapShiftCode = stored.swapShiftCode;
                            } else {
                                record.swapShiftCode = targetCode;
                            }
                            record.swapNote = value(swapFields.note);
                        } else if (overtime.isChecked()) {
                            String targetCode = overtimeFields.selectedCode();
                            if ("@legacy".equals(targetCode) && stored != null) {
                                record.overtime = true;
                                record.overtimeStart = stored.overtimeStart;
                                record.overtimeEnd = stored.overtimeEnd;
                                record.overtimeShiftCode = stored.overtimeShiftCode;
                                record.overtimeDayOffset = stored.overtimeDayOffset;
                                record.overtimeType = stored.overtimeType;
                            } else {
                                if (targetCode.isEmpty()) {
                                    Ui.toast(activity, "نوبت اضافه‌کاری را انتخاب کنید");
                                    return;
                                }
                                ShiftPreferences.Profile target =
                                        ShiftPreferences.get(activity, targetCode);
                                String overtimeType = overtimeFields.selectedType();
                                if (!OvertimePolicy.targetAllowed(overtimeType, base, target)) {
                                    Ui.toast(activity, "این نوبت برای نوع اضافه‌کاری انتخاب‌شده مجاز نیست");
                                    return;
                                }
                                int startMinute = DayWorkResolver.parseMinute(overtimeFields.start());
                                int endMinute = DayWorkResolver.parseMinute(overtimeFields.end());
                                if (!OvertimePolicy.actualRangeAllowed(target, startMinute, endMinute)) {
                                    Ui.toast(activity, "بازه حضور باید داخل نوبت اضافه‌کاری و حداقل ۱ ساعت باشد");
                                    return;
                                }
                                record.overtime = true;
                                record.overtimeShiftCode = target.code;
                                record.overtimeStart = overtimeFields.start();
                                record.overtimeEnd = overtimeFields.end();
                                record.overtimeDayOffset =
                                        OvertimePolicy.actualStartOffset(plan, date, target, startMinute);
                                record.overtimeType = overtimeType;
                            }
                            if (!WorkRestPolicy.canSave(
                                    activity, database, plan, date, record)) {
                                Ui.toast(activity,
                                        "این ثبت با حداقل زمان استراحت بین نوبت‌های کاری سازگار نیست");
                                return;
                            }
                        }

                        if (record.isEmpty() && stored == null) {
                            return;
                        }

                        try {
                            database.saveCardex(record);
                        } catch (IllegalStateException error) {
                            Ui.toast(activity, error.getMessage());
                            return;
                        }
                        dialog.dismiss();
                        if (listener != null) listener.onSaved();
                    });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setEnabled(stored != null
                            && DayWorkResolver.mode(stored) != DayWorkResolver.MODE_NORMAL);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(v -> {
                        CardexRecord cleared = stored.copy();
                        cleared.clearOperational();
                        boolean deleteDate = cleared.isEmpty();

                        if (DayWorkResolver.mode(stored)
                                == DayWorkResolver.MODE_OVERTIME) {
                            int requiredRest =
                                    OvertimeBalancePolicy.requiredRestReleaseMinutes(
                                            activity, database, date,
                                            deleteDate ? null : cleared,
                                            deleteDate);
                            if (requiredRest > 0) {
                                showOvertimeRestDependency(
                                        activity, database, date, requiredRest,
                                        dialog, listener);
                                return;
                            }
                        }

                        new AlertDialog.Builder(activity)
                                .setMessage("وضعیت ثبت‌شده این روز حذف شود؟")
                                .setPositiveButton("حذف وضعیت", (d, which) -> {
                                    try {
                                        if (deleteDate) {
                                            database.deleteCardex(date.toString());
                                        } else {
                                            database.saveCardex(cleared);
                                        }
                                    } catch (IllegalStateException error) {
                                        Ui.toast(activity, error.getMessage());
                                        return;
                                    }
                                    dialog.dismiss();
                                    if (listener != null) listener.onSaved();
                                })
                                .setNegativeButton("انصراف", null)
                                .show();
                    });
        });

        dialog.show();
    }

    private static void showOvertimeRestDependency(
            Activity activity,
            AppDatabase database,
            PersianDate overtimeDate,
            int requiredRestMinutes,
            AlertDialog editorDialog,
            Listener listener) {
        String amount = PersianDigits.of(
                CardexStats.formatDuration(requiredRestMinutes));

        new AlertDialog.Builder(activity)
                .setTitle("اول استراحت را کم کن")
                .setMessage("برای حذف این اضافه‌کار، اول "
                        + amount
                        + " از استراحت‌های همین ماه را حذف کن.")
                .setPositiveButton("استراحت‌ها", (d, which) ->
                        showRestChoices(
                                activity, database, overtimeDate,
                                editorDialog, listener))
                .setNegativeButton("بستن", null)
                .show();
    }

    private static void showRestChoices(
            Activity activity,
            AppDatabase database,
            PersianDate overtimeDate,
            AlertDialog editorDialog,
            Listener listener) {
        PersianDate first = overtimeDate.firstOfMonth();
        PersianDate last = first.plusMonths(1).plusDays(-1);
        ShiftPlan plan = database.activePlan();

        List<CardexRecord> rows =
                database.cardexBetween(first.toJdn(), last.toJdn());
        ArrayList<PersianDate> dates = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (CardexRecord row : rows) {
            if (DayWorkResolver.mode(row) != DayWorkResolver.MODE_REST) {
                continue;
            }

            PersianDate restDate = PersianDate.parse(row.date);
            int minutes = restMinutes(
                    activity, plan, restDate, row);
            String kind = row.restFullShift
                    ? "روزانه"
                    : "ساعتی";

            dates.add(restDate);
            labels.add(PersianDigits.of(
                    restDate.day + " " + restDate.monthName()
                            + " — " + kind
                            + "، " + CardexStats.formatDuration(minutes)));
        }

        if (dates.isEmpty()) {
            Ui.toast(activity, "استراحت این ماه پیدا نشد");
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("استراحت‌های این ماه")
                .setAdapter(
                        FancyDialogs.list(activity, labels.toArray(new String[0])),
                        (d, which) -> {
                            editorDialog.dismiss();
                            show(
                                    activity,
                                    dates.get(which),
                                    listener,
                                    DayWorkResolver.MODE_REST);
                        })
                .setNegativeButton("بستن", null)
                .show();
    }

    private static int restMinutes(
            Activity activity,
            ShiftPlan plan,
            PersianDate date,
            CardexRecord record) {
        if (plan != null) {
            DayWorkResolver.State state =
                    DayWorkResolver.resolve(
                            activity, date, plan, record);
            if (state.isFullDayAbsence()
                    && DayWorkResolver.validWork(state.baseProfile)) {
                return DayWorkResolver.durationMinutes(
                        state.baseProfile.startMinute,
                        state.baseProfile.endMinute);
            }
            int hourly = state.absenceMinutes();
            if (hourly > 0) return hourly;
        }

        return CardexStats.duration(
                record.restStart, record.restEnd, false);
    }

    private static boolean restOptionAllowed(
            Activity activity,
            AppDatabase database,
            PersianDate date,
            ShiftPreferences.Profile base,
            CardexRecord record) {
        OvertimeBalancePolicy.Balance balance =
                OvertimeBalancePolicy.monthBalance(activity, database, date);
        int reusableCurrentRest = 0;
        if (DayWorkResolver.mode(record) == DayWorkResolver.MODE_REST) {
            if (record.restFullShift) {
                reusableCurrentRest = DayWorkResolver.durationMinutes(
                        base.startMinute, base.endMinute);
            } else {
                reusableCurrentRest = DayWorkResolver.durationMinutes(
                        DayWorkResolver.parseMinute(record.restStart),
                        DayWorkResolver.parseMinute(record.restEnd));
            }
        }
        return balance.netOvertimeMinutes + reusableCurrentRest >= 60;
    }

    private static SwapFields buildSwapFields(
            Activity activity,
            LinearLayout body,
            ShiftPreferences.Profile base,
            CardexRecord record) {
        TextView caption = Ui.text(
                activity, "شیفت جایگزین", 12, Ui.MUTED);
        body.addView(caption, Ui.margins(
                activity, -1, -2, 0, 0, 0, 1));

        List<String> codes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        codes.add("");
        labels.add("انتخاب شیفت جایگزین…");

        for (String code : DayWorkResolver.workingCodes(activity)) {
            ShiftPreferences.Profile profile =
                    ShiftPreferences.get(activity, code);
            if (!DayWorkResolver.validWork(profile)
                    || profile.code.equals(base.code)) {
                continue;
            }
            codes.add(profile.code);
            labels.add(profile.title + " — "
                    + ShiftPreferences.formatMinute(profile.startMinute)
                    + " تا "
                    + ShiftPreferences.formatMinute(profile.endMinute));
        }

        String selected = record.swapShiftCode;
        if (record.swap && (selected == null || selected.trim().isEmpty())
                && DayWorkResolver.parseMinute(record.swapTime) >= 0) {
            codes.add("@legacy");
            labels.add("ثبت قبلی — شروع " + record.swapTime);
            selected = "@legacy";
        }
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(FancyDialogs.spinner(activity, labels));
        selectCode(spinner, codes, selected);
        body.addView(spinner, fieldParams(activity));

        EditText note = Ui.input(
                activity, "توضیح یا نام همکار (اختیاری)");
        note.setText(record.swapNote);
        body.addView(note, fieldParams(activity));

        return new SwapFields(spinner, note, codes);
    }

    private static OvertimeFields buildOvertimeFields(
            Activity activity,
            LinearLayout body,
            AppDatabase database,
            ShiftPlan plan,
            PersianDate date,
            ShiftPreferences.Profile base,
            CardexRecord record) {
        TextView typeLabel = Ui.text(activity, "نوع اضافه‌کاری", 12, Ui.MUTED);
        typeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(typeLabel, Ui.margins(activity, -1, -2, 0, 2, 0, 1));

        List<String> typeCodes = new ArrayList<>();
        List<String> typeLabels = new ArrayList<>();
        typeCodes.add("");
        typeLabels.add("انتخاب نوع…");
        typeCodes.add(OvertimePolicy.TYPE_REPLACEMENT);
        typeLabels.add(CardexRecord.overtimeTypeTitle(
                OvertimePolicy.TYPE_REPLACEMENT));
        typeCodes.add(OvertimePolicy.TYPE_NECESSITY);
        typeLabels.add(CardexRecord.overtimeTypeTitle(
                OvertimePolicy.TYPE_NECESSITY));

        Spinner typeSpinner = new Spinner(activity);
        ArrayAdapter<String> typeAdapter = FancyDialogs.spinner(activity, typeLabels);
        typeSpinner.setAdapter(typeAdapter);
        body.addView(typeSpinner, fieldParams(activity));

        TextView shiftLabel = Ui.text(activity, "نوبت اضافه‌کاری", 12, Ui.MUTED);
        shiftLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(shiftLabel, Ui.margins(activity, -1, -2, 0, 3, 0, 1));

        List<String> codes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Spinner spinner = new Spinner(activity);
        ArrayAdapter<String> shiftAdapter = FancyDialogs.spinner(activity, labels);
        spinner.setAdapter(shiftAdapter);
        body.addView(spinner, fieldParams(activity));

        TextView timeLabel = Ui.text(activity, "بازه حضور", 12, Ui.MUTED);
        timeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(timeLabel, Ui.margins(activity, -1, -2, 0, 3, 0, 1));

        EditText startField = timeInput(activity, "شروع", "");
        EditText endField = timeInput(activity, "پایان", "");
        body.addView(startField, fieldParams(activity));
        body.addView(endField, fieldParams(activity));

        boolean legacy = record.overtime
                && clean(record.overtimeShiftCode).isEmpty()
                && (!clean(record.overtimeStart).isEmpty()
                || !clean(record.overtimeEnd).isEmpty());

        OvertimeFields fields = new OvertimeFields(
                activity, body, database, plan, date, record, base,
                typeSpinner, typeCodes,
                spinner, shiftAdapter, codes, labels,
                startField, endField,
                typeLabel, shiftLabel, timeLabel);

        fields.initialize(
                OvertimePolicy.normalizeType(record.overtimeType),
                legacy ? "@legacy" : clean(record.overtimeShiftCode),
                clean(record.overtimeStart),
                clean(record.overtimeEnd),
                legacy);
        return fields;
    }

    private static void selectCode(
            Spinner spinner, List<String> codes, String code) {
        if (spinner == null || code == null) return;
        for (int i = 1; i < codes.size(); i++) {
            if (codes.get(i).equals(code)) {
                spinner.setSelection(i, false);
                return;
            }
        }
    }

    private static RadioButton option(
            Activity activity,
            RadioGroup body,
            String text,
            boolean checked,
            int background,
            int accent) {
        RadioButton box = new RadioButton(activity);
        box.setId(View.generateViewId());
        box.setText(text);
        box.setTextSize(15.5f);
        box.setTextColor(accent);
        box.setChecked(checked);
        box.setGravity(android.view.Gravity.CENTER_VERTICAL);
        box.setPadding(
                Ui.dp(activity, 10), 0,
                Ui.dp(activity, 10), 0);
        box.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.setBackground(Ui.roundedStroke(
                background, 0x33000000 | (accent & 0x00ffffff), 12, activity));
        try {
            box.setButtonTintList(new ColorStateList(
                    new int[][] {
                            new int[] {android.R.attr.state_checked},
                            new int[] {}
                    },
                    new int[] {accent, 0xffc4cdd2}));
        } catch (RuntimeException ignored) {
        }
        body.addView(box, Ui.margins(
                activity, -1, Ui.dp(activity, 46), 0, 4, 0, 2));
        return box;
    }

    private static AbsenceFields buildAbsenceFields(
            Activity activity,
            LinearLayout body,
            String title,
            ShiftPreferences.Profile base,
            boolean storedFullShift,
            String storedStart,
            String storedEnd,
            boolean compensatoryRest,
            AppDatabase database,
            PersianDate date,
            boolean hourlyAllowed) {
        TextView typeLabel = Ui.text(
                activity, "نوع " + title, 12, Ui.MUTED);
        typeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(typeLabel, Ui.margins(
                activity, -1, -2, 0, 1, 0, 1));

        Spinner type = new Spinner(activity);
        List<String> typeLabels = new ArrayList<>();
        typeLabels.add("روزانه");
        if (hourlyAllowed) typeLabels.add("ساعتی");
        type.setAdapter(FancyDialogs.spinner(activity, typeLabels));
        body.addView(type, fieldParams(activity));

        TextView rangeLabel = Ui.text(
                activity, "بازه " + title, 12, Ui.MUTED);
        rangeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(rangeLabel, Ui.margins(
                activity, -1, -2, 0, 1, 0, 1));

        int storedStartMinute = DayWorkResolver.parseMinute(storedStart);
        int storedEndMinute = DayWorkResolver.parseMinute(storedEnd);
        boolean storedHourlyRange = !storedFullShift
                && AbsencePolicy.hourlyRangeAllowed(
                        base, storedStartMinute, storedEndMinute);

        String defaultStart = storedHourlyRange
                ? clean(storedStart)
                : (DayWorkResolver.validWork(base)
                        ? ShiftPreferences.formatMinute(base.startMinute)
                        : "");
        String defaultEnd = storedHourlyRange
                ? clean(storedEnd)
                : (DayWorkResolver.validWork(base)
                        ? ShiftPreferences.formatMinute(base.endMinute)
                        : "");

        EditText start = timeInput(activity, "شروع", defaultStart);
        EditText end = timeInput(activity, "پایان", defaultEnd);
        body.addView(start, fieldParams(activity));
        body.addView(end, fieldParams(activity));

        TextView balance = null;
        if (compensatoryRest) {
            OvertimeBalancePolicy.Balance current =
                    OvertimeBalancePolicy.monthBalance(activity, database, date);
            balance = Ui.text(
                    activity,
                    "موجودی اضافه‌کاری قابل تبدیل: "
                            + com.eluqen.shiftcalendar.data.CardexStats.formatDuration(
                                    current.netOvertimeMinutes),
                    11.5f, 0xff2f6f9d);
            balance.setGravity(android.view.Gravity.CENTER);
            balance.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            body.addView(balance, Ui.margins(
                    activity, -1, -2, 0, 2, 0, 2));
        }

        boolean hourlyStored = hourlyAllowed && storedHourlyRange;
        type.setSelection(hourlyStored ? 1 : 0, false);

        AbsenceFields fields = new AbsenceFields(
                activity, body, title, base, type, start, end,
                rangeLabel, balance, hourlyAllowed);
        fields.updateRangeVisibility();
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                fields.updateRangeVisibility();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        return fields;
    }

    private static LinearLayout subMenu(Activity activity) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(
                Ui.dp(activity, 12), Ui.dp(activity, 8),
                Ui.dp(activity, 12), Ui.dp(activity, 5));
        box.setBackground(Ui.roundedStroke(
                0xfff7f9fa, 0xffdde6ea, 10, activity));
        LinearLayout.LayoutParams params = Ui.margins(
                activity, -1, -2, 10, 0, 10, 7);
        box.setLayoutParams(params);
        return box;
    }

    private static EditText timeInput(
            Activity activity, String hint, String value) {
        EditText input = Ui.input(activity, hint);
        input.setFocusable(false);
        input.setText(value == null ? "" : value);
        input.setOnClickListener(v -> {
            int hour = 8;
            int minute = 0;
            try {
                String[] parts = value(input).split(":");
                if (parts.length == 2) {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                }
            } catch (RuntimeException ignored) {
            }
            new TimePickerDialog(
                    activity,
                    (picker, h, m) -> input.setText(
                            String.format(
                                    Locale.US, "%02d:%02d", h, m)),
                    hour, minute, true).show();
        });
        input.setOnLongClickListener(v -> {
            input.setText("");
            return true;
        });
        return input;
    }

    private static LinearLayout.LayoutParams fieldParams(
            Activity activity) {
        return Ui.margins(
                activity, -1, -2, 0, 4, 0, 5);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String value(EditText editText) {
        return editText.getText().toString().trim();
    }

    private static final class AbsenceFields {
        final Activity activity;
        final LinearLayout container;
        final String title;
        final ShiftPreferences.Profile base;
        final Spinner type;
        final EditText start;
        final EditText end;
        final TextView rangeLabel;
        final TextView balance;
        final boolean hourlyAllowed;

        AbsenceFields(
                Activity activity,
                LinearLayout container,
                String title,
                ShiftPreferences.Profile base,
                Spinner type,
                EditText start,
                EditText end,
                TextView rangeLabel,
                TextView balance,
                boolean hourlyAllowed) {
            this.activity = activity;
            this.container = container;
            this.title = title;
            this.base = base;
            this.type = type;
            this.start = start;
            this.end = end;
            this.rangeLabel = rangeLabel;
            this.balance = balance;
            this.hourlyAllowed = hourlyAllowed;
        }

        void setVisible(boolean visible) {
            container.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        boolean hourly() {
            return hourlyAllowed && type.getSelectedItemPosition() == 1;
        }

        void updateRangeVisibility() {
            boolean show = hourly();
            if (show && DayWorkResolver.validWork(base)) {
                if (value(start).isEmpty()) {
                    start.setText(ShiftPreferences.formatMinute(
                            base.startMinute));
                }
                if (value(end).isEmpty()) {
                    end.setText(ShiftPreferences.formatMinute(
                            base.endMinute));
                }
            }
            int v = show ? View.VISIBLE : View.GONE;
            rangeLabel.setVisibility(v);
            start.setVisibility(v);
            end.setVisibility(v);
        }

        boolean applyTo(CardexRecord record, boolean rest) {
            if (!DayWorkResolver.validWork(base)) {
                Ui.toast(activity,
                        "برای روزی که شیفت کاری ندارد، "
                                + title + " قابل ثبت نیست");
                return false;
            }

            boolean hourly = hourly();
            String from;
            String to;
            if (hourly) {
                from = value(start);
                to = value(end);
                int startMinute = DayWorkResolver.parseMinute(from);
                int endMinute = DayWorkResolver.parseMinute(to);
                if (!AbsencePolicy.hourlyRangeAllowed(
                        base, startMinute, endMinute)) {
                    Ui.toast(activity,
                            "بازه " + title
                                    + " ساعتی باید داخل شیفت همان روز باشد و کل شیفت را پوشش ندهد");
                    return false;
                }
            } else {
                from = ShiftPreferences.formatMinute(base.startMinute);
                to = ShiftPreferences.formatMinute(base.endMinute);
            }

            if (rest) {
                record.rest = true;
                record.restFullShift = !hourly;
                record.restStart = from;
                record.restEnd = to;
            } else {
                record.leave = true;
                record.leaveFullShift = !hourly;
                record.leaveStart = from;
                record.leaveEnd = to;
            }
            return true;
        }
    }

    private static final class SwapFields {
        final Spinner spinner;
        final EditText note;
        final List<String> codes;

        SwapFields(
                Spinner spinner, EditText note, List<String> codes) {
            this.spinner = spinner;
            this.note = note;
            this.codes = codes;
        }

        void setVisible(boolean visible) {
            View parent = (View) spinner.getParent();
            if (parent != null) parent.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        String selectedCode() {
            int position = spinner.getSelectedItemPosition();
            return position >= 0 && position < codes.size()
                    ? codes.get(position) : "";
        }
    }

    private static final class OvertimeFields {
        final Activity activity;
        final LinearLayout container;
        final AppDatabase database;
        final ShiftPlan plan;
        final PersianDate date;
        final CardexRecord sourceRecord;
        final ShiftPreferences.Profile base;
        final Spinner typeSpinner;
        final List<String> typeCodes;
        final Spinner spinner;
        final ArrayAdapter<String> shiftAdapter;
        final List<String> codes;
        final List<String> labels;
        final EditText start;
        final EditText end;
        final TextView typeLabel;
        final TextView shiftLabel;
        final TextView timeLabel;
        boolean suppressEvents;
        boolean legacyAvailable;

        OvertimeFields(
                Activity activity,
                LinearLayout container,
                AppDatabase database,
                ShiftPlan plan,
                PersianDate date,
                CardexRecord sourceRecord,
                ShiftPreferences.Profile base,
                Spinner typeSpinner,
                List<String> typeCodes,
                Spinner spinner,
                ArrayAdapter<String> shiftAdapter,
                List<String> codes,
                List<String> labels,
                EditText start,
                EditText end,
                TextView typeLabel,
                TextView shiftLabel,
                TextView timeLabel) {
            this.activity = activity;
            this.container = container;
            this.database = database;
            this.plan = plan;
            this.date = date;
            this.sourceRecord = sourceRecord;
            this.base = base;
            this.typeSpinner = typeSpinner;
            this.typeCodes = typeCodes;
            this.spinner = spinner;
            this.shiftAdapter = shiftAdapter;
            this.codes = codes;
            this.labels = labels;
            this.start = start;
            this.end = end;
            this.typeLabel = typeLabel;
            this.shiftLabel = shiftLabel;
            this.timeLabel = timeLabel;

            typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(
                        AdapterView<?> parent, View view, int position, long id) {
                    if (suppressEvents) return;
                    String type = selectedType();
                    rebuildShiftChoices(type, "", false, true);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(
                        AdapterView<?> parent, View view, int position, long id) {
                    if (suppressEvents) return;
                    applySelectedShiftDefaults();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        void initialize(
                String type,
                String selectedCode,
                String startValue,
                String endValue,
                boolean legacy) {
            suppressEvents = true;
            legacyAvailable = legacy;
            selectCode(typeSpinner, typeCodes, type);
            rebuildShiftChoices(type, selectedCode, legacy, false);
            start.setText(startValue);
            end.setText(endValue);

            String selected = selectedCode();
            if (!"@legacy".equals(selected)
                    && !selected.isEmpty()
                    && (clean(startValue).isEmpty() || clean(endValue).isEmpty())) {
                ShiftPreferences.Profile profile =
                        ShiftPreferences.get(activity, selected);
                if (DayWorkResolver.validWork(profile)) {
                    applyDefaultOvertimeRange(profile);
                }
            }
            suppressEvents = false;
            updateTimeVisibility();
        }

        void setVisible(boolean visible) {
            container.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        String selectedCode() {
            int position = spinner.getSelectedItemPosition();
            return position >= 0 && position < codes.size()
                    ? codes.get(position) : "";
        }

        String selectedType() {
            int position = typeSpinner.getSelectedItemPosition();
            return position >= 0 && position < typeCodes.size()
                    ? typeCodes.get(position) : "";
        }

        String start() { return value(start); }
        String end() { return value(end); }

        private void rebuildShiftChoices(
                String type,
                String desiredCode,
                boolean includeLegacy,
                boolean userTypeChange) {
            suppressEvents = true;
            codes.clear();
            labels.clear();
            codes.add("");
            labels.add("انتخاب نوبت…");

            for (String code : new String[]{"N", "D", "E"}) {
                ShiftPreferences.Profile profile =
                        ShiftPreferences.get(activity, code);
                if (!OvertimePolicy.targetAllowed(type, base, profile)) continue;
                if (!WorkRestPolicy.fullShiftChoiceAllowed(
                        activity, database, plan, date, sourceRecord, profile, type)) {
                    continue;
                }
                codes.add(profile.code);
                labels.add(profile.title + " — "
                        + ShiftPreferences.formatMinute(profile.startMinute)
                        + " تا "
                        + ShiftPreferences.formatMinute(profile.endMinute));
            }
            if (includeLegacy) {
                codes.add("@legacy");
                labels.add("ثبت قبلی");
            }
            shiftAdapter.notifyDataSetChanged();

            String target = desiredCode;
            if (userTypeChange) {
                if (OvertimePolicy.TYPE_NECESSITY.equals(type)) {
                    target = necessityDefaultCode();
                } else {
                    target = "";
                }
            }
            selectCode(spinner, codes, target);
            if (userTypeChange) {
                if (target.isEmpty()) {
                    start.setText("");
                    end.setText("");
                } else {
                    ShiftPreferences.Profile profile =
                            ShiftPreferences.get(activity, target);
                    if (DayWorkResolver.validWork(profile)) {
                        applyDefaultOvertimeRange(profile);
                    }
                }
            }
            suppressEvents = false;
            updateTimeVisibility();
        }

        private String necessityDefaultCode() {
            if (codes.contains("D")) return "D";
            if (codes.contains("E")) return "E";
            return "";
        }

        private void applySelectedShiftDefaults() {
            String code = selectedCode();
            if (code.isEmpty()) {
                start.setText("");
                end.setText("");
                updateTimeVisibility();
                return;
            }
            if ("@legacy".equals(code)) {
                updateTimeVisibility();
                return;
            }
            ShiftPreferences.Profile profile =
                    ShiftPreferences.get(activity, code);
            if (DayWorkResolver.validWork(profile)) {
                applyDefaultOvertimeRange(profile);
            }
            updateTimeVisibility();
        }

        private void applyDefaultOvertimeRange(
                ShiftPreferences.Profile profile) {
            start.setText(ShiftPreferences.formatMinute(profile.startMinute));
            end.setText(ShiftPreferences.formatMinute(profile.endMinute));
        }

        private void updateTimeVisibility() {
            boolean show = !selectedCode().isEmpty();
            int visibility = show ? View.VISIBLE : View.GONE;
            timeLabel.setVisibility(visibility);
            start.setVisibility(visibility);
            end.setVisibility(visibility);
        }
    }

}
