package com.eluqen.shiftcalendar.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import java.io.File;

public final class AppUpdateInstaller {
    public enum Result {
        INSTALLER_OPENED,
        PERMISSION_REQUIRED
    }

    private AppUpdateInstaller() {}

    public static Result install(Activity activity, File apk) throws Exception {
        AppUpdateManager.validateDownloadedApk(activity, apk);
        if (Build.VERSION.SDK_INT >= 26
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent permissionIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(permissionIntent);
            return Result.PERMISSION_REQUIRED;
        }

        Uri uri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                apk);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
        return Result.INSTALLER_OPENED;
    }
}
