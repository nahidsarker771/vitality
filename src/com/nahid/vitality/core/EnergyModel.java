package com.nahid.vitality.core;

import android.content.Context;

public final class EnergyModel {

    public static final int MODE_REST = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_ACTIVE = 2;
    public static final int MODE_SLEEP = 3;

    public static final float STEP_DRAIN = 0.004f;

    private static final float SLEEP_GAIN_PER_HOUR = 12f;
    private static final float REST_GAIN_PER_HOUR = 3f;
    private static final float LIGHT_DRAIN_PER_HOUR = 1.5f;
    private static final float ACTIVE_DRAIN_PER_HOUR = 5f;

    private static final long ACTIVE_WINDOW_MS = 2 * 60 * 1000L;
    private static final long LIGHT_WINDOW_MS = 5 * 60 * 1000L;
    private static final float MAX_ELAPSED_HOURS = 12f;

    private EnergyModel() {
    }

    public static void initialize(Context context) {
        long now = System.currentTimeMillis();
        long updated = AppPrefs.energyUpdatedAt(context);
        if (AppPrefs.energyValue(context) < 0f || updated <= 0L
                || now - updated > 6 * 60 * 60 * 1000L) {
            AppPrefs.setEnergyValue(context, PowerTime.nowPercent());
            AppPrefs.setEnergyUpdatedAt(context, now);
        }
        AppPrefs.setEnergyMode(context, modeFor(context, now));
        ensureDay(context);
    }

    public static float current(Context context) {
        if (!AppPrefs.trackingEnabled(context)) {
            return PowerTime.nowPercent();
        }
        synchronized (EnergyModel.class) {
            accrue(context, System.currentTimeMillis());
            return clamp(AppPrefs.energyValue(context));
        }
    }

    public static void onSteps(Context context, int steps) {
        if (steps <= 0 || !AppPrefs.trackingEnabled(context)) {
            return;
        }
        synchronized (EnergyModel.class) {
            long now = System.currentTimeMillis();
            accrue(context, now);
            float drain = steps * STEP_DRAIN;
            AppPrefs.setEnergyValue(context, clamp(AppPrefs.energyValue(context) - drain));
            AppPrefs.addEnergyEffect(context, drain, 0f, 0f);
            AppPrefs.setEnergyUpdatedAt(context, now);
            AppPrefs.setEnergyMode(context, modeFor(context, now));
        }
    }

    public static void addBonus(Context context, float percent, boolean sleep) {
        if (percent <= 0f) {
            return;
        }
        synchronized (EnergyModel.class) {
            accrue(context, System.currentTimeMillis());
            AppPrefs.setEnergyValue(context, clamp(AppPrefs.energyValue(context) + percent));
            if (sleep) {
                AppPrefs.addEnergyEffect(context, 0f, 0f, percent);
            } else {
                AppPrefs.addEnergyEffect(context, 0f, percent, 0f);
            }
        }
    }

    public static void refreshMode(Context context) {
        if (!AppPrefs.trackingEnabled(context)) {
            return;
        }
        synchronized (EnergyModel.class) {
            long now = System.currentTimeMillis();
            accrue(context, now);
            AppPrefs.setEnergyMode(context, modeFor(context, now));
        }
    }

    public static float drainToday(Context context) {
        return AppPrefs.energyDrainToday(context);
    }

    public static float restToday(Context context) {
        return AppPrefs.energyRestToday(context);
    }

    public static float sleepToday(Context context) {
        return AppPrefs.energySleepToday(context);
    }

    public static String modeLabelKey(Context context) {
        switch (modeFor(context, System.currentTimeMillis())) {
            case MODE_SLEEP:
                return "sleep";
            case MODE_ACTIVE:
                return "active";
            case MODE_LIGHT:
                return "light";
            default:
                return "rest";
        }
    }

    private static void accrue(Context context, long now) {
        if (!AppPrefs.trackingEnabled(context)) {
            return;
        }
        float value = AppPrefs.energyValue(context);
        if (value < 0f) {
            initialize(context);
            return;
        }
        ensureDay(context);

        long last = AppPrefs.energyUpdatedAt(context);
        if (last <= 0L) {
            AppPrefs.setEnergyUpdatedAt(context, now);
            AppPrefs.setEnergyMode(context, modeFor(context, now));
            return;
        }
        if (now <= last) {
            AppPrefs.setEnergyUpdatedAt(context, now);
            return;
        }

        float hours = Math.min(MAX_ELAPSED_HOURS, (now - last) / 3_600_000f);
        int mode = AppPrefs.energyMode(context);
        float drain = 0f;
        float rest = 0f;
        float sleep = 0f;

        if (mode == MODE_SLEEP) {
            float gain = hours * SLEEP_GAIN_PER_HOUR;
            value += gain;
            sleep += gain;
        } else if (mode == MODE_ACTIVE) {
            float active = Math.min(hours, ACTIVE_WINDOW_MS / 3_600_000f);
            float left = hours - active;
            float light = Math.min(left, (LIGHT_WINDOW_MS - ACTIVE_WINDOW_MS) / 3_600_000f);
            float restHours = Math.max(0f, left - light);
            float lost = active * ACTIVE_DRAIN_PER_HOUR + light * LIGHT_DRAIN_PER_HOUR;
            value -= lost;
            drain += lost;
            value += restHours * REST_GAIN_PER_HOUR;
            rest += restHours * REST_GAIN_PER_HOUR;
        } else if (mode == MODE_LIGHT) {
            float light = Math.min(hours, LIGHT_WINDOW_MS / 3_600_000f);
            float restHours = hours - light;
            float lost = light * LIGHT_DRAIN_PER_HOUR;
            value -= lost;
            drain += lost;
            value += restHours * REST_GAIN_PER_HOUR;
            rest += restHours * REST_GAIN_PER_HOUR;
        } else {
            float gain = hours * REST_GAIN_PER_HOUR;
            value += gain;
            rest += gain;
        }

        AppPrefs.setEnergyValue(context, clamp(value));
        AppPrefs.addEnergyEffect(context, drain, rest, sleep);
        AppPrefs.setEnergyUpdatedAt(context, now);
        AppPrefs.setEnergyMode(context, modeFor(context, now));
    }

    private static void ensureDay(Context context) {
        String day = PowerTime.powerDayKey();
        if (!day.equals(AppPrefs.energyDay(context))) {
            AppPrefs.setEnergyDay(context, day);
            AppPrefs.resetEnergyEffects(context);
        }
    }

    private static int modeFor(Context context, long now) {
        if (AppPrefs.sleeping(context)) {
            return MODE_SLEEP;
        }
        long last = Math.max(AppPrefs.lastStepAt(context), AppPrefs.lastMotionAt(context));
        if (last <= 0L) {
            return MODE_REST;
        }
        long age = now - last;
        if (age < ACTIVE_WINDOW_MS) {
            return MODE_ACTIVE;
        }
        if (age < LIGHT_WINDOW_MS) {
            return MODE_LIGHT;
        }
        return MODE_REST;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }
}
