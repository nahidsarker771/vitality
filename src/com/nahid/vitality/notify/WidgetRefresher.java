package com.nahid.vitality.notify;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.nahid.vitality.widget.WidgetCompact;
import com.nahid.vitality.widget.WidgetLarge;
import com.nahid.vitality.widget.WidgetMedium;
import com.nahid.vitality.widget.WidgetWide;

public final class WidgetRefresher {

    private static final Class<?>[] PROVIDERS = {
            WidgetCompact.class, WidgetWide.class, WidgetMedium.class, WidgetLarge.class
    };

    private WidgetRefresher() {
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (Class<?> provider : PROVIDERS) {
            ComponentName component = new ComponentName(context, provider);
            int[] ids = manager.getAppWidgetIds(component);
            if (ids.length == 0) {
                continue;
            }
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setComponent(component);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        }
    }
}
