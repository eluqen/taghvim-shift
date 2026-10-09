package com.eluqen.shiftcalendar.update;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONObject;

public final class UpdateValidation {
    private UpdateValidation() {}
    public static int integer(JSONObject object, String key) {
        Object value = object.opt(key);
        if (!(value instanceof Integer) && !(value instanceof Long))
            throw new SecurityException("Integer required: " + key);
        long number = ((Number) value).longValue();
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
            throw new SecurityException("Integer out of range: " + key);
        return (int) number;
    }
    static long size(JSONObject object, String key, long maximum) {
        Object value = object.opt(key);
        if (!(value instanceof Integer) && !(value instanceof Long))
            throw new SecurityException("Integer size required");
        long size = ((Number) value).longValue();
        if (size < 1 || size > maximum) throw new SecurityException("Invalid size");
        return size;
    }
    public static void publishedAt(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?Z"))
            throw new SecurityException("Invalid UTC publication time");
        String pattern = value.contains(".") ? "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" : "yyyy-MM-dd'T'HH:mm:ss'Z'";
        String normalized = value;
        if (value.contains(".")) {
            int start = value.indexOf('.') + 1;
            String fraction = value.substring(start, value.length() - 1);
            normalized = value.substring(0, start) + (fraction + "000").substring(0, 3) + "Z";
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        if (format.parse(normalized, position) == null || position.getIndex() != normalized.length())
            throw new SecurityException("Invalid UTC publication time");
    }
    static void identity(JSONObject manifest, boolean app) {
        if (integer(manifest, "schemaVersion") != UpdateConfig.MANIFEST_SCHEMA_VERSION
                || !UpdateConfig.APP_SLUG.equals(manifest.optString("appSlug")))
            throw new SecurityException("Invalid calendar manifest identity");
        publishedAt(manifest.optString("publishedAt", ""));
    }
    static void downloaded(long size, String hash, long expectedSize, String expectedHash, long maximum) {
        if (expectedSize < 1 || expectedSize > maximum || size != expectedSize
                || !SecureUpdateTransport.isSha256(expectedHash) || !expectedHash.equals(hash))
            throw new SecurityException("Downloaded size/hash mismatch");
    }
}
