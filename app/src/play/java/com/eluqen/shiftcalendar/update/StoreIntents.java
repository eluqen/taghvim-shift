package com.eluqen.shiftcalendar.update;

import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import android.app.AlertDialog;
import android.widget.Toast;

final class StoreIntents {
    private StoreIntents() {}

    static UpdateSource source() { return UpdateSource.GOOGLE_PLAY; }
    static AppUpdateManager.OpenResult openStore(Context c) {
        return AppUpdateManager.open(c, Uri.parse("market://details?id=" + c.getPackageName()),
                "com.android.vending", "https://play.google.com/store/apps/details?id=" + c.getPackageName());
    }
    static boolean supportsReview(Context c) { return true; }
    static boolean supportsDeveloperPage(Context c) { return false; }
    static AppUpdateManager.OpenResult openReview(Context c) { return openStore(c); }
    static AppUpdateManager.OpenResult openDeveloperPage(Context c) {
        return new AppUpdateManager.OpenResult(false, "صفحه توسعه‌دهنده در دسترس نیست");
    }

}
