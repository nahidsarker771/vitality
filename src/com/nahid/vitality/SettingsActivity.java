package com.nahid.vitality;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.core.Themes;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.notify.Notifier;
import com.nahid.vitality.notify.WidgetRefresher;
import com.nahid.vitality.track.TrackingControl;
import com.nahid.vitality.track.TrackingService;

import java.util.List;

public class SettingsActivity extends BaseActivity {

    private static final int REQ_NOTIFICATIONS = 12;
    private static final int REQ_TRACKING = 13;

    private Switch swNotif;
    private Switch swWill;
    private Switch swWealth;
    private Switch swEnergy;
    private Switch swTracking;
    private TextView chip75;
    private TextView chip50;
    private TextView chip25;
    private TextView chip10;
    private TextView txtNotifPerm;
    private TextView txtScheduleStart;
    private TextView txtScheduleEnd;
    private TextView txtServiceStatus;
    private TextView txtBatteryStatus;
    private TextView txtNotificationStatus;
    private TextView txtDataInfo;
    private TextView txtVersion;
    private TextView txtDbInfo;
    private TextView txtRepo;
    private TextView txtThemeCurrent;
    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swNotif = findViewById(R.id.swNotif);
        swWill = findViewById(R.id.swWill);
        swWealth = findViewById(R.id.swWealth);
        swEnergy = findViewById(R.id.swEnergy);
        swTracking = findViewById(R.id.swTracking);
        chip75 = findViewById(R.id.chip75);
        chip50 = findViewById(R.id.chip50);
        chip25 = findViewById(R.id.chip25);
        chip10 = findViewById(R.id.chip10);
        txtNotifPerm = findViewById(R.id.txtNotifPerm);
        txtScheduleStart = findViewById(R.id.txtScheduleStart);
        txtScheduleEnd = findViewById(R.id.txtScheduleEnd);
        txtServiceStatus = findViewById(R.id.txtServiceStatus);
        txtBatteryStatus = findViewById(R.id.txtBatteryStatus);
        txtNotificationStatus = findViewById(R.id.txtNotificationStatus);
        txtDataInfo = findViewById(R.id.txtDataInfo);
        txtVersion = findViewById(R.id.txtVersion);
        txtDbInfo = findViewById(R.id.txtDbInfo);
        txtRepo = findViewById(R.id.txtRepo);
        txtThemeCurrent = findViewById(R.id.txtThemeCurrent);

