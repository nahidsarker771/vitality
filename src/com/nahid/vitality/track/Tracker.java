package com.nahid.vitality.track;

import android.content.Context;
import android.os.PowerManager;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.data.HistoryDb;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class Tracker {

    public static final int SLEEP_START_HOUR = 22;
    public static final int SLEEP_END_HOUR = 11;

    private static final long SLEEP_STILL_MS = 30 * 60 * 1000L;
    private static final long WAKE_STEPS = 5;
    private static final long MOTION_WRITE_GAP_MS = 60_000L;
    private static final long REST_GAP_MS = 5 * 60 * 1000L;
    private static final long MAX_TICK_MS = 30 * 60 * 1000L;
    private static final long MAX_SLEEP_TICK_MS = 12 * 60 * 60 * 1000L;
    private static final float SLEEP_BONUS_PER_HOUR = 9f;

    private Tracker() {
    }

    public static void tick(Context context) {
        if (!AppPrefs.trackingEnabled(context)) {
            AppPrefs.setLastTrackerTick(context, System.currentTimeMillis());
            return;
        }

        long now = System.currentTimeMillis();
        long last = AppPrefs.lastTrackerTick(context);
        long delta = last <= 0L ? 0L : Math.max(0L, now - last);
        AppPrefs.setLastTrackerTick(context, now);

        EnergyModel.current(context);

        boolean screenOn = isScreenOn(context);
        boolean night = isNight(now);
        boolean sleeping = AppPrefs.sleeping(context);
        long lastMotion = Math.max(AppPrefs.lastStepAt(context), AppPrefs.lastMotionAt(context));

        if (!sleeping) {
            if (night && !screenOn
                    && (lastMotion <= 0L || now - lastMotion >= SLEEP_STILL_MS)) {
                startSleep(context, now);
                sleeping = true;
            }
        } else {
            if (!night || screenOn) {
                endSleep(context);
                sleeping = false;
            }
        }

        if (sleeping) {
            long sleepMs = Math.min(delta, MAX_SLEEP_TICK_MS);
            if (sleepMs > 0L) {
                addSleep(context, sleepMs / 1000L);
            }
        } else if (delta > 0L) {
            long sinceStep = AppPrefs.lastStepAt(context) <= 0L
                    ? Long.MAX_VALUE
                    : now - AppPrefs.lastStepAt(context);
            if (sinceStep >= REST_GAP_MS) {
                long restMs = Math.min(delta, MAX_TICK_MS);
                if (restMs > 0L) {
                    HistoryDb.get(context).addRestSeconds(
                            PowerTime.powerDayKey(), restMs / 1000L);
                }
            }
        }

        EnergyModel.refreshMode(context);
    }

    public static void onStep(Context context, int stepDelta) {
        long now = System.currentTimeMillis();
        AppPrefs.setLastStepAt(context, now);
        AppPrefs.setLastMotionAt(context, now);
        if (!AppPrefs.trackingEnabled(context)) {
            return;
        }
        EnergyModel.onSteps(context, stepDelta);
        if (AppPrefs.sleeping(context) && stepDelta >= WAKE_STEPS) {
            endSleep(context);
            EnergyModel.refreshMode(context);
        }
    }

    public static void onMotion(Context context, float magnitude) {
        if (magnitude < 0.9f) {
            return;
        }
        if (AppPrefs.sleeping(context) && magnitude > 2.2f) {
            endSleep(context);
            EnergyModel.refreshMode(context);
            AppPrefs.setLastMotionAt(context, System.currentTimeMillis());
            return;
        }
        long now = System.currentTimeMillis();
        if (now - AppPrefs.lastMotionAt(context) < MOTION_WRITE_GAP_MS) {
            return;
        }
        AppPrefs.setLastMotionAt(context, now);
    }

    public static void startSleep(Context context, long now) {
        long lastMotion = AppPrefs.lastMotionAt(context);
        long from = Math.max(nightStartMs(now), now - MAX_SLEEP_TICK_MS);
        if (lastMotion > 0L) {
            from = Math.max(from, lastMotion + SLEEP_STILL_MS);
        }
        if (from > now) {
            from = now;
        }
        AppPrefs.setSleeping(context, true);
        AppPrefs.setSleepStartedAt(context, from);
        AppPrefs.setSleepAccumSeconds(context, 0L);
        long backfillMs = now - from;
        if (backfillMs > 0L) {
            addSleep(context, backfillMs / 1000L);
            float hours = Math.min(12f, backfillMs / 3_600_000f);
            EnergyModel.addBonus(context, hours * SLEEP_BONUS_PER_HOUR, true);
        }
        EnergyModel.refreshMode(context);
    }

    public static void endSleep(Context context) {
        if (!AppPrefs.sleeping(context)) {
            return;
        }
        long accum = AppPrefs.sleepAccumSeconds(context);
        if (accum > 0L) {
            AppPrefs.setLastSleepSeconds(context, accum);
        }
        AppPrefs.setSleeping(context, false);
        AppPrefs.setSleepStartedAt(context, 0L);
        AppPrefs.setSleepAccumSeconds(context, 0L);
    }

    public static boolean isNight(long now) {
        LocalTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(now), ZoneId.systemDefault()).toLocalTime();
        int hour = time.getHour();
        return hour >= SLEEP_START_HOUR || hour < SLEEP_END_HOUR;
    }

    public static long nightStartMs(long now) {
        LocalDateTime local = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(now), ZoneId.systemDefault());
        LocalDateTime start;
        if (local.getHour() < SLEEP_END_HOUR) {
            start = local.toLocalDate().minusDays(1).atTime(SLEEP_START_HOUR, 0);
        } else {
            start = local.toLocalDate().atTime(SLEEP_START_HOUR, 0);
        }
        return start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long currentOrLastSleepSeconds(Context context) {
        if (AppPrefs.sleeping(context)) {
            long started = AppPrefs.sleepStartedAt(context);
            if (started > 0L) {
                return Math.max(AppPrefs.sleepAccumSeconds(context),
                        (System.currentTimeMillis() - started) / 1000L);
            }
            return AppPrefs.sleepAccumSeconds(context);
        }
        return AppPrefs.lastSleepSeconds(context);
    }

    private static void addSleep(Context context, long seconds) {
        if (seconds <= 0L) {
            return;
        }
        HistoryDb.get(context).addSleepSeconds(PowerTime.powerDayKey(), seconds);
        AppPrefs.setSleepAccumSeconds(context,
                AppPrefs.sleepAccumSeconds(context) + seconds);
    }

    private static boolean isScreenOn(Context context) {
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power == null || power.isInteractive();
    }
}
