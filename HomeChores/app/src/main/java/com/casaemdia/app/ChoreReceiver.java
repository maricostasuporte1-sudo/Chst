package com.casaemdia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationScheduler.showDaily(context);
        NotificationScheduler.scheduleNext(context);
    }
}
