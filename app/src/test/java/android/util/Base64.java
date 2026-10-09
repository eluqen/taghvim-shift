package android.util;
/** JVM adapter for the platform Base64 codec; crypto/validation remain production code. */
public final class Base64 {
    public static final int DEFAULT = 0;
    public static byte[] decode(String text, int flags) {
        return java.util.Base64.getDecoder().decode(text.replaceAll("\\s", ""));
    }
}
