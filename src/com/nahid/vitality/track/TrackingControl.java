package com.nahid.vitality.track;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import com.nahid.vitality.R;
import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;

import java.util.ArrayList;
import java.util.List;

public final class TrackingControl {

    private TrackingControl() {
    }

    public static boolean hasActivityPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static List<String> requiredPermissions(Context context) {
        List<String> needed = new ArrayList<>();
        if (!hasActivityPermission(context)) {
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return needed;
    }

    public static boolean enable(Context context) {
        if (!hasActivityPermission(context)) {
            return false;
        }
        AppPrefs.setTrackingEnabled(context, true);
        AppPrefs.setTrackingAsked(context, true);
        EnergyModel.initialize(context);
        AppPrefs.setLastTrackerTick(context, System.currentTimeMillis());
        Tracker.tick(context);
        TrackingService.start(context);
        return true;
    }

    public static void disable(Context context) {
        AppPrefs.setTrackingEnabled(context, false);
        Tracker.endSleep(context);
        TrackingService.stop(context);
    }

    public static boolean batteryUnrestricted(Context context) {
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean notificationsEnabled(Context context) {
        NotificationManager notifications =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notifications != null && notifications.areNotificationsEnabled();
    }

    public static void openBatterySettings(Activity activity) {
        try {
            PowerManager power =
                    (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (power != null && !power.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                activity.startActivity(new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + activity.getPackageName())));
            } else {
                activity.startActivity(new Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        } catch (Exception error) {
            try {
                activity.startActivity(new Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(activity, R.string.track_battery_na, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void openNotificationSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            activity.startActivity(intent);
        } catch (Exception error) {
            try {
                Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                details.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(details);
            } catch (Exception ignored) {
            }
        }
    }
}
