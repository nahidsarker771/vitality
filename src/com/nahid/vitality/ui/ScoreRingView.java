package com.nahid.vitality.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;

public class ScoreRingView extends View implements ThemeAware {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private Theme theme;
    private float value;
    private ValueAnimator animator;
    private final float strokeWidth;

    public ScoreRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        strokeWidth = ThemeManager.dp(context, 12f);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0x33222C44);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFF8B7CFF);
    }

    public void setValue(float next) {
        float clamped = Math.max(0f, Math.min(100f, next));
        if (animator != null) {
            animator.cancel();
        }
        if (Math.abs(clamped - value) < 0.6f) {
            value = clamped;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(value, clamped);
        animator.setDuration(value == 0f ? 950L : 350L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            value = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    public void applyTheme(Theme theme) {
        this.theme = theme;
        trackPaint.setColor(ThemeManager.withAlpha(theme.border, 0.9f));
        dotPaint.setColor(theme.will);
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
        float size = Math.min(width, height);
        float inset = strokeWidth / 2f + ThemeManager.dp(getContext(), 2f);
        oval.set((width - size) / 2f + inset,
                (height - size) / 2f + inset,
                (width + size) / 2f - inset,
                (height + size) / 2f - inset);
        float centerX = oval.centerX();
        float centerY = oval.centerY();
        float radius = oval.width() / 2f;

        canvas.drawCircle(centerX, centerY, radius, trackPaint);

        if (value > 0.1f) {
            SweepGradient gradient = new SweepGradient(centerX, centerY,
                    new int[]{theme.will, theme.wealth, theme.energy, theme.will},
                    new float[]{0f, 0.34f, 0.67f, 1f});
            Matrix matrix = new Matrix();
            matrix.setRotate(-90f, centerX, centerY);
            gradient.setLocalMatrix(matrix);
            arcPaint.setShader(gradient);
            canvas.drawArc(oval, -90f, value * 3.6f, false, arcPaint);

            double angle = Math.toRadians(-90f + value * 3.6f);
            float dotX = centerX + radius * (float) Math.cos(angle);
            float dotY = centerY + radius * (float) Math.sin(angle);
            dotPaint.setColor(theme.will);
            canvas.drawCircle(dotX, dotY, ThemeManager.dp(getContext(), 4.2f), dotPaint);
            dotPaint.setColor(ThemeManager.withAlpha(theme.will, 0.22f));
            canvas.drawCircle(dotX, dotY, ThemeManager.dp(getContext(), 8.5f), dotPaint);
        }
    }
}
