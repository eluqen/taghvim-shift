package com.eluqen.shiftcalendar.events;

import android.annotation.SuppressLint;
import android.content.Context;
import com.eluqen.shiftcalendar.update.UpdateValidation;
import com.eluqen.shiftcalendar.core.Event;
import com.eluqen.shiftcalendar.core.GregorianDate;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.hijri.HijriResolver;
import com.eluqen.shiftcalendar.reminder.ReminderScheduler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class EventRepository {
    private static final String ASSET_NAME = "event_bank.json";
    private static final String DIRECTORY_NAME = "content";
    private static final String FILE_NAME = "event_bank.json";
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    // Safe: this singleton is constructed exclusively with context.getApplicationContext().
    @SuppressLint("StaticFieldLeak")
    private static EventRepository instance;
    private final Context context;
    private EventPack pack;

    public static synchronized EventRepository get(Context context) {
        if (instance == null) {
            instance = new EventRepository(context.getApplicationContext());
        }
        return instance;
    }

    private EventRepository(Context context) {
        this.context = context;
        reload();
    }

    public synchronized void reload() {
        File fileInstalledFile = installedFile(this.context);
        EventPack eventPack = null;
        if (fileInstalledFile.isFile()) {
            try {
                eventPack = parse(readFile(fileInstalledFile), false);
            } catch (Exception unused) {
            }
        }
        if (eventPack == null) {
            try {
                eventPack = parse(readAsset(this.context), true);
            } catch (Exception e) {
                throw new IllegalStateException("بانک داخلی مناسبت\u200cها قابل خواندن نیست", e);
            }
        }
        this.pack = eventPack;
    }

    public synchronized PackInfo info() {
        return this.pack.info();
    }

    public synchronized HijriDate resolveServerHijri(PersianDate date) {
        return this.pack.hijriPolicy.resolve(date);
    }

    public synchronized boolean hasAuthoritativeHijri(PersianDate date) {
        return this.pack.hijriPolicy.isAuthoritativeFor(date);
    }

    public synchronized int hijriCorrectionVersion() {
        return this.pack.hijriPolicy.correctionVersion;
    }

    public synchronized boolean hasHijriPolicy() {
        return this.pack.hijriPolicy.present;
    }

    public synchronized List<Event> forDate(PersianDate persianDate) {
        return this.pack.eventsForDate(
                persianDate,
                Prefs.hijriManualOffset(this.context));
    }

    public synchronized boolean isHoliday(PersianDate persianDate) {
        if (persianDate.dayOfWeek() == 6) {
            return true;
        }
        Iterator<Event> it = forDate(persianDate).iterator();
        while (it.hasNext()) {
            if (it.next().holiday) {
                return true;
            }
        }
        return false;
    }

    public static EventPack validate(byte[] bArr) throws JSONException {
        return parse(bArr, false);
    }

    public static File installedFile(Context context) {
        return new File(new File(context.getFilesDir(), DIRECTORY_NAME), "event_bank.json");
    }

    private static void addUnique(List<Event> list, Set<String> set, String str, boolean z, String str2) {
        if (set.add(str + "\u0000" + z)) {
            list.add(new Event(str, z, str2));
        }
    }

    public static EventPack validateCanonical(byte[] bytes, int expectedDataVersion,
                                              int expectedCoverageFrom,
                                              int expectedCoverageTo,
                                              String expectedPublishedAt)
            throws JSONException {
        JSONObject canonicalRoot = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!canonicalRoot.has("schemaVersion") || canonicalRoot.optJSONArray("exactEvents") == null)
            throw new JSONException("Canonical schema required");
        EventPack pack = parse(bytes, false);
        if (pack.dataVersion != expectedDataVersion
                || pack.coverageFrom != expectedCoverageFrom
                || pack.coverageTo != expectedCoverageTo
                || !pack.publishedAt.equals(expectedPublishedAt)) {
            throw new JSONException("Metadata بانک مناسبت‌ها با Manifest مطابقت ندارد");
        }
        return pack;
    }

    public static EventPack validateFile(File file) throws Exception {
        return parse(readFile(file), false);
    }

    private static EventPack parse(byte[] bytes, boolean builtIn) throws JSONException {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new JSONException("اندازه بانک مناسبت‌ها نامعتبر است");
        }
        JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        boolean canonical = root.has("schemaVersion");

        int dataVersion;
        int coverageFrom;
        int coverageTo;
        String publishedAt;
        String displayVersion;
        JSONArray exact;

        if (canonical) {
            if (UpdateValidation.integer(root, "schemaVersion") != 1) {
                throw new JSONException("نسخه ساختار بانک مناسبت‌ها پشتیبانی نمی‌شود");
            }
            dataVersion = UpdateValidation.integer(root, "dataVersion");
            coverageFrom = UpdateValidation.integer(root, "coverageFrom");
            coverageTo = UpdateValidation.integer(root, "coverageTo");
            publishedAt = root.optString("publishedAt", "").trim();
            displayVersion = String.valueOf(dataVersion);
            exact = root.optJSONArray("exactEvents");
            UpdateValidation.publishedAt(publishedAt);
            if (exact == null) throw new JSONException("exactEvents required");
        } else {
            if (root.optInt("schema", -1) != 1) {
                throw new JSONException("نسخه ساختار بانک مناسبت‌ها پشتیبانی نمی‌شود");
            }
            dataVersion = root.optInt("revision", -1);
            coverageFrom = root.optInt("coverage_from", -1);
            coverageTo = root.optInt("coverage_to", -1);
            publishedAt = root.optString("published_at", "").trim();
            displayVersion = root.optString("version", "").trim();
            exact = root.optJSONArray("exact_events");
        }

        if (dataVersion < 1 || displayVersion.isEmpty()
                || coverageFrom < 1200 || coverageTo < coverageFrom
                || coverageTo > 2000) {
            throw new JSONException("مشخصات پوشش بانک مناسبت‌ها نامعتبر است");
        }

        HijriPolicy hijriPolicy = canonical
                ? parseHijriPolicy(root.optJSONObject("hijri"), coverageFrom, coverageTo)
                : HijriPolicy.empty();

        JSONArray events = root.optJSONArray("events");
        if (events == null || events.length() > 5000) {
            throw new JSONException("فهرست مناسبت‌ها نامعتبر است");
        }

        ArrayList<Rule> rules = new ArrayList<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject item = events.getJSONObject(i);
            String calendar = item.optString("calendar", "");
            if (!"solar".equals(calendar)
                    && !"hijri".equals(calendar)
                    && !"gregorian".equals(calendar)) {
                throw new JSONException("نوع تقویم نامعتبر است");
            }
            int month = validMonth(item), day = validDay(item);
            int maximum = "hijri".equals(calendar) ? 30
                    : "solar".equals(calendar) ? (month <= 6 ? 31 : 30)
                    : (month == 2 ? 29 : (month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31));
            if (day > maximum) throw new JSONException("روز نامعتبر برای نوع تقویم");
            if (canonical && (!(item.opt("holiday") instanceof Boolean)
                    || !(item.opt("category") instanceof String))) throw new JSONException("نوع رکورد نامعتبر است");
            rules.add(new Rule(calendar, month, day,
                    validTitle(item), item.optBoolean("holiday"),
                    item.optString("category", "")));
        }

        ArrayList<ExactRule> exactRules = new ArrayList<>();
        if (exact != null) {
            if (exact.length() > 5000) {
                throw new JSONException("فهرست اصلاحات مناسبتی بیش از حد بزرگ است");
            }
            for (int i = 0; i < exact.length(); i++) {
                JSONObject item = exact.getJSONObject(i);
                int year;
                int month;
                int day;
                if (canonical && item.has("year")) {
                    year = UpdateValidation.integer(item, "year");
                    month = UpdateValidation.integer(item, "month");
                    day = UpdateValidation.integer(item, "day");
                    if (year < 1200 || year > 2000 || month < 1 || month > 12
                            || day < 1 || day > 31) {
                        throw new JSONException("تاریخ مناسبت دقیق نامعتبر است");
                    }
                } else {
                    PersianDate date = PersianDate.parse(item.getString(ReminderScheduler.EXTRA_DATE));
                    year = date.year;
                    month = date.month;
                    day = date.day;
                }
                if (year < coverageFrom || year > coverageTo || day > JalaliCalendar.daysInMonth(year, month))
                    throw new JSONException("تاریخ دقیق خارج از پوشش یا نامعتبر است");
                if (canonical && (!(item.opt("holiday") instanceof Boolean) || !(item.opt("category") instanceof String)))
                    throw new JSONException("نوع رکورد دقیق نامعتبر است");
                exactRules.add(new ExactRule(year, month, day,
                        validTitle(item), item.optBoolean("holiday"),
                        item.optString("category", "")));
            }
        }

        return new EventPack(dataVersion, displayVersion,
                coverageFrom, coverageTo, publishedAt, builtIn, rules, exactRules,
                hijriPolicy);
    }

    private static HijriPolicy parseHijriPolicy(JSONObject object,
                                                int coverageFrom,
                                                int coverageTo)
            throws JSONException {
        if (object == null) {
            return HijriPolicy.empty();
        }
        if (!"tabular".equals(object.optString("algorithm", ""))) {
            throw new JSONException("الگوریتم تاریخ قمری پشتیبانی نمی‌شود");
        }

        int correctionVersion = UpdateValidation.integer(object, "correctionVersion");
        if (correctionVersion < 1) {
            throw new JSONException("نسخه اصلاح قمری نامعتبر است");
        }

        JSONArray monthStarts = object.optJSONArray("monthStarts");
        if (monthStarts == null || monthStarts.length() > 256) {
            throw new JSONException("فهرست شروع ماه‌های قمری نامعتبر است");
        }

        ArrayList<HijriMonthStart> values = new ArrayList<>();
        long previousPersianJdn = Long.MIN_VALUE;
        int previousHijriIndex = Integer.MIN_VALUE;

        for (int index = 0; index < monthStarts.length(); index++) {
            JSONObject item = monthStarts.getJSONObject(index);
            int hijriYear = UpdateValidation.integer(item, "hijriYear");
            int hijriMonth = UpdateValidation.integer(item, "hijriMonth");
            if (hijriYear < 1 || hijriMonth < 1 || hijriMonth > 12) {
                throw new JSONException("ماه قمری مرجع نامعتبر است");
            }
            if (!(item.opt("persianDate") instanceof String)) {
                throw new JSONException("تاریخ شمسی شروع ماه قمری باید متن باشد");
            }

            PersianDate persianDate;
            try {
                String raw = item.getString("persianDate");
                if (!raw.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    throw new IllegalArgumentException();
                }
                persianDate = PersianDate.parse(raw);
            } catch (Exception e) {
                throw new JSONException("تاریخ شمسی شروع ماه قمری نامعتبر است");
            }
            if (persianDate.year < coverageFrom || persianDate.year > coverageTo) {
                throw new JSONException("شروع ماه قمری خارج از پوشش بانک داده است");
            }

            long persianJdn = persianDate.toJdn();
            long tabularJdn = HijriDate.toJdn(hijriYear, hijriMonth, 1);
            if (Math.abs(persianJdn - tabularJdn) > 2L) {
                throw new JSONException(
                        "شروع ماه رسمی بیش از ۲ روز با پایه محاسباتی اختلاف دارد");
            }
            int hijriIndex = hijriYear * 12 + (hijriMonth - 1);
            if (persianJdn <= previousPersianJdn || hijriIndex <= previousHijriIndex) {
                throw new JSONException("شروع ماه‌های قمری باید صعودی و بدون تکرار باشند");
            }

            if (!values.isEmpty() && hijriIndex == previousHijriIndex + 1) {
                long days = persianJdn - previousPersianJdn;
                if (days != 29L && days != 30L) {
                    throw new JSONException(
                            "فاصله دو ماه قمری متوالی باید ۲۹ یا ۳۰ روز باشد");
                }
            }

            values.add(new HijriMonthStart(
                    hijriYear, hijriMonth, persianDate, persianJdn));
            previousPersianJdn = persianJdn;
            previousHijriIndex = hijriIndex;
        }

        return new HijriPolicy(true, correctionVersion, values);
    }

    private static int validMonth(JSONObject jSONObject) throws JSONException {
        int i = UpdateValidation.integer(jSONObject, "month");
        if (i < 1 || i > 12) {
            throw new JSONException("ماه مناسبت نامعتبر است");
        }
        return i;
    }

    private static int validDay(JSONObject jSONObject) throws JSONException {
        int i = UpdateValidation.integer(jSONObject, "day");
        if (i < 1 || i > 31) {
            throw new JSONException("روز مناسبت نامعتبر است");
        }
        return i;
    }

    private static String validTitle(JSONObject jSONObject) throws JSONException {
        if (!(jSONObject.opt("title") instanceof String)) throw new JSONException("عنوان باید متن باشد");
        String strTrim = jSONObject.optString("title", "").trim();
        if (strTrim.isEmpty() || strTrim.length() > 240) {
            throw new JSONException("عنوان مناسبت نامعتبر است");
        }
        return strTrim;
    }

    private static byte[] readAsset(Context context) throws IOException {
        InputStream inputStreamOpen = context.getAssets().open("event_bank.json");
        try {
            return readLimited(inputStreamOpen, MAX_BYTES);
        } finally {
            inputStreamOpen.close();
        }
    }

    private static byte[] readFile(File file) throws IOException {
        if (file.length() > MAX_BYTES) {
            throw new IOException("بانک مناسبت\u200cها بیش از حد بزرگ است");
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            return readLimited(fileInputStream, MAX_BYTES);
        } finally {
            fileInputStream.close();
        }
    }

    private static byte[] readLimited(InputStream inputStream, int i) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        int i2 = 0;
        while (true) {
            int i3 = inputStream.read(bArr);
            if (i3 < 0) {
                return byteArrayOutputStream.toByteArray();
            }
            i2 += i3;
            if (i2 > i) {
                throw new IOException("حجم داده دریافتی بیش از حد مجاز است");
            }
            byteArrayOutputStream.write(bArr, 0, i3);
        }
    }

    public static final class PackInfo {
        public final boolean builtIn;
        public final int coverageFrom;
        public final int coverageTo;
        public final int eventCount;
        public final String publishedAt;
        public final int dataVersion;
        public final int revision;
        public final String version;

        PackInfo(int i, String str, int i2, int i3, String str2, boolean z, int i4) {
            this.dataVersion = i;
            this.revision = i;
            this.version = str;
            this.coverageFrom = i2;
            this.coverageTo = i3;
            this.publishedAt = str2;
            this.builtIn = z;
            this.eventCount = i4;
        }
    }

    public static final class EventPack {
        final boolean builtIn;
        final int coverageFrom;
        final int coverageTo;
        final List<ExactRule> exactRules;
        final String publishedAt;
        final int dataVersion;
        final int revision;
        final List<Rule> rules;
        final String version;
        final HijriPolicy hijriPolicy;

        EventPack(int i, String str, int i2, int i3, String str2, boolean z,
                  List<Rule> list, List<ExactRule> list2, HijriPolicy hijriPolicy) {
            this.dataVersion = i;
            this.revision = i;
            this.version = str;
            this.coverageFrom = i2;
            this.coverageTo = i3;
            this.publishedAt = str2;
            this.builtIn = z;
            this.rules = list;
            this.exactRules = list2;
            this.hijriPolicy = hijriPolicy == null ? HijriPolicy.empty() : hijriPolicy;
        }

        public PackInfo info() {
            return new PackInfo(this.revision, this.version, this.coverageFrom, this.coverageTo, this.publishedAt, this.builtIn, this.exactRules.size() + this.rules.size());
        }

        public HijriDate resolveServerHijri(PersianDate date) {
            return this.hijriPolicy.resolve(date);
        }

        public boolean hasAuthoritativeHijri(PersianDate date) {
            return this.hijriPolicy.isAuthoritativeFor(date);
        }

        public int hijriCorrectionVersion() {
            return this.hijriPolicy.correctionVersion;
        }

        public List<Event> eventsForDate(PersianDate persianDate, int manualOffset) {
            ArrayList<Event> result = new ArrayList<>();
            HashSet<String> seen = new HashSet<>();
            GregorianDate gregorian = JalaliCalendar.toGregorian(persianDate);
            HijriDate hijri = HijriResolver.resolve(
                    persianDate,
                    manualOffset,
                    this::resolveServerHijri);

            for (Rule rule : this.rules) {
                boolean matches;
                if ("solar".equals(rule.calendar)) {
                    matches = rule.month == persianDate.month
                            && rule.day == persianDate.day;
                } else if ("hijri".equals(rule.calendar)) {
                    matches = rule.month == hijri.month
                            && rule.day == hijri.day;
                } else {
                    matches = rule.month == gregorian.month
                            && rule.day == gregorian.day;
                }
                if (matches) {
                    addUnique(
                            result, seen, rule.title, rule.holiday, rule.category);
                }
            }

            for (ExactRule exactRule : this.exactRules) {
                if (exactRule.year == persianDate.year
                        && exactRule.month == persianDate.month
                        && exactRule.day == persianDate.day) {
                    addUnique(
                            result, seen, exactRule.title,
                            exactRule.holiday, exactRule.category);
                }
            }
            return result;
        }
    }

    private static final class HijriPolicy {
        final boolean present;
        final int correctionVersion;
        final List<HijriMonthStart> monthStarts;

        HijriPolicy(boolean present, int correctionVersion,
                    List<HijriMonthStart> monthStarts) {
            this.present = present;
            this.correctionVersion = correctionVersion;
            this.monthStarts = monthStarts;
        }

        static HijriPolicy empty() {
            return new HijriPolicy(
                    false, 0, new ArrayList<HijriMonthStart>());
        }

        HijriDate resolve(PersianDate date) {
            long jdn = date.toJdn();
            HijriMonthStart current = null;
            HijriMonthStart next = null;

            for (int i = 0; i < monthStarts.size(); i++) {
                HijriMonthStart candidate = monthStarts.get(i);
                if (candidate.persianJdn > jdn) {
                    next = candidate;
                    break;
                }
                current = candidate;
            }

            if (current != null) {
                long delta = jdn - current.persianJdn;
                boolean nextConsecutive = next != null
                        && next.hijriIndex == current.hijriIndex + 1;
                if (nextConsecutive) {
                    long monthLength = next.persianJdn - current.persianJdn;
                    if (delta >= 0 && delta < monthLength) {
                        return new HijriDate(
                                current.hijriYear,
                                current.hijriMonth,
                                (int) delta + 1);
                    }
                } else if (delta >= 0 && delta < 29) {
                    // The first 29 days are valid regardless of whether the
                    // official month ultimately has 29 or 30 days.
                    return new HijriDate(
                            current.hijriYear,
                            current.hijriMonth,
                            (int) delta + 1);
                }
            }

            return HijriDate.fromJdn(jdn, 0);
        }

        boolean isAuthoritativeFor(PersianDate date) {
            long jdn = date.toJdn();
            HijriMonthStart current = null;
            HijriMonthStart next = null;

            for (int i = 0; i < monthStarts.size(); i++) {
                HijriMonthStart candidate = monthStarts.get(i);
                if (candidate.persianJdn > jdn) {
                    next = candidate;
                    break;
                }
                current = candidate;
            }
            if (current == null) return false;

            long delta = jdn - current.persianJdn;
            if (next != null && next.hijriIndex == current.hijriIndex + 1) {
                return delta >= 0 && delta < (next.persianJdn - current.persianJdn);
            }
            return delta >= 0 && delta < 29;
        }
    }

    private static final class HijriMonthStart {
        final int hijriYear;
        final int hijriMonth;
        final int hijriIndex;
        final PersianDate persianDate;
        final long persianJdn;

        HijriMonthStart(int hijriYear, int hijriMonth,
                        PersianDate persianDate, long persianJdn) {
            this.hijriYear = hijriYear;
            this.hijriMonth = hijriMonth;
            this.hijriIndex = hijriYear * 12 + (hijriMonth - 1);
            this.persianDate = persianDate;
            this.persianJdn = persianJdn;
        }
    }

    private static final class Rule {
        final String calendar;
        final String category;
        final int day;
        final boolean holiday;
        final int month;
        final String title;

        Rule(String str, int i, int i2, String str2, boolean z, String str3) {
            this.calendar = str;
            this.month = i;
            this.day = i2;
            this.title = str2;
            this.holiday = z;
            this.category = str3;
        }
    }

    private static final class ExactRule {
        final String category;
        final int day;
        final boolean holiday;
        final int month;
        final String title;
        final int year;

        ExactRule(int i, int i2, int i3, String str, boolean z, String str2) {
            this.year = i;
            this.month = i2;
            this.day = i3;
            this.title = str;
            this.holiday = z;
            this.category = str2;
        }
    }
}
