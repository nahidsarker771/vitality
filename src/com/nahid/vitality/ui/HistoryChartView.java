package com.nahid.vitality.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HistoryChartView extends View implements ThemeAware {

    public static final class Point {
        public final LocalDate date;
        public final float value;
        public final boolean hasData;

        public Point(LocalDate date, float value, boolean hasData) {
            this.date = date;
            this.value = value;
            this.hasData = hasData;
        }
    }

    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Point> points = new ArrayList<>();
    private Theme theme;

    public HistoryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ghostPaint.setColor(0x22FFFFFF);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(ThemeManager.dp(context, 9f) * 1.15f);
        guidePaint.setColor(0x22FFFFFF);
        guidePaint.setStrokeWidth(ThemeManager.dp(context, 1f));
        guidePaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{ThemeManager.dp(context, 4f), ThemeManager.dp(context, 5f)}, 0f));
    }

    public void setPoints(List<Point> next) {
        points.clear();
        points.addAll(next);
        invalidate();
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
        if (theme == null || points.isEmpty()) {
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

        canvas.drawLine(left, top + chartHeight / 2f, right, top + chartHeight / 2f, guidePaint);

        int count = points.size();
        float slot = (right - left) / count;
        float barWidth = Math.min(ThemeManager.dp(getContext(), 18f), slot * 0.56f);
        float corner = Math.min(ThemeManager.dp(getContext(), 5f), barWidth / 2f);
        LocalDate today = LocalDate.now();

        for (int i = 0; i < count; i++) {
            Point point = points.get(i);
            float centerX = left + slot * i + slot / 2f;
            float barLeft = centerX - barWidth / 2f;
            float barRight = centerX + barWidth / 2f;

            rect.set(barLeft, top, barRight, baseline);
            canvas.drawRoundRect(rect, corner, corner, ghostPaint);

            if (point.hasData && point.value > 0.5f) {
                float barTop = baseline - chartHeight * Math.min(100f, point.value) / 100f;
                barPaint.setShader(new LinearGradient(0f, barTop, 0f, baseline,
                        ThemeManager.lighten(theme.will, 0.1f),
                        ThemeManager.blend(theme.energy, theme.surface, 0.25f),
                        Shader.TileMode.CLAMP));
                rect.set(barLeft, barTop, barRight, baseline);
                canvas.drawRoundRect(rect, corner, corner, barPaint);
            }

            labelPaint.setColor(point.date.equals(today) ? theme.will : theme.muted);
            canvas.drawText(PowerTime.weekdayNarrow(point.date),
                    centerX, height - ThemeManager.dp(getContext(), 4f), labelPaint);
        }
    }
}
