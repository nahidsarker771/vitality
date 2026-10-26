package com.nahid.vitality.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.nahid.vitality.core.ThemeManager;

public class VitalityBarView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float value;
    private float target;
    private int startColor = 0xFF8B7CFF;
    private int endColor = 0xFFB3A9FF;
    private ValueAnimator animator;
    private float density;

    public VitalityBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        trackPaint.setColor(0x33808080);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        capPaint.setColor(endColor);
    }

    public void setColors(int start, int end) {
        startColor = start;
        endColor = end;
        capPaint.setColor(end);
        invalidate();
    }

    public void setValueInstant(float next) {
        if (animator != null) {
            animator.cancel();
        }
        value = Math.max(0f, Math.min(100f, next));
        target = value;
        invalidate();
    }

    public void setValue(float next) {
        float clamped = Math.max(0f, Math.min(100f, next));
        target = clamped;
        if (animator != null) {
            animator.cancel();
        }
        if (Math.abs(clamped - value) < 0.6f) {
            value = clamped;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(value, clamped);
        animator.setDuration(value == 0f ? 900L : 350L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            value = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float radius = height / 2f;
        rect.set(0f, 0f, width, height);
        canvas.drawRoundRect(rect, radius, radius, trackPaint);

        float filled = width * value / 100f;
        if (filled > 0.5f) {
            float fillRadius = Math.min(radius, filled / 2f);
            fillPaint.setShader(new LinearGradient(0f, 0f, Math.max(filled, 1f), 0f,
                    startColor, endColor, Shader.TileMode.CLAMP));
            rect.set(0f, 0f, filled, height);
            canvas.drawRoundRect(rect, fillRadius, fillRadius, fillPaint);

            capPaint.setAlpha(230);
            float capRadius = Math.max(1.5f * density, height * 0.26f);
            float centerX = Math.max(fillRadius, filled - fillRadius);
            canvas.drawCircle(centerX, height / 2f, capRadius, capPaint);
            capPaint.setAlpha(255);
        } else {
            value = target;
        }
    }
}
