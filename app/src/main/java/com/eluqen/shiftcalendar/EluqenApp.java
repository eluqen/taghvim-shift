package com.eluqen.shiftcalendar;

import android.app.Application;
import com.eluqen.shiftcalendar.data.StorageIdentity;
import com.eluqen.shiftcalendar.ui.AppTheme;

public final class EluqenApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StorageIdentity.migrateLegacyStorage(this);
        AppTheme.apply(this);
    }
}
