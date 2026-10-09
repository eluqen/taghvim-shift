package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.eluqen.shiftcalendar.core.GregorianDate;
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
import com.eluqen.shiftcalendar.hijri.HijriResolver;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolsActivity extends BaseActivity {
    private static final int CREATE_ICS = 901;
    private PersianDate pendingIcsFrom;
    private PersianDate pendingIcsTo;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        buildUi();
        NavTools.direct(this);
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.addView(Ui.toolbar(this, "ابزارها", true));
        ScrollView scrollView = new ScrollView(this);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout2.setPadding(Ui.dp(this, 14.0f), Ui.dp(this, 14.0f), Ui.dp(this, 14.0f), Ui.dp(this, 24.0f));
        scrollView.addView(linearLayout2);
        addTool(linearLayout2, "محاسبات شیفت", "تعداد هر نوع تقویم شیفتی را بین دو تاریخ محاسبه می\u200cکند.", "محاسبه", ToolsActivity.createOnClickCallback9(this));
        addTool(linearLayout2, "تبدیل تاریخ", "تاریخ شمسی، میلادی، قمری و روز هفته را هم\u200cزمان می\u200cبینید.", "انتخاب تاریخ", ToolsActivity.createOnClickCallback10(this));
        addTool(linearLayout2, "تقویم چهارشیفت", "شیفت گروه\u200cهای A تا D یک شرکت را برای کل ماه کنار هم نشان می\u200cدهد.", "نمایش چهارشیفت", ToolsActivity.createOnClickCallback11(this));
        addTool(linearLayout2, "نمودار شیفت", "چرخهٔ شصت\u200cروزه را به\u200cصورت رنگی و پیوسته نمایش می\u200cدهد.", "نمایش نمودار", ToolsActivity.createOnClickCallback12(this));
        addTool(linearLayout2, "یادآوری شروع شیفت", "پیش از شروع نوبت، حتی در زمان بسته\u200cبودن برنامه اعلان دریافت کنید.", "تنظیم یادآوری", ToolsActivity.createOnClickCallback13(this));
        addTool(linearLayout2, "هم\u200cشیفت\u200cها و همکاران", "دفترچهٔ محلی اطلاعات تماس با امکان شماره\u200cگیری و پیامک.", "باز کردن دفترچه", ToolsActivity.createOnClickCallback14(this));
        addTool(linearLayout2, "خروجی تقویم", "شیفت\u200cهای بازهٔ دلخواه را به فایل استاندارد ICS برای تقویم گوشی تبدیل می\u200cکند.", "ساخت فایل ICS", ToolsActivity.createOnClickCallback15(this));
        addTool(linearLayout2, "اشتراک برنامهٔ ۳۰ روز آینده", "یک فهرست متنی از شیفت\u200cهای سی روز آینده آماده می\u200cکند.", "اشتراک متن", ToolsActivity.createOnClickCallback16(this));
        addTool(linearLayout2, "پشتیبان\u200cگیری و بازیابی", "ساخت یا بازیابی فایل مستقل پشتیبان تقویم شیفت.", "رفتن به تنظیمات", ToolsActivity.createOnClickCallback18(this));
        ScreenPolish.tools(linearLayout);
        setContentView(linearLayout);
    }

    void handleBuildUiCallback0(View view) {
        calculateShifts();
    }

    void handleBuildUiCallback1(View view) {
        convertDate();
    }

    void handleBuildUiCallback2(View view) {
        startActivity(new Intent(this, (Class<?>) FourShiftActivity.class));
    }

    void handleBuildUiCallback3(View view) {
        startActivity(new Intent(this, (Class<?>) GeneralSettingsActivity.class));
    }

    void handleBuildUiCallback4(View view) {
        startActivity(new Intent(this, (Class<?>) ReminderSettingsActivity.class));
    }

    void handleBuildUiCallback5(View view) {
        startActivity(new Intent(this, (Class<?>) ColleaguesActivity.class));
    }

    void handleBuildUiCallback6(View view) {
        chooseIcsRange();
    }

    void handleBuildUiCallback7(View view) {
        shareNextThirtyDays();
    }

    void handleBuildUiCallback9(View view) {
        startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
    }

    private void addTool(LinearLayout linearLayout, String str, String str2, String str3, View.OnClickListener onClickListener) {
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.title(this, str, 18.0f));
        linearLayoutCard.addView(Ui.text(this, str2, 14.0f, Ui.MUTED), Ui.margins(this, -1, -2, 0, 5, 0, 0));
        Button button = Ui.tonalButton(this, str3);
        button.setOnClickListener(onClickListener);
        linearLayoutCard.addView(button, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 12, 0, 0));
        linearLayout.addView(linearLayoutCard, Ui.margins(this, -1, -2, 0, 0, 0, 12));
    }

    private void calculateShifts() {
        PersianDate persianDate = PersianDate.todayTehran();
        PersianDate[] persianDateArr = {persianDate};
        PersianDate[] persianDateArr2 = {persianDate.plusDays(30L)};
        new AlertDialog.Builder(this).setTitle("محاسبهٔ شیفت").setView(dateRangeForm(persianDateArr, persianDateArr2)).setPositiveButton("محاسبه", ToolsActivity.createOnClickCallback4(this, persianDateArr, persianDateArr2)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
    }

    void handleCallback148(PersianDate[] persianDateArr, PersianDate[] persianDateArr2, DialogInterface dialogInterface, int i) {
        showShiftCalculation(persianDateArr[0], persianDateArr2[0]);
    }

    private void showShiftCalculation(PersianDate persianDate, PersianDate persianDate2) {
        ShiftPlan shiftPlanActivePlan = AppDatabase.get(this).activePlan();
        long jMax = Math.max(persianDate.toJdn(), persianDate2.toJdn());
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
        for (long jMin = Math.min(persianDate.toJdn(), persianDate2.toJdn()); jMin <= jMax; jMin++) {
            String strTitle = ShiftPreferences.title(this, GeneralWork.adjustJdn(this, jMin, shiftPlanActivePlan, ShiftEngine.tokenFor(jMin, shiftPlanActivePlan.anchorJdn, shiftPlanActivePlan.pattern)));
            Integer num = (Integer) linkedHashMap.get(strTitle);
            int iIntValue = 1;
            if (num != null) {
                iIntValue = 1 + num.intValue();
            }
            linkedHashMap.put(strTitle, Integer.valueOf(iIntValue));
        }
        StringBuilder sb = new StringBuilder("الگو: ");
        long jAbs = Math.abs(persianDate2.toJdn() - persianDate.toJdn()) + 1;
        sb.append(shiftPlanActivePlan.displayName());
        sb.append("\nتعداد روزهای بازه: ");
        sb.append(PersianDigits.of(Long.valueOf(jAbs)));
        sb.append("\n\n");
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            sb.append((String) entry.getKey());
            sb.append(": ");
            sb.append(PersianDigits.of(entry.getValue()));
            sb.append(" روز\n");
        }
        new AlertDialog.Builder(this).setTitle("نتیجه").setMessage(sb.toString().trim()).setPositiveButton("بستن", (DialogInterface.OnClickListener) null).show();
    }

    private void convertDate() {
        PersianDatePicker.show(this, PersianDate.todayTehran(), ToolsActivity.createOnPickedCallback0(this));
    }

    void handleConvertDateCallback11(PersianDate persianDate) {
        GregorianDate gregorian = JalaliCalendar.toGregorian(persianDate);
        HijriDate hijriDateFromJdn = HijriResolver.resolve(this, persianDate);
        new AlertDialog.Builder(this).setTitle(PersianDigits.of(persianDate.displayLong())).setMessage(PersianDigits.of("شمسی: " + persianDate + "\nروز هفته: " + persianDate.weekdayName() + "\nمیلادی: " + gregorian + "\nقمری: " + hijriDateFromJdn + " — " + hijriDateFromJdn.displayLong() + "\n\nعدد روز مطلق: " + persianDate.toJdn())).setPositiveButton("بستن", (DialogInterface.OnClickListener) null).show();
    }

    private void chooseIcsRange() {
        PersianDate persianDate = PersianDate.todayTehran();
        PersianDate[] persianDateArr = {persianDate};
        PersianDate[] persianDateArr2 = {persianDate.plusDays(365L)};
        new AlertDialog.Builder(this).setTitle("بازهٔ خروجی تقویم").setView(dateRangeForm(persianDateArr, persianDateArr2)).setPositiveButton("ادامه", ToolsActivity.createOnClickCallback8(this, persianDateArr2, persianDateArr)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
    }

    void handleCallback149(PersianDate[] persianDateArr, PersianDate[] persianDateArr2, DialogInterface dialogInterface, int i) {
        if (Math.abs(persianDateArr[0].toJdn() - persianDateArr2[0].toJdn()) > 3660) {
            Ui.toast(this, "حداکثر بازهٔ هر فایل ده سال است");
            return;
        }
        this.pendingIcsFrom = persianDateArr2[0];
        this.pendingIcsTo = persianDateArr[0];
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("text/calendar");
        intent.putExtra("android.intent.extra.TITLE", "TaghvimShift-" + fileDate(persianDateArr2[0]) + "-to-" + fileDate(persianDateArr[0]) + ".ics");
        startActivityForResult(intent, CREATE_ICS);
    }

    private LinearLayout dateRangeForm(PersianDate[] persianDateArr, PersianDate[] persianDateArr2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 18.0f), Ui.dp(this, 8.0f), Ui.dp(this, 18.0f), Ui.dp(this, 6.0f));
        Button button = Ui.tonalButton(this, "");
        Button button2 = Ui.tonalButton(this, "");
        updateDateButton(button, "از تاریخ", persianDateArr[0]);
        updateDateButton(button2, "تا تاریخ", persianDateArr2[0]);
        button.setOnClickListener(ToolsActivity.createOnClickCallback2(this, persianDateArr, button));
        button2.setOnClickListener(ToolsActivity.createOnClickCallback3(this, persianDateArr2, button2));
        linearLayout.addView(button, Ui.margins(this, -1, Ui.dp(this, 52.0f), 0, 5, 0, 5));
        linearLayout.addView(button2, Ui.margins(this, -1, Ui.dp(this, 52.0f), 0, 5, 0, 5));
        return linearLayout;
    }

    void handleCallback151(PersianDate[] persianDateArr, Button button, View view) {
        PersianDatePicker.show(this, persianDateArr[0], ToolsActivity.createOnPickedCallback7(this, persianDateArr, button));
    }

    void handleCallback150(PersianDate[] persianDateArr, Button button, PersianDate persianDate) {
        persianDateArr[0] = persianDate;
        updateDateButton(button, "از تاریخ", persianDate);
    }

    void handleCallback153(PersianDate[] persianDateArr, Button button, View view) {
        PersianDatePicker.show(this, persianDateArr[0], ToolsActivity.createOnPickedCallback19(this, persianDateArr, button));
    }

    void handleCallback152(PersianDate[] persianDateArr, Button button, PersianDate persianDate) {
        persianDateArr[0] = persianDate;
        updateDateButton(button, "تا تاریخ", persianDate);
    }

    private void updateDateButton(Button button, String str, PersianDate persianDate) {
        button.setText(PersianDigits.of(str + ": " + persianDate));
    }

    private void shareNextThirtyDays() {
        PersianDate persianDate = PersianDate.todayTehran();
        ShiftPlan shiftPlanActivePlan = AppDatabase.get(this).activePlan();
        StringBuilder sb = new StringBuilder("برنامهٔ شیفت ۳۰ روز آینده — ");
        sb.append(shiftPlanActivePlan.displayName());
        sb.append("\n\n");
        for (int i = 0; i < 30; i++) {
            PersianDate persianDatePlusDays = persianDate.plusDays(i);
            String strAdjust = GeneralWork.adjust(this, persianDatePlusDays, shiftPlanActivePlan, ShiftEngine.tokenFor(persianDatePlusDays.toJdn(), shiftPlanActivePlan.anchorJdn, shiftPlanActivePlan.pattern));
            sb.append(PersianDigits.of(persianDatePlusDays.toString()));
            sb.append("  ");
            sb.append(persianDatePlusDays.weekdayName());
            sb.append("  —  ");
            sb.append(ShiftPreferences.title(this, strAdjust));
            sb.append("\n");
        }
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.SUBJECT", "برنامهٔ شیفت");
        intent.putExtra("android.intent.extra.TEXT", sb.toString());
        startActivity(Intent.createChooser(intent, "اشتراک برنامهٔ شیفت"));
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != CREATE_ICS || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        new Thread(ToolsActivity.createRunCallback1(this, intent.getData(), this.pendingIcsFrom, this.pendingIcsTo), "eluqen-shift-calendar-ics").start();
    }

    void handleCallback156(Uri uri, PersianDate persianDate, PersianDate persianDate2) {
        try {
            BackupService.writeIcs(this, uri, persianDate, persianDate2);
            runOnUiThread(ToolsActivity.createRunCallback5(this));
        } catch (Exception e) {
            runOnUiThread(ToolsActivity.createRunCallback6(this, e));
        }
    }

    void handleCallback154() {
        Ui.toast(this, "فایل تقویم ساخته شد.");
    }

    void handleCallback155(Exception exc) {
        Ui.toast(this, "خطا: " + exc.getMessage());
    }

    private static String fileDate(PersianDate persianDate) {
        return persianDate.toString().replace('/', '-');
    }

    private static View.OnClickListener createOnClickCallback18(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback9(view);
        };
    }

    private static View.OnClickListener createOnClickCallback9(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback0(view);
        };
    }

    private static View.OnClickListener createOnClickCallback16(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback7(view);
        };
    }

    private static Runnable createRunCallback1(final ToolsActivity capture0, final Uri capture1, final PersianDate capture2, final PersianDate capture3) {
        return () -> {
        capture0.handleCallback156(capture1, capture2, capture3);
        };
    }

    private static View.OnClickListener createOnClickCallback10(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback1(view);
        };
    }

    private static View.OnClickListener createOnClickCallback3(final ToolsActivity capture0, final PersianDate[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleCallback153(capture1, capture2, view);
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback19(final ToolsActivity capture0, final PersianDate[] capture1, final Button capture2) {
        return (persianDate) -> {
        capture0.handleCallback152(capture1, capture2, persianDate);
        };
    }

    private static View.OnClickListener createOnClickCallback13(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback4(view);
        };
    }

    private static View.OnClickListener createOnClickCallback14(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback5(view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback4(final ToolsActivity capture0, final PersianDate[] capture1, final PersianDate[] capture2) {
        return (dialogInterface, i) -> {
        capture0.handleCallback148(capture1, capture2, dialogInterface, i);
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback7(final ToolsActivity capture0, final PersianDate[] capture1, final Button capture2) {
        return (persianDate) -> {
        capture0.handleCallback150(capture1, capture2, persianDate);
        };
    }

    private static View.OnClickListener createOnClickCallback12(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback3(view);
        };
    }

    private static Runnable createRunCallback5(final ToolsActivity capture0) {
        return () -> {
        capture0.handleCallback154();
        };
    }

    private static PersianDatePicker.Listener createOnPickedCallback0(final ToolsActivity capture0) {
        return (persianDate) -> {
        capture0.handleConvertDateCallback11(persianDate);
        };
    }

    private static Runnable createRunCallback6(final ToolsActivity capture0, final Exception capture1) {
        return () -> {
        capture0.handleCallback155(capture1);
        };
    }

    private static View.OnClickListener createOnClickCallback2(final ToolsActivity capture0, final PersianDate[] capture1, final Button capture2) {
        return (view) -> {
        capture0.handleCallback151(capture1, capture2, view);
        };
    }

    private static View.OnClickListener createOnClickCallback15(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback6(view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback8(final ToolsActivity capture0, final PersianDate[] capture1, final PersianDate[] capture2) {
        return (dialogInterface, i) -> {
        capture0.handleCallback149(capture1, capture2, dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback11(final ToolsActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback2(view);
        };
    }
}
