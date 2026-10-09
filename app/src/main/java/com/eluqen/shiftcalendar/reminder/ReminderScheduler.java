package com.eluqen.shiftcalendar.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.ShiftPlan;
import com.eluqen.shiftcalendar.data.ShiftPreferences;
import com.eluqen.shiftcalendar.ui.ShiftOccurrence;
import com.eluqen.shiftcalendar.work.DayWorkResolver;
import com.eluqen.shiftcalendar.work.OvertimePolicy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

public final class ReminderScheduler {
    public static final String ACTION_FIRE =
            "com.eluqen.shiftcalendar.action.SHIFT_REMINDER";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_PLAN = "plan";
    public static final String EXTRA_SHIFT_CODE = "shift_code";
    public static final String EXTRA_SHIFT_TIME = "shift_time";
    public static final String EXTRA_SHIFT_TITLE = "shift_title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_REMINDER_KIND = "reminder_kind";

    public static final String EXTRA_NOMINAL_DATE = "nominal_date";
    public static final String EXTRA_TRIGGER_AT = "original_trigger_at";

    public static final String KIND_SHIFT = "shift";
    public static final String KIND_OVERTIME = "overtime";

    private static final int MAX_ALARMS = 128;
    private static final int REQUEST_BASE = 31000;
    private static final int SNOOZE_BASE = 39000;
    private static final int SNOOZE_COUNT = 700;
    private static final int SEARCH_DAYS = 370;

    private ReminderScheduler() {}

