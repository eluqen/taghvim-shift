package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.UserEvent;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * App-owned, RTL personal-occasion management screen.
 * Existing user_events schema and existing per-record identities are unchanged.
 */
public final class PersonalEventsActivity extends BaseActivity {
    private static final String TAG = "ELUQEN_PERSONAL_EVENTS";
    private static final String[] MONTH_NAMES = {
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };
    private AppDatabase database;
    private LinearLayout list;
    private TextView summary;
    private String filter = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        database = AppDatabase.get(this);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) render();
    }

    private int dp(float value) { return Ui.dp(this, value); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "مناسبت‌های شخصی", true));

        LinearLayout controls = Ui.card(this);
        LinearLayout.LayoutParams clp = Ui.margins(this, -1, -2, 14, 12, 14, 8);
        root.addView(controls, clp);
        controls.addView(Ui.title(this, "مناسبت‌ها، همه در یک جا", 19));
        TextView description = Ui.text(this,
            "برای یک روز می‌توانید چند مناسبت مستقل ثبت کنید. هر مناسبت جداگانه ویرایش یا حذف می‌شود.",
            13.5f, Ui.MUTED);
        controls.addView(description, Ui.margins(this, -1, -2, 0, 7, 0, 9));
        summary = Ui.text(this, "", 13, Ui.SECONDARY);
        controls.addView(summary);
        Button add = Ui.button(this, "＋ افزودن مناسبت");
        controls.addView(add, Ui.margins(this, -1, dp(49), 0, 12, 0, 0));
        add.setOnClickListener(v -> chooseNewDate());

        EditText search = Ui.input(this, "جست‌وجوی عنوان یا تاریخ مناسبت");
        search.setSingleLine(true);
        root.addView(search, Ui.margins(this, -1, dp(53), 14, 2, 14, 10));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter = s.toString().trim().toLowerCase(Locale.ROOT);
                render();
            }
            @Override public void afterTextChanged(Editable text) {}
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(14), dp(2), dp(14), dp(28));
        scroll.addView(list);
        setContentView(root);
    }

    private String dayLabel(UserEvent e) {
        if (e.month < 1 || e.month > 12) return PersianDigits.of(e.day + "/" + e.month);
        return PersianDigits.of(e.day + " " + MONTH_NAMES[e.month - 1]);
    }

    private void render() {
        if (list == null || isFinishing()) return;
        try {
            List<UserEvent> all = PersonalEventItems.sorted(database.allUserEvents());
            int annualCount = 0;
            List<UserEvent> visible = new ArrayList<>();
            for (UserEvent e : all) {
                if (e.year == 0) annualCount++;
                String text = (e.title + " " + dayLabel(e) + " "
                    + e.year + " " + (e.year == 0 ? "هر سال" : ""))
                    .toLowerCase(Locale.ROOT);
                if (filter.isEmpty() || text.contains(filter)) visible.add(e);
            }
            summary.setText(PersianDigits.of(all.size() + " مناسبت ثبت‌شده • "
                + annualCount + " مناسبت سالانه"));
            list.removeAllViews();
            if (visible.isEmpty()) {
                LinearLayout empty = Ui.card(this);
                TextView headline = Ui.title(this,
                    all.isEmpty() ? "هنوز مناسبتی ثبت نشده است" : "مناسبتی پیدا نشد", 16);
                headline.setGravity(Gravity.CENTER);
                empty.addView(headline);
                TextView hint = Ui.text(this,
                    all.isEmpty() ? "برای شروع، روی «افزودن مناسبت» بزنید."
                        : "عبارت جست‌وجو را تغییر دهید.", 13, Ui.MUTED);
                hint.setGravity(Gravity.CENTER);
                empty.addView(hint, Ui.margins(this, -1, -2, 0, 8, 0, 0));
                list.addView(empty, Ui.margins(this, -1, -2, 0, 12, 0, 0));
                return;
            }
            for (UserEvent event : visible) {
                list.addView(eventCard(event), Ui.margins(this, -1, -2, 0, 0, 0, 7));
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not render stored occasions", error);
            Ui.toast(this, "نمایش مناسبت‌های شخصی انجام نشد");
        }
    }

    private View eventCard(UserEvent event) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(12), dp(8), dp(12), dp(9));
        card.setElevation(dp(1));
        card.setBackground(Ui.roundedStroke(
            event.holiday ? 0xffffe9e8 : Ui.CARD,
            event.holiday ? 0xffbd3947 : Ui.BORDER, 13, this));
        TextView date = Ui.text(this,
            "📅  " + dayLabel(event)
                + (event.year == 0 ? "  •  هر سال"
                : PersianDigits.of("  •  " + event.year
                   + "  •  " + new PersianDate(event.year, event.month, event.day).weekdayName())),
            14f, event.holiday ? 0xffae2538 : Ui.SECONDARY);
        date.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        card.addView(date, Ui.margins(this, -1, -2, 0, 0, 0, 3));
        card.addView(Ui.title(this, event.title, 16));
        LinearLayout flags = new LinearLayout(this);
        flags.setOrientation(LinearLayout.HORIZONTAL);
        flags.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        CheckBox holiday = new CheckBox(this);
        holiday.setText("روز تعطیل");
        holiday.setTextColor(event.holiday ? 0xffae2538 : Ui.MUTED);
        holiday.setTextSize(13);
        holiday.setPadding(0,0,0,0);
        holiday.setChecked(event.holiday);
        holiday.setOnClickListener(v -> changeFlags(event,event.year==0,holiday.isChecked()));
        flags.addView(holiday,new LinearLayout.LayoutParams(0,dp(44),1));
        CheckBox annual = new CheckBox(this);
        annual.setText("تکرار سالانه");
        annual.setTextColor(Ui.TEXT);
        annual.setTextSize(13);
        annual.setPadding(0,0,0,0);
        annual.setChecked(event.year == 0);
        annual.setOnClickListener(v -> changeFlags(event,annual.isChecked(),event.holiday));
        flags.addView(annual,new LinearLayout.LayoutParams(0,dp(44),1));
        card.addView(flags, Ui.margins(this, -1, -2, 0, 1, 0, 0));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button edit = Ui.tonalButton(this, "ویرایش");
        Button delete = Ui.tonalButton(this, "حذف");
        delete.setTextColor(Ui.HOLIDAY);
        delete.setBackgroundTintList(ColorStateList.valueOf(Ui.SURFACE_ALT));
        edit.setMinHeight(0);
        delete.setMinHeight(0);
        edit.setMinimumHeight(0);
        delete.setMinimumHeight(0);
        edit.setTextSize(13.5f);
        delete.setTextSize(13.5f);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(44), 1);
        left.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(44), 1);
        right.setMargins(dp(4), 0, 0, 0);
        actions.addView(edit, left);
        actions.addView(delete, right);
        card.addView(actions, Ui.margins(this, -1, -2, 0, 4, 0, 0));
        edit.setOnClickListener(v -> editExisting(event));
        delete.setOnClickListener(v -> confirmDelete(event));
        return card;
    }

    private void changeFlags(UserEvent current, boolean repeatEveryYear, boolean holiday) {
        try {
            long id = PersonalEventItems.persistedId(current);
            boolean exists = false;
            for (UserEvent saved : database.allUserEvents()) {
                if (saved.f2id == id) { exists = true; break; }
            }
            if (!exists) throw new IllegalStateException("Occasion was removed");
            UserEvent changed = new UserEvent();
            changed.f2id = id;
            changed.year = repeatEveryYear ? 0 :
                current.year == 0
                    ? PersonalEventRecurrence.nextSingleYear(
                        current.month, current.day, PersianDate.todayTehran())
                    : current.year;
            changed.month = current.month;
            changed.day = current.day;
            changed.title = current.title;
            changed.holiday = holiday;
            database.saveUserEvent(changed);
            boolean verified = false;
            for (UserEvent stored : database.allUserEvents()) {
                if (stored.f2id == id && stored.year == changed.year
                    && stored.month == changed.month && stored.day == changed.day
                    && stored.holiday == changed.holiday
                    && changed.title.equals(stored.title)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) throw new IllegalStateException("Occasion flags not persisted");
            updateWidget();
            render();
            if (current.year == 0 && !repeatEveryYear) {
                Ui.toast(this, PersianDigits.of(
                    "تکرار خاموش شد؛ مناسبت فقط برای سال " + changed.year + " ثبت شد"));
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update personal occasion flags", error);
            Ui.toast(this, "ثبت تغییرات مناسبت انجام نشد");
            render();
        }
    }

    private void chooseNewDate() {
        DualDatePicker.show(this, PersianDate.todayTehran(), new DualDatePicker.Result() {
            @Override public void selected(Object picked) {
                if (picked instanceof PersianDate) showEditor(null, (PersianDate) picked);
                else Ui.toast(PersonalEventsActivity.this, "تاریخ انتخابی معتبر نیست");
            }
        });
    }

    private void editExisting(UserEvent e) {
        // The year is not part of the identity of a repeating annual event.
        int year = e.year == 0
            ? PersonalEventRecurrence.nextSingleYear(e.month, e.day, PersianDate.todayTehran())
            : e.year;
        showEditor(e, new PersianDate(year, e.month, e.day));
    }

    private void showEditor(UserEvent existing, PersianDate initialDate) {
        final PersianDate[] chosenDate = {initialDate};
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(10), dp(16), dp(5));
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText title = Ui.input(this, "عنوان مناسبت");
        title.setSingleLine(true);
        if (existing != null) title.setText(existing.title);
        form.addView(title, Ui.margins(this, -1, dp(53), 0, 0, 0, 12));

        Button date = Ui.tonalButton(this, PersianDigits.of("تاریخ: " + initialDate));
        form.addView(date, new LinearLayout.LayoutParams(-1, dp(49)));
        date.setOnClickListener(v -> DualDatePicker.show(this, chosenDate[0], new DualDatePicker.Result() {
            @Override public void selected(Object result) {
                if (result instanceof PersianDate) {
                    chosenDate[0] = (PersianDate) result;
                    date.setText(PersianDigits.of("تاریخ: " + chosenDate[0]));
                }
            }
        }));

        CheckBox annual = new CheckBox(this);
        annual.setText("هر سال تکرار شود");
        annual.setTextColor(Ui.TEXT);
        annual.setChecked(existing == null || existing.year == 0);
        form.addView(annual, Ui.margins(this, -1, dp(51), 0, 8, 0, 0));
        CheckBox holiday = new CheckBox(this);
        holiday.setText("در تقویم به‌عنوان روز تعطیل علامت‌گذاری شود");
        holiday.setTextColor(Ui.TEXT);
        holiday.setChecked(existing != null && existing.holiday);
        form.addView(holiday, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(form);
        AlertDialog editor = new AlertDialog.Builder(this)
            .setTitle(existing == null ? "مناسبت جدید" : "ویرایش مناسبت")
            .setView(scroll).setNegativeButton("انصراف", null)
            .setPositiveButton(existing == null ? "ثبت مناسبت" : "ذخیره تغییرات", null)
            .create();
        editor.setOnShowListener(ignored ->
            editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = title.getText().toString().trim();
                if (value.isEmpty()) {
                    title.setError("عنوان مناسبت را وارد کنید");
                    title.requestFocus();
                    return;
                }
                UserEvent proposal = new UserEvent();
                proposal.f2id = existing == null ? 0 : existing.f2id;
                proposal.year = annual.isChecked() ? 0 : chosenDate[0].year;
                proposal.month = chosenDate[0].month;
                proposal.day = chosenDate[0].day;
                proposal.title = value;
                proposal.holiday = holiday.isChecked();
                UserEvent duplicate = PersonalEventItems.findDuplicate(
                    database.allUserEvents(), proposal);
                if (duplicate != null) {
                    new AlertDialog.Builder(this)
                        .setTitle("مناسبت مشابه")
                        .setMessage("مناسبتی با همین عنوان و تاریخ قبلاً ثبت شده است. بااین‌حال ذخیره شود؟")
                        .setNegativeButton("بازبینی", null)
                        .setPositiveButton("ذخیره با وجود تشابه",
                            (dialog, which) -> persist(proposal, editor))
                        .show();
                } else persist(proposal, editor);
            }));
        editor.show();
    }

    private void persist(UserEvent proposed, AlertDialog editor) {
        try {
            if (proposed.f2id > 0) {
                boolean found = false;
                for (UserEvent existing : database.allUserEvents()) {
                    if (existing.f2id == proposed.f2id) { found = true; break; }
                }
                if (!found) throw new IllegalStateException("Existing event disappeared");
            }
            boolean wasEditing = proposed.f2id > 0;
            long savedId = database.saveUserEvent(proposed);
            boolean verified = false;
            for (UserEvent record : database.allUserEvents()) {
                if (record.f2id == savedId
                    && record.year == proposed.year && record.month == proposed.month
                    && record.day == proposed.day && record.title.equals(proposed.title)
                    && record.holiday == proposed.holiday) {
                    verified = true; break;
                }
            }
            if (!verified) throw new IllegalStateException("Occasion edit was not persisted");
            updateWidget();
            editor.dismiss();
            render();
            Ui.toast(this, wasEditing ? "تغییرات مناسبت ذخیره شد" : "مناسبت ثبت شد");
        } catch (RuntimeException error) {
            Log.e(TAG, "Saving personal occasion failed", error);
            Ui.toast(this, "ذخیره مناسبت انجام نشد");
        }
    }

    private void confirmDelete(UserEvent event) {
        new AlertDialog.Builder(this)
            .setTitle("حذف مناسبت")
            .setMessage("«" + event.title + "» حذف شود؟ فقط همین مناسبت حذف می‌شود.")
            .setNegativeButton("انصراف", null)
            .setPositiveButton("حذف", (dialog, which) -> {
                try {
                    long id = PersonalEventItems.persistedId(event);
                    database.deleteUserEvent(id);
                    for (UserEvent item : database.allUserEvents()) {
                        if (item.f2id == id)
                            throw new IllegalStateException("Occasion was not deleted");
                    }
                    updateWidget();
                    render();
                    Ui.toast(this, "مناسبت حذف شد");
                } catch (RuntimeException error) {
                    Log.e(TAG, "Deleting occasion failed", error);
                    Ui.toast(this, "حذف مناسبت انجام نشد");
                }
            }).show();
    }

    private void updateWidget() {
        try { ShiftWidgetProvider.updateAll(this); }
        catch (RuntimeException error) { Log.w(TAG, "Widget refresh unavailable", error); }
    }
}
