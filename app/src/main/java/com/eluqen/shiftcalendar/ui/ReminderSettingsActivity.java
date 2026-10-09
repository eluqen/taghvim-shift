package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.data.ShiftPreferences.Profile;
import com.eluqen.shiftcalendar.reminder.ReminderNotifications;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.work.DayWorkResolver;

/**
 * Single authoritative UX for shift reminders.
 *
 * The page intentionally keeps the common path simple:
 * enable -> choose lead time -> choose sound/vibration -> done.
 * Per-shift overrides and Android-specific controls live behind explicit actions.
 */
public final class ReminderSettingsActivity extends BaseActivity {
    private static final int REQUEST_NOTIFICATIONS = 7101;
    private static final int REQUEST_NOTIFICATIONS_STANDALONE = 7102;
    private static final int[] LEAD_VALUES = {
            0, 10, 15, 30, 45, 60, 90, 120, 180, 360, 720, 1440
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Names "binding" and "enabled" are intentionally stable: the permission
    // gate uses them to reconcile a system permission result without recursion.
    private boolean binding;
    private Switch enabled;
    private Switch sound;
    private Switch vibration;

    private TextView heroState;
    private TextView nextStatus;
    private TextView leadValue;
    private TextView soundValue;
    private TextView shiftSummary;
    private TextView healthStatus;
    private TextView scheduleStatus;
    private TextView zoneValue;
    private Button notificationButton;
    private Button exactButton;

    private Runnable pendingSchedule;
    private int scheduleGeneration;
    private int previewGeneration;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        bindValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ReminderPermissionGate.reconcile(this);
        bindValues();
        refreshHealth();
        if (Prefs.reminderEnabled(this)) {
            requestSchedule(0);
        } else {
            previewGeneration++;
            renderNext(null);
        }
    }

