package com.eluqen.shiftcalendar.update;

import android.content.Context;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;

public final class DataUpdateManager {
    public interface Callback {
        void onFinished(Result result);
    }

    public enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        INSTALLED,
        NOT_PUBLISHED,
        ERROR
    }

    private DataUpdateManager() {}

    public static void check(Context context, boolean record, Callback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            Result result = perform(app, record, true);
            if (callback != null) callback.onFinished(result);
        }, "eluqen-shift-calendar-data-update").start();
    }

    public static void preview(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            Result result = perform(app, true, false);
            if (callback != null) callback.onFinished(result);
        }, "eluqen-shift-calendar-data-preview").start();
    }

    static synchronized Result perform(Context context, boolean record, boolean installPayload) {
        try {
            JSONObject manifest = SecureUpdateTransport.getVerifiedManifest(
                    UpdateConfig.CALENDAR_MANIFEST_URL,
                    UpdateConfig.CALENDAR_MANIFEST_SIGNATURE_URL);

            UpdateValidation.identity(manifest, false);

            int dataVersion = UpdateValidation.integer(manifest, "dataVersion");
            int coverageFrom = UpdateValidation.integer(manifest, "coverageFrom");
            int coverageTo = UpdateValidation.integer(manifest, "coverageTo");
            String publishedAt = manifest.optString("publishedAt", "").trim();
            if (dataVersion < 1 || coverageFrom < 1200 || coverageTo < coverageFrom
                    || coverageTo > 2000 || publishedAt.isEmpty()) {
                throw new SecurityException("مشخصات بانک داده نامعتبر است");
            }

            JSONObject payload = manifest.getJSONObject("payload");
            String payloadUrl = payload.optString("url", "").trim();
            String compression = payload.optString("compression", "").trim();
            String expectedHash = payload.optString("sha256", "").trim();
            long expectedSize = UpdateValidation.size(payload, "size", UpdateConfig.MAX_CALENDAR_GZIP_BYTES);

            SecureUpdateTransport.requireHttpsHost(
                    payloadUrl, UpdateConfig.UPDATE_HOST);
            if (!new java.net.URL(payloadUrl).getPath().endsWith("/events-" + dataVersion + ".json.gz")) throw new SecurityException("نام بسته بانک نامعتبر است");
            if (!"gzip".equals(compression)
                    || !SecureUpdateTransport.isSha256(expectedHash)
                    || expectedSize < 1
                    || expectedSize > UpdateConfig.MAX_CALENDAR_GZIP_BYTES) {
                throw new SecurityException("مشخصات امنیتی بسته بانک داده معتبر نیست");
            }

            EventRepository.PackInfo installed = EventRepository.get(context).info();
            if (!isRemoteNewer(dataVersion, installed.dataVersion)) {
                Result result = Result.upToDate(
                        "بانک مناسبت‌ها به‌روز است.", dataVersion,
                        coverageFrom, coverageTo, expectedSize, publishedAt);
                if (record) Prefs.recordDataCheck(context, result.message, false);
                return result;
            }

            if (!installPayload) {
                Result result = Result.available(
                        "نسخه جدید بانک مناسبت‌ها آماده دریافت است.", dataVersion,
                        coverageFrom, coverageTo, expectedSize, publishedAt,
                        payloadUrl, expectedHash);
                if (record) Prefs.recordDataCheck(context, result.message, false);
                return result;
            }

            File dir = new File(context.getCacheDir(), "calendar-update");
            File compressed = new File(dir, "events-" + dataVersion + ".json.gz");
            SecureUpdateTransport.DownloadInfo info =
                    SecureUpdateTransport.downloadToFile(
                            payloadUrl, compressed,
                            UpdateConfig.MAX_CALENDAR_GZIP_BYTES,
                            "application/gzip, application/octet-stream",
                            UpdateConfig.PAYLOAD_READ_TIMEOUT_MS);

            byte[] json;
            try {
                UpdateValidation.downloaded(info.size, info.sha256, expectedSize, expectedHash, UpdateConfig.MAX_CALENDAR_GZIP_BYTES);
                json = SecureUpdateTransport.gunzipLimited(compressed, UpdateConfig.MAX_CALENDAR_JSON_BYTES);
            } finally { compressed.delete(); }
            try {
                AtomicCalendarStore.replace(EventRepository.installedFile(context), json,
                        bytes -> EventRepository.validateCanonical(bytes, dataVersion, coverageFrom, coverageTo, publishedAt),
                        () -> {
                            EventRepository.get(context).reload();
                            EventRepository.PackInfo after = EventRepository.get(context).info();
                            if (after.dataVersion != dataVersion || after.coverageFrom != coverageFrom
                                    || after.coverageTo != coverageTo) throw new SecurityException("فعال‌سازی بانک داده تأیید نشد");
                        });
            } catch (Exception e) {
                EventRepository.get(context).reload();
                throw e;
            }

            Result result = Result.installed(
                    "بانک مناسبت‌ها با موفقیت به‌روزرسانی شد.",
                    dataVersion, coverageFrom, coverageTo, expectedSize, publishedAt);
            if (record) Prefs.recordDataCheck(context, result.message, true);
            return result;
        } catch (SecureUpdateTransport.HttpStatusException e) {
            if (e.statusCode == 404) {
                Result result = Result.notPublished(
                        "بسته جدیدی برای بانک مناسبت‌ها روی سرور منتشر نشده است.");
                if (record) Prefs.recordDataCheck(context, result.message, false);
                return result;
            }
            boolean retryable = e.statusCode == 429 || e.statusCode >= 500;
            Result result = Result.error(
                    e.statusCode == 429
                            ? "تعداد درخواست‌ها موقتاً محدود شده است."
                            : "سرور بانک مناسبت‌ها پاسخ معتبر نداد (HTTP "
                            + e.statusCode + ").",
                    retryable);
            if (record) Prefs.recordDataCheck(context, result.message, false);
            return result;
        } catch (SecurityException e) {
            Result result = Result.error(
                    "اعتبارسنجی امنیتی بانک مناسبت‌ها ناموفق بود.", false);
            if (record) Prefs.recordDataCheck(context, result.message, false);
            return result;
        } catch (Exception e) {
            Result result = Result.error(
                    "بررسی یا دریافت بانک مناسبت‌ها انجام نشد. اتصال اینترنت را بررسی کنید.",
                    true);
            if (record) Prefs.recordDataCheck(context, result.message, false);
            return result;
        }
    }

    static boolean isRemoteNewer(int remoteDataVersion, int installedDataVersion) {
        return remoteDataVersion > installedDataVersion;
    }

    public static final class Result {
        public final String changelog;
        public final int coverageFrom;
        public final int coverageTo;
        public final int dataVersion;
        public final String message;
        public final long size;
        public final Status status;
        public final String version;
        public final String publishedAt;
        public final String payloadUrl;
        public final String sha256;
        public final boolean retryable;

        Result(Status status, String message, int dataVersion,
               int coverageFrom, int coverageTo, long size, String publishedAt,
               String payloadUrl, String sha256, boolean retryable) {
            this.status = status;
            this.message = message;
            this.dataVersion = dataVersion;
            this.version = dataVersion > 0 ? String.valueOf(dataVersion) : "";
            this.coverageFrom = coverageFrom;
            this.coverageTo = coverageTo;
            this.size = size;
            this.publishedAt = publishedAt == null ? "" : publishedAt;
            this.payloadUrl = payloadUrl == null ? "" : payloadUrl;
            this.sha256 = sha256 == null ? "" : sha256;
            this.changelog = "";
            this.retryable = retryable;
        }

        static Result upToDate(String message, int dataVersion, int coverageFrom,
                               int coverageTo, long size, String publishedAt) {
            return new Result(Status.UP_TO_DATE, message, dataVersion,
                    coverageFrom, coverageTo, size, publishedAt, "", "", false);
        }

        static Result available(String message, int dataVersion, int coverageFrom,
                                int coverageTo, long size, String publishedAt,
                                String payloadUrl, String sha256) {
            return new Result(Status.UPDATE_AVAILABLE, message, dataVersion,
                    coverageFrom, coverageTo, size, publishedAt,
                    payloadUrl, sha256, false);
        }

        static Result installed(String message, int dataVersion, int coverageFrom,
                                int coverageTo, long size, String publishedAt) {
            return new Result(Status.INSTALLED, message, dataVersion,
                    coverageFrom, coverageTo, size, publishedAt, "", "", false);
        }

        static Result notPublished(String message) {
            return new Result(Status.NOT_PUBLISHED, message, 0, 0, 0,
                    0L, "", "", "", false);
        }

        static Result error(String message, boolean retryable) {
            return new Result(Status.ERROR, message, 0, 0, 0,
                    0L, "", "", "", retryable);
        }
    }
}
