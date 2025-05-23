package com.example.sneaker_shop;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class OrderStatusWorkerScheduler {
    public static void scheduleOrderStatusCheck(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(OrderCheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "order_status_check",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                );
    }
}