    @Override
    protected void onStop() {
        previewGeneration++;
        SoundPicker.stopPreview(this);
        // If a quick setting change was waiting for the short debounce window,
        // do not lose it just because the user immediately left the page.
        if (pendingSchedule != null) {
            mainHandler.removeCallbacks(pendingSchedule);
            pendingSchedule = null;
            if (Prefs.reminderEnabled(this)) {
                ReminderScheduler.syncAsync(getApplicationContext());
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        previewGeneration++;
        scheduleGeneration++;
        SoundPicker.stopPreview(this);
        if (pendingSchedule != null) {
            mainHandler.removeCallbacks(pendingSchedule);
            pendingSchedule = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (SoundPicker.result(this, requestCode, resultCode, data)) {
            bindValues();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS_STANDALONE) {
            bindValues();
            refreshHealth();
            if (!ReminderPermissionGate.notificationsAllowed(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("اعلان هنوز فعال نیست")
                        .setMessage(
                                "مجوز اعلان داده نشد. اگر اندروید دیگر پنجره مجوز را نشان نمی‌دهد، "
                                        + "اعلان‌های برنامه را از تنظیمات سیستم فعال کنید.")
                        .setPositiveButton(
                                "باز کردن تنظیمات اعلان",
                                (dialog, which) -> openNotificationSettings())
                        .setNegativeButton("بستن", null)
                        .show();
            }
            return;
        }
        ReminderPermissionGate.onResult(
                this, requestCode, grantResults);
        bindValues();
        refreshHealth();
        if (Prefs.reminderEnabled(this)) {
            requestSchedule(0);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "یادآوری شروع شیفت", true));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(34));
        scroll.addView(body);

        body.addView(heroCard());
        body.addView(sectionTitle("یادآوری بعدی"));
        body.addView(nextCard());
        body.addView(sectionTitle("تنظیم سریع"));
        body.addView(quickCard());
        body.addView(sectionTitle("برای هر نوع شیفت"));
        body.addView(shiftCard());
        body.addView(sectionTitle("سلامت یادآوری"));
        body.addView(healthCard());

        TextView footnote = Ui.text(
                this,
                "مرخصی، استراحت، تعویض شیفت و اضافه‌کاری ثبت‌شده در کارکرد "
                        + "به‌صورت خودکار در زمان‌بندی لحاظ می‌شوند.\n"
                        + "بستن عادی برنامه، یادآوری‌ها را لغو نمی‌کند. "
                        + "«توقف اجباری» در تنظیمات اندروید می‌تواند "
                        + "زمان‌بندی‌ها را تا اجرای دوباره برنامه متوقف کند.",
                12f,
                Ui.MUTED);
        footnote.setPadding(dp(6), dp(15), dp(6), dp(2));
        body.addView(footnote);

        setContentView(root);
    }

    private View heroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(heroBackground());
        card.setElevation(dp(3));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = Ui.text(
                this, "ELUQEN REMINDER", 11f, 0xffd9efff);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView title = Ui.text(
                this, "شیفت بعدی یادت نمی‌رود", 22f, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = Ui.text(
                this,
                "یک‌بار تنظیمش کن؛ ادامه زمان‌بندی خودکار است.",
                13f,
                0xffe8f3ff);
        subtitle.setPadding(0, dp(4), 0, 0);
        copy.addView(eyebrow);
        copy.addView(title);
        copy.addView(subtitle);
        top.addView(
                copy,
                new LinearLayout.LayoutParams(0, -2, 1f));

        enabled = new Switch(this);
        enabled.setText("روشن");
        enabled.setTextSize(13f);
        enabled.setTextColor(Color.WHITE);
        enabled.setGravity(Gravity.CENTER);
        enabled.setContentDescription("فعال یا غیرفعال کردن یادآوری شیفت");
        if (Build.VERSION.SDK_INT >= 21) {
            enabled.setThumbTintList(
                    new ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{0xffffd27a, 0xffffffff}));
        }
        enabled.setOnCheckedChangeListener(
                (button, on) -> onMasterChanged(on));
        top.addView(
                enabled,
                new LinearLayout.LayoutParams(
                        dp(108), dp(54)));
        card.addView(top);

        heroState = Ui.text(this, "", 13f, Color.WHITE);
        heroState.setPadding(0, dp(12), 0, 0);
        card.addView(heroState);
        return card;
    }

    private View nextCard() {
        LinearLayout card = Ui.card(this);
        TextView eyebrow = Ui.eyebrow(this, "وضعیت زنده");
        card.addView(eyebrow);

        nextStatus = Ui.text(this, "", 16f, Ui.TEXT);
        nextStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nextStatus.setPadding(0, dp(7), 0, dp(12));
        card.addView(nextStatus);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button upcoming = Ui.tonalButton(
                this, "۱۰ یادآوری بعدی");
        upcoming.setOnClickListener(
                v -> ReminderPanel.openUpcoming(this));
        actions.addView(
                upcoming,
                weightedButtonParams(1, 0, 4));

        Button test = Ui.button(this, "آزمایش اعلان");
        test.setOnClickListener(v -> sendTest());
        actions.addView(
                test,
                weightedButtonParams(1, 4, 0));

        card.addView(actions);
        return card;
    }

    private View quickCard() {
        LinearLayout card = Ui.card(this);

        leadValue = valueView();
        card.addView(actionRow(
                "زمان یادآوری",
                "چه مدت قبل از شروع نوبت خبر بدهد",
                leadValue,
                this::showLeadDialog));
        card.addView(divider());

        sound = new Switch(this);
        styleSwitch(sound);
        card.addView(switchRow(
                "صدا",
                "پخش صدا هنگام رسیدن اعلان",
                sound));
        sound.setOnCheckedChangeListener(
                (button, on) -> onSoundChanged(on));
        card.addView(divider());

        soundValue = valueView();
        card.addView(actionRow(
                "صدای عمومی",
                "پیش‌فرض همه شیفت‌ها؛ هر شیفت می‌تواند مستقل باشد",
                soundValue,
                this::chooseGlobalSound));
        card.addView(divider());

        vibration = new Switch(this);
        styleSwitch(vibration);
        card.addView(switchRow(
                "لرزش",
                "لرزش همزمان با اعلان",
                vibration));
        vibration.setOnCheckedChangeListener(
                (button, on) -> onVibrationChanged(on));

        return card;
    }

    private View shiftCard() {
        LinearLayout card = Ui.card(this);
        TextView title = Ui.title(
                this, "زمان و صدای هر شیفت", 17f);
        card.addView(title);

        TextView help = Ui.text(
                this,
                "اگر لازم است روزکار، عصرکار یا شبکار رفتار متفاوتی "
                        + "داشته باشند، فقط همان شیفت را باز کن.",
                13f,
                Ui.MUTED);
        help.setPadding(0, dp(5), 0, dp(12));
        card.addView(help);

        shiftSummary = Ui.text(this, "", 14f, Ui.TEXT);
        shiftSummary.setPadding(0, 0, 0, dp(10));
        card.addView(shiftSummary);

        Button edit = Ui.tonalButton(
                this, "تنظیم شیفت‌ها");
        edit.setOnClickListener(v -> openShiftList());
        card.addView(edit);

        return card;
    }

    private View healthCard() {
        LinearLayout card = Ui.card(this);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = Ui.title(this, "همه‌چیز آماده است؟", 17f);
        header.addView(
                title,
                new LinearLayout.LayoutParams(0, -2, 1f));

        zoneValue = statusPill("");
        header.addView(zoneValue);
        card.addView(header);

        healthStatus = Ui.text(this, "", 14f, Ui.TEXT);
        healthStatus.setPadding(0, dp(9), 0, dp(8));
        card.addView(healthStatus);

        notificationButton = Ui.tonalButton(
                this, "فعال کردن اعلان‌ها");
        notificationButton.setOnClickListener(
                v -> requestNotificationAccess());
        card.addView(
                notificationButton,
                Ui.margins(this, -1, dp(48), 0, 4, 0, 6));

        exactButton = Ui.tonalButton(
                this, "فعال کردن زمان‌بندی دقیق");
        exactButton.setOnClickListener(
                v -> requestExactAlarmPermission());
        card.addView(
                exactButton,
                Ui.margins(this, -1, dp(48), 0, 4, 0, 6));

        scheduleStatus = Ui.text(this, "", 12f, Ui.MUTED);
        scheduleStatus.setPadding(0, dp(5), 0, dp(10));
        card.addView(scheduleStatus);

        Button advanced = Ui.tonalButton(
                this, "تنظیمات پیشرفته و عیب‌یابی");
        advanced.setOnClickListener(v -> openAdvanced());
        card.addView(advanced);

        return card;
    }

    private View sectionTitle(String text) {
        TextView title = Ui.title(this, text, 18f);
        title.setPadding(dp(2), dp(20), dp(2), dp(8));
        return title;
    }

    private View actionRow(
            String title,
            String subtitle,
            TextView value,
            Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(2), dp(8), dp(2), dp(8));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.rounded(
                Ui.CARD, 14, this));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView heading = Ui.title(this, title, 15f);
        TextView sub = Ui.text(this, subtitle, 12f, Ui.MUTED);
        sub.setPadding(0, dp(2), 0, 0);
        copy.addView(heading);
        copy.addView(sub);
        row.addView(
                copy,
                new LinearLayout.LayoutParams(0, -2, 1f));

