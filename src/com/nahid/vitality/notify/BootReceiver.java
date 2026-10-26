package com.nahid.vitality.notify;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.track.TrackingService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Notifier.ensureChannel(context);
            AlarmScheduler.schedule(context);
            WidgetRefresher.updateAll(context);
            if (AppPrefs.trackingEnabled(context)
                    && context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    TrackingService.startFromBoot(context);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
