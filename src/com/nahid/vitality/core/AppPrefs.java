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
}
