package com.eluqen.shiftcalendar.update;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import com.eluqen.shiftcalendar.R;
import com.eluqen.shiftcalendar.data.Prefs;
import java.io.File;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public final class AppUpdateManager {
    private static final long AUTO_CHECK_INTERVAL = 86_400_000L;
    public static final String BAZAAR_DEVELOPER_ID = "eluqen";

    public interface Callback {
        void onFinished(Result result);
    }

    public interface DownloadCallback {
        void onFinished(DownloadResult result);
    }

    public enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        NOT_PUBLISHED,
        ERROR
    }

    private AppUpdateManager() {}

    // Immutable channel identity: no source selection based on installer or other stores.
    public static UpdateSource selectedSource(Context context) {
        UpdateSource source = StoreIntents.source();
        Prefs.setUpdateSource(context, source);
        return source;
    }

    public static List<UpdateSource> selectableSources(Context context) {
        return java.util.Collections.singletonList(StoreIntents.source());
    }

    private static boolean isMarketSource(UpdateSource source) {
        return source == UpdateSource.BAZAAR || source == UpdateSource.MYKET
                || source == UpdateSource.GALAXY;
    }

    public static boolean isDirectPolicyRestricted(Context context) {
        return "direct".equals(context.getString(R.string.build_channel));
    }

    // Legacy shared-MARKET channel was retired 2026-10-14.
    public static boolean isMarketPolicyRestricted(Context context) {
        return false;
    }

    public static boolean isGalaxyPolicyRestricted(Context context) {
        return "galaxy".equals(context.getString(R.string.build_channel));
    }

    public static boolean isMyketPolicyRestricted(Context context) {
        return "myket".equals(context.getString(R.string.build_channel));
    }

    public static boolean isBazaarPolicyRestricted(Context context) {
        return "bazaar".equals(context.getString(R.string.build_channel));
    }

    public static boolean isPlayPolicyRestricted(Context context) {
        return "play".equals(context.getString(R.string.build_channel));
    }

    public static OpenResult openStore(Context context, UpdateSource source) {
        if (source != StoreIntents.source()) {
            return new OpenResult(false, "به‌روزرسانی فقط از منبع همین نسخه انجام می‌شود");
        }
        return StoreIntents.openStore(context);
    }

    public static boolean isBazaarAvailable(Context context) {
        return context.getPackageManager()
                .getLaunchIntentForPackage(InstallSourceDetector.BAZAAR_PACKAGE) != null;
    }

    public static OpenResult openBazaarReview(Context context) {
        if (StoreIntents.source() != UpdateSource.BAZAAR) {
            return new OpenResult(false, "این قابلیت در نسخه نصب‌شده فعال نیست");
        }
        return StoreIntents.openReview(context);
    }

    public static OpenResult openBazaarDeveloper(Context context) {
        if (StoreIntents.source() != UpdateSource.BAZAAR) {
            return new OpenResult(false, "این قابلیت در نسخه نصب‌شده فعال نیست");
        }
        return StoreIntents.openDeveloperPage(context);
    }

    public static boolean supportsReview(Context context) {
        return StoreIntents.supportsReview(context);
    }

    public static boolean isGalaxyReviewEligible(Context context) {
        return StoreIntents.source() == UpdateSource.GALAXY
                && StoreIntents.supportsReview(context);
    }

    public static boolean supportsDeveloperPage(Context context) {
        return StoreIntents.supportsDeveloperPage(context);
    }

    public static String activeStoreLabel(Context context) {
        return StoreIntents.source().label;
    }

    public static OpenResult openReview(Context context) {
        return StoreIntents.openReview(context);
    }

    public static OpenResult openDeveloperPage(Context context) {
        return StoreIntents.openDeveloperPage(context);
    }

    static OpenResult open(Context context, Uri uri, String packageName,
                                   String fallback) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (packageName != null) intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return new OpenResult(true, "منبع به‌روزرسانی باز شد");
        } catch (ActivityNotFoundException first) {
            if (fallback == null || fallback.trim().isEmpty()) {
                return new OpenResult(false, "برنامه فروشگاه مناسب روی دستگاه پیدا نشد");
            }
            try {
                Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(fallback));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(web);
                return new OpenResult(true, "صفحه وب منبع به‌روزرسانی باز شد");
            } catch (Exception second) {
                return new OpenResult(false, "برنامه یا مرورگر مناسب برای این منبع پیدا نشد");
            }
        } catch (Exception e) {
            return new OpenResult(false, "باز کردن منبع به‌روزرسانی ممکن نشد");
        }
    }

    public static void maybeCheckDirectInBackground(Context context) {
        if (!Prefs.autoAppUpdate(context) || selectedSource(context) != UpdateSource.DIRECT) {
            return;
        }
        long lastCheck = Prefs.lastAppCheck(context);
        if (lastCheck > 0 && System.currentTimeMillis() - lastCheck < AUTO_CHECK_INTERVAL) {
            return;
        }
        checkDirect(context, result -> {
            if (result.status == Status.UPDATE_AVAILABLE) {
                AppUpdateNotifications.show(context, result);
            }
        });
    }

    public static void checkDirect(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            Result result = performDirectCheck(app);
            if (callback != null) callback.onFinished(result);
        }, "eluqen-shift-calendar-app-update").start();
    }

    private static Result performDirectCheck(Context context) {
        if (!isDirectPolicyRestricted(context)) {
            return error("این نسخه فقط از فروشگاه مربوط به خود به‌روزرسانی می‌شود.");
        }
        Prefs.setLastAppCheck(context, System.currentTimeMillis());
        try {
            JSONObject manifest = SecureUpdateTransport.getVerifiedManifest(
                    UpdateConfig.APP_MANIFEST_URL,
                    UpdateConfig.APP_MANIFEST_SIGNATURE_URL);

            UpdateValidation.identity(manifest, true);

            int remoteCode = UpdateValidation.integer(manifest, "versionCode");
            String remoteName = manifest.optString("versionName", "").trim();
            String publishedAt = manifest.optString("publishedAt", "").trim();
            if (remoteCode < 1 || remoteName.isEmpty() || publishedAt.isEmpty()) {
                throw new SecurityException("مشخصات نسخه برنامه ناقص است");
            }

            JSONObject apk = manifest.getJSONObject("apk");
            String downloadUrl = apk.optString("url", "").trim();
            String expectedHash = apk.optString("sha256", "").trim();
            long expectedSize = UpdateValidation.size(apk, "size", UpdateConfig.MAX_APK_BYTES);
            String expectedSigner = apk.optString("signingCertificateSha256", "").trim();

            SecureUpdateTransport.requireHttpsHost(
                    downloadUrl, UpdateConfig.DOWNLOAD_HOST);
            if (!SecureUpdateTransport.isSha256(expectedHash)
                    || expectedSize < 1 || expectedSize > UpdateConfig.MAX_APK_BYTES
                    || !UpdateConfig.APP_SIGNING_CERT_SHA256.equals(expectedSigner)) {
                throw new SecurityException("مشخصات امنیتی APK معتبر نیست");
            }

            JSONObject changelog = manifest.optJSONObject("changelog");
            String changes = "";
            if (changelog != null) {
                changes = changelog.optString("fa", "").trim();
                if (changes.isEmpty()) changes = changelog.optString("en", "").trim();
                if (changes.isEmpty()) changes = changelog.optString("fr", "").trim();
            }

            int installedCode = currentVersionCode(context);
            if (!isRemoteNewer(remoteCode, installedCode)) {
                Prefs.clearPendingAppUpdate(context);
                return new Result(Status.UP_TO_DATE,
                        "نسخه نصب‌شده به‌روز است.", remoteCode, remoteName,
                        changes, downloadUrl, expectedHash, expectedSize,
                        expectedSigner, publishedAt);
            }

            Prefs.setPendingAppUpdate(
                    context, remoteCode, remoteName, changes, downloadUrl);
            return new Result(Status.UPDATE_AVAILABLE,
                    "نسخه جدید آماده دریافت است.", remoteCode, remoteName,
                    changes, downloadUrl, expectedHash, expectedSize,
                    expectedSigner, publishedAt);
        } catch (SecureUpdateTransport.HttpStatusException e) {
            if (e.statusCode == 404) {
                Prefs.clearPendingAppUpdate(context);
                return new Result(Status.NOT_PUBLISHED,
                        "نسخه جدیدی روی سرور منتشر نشده است.",
                        0, "", "", "", "", 0L, "", "");
            }
            if (e.statusCode == 429) {
                String suffix = e.retryAfterSeconds > 0
                        ? " لطفاً حدود " + e.retryAfterSeconds + " ثانیه بعد دوباره تلاش کنید."
                        : "";
                return error("تعداد درخواست‌ها موقتاً محدود شده است." + suffix);
            }
            return error("سرور به‌روزرسانی پاسخ معتبر نداد (HTTP "
                    + e.statusCode + ").");
        } catch (SecurityException e) {
            return error("اعتبارسنجی امنیتی اطلاعات به‌روزرسانی ناموفق بود.");
        } catch (Exception e) {
            return error("بررسی نسخه انجام نشد. اتصال اینترنت و دسترسی به سرور را بررسی کنید.");
        }
    }

    public static void downloadVerifiedApk(Context context, Result update,
                                           DownloadCallback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            DownloadResult result = performDownload(app, update);
            if (callback != null) callback.onFinished(result);
        }, "eluqen-shift-calendar-apk-download").start();
    }

    private static synchronized DownloadResult performDownload(Context context, Result update) {
        if (!isDirectPolicyRestricted(context)) {
            return DownloadResult.error("دریافت مستقیم بسته نصب در این نسخه مجاز نیست.");
        }
        if (update == null || update.status != Status.UPDATE_AVAILABLE) {
            return DownloadResult.error("اطلاعات نسخه جدید معتبر نیست");
        }
        File dir = new File(context.getCacheDir(), "updates");
        File apkFile = new File(dir, "ELUQEN-Taghvim-Shift-update.apk");
        File marker = new File(dir, "verified.json");
        marker.delete();
        try {
            if (!isRemoteNewer(update.versionCode, currentVersionCode(context))) throw new SecurityException("نسخه جدید نیست");
            SecureUpdateTransport.requireHttpsHost(
                    update.downloadUrl, UpdateConfig.DOWNLOAD_HOST);
            SecureUpdateTransport.DownloadInfo info =
                    SecureUpdateTransport.downloadToFile(
                            update.downloadUrl, apkFile,
                            UpdateConfig.MAX_APK_BYTES,
                            "application/vnd.android.package-archive, application/octet-stream",
                            UpdateConfig.PAYLOAD_READ_TIMEOUT_MS);

            UpdateValidation.downloaded(info.size, info.sha256, update.size, update.sha256, UpdateConfig.MAX_APK_BYTES);
            verifyArchive(context, apkFile, update);
            JSONObject proof = new JSONObject().put("versionCode", update.versionCode)
                    .put("size", update.size).put("sha256", update.sha256)
                    .put("signer", update.signingCertificateSha256);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(marker)) {
                out.write(proof.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            return DownloadResult.success(apkFile);
        } catch (Exception e) {
            apkFile.delete();
            marker.delete();
            return DownloadResult.error(
                    e instanceof SecurityException
                            ? "اعتبارسنجی APK دانلودشده ناموفق بود."
                            : "دانلود APK کامل نشد. اتصال اینترنت را بررسی کنید.");
        }
    }

    private static void verifyArchive(Context context, File file, Result update)
            throws Exception {
        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        if (info == null) {
            throw new SecurityException("APK قابل خواندن نیست");
        }
        long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        UpdateValidation.archive(info.packageName, code, archiveSignerSha256(info),
                update.versionCode, update.signingCertificateSha256);
    }

    public static synchronized void validateDownloadedApk(Context context, File file) throws Exception {
        if (!isDirectPolicyRestricted(context)) {
            throw new SecurityException("Direct installer disabled");
        }
        File directory = new File(context.getCacheDir(), "updates");
        if (!file.getCanonicalFile().equals(new File(directory, "ELUQEN-Taghvim-Shift-update.apk").getCanonicalFile()))
            throw new SecurityException("Unexpected APK path");
        try {
            byte[] proofBytes;
            try (java.io.FileInputStream in = new java.io.FileInputStream(new File(directory, "verified.json"))) {
                proofBytes = SecureUpdateTransport.readLimited(in, 4096);
            }
            JSONObject proof = new JSONObject(new String(proofBytes, java.nio.charset.StandardCharsets.UTF_8));
            int code = UpdateValidation.integer(proof, "versionCode");
            if (!isRemoteNewer(code, currentVersionCode(context))) throw new SecurityException("Stale APK");
            long expectedSize = UpdateValidation.size(proof, "size", UpdateConfig.MAX_APK_BYTES);
            String expectedHash = proof.getString("sha256");
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            if (file.length() != expectedSize) throw new SecurityException("APK size changed");
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[32768]; int read;
                while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) hash.append(String.format(Locale.US, "%02x", b & 255));
            UpdateValidation.downloaded(file.length(), hash.toString(), expectedSize, expectedHash, UpdateConfig.MAX_APK_BYTES);
            Result result = new Result(Status.UPDATE_AVAILABLE, "", code, "", "", "", expectedHash,
                    expectedSize, proof.getString("signer"), "");
            verifyArchive(context, file, result);
        } catch (Exception e) {
            file.delete(); new File(directory, "verified.json").delete(); throw e;
        }
    }

    private static String archiveSignerSha256(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28) {
            if (info.signingInfo == null) throw new SecurityException("SigningInfo وجود ندارد");
            signatures = info.signingInfo.getApkContentsSigners();
        } else {
            signatures = info.signatures;
        }
        if (signatures == null || signatures.length != 1) {
            throw new SecurityException("تعداد امضاکنندگان APK نامعتبر است");
        }
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
        StringBuilder out = new StringBuilder(64);
        for (byte b : hash) out.append(String.format(Locale.US, "%02x", b & 0xff));
        return out.toString();
    }

    static boolean isRemoteNewer(int remoteVersionCode, int installedVersionCode) {
        return remoteVersionCode > installedVersionCode;
    }

    public static int currentVersionCode(Context context) throws Exception {
        PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28
                ? (int) Math.min(Integer.MAX_VALUE, info.getLongVersionCode())
                : info.versionCode;
    }

    private static Result error(String message) {
        return new Result(Status.ERROR, message, 0, "", "", "",
                "", 0L, "", "");
    }

    public static final class Result {
        public final String changelog;
        public final String downloadUrl;
        public final String message;
        public final Status status;
        public final int versionCode;
        public final String versionName;
        public final String sha256;
        public final long size;
        public final String signingCertificateSha256;
        public final String publishedAt;

        Result(Status status, String message, int versionCode, String versionName,
               String changelog, String downloadUrl, String sha256, long size,
               String signingCertificateSha256, String publishedAt) {
            this.status = status;
            this.message = message;
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
            this.size = size;
            this.signingCertificateSha256 = signingCertificateSha256;
            this.publishedAt = publishedAt;
        }
    }

    public static final class DownloadResult {
        public final boolean success;
        public final String message;
        public final File file;

        private DownloadResult(boolean success, String message, File file) {
            this.success = success;
            this.message = message;
            this.file = file;
        }

        static DownloadResult success(File file) {
            return new DownloadResult(true, "APK با موفقیت دریافت و اعتبارسنجی شد.", file);
        }

        static DownloadResult error(String message) {
            return new DownloadResult(false, message, null);
        }
    }

    public static final class OpenResult {
        public final String message;
        public final boolean opened;

        OpenResult(boolean opened, String message) {
            this.opened = opened;
            this.message = message;
        }
    }
}
