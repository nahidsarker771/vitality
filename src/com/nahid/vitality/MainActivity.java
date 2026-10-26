package com.nahid.vitality;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.notify.AlarmScheduler;
import com.nahid.vitality.notify.Notifier;
import com.nahid.vitality.notify.WidgetRefresher;
import com.nahid.vitality.ui.PowerCardView;
import com.nahid.vitality.ui.ScoreRingView;

import java.time.LocalDateTime;

public class MainActivity extends BaseActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, 1000);
        }
    };

    private TextView txtGreeting;
    private TextView txtDate;
    private TextView txtClock;
    private TextView txtLive;
    private TextView txtOverall;
    private TextView txtCountdown;
    private TextView txtCountdownLabel;
    private TextView txtSamples;
    private ScoreRingView ring;
    private PowerCardView cardWill;
    private PowerCardView cardWealth;
    private PowerCardView cardEnergy;
    private int lastSampleMinute = -1;
    private int lastWidgetMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtGreeting = findViewById(R.id.txtGreeting);
        txtDate = findViewById(R.id.txtDate);
        txtClock = findViewById(R.id.txtClock);
        txtLive = findViewById(R.id.txtLive);
        txtOverall = findViewById(R.id.txtOverall);
        txtCountdown = findViewById(R.id.txtCountdown);
        txtCountdownLabel = findViewById(R.id.txtCountdownLabel);
        txtSamples = findViewById(R.id.txtSamples);
        ring = findViewById(R.id.ring);
        cardWill = findViewById(R.id.cardWill);
        cardWealth = findViewById(R.id.cardWealth);
        cardEnergy = findViewById(R.id.cardEnergy);

        cardWill.setKind(PowerCardView.WILL);
        cardWealth.setKind(PowerCardView.WEALTH);
        cardEnergy.setKind(PowerCardView.ENERGY);

        applyChrome(0);

        Notifier.ensureChannel(this);
        AlarmScheduler.schedule(this);
        updateSamples();
        updateNow();
        maybeAskNotificationPermission();
        WidgetRefresher.updateAll(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void tick() {
        updateNow();

        LocalDateTime now = LocalDateTime.now();
        boolean active = PowerTime.isActive(now);
        float percent = PowerTime.percent(now);
        int minute = now.getHour() * 60 + now.getMinute();

        if (active && minute != lastSampleMinute) {
            lastSampleMinute = minute;
            HistoryDb.sample(this, percent);
            Notifier.check(this, percent);
            updateSamples();
        }
        if (minute != lastWidgetMinute && minute % 5 == 0) {
            lastWidgetMinute = minute;
            WidgetRefresher.updateAll(this);
        }
    }

    private void updateNow() {
        LocalDateTime now = LocalDateTime.now();
        boolean active = PowerTime.isActive(now);
        float percent = PowerTime.percent(now);
        int rounded = PowerTime.roundedPercent(percent);

        txtGreeting.setText(greeting(now.getHour()));
        txtDate.setText(PowerTime.fullDate(now.toLocalDate()));
        txtClock.setText(PowerTime.clockWithSeconds(now));
        txtOverall.setText(rounded + "%");
        txtLive.setText(active ? R.string.live : R.string.status_resting);

        ring.setValue(percent);
        cardWill.setPercent(percent);
        cardWealth.setPercent(percent);
        cardEnergy.setPercent(percent);

        CharSequence status = getString(active ? R.string.status_live : R.string.status_resting);
        String timeLeft;
        if (active) {
            String zero = PowerTime.duration(PowerTime.minutesUntilZero());
            txtCountdownLabel.setText(R.string.draining_to_zero);
            txtCountdown.setText(zero);
            timeLeft = getString(R.string.drain_left, zero);
        } else {
            String start = PowerTime.duration(PowerTime.minutesUntilStart());
            txtCountdownLabel.setText(R.string.recharging_upper);
            txtCountdown.setText(getString(R.string.reset_in, start));
            timeLeft = getString(R.string.reset_in, start);
        }
        cardWill.setStatus(status, timeLeft);
        cardWealth.setStatus(status, timeLeft);
        cardEnergy.setStatus(status, timeLeft);
    }

    private void updateSamples() {
        int samples = HistoryDb.get(this).samplesForDay(PowerTime.powerDayKey());
        txtSamples.setText(String.valueOf(samples));
    }

    private int greeting(int hour) {
        if (hour >= 5 && hour < 12) {
            return R.string.greeting_morning;
        }
        if (hour >= 12 && hour < 17) {
            return R.string.greeting_afternoon;
        }
        if (hour >= 17 && hour < 22) {
            return R.string.greeting_evening;
        }
        return R.string.greeting_resting;
    }

    private void maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33 || AppPrefs.notificationsAsked(this)) {
            return;
        }
        AppPrefs.setNotificationsAsked(this, true);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
    }
}
