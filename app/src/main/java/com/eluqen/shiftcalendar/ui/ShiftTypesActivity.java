package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;

public final class ShiftTypesActivity extends BaseActivity {
    private static final int[] COLORS = {Color.rgb(216, 148, 47), Color.rgb(201, 83, 122), Color.rgb(83, 103, 199), Color.rgb(23, 126, 121), Color.rgb(126, 87, 194), Color.rgb(0, 137, 123), Color.rgb(230, 94, 79), Color.rgb(63, 81, 181), Color.rgb(46, 125, 50), Color.rgb(96, 112, 128), Color.rgb(121, 85, 72), Color.rgb(0, 121, 107)};
    private LinearLayout list;

    interface ColorListener {
        void selected(int i);
    }

    interface MinuteListener {
        void selected(int i);
    }

    private void chooseColor(int i, ColorListener colorListener) {
        ShiftVisuals.open(this, COLORS, i, colorListener);
    }

    private void updateColorButton(Button button, int i) {
        ShiftVisuals.selection(button, i);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.addView(Ui.toolbar(this, "عنوان، رنگ و ساعت شیفت\u200cها", true));
        ScrollView scrollView = new ScrollView(this);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        this.list = linearLayout2;
        linearLayout2.setOrientation(android.widget.LinearLayout.VERTICAL);
        this.list.setPadding(Ui.dp(this, 14.0f), Ui.dp(this, 14.0f), Ui.dp(this, 14.0f), Ui.dp(this, 26.0f));
        scrollView.addView(this.list);
        setContentView(linearLayout);
    }

