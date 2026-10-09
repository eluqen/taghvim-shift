package com.eluqen.shiftcalendar.update;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;

final class SecureUpdateTransport {
    private static final int MAX_REDIRECTS = 3;

    static final class HttpStatusException extends IOException {
        final int statusCode;
        final int retryAfterSeconds;

        HttpStatusException(int statusCode, int retryAfterSeconds) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    static final class DownloadInfo {
        final long size;
        final String sha256;
        final String finalUrl;

        DownloadInfo(long size, String sha256, String finalUrl) {
            this.size = size;
            this.sha256 = sha256;
            this.finalUrl = finalUrl;
        }
    }

    private SecureUpdateTransport() {}

    static JSONObject getVerifiedManifest(String manifestUrl, String signatureUrl) throws Exception {
        byte[] manifest = getHttps(manifestUrl, UpdateConfig.MAX_MANIFEST_BYTES,
                "application/json", UpdateConfig.META_READ_TIMEOUT_MS);
        byte[] envelope = getHttps(signatureUrl, UpdateConfig.MAX_SIGNATURE_BYTES,
                "application/json", UpdateConfig.META_READ_TIMEOUT_MS);
        return verifyRawManifest(manifest, envelope);
    }

    static JSONObject verifyRawManifest(byte[] manifestBytes, byte[] signatureEnvelopeBytes) throws Exception {
        return verifyRawManifestWithKey(manifestBytes, signatureEnvelopeBytes,
                pinnedPublicKey(UpdateConfig.MANIFEST_KEY_ID));
    }

