package com.hasanmumin.ble.client;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import java.util.Objects;

import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class ClientDataCallback implements ProfileDataCallback, DataSentCallback {
    abstract void onDataAvailable(String message, Object... arguments);

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        onDataAvailable("onDataReceived() data is %s", new String(Objects.requireNonNull(data.getValue())));
    }

    @Override
    public void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {
        onDataAvailable("onDataSent() data is %s", new String(Objects.requireNonNull(data.getValue())));
    }

    @Override
    public void onInvalidDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        onDataAvailable("onInvalidDataReceived() data is %s", new String(Objects.requireNonNull(data.getValue())));
    }
}
