package com.nahid.vitality.notify;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.nahid.vitality.MainActivity;
import com.nahid.vitality.R;
import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.Themes;

import java.util.ArrayList;
import java.util.List;

public final class Notifier {

    private static final String CHANNEL_ID = "vitality_alerts";
    private static final int NOTIFICATION_ID = 7001;

    private Notifier() {
    }

    public static void ensureChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_desc));
        channel.enableVibration(true);
        channel.setLightColor(0xFF8B7CFF);
        manager.createNotificationChannel(channel);
    }

    public static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.areNotificationsEnabled();
    }

    public static void check(Context context, float percent) {
        if (percent <= 0f || percent >= 100f) {
            return;
        }
        if (!AppPrefs.notificationsEnabled(context) || !canNotify(context)) {
            return;
        }
        String day = PowerTime.powerDayKey();
        List<Integer> bars = new ArrayList<>();
        int best = 101;
        for (int bar = AppPrefs.BAR_WILL; bar <= AppPrefs.BAR_ENERGY; bar++) {
            if (!AppPrefs.barEnabled(context, bar)) {
                continue;
            }
            for (int threshold : AppPrefs.THRESHOLDS) {
                if (!AppPrefs.thresholdEnabled(context, threshold)) {
                    continue;
                }
                if (percent > threshold || AppPrefs.wasNotified(context, bar, threshold, day)) {
                    continue;
                }
                AppPrefs.markNotified(context, bar, threshold, day);
                if (!bars.contains(bar)) {
                    bars.add(bar);
                }
                best = Math.min(best, threshold);
            }
        }
        if (bars.isEmpty()) {
            return;
        }
        post(context, bars, best);
    }

    private static void post(Context context, List<Integer> bars, int threshold) {
        int current = PowerTime.roundedPercent(PowerTime.nowPercent());
        List<String> names = new ArrayList<>();
        for (int bar : bars) {
            names.add(barName(context, bar));
        }
        String title;
        String body;
        if (bars.size() == 3) {
            title = context.getString(R.string.notif_title_multi, current);
            body = context.getString(R.string.notif_body_all, threshold);
        } else if (bars.size() == 1) {
            title = context.getString(R.string.notif_title_bar, names.get(0), current);
            body = context.getString(R.string.notif_body_one, names.get(0), threshold);
        } else {
            title = context.getString(R.string.notif_title_multi, current);
            body = context.getString(R.string.notif_body_multi, join(names), threshold);
        }

        Theme theme = Themes.get(AppPrefs.themeIndex(context));
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_vitality)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(theme.will)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private static String join(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                output.append(i == names.size() - 1 ? " and " : ", ");
            }
            output.append(names.get(i));
        }
        return output.toString();
    }

    private static String barName(Context context, int bar) {
        switch (bar) {
            case AppPrefs.BAR_WILL:
                return context.getString(R.string.willpower);
            case AppPrefs.BAR_WEALTH:
                return context.getString(R.string.wealth_power);
            default:
                return context.getString(R.string.energy_power);
        }
    }
}