    public static void syncAsync(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> sync(app), "eluqen-shift-calendar-reminder-resync").start();
    }

    public static synchronized ScheduleResult sync(Context context) {
        Context app = context.getApplicationContext();
        cancelAll(app);
        ReminderNotifications.ensureChannels(app);

        if (!Prefs.reminderEnabled(app)) {
            Prefs.recordReminderSchedule(app, "یادآوری شیفت غیرفعال است");
            return new ScheduleResult(0, null, true);
        }

        AlarmManager alarmManager =
                (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Prefs.recordReminderSchedule(
                    app, "سرویس زمان‌بندی دستگاه در دسترس نیست");
            return new ScheduleResult(0, null, false);
        }

        boolean exact = canScheduleExact(alarmManager);
        List<NextReminder> reminders = collectUpcoming(
                app, MAX_ALARMS, exact);

        int count = 0;
        for (NextReminder reminder : reminders) {
            PendingIntent pending = alarmIntent(
                    app,
                    alarmRequestCode(count),
                    reminder,
                    notificationId(
                            reminder.date,
                            reminder.kind + ":" + reminder.code));
            schedule(
                    alarmManager,
                    reminder.triggerAtMillis,
                    pending,
                    exact);
            count++;
        }

        String mode = exact
                ? "دقیق"
                : "تقریبی؛ مجوز زمان‌بندی دقیق داده نشده";
        String status = count == 0
                ? "در وضعیت کاری فعلی یادآوری آینده‌ای پیدا نشد"
                : count + " یادآوری آینده با زمان‌بندی " + mode + " ثبت شد";
        Prefs.recordReminderSchedule(app, status);

        return new ScheduleResult(
                count,
                reminders.isEmpty() ? null : reminders.get(0),
                exact);
    }

    public static NextReminder previewNext(Context context) {
        List<NextReminder> items = previewUpcoming(context, 1);
        return items.isEmpty() ? null : items.get(0);
    }

    public static List<NextReminder> previewUpcoming(Context context, int limit) {
        if (!Prefs.reminderEnabled(context)) {
            return Collections.emptyList();
        }
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        boolean exact = alarmManager != null && canScheduleExact(alarmManager);
        return collectUpcoming(
                context.getApplicationContext(),
                Math.max(1, Math.min(limit, MAX_ALARMS)),
                exact);
    }

    private static List<NextReminder> collectUpcoming(
            Context context, int limit, boolean exact) {
        TimeZone zone = selectedTimeZone(context);
        PersianDate today = PersianDate.today(zone);
        AppDatabase database = AppDatabase.get(context);
        ShiftPlan plan = database.activePlan();
        long now = System.currentTimeMillis();
        int lead = Prefs.reminderLeadMinutes(context);
        ArrayList<NextReminder> candidates = new ArrayList<>();

        // A reminder rebuild scans up to 370 days. Read Cardex once for that
        // whole window rather than issuing one SQLite query for every date.
        PersianDate first = today.plusDays(-1);
        PersianDate last = today.plusDays(SEARCH_DAYS - 1);
        Map<String, CardexRecord> cardexByDate = new HashMap<>();
        for (CardexRecord record :
                database.cardexBetween(first.toJdn(), last.toJdn())) {
            if (record != null && record.date != null) {
                cardexByDate.put(record.date, record);
            }
        }

        for (int day = -1; day < SEARCH_DAYS; day++) {
            PersianDate date = today.plusDays(day);
            DayWorkResolver.State state =
                    DayWorkResolver.resolve(
                            context,
                            date,
                            plan,
                            cardexByDate.get(date.toString()));

            candidates.addAll(remindersForDay(state, zone, lead, now, exact));
        }

        Collections.sort(candidates, new Comparator<NextReminder>() {
            @Override
            public int compare(NextReminder left, NextReminder right) {
                if (left.triggerAtMillis < right.triggerAtMillis) return -1;
                if (left.triggerAtMillis > right.triggerAtMillis) return 1;
                return 0;
            }
        });
        if (candidates.size() > limit) {
            return new ArrayList<>(candidates.subList(0, limit));
        }
        return candidates;
    }

    public static List<NextReminder> remindersForDay(
            DayWorkResolver.State state, TimeZone zone, int lead, long now, boolean exact) {
        ArrayList<NextReminder> out = new ArrayList<>();
        addShiftCandidate(null, state, zone, lead, now, exact, out);
        addOvertimeCandidate(null, state, zone, lead, now, exact, out);
        return out;
    }

    /** Revalidate queued/snoozed alarms against today's persisted operational truth. */
    public static boolean isCurrent(Context context, Intent intent) {
        if (!Prefs.reminderEnabled(context)) return false;
        try {
            PersianDate nominal = PersianDate.parse(intent.getStringExtra(EXTRA_NOMINAL_DATE));
            DayWorkResolver.State state = DayWorkResolver.resolve(context, nominal, AppDatabase.get(context).activePlan());
            for (NextReminder item : remindersForDay(state, selectedTimeZone(context),
                    Prefs.reminderLeadMinutes(context), Long.MIN_VALUE / 4, true)) {
                if (item.kind.equals(intent.getStringExtra(EXTRA_REMINDER_KIND))
                        && item.code.equals(intent.getStringExtra(EXTRA_SHIFT_CODE))
                        && item.triggerAtMillis == intent.getLongExtra(EXTRA_TRIGGER_AT, Long.MIN_VALUE)
                        && item.plan.equals(intent.getStringExtra(EXTRA_PLAN))) return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    private static void addShiftCandidate(
            Context context,
            DayWorkResolver.State state,
            TimeZone zone,
            int lead,
            long now,
            boolean exact,
            List<NextReminder> out) {
        if (!state.shouldScheduleShift()) return;

        long trigger = state.shiftReminderStartEpoch(context, zone)
                - lead * 60000L;
        if (trigger <= now + 5000L) return;

        ShiftPreferences.Profile profile = state.effectiveProfile;
        PersianDate startDate = DayWorkResolver.dateAt(
                state.shiftReminderStartEpoch(context, zone), zone);
        String start = ShiftPreferences.formatMinute(
                state.shiftReminderStartMinute());
        String message;
        if (state.reminderStartsAfterAbsence()) {
            message = state.absenceTitle() + " تا ساعت " + start
                    + " ثبت شده است؛ ادامه شیفت " + profile.title
                    + " از ساعت " + start + ".";
        } else {
            message = state.swapApplied
                    ? "تعویض شیفت: " + profile.title
                        + " ساعت " + start + " شروع می‌شود."
                    : "شیفت " + profile.title + " ساعت " + start + " شروع می‌شود.";
        }

        out.add(new NextReminder(
                startDate, state.date,
                state.effectiveCode,
                state.effectiveTitle(),
                start,
                state.plan.displayName(),
                trigger,
                exact,
                zone.getID(),
                KIND_SHIFT,
                message));
    }

    private static void addOvertimeCandidate(
            Context context,
            DayWorkResolver.State state,
            TimeZone zone,
            int lead,
            long now,
            boolean exact,
            List<NextReminder> out) {
        if (!state.hasOvertime) return;

        long trigger = state.overtimeStartEpoch(context, zone)
                - lead * 60000L;
        if (trigger <= now + 5000L) return;

        String start =
                ShiftPreferences.formatMinute(state.overtimeStartMinute);
        String end =
                ShiftPreferences.formatMinute(state.overtimeEndMinute);
        boolean stay = state.overtimeContinuesWork(context, zone);
        boolean before = state.overtimeEndsAtOwnShiftStart(context, zone);

        String overtimeType = state.cardex == null
                ? "" : OvertimePolicy.typeTitle(state.cardex.overtimeType);
        String title = overtimeType.isEmpty()
                ? (stay ? "اضافه‌کاری — ادامه حضور"
                : (before ? "اضافه‌کاری — قبل از شیفت" : "اضافه‌کاری"))
                : "اضافه‌کاری — " + overtimeType;
        String message = stay
                ? "اضافه‌کاری پس از نوبت اصلی ثبت شده است؛ "
                    + state.overtimeTitle() + "، "
                    + start + " تا " + end + "."
                : (before
                    ? "اضافه‌کاری پیش از نوبت اصلی ثبت شده است؛ "
                        + state.overtimeTitle() + "، "
                        + start + " تا " + end + "."
                    : "اضافه‌کاری برای " + state.overtimeTitle()
                        + " ثبت شده است؛ " + start + " تا " + end + ".");

        PersianDate startDate = state.overtimeStartDate(zone);
        String code = state.overtimeProfile == null
                ? state.effectiveCode
                : state.overtimeProfile.code;
        out.add(new NextReminder(
                startDate, state.date,
                code,
                title,
                start,
                state.plan.displayName(),
                trigger,
                exact,
                zone.getID(),
                KIND_OVERTIME,
                message));
    }

    public static boolean exactPermissionGranted(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && canScheduleExact(alarmManager);
    }

    public static synchronized void cancelAll(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < MAX_ALARMS; i++) {
            cancelRequest(
                    context, alarmManager, REQUEST_BASE + i);
        }
        for (int i = 0; i < SNOOZE_COUNT; i++) {
            cancelRequest(
                    context, alarmManager, SNOOZE_BASE + i);
        }
    }

    private static void cancelRequest(
            Context context, AlarmManager alarmManager, int requestCode) {
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, ShiftReminderReceiver.class)
                        .setAction(ACTION_FIRE),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) {
            alarmManager.cancel(pending);
            pending.cancel();
        }
    }

    static void scheduleSnooze(Context context, Intent intent, int minutes) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent action = new Intent(
                context, ShiftReminderReceiver.class).setAction(ACTION_FIRE);
        copyExtras(intent, action);
        int notificationId =
                intent.getIntExtra(EXTRA_NOTIFICATION_ID, 39001);
        int requestCode =
                SNOOZE_BASE + Math.abs(notificationId % SNOOZE_COUNT);
        schedule(
                alarmManager,
                System.currentTimeMillis()
                        + Math.max(1, minutes) * 60000L,
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        action,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE),
                canScheduleExact(alarmManager));
    }

    static TimeZone selectedTimeZone(Context context) {
        return Prefs.REMINDER_ZONE_TEHRAN.equals(
                Prefs.reminderZone(context))
                ? TimeZone.getTimeZone("Asia/Tehran")
                : TimeZone.getDefault();
    }

    private static PendingIntent alarmIntent(
            Context context,
            int requestCode,
            NextReminder reminder,
            int notificationId) {
        Intent action = new Intent(
                context, ShiftReminderReceiver.class).setAction(ACTION_FIRE);
        action.putExtra(EXTRA_DATE, reminder.date.toString());
        action.putExtra(EXTRA_NOMINAL_DATE, reminder.nominalDate.toString());
        action.putExtra(EXTRA_TRIGGER_AT, reminder.triggerAtMillis);
        action.putExtra(EXTRA_SHIFT_CODE, reminder.code);
        action.putExtra(EXTRA_SHIFT_TITLE, reminder.title);
        action.putExtra(EXTRA_SHIFT_TIME, reminder.shiftTime);
        action.putExtra(EXTRA_PLAN, reminder.plan);
        action.putExtra(EXTRA_REMINDER_KIND, reminder.kind);
        action.putExtra(EXTRA_MESSAGE, reminder.message);
        action.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                action,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void copyExtras(Intent source, Intent target) {
        target.putExtra(EXTRA_DATE, source.getStringExtra(EXTRA_DATE));
        target.putExtra(EXTRA_NOMINAL_DATE, source.getStringExtra(EXTRA_NOMINAL_DATE));
        target.putExtra(EXTRA_TRIGGER_AT, source.getLongExtra(EXTRA_TRIGGER_AT, Long.MIN_VALUE));
        target.putExtra(
                EXTRA_SHIFT_CODE, source.getStringExtra(EXTRA_SHIFT_CODE));
        target.putExtra(
                EXTRA_SHIFT_TITLE, source.getStringExtra(EXTRA_SHIFT_TITLE));
        target.putExtra(
                EXTRA_SHIFT_TIME, source.getStringExtra(EXTRA_SHIFT_TIME));
        target.putExtra(EXTRA_PLAN, source.getStringExtra(EXTRA_PLAN));
        target.putExtra(
                EXTRA_REMINDER_KIND,
                source.getStringExtra(EXTRA_REMINDER_KIND));
        target.putExtra(EXTRA_MESSAGE, source.getStringExtra(EXTRA_MESSAGE));
        target.putExtra(
                EXTRA_NOTIFICATION_ID,
                source.getIntExtra(EXTRA_NOTIFICATION_ID, 39001));
    }

    private static boolean canScheduleExact(AlarmManager alarmManager) {
        return Build.VERSION.SDK_INT < 31
                || alarmManager.canScheduleExactAlarms();
    }

    private static void schedule(
            AlarmManager alarmManager,
            long trigger,
            PendingIntent pendingIntent,
            boolean exact) {
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
            }
        } catch (SecurityException ignored) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
        }
    }

    public static int notificationId(PersianDate date, String key) {
        // Exactly two disjoint identities per civil date, without a string hash collision.
        return 41000 + (int) Math.floorMod(date.toJdn(), 10000000L) * 2
                + (key.startsWith(KIND_OVERTIME) ? 1 : 0);
    }

    public static int alarmRequestCode(int index) {
        if (index < 0 || index >= MAX_ALARMS) throw new IllegalArgumentException("Alarm index");
        return REQUEST_BASE + index;
    }

    public static final class ScheduleResult {
        public final int count;
        public final boolean exact;
        public final NextReminder next;

        ScheduleResult(int count, NextReminder next, boolean exact) {
            this.count = count;
            this.next = next;
            this.exact = exact;
        }
    }

    public static final class NextReminder {
        public final String code;
        public final PersianDate date;
        public final PersianDate nominalDate;
        public final boolean exact;
        public final String plan;
        public final String shiftTime;
        public final String title;
        public final long triggerAtMillis;
        public final String zoneId;
        public final String kind;
        public final String message;

        NextReminder(
                PersianDate date, PersianDate nominalDate,
                String code,
                String title,
                String shiftTime,
                String plan,
                long triggerAtMillis,
                boolean exact,
                String zoneId,
                String kind,
                String message) {
            this.date = date;
            this.nominalDate = nominalDate;
            this.code = code;
            this.title = title;
            this.shiftTime = shiftTime;
            this.plan = plan;
            this.triggerAtMillis = triggerAtMillis;
            this.exact = exact;
            this.zoneId = zoneId;
            this.kind = kind;
            this.message = message;
        }

        public String triggerDisplay() {
            DateFormat format = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT,
                    new Locale("fa"));
            format.setTimeZone(TimeZone.getTimeZone(zoneId));
            return format.format(new Date(triggerAtMillis));
        }
    }
}
