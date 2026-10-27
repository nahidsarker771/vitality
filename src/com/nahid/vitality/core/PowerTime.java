package com.nahid.vitality.core;

import android.content.Context;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class PowerTime {

    public static final int DEFAULT_START_MINUTE = 5 * 60;
    public static final int DEFAULT_END_MINUTE = 22 * 60;
    public static final int DAY_MINUTES = 24 * 60;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter CLOCK_SECONDS = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);

    private static int startMinute = DEFAULT_START_MINUTE;
    private static int endMinute = DEFAULT_END_MINUTE;

    private PowerTime() {
    }

    public static void configure(Context context) {
        applyWindow(AppPrefs.powerStartMinute(context), AppPrefs.powerEndMinute(context));
    }

    public static void applyWindow(int start, int end) {
        startMinute = Math.floorMod(start, DAY_MINUTES);
        endMinute = Math.floorMod(end, DAY_MINUTES);
        if (startMinute == endMinute) {
            startMinute = DEFAULT_START_MINUTE;
            endMinute = DEFAULT_END_MINUTE;
        }
    }

    public static int startMinute() {
        return startMinute;
    }

    public static int endMinute() {
        return endMinute;
    }

    public static boolean isCustomWindow() {
        return startMinute != DEFAULT_START_MINUTE || endMinute != DEFAULT_END_MINUTE;
    }

    public static String startClock() {
        return clockOfMinute(startMinute);
    }

    public static String endClock() {
        return clockOfMinute(endMinute);
    }

    public static String clockOfMinute(int minute) {
        int normalized = Math.floorMod(minute, DAY_MINUTES);
        return CLOCK.format(LocalTime.of(normalized / 60, normalized % 60));
    }

    private static int startSeconds() {
        return startMinute * 60;
    }

    private static int endSeconds() {
        return endMinute * 60;
    }

    private static int activeSpanSeconds() {
        int span = endSeconds() - startSeconds();
        if (span <= 0) {
            span += 24 * 3600;
        }
        return span;
    }

    public static int secondsOfDay(LocalDateTime time) {
        LocalTime t = time.toLocalTime();
        return t.getHour() * 3600 + t.getMinute() * 60 + t.getSecond();
    }

    private static boolean isActiveSeconds(int s) {
        int start = startSeconds();
        int end = endSeconds();
        if (start < end) {
            return s >= start && s < end;
        }
        return s >= start || s < end;
    }

    public static boolean isActive(LocalDateTime time) {
        return isActiveSeconds(secondsOfDay(time));
    }

    public static float percent(LocalDateTime time) {
        int s = secondsOfDay(time);
        if (!isActiveSeconds(s)) {
            return 0f;
        }
        int elapsed = s - startSeconds();
        if (elapsed < 0) {
            elapsed += 24 * 3600;
        }
        int span = activeSpanSeconds();
        return (span - elapsed) * 100f / span;
    }

    public static float nowPercent() {
        return percent(LocalDateTime.now());
    }

    public static long minutesUntilZero() {
        int s = secondsOfDay(LocalDateTime.now());
        if (!isActiveSeconds(s)) {
            return 0;
        }
        int end = endSeconds();
        int until = end > s ? end - s : 24 * 3600 - s + end;
        return (until + 59) / 60;
    }

    public static long minutesUntilStart() {
        int s = secondsOfDay(LocalDateTime.now());
        int start = startSeconds();
        int target = s < start ? start : 24 * 3600 + start;
        return ((target - s) + 59) / 60;
    }

    public static long minutesUntilEvent() {
        return isActive(LocalDateTime.now()) ? minutesUntilZero() : minutesUntilStart();
    }

    public static String duration(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        return m + "m";
    }

    public static String clockNow() {
        return CLOCK.format(LocalDateTime.now());
    }

    public static String clock(LocalDateTime time) {
        return CLOCK.format(time);
    }

    public static String clockWithSeconds(LocalDateTime time) {
        return CLOCK_SECONDS.format(time);
    }

    public static String fullDate(LocalDate date) {
        return FULL_DATE.format(date);
    }

    public static String shortDate(LocalDate date) {
        return SHORT_DATE.format(date);
    }

    public static String weekdayNarrow(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day.getDisplayName(TextStyle.NARROW, Locale.US);
    }

    public static LocalDate powerDay() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(startMinute / 60, startMinute % 60);
        if (now.isBefore(start)) {
            return LocalDate.now().minusDays(1);
        }
        return LocalDate.now();
    }

    public static String powerDayKey() {
        return powerDay().toString();
    }

    public static int roundedPercent(float percent) {
        return Math.max(0, Math.min(100, Math.round(percent)));
    }
}
