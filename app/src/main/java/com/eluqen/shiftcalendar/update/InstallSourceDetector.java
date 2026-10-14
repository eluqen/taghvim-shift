package com.eluqen.shiftcalendar.update;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class InstallSourceDetector {
    public static final String BAZAAR_PACKAGE = "com.farsitel.bazaar";
    public static final String MYKET_PACKAGE = "ir.mservices.market";
    public static final String PLAY_PACKAGE = "com.android.vending";
    public static final String GALAXY_PACKAGE = "com.sec.android.app.samsungapps";

    private InstallSourceDetector() {
    }

    public static Detection detect(Context context) {
        String installerPackageName;
        try {
            PackageManager packageManager = context.getPackageManager();
            if (Build.VERSION.SDK_INT >= 30) {
                installerPackageName = packageManager.getInstallSourceInfo(context.getPackageName()).getInstallingPackageName();
            } else {
                installerPackageName = packageManager.getInstallerPackageName(context.getPackageName());
            }
        } catch (Exception unused) {
            installerPackageName = null;
        }
        if (PLAY_PACKAGE.equals(installerPackageName)) {
            return new Detection(installerPackageName, "گوگل\u200cپلی", UpdateSource.GOOGLE_PLAY, true);
        }
        if (BAZAAR_PACKAGE.equals(installerPackageName)) {
            return new Detection(installerPackageName, "بازار", UpdateSource.BAZAAR, true);
        }
        if (MYKET_PACKAGE.equals(installerPackageName)) {
            return new Detection(installerPackageName, "مایکت", UpdateSource.MYKET, true);
        }
        if (GALAXY_PACKAGE.equals(installerPackageName)) {
            return new Detection(installerPackageName, "Galaxy Store", UpdateSource.GALAXY, true);
        }
        String str = (installerPackageName == null || installerPackageName.trim().isEmpty()) ? "نصب مستقیم یا منبع نامشخص" : "نصب مستقیم یا منبع دیگر";
        if (installerPackageName == null) {
            installerPackageName = "";
        }
        return new Detection(installerPackageName, str, UpdateSource.DIRECT, false);
    }

    public static final class Detection {
        public final boolean knownStore;
        public final String label;
        public final String packageName;
        public final UpdateSource suggestedSource;

        Detection(String str, String str2, UpdateSource updateSource, boolean z) {
            this.packageName = str;
            this.label = str2;
            this.suggestedSource = updateSource;
            this.knownStore = z;
        }
    }
}
