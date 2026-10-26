package com.nahid.vitality.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {

    public static final int BAR_WILL = 0;
    public static final int BAR_WEALTH = 1;
    public static final int BAR_ENERGY = 2;
    public static final int[] THRESHOLDS = {75, 50, 25, 10};

    private static final String FILE = "vitality_prefs";
    private static final String K_THEME = "theme";
    private static final String K_NOTIF = "notif_enabled";
    private static final String K_NOTIF_ASKED = "notif_asked";

    private AppPrefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int themeIndex(Context context) {
        return sp(context).getInt(K_THEME, 0);
    }

    public static void setThemeIndex(Context context, int index) {
        sp(context).edit().putInt(K_THEME, index).apply();
    }

    public static boolean notificationsEnabled(Context context) {
        return sp(context).getBoolean(K_NOTIF, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean(K_NOTIF, enabled).apply();
    }

    public static boolean notificationsAsked(Context context) {
        return sp(context).getBoolean(K_NOTIF_ASKED, false);
    }

    public static void setNotificationsAsked(Context context, boolean asked) {
        sp(context).edit().putBoolean(K_NOTIF_ASKED, asked).apply();
    }

    public static boolean barEnabled(Context context, int bar) {
        return sp(context).getBoolean("bar_" + bar, true);
    }

    public static void setBarEnabled(Context context, int bar, boolean enabled) {
        sp(context).edit().putBoolean("bar_" + bar, enabled).apply();
    }

    public static boolean thresholdEnabled(Context context, int threshold) {
        return sp(context).getBoolean("th_" + threshold, true);
    }

    public static void setThresholdEnabled(Context context, int threshold, boolean enabled) {
        sp(context).edit().putBoolean("th_" + threshold, enabled).apply();
    }

    public static boolean wasNotified(Context context, int bar, int threshold, String day) {
        return day.equals(sp(context).getString("n_" + bar + "_" + threshold, ""));
    }

    public static void markNotified(Context context, int bar, int threshold, String day) {
        sp(context).edit().putString("n_" + bar + "_" + threshold, day).apply();
    }

    public static boolean trackingEnabled(Context context) {
        return sp(context).getBoolean("track_enabled", false);
    }

    public static void setTrackingEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean("track_enabled", enabled).apply();
    }

    public static boolean trackingAsked(Context context) {
        return sp(context).getBoolean("track_asked", false);
    }

    public static void setTrackingAsked(Context context, boolean asked) {
        sp(context).edit().putBoolean("track_asked", asked).apply();
    }

    public static boolean gpsEnabled(Context context) {
        return sp(context).getBoolean("track_gps", true);
    }

    public static void setGpsEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean("track_gps", enabled).apply();
    }

    public static long lastStepAt(Context context) {
        return sp(context).getLong("track_last_step", 0L);
    }

    public static void setLastStepAt(Context context, long value) {
        sp(context).edit().putLong("track_last_step", value).apply();
    }

    public static long lastMotionAt(Context context) {
        return sp(context).getLong("track_last_motion", 0L);
    }

    public static void setLastMotionAt(Context context, long value) {
        sp(context).edit().putLong("track_last_motion", value).apply();
    }

    public static boolean sleeping(Context context) {
        return sp(context).getBoolean("track_sleeping", false);
    }

    public static void setSleeping(Context context, boolean sleeping) {
        sp(context).edit().putBoolean("track_sleeping", sleeping).apply();
    }

    public static long sleepStartedAt(Context context) {
        return sp(context).getLong("track_sleep_start", 0L);
    }

    public static void setSleepStartedAt(Context context, long value) {
        sp(context).edit().putLong("track_sleep_start", value).apply();
    }

    public static long sleepAccumSeconds(Context context) {
        return sp(context).getLong("track_sleep_accum", 0L);
    }

    public static void setSleepAccumSeconds(Context context, long value) {
        sp(context).edit().putLong("track_sleep_accum", value).apply();
    }

    public static long lastSleepSeconds(Context context) {
        return sp(context).getLong("track_last_sleep", 0L);
    }

    public static void setLastSleepSeconds(Context context, long value) {
        sp(context).edit().putLong("track_last_sleep", value).apply();
    }

    public static long lastTrackerTick(Context context) {
        return sp(context).getLong("track_last_tick", 0L);
    }

    public static void setLastTrackerTick(Context context, long value) {
        sp(context).edit().putLong("track_last_tick", value).apply();
    }

    public static float energyValue(Context context) {
        return sp(context).getFloat("energy_value", -1f);
    }

    public static void setEnergyValue(Context context, float value) {
        sp(context).edit().putFloat("energy_value", value).apply();
    }

    public static long energyUpdatedAt(Context context) {
        return sp(context).getLong("energy_updated", 0L);
    }

    public static void setEnergyUpdatedAt(Context context, long value) {
        sp(context).edit().putLong("energy_updated", value).apply();
    }

    public static int energyMode(Context context) {
        return sp(context).getInt("energy_mode", 2);
    }

    public static void setEnergyMode(Context context, int mode) {
        sp(context).edit().putInt("energy_mode", mode).apply();
    }

    public static String energyDay(Context context) {
        return sp(context).getString("energy_day", "");
    }

    public static void setEnergyDay(Context context, String day) {
        sp(context).edit().putString("energy_day", day).apply();
    }

    public static float energyDrainToday(Context context) {
        return sp(context).getFloat("energy_drain_today", 0f);
    }

    public static float energyRestToday(Context context) {
        return sp(context).getFloat("energy_rest_today", 0f);
    }

    public static float energySleepToday(Context context) {
        return sp(context).getFloat("energy_sleep_today", 0f);
    }

    public static void addEnergyEffect(Context context, float drain, float rest, float sleep) {
        sp(context).edit()
                .putFloat("energy_drain_today", energyDrainToday(context) + drain)
                .putFloat("energy_rest_today", energyRestToday(context) + rest)
                .putFloat("energy_sleep_today", energySleepToday(context) + sleep)
                .apply();
    }

    public static void resetEnergyEffects(Context context) {
        sp(context).edit()
                .putFloat("energy_drain_today", 0f)
                .putFloat("energy_rest_today", 0f)
                .putFloat("energy_sleep_today", 0f)
                .apply();
    }

    public static float stepSensorLast(Context context) {
        return sp(context).getFloat("step_sensor_last", -1f);
    }

    public static void setStepSensorLast(Context context, float value) {
        sp(context).edit().putFloat("step_sensor_last", value).apply();
    }

    public static String stepDay(Context context) {
        return sp(context).getString("step_day", "");
    }

    public static void setStepDay(Context context, String day) {
        sp(context).edit().putString("step_day", day).apply();
    }
}
