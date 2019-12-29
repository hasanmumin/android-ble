package com.hasanmumin.ble.client;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import java.util.List;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public abstract class ClientScanCallback extends ScanCallback {
    abstract void onScanComplete(BluetoothDevice bluetoothDevice);

    abstract void onScanFailed(String message, int errorCode);

    @Override
    public void onBatchScanResults(@NonNull List<ScanResult> results) {
        if (!results.isEmpty()) {
            onScanComplete(results.get(0).getDevice());
        } else {
            onScanFailed("Result is empty %s", -1);
        }
    }

    @Override
    public void onScanResult(int callbackType, @NonNull ScanResult result) {
        onScanComplete(result.getDevice());
    }

    @Override
    public void onScanFailed(int errorCode) {
        onScanFailed("UNKNOWN %s", errorCode);
    }
}
