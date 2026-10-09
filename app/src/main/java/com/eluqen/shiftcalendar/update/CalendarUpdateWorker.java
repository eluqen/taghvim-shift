package com.eluqen.shiftcalendar.update;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.eluqen.shiftcalendar.data.Prefs;

public final class CalendarUpdateWorker extends Worker {
    public CalendarUpdateWorker(@NonNull Context appContext,
                                @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!Prefs.autoDataUpdate(context)) {
            return Result.success();
        }

        boolean installPayload = !Prefs.NETWORK_ASK.equals(Prefs.updateNetwork(context));
        DataUpdateManager.Result result =
                DataUpdateManager.perform(context, true, installPayload);

        if (result.status == DataUpdateManager.Status.ERROR && result.retryable) {
            return Result.retry();
        }
        return Result.success();
    }
}