    private void render() {
        this.list.removeAllViews();
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.title(this, "تعریف نوبت\u200cهای کاری", 21.0f));
        linearLayoutCard.addView(Ui.text(this, "عنوان کوتاه، رنگ، ساعت شروع و پایان و فعال\u200cبودن یادآوری هر نوع شیفت را تنظیم کنید. دو نوع سفارشی نیز برای چرخه\u200cهای خاص آماده است.", 14.0f, Ui.MUTED), Ui.margins(this, -1, -2, 0, 7, 0, 0));
        this.list.addView(linearLayoutCard, Ui.margins(this, -1, -2, 0, 0, 0, 12));
        for (String str : ShiftExtras.visibleCodes(this)) {
            this.list.addView(profileCard(ShiftPreferences.get(this, str)), Ui.margins(this, -1, -2, 0, 0, 0, 10));
        }
        Button button = Ui.tonalButton(this, "بازگردانی تنظیمات اولیهٔ شیفت\u200cها");
        button.setOnClickListener(ShiftTypesActivity.createOnClickCallback3(this));
        LinearLayout linearLayout = this.list;
        linearLayout.addView(button, Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 6, 0, 0));
        ShiftExtras.decorate(this, linearLayout);
    }

    void handleRenderCallback1(View view) {
        new AlertDialog.Builder(this).setTitle("بازگردانی تنظیمات").setMessage("عنوان\u200cها، رنگ\u200cها، ساعت\u200cها و وضعیت یادآوری انواع شیفت به حالت اولیه برگردد؟").setPositiveButton("بازگردانی", ShiftTypesActivity.createOnClickCallback14(this)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
    }

    void handleRenderCallback0(DialogInterface dialogInterface, int i) {
        ShiftPreferences.reset(this);
        afterChanged();
    }

    private View profileCard(ShiftPreferences.Profile profile) {
        String str;
        LinearLayout linearLayoutCard = Ui.card(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout.setGravity(16);
        linearLayout.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        TextView textView = new TextView(this);
        textView.setBackground(Ui.rounded(profile.color, 12, this));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(Ui.dp(this, 42.0f), Ui.dp(this, 42.0f)));
        TextView textViewTitle = Ui.title(this, profile.title + "  (" + profile.shortTitle + ")", 18.0f);
        linearLayout.addView(textViewTitle, Ui.margins(this, 0, Ui.dp(this, 46.0f), 10, 0, 0, 0));
        ((LinearLayout.LayoutParams) textViewTitle.getLayoutParams()).weight = 1.0f;
        linearLayoutCard.addView(linearLayout);
        if (profile.working) {
            str = "ساعت: " + ShiftPreferences.formatMinute(profile.startMinute) + " تا " + ShiftPreferences.formatMinute(profile.endMinute);
        } else {
            str = "نوع غیرکاری / استراحت";
        }
        linearLayoutCard.addView(Ui.text(this, str + (profile.reminder ? " • یادآوری فعال" : " • بدون یادآوری"), 14.0f, Ui.MUTED), Ui.margins(this, -1, -2, 0, 8, 0, 0));
        Button button = Ui.tonalButton(this, "ویرایش این نوع شیفت");
        button.setOnClickListener(ShiftTypesActivity.createOnClickCallback4(this, profile));
        linearLayoutCard.addView(button, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 10, 0, 0));
        return linearLayoutCard;
    }

    void handleCallback135(ShiftPreferences.Profile profile, View view) {
        edit(profile);
    }

    private void edit(ShiftPreferences.Profile profile) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 18.0f), Ui.dp(this, 8.0f), Ui.dp(this, 18.0f), Ui.dp(this, 4.0f));
        EditText editTextInput = Ui.input(this, "عنوان کامل");
        editTextInput.setText(profile.title);
        EditText editTextInput2 = Ui.input(this, "عنوان کوتاه، حداکثر سه حرف");
        editTextInput2.setText(profile.shortTitle);
        CheckBox checkBoxCheck = check("این نوع، شیفت کاری است", profile.working);
        CheckBox checkBoxCheck2 = check("برای این نوع شیفت یادآوری ساخته شود", profile.reminder);
        int[] iArr = {profile.startMinute < 0 ? 480 : profile.startMinute};
        int[] iArr2 = {profile.endMinute < 0 ? 960 : profile.endMinute};
        int[] iArr3 = {profile.color};
        Button button = Ui.tonalButton(this, "شروع: " + ShiftPreferences.formatMinute(iArr[0]));
        Button button2 = Ui.tonalButton(this, "پایان: " + ShiftPreferences.formatMinute(iArr2[0]));
        button.setOnClickListener(ShiftTypesActivity.createOnClickCallback6(this, iArr, button));
        button2.setOnClickListener(ShiftTypesActivity.createOnClickCallback7(this, iArr2, button2));
        Button button3 = Ui.tonalButton(this, "انتخاب رنگ");
        updateColorButton(button3, iArr3[0]);
        button3.setOnClickListener(ShiftTypesActivity.createOnClickCallback8(this, iArr3, button3));
        checkBoxCheck.setOnCheckedChangeListener(ShiftTypesActivity.createOnCheckedChangedCallback9(button, button2, checkBoxCheck2));
        button.setEnabled(checkBoxCheck.isChecked());
        button2.setEnabled(checkBoxCheck.isChecked());
        checkBoxCheck2.setEnabled(checkBoxCheck.isChecked());
        linearLayout.addView(editTextInput, field());
        linearLayout.addView(editTextInput2, field());
        linearLayout.addView(checkBoxCheck, new LinearLayout.LayoutParams(-1, Ui.dp(this, 44.0f)));
        linearLayout.addView(checkBoxCheck2, new LinearLayout.LayoutParams(-1, Ui.dp(this, 44.0f)));
        linearLayout.addView(button, field());
        linearLayout.addView(button2, field());
        linearLayout.addView(button3, field());
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("ویرایش نوع شیفت").setView(linearLayout).setPositiveButton("ذخیره", (DialogInterface.OnClickListener) null).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(ShiftTypesActivity.createOnShowCallback10(this, alertDialogCreate, editTextInput, editTextInput2, profile, checkBoxCheck, iArr, iArr2, iArr3, checkBoxCheck2));
        alertDialogCreate.show();
    }

    void handleEditCallback4(int[] iArr, Button button, View view) {
        chooseTime(iArr[0], ShiftTypesActivity.createSelectedCallback11(iArr, button));
    }

    static void handleEditCallback3(int[] iArr, Button button, int i) {
        iArr[0] = i;
        button.setText("شروع: " + ShiftPreferences.formatMinute(i));
    }

    void handleEditCallback6(int[] iArr, Button button, View view) {
        chooseTime(iArr[0], ShiftTypesActivity.createSelectedCallback13(iArr, button));
    }

    static void handleEditCallback5(int[] iArr, Button button, int i) {
        iArr[0] = i;
        button.setText("پایان: " + ShiftPreferences.formatMinute(i));
    }

    void handleEditCallback8(int[] iArr, Button button, View view) {
        chooseColor(iArr[0], ShiftTypesActivity.createSelectedCallback2(this, iArr, button));
    }

    void handleEditCallback7(int[] iArr, Button button, int i) {
        iArr[0] = i;
        updateColorButton(button, i);
    }

    static void handleEditCallback9(Button button, Button button2, CheckBox checkBox, CompoundButton compoundButton, boolean z) {
        button.setEnabled(z);
        button2.setEnabled(z);
        checkBox.setEnabled(z);
        if (z) {
            return;
        }
        checkBox.setChecked(false);
    }

    void handleEditCallback11(AlertDialog alertDialog, EditText editText, EditText editText2, ShiftPreferences.Profile profile, CheckBox checkBox, int[] iArr, int[] iArr2, int[] iArr3, CheckBox checkBox2, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(ShiftTypesActivity.createOnClickCallback1(this, editText, editText2, profile, checkBox, iArr, iArr2, iArr3, checkBox2, alertDialog));
    }

    void handleEditCallback10(EditText editText, EditText editText2, ShiftPreferences.Profile profile, CheckBox checkBox, int[] iArr, int[] iArr2, int[] iArr3, CheckBox checkBox2, AlertDialog alertDialog, View view) {
        String strTrim = editText.getText().toString().trim();
        String strTrim2 = editText2.getText().toString().trim();
        if (strTrim.isEmpty() || strTrim2.isEmpty()) {
            Ui.toast(this, "عنوان کامل و کوتاه باید وارد شود");
            return;
        }
        if (strTrim.length() > 24) {
            editText.setError("عنوان کامل حداکثر ۲۴ نویسه");
            editText.requestFocus();
            return;
        }
        if (strTrim2.length() > 3) {
            editText2.setError("عنوان کوتاه حداکثر ۳ نویسه");
            editText2.requestFocus();
            return;
        }
        ShiftPreferences.save(this, new ShiftPreferences.Profile(profile.code, strTrim, strTrim2, checkBox.isChecked() ? iArr[0] : -1, checkBox.isChecked() ? iArr2[0] : -1, iArr3[0], checkBox.isChecked(), checkBox.isChecked() && checkBox2.isChecked()));
        alertDialog.dismiss();
        afterChanged();
    }

    private void afterChanged() {
        render();
        ShiftWidgetProvider.updateAll(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(ShiftTypesActivity.createRunCallback0(this), "eluqen-shift-calendar-shift-types").start();
        }
    }

    void handleCallback134() {
        ReminderScheduler.sync(this);
    }

    private void chooseTime(int i, MinuteListener minuteListener) {
        new TimePickerDialog(this, ShiftTypesActivity.createOnTimeSetCallback12(minuteListener), i / 60, i % 60, true).show();
    }

    static void handleChooseTimeCallback13(MinuteListener minuteListener, TimePicker timePicker, int i, int i2) {
        minuteListener.selected((i * 60) + i2);
    }

    static void handleChooseColorCallback14(ColorListener colorListener, DialogInterface dialogInterface, int i) {
        colorListener.selected(COLORS[i]);
        dialogInterface.dismiss();
    }

    private CheckBox check(String str, boolean z) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(str);
        checkBox.setTextSize(14.0f);
        checkBox.setTextColor(Ui.TEXT);
        checkBox.setChecked(z);
        checkBox.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        return checkBox;
    }

    private LinearLayout.LayoutParams field() {
        return Ui.margins(this, -1, Ui.dp(this, 52.0f), 0, 5, 0, 5);
    }

    private static TimePickerDialog.OnTimeSetListener createOnTimeSetCallback12(final ShiftTypesActivity.MinuteListener capture0) {
        return (timePicker, i, i2) -> {
        ShiftTypesActivity.handleChooseTimeCallback13(capture0, timePicker, i, i2);
        };
    }

    private static DialogInterface.OnShowListener createOnShowCallback10(final ShiftTypesActivity capture0, final AlertDialog capture1, final EditText capture2, final EditText capture3, final ShiftPreferences.Profile capture4, final CheckBox capture5, final int[] capture6, final int[] capture7, final int[] capture8, final CheckBox capture9) {
        return (dialogInterface) -> {
        capture0.handleEditCallback11(capture1, capture2, capture3, capture4, capture5, capture6, capture7, capture8, capture9, dialogInterface);
        };
    }

    private static ShiftTypesActivity.MinuteListener createSelectedCallback11(final int[] capture0, final Button capture1) {
        return (i) -> {
        ShiftTypesActivity.handleEditCallback3(capture0, capture1, i);
        };
    }

    private static View.OnClickListener createOnClickCallback1(final ShiftTypesActivity capture0, final EditText capture1, final EditText capture2, final ShiftPreferences.Profile capture3, final CheckBox capture4, final int[] capture5, final int[] capture6, final int[] capture7, final CheckBox capture8, final AlertDialog capture9) {
        return (view) -> {
        capture0.handleEditCallback10(capture1, capture2, capture3, capture4, capture5, capture6, capture7, capture8, capture9, view);
        };
    }

    private static ShiftTypesActivity.MinuteListener createSelectedCallback13(final int[] capture0, final Button capture1) {
        return (i) -> {
        ShiftTypesActivity.handleEditCallback5(capture0, capture1, i);
        };
    }

    private static CompoundButton.OnCheckedChangeListener createOnCheckedChangedCallback9(final Button capture0, final Button capture1, final CheckBox capture2) {
        return (compoundButton, z) -> {
        ShiftTypesActivity.handleEditCallback9(capture0, capture1, capture2, compoundButton, z);
        };
    }

    private static View.OnClickListener createOnClickCallback8(final ShiftTypesActivity capture0, final int[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleEditCallback8(capture1, capture2, view);
        };
    }

    private static View.OnClickListener createOnClickCallback3(final ShiftTypesActivity capture0) {
        return (view) -> {
        capture0.handleRenderCallback1(view);
        };
    }

    private static ShiftTypesActivity.ColorListener createSelectedCallback2(final ShiftTypesActivity capture0, final int[] capture1, final Button capture2) {
        return (i) -> {
        capture0.handleEditCallback7(capture1, capture2, i);
        };
    }

    private static View.OnClickListener createOnClickCallback4(final ShiftTypesActivity capture0, final ShiftPreferences.Profile capture1) {
        return (view) -> {
        capture0.handleCallback135(capture1, view);
        };
    }

    private static Runnable createRunCallback0(final ShiftTypesActivity capture0) {
        return () -> {
        capture0.handleCallback134();
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback14(final ShiftTypesActivity capture0) {
        return (dialogInterface, i) -> {
        capture0.handleRenderCallback0(dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback6(final ShiftTypesActivity capture0, final int[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleEditCallback4(capture1, capture2, view);
        };
    }

    private static View.OnClickListener createOnClickCallback7(final ShiftTypesActivity capture0, final int[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleEditCallback6(capture1, capture2, view);
        };
    }
}
