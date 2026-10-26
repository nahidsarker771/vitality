package com.nahid.vitality.track;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.nahid.vitality.R;
import com.nahid.vitality.TrackActivity;
import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.Themes;
import com.nahid.vitality.data.HistoryDb;

import java.text.NumberFormat;
import java.util.Locale;

public class TrackingService extends Service implements SensorEventListener, LocationListener {

    public static final String ACTION_START = "com.nahid.vitality.track.START";
    public static final String ACTION_STOP = "com.nahid.vitality.track.STOP";
    public static final String ACTION_REFRESH = "com.nahid.vitality.track.REFRESH";
    public static final String EXTRA_LOCATION = "allow_location";

    private static final String CHANNEL_ID = "vitality_tracking";
    private static final int NOTIFICATION_ID = 7100;
    private static final float STEP_METERS = 0.762f;
    private static final long TICK_MS = 30_000L;

    private static boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            Tracker.tick(TrackingService.this);
            flush();
            updateNotification();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private SensorManager sensors;
    private Sensor stepSensor;
    private Sensor motionSensor;
    private boolean stepRegistered;
    private boolean motionRegistered;
    private float lastAccel = -1f;

    private int pendingSteps;
    private long lastFlushAt;
    private LocationManager locations;
    private Location lastLocation;
    private boolean locationStarted;
    private boolean allowLocation;

    public static boolean isRunning() {
        return running;
    }

    public static void ensureRunning(Context context) {
        if (!AppPrefs.trackingEnabled(context)) {
            return;
        }
        if (context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        start(context);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TrackingService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_LOCATION, true);
        startCompat(context, intent);
    }

    public static void startFromBoot(Context context) {
        Intent intent = new Intent(context, TrackingService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_LOCATION, false);
        startCompat(context, intent);
    }

    public static void refresh(Context context) {
        Intent intent = new Intent(context, TrackingService.class)
                .setAction(ACTION_REFRESH)
                .putExtra(EXTRA_LOCATION, true);
        startCompat(context, intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, TrackingService.class));
    }

    private static void startCompat(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createChannel();
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensors != null) {
            stepSensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            motionSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            registerSensors();
        }
        if (AppPrefs.lastMotionAt(this) <= 0L) {
            AppPrefs.setLastMotionAt(this, System.currentTimeMillis());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppPrefs.trackingEnabled(this)
                || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        allowLocation = intent != null && intent.getBooleanExtra(EXTRA_LOCATION, false);
        startForegroundCompat();
        Tracker.tick(this);
        if (allowLocation && AppPrefs.gpsEnabled(this) && hasLocationPermission()) {
            startLocation();
        } else {
            stopLocation();
        }

        handler.removeCallbacks(ticker);
        handler.post(ticker);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        flush();
        Tracker.tick(this);
        if (sensors != null) {
            sensors.unregisterListener(this);
            stepRegistered = false;
            motionRegistered = false;
        }
        stopLocation();
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerSensors() {
        if (sensors == null) {
            return;
        }
        if (!stepRegistered && stepSensor != null) {
            try {
                stepRegistered = sensors.registerListener(this, stepSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            } catch (Exception ignored) {
                stepRegistered = false;
            }
        }
        if (!motionRegistered && motionSensor != null) {
            try {
                motionRegistered = sensors.registerListener(this, motionSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            } catch (Exception ignored) {
                motionRegistered = false;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            handleStepCounter(event.values[0]);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            if (lastAccel >= 0f) {
                Tracker.onMotion(this, Math.abs(magnitude - lastAccel));
            }
            lastAccel = magnitude;
        }
    }

    private void handleStepCounter(float total) {
        String day = PowerTime.powerDayKey();
        float last = AppPrefs.stepSensorLast(this);
        if (!day.equals(AppPrefs.stepDay(this))) {
            AppPrefs.setStepDay(this, day);
            AppPrefs.setStepSensorLast(this, total);
            return;
        }
        if (last < 0f) {
            AppPrefs.setStepSensorLast(this, total);
            return;
        }
        int delta;
        if (total >= last) {
            delta = Math.round(total - last);
        } else {
            delta = Math.round(total);
        }
        if (delta <= 0) {
            return;
        }
        AppPrefs.setStepSensorLast(this, total);
        pendingSteps += delta;
        Tracker.onStep(this, delta);
        if (System.currentTimeMillis() - lastFlushAt > 10_000L) {
            flush();
        }
    }

    private void flush() {
        if (pendingSteps > 0) {
            HistoryDb.get(this).addSteps(PowerTime.powerDayKey(),
                    pendingSteps, pendingSteps * STEP_METERS);
            pendingSteps = 0;
        }
        lastFlushAt = System.currentTimeMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startForegroundCompat() {
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH;
                if (allowLocation && AppPrefs.gpsEnabled(this) && hasLocationPermission()) {
                    type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                }
                startForeground(NOTIFICATION_ID, notification, type);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception error) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (Exception second) {
                stopSelf();
            }
        }
    }

    private void updateNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        HistoryDb.Activity activity = HistoryDb.get(this)
                .activityForDay(PowerTime.powerDayKey());
        int steps = (activity == null ? 0 : activity.steps) + pendingSteps;
        double meters = (activity == null ? 0.0 : activity.displayDistanceM())
                + pendingSteps * STEP_METERS;
        int energy = PowerTime.roundedPercent(EnergyModel.current(this));

        String text = getString(R.string.track_notif_text, formatSteps(steps),
                getString(R.string.km_value, meters / 1000.0), energy);
        if (stepSensor == null) {
            text = getString(R.string.track_notif_no_sensor, energy);
        }

        Intent open = new Intent(this, TrackActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Theme theme = Themes.get(AppPrefs.themeIndex(this));

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_vitality)
                .setContentTitle(getString(R.string.track_notif_title))
                .setContentText(text)
                .setColor(theme.will)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setContentIntent(pending)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    private void createChannel() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.track_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.track_channel_desc));
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    private void startLocation() {
        if (locationStarted || !hasLocationPermission()) {
            return;
        }
        locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locations == null) {
            return;
        }
        try {
            if (!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return;
            }
            locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    60_000L, 50f, this, Looper.getMainLooper());
            locationStarted = true;
        } catch (Exception ignored) {
            locationStarted = false;
        }
    }

    private void stopLocation() {
        if (locations != null && locationStarted) {
            try {
                locations.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
        locationStarted = false;
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            return;
        }
        if (location.hasAccuracy() && location.getAccuracy() > 100f) {
            return;
        }
        if (lastLocation != null) {
            long gap = location.getTime() - lastLocation.getTime();
            if (gap > 0L && gap < 10 * 60 * 1000L) {
                float[] result = new float[1];
                Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(),
                        location.getLatitude(), location.getLongitude(), result);
                if (result[0] >= 5f && result[0] < 2000f) {
                    HistoryDb.get(this).addGpsMeters(PowerTime.powerDayKey(), result[0]);
                }
            }
        }
        lastLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        stopLocation();
    }

    private static String formatSteps(int steps) {
        return NumberFormat.getIntegerInstance(Locale.US).format(steps);
    }
}
