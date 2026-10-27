package com.nahid.vitality.track;

public final class SpeedStats {

    private static final int MINUTES_PER_DAY = 1440;
    private static final int LAST_WINDOW = 10;
    private static final int RANGE_WINDOW = 3;
    private static final int ACTIVE_MINUTE_STEPS = 30;
    private static final int MIN_WINDOW_STEPS = 90;

    public final float last10Kmh;
    public final float topKmh;
    public final float lowKmh;
    public final boolean hasLast10;
    public final boolean hasRange;

    private SpeedStats(float last10Kmh, float topKmh, float lowKmh,
                       boolean hasLast10, boolean hasRange) {
        this.last10Kmh = last10Kmh;
        this.topKmh = topKmh;
        this.lowKmh = lowKmh;
        this.hasLast10 = hasLast10;
        this.hasRange = hasRange;
    }

    public static SpeedStats fromMinutes(int[] perMinute, int minuteOfDay) {
        if (perMinute == null || perMinute.length < MINUTES_PER_DAY) {
            return empty();
        }
        int now = Math.max(0, Math.min(MINUTES_PER_DAY, minuteOfDay));

        int lastFrom = Math.max(0, now - LAST_WINDOW);
        int lastSteps = 0;
        int lastActive = 0;
        for (int m = lastFrom; m < now; m++) {
            int steps = Math.max(0, perMinute[m]);
            lastSteps += steps;
            if (steps >= ACTIVE_MINUTE_STEPS) {
                lastActive++;
            }
        }
        boolean hasLast10 = lastSteps > 0 && lastActive > 0;
        float last10 = hasLast10
                ? (float) StepMetrics.kmh(lastSteps, lastActive)
                : 0f;

        float top = 0f;
        float low = 0f;
        boolean hasRange = false;
        int lastEnd = Math.min(now - 1, MINUTES_PER_DAY - 1);
        for (int end = RANGE_WINDOW - 1; end <= lastEnd; end++) {
            int sum = 0;
            int active = 0;
            for (int m = end - RANGE_WINDOW + 1; m <= end; m++) {
                int steps = Math.max(0, perMinute[m]);
                sum += steps;
                if (steps >= ACTIVE_MINUTE_STEPS) {
                    active++;
                }
            }
            if (sum < MIN_WINDOW_STEPS || active < 2) {
                continue;
            }
            float speed = (float) StepMetrics.kmh(sum, active);
            if (!hasRange || speed > top) {
                top = speed;
            }
            if (!hasRange || speed < low) {
                low = speed;
            }
            hasRange = true;
        }

        return new SpeedStats(last10, top, low, hasLast10, hasRange);
    }

    private static SpeedStats empty() {
        return new SpeedStats(0f, 0f, 0f, false, false);
    }
}
