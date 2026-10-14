package com.eluqen.shiftcalendar.update;

public final class UpdateConfig {
    public static final int MANIFEST_SCHEMA_VERSION = 1;
    public static final String APP_SLUG = "shift-calendar";
    public static final String PACKAGE_NAME = "com.eluqen.shiftcalendar";
    public static final String RELEASE_CHANNEL = "stable";

    public static final String APP_MANIFEST_URL =
            "https://update.eluqen.com/v1/apps/shift-calendar/app/manifest.json";
    public static final String APP_MANIFEST_SIGNATURE_URL =
            "https://update.eluqen.com/v1/apps/shift-calendar/app/manifest.sig";
    public static final String CALENDAR_MANIFEST_URL =
            "https://update.eluqen.com/v1/apps/shift-calendar/calendar/manifest.json";
    public static final String CALENDAR_MANIFEST_SIGNATURE_URL =
            "https://update.eluqen.com/v1/apps/shift-calendar/calendar/manifest.sig";
    public static final String PUBLIC_KEY_URL =
            "https://update.eluqen.com/v1/keys/manifest-rsa-3072-2026-10-r2.pem";

    public static final String UPDATE_HOST = "update.eluqen.com";
    public static final String DOWNLOAD_HOST = "downloads.eluqen.com";

    public static final String MANIFEST_KEY_ID = "bf10bf6c3aacd97f";
    public static final String MANIFEST_SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String MANIFEST_SIGNATURE_ENCODING = "base64";
    public static final String MANIFEST_PUBLIC_KEY_BASE64 =
            "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA30J4aaiRh+TcOxhytglFucK9FVpajoXuL8dOhRVPpCNKmrHIYYZBr7rScKc5Gnihst8zPiomR1gY+nQa7GzK328XDu+0ujt+qkHAWOGXAvuVLwHM8YHugXzOm48Dt893iJC0MJnYqalWAOWRxuKi6VADnpMCdEAxiquZPQtqluIeb+RV5lg2Eq329SbsiCG8umN7Pi/eI96f68PfKphKRiBJnuIT/y5+hiLkzHxVPBwDf6UmafXE1Rt8uFbisGaoM1UDoVt/XyQEJThcvOW8qLMODSHWIxwVHfA2c5hhh8NwUflO8+D4mbZAXtu1MZkJKYLxHdT4VMDOlDdhZFpY7yxUF17+OtTzREEj1xgLIANg3ntEL51sbFEiNPFb9fthPuwxK6zIaJs0xAF22c9HihEWq/npB55huobescjeOJd8zvaxOpvtQEQoxamINMxGnfhKVBv5VDxCHsRQTfB52tXGY9TTn6WHIhUdq4fah6KO0U/LNnZYdek53k86FuxJAgMBAAE=";

    public static final String APP_SIGNING_CERT_SHA256 =
            "5e24b4794a5c4b2fcc3a1f688706c779c64ec09b52a06d7b717cd7908c644104";

    public static final int MAX_MANIFEST_BYTES = 64 * 1024;
    public static final int MAX_SIGNATURE_BYTES = 4 * 1024;
    public static final long MAX_APK_BYTES = 256L * 1024L * 1024L;
    public static final long MAX_CALENDAR_GZIP_BYTES = 16L * 1024L * 1024L;
    public static final int MAX_CALENDAR_JSON_BYTES = 64 * 1024 * 1024;

    public static final int CONNECT_TIMEOUT_MS = 15_000;
    public static final int META_READ_TIMEOUT_MS = 30_000;
    public static final int PAYLOAD_READ_TIMEOUT_MS = 120_000;

    private UpdateConfig() {}
}
