package com.hasanmumin.ble.client;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.data.Data;

public class ClientManager extends BleManager<BleManagerCallbacks> {

    public final static UUID DEVICE_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    public final static UUID DEVICE_UUID_CHAR_WRITE = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
    public final static UUID DEVICE_UUID_CHAR_NOTIFY = UUID.fromString("00001525-1212-efde-1523-785feabcd123");

    private final ClientActivity mClientActivity;

    private final ClientDataCallback mClientDataCallback = new ClientDataCallback() {

        @Override
        void onDataAvailable(String message, Object... arguments) {
            log(message, arguments);
        }
    };

    private BluetoothGattCharacteristic mCharacteristicWrite;
    private BluetoothGattCharacteristic mCharacteristicNotify;
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {


        @Override
        protected void initialize() {
            setNotificationCallback(mCharacteristicNotify).with(mClientDataCallback);

            readCharacteristic(mCharacteristicWrite).with(mClientDataCallback).enqueue();
            readCharacteristic(mCharacteristicNotify).with(mClientDataCallback).enqueue();

            enableNotifications(mCharacteristicNotify).enqueue();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(DEVICE_UUID_SERVICE);
            if (service != null) {
                mCharacteristicWrite = service.getCharacteristic(DEVICE_UUID_CHAR_WRITE);
                mCharacteristicNotify = service.getCharacteristic(DEVICE_UUID_CHAR_NOTIFY);
            }
            return mCharacteristicWrite != null && mCharacteristicNotify != null;
        }

        @Override
        protected boolean isOptionalServiceSupported(@NonNull BluetoothGatt gatt) {
            return false;
        }

        @Override
        protected void onDeviceDisconnected() {
            mCharacteristicWrite = null;
            mCharacteristicNotify = null;
        }
    };


    public ClientManager(@NonNull Context context) {
        super(context);
        mClientActivity = (ClientActivity) context;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    private void log(String message, Object... arguments) {
        mClientActivity.log(message, arguments);
    }

    public void send(String message) {
        log("Sending message to server : %s", message);
        writeCharacteristic(mCharacteristicWrite, Data.from(message))
                .with(mClientDataCallback).enqueue();
    }
}
