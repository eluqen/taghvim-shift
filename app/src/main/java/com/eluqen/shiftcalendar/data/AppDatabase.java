package com.eluqen.shiftcalendar.data;

import com.eluqen.shiftcalendar.work.CardexCompanyPolicy;
import com.eluqen.shiftcalendar.work.OvertimeBalancePolicy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.ShiftEngine;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AppDatabase extends SQLiteOpenHelper {
    private static final String NAME = StorageIdentity.DATABASE_NAME;
    private static final int VERSION = 1;
    private static final int DB_VERSION = CardexSchema.VERSION;
    private static AppDatabase instance;
    private final Context appContext;

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        CardexSchema.upgrade(new CardexSchema.Store() {
            public boolean hasColumn(String name) { return AppDatabase.hasColumn(db, "cardex", name); }
            public void exec(String sql) { db.execSQL(sql); }
        });
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {
            instance = new AppDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private AppDatabase(Context context) {
        super(context, NAME, (SQLiteDatabase.CursorFactory) null, DB_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase sQLiteDatabase) {
        super.onConfigure(sQLiteDatabase);
        sQLiteDatabase.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE shift_plans (id INTEGER PRIMARY KEY AUTOINCREMENT, company TEXT NOT NULL, team TEXT NOT NULL, anchor_jdn INTEGER NOT NULL, pattern TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");
        sQLiteDatabase.execSQL(CardexSchema.CREATE);
        sQLiteDatabase.execSQL("CREATE INDEX cardex_jdn ON cardex(jdn)");
        sQLiteDatabase.execSQL("CREATE TABLE colleagues (id INTEGER PRIMARY KEY AUTOINCREMENT, team TEXT NOT NULL DEFAULT '', name TEXT NOT NULL, role TEXT NOT NULL DEFAULT '', mobile1 TEXT NOT NULL DEFAULT '', mobile2 TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '', info TEXT NOT NULL DEFAULT '')");
        sQLiteDatabase.execSQL("CREATE TABLE user_events (id INTEGER PRIMARY KEY AUTOINCREMENT, year INTEGER NOT NULL DEFAULT 0, month INTEGER NOT NULL, day INTEGER NOT NULL, title TEXT NOT NULL, holiday INTEGER NOT NULL DEFAULT 0)");
        seedPresets(sQLiteDatabase);
    }

    private static void seedPresets(SQLiteDatabase sQLiteDatabase) {
        addPlan(sQLiteDatabase, "پلی اکریل ایران", "A", 1398, VERSION, 5, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "پلی اکریل ایران", "B", 1398, VERSION, VERSION, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "پلی اکریل ایران", "C", 1398, VERSION, 7, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "پلی اکریل ایران", ShiftEngine.DAY, 1398, VERSION, 3, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "فولاد مبارکه", "A", 1398, VERSION, VERSION, "D,N,O,O", false);
        addPlan(sQLiteDatabase, "فولاد مبارکه", "B", 1398, VERSION, 3, "D,N,O,O", false);
        addPlan(sQLiteDatabase, "فولاد مبارکه", "C", 1398, VERSION, 4, "D,N,O,O", false);
        addPlan(sQLiteDatabase, "فولاد مبارکه", ShiftEngine.DAY, 1398, VERSION, 2, "D,N,O,O", false);
        addPlan(sQLiteDatabase, "ذوب آهن", "A", 1398, VERSION, VERSION, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "ذوب آهن", "B", 1398, VERSION, 3, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "ذوب آهن", "C", 1398, VERSION, 5, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "ذوب آهن", ShiftEngine.DAY, 1398, VERSION, 7, "D,D,E,E,O,N,N,O", false);
        addPlan(sQLiteDatabase, "جنرال", "روزکار", 1398, VERSION, VERSION, "O,O,D,D,D,D,D", true);
    }

    private static long addPlan(SQLiteDatabase sQLiteDatabase, String str, String str2, int i, int i2, int i3, String str3, boolean z) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("company", str);
        contentValues.put("team", str2);
        contentValues.put("anchor_jdn", Long.valueOf(JalaliCalendar.toJdn(i, i2, i3)));
        contentValues.put("pattern", str3);
        contentValues.put("active", Integer.valueOf(z ? 1 : 0));
        return sQLiteDatabase.insertOrThrow("shift_plans", null, contentValues);
    }

    /** Profiles referenced by saved overrides must not be deleted. */
    public java.util.Set<String> cardexShiftCodes() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT swap_shift_code, overtime_shift_code FROM cardex WHERE swap_enabled=1 OR overtime_enabled=1", null)) {
            while (cursor.moveToNext()) {
                for (int i = 0; i < 2; i++) {
                    String code = cursor.getString(i);
                    if (code != null && !code.trim().isEmpty()) codes.add(code.trim().toUpperCase(java.util.Locale.US));
                }
            }
        }
        return codes;
    }

    public List<ShiftPlan> plans() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query("shift_plans", null, null, null, null, null, "company, team");
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readPlan(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public ShiftPlan activePlan() {
        Cursor cursorQuery = getReadableDatabase().query("shift_plans", null, "active=1", null, null, null, "id", "1");
        try {
            if (cursorQuery.moveToFirst()) {
                ShiftPlan plan = readPlan(cursorQuery);
                cursorQuery.close();
                return plan;
            }
            cursorQuery.close();
            List<ShiftPlan> listPlans = plans();
            if (listPlans.isEmpty()) {
                throw new IllegalStateException("هیچ الگوی شیفتی وجود ندارد");
            }
            setActivePlan(listPlans.get(0).id);
            return listPlans.get(0);
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    public void setActivePlan(long j) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("active", (Integer) 0);
            writableDatabase.update("shift_plans", contentValues, null, null);
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("active", Integer.valueOf(VERSION));
            writableDatabase.update("shift_plans", contentValues2, "id=?", new String[]{String.valueOf(j)});
            writableDatabase.setTransactionSuccessful();
        } finally {
            writableDatabase.endTransaction();
        }
    }

    public long savePlan(ShiftPlan shiftPlan) {
        long jInsertOrThrow;
        ContentValues contentValuesPlanValues = planValues(shiftPlan);
        if (shiftPlan.id > 0) {
            getWritableDatabase().update("shift_plans", contentValuesPlanValues, "id=?", new String[]{String.valueOf(shiftPlan.id)});
            jInsertOrThrow = shiftPlan.id;
        } else {
            jInsertOrThrow = getWritableDatabase().insertOrThrow("shift_plans", null, contentValuesPlanValues);
            shiftPlan.id = jInsertOrThrow;
        }
        if (shiftPlan.active) {
            setActivePlan(jInsertOrThrow);
        }
        return jInsertOrThrow;
    }

    public boolean deletePlan(long j) {
        return activePlan().id != j && getWritableDatabase().delete("shift_plans", "id=?", new String[]{String.valueOf(j)}) > 0;
    }

    public CardexRecord cardex(String str) {
        Cursor cursorQuery = getReadableDatabase().query(Prefs.SCREEN_CARDEX, null, "date=?", new String[]{str}, null, null, null);
        try {
            return cursorQuery.moveToFirst() ? readCardex(cursorQuery) : null;
        } finally {
            cursorQuery.close();
        }
    }

    public List<CardexRecord> recentCardex(int i) {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query(Prefs.SCREEN_CARDEX, null, null, null, null, null, "jdn DESC", String.valueOf(i));
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readCardex(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public List<CardexRecord> cardexBetween(long j, long j2) {
        long jMin = Math.min(j, j2);
        long jMax = Math.max(j, j2);
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query(Prefs.SCREEN_CARDEX, null, "jdn>=? AND jdn<=?", new String[]{String.valueOf(jMin), String.valueOf(jMax)}, null, null, "jdn");
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readCardex(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public void saveCardex(CardexRecord record) {
        if (record != null && record.date != null) {
            PersianDate date = PersianDate.parse(record.date);
            String companyPolicyError = CardexCompanyPolicy.validationError(
                    this, date, activePlan(), record);
            if (companyPolicyError != null) {
                throw new IllegalStateException(companyPolicyError);
            }
            if (!OvertimeBalancePolicy.mutationAllowed(
                    appContext, this, date, record, false)) {
                throw new IllegalStateException(
                        "موجودی اضافه‌کاری این ماه برای این مقدار استراحت کافی نیست");
            }
        }
        CardexMutations.save(record, cardexStore());
    }

    /** Atomically saves a group of Cardex mutations and refreshes consumers once. */
    public void saveCardexBatch(List<CardexRecord> records) {
        if (records == null || records.isEmpty()) return;
        final SQLiteDatabase db = getWritableDatabase();
        final ShiftPlan plan = activePlan();
        CardexMutations.Store store = new CardexMutations.Store() {
            public void write(java.util.Map<String,Object> values) {
                long inserted = db.insertWithOnConflict(
                        Prefs.SCREEN_CARDEX, null,
                        sqlContentValues(values), SQLiteDatabase.CONFLICT_REPLACE);
                if (inserted < 0) {
                    throw new android.database.sqlite.SQLiteException("Cardex batch save failed");
                }
            }
            public void delete(String date) {
                db.delete(Prefs.SCREEN_CARDEX, "date=?", new String[]{date});
            }
            public void changed() { /* one refresh after the transaction */ }
        };

        db.beginTransaction();
        try {
            for (CardexRecord record : records) {
                if (record == null || record.date == null) {
                    throw new IllegalArgumentException("Cardex batch record/date is required");
                }
                PersianDate date = PersianDate.parse(record.date);
                String companyPolicyError = CardexCompanyPolicy.validationError(
                        this, date, plan, record);
                if (companyPolicyError != null) {
                    throw new IllegalStateException(companyPolicyError);
                }
                if (!OvertimeBalancePolicy.mutationAllowed(
                        appContext, this, date, record, false)) {
                    throw new IllegalStateException(
                            "موجودی اضافه‌کاری این ماه برای این مقدار استراحت کافی نیست");
                }
                CardexMutations.save(record, store);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        cardexChanged();
    }

    public void deleteCardex(String date) {
        PersianDate parsed = PersianDate.parse(date);
        int requiredRest = OvertimeBalancePolicy.requiredRestReleaseMinutes(
                appContext, this, parsed, null, true);
        if (requiredRest > 0) {
            throw new IllegalStateException(
                    "اول "
                    + com.eluqen.shiftcalendar.core.PersianDigits.of(
                            CardexStats.formatDuration(requiredRest))
                    + " از استراحت‌های همین ماه را کم کن");
        }
        CardexMutations.delete(date, cardexStore());
    }

    private CardexMutations.Store cardexStore() {
        return new CardexMutations.Store() {
            public void write(java.util.Map<String,Object> values) {
                long inserted = getWritableDatabase().insertWithOnConflict(
                        Prefs.SCREEN_CARDEX, null, sqlContentValues(values), SQLiteDatabase.CONFLICT_REPLACE);
                if (inserted < 0) throw new android.database.sqlite.SQLiteException("Cardex save failed");
            }
            public void delete(String date) {
                getWritableDatabase().delete(Prefs.SCREEN_CARDEX, "date=?", new String[]{date});
            }
            public void changed() { cardexChanged(); }
        };
    }

    private void cardexChanged() {
        ReminderScheduler.syncAsync(appContext);
        com.eluqen.shiftcalendar.ui.WidgetStudio.refresh(appContext);
    }

    public List<Colleague> colleagues() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query("colleagues", null, null, null, null, null, "team, name");
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readColleague(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public long saveColleague(Colleague colleague) {
        ContentValues contentValuesColleagueValues = colleagueValues(colleague);
        if (colleague.f0id > 0) {
            getWritableDatabase().update("colleagues", contentValuesColleagueValues, "id=?", new String[]{String.valueOf(colleague.f0id)});
            return colleague.f0id;
        }
        colleague.f0id = getWritableDatabase().insertOrThrow("colleagues", null, contentValuesColleagueValues);
        return colleague.f0id;
    }

    public void deleteColleague(long j) {
        getWritableDatabase().delete("colleagues", "id=?", new String[]{String.valueOf(j)});
    }

    public List<UserEvent> userEvents(PersianDate persianDate) {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query("user_events", null, "month=? AND day=? AND (year=0 OR year=?)", new String[]{String.valueOf(persianDate.month), String.valueOf(persianDate.day), String.valueOf(persianDate.year)}, null, null, "id");
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readUserEvent(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public List<UserEvent> allUserEvents() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = getReadableDatabase().query("user_events", null, null, null, null, null, "month, day, year");
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(readUserEvent(cursorQuery));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public long saveUserEvent(UserEvent userEvent) {
        ContentValues contentValuesUserEventValues = userEventValues(userEvent);
        if (userEvent.f2id > 0) {
            getWritableDatabase().update("user_events", contentValuesUserEventValues, "id=?", new String[]{String.valueOf(userEvent.f2id)});
            return userEvent.f2id;
        }
        userEvent.f2id = getWritableDatabase().insertOrThrow("user_events", null, contentValuesUserEventValues);
        return userEvent.f2id;
    }

    public void deleteUserEvent(long j) {
        getWritableDatabase().delete("user_events", "id=?", new String[]{String.valueOf(j)});
    }

    public JSONObject exportJson(int i) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("format", StorageIdentity.BACKUP_FORMAT);
        jSONObject.put("version", VERSION);
        jSONObject.put("hijriOffset", i);
        jSONObject.put("plans", plansJson());
        jSONObject.put(Prefs.SCREEN_CARDEX, cardexJson());
        jSONObject.put("colleagues", colleaguesJson());
        jSONObject.put("events", eventsJson());
        return jSONObject;
    }

    public ImportResult importJson(JSONObject jSONObject) throws JSONException {
        String str = "team";
        String backupFormat = jSONObject.optString("format");
        if (!StorageIdentity.BACKUP_FORMAT.equals(backupFormat)
                && !StorageIdentity.LEGACY_BACKUP_FORMAT.equals(backupFormat)) {
            throw new JSONException("قالب فایل پشتیبان شناخته\u200cشده نیست");
        }
        ImportResult importResult = new ImportResult();
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("plans");
            if (jSONArrayOptJSONArray != null) {
                writableDatabase.delete("shift_plans", null, null);
                int i = 0;
                while (i < jSONArrayOptJSONArray.length()) {
                    JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("company", jSONObject2.optString("company", "سفارشی"));
                    contentValues.put(str, jSONObject2.optString(str, "A"));
                    contentValues.put("anchor_jdn", Long.valueOf(jSONObject2.getLong("anchorJdn")));
                    contentValues.put("pattern", jSONObject2.optString("pattern", ShiftEngine.OFF));
                    contentValues.put("active", Integer.valueOf(jSONObject2.optBoolean("active") ? 1 : 0));
                    writableDatabase.insertOrThrow("shift_plans", null, contentValues);
                    importResult.plans += VERSION;
                    i += VERSION;
                    jSONArrayOptJSONArray = jSONArrayOptJSONArray;
                    str = str;
                }
            }
            importResult.cardex = importArray(writableDatabase, Prefs.SCREEN_CARDEX, jSONObject.optJSONArray(Prefs.SCREEN_CARDEX));
            importResult.cardex = (int) queryLong(writableDatabase, "SELECT COUNT(*) FROM cardex");
            importResult.colleagues = importArray(writableDatabase, "colleagues", jSONObject.optJSONArray("colleagues"));
            importResult.events = importArray(writableDatabase, "events", jSONObject.optJSONArray("events"));
            if (queryLong(writableDatabase, "SELECT COUNT(*) FROM shift_plans") == 0) {
                seedPresets(writableDatabase);
            }
            if (queryLong(writableDatabase, "SELECT COUNT(*) FROM shift_plans WHERE active=1") == 0) {
                writableDatabase.execSQL("UPDATE shift_plans SET active=1 WHERE id=(SELECT MIN(id) FROM shift_plans)");
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
            importResult.hijriOffset = Math.max(-2, Math.min(2, jSONObject.optInt("hijriOffset", 0)));
            return importResult;
        } catch (Throwable th) {
            writableDatabase.endTransaction();
            throw th;
        }
    }

    private static String normalizedPattern(String str) {
        String[] pattern = ShiftEngine.parsePattern(str);
        StringBuilder sb = new StringBuilder();
        int length = pattern.length;
        for (int i = 0; i < length; i += VERSION) {
            String str2 = pattern[i];
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    private int importArray(SQLiteDatabase sQLiteDatabase, String str, JSONArray jSONArray) throws JSONException {
        if (jSONArray == null) {
            return 0;
        }
        String str2 = str.equals("events") ? "user_events" : str;
        sQLiteDatabase.delete(str2, null, null);
        int i = 0;
        for (int i2 = 0; i2 < jSONArray.length(); i2 += VERSION) {
            JSONObject jSONObject = jSONArray.getJSONObject(i2);
            ContentValues contentValues = new ContentValues();
            if (str.equals(Prefs.SCREEN_CARDEX)) {
                contentValues = cardexValues(CardexCodec.fromJson(jSONObject));
            } else if (str.equals("colleagues")) {
                contentValues.put("team", jSONObject.optString("team"));
                contentValues.put("name", jSONObject.optString("name"));
                contentValues.put("role", jSONObject.optString("role"));
                contentValues.put("mobile1", jSONObject.optString("mobile1"));
                contentValues.put("mobile2", jSONObject.optString("mobile2"));
                contentValues.put("phone", jSONObject.optString("phone"));
                contentValues.put("info", jSONObject.optString("info"));
            } else {
                contentValues.put("year", Integer.valueOf(jSONObject.optInt("year")));
                contentValues.put("month", Integer.valueOf(jSONObject.getInt("month")));
                contentValues.put("day", Integer.valueOf(jSONObject.getInt("day")));
                contentValues.put("title", jSONObject.optString("title"));
                contentValues.put("holiday", Integer.valueOf(jSONObject.optBoolean("holiday") ? 1 : 0));
            }
            sQLiteDatabase.insertOrThrow(str2, null, contentValues);
            i += VERSION;
        }
        return i;
    }

    private JSONArray plansJson() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (ShiftPlan shiftPlan : plans()) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("company", shiftPlan.company);
            jSONObject.put("team", shiftPlan.team);
            jSONObject.put("anchorJdn", shiftPlan.anchorJdn);
            jSONObject.put("pattern", shiftPlan.pattern);
            jSONObject.put("active", shiftPlan.active);
            jSONArray.put(jSONObject);
        }
        return jSONArray;
    }

    private JSONArray cardexJson() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (CardexRecord cardexRecord : recentCardex(Integer.MAX_VALUE)) {
            jSONArray.put(CardexCodec.toJson(cardexRecord));
        }
        return jSONArray;
    }

    private JSONArray colleaguesJson() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (Colleague colleague : colleagues()) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("team", colleague.team);
            jSONObject.put("name", colleague.name);
            jSONObject.put("role", colleague.role);
            jSONObject.put("mobile1", colleague.mobile1);
            jSONObject.put("mobile2", colleague.mobile2);
            jSONObject.put("phone", colleague.phone);
            jSONObject.put("info", colleague.info);
            jSONArray.put(jSONObject);
        }
        return jSONArray;
    }

    private JSONArray eventsJson() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (UserEvent userEvent : allUserEvents()) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("year", userEvent.year);
            jSONObject.put("month", userEvent.month);
            jSONObject.put("day", userEvent.day);
            jSONObject.put("title", userEvent.title);
            jSONObject.put("holiday", userEvent.holiday);
            jSONArray.put(jSONObject);
        }
        return jSONArray;
    }

    private static ShiftPlan readPlan(Cursor cursor) {
        ShiftPlan shiftPlan = new ShiftPlan();
        shiftPlan.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        shiftPlan.company = cursor.getString(cursor.getColumnIndexOrThrow("company"));
        shiftPlan.team = cursor.getString(cursor.getColumnIndexOrThrow("team"));
        shiftPlan.anchorJdn = cursor.getLong(cursor.getColumnIndexOrThrow("anchor_jdn"));
        shiftPlan.pattern = cursor.getString(cursor.getColumnIndexOrThrow("pattern"));
        shiftPlan.active = cursor.getInt(cursor.getColumnIndexOrThrow("active")) != 0;
        return shiftPlan;
    }

    private static CardexRecord readCardex(Cursor cursor) {
        java.util.Map<String,Object> values = new java.util.LinkedHashMap<>();
        for (String column : CardexCodec.COLUMNS) {
            int index = cursor.getColumnIndexOrThrow(column);
            Object value = (column.equals("jdn") || column.endsWith("_enabled") || column.endsWith("_full_shift"))
                    ? Long.valueOf(cursor.getLong(index)) : cursor.getString(index);
            values.put(column, value);
        }
        return CardexCodec.fromSql(values);
    }

    private static Colleague readColleague(Cursor cursor) {
        Colleague colleague = new Colleague();
        colleague.f0id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        colleague.team = cursor.getString(cursor.getColumnIndexOrThrow("team"));
        colleague.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        colleague.role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
        colleague.mobile1 = cursor.getString(cursor.getColumnIndexOrThrow("mobile1"));
        colleague.mobile2 = cursor.getString(cursor.getColumnIndexOrThrow("mobile2"));
        colleague.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        colleague.info = cursor.getString(cursor.getColumnIndexOrThrow("info"));
        return colleague;
    }

    private static UserEvent readUserEvent(Cursor cursor) {
        UserEvent userEvent = new UserEvent();
        userEvent.f2id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        userEvent.year = cursor.getInt(cursor.getColumnIndexOrThrow("year"));
        userEvent.month = cursor.getInt(cursor.getColumnIndexOrThrow("month"));
        userEvent.day = cursor.getInt(cursor.getColumnIndexOrThrow("day"));
        userEvent.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        userEvent.holiday = cursor.getInt(cursor.getColumnIndexOrThrow("holiday")) != 0;
        return userEvent;
    }

    private static ContentValues planValues(ShiftPlan shiftPlan) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("company", shiftPlan.company == null ? "سفارشی" : shiftPlan.company.trim());
        contentValues.put("team", shiftPlan.team == null ? "A" : shiftPlan.team.trim());
        contentValues.put("anchor_jdn", Long.valueOf(shiftPlan.anchorJdn));
        contentValues.put("pattern", normalizedPattern(shiftPlan.pattern));
        contentValues.put("active", Integer.valueOf(shiftPlan.active ? 1 : 0));
        return contentValues;
    }

    private static ContentValues cardexValues(CardexRecord record) {
        return sqlContentValues(CardexCodec.sqlValues(record));
    }

    private static ContentValues sqlContentValues(java.util.Map<String,Object> map) {
        ContentValues values = new ContentValues();
        for (java.util.Map.Entry<String,Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Integer) values.put(entry.getKey(), (Integer)v);
            else if (v instanceof Long) values.put(entry.getKey(), (Long)v);
            else values.put(entry.getKey(), safe(String.valueOf(v)));
        }
        return values;
    }

    private static ContentValues colleagueValues(Colleague colleague) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("team", safe(colleague.team));
        contentValues.put("name", safe(colleague.name));
        contentValues.put("role", safe(colleague.role));
        contentValues.put("mobile1", safe(colleague.mobile1));
        contentValues.put("mobile2", safe(colleague.mobile2));
        contentValues.put("phone", safe(colleague.phone));
        contentValues.put("info", safe(colleague.info));
        return contentValues;
    }

    private static ContentValues userEventValues(UserEvent userEvent) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("year", Integer.valueOf(userEvent.year));
        contentValues.put("month", Integer.valueOf(userEvent.month));
        contentValues.put("day", Integer.valueOf(userEvent.day));
        contentValues.put("title", safe(userEvent.title));
        contentValues.put("holiday", Integer.valueOf(userEvent.holiday ? 1 : 0));
        return contentValues;
    }

    private static long queryLong(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str, null);
        try {
            return cursorRawQuery.moveToFirst() ? cursorRawQuery.getLong(0) : 0L;
        } finally {
            cursorRawQuery.close();
        }
    }

    private static String safe(String str) {
        return str == null ? "" : str.trim();
    }

    public static final class ImportResult {
        public int cardex;
        public int colleagues;
        public int events;
        public int hijriOffset;
        public int plans;

        public String summary() {
            return "الگوها: " + this.plans + "، کارکرد: " + this.cardex + "، همکاران: " + this.colleagues + "، مناسبت\u200cها: " + this.events;
        }
    }
}
