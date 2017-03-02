package com.ihs.chargingscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ihs.chargingscreen.utils.ChargingManagerUtil;

import static com.ihs.chargingscreen.KeepAliveService.ACTION_START_CHARGING_ACTIVITY;
import static com.ihs.chargingscreen.KeepAliveService.isServiceRunning;

/**
 * Created by Arthur on 17/3/1.
 */

public class ChargingBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_START_CHARGING_ACTIVITY)) {
            ChargingManagerUtil.enableChargingAndStartActivity();
        } else if (!isServiceRunning) {
            context.startService(new Intent(context, KeepAliveService.class));
        }
    }
}
