package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.update.AppUpdateManager;
import com.eluqen.shiftcalendar.update.DataUpdateJobService;
import com.eluqen.shiftcalendar.update.DataUpdateManager;
import com.eluqen.shiftcalendar.update.UpdateSource;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class UpdateCenterActivity extends BaseActivity {
    private static final String STATE_WAITING_INSTALL_PERMISSION =
            "waiting_install_permission";

    private TextView appDetails;
    private TextView appUpdatePolicy;
    private CheckBox autoApp;
    private TextView dataDetails;
    private TextView dataStatus;
    private Button dataUpdateButton;
    private Button sourceButton;
    private Button networkButton;
    private Button appUpdateButton;
    private boolean waitingForInstallPermission;
    private boolean storeChoicePromptShown;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Prefs.ensureInitialInstallSource(this);
        if (state != null) {
            waitingForInstallPermission =
                    state.getBoolean(STATE_WAITING_INSTALL_PERMISSION, false);
        }
        buildUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(
                STATE_WAITING_INSTALL_PERMISSION,
                waitingForInstallPermission);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();

    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "مرکز به‌روزرسانی", true));

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(
                Ui.dp(this, 14.0f),
                Ui.dp(this, 14.0f),
                Ui.dp(this, 14.0f),
                Ui.dp(this, 28.0f));
        scroll.addView(body);

        LinearLayout appCard = Ui.card(this);
        appCard.addView(Ui.eyebrow(this, "نسخه برنامه"));
        appCard.addView(
                Ui.title(this, "تقویم شیفت", 22.0f),
                Ui.margins(this, -1, -2, 0, 4, 0, 0));

        appDetails = Ui.text(this, "", 14.0f, Ui.MUTED);
        appDetails.setLineSpacing(0.0f, 1.16f);
        appCard.addView(
                appDetails,
                Ui.margins(this, -1, -2, 0, 9, 0, 0));

        sourceButton = Ui.tonalButton(this, "");
        sourceButton.setOnClickListener(v -> chooseSource());
        appCard.addView(
                sourceButton,
                Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 12, 0, 0));

        autoApp = styledCheck(
                "بررسی خودکار نسخه جدید",
                Prefs.autoAppUpdate(this));
        autoApp.setOnCheckedChangeListener((button, checked) ->
                Prefs.setAutoAppUpdate(this, checked));
        appCard.addView(
                autoApp,
                Ui.margins(this, -1, -2, 0, 8, 0, 0));

        appUpdatePolicy = Ui.text(this, "", 13.0f, Ui.MUTED);
        appCard.addView(
                appUpdatePolicy,
                Ui.margins(this, -1, -2, 0, 5, 0, 0));

        appUpdateButton = Ui.button(this, "بررسی به‌روزرسانی برنامه");
        appUpdateButton.setOnClickListener(v -> checkAppUpdate());
        appCard.addView(
                appUpdateButton,
                Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 10, 0, 0));
        body.addView(appCard);

        LinearLayout dataCard = Ui.card(this);
        dataCard.addView(Ui.eyebrow(this, "بانک داده"));
        dataCard.addView(
                Ui.title(this, "مناسبت‌ها و تعطیلات", 21.0f),
                Ui.margins(this, -1, -2, 0, 4, 0, 0));

        dataDetails = Ui.text(this, "", 14.0f, Ui.MUTED);
        dataDetails.setLineSpacing(0.0f, 1.18f);
        dataCard.addView(
                dataDetails,
                Ui.margins(this, -1, -2, 0, 9, 0, 0));

        dataStatus = Ui.text(this, "", 13.0f, Ui.SECONDARY);
        dataStatus.setLineSpacing(0.0f, 1.15f);
        dataCard.addView(
                dataStatus,
                Ui.margins(this, -1, -2, 0, 8, 0, 0));

        CheckBox autoData = styledCheck(
                "به‌روزرسانی خودکار بانک داده",
                Prefs.autoDataUpdate(this));
        autoData.setOnCheckedChangeListener((button, checked) -> {
            Prefs.setAutoDataUpdate(this, checked);
            DataUpdateJobService.syncSchedule(this);
        });
        dataCard.addView(
                autoData,
                Ui.margins(this, -1, -2, 0, 8, 0, 0));

        networkButton = Ui.tonalButton(this, "");
        networkButton.setOnClickListener(v -> chooseNetwork());
        dataCard.addView(
                networkButton,
                Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 8, 0, 0));

        dataUpdateButton = Ui.button(this, "به‌روزرسانی بانک مناسبت‌ها");
        dataUpdateButton.setOnClickListener(v -> previewDataUpdate());
        dataCard.addView(
                dataUpdateButton,
                Ui.margins(this, -1, Ui.dp(this, 50.0f), 0, 10, 0, 0));

        body.addView(
                dataCard,
                Ui.margins(this, -1, -2, 0, 12, 0, 0));

        TextView note = Ui.text(
                this,
                "به‌روزرسانی بانک مناسبت‌ها مستقل از فروشگاه انتخابی است و فقط بسته داده امضاشده را دریافت می‌کند؛ اطلاعات شخصی، کارکرد و الگوهای شما تغییر نمی‌کنند.",
                13.0f,
                Ui.MUTED);
        note.setLineSpacing(0.0f, 1.16f);
        body.addView(
                note,
                Ui.margins(this, -1, -2, 6, 14, 6, 0));

        setContentView(root);
    }

    private CheckBox styledCheck(String text, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(text);
        box.setTextSize(14.0f);
        box.setTextColor(Ui.TEXT);
        box.setChecked(checked);
        box.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return box;
    }

    private void refresh() {
        UpdateSource source = AppUpdateManager.selectedSource(this);
        String pending = Prefs.pendingAppVersionCode(this) > currentVersionCode()
                ? "\nنسخه آمادهٔ دریافت: " + Prefs.pendingAppVersionName(this)
                : "";

        appDetails.setText(PersianDigits.of(
                "نسخه: " + versionName()
                        + "\nآخرین بررسی به‌روزرسانی: "
                        + formatTimestamp(Prefs.lastAppCheck(this))
                        + pending));

        sourceButton.setVisibility(View.GONE);
        autoApp.setVisibility(View.GONE);
        appUpdatePolicy.setVisibility(View.GONE);

        EventRepository.PackInfo info = EventRepository.get(this).info();
        dataDetails.setText(PersianDigits.of(
                "dataVersion: " + info.dataVersion
                        + "\nپوشش: " + info.coverageFrom + " تا " + info.coverageTo
                        + "\nتعداد رکوردها: " + info.eventCount
                        + "\nآخرین بررسی: "
                        + formatTimestamp(Prefs.lastDataCheck(this))
                        + "\nآخرین دریافت موفق: "
                        + formatTimestamp(Prefs.lastDataSuccess(this))));
        dataStatus.setText(Prefs.lastDataStatus(this));
        networkButton.setText(
                "شبکه به‌روزرسانی خودکار — "
                        + networkLabel(Prefs.updateNetwork(this)));
    }

    private void chooseSource() {}

    private void chooseNetwork() {
        String[] labels = {
                "همه شبکه‌ها",
                "فقط وای‌فای",
                "قبل از دانلود بپرس"
        };
        String[] values = {
                Prefs.NETWORK_ANY,
                Prefs.NETWORK_WIFI,
                Prefs.NETWORK_ASK
        };

        String current = Prefs.updateNetwork(this);
        int checked = Prefs.NETWORK_WIFI.equals(current)
                ? 1 : Prefs.NETWORK_ASK.equals(current) ? 2 : 0;
        final int initial = checked;

        new AlertDialog.Builder(this)
                .setTitle("شبکه به‌روزرسانی خودکار")
                .setSingleChoiceItems(FancyDialogs.single(this, labels), checked, null)
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    int position = ((AlertDialog) dialog)
                            .getListView().getCheckedItemPosition();
                    if (position < 0) position = initial;
                    Prefs.setUpdateNetwork(this, values[position]);
                    DataUpdateJobService.syncSchedule(this);
                    refresh();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void checkAppUpdate() {
        AppUpdateManager.OpenResult result = AppUpdateManager.openStore(this, UpdateSource.GOOGLE_PLAY);
        if (!result.opened) Ui.toast(this, result.message);
    }











    private void previewDataUpdate() {
        dataUpdateButton.setEnabled(false);
        dataUpdateButton.setText("در حال بررسی…");
        DataUpdateManager.preview(this, result ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    dataUpdateButton.setEnabled(true);
                    dataUpdateButton.setText("به‌روزرسانی بانک مناسبت‌ها");
                    if (result.status == DataUpdateManager.Status.UPDATE_AVAILABLE) {
                        showDataPreview(result);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("بانک مناسبت‌ها")
                                .setMessage(result.message)
                                .setPositiveButton("بستن", null)
                                .show();
                        refresh();
                    }
                }));
    }

    private void showDataPreview(DataUpdateManager.Result result) {
        EventRepository.PackInfo current = EventRepository.get(this).info();
        StringBuilder message = new StringBuilder();
        message.append("dataVersion فعلی: ").append(current.dataVersion);
        message.append("\ndataVersion جدید: ").append(result.dataVersion);
        message.append("\nپوشش فعلی: ")
                .append(current.coverageFrom)
                .append(" تا ")
                .append(current.coverageTo);
        message.append("\nپوشش بسته جدید: ")
                .append(result.coverageFrom)
                .append(" تا ")
                .append(result.coverageTo);
        if (result.size > 0) {
            message.append("\nحجم فشرده: ").append(formatBytes(result.size));
        }

        new AlertDialog.Builder(this)
                .setTitle("به‌روزرسانی بانک داده")
                .setMessage(PersianDigits.of(message.toString()))
                .setPositiveButton(
                        "دریافت و نصب",
                        (dialog, which) -> installDataUpdate())
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void installDataUpdate() {
        dataUpdateButton.setEnabled(false);
        dataUpdateButton.setText("در حال دریافت…");
        DataUpdateManager.check(this, true, result ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    dataUpdateButton.setEnabled(true);
                    dataUpdateButton.setText("به‌روزرسانی بانک مناسبت‌ها");
                    new AlertDialog.Builder(this)
                            .setTitle("بانک مناسبت‌ها")
                            .setMessage(result.message)
                            .setPositiveButton("بستن", null)
                            .show();
                    refresh();
                }));
    }

    private String versionName() {
        try {
            PackageInfo info =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "—" : info.versionName;
        } catch (Exception e) {
            return "—";
        }
    }

    private int currentVersionCode() {
        try {
            return AppUpdateManager.currentVersionCode(this);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String networkLabel(String value) {
        if (Prefs.NETWORK_WIFI.equals(value)) return "فقط وای‌فای";
        if (Prefs.NETWORK_ASK.equals(value)) return "قبل از دانلود بپرس";
        return "همه شبکه‌ها";
    }

    private static String formatTimestamp(long millis) {
        if (millis <= 0) return "—";
        Calendar calendar =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Tehran"),
                        Locale.US);
        calendar.setTimeInMillis(millis);

        PersianDate date =
                PersianDate.today(TimeZone.getTimeZone("Asia/Tehran"));
        try {
            date = JalaliCalendar.fromGregorian(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
        } catch (RuntimeException ignored) {
        }

        return PersianDigits.of(String.format(
                Locale.US,
                "%s ساعت %02d:%02d",
                date,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)));
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(
                    Locale.US,
                    "%.1f مگابایت",
                    bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format(
                    Locale.US,
                    "%.1f کیلوبایت",
                    bytes / 1024.0);
        }
        return bytes + " بایت";
    }
}
