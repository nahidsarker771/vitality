package com.nahid.vitality.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;

import java.time.LocalTime;
import java.util.Arrays;

public class DayActivityChartView extends View implements ThemeAware {

    private static final int HOURS = 24;

    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] hourly = new int[HOURS];
    private Theme theme;
    private int currentHour;
    private int maxSteps = 1;
    private boolean hasData;

    public DayActivityChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(ThemeManager.dp(context, 9f) * 1.15f);
        guidePaint.setStrokeWidth(ThemeManager.dp(context, 1f));
    }

    public void setMinuteSteps(int[] perMinute) {
        Arrays.fill(hourly, 0);
        if (perMinute != null) {
            int count = Math.min(perMinute.length, HOURS * 60);
            for (int minute = 0; minute < count; minute++) {
                hourly[minute / 60] += Math.max(0, perMinute[minute]);
            }
        }
        currentHour = LocalTime.now().getHour();
        maxSteps = 1;
        hasData = false;
        for (int steps : hourly) {
            if (steps > 0) {
                hasData = true;
            }
            maxSteps = Math.max(maxSteps, steps);
        }
        invalidate();
    }

    public boolean hasData() {
        return hasData;
    }

    @Override
    public void applyTheme(Theme theme) {
        this.theme = theme;
        labelPaint.setColor(theme.muted);
        ghostPaint.setColor(ThemeManager.withAlpha(theme.border, 0.55f));
        guidePaint.setColor(ThemeManager.withAlpha(theme.muted, 0.35f));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (theme == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float left = ThemeManager.dp(getContext(), 2f);
        float right = width - ThemeManager.dp(getContext(), 2f);
        float top = ThemeManager.dp(getContext(), 8f);
        float labelHeight = ThemeManager.dp(getContext(), 20f);
        float baseline = height - labelHeight;
        float chartHeight = baseline - top;
        if (chartHeight <= 0) {
            return;
        }

        canvas.drawLine(left, baseline, right, baseline, guidePaint);

        float slot = (right - left) / HOURS;
        float barWidth = Math.min(ThemeManager.dp(getContext(), 12f), slot * 0.58f);
        float corner = Math.min(ThemeManager.dp(getContext(), 4f), barWidth / 2f);

        for (int hour = 0; hour < HOURS; hour++) {
            float centerX = left + slot * hour + slot / 2f;
            float barLeft = centerX - barWidth / 2f;
            float barRight = centerX + barWidth / 2f;

            rect.set(barLeft, top, barRight, baseline);
            canvas.drawRoundRect(rect, corner, corner, ghostPaint);

            int steps = hourly[hour];
            if (steps > 0) {
                float ratio = Math.min(1f, steps / (float) maxSteps);
                float barTop = baseline - chartHeight * ratio;
                if (baseline - barTop < ThemeManager.dp(getContext(), 3f)) {
                    barTop = baseline - ThemeManager.dp(getContext(), 3f);
                }
                barPaint.setShader(new LinearGradient(0f, barTop, 0f, baseline,
                        ThemeManager.lighten(theme.will, 0.1f),
                        ThemeManager.blend(theme.energy, theme.surface, 0.25f),
                        Shader.TileMode.CLAMP));
                rect.set(barLeft, barTop, barRight, baseline);
                canvas.drawRoundRect(rect, corner, corner, barPaint);
            }
        }

        float labelY = height - ThemeManager.dp(getContext(), 4f);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("12a", left, labelY, labelPaint);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("6a", left + slot * 6.5f, labelY, labelPaint);
        canvas.drawText("12p", left + slot * 12.5f, labelY, labelPaint);
        canvas.drawText("6p", left + slot * 18.5f, labelY, labelPaint);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("12a", right, labelY, labelPaint);
    }
}
