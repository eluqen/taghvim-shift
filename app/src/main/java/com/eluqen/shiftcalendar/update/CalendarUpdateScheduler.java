package com.eluqen.shiftcalendar.update;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.data.StorageIdentity;
import java.util.concurrent.TimeUnit;

public final class CalendarUpdateScheduler {
    private static final String UNIQUE_WORK = StorageIdentity.CALENDAR_UPDATE_WORK;

    private CalendarUpdateScheduler() {}

    public static void syncSchedule(Context context) {
        Context app = context.getApplicationContext();
        WorkManager manager = WorkManager.getInstance(app);
        manager.cancelUniqueWork(StorageIdentity.LEGACY_CALENDAR_UPDATE_WORK);
        if (!Prefs.autoDataUpdate(app)) {
            manager.cancelUniqueWork(UNIQUE_WORK);
            return;
        }

        NetworkType networkType = Prefs.NETWORK_WIFI.equals(Prefs.updateNetwork(app))
                ? NetworkType.UNMETERED : NetworkType.CONNECTED;
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(CalendarUpdateWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build();

        manager.enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }
}
