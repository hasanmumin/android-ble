package com.hasanmumin.ble.client.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hasanmumin.ble.client.ClientApplication;

public class EveryOneMinuteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ClientApplication.getActivity().isConnected()) {
            ClientApplication.getActivity().startScan();
        }
    }
}