        applyChrome(3);
        wireTheme();
        wireSchedule();
        wireTracking();
        wireNotifications();
        wireData();
        wireRepo();
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (swTracking != null) {
            updateUi();
        }
    }

    private void wireTheme() {
        TextView change = findViewById(R.id.btnChangeTheme);
        change.setText(R.string.change);
        change.setOnClickListener(view -> open(ThemesActivity.class));
        txtThemeCurrent.setText(getString(R.string.theme_current,
                getString(Themes.get(AppPrefs.themeIndex(this)).nameRes)));
    }

    private void wireSchedule() {
        findViewById(R.id.rowScheduleStart).setOnClickListener(view -> pickStartTime());
        findViewById(R.id.rowScheduleEnd).setOnClickListener(view -> pickEndTime());
        findViewById(R.id.btnScheduleReset).setOnClickListener(view ->
                applySchedule(PowerTime.DEFAULT_START_MINUTE, PowerTime.DEFAULT_END_MINUTE));
    }

    private void pickStartTime() {
        int minute = AppPrefs.powerStartMinute(this);
        new TimePickerDialog(dialogContext(), (view, hour, minuteOfHour) ->
                applySchedule(hour * 60 + minuteOfHour, AppPrefs.powerEndMinute(this)),
                minute / 60, minute % 60, DateFormat.is24HourFormat(this)).show();
    }

    private void pickEndTime() {
        int minute = AppPrefs.powerEndMinute(this);
        new TimePickerDialog(dialogContext(), (view, hour, minuteOfHour) ->
                applySchedule(AppPrefs.powerStartMinute(this), hour * 60 + minuteOfHour),
                minute / 60, minute % 60, DateFormat.is24HourFormat(this)).show();
    }

    private void applySchedule(int startMinute, int endMinute) {
        if (startMinute == endMinute) {
            Toast.makeText(this, R.string.schedule_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        AppPrefs.setPowerWindow(this, startMinute, endMinute);
        PowerTime.configure(this);
        if (AppPrefs.trackingEnabled(this) && TrackingControl.hasActivityPermission(this)) {
            TrackingService.refresh(this);
        }
        WidgetRefresher.updateAll(this);
        updateUi();
    }

    private void wireTracking() {
        swTracking.setOnCheckedChangeListener((button, checked) -> {
            if (binding) {
                return;
            }
            if (checked) {
                requestTrackingPermissions();
            } else {
                TrackingControl.disable(this);
                updateUi();
            }
        });

        TextView battery = findViewById(R.id.btnBattery);
        battery.setText(R.string.track_battery_btn);
        battery.setOnClickListener(view -> TrackingControl.openBatterySettings(this));

        TextView notifications = findViewById(R.id.btnNotifications);
        notifications.setText(R.string.track_notifications_btn);
        notifications.setOnClickListener(view ->
                TrackingControl.openNotificationSettings(this));
    }

    private void requestTrackingPermissions() {
        List<String> needed = TrackingControl.requiredPermissions(this);
        if (needed.isEmpty()) {
            enableTracking();
        } else {
            requestPermissions(needed.toArray(new String[0]), REQ_TRACKING);
        }
    }

    private void enableTracking() {
        if (!TrackingControl.enable(this)) {
            Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
        }
        updateUi();
    }

    private void wireNotifications() {
        swNotif.setOnCheckedChangeListener((button, checked) -> {
            if (binding) {
                return;
            }
            AppPrefs.setNotificationsEnabled(this, checked);
            if (checked) {
                requestNotificationPermissionIfNeeded();
            }
            updateUi();
        });
        swWill.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) {
                AppPrefs.setBarEnabled(this, AppPrefs.BAR_WILL, checked);
            }
        });
        swWealth.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) {
                AppPrefs.setBarEnabled(this, AppPrefs.BAR_WEALTH, checked);
            }
        });
        swEnergy.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) {
                AppPrefs.setBarEnabled(this, AppPrefs.BAR_ENERGY, checked);
            }
        });

        bindChip(chip75, 75);
        bindChip(chip50, 50);
        bindChip(chip25, 25);
        bindChip(chip10, 10);

        txtNotifPerm.setOnClickListener(view -> requestNotificationPermissionIfNeeded());

        TextView refresh = findViewById(R.id.btnRefreshWidgets);
        refresh.setOnClickListener(view -> {
            WidgetRefresher.updateAll(this);
            Toast.makeText(this, R.string.widgets_refreshed, Toast.LENGTH_SHORT).show();
        });
    }

    private void wireData() {
        TextView export = findViewById(R.id.btnExport);
        export.setOnClickListener(view -> exportHistory());

        TextView clear = findViewById(R.id.btnClear);
        clear.setOnClickListener(view -> confirmClear());
    }

    private void wireRepo() {
        txtRepo.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.about_repo_url))));
            } catch (Exception error) {
                Toast.makeText(this, R.string.about_repo, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindChip(TextView chip, int threshold) {
        chip.setOnClickListener(view -> {
            if (!AppPrefs.notificationsEnabled(this)) {
                return;
            }
            AppPrefs.setThresholdEnabled(this, threshold,
                    !AppPrefs.thresholdEnabled(this, threshold));
            updateUi();
        });
    }

    private void updateUi() {
        int themeIndex = AppPrefs.themeIndex(this);
        binding = true;
        swNotif.setChecked(AppPrefs.notificationsEnabled(this));
        swWill.setChecked(AppPrefs.barEnabled(this, AppPrefs.BAR_WILL));
        swWealth.setChecked(AppPrefs.barEnabled(this, AppPrefs.BAR_WEALTH));
        swEnergy.setChecked(AppPrefs.barEnabled(this, AppPrefs.BAR_ENERGY));
        swTracking.setChecked(AppPrefs.trackingEnabled(this));
        binding = false;

        boolean master = AppPrefs.notificationsEnabled(this);
        swWill.setEnabled(master);
        swWealth.setEnabled(master);
        swEnergy.setEnabled(master);
        swWill.setAlpha(master ? 1f : 0.45f);
        swWealth.setAlpha(master ? 1f : 0.45f);
        swEnergy.setAlpha(master ? 1f : 0.45f);

        styleChip(chip75, 75, master);
        styleChip(chip50, 50, master);
        styleChip(chip25, 25, master);
        styleChip(chip10, 10, master);

        findViewById(R.id.dotWill).setBackground(ThemeManager.oval(theme.will));
        findViewById(R.id.dotWealth).setBackground(ThemeManager.oval(theme.wealth));
        findViewById(R.id.dotEnergy).setBackground(ThemeManager.oval(theme.energy));

        boolean tracking = AppPrefs.trackingEnabled(this);
        if (tracking && !TrackingControl.hasActivityPermission(this)) {
            txtServiceStatus.setText(R.string.track_permission_needed);
        } else if (tracking && TrackingService.isRunning()) {
            txtServiceStatus.setText(R.string.track_service_on);
        } else if (tracking) {
            txtServiceStatus.setText(R.string.track_service_paused);
        } else {
            txtServiceStatus.setText(R.string.track_service_off);
        }
        txtBatteryStatus.setText(TrackingControl.batteryUnrestricted(this)
                ? R.string.track_battery_ok : R.string.track_battery_opt);
        txtNotificationStatus.setText(TrackingControl.notificationsEnabled(this)
                ? R.string.track_notifications_ok : R.string.track_notifications_off);

        boolean needsPermission = master && !Notifier.canNotify(this);
        txtNotifPerm.setVisibility(needsPermission ? TextView.VISIBLE : TextView.GONE);

        txtScheduleStart.setText(PowerTime.startClock());
        txtScheduleEnd.setText(PowerTime.endClock());
        txtThemeCurrent.setText(getString(R.string.theme_current,
                getString(Themes.get(themeIndex).nameRes)));
        updateData();
        updateVersion();
    }

    private void styleChip(TextView chip, int threshold, boolean master) {
        boolean selected = AppPrefs.thresholdEnabled(this, threshold);
        chip.setEnabled(master);
        chip.setAlpha(master ? 1f : 0.45f);
        chip.setTextColor(selected ? theme.will : theme.text);
        chip.setBackground(selected
                ? ThemeManager.rounded(this,
                ThemeManager.withAlpha(theme.will, 0.18f),
                ThemeManager.withAlpha(theme.will, 0.55f), 99f)
                : ThemeManager.rounded(this, theme.surfaceAlt, theme.border, 99f));
    }

    private void updateData() {
        HistoryDb db = HistoryDb.get(this);
        txtDataInfo.setText(getString(R.string.data_info,
                db.countDays(), HistoryDb.SCHEMA_VERSION));
        txtDbInfo.setText(getString(R.string.db_line, HistoryDb.SCHEMA_VERSION));
    }

    private void updateVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtVersion.setText(getString(R.string.version_line,
                    info.versionName, info.versionCode));
        } catch (PackageManager.NameNotFoundException error) {
            txtVersion.setText(getString(R.string.version_line, "1.0", 10000));
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATIONS);
            } else {
                updateUi();
            }
        } else {
            try {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            } catch (Exception ignored) {
                openAppDetails();
            }
        }
    }

    private void openAppDetails() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_TRACKING) {
            if (!TrackingControl.hasActivityPermission(this)) {
                Toast.makeText(this, R.string.track_permission_denied, Toast.LENGTH_SHORT).show();
                updateUi();
                return;
            }
            enableTracking();
        } else {
            updateUi();
        }
    }

    private void exportHistory() {
        String csv = HistoryDb.get(this).csv();
        if (csv.indexOf('\n') == csv.length() - 1) {
            Toast.makeText(this, R.string.nothing_to_export, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject));
        send.putExtra(Intent.EXTRA_TEXT, csv);
        startActivity(Intent.createChooser(send, getString(R.string.export_csv)));
    }

    private void confirmClear() {
        new AlertDialog.Builder(dialogContext())
                .setTitle(R.string.clear_confirm_title)
                .setMessage(R.string.clear_confirm_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    HistoryDb.get(this).clear();
                    updateData();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
