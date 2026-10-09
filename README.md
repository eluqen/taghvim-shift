# ELUQEN Shift Calendar

Persian calendar and shift schedule for Android. Package: `com.eluqen.shiftcalendar`.

## Building the Google Play edition

Install JDK 17 and Android SDK platform 36. Gradle is pinned by the included wrapper.

```sh
./gradlew :app:assemblePlayDebug :app:testPlayDebugUnitTest
```

To produce a signed release, provide your own keystore through `ELUQEN_KEYSTORE_PATH`, `ELUQEN_KEYSTORE_PASSWORD_FILE` and optionally `ELUQEN_KEY_ALIAS`; never commit credentials or signing files. Google Play publishing requires a separately authorized release.

The app provides calendar dates and events, rotating-shift scheduling, reminders, work records, notes, and widgets.
