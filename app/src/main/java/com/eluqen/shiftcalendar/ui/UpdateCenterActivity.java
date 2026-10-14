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
import com.eluqen.shiftcalendar.update.AppUpdateInstaller;
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
        if (!storeChoicePromptShown
                && AppUpdateManager.isMarketPolicyRestricted(this)
                && AppUpdateManager.selectedSource(this) == UpdateSource.ASK) {
            storeChoicePromptShown = true;
            chooseSource();
        }
        if (waitingForInstallPermission && canInstallUnknownApps()) {
            File pending = verifiedApkFile();
            waitingForInstallPermission = false;
            if (pending.isFile()) {
                openInstaller(pending);
            }
        }
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

        boolean unknownMarket = AppUpdateManager.isMarketPolicyRestricted(this)
                && source == UpdateSource.ASK;
        sourceButton.setVisibility(unknownMarket ? View.VISIBLE : View.GONE);
        sourceButton.setText("انتخاب فروشگاه");

        boolean direct = source == UpdateSource.DIRECT;
        autoApp.setVisibility(direct ? View.VISIBLE : View.GONE);
        autoApp.setEnabled(direct);
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

    private void chooseSource() {
        List<UpdateSource> sources = AppUpdateManager.selectableSources(this);
        if (sources.size() == 1) {
            UpdateSource only = sources.get(0);
            new AlertDialog.Builder(this)
                    .setTitle(only.label)
                    .setMessage(
                            "مسیر به‌روزرسانی خود برنامه در این نسخه به «"
                                    + only.label
                                    + "» محدود است. بانک مناسبت‌ها همچنان مستقل به‌روزرسانی می‌شود.")
                    .setPositiveButton("متوجه شدم", null)
                    .show();
            return;
        }

        String[] labels = new String[sources.size()];
        UpdateSource selected = AppUpdateManager.selectedSource(this);
        int checked = -1;
        for (int i = 0; i < sources.size(); i++) {
            labels[i] = sources.get(i).label;
            if (sources.get(i) == selected) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(selected == UpdateSource.ASK
                        ? "منبع نصب مشخص نیست؛ فروشگاه موردنظر را انتخاب کنید"
                        : "منبع به‌روزرسانی برنامه")
                .setSingleChoiceItems(FancyDialogs.single(this, labels), checked, null)
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    int position = ((AlertDialog) dialog)
                            .getListView().getCheckedItemPosition();
                    if (position < 0) {
                        android.widget.Toast.makeText(this, "ابتدا بازار، مایکت یا گلکسی استور را انتخاب کنید", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    UpdateSource source = sources.get(position);
                    Prefs.setUpdateSource(this, source);
                    refresh();
                    if (source == UpdateSource.GOOGLE_PLAY
                            && !"گوگل‌پلی".equals(Prefs.initialInstallSourceLabel(this))) {
                        new AlertDialog.Builder(this)
                                .setTitle("نکته گوگل‌پلی")
                                .setMessage(
                                        "گوگل‌پلی زمانی می‌تواند این نصب را به‌روزرسانی کند که برنامه با همین شناسه و امضا در گوگل‌پلی منتشر شده و دست‌کم یک‌بار به حساب Google کاربر اضافه شده باشد.")
                                .setPositiveButton("متوجه شدم", null)
                                .show();
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

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
        UpdateSource source = AppUpdateManager.selectedSource(this);
        if (source == UpdateSource.ASK) {
            chooseSource();
            return;
        }
        if (source != UpdateSource.DIRECT) {
            AppUpdateManager.OpenResult result =
                    AppUpdateManager.openStore(this, source);
            if (!result.opened) Ui.toast(this, result.message);
            return;
        }

        appUpdateButton.setEnabled(false);
        appUpdateButton.setText("در حال بررسی…");
        AppUpdateManager.checkDirect(this, result -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            appUpdateButton.setEnabled(true);
            appUpdateButton.setText("بررسی به‌روزرسانی برنامه");
            showAppResult(result);
            refresh();
        }));
    }

    private void showAppResult(AppUpdateManager.Result result) {
        if (result.status != AppUpdateManager.Status.UPDATE_AVAILABLE) {
            new AlertDialog.Builder(this)
                    .setTitle("به‌روزرسانی برنامه")
                    .setMessage(result.message)
                    .setPositiveButton("بستن", null)
                    .show();
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("نسخه فعلی: ").append(versionName());
        message.append("\nنسخه جدید: ").append(result.versionName);
        if (!result.changelog.trim().isEmpty()) {
            message.append("\n\nتغییرات:\n").append(result.changelog.trim());
        }

        new AlertDialog.Builder(this)
                .setTitle("نسخه جدید آماده است")
                .setMessage(PersianDigits.of(message.toString()))
                .setPositiveButton(
                        "دانلود و نصب",
                        (dialog, which) -> downloadAndInstall(result))
                .setNegativeButton("بعداً", null)
                .show();
    }

    private void downloadAndInstall(AppUpdateManager.Result update) {
        appUpdateButton.setEnabled(false);
        appUpdateButton.setText("در حال دریافت…");
        Ui.toast(this, "در حال دریافت و اعتبارسنجی APK…");

        AppUpdateManager.downloadVerifiedApk(this, update, result ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    appUpdateButton.setEnabled(true);
                    appUpdateButton.setText("بررسی به‌روزرسانی برنامه");
                    if (!result.success || result.file == null) {
                        new AlertDialog.Builder(this)
                                .setTitle("دانلود نسخه جدید")
                                .setMessage(result.message)
                                .setPositiveButton("بستن", null)
                                .show();
                        return;
                    }
                    openInstaller(result.file);
                }));
    }

    private void openInstaller(File file) {
        try {
            AppUpdateInstaller.Result result =
                    AppUpdateInstaller.install(this, file);
            waitingForInstallPermission =
                    result == AppUpdateInstaller.Result.PERMISSION_REQUIRED;
            if (waitingForInstallPermission) {
                Ui.toast(
                        this,
                        "برای نصب به‌روزرسانی، اجازه نصب از این منبع را فعال کنید.");
            }
        } catch (Exception e) {
            waitingForInstallPermission = false;
            new AlertDialog.Builder(this)
                    .setTitle("نصب نسخه جدید")
                    .setMessage("باز کردن نصب‌کننده Android انجام نشد.")
                    .setPositiveButton("بستن", null)
                    .show();
        }
    }

    private boolean canInstallUnknownApps() {
        return Build.VERSION.SDK_INT < 26
                || getPackageManager().canRequestPackageInstalls();
    }

    private File verifiedApkFile() {
        return new File(
                new File(getCacheDir(), "updates"),
                "ELUQEN-Taghvim-Shift-update.apk");
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