        value.setGravity(Gravity.CENTER);
        row.addView(
                value,
                new LinearLayout.LayoutParams(
                        dp(132), dp(42)));

        row.setOnClickListener(v -> action.run());
        value.setOnClickListener(v -> action.run());
        return row;
    }

    private void styleSwitch(Switch toggle) {
        if (Build.VERSION.SDK_INT < 21) return;
        int checkedTrack =
                (Ui.SECONDARY & 0x00ffffff) | 0x55000000;
        toggle.setThumbTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_checked},
                                new int[]{}
                        },
                        new int[]{Ui.SECONDARY, Ui.CARD}));
        toggle.setTrackTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_checked},
                                new int[]{}
                        },
                        new int[]{checkedTrack, Ui.BORDER}));
    }

    private View switchRow(
            String title, String subtitle, Switch toggle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(2), dp(7), dp(2), dp(7));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(Ui.title(this, title, 15f));
        TextView sub = Ui.text(this, subtitle, 12f, Ui.MUTED);
        sub.setPadding(0, dp(2), 0, 0);
        copy.addView(sub);
        row.addView(
                copy,
                new LinearLayout.LayoutParams(0, -2, 1f));

        toggle.setContentDescription(title);
        row.addView(
                toggle,
                new LinearLayout.LayoutParams(
                        dp(66), dp(48)));
        return row;
    }

    private View divider() {
        View line = new View(this);
        line.setBackgroundColor(Ui.BORDER);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(0, dp(3), 0, dp(3));
        line.setLayoutParams(params);
        return line;
    }

    private TextView valueView() {
        TextView value = Ui.text(
                this, "", 12f, Ui.PRIMARY);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setBackground(Ui.roundedStroke(
                Ui.SURFACE_ALT, Ui.BORDER, 14, this));
        value.setPadding(dp(8), dp(5), dp(8), dp(5));
        return value;
    }

    private TextView statusPill(String text) {
        TextView pill = Ui.text(
                this, text, 11f, Ui.PRIMARY);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(Ui.roundedStroke(
                Ui.SURFACE_ALT, Ui.BORDER, 14, this));
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        return pill;
    }

    private GradientDrawable heroBackground() {
        GradientDrawable background =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Ui.PRIMARY_DARK,
                                Ui.PRIMARY,
                                Ui.SECONDARY
                        });
        background.setCornerRadius(dp(24));
        return background;
    }

    private LinearLayout.LayoutParams weightedButtonParams(
            int weight, int startMargin, int endMargin) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0, dp(50), weight);
        params.setMargins(
                dp(startMargin), 0, dp(endMargin), 0);
        return params;
    }

    private void bindValues() {
        binding = true;
        enabled.setChecked(Prefs.reminderEnabled(this));
        sound.setChecked(Prefs.reminderSound(this));
        vibration.setChecked(Prefs.reminderVibration(this));
        binding = false;

        int lead = Prefs.reminderLeadMinutes(this);
        leadValue.setText(leadLabel(lead));

        soundValue.setText(soundSummary());
        shiftSummary.setText(shiftSummaryText());

        String zone = Prefs.REMINDER_ZONE_TEHRAN.equals(
                Prefs.reminderZone(this))
                ? "تهران"
                : "دستگاه";
        zoneValue.setText("زمان: " + zone);

        refreshHero();
        refreshHealth();
    }

    private void refreshHero() {
        boolean on = Prefs.reminderEnabled(this);
        int lead = Prefs.reminderLeadMinutes(this);
        enabled.setText(on ? "روشن" : "خاموش");
        heroState.setText(
                on
                        ? "فعال · " + leadLabel(lead)
                                + " · "
                                + (Prefs.reminderSound(this)
                                        ? "با صدا" : "بی‌صدا")
                        : "خاموش است؛ هیچ یادآوری جدیدی زمان‌بندی نمی‌شود.");
    }

    private String soundSummary() {
        String selected = shortSoundTitle(
                ShiftAudio.globalChoice(this));
        if (!Prefs.reminderSound(this)) {
            return "خاموش · " + selected;
        }
        return selected;
    }

    private String shortSoundTitle(String choice) {
        if ("silent".equals(choice)) return "بی‌صدا";
        if ("default_alarm".equals(choice)) return "زنگ هشدار";
        if (choice == null
                || choice.isEmpty()
                || "default_notification".equals(choice)) {
            return "اعلان پیش‌فرض";
        }
        return "صدای شخصی";
    }

    private String shiftSummaryText() {
        int working = 0;
        int enabledCount = 0;
        for (String code : ShiftExtras.visibleCodes(this)) {
            Profile profile = ShiftPreferences.get(this, code);
            if (!profile.working) continue;
            working++;
            if (profile.reminder
                    && DayWorkResolver.validWork(profile)) {
                enabledCount++;
            }
        }
        int overrides = ShiftAudio.overrideCount(this);
        String text = PersianDigits.of(
                enabledCount + " از " + working
                        + " شیفت کاری یادآوری فعال دارند");
        if (overrides > 0) {
            text += PersianDigits.of(
                    " · " + overrides + " صدای اختصاصی");
        }
        return text;
    }

    private void onMasterChanged(boolean on) {
        if (binding) return;

        if (on && !ReminderPermissionGate.allowEnable(this)) {
            bindValues();
            refreshHealth();
            return;
        }

        Prefs.setReminderEnabled(this, on);
        WidgetStudio.refresh(this);

        if (on) {
            nextStatus.setText("در حال ساخت زمان‌بندی…");
            requestSchedule(0);
        } else {
            scheduleGeneration++;
            if (pendingSchedule != null) {
                mainHandler.removeCallbacks(pendingSchedule);
                pendingSchedule = null;
            }
            Prefs.recordReminderSchedule(
                    this, "یادآوری شیفت غیرفعال است");
            // Queue one authoritative reconciliation instead of a naked
            // cancel. sync() is serialized and re-reads the latest enabled
            // preference when it actually runs, so rapid OFF -> ON cannot
            // end with a stale cancel winning after the newer state.
            ReminderScheduler.syncAsync(getApplicationContext());
            renderNext(null);
            refreshHealth();
        }
        refreshHero();
    }

    private void onSoundChanged(boolean on) {
        if (binding) return;
        Prefs.setReminderSound(this, on);
        ReminderNotifications.ensureChannels(this);
        soundValue.setText(soundSummary());
        refreshHero();
    }

    private void onVibrationChanged(boolean on) {
        if (binding) return;
        Prefs.setReminderVibration(this, on);
        ReminderNotifications.ensureChannels(this);
        refreshHero();
    }

    private void showLeadDialog() {
        String[] labels = leadLabels();
        int selected = indexOfLead(
                Prefs.reminderLeadMinutes(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("چه زمانی خبر بدهم؟")
                .setSingleChoiceItems(
                        labels,
                        selected,
                        null)
                .setNegativeButton("انصراف", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            android.widget.ListView list =
                    dialog.getListView();
            list.setOnItemClickListener(
                    (parent, view, position, id) -> {
                        Prefs.setReminderLeadMinutes(
                                this,
                                LEAD_VALUES[position]);
                        WidgetStudio.refresh(this);
                        leadValue.setText(
                                leadLabel(LEAD_VALUES[position]));
                        refreshHero();
                        dialog.dismiss();
                        requestSchedule(120);
                    });
        });
        dialog.show();
    }

    private void chooseGlobalSound() {
        String current = ShiftAudio.globalChoice(this);
        SoundPicker.open(
                this,
                current,
                choice -> {
                    ShiftAudio.setGlobalChoice(this, choice);
                    ReminderNotifications.ensureChannels(this);
                    soundValue.setText(soundSummary());
                    if (!Prefs.reminderSound(this)) {
                        Ui.toast(
                                this,
                                "صدا انتخاب شد؛ گزینه «صدا» فعلاً خاموش است.");
                    }
                });
    }

    private void openShiftList() {
        String[] codes = ShiftExtras.visibleCodes(this);
        if (codes.length == 0) {
            Ui.toast(this, "ابتدا یک نوع شیفت تعریف کنید");
            return;
        }

        String[] labels = new String[codes.length];
        for (int i = 0; i < codes.length; i++) {
            labels[i] = shiftListLabel(codes[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("تنظیم هر نوع شیفت")
                .setAdapter(
                        FancyDialogs.list(this, labels),
                        (dialog, which) ->
                                openShiftEditor(codes[which]))
                .setNegativeButton("بستن", null)
                .show();
    }

    private String shiftListLabel(String code) {
        Profile p = ShiftPreferences.get(this, code);
        String time = DayWorkResolver.validWork(p)
                ? PersianDigits.of(
                        ShiftPreferences.formatMinute(p.startMinute)
                                + " تا "
                                + ShiftPreferences.formatMinute(p.endMinute))
                : "ساعت کامل نشده";
        String state = !p.working
                ? "غیرکاری"
                : (p.reminder ? "یادآوری روشن" : "یادآوری خاموش");
        String sound = ShiftAudio.hasTypeOverride(this, code)
                ? "صدای مستقل"
                : "صدای عمومی";
        return p.title + "  ·  " + state
                + "\n" + time + "  ·  " + sound;
    }

    private void openShiftEditor(String code) {
        Profile original = ShiftPreferences.get(this, code);
        final int[] start = {original.startMinute};
        final int[] end = {original.endMinute};
        final boolean[] inheritSound = {
                !ShiftAudio.hasTypeOverride(this, code)
        };
        final String[] tone = {
                ShiftAudio.typeChoice(this, code)
        };

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(4));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView colorDot = new TextView(this);
        colorDot.setBackground(circle(original.color));
        heading.addView(
                colorDot,
                new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView name = Ui.title(
                this,
                original.title + "  ·  " + original.code,
                17f);
        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.setMargins(dp(9), 0, 0, 0);
        heading.addView(name, nameParams);
        form.addView(heading);

        TextView note = Ui.text(
                this,
                original.working
                        ? "یادآوری این شیفت، ساعت شروع و صدای آن را همین‌جا تنظیم کن."
                        : "این نوع شیفت غیرکاری است؛ یادآوری شروع برای آن فعال نمی‌شود.",
                12f,
                Ui.MUTED);
        note.setPadding(0, dp(6), 0, dp(10));
        form.addView(note);

        Switch reminder = new Switch(this);
        styleSwitch(reminder);
        reminder.setText("یادآوری این شیفت");
        reminder.setTextSize(14f);
        reminder.setTextColor(Ui.TEXT);
        reminder.setChecked(original.reminder);
        reminder.setEnabled(original.working);
        form.addView(
                reminder,
                new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        times.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button startButton = Ui.tonalButton(this, "");
        Button endButton = Ui.tonalButton(this, "");
        updateTimeButton(startButton, "شروع", start[0]);
        updateTimeButton(endButton, "پایان", end[0]);

        startButton.setEnabled(original.working);
        endButton.setEnabled(original.working);
        times.addView(
                startButton,
                weightedButtonParams(1, 0, 4));
        times.addView(
                endButton,
                weightedButtonParams(1, 4, 0));
        form.addView(times);

        Button toneButton = Ui.tonalButton(this, "");
        Runnable refreshToneButton = () -> {
            String summary = inheritSound[0]
                    ? "صدای عمومی: "
                            + SoundPicker.title(
                                    ShiftAudio.globalChoice(this))
                    : "صدای مستقل: "
                            + SoundPicker.title(tone[0]);
            toneButton.setText(summary);
        };
        refreshToneButton.run();
        toneButton.setEnabled(original.working);
        toneButton.setOnClickListener(
                v -> showShiftSoundDialog(
                        code,
                        inheritSound,
                        tone,
                        refreshToneButton));
        form.addView(
                toneButton,
                Ui.margins(this, -1, dp(50), 0, 10, 0, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تنظیم " + original.title)
                .setView(form)
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .create();

        startButton.setOnClickListener(v ->
                pickTime(
                        start[0] >= 0 ? start[0] : 8 * 60,
                        value -> {
                            start[0] = value;
                            updateTimeButton(
                                    startButton, "شروع", value);
                        }));
        endButton.setOnClickListener(v ->
                pickTime(
                        end[0] >= 0 ? end[0] : 16 * 60,
                        value -> {
                            end[0] = value;
                            updateTimeButton(
                                    endButton, "پایان", value);
                        }));

        dialog.setOnShowListener(ignored ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            if (original.working
                                    && !validShiftTimes(
                                            start[0], end[0])) {
                                Ui.toast(
                                        this,
                                        "برای شیفت کاری، ساعت شروع و پایان معتبر و متفاوت لازم است.");
                                return;
                            }

                            Profile updated =
                                    new Profile(
                                            original.code,
                                            original.title,
                                            original.shortTitle,
                                            start[0],
                                            end[0],
                                            original.color,
                                            original.working,
                                            original.working
                                                    && reminder.isChecked());
                            ShiftPreferences.save(this, updated);

                            if (inheritSound[0]) {
                                ShiftAudio.clearTypeChoice(
                                        this, code);
                            } else {
                                ShiftAudio.setTypeChoice(
                                        this, code, tone[0]);
                            }

                            WidgetStudio.refresh(this);
                            shiftSummary.setText(
                                    shiftSummaryText());
                            dialog.dismiss();
                            requestSchedule(0);
                        }));
        dialog.show();
    }

    private void showShiftSoundDialog(
            String code,
            boolean[] inherit,
            String[] tone,
            Runnable refresh) {
        String[] actions = {
                "استفاده از صدای عمومی",
                "انتخاب یا تغییر صدای مستقل"
        };
        new AlertDialog.Builder(this)
                .setTitle("صدای " + ShiftPreferences.title(this, code))
                .setItems(
                        actions,
                        (dialog, which) -> {
                            if (which == 0) {
                                inherit[0] = true;
                                tone[0] =
                                        ShiftAudio.globalChoice(this);
                                SoundPicker.stopPreview(this);
                                refresh.run();
                            } else {
                                String current = inherit[0]
                                        ? ShiftAudio.globalChoice(this)
                                        : tone[0];
                                SoundPicker.open(
                                        this,
                                        current,
                                        choice -> {
                                            inherit[0] = false;
                                            tone[0] = choice;
                                            refresh.run();
                                        });
                            }
                        })
                .setNegativeButton("بستن", null)
                .show();
    }

    private void pickTime(
            int currentMinute, MinuteCallback callback) {
        int minute = Math.max(0, Math.min(1439, currentMinute));
        new TimePickerDialog(
                this,
                (picker, hour, min) ->
                        callback.selected(hour * 60 + min),
                minute / 60,
                minute % 60,
                true)
                .show();
    }

    private void updateTimeButton(
            Button button, String title, int minute) {
        button.setText(
                title + ": "
                        + (minute < 0
                        ? "مشخص نشده"
                        : PersianDigits.of(
                                ShiftPreferences.formatMinute(minute))));
    }

    private boolean validShiftTimes(int start, int end) {
        return start >= 0
                && start < 1440
                && end >= 0
                && end < 1440
                && start != end;
    }

    private void openAdvanced() {
        String zone = Prefs.REMINDER_ZONE_TEHRAN.equals(
                Prefs.reminderZone(this))
                ? "زمان رسمی تهران"
                : "زمان محلی دستگاه";

        String[] actions = {
                "منطقه زمانی  ·  " + zone,
                "تنظیمات اعلان در اندروید",
                "تنظیمات باتری برنامه",
                "بازسازی زمان‌بندی یادآوری"
        };

        new AlertDialog.Builder(this)
                .setTitle("تنظیمات پیشرفته")
                .setItems(
                        actions,
                        (dialog, which) -> {
                            if (which == 0) {
                                showZoneDialog();
                            } else if (which == 1) {
                                openNotificationSettings();
                            } else if (which == 2) {
                                openBatteryGuide();
                            } else {
                                if (!Prefs.reminderEnabled(this)) {
                                    Ui.toast(
                                            this,
                                            "یادآوری خاموش است.");
                                } else {
                                    nextStatus.setText(
                                            "در حال بازسازی زمان‌بندی…");
                                    requestSchedule(0);
                                }
                            }
                        })
                .setNegativeButton("بستن", null)
                .show();
    }

    private void showZoneDialog() {
        String[] zones = {
                "زمان محلی دستگاه",
                "زمان رسمی تهران"
        };
        int selected = Prefs.REMINDER_ZONE_TEHRAN.equals(
                Prefs.reminderZone(this)) ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("منطقه زمانی یادآوری")
                .setSingleChoiceItems(
                        zones,
                        selected,
                        (dialog, which) -> {
                            Prefs.setReminderZone(
                                    this,
                                    which == 1
                                            ? Prefs.REMINDER_ZONE_TEHRAN
                                            : Prefs.REMINDER_ZONE_DEVICE);
                            dialog.dismiss();
                            bindValues();
                            requestSchedule(0);
                        })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void openBatteryGuide() {
        new AlertDialog.Builder(this)
                .setTitle("اجرای مطمئن در پس‌زمینه")
                .setMessage(
                        "در صفحه اطلاعات برنامه، بخش Battery / مصرف باتری را "
                                + "باز کنید و اگر دستگاه اجازه می‌دهد حالت "
                                + "Unrestricted / بدون محدودیت را انتخاب کنید.\n\n"
                                + "در برخی گوشی‌ها نام گزینه‌ها متفاوت است.")
                .setPositiveButton(
                        "رفتن به تنظیمات",
                        (dialog, which) -> openAppSettings())
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void requestNotificationAccess() {
        if (ReminderPermissionGate.notificationsAllowed(this)) {
            Ui.toast(this, "اعلان‌های اندروید فعال هستند.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                        "android.permission.POST_NOTIFICATIONS")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    REQUEST_NOTIFICATIONS_STANDALONE);
            return;
        }
        openNotificationSettings();
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < 31
                || ReminderScheduler.exactPermissionGranted(this)) {
            Ui.toast(
                    this,
                    "زمان‌بندی دقیق در دسترس است.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("زمان‌بندی دقیق")
                .setMessage(
                        "بدون این مجوز هم یادآوری ثبت می‌شود، "
                                + "اما اندروید ممکن است آن را کمی دیرتر تحویل دهد.")
                .setPositiveButton(
                        "باز کردن تنظیمات",
                        (dialog, which) -> {
                            try {
                                startActivity(
                                        new Intent(
                                                Settings
                                                        .ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse(
                                                        "package:"
                                                                + getPackageName())));
                            } catch (RuntimeException error) {
                                Ui.toast(
                                        this,
                                        "صفحه مجوز در این دستگاه در دسترس نیست.");
                            }
                        })
                .setNegativeButton("فعلاً نه", null)
                .show();
    }

    private void openNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startActivity(
                        new Intent(
                                Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(
                                        Settings.EXTRA_APP_PACKAGE,
                                        getPackageName()));
            } else {
                openAppSettings();
            }
        } catch (RuntimeException first) {
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            startActivity(
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse(
                                    "package:" + getPackageName())));
        } catch (RuntimeException error) {
            Ui.toast(
                    this,
                    "تنظیمات برنامه در این دستگاه باز نشد.");
        }
    }

    private void sendTest() {
        if (!ReminderPermissionGate.notificationsAllowed(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("اعلان‌ها غیرفعال هستند")
                    .setMessage(
                            "برای دیدن اعلان آزمایشی، اعلان‌های برنامه "
                                    + "را در تنظیمات اندروید فعال کنید.")
                    .setPositiveButton(
                            "باز کردن تنظیمات",
                            (dialog, which) ->
                                    openNotificationSettings())
                    .setNegativeButton("بستن", null)
                    .show();
            return;
        }

        ReminderNotifications.showTest(this);
        Ui.toast(
                this,
                "اعلان آزمایشی با تنظیمات صدای عمومی ارسال شد.");
    }

    private void requestSchedule(long delayMillis) {
        if (!Prefs.reminderEnabled(this)) {
            renderNext(null);
            refreshHealth();
            return;
        }

        final int generation = ++scheduleGeneration;
        if (pendingSchedule != null) {
            mainHandler.removeCallbacks(pendingSchedule);
        }

        pendingSchedule = () -> {
            pendingSchedule = null;
            Context app = getApplicationContext();
            new Thread(() -> {
                ReminderScheduler.ScheduleResult result =
                        ReminderScheduler.sync(app);
                runOnUiThread(() -> {
                    if (generation != scheduleGeneration
                            || isFinishing()
                            || isDestroyed()) return;
                    previewGeneration++;
                    refreshHealth();
                    renderNext(result.next);
                });
            }, "eluqen-reminder-settings-sync").start();
        };

        if (delayMillis <= 0) {
            mainHandler.post(pendingSchedule);
        } else {
            mainHandler.postDelayed(
                    pendingSchedule, delayMillis);
        }
    }

    private void refreshHealth() {
        boolean allowed =
                ReminderPermissionGate.notificationsAllowed(this);
        boolean exact =
                ReminderScheduler.exactPermissionGranted(this);

        String notificationLine = allowed
                ? "● اعلان‌های اندروید آماده‌اند"
                : "● اعلان‌های اندروید نیاز به فعال‌سازی دارند";
        String exactLine = Build.VERSION.SDK_INT < 31 || exact
                ? "● زمان‌بندی دقیق در دسترس است"
                : "● زمان‌بندی تقریبی فعال است؛ مجوز دقیق اختیاری است";
        String soundLine = Prefs.reminderSound(this)
                ? "● صدا: " + SoundPicker.title(
                        ShiftAudio.globalChoice(this))
                : "● صدا خاموش است";

        healthStatus.setText(
                notificationLine + "\n"
                        + exactLine + "\n"
                        + soundLine);
        healthStatus.setTextColor(
                allowed ? Ui.TEXT : Ui.HOLIDAY);

        notificationButton.setVisibility(
                allowed ? View.GONE : View.VISIBLE);
        exactButton.setVisibility(
                Build.VERSION.SDK_INT >= 31 && !exact
                        ? View.VISIBLE
                        : View.GONE);

        scheduleStatus.setText(
                ReminderPanel.scheduleStatusText(this));

        String zone = Prefs.REMINDER_ZONE_TEHRAN.equals(
                Prefs.reminderZone(this))
                ? "تهران"
                : "دستگاه";
        zoneValue.setText("زمان: " + zone);
    }

    private void renderNext(
            ReminderScheduler.NextReminder next) {
        if (!Prefs.reminderEnabled(this)) {
            nextStatus.setText(
                    "یادآوری خاموش است. با کلید بالای صفحه فعالش کن.");
            return;
        }
        if (next == null) {
            nextStatus.setText(
                    "با وضعیت فعلی شیفت‌ها و کارکرد، "
                            + "یادآوری آینده‌ای پیدا نشد.");
            return;
        }

        nextStatus.setText(
                PersianDigits.of(
                        next.title
                                + "\n"
                                + next.date.displayLong()
                                + " · اعلان: "
                                + next.triggerDisplay()
                                + "\n"
                                + next.message));
    }

    private String leadLabel(int minutes) {
        if (minutes == 0) {
            return "لحظه شروع";
        }
        if (minutes < 60) {
            return PersianDigits.of(
                    minutes + " دقیقه قبل");
        }
        if (minutes == 1440) {
            return "۱ روز قبل";
        }
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (rest == 0) {
            return PersianDigits.of(
                    hours + " ساعت قبل");
        }
        return PersianDigits.of(
                hours + " ساعت و " + rest + " دقیقه قبل");
    }

    private String[] leadLabels() {
        String[] labels = new String[LEAD_VALUES.length];
        for (int i = 0; i < LEAD_VALUES.length; i++) {
            labels[i] = leadLabel(LEAD_VALUES[i]);
        }
        return labels;
    }

    private int indexOfLead(int minutes) {
        for (int i = 0; i < LEAD_VALUES.length; i++) {
            if (LEAD_VALUES[i] == minutes) return i;
        }
        return 3;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setStroke(dp(1), Ui.BORDER);
        return dot;
    }

    private int dp(float value) {
        return Ui.dp(this, value);
    }

    private interface MinuteCallback {
        void selected(int minute);
    }
}
