package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.BackupService;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.data.UserEvent;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.hijri.HijriResolver;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.update.DataUpdateJobService;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;
import java.util.List;

public final class SettingsActivity extends BaseActivity {
    private static final int CREATE_BACKUP = 801;
    private static final int OPEN_BACKUP = 802;
    private boolean bindingSpinner;
    private AppDatabase database;
    private TextView hijriDetails;
    private TextView planDetails;
    private Spinner planSpinner;
    private List<ShiftPlan> plans;

    interface IoTask {
        String run() throws Exception;
    }

    void handleBuildUiCallback0(View view) {
        CompanyPlans.createPattern(this);
    }

    void handleBuildUiCallback1(View view) {
        CompanyPlans.editShown(this);
    }

    void handleBuildUiCallback2(View view) {
        PlanManagement.confirmDeleteActive(this);
    }

    static boolean access$000(SettingsActivity settingsActivity) {
        return settingsActivity.bindingSpinner;
    }

    static List access$100(SettingsActivity settingsActivity) {
        return settingsActivity.plans;
    }

    static AppDatabase access$200(SettingsActivity settingsActivity) {
        return settingsActivity.database;
    }

    static void access$300(SettingsActivity settingsActivity) {
        settingsActivity.refreshDetails();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.database = AppDatabase.get(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPlans();
        refreshDetails();
        CompanyPlans.attach(this);
        polishManagement();
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.addView(Ui.toolbar(this, "تنظیمات", true));
        ScrollView scrollView = new ScrollView(this);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout2.setPadding(Ui.dp(this, 14.0f), Ui.dp(this, 12.0f), Ui.dp(this, 14.0f), Ui.dp(this, 24.0f));
        scrollView.addView(linearLayout2);
        linearLayout2.addView(Ui.title(this, "الگوی شیفت", 20.0f), sectionParams());
        LinearLayout linearLayoutCard = Ui.card(this);
        Spinner spinner = new Spinner(this);
        this.planSpinner = spinner;
        spinner.setTextDirection(4);
        this.planSpinner.setOnItemSelectedListener(new PlanSelectionListener());
        linearLayoutCard.addView(this.planSpinner, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56.0f)));
        TextView textViewText = Ui.text(this, "", 14.0f, Ui.MUTED);
        this.planDetails = textViewText;
        linearLayoutCard.addView(textViewText, Ui.margins(this, -1, -2, 0, 8, 0, 8));
        LinearLayout linearLayoutHorizontal = horizontal();
        Button button = Ui.button(this, "الگوی جدید این محل کار");
        button.setOnClickListener(SettingsActivity.createOnClickCallback15(this));
        Button button2 = Ui.tonalButton(this, "ویرایش فعال");
        button2.setOnClickListener(SettingsActivity.createOnClickCallback20(this));
        linearLayoutHorizontal.addView(button, weightedButton());
        linearLayoutHorizontal.addView(button2, weightedButton());
        linearLayoutCard.addView(linearLayoutHorizontal);
        Button button3 = Ui.tonalButton(this, "حذف الگوی فعال");
        button3.setOnClickListener(SettingsActivity.createOnClickCallback21(this));
        linearLayoutCard.addView(button3, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 8, 0, 0));
        linearLayout2.addView(linearLayoutCard);
        linearLayout2.addView(Ui.title(this, "تعریف نوبت\u200cها و یادآوری", 20.0f), sectionParams());
        LinearLayout linearLayoutCard2 = Ui.card(this);
        linearLayoutCard2.addView(Ui.text(this, "عنوان، رنگ، ساعت شروع و پایان شش نوع شیفت را تنظیم کنید و مشخص کنید کدام نوبت\u200cها اعلان داشته باشند.", 14.0f, Ui.MUTED));
        Button button4 = Ui.tonalButton(this, "عنوان، رنگ و ساعت شیفت\u200cها");
        button4.setOnClickListener(SettingsActivity.createOnClickCallback22(this));
        linearLayoutCard2.addView(button4, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 11, 0, 0));
        Button button5 = Ui.button(this, "تنظیم یادآوری شروع شیفت");
        button5.setOnClickListener(SettingsActivity.createOnClickCallback23(this));
        linearLayoutCard2.addView(button5, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 8, 0, 0));
        linearLayout2.addView(linearLayoutCard2);
        linearLayout2.addView(Ui.title(this, "تنظیمات عمومی", 20.0f), sectionParams());
        LinearLayout linearLayoutCard3 = Ui.card(this);
        linearLayoutCard3.addView(Ui.text(this, "صفحهٔ شروع برنامه", 14.0f, Ui.MUTED));
        Spinner spinner2 = new Spinner(this);
        String[] strArr = {Prefs.SCREEN_CALENDAR, Prefs.SCREEN_FEATURES, Prefs.SCREEN_FOUR_SHIFT, Prefs.SCREEN_CARDEX, "settings"};
        spinner2.setAdapter((SpinnerAdapter) FancyDialogs.spinner(this, new String[]{"تقویم ماهانه", "همهٔ امکانات", "تقویم چهارشیفت", "کارکرد", "تنظیمات"}));
        int i = 0;
        for (int i2 = 0; i2 < 5; i2++) {
            if (strArr[i2].equals(Prefs.defaultScreen(this))) {
                i = i2;
            }
        }
        spinner2.setSelection(i, false);
        spinner2.setOnItemSelectedListener(new DefaultScreenSelectionListener(strArr));
        linearLayoutCard3.addView(spinner2, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56.0f)));
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("بازهٔ ۱۲ تا ۱۳ از جمع استراحت و مرخصی کسر شود");
        checkBox.setTextColor(Ui.TEXT);
        checkBox.setTextSize(14.0f);
        checkBox.setChecked(Prefs.excludeLunchHour(this));
        checkBox.setOnCheckedChangeListener(SettingsActivity.createOnCheckedChangedCallback24(this));
        linearLayoutCard3.addView(checkBox, Ui.margins(this, -1, Ui.dp(this, 52.0f), 0, 7, 0, 0));
        Button button6 = Ui.tonalButton(this, "سفارشی\u200cسازی ویجت");
        button6.setOnClickListener(SettingsActivity.createOnClickCallback25(this));
        linearLayoutCard3.addView(button6, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 8, 0, 0));
        linearLayout2.addView(linearLayoutCard3);
        linearLayout2.addView(Ui.title(this, "تقویم و مناسبت\u200cها", 20.0f), sectionParams());
        LinearLayout linearLayoutCard4 = Ui.card(this);
        TextView textViewText2 = Ui.text(this, "", 14.0f, Ui.MUTED);
        this.hijriDetails = textViewText2;
        linearLayoutCard4.addView(textViewText2);
        Button button8 = Ui.tonalButton(this, "تنظیم دستی تاریخ قمری");
        button8.setOnClickListener(SettingsActivity.createOnClickCallback27(this));
        linearLayoutCard4.addView(button8, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 10, 0, 0));
        Button button9 = Ui.tonalButton(this, "مناسبت\u200cهای شخصی");
        button9.setOnClickListener(SettingsActivity.createOnClickCallback28(this));
        linearLayoutCard4.addView(button9, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 8, 0, 0));
        linearLayout2.addView(linearLayoutCard4);
        linearLayout2.addView(Ui.title(this, "به\u200cروزرسانی", 20.0f), sectionParams());
        LinearLayout linearLayoutCard5 = Ui.card(this);
        linearLayoutCard5.addView(Ui.text(this, "مدیریت منبع دریافت نسخه برنامه، بررسی خودکار و به\u200cروزرسانی مستقل بانک مناسبت\u200cها.", 14.0f, Ui.MUTED));
        Button button10 = Ui.button(this, "باز کردن مرکز به\u200cروزرسانی");
        button10.setOnClickListener(SettingsActivity.createOnClickCallback16(this));
        linearLayoutCard5.addView(button10, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 12, 0, 0));
        linearLayout2.addView(linearLayoutCard5);
        linearLayout2.addView(Ui.title(this, "پشتیبان\u200cگیری و بازیابی", 20.0f), sectionParams());
        LinearLayout linearLayoutCard6 = Ui.card(this);
        linearLayoutCard6.addView(Ui.text(this, "فایل پشتیبان تقویم شیفت همه الگوها، کارکرد، همکاران، تنظیمات تقویم و مناسبت\u200cهای شخصی را نگه می\u200cدارد.", 14.0f, Ui.MUTED));
        Button button11 = Ui.button(this, "ساخت فایل پشتیبان");
        button11.setOnClickListener(SettingsActivity.createOnClickCallback17(this));
        linearLayoutCard6.addView(button11, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 12, 0, 0));
        Button button12 = Ui.tonalButton(this, "بازیابی فایل پشتیبان");
        button12.setOnClickListener(SettingsActivity.createOnClickCallback18(this));
        linearLayoutCard6.addView(button12, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 8, 0, 0));
        linearLayout2.addView(linearLayoutCard6);
        linearLayout2.addView(Ui.title(this, "درباره", 20.0f), sectionParams());
        LinearLayout linearLayoutCard7 = Ui.card(this);
        linearLayoutCard7.addView(Ui.text(this, "بدون تبلیغ و رهگیری؛ اطلاعات شخصی فقط روی دستگاه می\u200cمانند. اینترنت تنها برای به\u200cروزرسانی\u200cهای انتخابی استفاده می\u200cشود. منطقه زمانی «امروز»: تهران.", 14.0f, Ui.MUTED));
        Button button13 = Ui.tonalButton(this, "اطلاعات نسخه و موتور تاریخ");
        button13.setOnClickListener(SettingsActivity.createOnClickCallback19(this));
        linearLayoutCard7.addView(button13, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 10, 0, 0));
        linearLayout2.addView(linearLayoutCard7);
        ScreenPolish.settings(linearLayout);
        polishManagement();
        setContentView(linearLayout);
    }

    private void polishManagement() {
        if (!(planSpinner.getParent() instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) planSpinner.getParent();
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        card.setBackground(SettingsFeaturesUi.shape(this, 0xffffffff, SettingsFeaturesUi.BORDER, 16));
        card.setElevation(Ui.dp(this, 1));
        planSpinner.setMinimumHeight(Ui.dp(this, 56));
        planSpinner.setBackground(SettingsFeaturesUi.shape(this, 0xfff2f6fa, SettingsFeaturesUi.BORDER, 12));
        planSpinner.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        planDetails.setTextSize(13);
        planDetails.setTextColor(SettingsFeaturesUi.MUTED);
        planDetails.setLineSpacing(Ui.dp(this, 3), 1);
    }

    class PlanSelectionListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }

        PlanSelectionListener() {
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            if (SettingsActivity.access$000(SettingsActivity.this) || SettingsActivity.access$100(SettingsActivity.this) == null || i < 0 || i >= SettingsActivity.access$100(SettingsActivity.this).size()) {
                return;
            }
            CompanyPlans.stagePlan(SettingsActivity.this, SettingsActivity.access$100(SettingsActivity.this).get(i));
            SettingsActivity.access$300(SettingsActivity.this);
        }

        void handleCallback129() {
            ReminderScheduler.sync(SettingsActivity.this);
        }
    }

    void handleBuildUiCallback3(View view) {
        startActivity(new Intent(this, (Class<?>) ShiftTypesActivity.class));
    }

    void handleBuildUiCallback4(View view) {
        startActivity(new Intent(this, (Class<?>) ReminderSettingsActivity.class));
    }

    class DefaultScreenSelectionListener implements AdapterView.OnItemSelectedListener {
        final String[] val$screenKeys;

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }

        DefaultScreenSelectionListener(String[] strArr) {
            this.val$screenKeys = strArr;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            Prefs.setDefaultScreen(SettingsActivity.this, this.val$screenKeys[i]);
        }
    }

    void handleBuildUiCallback5(CompoundButton compoundButton, boolean z) {
        Prefs.setExcludeLunchHour(this, z);
    }

    void handleBuildUiCallback6(View view) {
        startActivity(new Intent(this, (Class<?>) WidgetSettingsActivity.class));
    }

    void handleBuildUiCallback8(View view) {
        chooseHijriOffset();
    }

    void handleBuildUiCallback9(View view) {
        HubGeneral.openPersonalEvents(this);
    }

    void handleBuildUiCallback10(View view) {
        startActivity(new Intent(this, (Class<?>) UpdateCenterActivity.class));
    }

    void handleBuildUiCallback11(View view) {
        createBackup();
    }

    void handleBuildUiCallback12(View view) {
        openBackup();
    }

    void handleBuildUiCallback13(View view) {
        startActivity(new Intent(this, (Class<?>) AboutActivity.class));
    }

    private void bindPlans() {
        this.bindingSpinner = true;
        List listFilter = CompanyPlans.filter(this, this.database.plans());
        this.plans = listFilter;
        String[] strArr = new String[listFilter.size()];
        int i = 0;
        for (int i2 = 0; i2 < this.plans.size(); i2++) {
            strArr[i2] = CompanyPlans.label(this.plans.get(i2));
            if (CompanyPlans.isShown(this, this.plans.get(i2))) {
                i = i2;
            }
        }
        ArrayAdapter arrayAdapter = FancyDialogs.spinner(this, strArr);
        this.planSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        this.planSpinner.setSelection(i, false);
        this.bindingSpinner = false;
    }

    private void refreshDetails() {
        ShiftPlan shiftPlan = (ShiftPlan) CompanyPlans.shownPlan(this);
        PersianDate persianDateFromJdn = JalaliCalendar.fromJdn(shiftPlan.anchorJdn);
        String[] pattern = ShiftEngine.parsePattern(shiftPlan.pattern);
        StringBuilder sb = new StringBuilder();
        for (String str : pattern) {
            if (sb.length() > 0) {
                sb.append("، ");
            }
            sb.append(ShiftPreferences.title(this, str));
        }
        this.planDetails.setText(PersianDigits.of("شروع چرخه: " + persianDateFromJdn + "\nچرخه: " + ((Object) sb)));
        PersianDate hijriToday = PersianDate.todayTehran();
        EventRepository hijriRepository = EventRepository.get(this);
        int manualOffset = Prefs.hijriManualOffset(this);
        boolean authoritative = hijriRepository.hasAuthoritativeHijri(
                hijriToday.plusDays(manualOffset));
        HijriDate resolvedHijri = HijriResolver.resolve(this, hijriToday);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("اصلاح خودکار بانک: ")
                .append(authoritative
                        ? "شروع ماه رسمی برای امروز ثبت شده"
                        : "محاسباتی؛ شروع ماه رسمی برای امروز ثبت نشده");
        if (hijriRepository.hasHijriPolicy()) {
            sb2.append("  •  نسخه ")
                    .append(hijriRepository.hijriCorrectionVersion());
        }
        sb2.append("\nاصلاح دستی: ")
                .append(manualOffset > 0 ? "+" : "")
                .append(manualOffset)
                .append(" روز");
        sb2.append("\nتاریخ قمری امروز: ")
                .append(resolvedHijri.displayLong());
        this.hijriDetails.setText(PersianDigits.of(sb2.toString()));
    }

    private void editPlan(ShiftPlan shiftPlan) {
        ShiftPlan shiftPlan2 = shiftPlan == null ? new ShiftPlan() : shiftPlan;
        PersianDate[] persianDateArr = new PersianDate[1];
        persianDateArr[0] = shiftPlan == null ? PersianDate.todayTehran() : JalaliCalendar.fromJdn(shiftPlan.anchorJdn);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 18.0f), Ui.dp(this, 8.0f), Ui.dp(this, 18.0f), Ui.dp(this, 4.0f));
        EditText editTextInput = Ui.input(this, "نام شرکت / محل کار");
        editTextInput.setText(shiftPlan == null ? CompanyPlans.defaultCompany(this) : shiftPlan.company);
        EditText editTextInput2 = Ui.input(this, "نام گروه یا شیفت (مثلاً A)");
        editTextInput2.setText(shiftPlan == null ? "A" : shiftPlan.team);
        Button button = Ui.tonalButton(this, "");
        button.setText(PersianDigits.of("شروع چرخه: " + persianDateArr[0]));
        button.setOnClickListener(SettingsActivity.createOnClickCallback1(this, persianDateArr, button));
        String str = "D,D,E,E,O,N,N,O";
        EditText editTextInput3 = Ui.input(this, "D,D,E,E,O,N,N,O");
        if (shiftPlan != null) {
            str = shiftPlan.pattern;
        }
        editTextInput3.setText(str);
        TextView textViewText = Ui.text(this, "با دکمه\u200cها چرخه را بسازید یا کدها را مستقیماً ویرایش کنید. چرخه بدون محدودیت سال تکرار می\u200cشود.", 13.0f, Ui.MUTED);
        LinearLayout linearLayoutHorizontal = horizontal();
        linearLayoutHorizontal.addView(patternButton(editTextInput3, ShiftEngine.DAY), weightedButton());
        linearLayoutHorizontal.addView(patternButton(editTextInput3, ShiftEngine.EVENING), weightedButton());
        linearLayoutHorizontal.addView(patternButton(editTextInput3, ShiftEngine.NIGHT), weightedButton());
        LinearLayout linearLayoutHorizontal2 = horizontal();
        linearLayoutHorizontal2.addView(patternButton(editTextInput3, ShiftEngine.OFF), weightedButton());
        Button button2 = Ui.tonalButton(this, "پاک\u200cکردن چرخه");
        button2.setOnClickListener(SettingsActivity.createOnClickCallback2(editTextInput3));
        linearLayout.addView(editTextInput, dialogField());
        linearLayout.addView(editTextInput2, dialogField());
        linearLayout.addView(button, dialogField());
        linearLayout.addView(editTextInput3, dialogField());
        linearLayout.addView(textViewText, dialogField());
        linearLayout.addView(linearLayoutHorizontal, Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 4, 0, 0));
        linearLayout.addView(linearLayoutHorizontal2, Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 4, 0, 0));
        linearLayout.addView(button2, Ui.margins(this, -1, Ui.dp(this, 46.0f), 0, 5, 0, 0));
        AlertDialog.Builder title = new AlertDialog.Builder(this).setTitle(shiftPlan == null ? "الگوی شیفت جدید" : "ویرایش الگوی شیفت");
        ShiftExtras.patternButtons(this, linearLayout, editTextInput3);
        AlertDialog alertDialogCreate = title.setView(linearLayout).setPositiveButton("ذخیره", (DialogInterface.OnClickListener) null).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(SettingsActivity.createOnShowCallback3(this, alertDialogCreate, editTextInput, editTextInput2, editTextInput3, shiftPlan2, persianDateArr, shiftPlan));
        alertDialogCreate.show();
    }

    void handleEditPlanCallback15(PersianDate[] persianDateArr, Button button, View view) {
        PersianDatePicker.show(this, persianDateArr[0], SettingsActivity.createOnPickedCallback14(persianDateArr, button));
    }

    static void handleEditPlanCallback14(PersianDate[] persianDateArr, Button button, PersianDate persianDate) {
        persianDateArr[0] = persianDate;
        button.setText(PersianDigits.of("شروع چرخه: " + persianDate));
    }

    static void handleEditPlanCallback16(EditText editText, View view) {
        editText.setText("");
    }

    void handleEditPlanCallback19(AlertDialog alertDialog, EditText editText, EditText editText2, EditText editText3, ShiftPlan shiftPlan, PersianDate[] persianDateArr, ShiftPlan shiftPlan2, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(SettingsActivity.createOnClickCallback7(this, editText, editText2, editText3, shiftPlan, persianDateArr, shiftPlan2, alertDialog));
    }

    void handleEditPlanCallback18(EditText editText, EditText editText2, EditText editText3, ShiftPlan shiftPlan, PersianDate[] persianDateArr, ShiftPlan shiftPlan2, AlertDialog alertDialog, View view) {
        String strTrim = editText.getText().toString().trim();
        String strTrim2 = editText2.getText().toString().trim();
        String strTrim3 = editText3.getText().toString().trim();
        if (strTrim.isEmpty() || strTrim2.isEmpty() || strTrim3.isEmpty()) {
            Ui.toast(this, "نام، گروه و چرخه باید تکمیل شوند");
            return;
        }
        if (ShiftEngine.parsePattern(strTrim3).length > 120) {
            Ui.toast(this, "چرخه نباید بیش از ۱۲۰ روز باشد");
            return;
        }
        shiftPlan.company = strTrim;
        shiftPlan.team = strTrim2;
        boolean z = false;
        shiftPlan.anchorJdn = persianDateArr[0].toJdn();
        shiftPlan.pattern = strTrim3;
        if (shiftPlan2 != null && shiftPlan2.active) {
            z = true;
        }
        shiftPlan.active = z;
        this.database.savePlan(shiftPlan);
        alertDialog.dismiss();
        CompanyPlans.afterPlanMutation(this, shiftPlan);
        bindPlans();
        refreshDetails();
        CompanyPlans.attach(this);
        polishManagement();
        ShiftWidgetProvider.updateAll(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(SettingsActivity.createRunCallback30(this), "eluqen-shift-calendar-save-plan-reminder").start();
        }
    }

    void handleEditPlanCallback17() {
        ReminderScheduler.sync(this);
    }

    private void deleteActivePlan() {
        ShiftPlan active = (ShiftPlan) CompanyPlans.shownPlan(this);
        if (this.plans == null || this.plans.size() <= 1) {
            Ui.toast(this, "حداقل یک الگوی شیفت باید باقی بماند");
            return;
        }
        ShiftPlan replacement = null;
        for (ShiftPlan candidate : this.plans) {
            if (candidate.id != active.id) {
                replacement = candidate;
                break;
            }
        }
        if (replacement == null) {
            Ui.toast(this, "الگوی جایگزین پیدا نشد");
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("الگوی «" + active.displayName() + "» حذف و «" + replacement.displayName() + "» فعال شود؟")
                .setPositiveButton("حذف", SettingsActivity.createOnClickCallback29(this, replacement, active))
                .setNegativeButton("انصراف", (DialogInterface.OnClickListener) null)
                .show();
    }

    void handleCallback123(ShiftPlan shiftPlan, ShiftPlan shiftPlan2, DialogInterface dialogInterface, int i) {
        this.database.setActivePlan(shiftPlan.id);
        this.database.deletePlan(shiftPlan2.id);
        bindPlans();
        refreshDetails();
        DataUpdateJobService.syncSchedule(this);
        ShiftWidgetProvider.updateAll(this);
        if (Prefs.reminderEnabled(this)) {
            new Thread(SettingsActivity.createRunCallback4(this), "eluqen-shift-calendar-delete-plan-reminder").start();
        }
    }

    void handleCallback122() {
        ReminderScheduler.sync(this);
    }

    private void chooseHijriOffset() {
        HijriCorrectionDialog.show(this, () -> {
            refreshDetails();
            ShiftWidgetProvider.updateAll(this);
        });
    }

    private void showEvents() {
        List<UserEvent> listAllUserEvents = this.database.allUserEvents();
        String[] strArr = new String[listAllUserEvents.size()];
        for (int i = 0; i < listAllUserEvents.size(); i++) {
            UserEvent userEvent = listAllUserEvents.get(i);
            String strValueOf = userEvent.year == 0 ? "هر سال" : String.valueOf(userEvent.year);
            StringBuilder sb = new StringBuilder();
            sb.append(strValueOf);
            sb.append(" — ");
            sb.append(userEvent.month);
            sb.append("/");
            sb.append(userEvent.day);
            sb.append(" — ");
            sb.append(userEvent.title);
            sb.append(userEvent.holiday ? " (تعطیل)" : "");
            strArr[i] = PersianDigits.of(sb.toString());
        }
        new AlertDialog.Builder(this).setTitle("مناسبت\u200cهای شخصی").setAdapter(FancyDialogs.list(this, strArr),
                SettingsActivity.createOnClickCallback31(this, listAllUserEvents)).setPositiveButton("افزودن", SettingsActivity.createOnClickCallback32(this)).setNegativeButton("بستن", (DialogInterface.OnClickListener) null).create().show();
    }

    void handleCallback127(List list, DialogInterface dialogInterface, int i) {
        confirmDeleteEvent((UserEvent) list.get(i));
    }

    void handleCallback128(DialogInterface dialogInterface, int i) {
        addEvent();
    }

    private void addEvent() {
        PersianDatePicker.show(this, PersianDate.todayTehran(), SettingsActivity.createOnPickedCallback12(this));
    }

    void handleAddEventCallback26(PersianDate persianDate) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 18.0f), Ui.dp(this, 8.0f), Ui.dp(this, 18.0f), Ui.dp(this, 4.0f));
        EditText editTextInput = Ui.input(this, "عنوان مناسبت");
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("هر سال تکرار شود");
        checkBox.setChecked(true);
        CheckBox checkBox2 = new CheckBox(this);
        checkBox2.setText("روز تعطیل نمایش داده شود");
        linearLayout.addView(editTextInput, dialogField());
        linearLayout.addView(checkBox);
        linearLayout.addView(checkBox2);
        AlertDialog eventDialog = new AlertDialog.Builder(this)
                .setTitle(PersianDigits.of("مناسبت " + persianDate))
                .setView(linearLayout)
                .setPositiveButton("ذخیره", (DialogInterface.OnClickListener) null)
                .setNegativeButton("انصراف", (DialogInterface.OnClickListener) null)
                .create();
        eventDialog.setOnShowListener(ignored ->
                eventDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String value = editTextInput.getText().toString().trim();
                            if (value.isEmpty()) {
                                editTextInput.setError("عنوان مناسبت را وارد کنید");
                                editTextInput.requestFocus();
                                return;
                            }
                            handleAddEventCallback25(
                                    editTextInput, checkBox, persianDate, checkBox2,
                                    eventDialog, AlertDialog.BUTTON_POSITIVE);
                            eventDialog.dismiss();
                        }));
        eventDialog.show();
    }

    void handleAddEventCallback25(EditText editText, CheckBox checkBox, PersianDate persianDate, CheckBox checkBox2, DialogInterface dialogInterface, int i) {
        String strTrim = editText.getText().toString().trim();
        if (strTrim.isEmpty()) {
            Ui.toast(this, "عنوان مناسبت خالی بود");
            return;
        }
        UserEvent userEvent = new UserEvent();
        userEvent.year = checkBox.isChecked() ? 0 : persianDate.year;
        userEvent.month = persianDate.month;
        userEvent.day = persianDate.day;
        userEvent.title = strTrim;
        userEvent.holiday = checkBox2.isChecked();
        this.database.saveUserEvent(userEvent);
        ShiftWidgetProvider.updateAll(this);
    }

    private void confirmDeleteEvent(UserEvent userEvent) {
        new AlertDialog.Builder(this).setMessage("«" + userEvent.title + "» حذف شود؟").setPositiveButton("حذف", SettingsActivity.createOnClickCallback11(this, userEvent)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
    }

    void handleCallback121(UserEvent userEvent, DialogInterface dialogInterface, int i) {
        this.database.deleteUserEvent(userEvent.f2id);
    }

    private void createBackup() {
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("application/json");
        intent.putExtra("android.intent.extra.TITLE", "TaghvimShift-Backup-" + PersianDate.todayTehran().toString().replace('/', '-') + ".json");
        startActivityForResult(intent, CREATE_BACKUP);
    }

    private void openBackup() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_BACKUP);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        Uri data = intent.getData();
        if (i == CREATE_BACKUP) {
            runIo(SettingsActivity.createRunCallback5(this, data));
        } else if (i == OPEN_BACKUP) {
            new AlertDialog.Builder(this).setTitle("بازیابی اطلاعات").setMessage("الگوها، کارکرد، همکاران و مناسبت\u200cهای شخصی با محتوای فایل پشتیبان جایگزین می\u200cشوند. ادامه می\u200cدهید؟").setPositiveButton("بازیابی", SettingsActivity.createOnClickCallback6(this, data)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
        }
    }

    String handleCallback124(Uri uri) throws Exception {
        BackupService.writeBackup(this, uri);
        return "فایل پشتیبان با موفقیت ساخته شد.";
    }

    void handleCallback126(Uri uri, DialogInterface dialogInterface, int i) {
        runIo(SettingsActivity.createRunCallback33(this, uri));
    }

    String handleCallback125(Uri uri) throws Exception {
        return "بازیابی انجام شد. " + BackupService.restore(this, uri).summary();
    }

    private void runIo(IoTask ioTask) {
        Ui.toast(this, "در حال انجام…");
        new Thread(SettingsActivity.createRunCallback34(this, ioTask), "eluqen-shift-calendar-io").start();
    }

    void handleRunIoCallback33(IoTask ioTask) {
        try {
            runOnUiThread(SettingsActivity.createRunCallback8(this, ioTask.run()));
        } catch (Exception e) {
            runOnUiThread(SettingsActivity.createRunCallback9(this, e));
        }
    }

    void handleRunIoCallback31(String str) {
        Ui.toast(this, str);
        bindPlans();
        refreshDetails();
        ShiftWidgetProvider.updateAll(this);
    }

    void handleRunIoCallback32(Exception exc) {
        Ui.toast(this, "خطا: " + exc.getMessage());
    }

    private LinearLayout horizontal() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout.setGravity(17);
        linearLayout.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        return linearLayout;
    }

    private Button patternButton(EditText editText, String str) {
        Button button = Ui.tonalButton(this, ShiftPreferences.shortTitle(this, str) + " — " + ShiftPreferences.title(this, str));
        button.setTextSize(11.0f);
        button.setOnClickListener(SettingsActivity.createOnClickCallback10(editText, str));
        return button;
    }

    static void handlePatternButtonCallback34(EditText editText, String str, View view) {
        String strTrim = editText.getText().toString().trim();
        if (!strTrim.isEmpty()) {
            str = strTrim + "," + str;
        }
        editText.setText(str);
        editText.setSelection(editText.getText().length());
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 48.0f), 1.0f);
        layoutParams.setMargins(Ui.dp(this, 3.0f), 0, Ui.dp(this, 3.0f), 0);
        return layoutParams;
    }

    private LinearLayout.LayoutParams sectionParams() {
        return Ui.margins(this, -1, -2, 0, 20, 0, 8);
    }

    private LinearLayout.LayoutParams dialogField() {
        return Ui.margins(this, -1, -2, 0, 5, 0, 5);
    }

    private static DialogInterface.OnClickListener createOnClickCallback11(final SettingsActivity capture0, final UserEvent capture1) {
        return (dialogInterface, i) -> {
        capture0.handleCallback121(capture1, dialogInterface, i);
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback12(final SettingsActivity capture0) {
        return (persianDate) -> {
        capture0.handleAddEventCallback26(persianDate);
        };
    }

    private static View.OnClickListener createOnClickCallback1(final SettingsActivity capture0, final PersianDate[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleEditPlanCallback15(capture1, capture2, view);
        };
    }

    private static SettingsActivity.IoTask createRunCallback5(final SettingsActivity capture0, final Uri capture1) {
        return () -> {
        return capture0.handleCallback124(capture1);
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback14(final PersianDate[] capture0, final Button capture1) {
        return (persianDate) -> {
        SettingsActivity.handleEditPlanCallback14(capture0, capture1, persianDate);
        };
    }

    private static View.OnClickListener createOnClickCallback25(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback6(view);
        };
    }

    private static View.OnClickListener createOnClickCallback22(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback3(view);
        };
    }

    private static View.OnClickListener createOnClickCallback10(final EditText capture0, final String capture1) {
        return (view) -> {
        SettingsActivity.handlePatternButtonCallback34(capture0, capture1, view);
        };
    }

    private static View.OnClickListener createOnClickCallback17(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback11(view);
        };
    }

    private static View.OnClickListener createOnClickCallback19(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback13(view);
        };
    }

    private static View.OnClickListener createOnClickCallback20(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback1(view);
        };
    }

    private static View.OnClickListener createOnClickCallback23(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback4(view);
        };
    }

    private static View.OnClickListener createOnClickCallback28(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback9(view);
        };
    }

    private static View.OnClickListener createOnClickCallback2(final EditText capture0) {
        return (view) -> {
        SettingsActivity.handleEditPlanCallback16(capture0, view);
        };
    }

    private static Runnable createRunCallback9(final SettingsActivity capture0, final Exception capture1) {
        return () -> {
        capture0.handleRunIoCallback32(capture1);
        };
    }

    private static CompoundButton.OnCheckedChangeListener createOnCheckedChangedCallback24(final SettingsActivity capture0) {
        return (compoundButton, z) -> {
        capture0.handleBuildUiCallback5(compoundButton, z);
        };
    }

    private static View.OnClickListener createOnClickCallback15(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback0(view);
        };
    }

    private static Runnable createRunCallback8(final SettingsActivity capture0, final String capture1) {
        return () -> {
        capture0.handleRunIoCallback31(capture1);
        };
    }

    private static Runnable createRunCallback34(final SettingsActivity capture0, final SettingsActivity.IoTask capture1) {
        return () -> {
        capture0.handleRunIoCallback33(capture1);
        };
    }

    private static View.OnClickListener createOnClickCallback16(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback10(view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback29(final SettingsActivity capture0, final ShiftPlan capture1, final ShiftPlan capture2) {
        return (dialogInterface, i) -> {
        capture0.handleCallback123(capture1, capture2, dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback27(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback8(view);
        };
    }

    private static View.OnClickListener createOnClickCallback7(final SettingsActivity capture0, final EditText capture1, final EditText capture2, final EditText capture3, final ShiftPlan capture4, final PersianDate[] capture5, final ShiftPlan capture6, final AlertDialog capture7) {
        return (view) -> {
        capture0.handleEditPlanCallback18(capture1, capture2, capture3, capture4, capture5, capture6, capture7, view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback31(final SettingsActivity capture0, final List capture1) {
        return (dialogInterface, i) -> {
        capture0.handleCallback127(capture1, dialogInterface, i);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback32(final SettingsActivity capture0) {
        return (dialogInterface, i) -> {
        capture0.handleCallback128(dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback18(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback12(view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback6(final SettingsActivity capture0, final Uri capture1) {
        return (dialogInterface, i) -> {
        capture0.handleCallback126(capture1, dialogInterface, i);
        };
    }

    private static Runnable createRunCallback4(final SettingsActivity capture0) {
        return () -> {
        capture0.handleCallback122();
        };
    }

    private static View.OnClickListener createOnClickCallback21(final SettingsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback2(view);
        };
    }

    private static Runnable createRunCallback30(final SettingsActivity capture0) {
        return () -> {
        capture0.handleEditPlanCallback17();
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback13(final SettingsActivity capture0, final EditText capture1, final CheckBox capture2, final PersianDate capture3, final CheckBox capture4) {
        return (dialogInterface, i) -> {
        capture0.handleAddEventCallback25(capture1, capture2, capture3, capture4, dialogInterface, i);
        };
    }

    private static DialogInterface.OnShowListener createOnShowCallback3(final SettingsActivity capture0, final AlertDialog capture1, final EditText capture2, final EditText capture3, final EditText capture4, final ShiftPlan capture5, final PersianDate[] capture6, final ShiftPlan capture7) {
        return (dialogInterface) -> {
        capture0.handleEditPlanCallback19(capture1, capture2, capture3, capture4, capture5, capture6, capture7, dialogInterface);
        };
    }

    private static SettingsActivity.IoTask createRunCallback33(final SettingsActivity capture0, final Uri capture1) {
        return () -> {
        return capture0.handleCallback125(capture1);
        };
    }
}
