package com.hasanmumin.ble.server;

import android.app.Application;

import com.hasanmumin.ble.server.log.Slf4jHelper;

public class ServerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Slf4jHelper.configure();
    }
}
