# ELUQEN Taghvim Shift

An Android calendar and work-record companion for people on rotating shifts. The current app interface is in Persian.

**Version:** 1.0.3 (build 4)
**Application ID:** `com.eluqen.shiftcalendar`
**Minimum Android version:** 6.0 (API 23)

## Features

- Persian calendar with Gregorian and Hijri date references and calendar events
- Configurable rotating shift patterns and a four-team monthly view
- Daily work records for shift swaps, leave, rest and overtime
- Shift reminders, personal tasks, notes and home-screen widget
- Local backup and restore, and calendar (ICS) export

## Latest release

Version 1.0.3 improves monthly work-statistics readability and colors, and ensures personal-event sorting works on Android 6 and later without changing work-hour calculations.

Download the **Google Play edition APK** from the [GitHub Releases page](https://github.com/eluqen/taghvim-shift/releases/tag/1.0.3). The separate website edition is available from the official ELUQEN site.

## Official links

- [Application overview](https://eluqen.com/apps/shift-calendar/)
- [Website download](https://eluqen.com/fa/apps/shift-calendar/)
- [Privacy policy](https://eluqen.com/apps/taghvim-shift/privacy-policy/)
- [Support](mailto:dev@eluqen.com)

This repository contains the source code for the **Google Play edition** of the Android application. Its APK uses Google Play for application updates. If you prefer to install and update the app directly from the ELUQEN website, use the website download above.

## Build

Prerequisites: JDK 17 and Android SDK Platform 36.

```sh
./gradlew :app:assemblePlayDebug :app:testPlayDebugUnitTest
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

Release signing uses private signing credentials. Never commit keystores, passwords, locally generated build outputs or other secrets.
