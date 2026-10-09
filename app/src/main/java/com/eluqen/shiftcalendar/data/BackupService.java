package com.eluqen.shiftcalendar.data;

import android.content.Context;
import android.net.Uri;
import com.eluqen.shiftcalendar.core.GregorianDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.ui.GeneralWork;
import com.eluqen.shiftcalendar.ui.NotebookActivity;
import com.eluqen.shiftcalendar.ui.PersonalStore;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import com.eluqen.shiftcalendar.update.UpdateSource;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

public final class BackupService {
    private static final int BUFFER_SIZE = 16384;
    private static final int MAX_JSON_BYTES = 33554432;

    private BackupService() {
    }

    public static void writeBackup(Context context, Uri uri) throws Exception {
        JSONObject jSONObjectExportJson = AppDatabase.get(context).exportJson(Prefs.hijriManualOffset(context));
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("autoAppUpdate", Prefs.autoAppUpdate(context));
        jSONObject.put("autoDataUpdate", Prefs.autoDataUpdate(context));
        jSONObject.put("updateNetwork", Prefs.updateNetwork(context));
        jSONObject.put("updateSource", Prefs.updateSource(context).key);
        jSONObject.put("defaultScreen", Prefs.defaultScreen(context));
        jSONObject.put("excludeLunchHour", Prefs.excludeLunchHour(context));
        jSONObject.put("reminderEnabled", Prefs.reminderEnabled(context));
        jSONObject.put("reminderLeadMinutes", Prefs.reminderLeadMinutes(context));
        jSONObject.put("reminderSound", Prefs.reminderSound(context));
        jSONObject.put("reminderVibration", Prefs.reminderVibration(context));
        jSONObject.put("reminderZone", Prefs.reminderZone(context));
        jSONObject.put("widgetTheme", Prefs.widgetTheme(context));
        jSONObject.put("widgetShowGroups", Prefs.widgetShowGroups(context));
        jSONObject.put("widgetShowNext", Prefs.widgetShowNext(context));
        jSONObjectExportJson.put("settings", jSONObject);
        jSONObjectExportJson.put("shiftTypes", ShiftPreferences.exportJson(context));
        jSONObjectExportJson.put("personalPlanner", PersonalStore.exportJson(context));
        jSONObjectExportJson.put("notebook", NotebookActivity.exportJson(context));
        OutputStream outputStreamOpenOutputStream = context.getContentResolver().openOutputStream(uri, "wt");
        if (outputStreamOpenOutputStream == null) {
            throw new IOException("فایل خروجی باز نشد");
        }
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStreamOpenOutputStream, StandardCharsets.UTF_8);
        try {
            outputStreamWriter.write(jSONObjectExportJson.toString(2));
            outputStreamWriter.flush();
        } finally {
            outputStreamWriter.close();
        }
    }

    public static AppDatabase.ImportResult restore(Context context, Uri uri) throws Exception {
        InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        if (inputStreamOpenInputStream == null) {
            throw new IOException("فایل ورودی باز نشد");
        }
        try {
            JSONObject jSONObject = new JSONObject(new String(readLimited(inputStreamOpenInputStream, MAX_JSON_BYTES), StandardCharsets.UTF_8));
            AppDatabase.ImportResult importResultImportJson = AppDatabase.get(context).importJson(jSONObject);
            Prefs.setHijriManualOffset(context, importResultImportJson.hijriOffset);
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("settings");
            if (jSONObjectOptJSONObject != null) {
                Prefs.setAutoAppUpdate(context, jSONObjectOptJSONObject.optBoolean("autoAppUpdate", true));
                Prefs.setAutoDataUpdate(context, jSONObjectOptJSONObject.optBoolean("autoDataUpdate", true));
                Prefs.setUpdateNetwork(context, jSONObjectOptJSONObject.optString("updateNetwork", Prefs.NETWORK_ANY));
                Prefs.setUpdateSource(context, UpdateSource.fromKey(jSONObjectOptJSONObject.optString("updateSource", ""), Prefs.updateSource(context)));
                Prefs.setDefaultScreen(context, jSONObjectOptJSONObject.optString("defaultScreen", Prefs.SCREEN_CALENDAR));
                Prefs.setExcludeLunchHour(context, jSONObjectOptJSONObject.optBoolean("excludeLunchHour", false));
                Prefs.setReminderEnabled(context, jSONObjectOptJSONObject.optBoolean("reminderEnabled", false));
                Prefs.setReminderLeadMinutes(context, jSONObjectOptJSONObject.optInt("reminderLeadMinutes", 30));
                Prefs.setReminderSound(context, jSONObjectOptJSONObject.optBoolean("reminderSound", true));
                Prefs.setReminderVibration(context, jSONObjectOptJSONObject.optBoolean("reminderVibration", true));
                Prefs.setReminderZone(context, jSONObjectOptJSONObject.optString("reminderZone", Prefs.REMINDER_ZONE_DEVICE));
                Prefs.setWidgetTheme(context, jSONObjectOptJSONObject.optString("widgetTheme", Prefs.WIDGET_DARK));
                Prefs.setWidgetShowGroups(context, jSONObjectOptJSONObject.optBoolean("widgetShowGroups", true));
                Prefs.setWidgetShowNext(context, jSONObjectOptJSONObject.optBoolean("widgetShowNext", true));
            }
            ShiftPreferences.importJson(context, jSONObject.optJSONObject("shiftTypes"));
            PersonalStore.importJson(context, jSONObject.optJSONObject("personalPlanner"));
            NotebookActivity.importJson(context, jSONObject.optJSONArray("notebook"));
            ShiftWidgetProvider.updateAll(context);
            ReminderScheduler.sync(context);
            return importResultImportJson;
        } finally {
            inputStreamOpenInputStream.close();
        }
    }

    public static void writeIcs(Context context, Uri uri, PersianDate persianDate, PersianDate persianDate2) throws Exception {
        long jMin = Math.min(persianDate.toJdn(), persianDate2.toJdn());
        long jMax = Math.max(persianDate.toJdn(), persianDate2.toJdn());
        if (jMax - jMin > 3660) {
            throw new IllegalArgumentException("بازهٔ خروجی بیشتر از ده سال است");
        }
        ShiftPlan shiftPlanActivePlan = AppDatabase.get(context).activePlan();
        OutputStream outputStreamOpenOutputStream = context.getContentResolver().openOutputStream(uri, "wt");
        if (outputStreamOpenOutputStream == null) {
            throw new IOException("فایل خروجی باز نشد");
        }
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStreamOpenOutputStream, StandardCharsets.UTF_8);
        try {
            outputStreamWriter.write("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ELUQEN//Taghvim Shift//FA\r\nCALSCALE:GREGORIAN\r\n");
            while (jMin <= jMax) {
                PersianDate persianDateFromJdn = JalaliCalendar.fromJdn(jMin);
                GregorianDate gregorianDateFromGregorianJdn = JalaliCalendar.fromGregorianJdn(jMin);
                com.eluqen.shiftcalendar.work.DayWorkResolver.State work = com.eluqen.shiftcalendar.work.DayWorkResolver.resolve(context, persianDateFromJdn, shiftPlanActivePlan);
                outputStreamWriter.write("BEGIN:VEVENT\r\n");
                outputStreamWriter.write("UID:" + jMin + "-" + shiftPlanActivePlan.id + "@" + StorageIdentity.ICS_UID_DOMAIN + "\r\n");
                outputStreamWriter.write(String.format(Locale.US, "DTSTART;VALUE=DATE:%04d%02d%02d\r\n", Integer.valueOf(gregorianDateFromGregorianJdn.year), Integer.valueOf(gregorianDateFromGregorianJdn.month), Integer.valueOf(gregorianDateFromGregorianJdn.day)));
                StringBuilder sb = new StringBuilder();
                sb.append("SUMMARY:");
                sb.append(icsEscape("شیفت: " + work.actualTitle()));
                sb.append("\r\n");
                outputStreamWriter.write(sb.toString());
                StringBuilder sb2 = new StringBuilder();
                sb2.append("DESCRIPTION:");
                sb2.append(icsEscape(persianDateFromJdn.displayLong() + " — " + shiftPlanActivePlan.displayName()));
                sb2.append("\r\n");
                outputStreamWriter.write(sb2.toString());
                outputStreamWriter.write("END:VEVENT\r\n");
                jMin++;
            }
            outputStreamWriter.write("END:VCALENDAR\r\n");
            outputStreamWriter.flush();
        } finally {
            outputStreamWriter.close();
        }
    }

    private static byte[] readLimited(InputStream inputStream, int i) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(65536);
        byte[] bArr = new byte[BUFFER_SIZE];
        int i2 = 0;
        while (true) {
            int i3 = inputStream.read(bArr);
            if (i3 < 0) {
                return byteArrayOutputStream.toByteArray();
            }
            i2 += i3;
            if (i2 > i) {
                throw new IOException("فایل پشتیبان بیش از حد بزرگ است");
            }
            byteArrayOutputStream.write(bArr, 0, i3);
        }
    }

    private static String icsEscape(String str) {
        return str.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}
