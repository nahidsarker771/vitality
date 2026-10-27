package com.nahid.vitality.track;

public final class StepMetrics {

    public static final double STEP_METERS = 0.762;

    private StepMetrics() {
    }

    public static double kmh(int steps, int minutes) {
        if (steps <= 0 || minutes <= 0) {
            return 0.0;
        }
        double meters = steps * STEP_METERS;
        double seconds = minutes * 60.0;
        return meters / seconds * 3.6;
    }
}
