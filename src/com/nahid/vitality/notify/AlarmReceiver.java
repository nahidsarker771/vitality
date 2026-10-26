package com.nahid.vitality.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.data.HistoryDb;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_TICK = "com.nahid.vitality.action.TICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TICK.equals(intent.getAction())) {
            return;
        }
        float percent = PowerTime.nowPercent();
        HistoryDb.sample(context, percent);
        if (percent > 0f) {
            Notifier.check(context, percent);
        }
        WidgetRefresher.updateAll(context);
    }
}
