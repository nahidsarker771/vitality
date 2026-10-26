package com.nahid.vitality;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.track.Tracker;
import com.nahid.vitality.track.TrackingService;
import com.nahid.vitality.ui.VitalityBarView;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackActivity extends BaseActivity {

    private static final int REQ_TRACK = 21;
    private static final int REQ_GPS = 22;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateUi();
            handler.postDelayed(this, 2000);
        }
    };

    private TextView txtTrackStatus;
    private TextView txtSteps;
    private TextView txtDistance;
    private TextView txtActiveMin;
    private TextView txtEnergy;
    private TextView txtEnergyEffect;
    private TextView txtSleep;
    private TextView txtRest;
    private TextView txtMotion;
    private TextView txtSync;
    private TextView txtPermission;
    private TextView txtServiceStatus;
    private TextView txtBatteryStatus;
    private TextView txtNotificationStatus;
    private LinearLayout cardPermission;
    private LinearLayout listRecent;
    private TextView txtRecentEmpty;
    private Switch swTracking;
    private Switch swGps;
    private VitalityBarView energyBar;
    private boolean binding;
    private long lastRecentStamp = Long.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        txtTrackStatus = findViewById(R.id.txtTrackStatus);
        txtSteps = findViewById(R.id.txtSteps);
        txtDistance = findViewById(R.id.txtDistance);
        txtActiveMin = findViewById(R.id.txtActiveMin);
        txtEnergy = findViewById(R.id.txtEnergy);
        txtEnergyEffect = findViewById(R.id.txtEnergyEffect);
        txtSleep = findViewById(R.id.txtSleep);
        txtRest = findViewById(R.id.txtRest);
        txtMotion = findViewById(R.id.txtMotion);
        txtSync = findViewById(R.id.txtSync);
        txtPermission = findViewById(R.id.txtPermission);
        txtServiceStatus = findViewById(R.id.txtServiceStatus);
        txtBatteryStatus = findViewById(R.id.txtBatteryStatus);
        txtNotificationStatus = findViewById(R.id.txtNotificationStatus);
        cardPermission = findViewById(R.id.cardPermission);
        listRecent = findViewById(R.id.listRecent);
        txtRecentEmpty = findViewById(R.id.txtRecentEmpty);
        swTracking = findViewById(R.id.swTracking);
        swGps = findViewById(R.id.swGps);
        energyBar = findViewById(R.id.trackEnergyBar);

        applyChrome(1);
        energyBar.setColors(theme.energy, ThemeManager.lighten(theme.energy, 0.35f));

        wireTracking();
        wireGps();
        wirePermission();
        wireKeepAlive();
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (AppPrefs.trackingEnabled(this) && hasActivityPermission()) {
            TrackingService.ensureRunning(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void wireTracking() {
        swTracking.setOnCheckedChangeListener((button, checked) -> {
            if (binding) {
                return;
            }
            if (checked) {
                requestTrackingPermissions();
            } else {
                AppPrefs.setTrackingEnabled(this, false);
                Tracker.endSleep(this);
                TrackingService.stop(this);
                updateUi();
            }
        });
    }

    private void wireGps() {
        swGps.setOnCheckedChangeListener((button, checked) -> {
            if (binding) {
                return;
            }
            AppPrefs.setGpsEnabled(this, checked);
            if (checked && !hasLocationPermission()) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_GPS);
            } else if (AppPrefs.trackingEnabled(this)) {
                TrackingService.refresh(this);
            }
            updateUi();
        });
    }

    private void wirePermission() {
        findViewById(R.id.btnPermission).setOnClickListener(view -> requestTrackingPermissions());
    }

    private void wireKeepAlive() {
        findViewById(R.id.btnBattery).setOnClickListener(view -> openBatterySettings());
        findViewById(R.id.btnNotifications).setOnClickListener(view -> openNotificationSettings());
    }

    private void requestTrackingPermissions() {
        List<String> needed = new ArrayList<>();
        if (!hasActivityPermission()) {
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (AppPrefs.gpsEnabled(this) && !hasLocationPermission()) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (needed.isEmpty()) {
            enableTracking();
        } else {
            requestPermissions(needed.toArray(new String[0]), REQ_TRACK);
        }
    }

    private void enableTracking() {
        if (!hasActivityPermission()) {
            Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
            updateUi();
            return;
        }
        AppPrefs.setTrackingEnabled(this, true);
        AppPrefs.setTrackingAsked(this, true);
        EnergyModel.initialize(this);
        AppPrefs.setLastTrackerTick(this, System.currentTimeMillis());
        Tracker.tick(this);
        TrackingService.start(this);
        updateUi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_TRACK) {
            if (!hasActivityPermission()) {
                Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
                updateUi();
                return;
            }
            if (!hasLocationPermission() && AppPrefs.gpsEnabled(this)) {
                AppPrefs.setGpsEnabled(this, false);
                Toast.makeText(this, R.string.track_location_off, Toast.LENGTH_SHORT).show();
            }
            enableTracking();
        } else if (requestCode == REQ_GPS) {
            if (!hasLocationPermission()) {
                AppPrefs.setGpsEnabled(this, false);
                Toast.makeText(this, R.string.track_location_off, Toast.LENGTH_SHORT).show();
            } else if (AppPrefs.trackingEnabled(this)) {
                TrackingService.refresh(this);
            }
            updateUi();
        }
    }

    private void updateUi() {
        if (txtSteps == null) {
            return;
        }
        HistoryDb db = HistoryDb.get(this);
        HistoryDb.Activity activity = db.activityForDay(PowerTime.powerDayKey());
        int steps = activity == null ? 0 : activity.steps;
        double meters = activity == null ? 0.0 : activity.displayDistanceM();
        int active = activity == null ? 0 : activity.activeMinutes();
        long sleepSeconds = Tracker.currentOrLastSleepSeconds(this);

        txtSteps.setText(formatSteps(steps));
        txtDistance.setText(getString(R.string.km_value, meters / 1000.0));
        txtActiveMin.setText(getString(R.string.track_active_value, active));
        txtSleep.setText(sleepSeconds <= 0L ? getString(R.string.dash)
                : PowerTime.duration(sleepSeconds / 60L));
        txtRest.setText(activity == null || activity.restSeconds <= 0L
                ? getString(R.string.dash)
                : PowerTime.duration(activity.restSeconds / 60L));

        boolean tracking = AppPrefs.trackingEnabled(this);
        boolean sleeping = AppPrefs.sleeping(this);
        if (!tracking) {
            txtMotion.setText(R.string.dash);
        } else if (sleeping) {
            txtMotion.setText(R.string.track_motion_sleeping);
        } else {
            String mode = EnergyModel.modeLabelKey(this);
            if ("active".equals(mode)) {
                txtMotion.setText(R.string.track_motion_active);
            } else if ("light".equals(mode)) {
                txtMotion.setText(R.string.track_motion_light);
            } else {
                txtMotion.setText(R.string.track_motion_rest);
            }
        }
        if (activity == null || activity.updatedAt <= 0L) {
            txtSync.setText(R.string.dash);
        } else {
            LocalDateTime updated = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(activity.updatedAt), ZoneId.systemDefault());
            txtSync.setText(TIME.format(updated));
        }

        float energy = EnergyModel.current(this);
        txtEnergy.setText(PowerTime.roundedPercent(energy) + "%");
        energyBar.setValue(energy);
        if (tracking) {
            txtEnergyEffect.setText(getString(R.string.track_energy_effect,
                    Math.round(EnergyModel.drainToday(this)),
                    Math.round(EnergyModel.restToday(this)),
                    Math.round(EnergyModel.sleepToday(this))));
        } else {
            txtEnergyEffect.setText(R.string.track_energy_off);
        }

        txtTrackStatus.setText(tracking ? R.string.track_tracking_on
                : R.string.track_tracking_off);
        txtTrackStatus.setTag(tracking ? "chipAccent" : "chip");
        txtTrackStatus.setBackground(ThemeManager.rounded(this,
                ThemeManager.withAlpha(tracking ? theme.will : theme.surfaceAlt, tracking ? 0.14f : 1f),
                tracking ? ThemeManager.withAlpha(theme.will, 0.5f) : theme.border, 99f));
        txtTrackStatus.setTextColor(tracking ? theme.will : theme.muted);

        if (tracking && TrackingService.isRunning()) {
            txtServiceStatus.setText(getString(R.string.track_service_on,
                    PowerTime.clockNow()));
        } else if (tracking) {
            txtServiceStatus.setText(R.string.track_service_paused);
        } else {
            txtServiceStatus.setText(R.string.track_service_off);
        }

        binding = true;
        swTracking.setChecked(tracking);
        swGps.setChecked(AppPrefs.gpsEnabled(this));
        binding = false;

        boolean activityGranted = hasActivityPermission();
        cardPermission.setVisibility(activityGranted ? View.GONE : View.VISIBLE);
        if (!activityGranted) {
            SensorManager sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
            boolean hasStep = sensors != null
                    && sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
            txtPermission.setText(hasStep ? R.string.track_permission_body
                    : R.string.track_permission_sensor);
        }

        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        boolean unrestricted = power != null
                && power.isIgnoringBatteryOptimizations(getPackageName());
        txtBatteryStatus.setText(unrestricted
                ? R.string.track_battery_ok : R.string.track_battery_opt);

        NotificationManager notifications =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean notificationsOn = notifications != null
                && notifications.areNotificationsEnabled();
        txtNotificationStatus.setText(notificationsOn
                ? R.string.track_notifications_ok : R.string.track_notifications_off);

        long stamp = activity == null ? -1L : activity.updatedAt;
        if (stamp != lastRecentStamp) {
            lastRecentStamp = stamp;
            loadRecent(db);
        }
    }

    private void loadRecent(HistoryDb db) {
        List<HistoryDb.Activity> days = db.recentActivity(7);
        listRecent.removeAllViews();
        txtRecentEmpty.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        LocalDate today = LocalDate.now();
        for (HistoryDb.Activity day : days) {
            View row = inflater.inflate(R.layout.row_track_day, listRecent, false);
            TextView date = row.findViewById(R.id.txtDayDate);
            TextView meta = row.findViewById(R.id.txtDayMeta);
            TextView steps = row.findViewById(R.id.txtDaySteps);
            TextView distance = row.findViewById(R.id.txtDayDistance);

            String label;
            if (day.date.equals(today)) {
                label = getString(R.string.today);
            } else if (day.date.equals(today.minusDays(1))) {
                label = getString(R.string.yesterday);
            } else {
                label = PowerTime.shortDate(day.date);
            }
            date.setText(label);
            steps.setText(getString(R.string.track_steps_value, formatSteps(day.steps)));
            distance.setText(getString(R.string.km_value, day.displayDistanceM() / 1000.0));
            long sleep = day.sleepSeconds;
            meta.setText(getString(R.string.track_day_meta,
                    getString(R.string.track_active_value, day.activeMinutes()),
                    sleep <= 0L ? getString(R.string.dash)
                            : PowerTime.duration(sleep / 60L)));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Math.round(ThemeManager.dp(this, 8f));
            row.setLayoutParams(params);
            row.setBackground(ThemeManager.rounded(this,
                    theme.surface, theme.border, 14f));
            listRecent.addView(row);
        }
    }

    private void openBatterySettings() {
        try {
            PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
            if (power != null && !power.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent request = new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(request);
            } else {
                startActivity(new Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        } catch (Exception error) {
            try {
                startActivity(new Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.track_battery_na, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception error) {
            try {
                Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                details.setData(Uri.parse("package:" + getPackageName()));
                startActivity(details);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean hasActivityPermission() {
        return checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static String formatSteps(int steps) {
        return NumberFormat.getIntegerInstance(Locale.US).format(steps);
    }
}
