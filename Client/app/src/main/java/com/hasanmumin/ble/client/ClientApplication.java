package com.hasanmumin.ble.client;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.hasanmumin.ble.client.log.Slf4jHelper;
import com.hasanmumin.ble.client.receiver.EveryOneMinuteReceiver;

public class ClientApplication extends Application {

    private static ClientActivity sActivity;

    public static ClientActivity getActivity() {
        return sActivity;
    }

    public static void setActivity(ClientActivity sActivity) {
        ClientApplication.sActivity = sActivity;
    }

    AlarmManager getAlarmManager() {
        return (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAlarm(int minute, Class<?> clazz) {
        PendingIntent pendingIntent = getBroadcastPendingIntent(clazz);
        setRepeating(minute, pendingIntent);
    }

    private void setRepeating(int minute, PendingIntent pendingIntent) {
        getAlarmManager().setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minute, minute, pendingIntent);
    }

    private PendingIntent getBroadcastPendingIntent(Class<?> clazz) {
        return PendingIntent.getBroadcast(this, 0, new Intent(this, clazz), 0);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Slf4jHelper.configure();
        ClientActivity.disableBluetooth();
        scheduleAlarm(1000 * 60, EveryOneMinuteReceiver.class);
    }
}
