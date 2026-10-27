package com.nahid.vitality;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.track.SpeedStats;
import com.nahid.vitality.track.Tracker;
import com.nahid.vitality.track.TrackingControl;
import com.nahid.vitality.track.TrackingService;
import com.nahid.vitality.ui.DayActivityChartView;
import com.nahid.vitality.ui.VitalityBarView;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TrackActivity extends BaseActivity {

    private static final int REQ_TRACK = 21;
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
    private TextView txtPermissionTitle;
    private TextView txtPermission;
    private TextView btnPermission;
    private LinearLayout cardPermission;
    private LinearLayout listRecent;
    private TextView txtRecentEmpty;
    private VitalityBarView energyBar;
    private DayActivityChartView dayChart;
    private TextView txtActivityEmpty;
    private TextView txtSpeedLast;
    private TextView txtSpeedTop;
    private TextView txtSpeedLow;
    private int[] minuteSteps;
    private long lastRecentStamp = Long.MIN_VALUE;
    private long lastChartStamp = Long.MIN_VALUE;

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
        txtPermissionTitle = findViewById(R.id.txtPermissionTitle);
        txtPermission = findViewById(R.id.txtPermission);
        btnPermission = findViewById(R.id.btnPermission);
        cardPermission = findViewById(R.id.cardPermission);
        listRecent = findViewById(R.id.listRecent);
        txtRecentEmpty = findViewById(R.id.txtRecentEmpty);
        energyBar = findViewById(R.id.trackEnergyBar);
        dayChart = findViewById(R.id.trackDayChart);
        txtActivityEmpty = findViewById(R.id.txtActivityEmpty);
        txtSpeedLast = findViewById(R.id.txtSpeedLast);
        txtSpeedTop = findViewById(R.id.txtSpeedTop);
        txtSpeedLow = findViewById(R.id.txtSpeedLow);

        applyChrome(1);
        energyBar.setColors(theme.energy, ThemeManager.lighten(theme.energy, 0.35f));

        wirePermission();
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (AppPrefs.trackingEnabled(this) && TrackingControl.hasActivityPermission(this)) {
            TrackingService.ensureRunning(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void wirePermission() {
        btnPermission.setOnClickListener(view -> requestTrackingPermissions());
    }

    private void requestTrackingPermissions() {
        List<String> needed = TrackingControl.requiredPermissions(this);
        if (needed.isEmpty()) {
            enableTracking();
        } else {
            requestPermissions(needed.toArray(new String[0]), REQ_TRACK);
        }
    }

    private void enableTracking() {
        if (!TrackingControl.enable(this)) {
            Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
        }
        updateUi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_TRACK) {
            return;
        }
        if (!TrackingControl.hasActivityPermission(this)) {
            Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
            updateUi();
            return;
        }
        enableTracking();
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

        boolean granted = TrackingControl.hasActivityPermission(this);
        boolean needsSetup = !granted || !tracking;
        cardPermission.setVisibility(needsSetup ? View.VISIBLE : View.GONE);
        if (needsSetup) {
            if (!granted) {
                SensorManager sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
                boolean hasStep = sensors != null
                        && sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
                txtPermissionTitle.setText(R.string.track_permission_title);
                txtPermission.setText(hasStep ? R.string.track_permission_body
                        : R.string.track_permission_sensor);
                btnPermission.setText(R.string.track_allow);
            } else {
                txtPermissionTitle.setText(R.string.track_enable_title);
                txtPermission.setText(R.string.track_enable_body);
                btnPermission.setText(R.string.track_enable);
            }
        }

        long stamp = activity == null ? -1L : activity.updatedAt;
        if (stamp != lastChartStamp) {
            lastChartStamp = stamp;
            minuteSteps = db.minuteStepsForDay(PowerTime.powerDayKey());
            dayChart.setMinuteSteps(minuteSteps);
            txtActivityEmpty.setVisibility(dayChart.hasData() ? View.GONE : View.VISIBLE);
        }
        SpeedStats speeds = SpeedStats.fromMinutes(minuteSteps, currentMinuteOfDay());
        txtSpeedLast.setText(speeds.hasLast10
                ? getString(R.string.track_speed_value, speeds.last10Kmh)
                : getString(R.string.dash));
        txtSpeedTop.setText(speeds.hasRange
                ? getString(R.string.track_speed_value, speeds.topKmh)
                : getString(R.string.dash));
        txtSpeedLow.setText(speeds.hasRange
                ? getString(R.string.track_speed_value, speeds.lowKmh)
                : getString(R.string.dash));

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

    private static int currentMinuteOfDay() {
        LocalTime now = LocalTime.now();
        return now.getHour() * 60 + now.getMinute();
    }

    private static String formatSteps(int steps) {
        return NumberFormat.getIntegerInstance(Locale.US).format(steps);
    }
}
