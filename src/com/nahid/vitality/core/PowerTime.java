package com.nahid.vitality.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class PowerTime {

    public static final int START_HOUR = 5;
    public static final int END_HOUR = 22;
    private static final int START_SECONDS = START_HOUR * 3600;
    private static final int END_SECONDS = END_HOUR * 3600;
    private static final int SPAN_SECONDS = END_SECONDS - START_SECONDS;
    private static final int DAY_SECONDS = 24 * 3600;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter CLOCK_SECONDS = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);

    private PowerTime() {
    }

    public static int secondsOfDay(LocalDateTime time) {
        LocalTime t = time.toLocalTime();
        return t.getHour() * 3600 + t.getMinute() * 60 + t.getSecond();
    }

    public static boolean isActive(LocalDateTime time) {
        int s = secondsOfDay(time);
        return s >= START_SECONDS && s < END_SECONDS;
    }

    public static float percent(LocalDateTime time) {
        int s = secondsOfDay(time);
        if (s < START_SECONDS || s >= END_SECONDS) {
            return 0f;
        }
        return (END_SECONDS - s) * 100f / SPAN_SECONDS;
    }

    public static float nowPercent() {
        return percent(LocalDateTime.now());
    }

    public static long minutesUntilZero() {
        int s = secondsOfDay(LocalDateTime.now());
        if (s < START_SECONDS || s >= END_SECONDS) {
            return 0;
        }
        return ((END_SECONDS - s) + 59) / 60;
    }

    public static long minutesUntilStart() {
        int s = secondsOfDay(LocalDateTime.now());
        int target;
        if (s < START_SECONDS) {
            target = START_SECONDS;
        } else {
            target = DAY_SECONDS + START_SECONDS;
        }
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
        if (now.isBefore(LocalTime.of(START_HOUR, 0))) {
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
