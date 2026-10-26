package com.nahid.vitality.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.nahid.vitality.MainActivity;
import com.nahid.vitality.R;
import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.Themes;

public abstract class BaseVitalityWidget extends AppWidgetProvider {

    protected abstract int layoutRes();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Theme theme = Themes.get(AppPrefs.themeIndex(context));
        float percent = PowerTime.nowPercent();
        float energy = EnergyModel.current(context);
        RemoteViews views = build(context, theme, percent, energy);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private RemoteViews build(Context context, Theme theme, float percent, float energy) {
        int layout = layoutRes();
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        String value = PowerTime.roundedPercent((percent * 2f + energy) / 3f) + "%";
        String base = PowerTime.roundedPercent(percent) + "%";
        String energyValue = PowerTime.roundedPercent(energy) + "%";

        views.setInt(R.id.widgetRoot, "setBackgroundResource", backgroundFor(theme.index));
        views.setTextColor(R.id.wTitle, theme.will);
        views.setTextColor(R.id.wOverall, theme.text);
        views.setTextColor(R.id.wWillLabel, theme.muted);
        views.setTextColor(R.id.wWealthLabel, theme.muted);
        views.setTextColor(R.id.wEnergyLabel, theme.muted);
        views.setTextColor(R.id.wWillPct, theme.text);
        views.setTextColor(R.id.wWealthPct, theme.text);
        views.setTextColor(R.id.wEnergyPct, theme.text);

        views.setTextViewText(R.id.wOverall, value);
        views.setTextViewText(R.id.wWillPct, base);
        views.setTextViewText(R.id.wWealthPct, base);
        views.setTextViewText(R.id.wEnergyPct, energyValue);
        views.setProgressBar(R.id.wWillBar, 100, PowerTime.roundedPercent(percent), false);
        views.setProgressBar(R.id.wWealthBar, 100, PowerTime.roundedPercent(percent), false);
        views.setProgressBar(R.id.wEnergyBar, 100, PowerTime.roundedPercent(energy), false);

        if (layout == R.layout.widget_medium || layout == R.layout.widget_large) {
            views.setTextViewText(R.id.wTime,
                    context.getString(R.string.widget_updated, PowerTime.clockNow()));
            views.setTextColor(R.id.wTime, theme.muted);
        }
        if (layout == R.layout.widget_large) {
            views.setTextColor(R.id.wReset, theme.muted);
            views.setTextViewText(R.id.wReset, context.getString(R.string.widget_resets));
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pending);
        return views;
    }

    private int backgroundFor(int themeIndex) {
        switch (themeIndex) {
            case 1:
                return R.drawable.widget_bg_1;
            case 2:
                return R.drawable.widget_bg_2;
            case 3:
                return R.drawable.widget_bg_3;
            case 4:
                return R.drawable.widget_bg_4;
            case 5:
                return R.drawable.widget_bg_5;
            case 6:
                return R.drawable.widget_bg_6;
            case 7:
                return R.drawable.widget_bg_7;
            default:
                return R.drawable.widget_bg_0;
        }
    }
}
