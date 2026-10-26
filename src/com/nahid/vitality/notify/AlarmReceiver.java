package com.nahid.vitality.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nahid.vitality.core.EnergyModel;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.track.Tracker;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_TICK = "com.nahid.vitality.action.TICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TICK.equals(intent.getAction())) {
            return;
        }
        Tracker.tick(context);
        float percent = PowerTime.nowPercent();
        float energy = EnergyModel.current(context);
        if (percent > 0f) {
            HistoryDb.sample(context, percent, percent, energy);
            Notifier.check(context, percent, percent, energy);
        }
        WidgetRefresher.updateAll(context);
    }
}