    static JSONObject verifyRawManifestWithKey(byte[] manifestBytes, byte[] signatureEnvelopeBytes,
                                              PublicKey publicKey) throws Exception {
        if (manifestBytes == null || manifestBytes.length == 0
                || manifestBytes.length > UpdateConfig.MAX_MANIFEST_BYTES) {
            throw new SecurityException("اندازه Manifest نامعتبر است");
        }
        if (signatureEnvelopeBytes == null || signatureEnvelopeBytes.length == 0
                || signatureEnvelopeBytes.length > UpdateConfig.MAX_SIGNATURE_BYTES) {
            throw new SecurityException("اندازه امضای Manifest نامعتبر است");
        }

        JSONObject envelope = new JSONObject(
                new String(signatureEnvelopeBytes, StandardCharsets.UTF_8));
        if (UpdateValidation.integer(envelope, "schemaVersion") != UpdateConfig.MANIFEST_SCHEMA_VERSION) {
            throw new SecurityException("نسخه ساختار امضا پشتیبانی نمی‌شود");
        }
        if (!UpdateConfig.MANIFEST_SIGNATURE_ALGORITHM.equals(envelope.optString("algorithm"))) {
            throw new SecurityException("الگوریتم امضا نامعتبر است");
        }
        if (!UpdateConfig.MANIFEST_SIGNATURE_ENCODING.equals(envelope.optString("encoding"))) {
            throw new SecurityException("Encoding امضا نامعتبر است");
        }
        String keyId = envelope.optString("keyId", "");
        if (!UpdateConfig.MANIFEST_KEY_ID.equals(keyId)) {
            throw new SecurityException("کلید امضا شناخته‌شده نیست");
        }
        String encodedSignature = envelope.optString("signature", "");
        if (encodedSignature.isEmpty() || !encodedSignature.matches("[A-Za-z0-9+/]+={0,2}")) {
            throw new SecurityException("امضای Manifest وجود ندارد");
        }

        byte[] signatureBytes;
        try {
            signatureBytes = Base64.decode(encodedSignature, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Base64 امضا نامعتبر است", e);
        }
        if (!verifySignature(publicKey, manifestBytes, signatureBytes)) {
            throw new SecurityException("امضای Manifest معتبر نیست");
        }

        return new JSONObject(new String(manifestBytes, StandardCharsets.UTF_8));
    }

    static boolean verifySignature(PublicKey publicKey, byte[] rawBytes,
                                   byte[] signatureBytes) throws Exception {
        Signature verifier = Signature.getInstance(UpdateConfig.MANIFEST_SIGNATURE_ALGORITHM);
        verifier.initVerify(publicKey);
        verifier.update(rawBytes);
        return verifier.verify(signatureBytes);
    }

    private static PublicKey pinnedPublicKey(String keyId) throws Exception {
        if (!UpdateConfig.MANIFEST_KEY_ID.equals(keyId)) {
            throw new SecurityException("کلید امضا Pin نشده است");
        }
        byte[] der = Base64.decode(UpdateConfig.MANIFEST_PUBLIC_KEY_BASE64, Base64.DEFAULT);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    static byte[] getHttps(String url, int maxBytes, String accept, int readTimeoutMs)
            throws IOException {
        requireHttpsHost(url, UpdateConfig.UPDATE_HOST);
        String expectedHost = UpdateConfig.UPDATE_HOST;
        HttpURLConnection connection = open(url, accept, readTimeoutMs, 0, expectedHost);
        try {
            long contentLength = contentLength(connection);
            if (contentLength > maxBytes) {
                throw new IOException("حجم پاسخ بیشتر از حد مجاز است");
            }
            try (InputStream input = connection.getInputStream()) {
                return readLimited(input, maxBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    static DownloadInfo downloadToFile(String url, File destination, long maxBytes,
                                       String accept, int readTimeoutMs) throws Exception {
        String expectedHost = new URL(url).getHost();
        HttpURLConnection connection = open(url, accept, readTimeoutMs, 0, expectedHost);
        File parent = destination.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            connection.disconnect();
            throw new IOException("پوشه فایل موقت ساخته نشد");
        }
        if (destination.exists() && !destination.delete()) {
            connection.disconnect();
            throw new IOException("فایل موقت قبلی پاک نشد");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        try {
            long contentLength = contentLength(connection);
            if (contentLength > maxBytes) {
                throw new IOException("حجم فایل بیشتر از حد مجاز است");
            }
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(destination, false)) {
                byte[] buffer = new byte[32 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read == 0) continue;
                    total += read;
                    if (total > maxBytes) {
                        throw new IOException("حجم واقعی فایل بیشتر از حد مجاز است");
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
                output.flush();
                output.getFD().sync();
            }
            return new DownloadInfo(total, hex(digest.digest()),
                    connection.getURL().toExternalForm());
        } catch (Throwable t) {
            destination.delete();
            throw t;
        } finally {
            connection.disconnect();
        }
    }

    static byte[] gunzipLimited(File source, int maxDecompressedBytes) throws IOException {
        try (InputStream fileInput = new java.io.FileInputStream(source);
             GZIPInputStream gzip = new GZIPInputStream(fileInput)) {
            return readLimited(gzip, maxDecompressedBytes);
        }
    }

    static String sha256(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    static void requireHttpsHost(String value, String requiredHost) throws IOException {
        URL url = new URL(value);
        if (!"https".equalsIgnoreCase(url.getProtocol())
                || !requiredHost.equalsIgnoreCase(url.getHost())
                || url.getUserInfo() != null || (url.getPort() != -1 && url.getPort() != 443)) {
            throw new IOException("نشانی سرور به‌روزرسانی معتبر نیست");
        }
    }

    private static HttpURLConnection open(String url, String accept, int readTimeoutMs,
                                          int redirects, String expectedHost) throws IOException {
        if (redirects > MAX_REDIRECTS) {
            throw new IOException("تعداد تغییر مسیر بیش از حد مجاز است");
        }
        URL parsed = new URL(url);
        if (!"https".equalsIgnoreCase(parsed.getProtocol())) {
            throw new IOException("فقط HTTPS مجاز است");
        }
        if (expectedHost == null || !expectedHost.equalsIgnoreCase(parsed.getHost())) {
            throw new IOException("تغییر دامنه در مسیر به‌روزرسانی مجاز نیست");
        }

        requireHttpsHost(url, expectedHost);
        HttpURLConnection connection = (HttpURLConnection) parsed.openConnection();
        connection.setConnectTimeout(UpdateConfig.CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(readTimeoutMs);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("User-Agent", "ELUQEN-TaghvimShift/1.0.0 (Android)");

        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.trim().isEmpty()) {
                throw new IOException("تغییر مسیر بدون مقصد معتبر دریافت شد");
            }
            URL redirected = new URL(parsed, location);
            if (!"https".equalsIgnoreCase(redirected.getProtocol())) {
                throw new IOException("تغییر مسیر ناامن رد شد");
            }
            return open(redirected.toExternalForm(), accept, readTimeoutMs, redirects + 1, expectedHost);
        }
        if (status < 200 || status >= 300) {
            int retryAfter = parseRetryAfter(connection.getHeaderField("Retry-After"));
            connection.disconnect();
            throw new HttpStatusException(status, retryAfter);
        }
        return connection;
    }

    private static long contentLength(HttpURLConnection connection) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24) return connection.getContentLengthLong();
            return connection.getContentLength();
        } catch (Throwable ignored) {
            return connection.getContentLength();
        }
    }

    private static int parseRetryAfter(String value) {
        if (value == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (Exception ignored) {
            return 0;
        }
    }

    static byte[] readLimited(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                Math.min(maxBytes, 32 * 1024));
        byte[] buffer = new byte[16 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) continue;
            total += read;
            if (total > maxBytes) {
                throw new IOException("حجم واقعی داده بیشتر از حد مجاز است");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return out.toString();
    }
}
